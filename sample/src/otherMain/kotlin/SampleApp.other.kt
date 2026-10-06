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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import net.slions.compose.preference.ProvidePreferenceLocals
import net.slions.compose.preference.preferenceCategory

/**
 * The non-Android fallback: [PreferencePageScreen] is Android-only, so the pages are shown
 * as a single flat list.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
actual fun SampleApp() {
    // The theme flow is the single source of truth; the default flow persists it to disk.
    ProvidePreferenceLocals {
        val (themeValues, writeThemeValues) = rememberSampleThemeValues()
        val darkTheme = effectiveDarkTheme(themeValues.themeMode, isSystemInDarkTheme())
        CompositionLocalProvider(LocalSampleThemeValues provides themeValues) {
            SampleTheme(darkTheme = darkTheme) {
                Scaffold(
                    topBar = { TopAppBar(title = { Text(text = SampleTitle) }) },
                ) { contentPadding ->
                    val pages = samplePages(themeValues, writeThemeValues)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                    ) {
                        pages.forEach { page ->
                            preferenceCategory(
                                key = "page_${page.id}",
                                title = page.title,
                            )
                            page.content(this)
                        }
                    }
                }
            }
        }
    }
}
