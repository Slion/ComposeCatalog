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
 * Style of a card.
 *
 * A card is a *style*, not a container: it is applied to a row ([item] with [style]) or to the
 * rows of a group ([group] with [style]), never built as a separate logical thing.
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

    /** No card at all: the rows are drawn plainly (a group keeps its spacing and outer
     * clearance, and [item] keeps its standard row). */
    public object None : CardStyle()
}

/**
 * Renders [content] in a Material Design 3 card of [style].
 *
 * @param style Style of the card.
 * @param shape Shape of the card. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, the default container color of [style] is
 * used.
 * @param cardElevation Card elevation. Only applies to [CardStyle.Elevated]. If null,
 * [CardDefaults.elevatedCardElevation] is used.
 * @param cardBorder Card border. Only applies to [CardStyle.Outlined]. If null,
 * [CardDefaults.outlinedCardBorder] is used.
 */
@Composable
internal fun CardSurface(
    style: CardStyle,
    shape: Shape? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val s = shape ?: MaterialTheme.shapes.medium
    when (style) {
        CardStyle.Filled -> Card(
            modifier = Modifier.fillMaxWidth(),
            shape = s,
            colors =
                cardColor?.let { CardDefaults.cardColors(containerColor = it) }
                    ?: CardDefaults.cardColors(),
        ) {
            content()
        }

        CardStyle.Elevated -> ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = s,
            colors =
                cardColor?.let { CardDefaults.elevatedCardColors(containerColor = it) }
                    ?: CardDefaults.elevatedCardColors(),
            elevation = cardElevation ?: CardDefaults.elevatedCardElevation(),
        ) {
            content()
        }

        CardStyle.Outlined -> OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = s,
            colors =
                cardColor?.let { CardDefaults.outlinedCardColors(containerColor = it) }
                    ?: CardDefaults.outlinedCardColors(),
            border = cardBorder ?: CardDefaults.outlinedCardBorder(),
        ) {
            content()
        }

        CardStyle.None -> error("CardSurface is not used with CardStyle.None")
    }
}

/** A single item of a [group], as built with [GroupScope.item]. */
public data class GroupItem(
    /** The title of the row; also the search text of the item. */
    public val title: String,
    /** Optional summary text of the row, used to index it. */
    public val summary: String?,
    /** The page the item opens; when non-null the item registers the child-page reference
     * (and is not indexed for search, as the page itself is searchable). */
    public val page: Page? = null,
    /** The leading icon of a data row. If null, [page]'s icon is used. */
    public val icon: @Composable (() -> Unit)? = null,
    /** The trailing widget of a data row (e.g. a switch or checkbox). */
    public val widgetContainer: @Composable (() -> Unit)? = null,
    /** Whether a data row is enabled. */
    public val enabled: Boolean = true,
    /**
     * The click action of a data row; for a page row, when null the row navigates into
     * [page], and it is also recorded on the child-page reference so the catalog can apply
     * it (auto-open filler, search results).
     */
    public val onClick: (() -> Unit)? = null,
    /**
     * The composable content drawn inside the item's card. When null, the item renders the
     * standard row from its data ([title], [summary], [page], [icon], [widgetContainer],
     * [enabled], [onClick]) — for a page row that is exactly the [item] row.
     */
    public val content: (@Composable () -> Unit)? = null,
)

/**
 * Scope for [group]. Use [item] to add an item; each item is drawn in its own card, and the
 * items are indexed for search.
 */
public class GroupScope {
    internal val items = mutableListOf<GroupItem>()

    /**
     * Adds an item to the group; it is drawn in its own card.
     *
     * With [page], the item is a page row, like [item] in a page's content: it registers the
     * child-page reference (so the page joins the tree), is not indexed for search (the page
     * itself is searchable in its own right), and [title]/[summary]/[icon] default to the
     * page's.
     *
     * When [content] is null the item renders the standard row from its data (the [item] row);
     * when set, [content] is drawn inside the card instead, and [title]/[summary] describe the
     * row for search indexing (the content is composable and not walked by
     * [buildSearchIndex], so the row's own text cannot be read automatically). Pass the same
     * title/summary the row displays.
     *
     * @param page The page the item opens; when null, the item is a regular preference row.
     * @param title Title of the row. If null, [page]'s title is used.
     * @param summary Summary of the row. If null, [page]'s summary is used.
     * @param icon Icon to draw next to the text. If null, [page]'s icon is used.
     * @param widgetContainer Container to draw at the end of the preference row.
     * @param enabled Whether the row is enabled.
     * @param onClick Callback invoked when the row is clicked; for a page row, when null the
     *   row navigates into [page].
     * @param content Composable content drawn inside the item's card. If null, the standard
     *   row is rendered from the item's data.
     */
    public fun item(
        page: Page? = null,
        title: String? = null,
        summary: String? = null,
        icon: @Composable (() -> Unit)? = null,
        widgetContainer: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        content: (@Composable () -> Unit)? = null,
    ) {
        items.add(
            GroupItem(
                title = title ?: page?.title ?: "",
                summary = summary ?: page?.summary,
                page = page,
                icon = icon ?: page?.icon,
                widgetContainer = widgetContainer,
                enabled = enabled,
                onClick = onClick,
                content = content,
            )
        )
    }
}

