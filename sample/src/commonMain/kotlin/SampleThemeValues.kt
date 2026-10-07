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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import net.slions.compose.preference.LocalPreferenceFlow
import net.slions.compose.preference.MutablePreferences
import net.slions.compose.preference.Preferences

/**
 * The user-selectable theme properties the sample's "Theme" page can change.
 *
 * All properties are optional: a null value means "use the built-in default".
 *
 * @param accent hex color string (e.g. `"#6750A4"`) used as the color-scheme seed; null =
 *   the platform default (dynamic colors on Android 12+ when [SampleTheme] is asked for them).
 * @param cornerRadiusDp corner-radius scale for cards/rows (the "medium" size of the scale);
 *   null = [DEFAULT_CORNER_RADIUS_DP].
 * @param fontSizePercent font-size scale as a percentage of the default type scale; null = 100.
 * @param tintFactorPercent how strongly the accent tints the neutral roles (background,
 *   surfaces, text), as a percentage of the seed's chroma; null = [DEFAULT_TINT_FACTOR_PERCENT].
 * @param fontFamily one of [FONT_FAMILIES]; null = [DEFAULT_FONT].
 */
@Immutable
data class SampleThemeValues(
    val themeMode: SampleThemeMode = SampleThemeMode.SYSTEM,
    val accent: String? = null,
    val cornerRadiusDp: Int? = null,
    val fontSizePercent: Int? = null,
    val tintFactorPercent: Int? = null,
    val fontFamily: String? = null,
) {
    companion object {
        /** The platform default font. */
        const val DEFAULT_FONT = "default"

        /** A serif font. */
        const val SERIF_FONT = "serif"

        /** A monospace font. */
        const val MONO_FONT = "monospace"

        /** All selectable font families, in display order. */
        val FONT_FAMILIES: List<String> = listOf(DEFAULT_FONT, SERIF_FONT, MONO_FONT)

        /**
         * The accent presets, in display order (dynamic first). The first entry, whose [hex]
         * is null, means the platform default (dynamic colors on Android 12+). The rest are
         * fixed seeds — darker pastels rather than fully saturated colors, so the derived
         * scheme stays readable in both themes.
         */
        val ACCENT_PRESETS: List<AccentColor> = listOf(
            AccentColor("Default", null),
            AccentColor("Purple", "#6750A4"),
            AccentColor("Blue", "#6288C0"),
            AccentColor("Teal", "#4FA39B"),
            AccentColor("Green", "#7DA86E"),
            AccentColor("Amber", "#C9A24B"),
            AccentColor("Red", "#C2685C"),
            AccentColor("Orange", "#CE8A55"),
            AccentColor("Pink", "#C57FB5"),
        )
    }
}

/**
 * The selectable accent colors, shown as the "Color" row. A null [hex] means the
 * platform default (dynamic colors on Android 12+); otherwise [hex] is the fixed seed.
 */
@Immutable
data class AccentColor(val name: String, val hex: String?)

/** The light/dark theme the app should use. */
enum class SampleThemeMode(val label: String) {
    /** Follow the system setting. */
    SYSTEM("System"),

    /** Always light. */
    LIGHT("Light"),

    /** Always dark. */
    DARK("Dark"),
}

/** The current [SampleThemeValues]. The sample provides this around [SampleTheme]. */
val LocalSampleThemeValues = staticCompositionLocalOf { SampleThemeValues() }

/** Default corner radius (the "medium" size of the shape scale) in dp. */
internal const val DEFAULT_CORNER_RADIUS_DP = 12

/**
 * The default neutral-tint factor as a percentage: how strongly the seed hue tints the
 * neutral roles (background, surfaces, text). 5% is the standard M3 dynamic factor; higher
 * values push the accent into the neutrals.
 */
internal const val DEFAULT_TINT_FACTOR_PERCENT = 5

/** The tint-factor range, in percent, as a slider. */
internal val TINT_FACTOR_RANGE: IntRange = 0..50

// Preference keys the theme values are persisted under. Only primitive types (Int/String) are
// stored, matching what the default preference flow can serialize on every platform.
private const val KEY_THEME_MODE = "sample.theme.mode"
private const val KEY_ACCENT = "sample.theme.accent"
private const val KEY_CORNER_RADIUS = "sample.theme.cornerRadius"
private const val KEY_FONT_SIZE = "sample.theme.fontSize"
private const val KEY_TINT_FACTOR = "sample.theme.tintFactor"
private const val KEY_FONT_FAMILY = "sample.theme.fontFamily"

