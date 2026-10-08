// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.impl.encryption

import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.encryption.BackupUploadState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.matrix.rustcomponents.sdk.BackupSteadyStateListener
import org.matrix.rustcomponents.sdk.BackupUploadState as RustBackupUploadState
import org.matrix.rustcomponents.sdk.SteadyStateException as RustSteadyStateException

/** FFI success is authoritative even if its progress task is aborted before delivering Done. */
internal fun backupUploadFlow(waitForUpload: suspend (BackupSteadyStateListener) -> Unit): Flow<BackupUploadState> = callbackFlow {
    val progressMapper = BackupUploadStateMapper()
    runCatchingExceptions {
        waitForUpload(object : BackupSteadyStateListener {
            override fun onUpdate(status: RustBackupUploadState) {
                // Only the awaited SDK result can confirm success, even if progress reports Done early.
                if (status != RustBackupUploadState.Done) trySend(progressMapper.map(status))
            }
        })
    }.onSuccess {
        trySend(BackupUploadState.Done)
        close()
    }.onFailure {
        if (it is RustSteadyStateException) {
            trySend(BackupUploadState.SteadyException(SteadyStateExceptionMapper().map(it)))
        } else {
            trySend(BackupUploadState.Error)
        }
        close()
    }
    awaitClose {}
}
