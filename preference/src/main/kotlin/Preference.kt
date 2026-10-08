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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a preference row to the lazy list.
 *
 * @param key The lazy list key of the row, and the preference state key.
 * @param title The title of the row. Also used as the row's search text.
 * @param modifier Modifier applied to the row.
 * @param enabled Whether the row is enabled.
 * @param icon The leading icon.
 * @param actionIcon The action icon, shown just before the widget.
 * @param summary The summary text, shown below the title.
 * @param staticSummary A static summary used in the [buildSearchIndex] index. If null,
 * [summary] is used.
 * @param widgetContainer The trailing widget (e.g. a switch or checkbox).
 * @param onClick Click handler; when null, the row is not clickable.
 */
public fun LazyListScope.preference(
    key: String,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary)
    item(key = key, contentType = "Preference") {
        Preference(
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            icon = icon,
            actionIcon = actionIcon,
            summary = summary,
            widgetContainer = widgetContainer,
            onClick = onClick,
        )
    }
}

@Composable
public fun Preference(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    BasicPreference(
        textContainer = {
            val theme = LocalPreferenceTheme.current
            Column(
                modifier =
                    Modifier.padding(
                        theme.padding.copy(
                            start = if (icon != null) 0.dp else Dp.Unspecified,
                            end = if (widgetContainer != null || actionIcon != null) 0.dp else Dp.Unspecified,
                        )
                    )
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides
                        theme.titleColor.let {
                            if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                        }
                ) {
                    ProvideTextStyle(value = theme.titleTextStyle) {
                        Text(text = title)
                    }
                }
                if (summary != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.summaryColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            }
                    ) {
                        ProvideTextStyle(value = theme.summaryTextStyle) {
                            Text(text = summary)
                        }
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        iconContainer = {
            if (icon != null) {
                val theme = LocalPreferenceTheme.current
                Box(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(end = 0.dp)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                        content = icon,
                    )
                }
            }
        },
        actionIconContainer = {
            if (actionIcon != null) {
                val theme = LocalPreferenceTheme.current
                Box(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(start = 0.dp)),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                        content = actionIcon,
                    )
                }
            }
        },
        widgetContainer = { widgetContainer?.invoke() },
        onClick = onClick,
    )
}
