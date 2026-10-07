/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import io.element.android.appnav.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Social authentication stays independent of Matrix session storage and callbacks. */
class AntintwrkNativeAuthNode(buildContext: BuildContext) : Node(buildContext = buildContext) {
    @Composable
    override fun View(modifier: Modifier) {
        val client = remember { AntintwrkNativeAuthClient() }
        val context = LocalAntintwrkActivity.current ?: LocalContext.current
        val credentials = remember { AntintwrkNativeCredentials(context) }
        val presenter = remember { AntintwrkWelcomePresenter() }
        val shell = remember { AntintwrkShellPresenter() }
        val shellState = shell.present()
        val webController = rememberAntintwrkWebFeedController()
        AntintwrkWebFilePicker(webController)
        val state = presenter.present()
        val scope = rememberCoroutineScope()
        val language = LocalConfiguration.current.locales[0].language
        val profileError = stringResource(R.string.screen_ant_auth_profile_requirements)
        val expiredError = stringResource(R.string.screen_ant_auth_registration_expired)
        val passwordError = stringResource(R.string.screen_ant_auth_password_requirements)
        var session by remember { mutableStateOf<AntintwrkAuthSession?>(null) }
        var busy by remember { mutableStateOf(true) }
        var failed by remember { mutableStateOf(false) }
        var googleRegistration by remember { mutableStateOf("") }
        var serverMessage by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        suspend fun runRequest(request: suspend () -> AntintwrkAuthSession) {
            busy = true
            failed = false
            serverMessage = ""
            try {
                session = request()
                if (session?.stage != AntintwrkAuthStage.AUTHENTICATED) webController.destroy()
                if (session?.stage == AntintwrkAuthStage.DEVICE) client.startDevice(session!!.csrf)
            } catch (required: AntintwrkRegistrationRequired) {
                googleRegistration = required.ticket
                state.eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Profile))
                state.eventSink(AntintwrkWelcomeEvent.Email(required.email))
                state.eventSink(AntintwrkWelcomeEvent.FirstName(required.firstName))
                state.eventSink(AntintwrkWelcomeEvent.Surname(required.surname))
            } catch (error: AntintwrkRequestError) {
                serverMessage = when (error.code) {
                    "PROFILE" -> profileError
                    "REGISTRATION_EXPIRED" -> expiredError
                    else -> ""
                }
                failed = true
            } catch (error: AntintwrkAccountError) {
                serverMessage = error.messageForUser
                failed = true
            } catch (_: GetCredentialCancellationException) {
                // Dismissing the system picker never admits a session.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed = true
                // An uncertain request never admits a user to the shell.
                if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
                    session = null
                    webController.destroy()
                }
            } finally {
                if (failed && session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
                    session = null
                    webController.destroy()
                }
                busy = false
                code = ""
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED || !busy || (session == null && state.page == AntintwrkPage.Welcome)) {
                scope.launch { runRequest { client.session() } }
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) busy = true }
        if (busy && (session == null || session?.stage == AntintwrkAuthStage.AUTHENTICATED)) {
            AntintwrkLoadingView(modifier)
        } else if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
            AntintwrkShellView(shellState, modifier, webFeed = true, retainedWebController = webController, onAuthRequired = {
                webController.destroy()
                session = null
                state.eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Login))
                scope.launch { runRequest { client.session() } }
            }, onSignOut = {
                val current = session!!
                // Hide content immediately, even if the revocation request fails.
                session = null
                webController.destroy()
                scope.launch {
                    runRequest {
                    try {
                        client.logout(current.csrf)
                    } finally {
                        runCatching { credentials.clear() }
                    }
                    client.session()
                }
                }
            })
        } else if (session?.stage in setOf(AntintwrkAuthStage.DEVICE, AntintwrkAuthStage.TOTP, AntintwrkAuthStage.SETUP_REQUIRED)) {
            AntintwrkAuthChallengeView(
                session!!.stage,
                code,
                busy,
                failed,
                onCode = { code = it.filter(Char::isDigit).take(6) },
                onConfirm = {
                    val current = session!!
                    val submitted = code
                    scope.launch { runRequest { client.confirm(current.stage, submitted, current.csrf) } }
                },
                onBack = {
                    val current = session!!
                    scope.launch {
                        runRequest {
                        client.logout(current.csrf)
                        client.session()
                    }
                    }
                },
                modifier = modifier
            )
        } else {
            AntintwrkWelcomeView(state.copy(nativeLogin = true, busy = busy, networkError = failed, serverMessage = serverMessage, eventSink = { event ->
                if (!busy && event == AntintwrkWelcomeEvent.Continue && state.page == AntintwrkPage.Register &&
                    !AntintwrkRegistrationRules.strongPassword(state.password)) {
                    serverMessage = passwordError
                } else if (!busy && event == AntintwrkWelcomeEvent.Continue && state.page == AntintwrkPage.Login) {
                    val email = state.email
                    val password = state.password
                    state.eventSink(AntintwrkWelcomeEvent.Password(""))
                    scope.launch { runRequest { client.login(email, password, language, (session ?: client.session()).csrf) } }
                } else if (!busy && event == AntintwrkWelcomeEvent.Continue && state.page in setOf(AntintwrkPage.Profile, AntintwrkPage.Reset)) {
                    if (state.page == AntintwrkPage.Profile && (state.firstName.isBlank() || state.birthdate.isBlank() || !state.agreed)) {
                        state.eventSink(event)
                    } else {
                        val fields = mapOf(
                            "email" to state.email,
                            "password" to state.password,
                            "yourname" to state.firstName,
                            "surname" to state.surname,
                            "birthdate" to state.birthdate,
                            "agreement" to if (state.agreed) "1" else "0",
                            "lng" to language
                        )
                        scope.launch {
                            runRequest {
                                val csrf = client.session().csrf
                                if (googleRegistration.isNotEmpty() && state.page == AntintwrkPage.Profile) {
                                    client.registerGoogle(fields, googleRegistration, csrf).also { googleRegistration = "" }
                                } else {
                                    val registering = state.page == AntintwrkPage.Profile
                                    client.requestAccount(if (registering) "register" else "reset", fields, csrf)
                                    state.eventSink(AntintwrkWelcomeEvent.Open(if (registering) AntintwrkPage.RegisterSent else AntintwrkPage.ResetSent))
                                    client.session()
                                }
                            }
                        }
                    }
                } else if (!busy && state.page in setOf(AntintwrkPage.Login, AntintwrkPage.Register) &&
                    event in setOf(AntintwrkWelcomeEvent.GoogleSignIn, AntintwrkWelcomeEvent.PasskeySignIn)) {
                    state.eventSink(AntintwrkWelcomeEvent.Password(""))
                    val provider = if (event == AntintwrkWelcomeEvent.GoogleSignIn) "google" else "passkey"
                    scope.launch { runRequest { credentials.signIn(context, client, provider, (session ?: client.session()).csrf) } }
                } else if (!busy) {
                    failed = false
                    serverMessage = ""
                    if (event is AntintwrkWelcomeEvent.Open || event == AntintwrkWelcomeEvent.Back) googleRegistration = ""
                    state.eventSink(event)
                }
            }), modifier)
        }
    }
}
