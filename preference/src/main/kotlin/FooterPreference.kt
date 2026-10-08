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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/**
 * Adds a footer row to the lazy list.
 *
 * @param key The lazy list key of the row.
 * @param title The search text of the row; it is not displayed.
 * @param summary The summary text, shown below the icon.
 * @param modifier Modifier applied to the row.
 * @param icon The leading icon.
 */
public fun LazyListScope.footerPreference(
    key: String,
    title: String,
    summary: String?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: @Composable () -> Unit = FooterPreferenceDefaults.Icon,
) {
    SearchIndexer.record(key, title, summary)
    item(key = key, contentType = "FooterPreference") {
        FooterPreference(summary = summary, modifier = modifier.then(highlightedKeyModifier(key)), icon = icon)
    }
}

@Composable
public fun FooterPreference(
    summary: String?,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = FooterPreferenceDefaults.Icon,
) {
    BasicPreference(
        textContainer = {
            val theme = LocalPreferenceTheme.current
            Column {
                Box(modifier = Modifier.padding(bottom = theme.verticalSpacing)) {
                    CompositionLocalProvider(LocalContentColor provides theme.iconColor, content = icon)
                }
                if (summary != null) {
                    CompositionLocalProvider(LocalContentColor provides theme.summaryColor) {
                        ProvideTextStyle(value = theme.summaryTextStyle) {
                            Text(text = summary)
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}

private object FooterPreferenceDefaults {
    val Icon: @Composable () -> Unit = { Icon(imageVector = Icons.Default.Info, contentDescription = null) }
}
