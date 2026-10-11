/*
 * Copyright (c) 2026 Super David Universe LLC.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.element.android.features.home.impl.components

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import io.element.android.features.home.impl.HomeNavigationBarItem
import io.element.android.features.home.impl.filters.aRoomListFiltersState
import io.element.android.features.home.impl.spacefilters.anUnselectedSpaceFiltersState
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.tests.testutils.setSafeContent
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEncryptionHeaderTest : RobolectricTest() {
    @Test
    fun `chat list shows encryption feature with working search but spaces do not`() = runAndroidComposeUiTest {
        val selected = mutableStateOf(HomeNavigationBarItem.Chats)
        var searchOpened = false
        setSafeContent {
            HomeTopBar(
                selectedNavigationItem = selected.value,
                currentUserAndNeighbors = persistentListOf(aMatrixUser()),
                showAvatarIndicator = false,
                areSearchResultsDisplayed = false,
                onToggleSearch = { searchOpened = true },
                onMenuActionClick = {},
                onOpenSettings = {},
                onAccountSwitch = {},
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
                canReportBug = false,
                displayFilters = false,
                filtersState = aRoomListFiltersState(),
                spaceFiltersState = anUnselectedSpaceFiltersState(),
            )
        }
        val label = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getString(
            CommonStrings.screen_ant_encryption_feature
        )
        onNodeWithText(label).assertExists()
        onNodeWithContentDescription(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getString(CommonStrings.action_search)
        ).performClick()
        assertTrue(searchOpened)
        runOnIdle { selected.value = HomeNavigationBarItem.Spaces }
        onNodeWithText(label).assertDoesNotExist()
    }
}
