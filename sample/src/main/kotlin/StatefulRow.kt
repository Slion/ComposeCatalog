/*
 * Copyright 2026 Stéphane Lenclud
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

package net.slions.compose.preference.sample

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import net.slions.compose.preference.rememberPreferenceState

/**
 * The sample's version of the library's stateful `*Preference` builders: it remembers a
 * [defaultValue] by [key] (via the same [rememberPreferenceState] the library's own stateful
 * builders use) and feeds the composable, value-based [row] with the current value and a change
 * handler, so the sample exercises both API styles (stateful builders and value-based
 * composables).
 */
fun <T> LazyListScope.statefulRow(
    key: String,
    defaultValue: T,
    row: @Composable (value: T, onValueChange: (T) -> Unit) -> Unit,
) {
    item(key = key, contentType = "Stateful") {
        val state = rememberPreferenceState(key, defaultValue)
        val value by state
        row(value) { state.value = it }
    }
}
