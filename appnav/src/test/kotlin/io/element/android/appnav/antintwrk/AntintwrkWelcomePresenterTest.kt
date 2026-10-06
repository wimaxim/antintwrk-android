/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkWelcomePresenterTest {
    @Test
    fun `registration back preserves credentials but leaving registration clears password`() = runTest {
        val presenter = AntintwrkWelcomePresenter { it == "preview@example.test" }
        moleculeFlow(RecompositionMode.Immediate) { presenter.present() }.test {
            awaitItem().eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Register))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Email("preview@example.test"))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Password("example-password"))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Continue)
            val profile = awaitItem()
            assertThat(profile.page).isEqualTo(AntintwrkPage.Profile)
            profile.eventSink(AntintwrkWelcomeEvent.Back)
            val register = awaitItem()
            assertThat(register.password).isEqualTo("example-password")
            register.eventSink(AntintwrkWelcomeEvent.Back)
            val welcome = awaitItem()
            assertThat(welcome.page).isEqualTo(AntintwrkPage.Welcome)
            assertThat(welcome.password).isEmpty()
        }
    }

    @Test
    fun `invalid credentials never advance to profile`() = runTest {
        val presenter = AntintwrkWelcomePresenter { false }
        moleculeFlow(RecompositionMode.Immediate) { presenter.present() }.test {
            awaitItem().eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Register))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Continue)
            val error = awaitItem()
            assertThat(error.page).isEqualTo(AntintwrkPage.Register)
            assertThat(error.error).isTrue()
            assertThat(error.unavailable).isFalse()
        }
    }

    @Test
    fun `switching to password reset removes sign in credentials`() = runTest {
        val presenter = AntintwrkWelcomePresenter { true }
        moleculeFlow(RecompositionMode.Immediate) { presenter.present() }.test {
            awaitItem().eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Login))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Password("example-password"))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Reset))
            val reset = awaitItem()
            assertThat(reset.password).isEmpty()
            reset.eventSink(AntintwrkWelcomeEvent.Continue)
            assertThat(awaitItem().unavailable).isTrue()
        }
    }
}
