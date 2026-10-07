/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.appnav.R
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.compound.tokens.generated.compoundColorsDark
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextField
import java.lang.ref.WeakReference
import java.util.WeakHashMap

// Share the activity window across overlapping Appyx loading/welcome screens.
val LocalAntintwrkActivity = staticCompositionLocalOf<ComponentActivity?> { null }

// Public antintwrk web palette; scoped to account/social previews, never to Matrix screens.
internal val AntintwrkBackground = Color(0xFF101214)
private val AntintwrkPanel = Color(0xFF1B1E22)
private val AntintwrkField = Color(0xFF2B3036)
private val AntintwrkText = Color(0xFFF1F1ED)
private val AntintwrkMuted = Color(0xFF9BA1AA)
private val AntintwrkCoral = Color(0xFFF06B55)
private val AntintwrkOnCoral = Color(0xFF160F0D)
private val activeWindowOwners = WeakHashMap<ComponentActivity, AntintwrkSystemBarOwner>()

@Composable
internal fun AntintwrkBrandTheme(content: @Composable () -> Unit) {
    val activity = LocalAntintwrkActivity.current
    val parentIsLight = ElementTheme.isLightTheme
    val parentBackground = ElementTheme.colors.bgCanvasDefault
    LaunchedEffect(activity, parentIsLight, parentBackground) {
        // Apply after the parent theme has updated the window on entry.
        withFrameNanos { }
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AntintwrkBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
    }
    DisposableEffect(activity, parentIsLight, parentBackground) {
        val release = activity?.let { host ->
            val reference = WeakReference(host)
            val owner = activeWindowOwners.getOrPut(host) { AntintwrkSystemBarOwner() }
            owner.acquire {
                reference.get()?.let { windowOwner ->
                    windowOwner.enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(parentBackground.toArgb(), parentBackground.toArgb()) { !parentIsLight },
                        navigationBarStyle = if (parentIsLight) {
                            SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                        } else {
                            SystemBarStyle.dark(Color.Transparent.toArgb())
                        },
                    )
                    activeWindowOwners.remove(windowOwner)
                }
            }
        }
        onDispose { release?.invoke() }
    }
    ElementTheme(
        theme = Theme.Dark,
        applySystemBarsUpdate = false,
        compoundDark = compoundColorsDark.copy(
            bgCanvasDefault = AntintwrkBackground,
            bgCanvasDefaultLevel1 = AntintwrkPanel,
            bgAccentRest = AntintwrkCoral,
            bgActionPrimaryRest = AntintwrkCoral,
            bgSubtleSecondary = AntintwrkPanel,
            borderFocused = AntintwrkCoral,
            textPrimary = AntintwrkText,
            textSecondary = AntintwrkMuted,
            textActionAccent = AntintwrkText,
            textLinkExternal = AntintwrkCoral,
        ),
        materialColorsDark = darkColorScheme(
            primary = AntintwrkCoral,
            onPrimary = AntintwrkOnCoral,
            background = AntintwrkBackground,
            onBackground = AntintwrkText,
            surface = AntintwrkPanel,
            onSurface = AntintwrkText,
            surfaceVariant = AntintwrkField,
            onSurfaceVariant = AntintwrkMuted,
        ),
        content = content,
    )
}

@Composable
internal fun AntintwrkBrandMark(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.antintwrk_brand_mark), contentDescription = null, modifier = modifier.size(size))
}

@Composable
internal fun AntintwrkWordmark() {
    Text(
        buildAnnotatedString {
            append("anti")
            withStyle(SpanStyle(color = AntintwrkCoral)) { append("·") }
            append("ntwrk")
        },
        style = ElementTheme.typography.fontHeadingLgBold,
    )
}

@Composable
internal fun AntintwrkPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AntintwrkCoral, contentColor = AntintwrkOnCoral),
    ) { Text(text, style = ElementTheme.typography.fontBodyMdRegular.copy(fontWeight = FontWeight.Bold)) }
}

@Composable
internal fun AntintwrkSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, AntintwrkText.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = AntintwrkPanel, contentColor = AntintwrkText),
    ) { Text(text, style = ElementTheme.typography.fontBodyMdRegular.copy(fontWeight = FontWeight.Bold)) }
}

@Composable
internal fun AntintwrkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    supportingText: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = ElementTheme.colors.copy(bgCanvasDefault = AntintwrkField)
    val materialColors = ElementTheme.materialColors
    ElementTheme(Theme.Dark, applySystemBarsUpdate = false, compoundDark = colors, materialColorsDark = materialColors) {
        TextField(
            value,
            onValueChange,
            modifier,
            label = label,
            supportingText = supportingText,
            singleLine = singleLine,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
        )
    }
}
