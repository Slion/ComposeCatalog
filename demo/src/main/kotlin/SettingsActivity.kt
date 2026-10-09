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

package net.slions.compose.catalog.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import net.slions.compose.catalog.Catalog
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.ProvidePreferenceLocals
import net.slions.compose.catalog.ProvidePreferenceTheme
import net.slions.compose.catalog.item

/**
 * A second settings host: a dedicated activity running its own
 * [net.slions.compose.catalog.Catalog] that contains only the theme page.
 *
 * This exists to demonstrate that one app can host **several independent catalogs**. Each
 * [Catalog] builds its own [net.slions.compose.catalog.SearchIndex] from its own page tree,
 * so the search field here covers only the theme page — a separate search scope from the
 * main activity's catalog — while both catalogs read and write the same process-wide
 * [net.slions.compose.catalog.Store] (see [net.slions.compose.catalog.createDefaultStore]):
 * a change made here is persisted and picked up by the main activity on return.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsActivityScreen(onBack = { finish() })
        }
    }
}

@Composable
private fun SettingsActivityScreen(onBack: () -> Unit) {
    // The default flow is process-global, so this catalog and the main activity's catalog
    // observe the same store: the theme set here re-themes the main activity when it
    // returns, even though the two catalogs are otherwise unrelated.
    ProvidePreferenceLocals {
        val (themeValues, writeThemeValues) = rememberSampleThemeValues()
        val darkTheme = effectiveDarkTheme(themeValues.themeMode, isSystemInDarkTheme())
        // Resolved here (in the composable scope) and passed to the plain page builder.
        val defaultAccent = systemDefaultAccentColor(darkTheme)
        CompositionLocalProvider(LocalSampleThemeValues provides themeValues) {
            SampleTheme(darkTheme = darkTheme) {
                ProvidePreferenceTheme {
                    Catalog(
                        title = "Settings",
                        // A wrapper root holding the theme as its only page row: a
                        // childless root would leave the two-pane detail empty.
                        root =
                            Page(id = "settings_root", title = "Settings") {
                                item(
                                    page =
                                        themePage(
                                            values = themeValues,
                                            onValuesChange = writeThemeValues,
                                            dark = darkTheme,
                                            defaultAccent = defaultAccent,
                                        )
                                )
                            },
                        // This is a second surface, not the app's root: the title bar
                        // carries a back chevron and the system back at the root of the
                        // tree both close the activity.
                        onBack = onBack,
                        showBackButton = true,
                    )
                }
            }
        }
    }
}
