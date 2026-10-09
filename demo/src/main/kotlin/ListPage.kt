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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.AnnotatedString
import net.slions.compose.catalog.ItemList
import net.slions.compose.catalog.ItemListType
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.itemList
import net.slions.compose.catalog.card
import net.slions.compose.catalog.cardGroup
import net.slions.compose.catalog.section
import net.slions.compose.catalog.rememberValue

/** The [net.slions.compose.catalog.ItemList] page: alert dialogs and dropdowns. */
@Composable
fun listPage(): Page =
    Page(
        id = "list",
        title = "List",
        summary = "Alert dialogs, dropdown menus, and custom value rendering.",
    ) {
        section(key = "list_stateful_category", title = "Stateful")
        itemList(
            key = "list_alert",
            defaultValue = "Alpha",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Alert dialog",
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        itemList(
            key = "list_dropdown",
            defaultValue = "Alpha",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Dropdown menu",
            type = ItemListType.DROPDOWN_MENU,
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        itemList(
            key = "list_icon",
            defaultValue = "Beta",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.List, contentDescription = null) },
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        itemList(
            key = "list_disabled",
            defaultValue = "Alpha",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Disabled",
            enabled = { false },
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        itemList(
            key = "list_value_to_text",
            defaultValue = 1,
            values = listOf(1, 2, 3),
            title = "Custom valueToText",
            valueToText = { AnnotatedString("Value $it") },
            summary = { "Selected: Value $it" },
            staticSummary = "Value 1, Value 2, Value 3",
        )
        statefulRow(key = "list_stateful_row", defaultValue = "Beta") { value, onValueChange ->
            ItemList(
                value = value,
                onValueChange = onValueChange,
                values = listOf("Alpha", "Beta", "Canary"),
                title = "Sample's stateful row",
                summary = "Selected: $value",
            )
        }
        section(key = "list_value_category", title = "Value-based")
        itemList(
            key = "list_value",
            value = "Canary",
            onValueChange = {},
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Static",
            summary = "Selected: Canary",
        )
        itemList(
            key = "list_value_dropdown",
            value = "Beta",
            onValueChange = {},
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Static dropdown",
            type = ItemListType.DROPDOWN_MENU,
            summary = "Selected: Beta",
        )
        section(key = "list_cards_category", title = "Cards")
        card(key = "list_card") {
            item(
                title = "Card list",
                summary = "A static row inside a real card.",
            )
        }
        cardGroup {
            card(title = "Card group list", summary = "Alpha, Beta, Canary") {
                val state = rememberValue("list_group_state", "Canary")
                val value by state
                ItemList(
                    value = value,
                    onValueChange = { state.value = it },
                    values = listOf("Alpha", "Beta", "Canary"),
                    title = "Card group list",
                    summary = "Selected: $value",
                )
            }
            card(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.catalog.Item(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }
