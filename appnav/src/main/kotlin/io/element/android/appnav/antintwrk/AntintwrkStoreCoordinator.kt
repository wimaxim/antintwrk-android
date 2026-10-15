// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One admitted actor/cookie snapshot. A stale store result can never credit the newly signed-in user. */
internal class AntintwrkStoreCoordinator(
    private val store: AntintwrkStore,
    private val client: AntintwrkNativeAuthClient,
    private val session: AntintwrkAuthSession,
    private val cookies: String,
    private val packageName: String,
) {
    private val actor = requireNotNull(session.matrixUserId)
    private val lock = Mutex()
    private var gratitudeRequestKey: String? = null
    fun currentGratitudeRequestKey(): String? = gratitudeRequestKey
    private var accountId: String? = null
    private var products = emptyList<AntintwrkStoreProduct>()
    private val launchedProfiles = mutableMapOf<String, String>()
    private suspend fun request(action: String, fields: Map<String, String> = emptyMap()): JSONObject =
        client.storeRequest(action, actor, session.csrf, fields + ("package" to packageName), cookies)

    suspend fun catalog(): JSONObject = lock.withLock {
        // A failed refresh must not leave a previously offered checkout launchable.
        products = emptyList()
        accountId = null
        val result = request("store-catalog", mapOf("support_v1" to "1"))
        val binding = result.getString("accountId").also { require(it.matches(Regex("[a-f0-9]{64}"))) }
        val items = result.getJSONArray("products")
        require(items.length() <= 20)
        products = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            require(item.getString("purpose") in setOf("verification", "kindness", "support"))
            require(item.getString("type") in setOf("inapp", "subs"))
            require(item.getString("purpose") !in setOf("kindness", "support") || item.getString("type") == "inapp")
            AntintwrkStoreProduct(
                item.getString("key"),
                item.getString("productId"),
                item.getString("type") == "subs",
                item.optString("basePlanId").takeIf(String::isNotEmpty),
                item.optString("purchaseOptionId").takeIf(String::isNotEmpty),
                item.getString("purpose"),
                item.optString("plan"),
                item.optInt("points"),
            )
        }
        val prices = store.prices(products)
        products = prices.map { it.product }
        accountId = binding
        JSONObject().put("ok", true).put("gratitudePurchaseVersion", 1).put("products", JSONArray().apply {
            prices.forEach {
                put(JSONObject().put("key", it.product.key).put("price", it.formatted).put("subscription", it.product.subscription)
                    .put("purpose", it.product.purpose).put("plan", it.product.plan).put("points", it.product.points))
            }
        })
    }

    suspend fun purchase(json: JSONObject): JSONObject = lock.withLock {
        val product = products.singleOrNull { it.key == json.optString("key") } ?: error("Unavailable")
        val binding = requireNotNull(accountId)
        val application = json.opt("applicationId")
        require(application == null || application is Int || application is Long)
        val gratitude = json.optJSONObject("gratitude")
        require(!json.has("gratitude") || gratitude != null)
        if (gratitude != null) {
            require(product.purpose == "kindness" && application == null)
            require(gratitude.optString("targetKind") in setOf("publication", "reel"))
            require(gratitude.optString("targetId").matches(Regex("[1-9][0-9]{0,18}")))
            require(gratitude.optString("requestKey").matches(Regex("[a-f0-9]{32}")))
        }
        val fields = mutableMapOf(
            "key" to product.key,
            "applicationId" to (application?.toString() ?: "0"),
            "requestKey" to (gratitude?.getString("requestKey") ?: UUID.randomUUID().toString().replace("-", "")),
        )
        if (gratitude != null) {
            fields["gratitudeTargetKind"] = gratitude.getString("targetKind")
            fields["gratitudeTargetId"] = gratitude.getString("targetId")
        }
        val order = request("store-order", fields)
        require(order.getString("accountId") == binding)
        val profile = order.getString("profileId").also { require(it.matches(Regex("[a-f0-9]{64}"))) }
        // A restored or delayed purchase must be verified before another checkout can open.
        if (gratitude != null) {
            gratitudeRequestKey = gratitude.getString("requestKey")
            val owned = if (order.optBoolean("replayed")) store.owned().filter { it.profileId == profile } else emptyList()
            for (purchase in owned) {
                require(purchase.accountId == binding && purchase.products == listOf(product.productId))
                request("store-verify", mapOf("profileId" to profile, "purchaseToken" to purchase.token))
            }
            if (owned.isNotEmpty() || !order.optBoolean("checkoutAllowed", false)) {
                return@withLock JSONObject().put("ok", true).put("pending", true)
            }
        } else {
            gratitudeRequestKey = null
        }
        launchedProfiles[product.productId] = profile
        when (store.launch(product, binding, profile)) {
            AntintwrkStoreLaunch.Started -> Unit
            AntintwrkStoreLaunch.Cancelled -> throw AntintwrkStoreError("STORE_CANCELLED")
            AntintwrkStoreLaunch.AlreadyOwned -> throw AntintwrkStoreError("STORE_ALREADY_OWNED")
            AntintwrkStoreLaunch.Unavailable -> throw AntintwrkStoreError("STORE_UNAVAILABLE")
        }
        JSONObject().put("ok", true).put("pending", true)
    }

    suspend fun restore(): Boolean {
        if (accountId == null) catalog()
        var changed = false
        for (purchase in store.owned()) if (verify(purchase)) changed = true
        return changed
    }

    suspend fun verify(purchase: AntintwrkStorePurchase): Boolean = lock.withLock {
        val profile = purchase.profileId ?: purchase.products.singleOrNull()?.let(launchedProfiles::get)
        if ((purchase.accountId != null && purchase.accountId != accountId) || profile?.matches(Regex("[a-f0-9]{64}")) != true ||
            purchase.products.size != 1) {
                return@withLock false
            }
        val result = request("store-verify", mapOf("profileId" to profile, "purchaseToken" to purchase.token))
        result.optBoolean("verified")
    }
}
