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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PageTest {
    private val leaf = Page(id = "leaf", title = "Leaf", content = {})
    private val mid = Page(id = "mid", title = "Mid") { item(page = leaf) }
    private val other = Page(id = "other", title = "Other", content = {})
    private val root =
        Page(id = "root", title = "Root") {
            item(page = mid)
            item(page = other)
        }

    @Test
    fun `walkPages is depth-first in declaration order`() {
        assertEquals(listOf("root", "mid", "leaf", "other"), root.walkPages().map { it.id })
    }

    @Test
    fun `walkPages of a leafless tree is the root itself`() {
        assertEquals(listOf("leaf"), leaf.walkPages().map { it.id })
    }

    @Test
    fun `findPage finds nested pages and returns null for missing ids`() {
        assertSame(leaf, findPage(root, "leaf"))
        assertSame(other, findPage(root, "other"))
        assertNull(findPage(root, "nope"))
    }

    @Test
    fun `findPage returns the first page when ids are duplicated`() {
        val dup = Page(id = "dup", title = "First", content = {})
        val dup2 = Page(id = "dup", title = "Second", content = {})
        val tree =
            Page(id = "root", title = "R") {
                item(page = dup)
                item(page = dup2)
            }
        assertSame(dup, findPage(tree, "dup"))
    }

    @Test
    fun `findPagePath returns the chain from the root down`() {
        assertEquals(listOf("root", "mid", "leaf"), findPagePath(root, "leaf")!!.map { it.id })
        assertEquals(listOf("root"), findPagePath(root, "root")!!.map { it.id })
        assertNull(findPagePath(root, "nope"))
    }

    @Test
    fun `findPagePath does not leak siblings into the path`() {
        // 'other' is a sibling of 'mid' and is visited after the failed 'mid' branch.
        val path = findPagePath(root, "other")!!
        assertEquals(listOf("root", "other"), path.map { it.id })
    }

    @Test
    fun `structure subPages are the page rows in content order`() {
        assertEquals(listOf("mid", "other"), root.structure.subPages.map { it.page.id })
        assertEquals(listOf("leaf"), mid.structure.subPages.map { it.page.id })
        // The row's index is its position in the page's own lazy list.
        assertEquals(0, root.structure.subPages[0].index)
        assertEquals(1, root.structure.subPages[1].index)
        // A page row is not a searchable entry of the parent.
        assertTrue(root.structure.entries.isEmpty())
    }

    @Test
    fun `data class equality covers the content lambda reference`() {
        val content: LazyListScope.() -> Unit = {}
        assertEquals(
            Page(id = "a", title = "A", content = content),
            Page(id = "a", title = "A", content = content),
        )
        // A different lambda instance is a different page (function equality is by identity).
        assertFailsWith<AssertionError> {
            assertEquals(
                Page(id = "a", title = "A", content = content),
                Page(id = "a", title = "A", content = {}),
                "different lambda instances must not be equal",
            )
        }
    }
}
