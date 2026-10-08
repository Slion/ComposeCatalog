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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/** Log tag for the fold/resize transition tracing (enable with logcat -s PrefPageFold). */
private const val LOG_TAG = "PrefPageFold"

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
 * @param backEnabled Whether the screen's system-back handling is active. A host that
 * covers the screen with its own layer (e.g. a fragment or view shown over it) passes
 * false so that back goes to that layer — and its back stack — first.
 * @param adaptiveInfo Adaptive layout info computed for the host of the screen instead of
 * for the window. The screen's adaptive layout (single- vs two-pane) follows this value,
 * which is what a host smaller than the window — e.g. a modal bottom sheet, a dialog, or a
 * split — passes: it measures its own size and builds a [WindowAdaptiveInfo] for it (with
 * no hinges, since the fold does not apply to the host), so the screen adapts to the host
 * rather than to the window. Null (the default) uses the window.
 * @param singlePaneOnly Forces the single-pane layout regardless of the (host) width, so a
 * host that must never split into a list + detail (e.g. a bottom sheet, a dialog) stays
 * single-pane even when it is wide; the list navigates to the detail and back.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
public fun PreferencePageScreen(
    title: String,
    pages: List<PreferencePage>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    backEnabled: Boolean = true,
    adaptiveInfo: WindowAdaptiveInfo? = null,
    singlePaneOnly: Boolean = false,
) {
    val density = LocalDensity.current
    // Both LocalWindowInfo.current.containerSize, currentWindowAdaptiveInfoV2(), and
    // Compose's onSizeChanged lag behind a non-recreating resize on foldables (e.g.
    // spreading the app across both Surface Duo screens): the actual window is already at
    // the new size, but the composition locals haven't caught up yet. The one source
    // proven to be current on every frame is the Android view itself: poll the root view's
    // size on the frame clock and feed that into the size class below.
    val rootView = LocalView.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    DisposableEffect(rootView) {
        // One Choreographer frame callback per frame; the view's width/height are
        // updated during layout, so this reads the size as of the last frame.
        @Suppress("DEPRECATION") // main-thread Choreographer, same instance Compose uses
        val choreographer = android.view.Choreographer.getInstance()
        val frameCallback =
            object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    val size = IntSize(rootView.width, rootView.height)
                    if (size.width > 0 && size.height > 0 && size != measuredSize) {
                        measuredSize = size
                        Timber.d("$LOG_TAG: SIZE view=${size.width}x${size.height}px")
                    }
                    choreographer.postFrameCallback(this)
                }
            }
        choreographer.postFrameCallback(frameCallback)
        onDispose { choreographer.removeFrameCallback(frameCallback) }
    }
    // Fallback for the very first frame before the poll has run.
    val containerSize = LocalWindowInfo.current.containerSize
    val effectiveSize = if (measuredSize != IntSize.Zero) measuredSize else containerSize
    // currentWindowAdaptiveInfoV2() provides the window posture (hinges), which we still
    // need. The size class is re-derived from the measured size above.
    val baseInfo = adaptiveInfo ?: currentWindowAdaptiveInfoV2()
    val windowAdaptiveInfo =
        remember(baseInfo, adaptiveInfo, effectiveSize, density) {
            if (adaptiveInfo != null) {
                baseInfo
            } else {
                // Build the size class the same way currentWindowAdaptiveInfoV2() does:
                // quantize the current dp size to the standard breakpoints (width to
                // 0/600/840, height to 0/480/900). The scaffold directive matches these
                // exact breakpoint values (it does `when (minWidthDp.dp) { 0.dp -> …;
                // 600.dp -> …; 840.dp -> … }`), so a raw, non-breakpoint dp such as 537
                // would not classify and would fall through to the default (three panes).
                val widthDp = with(density) { effectiveSize.width.toDp().value }
                val heightDp = with(density) { effectiveSize.height.toDp().value }
                WindowAdaptiveInfo(
                    // `compute` is deprecated in favour of `computeWindowSizeClass`, which is
                    // not resolvable against the window-core version this project compiles
                    // against, so the deprecated overload is used.
                    windowSizeClass = WindowSizeClass.Companion.compute(widthDp, heightDp),
                    windowPosture = baseInfo.windowPosture,
                )
            }
        }
    // calculatePaneScaffoldDirective only goes two-pane at the "Expanded" width class
    // (>= 720dp). Unfolded foldables sit right on that boundary (e.g. 719dp -> "Medium"),
    // so the default directive keeps them single-pane in both portrait and landscape.
    // A settings list of short page titles is comfortably usable in a medium-width window,
    // so use the "two panes on medium width" variant to activate the detail pane there too.
    //
    // HingePolicy.NeverAvoid: the default (AvoidSeparating) inserts the separating fold
    // hinge as an excluded bound, so in half-folded portrait the scaffold snaps the
    // pane divider onto the crease and the list pane grows past 50%. With NeverAvoid the
    // two preferredWidth(0.5f) panes stay exactly half each in every fold state.
    val directive =
        remember(windowAdaptiveInfo, singlePaneOnly) {
            val base =
                calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                    windowAdaptiveInfo = windowAdaptiveInfo,
                    verticalHingePolicy = HingePolicy.NeverAvoid,
                )
            // A host that must never split (a bottom sheet, a dialog) forces a single
            // partition so the screen stays single-pane at any width.
            if (singlePaneOnly) base.copy(maxHorizontalPartitions = 1) else base
        }
    val isTwoPane = directive.maxHorizontalPartitions >= 2

    // Fold-debug logging: record the actual measured pane widths so we can compare the
    // scaffold's input (window size + fold posture) against what it produced on each
    // fold transition. Filter logcat by "PrefPageFold".
    var listPaneSize by remember { mutableStateOf(IntSize.Zero) }
    var detailPaneSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(effectiveSize, windowAdaptiveInfo, isTwoPane) {
        Timber.d(
            "$LOG_TAG: IN  window=${effectiveSize.width}x${effectiveSize.height}px " +
                "wsc=${windowAdaptiveInfo.windowSizeClass} " +
                "posture=${windowAdaptiveInfo.windowPosture} " +
                "maxParts=${directive.maxHorizontalPartitions} twoPane=$isTwoPane"
        )
    }
    LaunchedEffect(listPaneSize, detailPaneSize) {
        if (listPaneSize != IntSize.Zero || detailPaneSize != IntSize.Zero) {
            Timber.d(
                "$LOG_TAG: OUT listPx=${listPaneSize.width} detailPx=${detailPaneSize.width} " +
                    "sum=${listPaneSize.width + detailPaneSize.width}"
            )
        }
    }

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
    // The navigation trail within the page tree: the chain of page ids from the top level
    // down to the page currently shown in the detail pane (empty = none selected). The
    // detail shows the deepest page; the list pane shows the rows of its parent (the
    // top-level pages while at the root). Tapping a sub-page row pushes its id, tapping a
    // breadcrumb segment (or back) pops to that ancestor.
    var pagePath by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    // True once the user explicitly opens a page (row tap, breadcrumb, search result).
    // Survives rotation so a later grow to two-pane can refill the detail pane; a pure
    // rotation round trip on a fresh launch must stay at the root level.
    var userSelectedPage by rememberSaveable { mutableStateOf(false) }
    fun pathOf(pageId: String): List<String> =
        findPagePath(pages, pageId)?.map { it.id } ?: listOf(pageId)
    fun selectPageById(pageId: String) {
        userSelectedPage = true
        pagePath = pathOf(pageId)
        scope.launch {
            navigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = pageId)
        }
    }
    fun navigateToPath(id: List<String>) {
        // An empty path means "back to the root list" (breadcrumb at the top level),
        // which is not a selection: forget the flag so size round trips stay at the root.
        userSelectedPage = id.isNotEmpty()
        pagePath = id
        if (id.isNotEmpty()) {
            scope.launch {
                navigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = id.last())
            }
        }
    }
    // Keep the pane layout in sync with the window size:
    // - Growing to two-pane with no detail destination opens a page right away (the last
    //   open one, or the first page) so the two-pane layout is not empty. The page is
    //   only a visual filler when the user never selected one: shrinking back pops to the
    //   root list, so a rotation round trip on a fresh launch ends where it started.
    // - Shrinking back to a single partition leaves the destination history pointing at the
    //   detail pane, which keeps the detail pane expanded (and the list hidden) until the user
    //   presses back. Pop back to the list so the layout collapses to single-pane immediately.
    LaunchedEffect(isTwoPane) {
        val onDetail = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
        when {
            isTwoPane && !onDetail -> {
                // A page with its own onClick is an action row, not a viewable page: the
                // auto-open filler skips it so the detail does not show its (empty) content.
                val id =
                    navigator.currentDestination?.contentKey
                        ?: pages.firstOrNull { it.onClick == null }?.id
                        ?: pages.first().id
                pagePath = pathOf(id)
                navigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.Detail,
                    contentKey = id,
                )
            }
            isTwoPane && onDetail -> {
                // Material3's internal snapTo (in rememberThreePaneScaffoldNavigator) does not
                // reliably fire across a config change when the detail is already open, so the
                // scaffold state stays at the stale single-pane value. Force it to the derived
                // two-pane value.
                (navigator.scaffoldState as? MutableThreePaneScaffoldState)
                    ?.snapTo(navigator.scaffoldValue)
            }
            !isTwoPane && onDetail -> {
                if (!userSelectedPage) {
                    // The detail shows a page we auto-opened as a visual filler for the
                    // two-pane layout; the user never chose it, so a rotation round trip
                    // must end back at the root list.
                    fieldFocusEnabled = false
                    navigator.navigateBack()
                    pagePath = emptyList()
                    delay(400)
                    fieldFocusEnabled = true
                } else if (lastActivePane != ActivePane.List) {
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
        val onDetail = destination?.pane == ListDetailPaneScaffoldRole.Detail
        if (onDetail && pagePath.size > 1) {
            // Nested in the tree (single- or two-pane): pop one level up (child -> parent)
            // before considering a navigation back or leaving the screen.
            scope.launch {
                // Keep the field out of the focus tree while the list pane is restored, so
                // the focus system does not put focus (and the keyboard) back on it.
                fieldFocusEnabled = false
                navigateToPath(pagePath.dropLast(1))
                delay(400)
                fieldFocusEnabled = true
            }
        } else if (!isTwoPane && onDetail) {
            scope.launch {
                // Keep the field out of the focus tree while the list pane is restored, so
                // the focus system does not put focus (and the keyboard) back on it.
                fieldFocusEnabled = false
                navigator.navigateBack()
                // The user walked back to the root list (the nested levels were already
                // popped above, so only a root-level page is open here). Forget the
                // selection so a later size round trip stays at the root too.
                pagePath = emptyList()
                userSelectedPage = false
                delay(400)
                fieldFocusEnabled = true
            }
        } else {
            onBack()
        }
    }

    // System back: the scaffold pops detail -> list first, then the host leaves the screen.
    // A host that covers the screen with its own layer disables this so that back goes to
    // that layer (and its back stack) first.
    BackHandler(enabled = backEnabled, onBack = ::backAction)

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
                // The page's trail in the tree (e.g. "Theme > Colors"), shown under each of
                // its result rows.
                val path = (findPagePath(pages, match.page.id)?.map { it.title } ?: emptyList())
                    .joinToString(" > ")
                buildList {
                    if (match.matches.isEmpty()) {
                        // The page itself matched (by title/summary): show the page row.
                        add(SearchEntry(id = rowId++, page = match.page, path = path, entry = null))
                    }
                    // Show one row per matching preference entry of the page's tree.
                    addAll(
                        match.matches.map {
                            SearchEntry(id = rowId++, page = match.page, path = path, entry = it)
                        },
                    )
                }
            }
        }

    val currentPage = selectedPageId?.let { id -> findPage(pages, id) }
    // The trail of pages from the top level to the current one, for the breadcrumb and the
    // search supporting text (may be stale for one frame after a navigation; it is re-derived
    // from the real destination below).
    val currentPath: List<PreferencePage> =
        remember(pages, selectedPageId) {
            if (selectedPageId != null) findPagePath(pages, selectedPageId) ?: emptyList() else emptyList()
        }
    // The parent of the page shown in the detail: its children are the rows of the list
    // pane (the top-level pages while at the root).
    val listRows: List<PreferencePage> =
        if (selectedPageId != null && currentPath.size > 1) {
            currentPath[currentPath.size - 2].subPages
        } else {
            pages
        }
    val showBack =
        !isTwoPane && destination?.pane == ListDetailPaneScaffoldRole.Detail && currentPage != null

    // Single-pane headers: the list pane carries a persistent, static 56dp title bar
    // with an always-visible search affordance; the search pill (the first in-list
    // item) simply scrolls behind it. Tapping the affordance scrolls the pill back
    // into view and focuses the field. The detail pane's compact bar is equally
    // static in single-pane (it always shows, carrying the back arrow), but in
    // two-pane it grows in as the page scrolls. Offsets are observed via
    // snapshotFlow because reading them directly in composition does not reliably
    // schedule a recomposition.
    val listState = remember { LazyListState() }
    // Keep the list pane showing the page currently open in the detail: when the row set
    // changes (popping up the tree in two-pane, opening a page from a search result), the
    // pane's scroll offset would otherwise be left stale — e.g. scrolled past the end of a
    // new, shorter list, with the selected row out of view. Keyed on the row set only (not
    // on selectedPageId): tapping a row does not swap the set (at the root level the rows
    // stay the top-level pages), so a tap must not re-scroll the list to the tapped row —
    // only a navigation that changes the level does.
    LaunchedEffect(listRows, isSearching) {
        if (isSearching) return@LaunchedEffect
        val sel = selectedPageId ?: return@LaunchedEffect
        // Item 0 is the search pill; the page rows follow from index 1.
        val target = 1 + listRows.indexOfFirst { it.id == sel }
        if (target < 1) return@LaunchedEffect
        // The list is recomposed with the new rows asynchronously; wait until the target
        // row exists before scrolling to it (same pattern as the detail pane).
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > target }
        listState.animateScrollToItem(target)
    }
    val detailState = rememberLazyListState()
    val barHeightPx = with(LocalDensity.current) { 56.dp.toPx() }
    fun headerProgress(state: LazyListState, distancePx: Float): Float =
        if (state.firstVisibleItemIndex == 0) {
            (state.firstVisibleItemScrollOffset / distancePx).coerceIn(0f, 1f)
        } else {
            1f
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
        // systemBarsPadding (status + navigation bars) so an edge-to-edge host keeps the
        // content clear of both bars while the surface fills the whole window.
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // A breadcrumb that spans both panes: the screen title, a separator, and the
            // page currently shown in the detail pane. Only present in two-pane mode, where
            // there is room to show the navigation trail; in single-pane the per-pane bars
            // (with their back arrow) already convey position.
            if (isTwoPane) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // A back chevron appears when we are nested inside a sub-page, so the
                        // user can pop one level up without hunting the breadcrumb trail.
                        if (currentPath.size > 1) {
                            IconButton(onClick = { navigateToPath(pagePath.dropLast(1)) }) {
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
                        // One chevron-separated segment per level of the current page's
                        // trail; tapping an ancestor pops the trail back to it. The last
                        // (current) segment is plain text.
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
                                    modifier = Modifier.clickable {
                                        navigateToPath(pagePath.take(i + 1))
                                    },
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
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                scaffoldState = navigator.scaffoldState,
                listPane = {
                    // The scaffold only reads preferredWidth from the parentData of the
                    // pane's root node (the AnimatedPane); on an inner Box it is ignored
                    // and the pane falls back to the directive's 360dp default, letting
                    // the higher-priority detail pane absorb all the leftover width.
                    AnimatedPane(
                        Modifier
                            .preferredWidth(0.5f)
                            .fillMaxSize(),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .onSizeChanged { listPaneSize = it }
                                .then(trackPaneFocus(ActivePane.List)),
                        ) {
                            LazyColumn(
                                state = listState,
                                // In single-pane the fixed 56dp header overlays the top of
                                // the list, so the top fade is offset down below it (and
                                // the content padded past it); in two-pane the fade sits at
                                // the very top, just below the breadcrumb.
                                modifier =
                                    Modifier.fillMaxSize()
                                        .paneFadingEdges(
                                            topOffset = if (isTwoPane) 0.dp else 56.dp,
                                        ),
                                // Inset the content past the static fading edges so items
                                // scroll under the fades instead of ending right at the
                                // pane boundary; the fixed single-pane header bar adds to
                                // the top inset.
                                contentPadding =
                                    PaddingValues(
                                        top =
                                            if (isTwoPane) {
                                                PANE_FADING_EDGE_LENGTH
                                            } else {
                                                56.dp + PANE_FADING_EDGE_LENGTH
                                            },
                                        bottom = PANE_FADING_EDGE_LENGTH,
                                    ),
                            ) {
                                // The search pill: an in-list item at the top of the
                                // list pane. In single-pane it sits below the fixed 56dp
                                // header bar; in two-pane there is no per-pane header,
                                // so it only needs a small top inset.
                                item {
                                    Column(Modifier.fillMaxWidth()) {
                                        // A little breathing room below the breadcrumb in
                                        // two-pane; in single-pane the fixed header bar
                                        // (56dp) is already accounted for in the
                                        // LazyColumn's content padding.
                                        if (isTwoPane) {
                                            Spacer(Modifier.height(8.dp))
                                        }
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
                                                    .padding(
                                                        horizontal =
                                                            LocalPreferenceTheme.current
                                                                .horizontalSpacing,
                                                    ),
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
                                            // with the page's trail as its subtitle; a page
                                            // match shows the page's title and summary.
                                            entry = matchedEntry,
                                            page = entry.page,
                                            path = entry.path,
                                            onClick = {
                                                if (entry.page.onClick != null) {
                                                    // An action page: the result just
                                                    // runs its action, no deep navigation.
                                                    clearQuery()
                                                    entry.page.onClick?.invoke()
                                                } else {
                                                    clearQuery()
                                                    if (matchedEntry != null) {
                                                        highlightedKey = matchedEntry.key
                                                        // The detail column lists the page's
                                                        // sub-page rows above its content, so
                                                        // shift the entry's content-relative
                                                        // index past them.
                                                        scrollToIndex =
                                                            matchedEntry.index + entry.page.subPages.size
                                                    }
                                                    selectPageById(entry.page.id)
                                                }
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
                                    // The page rows are each drawn in their own card, with
                                    // the first and last showing rounded top/bottom corners
                                    // (mirroring PreferenceCardGroup). They are the children
                                    // of the page currently shown in the detail pane (the
                                    // top-level pages while at the root). Each row is its
                                    // own lazy item — not one group item — so the pane can
                                    // scroll to the selected row (e.g. after popping up the
                                    // tree in two-pane, when the row set changes).
                                    items(listRows.size, key = { listRows[it].id }) {
                                        index ->
                                        val page = listRows[index]
                                        val horizontalSpacing =
                                            LocalPreferenceTheme.current.horizontalSpacing
                                        val cardShape = MaterialTheme.shapes.medium
                                        val cornerSize =
                                            (cardShape as? RoundedCornerShape
                                                ?: RoundedCornerShape(12.dp)).topStart
                                        Card(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        // The 8.dp replaces the spacer
                                                        // the group used to lead with;
                                                        // the 4.dp between rows is the
                                                        // group's item spacing.
                                                        start = horizontalSpacing,
                                                        top =
                                                            if (index == 0) {
                                                                8.dp +
                                                                    horizontalSpacing
                                                            } else {
                                                                4.dp
                                                            },
                                                        end = horizontalSpacing,
                                                        bottom =
                                                            if (index ==
                                                                listRows.lastIndex) {
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
                                                // A page with its own onClick is an action row: tapping it runs the action
                                                // instead of navigating to the detail.
                                                onClick = { page.onClick?.invoke() ?: selectPageById(page.id) },
                                            )
                                        }
                                    }
                                }
                            }
                            // The single persistent header: a fixed 56dp bar at the top
                            // that never vanishes and never changes — just the title at a
                            // single size. The search lives in the in-list pill, which
                            // simply scrolls behind the bar.
                            // In two-pane mode it is not shown at all: the breadcrumb
                            // above the panes carries the title, and the search pill is
                            // a plain in-list item there.
                            if (!isTwoPane) {
                                Surface(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.TopStart)
                                            .height(56.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 6.dp,
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
                },
                detailPane = {
                    // Mirror the list pane's 0.5f proportion on the AnimatedPane (the
                    // pane root) so both halves sum to the full width and the scaffold
                    // never has to scale them unequally (keeps the split 50/50 at any
                    // fold angle).
                    AnimatedPane(
                        Modifier
                            .preferredWidth(0.5f)
                            .fillMaxSize(),
                    ) {
                        val page = currentPage
                        if (page != null) {
                            CompositionLocalProvider(
                                LocalHighlightedPreferenceKey provides highlightedKey,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .onSizeChanged { detailPaneSize = it }
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
                                        // In single-pane the compact bar overlays the top of
                                        // the list, so the top fade is offset down below it;
                                        // in two-pane there is no bar, so the fade sits at the
                                        // very top, just below the breadcrumb.
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .paneFadingEdges(
                                                    topOffset = if (showBack) 56.dp else 0.dp,
                                                ),
                                        // The compact bar overlays the top of the list and is
                                        // always visible in single-pane (where the back arrow
                                        // lives); inset the first item by the bar's height so
                                        // the page's first row — e.g. its top category header —
                                        // isn't hidden beneath it. The top and bottom insets
                                        // also keep the content clear of the static fading
                                        // edges so items scroll under them.
                                        contentPadding =
                                            PaddingValues(
                                                top =
                                                    if (showBack) {
                                                        56.dp + PANE_FADING_EDGE_LENGTH
                                                    } else {
                                                        PANE_FADING_EDGE_LENGTH
                                                    },
                                                bottom = PANE_FADING_EDGE_LENGTH,
                                            ),
                                    ) {
                                        // This page's child pages, as rows above its own
                                        // preferences: tapping one navigates deeper into the
                                        // tree (the list pane switches to that page's
                                        // children).
                                        if (page.subPages.isNotEmpty()) {
                                            items(
                                                page.subPages,
                                                key = { sub -> "subpage:${sub.id}" },
                                            ) { sub ->
                                                PreferencePageRow(
                                                    page = sub,
                                                    selected = sub.id == selectedPageId,
                                                    onClick = { sub.onClick?.invoke() ?: selectPageById(sub.id) },
                                                )
                                            }
                                        }
                                        page.content(this)
                                    }
                                    // The compact bar: declared after the list so it draws
                                    // above the content, and clipped to a fraction of its
                                    // height so it slides down from the top as the page's
                                    // first row scrolls away. In two-pane mode it is never
                                    // shown: the breadcrumb above the panes already names
                                    // the page, so the bar would only duplicate it.
                                    if (!isTwoPane && detailHeaderProgress > 0.01f) {
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
                                                        BackChevron()
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

/**
 * Builds a [WindowAdaptiveInfo] for a host measured at [size] px, so a surface smaller than
 * the window (a modal bottom sheet, a dialog, a split pane, …) can pass it to
 * [PreferencePageScreen.adaptiveInfo] and have the screen adapt to *the host* rather than to
 * the window. The posture is empty (no hinges): the device's fold does not apply to the
 * host's own surface, and including it would make the sheet snap to the crease.
 *
 * A pure function so it can be memoized (e.g. in `remember`); pass [LocalDensity.current].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
public fun windowAdaptiveInfoFor(size: IntSize, density: Density): WindowAdaptiveInfo =
    with(density) {
        WindowAdaptiveInfo(
            // Built from the public WindowSizeClass constructor (the computeFromDpSize*
            // helpers are not exposed by the window-core on the compile classpath). The
            // constructor takes the min width/height in dp and classifies the size the same
            // way for the medium-width breakpoint the two-pane directive keys on.
            windowSizeClass =
                WindowSizeClass(size.width.toDp().value.toInt(), size.height.toDp().value.toInt()),
            windowPosture = Posture(),
        )
    }

/** The panes that can hold focus; the last one to do so decides single-pane landing. */
private enum class ActivePane { List, Detail }

/**
 * A search result row: a page (optionally nested in the tree; [path] names its trail, e.g.
 * "Theme > Colors"), or a preference entry of one of its pages.
 */
private data class SearchEntry(
    val id: Int,
    val page: PreferencePage,
    val path: String,
    val entry: SearchIndexEntry?,
)

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
 * a leading search icon, the entry's title, and a supporting line naming where the page
 * lives in the tree. A page-level match (no [entry]) shows the page's own title and summary
 * instead.
 */
@Composable
private fun SearchEntryRow(
    entry: SearchIndexEntry?,
    page: PreferencePage,
    path: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = entry?.title ?: page.title) },
        supportingContent = {
            val supporting =
                if (entry != null) {
                    // The entry lives in [page]; the page's trail (e.g. "Theme > Colors")
                    // shows where. Top-level pages have a one-segment trail, equal to
                    // their own title — show just it.
                    path
                } else {
                    page.summary
                }
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

/** Length of the gradient that fades out the pane content at its top and bottom edges. */
private val PANE_FADING_EDGE_LENGTH = 32.dp

/**
 * Static fading edges for a vertical scroll area: short surface-colored gradients at the
 * top and bottom of the pane that are always present. The panes pair this with vertical
 * [PaddingValues] of the same length on their [LazyColumn], so the content scrolls under
 * the fades instead of the fades popping in and out with the scroll position.
 *
 * [topOffset] pushes the top fade down below an opaque bar that overlays the top of the
 * list in single-pane mode (the fixed list header, or the detail compact bar); without it
 * the fade would be painted behind the bar and never visible. In two-pane there is no such
 * overlay, so it stays 0.
 */
@Composable
private fun Modifier.paneFadingEdges(topOffset: Dp = 0.dp): Modifier {
    // Captured in composition (theme/density are composable reads), then used from the
    // non-composable draw lambda.
    val surface = MaterialTheme.colorScheme.surface
    val edgePx = with(LocalDensity.current) { PANE_FADING_EDGE_LENGTH.toPx() }
    val topOffsetPx = with(LocalDensity.current) { topOffset.toPx() }
    return this.then(
        Modifier.drawWithContent {
            drawContent()
            // Top edge, starting below any overlaying bar.
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(surface, surface.copy(alpha = 0f)),
                        startY = topOffsetPx,
                        endY = topOffsetPx + edgePx,
                    ),
                topLeft = Offset(0f, topOffsetPx),
                size = Size(size.width, edgePx),
            )
            // Bottom edge.
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(surface.copy(alpha = 0f), surface),
                        startY = size.height - edgePx,
                        endY = size.height,
                    ),
                topLeft = Offset(0f, size.height - edgePx),
                size = Size(size.width, edgePx),
            )
        },
    )
}
