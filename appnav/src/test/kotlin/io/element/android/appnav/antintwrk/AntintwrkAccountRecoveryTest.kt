// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.BackupUploadState
import io.element.android.libraries.matrix.api.encryption.RecoveryException
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import io.element.android.libraries.matrix.test.encryption.FakeEncryptionService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkAccountRecoveryTest {
    private val actor = "@u123:matrix.antimsg.com"
    private val admitted = AntintwrkAuthSession(AntintwrkAuthStage.AUTHENTICATED, "csrf", matrixUserId = actor)

    @Test
    fun `initialization finishes before escrow creation or SDK setup`() = runTest {
        var initialized = false
        lateinit var encryption: FakeEncryptionService
        encryption = FakeEncryptionService(enableRecoveryLambda = { _, _ ->
            assertThat(initialized).isTrue()
            encryption.recoveryStateStateFlow.value = RecoveryState.ENABLED
            Result.success("unused")
        }).apply {
            recoveryStateStateFlow.value = RecoveryState.DISABLED
            initialization = {
                initialized = true
                Result.success(Unit)
            }
            givenDoesBackupExistOnServerResult(Result.success(false))
            givenWaitForBackupUploadSteadyStateFlow(flowOf(BackupUploadState.Done))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ ->
            assertThat(initialized).isTrue()
            "synthetic-secret"
        })
        assertThat(recovery.prepare()).isTrue()
    }

    @Test
    fun `initialization failure never writes escrow or creates recovery`() = runTest {
        val encryption = FakeEncryptionService().apply {
            initialization = { Result.failure(IllegalStateException("synthetic")) }
        }
        var contacted = false
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ ->
            contacted = true
            null
        })
        assertThat(runCatching { recovery.prepare() }.isFailure).isTrue()
        assertThat(contacted).isFalse()
    }

    @Test
    fun `existing unknown recovery is never reset`() = runTest {
        val encryption = FakeEncryptionService().apply { recoveryStateStateFlow.value = RecoveryState.ENABLED }
        var operations = 0
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ ->
            operations++
            null
        })
        assertThat(recovery.prepare()).isFalse()
        assertThat(operations).isEqualTo(1)
    }

    @Test
    fun `another social actor cannot access recovery`() = runTest {
        var contacted = false
        val recovery = AntintwrkAccountRecovery(
            FakeEncryptionService(),
            actor,
            admission = { admitted.copy(matrixUserId = "@u456:matrix.antimsg.com") },
            secretStore = { _, _ ->
                contacted = true
                null
            },
        )
        assertThat(runCatching { recovery.prepare() }.isFailure).isTrue()
        assertThat(contacted).isFalse()
    }

    @Test
    fun `escrow is durable before creation and crash retry uses same secret`() = runTest {
        var secret: String? = null
        var created = 0
        lateinit var encryption: FakeEncryptionService
        encryption = FakeEncryptionService(enableRecoveryLambda = { wait, passphrase ->
            assertThat(wait).isTrue()
            assertThat(passphrase).isEqualTo(secret)
            assertThat(secret).isNotNull()
            created++
            encryption.recoveryStateStateFlow.value = RecoveryState.ENABLED
            Result.success("unused-sdk-return")
        }).apply {
            recoveryStateStateFlow.value = RecoveryState.DISABLED
            givenDoesBackupExistOnServerResult(Result.success(false))
            givenWaitForBackupUploadSteadyStateFlow(flowOf(BackupUploadState.Done))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { operation, _ ->
            if (operation == "create") secret = "synthetic-secret"
            secret
        })
        assertThat(recovery.prepare()).isTrue()
        assertThat(created).isEqualTo(1)
        encryption.recoveryStateStateFlow.value = RecoveryState.ENABLED
        assertThat(recovery.prepare()).isTrue()
        assertThat(created).isEqualTo(1)
    }

    @Test
    fun `failed upload does not permit simplified logout`() = runTest {
        val encryption = FakeEncryptionService().apply {
            recoveryStateStateFlow.value = RecoveryState.ENABLED
            givenWaitForBackupUploadSteadyStateFlow(flowOf(BackupUploadState.Error))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ -> "synthetic-secret" })
        assertThat(runCatching { recovery.prepare() }.isFailure).isTrue()
    }

    @Test
    fun `SDK automatic backup is reused only when this device owns it`() = runTest {
        var enabled = false
        lateinit var encryption: FakeEncryptionService
        encryption = FakeEncryptionService(enableRecoveryLambda = { _, _ ->
            encryption.recoveryStateStateFlow.value = RecoveryState.ENABLED
            enabled = true
            Result.success("unused")
        }).apply {
            recoveryStateStateFlow.value = RecoveryState.DISABLED
            backupStateStateFlow.value = BackupState.ENABLED
            givenDoesBackupExistOnServerResult(Result.success(true))
            givenWaitForBackupUploadSteadyStateFlow(flowOf(BackupUploadState.Done))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ -> "synthetic-secret" })
        assertThat(recovery.prepare()).isTrue()
        assertThat(enabled).isTrue()
    }

    @Test
    fun `an existing backup without local keys is never replaced`() = runTest {
        var wrote = false
        val encryption = FakeEncryptionService().apply {
            recoveryStateStateFlow.value = RecoveryState.DISABLED
            givenDoesBackupExistOnServerResult(Result.success(true))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { operation, _ ->
            if (operation == "create") wrote = true
            null
        })
        assertThat(recovery.prepare()).isFalse()
        assertThat(wrote).isFalse()
    }

    @Test
    fun `already enabled backup does not fail on non idempotent SDK enable`() = runTest {
        val encryption = FakeEncryptionService().apply {
            recoveryStateStateFlow.value = RecoveryState.ENABLED
            backupStateStateFlow.value = BackupState.ENABLED
            givenEnableBackupsFailure(RecoveryException.BackupExistsOnServer)
            givenWaitForBackupUploadSteadyStateFlow(flowOf(BackupUploadState.Done))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ -> "synthetic-secret" })
        assertThat(recovery.prepare()).isTrue()
    }

    @Test
    fun `existing server backup without local enable confirmation prevents logout`() = runTest {
        val encryption = FakeEncryptionService().apply {
            recoveryStateStateFlow.value = RecoveryState.ENABLED
            backupStateStateFlow.value = BackupState.UNKNOWN
            givenEnableBackupsFailure(RecoveryException.BackupExistsOnServer)
            givenWaitForBackupUploadSteadyStateFlow(flowOf(BackupUploadState.Done))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ -> "synthetic-secret" })
        assertThat(runCatching { recovery.prepare() }.isFailure).isTrue()
    }
    @Test
    fun `SDK success without enabled recovery cannot permit simplified logout`() = runTest {
        val encryption = FakeEncryptionService().apply {
            recoveryStateStateFlow.value = RecoveryState.INCOMPLETE
            backupStateStateFlow.value = BackupState.ENABLED
            givenWaitForBackupUploadSteadyStateFlow(flowOf(BackupUploadState.Done))
        }
        val recovery = AntintwrkAccountRecovery(encryption, actor, admission = { admitted }, secretStore = { _, _ -> "synthetic-secret" })
        assertThat(runCatching { recovery.prepare() }.isFailure).isTrue()
    }
}
