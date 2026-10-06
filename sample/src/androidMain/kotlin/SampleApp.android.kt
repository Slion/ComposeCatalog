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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import net.slions.compose.preference.PreferencePageScreen
import net.slions.compose.preference.ProvidePreferenceLocals

@Composable
actual fun SampleApp() {
    var themeValues by remember { mutableStateOf(SampleThemeValues()) }
    val darkTheme = effectiveDarkTheme(themeValues.themeMode, isSystemInDarkTheme())
    CompositionLocalProvider(LocalSampleThemeValues provides themeValues) {
        SampleTheme(darkTheme = darkTheme) {
            ProvidePreferenceLocals {
                PreferencePageScreen(
                    title = SampleTitle,
                    pages = samplePages(themeValues) { themeValues = it },
                )
            }
        }
    }
}
