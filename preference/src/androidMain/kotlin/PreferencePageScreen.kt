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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The adaptive two-pane settings screen: a list pane of [pages] with a search field, and a
 * detail pane showing the selected page's preferences.
 *
 * - Single-pane (e.g. a phone in portrait): selecting a page navigates to the detail pane;
 *   the top bar shows the page title with a back arrow, and system back pops the detail
 *   before dismissing the screen.
 * - Two-pane (e.g. wide windows): both panes are visible at once; the top bar shows the
 *   screen title and there is no navigation.
 *
 * The list pane shows an MD3-style search pill; while a query is entered, the page list is
 * replaced by the matching pages and preference entries, and selecting a result clears the
 * query and opens the page.
 *
 * @param title Title of the screen, shown in the top bar.
 * @param pages The pages to show in the list pane, in order.
 * @param modifier Modifier applied to the root surface.
 * @param onBack Called when the host should dismiss the screen (i.e. system back while the
 * list pane is on screen in a single-pane layout).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
public fun PreferencePageScreen(
    title: String,
    pages: List<PreferencePage>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    // calculatePaneScaffoldDirective only goes two-pane at the "Expanded" width class
    // (>= 720dp). Unfolded foldables sit right on that boundary (e.g. 719dp -> "Medium"),
    // so the default directive keeps them single-pane in both portrait and landscape.
    // A settings list of short page titles is comfortably usable in a medium-width window,
    // so use the "two panes on medium width" variant to activate the detail pane there too.
    val directive =
        remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowAdaptiveInfo)
        }
    val isTwoPane = directive.maxHorizontalPartitions >= 2
    val navigator =
        rememberListDetailPaneScaffoldNavigator<String>(
            scaffoldDirective = directive,
            adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
            isDestinationHistoryAware = true,
        )
    val scope = rememberCoroutineScope()

    // The search field is the first focusable in the list pane, so the focus system restores
    // focus to it on launch and when the list pane is restored, showing a brief focus flash and
    // keyboard. Keep it out of the focus tree during those transitions so focus is never gained
    // in the first place; it becomes focusable once the pane has settled.
    var fieldFocusEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(400)
        fieldFocusEnabled = true
    }

    // The pane whose content last had focus (e.g. the user was scrolling/interacting with the
    // detail list, or with the page list/search). Decides which pane a collapse to single-pane
    // lands on: the sub page if the detail was in use, the root list otherwise. Null until
    // focus is actually observed (fresh launch).
    var lastActivePane by rememberSaveable { mutableStateOf<ActivePane?>(null) }

    fun trackPaneFocus(pane: ActivePane) =
        Modifier.onFocusChanged {
            if (it.isFocused && lastActivePane != pane) lastActivePane = pane
        }

    // The scaffold's destination is the single source of truth for which page (if any) the
    // detail pane shows, so the top bar title and the list-pane selection highlight follow
    // the real navigation state (including across configuration changes such as rotation).
    val destination = navigator.currentDestination
    val selectedPageId: String? =
        if (destination?.pane == ListDetailPaneScaffoldRole.Detail) {
            destination.contentKey
        } else {
            null
        }
    // Keep the pane layout in sync with the window size:
    // - Growing to two-pane with no detail destination shows only the list pane until the user
    //   taps a row. Open a page right away (the last selected one, or the first page) so the
    //   two-pane layout appears immediately on rotation.
    // - Shrinking back to a single partition leaves the destination history pointing at the
    //   detail pane, which keeps the detail pane expanded (and the list hidden) until the user
    //   presses back. Pop back to the list so the layout collapses to single-pane immediately.
    LaunchedEffect(isTwoPane) {
        val onDetail = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
        when {
            isTwoPane && !onDetail ->
                navigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.Detail,
                    contentKey = navigator.currentDestination?.contentKey ?: pages.first().id,
                )
            isTwoPane && onDetail -> {
                // Material3's internal snapTo (in rememberThreePaneScaffoldNavigator) does not
                // reliably fire across a config change when the detail is already open, so the
                // scaffold state stays at the stale single-pane value. Force it to the derived
                // two-pane value.
                (navigator.scaffoldState as? MutableThreePaneScaffoldState)
                    ?.snapTo(navigator.scaffoldValue)
            }
            !isTwoPane && onDetail -> {
                if (lastActivePane != ActivePane.List) {
                    // The detail was in use (or focus was never observed, e.g. right after
                    // launch in two-pane), so keep it visible in single-pane too.
                    (navigator.scaffoldState as? MutableThreePaneScaffoldState)
                        ?.snapTo(navigator.scaffoldValue)
                } else {
                    // Keep the field out of the focus tree while the list pane is restored, so
                    // the focus system does not put focus (and the keyboard) back on it.
                    fieldFocusEnabled = false
                    navigator.navigateBack()
                    delay(400)
                    fieldFocusEnabled = true
                }
            }
        }
    }
    val textFieldState = rememberTextFieldState()
    var query by remember { mutableStateOf("") }
    // Keep the query in sync with the input field's text.
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect { text ->
            if (query != text) query = text
        }
    }
    val focusManager = LocalFocusManager.current
    val fieldFocusRequester = remember { FocusRequester() }
    var fieldFocused by remember { mutableStateOf(false) }

    fun clearQuery() {
        query = ""
        textFieldState.edit { replace(0, length, "") }
    }

    // The preference row that a search result navigated to is highlighted briefly, so the user
    // can find it in the opened page (mirrors the launcher settings behavior).
    var highlightedKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(highlightedKey) {
        if (highlightedKey != null) {
            delay(HIGHLIGHT_DURATION_MS)
            highlightedKey = null
        }
    }

    // The lazy list index of the row a search result navigated to; the detail pane scrolls to
    // it when set.
    var scrollToIndex by remember { mutableStateOf<Int?>(null) }

    fun backAction() {
        if (!isTwoPane && destination?.pane == ListDetailPaneScaffoldRole.Detail) {
            scope.launch {
                // Keep the field out of the focus tree while the list pane is restored, so
                // the focus system does not put focus (and the keyboard) back on it.
                fieldFocusEnabled = false
                navigator.navigateBack()
                delay(400)
                fieldFocusEnabled = true
            }
        } else {
            onBack()
        }
    }

    // System back: the scaffold pops detail -> list first, then the host leaves the screen.
    BackHandler(onBack = ::backAction)

    fun selectPage(pageId: String) {
        scope.launch {
            navigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = pageId)
        }
    }

    val isSearching = query.isNotEmpty()
    // The search index is built once by walking each page's preference tree, so it is always
    // in sync with the rows — no separate search entries to maintain.
    val index = remember(pages) { buildSearchIndex(pages) }
    val matches = remember(pages, index, query) { searchPreferencePages(pages, index, query) }
    val searchEntries =
        remember(matches) {
            // A unique id per row (several rows can share an entry key, e.g. the rows of the
            // same card), so the results list can key on it.
            var rowId = 0
            matches.flatMap { match ->
                buildList {
                    if (match.matches.isEmpty()) {
                        // The page itself matched (by title/summary): show the page row.
                        add(SearchEntry(id = rowId++, page = match.page, entry = null))
                    }
                    // Show one row per matching preference entry of the page's tree.
                    addAll(match.matches.map { SearchEntry(id = rowId++, page = match.page, entry = it) })
                }
            }
        }

    val currentPage = selectedPageId?.let { id -> pages.firstOrNull { it.id == id } }
    val showBack =
        !isTwoPane && destination?.pane == ListDetailPaneScaffoldRole.Detail && currentPage != null

    // Collapsing header: a single persistent 56dp bar (the title bar) that never
    // vanishes. Its title morphs continuously between 30sp (expanded) and 22sp
    // (collapsed), and the search pill — the first in-list item — slides behind the
    // bar as the list scrolls, so the header's effective height shrinks from
    // bar+pill to just the bar. A faded search affordance fades into the bar's
    // trailing edge and, when tapped, scrolls the pill back into view and focuses
    // the field. The same pattern (bar height as a pure function of scroll offset)
    // applies to the detail pane, whose bar carries the back arrow in single-pane
    // mode. Offsets are observed via snapshotFlow because reading them directly in
    // composition does not reliably schedule a recomposition.
    val listState = remember { LazyListState() }
    val detailState = rememberLazyListState()
    // The scroll distance at which the search pill (56dp inset + 48dp tall) has fully
    // slid behind the 56dp bar: 104 - 56 = 48dp.
    val listHeaderHeight = with(LocalDensity.current) { 48.dp.toPx() }
    val barHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    fun headerProgress(state: LazyListState, distancePx: Float): Float =
        if (state.firstVisibleItemIndex == 0) {
            (state.firstVisibleItemScrollOffset / distancePx).coerceIn(0f, 1f)
        } else {
            1f
        }
    var listHeaderProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState) {
        snapshotFlow { headerProgress(listState, listHeaderHeight) }
            .collect { listHeaderProgress = it }
    }
    var detailHeaderProgressRaw by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(detailState) {
        snapshotFlow { headerProgress(detailState, barHeightPx) }
            .collect { detailHeaderProgressRaw = it }
    }
    // In single-pane the detail page has no big header of its own, and the back arrow
    // lives in the bar — so the bar is always shown there; in two-pane it grows in as
    // the page scrolls.
    val detailHeaderProgress = if (showBack) 1f else detailHeaderProgressRaw

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                scaffoldState = navigator.scaffoldState,
                listPane = {
                    AnimatedPane {
                        Box(Modifier.fillMaxSize().then(trackPaneFocus(ActivePane.List))) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                // The search pill: an in-list item that scrolls with the
                                // content. The 56dp top spacer pushes the pill just
                                // below the fixed 56dp header bar when the list is at
                                // the top; as the list scrolls the pill slides up behind
                                // the bar, collapsing the header down to just the bar.
                                item {
                                    Column(Modifier.fillMaxWidth()) {
                                        Spacer(Modifier.height(56.dp))
                                        // An MD3-style search pill. The results are shown
                                        // inline in this pane, so a plain text field is
                                        // used instead of the state-based
                                        // SearchBarDefaults.InputField, whose touch-mode
                                        // focus coupling would force-expand the bar.
                                        Surface(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .padding(start = 8.dp, end = 8.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                        ) {
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(start = 8.dp, end = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    // A fixed-width tappable slot; the
                                                    // icon is centered in it, so its edge
                                                    // lands 16.dp from the pill edge (8.dp
                                                    // row padding + 8.dp within the slot).
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
                                                            modifier =
                                                                Modifier.align(Alignment.CenterStart),
                                                        )
                                                    }
                                                    BasicTextField(
                                                        state = textFieldState,
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .then(
                                                                    if (fieldFocusEnabled) {
                                                                        Modifier
                                                                    } else {
                                                                        Modifier.focusProperties {
                                                                            canFocus = false
                                                                        }
                                                                    },
                                                                )
                                                                .focusRequester(fieldFocusRequester)
                                                                .onFocusChanged { fieldFocused = it.isFocused },
                                                        textStyle =
                                                            MaterialTheme.typography.bodyLarge.copy(
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            ),
                                                        cursorBrush =
                                                            SolidColor(MaterialTheme.colorScheme.primary),
                                                        lineLimits = TextFieldLineLimits.SingleLine,
                                                    )
                                                }
                                                if (query.isNotEmpty()) {
                                                    IconButton(onClick = ::clearQuery) {
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
                                if (isSearching) {
                                    items(searchEntries, key = { it.id }) {
                                        entry ->
                                        val matchedEntry = entry.entry
                                        SearchEntryRow(
                                            // An entry match shows the preference's own title
                                            // with the page as its subtitle; a page match
                                            // shows the page's title and summary.
                                            entry = matchedEntry,
                                            page = entry.page,
                                            onClick = {
                                                clearQuery()
                                                if (matchedEntry != null) {
                                                    highlightedKey = matchedEntry.key
                                                    scrollToIndex = matchedEntry.index
                                                }
                                                selectPage(entry.page.id)
                                            },
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
                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        // The page rows are each drawn in their own card,
                                        // with the first and last showing rounded
                                        // top/bottom corners.
                                        PreferenceCardGroup {
                                            pages.forEach { page ->
                                                card {
                                                    PreferencePageRow(
                                                        page = page,
                                                        selected = page.id == selectedPageId,
                                                        onClick = { selectPage(page.id) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // The single persistent header: a fixed 56dp bar at the top
                            // that never vanishes — the header's height changes by the
                            // pill sliding in/out below it, and the title morphs
                            // continuously in place (22sp collapsed → 30sp expanded).
                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopStart)
                                        .height(56.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 6.dp * listHeaderProgress,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = title,
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .padding(horizontal = 16.dp),
                                        style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontSize = lerp(30f, 22f, listHeaderProgress).sp,
                                            ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    // A search affordance that fades in on the right as
                                    // the pill collapses behind the bar; tapping it
                                    // scrolls the pill back into view and focuses it.
                                    Box(
                                        modifier =
                                            Modifier
                                                .height(48.dp)
                                                // Alpha must wrap the background, so
                                                // the whole affordance — pill and
                                                // icon — fades, not just the icon.
                                                .alpha(listHeaderProgress)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                )
                                                .padding(horizontal = 20.dp)
                                                .clickable {
                                                    scope.launch {
                                                        listState.animateScrollToItem(0)
                                                        fieldFocusRequester.requestFocus()
                                                    }
                                                },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Search",
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                detailPane = {
                    AnimatedPane {
                        val page = currentPage
                        if (page != null) {
                            CompositionLocalProvider(
                                LocalHighlightedPreferenceKey provides highlightedKey,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .then(trackPaneFocus(ActivePane.Detail)),
                                ) {
                                    // The detail state is shared across pages, so a page
                                    // change starts at the top (the old offset would not
                                    // mean anything with the new rows).
                                    LaunchedEffect(page.id) {
                                        detailState.scrollToItem(0)
                                    }
                                    // Scroll to the row a search result navigated to, once
                                    // the page's list is composed (the detail pane is
                                    // composed asynchronously, so wait for it to be laid
                                    // out).
                                    LaunchedEffect(page.id, scrollToIndex) {
                                        val target = scrollToIndex ?: return@LaunchedEffect
                                        // The detail pane is composed asynchronously, so
                                        // wait for the page's list to be laid out with
                                        // enough items first.
                                        snapshotFlow { detailState.layoutInfo.totalItemsCount }
                                            .first { it > target }
                                        detailState.animateScrollToItem(target)
                                        scrollToIndex = null
                                    }
                                    LazyColumn(
                                        state = detailState,
                                        modifier = Modifier.fillMaxSize(),
                                        // The compact bar overlays the top of the list and is always
                                        // visible in single-pane (where the back arrow lives); inset
                                        // the first item by the bar's height so the page's first row —
                                        // e.g. its top category header — isn't hidden beneath it.
                                        contentPadding =
                                            if (showBack) PaddingValues(top = 56.dp) else PaddingValues(),
                                    ) {
                                        page.content(this)
                                    }
                                    // The compact bar: declared after the list so it draws
                                    // above the content, and clipped to a fraction of its
                                    // height so it slides down from the top as the page's
                                    // first row scrolls away.
                                    if (detailHeaderProgress > 0.01f) {
                                        Surface(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.TopStart)
                                                    .height(56.dp * detailHeaderProgress)
                                                    .clipToBounds(),
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 6.dp * detailHeaderProgress,
                                        ) {
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (showBack) {
                                                    IconButton(onClick = ::backAction) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ArrowBack,
                                                            contentDescription = "Back",
                                                        )
                                                    }
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
                    }
                },
            )
        }
    }
}

/** The panes that can hold focus; the last one to do so decides single-pane landing. */
private enum class ActivePane { List, Detail }

/** A search result row: a page, or a preference entry of one of its pages. */
private data class SearchEntry(val id: Int, val page: PreferencePage, val entry: SearchIndexEntry?)

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
            Modifier.fillMaxWidth().background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
            ),
    )
}

/**
 * One search result row, styled as an MD3 [ListItem] (as in the material3 search bar samples):
 * a leading search icon, the entry's title, and a supporting line naming the page it lives in.
 * A page-level match (no [entry]) shows the page's own title and summary instead.
 */
@Composable
private fun SearchEntryRow(
    entry: SearchIndexEntry?,
    page: PreferencePage,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = entry?.title ?: page.title) },
        supportingContent = {
            val supporting = entry?.let { page.title } ?: page.summary
            if (!supporting.isNullOrEmpty()) {
                Text(text = supporting)
            }
        },
        leadingContent = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/** How long a search-selected preference row stays highlighted in the detail pane. */
private const val HIGHLIGHT_DURATION_MS = 2000L
