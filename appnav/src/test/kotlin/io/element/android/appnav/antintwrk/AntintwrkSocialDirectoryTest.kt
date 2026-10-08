// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.user.AntintwrkSocialDirectory
import io.element.android.libraries.matrix.api.user.MatrixSearchUserResults
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkSocialDirectoryTest {
    @Test
    fun `social catalogue is bound to the current actor`() = runTest {
        var contacted = false
        val binding = AntintwrkSocialDirectory.Binding("actor-one") {
            contacted = true
            MatrixSearchUserResults(persistentListOf(), false)
        }
        AntintwrkSocialDirectory.bind(binding)
        try {
            assertThat(AntintwrkSocialDirectory.search("actor-two", "test")).isNull()
            assertThat(contacted).isFalse()
            assertThat(AntintwrkSocialDirectory.search("actor-one", "test")).isNotNull()
        } finally {
            AntintwrkSocialDirectory.clear(binding)
        }
        assertThat(AntintwrkSocialDirectory.search("actor-one", "test")).isNull()
    }

    @Test
    fun `late response from a previous account is discarded`() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val old = AntintwrkSocialDirectory.Binding("old", search = {
            started.complete(Unit)
            release.await()
            MatrixSearchUserResults(persistentListOf(), false)
        })
        val next = AntintwrkSocialDirectory.Binding("next", search = { MatrixSearchUserResults(persistentListOf(), false) })
        AntintwrkSocialDirectory.bind(old)
        val pending = async { AntintwrkSocialDirectory.search("old", "test") }
        started.await()
        AntintwrkSocialDirectory.bind(next)
        AntintwrkSocialDirectory.clear(old)
        release.complete(Unit)
        try {
            assertThat(pending.await()).isNull()
            assertThat(AntintwrkSocialDirectory.search("next", "test")).isNotNull()
        } finally {
            AntintwrkSocialDirectory.clear(next)
        }
    }
}
