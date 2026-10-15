// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class AntintwrkStoreConnection { Disconnected, Connecting, Ready, Unavailable, Closed }
data class AntintwrkStoreProduct(
    val key: String,
    val productId: String,
    val subscription: Boolean,
    val basePlanId: String? = null,
    val purchaseOptionId: String? = null,
    val purpose: String = "verification",
    val plan: String = "",
    val points: Int = 0,
)
data class AntintwrkStorePrice(val product: AntintwrkStoreProduct, val formatted: String)

/** Native memory/authenticated API only; never expose a token in WebView or diagnostics. */
class AntintwrkStorePurchase(
    val token: String,
    val products: List<String>,
    val purchased: Boolean,
    val accountId: String?,
    val profileId: String?,
) {
    override fun toString() = "AntintwrkStorePurchase(redacted)"
}
enum class AntintwrkStoreLaunch { Started, Cancelled, AlreadyOwned, Unavailable }
internal class AntintwrkStoreError(val code: String) : Exception(code)
interface AntintwrkStore {
    val connection: StateFlow<AntintwrkStoreConnection>
    val failures: Flow<String> get() = kotlinx.coroutines.flow.emptyFlow()
    val updates: Flow<List<AntintwrkStorePurchase>>
    suspend fun prices(products: List<AntintwrkStoreProduct>): List<AntintwrkStorePrice>
    suspend fun owned(): List<AntintwrkStorePurchase>

    /** Accepted checkout launch is never payment confirmation. */
    fun launch(product: AntintwrkStoreProduct, accountId: String, profileId: String): AntintwrkStoreLaunch
}
val LocalAntintwrkStore = staticCompositionLocalOf<AntintwrkStore?> { null }
