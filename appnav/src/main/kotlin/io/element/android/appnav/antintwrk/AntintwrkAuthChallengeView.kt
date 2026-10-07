/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.element.android.appnav.R
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton

@Composable
internal fun AntintwrkAuthChallengeView(
    stage: AntintwrkAuthStage,
    code: String,
    busy: Boolean,
    failed: Boolean,
    onCode: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = AntintwrkBrandTheme {
    BackHandler { if (!busy) onBack() }
    Column(
        modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AntintwrkBrandMark(40.dp)
        Text(stringResource(
            when (stage) {
            AntintwrkAuthStage.DEVICE -> R.string.screen_ant_auth_device
            AntintwrkAuthStage.TOTP -> R.string.screen_ant_auth_totp
            else -> R.string.screen_ant_auth_setup
        }
        ), style = ElementTheme.typography.fontHeadingMdBold)
        if (stage != AntintwrkAuthStage.SETUP_REQUIRED) {
            AntintwrkTextField(
                code,
                onCode,
                label = stringResource(R.string.screen_ant_auth_code),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            AntintwrkPrimaryButton(stringResource(R.string.screen_antintwrk_continue), { if (!busy) onConfirm() }, enabled = !busy)
        }
        if (busy) CircularProgressIndicator()
        if (failed) Text(stringResource(R.string.screen_ant_auth_error), color = ElementTheme.colors.textCriticalPrimary)
        TextButton(stringResource(R.string.screen_antintwrk_back), { if (!busy) onBack() })
    }
}

@PreviewsDayNight
@Composable
internal fun AntintwrkDeviceChallengePreview() = ElementPreview {
    AntintwrkAuthChallengeView(AntintwrkAuthStage.DEVICE, "", false, false, {}, {}, {})
}

@PreviewsDayNight
@Composable
internal fun AntintwrkTotpChallengePreview() = ElementPreview {
    AntintwrkAuthChallengeView(AntintwrkAuthStage.TOTP, "", false, true, {}, {}, {})
}

@PreviewsDayNight
@Composable
internal fun AntintwrkChallengeLoadingPreview() = ElementPreview {
    AntintwrkAuthChallengeView(AntintwrkAuthStage.TOTP, "", true, false, {}, {}, {})
}

@PreviewsDayNight
@Composable
internal fun AntintwrkChallengeSetupPreview() = ElementPreview {
    AntintwrkAuthChallengeView(AntintwrkAuthStage.SETUP_REQUIRED, "", false, false, {}, {}, {})
}
