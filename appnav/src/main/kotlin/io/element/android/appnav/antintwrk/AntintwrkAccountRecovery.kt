// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.encryption.AntintwrkRecoveryStatus
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.BackupUploadState
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.encryption.RecoveryException
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/** Never reset an existing identity. Escrow must be durable before SDK key creation. */
internal class AntintwrkAccountRecovery(
    private val encryption: EncryptionService,
    private val actor: String,
    client: AntintwrkNativeAuthClient = AntintwrkNativeAuthClient(),
    private val admission: suspend () -> AntintwrkAuthSession = { client.session() },
    private val secretStore: suspend (String, String) -> String? = { operation, csrf -> client.recoverySecret(actor, operation, csrf) },
) {
    private val mutex = actorLocks.getOrPut(actor) { Mutex() }

    private companion object {
        val actorLocks = ConcurrentHashMap<String, Mutex>()
    }

    suspend fun prepare(): Boolean = mutex.withLock {
        var step = "admission"
        var ready = false
        AntintwrkRecoveryStatus.update(actor, AntintwrkRecoveryStatus.Phase.Working)
        try {
            ready = withTimeout(120_000) {
                val admission = admission()
                require(admission.stage == AntintwrkAuthStage.AUTHENTICATED && admission.matrixUserId == actor)
                step = "initialization"
                encryption.awaitInitialization().getOrThrow()
                step = "state"
                val state = encryption.recoveryStateStateFlow.first { it != RecoveryState.WAITING_FOR_SYNC && it != RecoveryState.UNKNOWN }
                val canSetUp = state == RecoveryState.DISABLED &&
                    (!encryption.doesBackupExistOnServer().getOrThrow() || encryption.backupStateStateFlow.value == BackupState.ENABLED)
                step = "escrow_read"
                var secret = secretStore("read", admission.csrf)
                if (secret == null) {
                    // A previously configured recovery key must be imported by its owner,
                    // never replaced just because our new custody store has no record.
                    if (!canSetUp) return@withTimeout false
                    step = "escrow_create"
                    secret = requireNotNull(secretStore("create", admission.csrf))
                    // Explicit passphrase was stored before encryption setup. A crash cannot lose it.
                }
                if (canSetUp) {
                    step = "sdk_enable"
                    encryption.enableRecovery(waitForBackupsToUpload = true, passphrase = secret).getOrThrow()
                }
                step = "sdk_recover"
                encryption.recover(secret).getOrThrow()
                step = "escrow_verify"
                require(secretStore("read", admission.csrf) == secret)
                step = "backup_enable"
                if (encryption.backupStateStateFlow.value != BackupState.ENABLED) {
                    encryption.enableBackups().getOrElse { failure ->
                        if (failure != RecoveryException.BackupExistsOnServer) throw failure
                        // Recovery may enable the existing backup before its state observer updates.
                        // An existing server backup alone does not prove that this device owns its key.
                        withTimeout(10_000) {
                            encryption.backupStateStateFlow.first { it == BackupState.ENABLED }
                        }
                    }
                }
                step = "backup_upload"
                val upload = encryption.waitForBackupUploadSteadyState().first {
                    it == BackupUploadState.Done || it == BackupUploadState.Error || it is BackupUploadState.SteadyException
                }
                require(upload == BackupUploadState.Done)
                step = "state_confirm"
                withTimeout(10_000) {
                    encryption.recoveryStateStateFlow.first { it == RecoveryState.ENABLED }
                }
                Timber.i("Account recovery ready")
                true
            }
            ready
        } catch (failure: Exception) {
            if (failure !is CancellationException) {
                // Log only a fixed stage and exception type, never exception text or credentials.
                Timber.w("Account recovery failed at %s (%s)", step, failure.javaClass.simpleName)
            }
            throw failure
        } finally {
            AntintwrkRecoveryStatus.update(actor, if (ready) AntintwrkRecoveryStatus.Phase.Ready else AntintwrkRecoveryStatus.Phase.Failed)
        }
    }
}
