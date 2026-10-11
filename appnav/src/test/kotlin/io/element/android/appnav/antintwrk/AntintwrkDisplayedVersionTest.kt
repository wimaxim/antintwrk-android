// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AntintwrkDisplayedVersionTest {
    @Test fun `version displays build identification without the old test marker`() {
        assertThat(antintwrkDisplayedVersion("0.1.0-test.20261010.6")).isEqualTo("0.1.0.20261010.6")
        assertThat(antintwrkDisplayedVersion("1.0.5")).isEqualTo("1.0.5")
    }
}
