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

package net.slions.compose.toolkit.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.slions.compose.toolkit.CardStyle
import net.slions.compose.toolkit.ItemSwitch
import net.slions.compose.toolkit.Page
import net.slions.compose.toolkit.group
import net.slions.compose.toolkit.rememberValue
import net.slions.compose.toolkit.section

/**
 * The [net.slions.compose.toolkit.group] page: a group is a list of items, each drawn in its
 * own card, so the group looks like one card split into rows. The card is a *style* of the
 * group ([CardStyle]) or of a single row ([net.slions.compose.toolkit.item] with [style]),
 * not a logical container.
 *
 * @param colorScheme The ambient color scheme, resolved by the caller (the page's content is
 *   not composable, so it cannot read [androidx.compose.material3.MaterialTheme]).
 */
fun groupPage(colorScheme: ColorScheme): Page =
    Page(
        id = "group",
        title = "Group",
        summary = "Each item is its own card; the card is a style, not a container.",
    ) {
        section(key = "gp_basic_category", title = "Basic")
        // The default: a filled group of three rows — rounded top corners, square middles,
        // rounded bottom corners.
        group(key = "gp_basic") {
            item(title = "First row", summary = "Rounded top corners.")
            item(title = "Middle row", summary = "Square corners.")
            item(title = "Last row", summary = "Rounded bottom corners.")
        }

        section(key = "gp_rows_category", title = "Row features")
        group(key = "gp_rows") {
            item(
                title = "With icon",
                summary = "The leading icon slot.",
                icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            )
            item(title = "Disabled", summary = "Not clickable.", enabled = false)
            item(
                title = "With widget",
                summary = "A switch in the trailing slot.",
                widgetContainer = {
                    val state = rememberValue("gp_widget_switch", true)
                    val value by state
                    Switch(checked = value, onCheckedChange = { state.value = it })
                },
            )
        }

        section(key = "gp_content_category", title = "Custom content")
        group(key = "gp_content") {
            // With content, the item draws it inside its card instead of the standard row;
            // the title/summary still index the item for search.
            item(title = "Stateful row", summary = "A full switch row drawn as content.") {
                val state = rememberValue("gp_content_switch", true)
                val value by state
                ItemSwitch(
                    value = value,
                    onValueChange = { state.value = it },
                    title = "Stateful row",
                    summary = "A full switch row drawn as content.",
                )
            }
            item(title = "Any layout", summary = "A card is not limited to one row.") {
                Column(Modifier.padding(16.dp)) {
                    Text("A card can hold any layout.")
                    Text("Here, two plain lines of text.")
                }
            }
        }

        section(key = "gp_pages_category", title = "Page rows")
        // An item with a page is a page row: it registers the child page (so the page joins
        // the tree) and navigates by default, exactly like item(page = ...).
        group(key = "gp_pages") {
            item(page = checkboxPage())
            item(page = switchPage())
            item(page = listPage())
        }

        section(key = "category_filled", title = "Filled")
        group(key = "gp_filled", style = CardStyle.Filled) {
            item(title = "Filled, first", summary = "CardStyle.Filled")
            item(title = "Filled, last", summary = "Filled surface.")
        }
        
        section(key = "category_elevated", title = "Elevated")
        group(key = "gp_elevated", style = CardStyle.Elevated) {
            item(title = "Elevated, first", summary = "CardStyle.Elevated.")
            item(title = "Elevated, last", summary = "A shadow instead of a filled surface.")
        }
        
        section(key = "category_outline", title = "Outlined")
        group(key = "gp_outlined", style = CardStyle.Outlined) {
            item(title = "Outlined, first", summary = "CardStyle.Outlined.")
            item(title = "Outlined, last", summary = "A border instead of a surface.")
        }
        
        section(key = "category_nocard", title = "No card")
        // No card at all: the group keeps its spacing and outer clearance, but the rows are
        // drawn plainly.
        group(key = "gp_none", style = CardStyle.None) {
            item(title = "No card, first", summary = "CardStyle.None: plain rows.")
            item(title = "No card, last", summary = "Same group, no card.")
        }

        section(key = "gp_custom_category", title = "Customization")
        group(
            key = "gp_color",
            cardColor = colorScheme.primaryContainer,
            shape = RoundedCornerShape(24.dp),
        ) {
            item(title = "Custom color and shape", summary = "primaryContainer, 24.dp corners.")
        }
        group(key = "gp_spacing", itemSpacing = 16.dp, outerPadding = PaddingValues(0.dp)) {
            item(title = "Wide spacing", summary = "16.dp between the cards, no outer clearance.")
            item(title = "Wide spacing, last", summary = "The gap is what splits one card into many.")
        }
    }
