// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.push.impl.notifications

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import org.junit.Test

class ManagedNotificationSenderTest {
    private val current = SessionId("@recipient:antintwrk.com")
    private val caller = UserId("@caller:antintwrk.com")

    @Test
    fun `a previously stored account can call the current managed actor`() {
        assertThat(suppressNotificationSender(caller, current, setOf(caller), managed = true)).isFalse()
    }

    @Test
    fun `own managed events cannot ring oneself`() {
        assertThat(suppressNotificationSender(UserId(current.value), current, emptySet(), managed = true)).isTrue()
    }

    @Test
    fun `classic multiple account suppression is preserved`() {
        assertThat(suppressNotificationSender(caller, current, setOf(caller), managed = false)).isTrue()
    }
}
