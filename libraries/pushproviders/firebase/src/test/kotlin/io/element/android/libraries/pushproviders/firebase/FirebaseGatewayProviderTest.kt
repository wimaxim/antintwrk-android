/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.libraries.pushproviders.firebase

import com.google.common.truth.Truth.assertThat
import io.element.android.features.enterprise.test.FakeEnterpriseService
import org.junit.Test

class FirebaseGatewayProviderTest {
    @Test
    fun `native build uses own gateway without consulting enterprise configuration`() {
        var calls = 0
        val provider = DefaultFirebaseGatewayProvider(FakeEnterpriseService(firebasePushGatewayResult = {
            calls++
            "https://enterprise.example/_matrix/push/v1/notify"
        }))
        assertThat(provider.getFirebaseGateway()).isEqualTo(
            if (BuildConfig.ANTINTWRK_NATIVE_AUTH) {
                FirebaseConfig.ANTINTWRK_PUSHER_HTTP_URL
            } else {
                "https://enterprise.example/_matrix/push/v1/notify"
            }
        )
        assertThat(calls).isEqualTo(if (BuildConfig.ANTINTWRK_NATIVE_AUTH) 0 else 1)
    }

    @Test
    fun `unconfigured original build retains the upstream fallback`() {
        val provider = DefaultFirebaseGatewayProvider(FakeEnterpriseService(firebasePushGatewayResult = { null }))
        assertThat(provider.getFirebaseGateway()).isEqualTo(
            if (BuildConfig.ANTINTWRK_NATIVE_AUTH) FirebaseConfig.ANTINTWRK_PUSHER_HTTP_URL else FirebaseConfig.PUSHER_HTTP_URL
        )
    }
}
