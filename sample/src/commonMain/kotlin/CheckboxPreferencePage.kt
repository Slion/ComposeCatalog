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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import net.slions.compose.preference.CheckboxPreference
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.checkboxPreference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory
import net.slions.compose.preference.rememberPreferenceState

/** The [net.slions.compose.preference.CheckboxPreference] page: stateful and value-based. */
@Composable
fun checkboxPreferencePage(): PreferencePage =
    PreferencePage(
        id = "checkbox",
        title = "Checkbox",
        summary = "Stateful builders, dynamic summaries, and disabled rows.",
    ) {
        preferenceCategory(key = "cb_stateful_category", title = "Stateful")
        checkboxPreference(
            key = "cb_stateful",
            defaultValue = false,
            title = "Stateful",
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        checkboxPreference(
            key = "cb_stateful_disabled",
            defaultValue = true,
            title = "Stateful, disabled",
            enabled = { false },
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        checkboxPreference(
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
            CheckboxPreference(
                value = value,
                onValueChange = onValueChange,
                title = "Sample's stateful row",
                summary = if (value) "On" else "Off",
            )
        }
        preferenceCategory(key = "cb_value_category", title = "Value-based")
        checkboxPreference(
            key = "cb_static",
            value = false,
            onValueChange = {},
            title = "Static summary",
            summary = "A fixed description.",
        )
        checkboxPreference(
            key = "cb_static_override",
            value = true,
            onValueChange = {},
            title = "staticSummary override",
            summary = "This summary is not shown in the index.",
            staticSummary = "staticSummary is what search sees.",
        )
        checkboxPreference(
            key = "cb_icon",
            value = true,
            onValueChange = {},
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            summary = "The leading icon slot.",
        )
        checkboxPreference(
            key = "cb_disabled",
            value = true,
            onValueChange = {},
            title = "Disabled",
            enabled = false,
            summary = "Not toggleable.",
        )
        preferenceCategory(key = "cb_cards_category", title = "Cards")
        preferenceCard(key = "cb_card") {
            preference(title = "Card checkbox", summary = "Static row inside a real card.")
        }
        preferenceCardGroup {
            card(title = "Card group checkbox", summary = "A stateful row inside a card group.") {
                val state = rememberPreferenceState("cb_group_state", true)
                val value by state
                CheckboxPreference(
                    value = value,
                    onValueChange = { state.value = it },
                    title = "Card group checkbox",
                    summary = "A stateful row inside a card group.",
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
