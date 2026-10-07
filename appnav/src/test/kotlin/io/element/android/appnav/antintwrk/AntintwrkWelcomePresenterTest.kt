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
    fun `password login validation requires email and password without applying registration rules`() {
        val presenter = AntintwrkWelcomePresenter { it == "member@example.invalid" }
        assertThat(presenter.canSignIn(AntintwrkWelcomeState(email = "member@example.invalid"))).isFalse()
        assertThat(presenter.canSignIn(AntintwrkWelcomeState(email = "invalid", password = "fixture"))).isFalse()
        assertThat(presenter.canSignIn(AntintwrkWelcomeState(email = " member@example.invalid ", password = "x"))).isTrue()
    }

    @Test
    fun `incomplete login shows a field error and clears it when the password is supplied`() = runTest {
        val presenter = AntintwrkWelcomePresenter { it == "member@example.invalid" }
        moleculeFlow(RecompositionMode.Immediate) { presenter.present() }.test {
            awaitItem().eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Login))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Email("member@example.invalid"))
            awaitItem().eventSink(AntintwrkWelcomeEvent.Continue)
            val error = awaitItem()
            assertThat(error.error).isTrue()
            assertThat(error.unavailable).isFalse()
            assertThat(error.page).isEqualTo(AntintwrkPage.Login)
            error.eventSink(AntintwrkWelcomeEvent.Password("fixture"))
            assertThat(awaitItem().error).isFalse()
        }
    }

    @Test
    fun `provider preview clears passwords without advancing or creating a session`() = runTest {
        for (event in listOf(AntintwrkWelcomeEvent.GoogleSignIn, AntintwrkWelcomeEvent.PasskeySignIn)) {
            val presenter = AntintwrkWelcomePresenter { true }
            moleculeFlow(RecompositionMode.Immediate) { presenter.present() }.test {
                awaitItem().eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Login))
                awaitItem().eventSink(AntintwrkWelcomeEvent.Password("example-password"))
                awaitItem().eventSink(AntintwrkWelcomeEvent.TogglePassword)
                awaitItem().eventSink(event)
                val state = awaitItem()
                assertThat(state.page).isEqualTo(AntintwrkPage.Login)
                assertThat(state.password).isEmpty()
                assertThat(state.showPassword).isFalse()
                assertThat(state.unavailable).isTrue()
                assertThat(state.error).isFalse()
            }
        }
    }

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
