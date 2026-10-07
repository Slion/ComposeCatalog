/*
 * Copyright 2026 Stéphane Lenclud
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import net.slions.compose.preference.PreferencePageScreen
import net.slions.compose.preference.ProvidePreferenceLocals
import net.slions.compose.preference.ProvidePreferenceTheme

@Composable
actual fun SampleApp() {
    // The theme flow is the single source of truth; the default flow persists it to disk.
    ProvidePreferenceLocals {
        val (themeValues, writeThemeValues) = rememberSampleThemeValues()
        val darkTheme = effectiveDarkTheme(themeValues.themeMode, isSystemInDarkTheme())
        CompositionLocalProvider(LocalSampleThemeValues provides themeValues) {
            SampleTheme(darkTheme = darkTheme) {
                // Re-provide the preference theme *inside* SampleTheme: its default colors
                // (title/summary/icon, drawn from MaterialTheme.colorScheme) are otherwise
                // captured once, against the outer light scheme, and would not follow the
                // live light/dark switch.
                ProvidePreferenceTheme {
                    PreferencePageScreen(
                        title = SampleTitle,
                        // The common sample pages plus a root row that opens the same tree
                        // in a bottom sheet (Android-only, so it is added here).
                        pages =
                            samplePages(themeValues, writeThemeValues) +
                                listOf(sheetSettingsPage()),
                    )
                }
            }
        }
    }
}
