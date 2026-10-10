// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.x

import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Store transport only. Server verification owns entitlement, acknowledgement and consumption. */
internal class AntintwrkPlayBilling internal constructor(private val client: BillingClient) : DefaultLifecycleObserver {
    enum class Connection { Disconnected, Connecting, Ready, Unavailable, Closed }

    data class Response<T>(val code: Int, val value: T?) {
        val successful: Boolean get() = code == BillingClient.BillingResponseCode.OK && value != null
    }

    private val mutableConnection = MutableStateFlow(Connection.Disconnected)
    val connection = mutableConnection.asStateFlow()
    private val mutableUpdates = MutableSharedFlow<Response<List<Purchase>>>(extraBufferCapacity = 1)
    val purchaseUpdates = mutableUpdates.asSharedFlow()

    override fun onStart(owner: LifecycleOwner) = connect()

    internal fun connect() {
        if (connection.value in setOf(Connection.Closed, Connection.Connecting)) return
        if (client.isReady) {
            mutableConnection.value = Connection.Ready
            return
        }
        mutableConnection.value = Connection.Connecting
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (connection.value == Connection.Closed) return
                mutableConnection.value = if (result.responseCode == BillingClient.BillingResponseCode.OK) Connection.Ready else Connection.Unavailable
            }

            override fun onBillingServiceDisconnected() {
                if (connection.value != Connection.Closed) mutableConnection.value = Connection.Disconnected
                // SDK automatic reconnection is used on the next call; do not start a second connection here.
            }
        })
    }

    /** Prices/offers must come from Play. Product IDs will be agreed after the web verification flow is ready. */
    suspend fun products(requests: List<QueryProductDetailsParams.Product>): Response<List<ProductDetails>> {
        if (!awaitReady()) return unavailable()
        require(requests.isNotEmpty())
        return withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine { continuation ->
                client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(requests).build()) { result, details ->
                    if (continuation.isActive) continuation.resume(Response(result.responseCode, details.productDetailsList))
                }
            }
        } ?: unavailable()
    }

    /** PURCHASED and PENDING are untrusted candidates, never authorization to issue a badge or credit. */
    suspend fun purchases(type: String): Response<List<Purchase>> {
        require(type == BillingClient.ProductType.INAPP || type == BillingClient.ProductType.SUBS)
        if (!awaitReady()) return unavailable()
        return withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine { continuation ->
                client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                    if (continuation.isActive) continuation.resume(Response(result.responseCode, purchases))
                }
            }
        } ?: unavailable()
    }

    internal fun received(result: BillingResult, purchases: List<Purchase>?) {
        if (connection.value != Connection.Closed) mutableUpdates.tryEmit(Response(result.responseCode, purchases))
    }

    private fun available() = connection.value != Connection.Closed && client.isReady

    private suspend fun awaitReady(): Boolean {
        if (available()) return true
        if (connection.value == Connection.Closed) return false
        connect()
        withTimeoutOrNull(15_000) {
            connection.first { it in setOf(Connection.Ready, Connection.Unavailable, Connection.Closed) }
        }
        return available()
    }

    internal fun launch(activity: Activity, details: ProductDetails, offerToken: String, accountId: String, profileId: String): Int {
        if (!available()) return BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offerToken).build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item))
            .setObfuscatedAccountId(accountId).setObfuscatedProfileId(profileId).build()
        return client.launchBillingFlow(activity, params).responseCode
    }
    private fun <T> unavailable() = Response<T>(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, null)

    override fun onDestroy(owner: LifecycleOwner) {
        mutableConnection.value = Connection.Closed
        client.endConnection()
    }

    companion object {
        fun create(context: Context): AntintwrkPlayBilling {
            lateinit var transport: AntintwrkPlayBilling
            val client = BillingClient.newBuilder(context)
                .setListener { result, purchases -> transport.received(result, purchases) }
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build()
            transport = AntintwrkPlayBilling(client)
            return transport
        }
    }
}