/**
 * Renders a group of items, each drawn in its own Material Design 3 card of [style],
 * separated by [itemSpacing]. The first item shows rounded top corners, the last item shows
 * rounded bottom corners, and middle items have square corners, giving the impression of a
 * single card split into individual rows. With [CardStyle.None], the items are drawn
 * plainly, without a card.
 *
 * @param style Style of the individual cards.
 * @param modifier Modifier used to draw the group.
 * @param itemSpacing Gap between the individual cards.
 * @param shape Shape of the individual cards. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, the default container color of [style] is
 * used.
 * @param outerPadding Clearance between the group and its container. If null,
 * `PreferenceTheme.horizontalSpacing` is used on all sides.
 * @param items The pre-built items of the group, as produced by [GroupScope].
 */
@Composable
public fun Group(
    style: CardStyle = CardStyle.Filled,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 4.dp,
    shape: Shape? = null,
    cardColor: Color? = null,
    outerPadding: PaddingValues? = null,
    items: List<GroupItem>,
) {
    val outer = outerPadding ?: PaddingValues(LocalPreferenceTheme.current.horizontalSpacing)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(outer),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        if (style == CardStyle.None) {
            items.forEach { item ->
                if (item.content != null) {
                    item.content()
                } else {
                    Item(
                        title = item.title,
                        modifier = Modifier.fillMaxWidth(),
                        page = item.page,
                        summary = item.summary,
                        icon = item.icon,
                        widgetContainer = item.widgetContainer,
                        enabled = item.enabled,
                        onClick = item.onClick,
                    )
                }
            }
            return
        }
        val cardShape = shape ?: MaterialTheme.shapes.medium
        val cornerSize =
            (cardShape as? RoundedCornerShape ?: RoundedCornerShape(12.dp)).topStart
        val zero = CornerSize(0f)
        val last = items.size - 1
        items.forEachIndexed { index, item ->
            val itemShape =
                when {
                    last <= 0 -> cardShape
                    index == 0 -> RoundedCornerShape(cornerSize, cornerSize, zero, zero)
                    index == last -> RoundedCornerShape(zero, zero, cornerSize, cornerSize)
                    else -> RoundedCornerShape(zero)
                }
            CardSurface(
                style = style,
                shape = itemShape,
                cardColor = cardColor,
            ) {
                if (item.content != null) {
                    item.content()
                } else {
                    Item(
                        title = item.title,
                        modifier = Modifier.fillMaxWidth(),
                        page = item.page,
                        summary = item.summary,
                        icon = item.icon,
                        widgetContainer = item.widgetContainer,
                        enabled = item.enabled,
                        onClick = item.onClick,
                    )
                }
            }
        }
    }
}

/**
 * Adds a group of items to the lazy list, each drawn in its own card of [style], with the
 * first showing rounded top corners and the last rounded bottom corners.
 *
 * @param key Key used to identify the group in the lazy list. If null, no key is used.
 * @param style Style of the individual cards.
 * @param modifier Modifier used to draw the group.
 * @param itemSpacing Gap between the individual cards.
 * @param shape Shape of the individual cards. If null, `MaterialTheme.shapes.medium` is used.
 * @param cardColor Card background color. If null, the default container color of [style] is
 * used.
 * @param outerPadding Clearance between the group and its container. If null,
 * `PreferenceTheme.horizontalSpacing` is used on all sides.
 * @param content Content of the group. Use [GroupScope.item] to add items. The content is
 * built eagerly (not composed) so that [buildSearchIndex] can read each item's title/summary
 * and index the group's rows.
 */
public fun LazyListScope.group(
    key: String? = null,
    style: CardStyle = CardStyle.Filled,
    modifier: Modifier = Modifier.fillMaxWidth(),
    itemSpacing: Dp = 4.dp,
    shape: Shape? = null,
    cardColor: Color? = null,
    outerPadding: PaddingValues? = null,
    content: GroupScope.() -> Unit,
) {
    // Runs the content (a plain data builder, not composable) before registering the item, so
    // the group's items are recorded with the index of the group's single lazy list item. A
    // stable key is always used (a generated one when the caller passes none) so a search
    // result in the group can be highlighted.
    val scope = GroupScope()
    scope.content()
    val groupKey = key ?: "group:${SearchIndexer.itemCount()}"
    val groupIndex = SearchIndexer.itemCount()
    val fixes =
        scope.items.mapIndexedNotNull { index, item ->
            when {
                // A page card: registers the child-page reference (with the action that
                // replaces navigation, if any); no search entry, as the page itself is
                // searchable in its own right.
                item.page != null -> {
                    SearchIndexer.recordSubPage(item.page, item.onClick)
                    null
                }
                else ->
                    SearchIndexer.record(
                            key = groupKey,
                            title = item.title,
                            summary = item.summary,
                        )
                        ?.let { it to groupIndex }
            }
        }
    SearchIndexer.setIndices(fixes.toMap())
    item(key = groupKey, contentType = "Group") {
        Group(
            style = style,
            modifier = modifier.then(highlightedKeyModifier(groupKey)),
            itemSpacing = itemSpacing,
            shape = shape,
            cardColor = cardColor,
            outerPadding = outerPadding,
            items = scope.items,
        )
    }
}
