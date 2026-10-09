/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(DelicateCoilApi::class)

package io.element.android.appnav

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.bumble.appyx.core.lifecycle.subscribe
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.navmodel.backstack.BackStack
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.annotations.ContributesNode
import io.element.android.appnav.antintwrk.AntintwrkNativeAuthNode
import io.element.android.appnav.antintwrk.AntintwrkShellNode
import io.element.android.appnav.antintwrk.AntintwrkWelcomeNode
import io.element.android.features.login.api.LoginEntryPoint
import io.element.android.features.login.api.LoginParams
import io.element.android.libraries.architecture.BackstackView
import io.element.android.libraries.architecture.BaseFlowNode
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.callback
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.designsystem.utils.ForceOrientationInMobileDevices
import io.element.android.libraries.designsystem.utils.ScreenOrientation
import io.element.android.libraries.matrix.ui.media.ImageLoaderHolder
import io.element.android.services.analytics.api.watchers.AnalyticsColdStartWatcher
import kotlinx.parcelize.Parcelize

@ContributesNode(AppScope::class)
@AssistedInject
class NotLoggedInFlowNode(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val loginEntryPoint: LoginEntryPoint,
    private val imageLoaderHolder: ImageLoaderHolder,
    private val analyticsColdStartWatcher: AnalyticsColdStartWatcher,
) : BaseFlowNode<NotLoggedInFlowNode.NavTarget>(
    backstack = BackStack(
        initialElement = NavTarget.Root,
        savedStateMap = buildContext.savedStateMap
    ),
    buildContext = buildContext,
    plugins = plugins,
) {
    data class Params(
        val loginParams: LoginParams?,
    ) : NodeInputs

    interface Callback : Plugin {
        fun navigateToBugReport()
        fun onDone()
        fun navigateToSocialAccount(userId: String) {}
    }

    private val callback: Callback = callback()
    private val inputs = inputs<Params>()

    override fun onBuilt() {
        super.onBuilt()
        analyticsColdStartWatcher.whenLoggingIn()
        lifecycle.subscribe(
            onResume = {
                SingletonImageLoader.setUnsafe(imageLoaderHolder.get())
            },
        )
    }

    sealed interface NavTarget : Parcelable {
        @Parcelize
        data object Root : NavTarget

        @Parcelize
        data object MatrixLogin : NavTarget
    }

    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node {
        return when (navTarget) {
            NavTarget.Root -> {
                if (BuildConfig.ANTINTWRK_ONBOARDING && inputs.loginParams == null) {
                    if (BuildConfig.ANTINTWRK_NATIVE_AUTH) return AntintwrkNativeAuthNode(buildContext, callback::navigateToSocialAccount)
                    if (BuildConfig.DEBUG && BuildConfig.ANTINTWRK_SHELL_PREVIEW) return AntintwrkShellNode(buildContext)
                    return AntintwrkWelcomeNode(buildContext)
                }
                createMatrixLogin(buildContext)
            }
            NavTarget.MatrixLogin -> createMatrixLogin(buildContext)
        }
    }

    private fun createMatrixLogin(buildContext: BuildContext): Node {
        val callback = object : LoginEntryPoint.Callback {
            override fun navigateToBugReport() {
                callback.navigateToBugReport()
            }

            override fun onDone() {
                callback.onDone()
            }
        }
        return loginEntryPoint.createNode(
            parentNode = this,
            buildContext = buildContext,
            params = LoginEntryPoint.Params(
                accountProvider = inputs.loginParams?.accountProvider,
                loginHint = inputs.loginParams?.loginHint,
            ),
            callback = callback,
        )
    }

    @Composable
    override fun View(modifier: Modifier) {
        // The login flow doesn't support landscape mode on mobile devices yet
        ForceOrientationInMobileDevices(orientation = ScreenOrientation.PORTRAIT)

        BackstackView()
    }
}
