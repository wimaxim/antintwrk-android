/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.roomlist

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.matrix.rustcomponents.sdk.NoHandle
import org.matrix.rustcomponents.sdk.RoomListDynamicEntriesController

class RoomListControllerTest {
    @Test
    fun `late pagination after stream disposal never uses the destroyed controller`() {
        val owner = RoomListController()
        val controller = RoomListDynamicEntriesController(NoHandle)
        owner.attach(controller)
        var calls = 0
        owner.use { calls++ }
        owner.release(controller)
        // This is the late Compose pagination callback that used to reach destroyed FFI.
        owner.use { it.addOnePage() }
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `disposing an old stream does not remove a replacement controller`() {
        val owner = RoomListController()
        val old = RoomListDynamicEntriesController(NoHandle)
        val replacement = RoomListDynamicEntriesController(NoHandle)
        owner.attach(old)
        owner.attach(replacement)
        owner.release(old)
        var active: RoomListDynamicEntriesController? = null
        owner.use { active = it }
        assertThat(active).isSameInstanceAs(replacement)
        owner.release(replacement)
    }
}
