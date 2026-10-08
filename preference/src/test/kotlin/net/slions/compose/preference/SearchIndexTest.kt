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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchIndexTest {
    private val settings = Page(
        id = "settings",
        title = "Settings",
        content = {
            itemSwitch("dark", value = false, onValueChange = {}, title = "Dark theme", summary = "Use dark theme")
            itemSwitch("sound", value = true, onValueChange = {}, title = "Sounds")
        },
    )
    private val nested = Page(
        id = "nested",
        title = "Nested",
        subPages = listOf(
            Page(
                id = "nested-child",
                title = "Nested child",
                content = {
                    itemSwitch("wifi", value = false, onValueChange = {}, title = "Wi-Fi", summary = "Connected")
                },
            ),
        ),
        content = {},
    )
    private val pages = listOf(settings, nested)

    @Test
    fun `buildSearchIndex records every row with its key, text and item index`() {
        val index = buildSearchIndex(pages)
        // Sub-pages are indexed too.
        assertEquals(listOf("settings", "nested", "nested-child"), index.keys.toList())
        assertEquals(
            listOf(
                SearchIndexEntry("dark", "Dark theme", "Use dark theme", 0),
                SearchIndexEntry("sound", "Sounds", null, 1),
            ),
            index["settings"],
        )
        assertEquals(
            listOf(SearchIndexEntry("wifi", "Wi-Fi", "Connected", 0)),
            index["nested-child"],
        )
        // A page without searchable rows still gets an (empty) index entry.
        assertTrue(index["nested"]!!.isEmpty())
    }

    @Test
    fun `buildSearchIndex of root content records the root rows`() {
        val rootContent: LazyListScope.() -> Unit = {
            itemSwitch("root-row", value = false, onValueChange = {}, title = "Root row")
        }
        assertEquals(
            listOf(SearchIndexEntry("root-row", "Root row", null, 0)),
            buildSearchIndex(rootContent),
        )
    }

    @Test
    fun `a blank query matches only the top-level pages`() {
        val matches = searchPages(pages, buildSearchIndex(pages), "   ")
        assertEquals(listOf("settings", "nested"), matches.map { it.page.id })
        assertTrue(matches.all { it.matches.isEmpty() })
    }

    @Test
    fun `search is case-insensitive on titles, summaries and page titles`() {
        val index = buildSearchIndex(pages)
        assertEquals(listOf("settings"), searchPages(pages, index, "DARK").map { it.page.id })
        assertEquals(listOf("settings"), searchPages(pages, index, "use dark").map { it.page.id })
        assertEquals(listOf("nested-child"), searchPages(pages, index, "connected").map { it.page.id })
        assertEquals(listOf("settings"), searchPages(pages, index, "sEtTiNgS").map { it.page.id })
    }

    @Test
    fun `search matches nested pages by their rows`() {
        val matches = searchPages(pages, buildSearchIndex(pages), "wi-fi")
        assertEquals(listOf("nested-child"), matches.map { it.page.id })
        assertEquals(listOf("wifi"), matches.single().matches.map { it.key })
    }

    @Test
    fun `search returns no match for an unknown query`() {
        assertTrue(searchPages(pages, buildSearchIndex(pages), "zzz").isEmpty())
    }

    @Test
    fun `record outside a walk is a no-op`() {
        assertNull(SearchIndexer.record("k", "t"))
    }
}
