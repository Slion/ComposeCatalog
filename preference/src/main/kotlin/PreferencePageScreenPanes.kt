/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.preference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

/**
 * The breadcrumb that spans both panes in two-pane mode: the screen title, a back chevron
 * (when nested in a sub-page, so the user can pop one level up without hunting the trail),
 * and the current page's trail. Tapping an ancestor segment pops the trail back to it;
 * the last (current) segment is plain text.
 */
@Composable
internal fun BreadcrumbBar(
    title: String,
    currentPath: List<PreferencePage>,
    pagePath: List<String>,
    onNavigateToPath: (List<String>) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentPath.size > 1) {
                IconButton(onClick = { onNavigateToPath(pagePath.dropLast(1)) }) {
                    BackChevron()
                }
            }
            Text(
                text = title,
                modifier =
                    Modifier.padding(
                        start = if (currentPath.size > 1) 0.dp else 16.dp,
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // One chevron-separated segment per level of the current page's trail.
            currentPath.forEachIndexed { i, page ->
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(horizontal = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (i == currentPath.lastIndex) {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = page.title,
                        modifier =
                            Modifier.clickable { onNavigateToPath(pagePath.take(i + 1)) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The list pane: the in-list search pill, the page rows (or the search results while
 * querying), the host's root rows, and — in single-pane mode — the fixed 56dp title bar
 * the list scrolls under. Owns the pane's [LazyListState] and its scroll-to-selection
 * effects.
 *
 * The search pill's text field is kept out of the focus tree while [fieldFocusEnabled]
 * is false (launch and pane transitions), so the focus system never restores focus (and
 * the keyboard) to it mid-transition.
 */
@Composable
internal fun ListPane(
    title: String,
    isTwoPane: Boolean,
    listRows: List<PreferencePage>,
    selectedPageId: String?,
    onSelectPage: (String) -> Unit,
    isAtRootLevel: Boolean,
    rootContent: LazyListScope.() -> Unit,
    isSearching: Boolean,
    searchEntries: List<SearchEntry>,
    onSearchEntryClick: (SearchEntry) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    fieldFocusEnabled: Boolean,
    highlightedKey: String?,
    fadingEdges: Boolean,
    listScrollToIndex: Int?,
    onListScrollConsumed: () -> Unit,
    onActive: () -> Unit,
) {
    val listState = remember { LazyListState() }
    // Keep the list pane showing the page currently open in the detail: when the row set
    // changes (popping up the tree in two-pane, opening a page from a search result), the
    // pane's scroll offset would otherwise be left stale — e.g. scrolled past the end of
    // a new, shorter list, with the selected row out of view. Keyed on the row set's ids
    // (not the list instance — hosts rebuild the list on unrelated recompositions, e.g. a
    // theme change — and not on selectedPageId: a tap does not swap the set at the root
    // level), so only a navigation that changes the level re-triggers it.
    LaunchedEffect(listRows.map { it.id }, isSearching) {
        if (isSearching || listScrollToIndex != null) return@LaunchedEffect
        val sel = selectedPageId ?: return@LaunchedEffect
        // Item 0 is the search pill; the page rows follow from index 1.
        val target = 1 + listRows.indexOfFirst { it.id == sel }
        if (target < 1) return@LaunchedEffect
        // The list is recomposed with the new rows asynchronously; wait until the target
        // row exists before scrolling to it (same pattern as the detail pane).
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > target }
        // No-op when the row is already on screen, so a restart never fights a scroll
        // the user is in the middle of.
        if (listState.layoutInfo.visibleItemsInfo.any { it.index == target }) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(target)
    }
    // Scroll to a root row a search result selected, once it is laid out.
    LaunchedEffect(listScrollToIndex) {
        val target = listScrollToIndex ?: return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > target }
        listState.animateScrollToItem(target)
        onListScrollConsumed()
    }

    val edge = if (fadingEdges) PANE_FADING_EDGE_LENGTH else 0.dp
    // Root rows highlight on a search selection, as detail rows do.
    CompositionLocalProvider(LocalHighlightedPreferenceKey provides highlightedKey) {
        Box(
            Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onActive() },
        ) {
            LazyColumn(
                state = listState,
                // In single-pane the fixed 56dp header overlays the top of the list, so
                // the top fade is offset down below it (and the content padded past it);
                // in two-pane the fade sits at the very top, just below the breadcrumb.
                modifier =
                    if (fadingEdges) {
                        Modifier
                            .fillMaxSize()
                            .paneFadingEdges(topOffset = if (isTwoPane) 0.dp else 56.dp)
                    } else {
                        Modifier.fillMaxSize()
                    },
                // Inset the content past the static fading edges so items scroll under
                // the fades instead of ending right at the pane boundary; the fixed
                // single-pane header bar adds to the top inset.
                contentPadding =
                    PaddingValues(
                        top =
                            if (isTwoPane) {
                                edge
                            } else {
                                56.dp + edge
                            },
                        bottom = edge,
                    ),
            ) {
                // The search pill: an in-list item at the top of the list pane.
                // In single-pane it sits below the fixed 56dp header bar (accounted for
                // in the content padding); in two-pane a little breathing room below
                // the breadcrumb.
                item {
                    SearchPill(
                        isTwoPane = isTwoPane,
                        query = query,
                        onQueryChange = onQueryChange,
                        focusEnabled = fieldFocusEnabled,
                    )
                }
                if (isSearching) {
                    items(searchEntries, key = { it.id }) { entry ->
                        SearchEntryRow(
                            // An entry match shows the preference's own title with the
                            // page's trail as its subtitle; a page match shows the
                            // page's title and summary.
                            entry = entry.entry,
                            page = entry.page,
                            path = entry.path,
                            onClick = { onSearchEntryClick(entry) },
                        )
                    }
                    if (searchEntries.isEmpty()) {
                        item {
                            Text(
                                text = "No matching pages",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    // The page rows are each drawn in their own card, with the first and
                    // last showing rounded top/bottom corners (mirroring
                    // PreferenceCardGroup). They are the children of the page currently
                    // shown in the detail pane (the top-level pages while at the root).
                    // Each row is its own lazy item — not one group item — so the pane
                    // can scroll to the selected row (e.g. after popping up the tree in
                    // two-pane, when the row set changes).
                    items(listRows.size, key = { listRows[it].id }) { index ->
                        val page = listRows[index]
                        val horizontalSpacing =
                            LocalPreferenceTheme.current.horizontalSpacing
                        val cardShape = MaterialTheme.shapes.medium
                        val cornerSize =
                            (cardShape as? RoundedCornerShape
                                ?: RoundedCornerShape(12.dp)).topStart
                        // A flat surface (the M3 card color, no elevation): a per-row
                        // shadow on every visible row is pure scroll cost for a
                        // settings list.
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shadowElevation = 0.dp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        // The 8.dp replaces the spacer the group used
                                        // to lead with; the 4.dp between rows is the
                                        // group's item spacing.
                                        start = horizontalSpacing,
                                        top =
                                            if (index == 0) {
                                                8.dp + horizontalSpacing
                                            } else {
                                                4.dp
                                            },
                                        end = horizontalSpacing,
                                        bottom =
                                            if (index == listRows.lastIndex) {
                                                horizontalSpacing
                                            } else {
                                                0.dp
                                            },
                                    ),
                            shape =
                                when {
                                    listRows.size <= 1 -> cardShape
                                    index == 0 ->
                                        RoundedCornerShape(
                                            cornerSize,
                                            cornerSize,
                                            CornerSize(0f),
                                            CornerSize(0f),
                                        )
                                    index == listRows.lastIndex ->
                                        RoundedCornerShape(
                                            CornerSize(0f),
                                            CornerSize(0f),
                                            cornerSize,
                                            cornerSize,
                                        )
                                    else -> RoundedCornerShape(0.dp)
                                },
                        ) {
                            PreferencePageRow(
                                page = page,
                                selected = page.id == selectedPageId,
                                // A page with its own onClick is an action row: tapping
                                // it runs the action instead of navigating.
                                onClick = {
                                    page.onClick?.invoke() ?: onSelectPage(page.id)
                                },
                            )
                        }
                    }
                    // The root level hosts regular preferences below the page rows
                    // (any builder works, as in a page's content).
                    if (isAtRootLevel) {
                        rootContent(this)
                    }
                }
            }
            // The single persistent header: a fixed 56dp bar at the top that never
            // vanishes and never changes — just the title at a single size. The search
            // lives in the in-list pill, which simply scrolls behind the bar. In
            // two-pane mode it is not shown at all: the breadcrumb above the panes
            // carries the title, and the search pill is a plain in-list item there.
            if (!isTwoPane) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .height(56.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The MD3-style search pill, an in-list item at the top of the list pane. Owns the text
 * field and its focus state; [query] mirrors the field's text in both directions (typing
 * updates the query; a query change from outside — e.g. a result selection clearing it —
 * empties the field).
 *
 * While [focusEnabled] is false the field is kept out of the focus tree entirely, so
 * the focus system does not restore focus (and show the keyboard) on it during a pane
 * transition.
 */
@Composable
private fun SearchPill(
    isTwoPane: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    focusEnabled: Boolean,
) {
    val textFieldState = rememberTextFieldState()
    // Keep the query in sync with the field's text. Keyed on query so the collector
    // always reads the current value (a coroutine closure would otherwise capture the
    // value from the composition that started it).
    LaunchedEffect(query) {
        snapshotFlow { textFieldState.text.toString() }.collect { text ->
            if (text != query) onQueryChange(text)
        }
    }
    // A query change from outside (clearing after a result selection) empties the field.
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) {
            textFieldState.edit { replace(0, length, query) }
        }
    }
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        if (isTwoPane) {
            Spacer(Modifier.height(8.dp))
        }
        // The results are shown inline in this pane, so a plain text field is used
        // instead of the state-based SearchBarDefaults.InputField, whose touch-mode
        // focus coupling would force-expand the bar.
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = LocalPreferenceTheme.current.horizontalSpacing),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    // A fixed-width tappable slot; the icon is centered in it, so its
                    // edge lands 16.dp from the pill edge (8.dp row padding + 8.dp
                    // within the slot).
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (fieldFocused) {
                                    Modifier.clickable {
                                        focusManager.clearFocus(force = true)
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (fieldFocused) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Dismiss search",
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    BasicTextField(
                        state = textFieldState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (focusEnabled) {
                                        Modifier
                                    } else {
                                        Modifier.focusProperties { canFocus = false }
                                    },
                                )
                                .onFocusChanged { fieldFocused = it.isFocused },
                        textStyle =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                        )
                    }
                }
            }
        }
    }
}

