// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.impl.encryption

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.encryption.BackupUploadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.rustcomponents.sdk.BackupUploadState as RustBackupUploadState

class BackupUploadFlowTest {
    @Test
    fun `FFI success completes even without the progress callback`() = runTest {
        assertThat(backupUploadFlow { }.toList()).containsExactly(BackupUploadState.Done)
    }

    @Test
    fun `delivered Done completes without duplicate terminal states`() = runTest {
        assertThat(backupUploadFlow { it.onUpdate(RustBackupUploadState.Done) }.toList()).containsExactly(BackupUploadState.Done)
    }

    @Test
    fun `failed upload never reports success`() = runTest {
        assertThat(backupUploadFlow { throw IllegalStateException("synthetic") }.toList()).containsExactly(BackupUploadState.Error)
    }

    @Test
    fun `early Done callback cannot hide an FFI failure`() = runTest {
        assertThat(backupUploadFlow {
            it.onUpdate(RustBackupUploadState.Done)
            throw IllegalStateException("synthetic")
        }.toList()).containsExactly(BackupUploadState.Error)
    }

    @Test
    fun `cancelled upload propagates cancellation`() = runTest {
        val result = runCatching { backupUploadFlow { throw CancellationException("synthetic") }.toList() }
        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}
