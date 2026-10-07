/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

internal object AntintwrkRegistrationRules {
    fun strongPassword(value: String): Boolean = value.length in 8..128 &&
        value.any { it in 'A'..'Z' } && value.any { it in 'a'..'z' } && value.any { it in '0'..'9' } &&
        value.any { !it.isLetterOrDigit() }
}
