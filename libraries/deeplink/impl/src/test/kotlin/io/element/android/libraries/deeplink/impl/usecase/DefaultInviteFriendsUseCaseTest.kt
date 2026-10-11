/*
 * Copyright (c) 2026 Super David Universe LLC.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.deeplink.impl.usecase

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.core.aBuildMeta
import io.element.android.libraries.matrix.test.permalink.FakePermalinkBuilder
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.InstrumentationStringProvider
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class DefaultInviteFriendsUseCaseTest : RobolectricTest() {
    @Test
    fun `messenger invite shares social invitation without accessing matrix permalink`() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            val strings = InstrumentationStringProvider()
            DefaultInviteFriendsUseCase(
                stringProvider = strings,
                matrixClient = FakeMatrixClient(),
                buildMeta = aBuildMeta(applicationName = "antintwrk"),
                permalinkBuilder = FakePermalinkBuilder(), // Throws on any access: no transport or user ID may be exposed.
            ).execute(activity)
            val chooser = shadowOf(activity).nextStartedActivity
            assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
            val share = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertThat(share.action).isEqualTo(Intent.ACTION_SEND)
            assertThat(share.type).isEqualTo("text/plain")
            assertThat(share.`package`).isNull()
            val text = share.getStringExtra(Intent.EXTRA_TEXT)
            assertThat(text).isEqualTo(strings.getString(CommonStrings.screen_ant_invite_message))
            assertThat(text).contains("https://antintwrk.com")
            assertThat(text).doesNotContain("antimsg")
            assertThat(text).doesNotContain("matrix")
        }
    }

    @Config(qualifiers = "ru")
    @Test
    fun `messenger invite uses russian public invitation`() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            DefaultInviteFriendsUseCase(
                stringProvider = InstrumentationStringProvider(),
                matrixClient = FakeMatrixClient(),
                buildMeta = aBuildMeta(),
                permalinkBuilder = FakePermalinkBuilder(),
            ).execute(controller.get())
            val chooser = shadowOf(controller.get()).nextStartedActivity
            val share = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertThat(share.getStringExtra(Intent.EXTRA_TEXT)).startsWith("Привет!")
            assertThat(chooser.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo("Пригласить друзей")
        }
    }
}
