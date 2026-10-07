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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import dev.vicart.compose.material.symbols.MaterialSymbol
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.slions.compose.preference.ListPreference
import net.slions.compose.preference.Preference
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
        icon = { MaterialSymbol.Outlined(icon = "palette") },
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
                    icon = { Icon(imageVector = Icons.Outlined.Contrast, contentDescription = null) },
                    valueToText = { AnnotatedString(it.label) },
                )
            }
            card {
                AccentList(
                    value = values.accent ?: "",
                    onValueChange = { onValuesChange(values.copy(accent = it.ifEmpty { null })) },
                    values = SampleThemeValues.ACCENT_PRESETS,
                    title = "Color",
                    summary = accentNameOf(values.accent),
                    icon = { MaterialSymbol.Outlined(icon = "colors") },
                )
            }
            card {
                ThemeSlider(
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
                ThemeSlider(
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
                ThemeSlider(
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
    icon: @Composable (() -> Unit)? = null,
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
        // Right-align the value on the title line (e.g. "Tint        30%"); it tracks the drag
        // state, so it updates live while dragging.
        titlePostfix = { Text(valueText(sliderValue)) },
        valueRange = valueRange,
        valueSteps = valueSteps,
        icon = icon,
    )
}

/**
 * An accent-color picker: an alert dialog of [AccentColor] swatches. A null [hex] is the
 * platform default (dynamic colors).
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AccentList(
    value: String,
    onValueChange: (String) -> Unit,
    values: List<AccentColor>,
    title: String,
    summary: String,
    icon: @Composable (() -> Unit)? = null,
) {
    var openSelector by rememberSaveable { mutableStateOf(false) }
    val swatchColor = if (value.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
        else parseHexColor(value)

    if (openSelector) {
        BasicAlertDialog(onDismissRequest = { openSelector = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlertDialogDefaults.titleContentColor,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
                    )
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(values) { accent ->
                            val accentHex = accent.hex ?: ""
                            val selected = accentHex == value
                            val accentColor = if (accentHex.isEmpty()) MaterialTheme.colorScheme.primary
                                else parseHexColor(accentHex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected, true, Role.RadioButton,
                                        onClick = { onValueChange(accentHex); openSelector = false },
                                    )
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor),
                                )
                                Spacer(modifier = Modifier.width(24.dp))
                                Text(
                                    text = accent.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (accentHex.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
                                                else accentColor,
                                            shape = RoundedCornerShape(8.dp),
                                        ),
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { openSelector = false },
                        modifier = Modifier.align(Alignment.End).padding(end = 16.dp, bottom = 16.dp),
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    Preference(
        title = title,
        summary = summary,
        icon = icon,
        widgetContainer = {
            Box(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .size(24.dp)
                    .background(swatchColor, shape = RoundedCornerShape(8.dp)),
            )
        },
        onClick = { openSelector = true },
    )
}

private fun accentNameOf(hex: String?): String =
    SampleThemeValues.ACCENT_PRESETS.firstOrNull { it.hex == hex }?.name ?: "Default"

private fun fontLabel(font: String?): String = when (font) {
    SampleThemeValues.SERIF_FONT -> "Serif"
    SampleThemeValues.MONO_FONT -> "Monospace"
    else -> "Default"
}
