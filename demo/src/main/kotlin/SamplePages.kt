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
import net.slions.compose.catalog.Item
import net.slions.compose.catalog.ItemFooter
import net.slions.compose.catalog.ItemSwitch
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.cardGroup
import net.slions.compose.catalog.rememberValue
import net.slions.compose.catalog.section

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
 * The preferences hosted at the root of the sample tree, below the top-level page rows (via
 * [net.slions.compose.catalog.Catalog.rootContent]): the same builders as any
 * page's content, showing that the root level hosts regular preferences too. Shared by every
 * sample host (the full-screen screen and the bottom sheet).
 *
 * The last card opens the theme settings in a separate activity hosting its own
 * [net.slions.compose.catalog.Catalog], demonstrating that one app can host several
 * independent catalogs: that activity's catalog has its own page tree and a separate
 * search scope, yet shares the same process-wide store as this one. Its trailing action
 * icon is [open_in_new] (not the page-row chevron): the tap launches an activity rather
 * than navigating the tree.
 *
 * @param onOpenSettings Invoked when the Settings card is tapped, to start the settings
 *   activity.
 */
fun LazyListScope.sampleRootContent(onOpenSettings: () -> Unit) {
    section(key = "root_category", title = "Root page")
    cardGroup(key = "root_group") {
        card(
            title = "Root switch",
            summary = "On",
        ) {
            val state = rememberValue(key = "root_switch", defaultValue = true)
            ItemSwitch(
                state = state,
                title = "Root switch",
                summary = { if (it) "On" else "Off" },
            )
        }
        card(
            title = "Root footer",
            summary = "The root level hosts any preference, like a page.",
        ) {
            ItemFooter(summary = "The root level hosts any preference, like a page.")
        }
        card(
            title = "Settings",
            summary = "A second catalog: its own tree and search, sharing this app's store.",
        ) {
            Item(
                title = "Settings",
                summary = "A second catalog: its own tree and search, sharing this app's store.",
                actionIcon = { MaterialSymbol.Outlined(icon = "open_in_new") },
                onClick = onOpenSettings,
            )
        }
    }
}
