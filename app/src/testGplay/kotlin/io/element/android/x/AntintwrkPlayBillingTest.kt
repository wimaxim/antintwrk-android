// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.x

import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkPlayBillingTest {
    private val client = mockk<BillingClient>(relaxed = true)
    private val owner = mockk<LifecycleOwner>()
    private val listener = slot<BillingClientStateListener>()
    private val sut = AntintwrkPlayBilling(client)

    private fun result(code: Int) = BillingResult.newBuilder().setResponseCode(code).build()
    private fun connect(code: Int = BillingClient.BillingResponseCode.OK) {
        every { client.isReady } returns false
        every { client.startConnection(capture(listener)) } answers { listener.captured.onBillingSetupFinished(result(code)) }
        sut.onStart(owner)
    }

    @Test
    fun `connection failure never grants anything or stops the host`() {
        connect(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
        assertThat(sut.connection.value).isEqualTo(AntintwrkPlayBilling.Connection.Unavailable)
        verify(exactly = 0) { client.acknowledgePurchase(any(), any()) }
        verify(exactly = 0) { client.consumeAsync(any(), any()) }
        verify(exactly = 0) { client.launchBillingFlow(any(), any()) }
    }

    @Test
    fun `foreground ready connection is not started twice`() {
        every { client.isReady } returns true
        sut.onStart(owner)
        sut.onStart(owner)
        assertThat(sut.connection.value).isEqualTo(AntintwrkPlayBilling.Connection.Ready)
        verify(exactly = 0) { client.startConnection(any()) }
    }

    @Test
    fun `connecting state prevents duplicate connection and destroy ignores late callbacks`() {
        every { client.isReady } returns false
        every { client.startConnection(capture(listener)) } returns Unit
        sut.onStart(owner)
        sut.onStart(owner)
        verify(exactly = 1) { client.startConnection(any()) }
        sut.onDestroy(owner)
        listener.captured.onBillingSetupFinished(result(BillingClient.BillingResponseCode.OK))
        listener.captured.onBillingServiceDisconnected()
        sut.onStart(owner)
        assertThat(sut.connection.value).isEqualTo(AntintwrkPlayBilling.Connection.Closed)
        verify(exactly = 1) { client.endConnection() }
        verify(exactly = 1) { client.startConnection(any()) }
    }

    @Test
    fun `failed restore is not interpreted as empty successful ownership`() = runTest {
        every { client.isReady } returns true
        every { client.queryPurchasesAsync(any(), any<PurchasesResponseListener>()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(result(BillingClient.BillingResponseCode.ERROR), emptyList())
        }
        assertThat(sut.purchases(BillingClient.ProductType.INAPP).successful).isFalse()
    }

    @Test
    fun `purchased and pending restore candidates never acknowledge consume or grant`() = runTest {
        val purchases = listOf(mockk<Purchase>(), mockk<Purchase>())
        every { client.isReady } returns true
        every { client.queryPurchasesAsync(any(), any<PurchasesResponseListener>()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(result(BillingClient.BillingResponseCode.OK), purchases)
        }
        for (type in listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS)) {
            assertThat(sut.purchases(type).value).containsExactlyElementsIn(purchases)
        }
        verify(exactly = 0) { client.acknowledgePurchase(any(), any()) }
        verify(exactly = 0) { client.consumeAsync(any(), any()) }
        verify(exactly = 0) { client.launchBillingFlow(any(), any()) }
    }

    @Test
    fun `closed or unavailable transport does not query Play`() = runTest {
        every { client.isReady } returns false
        assertThat(sut.purchases(BillingClient.ProductType.SUBS).successful).isFalse()
        sut.onDestroy(owner)
        every { client.isReady } returns true
        assertThat(sut.purchases(BillingClient.ProductType.SUBS).successful).isFalse()
        verify(exactly = 0) { client.queryPurchasesAsync(any(), any<PurchasesResponseListener>()) }
    }

    @Test
    fun `unresponsive store query has a bounded timeout`() = runTest {
        every { client.isReady } returns true
        val response = sut.purchases(BillingClient.ProductType.SUBS)
        assertThat(response.code).isEqualTo(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
        assertThat(response.successful).isFalse()
    }
}
