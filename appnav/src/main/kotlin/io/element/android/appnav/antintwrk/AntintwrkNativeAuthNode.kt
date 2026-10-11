/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import io.element.android.appnav.R
import io.element.android.features.userprofile.api.AntintwrkProfileNavigation
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.designsystem.utils.LocalEmbeddedLinkHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Social authentication stays independent of Matrix session storage and callbacks. */
class AntintwrkNativeAuthNode(buildContext: BuildContext, private val onSwitchAccount: (String) -> Unit = {}) : Node(buildContext = buildContext) {
    @Composable
    override fun View(modifier: Modifier) = AntintwrkNativeAppView(modifier, onSwitchMatrixAccount = onSwitchAccount)
}

@Composable
@OptIn(kotlinx.coroutines.FlowPreview::class)
internal fun AntintwrkNativeAppView(
    modifier: Modifier = Modifier,
    matrixUserId: String? = null,
    messengerRooms: io.element.android.libraries.matrix.api.roomlist.RoomList? = null,
    hiddenRooms: kotlinx.coroutines.flow.StateFlow<Map<String, Long>>? = null,
    onRequestSignOut: (() -> Unit)? = null,
    onPrepareRecovery: (suspend () -> Boolean)? = null,
    recoveryStates: kotlinx.coroutines.flow.StateFlow<io.element.android.libraries.matrix.api.encryption.RecoveryState>? = null,
    onRecoveredSignOut: (suspend () -> Unit)? = null,
    onSwitchMatrixAccount: ((String) -> Unit)? = null,
    onOpenChats: (suspend () -> Unit)? = null,
    onOpenAppPreference: (suspend (io.element.android.features.preferences.api.PreferencesEntryPoint.InitialTarget) -> Unit)? = null,
    onOpenMessage: (suspend (String) -> Unit)? = null,
    messengerContent: (@Composable (Modifier) -> Unit)? = null,
) {
        val unreadFlow = remember(messengerRooms, hiddenRooms) {
            messengerRooms?.summaries?.let { rooms ->
                kotlinx.coroutines.flow.combine(rooms, hiddenRooms ?: kotlinx.coroutines.flow.MutableStateFlow(emptyMap())) { entries, hidden ->
                    antintwrkUnreadMessages(entries.filter {
                        it.info.currentUserMembership == io.element.android.libraries.matrix.api.room.CurrentUserMembership.INVITED ||
                            it.roomId.value !in hidden
                    })
                }.distinctUntilChanged()
            } ?: kotlinx.coroutines.flow.flowOf(0L)
        }
        val unreadMessages by androidx.compose.runtime.key(matrixUserId) { unreadFlow.collectAsState(initial = 0L) }
        val context = LocalAntintwrkActivity.current ?: LocalContext.current
        LaunchedEffect(Unit) { AntintwrkIncomingShares.cleanup(context.applicationContext) }
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
        val client = remember(webController.authOrigin) { AntintwrkNativeAuthClient(webController.authOrigin) }
        val routing = remember { AntintwrkSocialOriginCoordinator(context.applicationContext) }
        val geoNetwork = remember { AntintwrkGeoNetworkMonitor(context.applicationContext) }
        webController.onRoutingApplied = routing::recordApplied
        val socialClient = remember(webController.origin) { AntintwrkNativeAuthClient(webController.origin) }
        LaunchedEffect(routing, webController.routingSelected) { if (!webController.routingSelected) routing.preload() }
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
                    AntintwrkWebPolicy(webController.origin).root(language, shellState.section, webController.ownProfileIdentity)
            }
        }
        val profileError = stringResource(R.string.screen_ant_auth_profile_requirements)
        val expiredError = stringResource(R.string.screen_ant_auth_registration_expired)
        val passwordError = stringResource(R.string.screen_ant_auth_password_requirements)
        val sessionExpiredError = stringResource(R.string.screen_ant_auth_session_expired)
        val rateLimitError = stringResource(R.string.screen_ant_auth_rate_limited)
        var session by (retainedSurface?.admission ?: remember { mutableStateOf<AntintwrkAuthSession?>(null) })
        AntintwrkStoreBinding(socialClient, webController.routedSession?.takeIf { it.matrixUserId == session?.matrixUserId }, webController)
        val actor = session?.matrixUserId
        AntintwrkIncomingShareView(
            actor = actor.takeIf { session?.stage == AntintwrkAuthStage.AUTHENTICATED },
            controller = webController,
            language = language,
            onSocial = { shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social)) },
        )
        var oauthUrl by remember(actor) { mutableStateOf<String?>(null) }
        var messengerBusy by remember(actor) { mutableStateOf(false) }
        var messengerFailed by remember(actor) { mutableStateOf(false) }
        var messengerAttempt by remember(actor) { mutableLongStateOf(0L) }
        var settingsOpen by remember(actor) { mutableStateOf(false) }
        var settingsFailed by remember(actor) { mutableStateOf(false) }
        var socialUnavailable by remember { mutableStateOf(false) }
        var matrixUnavailable by remember { mutableStateOf(false) }
        var healthAttempt by remember { mutableLongStateOf(0L) }
        var foreground by remember { mutableStateOf(true) }
        var transportFailure by remember { mutableLongStateOf(0L) }
        val latestClient by rememberUpdatedState(client)
        suspend fun refreshGeo(force: Boolean, unavailable: Boolean = false) {
            val admitted = session?.takeIf { it.stage == AntintwrkAuthStage.AUTHENTICATED }
            if (admitted == null) {
                routing.preload(force)
                return
            }
            routing.refresh(latestClient, admitted, webController, force, unavailable)
            if (unavailable && foreground && (socialUnavailable || webController.failed) && webController.applyPreparedRouting()) {
                socialUnavailable = false
                webController.retry()
            }
        }
        webController.onTransportFailure = { transportFailure++ }
        LaunchedEffect(actor, foreground) {
            if (foreground) {
                val periodic = flow {
                    emit(false) // Resume only probes if the five-minute cache has expired.
                    while (true) {
                        delay(routing.nextCheckDelay())
                        emit(false)
                    }
                }
                val network = geoNetwork.changes.debounce(2500).map { true }
                merge(periodic, network).collectLatest { force -> refreshGeo(force, unavailable = socialUnavailable || webController.failed) }
            }
        }
        LaunchedEffect(actor, foreground, socialUnavailable, transportFailure) {
            if (actor != null && foreground && (socialUnavailable || webController.failed)) refreshGeo(true, unavailable = true)
        }
        LaunchedEffect(actor, healthAttempt, foreground, webController.origin) {
            if (actor != null && foreground) {
                val health = AntintwrkServiceHealth()
                val socialGate = AntintwrkHealthGate()
                val matrixGate = AntintwrkHealthGate()
                while (true) {
                    kotlinx.coroutines.coroutineScope {
                        val social = async { health.reachable(false, webController.origin) }
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
        var backgroundValidationPending by remember { mutableStateOf(false) }
        val chatsRequestState: androidx.compose.runtime.MutableState<Boolean> = retainedSurface?.chatsRequested ?: remember { mutableStateOf(false) }
        var chatsRequested by chatsRequestState
        var navigationSequence by remember { mutableLongStateOf(0L) }
        val navigationEpoch = remember { AntintwrkNavigationEpoch() }
        val navigationSnackbar = remember { SnackbarHostState() }
        fun selectMode(mode: AntintwrkMode) {
            navigationSequence++
            navigationEpoch.advance()
            navigationSnackbar.currentSnackbarData?.dismiss()
            directJob?.cancel()
            directBusy = false
            directFailed = false
            AntintwrkMessengerRoutes.clear()
            chatsRequested = mode == AntintwrkMode.Messenger
            if (mode == AntintwrkMode.Social) shellState.eventSink(AntintwrkShellEvent.Mode(mode))
        }
        val currentOpenChats by androidx.compose.runtime.rememberUpdatedState(onOpenChats)
        val navigationActor = session?.matrixUserId ?: accountPreferences.getString("social-actor", null)
        LaunchedEffect(chatsRequested, matrixUserId, navigationActor, navigationSequence) {
            if (chatsRequested && matrixUserId != null && matrixUserId == navigationActor && currentOpenChats != null) {
                val sequence = navigationSequence
                val epoch = navigationEpoch.current
                try {
                    antintwrkPrepareChats({ navigationEpoch.accepts(epoch, foreground) }) { currentOpenChats?.invoke() }
                    if (sequence == navigationSequence && navigationEpoch.accepts(epoch, foreground)) {
                        chatsRequested = false
                        shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
                    }
                } catch (cancelled: CancellationException) {
                    if (cancelled !is TimeoutCancellationException) throw cancelled
                    if (navigationEpoch.accepts(epoch, foreground)) {
                        chatsRequested = false
                        directFailed = true
                    }
                } catch (_: Exception) {
                    if (navigationEpoch.accepts(epoch, foreground)) {
                        chatsRequested = false
                        directFailed = true
                    }
                }
            }
        }
        var googleRegistration by remember { mutableStateOf("") }
        var serverMessage by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var lastStatusAt by remember { mutableLongStateOf(0L) }
        webController.onReturnToMessenger = { shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger)) }
        webController.onMessenger = { target ->
            val current = session?.takeIf { it.stage == AntintwrkAuthStage.AUTHENTICATED }
            if (current != null && !directBusy) {
                if (target == null) {
                    selectMode(AntintwrkMode.Messenger)
                } else {
                    chatsRequested = false
                    val sequence = ++navigationSequence
                    val epoch = navigationEpoch.advance()
                    navigationSnackbar.currentSnackbarData?.dismiss()
                    directBusy = true
                    scope.launch {
                    directJob = currentCoroutineContext()[Job]
                    directBusy = true
                    directFailed = false
                    try {
                        val response = antintwrkRetryNavigationRead({ navigationEpoch.accepts(epoch, foreground) }) {
                            client.messengerRecipient(target, current.csrf)
                        }
                        require(response.getString("actorUserId") == current.matrixUserId)
                        if (sequence == navigationSequence && session?.matrixUserId == current.matrixUserId) {
                            AntintwrkMessengerRoutes.open(response.getString("actorUserId"), response.getString("recipientUserId"))
                            shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Messenger))
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        if (navigationEpoch.accepts(epoch, foreground)) directFailed = true
                    } finally {
                        if (navigationEpoch.current == epoch) directBusy = false
                    }
                }
                }
            }
        }
        LaunchedEffect(messageRequest, matrixUserId, session?.matrixUserId, shellState.mode, directAttempt, foreground) {
            val request = messageRequest
            if (foreground && request != null && request.actor == session?.matrixUserId) {
                if (shellState.mode != AntintwrkMode.Messenger) return@LaunchedEffect
                if (matrixUserId == request.actor && onOpenMessage != null && !directFailed && !directBusy) {
                    val epoch = navigationEpoch.advance()
                    directJob = currentCoroutineContext()[Job]
                    directBusy = true
                    try {
                        withTimeout(30_000) { onOpenMessage(request.recipient) }
                        AntintwrkMessengerRoutes.consume(request)
                    } catch (_: TimeoutCancellationException) {
                        if (navigationEpoch.accepts(epoch, foreground)) directFailed = true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        if (navigationEpoch.accepts(epoch, foreground)) directFailed = true
                    } finally {
                        if (navigationEpoch.current == epoch) directBusy = false
                    }
                }
            }
        }
        suspend fun readAdmission(): AntintwrkAuthSession {
            val expected = accountPreferences.getString("social-actor", null).takeUnless { accountPreferences.getBoolean("logged-out", false) }
            val epoch = AntintwrkSocialCookieEpoch.current()
            if (client.origin != AntintwrkSocialOrigins.RUSSIAN) return client.session()
            return antintwrkRestoreSocialAdmission(expected, primary = { client.session() }, fallback = {
                AntintwrkNativeAuthClient(AntintwrkSocialOrigins.PUBLIC).session()
            }, acceptedFallback = {
                synchronized(AntintwrkSocialCookieEpoch.monitor) { AntintwrkSocialCookieEpoch.check(epoch) }
                check(!accountPreferences.getBoolean("logged-out", false) && accountPreferences.getString("social-actor", null) == expected)
                webController.authOrigin = AntintwrkSocialOrigins.PUBLIC
            })
        }
        val admissionEpoch = retainedSurface?.admissionEpoch ?: remember { AntintwrkNavigationEpoch() }
        var admissionPending by remember { mutableStateOf<Long?>(null) }
        suspend fun runRequest(revalidate: Boolean = false, request: suspend () -> AntintwrkAuthSession) {
            if (revalidate && admissionPending == admissionEpoch.current) return
            val operation = if (revalidate) admissionEpoch.current else admissionEpoch.advance()
            admissionPending = operation
            fun checkCurrent() {
                if (operation != admissionEpoch.current) throw CancellationException("Admission superseded")
            }
            // A transient background check must not destroy an already admitted page.
            var retainAdmission = antintwrkRetainAdmission(revalidate, session?.stage == AntintwrkAuthStage.AUTHENTICATED)
            backgroundValidationPending = false
            busy = !revalidate || session?.stage != AntintwrkAuthStage.AUTHENTICATED
            sessionVerificationFailed = false
            failed = false
            serverMessage = ""
            try {
                val candidate = request()
                checkCurrent()
                if (candidate.stage == AntintwrkAuthStage.AUTHENTICATED) {
                    val previousActor = accountPreferences.getString("social-actor", null)
                    synchronized(io.element.android.libraries.push.api.PushAccountPolicyLock.monitor) {
                        accountPreferences.edit().putBoolean("logged-out", false).putString("social-actor", candidate.matrixUserId)
                            .remove("logged-out-actor").commit()
                        if (previousActor != null && previousActor != candidate.matrixUserId) {
                            androidx.core.app.NotificationManagerCompat.from(context).cancelAll()
                        }
                    }
                    if (previousActor != null && previousActor != candidate.matrixUserId) {
                        retainAdmission = false
                        busy = true
                        session = null
                        chatsRequested = false
                        navigationSequence++
                        navigationEpoch.advance()
                        navigationSnackbar.currentSnackbarData?.dismiss()
                        directFailed = false
                        externalLinkFailed = false
                        directJob?.cancel()
                        webController.destroy()
                        webController.resetRouting()
                        webController.currentUrl = null
                        shellState.eventSink(AntintwrkShellEvent.Section(AntintwrkSection.Feed))
                        shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                        pushStore.clear()
                        AntintwrkIncomingShares.discardOtherActor(candidate.matrixUserId)
                        AntintwrkSocialPushRoutes.consume()
                        AntintwrkSocialProfileRoutes.consume()
                        AntintwrkProfileNavigation.clear()
                    }
                    AntintwrkMessengerRoutes.discardOtherActor(candidate.matrixUserId)
                    routing.prepare(AntintwrkNativeAuthClient(webController.authOrigin), candidate, webController)
                    checkCurrent()
                }
                session = candidate
                webController.ownProfileIdentity = candidate.profileIdentity
                if (candidate.stage != AntintwrkAuthStage.AUTHENTICATED) {
                    pushStore.clear()
                    pushOpen = false
                    webController.destroy()
                    webController.resetRouting()
                }
                if (candidate.stage == AntintwrkAuthStage.DEVICE) client.startDevice(candidate.csrf)
            } catch (required: AntintwrkRegistrationRequired) {
                checkCurrent()
                googleRegistration = required.ticket
                state.eventSink(AntintwrkWelcomeEvent.Open(AntintwrkPage.Profile))
                state.eventSink(AntintwrkWelcomeEvent.Email(required.email))
                state.eventSink(AntintwrkWelcomeEvent.FirstName(required.firstName))
                state.eventSink(AntintwrkWelcomeEvent.Surname(required.surname))
            } catch (error: AntintwrkRequestError) {
                checkCurrent()
                retainAdmission = retainAdmission && antintwrkRetainAdmission(revalidate, true, error.code)
                serverMessage = when (error.code) {
                    "PROFILE" -> profileError
                    "REGISTRATION_EXPIRED" -> expiredError
                    "SESSION_EXPIRED" -> sessionExpiredError
                    "RATE_LIMITED" -> rateLimitError
                    else -> ""
                }
                failed = true
            } catch (error: AntintwrkAccountError) {
                checkCurrent()
                retainAdmission = false
                serverMessage = error.messageForUser
                failed = true
            } catch (_: GetCredentialCancellationException) {
                // Dismissing the system picker never admits a session.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                checkCurrent()
                failed = true
                // An uncertain request never admits a user to the shell.
                if (!retainAdmission && session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
                    session = null
                    webController.destroy()
                }
            } finally {
                if (operation == admissionEpoch.current) {
                admissionPending = null
                if (failed && !retainAdmission && session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
                    session = null
                    webController.destroy()
                }
                backgroundValidationPending = revalidate && failed && retainAdmission
                sessionVerificationFailed = revalidate && failed && !retainAdmission
                busy = false
                code = ""
                }
            }
        }
        LaunchedEffect(sessionVerificationFailed, backgroundValidationPending, foreground) {
            while ((sessionVerificationFailed || backgroundValidationPending) && foreground && !accountPreferences.getBoolean("logged-out", false)) {
                delay(15_000)
                runRequest(revalidate = true) { readAdmission() }
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
        LaunchedEffect(session?.stage, busy, webController.refreshVersion, webController.currentUrl, webController.origin, webController.routedSession?.csrf) {
            if (session?.stage == AntintwrkAuthStage.AUTHENTICATED && !busy) {
                while (true) {
                    delay((10_000L - (android.os.SystemClock.elapsedRealtime() - lastStatusAt)).coerceAtLeast(0L))
                    lastStatusAt = android.os.SystemClock.elapsedRealtime()
                    try {
                        val status = socialClient.status((webController.routedSession ?: session!!).csrf)
                        webController.notifications = status.notifications
                        webController.ownProfileIdentity = status.profileIdentity
                        webController.profileName = status.name
                        if (status.avatar != webController.profileAvatarSource) {
                            val avatar = status.avatar?.let { socialClient.avatar(it) }
                            if (avatar != null || status.avatar == null) {
                                webController.profileAvatar = avatar
                                webController.profileAvatarSource = status.avatar
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: AntintwrkRequestError) {
                        if (error.code in setOf("AUTH_REQUIRED", "SESSION_EXPIRED")) {
                            runRequest(revalidate = true) { readAdmission() }
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
                scope.launch { runRequest(revalidate = true) { readAdmission() } }
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            foreground = false
            navigationEpoch.advance()
            navigationSequence++
            chatsRequested = false
            directJob?.cancel()
            directBusy = false
            directFailed = false
            externalLinkFailed = false
            navigationSnackbar.currentSnackbarData?.dismiss()
        }
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
                    val policy = AntintwrkWebPolicy(webController.origin)
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
                webController.navigate(AntintwrkWebPolicy(webController.origin).route(language, "notifications"))
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
                scope.launch { runRequest(revalidate = true) { readAdmission() } }
            }
        )
        val signOutRetry = stringResource(R.string.screen_ant_recovery_retry)
        fun signOut() {
                admissionEpoch.advance()
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
                scope.launch {
                try {
                    val stored = withTimeout(10_000) { current.matrixUserId?.let { messengerBindings.sessionStore().getSession(it) } }
                    if (stored?.isTokenValid == true) {
                        // Bind the matching SDK session before logout; never leave its live token behind.
                        val switchAccount = requireNotNull(onSwitchMatrixAccount)
                        AntintwrkAppNavigation.open(stored.userId, AntintwrkAppDestination.SignOut)
                        switchAccount(stored.userId)
                        return@launch
                    }
                } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                    navigationSnackbar.showSnackbar(signOutRetry, withDismissAction = true)
                    return@launch
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    navigationSnackbar.showSnackbar(signOutRetry, withDismissAction = true)
                    return@launch
                }
                val pushFid = pushStore.fid
                busy = true
                webController.destroy()
                val cookieSnapshot = client.clearLocalSession()
                AntintwrkMessengerRoutes.clear()
                AntintwrkAppNavigation.clear()
                accountPreferences.edit().putBoolean("logged-out", true).putString("logged-out-actor", current.matrixUserId)
                    .remove("social-actor").remove("connect-messenger-for").commit()
                AntintwrkIncomingShares.clear()
                AntintwrkSocialPushRoutes.consume()
                AntintwrkSocialProfileRoutes.consume()
                AntintwrkProfileNavigation.clear()
                pushStore.clear()
                pushOpen = false
                // Hide content immediately, even if the revocation request fails.
                shellState.eventSink(AntintwrkShellEvent.Section(AntintwrkSection.Feed))
                session = null
                webController.destroy()
                webController.resetRouting()
                webController.authOrigin = AntintwrkSocialOrigins.RUSSIAN
                scope.launch {
                    try {
                        withTimeout(10_000) {
                        if (pushFid.isNotEmpty()) {
                            runCatching {
                            withTimeout(
                                5_000
                            ) { client.push("push-unsubscribe", current.csrf, mapOf("fid" to pushFid), cookieSnapshot) }
                        }
                        }
                        client.logout(current.csrf, cookieSnapshot)
                        }
                    } catch (cancelled: CancellationException) {
                        if (cancelled !is TimeoutCancellationException) throw cancelled
                    } catch (_: Exception) {
                        // The old server session cannot restore cookies or admission.
                    } finally {
                        if (accountPreferences.getBoolean("logged-out", false)) runCatching { credentials.clear() }
                    }
                }
                runRequest { AntintwrkNativeAuthClient(webController.authOrigin).session() }
                }
                }
        }
        val appRequest by AntintwrkAppNavigation.requests.collectAsState()
        LaunchedEffect(appRequest, session?.stage, actor, matrixUserId) {
            val request = appRequest ?: return@LaunchedEffect
            if (session?.stage != AntintwrkAuthStage.AUTHENTICATED) return@LaunchedEffect
            if (request.actor != actor) {
                AntintwrkAppNavigation.consume(request)
                return@LaunchedEffect
            }
            if (request.destination == AntintwrkAppDestination.SignOut && matrixUserId != actor) return@LaunchedEffect
            AntintwrkAppNavigation.consume(request)
            when (request.destination) {
                AntintwrkAppDestination.Settings -> settingsOpen = true
                AntintwrkAppDestination.Support -> {
                    settingsOpen = false
                    shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                    webController.openFromMessenger(AntintwrkWebPolicy(webController.origin).route(language, "support"))
                }
                AntintwrkAppDestination.SignOut -> signOut()
            }
        }
        if (settingsOpen) {
            AntintwrkAppSettings(
                onDismiss = { settingsOpen = false },
                messengerSettingsReady = matrixUserId == actor && onOpenAppPreference != null,
                failed = settingsFailed,
                socialOrigin = webController.origin,
                onSignOut = {
                    settingsOpen = false
                    signOut()
                },
                onRoute = { path ->
                    settingsOpen = false
                    when (path) {
                        "native-push" -> pushOpen = true
                        "native-licences" -> licencesOpen = true
                        "native-media", "native-screen-lock", "native-location", "native-messenger-notifications", "native-moderation" -> {
                            val target = requireNotNull(antintwrkAppPreference(path))
                            settingsFailed = false
                            scope.launch {
                                try {
                                    withTimeout(6_000) { requireNotNull(onOpenAppPreference).invoke(target) }
                                } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                                    settingsFailed = true
                                    settingsOpen = true
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    settingsFailed = true
                                    settingsOpen = true
                                }
                            }
                        }
                        else -> {
                            val url = AntintwrkWebPolicy(webController.origin).route(language, path)
                            if (shellState.mode == AntintwrkMode.Messenger) webController.openFromMessenger(url) else webController.navigate(url)
                            shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                        }
                    }
                }
            )
        }
        val externalUriHandler = LocalUriHandler.current
        @Composable
        fun RenderMessenger(contentModifier: Modifier) {
            CompositionLocalProvider(LocalEmbeddedLinkHandler provides { url ->
                val policy = AntintwrkWebPolicy(webController.origin)
                val canonical = policy.canonical(url)
                val embedded = policy.embedded(canonical)
                if (policy.sameOrigin(canonical) && policy.outbound(canonical)) {
                    val epoch = navigationEpoch.advance()
                    directFailed = false
                    externalLinkFailed = false
                    navigationSnackbar.currentSnackbarData?.dismiss()
                    scope.launch {
                        try {
                            val resolved = antintwrkRetryNavigationRead({ navigationEpoch.accepts(epoch, foreground) }) {
                                antintwrkExternalRedirect(canonical, policy)
                            }
                            if (navigationEpoch.accepts(epoch, foreground)) externalUriHandler.openUri(resolved)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            if (navigationEpoch.accepts(epoch, foreground)) externalLinkFailed = true
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
                    scope.launch { runRequest(revalidate = true) { readAdmission() } }
                }
            }
        } else if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
            AntintwrkShellView(
                shellState,
                onSelectMode = ::selectMode,
                navigationLoading = chatsRequested,
                messengerInteractive = !directBusy && messageRequest == null,
                messengerUnread = if (matrixUserId == session?.matrixUserId) unreadMessages else 0L,
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
                scope.launch { runRequest(revalidate = true) { readAdmission() } }
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
                    scope.launch {
                        runRequest {
                        if (accountPreferences.getBoolean("logged-out", false)) client.clearLocalSession()
                        client.login(email, password, language, client.session().csrf)
                    }
                    }
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
                    scope.launch {
                        runRequest {
                        if (accountPreferences.getBoolean("logged-out", false)) client.clearLocalSession()
                        credentials.signIn(context, client, provider, (session ?: client.session()).csrf)
                    }
                    }
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
                onSelectMode = ::selectMode,
                navigationLoading = chatsRequested,
                messengerInteractive = !directBusy && messageRequest == null,
                onOpenSettings = { settingsOpen = true },
                socialContent = { SocialAdmission(it) },
                messengerContent = { RenderMessenger(it) }
            )
        } else {
            SocialAdmission(modifier)
        }
        if (licencesOpen) {
            AntintwrkLicencesDialog(
            onOpenAuthor = if (session?.stage == AntintwrkAuthStage.AUTHENTICATED) {
                {
                    licencesOpen = false
                    settingsOpen = false
                    shellState.eventSink(AntintwrkShellEvent.Mode(AntintwrkMode.Social))
                    webController.navigate(AntintwrkWebPolicy(webController.origin).route(language, "hero/2266620"))
                }
            } else {
                null
            },
            onDismiss = { licencesOpen = false },
        )
        }
        if (directBusy) {
            Dialog(onDismissRequest = {
                directJob?.cancel()
                AntintwrkMessengerRoutes.clear()
                directBusy = false
            }) {
            AntintwrkLoadingView()
        }
        }
        val navigationError = stringResource(io.element.android.libraries.ui.strings.R.string.screen_ant_account_messages_retry)
        val externalError = stringResource(R.string.screen_ant_auth_request_error)
        val retryLabel = stringResource(R.string.screen_ant_web_retry)
        LaunchedEffect(directFailed, externalLinkFailed, foreground) {
            if (foreground && (directFailed || externalLinkFailed)) {
                val pending = messageRequest
                val epoch = navigationEpoch.current
                val result = navigationSnackbar.showSnackbar(
                    message = if (directFailed) navigationError else externalError,
                    actionLabel = if (directFailed && pending != null) retryLabel else null,
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
                if (navigationEpoch.accepts(epoch, foreground)) {
                    directFailed = false
                    externalLinkFailed = false
                    if (result == SnackbarResult.ActionPerformed && pending != null) {
                        directAttempt++
                    } else if (pending != null) {
                        AntintwrkMessengerRoutes.consume(pending)
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AntintwrkBrandTheme {
                SnackbarHost(
                    navigationSnackbar,
                    Modifier.imePadding().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 80.dp)
                )
            }
        }
    }
