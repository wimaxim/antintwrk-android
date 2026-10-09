/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.element.android.appnav.R
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.Text

@Composable
internal fun AntintwrkSocialMenu(onRoute: (String) -> Unit, onSignOut: (() -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(CompoundIcons.OverflowVertical(), stringResource(R.string.screen_ant_menu_more))
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "hero-compose" to R.string.screen_ant_menu_compose,
                "people" to R.string.screen_ant_menu_people,
                "native-settings" to R.string.screen_ant_menu_settings,
            ).forEach { (route, label) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                    expanded = false
                    onRoute(route)
                })
            }
        }
    }
}
