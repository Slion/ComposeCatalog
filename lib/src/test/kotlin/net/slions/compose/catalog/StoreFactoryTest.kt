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

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 003: persistence must write only the changed keys, at most once per coalescing window
 * (100 ms), with no full-store rewrite.
 */
class StoreFactoryTest {
    @Test
    fun togglingOneKeyWritesExactlyOnePut() = runTest {
        val store = FakeSharedPreferences()
        val flow = createStore(store, false, backgroundScope)
        runCurrent() // start the writer before the first change (it drops the seed value)

        setPref(flow, "enabled", true)
        advanceTimeBy(150)

        assertEquals(listOf("putBoolean:enabled"), store.puts)
        assertEquals(1, store.applies)
    }

    @Test
    fun changingOneOfTwoKeysWritesOnlyTheChangedOne() = runTest {
        val store = FakeSharedPreferences(mapOf("a" to true, "b" to 1))
        val flow = createStore(store, false, backgroundScope)
        runCurrent()

        setPref(flow, "a", false)
        advanceTimeBy(150)

        assertEquals(listOf("putBoolean:a"), store.puts)
        assertEquals(1, store.applies)
        assertEquals(mapOf("a" to false, "b" to 1), store.map)
    }

    @Test
    fun removingAKeyWritesOnlyTheRemoval() = runTest {
        val store = FakeSharedPreferences(mapOf("a" to true, "b" to 1))
        val flow = createStore(store, false, backgroundScope)
        runCurrent()

        setPref(flow, "a", null)
        advanceTimeBy(150)

        assertTrue(store.puts.isEmpty())
        assertEquals(listOf("remove:a"), store.removals)
        assertEquals(mapOf("b" to 1), store.map)
    }

    @Test
    fun aThirtyFrameDragCostsAtMostFiveWrites() = runTest {
        val store = FakeSharedPreferences()
        val flow = createStore(store, false, backgroundScope)
        runCurrent()

        repeat(30) { i ->
            setPref(flow, "slider", i)
            advanceTimeBy(16) // one 60 Hz frame
        }
        advanceTimeBy(200) // let the last window commit

        assertTrue("applies=${store.applies}", store.applies <= 5)
        assertEquals(29, store.map["slider"])
    }

    @Test
    fun longValuesWithoutLongSupportFailTheWriter() = runTest {
        val store = FakeSharedPreferences()
        // A supervisor scope with a handler: the writer fails fast on the unsupported
        // type (fail-fast, as before the 003 rewrite) and must not take down the test
        // scope with it (a supervisor job never completes on a child failure, so the
        // failure is observed through the handler, and the scope is cancelled by hand).
        var error: Throwable? = null
        val handler = CoroutineExceptionHandler { _, e -> error = e }
        // SupervisorJob last so its Job element wins (a context element later in the
        // chain shadows the earlier one; the test context also carries a Job).
        val writer =
            CoroutineScope(backgroundScope.coroutineContext + SupervisorJob() + handler)
        val flow = createStore(store, longSupport = false, writerScope = writer)
        runCurrent()

        setPref(flow, "n", 42L)
        advanceTimeBy(150)
        (writer.coroutineContext[Job] as? Job)?.cancel()

        assertTrue("expected ISE from check() but was: $error", error is IllegalStateException)
        assertTrue("puts should be empty but was: ${store.puts}", store.puts.isEmpty())
    }

    @Test
    fun longValuesWriteWithLongSupport() = runTest {
        val store = FakeSharedPreferences()
        val flow = createStore(store, longSupport = true, writerScope = backgroundScope)
        runCurrent()

        setPref(flow, "n", 42L)
        advanceTimeBy(150)

        assertEquals(listOf("putLong:n"), store.puts)
        assertEquals(42L, store.map["n"])
    }
}

private fun setPref(flow: MutableStateFlow<Store>, key: String, value: Any?) {
    val prefs = flow.value.toMutableStore()
    prefs[key] = value
    flow.value = prefs
}

private class FakeSharedPreferences(initial: Map<String, Any> = emptyMap()) :
    SharedPreferences {
    val map: MutableMap<String, Any> = initial.toMutableMap()
    val puts = mutableListOf<String>()
    val removals = mutableListOf<String>()
    var applies = 0

    private val editor =
        object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()

            override fun putBoolean(key: String, value: Boolean): Editor {
                puts += "putBoolean:$key"
                pending[key] = value
                return this
            }

            override fun putString(key: String, value: String?): Editor {
                puts += "putString:$key"
                pending[key] = value
                return this
            }

            override fun putStringSet(key: String, values: MutableSet<String>?): Editor {
                puts += "putStringSet:$key"
                pending[key] = values?.toSet()
                return this
            }

            override fun putInt(key: String, value: Int): Editor {
                puts += "putInt:$key"
                pending[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): Editor {
                puts += "putLong:$key"
                pending[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): Editor {
                puts += "putFloat:$key"
                pending[key] = value
                return this
            }

            override fun remove(key: String): Editor {
                removals += "remove:$key"
                removed += key
                return this
            }

            override fun clear(): Editor {
                pending.clear()
                removed += map.keys
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                applies++
                removed.forEach { map.remove(it) }
                pending.forEach { (key, value) ->
                    if (value == null) map.remove(key) else map[key] = value
                }
                pending.clear()
                removed.clear()
            }
        }

    override fun getAll(): Map<String, *> = map

    override fun getString(key: String, defValue: String?): String? =
        map[key] as? String ?: defValue

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String> =
        (map[key] as? Set<String>)?.toMutableSet() ?: defValues ?: mutableSetOf()

    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        map[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = editor

    override fun registerOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener?
    ) = Unit
}
