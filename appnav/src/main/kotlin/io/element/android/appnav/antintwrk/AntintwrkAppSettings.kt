// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.element.android.appnav.R
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.ui.strings.CommonStrings

/** Shared setting for native resources and embedded social routes, including Android 24–32. */
object AntintwrkAppLocale {
    val languages = linkedMapOf(
        "en" to "English",
        "ru" to "Русский",
        "uk" to "Українська",
        "de" to "Deutsch",
        "es" to "Español",
        "fr" to "Français",
        "it" to "Italiano",
        "ar" to "العربية",
        "zh" to "中文"
    )
    fun selected(context: Context): String = context.getSharedPreferences("antintwrk-settings", Context.MODE_PRIVATE).getString("language", "").orEmpty()
    fun wrap(context: Context): Context {
        val language = selected(context)
        if (language !in languages) return context
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(language))
        })
    }
    fun choose(activity: Activity, language: String) {
        require(language.isEmpty() || language in languages)
        activity.getSharedPreferences("antintwrk-settings", Context.MODE_PRIVATE).edit().putString("language", language).commit()
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(android.app.LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language)
        }
        activity.recreate()
    }
}

@Composable
internal fun AntintwrkAppSettings(
    onDismiss: () -> Unit,
    onRoute: (String) -> Unit,
    onSignOut: () -> Unit,
    messengerSettingsReady: Boolean = false,
    failed: Boolean = false,
    socialOrigin: String = AntintwrkSocialOrigins.RUSSIAN,
) {
    val context = LocalAntintwrkActivity.current ?: LocalContext.current
    val inviteFriends = rememberAntintwrkInviteShare()
    var choosingLanguage by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AntintwrkBrandTheme {
            Column(Modifier.fillMaxSize().background(AntintwrkBackground).safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState())) {
                TextButton(stringResource(R.string.screen_antintwrk_back), {
                    if (choosingLanguage) choosingLanguage = false else onDismiss()
                })
                if (choosingLanguage) {
                    Text(stringResource(R.string.screen_ant_settings_language), style = ElementTheme.typography.fontHeadingLgRegular)
                    val choices = linkedMapOf("" to stringResource(R.string.screen_ant_settings_system_language)) + AntintwrkAppLocale.languages
                    choices.forEach { (code, name) ->
                        TextButton((if (AntintwrkAppLocale.selected(context) == code) "✓ " else "") + name, {
                            choosingLanguage = false
                            (context as? Activity)?.let { AntintwrkAppLocale.choose(it, code) }
                        })
                    }
                } else {
                Text(stringResource(R.string.screen_ant_menu_settings), style = ElementTheme.typography.fontHeadingLgRegular)
                if (failed) Text(stringResource(R.string.screen_ant_settings_retry), color = ElementTheme.colors.textCriticalPrimary)
                TextButton(stringResource(R.string.screen_ant_settings_language), { choosingLanguage = true })
                SettingsGroup(R.string.screen_ant_settings_account, listOf(
                    "profile" to R.string.screen_ant_settings_profile,
                    "native-push" to R.string.screen_ant_push_title,
                ), onRoute)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                TextButton(stringResource(CommonStrings.screen_ant_invite_friends), inviteFriends)
                SettingsGroup(CommonStrings.common_app_settings, listOf(
                    "native-media" to CommonStrings.common_media_upload_quality,
                    "native-screen-lock" to CommonStrings.common_screen_lock,
                    "native-location" to CommonStrings.common_location_sharing,
                    "native-messenger-notifications" to R.string.screen_ant_settings_messenger_notifications,
                    "native-moderation" to CommonStrings.common_moderation_and_safety,
                ), onRoute, enabled = messengerSettingsReady)
                if (!messengerSettingsReady) Text(stringResource(CommonStrings.common_loading), color = ElementTheme.colors.textSecondary)
                SettingsGroup(R.string.screen_ant_settings_help, listOf(
                    "support" to R.string.screen_ant_menu_support,
                    "terms" to R.string.screen_ant_menu_terms,
                    "privacy" to R.string.screen_ant_menu_privacy,
                    "native-licences" to R.string.screen_ant_licences,
                ), onRoute)
                HorizontalDivider()
                TextButton(stringResource(R.string.screen_ant_auth_logout), onSignOut)
                val version = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
                val code = if (Build.VERSION.SDK_INT >= 28) version.longVersionCode else version.versionCode.toLong()
                Text(
                    stringResource(
                        CommonStrings.settings_version_number,
                        "${antintwrkDisplayedVersion(version.versionName.orEmpty())} ${antintwrkServerLabel(socialOrigin)}",
                        code.toString()
                    ),
                    color = ElementTheme.colors.textSecondary,
                    style = ElementTheme.typography.fontBodySmRegular
                )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: Int, routes: List<Pair<String, Int>>, onRoute: (String) -> Unit, enabled: Boolean = true) {
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    Text(stringResource(title), color = ElementTheme.colors.textSecondary, style = ElementTheme.typography.fontBodySmRegular)
    routes.forEach { (route, label) -> TextButton(stringResource(label), { onRoute(route) }, enabled = enabled) }
}

/** Actual applied social origin; Matrix remains on the Russian transport in both cases. */
internal fun antintwrkServerLabel(origin: String): String = when (origin) {
    AntintwrkSocialOrigins.RUSSIAN -> "R"
    AntintwrkSocialOrigins.PUBLIC -> "G"
    else -> "?"
}

internal fun antintwrkDisplayedVersion(version: String): String = version.replace(Regex("(?i)(?:^|[-.])test(?=[-.]|$)"), "").trim('-', '.')
