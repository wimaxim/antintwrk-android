// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.preferences.impl.notifications

import io.element.android.features.preferences.impl.R
import io.element.android.libraries.matrix.api.BuildConfig

/** Branding only: preserve sound sentinels, stored choices and the actual audio resources. */
internal fun managedNotificationSoundLabel(fade: Boolean): Int = when {
    BuildConfig.ANTINTWRK_NATIVE_AUTH && fade -> R.string.screen_ant_sound_fade
    BuildConfig.ANTINTWRK_NATIVE_AUTH -> R.string.screen_ant_sound_standard
    fade -> R.string.screen_notification_settings_sound_element_fade
    else -> R.string.screen_notification_settings_sound_element_default
}
