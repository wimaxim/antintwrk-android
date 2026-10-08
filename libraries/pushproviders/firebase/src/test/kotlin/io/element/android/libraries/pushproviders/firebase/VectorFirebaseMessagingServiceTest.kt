/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package io.element.android.libraries.pushproviders.firebase

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import io.element.android.libraries.matrix.api.BuildConfig
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_SECRET
import io.element.android.libraries.push.api.SocialPushReceiver
import io.element.android.libraries.push.test.push.FakeFetchPushForegroundServiceManager
import io.element.android.libraries.push.test.test.FakePushHandler
import io.element.android.libraries.pushproviders.api.PushData
import io.element.android.libraries.pushproviders.api.PushHandler
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.lambda.value
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VectorFirebaseMessagingServiceTest : RobolectricTest() {
    @Test
    fun `test receiving invalid data`() = runTest {
        val lambda = lambdaRecorder<String, String, Unit> { _, _ -> }
        val vectorFirebaseMessagingService = createVectorFirebaseMessagingService(
            pushHandler = FakePushHandler(handleInvalidResult = lambda)
        )
        vectorFirebaseMessagingService.onMessageReceived(
            message = RemoteMessage(
                Bundle().apply {
                    putString("a", "A")
                    putString("b", "B")
                }
            )
        )
        runCurrent()
        lambda.assertions().isCalledOnce()
            .with(
                value(FirebaseConfig.NAME),
                value(if (BuildConfig.ANTINTWRK_NATIVE_AUTH) "Firebase payload omitted" else "a: A\nb: B"),
            )
    }

    @Test
    fun `test receiving valid data`() = runTest {
        val lambda = lambdaRecorder<PushData, String, Boolean> { _, _ -> true }
        val vectorFirebaseMessagingService = createVectorFirebaseMessagingService(
            pushHandler = FakePushHandler(handleResult = lambda)
        )
        vectorFirebaseMessagingService.onMessageReceived(
            message = RemoteMessage(
                Bundle().apply {
                    putString("event_id", AN_EVENT_ID.value)
                    putString("room_id", A_ROOM_ID.value)
                    putString("cs", A_SECRET)
                },
            )
        )
        advanceUntilIdle()
        lambda.assertions()
            .isCalledOnce()
            .with(
                value(PushData(AN_EVENT_ID, A_ROOM_ID, null, A_SECRET)),
                value(FirebaseConfig.NAME)
            )
    }

    @Test
    fun `test pushHandler returning true locks and does not unlock the wakelock so it continues running`() = runTest {
        val lockLambda = lambdaRecorder<Boolean> { true }
        val unlockLambda = lambdaRecorder<Boolean> { true }
        val vectorFirebaseMessagingService = createVectorFirebaseMessagingService(
            pushHandler = FakePushHandler(handleResult = { _, _ -> true }),
            pushHandlingWakeLock = FakeFetchPushForegroundServiceManager(
                lock = lockLambda,
                unlock = unlockLambda
            )
        )
        vectorFirebaseMessagingService.onMessageReceived(
            message = RemoteMessage(
                Bundle().apply {
                    putString("event_id", AN_EVENT_ID.value)
                    putString("room_id", A_ROOM_ID.value)
                    putString("cs", A_SECRET)
                    putString("google.priority", "high")
                },
            )
        )

        // The wakelock should be locked but not unlocked
        lockLambda.assertions().isCalledOnce()
        unlockLambda.assertions().isNeverCalled()

        advanceUntilIdle()

        // After handling the push, the wakelock should still not be unlocked
        unlockLambda.assertions().isNeverCalled()
    }

    @Test
    fun `test pushHandler returning false locks and unlocks the wakelock early`() = runTest {
        val lockLambda = lambdaRecorder<Boolean> { true }
        val unlockLambda = lambdaRecorder<Boolean> { true }
        val vectorFirebaseMessagingService = createVectorFirebaseMessagingService(
            pushHandler = FakePushHandler(handleResult = { _, _ -> false }),
            pushHandlingWakeLock = FakeFetchPushForegroundServiceManager(
                lock = lockLambda,
                unlock = unlockLambda
            )
        )
        vectorFirebaseMessagingService.onMessageReceived(
            message = RemoteMessage(
                Bundle().apply {
                    putString("event_id", AN_EVENT_ID.value)
                    putString("room_id", A_ROOM_ID.value)
                    putString("cs", A_SECRET)
                    putString("google.priority", "high")
                },
            )
        )

        // The wakelock should be locked but not unlocked
        lockLambda.assertions().isCalledOnce()
        unlockLambda.assertions().isNeverCalled()

        advanceUntilIdle()

        // After handling the push, the wakelock should be unlocked
        unlockLambda.assertions().isCalledOnce()
    }

    @Test
    fun `test pushHandler with a remote message with normal priority won't lock the wakelock`() = runTest {
        val lockLambda = lambdaRecorder<Boolean> { true }
        val unlockLambda = lambdaRecorder<Boolean> { true }
        val vectorFirebaseMessagingService = createVectorFirebaseMessagingService(
            pushHandler = FakePushHandler(handleResult = { _, _ -> false }),
            pushHandlingWakeLock = FakeFetchPushForegroundServiceManager(
                lock = lockLambda,
                unlock = unlockLambda
            )
        )
        vectorFirebaseMessagingService.onMessageReceived(
            message = RemoteMessage(
                Bundle().apply {
                    putString("event_id", AN_EVENT_ID.value)
                    putString("room_id", A_ROOM_ID.value)
                    putString("cs", A_SECRET)
                    putString("google.priority", "normal")
                },
            )
        )

        // The wakelock should not be locked
        lockLambda.assertions().isNeverCalled()
        unlockLambda.assertions().isNeverCalled()
    }

    @Test
    fun `test new token is forwarded to the handler`() = runTest {
        val lambda = lambdaRecorder<String, Unit> { }
        val vectorFirebaseMessagingService = createVectorFirebaseMessagingService(
            firebaseNewInstallationIdHandler = FakeFirebaseNewInstallationIdHandler(handleResult = lambda)
        )
        vectorFirebaseMessagingService.onRegistered("installationId")
        advanceUntilIdle()
        lambda.assertions()
            .isCalledOnce()
            .with(value("installationId"))
    }

    private fun TestScope.createVectorFirebaseMessagingService(
        firebaseNewInstallationIdHandler: FirebaseNewInstallationIdHandler = FakeFirebaseNewInstallationIdHandler(),
        pushHandler: PushHandler = FakePushHandler(),
        pushHandlingWakeLock: FakeFetchPushForegroundServiceManager = FakeFetchPushForegroundServiceManager(),
        socialReceiver: SocialPushReceiver = object : SocialPushReceiver {
            override suspend fun registered(installationId: String) = Unit
            override fun receive(data: Map<String, String>) = false
        },
    ): VectorFirebaseMessagingService {
        return VectorFirebaseMessagingService().apply {
            this.firebaseNewInstallationIdHandler = firebaseNewInstallationIdHandler
            this.pushParser = FirebasePushParser()
            this.pushHandler = pushHandler
            this.socialPushReceiver = socialReceiver
            this.coroutineScope = this@createVectorFirebaseMessagingService
            this.fetchPushForegroundServiceManager = pushHandlingWakeLock
        }
    }

    @Test
    fun `social signals do not enter Matrix parsing or acquire its wakelock`() = runTest {
        val invalid = lambdaRecorder<String, String, Unit> { _, _ -> }
        val lock = lambdaRecorder<Boolean> { true }
        val service = createVectorFirebaseMessagingService(
            pushHandler = FakePushHandler(handleInvalidResult = invalid),
            pushHandlingWakeLock = FakeFetchPushForegroundServiceManager(lock = lock),
            socialReceiver = object : SocialPushReceiver {
                override suspend fun registered(installationId: String) = Unit
                override fun receive(data: Map<String, String>) = data["kind"] == "antintwrk.social.v1"
            },
        )
        service.onMessageReceived(RemoteMessage(Bundle().apply {
            putString("kind", "antintwrk.social.v1")
            putString("google.priority", "high")
        }))
        advanceUntilIdle()
        invalid.assertions().isNeverCalled()
        lock.assertions().isNeverCalled()
    }

    @Test
    fun `slow social registration does not delay Matrix installation update`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val matrix = lambdaRecorder<String, Unit> { }
        val service = createVectorFirebaseMessagingService(
            firebaseNewInstallationIdHandler = FakeFirebaseNewInstallationIdHandler(handleResult = matrix),
            socialReceiver = object : SocialPushReceiver {
                override suspend fun registered(installationId: String) {
                    gate.await()
                }
                override fun receive(data: Map<String, String>) = false
            },
        )
        service.onRegistered("installationId")
        runCurrent()
        matrix.assertions().isCalledOnce().with(value("installationId"))
        gate.complete(Unit)
        advanceUntilIdle()
    }
}
