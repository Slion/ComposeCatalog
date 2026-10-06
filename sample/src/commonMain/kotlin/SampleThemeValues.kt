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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
 * @param fontFamily one of [FONT_FAMILIES]; null = [DEFAULT_FONT].
 */
@Immutable
data class SampleThemeValues(
    val themeMode: SampleThemeMode = SampleThemeMode.SYSTEM,
    val accent: String? = null,
    val cornerRadiusDp: Int? = null,
    val fontSizePercent: Int? = null,
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
 * The selectable accent colors, shown as the "Accent color" row. A null [hex] means the
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
 */
@Composable
internal expect fun sampleColorScheme(
    accent: String?,
    dark: Boolean,
    dynamicColor: Boolean,
): ColorScheme

/**
 * Derives a full Material 3 [ColorScheme] from a fixed accent seed ([seed], an ARGB int).
 *
 * Seeding only `primary` (e.g. `darkColorScheme(primary = seed)`) leaves every other role on
 * the baseline palette, so components that read multiple roles (the switch knob is
 * `onPrimary`, the track `primary`) don't follow the accent. This maps the seed's HCT hue +
 * chroma to the standard M3 tones for the given theme, and tints the neutral roles
 * (background, surfaces, text) with the seed hue at a small fraction of its chroma, matching
 * how the dynamic (wallpaper-seeded) scheme looks.
 */
internal expect fun sampleSeededScheme(seed: Int, dark: Boolean): ColorScheme
