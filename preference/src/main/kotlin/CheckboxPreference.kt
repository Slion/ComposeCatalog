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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Adds a checkbox preference row to the lazy list, whose state is remembered by [key].
 *
 * @param key The lazy list key of the row, and the preference state key.
 * @param defaultValue The initial value of the checkbox.
 * @param title The title of the row. Also used as the row's search text.
 * @param modifier Modifier applied to the row.
 * @param rememberState How the row's state is remembered.
 * @param enabled Whether the row is enabled, based on the current value.
 * @param icon The leading icon, based on the current value.
 * @param summary The summary text, based on the current value.
 * @param staticSummary A static summary used in the [buildSearchIndex] index.
 */
public inline fun LazyListScope.checkboxPreference(
    key: String,
    defaultValue: Boolean,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    crossinline rememberState: @Composable () -> MutableState<Boolean> = {
        rememberPreferenceState(key, defaultValue)
    },
    noinline enabled: (Boolean) -> Boolean = { true },
    noinline icon: @Composable ((Boolean) -> Unit)? = null,
    noinline summary: ((Boolean) -> String?)? = null,
    staticSummary: String? = null,
) {
    SearchIndexer.record(key, title, staticSummary)
    item(key = key, contentType = "CheckboxPreference") {
        val state = rememberState()
        val value by state
        CheckboxPreference(
            state = state,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            icon = icon,
            summary = summary,
        )
    }
}

/**
 * Adds a checkbox preference row to the lazy list, whose value is controlled by the caller.
 *
 * @param key The lazy list key of the row.
 * @param value The current value of the checkbox.
 * @param onValueChange Called when the checkbox is toggled.
 * @param title The title of the row. Also used as the row's search text.
 * @param modifier Modifier applied to the row.
 * @param enabled Whether the row is enabled.
 * @param icon The leading icon.
 * @param summary The summary text.
 * @param staticSummary A static summary used in the [buildSearchIndex] index. If null,
 * [summary] is used.
 */
public fun LazyListScope.checkboxPreference(
    key: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary)
    item(key = key, contentType = "CheckboxPreference") {
        CheckboxPreference(
            value = value,
            onValueChange = onValueChange,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            icon = icon,
            summary = summary,
        )
    }
}

/**
 * A checkbox preference row, whose state is a [MutableState].
 *
 * @param state The state of the checkbox.
 * @param title The title of the row.
 * @param modifier Modifier applied to the row.
 * @param enabled Whether the row is enabled.
 * @param icon The leading icon.
 * @param summary The summary text, based on the current value.
 */
@Composable
public fun CheckboxPreference(
    state: MutableState<Boolean>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: (Boolean) -> Boolean = { true },
    icon: @Composable ((Boolean) -> Unit)? = null,
    summary: ((Boolean) -> String?)? = null,
) {
    var value by state
    CheckboxPreference(
        value = value,
        onValueChange = { value = it },
        title = title,
        modifier = modifier,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
    )
}

@Composable
public fun CheckboxPreference(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
) {
    Preference(
        title = title,
        modifier =
            modifier.toggleable(value, enabled, Role.Checkbox, onValueChange = onValueChange),
        enabled = enabled,
        icon = icon,
        summary = summary,
        widgetContainer = {
            val theme = LocalPreferenceTheme.current
            Checkbox(
                checked = value,
                onCheckedChange = null,
                modifier = Modifier.padding(theme.padding.copy(start = theme.horizontalSpacing)),
                enabled = enabled,
            )
        },
    )
}
