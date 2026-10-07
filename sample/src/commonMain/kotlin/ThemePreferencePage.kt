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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import net.slions.compose.preference.ListPreference
import net.slions.compose.preference.ListPreferenceType
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.SliderPreference
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
): PreferencePage =
    PreferencePage(
        id = "theme",
        title = "Theme",
        summary = "Mode, colors, and text. Settings are persisted.",
    ) {
        preferenceCategory(key = "theme_colors_category", title = "Colors")
        preferenceCardGroup(key = "theme_colors_group") {
            card {
                ListPreference(
                    value = values.themeMode,
                    onValueChange = { onValuesChange(values.copy(themeMode = it)) },
                    values = SampleThemeMode.entries,
                    title = "Mode",
                    summary = values.themeMode.label,
                    valueToText = { AnnotatedString(it.label) },
                )
            }
            card {
                AccentList(
                    value = values.accent ?: "",
                    onValueChange = { onValuesChange(values.copy(accent = it.ifEmpty { null })) },
                    values = SampleThemeValues.ACCENT_PRESETS,
                    title = "Accent color",
                    summary = accentNameOf(values.accent),
                )
            }
            card {
                ThemeSlider(
                    title = "Neutral tint",
                    value = (values.tintFactorPercent ?: DEFAULT_TINT_FACTOR_PERCENT).toFloat(),
                    onValueChange = { onValuesChange(values.copy(tintFactorPercent = it.toInt())) },
                    valueRange = TINT_FACTOR_RANGE.first.toFloat()..TINT_FACTOR_RANGE.last.toFloat(),
                    // M3 'steps' counts intermediate stops (segments = steps + 1), so for 5% steps
                    // over 0..50 (10 segments) we pass 9, not 10.
                    valueSteps = (TINT_FACTOR_RANGE.last - TINT_FACTOR_RANGE.first) / 5 - 1,
                    valueText = { "${it.toInt()}%" },
                    live = true,
                )
            }
        }

        preferenceCategory(key = "theme_shapes_text_category", title = "Shapes and text")
        preferenceCardGroup(key = "theme_shapes_text_group") {
            card {
                ThemeSlider(
                    title = "Corner radius",
                    value = (values.cornerRadiusDp ?: DEFAULT_CORNER_RADIUS_DP).toFloat(),
                    onValueChange = { onValuesChange(values.copy(cornerRadiusDp = it.toInt())) },
                    valueRange = 0f..32f,
                    // 2 dp steps over 0..32 = 16 segments, so 15 intermediate stops.
                    valueSteps = (32 - 0) / 2 - 1,
                    valueText = { "${it.toInt()} dp" },
                    live = true,
                )
            }
            card {
                ThemeSlider(
                    title = "Text size",
                    value = (values.fontSizePercent ?: 100).toFloat(),
                    onValueChange = { onValuesChange(values.copy(fontSizePercent = it.toInt())) },
                    valueRange = 80f..140f,
                    // 5% steps over 80..140 = 12 segments, so 11 intermediate stops.
                    valueSteps = (140 - 80) / 5 - 1,
                    valueText = { "${it.toInt()}%" },
                    live = true,
                )
            }
            card {
                ListPreference(
                    value = values.fontFamily ?: SampleThemeValues.DEFAULT_FONT,
                    onValueChange = { onValuesChange(values.copy(fontFamily = it)) },
                    values = SampleThemeValues.FONT_FAMILIES,
                    title = "Font",
                    summary = fontLabel(values.fontFamily),
                    type = ListPreferenceType.DROPDOWN_MENU,
                    valueToText = { AnnotatedString(fontLabel(it)) },
                )
            }
        }
    }

/**
 * A host-controlled slider: owns its committed [value] and a separate drag state, committing
 * on release via [onValueChange].
 */
@Composable
private fun ThemeSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueSteps: Int,
    valueText: (Float) -> String,
    live: Boolean = false,
) {
    // The drag position is its own stable state (not keyed on [value]); keying it on [value]
    // would re-init it on every commit, which — with a live slider that commits while dragging —
    // makes the thumb fight the finger and the effect never settles.
    var sliderValue by remember { mutableFloatStateOf(value) }
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        sliderValue = sliderValue,
        onSliderValueChange = {
            sliderValue = it
            // Live: commit while dragging so the effect applies in real time; otherwise only
            // on release (the default, via onValueChange).
            if (live) onValueChange(it)
        },
        title = title,
        valueRange = valueRange,
        valueSteps = valueSteps,
        valueText = valueText,
    )
}

/**
 * An accent-color picker: an alert dialog of [AccentColor] swatches. A null [hex] is the
 * platform default (dynamic colors).
 */
@Composable
private fun AccentList(
    value: String,
    onValueChange: (String) -> Unit,
    values: List<AccentColor>,
    title: String,
    summary: String,
) {
    ListPreference(
        value = value,
        onValueChange = onValueChange,
        values = values.map { it.hex ?: "" },
        title = title,
        summary = summary,
        valueToText = { AnnotatedString(accentNameOf(it.ifEmpty { null })) },
        item = { accentHex, currentValue, onClick ->
            // M3 ListItem isn't clickable on its own; wrap the swatch row in a clickable
            // container so tapping it confirms the choice.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
            ) {
                ListItem(
                    headlineContent = {
                        Text(accentNameOf(accentHex.ifEmpty { null }))
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (accentHex.isEmpty()) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        parseHexColor(accentHex)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                ),
                        )
                    },
                    trailingContent = {
                        if (accentHex == currentValue) {
                            Checkbox(checked = true, onCheckedChange = null)
                        }
                    },
                )
            }
        },
    )
}

private fun accentNameOf(hex: String?): String =
    SampleThemeValues.ACCENT_PRESETS.firstOrNull { it.hex == hex }?.name ?: "Default"

private fun fontLabel(font: String?): String = when (font) {
    SampleThemeValues.SERIF_FONT -> "Serif"
    SampleThemeValues.MONO_FONT -> "Monospace"
    else -> "Default"
}
