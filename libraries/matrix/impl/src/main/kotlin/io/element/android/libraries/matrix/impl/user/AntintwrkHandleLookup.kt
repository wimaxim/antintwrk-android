// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.impl.user

import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/** Optional exact-handle resolver using the messenger's durable social projection. */
internal suspend fun antintwrkHandleLookup(handle: String, accessToken: String): UserId? = withContext(Dispatchers.IO) {
    val query = handle.removePrefix("@")
    if (!query.matches(Regex("[A-Za-z0-9._-]{1,100}"))) return@withContext null
    val connection = URI("https://auth.antimsg.com/social-identity/directory?handle=${URLEncoder.encode(query, "UTF-8")}")
        .toURL().openConnection() as HttpsURLConnection
    try {
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        if (connection.responseCode != 200) return@withContext null
        val bytes = connection.inputStream.use { input ->
            val buffer = ByteArray(4097)
            var count = 0
            while (count < buffer.size) {
                val read = input.read(buffer, count, buffer.size - count)
                if (read < 0) break
                count += read
            }
            buffer.copyOf(count)
        }
        if (bytes.size > 4096) return@withContext null
        val results = JSONObject(bytes.toString(Charsets.UTF_8)).getJSONArray("results")
        if (results.length() != 1) return@withContext null
        results.getJSONObject(0).getString("userId").takeIf { it.matches(Regex("@u[1-9][0-9]*:matrix\\.antimsg\\.com")) }?.let(::UserId)
    } finally {
        connection.disconnect()
    }
}
