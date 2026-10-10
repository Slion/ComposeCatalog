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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import net.slions.compose.catalog.CardGroupScope
import net.slions.compose.catalog.Item
import net.slions.compose.catalog.ItemFooter
import net.slions.compose.catalog.ItemSwitch
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.cardGroup
import net.slions.compose.catalog.rememberValue
import net.slions.compose.catalog.section

const val SampleTitle = "Compose Catalog"

/**
 * A page card: a page row drawn in its own card, exactly like any other card row (the page
 * row's defaults provide the chevron and the navigation). With [onClick] the action
 * replaces the navigation (the row's action icon becomes open-in-new) and is also recorded
 * on the page reference.
 */
private fun CardGroupScope.pageCard(page: Page, onClick: (() -> Unit)? = null) {
    card(page = page, onClick = onClick) {
        Item(
            title = page.title,
            page = page,
            icon = page.icon,
            summary = page.summary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
        )
    }
}

/**
 * The root page of the sample tree, shared by every sample host (the full-screen screen
 * and the bottom sheet): a card group with one page row per item type (each exercising the
 * type's various configurations), the root's own preferences below them, and two action
 * rows at the end.
 *
 * The action rows carry an [item] action that replaces navigation — their trailing action
 * icon is the open-in-new (not the page-row chevron) accordingly: the Settings row opens
 * the theme in a separate activity hosting its own [net.slions.compose.catalog.Catalog]
 * (its own page tree and a separate search scope, yet the same process-wide store), and
 * the Sheet row opens this very tree in a bottom sheet in the current activity. The live
 * "Theme" page is not part of this tree at all: it is hosted only by that activity.
 *
 * @param onOpenSettings Invoked when the Settings row is tapped, to start the settings
 *   activity.
 * @param onOpenSheet Invoked when the Sheet row is tapped, to show the bottom sheet.
 */
@Composable
fun sampleRootPage(
    onOpenSettings: () -> Unit,
    onOpenSheet: () -> Unit,
): Page {
    // The page builders are plain functions (a page is plain data), so any theme values
    // or state they need are resolved here, in the composable scope.
    val colorScheme = MaterialTheme.colorScheme
    val elevatedElevation = CardDefaults.elevatedCardElevation()
    val sliderState = remember { mutableFloatStateOf(0.75f) }
    val radioGroup1 = rememberSaveable { mutableStateOf("a") }
    val radioGroup3 = rememberSaveable { mutableStateOf("c") }
    return Page(id = "root", title = SampleTitle) {
        section(key = "key_paged", title = "Pages")
        cardGroup(key = "pages_group") {
            pageCard(itemPage(colorScheme, elevatedElevation))
            pageCard(checkboxPage())
            pageCard(switchPage())
            pageCard(sliderPage(sliderState))
            pageCard(listPage())
            pageCard(multiSelectListPage())
            pageCard(textFieldPage())
            pageCard(radioButtonPage(radioGroup1, radioGroup3))
            pageCard(footerPage())
            pageCard(itemActionsPage())
            pageCard(itemActionIconButtonPage())
            pageCard(itemActionsSwitchPage())
            pageCard(nestedPage())
        }
        section(key = "section_others", title = "Others")
        cardGroup(key = "actions_group") {
            // Action pages: their rows carry an action, so they are never navigated to
            // and their content stays empty.
            pageCard(
                page =
                    Page(
                        id = "settings_activity",
                        title = "Settings",
                        summary = "A second catalog: its own tree and search, sharing this app's store.",
                    ) {},
                onClick = onOpenSettings,
            )
            pageCard(
                page =
                    Page(
                        id = "sheet_settings",
                        title = "Bottom sheet",
                        summary = "This settings tree inside a bottom sheet.",
                    ) {},
                onClick = onOpenSheet,
            )
        }
    }
}
