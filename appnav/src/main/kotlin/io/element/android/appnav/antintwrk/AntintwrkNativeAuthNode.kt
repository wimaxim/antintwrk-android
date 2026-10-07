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
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
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
        val state = presenter.present()
        val scope = rememberCoroutineScope()
        val language = LocalConfiguration.current.locales[0].language
        var session by remember { mutableStateOf<AntintwrkAuthSession?>(null) }
        var busy by remember { mutableStateOf(true) }
        var failed by remember { mutableStateOf(false) }
        var code by remember { mutableStateOf("") }
        suspend fun runRequest(request: suspend () -> AntintwrkAuthSession) {
            busy = true
            failed = false
            try {
                session = request()
                if (session?.stage != AntintwrkAuthStage.AUTHENTICATED) webController.destroy()
                if (session?.stage == AntintwrkAuthStage.DEVICE) client.startDevice(session!!.csrf)
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
            AntintwrkWelcomeView(state.copy(nativeLogin = true, busy = busy, networkError = failed, eventSink = { event ->
                if (!busy && event == AntintwrkWelcomeEvent.Continue && state.page == AntintwrkPage.Login) {
                    val email = state.email
                    val password = state.password
                    state.eventSink(AntintwrkWelcomeEvent.Password(""))
                    scope.launch { runRequest { client.login(email, password, language, (session ?: client.session()).csrf) } }
                } else if (!busy && state.page == AntintwrkPage.Login &&
                    event in setOf(AntintwrkWelcomeEvent.GoogleSignIn, AntintwrkWelcomeEvent.PasskeySignIn)) {
                    state.eventSink(AntintwrkWelcomeEvent.Password(""))
                    val provider = if (event == AntintwrkWelcomeEvent.GoogleSignIn) "google" else "passkey"
                    scope.launch { runRequest { credentials.signIn(context, client, provider, (session ?: client.session()).csrf) } }
                } else if (!busy) {
                    failed = false
                    state.eventSink(event)
                }
            }), modifier)
        }
    }
}
