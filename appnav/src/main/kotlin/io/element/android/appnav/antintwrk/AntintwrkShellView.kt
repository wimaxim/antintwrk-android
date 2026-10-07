/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton

@Composable
fun AntintwrkShellView(state: AntintwrkShellState, modifier: Modifier = Modifier, webFeed: Boolean = false) = AntintwrkBrandTheme {
    val emit = state.eventSink
    val holder = rememberSaveableStateHolder()
    val language = LocalConfiguration.current.locales[0].language
    val webController = if (webFeed) rememberAntintwrkWebFeedController() else null
    BackHandler(state.overlay != AntintwrkOverlay.None || state.mode != AntintwrkMode.Social || state.section != AntintwrkSection.Feed) {
        emit(AntintwrkShellEvent.Back)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AntintwrkBackground,
        topBar = {
            ShellHeader(state) {
                if (webController != null) {
                    emit(AntintwrkShellEvent.Section(AntintwrkSection.Feed))
                    val url = AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN).feed(language).replace("/hero-feed?", "/discover?")
                    webController.view?.loadUrl(url)
                } else {
                    emit(AntintwrkShellEvent.Overlay(AntintwrkOverlay.Search))
                }
            }
        },
        bottomBar = {
            if (state.mode == AntintwrkMode.Social) {
                NavigationBar(containerColor = AntintwrkBackground) {
                    AntintwrkSection.entries.forEach { section ->
                        NavigationBarItem(
                            selected = state.section == section,
                            onClick = { emit(AntintwrkShellEvent.Section(section)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElementTheme.materialColors.primary,
                                selectedTextColor = ElementTheme.colors.textPrimary,
                                indicatorColor = ElementTheme.colors.bgCanvasDefaultLevel1,
                                unselectedIconColor = ElementTheme.colors.textSecondary,
                                unselectedTextColor = ElementTheme.colors.textSecondary,
                            ),
                            icon = {
                                Icon(
                                    when (section) {
                                    AntintwrkSection.Feed -> CompoundIcons.Home()
                                    AntintwrkSection.Communities -> CompoundIcons.Group()
                                    AntintwrkSection.Profile -> CompoundIcons.UserProfile()
                                },
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    stringResource(section.label),
                                    modifier = Modifier.requiredWidth((LocalConfiguration.current.screenWidthDp / 3).dp),
                                    style = ElementTheme.typography.fontBodyXsRegular,
                                    textAlign = TextAlign.Center,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!webFeed) {
                Text(
                stringResource(R.string.screen_ant_shell_preview),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = ElementTheme.colors.textSecondary,
                style = ElementTheme.typography.fontBodyXsRegular,
            )
            }
            if (state.mode == AntintwrkMode.Messenger) {
                ShellEmpty(R.string.screen_ant_shell_messenger, R.string.screen_ant_shell_messenger_pending)
            } else {
                holder.SaveableStateProvider(state.section.name) {
                    when (state.section) {
                        AntintwrkSection.Feed -> if (webController !=
                            null) {
                                AntintwrkWebFeed(webController, backEnabled = state.overlay == AntintwrkOverlay.None)
                            } else {
                                AntintwrkFeedView(state)
                            }
                        AntintwrkSection.Communities -> ShellEmpty(state.section.label, R.string.screen_ant_shell_communities_pending)
                        AntintwrkSection.Profile -> ShellEmpty(state.section.label, R.string.screen_ant_shell_profile_pending)
                    }
                }
            }
        }
    }
    if (state.overlay != AntintwrkOverlay.None) ShellDialog(state)
}

@Composable
private fun ShellHeader(state: AntintwrkShellState, onSearch: () -> Unit) {
    val expanded = LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale > 1.2f
    Column(Modifier.statusBarsPadding().padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShellModeTabs(state, Modifier.weight(1f))
            if (!expanded && state.mode == AntintwrkMode.Social) ShellSocialActions(state, onSearch)
        }
        if (expanded && state.mode == AntintwrkMode.Social) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ShellSocialActions(state, onSearch) }
        }
    }
}

@Composable
private fun ShellModeTabs(state: AntintwrkShellState, modifier: Modifier) {
    TabRow(
        selectedTabIndex = state.mode.ordinal,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        containerColor = ElementTheme.colors.bgCanvasDefaultLevel1,
        indicator = {},
        divider = {},
    ) {
        AntintwrkMode.entries.forEach { mode ->
            Tab(
                selected = state.mode == mode,
                onClick = { state.eventSink(AntintwrkShellEvent.Mode(mode)) },
                selectedContentColor = ElementTheme.materialColors.primary,
                unselectedContentColor = ElementTheme.colors.textSecondary,
            ) {
                Text(
                    stringResource(if (mode == AntintwrkMode.Social) R.string.screen_ant_shell_social else R.string.screen_ant_shell_messenger),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                    style = ElementTheme.typography.fontBodySmRegular,
                )
            }
        }
    }
}

@Composable
private fun ShellSocialActions(state: AntintwrkShellState, onSearch: () -> Unit) {
    IconButton(onClick = onSearch) {
        Icon(CompoundIcons.Search(), stringResource(R.string.screen_ant_shell_search))
    }
    IconButton(onClick = { state.eventSink(AntintwrkShellEvent.Overlay(AntintwrkOverlay.Notifications)) }) {
        Icon(CompoundIcons.Notifications(), stringResource(R.string.screen_ant_shell_notifications))
    }
}

internal val AntintwrkSection.label: Int get() = when (this) {
    AntintwrkSection.Feed -> R.string.screen_ant_shell_feed
    AntintwrkSection.Communities -> R.string.screen_ant_shell_communities
    AntintwrkSection.Profile -> R.string.screen_ant_shell_profile
}

@Composable
private fun ShellEmpty(title: Int, message: Int) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        AntintwrkBrandMark(48.dp)
        Text(stringResource(title), Modifier.padding(top = 24.dp, bottom = 12.dp), style = ElementTheme.typography.fontHeadingMdBold)
        Text(stringResource(message), color = ElementTheme.colors.textSecondary)
    }
}

