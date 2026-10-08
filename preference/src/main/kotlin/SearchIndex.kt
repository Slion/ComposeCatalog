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
 * Builds the search index of [pages] by walking each page's content — including every level
 * of [Page.subPages] — against a recording [LazyListScope]. The builders'
 * registration code runs, but no item content is ever composed, so the index is always in
 * sync with the page's rows — no separate search entries to maintain. Page ids must be
 * unique within the whole tree, as they key both the index and the navigation.
 */
public fun buildSearchIndex(pages: List<Page>): Map<String, List<SearchIndexEntry>> =
    pages.walkPages().associateBy({ it.id }) { page ->
        SearchIndexer.withCollector { recorder ->
            page.content(SearchIndexScope(recorder))
            recorder.entries.toList()
        }
    }

/**
 * Builds the search entries of a `LazyListScope` content block (e.g. the root rows of a
 * settings screen) the same way as [buildSearchIndex] does for a page's content.
 */
public fun buildSearchIndex(rootContent: LazyListScope.() -> Unit): List<SearchIndexEntry> =
    SearchIndexer.withCollector { recorder ->
        rootContent(SearchIndexScope(recorder))
        recorder.entries.toList()
    }

/**
 * Flattens the search [matches] (and the [rootIndex] rows matching [query]) into the
 * result rows the list pane shows, in display order. A row gets a unique id (several
 * rows can share an entry key, e.g. the rows of the same card), so the results list can
 * key on it. [title] is the screen title, shown as the path of the root rows.
 *
 * A pure function so it can be memoized (e.g. in `remember`) and unit-tested.
 */
internal fun buildSearchEntries(
    pages: List<Page>,
    matches: List<PageMatch>,
    rootIndex: List<SearchIndexEntry>,
    query: String,
    title: String,
): List<SearchEntry> {
    var rowId = 0
    val q = query.trim().lowercase()
    return buildList {
        matches.flatMapTo(this) { match ->
            // The page's trail in the tree (e.g. "Theme > Colors"), shown under each of
            // its result rows.
            val path =
                (findPagePath(pages, match.page.id)?.map { it.title } ?: emptyList())
                    .joinToString(" > ")
            buildList {
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
                // One row per matching preference entry of the page's tree.
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
        // Root rows live in the list pane (no page of their own).
        if (q.isNotEmpty()) {
            addAll(
                // The lowercase is precomputed at index-build time.
                rootIndex
                    .filter {
                        it.titleLowercase.contains(q) || it.summaryLowercase?.contains(q) == true
                    }
                    .map {
                        SearchEntry(
                            id = rowId++,
                            page = null,
                            path = title,
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

    /** The number of lazy list items registered so far. */
    internal var count: Int = 0
        private set

    internal fun registerItem() {
        count++
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
