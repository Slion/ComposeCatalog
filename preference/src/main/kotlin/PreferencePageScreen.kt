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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a search-selected preference row stays highlighted in the detail pane. */
private const val HIGHLIGHT_DURATION_MS = 2000L

/**
 * How long a pane transition keeps the search field out of the focus tree, so the focus
 * system does not restore focus (and show the keyboard) on it mid-transition.
 */
private const val FIELD_FOCUS_SETTLE_MS = 400L

/** The panes that can hold focus; the last one to do so decides single-pane landing. */
private enum class ActivePane { List, Detail }

/**
 * A search result row: a page (optionally nested; [path] names its trail, e.g. "Theme >
 * Colors"), a preference entry of a page, or a root row ([page] null; [path] is the
 * screen title).
 */
internal data class SearchEntry(
    val id: Int,
    val page: PreferencePage?,
    val path: String,
    val entry: SearchIndexEntry?,
)

/**
 * Tunables for the adaptive-layout workarounds [PreferencePageScreen] applies on
 * foldables; the defaults are the workarounds documented in `docs/known-issues.md`.
 */
public class PreferencePageScreenOptions(
    /**
     * The hinge policy of the pane scaffold directive. [HingePolicy.NeverAvoid] (the
     * default) keeps the two panes exactly half each in every fold state; the material3
     * default (AvoidSeparating) inserts the separating fold as an excluded bound, so in
     * half-folded portrait the scaffold snaps the pane divider onto the crease and the
     * list pane grows past 50%.
     */
    public val hingePolicy: HingePolicy = HingePolicy.NeverAvoid,
    /**
     * Whether the two-pane layout also activates at the "medium" width class. The
     * material3 default only goes two-pane at the "expanded" width class (>= 720dp),
     * which unfolded foldables sit just below (e.g. 719dp); a settings list of short
     * page titles is comfortably usable at medium width, so the default is true.
     */
    public val twoPanesOnMediumWidth: Boolean = true,
    /**
     * Whether each pane draws the static surface fades at its top and bottom edges (with
     * the list content inset by their length so it scrolls under them).
     */
    public val fadingEdges: Boolean = true,
)

