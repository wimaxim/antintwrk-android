/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import io.element.android.appnav.BuildConfig
import io.element.android.appnav.R
import io.element.android.features.userprofile.api.AntintwrkProfileNavigation
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.utils.LocalEmbeddedLinkHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Social authentication stays independent of Matrix session storage and callbacks. */
class AntintwrkNativeAuthNode(buildContext: BuildContext, private val onSwitchAccount: (String) -> Unit = {}) : Node(buildContext = buildContext) {
    @Composable
    override fun View(modifier: Modifier) = AntintwrkNativeAppView(modifier, onSwitchMatrixAccount = onSwitchAccount)
}

@Composable
internal fun AntintwrkNativeAppView(
    modifier: Modifier = Modifier,
    matrixUserId: String? = null,
    onRequestSignOut: (() -> Unit)? = null,
    onPrepareRecovery: (suspend () -> Boolean)? = null,
    recoveryStates: kotlinx.coroutines.flow.StateFlow<io.element.android.libraries.matrix.api.encryption.RecoveryState>? = null,
    onRecoveredSignOut: (suspend () -> Unit)? = null,
    onSwitchMatrixAccount: ((String) -> Unit)? = null,
    onOpenMessage: (suspend (String) -> Unit)? = null,
    messengerContent: (@Composable (Modifier) -> Unit)? = null,
) {
        val client = remember { AntintwrkNativeAuthClient() }
        val context = LocalAntintwrkActivity.current ?: LocalContext.current
        val credentials = remember { AntintwrkNativeCredentials(context) }
        val accountPreferences = remember { context.getSharedPreferences("antintwrk-account", android.content.Context.MODE_PRIVATE) }

        val pushStore = remember { AntintwrkSocialPushStore(context.applicationContext) }
        val pushRegistrar =
            remember {
                context.bindings<AntintwrkSocialPushBindings>().socialPushRegistrars().let { options ->
                    options.firstOrNull { it.available }
            ?: options.first()
                }
            }
        val messengerBindings = remember { context.bindings<AntintwrkMessengerBindings>() }
        val authentication = messengerBindings.matrixAuthenticationService()
        var pushOpen by remember { mutableStateOf(false) }
        var licencesOpen by remember { mutableStateOf(false) }
        val pushRequests by AntintwrkSocialPushRoutes.opens.collectAsState()
        val recoveryRetries by io.element.android.libraries.matrix.api.encryption.AntintwrkRecoveryStatus.retries.collectAsState()
        val profileRequests by AntintwrkSocialProfileRoutes.opens.collectAsState()
        val memberProfileRequest by AntintwrkProfileNavigation.requests.collectAsState()
        val presenter = remember { AntintwrkWelcomePresenter() }
        val shell = remember { AntintwrkShellPresenter() }
        val retainedSurface = LocalAntintwrkSocialSurface.current
        val shellState = retainedSurface?.let { it.shell.value.copy(eventSink = it::event) } ?: shell.present()
        val webController = retainedSurface?.web ?: rememberAntintwrkWebFeedController()
        AntintwrkWebFilePicker(webController)
        val state = presenter.present()
        val scope = rememberCoroutineScope()
        val messageRequest by AntintwrkMessengerRoutes.requests.collectAsState()
        var directBusy by remember { mutableStateOf(false) }
        var directFailed by remember { mutableStateOf(false) }
        var externalLinkFailed by remember { mutableStateOf(false) }
        var directAttempt by remember { mutableLongStateOf(0L) }
        var directJob by remember { mutableStateOf<Job?>(null) }
        val language = LocalConfiguration.current.locales[0].language
        LaunchedEffect(language) {
            val current = webController.currentUrl
            if (current != null && android.net.Uri.parse(current).path?.split('/')?.getOrNull(1) != language) {
                webController.destroy()
                webController.currentUrl =
                    AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN).root(language, shellState.section, webController.ownProfileIdentity)
            }
        }
        val profileError = stringResource(R.string.screen_ant_auth_profile_requirements)
        val expiredError = stringResource(R.string.screen_ant_auth_registration_expired)
        val passwordError = stringResource(R.string.screen_ant_auth_password_requirements)
        val sessionExpiredError = stringResource(R.string.screen_ant_auth_session_expired)
        val rateLimitError = stringResource(R.string.screen_ant_auth_rate_limited)
        var session by (retainedSurface?.admission ?: remember { mutableStateOf<AntintwrkAuthSession?>(null) })
        val actor = session?.matrixUserId
        var oauthUrl by remember(actor) { mutableStateOf<String?>(null) }
        var messengerBusy by remember(actor) { mutableStateOf(false) }
        var messengerFailed by remember(actor) { mutableStateOf(false) }
        var messengerAttempt by remember(actor) { mutableLongStateOf(0L) }
        var settingsOpen by remember { mutableStateOf(false) }
        var socialUnavailable by remember { mutableStateOf(false) }
        var matrixUnavailable by remember { mutableStateOf(false) }
        var healthAttempt by remember { mutableLongStateOf(0L) }
        var foreground by remember { mutableStateOf(true) }
        LaunchedEffect(actor, healthAttempt, foreground) {
            if (actor != null && foreground) {
                val health = AntintwrkServiceHealth()
                val socialGate = AntintwrkHealthGate()
                val matrixGate = AntintwrkHealthGate()
                while (true) {
                    kotlinx.coroutines.coroutineScope {
                        val social = async { health.reachable(false) }
                        val matrix = async { health.reachable(true) }
                        socialUnavailable = socialGate.record(social.await())
                        matrixUnavailable = matrixGate.record(matrix.await())
                    }
                    delay(if (socialGate.failures > 0 || matrixGate.failures > 0) 15_000 else 60_000)
                }
            }
        }

        val busyState: androidx.compose.runtime.MutableState<Boolean> = retainedSurface?.busy
            ?: remember { mutableStateOf(!accountPreferences.getBoolean("logged-out", false)) }
        var busy by busyState
        var failed by remember { mutableStateOf(false) }
        var sessionVerificationFailed by remember { mutableStateOf(false) }
        var googleRegistration by remember { mutableStateOf("") }
        var serverMessage by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var lastStatusAt by remember { mutableLongStateOf(0L) }
        webController.onReturnToMessenger = { shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger)) }
        webController.onMessenger = { target ->
            val current = session?.takeIf { it.stage == AntintwrkAuthStage.AUTHENTICATED }
            if (current != null && !directBusy) {
                if (target == null) {
                    shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
                } else {
                    directBusy = true
                    scope.launch {
                    directJob = currentCoroutineContext()[Job]
                    directBusy = true
                    directFailed = false
                    try {
                        val response = client.messengerRecipient(target, current.csrf)
                        require(response.getString("actorUserId") == current.matrixUserId)
                        if (session?.matrixUserId == current.matrixUserId) {
                            AntintwrkMessengerRoutes.open(response.getString("actorUserId"), response.getString("recipientUserId"))
                            shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        directFailed = true
                    } finally {
                        directBusy = false
                    }
                }
                }
            }
        }
        LaunchedEffect(messageRequest, matrixUserId, session?.matrixUserId, shellState.mode, directAttempt) {
            val request = messageRequest
            if (request != null && request.actor == session?.matrixUserId) {
                if (shellState.mode != AntintwrkMode.Messenger) return@LaunchedEffect
                if (matrixUserId == request.actor && onOpenMessage != null && !directFailed && !directBusy) {
                    directJob = currentCoroutineContext()[Job]
                    directBusy = true
                    try {
                        withTimeout(30_000) { onOpenMessage(request.recipient) }
                        AntintwrkMessengerRoutes.consume(request)
                    } catch (_: TimeoutCancellationException) {
                        directFailed = true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        directFailed = true
                    } finally {
                        directBusy = false
                    }
                }
            }
        }
        suspend fun runRequest(revalidate: Boolean = false, request: suspend () -> AntintwrkAuthSession) {
            // A messenger-node handoff must not replace an already admitted social page.
            busy = !revalidate || session?.stage != AntintwrkAuthStage.AUTHENTICATED
            sessionVerificationFailed = false
            failed = false
            serverMessage = ""
            try {
                session = request()
                if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
                    val previousActor = accountPreferences.getString("social-actor", null)
                    synchronized(io.element.android.libraries.push.api.PushAccountPolicyLock.monitor) {
                        accountPreferences.edit().putBoolean("logged-out", false).putString("social-actor", session?.matrixUserId)
                            .remove("logged-out-actor").commit()
                        if (previousActor != null && previousActor != session?.matrixUserId) {
                            androidx.core.app.NotificationManagerCompat.from(context).cancelAll()
                        }
                    }
                    if (previousActor != null && previousActor != session?.matrixUserId) {
                        webController.destroy()
                        webController.currentUrl = null
                        shellState.eventSink(AntintwrkShellEvent.Section(AntintwrkSection.Feed))
                        shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                        pushStore.clear()
                        AntintwrkSocialPushRoutes.consume()
                        AntintwrkSocialProfileRoutes.consume()
                        AntintwrkProfileNavigation.clear()
                    }
                    AntintwrkMessengerRoutes.discardOtherActor(session?.matrixUserId)
                }
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
        LaunchedEffect(sessionVerificationFailed, foreground) {
            while (sessionVerificationFailed && foreground && !accountPreferences.getBoolean("logged-out", false)) {
                delay(15_000)
                runRequest(revalidate = true) { client.session() }
            }
        }
        DisposableEffect(session?.stage, session?.matrixUserId, matrixUserId, session?.csrf) {
            val current = session
            val binding = if (current?.stage == AntintwrkAuthStage.AUTHENTICATED && current.matrixUserId == matrixUserId && matrixUserId != null) {
                io.element.android.libraries.matrix.api.user.AntintwrkSocialDirectory.Binding(
                    matrixUserId,
                    authorize = { recipient ->
                        val id = io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity.socialId(
                            recipient
                        )?.takeIf { io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity.sameRealm(matrixUserId, recipient) }
                        if (id == null) {
                            false
                        } else {
                            val result = try {
                                client.messengerRecipient(id, current.csrf)
                            } catch (failure: AntintwrkRequestError) {
                                if (failure.code == "CONTACT_UNAVAILABLE") {
                                    throw io.element.android.libraries.matrix.api.user.AntintwrkContactDeniedException()
                                }
                                throw failure
                            }
                            result.optString("actorUserId") == matrixUserId && result.optString("recipientUserId") == recipient
                        }
                    },
                    search = { query -> client.searchPeople(query, current.csrf) }
                )
            } else {
                null
            }
            binding?.let(io.element.android.libraries.matrix.api.user.AntintwrkSocialDirectory::bind)
            onDispose { binding?.let(io.element.android.libraries.matrix.api.user.AntintwrkSocialDirectory::clear) }
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
                        webController.profileName = status.name
                        if (status.avatar != webController.profileAvatarSource) {
                            val avatar = status.avatar?.let { client.avatar(it) }
                            if (avatar != null || status.avatar == null) {
                                webController.profileAvatar = avatar
                                webController.profileAvatarSource = status.avatar
                            }
                        }
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
            foreground = true
            if (!accountPreferences.getBoolean("logged-out", false) &&
                (session?.stage == AntintwrkAuthStage.AUTHENTICATED || !busy || (session == null && state.page == AntintwrkPage.Welcome))) {
                scope.launch { runRequest(revalidate = true) { client.session() } }
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { foreground = false }
        LaunchedEffect(session?.stage, busy) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy && pushStore.enabled && pushRegistrar.available) {
                pushRegistrar.register()
            }
        }
        LaunchedEffect(memberProfileRequest, session?.stage, busy) {
            val request = memberProfileRequest
            if (request != null && session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy) {
                AntintwrkProfileNavigation.consume(request)
                if (request.actor == session?.matrixUserId && request.actor == matrixUserId) {
                    val policy = AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN)
                    webController.openMessengerProfile(policy.route(language, "hero/${request.socialId}"))
                    shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                }
            }
        }
        LaunchedEffect(profileRequests, session?.stage, busy) {
            if (profileRequests > 0 && session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy) {
                AntintwrkSocialProfileRoutes.consume()
                AntintwrkProfileNavigation.clear()
                shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                shellState.eventSink(AntintwrkShellEvent.Section(AntintwrkSection.Profile))
            }
        }
        LaunchedEffect(pushRequests, session?.stage, busy) {
            if (pushRequests > 0 && session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy) {
                AntintwrkSocialPushRoutes.consume()
                AntintwrkSocialProfileRoutes.consume()
                AntintwrkProfileNavigation.clear()
                shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                webController.navigate(AntintwrkWebPolicy(io.element.android.appnav.BuildConfig.ANTINTWRK_SOCIAL_ORIGIN).route(language, "notifications"))
            }
        }
        LaunchedEffect(session?.matrixUserId, matrixUserId, recoveryRetries[matrixUserId.orEmpty()]) {
            if (matrixUserId != null && session?.matrixUserId == matrixUserId && onPrepareRecovery != null) {
                AntintwrkRecoveryMonitor(recoveryStates, onPrepareRecovery).run()
            }
        }
        LaunchedEffect(session?.matrixUserId, matrixUserId) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED && matrixUserId != null &&
                session?.matrixUserId != null && session?.matrixUserId != matrixUserId) {
                val actor = requireNotNull(session?.matrixUserId)
                if (shellState.mode == AntintwrkMode.Messenger) accountPreferences.edit().putString("connect-messenger-for", actor).commit()
                onSwitchMatrixAccount?.invoke(actor)
            }
        }
        LaunchedEffect(session?.matrixUserId) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED &&
                accountPreferences.getString("connect-messenger-for", null) == session?.matrixUserId) {
                shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
                if (matrixUserId == session?.matrixUserId) accountPreferences.edit().remove("connect-messenger-for").commit()
            }
        }
        LaunchedEffect(session?.matrixUserId, messengerAttempt) {
            if (matrixUserId == null &&
                session?.stage == AntintwrkAuthStage.AUTHENTICATED && !messengerBusy && !messengerFailed) {
                messengerBusy = true
                try {
                    val existing = actor?.let { messengerBindings.sessionStore().getSession(it) }
                    if (existing?.isTokenValid == true && onSwitchMatrixAccount != null) {
                        onSwitchMatrixAccount(existing.userId)
                        return@LaunchedEffect
                    }
                    authentication.cancelOAuthLogin()
                    clearAntintwrkOAuthCookies()
                    withTimeout(30_000) {
                        authentication.setHomeserver("https://matrix.antimsg.com").getOrThrow()
                        oauthUrl = authentication.getOAuthUrl(io.element.android.libraries.matrix.api.auth.OAuthPrompt.Login, "mxid:$actor").getOrThrow().url
                    }
                } catch (_: TimeoutCancellationException) {
                    messengerFailed = true
                    messengerBusy = false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    messengerFailed = true
                    messengerBusy = false
                }
            }
        }
        LaunchedEffect(actor, messengerFailed) {
            if (actor != null && messengerFailed) {
                delay((15_000L * (messengerAttempt + 1)).coerceAtMost(60_000L))
                messengerFailed = false
                messengerAttempt++
            }
        }
        LaunchedEffect(actor, oauthUrl) {
            if (oauthUrl != null) {
                delay(45_000)
                oauthUrl = null
                messengerBusy = false
                messengerFailed = true
                authentication.cancelOAuthLogin()
            }
        }
        oauthUrl?.let { url ->
            val expected = session?.matrixUserId
            if (expected != null && session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
                AntintwrkMessengerOAuth(
                    url,
                    expected,
                    session!!.csrf,
                    client,
                    messengerBindings.oauthRedirectUrlProvider().provide(),
                    onComplete = { callback ->
                        if (session?.matrixUserId != expected) return@AntintwrkMessengerOAuth
                        oauthUrl = null
                        if (shellState.mode == AntintwrkMode.Messenger) accountPreferences.edit().putString("connect-messenger-for", expected).commit()
                        scope.launch {
                            try {
                                if (session?.matrixUserId == expected) {
                                    authentication.loginWithOAuthForAccount(
                                        callback,
                                        io.element.android.libraries.matrix.api.core.UserId(expected)
                                    ).getOrThrow()
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                messengerFailed = true
                            } finally {
                                messengerBusy = false
                            }
                        }
                    },
                    onCancel = {
                        if (session?.matrixUserId != expected) return@AntintwrkMessengerOAuth
                        oauthUrl = null
                        messengerBusy = false
                        messengerFailed = true
                        scope.launch { authentication.cancelOAuthLogin() }
                    },
                    onError = {
                        if (session?.matrixUserId != expected) return@AntintwrkMessengerOAuth
                        oauthUrl = null
                        messengerBusy = false
                        messengerFailed = true
                        scope.launch { authentication.cancelOAuthLogin() }
                    }
                )
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
        fun signOut() {
                if (matrixUserId != null && matrixUserId == (session?.matrixUserId ?: accountPreferences.getString("social-actor", null)) &&
                    onRequestSignOut != null) {
                    scope.launch {
                        busy = true
                        try {
                            if (onRecoveredSignOut != null && onPrepareRecovery?.invoke() == true) {
                                onRecoveredSignOut()
                            } else {
                                shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
                                onRequestSignOut()
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
                            onRequestSignOut()
                        } finally {
                            busy = false
                        }
                    }
                } else {
                val current = session ?: return
                val pushFid = pushStore.fid
                val cookieSnapshot = client.clearLocalSession()
                AntintwrkMessengerRoutes.clear()
                accountPreferences.edit().putBoolean("logged-out", true).putString("logged-out-actor", current.matrixUserId).commit()
                AntintwrkSocialPushRoutes.consume()
                AntintwrkSocialProfileRoutes.consume()
                AntintwrkProfileNavigation.clear()
                pushStore.clear()
                pushOpen = false
                // Hide content immediately, even if the revocation request fails.
                shellState.eventSink(AntintwrkShellEvent.Section(AntintwrkSection.Feed))
                session = null
                webController.destroy()
                scope.launch {
                    runRequest {
                    try {
                        if (pushFid.isNotEmpty()) {
                            runCatching {
                            withTimeout(
                                5_000
                            ) { client.push("push-unsubscribe", current.csrf, mapOf("fid" to pushFid), cookieSnapshot) }
                        }
                        }
                        client.logout(current.csrf, cookieSnapshot)
                    } finally {
                        runCatching { credentials.clear() }
                    }
                    client.session()
                }
                }
                }
        }
        if (settingsOpen) {
            AntintwrkAppSettings(
                onDismiss = { settingsOpen = false },
                onSignOut = {
                    settingsOpen = false
                    signOut()
                },
                onRoute = { path ->
                    settingsOpen = false
                    when (path) {
                        "native-push" -> pushOpen = true
                        "native-licences" -> licencesOpen = true
                        else -> {
                            shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                            webController.navigate(AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN).route(language, path))
                        }
                    }
                }
            )
        }
        val externalUriHandler = LocalUriHandler.current
        @Composable
        fun RenderMessenger(contentModifier: Modifier) {
            CompositionLocalProvider(LocalEmbeddedLinkHandler provides { url ->
                val policy = AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN)
                val canonical = policy.canonical(url)
                val embedded = policy.embedded(canonical)
                if (policy.sameOrigin(canonical) && policy.outbound(canonical)) {
                    scope.launch {
                        try {
                            externalUriHandler.openUri(antintwrkExternalRedirect(canonical, policy))
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            externalLinkFailed = true
                        }
                    }
                    true
                } else if (embedded != null) {
                    webController.navigate(embedded)
                    shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                    true
                } else {
                    false
                }
            }) { messengerContent?.invoke(contentModifier) }
        }
        @Composable
        fun SocialAdmission(modifier: Modifier) {
        if (busy && (session == null || session?.stage == AntintwrkAuthStage.AUTHENTICATED)) {
            AntintwrkLoadingView(modifier)
        } else if (sessionVerificationFailed) {
            AntintwrkBrandTheme {
                AntintwrkServiceUnavailable(modifier, R.string.screen_ant_social_unavailable) {
                    scope.launch { runRequest(revalidate = true) { client.session() } }
                }
            }
        } else if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
            AntintwrkShellView(
                shellState,
                socialUnavailable = socialUnavailable,
                onRetrySocial = {
                    healthAttempt++
                    webController.retry()
                },
                modifier = modifier,
                webFeed = true,
                retainedWebController = webController,
                messengerContent = { contentModifier ->
                    if (messengerContent != null && matrixUserId == session?.matrixUserId) {
                        Box(contentModifier) {
                            RenderMessenger(if (matrixUnavailable) Modifier.fillMaxSize().clearAndSetSemantics {} else Modifier.fillMaxSize())
                            if (matrixUnavailable) {
                                AntintwrkServiceUnavailable(
                                    Modifier.fillMaxSize().background(AntintwrkBackground),
                                    R.string.screen_ant_messenger_unavailable
                                ) { healthAttempt++ }
                            }
                        }
                    } else {
                        if (messengerFailed) {
                            AntintwrkServiceUnavailable(contentModifier, R.string.screen_ant_messenger_unavailable) {
                                messengerFailed = false
                                messengerAttempt++
                            }
                        } else {
                            AntintwrkLoadingView(contentModifier)
                        }
                    }
                },
                onAuthRequired = {
                val returnUrl = webController.currentUrl
                webController.destroy()
                webController.currentUrl = returnUrl
                session = null
                state.eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Login))
                scope.launch { runRequest(revalidate = true) { client.session() } }
            },
                onOpenSettings = { settingsOpen = true },
                onPushSettings = { pushOpen = true },
                onOpenLicences = { licencesOpen = true },
                onSignOut = ::signOut
            )
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
        }
        val requiresSocialAdmission = busy || session?.stage != AntintwrkAuthStage.AUTHENTICATED || sessionVerificationFailed
        if (matrixUserId != null && messengerContent != null && requiresSocialAdmission &&
            !accountPreferences.getBoolean("logged-out", false) && accountPreferences.getString("social-actor", null) == matrixUserId) {
            AntintwrkShellView(
                shellState,
                modifier,
                onOpenSettings = { settingsOpen = true },
                socialContent = { SocialAdmission(it) },
                messengerContent = { RenderMessenger(it) }
            )
        } else {
            SocialAdmission(modifier)
        }
        if (licencesOpen) AntintwrkLicencesDialog { licencesOpen = false }
        if (directBusy) {
            Dialog(onDismissRequest = {
                directJob?.cancel()
                AntintwrkMessengerRoutes.clear()
                directBusy = false
            }) {
            AntintwrkLoadingView()
        }
        }
        if (directFailed || externalLinkFailed) {
            Dialog(onDismissRequest = {
                directFailed = false
                externalLinkFailed = false
                AntintwrkMessengerRoutes.clear()
            }) {
            AntintwrkBrandTheme {
                Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.screen_ant_auth_request_error))
                    if (messageRequest != null) {
                        TextButton(stringResource(R.string.screen_ant_web_retry), {
                        directFailed = false
                        directAttempt++
                    })
                    }
                    TextButton(stringResource(R.string.screen_antintwrk_back), {
                        directFailed = false
                        externalLinkFailed = false
                        AntintwrkMessengerRoutes.clear()
                    })
                }
            }
        }
        }
    }
