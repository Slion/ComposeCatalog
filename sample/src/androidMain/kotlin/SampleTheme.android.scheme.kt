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

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

@Composable
internal actual fun sampleColorScheme(
    accent: String?,
    dark: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    val seedArgb = accent?.let { hex ->
        runCatching {
            val color = Color(android.graphics.Color.parseColor(hex))
            color.toArgb()
        }.getOrNull()
    }
    return when {
        // No fixed accent: the platform dynamic (wallpaper) colors on Android 12+.
        seedArgb == null && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        seedArgb != null -> sampleSeededScheme(seedArgb, dark)
        else -> sampleDefaultScheme(dark)
    }
}

internal actual fun sampleSeededScheme(seed: Int, dark: Boolean): ColorScheme {
    val base = sampleDefaultScheme(dark)
    val hct = FloatArray(3)
    ColorUtils.colorToM3HCT(seed, hct)
    val hue = hct[0]
    val chroma = hct[1]
    fun t(tone: Int) = Color(ColorUtils.M3HCTToColor(hue, chroma, tone.toFloat()))
    // Neutral roles: the seed's hue at a small fraction of its chroma (Material tints its
    // dynamic neutral palette at 0.05).
    fun n(tone: Int) = Color(ColorUtils.M3HCTToColor(hue, chroma * 0.05f, tone.toFloat()))
    // The M3 tonal palette assumes a vivid seed. With a *dark* seed (e.g. #386A20, lightness
    // ~0.4) the dark-mode tone 80 lands near-black and vanishes against the dark surface — the
    // slider's active track disappears. Floor the primary's lightness (hue + chroma kept).
    val primary = if (dark) {
        val hct2 = FloatArray(3)
        ColorUtils.colorToM3HCT(ColorUtils.M3HCTToColor(hue, chroma, 80f), hct2)
        Color(ColorUtils.M3HCTToColor(hct2[0], hct2[1], hct2[2].coerceAtLeast(0.45f)))
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
