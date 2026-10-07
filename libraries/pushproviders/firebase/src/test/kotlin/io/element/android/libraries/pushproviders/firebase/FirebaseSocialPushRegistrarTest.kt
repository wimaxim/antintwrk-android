// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.pushproviders.firebase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FirebaseSocialPushRegistrarTest {
    @Test
    fun `registration runs outside caller thread and preserves SDK failure`() = runTest {
        val caller = Thread.currentThread()
        val failure = IllegalStateException("fixture")
        var registrationThread: Thread? = null
        val registrar = FirebaseSocialPushRegistrar(RegisterFirebaseSession {
            registrationThread = Thread.currentThread()
            Result.failure(failure)
        })
        assertThat(registrar.register().exceptionOrNull()).isSameInstanceAs(failure)
        assertThat(registrationThread).isNotNull()
        assertThat(registrationThread).isNotSameInstanceAs(caller)
    }
}
