/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class AntintwrkWebFilePickerTest : RobolectricTest() {
    private val app = "com.antintwrk.app.debug"
    private val first = Uri.parse("content://media/picker/1")
    private val second = Uri.parse("content://media/picker/2")

    @Test
    fun `camera is offered only for an exclusively supported media input`() {
        assertThat(socialCaptureKind(arrayOf("image/jpeg,image/png,image/webp"))).isEqualTo("image")
        assertThat(socialCaptureKind(arrayOf("video/mp4", "video/webm"))).isEqualTo("video")
        assertThat(socialCaptureKind(arrayOf("*/*"))).isNull()
        assertThat(socialCaptureKind(arrayOf("image/*", "application/pdf"))).isNull()
        assertThat(socialCaptureKind(emptyArray())).isNull()
    }

    @Test
    fun `multiple photo picker results work without intent data`() {
        val clip = ClipData.newRawUri("photo", first).apply {
            addItem(ClipData.Item(second))
            addItem(ClipData.Item(first))
        }
        val intent = Intent().apply { clipData = clip }
        assertThat(selectedSocialFiles(Activity.RESULT_OK, intent, true, app)?.toList()).containsExactly(first, second).inOrder()
        assertThat(selectedSocialFiles(Activity.RESULT_OK, intent, false, app)?.toList()).containsExactly(first)
    }

    @Test
    fun `cancellation missing results and private app providers never reach web content`() {
        assertThat(selectedSocialFiles(Activity.RESULT_CANCELED, Intent().setData(first), true, app)).isNull()
        assertThat(selectedSocialFiles(Activity.RESULT_OK, null, true, app)).isNull()
        listOf("file:///data/private", "content://$app.files/private", "content://$app/private", "content:///missing").forEach {
            assertThat(selectedSocialFiles(Activity.RESULT_OK, Intent().setData(Uri.parse(it)), true, app)).isNull()
        }
        assertThat(selectedSocialFiles(Activity.RESULT_OK, Intent().setData(first), false, app)?.toList()).containsExactly(first)
    }
}
