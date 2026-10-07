/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node

/** Debug-only preview destination; never authenticates or creates a Matrix session. */
class AntintwrkShellNode(buildContext: BuildContext) : Node(buildContext = buildContext) {
    private val presenter = AntintwrkShellPresenter()

    @Composable
    override fun View(modifier: Modifier) {
        AntintwrkShellView(presenter.present(), modifier)
    }
}
