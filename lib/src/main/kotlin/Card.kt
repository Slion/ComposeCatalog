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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Style of the [Card].
 */
public sealed class CardStyle {
    /** A filled card (Material Design 3 [Card]) with [CardDefaults.cardColors]. */
    public object Filled : CardStyle()

    /** An elevated card (Material Design 3 [ElevatedCard]) with
     * [CardDefaults.elevatedCardColors]. */
    public object Elevated : CardStyle()

    /** An outlined card (Material Design 3 [OutlinedCard]) with
     * [CardDefaults.outlinedCardColors]. */
    public object Outlined : CardStyle()
}

/**
 * Groups a set of preferences in a Material Design 3 card.
 *
 * The [content] is built with [CardScope], so the card's rows are regular data and
 * can be indexed for search.
 *
 * @param key The unique key of the card.
 * @param modifier Modifier used to draw the card.
 * @param style Style of the card.
 * @param shape Shape of the card. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, the default container color of [style] is
 * used.
 * @param cardElevation Card elevation. Only applies to [CardStyle.Elevated]. If null,
 * [CardDefaults.elevatedCardElevation] is used.
 * @param cardBorder Card border. Only applies to [CardStyle.Outlined]. If null,
 * [CardDefaults.outlinedCardBorder] is used.
 * @param outerPadding Clearance between the card and its container. If null,
 * `PreferenceTheme.horizontalSpacing` is used on all sides.
 * @param contentPadding Padding applied around the card content. If null, no padding is applied
 * (preferences already provide their own spacing).
 * @param itemSpacing Vertical gap inserted between the card's top-level items. If 0.dp, no gap
 * is inserted.
 * @param content Content of the card, built with [CardScope].
 */
public fun LazyListScope.card(
    key: String? = null,
    modifier: Modifier = Modifier,
    style: CardStyle = CardStyle.Filled,
    shape: Shape? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    outerPadding: PaddingValues? = null,
    contentPadding: PaddingValues? = null,
    itemSpacing: Dp = 0.dp,
    content: CardScope.() -> Unit,
) {
    // Runs the content (a plain data builder, not composable) before registering the item, so
    // the card's rows are recorded with the index of the card's single lazy list item.
    val scope = CardScope()
    scope.content()
    val cardIndex = SearchIndexer.itemCount()
    val fixes =
        scope.rows.mapIndexedNotNull { index, row ->
            SearchIndexer.record(
                    key = key ?: "card:${cardIndex}:row:$index",
                    title = row.title,
                    summary = row.summary,
                )
                ?.let { it to cardIndex }
        }
    SearchIndexer.setIndices(fixes.toMap())
    item(key) {
        val cardShape = shape ?: MaterialTheme.shapes.medium
        val outer = outerPadding ?: PaddingValues(LocalPreferenceTheme.current.horizontalSpacing)
        val cardPadding = contentPadding ?: PaddingValues(0.dp)
        val cardModifier =
            modifier.then(if (key != null) highlightedKeyModifier(key) else Modifier)
        Column(modifier = cardModifier.fillMaxWidth().padding(outer)) {
            when (style) {
                CardStyle.Filled -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors =
                        cardColor?.let { CardDefaults.cardColors(containerColor = it) }
                            ?: CardDefaults.cardColors(),
                ) {
                    CardContent(cardPadding, itemSpacing, rows = scope.rows)
                }

                CardStyle.Elevated -> ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors =
                        cardColor?.let { CardDefaults.elevatedCardColors(containerColor = it) }
                            ?: CardDefaults.elevatedCardColors(),
                    elevation = cardElevation ?: CardDefaults.elevatedCardElevation(),
                ) {
                    CardContent(cardPadding, itemSpacing, rows = scope.rows)
                }

                CardStyle.Outlined -> OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors =
                        cardColor?.let { CardDefaults.outlinedCardColors(containerColor = it) }
                            ?: CardDefaults.outlinedCardColors(),
                    border = cardBorder ?: CardDefaults.outlinedCardBorder(),
                ) {
                    CardContent(cardPadding, itemSpacing, rows = scope.rows)
                }
            }
        }
    }
}

/** A single row of a [card], as built with [CardScope.preference]. */
public data class CardRow(
    public val title: String,
    public val summary: String?,
    public val icon: @Composable (() -> Unit)? = null,
    public val widgetContainer: @Composable (() -> Unit)? = null,
    public val enabled: Boolean = true,
    public val onClick: (() -> Unit)? = null,
)

/**
 * Scope for [card]. Use [item] to add a row; the rows are indexed for search
 * and rendered as regular preference rows.
 */
public class CardScope {
    internal val rows = mutableListOf<CardRow>()

    /**
     * Adds a preference row to the card.
     *
     * @param title Title of the preference.
     * @param summary Summary of the preference.
     * @param icon Icon to draw next to the text.
     * @param widgetContainer Container to draw at the end of the preference row.
     * @param enabled Whether the preference is enabled.
     * @param onClick Callback invoked when the preference is clicked.
     */
    public fun item(
        title: String,
        summary: String? = null,
        icon: @Composable (() -> Unit)? = null,
        widgetContainer: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
    ) {
        rows.add(
            CardRow(
                title = title,
                summary = summary,
                icon = icon,
                widgetContainer = widgetContainer,
                enabled = enabled,
                onClick = onClick,
            )
        )
    }
}

/**
 * The card content, inserting [itemSpacing] gaps between the top-level rows of [rows].
 */
