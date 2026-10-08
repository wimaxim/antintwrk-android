// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import io.element.android.appnav.R
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.CancellationException

/** Own immutable escrow must never be invalidated by stock key-change/disable controls. */
class AntintwrkRecoveryNode(buildContext: BuildContext, private val matrixClient: MatrixClient) : Node(buildContext) {
    private val recovery = AntintwrkAccountRecovery(matrixClient.encryptionService, matrixClient.sessionId.value)

    @Composable
    override fun View(modifier: Modifier) {
        var attempt by remember { mutableIntStateOf(0) }
        var ready by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(attempt) {
            ready = null
            ready = try {
                recovery.prepare()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }
        PreferencePage(
            modifier = modifier,
            title = stringResource(CommonStrings.common_encryption),
            onBackClick = ::navigateUp,
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.screen_ant_recovery_account))
                when (ready) {
                    null -> CircularProgressIndicator()
                    true -> Text(stringResource(R.string.screen_ant_recovery_ready))
                    false -> {
                        Text(stringResource(R.string.screen_ant_recovery_retry))
                        TextButton(stringResource(R.string.screen_ant_web_retry), onClick = { attempt++ })
                    }
                }
            }
        }
    }
}
