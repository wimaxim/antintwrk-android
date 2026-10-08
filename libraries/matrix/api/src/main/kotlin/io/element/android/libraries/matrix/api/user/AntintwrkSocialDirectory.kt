// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.user

/** In-memory, actor-bound source catalogue. No cookies or credentials leave the native auth layer. */
object AntintwrkSocialDirectory {
    data class Binding(val actor: String, val authorize: suspend (String) -> Boolean = { false }, val search: suspend (String) -> MatrixSearchUserResults)
    @Volatile private var binding: Binding? = null
    fun bind(value: Binding) {
        binding = value
    }
    fun clear(value: Binding) {
        synchronized(this) { if (binding === value) binding = null }
    }
    suspend fun authorize(actor: String, recipient: String): Boolean {
        val current = binding?.takeIf { it.actor == actor } ?: return false
        return current.authorize(recipient) && binding === current
    }
    suspend fun search(actor: String, query: String): MatrixSearchUserResults? {
        val current = binding?.takeIf { it.actor == actor } ?: return null
        val result = current.search(query)
        return result.takeIf { binding === current }
    }
}
