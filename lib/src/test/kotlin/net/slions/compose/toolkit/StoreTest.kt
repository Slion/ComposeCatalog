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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapStoreTest {
    @Test
    fun `get returns stored value and null for missing keys`() {
        val prefs = MapStore(mapOf("a" to true, "b" to 42, "c" to "text"))
        assertEquals(true, prefs.get<Boolean>("a"))
        assertEquals(42, prefs.get<Int>("b"))
        assertEquals("text", prefs.get<String>("c"))
        assertNull(prefs.get<Boolean>("missing"))
    }

    @Test
    fun `asMap exposes the backing map`() {
        val map = mapOf("a" to 1L)
        assertEquals(map, MapStore(map).asMap())
    }

    @Test
    fun `toMutableStore copies the map`() {
        val prefs = MapStore(mapOf("a" to 1))
        val mutable = prefs.toMutableStore()
        mutable["a"] = 2
        mutable["b"] = 3
        assertEquals(1, prefs.get<Int>("a"))
        assertNull(prefs.get<String>("b"))
    }
}

class MutableMapStoreTest {
    @Test
    fun `set and get round-trip all value types`() {
        val prefs = MutableMapStore()
        prefs["bool"] = true
        prefs["int"] = 7
        prefs["long"] = 8L
        prefs["float"] = 1.5f
        prefs["string"] = "s"
        prefs["set"] = setOf("x")
        assertEquals(true, prefs.get<Boolean>("bool"))
        assertEquals(7, prefs.get<Int>("int"))
        assertEquals(8L, prefs.get<Long>("long"))
        assertEquals(1.5f, prefs.get<Float>("float"))
        assertEquals("s", prefs.get<String>("string"))
        assertEquals(setOf("x"), prefs.get<Set<String>>("set"))
    }

    @Test
    fun `setting null removes the key`() {
        val prefs = MutableMapStore()
        prefs["a"] = 1
        prefs.set<Int>("a", null)
        assertNull(prefs.get<Int>("a"))
        assertTrue("a" !in prefs.asMap())
    }

    @Test
    fun `remove and minusAssign remove the key`() {
        val prefs = MutableMapStore()
        prefs["a"] = 1
        prefs["b"] = 2
        prefs.remove("a")
        prefs -= "b"
        assertTrue(prefs.asMap().isEmpty())
    }

    @Test
    fun `clear removes everything`() {
        val prefs = MutableMapStore()
        prefs["a"] = 1
        prefs.clear()
        assertTrue(prefs.asMap().isEmpty())
    }

    @Test
    fun `toMutableStore copies the map`() {
        val prefs = MutableMapStore()
        prefs["a"] = 1
        val copy = prefs.toMutableStore()
        copy["a"] = 2
        assertEquals(1, prefs.get<Int>("a"))
    }
}
