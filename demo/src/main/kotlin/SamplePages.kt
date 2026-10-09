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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.itemFooter
import net.slions.compose.catalog.section
import net.slions.compose.catalog.itemSwitch

const val SampleTitle = "Compose Preference"

/**
 * The sample pages: the live "Theme" page first, then one page per preference type, each
 * exercising the type's various configurations. Card groups and categories are used
 * throughout the pages, so they have no page of their own. Hosted by
 * [net.slions.compose.catalog.Catalog].
 *
 * @param themeValues the current [SampleThemeValues], applied to the app by [SampleTheme].
 * @param onThemeValuesChange invoked with the next [SampleThemeValues] when the Theme page
 *   changes a setting.
 */
@Composable
fun samplePages(
    themeValues: SampleThemeValues,
    onThemeValuesChange: (SampleThemeValues) -> Unit,
): List<Page> =
    listOf(
        themePage(themeValues, onThemeValuesChange),
        itemPage(),
        checkboxPage(),
        switchPage(),
        sliderPage(),
        listPage(),
        multiSelectListPage(),
        textFieldPage(),
        radioButtonPage(),
        footerPage(),
        itemActionsPage(),
        itemActionIconButtonPage(),
        itemActionsSwitchPage(),
        nestedPage(),
    )

/**
 * The preferences hosted at the root of the sample tree, below the top-level page rows (via
 * [net.slions.compose.catalog.Catalog.rootContent]): the same builders as any
 * page's content, showing that the root level hosts regular preferences too. Shared by every
 * sample host (the full-screen screen and the bottom sheet).
 */
fun LazyListScope.sampleRootContent() {
    section(key = "root_category", title = "Root page")
    itemSwitch(
        key = "root_switch",
        defaultValue = true,
        title = "Root switch",
        summary = { if (it) "On" else "Off" },
    )
    itemFooter(
        key = "root_footer",
        title = "Root footer",
        summary = "The root level hosts any preference, like a page.",
    )
}
