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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun sampleColorScheme(
    accent: String?,
    dark: Boolean,
    dynamicColor: Boolean,
    tintFactor: Float,
): ColorScheme {
    // No dynamic (wallpaper) colors off Android; a fixed accent seeds the scheme.
    val seed = accent?.let { parseHexArgb(it) }
    return seed?.let { sampleSeededScheme(it, dark, tintFactor) } ?: sampleDefaultScheme(dark)
}

internal actual fun sampleSeededScheme(seed: Int, dark: Boolean, tintFactor: Float): ColorScheme {
    val base = sampleDefaultScheme(dark)
    val hsl = hslOf(seed)
    val hue = hsl[0]
    // M3 tonal steps keep the seed hue and scale saturation with the tone; the lightness
    // roughly tracks the M3 tone value (tone/100).
    fun t(tone: Int): Color {
        val lightness = tone / 100f
        val saturation = (hsl[1] * 0.6f) * (lightness.coerceIn(0.1f, 0.9f))
        return hslColor(hue, saturation.coerceIn(0f, 0.9f), lightness)
    }
    // Neutral roles: the seed hue at a low saturation scaled by [tintFactor] (a soft tint of
    // the M3 neutrals; a higher factor pushes the accent into the neutrals).
    fun n(tone: Int): Color {
        val lightness = tone / 100f
        return hslColor(hue, (hsl[1] * tintFactor).coerceIn(0f, 0.9f), lightness)
    }
    // Floor the dark-mode primary so a dark seed doesn't vanish on the dark surface.
    val primary = if (dark) {
        val p = t(80)
        if (luminance(p) < 0.22f) hslColor(hue, hsl[1] * 0.6f, 0.45f) else p
    } else {
        t(40)
    }
    return base.copy(
        primary = primary,
        onPrimary = if (dark) t(20) else t(100),
        primaryContainer = if (dark) t(30) else t(90),
        onPrimaryContainer = if (dark) t(90) else t(10),
        inversePrimary = t(40),
        secondary = if (dark) t(80) else t(40),
        onSecondary = if (dark) t(20) else t(100),
        secondaryContainer = if (dark) t(30) else t(90),
        onSecondaryContainer = if (dark) t(90) else t(10),
        tertiary = if (dark) t(80) else t(40),
        onTertiary = if (dark) t(20) else t(100),
        tertiaryContainer = if (dark) t(30) else t(90),
        onTertiaryContainer = if (dark) t(90) else t(10),
        surfaceTint = primary,
        // Neutral family, at the standard M3 tones for the given theme.
        background = if (dark) n(10) else n(98),
        onBackground = if (dark) n(90) else n(10),
        surface = if (dark) n(6) else n(98),
        onSurface = if (dark) n(90) else n(10),
        surfaceVariant = if (dark) n(30) else n(90),
        onSurfaceVariant = if (dark) n(80) else n(30),
        outline = if (dark) n(60) else n(50),
        outlineVariant = if (dark) n(30) else n(80),
        inverseSurface = if (dark) n(90) else n(20),
        inverseOnSurface = if (dark) n(20) else n(95),
        surfaceContainerLowest = if (dark) n(4) else n(100),
        surfaceContainerLow = if (dark) n(10) else n(96),
        surfaceContainer = if (dark) n(12) else n(94),
        surfaceContainerHigh = if (dark) n(17) else n(92),
        surfaceContainerHighest = if (dark) n(22) else n(90),
    )
}

/** Parses `#RRGGBB` / `#AARRGGBB` hex to an ARGB int, or null if malformed. */
internal fun parseHexArgb(hex: String): Int? =
    runCatching {
        var h = hex.trim().removePrefix("#")
        if (h.length == 6) h = "FF$h"
        if (h.length == 8) h.toLong(16).toInt() else null
    }.getOrNull()

private fun hslOf(argb: Int): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    var h = 0f
    var s = 0f
    if (d > 0f) {
        s = d / if (l < 0.5f) (max + min) else (2f - max - min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } / 6f
    }
    return floatArrayOf(h, s, l)
}

private fun hslColor(h: Float, s: Float, l: Float): Color {
    fun channel(hp: Float, p: Float, q: Float): Float {
        var hh = hp
        if (hh < 0f) hh += 1f
        if (hh > 1f) hh -= 1f
        return when {
            hh < 1f / 6f -> p + (q - p) * 6f * hh
            hh < 1f / 2f -> q
            hh < 2f / 3f -> p + (q - p) * (2f / 3f - hh) * 6f
            else -> p
        }
    }
    val (p, q) = if (s == 0f) Pair(l, l) else {
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        Pair(p, q)
    }
    return Color(
        red = channel(h + 1f / 3f, p, q),
        green = channel(h, p, q),
        blue = channel(h - 1f / 3f, p, q),
    )
}

private fun luminance(color: Color): Float =
    0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
