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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.TextFieldPreference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory
import net.slions.compose.preference.rememberPreferenceState
import net.slions.compose.preference.textFieldPreference

/** The [net.slions.compose.preference.TextFieldPreference] page: string and numeric values. */
@Composable
fun textFieldPreferencePage(): PreferencePage =
    PreferencePage(
        id = "text_field",
        title = "Text field",
        summary = "String and numeric values, custom text fields, and disabled rows.",
    ) {
        preferenceCategory(key = "tf_stateful_category", title = "Stateful")
        textFieldPreference(
            key = "tf_string",
            defaultValue = "Sample",
            title = "String",
            textToValue = { it },
            summary = { "Value: $it" },
            staticSummary = "A text value",
        )
        textFieldPreference(
            key = "tf_number",
            defaultValue = 42,
            title = "Number",
            textToValue = { it.toIntOrNull() },
            summary = { "Value: $it" },
            staticSummary = "An integer value",
        )
        textFieldPreference(
            key = "tf_icon",
            defaultValue = "Sample",
            title = "With icon",
            textToValue = { it },
            icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
            summary = { "Value: $it" },
            staticSummary = "A text value",
        )
        textFieldPreference(
            key = "tf_disabled",
            defaultValue = "Sample",
            title = "Disabled",
            textToValue = { it },
            enabled = { false },
            summary = { "Value: $it" },
            staticSummary = "A text value",
        )
        textFieldPreference(
            key = "tf_password",
            defaultValue = "secret",
            title = "Password field",
            textToValue = { it },
            valueToText = { it },
            summary = { "(hidden) ${it.length} characters" },
            staticSummary = "A hidden value",
            textField = { value, onValueChange, onOk ->
                androidx.compose.material3.OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            },
        )
        statefulRow(key = "tf_stateful_row", defaultValue = "Sample") { value, onValueChange ->
            TextFieldPreference(
                value = value,
                onValueChange = onValueChange,
                title = "Sample's stateful row",
                textToValue = { it },
                summary = "Value: $value",
            )
        }
        preferenceCategory(key = "tf_value_category", title = "Value-based")
        textFieldPreference(
            key = "tf_value",
            value = "Static",
            onValueChange = {},
            title = "Static",
            textToValue = { it },
            summary = "Value: Static",
        )
        preferenceCategory(key = "tf_cards_category", title = "Cards")
        preferenceCard(key = "tf_card") {
            preference(title = "Card text field", summary = "Static row inside a real card.")
        }
        preferenceCardGroup {
            card(title = "Card group text field") {
                val state = rememberPreferenceState("tf_group_state", "Group")
                val value by state
                TextFieldPreference(
                    value = value,
                    onValueChange = { state.value = it },
                    title = "Card group text field",
                    textToValue = { it },
                    summary = "Value: $value",
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
