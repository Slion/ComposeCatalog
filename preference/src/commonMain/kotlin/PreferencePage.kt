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

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * A settings page: an id, a title, and its preferences.
 *
 * The page content is a `LazyListScope` builder so callers can mix any of the library's
 * `*Preference` / `preferenceCategory` / `preferenceCard` items.
 *
 * @property id Unique id of the page; it is used as the lazy list key and as the
 * navigation destination.
 * @property title Title of the page, shown in the list pane and in the detail top bar.
 * @property summary Optional summary shown below the title in the list pane.
 * @property icon Optional leading icon, shown to the left of the title in the list pane.
 * @property content The preferences of the page. This runs in the detail pane's lazy list
 * scope, so it cannot read the composition directly: capture any theme values (e.g.
 * `MaterialTheme.colorScheme`) in an enclosing `@Composable` scope before building the page.
 */
public data class PreferencePage(
    public val id: String,
    public val title: String,
    public val summary: String? = null,
    public val icon: @Composable (() -> Unit)? = null,
    public val content: LazyListScope.() -> Unit,
)

/**
 * A search result: a [page] whose title/summary or [matches] (entries of the preference
 * tree, see [buildSearchIndex]) contain the query.
 */
public data class PageMatch(
    public val page: PreferencePage,
    public val matches: List<SearchIndexEntry>,
)

/**
 * Case-insensitively searches the whole preference tree of [pages] for pages whose title,
 * summary, or any entry of [index] contains [query] (a blank query matches everything).
 * The returned [PageMatch.matches] list only contains the entries that matched the query.
 *
 * @param index The search index built with [buildSearchIndex].
 */
public fun searchPreferencePages(
    pages: List<PreferencePage>,
    index: Map<String, List<SearchIndexEntry>>,
    query: String,
): List<PageMatch> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) {
        return pages.map { PageMatch(it, emptyList()) }
    }
    return pages.mapNotNull { page ->
        val pageMatches =
            page.title.lowercase().contains(q) || page.summary?.lowercase()?.contains(q) == true
        val matchingEntries = (index[page.id] ?: emptyList()).filter {
            it.title.lowercase().contains(q) || it.summary?.lowercase()?.contains(q) == true
        }
        if (pageMatches || matchingEntries.isNotEmpty()) {
            PageMatch(page = page, matches = matchingEntries)
        } else {
            null
        }
    }
}
