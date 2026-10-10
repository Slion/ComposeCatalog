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

package net.slions.compose.toolkit

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * Holds the [SearchIndexRecorder] of the [buildSearchIndex] walk currently in progress, so that
 * every `*Item` builder can record itself as it registers its lazy list item.
 *
 * The walk is synchronous on the composition thread, so a plain field (saved and restored
 * around each page) is sufficient; no thread confinement is required.
 */
@PublishedApi
internal object SearchIndexer {
    private var collector: SearchIndexRecorder? = null

    fun <T> withCollector(block: (SearchIndexRecorder) -> T): T {
        val previous = collector
        val recorder = SearchIndexRecorder()
        try {
            collector = recorder
            return block(recorder)
        } finally {
            collector = previous
        }
    }

    /**
     * Records a searchable entry for the preference row being registered, with the index of
     * the lazy list item it belongs to. No-op when no index walk is in progress, so builders
     * call it unconditionally.
     */
    fun record(key: String, title: String, summary: String? = null): SearchIndexEntry? =
        collector?.record(key, title, summary)

    /**
     * Records a page reference for the page row ([item] with a [Page]) being registered
     * (its row is registered with the fake scope immediately after). No-op when no walk is in
     * progress. Unlike [record], it does not add a search entry: the child page is
     * searchable as a page of the tree in its own right.
     *
     * @param key The key of the lazy list item that hosts the row, so a search result can
     *   scroll to and highlight it.
     */
    fun recordSubPage(page: Page, onClick: (() -> Unit)?, key: String? = null): SubPageRef? {
        return collector?.recordSubPage(page, onClick, key)
    }

    /**
     * The number of lazy list items registered so far in the current walk, so a builder can
     * know the index its item will have. 0 when no walk is in progress.
     */
    fun itemCount(): Int = collector?.count ?: 0
}

/**
 * A child-page reference registered by a page row ([item] with a [Page]): the child page,
 * an optional action
 * that replaces navigation when the row is tapped (e.g. launching an activity that
 * hosts the page in its own Catalog), the index of the row in the owning page's lazy
 * list, and the key of the lazy list item that hosts the row (so a search result can
 * highlight it; null when the row cannot be highlighted).
 */
public data class SubPageRef(
    public val page: Page,
    public val onClick: (() -> Unit)? = null,
    public val index: Int = 0,
    public val key: String? = null,
)

/**
 * The structure of one page, derived from a single walk of its content: the searchable
 * [entries] and the child-page references ([subPages]), both in content order.
 */
public data class PageStructure(
    public val entries: List<SearchIndexEntry>,
    public val subPages: List<SubPageRef>,
)

/** A searchable entry collected from a page's preference tree. */
public data class SearchIndexEntry(
    /** The lazy list `key` of the entry, or null for entries that cannot be highlighted. */
    public val key: String?,
    /** The text of the entry. */
    public val title: String,
    /** Optional summary text of the entry. */
    public val summary: String?,
    /**
     * The index of the entry's lazy list item within its page; used to scroll to the entry when
     * a search result is selected.
     */
    public val index: Int,
    /**
     * [title] lowercased once at index-build time, so that a search never re-lowercases the
     * text of every entry on every keystroke.
     */
    public val titleLowercase: String = title.lowercase(),
    /** [summary] lowercased once at index-build time; null when [summary] is null. */
    public val summaryLowercase: String? = summary?.lowercase(),
)

/**
 * Walks a single page's content against a recording [LazyListScope]: the builders'
 * registration code runs, but no item content is ever composed, so the structure is
 * always in sync with the page's items — no separate search entries to maintain.
 */
internal fun walkPageContent(page: Page): PageStructure =
    SearchIndexer.withCollector { recorder ->
        page.content(SearchIndexScope(recorder))
        PageStructure(
            entries = recorder.entries.toList(),
            subPages = recorder.subPages.toList(),
        )
    }

/**
 * The structure of every page in the tree rooted at [root] — including every level of
 * page references. A page hosted from several places (the same id) appears once, under
 * its shallowest instance, as its id keys both the structure and the navigation.
 */
public fun buildPageStructure(root: Page): Map<String, PageStructure> =
    root.walkDistinctPages().associateBy({ it.id }) { it.structure }

/**
 * Builds the search index of the page tree rooted at [root] (the searchable entries of
 * every page, including every level of page references), keyed by page id.
 */
public fun buildSearchIndex(root: Page): Map<String, List<SearchIndexEntry>> =
    buildPageStructure(root).mapValues { it.value.entries }

