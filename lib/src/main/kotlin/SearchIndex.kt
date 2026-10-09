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

package net.slions.compose.catalog

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
     * Records a searchable entry for the preference row being registered. No-op when no index
     * walk is in progress, so builders call it unconditionally.
     *
     * Returns the recorded [SearchIndexEntry], or null when no walk is in progress. Multi-row
     * containers such as [card] use it to fix up the entries' [SearchIndexEntry.index]
     * with the single index of their lazy list item.
     */
    fun record(key: String, title: String, summary: String? = null): SearchIndexEntry? =
        collector?.record(key, title, summary)

    /**
     * Records a page reference for the page row ([item] with a [Page]) being registered
     * (its row is registered with the fake scope immediately after). No-op when no walk is in
     * progress. Unlike [record], it does not add a search entry: the child page is
     * searchable as a page of the tree in its own right.
     */
    fun recordSubPage(page: Page, onClick: (() -> Unit)?) {
        collector?.recordSubPage(page, onClick)
    }

    /**
     * Replaces the placeholder index of [entries] with the index of the lazy list item they
     * belong to. No-op when no walk is in progress.
     */
    fun setIndices(indices: Map<SearchIndexEntry, Int>) {
        collector?.setIndices(indices)
    }

    /**
     * The number of lazy list items registered so far in the current walk, so that a multi-row
     * container can know the index its (single) item will have. 0 when no walk is in progress.
     */
    fun itemCount(): Int = collector?.count ?: 0
}

/**
 * A child-page reference registered by a page row ([item] with a [Page]): the child page,
 * an optional action
 * that replaces navigation when the row is tapped (e.g. launching an activity that
 * hosts the page in its own catalog), and the index of the row in the owning page's
 * lazy list.
 */
public data class SubPageRef(
    public val page: Page,
    public val onClick: (() -> Unit)? = null,
    public val index: Int = 0,
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
 * page references. Page ids must be unique within the whole tree, as they key both the
 * structure and the navigation.
 */
public fun buildPageStructure(root: Page): Map<String, PageStructure> =
    root.walkPages().associateBy({ it.id }) { it.structure }

/**
 * Builds the search index of the page tree rooted at [root] (the searchable entries of
 * every page, including every level of page references), keyed by page id.
 */
public fun buildSearchIndex(root: Page): Map<String, List<SearchIndexEntry>> =
    buildPageStructure(root).mapValues { it.value.entries }

/**
 * Flattens the search [matches] into the result rows the list pane shows, in display
 * order (the root page first, as in the walk). A row gets a unique id (several rows can
 * share an entry key, e.g. the rows of the same card), so the results list can key on
 * it. [title] is the screen title, shown as the path of the root page's rows (the root
 * is the screen itself, so it has no trail segment).
 *
 * A pure function so it can be memoized (e.g. in `remember`) and unit-tested.
 */
internal fun buildSearchEntries(
    root: Page,
    matches: List<PageMatch>,
    query: String,
    title: String,
): List<SearchEntry> {
    var rowId = 0
    return buildList {
        matches.forEach { match ->
            // The page's trail in the tree (e.g. "Nested > Advanced"), shown under each
            // of its result rows; the root's trail is the screen title.
            val path =
                (findPagePath(root, match.page.id)?.drop(1)?.map { it.title } ?: emptyList())
                    .joinToString(" > ")
                    .ifEmpty { title }
            if (match.matches.isEmpty()) {
                // The page itself matched (by title/summary): show the page row.
                add(
                    SearchEntry(
                        id = rowId++,
                        page = match.page,
                        path = path,
                        entry = null,
                    )
                )
            }
            // One row per matching preference entry of the page.
            addAll(
                match.matches.map {
                    SearchEntry(
                        id = rowId++,
                        page = match.page,
                        path = path,
                        entry = it,
                    )
                },
            )
        }
    }
}

/** Collects the [SearchIndexEntry]s of a page's preference tree, in registration order. */
@PublishedApi
internal class SearchIndexRecorder {
    /**
     * The entries, in registration order. Entries of a multi-row card (which is a single lazy
     * list item) are added with a placeholder index and fixed up with [setIndices] once the
     * card's item has been registered.
     */
    val entries: MutableList<SearchIndexEntry> = mutableListOf()

    /** The sub-page references, in registration (content) order. */
    val subPages: MutableList<SubPageRef> = mutableListOf()

    /** The number of lazy list items registered so far. */
    internal var count: Int = 0
        private set

    internal fun registerItem() {
        count++
    }

    internal fun recordSubPage(page: Page, onClick: (() -> Unit)?) {
        subPages.add(SubPageRef(page = page, onClick = onClick, index = count))
    }

    fun record(key: String, title: String, summary: String? = null): SearchIndexEntry {
        val entry = SearchIndexEntry(key = key, title = title, summary = summary, index = count)
        entries.add(entry)
        return entry
    }

    /** Replaces the placeholder index of [entries] with the index of the item they belong to. */
    internal fun setIndices(indices: Map<SearchIndexEntry, Int>) {
        for ((entry, index) in indices) {
            val i = this.entries.indexOfFirst { it === entry }
            if (i != -1) {
                this.entries[i] = entry.copy(index = index)
            }
        }
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
