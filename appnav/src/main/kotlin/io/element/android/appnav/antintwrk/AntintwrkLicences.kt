// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

private data class AntintwrkLicence(val name: String, val licences: List<Pair<String, String>>)

@Composable
internal fun AntintwrkLicencesDialog(onOpenAuthor: (() -> Unit)? = null, onDismiss: () -> Unit) = AntintwrkBrandTheme {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var licence by remember { mutableStateOf("") }
    var dependencies by remember { mutableStateOf<List<AntintwrkLicence>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val agpl = context.assets.open("antintwrk-LICENSE.txt").bufferedReader().use { it.readText() }
                val array = JSONArray(context.assets.open("licensee-artifacts.json").bufferedReader().use { it.readText() })
                val records = (0 until array.length()).map { index ->
                    val item = array.getJSONObject(index)
                    val licenses = item.optJSONArray("spdxLicenses") ?: JSONArray()
                    val otherLicenses = item.optJSONArray("unknownLicenses") ?: JSONArray()
                    AntintwrkLicence(
                        "${item.getString("groupId")}:${item.getString("artifactId")} ${item.getString("version")}",
                        (0 until licenses.length()).map { n -> licenses.getJSONObject(n).let { it.getString("identifier") to it.optString("url") } } +
                            (0 until otherLicenses.length()).map { n ->
                                otherLicenses.getJSONObject(n).let { entry ->
                                entry.optString("name").takeUnless { it.isBlank() || it == "null" }.orEmpty().ifEmpty { entry.optString("url") } to
                                    entry.optString("url")
                            }
                            }
                    )
                }
                agpl to records
            }
        }
        result.onSuccess { (agpl, records) ->
            licence = agpl
            dependencies = records
        }.onFailure { failed = true }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screen_ant_licences)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item {
                    Text(stringResource(R.string.screen_ant_licences_project))
                    Text(stringResource(R.string.screen_ant_licences_operator, "Super David Universe LLC"))
                    TextButton(stringResource(R.string.screen_ant_licences_author, "Maxim Novichkov"), onClick = {
                        onOpenAuthor?.invoke() ?: uriHandler.openUri("https://antintwrk.com/en/hero/2266620")
                    })
                    Text(stringResource(R.string.screen_ant_licences_backend_title))
                    Text(stringResource(R.string.screen_ant_licences_backend))
                    Text(stringResource(R.string.screen_ant_licences_client_title))
                    Text(stringResource(R.string.screen_ant_licences_client))
                    Text(
                        "Copyright 2026 Maxim Novichkov\nElement X — AGPL-3.0-only\nCopyright 2022–2025 New Vector Ltd.\nCopyright 2025 Element Creations Ltd."
                    )
                    TextButton(stringResource(R.string.screen_ant_source), onClick = { uriHandler.openUri("https://github.com/wimaxim/antintwrk-android") })
                    TextButton("AGPL-3.0", onClick = { expanded = !expanded })
                    if (expanded) Text(licence)
                    if (failed) Text(stringResource(R.string.screen_ant_licences_failed))
                }
                items(dependencies) { dependency ->
                    Text(dependency.name)
                    dependency.licences.forEach { (name, url) ->
                        if (url.startsWith("https://")) TextButton(name, onClick = { uriHandler.openUri(url) }) else Text(name)
                    }
                }
            }
        },
        confirmButton = { TextButton(stringResource(android.R.string.ok), onClick = onDismiss) },
    )
}