/**
 * The detail pane: the selected page's sub-page rows above its own preferences, and —
 * in single-pane mode — the fixed 56dp compact bar (back arrow + title) the list scrolls
 * under. Owns the pane's [LazyListState] (shared across pages, so a page change starts
 * at the top) and the scroll-to-row effect for a selected search result.
 */
@Composable
internal fun DetailPane(
    page: PreferencePage?,
    selectedPageId: String?,
    onSelectPage: (String) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    highlightedKey: String?,
    fadingEdges: Boolean,
    scrollToIndex: Int?,
    onDetailScrollConsumed: () -> Unit,
    onActive: () -> Unit,
) {
    if (page == null) return
    val detailState = rememberLazyListState()
    LaunchedEffect(page.id) {
        detailState.scrollToItem(0)
    }
    // Scroll to the row a search result navigated to, once the page's list is composed
    // (the detail pane is composed asynchronously, so wait for it to be laid out with
    // enough items first).
    LaunchedEffect(page.id, scrollToIndex) {
        val target = scrollToIndex ?: return@LaunchedEffect
        snapshotFlow { detailState.layoutInfo.totalItemsCount }
            .first { it > target }
        detailState.animateScrollToItem(target)
        onDetailScrollConsumed()
    }
    val edge = if (fadingEdges) PANE_FADING_EDGE_LENGTH else 0.dp
    CompositionLocalProvider(LocalHighlightedPreferenceKey provides highlightedKey) {
        Box(
            Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onActive() },
        ) {
            LazyColumn(
                state = detailState,
                // In single-pane the compact bar overlays the top of the list, so the
                // top fade is offset down below it; in two-pane there is no bar, so the
                // fade sits at the very top, just below the breadcrumb.
                modifier =
                    if (fadingEdges) {
                        Modifier
                            .fillMaxSize()
                            .paneFadingEdges(topOffset = if (showBack) 56.dp else 0.dp)
                    } else {
                        Modifier.fillMaxSize()
                    },
                // The compact bar overlays the top of the list and is always visible in
                // single-pane (where the back arrow lives); inset the first item by the
                // bar's height so the page's first row — e.g. its top category header —
                // isn't hidden beneath it. The top and bottom insets also keep the
                // content clear of the static fading edges so items scroll under them.
                contentPadding =
                    PaddingValues(
                        top =
                            if (showBack) {
                                56.dp + edge
                            } else {
                                edge
                            },
                        bottom = edge,
                    ),
            ) {
                // This page's child pages, as rows above its own preferences: tapping
                // one navigates deeper into the tree (the list pane switches to that
                // page's children).
                if (page.subPages.isNotEmpty()) {
                    items(page.subPages, key = { sub -> "subpage:${sub.id}" }) { sub ->
                        PreferencePageRow(
                            page = sub,
                            selected = sub.id == selectedPageId,
                            onClick = { sub.onClick?.invoke() ?: onSelectPage(sub.id) },
                        )
                    }
                }
                page.content(this)
            }
            // The compact bar: declared after the list so it draws above the content.
            // Static — always shown in full whenever the detail page is open in
            // single-pane, with the content scrolling under it (the pane's top fade
            // provides the separation). In two-pane mode it is never shown: the
            // breadcrumb above the panes already names the page, so the bar would only
            // duplicate it.
            if (showBack) {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .height(56.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            BackChevron()
                        }
                        Text(
                            text = page.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One row of the list pane, styled with the library's preference theme.
 */
@Composable
private fun PreferencePageRow(
    page: PreferencePage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Preference(
        title = page.title,
        summary = page.summary,
        icon = page.icon,
        actionIcon = {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
            )
        },
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    } else {
                        Color.Transparent
                    },
                ),
    )
}

/**
 * One search result row, styled as an MD3 [ListItem] (as in the material3 search bar
 * samples): a leading search icon, the entry's title, and a supporting line naming where
 * the page lives in the tree. A page-level match (no [entry]) shows the page's own title
 * and summary instead.
 */
@Composable
private fun SearchEntryRow(
    entry: SearchIndexEntry?,
    page: PreferencePage?,
    path: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = entry?.title ?: page?.title.orEmpty()) },
        supportingContent = {
            val supporting =
                if (entry != null) {
                    // The entry lives in [page]; the page's trail (e.g. "Theme > Colors")
                    // shows where. Top-level pages have a one-segment trail, equal to
                    // their own title — show just it.
                    path
                } else {
                    page?.summary
                }
            if (!supporting.isNullOrEmpty()) {
                Text(text = supporting)
            }
        },
        leadingContent = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/**
 * A left-pointing chevron used as the back affordance, matching the chevron style of the
 * breadcrumb separators (a right chevron rotated 180°) rather than the filled arrow.
 */
