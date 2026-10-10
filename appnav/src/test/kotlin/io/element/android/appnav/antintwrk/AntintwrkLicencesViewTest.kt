// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp")
class AntintwrkLicencesViewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `founder remains fully laid out and opens the social profile with enlarged text`() {
        var opened = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.6f)) {
                AntintwrkLicencesDialog(onOpenAuthor = { opened = true }, onDismiss = {})
            }
        }
        val name = compose.onNodeWithText("Maxim Novichkov", useUnmergedTree = true)
        name.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        val layoutAction = name.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!
        compose.runOnIdle {
            layoutAction.invoke(layouts)
        }
        assertThat(layouts.single().hasVisualOverflow).isFalse()
        assertThat(layouts.single().getLineEnd(layouts.single().lineCount - 1)).isEqualTo("Maxim Novichkov".length)
        name.performClick()
        assertThat(opened).isTrue()
    }
}
