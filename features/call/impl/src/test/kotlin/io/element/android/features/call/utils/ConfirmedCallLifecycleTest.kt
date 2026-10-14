// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.call.impl.utils

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConfirmedCallLifecycleTest {
    @Test
    fun `cached absent state cannot cancel but observed hangup can`() = runTest {
        assertThat(flowOf(false to false, false to false, true to false, false to false).confirmedCallLifecycle().toList())
            .containsExactly(true to false, false to false).inOrder()
    }

    @Test
    fun `answer on another device cancels without waiting for caller cache`() = runTest {
        assertThat(flowOf(false to false, false to true).confirmedCallLifecycle().toList()).containsExactly(false to true)
    }

    @Test
    fun `each subsequent call retains its own first observed state`() = runTest {
        repeat(2) {
            assertThat(flowOf(true to false, true to false, false to false).confirmedCallLifecycle().toList())
                .containsExactly(true to false, false to false).inOrder()
        }
    }
}
