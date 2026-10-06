/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.preference.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
expect fun SampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
)

/**
 * Resolves the effective dark-theme flag for [mode]: [SampleThemeMode.DARK] is always true,
 * [SampleThemeMode.LIGHT] always false, and [SampleThemeMode.SYSTEM] follows
 * [systemInDarkTheme].
 */
fun effectiveDarkTheme(mode: SampleThemeMode, systemInDarkTheme: Boolean): Boolean =
    when (mode) {
        SampleThemeMode.DARK -> true
        SampleThemeMode.LIGHT -> false
        SampleThemeMode.SYSTEM -> systemInDarkTheme
    }

/**
 * Applies the sample's Material theme: the color scheme from [sampleColorScheme] (dynamic
 * colors / a fixed accent / the M3 default) plus the [SampleThemeValues] shapes and
 * typography. Reads the current [LocalSampleThemeValues] from the composition.
 */
@Composable
internal fun applySampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val values = LocalSampleThemeValues.current
    MaterialTheme(
        colorScheme = sampleColorScheme(values.accent, darkTheme, dynamicColor),
        shapes = sampleShapes(values.cornerRadiusDp ?: DEFAULT_CORNER_RADIUS_DP),
        typography = sampleTypography(values.fontSizePercent ?: 100, values.fontFamily),
        content = content,
    )
}

/** Shape scale built on the user's corner radius (the "medium" size of the scale). */
internal fun sampleShapes(cornerRadiusDp: Int): Shapes =
    Shapes(
        extraSmall = RoundedCornerShape((cornerRadiusDp / 3).dp),
        small = RoundedCornerShape((cornerRadiusDp / 2).dp),
        medium = RoundedCornerShape(cornerRadiusDp.dp),
        large = RoundedCornerShape((cornerRadiusDp * 1.5f).dp),
        extraLarge = RoundedCornerShape((cornerRadiusDp * 2.5f).dp),
    )

/** The [fontFamily] type scale, every style scaled by [percent] (clamped to 85..130). */
internal fun sampleTypography(percent: Int, fontFamily: String?): Typography {
    val family = when (fontFamily) {
        SampleThemeValues.SERIF_FONT -> FontFamily.Serif
        SampleThemeValues.MONO_FONT -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    val factor = percent.coerceIn(85, 130) / 100f
    // Scale the font size and set the family; the M3 type scale's proportional line-height
    // and letter-spacing stay correct relative to the new size.
    fun TextStyle.scale(): TextStyle = copy(
        fontSize = (fontSize.value * factor).sp,
        fontFamily = family,
    )
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.scale(),
        displayMedium = base.displayMedium.scale(),
        displaySmall = base.displaySmall.scale(),
        headlineLarge = base.headlineLarge.scale(),
        headlineMedium = base.headlineMedium.scale(),
        headlineSmall = base.headlineSmall.scale(),
        titleLarge = base.titleLarge.scale(),
        titleMedium = base.titleMedium.scale(),
        titleSmall = base.titleSmall.scale(),
        bodyLarge = base.bodyLarge.scale(),
        bodyMedium = base.bodyMedium.scale(),
        bodySmall = base.bodySmall.scale(),
        labelLarge = base.labelLarge.scale(),
        labelMedium = base.labelMedium.scale(),
        labelSmall = base.labelSmall.scale(),
    )
}
