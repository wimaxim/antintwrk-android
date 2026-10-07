/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.appnav.R
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.Text

internal data class AntintwrkDemoPost(val id: Int, val author: Int, val title: Int, val body: Int)
internal val antintwrkDemoPosts = listOf(
    AntintwrkDemoPost(1, R.string.screen_ant_shell_author_one, R.string.screen_ant_shell_title_one, R.string.screen_ant_shell_body_one),
    AntintwrkDemoPost(2, R.string.screen_ant_shell_author_two, R.string.screen_ant_shell_title_two, R.string.screen_ant_shell_body_two),
)

@Composable
internal fun AntintwrkFeedView(state: AntintwrkShellState) {
    val posts = when (state.scope) {
        AntintwrkFeedScope.Recommended -> antintwrkDemoPosts
        AntintwrkFeedScope.Latest -> antintwrkDemoPosts.reversed()
        AntintwrkFeedScope.Network -> antintwrkDemoPosts.take(1)
    }
    LazyColumn(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.testTag("antintwrk-feed"),
    ) {
        item(key = "heading") {
            Text(stringResource(R.string.screen_ant_shell_feed), style = ElementTheme.typography.fontHeadingMdBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AntintwrkFeedScope.entries.forEach { scope ->
                    FilterChip(
                        selected = state.scope == scope,
                        onClick = { state.eventSink(AntintwrkShellEvent.Scope(scope)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ElementTheme.colors.bgCanvasDefaultLevel1,
                            selectedContainerColor = ElementTheme.materialColors.primary.copy(alpha = 0.14f),
                            selectedLabelColor = ElementTheme.materialColors.primary,
                            labelColor = ElementTheme.colors.textSecondary,
                        ),
                        label = {
                            Text(stringResource(
                                when (scope) {
                            AntintwrkFeedScope.Recommended -> R.string.screen_ant_shell_recommended
                            AntintwrkFeedScope.Latest -> R.string.screen_ant_shell_latest
                            AntintwrkFeedScope.Network -> R.string.screen_ant_shell_network
                        }
                            ))
                        },
                    )
                }
            }
        }
        items(posts, key = { it.id }) { post -> DemoPostCard(post, state.eventSink) }
    }
}

@Composable
private fun DemoPostCard(post: AntintwrkDemoPost, emit: (AntintwrkShellEvent) -> Unit) {
    var vote by rememberSaveable { mutableIntStateOf(0) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("demo-post-${post.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ElementTheme.colors.bgCanvasDefaultLevel1),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(CompoundIcons.UserProfile(), contentDescription = null, tint = ElementTheme.colors.textSecondary)
                Text(stringResource(post.author), style = ElementTheme.typography.fontBodySmRegular, color = ElementTheme.colors.textSecondary)
            }
            Text(stringResource(post.title), style = ElementTheme.typography.fontHeadingSmMedium)
            Text(stringResource(post.body), style = ElementTheme.typography.fontBodyMdRegular)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconToggleButton(checked = vote == 1, onCheckedChange = { vote = if (it) 1 else 0 }) {
                    Icon(
                        CompoundIcons.ArrowUp(),
                        stringResource(R.string.screen_ant_shell_interesting),
                        tint = if (vote == 1) ElementTheme.materialColors.primary else ElementTheme.colors.textSecondary
                    )
                }
                Text(vote.toString(), Modifier.testTag("demo-vote-${post.id}"))
                IconToggleButton(checked = vote == -1, onCheckedChange = { vote = if (it) -1 else 0 }) {
                    Icon(
                        CompoundIcons.ArrowDown(),
                        stringResource(R.string.screen_ant_shell_not_interesting),
                        tint = if (vote == -1) ElementTheme.materialColors.primary else ElementTheme.colors.textSecondary
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { emit(AntintwrkShellEvent.Overlay(AntintwrkOverlay.Comments)) }) {
                    Icon(CompoundIcons.Chat(), stringResource(R.string.screen_ant_shell_comments))
                }
                IconButton(onClick = { emit(AntintwrkShellEvent.Overlay(AntintwrkOverlay.Share)) }) {
                    Icon(CompoundIcons.ShareAndroid(), stringResource(R.string.screen_ant_shell_share))
                }
            }
        }
    }
}
