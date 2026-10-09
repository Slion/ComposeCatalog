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
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.ItemTextField
import net.slions.compose.catalog.card
import net.slions.compose.catalog.cardGroup
import net.slions.compose.catalog.section
import net.slions.compose.catalog.rememberValue
import net.slions.compose.catalog.itemTextField

/** The [net.slions.compose.catalog.ItemTextField] page: string and numeric values. */
@Composable
fun textFieldPage(): Page =
    Page(
        id = "text_field",
        title = "Text field",
        summary = "String and numeric values, custom text fields, and disabled rows.",
    ) {
        section(key = "tf_stateful_category", title = "Stateful")
        itemTextField(
            key = "tf_string",
            defaultValue = "Sample",
            title = "String",
            textToValue = { it },
            summary = { "Value: $it" },
            staticSummary = "A text value",
        )
        itemTextField(
            key = "tf_number",
            defaultValue = 42,
            title = "Number",
            textToValue = { it.toIntOrNull() },
            summary = { "Value: $it" },
            staticSummary = "An integer value",
        )
        itemTextField(
            key = "tf_icon",
            defaultValue = "Sample",
            title = "With icon",
            textToValue = { it },
            icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
            summary = { "Value: $it" },
            staticSummary = "A text value",
        )
        itemTextField(
            key = "tf_disabled",
            defaultValue = "Sample",
            title = "Disabled",
            textToValue = { it },
            enabled = { false },
            summary = { "Value: $it" },
            staticSummary = "A text value",
        )
        itemTextField(
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
            ItemTextField(
                value = value,
                onValueChange = onValueChange,
                title = "Sample's stateful row",
                textToValue = { it },
                summary = "Value: $value",
            )
        }
        section(key = "tf_value_category", title = "Value-based")
        itemTextField(
            key = "tf_value",
            value = "Static",
            onValueChange = {},
            title = "Static",
            textToValue = { it },
            summary = "Value: Static",
        )
        section(key = "tf_cards_category", title = "Cards")
        card(key = "tf_card") {
            item(title = "Card text field", summary = "Static row inside a real card.")
        }
        cardGroup {
            card(title = "Card group text field") {
                val state = rememberValue("tf_group_state", "Group")
                val value by state
                ItemTextField(
                    value = value,
                    onValueChange = { state.value = it },
                    title = "Card group text field",
                    textToValue = { it },
                    summary = "Value: $value",
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
