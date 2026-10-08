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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import net.slions.compose.preference.Preference
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.preference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory
import net.slions.compose.preference.twoTargetIconButtonPreference

/**
 * The [net.slions.compose.preference.TwoTargetIconButtonPreference] page: a row with an icon
 * button as the second target.
 */
@Composable
fun twoTargetIconButtonPreferencePage(): PreferencePage =
    PreferencePage(
        id = "two_target_icon_button",
        title = "Two target icon button",
        summary = "Rows with an icon button after a divider.",
    ) {
        preferenceCategory(key = "ttib_basic_category", title = "Basics")
        twoTargetIconButtonPreference(
            key = "ttib_basic",
            title = "Basic icon button",
            summary = "The icon button fires its own callback.",
            iconButtonIcon = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
            },
            onIconButtonClick = {},
        )
        twoTargetIconButtonPreference(
            key = "ttib_icon",
            title = "With leading icon",
            summary = "Both the leading icon and the icon button.",
            icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            iconButtonIcon = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
            },
            onIconButtonClick = {},
        )
        twoTargetIconButtonPreference(
            key = "ttib_click",
            title = "Main target clickable",
            summary = "Both targets have their own callbacks.",
            onClick = {},
            iconButtonIcon = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
            },
            onIconButtonClick = {},
        )
        twoTargetIconButtonPreference(
            key = "ttib_disabled",
            title = "Disabled row",
            enabled = false,
            summary = "The whole row, button included, is disabled.",
            iconButtonIcon = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
            },
            onIconButtonClick = {},
        )
        twoTargetIconButtonPreference(
            key = "ttib_button_disabled",
            title = "Disabled icon button",
            summary = "Only the icon button is disabled.",
            iconButtonEnabled = false,
            iconButtonIcon = {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
            },
            onIconButtonClick = {},
        )
        preferenceCategory(key = "ttib_cards_category", title = "Cards")
        preferenceCard(key = "ttib_card") {
            preference(title = "Card icon button", summary = "Static row inside a real card.")
        }
        preferenceCardGroup {
            card(title = "Card group icon button", summary = "A two-target row inside a card group.") {
                net.slions.compose.preference.TwoTargetIconButtonPreference(
                    title = "Card group icon button",
                    summary = "A two-target row inside a card group.",
                    iconButtonIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                        )
                    },
                    onIconButtonClick = {},
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