@Composable
private fun ShellDialog(state: AntintwrkShellState) {
    val title = when (state.overlay) {
        AntintwrkOverlay.Search -> R.string.screen_ant_shell_search
        AntintwrkOverlay.Notifications -> R.string.screen_ant_shell_notifications
        AntintwrkOverlay.Comments -> R.string.screen_ant_shell_comments
        else -> R.string.screen_ant_shell_share
    }
    AlertDialog(
        onDismissRequest = { state.eventSink(AntintwrkShellEvent.Back) },
        title = { Text(stringResource(title)) },
        text = {
            if (state.overlay == AntintwrkOverlay.Search) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AntintwrkTextField(
                        state.query,
                        { state.eventSink(AntintwrkShellEvent.Query(it)) },
                        label = stringResource(R.string.screen_ant_shell_search_hint)
                    )
                    val posts = antintwrkDemoPosts.filter { post ->
                        (stringResource(post.title) + " " + stringResource(post.body)).contains(state.query.trim(), ignoreCase = true)
                    }
                    LazyColumn {
                        items(posts, key = { it.id }) { Text(stringResource(it.title), Modifier.padding(vertical = 12.dp)) }
                        if (posts.isEmpty()) item { Text(stringResource(R.string.screen_ant_shell_no_results)) }
                    }
                }
            } else {
                Text(stringResource(if (state.overlay ==
                    AntintwrkOverlay.Notifications) {
                        R.string.screen_ant_shell_notifications_pending
                    } else {
                        R.string.screen_ant_shell_action_pending
                    }))
            }
        },
        confirmButton = { TextButton(stringResource(R.string.screen_ant_shell_close), { state.eventSink(AntintwrkShellEvent.Back) }) },
    )
}

internal class AntintwrkShellPreviewStates : PreviewParameterProvider<AntintwrkShellState> {
    override val values = sequenceOf(
        AntintwrkShellState(),
        AntintwrkShellState(scope = AntintwrkFeedScope.Latest),
        AntintwrkShellState(scope = AntintwrkFeedScope.Network),
        AntintwrkShellState(mode = AntintwrkMode.Messenger),
        AntintwrkShellState(section = AntintwrkSection.Communities),
        AntintwrkShellState(section = AntintwrkSection.Profile),
        AntintwrkShellState(overlay = AntintwrkOverlay.Search),
        AntintwrkShellState(overlay = AntintwrkOverlay.Search, query = "absent"),
        AntintwrkShellState(overlay = AntintwrkOverlay.Notifications),
        AntintwrkShellState(overlay = AntintwrkOverlay.Comments),
        AntintwrkShellState(overlay = AntintwrkOverlay.Share),
    )
}

@PreviewsDayNight
@Composable
internal fun AntintwrkShellPreview(@PreviewParameter(AntintwrkShellPreviewStates::class) state: AntintwrkShellState) = ElementPreview {
    AntintwrkShellView(state)
}
