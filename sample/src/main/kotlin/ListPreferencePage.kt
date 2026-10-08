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
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.AnnotatedString
import net.slions.compose.preference.ListPreference
import net.slions.compose.preference.ListPreferenceType
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.listPreference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory
import net.slions.compose.preference.rememberPreferenceState

/** The [net.slions.compose.preference.ListPreference] page: alert dialogs and dropdowns. */
@Composable
fun listPreferencePage(): PreferencePage =
    PreferencePage(
        id = "list",
        title = "List",
        summary = "Alert dialogs, dropdown menus, and custom value rendering.",
    ) {
        preferenceCategory(key = "list_stateful_category", title = "Stateful")
        listPreference(
            key = "list_alert",
            defaultValue = "Alpha",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Alert dialog",
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        listPreference(
            key = "list_dropdown",
            defaultValue = "Alpha",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Dropdown menu",
            type = ListPreferenceType.DROPDOWN_MENU,
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        listPreference(
            key = "list_icon",
            defaultValue = "Beta",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.List, contentDescription = null) },
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        listPreference(
            key = "list_disabled",
            defaultValue = "Alpha",
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Disabled",
            enabled = { false },
            summary = { "Selected: $it" },
            staticSummary = "Alpha, Beta, Canary",
        )
        listPreference(
            key = "list_value_to_text",
            defaultValue = 1,
            values = listOf(1, 2, 3),
            title = "Custom valueToText",
            valueToText = { AnnotatedString("Value $it") },
            summary = { "Selected: Value $it" },
            staticSummary = "Value 1, Value 2, Value 3",
        )
        statefulRow(key = "list_stateful_row", defaultValue = "Beta") { value, onValueChange ->
            ListPreference(
                value = value,
                onValueChange = onValueChange,
                values = listOf("Alpha", "Beta", "Canary"),
                title = "Sample's stateful row",
                summary = "Selected: $value",
            )
        }
        preferenceCategory(key = "list_value_category", title = "Value-based")
        listPreference(
            key = "list_value",
            value = "Canary",
            onValueChange = {},
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Static",
            summary = "Selected: Canary",
        )
        listPreference(
            key = "list_value_dropdown",
            value = "Beta",
            onValueChange = {},
            values = listOf("Alpha", "Beta", "Canary"),
            title = "Static dropdown",
            type = ListPreferenceType.DROPDOWN_MENU,
            summary = "Selected: Beta",
        )
        preferenceCategory(key = "list_cards_category", title = "Cards")
        preferenceCard(key = "list_card") {
            preference(
                title = "Card list",
                summary = "A static row inside a real card.",
            )
        }
        preferenceCardGroup {
            card(title = "Card group list", summary = "Alpha, Beta, Canary") {
                val state = rememberPreferenceState("list_group_state", "Canary")
                val value by state
                ListPreference(
                    value = value,
                    onValueChange = { state.value = it },
                    values = listOf("Alpha", "Beta", "Canary"),
                    title = "Card group list",
                    summary = "Selected: $value",
                )
            }
            card(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.preference.Preference(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }
