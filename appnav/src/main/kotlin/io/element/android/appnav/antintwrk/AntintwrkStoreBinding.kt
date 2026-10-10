// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.webkit.CookieManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.element.android.appnav.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun AntintwrkStoreBinding(client: AntintwrkNativeAuthClient, session: AntintwrkAuthSession?, controller: AntintwrkWebFeedController) {
    val store = LocalAntintwrkStore.current
    val context = LocalContext.current
    val current = rememberUpdatedState(session)
    val coordinator = remember(store, session?.matrixUserId, session?.csrf, session?.stage) {
        if (store != null && session?.stage == AntintwrkAuthStage.AUTHENTICATED && session.matrixUserId != null) {
            AntintwrkStoreCoordinator(
                store,
                client,
                session,
                CookieManager.getInstance().getCookie(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN).orEmpty(),
                context.packageName
            )
        } else {
            null
        }
    }
    val scope = rememberCoroutineScope()
    val actor = session?.matrixUserId
    fun updated() {
        if (current.value?.stage == AntintwrkAuthStage.AUTHENTICATED && current.value?.matrixUserId == actor) {
            val page = controller.view ?: return
            if (AntintwrkWebPolicy(BuildConfig.ANTINTWRK_SOCIAL_ORIGIN).embedded(page.url.orEmpty()) != null) {
                page.evaluateJavascript("window.dispatchEvent(new Event('antintwrk-store-updated'))", null)
            }
        }
    }
    controller.onStoreRequest = coordinator?.let { bound ->
        { json, reply ->
            scope.launch {
                try {
                    check(current.value?.matrixUserId == actor && current.value?.stage == AntintwrkAuthStage.AUTHENTICATED)
                    val result = when (json.optString("type")) {
                        "store-catalog" -> bound.catalog()
                        "store-purchase" -> bound.purchase(json)
                        "store-restore" -> JSONObject().put("ok", true).also { if (bound.restore()) updated() }
                        else -> error("Unavailable")
                    }
                    if (current.value?.matrixUserId == actor) reply(result)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: AntintwrkStoreError) {
                    if (current.value?.matrixUserId == actor) reply(JSONObject().put("ok", false).put("error", error.code))
                } catch (_: Exception) {
                    if (current.value?.matrixUserId == actor) reply(JSONObject().put("ok", false).put("error", "STORE_UNAVAILABLE"))
                }
            }
        }
    }
    LaunchedEffect(coordinator) {
        if (coordinator == null || store == null) return@LaunchedEffect
        launch {
            store.updates.collect { candidates ->
                for (candidate in candidates) {
                    try {
                        if (coordinator.verify(candidate)) updated()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Unacknowledged ownership remains in Play; foreground restore retries safely.
                    }
                }
            }
        }
        store.connection.collect { state ->
            if (state == AntintwrkStoreConnection.Ready) {
                try {
                    if (coordinator.restore()) updated()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Store/server availability never resets social content or messenger admission.
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (coordinator != null && store?.connection?.value == AntintwrkStoreConnection.Ready) {
            scope.launch {
                try {
                    if (coordinator.restore()) updated()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A previous unverified purchase is retried after foreground/network recovery.
                }
            }
        }
    }
}
