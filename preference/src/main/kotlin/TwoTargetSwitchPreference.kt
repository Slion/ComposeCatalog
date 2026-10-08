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
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

public inline fun LazyListScope.twoTargetSwitchPreference(
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
    noinline switchEnabled: (Boolean) -> Boolean = enabled,
    noinline onClick: ((Boolean) -> Unit)? = null,
) {
    SearchIndexer.record(key, title, staticSummary)
    item(key = key, contentType = "TwoTargetSwitchPreference") {
        val state = rememberState()
        TwoTargetSwitchPreference(
            state = state,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            icon = icon,
            summary = summary,
            switchEnabled = switchEnabled,
            onClick = onClick,
        )
    }
}

public fun LazyListScope.twoTargetSwitchPreference(
    key: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    switchEnabled: Boolean = enabled,
    onClick: (() -> Unit)? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary)
    item(key = key, contentType = "TwoTargetSwitchPreference") {
        TwoTargetSwitchPreference(
            value = value,
            onValueChange = onValueChange,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            icon = icon,
            summary = summary,
            switchEnabled = switchEnabled,
            onClick = onClick,
        )
    }
}

@Composable
public fun TwoTargetSwitchPreference(
    state: MutableState<Boolean>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: (Boolean) -> Boolean = { true },
    icon: @Composable ((Boolean) -> Unit)? = null,
    summary: ((Boolean) -> String?)? = null,
    switchEnabled: (Boolean) -> Boolean = { true },
    onClick: ((Boolean) -> Unit)? = null,
) {
    var value by state
    TwoTargetSwitchPreference(
        value = value,
        onValueChange = { value = it },
        title = title,
        modifier = modifier,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
        switchEnabled = switchEnabled(value),
        onClick = onClick?.let { { it(value) } },
    )
}

@Composable
public fun TwoTargetSwitchPreference(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    switchEnabled: Boolean = enabled,
    onClick: (() -> Unit)? = null,
) {
    TwoTargetPreference(
        title = title,
        secondTarget = {
            val theme = LocalPreferenceTheme.current
            Switch(
                checked = value,
                onCheckedChange = onValueChange,
                modifier =
                    Modifier.padding(
                        theme.padding
                            .copy(start = theme.horizontalSpacing)
                            .offset(vertical = (-8).dp)
                    ),
                enabled = switchEnabled,
            )
        },
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
        onClick = onClick,
    )
}