/**
 * Rebuilds a [SampleThemeValues] from a [Preferences] store. Unset or invalid entries fall
 * back to the built-in default for that field.
 */
private fun SampleThemeValues.Companion.fromPrefs(prefs: Preferences): SampleThemeValues =
    SampleThemeValues(
        themeMode = prefs.getString(KEY_THEME_MODE)?.let { name ->
            SampleThemeMode.entries.firstOrNull { it.name == name }
        } ?: SampleThemeMode.SYSTEM,
        accent = prefs.getString(KEY_ACCENT),
        cornerRadiusDp = prefs.getInt(KEY_CORNER_RADIUS),
        fontSizePercent = prefs.getInt(KEY_FONT_SIZE),
        tintFactorPercent = prefs.getInt(KEY_TINT_FACTOR),
        fontFamily = prefs.getString(KEY_FONT_FAMILY),
    )

private fun Preferences.getString(key: String): String? = this[key]

private fun Preferences.getInt(key: String): Int? = this[key]

/** Writes this [SampleThemeValues] into a [MutablePreferences] store (nulls are removed). */
private fun SampleThemeValues.writeTo(prefs: MutablePreferences) {
    prefs[KEY_THEME_MODE] = themeMode.name
    prefs[KEY_ACCENT] = accent
    prefs[KEY_CORNER_RADIUS] = cornerRadiusDp
    prefs[KEY_FONT_SIZE] = fontSizePercent
    prefs[KEY_TINT_FACTOR] = tintFactorPercent
    prefs[KEY_FONT_FAMILY] = fontFamily
}

/**
 * The current [SampleThemeValues], backed by the shared preference [flow] (the single source of
 * truth). Reads the store reactively into a [SampleThemeValues] and returns a [write] function
 * that applies a new value — updating the UI and persisting it to disk via the flow. The sample
 * provides this around [SampleTheme] so the whole app re-themes live and the settings survive a
 * relaunch.
 */
@Composable
internal fun rememberSampleThemeValues(
    flow: MutableStateFlow<Preferences> = LocalPreferenceFlow.current,
): Pair<SampleThemeValues, (SampleThemeValues) -> Unit> {
    val store = flow.collectAsState().value
    val values = remember(store) { SampleThemeValues.fromPrefs(store) }
    val write: (SampleThemeValues) -> Unit = { next ->
        flow.value = flow.value.toMutablePreferences().apply { next.writeTo(this) }
    }
    return values to write
}

/** The default M3 scheme for the given theme, used when no accent is set. */
internal fun sampleDefaultScheme(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme() else lightColorScheme()

/** Parses `#RRGGBB` / `#AARRGGBB` hex to a [Color], or [fallback] if malformed. */
internal fun parseHexColor(hex: String, fallback: Color = Color.Unspecified): Color =
    runCatching {
        var h = hex.trim().removePrefix("#")
        if (h.length == 6) h = "FF$h"
        if (h.length == 8) Color(h.toLong(16).toInt()) else fallback
    }.getOrDefault(fallback)

/**
 * The sample's color scheme: dynamic colors on Android when [dynamicColor] is on and no
 * [accent] is set; a fixed [accent] (a hex color string) seeds the full scheme via
 * [sampleSeededScheme]; otherwise the default M3 scheme.
 *
 * @param tintFactor the neutral-tint factor as a fraction (e.g. 0.05 = 5%), controlling how
 *   strongly the seed hue tints the neutral roles.
 */
@Composable
internal expect fun sampleColorScheme(
    accent: String?,
    dark: Boolean,
    dynamicColor: Boolean,
    tintFactor: Float,
): ColorScheme

/**
 * Derives a full Material 3 [ColorScheme] from a fixed accent seed ([seed], an ARGB int).
 *
 * Seeding only `primary` (e.g. `darkColorScheme(primary = seed)`) leaves every other role on
 * the baseline palette, so components that read multiple roles (the switch knob is
 * `onPrimary`, the track `primary`) don't follow the accent. This maps the seed's HCT hue +
 * chroma to the standard M3 tones for the given theme, and tints the neutral roles
 * (background, surfaces, text) with the seed hue at [tintFactor] of its chroma, matching how
 * the dynamic (wallpaper-seeded) scheme looks.
 */
internal expect fun sampleSeededScheme(
    seed: Int,
    dark: Boolean,
    tintFactor: Float,
): ColorScheme
