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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.AnnotatedString
import net.slions.compose.preference.ItemMultiSelectList
import net.slions.compose.preference.Page
import net.slions.compose.preference.itemMultiSelectList
import net.slions.compose.preference.card
import net.slions.compose.preference.cardGroup
import net.slions.compose.preference.section
import net.slions.compose.preference.rememberValue

/** The [net.slions.compose.preference.ItemMultiSelectList] page. */
@Composable
fun multiSelectListPage(): Page =
    Page(
        id = "multi_select",
        title = "Multi-select list",
        summary = "Set-valued rows: dynamic summaries and custom rendering.",
    ) {
        section(key = "msl_stateful_category", title = "Stateful")
        itemMultiSelectList(
            key = "msl_stateful",
            defaultValue = setOf("Alpha", "Beta"),
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Stateful",
            summary = { it.sorted().joinToString(", ").ifEmpty { "None selected" } },
            staticSummary = "Alpha, Beta, Canary",
        )
        itemMultiSelectList(
            key = "msl_empty",
            defaultValue = emptySet(),
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Empty selection",
            summary = { it.sorted().joinToString(", ").ifEmpty { "None selected" } },
            staticSummary = "Alpha, Beta, Canary",
        )
        itemMultiSelectList(
            key = "msl_icon",
            defaultValue = setOf("Canary"),
            values = listOf("Alpha", "Beta", "Canary"),
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.Check, contentDescription = null) },
            summary = { "${it.size} selected" },
            staticSummary = "Count of selected items",
        )
        itemMultiSelectList(
            key = "msl_disabled",
            defaultValue = setOf("Alpha"),
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Disabled",
            enabled = { false },
            summary = { "${it.size} selected" },
            staticSummary = "Count of selected items",
        )
        itemMultiSelectList(
            key = "msl_value_to_text",
            defaultValue = setOf(1),
            values = listOf(1, 2, 3),
            title = "Custom valueToText",
            valueToText = { AnnotatedString("Value $it") },
            summary = { s -> if (s.isEmpty()) "None selected" else s.joinToString(", ") { v -> "Value $v" } },
            staticSummary = "Value 1, Value 2, Value 3",
        )
        statefulRow(key = "msl_stateful_row", defaultValue = setOf<String>("Beta")) {
            value, onValueChange ->
            ItemMultiSelectList(
                value = value,
                onValueChange = onValueChange,
                values = listOf("Alpha", "Beta", "Canary"),
                title = "Sample's stateful row",
                summary = value.sorted().joinToString(", ").ifEmpty { "None selected" },
            )
        }
        section(key = "msl_value_category", title = "Value-based")
        itemMultiSelectList(
            key = "msl_value",
            value = setOf("Alpha", "Canary"),
            onValueChange = {},
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Static",
            summary = "Alpha, Canary",
        )
        section(key = "msl_cards_category", title = "Cards")
        card(key = "msl_card") {
            item(title = "Card multi-select", summary = "Static row inside a real card.")
        }
        cardGroup {
            card(title = "Card group multi-select", summary = "Alpha, Beta, Canary") {
                val state = rememberValue("msl_group_state", setOf<String>("Alpha"))
                val value by state
                ItemMultiSelectList(
                    value = value,
                    onValueChange = { state.value = it },
                    values = listOf("Alpha", "Beta", "Canary"),
                    title = "Card group multi-select",
                    summary = value.sorted().joinToString(", ").ifEmpty { "None selected" },
                )
            }
            card(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.preference.Item(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }
