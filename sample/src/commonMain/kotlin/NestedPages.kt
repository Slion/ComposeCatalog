/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.preference.sample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Expand
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import dev.vicart.compose.material.symbols.MaterialSymbol
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.checkboxPreference
import net.slions.compose.preference.preference
import net.slions.compose.preference.preferenceCategory
import net.slions.compose.preference.switchPreference

/**
 * The "Nested" page: a page tree used to test the screen's nested navigation. It has two
 * sub-pages, one of which ("Advanced") has a sub-page of its own, so the tree is three levels
 * deep. The breadcrumb should show the full trail (e.g. "Nested > Advanced > Developer") and
 * back should pop one level at a time.
 */
@Composable
fun nestedPreferencePage(): PreferencePage =
    PreferencePage(
        id = "nested",
        title = "Nested",
        summary = "A page tree: sub-pages, a three-level trail, and breadcrumb navigation.",
        subPages = listOf(nestedGeneralPage(), nestedAdvancedPage()),
    ) {
        preferenceCategory(key = "nested_intro_category", title = "About this page")
        preference(
            key = "nested_intro",
            title = "Sub-pages",
            summary = "This page's own rows are followed by its sub-page rows. Tapping one " +
                "navigates into it; the list pane switches to that page's children.",
        )
        preference(
            key = "nested_trail",
            title = "Breadcrumb",
            summary = "In two-pane mode the trail above the panes shows every level; tapping " +
                "an ancestor jumps back to it.",
        )
    }

/** First level of the "Nested" tree: a couple of rows, no sub-pages. */
@Composable
private fun nestedGeneralPage(): PreferencePage =
    PreferencePage(
        id = "nested_general",
        title = "General",
        summary = "First level: plain rows.",
        icon = { Icon(imageVector = Icons.Outlined.Expand, contentDescription = null) },
    ) {
        preferenceCategory(key = "nested_general_category", title = "General")
        preference(
            key = "nested_general_row",
            title = "General row",
            summary = "A row on the General page (Nested > General).",
        )
        switchPreference(
            key = "nested_general_switch",
            defaultValue = true,
            title = "General switch",
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
        checkboxPreference(
            key = "nested_general_checkbox",
            defaultValue = false,
            title = "General checkbox",
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
    }

/** Second level: a few rows plus a third-level sub-page ("Developer"). */
@Composable
private fun nestedAdvancedPage(): PreferencePage =
    PreferencePage(
        id = "nested_advanced",
        title = "Advanced",
        summary = "Second level: rows and a further sub-page.",
        icon = { Icon(imageVector = Icons.Outlined.Expand, contentDescription = null) },
        subPages = listOf(nestedDeveloperPage()),
    ) {
        preferenceCategory(key = "nested_advanced_category", title = "Advanced")
        preference(
            key = "nested_advanced_row",
            title = "Advanced row",
            summary = "A row on the Advanced page (Nested > Advanced).",
        )
        preference(
            key = "nested_advanced_row2",
            title = "Another advanced row",
            summary = "Search should find this under the 'Nested > Advanced' trail.",
        )
    }

/** Third level of the "Nested" tree: the deepest page. */
@Composable
private fun nestedDeveloperPage(): PreferencePage =
    PreferencePage(
        id = "nested_developer",
        title = "Developer",
        summary = "Third level: the deepest page of the test tree.",
        icon = { Icon(imageVector = Icons.Outlined.Expand, contentDescription = null) },
    ) {
        preferenceCategory(key = "nested_dev_category", title = "Developer")
        preference(
            key = "nested_dev_row",
            title = "Developer row",
            summary = "A row on the Developer page (Nested > Advanced > Developer).",
        )
        switchPreference(
            key = "nested_dev_switch",
            defaultValue = false,
            title = "Developer switch",
            summary = { if (it) "On" else "Off" },
            staticSummary = "On/Off",
        )
    }
