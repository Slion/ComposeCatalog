/*
 * Copyright 2023 Google LLC
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

package net.slions.compose.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a row to the lazy list: a preference row, or — with [page] — a row that opens a
 * page. A page row is the basic way a page references its children: it can be placed
 * anywhere in a page's content, next to any other item, and it is styled exactly like
 * any other row (a host may give it a [CardStyle] or put it in a [group] like any other
 * item). It
 * only defaults its action: tapping a page row navigates into [page], unless [onClick]
 * is set, in which case the action runs instead (e.g. launching an activity that hosts
 * the page in its own catalog), and the trailing action icon indicates which — a chevron
 * for navigation, an open-in-new for an action, or a custom [actionIcon].
 *
 * @param key The lazy list key of the row, and the preference state key. If null,
 *   [page]'s id is used (page rows are keyed by their page's id).
 * @param page The page the row opens; when null, the row is a regular preference row.
 * @param title The title of the row. Also used as the row's search text. If null,
 *   [page]'s title is used.
 * @param modifier Modifier applied to the row.
 * @param enabled Whether the row is enabled.
 * @param icon The leading icon. If null, [page]'s icon is used.
 * @param actionIcon The action icon, shown just before the widget. For a page row,
 *   a chevron for navigation and an open-in-new icon for an action if null.
 * @param summary The summary text, shown below the title. If null, [page]'s summary is
 *   used.
 * @param staticSummary A static summary used in the [buildSearchIndex] index. If null,
 *   [summary] is used.
 * @param widgetContainer The trailing widget (e.g. a switch or checkbox).
 * @param onClick Click handler; for a page row, when null the row navigates into [page];
 *   when set, it replaces the navigation.
 * @param style When non-null, the row is drawn in its own card of this style (a card is a
 *   style, not a container); [CardStyle.None] is equivalent to null — the row is drawn
 *   without a card.
 * @param cardColor Card background color. Only applies with [style]; if null, the default
 * container color of [style] is used.
 * @param cardElevation Card elevation. Only applies to [CardStyle.Elevated]; if null,
 * [CardDefaults.elevatedCardElevation] is used.
 * @param cardBorder Card border. Only applies to [CardStyle.Outlined]; if null,
 * [CardDefaults.outlinedCardBorder] is used.
 * @param shape Card shape. Only applies with [style]; if null, `MaterialTheme.shapes.medium`
 * is used.
 * @param outerPadding Clearance between the card and its container. Only applies with [style];
 * if null, `PreferenceTheme.horizontalSpacing` is used on all sides.
 */
public fun LazyListScope.item(
    key: String? = null,
    page: Page? = null,
    title: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    val rowKey = key ?: page?.id
    val rowTitle = title ?: page?.title
    val rowSummary = summary ?: page?.summary
    when {
        // A page row: registers the page reference (for the tree walk and navigation).
        // No search entry: the child page is searchable as a page of the tree in its
        // own right, and a row entry would only duplicate it.
        page != null -> SearchIndexer.recordSubPage(page, onClick, rowKey)
        // A preference row: searchable entry, keyed.
        rowKey != null -> SearchIndexer.record(rowKey, rowTitle ?: "", staticSummary ?: rowSummary)
    }
    item(key = rowKey, contentType = if (page != null) "PageItem" else "Item") {
        val row: @Composable (Modifier) -> Unit = { m ->
            Item(
                title = rowTitle ?: "",
                page = page,
                modifier = m,
                enabled = enabled,
                icon = icon ?: page?.icon,
                actionIcon = actionIcon,
                summary = rowSummary,
                widgetContainer = widgetContainer,
                onClick = onClick,
            )
        }
        if (style != null && style != CardStyle.None) {
            // A carded row: the card is a style of the row, with the group's outer clearance.
            val outer = outerPadding ?: PaddingValues(LocalPreferenceTheme.current.horizontalSpacing)
            Column(
                modifier =
                    modifier
                        .then(highlightedKeyModifier(rowKey))
                        .fillMaxWidth()
                        .padding(outer),
            ) {
                CardSurface(
                    style = style,
                    shape = shape,
                    cardColor = cardColor,
                    cardElevation = cardElevation,
                    cardBorder = cardBorder,
                ) {
                    row(Modifier.fillMaxWidth())
                }
            }
        } else {
            row(modifier.then(highlightedKeyModifier(rowKey)))
        }
    }
}

/**
 * The id of the page currently open in the detail pane, so a page row can highlight
 * itself; null when no page is open. Provided by the catalog's panes.
 */
/**
 * The navigation callback of the pane hosting a page row: invoked with the page id to
 * navigate into it. Provided by the catalog's panes.
 */
internal val LocalOnSelectPage: ProvidableCompositionLocal<(String) -> Unit> =
    compositionLocalOf { { } }

/**
 * One row: a preference row, or — with [page] — a page row that opens the page. A page
 * row is rendered exactly like any other row (styled like any other: a host may give it a
 * [CardStyle] or put it in a [group] like any other item); it only defaults its action — the
 * trailing action icon (a chevron for navigation, an open-in-new for an action) and the
 * click (navigate into [page] unless [onClick] is set).
 */
@Composable
public fun Item(
    title: String,
    modifier: Modifier = Modifier,
    page: Page? = null,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    // Read in the composable scope: the click lambda is not.
    val navigate = LocalOnSelectPage.current
    // A page row's default action icon: a chevron for navigation, an open-in-new for
    // the action that replaces the navigation.
    val effectiveActionIcon =
        if (page != null && actionIcon == null) {
            @Composable {
                Icon(
                    imageVector =
                        if (onClick == null) {
                            Icons.Filled.ChevronRight
                        } else {
                            Icons.AutoMirrored.Filled.OpenInNew
                        },
                    contentDescription = null,
                )
            }
        } else {
            actionIcon
        }
    val effectiveOnClick =
        if (page != null) {
            onClick ?: { navigate(page.id) }
        } else {
            onClick
        }
    BasicItem(
        textContainer = {
            val theme = LocalPreferenceTheme.current
            Column(
                modifier =
                    Modifier.padding(
                        theme.padding.copy(
                            start = if (icon != null) 0.dp else Dp.Unspecified,
                            end =
                                if (widgetContainer != null || effectiveActionIcon != null) {
                                    0.dp
                                } else {
                                    Dp.Unspecified
                                },
                        )
                    )
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides
                        theme.titleColor.let {
                            if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                        }
                ) {
                    ProvideTextStyle(value = theme.titleTextStyle) {
                        Text(text = title)
                    }
                }
                if (summary != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.summaryColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            }
                    ) {
                        ProvideTextStyle(value = theme.summaryTextStyle) {
                            Text(text = summary)
                        }
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        iconContainer = {
            if (icon != null) {
                val theme = LocalPreferenceTheme.current
                Box(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(end = 0.dp)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                        content = icon,
                    )
                }
            }
        },
        actionIconContainer = {
            if (effectiveActionIcon != null) {
                val theme = LocalPreferenceTheme.current
                Box(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(start = 0.dp)),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                        content = effectiveActionIcon,
                    )
                }
            }
        },
        widgetContainer = { widgetContainer?.invoke() },
        onClick = effectiveOnClick,
    )
}

