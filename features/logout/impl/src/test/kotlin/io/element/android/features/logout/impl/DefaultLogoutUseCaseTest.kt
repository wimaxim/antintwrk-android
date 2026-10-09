/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.features.logout.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.features.logout.api.ManagedLogoutGuard
import io.element.android.libraries.matrix.api.BuildConfig
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.FakeMatrixClientProvider
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.lambda.value
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Test

class DefaultLogoutUseCaseTest {
    @Test
    fun `test logout from one session`() = runTest {
        val logoutLambda1 = lambdaRecorder<Boolean, Boolean, Unit> { _, _ -> }
        val client1 = FakeMatrixClient(A_USER_ID).apply {
            logoutLambda = logoutLambda1
        }
        val sut = DefaultLogoutUseCase(
            managedLogoutGuard = ManagedLogoutGuard { true },
            sessionStore = InMemorySessionStore(
                initialList = listOf(
                    aSessionData(sessionId = A_USER_ID.value),
                )
            ),
            matrixClientProvider = FakeMatrixClientProvider(
                getClient = { sessionId ->
                    when (sessionId) {
                        A_USER_ID -> Result.success(client1)
                        else -> error("Unexpected sessionId")
                    }
                }
            ),
        )
        sut.logoutAll(ignoreSdkError = true)
        logoutLambda1.assertions().isCalledOnce().with(value(!BuildConfig.ANTINTWRK_NATIVE_AUTH), value(true))
    }

    @Test
    fun `test logout from several sessions`() = runTest {
        val logoutLambda1 = lambdaRecorder<Boolean, Boolean, Unit> { _, _ -> }
        val logoutLambda2 = lambdaRecorder<Boolean, Boolean, Unit> { _, _ -> }
        val client1 = FakeMatrixClient(A_USER_ID).apply {
            logoutLambda = logoutLambda1
        }
        val client2 = FakeMatrixClient(A_USER_ID_2).apply {
            logoutLambda = logoutLambda2
        }
        val sut = DefaultLogoutUseCase(
            managedLogoutGuard = ManagedLogoutGuard { true },
            sessionStore = InMemorySessionStore(
                initialList = listOf(
                    aSessionData(sessionId = A_USER_ID.value),
                    aSessionData(sessionId = A_USER_ID_2.value),
                )
            ),
            matrixClientProvider = FakeMatrixClientProvider(
                getClient = { sessionId ->
                    when (sessionId) {
                        A_USER_ID -> Result.success(client1)
                        A_USER_ID_2 -> Result.success(client2)
                        else -> error("Unexpected sessionId")
                    }
                }
            ),
        )
        sut.logoutAll(ignoreSdkError = true)
        logoutLambda1.assertions().isCalledOnce().with(value(!BuildConfig.ANTINTWRK_NATIVE_AUTH), value(true))
        logoutLambda2.assertions().isCalledOnce().with(value(!BuildConfig.ANTINTWRK_NATIVE_AUTH), value(true))
    }

    @Test
    fun `test logout session not found is ignored`() = runTest {
        val sut = DefaultLogoutUseCase(
            managedLogoutGuard = ManagedLogoutGuard { true },
            sessionStore = InMemorySessionStore(
                initialList = listOf(
                    aSessionData(sessionId = A_USER_ID.value),
                )
            ),
            matrixClientProvider = FakeMatrixClientProvider(
                getClient = { sessionId ->
                    when (sessionId) {
                        A_USER_ID -> Result.failure(Exception("Session not found"))
                        else -> error("Unexpected sessionId")
                    }
                }
            ),
        )
        if (BuildConfig.ANTINTWRK_NATIVE_AUTH) {
            assertThat(runCatching { sut.logoutAll(ignoreSdkError = true) }.exceptionOrNull()).isInstanceOf(Exception::class.java)
        } else {
            sut.logoutAll(ignoreSdkError = true)
        }
    }

    @Test
    fun `test logout no sessions`() = runTest {
        val sut = DefaultLogoutUseCase(
            managedLogoutGuard = ManagedLogoutGuard { true },
            sessionStore = InMemorySessionStore(
                initialList = emptyList()
            ),
            matrixClientProvider = FakeMatrixClientProvider(
                getClient = { sessionId ->
                    when (sessionId) {
                        else -> error("Unexpected sessionId")
                    }
                }
            ),
        )
        sut.logoutAll(ignoreSdkError = true)
        // No error
    }
    @Test
    fun `failed recovery of one account prevents revocation of every account`() = runTest {
        assumeTrue(BuildConfig.ANTINTWRK_NATIVE_AUTH)
        var revocations = 0
        val client = FakeMatrixClient(A_USER_ID).apply { logoutLambda = { _, _ -> revocations++ } }
        val sut = DefaultLogoutUseCase(
            sessionStore = InMemorySessionStore(initialList = listOf(aSessionData(sessionId = A_USER_ID.value), aSessionData(sessionId = A_USER_ID_2.value))),
            matrixClientProvider = FakeMatrixClientProvider(getClient = { Result.success(client) }),
            managedLogoutGuard = ManagedLogoutGuard { it == A_USER_ID.value },
        )
        assertThat(runCatching { sut.logoutAll(ignoreSdkError = true) }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(revocations).isEqualTo(0)
    }
}
