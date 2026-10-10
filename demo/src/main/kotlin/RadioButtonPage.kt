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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import net.slions.compose.toolkit.Page
import net.slions.compose.toolkit.ItemRadio
import net.slions.compose.toolkit.CardStyle
import net.slions.compose.toolkit.group
import net.slions.compose.toolkit.item
import net.slions.compose.toolkit.section
import net.slions.compose.toolkit.itemRadio
import androidx.compose.runtime.MutableState
import net.slions.compose.toolkit.rememberValue

/**
 * The [net.slions.compose.toolkit.ItemRadio] page. The rows have no icon of their
 * own (the radio button is the icon); selection is held per group (by the lazy builder where
 * possible, and with [rememberSaveable] in the composable card group rows).
 */
fun radioButtonPage(
    group1: MutableState<String>,
    group3: MutableState<String>,
): Page {
    val selected1 by group1
    val selected3 by group3
    return Page(
        id = "radio_button",
        title = "Radio button",
        summary = "Selected, unselected, and disabled rows, grouped per card.",
    ) {
        section(key = "radio_basic_category", title = "Basics")
        itemRadio(
            key = "radio_selected",
            selected = selected1 == "a",
            title = "Selected",
            summary = "The selected row of the group.",
            onClick = { group1.value = "a" },
        )
        itemRadio(
            key = "radio_unselected",
            selected = selected1 == "b",
            title = "Unselected",
            summary = "Tap to select this row instead.",
            onClick = { group1.value = "b" },
        )
        itemRadio(
            key = "radio_disabled",
            selected = false,
            title = "Disabled",
            enabled = false,
            summary = "Not selectable.",
            onClick = {},
        )
        statefulRow(key = "radio_stateful_row", defaultValue = "x") { value, onValueChange ->
            val group2 = rememberSaveable { mutableStateOf("x") }
            val selected2 by group2
            ItemRadio(
                selected = selected2 == value,
                title = "Sample's stateful row (${value})",
                summary = "Tapping switches the group to this row.",
                onClick = { onValueChange(value); group2.value = value },
            )
        }
        section(key = "radio_cards_category", title = "Cards")
        item(
            key = "radio_card",
            title = "Card radio group",
            summary = "Radio rows rendered as card rows.",
            style = CardStyle.Filled,
        )
        group {
            item(title = "Card group option 1", summary = "Selected option.") {
                ItemRadio(
                    selected = selected3 == "c",
                    title = "Card group option 1",
                    summary = "Selected option.",
                    onClick = { group3.value = "c" },
                )
            }
            item(title = "Card group option 2", summary = "Unselected option.") {
                ItemRadio(
                    selected = selected3 == "d",
                    title = "Card group option 2",
                    summary = "Unselected option.",
                    onClick = { group3.value = "d" },
                )
            }
        }
    }
}
