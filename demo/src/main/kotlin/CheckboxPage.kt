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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import net.slions.compose.catalog.ItemCheckbox
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.itemCheckbox
import net.slions.compose.catalog.card
import net.slions.compose.catalog.cardGroup
import net.slions.compose.catalog.section
import net.slions.compose.catalog.rememberValue

/** The [net.slions.compose.catalog.ItemCheckbox] page: stateful and value-based. */
fun checkboxPage(): Page =
    Page(
        id = "checkbox",
        title = "Checkbox",
        summary = "Stateful builders, dynamic summaries, and disabled rows.",
    ) {
        section(key = "cb_stateful_category", title = "Stateful")
        itemCheckbox(
            key = "cb_stateful",
            defaultValue = false,
            title = "Stateful",
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        itemCheckbox(
            key = "cb_stateful_disabled",
            defaultValue = true,
            title = "Stateful, disabled",
            enabled = { false },
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        itemCheckbox(
            key = "cb_stateful_icon",
            defaultValue = true,
            title = "Stateful, icon follows the value",
            icon = {
                if (it) {
                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = null)
                } else {
                    Icon(imageVector = Icons.Outlined.Favorite, contentDescription = null)
                }
            },
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        statefulRow(key = "cb_stateful_row", defaultValue = false) { value, onValueChange ->
            ItemCheckbox(
                value = value,
                onValueChange = onValueChange,
                title = "Sample's stateful row",
                summary = if (value) "On" else "Off",
            )
        }
        section(key = "cb_value_category", title = "Value-based")
        itemCheckbox(
            key = "cb_static",
            value = false,
            onValueChange = {},
            title = "Static summary",
            summary = "A fixed description.",
        )
        itemCheckbox(
            key = "cb_static_override",
            value = true,
            onValueChange = {},
            title = "staticSummary override",
            summary = "This summary is not shown in the index.",
            staticSummary = "staticSummary is what search sees.",
        )
        itemCheckbox(
            key = "cb_icon",
            value = true,
            onValueChange = {},
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            summary = "The leading icon slot.",
        )
        itemCheckbox(
            key = "cb_disabled",
            value = true,
            onValueChange = {},
            title = "Disabled",
            enabled = false,
            summary = "Not toggleable.",
        )
        section(key = "cb_cards_category", title = "Cards")
        card(key = "cb_card") {
            item(title = "Card checkbox", summary = "Static row inside a real card.")
        }
        cardGroup {
            card(title = "Card group checkbox", summary = "A stateful row inside a card group.") {
                val state = rememberValue("cb_group_state", true)
                val value by state
                ItemCheckbox(
                    value = value,
                    onValueChange = { state.value = it },
                    title = "Card group checkbox",
                    summary = "A stateful row inside a card group.",
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