@Composable
private fun BackChevron() {
    Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = "Back",
        modifier = Modifier.size(24.dp).rotate(180f),
    )
}

/** Length of the gradient that fades out the pane content at its top and bottom edges. */
private val PANE_FADING_EDGE_LENGTH = 32.dp

/**
 * Static fading edges for a vertical scroll area: short surface-colored gradients at the
 * top and bottom of the pane that are always present. The panes pair this with vertical
 * [androidx.compose.foundation.layout.PaddingValues] of the same length on their
 * [LazyColumn], so the content scrolls under the fades instead of the fades popping in
 * and out with the scroll position.
 *
 * [topOffset] pushes the top fade down below an opaque bar that overlays the top of the
 * list in single-pane mode (the fixed list header, or the detail compact bar); without it
 * the fade would be painted behind the bar and never visible. In two-pane there is no
 * such overlay, so it stays 0.
 */
@Composable
private fun Modifier.paneFadingEdges(topOffset: Dp = 0.dp): Modifier {
    // Captured in composition (theme/density are composable reads), then used from the
    // non-composable draw lambda. The whole modifier is remembered so the pane's
    // [LazyColumn] sees a stable modifier instance across recompositions (a fresh
    // drawWithContent each time would defeat the lazy list's skip). The brushes are
    // built in the draw cache, not on every draw pass.
    val base = this
    val surface = MaterialTheme.colorScheme.surface
    val edgePx = with(LocalDensity.current) { PANE_FADING_EDGE_LENGTH.toPx() }
    val topOffsetPx = with(LocalDensity.current) { topOffset.toPx() }
    return remember(base, surface, edgePx, topOffsetPx) {
        base.drawWithCache {
            // Gradient coordinates are in canvas space, so the bottom brush is built from
            // the cache's size (the cache is invalidated when it changes).
            val topBrush =
                Brush.verticalGradient(
                    colors = listOf(surface, surface.copy(alpha = 0f)),
                    startY = topOffsetPx,
                    endY = topOffsetPx + edgePx,
                )
            val bottomBrush =
                Brush.verticalGradient(
                    colors = listOf(surface.copy(alpha = 0f), surface),
                    startY = size.height - edgePx,
                    endY = size.height,
                )
            onDrawWithContent {
                drawContent()
                // Top edge, starting below any overlaying bar.
                drawRect(
                    brush = topBrush,
                    topLeft = Offset(0f, topOffsetPx),
                    size = Size(size.width, edgePx),
                )
                // Bottom edge.
                drawRect(
                    brush = bottomBrush,
                    topLeft = Offset(0f, size.height - edgePx),
                    size = Size(size.width, edgePx),
                )
            }
        }
    }
}
