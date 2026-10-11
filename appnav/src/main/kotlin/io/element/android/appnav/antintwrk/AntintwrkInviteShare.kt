// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.element.android.libraries.androidutils.system.startSharePlainTextIntent
import io.element.android.libraries.ui.strings.CommonStrings

/** Opens the system sharesheet; recipient selection and sending remain under the user's control. */
@Composable
internal fun rememberAntintwrkInviteShare(): () -> Unit {
    val context = LocalContext.current
    val message = context.getString(CommonStrings.screen_ant_invite_message)
    val title = context.getString(CommonStrings.screen_ant_invite_friends)
    return {
        context.startSharePlainTextIntent(
            activityResultLauncher = null,
            chooserTitle = title,
            text = message,
            extraTitle = title,
        )
    }
}