/**
 * Flattens the search [matches] into the result rows the list pane shows, ordered by
 * relevance: a page whose title is exactly the query first, then pages whose title
 * contains it, then entries whose title contains it, and finally matches that only hit
 * a summary. Within a rank the shallower page (closer to the root) comes first, and the
 * tree order (the walk order) is kept as the last resort. A row gets a unique id
 * (several rows can share an entry key, e.g. the rows of the same card), so the results
 * list can key on it. [title] is the screen title, shown as the path of the root
 * page's rows (the root is the screen itself, so it has no trail segment).
 *
 * A pure function so it can be memoized (e.g. in `remember`) and unit-tested.
 */
internal fun buildSearchEntries(
    root: Page,
    matches: List<PageMatch>,
    query: String,
    title: String,
): List<SearchEntry> {
    val q = query.trim().lowercase()
    var rowId = 0
    // (relevance rank, page depth in the tree, row) — see the sort at the end.
    val ranked = mutableListOf<Triple<Int, Int, SearchEntry>>()
    matches.forEach { match ->
        // The page's preferred trail (the shallowest one when the page is hosted from
        // several places), shown under each of its result rows and stored on the row:
        // selecting it enters the page through this trail.
        val pagePath = findPagePath(root, match.page.id)
        // The trail as titles (e.g. "Nested > Advanced"); the root's trail is the
        // screen title.
        val path =
            (pagePath?.drop(1)?.map { it.title } ?: emptyList())
                .joinToString(" > ")
                .ifEmpty { title }
        // The trail as ids, below the root (empty = the root level). The page's depth
        // is its size: within a rank, the shallower page is closer to what the user is
        // looking at, so it comes first.
        val trail = pagePath?.drop(1)?.map { it.id } ?: emptyList()
        val depth = trail.size
        if (match.pageMatched) {
            // The page itself matched (by title/summary): show the page row, even when
            // some of the page's entries matched as well.
            ranked.add(
                Triple(
                    pageMatchRank(match.page, q),
                    depth,
                    SearchEntry(
                        id = rowId++,
                        page = match.page,
                        path = path,
                        trail = trail,
                        entry = null,
                    ),
                )
            )
        }
        // One row per matching preference entry of the page.
        match.matches.forEach { entry ->
            ranked.add(
                Triple(
                    entryMatchRank(entry, q),
                    depth,
                    SearchEntry(
                        id = rowId++,
                        page = match.page,
                        path = path,
                        trail = trail,
                        entry = entry,
                    ),
                )
            )
        }
    }
    // Stable sort by rank then depth: relevance first, the shallower page first within
    // a rank, and the tree order kept as the last resort.
    return ranked.sortedWith(compareBy({ it.first }, { it.second })).map { it.third }
}

/**
 * The relevance rank of a page that matched [q]: an exact title match first (0), then a
 * title that merely contains the query (1); a page that only matched by its summary is
 * least relevant (3).
 */
private fun pageMatchRank(page: Page, q: String): Int =
    when {
        q.isEmpty() -> 0
        page.title.lowercase() == q -> 0
        page.title.lowercase().contains(q) -> 1
        else -> 3 // matched by its summary only
    }

/**
 * The relevance rank of a preference entry that matched [q]: a title match (2) is more
 * relevant than a summary-only match (3).
 */
private fun entryMatchRank(entry: SearchIndexEntry, q: String): Int =
    if (entry.titleLowercase.contains(q)) 2 else 3

/** Collects the [SearchIndexEntry]s of a page's preference tree, in registration order. */
@PublishedApi
internal class SearchIndexRecorder {
    /** The entries, in registration order. */
    val entries: MutableList<SearchIndexEntry> = mutableListOf()

    /** The sub-page references, in registration (content) order. */
    val subPages: MutableList<SubPageRef> = mutableListOf()

    /** The number of lazy list items registered so far. */
    internal var count: Int = 0
        private set

    internal fun registerItem() {
        count++
    }

    internal fun recordSubPage(page: Page, onClick: (() -> Unit)?, key: String? = null): SubPageRef {
        val ref = SubPageRef(page = page, onClick = onClick, index = count, key = key)
        subPages.add(ref)
        return ref
    }

    fun record(key: String, title: String, summary: String? = null): SearchIndexEntry {
        val entry = SearchIndexEntry(key = key, title = title, summary = summary, index = count)
        entries.add(entry)
        return entry
    }
}

/**
 * A [LazyListScope] that only records item registrations. It lets [buildSearchIndex] run a
 * page's content without laying anything out: the builders' registration code runs, but no
 * item content is ever composed.
 */
internal class SearchIndexScope(private val recorder: SearchIndexRecorder) : LazyListScope {
    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyItemScope.() -> Unit,
    ) {
        recorder.registerItem()
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        repeat(count) { recorder.registerItem() }
    }
}
