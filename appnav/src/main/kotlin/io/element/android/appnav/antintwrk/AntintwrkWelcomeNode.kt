/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node

/** UI-only entry. Never creates or changes a Matrix session. */
class AntintwrkWelcomeNode(
    buildContext: BuildContext,
) : Node(buildContext = buildContext) {
    private val presenter = AntintwrkWelcomePresenter()

    @Composable
    override fun View(modifier: Modifier) {
        AntintwrkWelcomeView(presenter.present(), modifier)
    }
}
