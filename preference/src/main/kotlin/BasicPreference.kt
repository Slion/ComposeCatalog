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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The `key` of the preference row that should currently be highlighted (tinted with the theme's
 * `primaryContainer`), or null when no row is highlighted.
 *
 * Hosts (e.g. a search that navigates to a page) provide this for the duration of the highlight;
 * every preference row reads it and highlights itself when its lazy list key matches.
 */
public val LocalHighlightedPreferenceKey: ProvidableCompositionLocal<String?> =
    compositionLocalOf { null }

/**
 * A [Modifier] that highlights the preference row when its lazy list `key` matches
 * [LocalHighlightedPreferenceKey]: the row is tinted with the theme's `primaryContainer` color.
 *
 * Every lazy `*Preference` extension applies this to its row, so a host can highlight a single
 * row (e.g. the entry a search navigated to) by providing [LocalHighlightedPreferenceKey] with
 * the row's key for a short duration.
 */
@Composable
public fun highlightedKeyModifier(key: String?): Modifier =
    if (key == LocalHighlightedPreferenceKey.current) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
    } else {
        Modifier
    }

public fun LazyListScope.basicPreference(
    key: String,
    textContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    iconContainer: @Composable () -> Unit = {},
    actionIconContainer: @Composable () -> Unit = {},
    widgetContainer: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    item(key = key, contentType = "BasicPreference") {
        BasicPreference(
            textContainer = textContainer,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            iconContainer = iconContainer,
            actionIconContainer = actionIconContainer,
            widgetContainer = widgetContainer,
            onClick = onClick,
        )
    }
}

/**
 * A basic preference row: an icon, a text block, an action icon, and a widget, laid out in a
 * single row.
 *
 * @param textContainer The title and (optionally) summary block.
 * @param modifier Modifier applied to the row.
 * @param enabled Whether the preference is enabled.
 * @param iconContainer The leading icon.
 * @param actionIconContainer The action icon, shown just before the widget.
 * @param widgetContainer The trailing widget (e.g. a switch or checkbox).
 * @param onClick Click handler; when null, the row is not clickable.
 */
@Composable
public fun BasicPreference(
    textContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconContainer: @Composable () -> Unit = {},
    actionIconContainer: @Composable () -> Unit = {},
    widgetContainer: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier.then(
                if (onClick != null) {
                    Modifier.clickable(enabled, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconContainer()
        Box(modifier = Modifier.weight(1f)) { textContainer() }
        actionIconContainer()
        widgetContainer()
    }
}
