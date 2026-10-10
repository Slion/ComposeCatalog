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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.ItemSwitch
import net.slions.compose.catalog.CardStyle
import net.slions.compose.catalog.group
import net.slions.compose.catalog.item
import net.slions.compose.catalog.section
import net.slions.compose.catalog.rememberValue
import net.slions.compose.catalog.itemSwitch

/** The [net.slions.compose.catalog.ItemSwitch] page: stateful and value-based. */
fun switchPage(): Page =
    Page(
        id = "switch",
        title = "Switch",
        summary = "Stateful builders, dynamic summaries, and disabled rows.",
    ) {
        section(key = "sw_stateful_category", title = "Stateful")
        itemSwitch(
            key = "sw_stateful",
            defaultValue = false,
            title = "Stateful",
            summary = { if (it) "Notifications on" else "Notifications off" },
            staticSummary = "Notifications",
        )
        itemSwitch(
            key = "sw_stateful_disabled",
            defaultValue = true,
            title = "Stateful, disabled",
            enabled = { false },
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        itemSwitch(
            key = "sw_stateful_icon",
            defaultValue = true,
            title = "Stateful, with icon",
            icon = { Icon(imageVector = Icons.Filled.Notifications, contentDescription = null) },
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        statefulRow(key = "sw_stateful_row", defaultValue = true) { value, onValueChange ->
            ItemSwitch(
                value = value,
                onValueChange = onValueChange,
                title = "Sample's stateful row",
                summary = if (value) "On" else "Off",
            )
        }
        section(key = "sw_value_category", title = "Value-based")
        itemSwitch(
            key = "sw_static",
            value = true,
            onValueChange = {},
            title = "Static summary",
            summary = "A fixed description.",
        )
        itemSwitch(
            key = "sw_static_override",
            value = false,
            onValueChange = {},
            title = "staticSummary override",
            summary = "This summary is not shown in the index.",
            staticSummary = "staticSummary is what search sees.",
        )
        itemSwitch(
            key = "sw_disabled",
            value = true,
            onValueChange = {},
            title = "Disabled",
            enabled = false,
            summary = "Not toggleable.",
        )
        section(key = "sw_cards_category", title = "Cards")
        item(key = "sw_card", title = "Card switch", summary = "Static row drawn in a card.", style = CardStyle.Filled)
        group {
            item(title = "Card group switch", summary = "A stateful row inside a card group.") {
                val state = rememberValue("sw_group_state", true)
                val value by state
                ItemSwitch(
                    value = value,
                    onValueChange = { state.value = it },
                    title = "Card group switch",
                    summary = "A stateful row inside a card group.",
                )
            }
            item(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.catalog.Item(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }
