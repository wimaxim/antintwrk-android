// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.designsystem.utils

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/** A retained embedded conversation can sync while hidden without sending read receipts. */
val LocalEmbeddedConversationVisibility = compositionLocalOf<State<Boolean>> { mutableStateOf(true) }
