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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import net.slions.compose.preference.FooterPreference
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.footerPreference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory

/** The [net.slions.compose.preference.FooterPreference] page: icon plus summary rows. */
@Composable
fun footerPreferencePage(): PreferencePage =
    PreferencePage(
        id = "footer",
        title = "Footer",
        summary = "Icon-and-summary rows; the title is search-only, not displayed.",
    ) {
        preferenceCategory(key = "footer_basic_category", title = "Basics")
        footerPreference(
            key = "footer_default",
            title = "Default footer",
            summary = "The default info icon and a summary.",
        )
        footerPreference(
            key = "footer_long",
            title = "Long footer",
            summary = "A rather long footer summary that wraps across several lines on " +
                "narrow screens, to check text layout.",
        )
        footerPreference(
            key = "footer_no_summary",
            title = "Footer without summary",
            summary = null,
        )
        preferenceCategory(key = "footer_icon_category", title = "Custom icon")
        footerPreference(
            key = "footer_custom_icon",
            title = "Custom icon",
            summary = "The icon is a regular composable.",
            icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = null) },
        )
        preferenceCategory(key = "footer_cards_category", title = "Cards")
        preferenceCard(key = "footer_card") {
            preference(title = "Card footer", summary = "Static row inside a real card.")
        }
        preferenceCardGroup {
            card(title = "Footer", summary = "A footer row inside a card group.") {
                FooterPreference(summary = "A footer row inside a card group.")
            }
            card(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.preference.Preference(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }
