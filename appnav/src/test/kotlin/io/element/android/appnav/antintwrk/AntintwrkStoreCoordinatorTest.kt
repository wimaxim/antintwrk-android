// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AntintwrkStoreCoordinatorTest {
    private val server = MockWebServer()
    private val actor = "@u18:antintwrk.com"
    private val account = "a".repeat(64)
    private val profile = "b".repeat(64)
    private val token = "synthetic-private-store-token"
    private val fake = object : AntintwrkStore {
        override val connection = MutableStateFlow(AntintwrkStoreConnection.Ready)
        override val updates = MutableSharedFlow<List<AntintwrkStorePurchase>>()
        var launchResult = AntintwrkStoreLaunch.Started
        var launches = 0
        var lastProfile: String? = null
        var ownedPurchases = emptyList<AntintwrkStorePurchase>()
        var failPrices = false
        override suspend fun prices(products: List<AntintwrkStoreProduct>): List<AntintwrkStorePrice> {
            check(!failPrices)
            return products.map { AntintwrkStorePrice(it, "€4.99") }
        }
        override suspend fun owned() = ownedPurchases
        override fun launch(product: AntintwrkStoreProduct, accountId: String, profileId: String): AntintwrkStoreLaunch {
            assertThat(accountId).isEqualTo(account)
            launches++
            lastProfile = profileId
            return launchResult
        }
    }
    private lateinit var coordinator: AntintwrkStoreCoordinator
    @Before
    fun setup() {
        server.start()
        coordinator = AntintwrkStoreCoordinator(
            fake,
            AntintwrkNativeAuthClient(server.url("/").toString().trimEnd('/')),
            AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "c".repeat(64), matrixUserId = actor),
            "captured-session=original",
            "com.antintwrk.app"
        )
    }
    @After
    fun cleanup() = server.shutdown()
    private fun enqueue(fields: String, responseActor: String = actor) = server.enqueue(
        MockResponse().setBody("""{"ok":true,"matrixUserId":"$responseActor",$fields}""")
    )
    private suspend fun catalog() {
        enqueue(
            """"accountId":"$account","products":[{"key":"points","productId":"points_1000","type":"inapp","purpose":"kindness","points":1000,"purchaseOptionId":"buy"}]"""
        )
        coordinator.catalog()
        server.takeRequest()
    }
    @Test
    fun `store receives opaque ownership and web receives only displayed prices`() = runTest {
        enqueue(
            """"accountId":"$account","products":[{"key":"points","productId":"points_1000","type":"inapp","purpose":"kindness","points":1000,"purchaseOptionId":"buy"}]"""
        )
        val result = coordinator.catalog().toString()
        assertThat(result).contains("€4.99")
        assertThat(result).doesNotContain(account)
        assertThat(result).doesNotContain("points_1000")
        val request = server.takeRequest()
        assertThat(request.getHeader("Cookie")).isEqualTo("captured-session=original")
        assertThat(request.body.readUtf8()).contains("package=com.antintwrk.app")
        enqueue(""""accountId":"$account","profileId":"$profile"""")
        val launched = coordinator.purchase(JSONObject().put("key", "points"))
        assertThat(launched.getBoolean("pending")).isTrue()
        assertThat(launched.has("verified")).isFalse()
        assertThat(fake.lastProfile).isEqualTo(profile)
        val order = server.takeRequest().body.readUtf8()
        assertThat(order).contains("applicationId=0")
        assertThat(order).doesNotContain(token)
    }
    @Test
    fun `charity and unsupported product types cannot enter native checkout`() = runTest {
        for ((purpose, type) in listOf("ukraine" to "inapp", "david" to "inapp", "foundation" to "inapp", "kindness" to "subs", "verification" to "unknown")) {
            enqueue(""""accountId":"$account","products":[{"key":"forbidden","productId":"foreign","type":"$type","purpose":"$purpose"}]""")
            assertThat(runCatching { coordinator.catalog() }.isFailure).isTrue()
            server.takeRequest()
            assertThat(runCatching { coordinator.purchase(JSONObject().put("key", "forbidden")) }.isFailure).isTrue()
        }
        assertThat(fake.launches).isEqualTo(0)
        assertThat(server.requestCount).isEqualTo(5)
    }
    @Test
    fun `failed catalog refresh cannot launch an old offer`() = runTest {
        catalog()
        fake.failPrices = true
        enqueue(""""accountId":"$account","products":[{"key":"points","productId":"points_1000","type":"inapp","purpose":"kindness"}]""")
        assertThat(runCatching { coordinator.catalog() }.isFailure).isTrue()
        server.takeRequest()
        assertThat(runCatching { coordinator.purchase(JSONObject().put("key", "points")) }.isFailure).isTrue()
        assertThat(fake.launches).isEqualTo(0)
        assertThat(server.requestCount).isEqualTo(2)
    }
    @Test
    fun `other social account purchase is ignored without any verification request`() = runTest {
        catalog()
        assertThat(coordinator.verify(AntintwrkStorePurchase(token, listOf("points_1000"), true, "d".repeat(64), profile))).isFalse()
        assertThat(server.requestCount).isEqualTo(1)
    }
    @Test
    fun `pending purchase is checked server side and never assumed paid`() = runTest {
        catalog()
        enqueue(""""verified":false,"state":"pending"""")
        assertThat(coordinator.verify(AntintwrkStorePurchase(token, listOf("points_1000"), false, account, profile))).isFalse()
        val request = server.takeRequest()
        assertThat(request.getHeader("Cookie")).isEqualTo("captured-session=original")
        assertThat(request.body.readUtf8()).contains("purchaseToken=$token")
    }
    @Test
    fun `retired SKU restores through immutable server order`() = runTest {
        catalog()
        enqueue(""""verified":true,"changed":false""")
        assertThat(coordinator.verify(AntintwrkStorePurchase(token, listOf("retired_sku"), true, account, profile))).isTrue()
        server.takeRequest()
    }
    @Test
    fun `session change in server response cannot launch checkout`() = runTest {
        catalog()
        enqueue(""""accountId":"$account","profileId":"$profile""", "@u99:antintwrk.com")
        assertThat(runCatching { coordinator.purchase(JSONObject().put("key", "points")) }.isFailure).isTrue()
        assertThat(fake.launches).isEqualTo(0)
        server.takeRequest()
    }
    @Test
    fun `malformed IDs multi product and missing ownership cannot request a credit`() = runTest {
        catalog()
        for (purchase in listOf(
            AntintwrkStorePurchase(token, listOf("points_1000"), true, account, "malformed"),
            AntintwrkStorePurchase(token, listOf("points_1000", "another"), true, account, profile),
            AntintwrkStorePurchase(token, listOf("points_1000"), true, account, null),
        )) {
            assertThat(coordinator.verify(purchase)).isFalse()
        }
        assertThat(server.requestCount).isEqualTo(1)
    }
    @Test
    fun `already owned checkout provides a distinct actionable outcome`() = runTest {
        catalog()
        fake.launchResult = AntintwrkStoreLaunch.AlreadyOwned
        enqueue(""""accountId":"$account","profileId":"$profile"""")
        val error = runCatching { coordinator.purchase(JSONObject().put("key", "points")) }.exceptionOrNull()
        assertThat((error as AntintwrkStoreError).code).isEqualTo("STORE_ALREADY_OWNED")
        server.takeRequest()
    }
    @Test
    fun `purchase diagnostics never disclose token or opaque ownership`() {
        val purchase = AntintwrkStorePurchase(token, listOf("points_1000"), true, account, profile)
        assertThat(purchase.toString()).doesNotContain(token)
        assertThat(purchase.toString()).doesNotContain(profile)
    }
}
