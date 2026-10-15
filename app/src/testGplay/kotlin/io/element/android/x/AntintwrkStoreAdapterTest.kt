// Copyright 2026 Super David Universe LLC.
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.x

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.QueryProductDetailsParams
import io.element.android.appnav.antintwrk.AntintwrkStoreProduct
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkStoreAdapterTest {
    @Test
    fun `mixed live catalogue is split into valid Play queries`() = runTest {
        val transport = mockk<AntintwrkPlayBilling>()
        every { transport.connection } returns MutableStateFlow(AntintwrkPlayBilling.Connection.Ready)
        every { transport.purchaseUpdates } returns MutableSharedFlow()
        coEvery { transport.products(any()) } answers {
            // Real Play builder throws for the old single mixed INAPP and SUBS query.
            QueryProductDetailsParams.newBuilder().setProductList(firstArg()).build()
            AntintwrkPlayBilling.Response(BillingClient.BillingResponseCode.OK, emptyList())
        }
        val adapter = AntintwrkStoreAdapter(mockk<Activity>(), transport, backgroundScope) { true }
        adapter.prices(listOf(
            AntintwrkStoreProduct("kindness_1000", "kindness_1000", false, null, "buy", "kindness", "", 1000),
            AntintwrkStoreProduct("verification_monthly", "verification_monthly", true, "monthly", null, "verification", "monthly", 0),
            AntintwrkStoreProduct("support_1000", "support_1000", false, null, "buy", "support", "", 1000),
        ))
        coVerify(exactly = 2) { transport.products(any()) }
    }
}