@Composable
private fun CardContent(
    contentPadding: PaddingValues,
    itemSpacing: Dp,
    rows: List<CardRow>,
) {
    Column(
        modifier = Modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        for (row in rows) {
            Item(
                title = row.title,
                modifier = Modifier.fillMaxWidth(),
                summary = row.summary,
                icon = row.icon,
                widgetContainer = row.widgetContainer,
                enabled = row.enabled,
                onClick = { row.onClick?.invoke() },
            )
        }
    }
}

/** A single item of a [CardGroup], as built with [CardGroupScope.card]. */
public data class CardItem(
    /** The search text of the item; also the text used to index it. */
    public val title: String,
    /** Optional summary text of the item, used to index it. */
    public val summary: String?,
    /** The composable content drawn inside the item's [Card]. */
    public val content: @Composable () -> Unit,
)

/**
 * Scope for [CardGroup]. Use [card] to add an item; each item is rendered in its own
 * [Card].
 */
public class CardGroupScope {
    internal val items = mutableListOf<CardItem>()

    /**
     * Adds an item to the group. Each item is drawn in its own [Card].
     *
     * [title] and [summary] describe the item's single row for search indexing (the card's
     * content is composable and not walked by [buildSearchIndex], so the row's own text cannot
     * be read automatically). Pass the same title/summary the row displays.
     */
    public fun card(
        title: String,
        summary: String? = null,
        content: @Composable () -> Unit,
    ) {
        items.add(CardItem(title, summary, content))
    }
}

/**
 * Renders a group of preferences where each item is drawn in its own Material Design 3 [Card],
 * separated by [itemSpacing]. The first item shows rounded top corners, the last item shows
 * rounded bottom corners, and middle items have square corners, giving the impression of a
 * single card split into individual rows.
 *
 * @param modifier Modifier used to draw the group.
 * @param itemSpacing Gap between the individual cards.
 * @param shape Shape of the individual cards. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, [CardDefaults.cardColors] is used.
 * @param outerPadding Clearance between the group and its container. If null,
 * `PreferenceTheme.horizontalSpacing` is used on all sides.
 * @param items The pre-built items of the group, as produced by [CardGroupScope].
 */
@Composable
public fun CardGroup(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 4.dp,
    shape: Shape? = null,
    cardColor: Color? = null,
    outerPadding: PaddingValues? = null,
    items: List<CardItem>,
) {
    val cardShape = shape ?: MaterialTheme.shapes.medium
    val cornerSize =
        (cardShape as? RoundedCornerShape ?: RoundedCornerShape(12.dp)).topStart
    val zero = CornerSize(0f)
    val outer = outerPadding ?: PaddingValues(LocalPreferenceTheme.current.horizontalSpacing)
    val colors =
        cardColor?.let { CardDefaults.cardColors(containerColor = it) }
            ?: CardDefaults.cardColors()
    val last = items.size - 1
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(outer),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        items.forEachIndexed { index, card ->
            val itemShape =
                when {
                    last <= 0 -> cardShape
                    index == 0 -> RoundedCornerShape(cornerSize, cornerSize, zero, zero)
                    index == last -> RoundedCornerShape(zero, zero, cornerSize, cornerSize)
                    else -> RoundedCornerShape(zero)
                }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = itemShape,
                colors = colors,
            ) {
                card.content()
            }
        }
    }
}

/**
 * Adds a group of individual cards to the lazy list. Each item is drawn in its own [Card], with
 * the first showing rounded top corners and the last showing rounded bottom corners.
 *
 * @param key Key used to identify the group in the lazy list. If null, no key is used.
 * @param modifier Modifier used to draw the group.
 * @param itemSpacing Gap between the individual cards.
 * @param shape Shape of the individual cards. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, [CardDefaults.cardColors] is used.
 * @param outerPadding Clearance between the group and its container. If null,
 * `PreferenceTheme.horizontalSpacing` is used on all sides.
 * @param content Content of the group. Use [CardGroupScope.card] to add items. The
 * content is built eagerly (not composed) so that [buildSearchIndex] can read each card's
 * title/summary and index the group's rows.
 */
public fun LazyListScope.cardGroup(
    key: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    itemSpacing: Dp = 4.dp,
    shape: Shape? = null,
    cardColor: Color? = null,
    outerPadding: PaddingValues? = null,
    content: CardGroupScope.() -> Unit,
) {
    // Runs the content (a plain data builder, not composable) before registering the item, so
    // the group's cards are recorded with the index of the group's single lazy list item. A
    // stable key is always used (a generated one when the caller passes none) so a search
    // result in the group can be highlighted.
    val scope = CardGroupScope()
    scope.content()
    val groupKey = key ?: "cardgroup:${SearchIndexer.itemCount()}"
    val groupIndex = SearchIndexer.itemCount()
    val fixes =
        scope.items.mapIndexedNotNull { index, card ->
            SearchIndexer.record(
                    key = groupKey,
                    title = card.title,
                    summary = card.summary,
                )
                ?.let { it to groupIndex }
        }
    SearchIndexer.setIndices(fixes.toMap())
    item(key = groupKey, contentType = "CardGroup") {
        CardGroup(
            modifier = modifier.then(highlightedKeyModifier(groupKey)),
            itemSpacing = itemSpacing,
            shape = shape,
            cardColor = cardColor,
            outerPadding = outerPadding,
            items = scope.items,
        )
    }
}
