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
import net.slions.compose.preference.Preference
import net.slions.compose.preference.PreferenceCardStyle
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.preference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory

/** The [Preference] page: plain rows with the various title/summary/icon configurations. */
@Composable
fun preferenceRowPage(): PreferencePage {
    val colorScheme = MaterialTheme.colorScheme
    val elevatedElevation = CardDefaults.elevatedCardElevation()
    return PreferencePage(
        id = "preference",
        title = "Preference",
        summary = "Plain rows: icons, action icons, summaries, disabled rows, and cards.",
    ) {
        preferenceCategory(key = "pref_basic_category", title = "Basics")
        preference(
            key = "pref_basic",
            title = "Basic preference",
            summary = "A row with a title and a summary.",
        )
        preference(
            key = "pref_no_summary",
            title = "No summary",
        )
        preference(
            key = "pref_icon",
            title = "With icon",
            icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            summary = "The leading icon slot.",
        )
        preference(
            key = "pref_action_icon",
            title = "With action icon",
            actionIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null) },
            summary = "The trailing action icon slot.",
        )
        preference(
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
        preference(
            key = "pref_long",
            title = "A rather long title that needs to wrap on narrow screens",
            summary = "A rather long summary that wraps across several lines on narrow " +
                "screens, to check text layout.",
        )
        preference(
            key = "pref_disabled",
            title = "Disabled",
            enabled = false,
            summary = "Not clickable.",
        )
        preference(
            key = "pref_click",
            title = "With click handler",
            summary = "Tapping fires the onClick callback.",
            onClick = {},
        )
        preferenceCategory(key = "pref_cards_category", title = "Cards")
        preferenceCard(key = "pref_card") {
            preference(title = "Card row 1", summary = "First row of a real card.")
            preference(
                title = "Card row 2",
                icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            )
            preference(title = "Card row 3", summary = "Last row.")
        }
        preferenceCardGroup {
            card(title = "Card group row 1", summary = "Each card group item is its own card.") {
                Preference(title = "Card group row 1", summary = "Each card group item is its own card.")
            }
            card(title = "Card group row 2") {
                Preference(
                    title = "Card group row 2",
                    actionIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    },
                )
            }
        }
        preferenceCard(
            key = "pref_card_custom",
            cardColor = colorScheme.primaryContainer,
            shape = RoundedCornerShape(24.dp),
        ) {
            preference(title = "Custom color and shape", summary = "primaryContainer, 24.dp corners.")
        }
        preferenceCard(
            key = "pref_card_elevated",
            style = PreferenceCardStyle.Elevated,
            cardColor = colorScheme.secondaryContainer,
            cardElevation = elevatedElevation,
        ) {
            preference(title = "Elevated card", summary = "Custom color and elevation.")
        }
        preferenceCard(
            key = "pref_card_outlined",
            style = PreferenceCardStyle.Outlined,
            cardColor = colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            itemSpacing = 4.dp,
        ) {
            preference(title = "Outlined card row", summary = "Custom style, color, and shape.")
        }
        preferenceCard(
            key = "pref_card_padding",
            outerPadding = PaddingValues(0.dp),
            contentPadding = PaddingValues(8.dp),
            itemSpacing = 8.dp,
        ) {
            preference(title = "Custom padding", summary = "No outer padding, 8.dp content and item spacing.")
        }
    }
}
