// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.x

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import io.element.android.appnav.antintwrk.AntintwrkStore
import io.element.android.appnav.antintwrk.AntintwrkStoreConnection
import io.element.android.appnav.antintwrk.AntintwrkStoreLaunch
import io.element.android.appnav.antintwrk.AntintwrkStorePrice
import io.element.android.appnav.antintwrk.AntintwrkStoreProduct
import io.element.android.appnav.antintwrk.AntintwrkStorePurchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.lang.ref.WeakReference

internal class AntintwrkStoreAdapter(
    activity: Activity,
    private val transport: AntintwrkPlayBilling,
    scope: CoroutineScope,
    private val canLaunch: () -> Boolean,
) : AntintwrkStore {
    private val host = WeakReference(activity)
    private data class Offer(val details: ProductDetails, val token: String, val price: String)
    private val offers = mutableMapOf<AntintwrkStoreProduct, Offer>()
    override val connection = transport.connection.map { AntintwrkStoreConnection.valueOf(it.name) }
        .stateIn(scope, SharingStarted.Eagerly, AntintwrkStoreConnection.Disconnected)
    override val updates = transport.purchaseUpdates.map { result ->
        if (result.successful) result.value.orEmpty().map(::candidate) else emptyList()
    }
    override suspend fun prices(products: List<AntintwrkStoreProduct>): List<AntintwrkStorePrice> {
        offers.clear()
        if (products.isEmpty()) return emptyList()
        require(products.size <= 20 && products.distinctBy { it.key }.size == products.size)
        // Play requires a homogeneous product type per query. The server catalogue has both.
        val detailsByType = products.groupBy { it.subscription }.values.flatMap { group ->
            val response = transport.products(group.map { product ->
                QueryProductDetailsParams.Product.newBuilder().setProductId(product.productId)
                    .setProductType(if (product.subscription) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP).build()
            })
            check(response.successful)
            response.value.orEmpty()
        }
        for (product in products) {
            val details = detailsByType.singleOrNull { it.productId == product.productId } ?: continue
            val offer = if (product.subscription) {
                details.subscriptionOfferDetails?.singleOrNull {
                    it.basePlanId == product.basePlanId && it.offerId == null && it.installmentPlanDetails == null
                }?.let {
                    val price = it.pricingPhases.pricingPhaseList.singleOrNull()?.takeIf { phase ->
                        phase.billingPeriod == "P1M" && phase.priceAmountMicros > 0 && phase.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING
                    } ?: return@let null
                    Offer(details, it.offerToken, price.formattedPrice)
                }
            } else {
                details.oneTimePurchaseOfferDetailsList?.singleOrNull {
                    it.purchaseOptionId == product.purchaseOptionId && it.offerId == null && it.priceAmountMicros > 0
                }?.let { Offer(details, it.offerToken ?: return@let null, it.formattedPrice) }
            }
            if (offer != null) offers[product] = offer
        }
        return products.mapNotNull { product -> offers[product]?.let { AntintwrkStorePrice(product, it.price) } }
    }
    override suspend fun owned(): List<AntintwrkStorePurchase> {
        val inapp = transport.purchases(BillingClient.ProductType.INAPP)
        val subscriptions = transport.purchases(BillingClient.ProductType.SUBS)
        check(inapp.successful && subscriptions.successful)
        return (inapp.value.orEmpty() + subscriptions.value.orEmpty()).map(::candidate)
    }
    override fun launch(product: AntintwrkStoreProduct, accountId: String, profileId: String): AntintwrkStoreLaunch {
        require(accountId.matches(Regex("[a-f0-9]{64}")) && profileId.matches(Regex("[a-f0-9]{64}")))
        if (!canLaunch()) return AntintwrkStoreLaunch.Unavailable
        val activity = host.get()?.takeUnless { it.isFinishing || it.isDestroyed } ?: return AntintwrkStoreLaunch.Unavailable
        val offer = offers[product] ?: return AntintwrkStoreLaunch.Unavailable
        return when (transport.launch(activity, offer.details, offer.token, accountId, profileId)) {
            BillingClient.BillingResponseCode.OK -> AntintwrkStoreLaunch.Started
            BillingClient.BillingResponseCode.USER_CANCELED -> AntintwrkStoreLaunch.Cancelled
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> AntintwrkStoreLaunch.AlreadyOwned
            else -> AntintwrkStoreLaunch.Unavailable
        }
    }
    private fun candidate(purchase: Purchase) = AntintwrkStorePurchase(
        purchase.purchaseToken,
        purchase.products,
        purchase.purchaseState == Purchase.PurchaseState.PURCHASED,
        purchase.accountIdentifiers?.obfuscatedAccountId,
        purchase.accountIdentifiers?.obfuscatedProfileId,
    )
}
