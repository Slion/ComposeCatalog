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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CardDefaults
import net.slions.compose.catalog.Item
import net.slions.compose.catalog.CardStyle
import net.slions.compose.catalog.Page
import net.slions.compose.catalog.item
import net.slions.compose.catalog.card
import net.slions.compose.catalog.cardGroup
import net.slions.compose.catalog.section

/** The [Item] page: plain rows with the various title/summary/icon configurations. */
@Composable
fun itemPage(): Page {
    val colorScheme = MaterialTheme.colorScheme
    val elevatedElevation = CardDefaults.elevatedCardElevation()
    return Page(
        id = "item",
        title = "Item",
        summary = "Plain rows: icons, action icons, summaries, disabled rows, and cards.",
    ) {
        section(key = "pref_basic_category", title = "Basics")
        item(
            key = "pref_basic",
            title = "Basic item",
            summary = "A row with a title and a summary.",
        )
        item(
            key = "pref_no_summary",
            title = "No summary",
        )
        item(
            key = "pref_icon",
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            summary = "The leading icon slot.",
        )
        item(
            key = "pref_action_icon",
            title = "With action icon",
            actionIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null) },
            summary = "The trailing action icon slot.",
        )
        item(
            key = "pref_widget",
            title = "With widget",
            summary = "A trailing widget slot, no icon.",
            widgetContainer = {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                )
            },
        )
        item(
            key = "pref_long",
            title = "A rather long title that needs to wrap on narrow screens",
            summary = "A rather long summary that wraps across several lines on narrow " +
                "screens, to check text layout.",
        )
        item(
            key = "pref_disabled",
            title = "Disabled",
            enabled = false,
            summary = "Not clickable.",
        )
        item(
            key = "pref_click",
            title = "With click handler",
            summary = "Tapping fires the onClick callback.",
            onClick = {},
        )
        section(key = "pref_cards_category", title = "Cards")
        card(key = "pref_card") {
            item(title = "Card row 1", summary = "First row of a real card.")
            item(
                title = "Card row 2",
                icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            )
            item(title = "Card row 3", summary = "Last row.")
        }
        cardGroup {
            card(title = "Card group row 1", summary = "Each card group item is its own card.") {
                Item(title = "Card group row 1", summary = "Each card group item is its own card.")
            }
            card(title = "Card group row 2") {
                Item(
                    title = "Card group row 2",
                    actionIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    },
                )
            }
        }
        card(
            key = "pref_card_custom",
            cardColor = colorScheme.primaryContainer,
            shape = RoundedCornerShape(24.dp),
        ) {
            item(title = "Custom color and shape", summary = "primaryContainer, 24.dp corners.")
        }
        card(
            key = "pref_card_elevated",
            style = CardStyle.Elevated,
            cardColor = colorScheme.secondaryContainer,
            cardElevation = elevatedElevation,
        ) {
            item(title = "Elevated card", summary = "Custom color and elevation.")
        }
        card(
            key = "pref_card_outlined",
            style = CardStyle.Outlined,
            cardColor = colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            itemSpacing = 4.dp,
        ) {
            item(title = "Outlined card row", summary = "Custom style, color, and shape.")
        }
        card(
            key = "pref_card_padding",
            outerPadding = PaddingValues(0.dp),
            contentPadding = PaddingValues(8.dp),
            itemSpacing = 8.dp,
        ) {
            item(title = "Custom padding", summary = "No outer padding, 8.dp content and item spacing.")
        }
    }
}
