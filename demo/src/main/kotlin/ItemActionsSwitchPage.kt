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

package net.slions.compose.toolkit.demo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import net.slions.compose.toolkit.Page
import net.slions.compose.toolkit.ItemActionsSwitch
import net.slions.compose.toolkit.CardStyle
import net.slions.compose.toolkit.group
import net.slions.compose.toolkit.item
import net.slions.compose.toolkit.section
import net.slions.compose.toolkit.rememberValue
import net.slions.compose.toolkit.itemActionsSwitch

/**
 * The [net.slions.compose.toolkit.ItemActionsSwitch] page: a row with a switch as the
 * second target, plus a click on the main target.
 */
fun itemActionsSwitchPage(): Page =
    Page(
        id = "two_target_switch",
        title = "Two target switch",
        summary = "Rows with a switch after a divider and a clickable main target.",
    ) {
        section(key = "tts_stateful_category", title = "Stateful")
        itemActionsSwitch(
            key = "tts_stateful",
            defaultValue = false,
            title = "Stateful",
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
            onClick = { },
        )
        itemActionsSwitch(
            key = "tts_stateful_disabled",
            defaultValue = true,
            title = "Stateful, disabled",
            enabled = { false },
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        itemActionsSwitch(
            key = "tts_stateful_icon",
            defaultValue = true,
            title = "Stateful, with icon",
            icon = { Icon(imageVector = Icons.Filled.Notifications, contentDescription = null) },
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        statefulRow(key = "tts_stateful_row", defaultValue = false) { value, onValueChange ->
            ItemActionsSwitch(
                value = value,
                onValueChange = onValueChange,
                title = "Sample's stateful row",
                summary = if (value) "On" else "Off",
                onClick = { },
            )
        }
        section(key = "tts_value_category", title = "Value-based")
        itemActionsSwitch(
            key = "tts_static",
            value = true,
            onValueChange = {},
            title = "Static",
            summary = "A fixed description.",
        )
        itemActionsSwitch(
            key = "tts_switch_disabled",
            value = true,
            onValueChange = {},
            title = "Disabled switch",
            switchEnabled = false,
            summary = "The switch is disabled, the row is not.",
        )
        itemActionsSwitch(
            key = "tts_disabled",
            value = true,
            onValueChange = {},
            title = "Disabled row",
            enabled = false,
            summary = "The whole row is disabled.",
        )
        section(key = "tts_cards_category", title = "Cards")
        item(key = "tts_card", title = "Card two-target switch", summary = "Static row drawn in a card.", style = CardStyle.Filled)
        group {
            item(title = "Card group two-target switch", summary = "On/Off") {
                val state = rememberValue("tts_group_state", false)
                val value by state
                ItemActionsSwitch(
                    value = value,
                    onValueChange = { state.value = it },
                    title = "Card group two-target switch",
                    summary = if (value) "On" else "Off",
                    onClick = { },
                )
            }
            item(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.toolkit.Item(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }
