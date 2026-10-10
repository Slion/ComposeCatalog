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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.ItemSlider
import net.slions.compose.catalog.CardStyle
import net.slions.compose.catalog.group
import net.slions.compose.catalog.item
import net.slions.compose.catalog.section
import net.slions.compose.catalog.rememberValue
import net.slions.compose.catalog.itemSlider
import kotlin.math.roundToInt

/**
 * The [net.slions.compose.catalog.ItemSlider] page: stateful and value-based. The value
 * of the value-based slider is hoisted to the host composable (a page is plain data, and
 * its rows are not a composable scope).
 */
fun sliderPage(valueBasedState: MutableFloatState): Page {
    return Page(
        id = "slider",
        title = "Slider",
        summary = "Ranges, steps, dynamic text, and disabled rows.",
    ) {
        section(key = "slider_stateful_category", title = "Stateful")
        itemSlider(
            key = "slider_default",
            defaultValue = 0f,
            title = "Default range (0..1)",
        )
        itemSlider(
            key = "slider_steps",
            defaultValue = 2f,
            title = "With steps",
            valueRange = 0f..5f,
            valueSteps = 9,
            summary = { "Half steps: ${((it / 0.5f).roundToInt() * 0.5f)}" },
            staticSummary = "Half steps",
            valueText = { ((it / 0.5f).roundToInt() * 0.5f).toString() },
        )
        itemSlider(
            key = "slider_wide",
            defaultValue = 50f,
            title = "Wide range (0..100)",
            valueRange = 0f..100f,
            summary = { "${it.roundToInt()}%" },
            staticSummary = "Percentage",
        )
        itemSlider(
            key = "slider_disabled",
            defaultValue = 0.5f,
            title = "Disabled",
            enabled = { false },
            valueText = { it.toString() },
        )
        itemSlider(
            key = "slider_icon",
            defaultValue = 0.25f,
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.Speed, contentDescription = null) },
            valueText = { (it * 100).roundToInt().toString() },
        )
        statefulRow(key = "slider_stateful_row", defaultValue = 3f) { value, onValueChange ->
            val sliderState = remember { mutableFloatStateOf(value) }
            val sliderValue by sliderState
            ItemSlider(
                value = value,
                onValueChange = onValueChange,
                sliderValue = sliderValue,
                onSliderValueChange = { sliderState.floatValue = it },
                title = "Sample's stateful row",
                valueText = { it.roundToInt().toString() },
            )
        }
        section(key = "slider_value_category", title = "Value-based")
        // Value-based: the page owns the value and hands it to the composable with real
        // change handlers. The value is read inside the item's composable scope (not in the
        // non-composable content builder), so the row recomposes when the value changes.
        item(key = "slider_value", contentType = "ItemSlider") {
            val value by valueBasedState
            ItemSlider(
                value = value,
                onValueChange = { valueBasedState.floatValue = it },
                sliderValue = value,
                onSliderValueChange = { valueBasedState.floatValue = it },
                title = "Host-controlled",
                summary = "The page supplies the value and handlers.",
                valueText = { (it * 100).roundToInt().toString() },
            )
        }
        section(key = "slider_cards_category", title = "Cards")
        item(
            key = "slider_card",
            title = "Card slider",
            summary = "A slider in a card row's widget slot.",
            widgetContainer = {
                val state = rememberValue<Float>("slider_card_value", 0.5f)
                val value by state
                // The row is full-width, but the slider must not take all of it: a
                // weightless Slider in the (non-weighted) widget slot would squeeze the
                // title, so pin a sensible width. The end padding gives the slider the
                // same clearance from the card edge the switch widget gets.
                androidx.compose.material3.Slider(
                    value = value,
                    onValueChange = { state.value = it },
                    modifier = Modifier.width(180.dp).padding(end = 16.dp),
                )
            },
            style = CardStyle.Filled,
        )
        group {
            item(title = "Card group slider") {
                val state = rememberValue<Float>("slider_group_state", 2f)
                val value by state
                val sliderState = remember { mutableFloatStateOf(value) }
                val sliderValue by sliderState
                ItemSlider(
                    value = value,
                    onValueChange = { state.value = it },
                    sliderValue = sliderValue,
                    onSliderValueChange = { sliderState.floatValue = it },
                    title = "Card group slider",
                    valueRange = 0f..5f,
                    valueSteps = 9,
                    valueText = { it.roundToInt().toString() },
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
}
