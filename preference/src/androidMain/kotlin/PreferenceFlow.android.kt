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

package net.slions.compose.preference

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Volatile
public var isDefaultPreferenceFlowAndroidLongSupportEnabled: Boolean = false

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
    var flow: MutableStateFlow<Preferences>? = null
}

@Composable
public actual fun createDefaultPreferenceFlow(): MutableStateFlow<Preferences> {
    // Remembered per composition: the default argument of the `Provide*` composables is
    // re-evaluated on every recomposition, so without this every recomposition would create
    // a fresh flow (re-reading disk) and leak a fresh background collector.
    val view = LocalView.current
    if (view.isInEditMode) {
        return MutableStateFlow(MapPreferences())
    }
    val context = LocalContext.current
    return remember {
        synchronized(DefaultFlowHolder) {
            @Suppress("DEPRECATION")
            DefaultFlowHolder.flow
                ?: createPreferenceFlow(
                    android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                ).also { DefaultFlowHolder.flow = it }
        }
    }
}

public fun createPreferenceFlow(
    sharedPreferences: SharedPreferences
): MutableStateFlow<Preferences> =
    MutableStateFlow(sharedPreferences.preferences).also {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.Main.immediate) {
            it.drop(1).collect { sharedPreferences.preferences = it }
        }
    }

private var SharedPreferences.preferences: Preferences
    get() = @Suppress("UNCHECKED_CAST") MapPreferences(all as Map<String, Any>)
    set(value) {
        edit {
            clear()
            for ((key, mapValue) in value.asMap()) {
                when (mapValue) {
                    is Boolean -> putBoolean(key, mapValue)
                    is Int -> putInt(key, mapValue)
                    is Long -> {
                        check(isDefaultPreferenceFlowAndroidLongSupportEnabled) {
                            "Android-only support for Long isn't enabled by default. You can opt" +
                                " in by setting isDefaultPreferenceFlowLongSupportEnabled to true"
                        }
                        putLong(key, mapValue)
                    }
                    is Float -> putFloat(key, mapValue)
                    is String -> putString(key, mapValue)
                    is Set<*> ->
                        @Suppress("UNCHECKED_CAST") putStringSet(key, mapValue as Set<String>)
                    else -> throw IllegalArgumentException("Unsupported type for value $mapValue")
                }
            }
        }
    }

private inline fun SharedPreferences.edit(action: SharedPreferences.Editor.() -> Unit) {
    edit().apply {
        action()
        apply()
    }
}
