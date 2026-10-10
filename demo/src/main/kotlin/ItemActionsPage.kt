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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.slions.compose.catalog.Item
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.item
import net.slions.compose.catalog.CardStyle
import net.slions.compose.catalog.group
import net.slions.compose.catalog.section
import net.slions.compose.catalog.itemActions

/**
 * The [net.slions.compose.catalog.ItemActions] page: a row with a second target
 * (anything composable) after a vertical divider.
 */
fun itemActionsPage(): Page =
    Page(
        id = "two_target",
        title = "Two target",
        summary = "Rows with a second target after a divider.",
    ) {
        section(key = "tt_basic_category", title = "Basics")
        itemActions(
            key = "tt_basic",
            title = "Basic two target",
            summary = "An icon second target after a divider.",
            secondTarget = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )
        itemActions(
            key = "tt_icon",
            title = "With leading icon",
            summary = "A text button second target; the leading icon slot still works.",
            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
            secondTarget = {
                TextButton(onClick = {}) { Text("Open") }
            },
        )
        itemActions(
            key = "tt_click",
            title = "Main target clickable",
            summary = "Tapping the title fires the row's onClick.",
            onClick = {},
            secondTarget = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )
        itemActions(
            key = "tt_disabled",
            title = "Disabled",
            enabled = false,
            summary = "The whole row is disabled.",
            secondTarget = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )
        itemActions(
            key = "tt_divider",
            title = "Custom second target",
            summary = "The second target is any fixed-size composable (full-width ones like " +
                "HorizontalDivider would squeeze the title).",
            secondTarget = {
                Box(
                    modifier =
                        Modifier.size(2.dp, 32.dp)
                            .background(DividerDefaults.color)
                            .padding(start = 8.dp),
                )
            },
        )
        section(key = "tt_cards_category", title = "Cards")
        item(key = "tt_card", title = "Card two target", summary = "Static row drawn in a card.", style = CardStyle.Filled)
        group {
            item(title = "Card group two target", summary = "A two-target row inside a card group.") {
                ItemActionsRowSample()
            }
            item(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.catalog.Item(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }

@Composable
private fun ItemActionsRowSample() {
    net.slions.compose.catalog.ItemActions(
        title = "Card group two target",
        summary = "A two-target row inside a card group.",
        secondTarget = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
    )
}