/**
 * The adaptive two-pane settings screen: a list pane of [pages] with a search field, and
 * a detail pane showing the selected page's preferences.
 *
 * - Single-pane (e.g. a phone in portrait): selecting a page navigates to the detail
 *   pane; the top bar shows the page title with a back arrow, and system back pops the
 *   detail before dismissing the screen.
 * - Two-pane (e.g. wide windows): both panes are visible at once; the top bar shows the
 *   screen title and there is no navigation.
 *
 * The list pane shows an MD3-style search pill; while a query is entered, the page list
 * is replaced by the matching pages, preference entries, and root rows, and selecting a
 * result clears the query and opens the page (or scrolls to the root row).
 *
 * @param title Title of the screen, shown in the top bar.
 * @param pages The pages to show in the list pane, in order.
 * @param modifier Modifier applied to the root surface.
 * @param onBack Called when the host should dismiss the screen (i.e. system back while
 * the list pane is on screen in a single-pane layout).
 * @param backEnabled Whether the screen's system-back handling is active. A host that
 * covers the screen with its own layer (e.g. a fragment or view shown over it) passes
 * false so that back goes to that layer — and its back stack — first.
 * @param adaptiveInfo Adaptive layout info computed for the host of the screen instead
 * of for the window. The screen's adaptive layout (single- vs two-pane) follows this
 * value, which is what a host smaller than the window — e.g. a modal bottom sheet, a
 * dialog, or a split — passes: it measures its own size and builds a
 * [WindowAdaptiveInfo] for it (with no hinges, since the fold does not apply to the
 * host), so the screen adapts to the host rather than to the window. Null (the default)
 * uses the window.
 * @param singlePaneOnly Forces the single-pane layout regardless of the (host) width, so
 * a host that must never split into a list + detail (e.g. a bottom sheet, a dialog)
 * stays single-pane even when it is wide; the list navigates to the detail and back.
 * @param rootContent Rows hosted at the root of the tree, below the top-level page rows
 * (hidden while searching); any `LazyListScope` builder works, as in
 * [PreferencePage.content]. The rows are searchable; their keys must not collide with
 * page ids, as they share the list pane's lazy list.
 * @param rootContentVersion Increment when [rootContent]'s rows change at runtime so the
 * search index is rebuilt (see [PreferencePage.contentVersion]).
 * @param options Tunables for the fold/adaptive workarounds (hinge policy, two-pane
 * width class, pane fading edges); see [PreferencePageScreenOptions].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
public fun PreferencePageScreen(
    title: String,
    pages: List<PreferencePage>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    backEnabled: Boolean = true,
    adaptiveInfo: WindowAdaptiveInfo? = null,
    singlePaneOnly: Boolean = false,
    rootContent: LazyListScope.() -> Unit = {},
    rootContentVersion: Int = 0,
    options: PreferencePageScreenOptions = PreferencePageScreenOptions(),
) {
    // The window-derived adaptive state (quantized size class + scaffold directive);
    // see rememberScreenAdaptive for the fold-resize rationale.
    val adaptive = rememberScreenAdaptive(adaptiveInfo, singlePaneOnly, options)
    val isTwoPane = adaptive.isTwoPane

    val navigator =
        rememberListDetailPaneScaffoldNavigator<String>(
            scaffoldDirective = adaptive.directive,
            adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
            isDestinationHistoryAware = true,
        )
    val scope = rememberCoroutineScope()

    // The search field is the first focusable in the list pane, so the focus system
    // restores focus to it on launch and when the list pane is restored, showing a brief
    // focus flash and keyboard. Keep it out of the focus tree during those transitions
    // so focus is never gained in the first place; it becomes focusable once the pane
    // has settled.
    var fieldFocusEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(FIELD_FOCUS_SETTLE_MS)
        fieldFocusEnabled = true
    }
    // Keep the field out of the focus tree across a pane transition (or the system would
    // restore focus and show the keyboard), then let it settle back in.
    suspend fun guardedNavigation(block: suspend () -> Unit) {
        fieldFocusEnabled = false
        block()
        delay(FIELD_FOCUS_SETTLE_MS)
        fieldFocusEnabled = true
    }

    // The pane whose content last had focus (e.g. the user was scrolling/interacting
    // with the detail list, or with the page list/search). Decides which pane a collapse
    // to single-pane lands on: the sub page if the detail was in use, the root list
    // otherwise. Null until focus is actually observed (fresh launch).
    var lastActivePane by rememberSaveable { mutableStateOf<ActivePane?>(null) }
    fun onPaneActive(pane: ActivePane) {
        if (lastActivePane != pane) lastActivePane = pane
    }

    // The scaffold's destination is the single source of truth for which page (if any)
    // the detail pane shows, so the top bar title and the list-pane selection highlight
    // follow the real navigation state (including across configuration changes such as
    // rotation).
    val destination = navigator.currentDestination
    val selectedPageId: String? =
        if (destination?.pane == ListDetailPaneScaffoldRole.Detail) {
            destination.contentKey
        } else {
            null
        }
    // The navigation trail within the page tree: the chain of page ids from the top
    // level down to the page currently shown in the detail pane (empty = none
    // selected). The detail shows the deepest page; the list pane shows the rows of its
    // parent (the top-level pages while at the root). Tapping a sub-page row pushes its
    // id, tapping a breadcrumb segment (or back) pops to that ancestor.
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
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                contentKey = pageId,
            )
        }
    }
    fun navigateToPath(id: List<String>) {
        // An empty path means "back to the root list" (breadcrumb at the top level),
        // which is not a selection: forget the flag so size round trips stay at the
        // root.
        userSelectedPage = id.isNotEmpty()
        pagePath = id
        if (id.isNotEmpty()) {
            scope.launch {
                navigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.Detail,
                    contentKey = id.last(),
                )
            }
        }
    }
    // Pops the scaffold to the root list, keeping the search field out of the focus tree
    // for the transition.
    suspend fun popToRootList() =
        guardedNavigation {
            navigator.navigateBack()
            pagePath = emptyList()
            userSelectedPage = false
        }

    // Keep the pane layout in sync with the window size:
    // - Growing to two-pane with no detail destination opens a page right away (the last
    //   open one, or the first page) so the two-pane layout is not empty. The page is
    //   only a visual filler when the user never selected one: shrinking back pops to
    //   the root list, so a rotation round trip on a fresh launch ends where it
    //   started.
    // - Shrinking back to a single partition leaves the destination history pointing at
    //   the detail pane, which keeps the detail pane expanded (and the list hidden)
    //   until the user presses back. Pop back to the list so the layout collapses to
    //   single-pane immediately.
    LaunchedEffect(isTwoPane) {
        val onDetail =
            navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail
        when {
            isTwoPane && !onDetail -> {
                // A page with its own onClick is an action row, not a viewable page: the
                // auto-open filler skips it so the detail does not show its (empty)
                // content.
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
                // Material3's internal snapTo (in rememberThreePaneScaffoldNavigator)
                // does not reliably fire across a config change when the detail is
                // already open, so the scaffold state stays at the stale single-pane
                // value. Force it to the derived two-pane value.
                (navigator.scaffoldState as? MutableThreePaneScaffoldState)
                    ?.snapTo(navigator.scaffoldValue)
            }
            !isTwoPane && onDetail -> {
                if (!userSelectedPage) {
                    // The detail shows a page we auto-opened as a visual filler; a
                    // rotation round trip must end back at the root list.
                    popToRootList()
                } else if (lastActivePane != ActivePane.List) {
                    // The detail was in use (or focus was never observed, e.g. right
                    // after launch in two-pane), so keep it visible in single-pane too.
                    (navigator.scaffoldState as? MutableThreePaneScaffoldState)
                        ?.snapTo(navigator.scaffoldValue)
                } else {
                    // Keep the field out of the focus tree while the list pane is
                    // restored, so the focus system does not put focus (and the
                    // keyboard) back on it.
                    guardedNavigation { navigator.navigateBack() }
                }
            }
        }
    }

    var query by remember { mutableStateOf("") }

    // The preference row a search result navigated to is highlighted briefly, so the
    // user can find it in the opened page (mirrors the launcher settings behavior).
    var highlightedKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(highlightedKey) {
        if (highlightedKey != null) {
            delay(HIGHLIGHT_DURATION_MS)
            highlightedKey = null
        }
    }

    // The lazy list index of the row a search result navigated to; the detail pane
    // scrolls to it when set.
    var scrollToIndex by remember { mutableStateOf<Int?>(null) }
    // The list-pane counterpart for root rows: the search pill is item 0, the page rows
    // follow, then the root rows.
    var listScrollToIndex by remember { mutableStateOf<Int?>(null) }

    fun backAction() {
        val onDetail = destination?.pane == ListDetailPaneScaffoldRole.Detail
        when {
            // Nested in the tree: pop one level up (child -> parent) first.
            onDetail && pagePath.size > 1 ->
                scope.launch { guardedNavigation { navigateToPath(pagePath.dropLast(1)) } }
            !isTwoPane && onDetail -> scope.launch { popToRootList() }
            else -> onBack()
        }
    }

    // System back: the scaffold pops detail -> list first, then the host leaves the
    // screen. A host that covers the screen with its own layer disables this so that
    // back goes to that layer (and its back stack) first.
    BackHandler(enabled = backEnabled, onBack = ::backAction)

    val isSearching = query.isNotEmpty()
    // The index is rebuilt when the tree changes or when a page/root bumps its content
    // version (rows added or changed at runtime).
    val index =
        remember(pages, pages.walkPages().map { it.contentVersion }) {
            buildSearchIndex(pages)
        }
    val rootIndex = remember(rootContentVersion) { buildSearchIndex(rootContent) }
    val matches =
        remember(pages, index, query) { searchPreferencePages(pages, index, query) }
    val searchEntries =
        remember(matches, rootIndex, query) {
            buildSearchEntries(pages, matches, rootIndex, query, title)
        }

    val currentPage = selectedPageId?.let { id -> findPage(pages, id) }
    // The trail of pages from the top level to the current one, for the breadcrumb and
    // the search supporting text (may be stale for one frame after a navigation; it is
    // re-derived from the real destination below).
    val currentPath: List<PreferencePage> =
        remember(pages, selectedPageId) {
            if (selectedPageId != null) {
                findPagePath(pages, selectedPageId) ?: emptyList()
            } else {
                emptyList()
            }
        }
    // True while the list pane shows the top-level pages (the root of the tree), as
    // opposed to the children of the page currently shown in the detail pane.
    val isAtRootLevel = selectedPageId == null || currentPath.size <= 1
    // The parent of the page shown in the detail: its children are the rows of the list
    // pane (the top-level pages while at the root).
    val listRows: List<PreferencePage> =
        if (isAtRootLevel) pages else currentPath[currentPath.size - 2].subPages
    val showBack =
        !isTwoPane &&
            destination?.pane == ListDetailPaneScaffoldRole.Detail &&
            currentPage != null

    // A root row matched by search: back to the root level, then scroll the list pane to
    // the row and highlight it.
    fun showRootEntry(entry: SearchIndexEntry) {
        query = ""
        when {
            !isTwoPane && destination?.pane == ListDetailPaneScaffoldRole.Detail ->
                scope.launch { popToRootList() }

            // The detail must show a top-level page for the list pane to be at the root
            // level.
            isTwoPane && currentPath.size > 1 ->
                navigateToPath(currentPath.take(1).map { it.id })
        }
        highlightedKey = entry.key
        listScrollToIndex = 1 + pages.size + entry.index
    }

    // A search result row was tapped.
    fun onSearchEntrySelected(entry: SearchEntry) {
        query = ""
        val page = entry.page
        when {
            // A root row: no page to open.
            page == null -> entry.entry?.let(::showRootEntry)
            // An action page: just run its action, no deep navigation.
            page.onClick != null -> page.onClick()
            else -> {
                entry.entry?.let {
                    highlightedKey = it.key
                    // The detail column lists the page's sub-page rows above its
                    // content, so shift the content-relative index past them.
                    scrollToIndex = it.index + page.subPages.size
                }
                selectPageById(page.id)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // systemBarsPadding (status + navigation bars) so an edge-to-edge host keeps
        // the content clear of both bars while the surface fills the whole window.
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // The breadcrumb spanning both panes (two-pane only: in single-pane the
            // per-pane bars with their back arrow already convey position).
            if (isTwoPane) {
                BreadcrumbBar(
                    title = title,
                    currentPath = currentPath,
                    pagePath = pagePath,
                    onNavigateToPath = ::navigateToPath,
                )
            }
            ListDetailPaneScaffold(
                directive = adaptive.directive,
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
                        ListPane(
                            title = title,
                            isTwoPane = isTwoPane,
                            listRows = listRows,
                            selectedPageId = selectedPageId,
                            onSelectPage = ::selectPageById,
                            isAtRootLevel = isAtRootLevel,
                            rootContent = rootContent,
                            isSearching = isSearching,
                            searchEntries = searchEntries,
                            onSearchEntryClick = ::onSearchEntrySelected,
                            query = query,
                            onQueryChange = { query = it },
                            fieldFocusEnabled = fieldFocusEnabled,
                            highlightedKey = highlightedKey,
                            fadingEdges = options.fadingEdges,
                            listScrollToIndex = listScrollToIndex,
                            onListScrollConsumed = { listScrollToIndex = null },
                            onActive = { onPaneActive(ActivePane.List) },
                        )
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
                        DetailPane(
                            page = currentPage,
                            selectedPageId = selectedPageId,
                            onSelectPage = ::selectPageById,
                            onBack = ::backAction,
                            showBack = showBack,
                            highlightedKey = highlightedKey,
                            fadingEdges = options.fadingEdges,
                            scrollToIndex = scrollToIndex,
                            onDetailScrollConsumed = { scrollToIndex = null },
                            onActive = { onPaneActive(ActivePane.Detail) },
                        )
                    }
                },
            )
        }
    }
}

/**
 * Builds a [WindowAdaptiveInfo] for a host measured at [size] px, so a surface smaller
 * than the window (a modal bottom sheet, a dialog, a split pane, …) can pass it to
 * [PreferencePageScreen.adaptiveInfo] and have the screen adapt to *the host* rather
 * than to the window. The posture is empty (no hinges): the device's fold does not
 * apply to the host's own surface, and including it would make the sheet snap to the
 * crease.
 *
 * A pure function so it can be memoized (e.g. in `remember`); pass
 * [LocalDensity.current].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
public fun windowAdaptiveInfoFor(size: IntSize, density: Density): WindowAdaptiveInfo =
    with(density) {
        WindowAdaptiveInfo(
            // Built from the public WindowSizeClass constructor (the computeFromDpSize*
            // helpers are not exposed by the window-core on the compile classpath). The
            // constructor takes the min width/height in dp and classifies the size the
            // same way for the medium-width breakpoint the two-pane directive keys on.
            windowSizeClass =
                WindowSizeClass(
                    size.width.toDp().value.toInt(),
                    size.height.toDp().value.toInt(),
                ),
            windowPosture = Posture(),
        )
    }
