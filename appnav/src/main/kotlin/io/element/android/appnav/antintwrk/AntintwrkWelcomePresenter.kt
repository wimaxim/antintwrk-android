/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.util.Patterns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class AntintwrkWelcomePresenter(
    private val isValidEmail: (String) -> Boolean = { Patterns.EMAIL_ADDRESS.matcher(it).matches() },
) {
    @Composable
    fun present(): AntintwrkWelcomeState {
        // Credentials stay in memory only; never save them in a Bundle or logs.
        var state by remember { mutableStateOf(AntintwrkWelcomeState()) }
        return state.copy(eventSink = { event ->
            state = when (event) {
                is AntintwrkWelcomeEvent.Open -> AntintwrkWelcomeState(page = event.page)
                is AntintwrkWelcomeEvent.Email -> state.copy(email = event.value.take(50), error = false, unavailable = false)
                is AntintwrkWelcomeEvent.Password -> state.copy(password = event.value.take(128), error = false, unavailable = false)
                is AntintwrkWelcomeEvent.FirstName -> state.copy(firstName = event.value.take(50), error = false)
                is AntintwrkWelcomeEvent.Surname -> state.copy(surname = event.value.take(50))
                is AntintwrkWelcomeEvent.Birthdate -> state.copy(birthdate = event.value.take(10), error = false)
                is AntintwrkWelcomeEvent.Agreement -> state.copy(agreed = event.value, error = false)
                AntintwrkWelcomeEvent.TogglePassword -> state.copy(showPassword = !state.showPassword)
                AntintwrkWelcomeEvent.GoogleSignIn, AntintwrkWelcomeEvent.PasskeySignIn -> {
                    // Provider verification and mobile session exchange are not connected yet.
                    state.copy(password = "", showPassword = false, unavailable = true, error = false)
                }
                AntintwrkWelcomeEvent.Back -> when (state.page) {
                    AntintwrkPage.Profile -> state.copy(page = AntintwrkPage.Register, error = false, unavailable = false)
                    AntintwrkPage.Reset -> AntintwrkWelcomeState(page = AntintwrkPage.Login, email = state.email)
                    else -> AntintwrkWelcomeState()
                }
                AntintwrkWelcomeEvent.Continue -> {
                    val emailValid = isValidEmail(state.email.trim())
                    if (state.page == AntintwrkPage.Register && emailValid && state.password.length >= 8) {
                        state.copy(page = AntintwrkPage.Profile, error = false, showPassword = false)
                    } else if (state.page == AntintwrkPage.Register) {
                        state.copy(error = true, unavailable = false)
                    } else if (state.page == AntintwrkPage.Profile && (state.firstName.isBlank() || state.birthdate.isBlank() || !state.agreed)) {
                        state.copy(error = true, unavailable = false)
                    } else {
                        // No authentication or registration request until the server contract is integrated.
                        state.copy(unavailable = true, error = false)
                    }
                }
            }
        })
    }
}
