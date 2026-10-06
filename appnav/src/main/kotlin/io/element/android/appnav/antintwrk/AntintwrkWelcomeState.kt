/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

enum class AntintwrkPage { Welcome, Register, Profile, Login, Reset }

data class AntintwrkWelcomeState(
    val page: AntintwrkPage = AntintwrkPage.Welcome,
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val surname: String = "",
    val birthdate: String = "",
    val agreed: Boolean = false,
    val showPassword: Boolean = false,
    val error: Boolean = false,
    val unavailable: Boolean = false,
    val eventSink: (AntintwrkWelcomeEvent) -> Unit = {},
)

sealed interface AntintwrkWelcomeEvent {
    data class Open(val page: AntintwrkPage) : AntintwrkWelcomeEvent
    data class Email(val value: String) : AntintwrkWelcomeEvent
    data class Password(val value: String) : AntintwrkWelcomeEvent
    data class FirstName(val value: String) : AntintwrkWelcomeEvent
    data class Surname(val value: String) : AntintwrkWelcomeEvent
    data class Birthdate(val value: String) : AntintwrkWelcomeEvent
    data class Agreement(val value: Boolean) : AntintwrkWelcomeEvent
    data object TogglePassword : AntintwrkWelcomeEvent
    data object GoogleSignIn : AntintwrkWelcomeEvent
    data object PasskeySignIn : AntintwrkWelcomeEvent
    data object Continue : AntintwrkWelcomeEvent
    data object Back : AntintwrkWelcomeEvent
}
