/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import io.element.android.libraries.architecture.bindings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Social authentication stays independent of Matrix session storage and callbacks. */
class AntintwrkNativeAuthNode(buildContext: BuildContext) : Node(buildContext = buildContext) {
    @Composable
    override fun View(modifier: Modifier) {
        val client = remember { AntintwrkNativeAuthClient() }
        val context = LocalAntintwrkActivity.current ?: LocalContext.current
        val credentials = remember { AntintwrkNativeCredentials(context) }
        val pushStore = remember { AntintwrkSocialPushStore(context.applicationContext) }
        val pushRegistrar =
            remember {
                context.bindings<AntintwrkSocialPushBindings>().socialPushRegistrars().let { options ->
                    options.firstOrNull { it.available }
            ?: options.first()
                }
            }
        var pushOpen by remember { mutableStateOf(false) }
        var licencesOpen by remember { mutableStateOf(false) }
        val pushRequests by AntintwrkSocialPushRoutes.opens.collectAsState()
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
        val sessionExpiredError = stringResource(R.string.screen_ant_auth_session_expired)
        val rateLimitError = stringResource(R.string.screen_ant_auth_rate_limited)
        var session by remember { mutableStateOf<AntintwrkAuthSession?>(null) }
        var busy by remember { mutableStateOf(true) }
        var failed by remember { mutableStateOf(false) }
        var sessionVerificationFailed by remember { mutableStateOf(false) }
        var googleRegistration by remember { mutableStateOf("") }
        var serverMessage by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var lastStatusAt by remember { mutableLongStateOf(0L) }
        suspend fun runRequest(revalidate: Boolean = false, request: suspend () -> AntintwrkAuthSession) {
            busy = true
            sessionVerificationFailed = false
            failed = false
            serverMessage = ""
            try {
                session = request()
                webController.ownProfileIdentity = session?.profileIdentity
                if (session?.stage != AntintwrkAuthStage.AUTHENTICATED) {
                    pushStore.clear()
                    pushOpen = false
                    webController.destroy()
                }
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
                    "SESSION_EXPIRED" -> sessionExpiredError
                    "RATE_LIMITED" -> rateLimitError
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
                sessionVerificationFailed = revalidate && failed
                busy = false
                code = ""
            }
        }
        LaunchedEffect(session?.stage, busy, webController.refreshVersion, webController.currentUrl) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy) {
                while (true) {
                    delay((10_000L - (android.os.SystemClock.elapsedRealtime() - lastStatusAt)).coerceAtLeast(0L))
                    lastStatusAt = android.os.SystemClock.elapsedRealtime()
                    try {
                        val status = client.status(session!!.csrf)
                        webController.notifications = status.notifications
                        webController.ownProfileIdentity = status.profileIdentity
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: AntintwrkRequestError) {
                        if (error.code == "AUTH_REQUIRED") {
                            runRequest(revalidate = true) { client.session() }
                            break
                        }
                    } catch (_: Exception) {
                        // A failed optional count refresh never grants or changes account admission.
                    }
                    delay(30_000)
                }
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED || !busy || (session == null && state.page == AntintwrkPage.Welcome)) {
                scope.launch { runRequest(revalidate = true) { client.session() } }
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) busy = true }
        LaunchedEffect(session?.stage, busy) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy && pushStore.enabled && pushRegistrar.available) {
                pushRegistrar.register()
            }
        }
        LaunchedEffect(pushRequests, session?.stage, busy) {
            if (pushRequests > 0 && session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy) {
                AntintwrkSocialPushRoutes.consume()
                shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                webController.navigate(AntintwrkWebPolicy(io.element.android.appnav.BuildConfig.ANTINTWRK_SOCIAL_ORIGIN).route(language, "notifications"))
            }
        }
        AntintwrkPushSettings(
            client,
            session?.takeIf { it.stage == AntintwrkAuthStage.AUTHENTICATED }?.csrf,
            pushOpen,
            !busy,
            pushRegistrar,
            pushStore,
            onDismiss = { pushOpen = false },
            onAuthRequired = {
                pushStore.clear()
                scope.launch { runRequest(revalidate = true) { client.session() } }
            }
        )
        if (busy && (session == null || session?.stage == AntintwrkAuthStage.AUTHENTICATED)) {
            AntintwrkLoadingView(modifier)
        } else if (sessionVerificationFailed) {
            AntintwrkBrandTheme {
                Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.screen_ant_auth_session_check_error))
                    TextButton(stringResource(R.string.screen_ant_web_retry), { scope.launch { runRequest(revalidate = true) { client.session() } } })
                }
            }
        } else if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
            AntintwrkShellView(shellState, modifier, webFeed = true, retainedWebController = webController, onAuthRequired = {
                val returnUrl = webController.currentUrl
                webController.destroy()
                webController.currentUrl = returnUrl
                session = null
                state.eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Login))
                scope.launch { runRequest(revalidate = true) { client.session() } }
            }, onPushSettings = { pushOpen = true }, onOpenLicences = { licencesOpen = true }, onSignOut = {
                val current = session!!
                val pushFid = pushStore.fid
                AntintwrkSocialPushRoutes.consume()
                pushStore.clear()
                pushOpen = false
                // Hide content immediately, even if the revocation request fails.
                shellState.eventSink(AntintwrkShellEvent.Section(AntintwrkSection.Feed))
                session = null
                webController.destroy()
                scope.launch {
                    runRequest {
                    try {
                        if (pushFid.isNotEmpty()) runCatching { withTimeout(5_000) { client.push("push-unsubscribe", current.csrf, mapOf("fid" to pushFid)) } }
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
                    if (!presenter.canSignIn(state)) {
                        failed = false
                        serverMessage = ""
                        state.eventSink(event)
                    } else {
                        val email = state.email
                        val password = state.password
                        state.eventSink(AntintwrkWelcomeEvent.Password(""))
                        scope.launch { runRequest { client.login(email, password, language, client.session().csrf) } }
                    }
                } else if (!busy && event == AntintwrkWelcomeEvent.Continue && state.page in setOf(AntintwrkPage.Profile, AntintwrkPage.Reset)) {
                    if (state.page == AntintwrkPage.Reset && !presenter.canRequestEmail(state) ||
                        state.page == AntintwrkPage.Profile && (state.firstName.isBlank() || state.birthdate.isBlank() || !state.agreed)) {
                        failed = false
                        serverMessage = ""
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
            }), modifier, onOpenLicences = { licencesOpen = true })
        }
        if (licencesOpen) AntintwrkLicencesDialog { licencesOpen = false }
    }
}
