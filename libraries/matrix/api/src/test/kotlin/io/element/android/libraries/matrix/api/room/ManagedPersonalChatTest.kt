// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.room

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.test.room.aRoomInfo
import org.junit.Test

class ManagedPersonalChatTest {
    @Test
    fun `direct metadata survives missing or departed member details`() {
        for (count in 0L..2L) assertThat(aRoomInfo(isDirect = true, isDm = false, activeMembersCount = count).isManagedPersonalChat()).isTrue()
    }

    @Test
    fun `two people do not turn an ordinary group into a personal chat`() {
        assertThat(aRoomInfo(isDirect = false, isDm = false, activeMembersCount = 2).isManagedPersonalChat()).isFalse()
        assertThat(aRoomInfo(isDirect = true, isDm = false, activeMembersCount = 3).isManagedPersonalChat()).isFalse()
        assertThat(aRoomInfo(isDirect = true, isDm = true, activeMembersCount = 3).isManagedPersonalChat()).isFalse()
    }

    @Test
    fun `spaces never acquire the personal chat actions`() {
        assertThat(aRoomInfo(isDirect = true, isDm = true, isSpace = true).isManagedPersonalChat()).isFalse()
    }
}
