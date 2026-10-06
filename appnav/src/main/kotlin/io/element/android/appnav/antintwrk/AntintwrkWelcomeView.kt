/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.element.android.appnav.R
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import java.util.Calendar
import java.util.Locale

@Composable
fun AntintwrkLoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(ElementTheme.colors.bgCanvasDefault), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("anti·ntwrk", style = ElementTheme.typography.fontHeadingLgBold)
            CircularProgressIndicator()
            Text(stringResource(R.string.screen_antintwrk_loading), style = ElementTheme.typography.fontBodyMdRegular)
        }
    }
}

@Composable
fun AntintwrkWelcomeView(
    state: AntintwrkWelcomeState,
    openMatrixLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val emit = state.eventSink
    BackHandler(enabled = state.page != AntintwrkPage.Welcome) { emit(AntintwrkWelcomeEvent.Back) }
    Column(
        modifier.fillMaxSize().background(ElementTheme.colors.bgCanvasDefault).safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.page != AntintwrkPage.Welcome) {
            TextButton(stringResource(R.string.screen_antintwrk_back), { emit(AntintwrkWelcomeEvent.Back) })
        }
        Spacer(Modifier.height(if (state.page == AntintwrkPage.Welcome) 64.dp else 8.dp))
        Text("anti·ntwrk", style = ElementTheme.typography.fontHeadingLgBold)
        val title = when (state.page) {
            AntintwrkPage.Welcome -> R.string.screen_antintwrk_welcome
            AntintwrkPage.Register -> R.string.screen_antintwrk_register
            AntintwrkPage.Profile -> R.string.screen_antintwrk_profile
            AntintwrkPage.Login -> R.string.screen_antintwrk_login
            AntintwrkPage.Reset -> R.string.screen_antintwrk_reset
        }
        Text(stringResource(title), style = ElementTheme.typography.fontHeadingMdBold)
        if (state.page == AntintwrkPage.Welcome) {
            Text(stringResource(R.string.screen_antintwrk_intro), style = ElementTheme.typography.fontBodyLgRegular)
            Spacer(Modifier.height(32.dp))
            Button(stringResource(R.string.screen_antintwrk_register), { emit(AntintwrkWelcomeEvent.Open(AntintwrkPage.Register)) }, Modifier.fillMaxWidth())
            TextButton(stringResource(R.string.screen_antintwrk_login), { emit(AntintwrkWelcomeEvent.Open(AntintwrkPage.Login)) }, Modifier.fillMaxWidth())
            TextButton(stringResource(R.string.screen_antintwrk_matrix_login), openMatrixLogin, Modifier.fillMaxWidth())
        } else {
            Text(stringResource(R.string.screen_antintwrk_preview), color = ElementTheme.colors.textSecondary)
            if (state.page != AntintwrkPage.Profile) {
                TextField(
                    state.email,
                    { emit(AntintwrkWelcomeEvent.Email(it)) },
                    Modifier.fillMaxWidth(),
                    label = stringResource(R.string.screen_antintwrk_email),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
            if (state.page == AntintwrkPage.Register || state.page == AntintwrkPage.Login) {
                TextField(
                    state.password,
                    { emit(AntintwrkWelcomeEvent.Password(it)) },
                    Modifier.fillMaxWidth(),
                    label = stringResource(R.string.screen_antintwrk_password),
                    singleLine = true,
                    supportingText = if (state.page == AntintwrkPage.Register) stringResource(R.string.screen_antintwrk_password_hint) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            stringResource(if (state.showPassword) R.string.screen_antintwrk_hide else R.string.screen_antintwrk_show),
                            { emit(AntintwrkWelcomeEvent.TogglePassword) }
                        )
                    }
                )
            }
            if (state.page == AntintwrkPage.Profile) {
                TextField(
                    state.firstName,
                    { emit(AntintwrkWelcomeEvent.FirstName(it)) },
                    Modifier.fillMaxWidth(),
                    label = stringResource(R.string.screen_antintwrk_first_name),
                    singleLine = true
                )
                TextField(
                    state.surname,
                    { emit(AntintwrkWelcomeEvent.Surname(it)) },
                    Modifier.fillMaxWidth(),
                    label = stringResource(R.string.screen_antintwrk_surname),
                    singleLine = true
                )
                TextField(
                    state.birthdate,
                    { emit(AntintwrkWelcomeEvent.Birthdate(it)) },
                    Modifier.fillMaxWidth(),
                    label = stringResource(R.string.screen_antintwrk_birthdate),
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        TextButton(stringResource(R.string.screen_antintwrk_choose_date), {
                            val calendar = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
                            DatePickerDialog(context, { _, year, month, day ->
                                emit(AntintwrkWelcomeEvent.Birthdate(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)))
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        })
                    }
                )
                TextButton(stringResource(R.string.screen_antintwrk_terms), {
                    uriHandler.openUri("https://antintwrk.com/terms")
                })
                Row(Modifier.fillMaxWidth().toggleable(
                    state.agreed,
                    role = Role.Checkbox,
                    onValueChange = { emit(AntintwrkWelcomeEvent.Agreement(it)) }
                ), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = state.agreed, onCheckedChange = null)
                    Text(stringResource(R.string.screen_antintwrk_agree))
                }
            }
            if (state.error) {
                Text(stringResource(if (state.page == AntintwrkPage.Profile) {
                    R.string.screen_antintwrk_profile_error
                } else {
                    R.string.screen_antintwrk_credentials_error
                }), color = ElementTheme.colors.textCriticalPrimary)
            }
            if (state.unavailable) Text(stringResource(R.string.screen_antintwrk_unavailable), color = ElementTheme.colors.textSecondary)
            Button(
                stringResource(if (state.page == AntintwrkPage.Profile) R.string.screen_antintwrk_register else R.string.screen_antintwrk_continue),
                { emit(AntintwrkWelcomeEvent.Continue) },
                Modifier.fillMaxWidth()
            )
            if (state.page == AntintwrkPage.Login) {
                TextButton(stringResource(R.string.screen_antintwrk_reset), { emit(AntintwrkWelcomeEvent.Open(AntintwrkPage.Reset)) })
            }
        }
    }
}

internal class AntintwrkWelcomePreviewStates : PreviewParameterProvider<AntintwrkWelcomeState> {
    override val values = sequenceOf(
        AntintwrkWelcomeState(),
        AntintwrkWelcomeState(page = AntintwrkPage.Register),
        AntintwrkWelcomeState(page = AntintwrkPage.Register, error = true),
        AntintwrkWelcomeState(page = AntintwrkPage.Profile),
        AntintwrkWelcomeState(page = AntintwrkPage.Login),
        AntintwrkWelcomeState(page = AntintwrkPage.Reset, unavailable = true),
    )
}

@PreviewsDayNight
@Composable
internal fun AntintwrkWelcomePreview(@PreviewParameter(AntintwrkWelcomePreviewStates::class) state: AntintwrkWelcomeState) = ElementPreview {
    AntintwrkWelcomeView(state, {})
}

@PreviewsDayNight
@Composable
internal fun AntintwrkLoadingPreview() = ElementPreview { AntintwrkLoadingView() }
