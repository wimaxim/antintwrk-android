/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.createroom.impl.addpeople

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.features.createroom.impl.R
import io.element.android.features.invitepeople.api.InvitePeopleEvent
import io.element.android.features.invitepeople.api.InvitePeopleState
import io.element.android.features.invitepeople.api.InvitePeopleStatePreviewParam
import io.element.android.libraries.designsystem.atomic.pages.HeaderFooterPage
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AddPeopleView(
    state: InvitePeopleState,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPeopleSelected: ((List<MatrixUser>) -> Unit)? = null,
    invitePeopleView: @Composable () -> Unit,
) {
    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(state.sendInvitesAction, state.createRoomFromDmAction, onPeopleSelected) {
        if (onPeopleSelected == null && (state.sendInvitesAction.isSuccess() || state.createRoomFromDmAction.isSuccess())) {
            currentOnFinish()
        }
    }

    HeaderFooterPage(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        topBar = {
            AddPeopleTopBar(onSkipClick = onFinish, selectionRequired = onPeopleSelected != null, onBackClick = onBackClick)
        },
        footer = {
            Button(
                text = stringResource(if (onPeopleSelected != null) CommonStrings.action_next else CommonStrings.action_finish),
                onClick = {
                    if (onPeopleSelected != null) {
                        val users = state.selectedUsers.distinctBy { it.userId }
                        if (users.size >= 2) onPeopleSelected(users)
                    } else {
                        state.eventSink(InvitePeopleEvent.SendInvites)
                    }
                },
                enabled = state.canInvite && (onPeopleSelected == null || state.selectedUsers.distinctBy { it.userId }.size >= 2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        },
        content = {
            Column {
                if (onPeopleSelected != null) {
                    Text(
                        text = stringResource(CommonStrings.screen_ant_group_minimum_people),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                invitePeopleView()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPeopleTopBar(
    onSkipClick: () -> Unit,
    selectionRequired: Boolean,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        titleStr = stringResource(R.string.screen_create_room_add_people_title),
        navigationIcon = { if (selectionRequired) BackButton(onClick = onBackClick) },
        actions = {
            if (!selectionRequired) {
                TextButton(
                text = stringResource(CommonStrings.action_skip),
                onClick = onSkipClick,
            )
            }
        }
    )
}

@PreviewsDayNight
@Composable
internal fun AddPeopleViewPreview(@PreviewParameter(InvitePeopleStatePreviewParam::class) state: InvitePeopleState) = ElementPreview {
    AddPeopleView(
        state = state,
        invitePeopleView = {},
        onFinish = {},
    )
}

internal class RequiredGroupPeoplePreviewParam : androidx.compose.ui.tooling.preview.PreviewParameterProvider<InvitePeopleState> {
    override val values = (0..2).asSequence().map { count ->
        object : InvitePeopleState {
            override val selectedUsers = (1..count).map {
                MatrixUser(io.element.android.libraries.matrix.api.core.UserId("@person$it:antintwrk.com"), displayName = "Person $it")
            }.toImmutableList()
            override val canInvite = count > 0
            override val isSearchActive = false
            override val sendInvitesAction: io.element.android.libraries.architecture.AsyncAction<Unit> =
                io.element.android.libraries.architecture.AsyncAction.Uninitialized
            override val createRoomFromDmAction: io.element.android.libraries.architecture.AsyncAction<io.element.android.libraries.matrix.api.core.RoomId> =
                io.element.android.libraries.architecture.AsyncAction.Uninitialized
            override val eventSink: (InvitePeopleEvent) -> Unit = {}
        }
    }
}

@PreviewsDayNight
@Composable
internal fun RequiredGroupPeoplePreview(@PreviewParameter(RequiredGroupPeoplePreviewParam::class) state: InvitePeopleState) = ElementPreview {
    AddPeopleView(state = state, onFinish = {}, onPeopleSelected = {}, invitePeopleView = {
        state.selectedUsers.forEach { Text(it.displayName.orEmpty(), modifier = Modifier.padding(16.dp)) }
    })
}
