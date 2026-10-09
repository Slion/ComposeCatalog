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
import dev.vicart.compose.material.symbols.MaterialSymbol
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.item
import net.slions.compose.catalog.itemFooter
import net.slions.compose.catalog.section
import net.slions.compose.catalog.itemSwitch

const val SampleTitle = "Compose Catalog"

/**
 * The sample pages: one page per item type, each exercising the type's various
 * configurations. The live "Theme" page is hosted by a separate activity with its own
 * catalog (see [net.slions.compose.catalog.demo.SettingsActivity]). Card groups and
 * sections are used throughout the pages, so they have no page of their own. Hosted by
 * [net.slions.compose.catalog.Catalog].
 */
@Composable
fun samplePages(): List<Page> =
    listOf(
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
 * A root row that opens the theme settings in a separate activity hosting its own
 * [net.slions.compose.catalog.Catalog]. It demonstrates that one app can host several
 * independent catalogs: the settings activity's catalog has its own page tree and a
 * separate search scope, yet shares the same process-wide store as this one.
 *
 * @param onOpen Invoked when the row is tapped, to start the settings activity.
 */
fun LazyListScope.settingsRootRow(onOpen: () -> Unit) {
    item(
        key = "settings_row",
        title = "Settings",
        summary = "A second catalog: its own tree and search, sharing this app's store.",
        icon = { MaterialSymbol.Outlined(icon = "open_in_new") },
        onClick = onOpen,
    )
}

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
