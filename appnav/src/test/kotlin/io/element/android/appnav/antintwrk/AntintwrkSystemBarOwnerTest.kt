/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import org.junit.Assert.assertEquals
import org.junit.Test

class AntintwrkSystemBarOwnerTest {
    @Test
    fun `loading disposal does not restore bars while welcome remains visible`() {
        val owner = AntintwrkSystemBarOwner()
        var restored = ""
        val loading = owner.acquire { restored = "old parent" }
        val welcome = owner.acquire { restored = "current parent" }
        loading()
        assertEquals("", restored)
        welcome()
        assertEquals("current parent", restored)
    }

    @Test
    fun `repeated disposal only restores once`() {
        val owner = AntintwrkSystemBarOwner()
        var restores = 0
        val release = owner.acquire { restores++ }
        release()
        release()
        assertEquals(1, restores)
    }
}
