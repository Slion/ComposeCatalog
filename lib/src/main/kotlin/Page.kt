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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * A settings page: an id, a title, and its items.
 *
 * A page is just a collection of items: any of the library's `*Item` / `section` /
 * `card` builders, in any order, plus [item] rows with a [Page] that reference child
 * pages (the way a page opens its children). The tree can nest to any depth; page ids
 * must be unique within the whole tree.
 *
 * @property id Unique id of the page; it is used as the lazy list key and as the
 * navigation destination.
 * @property title Title of the page, shown in the list pane and in the detail top bar.
 * @property summary Optional summary shown below the title in the list pane.
 * @property icon Optional leading icon, shown to the left of the title in the list pane.
 * @property contentVersion Increment when the items [content] produces change at runtime
 * (items added or removed without a new [Page]) so the screen's search index is
 * rebuilt. It is declared before [content] so trailing-lambda call sites
 * (`Page(id, title) { ... }`) still bind their lambda to [content].
 * @property content The items of the page. It runs in a pane's lazy list scope, so it
 * cannot read the composition directly: capture any theme values (e.g.
 * `MaterialTheme.colorScheme`) in an enclosing `@Composable` scope before building the
 * page.
 */
public data class Page(
    public val id: String,
    public val title: String,
    public val summary: String? = null,
    public val icon: @Composable (() -> Unit)? = null,
    public val contentVersion: Int = 0,
    public val content: LazyListScope.() -> Unit,
) {
    /**
     * The structure of this page (searchable entries + child page references), derived
     * from a single walk of [content]; cached per instance.
     */
    internal val structure: PageStructure by lazy { walkPageContent(this) }

    /** The child pages referenced by [content] via [item] (with a [Page]), in content order. */
    internal val childPages: List<Page>
        get() = structure.subPages.map { it.page }
}

/**
 * Depth-first traversal of the page tree rooted at this page, in declaration order,
 * including every level of page references.
 */
public fun Page.walkPages(): List<Page> {
    val all = mutableListOf<Page>()
    fun visit(page: Page) {
        all += page
        page.childPages.forEach(::visit)
    }
    visit(this)
    return all
}

/** Finds the page with [id] anywhere in the tree rooted at [pages], or null. */
public fun findPage(root: Page, id: String): Page? =
    root.walkPages().firstOrNull { it.id == id }

/**
 * The chain of pages from the top level down to (and including) the page with [id], or null
 * when no page in the tree has [id].
 */
public fun findPagePath(root: Page, id: String): List<Page>? {
    val path = mutableListOf<Page>()
    fun visit(page: Page): Boolean {
        path += page
        if (page.id == id) return true
        for (child in page.childPages) {
            if (visit(child)) return true
        }
        path.removeAt(path.lastIndex)
        return false
    }
    return if (visit(root)) path.toList() else null
}

/**
 * A search result: a [page] whose title/summary or [matches] (entries of the preference
 * tree, see [buildSearchIndex]) contain the query.
 */
public data class PageMatch(
    public val page: Page,
    public val matches: List<SearchIndexEntry>,
)

/**
 * Case-insensitively searches the whole page tree rooted at [root] — including every level
 * of page references, in depth-first order — for pages whose title, summary, or
 * any entry of [index] contains [query] (a blank query matches only the root page, whose
 * items are the list itself).
 * The returned [PageMatch.matches] list only contains the entries that matched the query.
 *
 * @param index The search index built with [buildSearchIndex].
 */
public fun searchPages(
    root: Page,
    index: Map<String, List<SearchIndexEntry>>,
    query: String,
): List<PageMatch> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) {
        return listOf(PageMatch(root, emptyList()))
    }
    return root.walkPages().mapNotNull { page ->
        val pageMatches =
            page.title.lowercase().contains(q) || page.summary?.lowercase()?.contains(q) == true
        // Entries use the lowercase precomputed at index-build time.
        val matchingEntries = (index[page.id] ?: emptyList()).filter {
            it.titleLowercase.contains(q) || it.summaryLowercase?.contains(q) == true
        }
        if (pageMatches || matchingEntries.isNotEmpty()) {
            PageMatch(page = page, matches = matchingEntries)
        } else {
            null
        }
    }
}
