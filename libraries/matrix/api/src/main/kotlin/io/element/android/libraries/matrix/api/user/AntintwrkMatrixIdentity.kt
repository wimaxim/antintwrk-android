// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.user

/** The public identity domain is independent of the Russian native API address.
 * Legacy IDs stay readable during migration; they are never rewritten in place.
 */
object AntintwrkMatrixIdentity {
    private val pattern = Regex("@u([1-9][0-9]{0,17}):(antintwrk\\.com|matrix\\.antimsg\\.com)")

    fun socialId(value: String): String? = pattern.matchEntire(value)?.groupValues?.get(1)

    fun sameRealm(actor: String, recipient: String): Boolean {
        val source = pattern.matchEntire(actor) ?: return false
        val target = pattern.matchEntire(recipient) ?: return false
        return source.groupValues[2] == target.groupValues[2]
    }
}
