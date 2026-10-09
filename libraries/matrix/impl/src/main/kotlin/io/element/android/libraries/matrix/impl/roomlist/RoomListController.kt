/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.roomlist

import org.matrix.rustcomponents.sdk.RoomListDynamicEntriesController

/** Serializes synchronous FFI calls with disposal when the entries stream is cancelled. */
internal class RoomListController {
    private val lock = Any()
    private var controller: RoomListDynamicEntriesController? = null

    fun attach(value: RoomListDynamicEntriesController) = synchronized(lock) {
        controller = value
    }

    fun use(block: (RoomListDynamicEntriesController) -> Unit) = synchronized(lock) {
        controller?.let(block)
        Unit
    }

    fun release(value: RoomListDynamicEntriesController) = synchronized(lock) {
        if (controller === value) controller = null
        value.destroy()
    }
}
