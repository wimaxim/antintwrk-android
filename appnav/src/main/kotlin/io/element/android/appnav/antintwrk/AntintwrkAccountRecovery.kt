// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.encryption.BackupUploadState
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** Never reset an existing identity. Escrow must be durable before SDK key creation. */
internal class AntintwrkAccountRecovery(
    private val encryption: EncryptionService,
    private val actor: String,
    client: AntintwrkNativeAuthClient = AntintwrkNativeAuthClient(),
    private val admission: suspend () -> AntintwrkAuthSession = { client.session() },
    private val secretStore: suspend (String, String) -> String? = { operation, csrf -> client.recoverySecret(actor, operation, csrf) },
) {
    private val mutex = Mutex()

    suspend fun prepare(): Boolean = mutex.withLock {
        withTimeout(120_000) {
            val admission = admission()
            require(admission.stage == AntintwrkAuthStage.AUTHENTICATED && admission.matrixUserId == actor)
            val state = encryption.recoveryStateStateFlow.first { it != RecoveryState.WAITING_FOR_SYNC && it != RecoveryState.UNKNOWN }
            var secret = secretStore("read", admission.csrf)
            if (secret == null) {
                // A previously configured recovery key must be imported by its owner,
                // never replaced just because our new custody store has no record.
                if (state != RecoveryState.DISABLED || encryption.doesBackupExistOnServer().getOrThrow()) return@withTimeout false
                secret = requireNotNull(secretStore("create", admission.csrf))
                // Explicit passphrase was stored before encryption setup. A crash cannot lose it.
            }
            if (state == RecoveryState.DISABLED && !encryption.doesBackupExistOnServer().getOrThrow()) {
                encryption.enableRecovery(waitForBackupsToUpload = true, passphrase = secret).getOrThrow()
            }
            encryption.recover(secret).getOrThrow()
            require(secretStore("read", admission.csrf) == secret)
            encryption.enableBackups().getOrThrow()
            val upload = encryption.waitForBackupUploadSteadyState().first {
                it == BackupUploadState.Done || it == BackupUploadState.Error || it is BackupUploadState.SteadyException
            }
            require(upload == BackupUploadState.Done)
            true
        }
    }
}
