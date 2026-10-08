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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.RadioButtonPreference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory
import net.slions.compose.preference.radioButtonPreference
import net.slions.compose.preference.rememberPreferenceState

/**
 * The [net.slions.compose.preference.RadioButtonPreference] page. The rows have no icon of their
 * own (the radio button is the icon); selection is held per group (by the lazy builder where
 * possible, and with [rememberSaveable] in the composable card group rows).
 */
@Composable
fun radioButtonPreferencePage(): PreferencePage {
    val group1 = rememberSaveable { mutableStateOf("a") }
    val selected1 by group1
    val group3 = rememberSaveable { mutableStateOf("c") }
    val selected3 by group3
    return PreferencePage(
        id = "radio_button",
        title = "Radio button",
        summary = "Selected, unselected, and disabled rows, grouped per card.",
    ) {
        preferenceCategory(key = "radio_basic_category", title = "Basics")
        radioButtonPreference(
            key = "radio_selected",
            selected = selected1 == "a",
            title = "Selected",
            summary = "The selected row of the group.",
            onClick = { group1.value = "a" },
        )
        radioButtonPreference(
            key = "radio_unselected",
            selected = selected1 == "b",
            title = "Unselected",
            summary = "Tap to select this row instead.",
            onClick = { group1.value = "b" },
        )
        radioButtonPreference(
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
            RadioButtonPreference(
                selected = selected2 == value,
                title = "Sample's stateful row (${value})",
                summary = "Tapping switches the group to this row.",
                onClick = { onValueChange(value); group2.value = value },
            )
        }
        preferenceCategory(key = "radio_cards_category", title = "Cards")
        preferenceCard(key = "radio_card") {
            preference(
                title = "Card radio group",
                summary = "Radio rows rendered as card rows.",
            )
        }
        preferenceCardGroup {
            card(title = "Card group option 1", summary = "Selected option.") {
                RadioButtonPreference(
                    selected = selected3 == "c",
                    title = "Card group option 1",
                    summary = "Selected option.",
                    onClick = { group3.value = "c" },
                )
            }
            card(title = "Card group option 2", summary = "Unselected option.") {
                RadioButtonPreference(
                    selected = selected3 == "d",
                    title = "Card group option 2",
                    summary = "Unselected option.",
                    onClick = { group3.value = "d" },
                )
            }
        }
    }
}
