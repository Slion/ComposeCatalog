/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.catalog

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Deprecated(
    "Use the longSupport parameter of createStore / createDefaultStore " +
        "instead; the global flag is read as a fallback for existing callers.",
    ReplaceWith("createDefaultStore(longSupport = true)"),
)
@Volatile
public var isDefaultStoreAndroidLongSupportEnabled: Boolean = false

/**
 * Coalescing window for store writes: changes that arrive while a window is open are
 * conflated by the state flow and land in the same commit, so a live-slider drag costs one
 * write per window instead of one per frame.
 */
private const val WRITE_COALESCE_MS = 100L

/**
 * Process-wide scope for the default flows' writers (the default flow is process-global,
 * so its writer is too); a supervised scope instead of a [kotlinx.coroutines.GlobalScope]
 * launch, so a leaked flow can be cancelled with it. Lazy: the [Dispatchers.Main] access
 * must not run at class load (e.g. in a JVM test that passes its own writer scope).
 */
internal val preferenceFlowWriterScope: CoroutineScope by lazy {
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

/**
 * The process-wide default flow, created once per process.
 *
 * Sharing a single flow across the whole process (rather than one per activity) is what keeps
 * several activities of the same app — e.g. a full-screen settings host and a bottom-sheet
 * settings host — in sync: they read the same in-memory state, so an activity that starts
 * later shows the current values instead of re-reading (possibly stale) disk, and every
 * activity's writes land in the same state.
 */
private object DefaultFlowHolder {
    var flow: MutableStateFlow<Store>? = null
}

@Composable
public fun createDefaultStore(
    longSupport: Boolean = false,
): MutableStateFlow<Store> {
    // Remembered per composition: the default argument of the `Provide*` composables is
    // re-evaluated on every recomposition, so without this every recomposition would create
    // a fresh flow (re-reading disk) and leak a fresh background collector.
    val view = LocalView.current
    if (view.isInEditMode) {
        return MutableStateFlow(MapStore())
    }
    val context = LocalContext.current
    return remember {
        synchronized(DefaultFlowHolder) {
            DefaultFlowHolder.flow
                ?: createStore(
                    sharedStore =
                        android.preference.PreferenceManager.getDefaultSharedPreferences(context),
                    longSupport =
                        longSupport ||
                            @Suppress("DEPRECATION")
                            isDefaultStoreAndroidLongSupportEnabled,
                ).also { DefaultFlowHolder.flow = it }
        }
    }
}

public fun createStore(
    sharedStore: SharedPreferences,
    longSupport: Boolean = false,
): MutableStateFlow<Store> =
    createStore(sharedStore, longSupport, preferenceFlowWriterScope)

/**
 * Binds [sharedStore] to a state flow: reads seed the flow from the store, and every
 * later change is written back diff-based (only added, changed, or removed keys — never a
 * full-store rewrite) and coalesced over [WRITE_COALESCE_MS], so a burst of changes (e.g.
 * a live-slider drag) costs at most one editor commit per window. The writer runs in
 * [writerScope] (process-wide for default flows, a test scope in unit tests).
 */
internal fun createStore(
    sharedStore: SharedPreferences,
    longSupport: Boolean,
    writerScope: CoroutineScope,
): MutableStateFlow<Store> {
    val flow = MutableStateFlow(sharedStore.readStore())
    writerScope.launch {
        // The last state actually committed to the store; each write diffs against it.
        var diskState = flow.value
        flow.drop(1).collect {
            // The coalescing window: while suspended, the state flow conflates the
            // burst, so the commit below picks up the latest value only.
            delay(WRITE_COALESCE_MS)
            val latest = flow.value
            if (sharedStore.writeDiff(diskState, latest, longSupport)) {
                diskState = latest
            }
        }
    }
    return flow
}

private fun SharedPreferences.readStore(): Store =
    @Suppress("UNCHECKED_CAST") MapStore(all as Map<String, Any>)

/** Writes the diff of [new] against [old]; returns whether anything was committed. */
private fun SharedPreferences.writeDiff(
    old: Store,
    new: Store,
    longSupport: Boolean,
): Boolean {
    val oldMap = old.asMap()
    val newMap = new.asMap()
    val editor = edit()
    var hasChanges = false
    for ((key, value) in newMap) {
        if (oldMap[key] != value) {
            putTyped(editor, key, value, longSupport)
            hasChanges = true
        }
    }
    for (key in oldMap.keys) {
        if (key !in newMap) {
            editor.remove(key)
            hasChanges = true
        }
    }
    if (hasChanges) editor.apply()
    return hasChanges
}

private fun putTyped(
    editor: SharedPreferences.Editor,
    key: String,
    value: Any,
    longSupport: Boolean,
) {
    when (value) {
        is Boolean -> editor.putBoolean(key, value)
        is Int -> editor.putInt(key, value)
        is Long -> {
            check(longSupport || @Suppress("DEPRECATION") isDefaultStoreAndroidLongSupportEnabled) {
                "Android-only support for Long isn't enabled by default. You can opt in by " +
                    "passing longSupport = true to createStore."
            }
            editor.putLong(key, value)
        }
        is Float -> editor.putFloat(key, value)
        is String -> editor.putString(key, value)
        is Set<*> ->
            @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
        else -> throw IllegalArgumentException("Unsupported type for value $value")
    }
}

public val LocalStore: ProvidableCompositionLocal<MutableStateFlow<Store>> =
    compositionLocalOf {
        noLocalProvidedFor("LocalStore")
    }

@Composable
public fun ProvideStore(
    flow: MutableStateFlow<Store> = createDefaultStore(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStore provides flow, content = content)
}
