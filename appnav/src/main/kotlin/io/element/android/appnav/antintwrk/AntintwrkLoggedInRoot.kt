// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.bumble.appyx.core.navigation.NavKey
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.BackStackElements
import com.bumble.appyx.navmodel.backstack.operation.NewRoot
import io.element.android.appnav.LoggedInFlowNode
import io.element.android.appnav.safeRoot
import io.element.android.features.ftue.api.state.FtueState

/** Social admission owns onboarding; SDK verification/recovery remain independent background services. */
internal fun updateAntintwrkLoggedInRoot(
    backstack: BackStack<LoggedInFlowNode.NavTarget>,
    ftue: FtueState,
    managed: Boolean,
) {
    if (managed) {
        // A stock notification/lockscreen/consent step must never hide Chats in the embedded app.
        // Also migrate saved Ftue/Placeholder roots without replacing a working Home/session.
        if (backstack.elements.value.none {
                it.key.navTarget == LoggedInFlowNode.NavTarget.Home && it.targetState != BackStack.State.DESTROYED
            }) {
            backstack.accept(NewRoot(LoggedInFlowNode.NavTarget.Home))
        }
    } else {
        when (ftue) {
            FtueState.Unknown -> Unit
            FtueState.Incomplete -> backstack.safeRoot(LoggedInFlowNode.NavTarget.Ftue)
            FtueState.Complete -> backstack.safeRoot(LoggedInFlowNode.NavTarget.Home)
        }
    }
}

/** Resolve by NavKey, never by the first matching target left over from a previous transition. */
internal fun activeAntintwrkHomeKey(elements: BackStackElements<LoggedInFlowNode.NavTarget>): NavKey<LoggedInFlowNode.NavTarget>? =
    elements.lastOrNull { it.key.navTarget == LoggedInFlowNode.NavTarget.Home && it.targetState == BackStack.State.ACTIVE }?.key
