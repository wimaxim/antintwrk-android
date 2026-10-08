// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.designsystem.utils

import androidx.compose.runtime.staticCompositionLocalOf

/** Host-supplied navigation. Returning false preserves the feature's normal link behavior. */
val LocalEmbeddedLinkHandler = staticCompositionLocalOf<((String) -> Boolean)?> { null }
