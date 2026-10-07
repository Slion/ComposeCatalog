/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may obtain a copy of the License at
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import dev.vicart.compose.material.symbols.MaterialSymbol
import androidx.compose.ui.text.AnnotatedString
import net.slions.compose.preference.ColorPreference
import net.slions.compose.preference.AccentColorOption
import net.slions.compose.preference.LiveSliderPreference
import net.slions.compose.preference.ListPreference
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory

/**
 * The "Theme" page: live theme controls. Its state is hoisted in [SampleApp] as a
 * [SampleThemeValues] and applied by [SampleTheme], so changing a row here re-themes the whole
 * app immediately — a working example of the controlled preference forms.
 */
@Composable
fun themePreferencePage(
    values: SampleThemeValues,
    onValuesChange: (SampleThemeValues) -> Unit,
): PreferencePage {
    // The platform's real "Default" accent, resolved against the effective theme (so the
    // swatch shows what Default will look like, not the currently-selected accent).
    val dark = effectiveDarkTheme(values.themeMode, isSystemInDarkTheme())
    val defaultAccent = systemDefaultAccentColor(dark)
    return PreferencePage(
        id = "theme",
        title = "Theme",
        summary = "Contrast, colors, and text. Settings are persisted.",
        icon = { MaterialSymbol.Outlined(icon = "palette") },
    ) {
        preferenceCategory(key = "theme_colors_category", title = "Colors")
        preferenceCardGroup(key = "theme_colors_group") {
            card {
                ListPreference(
                    value = values.themeMode,
                    onValueChange = { onValuesChange(values.copy(themeMode = it)) },
                    values = SampleThemeMode.entries,
                    title = "Contrast",
                    summary = values.themeMode.label,
                    icon = { Icon(imageVector = Icons.Outlined.Contrast, contentDescription = null) },
                    valueToText = { AnnotatedString(it.label) },
                )
            }
            card {
                ColorPreference(
                    value = values.accent ?: "",
                    onValueChange = { onValuesChange(values.copy(accent = it.ifEmpty { null })) },
                    options = SampleThemeValues.ACCENT_PRESETS.map { AccentColorOption(it.name, it.hex) },
                    title = "Color",
                    summary = accentNameOf(values.accent),
                    icon = { MaterialSymbol.Outlined(icon = "colors") },
                    defaultOptionColor = defaultAccent,
                )
            }
            card {
                LiveSliderPreference(
                    title = "Tint",
                    value = (values.tintFactorPercent ?: DEFAULT_TINT_FACTOR_PERCENT).toFloat(),
                    onValueChange = { onValuesChange(values.copy(tintFactorPercent = it.toInt())) },
                    valueRange = TINT_FACTOR_RANGE.first.toFloat()..TINT_FACTOR_RANGE.last.toFloat(),
                    // M3 'steps' counts intermediate stops (segments = steps + 1), so for 5% steps
                    // over 0..50 (10 segments) we pass 9, not 10.
                    valueSteps = (TINT_FACTOR_RANGE.last - TINT_FACTOR_RANGE.first) / 5 - 1,
                    valueText = { "${it.toInt()}%" },
                    live = true,
                    icon = { MaterialSymbol.Outlined(icon = "imagesearch_roller") },
                )
            }
        }

        preferenceCategory(key = "theme_text_category", title = "Texts")
        preferenceCardGroup(key = "theme_text_group") {
            card {
                ListPreference(
                    value = values.fontFamily ?: SampleThemeValues.DEFAULT_FONT,
                    onValueChange = { onValuesChange(values.copy(fontFamily = it)) },
                    values = SampleThemeValues.FONT_FAMILIES,
                    title = "Font",
                    summary = fontLabel(values.fontFamily),
                    icon = { Icon(imageVector = Icons.Outlined.TextFields, contentDescription = null) },
                    valueToText = { AnnotatedString(fontLabel(it)) },
                )
            }
            card {
                LiveSliderPreference(
                    title = "Size",
                    value = (values.fontSizePercent ?: 100).toFloat(),
                    onValueChange = { onValuesChange(values.copy(fontSizePercent = it.toInt())) },
                    valueRange = 80f..140f,
                    // 5% steps over 80..140 = 12 segments, so 11 intermediate stops.
                    valueSteps = (140 - 80) / 5 - 1,
                    valueText = { "${it.toInt()}%" },
                    live = true,
                    icon = { MaterialSymbol.Outlined(icon = "format_size") },
                )
            }
        }

        preferenceCategory(key = "theme_shapes_category", title = "Shapes")
        preferenceCardGroup(key = "theme_shapes_group") {
            card {
                LiveSliderPreference(
                    title = "Corner",
                    value = (values.cornerRadiusDp ?: DEFAULT_CORNER_RADIUS_DP).toFloat(),
                    onValueChange = { onValuesChange(values.copy(cornerRadiusDp = it.toInt())) },
                    valueRange = 0f..32f,
                    // 2 dp steps over 0..32 = 16 segments, so 15 intermediate stops.
                    valueSteps = (32 - 0) / 2 - 1,
                    valueText = { "${it.toInt()} dp" },
                    live = true,
                    icon = { MaterialSymbol.Outlined(icon = "rounded_corner") },
                )
            }
        }
    }
}

private fun accentNameOf(hex: String?): String =
    SampleThemeValues.ACCENT_PRESETS.firstOrNull { it.hex == hex }?.name ?: "Default"

private fun fontLabel(font: String?): String = when (font) {
    SampleThemeValues.SERIF_FONT -> "Serif"
    SampleThemeValues.MONO_FONT -> "Monospace"
    else -> "Default"
}
