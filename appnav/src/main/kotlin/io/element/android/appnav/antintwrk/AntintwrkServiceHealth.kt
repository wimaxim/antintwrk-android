// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Two failures avoid treating a single slow request as an outage. One success restores service. */
internal class AntintwrkHealthGate {
    var failures = 0
        private set
    val unavailable get() = failures >= 2
    fun record(reachable: Boolean): Boolean {
        failures = if (reachable) 0 else (failures + 1).coerceAtMost(2)
        return unavailable
    }
}

internal class AntintwrkServiceHealth {
    private val http = OkHttpClient.Builder().followRedirects(false).callTimeout(5, TimeUnit.SECONDS).build()
    suspend fun reachable(messenger: Boolean, origin: String = BuildConfig.ANTINTWRK_SOCIAL_ORIGIN): Boolean = withContext(Dispatchers.IO) {
        val url = if (messenger) "https://matrix.antimsg.com/_matrix/client/versions" else "$origin/antintwrk_mobile.php"
        try {
            val request = Request.Builder().url(url).head().header("User-Agent", AntintwrkNativeAuthClient.USER_AGENT).build()
            http.newCall(request).execute().use { response -> response.code < 500 }
        } catch (_: java.io.IOException) {
            false
        }
    }
}

@Composable
internal fun AntintwrkServiceUnavailable(modifier: Modifier = Modifier, message: Int, onRetry: () -> Unit) {
    val context = LocalContext.current
    val offline = context.getSystemService(Context.CONNECTIVITY_SERVICE).let { (it as ConnectivityManager).activeNetwork == null }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AntintwrkBrandMark(48.dp)
            Text(stringResource(if (offline) R.string.screen_ant_offline else message))
            TextButton(stringResource(R.string.screen_ant_web_retry), onRetry)
        }
    }
}
