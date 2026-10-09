// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.operation.NewRoot
import com.bumble.appyx.navmodel.backstack.operation.push
import com.google.common.truth.Truth.assertThat
import io.element.android.appnav.LoggedInFlowNode.NavTarget
import io.element.android.features.ftue.api.state.FtueState
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AntintwrkLoggedInRootTest : RobolectricTest() {
    @Test
    fun `stock incomplete onboarding has no chats root but managed admission makes it available`() {
        val backstack = BackStack<NavTarget>(NavTarget.Placeholder, null)
        updateAntintwrkLoggedInRoot(backstack, FtueState.Incomplete, managed = false)
        assertThat(backstack.elements.value.any { it.key.navTarget == NavTarget.Home }).isFalse()
        updateAntintwrkLoggedInRoot(backstack, FtueState.Incomplete, managed = true)
        assertThat(backstack.elements.value.any { it.key.navTarget == NavTarget.Home && it.targetState == BackStack.State.ACTIVE }).isTrue()
        assertThat(backstack.elements.value.filter { it.targetState != BackStack.State.DESTROYED }.map { it.key.navTarget })
            .containsExactly(NavTarget.Home)
    }
    @Test
    fun `unknown or changing stock setup cannot replace managed chats or an open screen`() {
        val backstack = BackStack<NavTarget>(NavTarget.Home, null)
        val originalHome = backstack.elements.value.single().key
        backstack.push(NavTarget.Placeholder)
        for (state in listOf(FtueState.Unknown, FtueState.Incomplete, FtueState.Complete, FtueState.Incomplete)) {
            updateAntintwrkLoggedInRoot(backstack, state, managed = true)
            assertThat(backstack.elements.value.single { it.key.navTarget == NavTarget.Home }.key).isEqualTo(originalHome)
            assertThat(backstack.elements.value.last { it.targetState == BackStack.State.ACTIVE }.key.navTarget).isEqualTo(NavTarget.Placeholder)
        }
    }
    @Test
    fun `destroyed home cannot suppress a replacement or be chosen as the attached chats screen`() {
        val backstack = BackStack<NavTarget>(NavTarget.Home, null)
        val oldKey = backstack.elements.value.single().key
        backstack.onTransitionFinished(oldKey)
        backstack.accept(NewRoot(NavTarget.Ftue))
        assertThat(backstack.elements.value.single { it.key == oldKey }.targetState).isEqualTo(BackStack.State.DESTROYED)
        assertThat(activeAntintwrkHomeKey(backstack.elements.value)).isNull()
        val staleHome = backstack.elements.value.single { it.key == oldKey }
        updateAntintwrkLoggedInRoot(backstack, FtueState.Incomplete, managed = true)
        val liveKey = activeAntintwrkHomeKey(backstack.elements.value)
        assertThat(liveKey).isNotNull()
        assertThat(liveKey).isNotEqualTo(oldKey)
        val delayedRemoval = listOf(staleHome) + backstack.elements.value
        assertThat(delayedRemoval.first { it.key.navTarget == NavTarget.Home }.key).isEqualTo(oldKey)
        assertThat(activeAntintwrkHomeKey(delayedRemoval)).isEqualTo(liveKey)
        assertThat(backstack.elements.value.firstOrNull { it.key == oldKey }?.targetState).isAnyOf(null, BackStack.State.DESTROYED)
    }
    @Test
    fun `restored stock setup is migrated to home even without a completion event`() {
        val backstack = BackStack<NavTarget>(NavTarget.Ftue, null)
        updateAntintwrkLoggedInRoot(backstack, FtueState.Unknown, managed = true)
        assertThat(backstack.elements.value.filter { it.targetState == BackStack.State.ACTIVE }.map { it.key.navTarget }).containsExactly(NavTarget.Home)
    }
}
