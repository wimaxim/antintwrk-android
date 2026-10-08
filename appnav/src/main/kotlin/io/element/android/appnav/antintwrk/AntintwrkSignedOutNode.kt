// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.composable.Children
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.navigation.model.permanent.PermanentNavModel
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.node.ParentNode
import io.element.android.features.signedout.api.SignedOutEntryPoint
import io.element.android.libraries.matrix.api.core.SessionId
import kotlinx.parcelize.Parcelize

/** An expired Matrix session must not replace the entire social application. */
internal class AntintwrkSignedOutNode(
    buildContext: BuildContext,
    private val sessionId: SessionId,
    private val entryPoint: SignedOutEntryPoint,
    private val onSwitchMatrixAccount: (String) -> Unit,
) : ParentNode<AntintwrkSignedOutNode.Target>(
    navModel = PermanentNavModel(navTargets = setOf(Target), savedStateMap = buildContext.savedStateMap),
    buildContext = buildContext,
) {
    @Parcelize
    object Target : Parcelable

    override fun resolve(navTarget: Target, buildContext: BuildContext): Node =
        entryPoint.createNode(this, buildContext, SignedOutEntryPoint.Params(sessionId))

    @Composable
    override fun View(modifier: Modifier) {
        AntintwrkNativeAppView(
            modifier,
            matrixUserId = sessionId.value,
            onSwitchMatrixAccount = onSwitchMatrixAccount,
            messengerContent = { contentModifier -> Children(navModel = navModel, modifier = contentModifier) },
        )
    }
}
