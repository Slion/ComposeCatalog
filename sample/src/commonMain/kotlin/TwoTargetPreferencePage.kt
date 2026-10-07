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
import net.slions.compose.preference.Preference
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.preference
import net.slions.compose.preference.preferenceCard
import net.slions.compose.preference.preferenceCardGroup
import net.slions.compose.preference.preferenceCategory
import net.slions.compose.preference.twoTargetPreference

/**
 * The [net.slions.compose.preference.TwoTargetPreference] page: a row with a second target
 * (anything composable) after a vertical divider.
 */
@Composable
fun twoTargetPreferencePage(): PreferencePage =
    PreferencePage(
        id = "two_target",
        title = "Two target",
        summary = "Rows with a second target after a divider.",
    ) {
        preferenceCategory(key = "tt_basic_category", title = "Basics")
        twoTargetPreference(
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
        twoTargetPreference(
            key = "tt_icon",
            title = "With leading icon",
            summary = "A text button second target; the leading icon slot still works.",
            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
            secondTarget = {
                TextButton(onClick = {}) { Text("Open") }
            },
        )
        twoTargetPreference(
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
        twoTargetPreference(
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
        twoTargetPreference(
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
        preferenceCategory(key = "tt_cards_category", title = "Cards")
        preferenceCard(key = "tt_card") {
            preference(title = "Card two target", summary = "Static row inside a real card.")
        }
        preferenceCardGroup {
            card(title = "Card group two target", summary = "A two-target row inside a card group.") {
                TwoTargetRowSample()
            }
            card(title = "Card group row 2", summary = "Each card group item is its own card.") {
                net.slions.compose.preference.Preference(
                    title = "Card group row 2",
                    summary = "Each card group item is its own card.",
                )
            }
        }
    }

@Composable
private fun TwoTargetRowSample() {
    net.slions.compose.preference.TwoTargetPreference(
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
