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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * The `key` of the preference row that should currently be highlighted (tinted with the theme's
 * `primaryContainer`), or null when no row is highlighted.
 *
 * Hosts (e.g. a search that navigates to a page) provide this for the duration of the highlight;
 * every preference row reads it and highlights itself when its lazy list key matches.
 */
public val LocalHighlightedItemKey: ProvidableCompositionLocal<String?> =
    compositionLocalOf { null }

/**
 * A [Modifier] that highlights the preference row when its lazy list `key` matches
 * [LocalHighlightedItemKey]: the row is tinted with the theme's `primaryContainer` color
 * and flashed with a translucent [androidx.compose.material3.MaterialTheme.colorScheme.primary]
 * overlay a couple of times (a ripple-like cue), so it stands out while the list scrolls
 * to it.
 *
 * Every lazy `*Item` extension applies this to its row, so a host can highlight a single
 * row (e.g. the entry a search navigated to) by providing [LocalHighlightedItemKey] with
 * the row's key for a short duration.
 */
@Composable
public fun highlightedKeyModifier(key: String?): Modifier {
    if (key != LocalHighlightedItemKey.current) return Modifier
    // The highlighted row takes focus: the search field is unfocused, the keyboard
    // closes, and on d-pad and TV the focus ring lands on the row as well.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // The static tint alone is easy to miss while the list is still scrolling, so the
    // row also flashes (fades in and out) twice when the highlight starts.
    val flash = remember { Animatable(0f) }
    val flashColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.hide()
        repeat(2) {
            flash.animateTo(0.35f, tween(180, easing = FastOutSlowInEasing))
            flash.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
        }
    }
    return Modifier
        .focusable()
        .focusRequester(focusRequester)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .drawWithContent {
            drawContent()
            if (flash.value > 0f) drawRect(color = flashColor.copy(alpha = flash.value))
        }
}

public fun LazyListScope.basicItem(
    key: String,
    textContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    iconContainer: @Composable () -> Unit = {},
    actionIconContainer: @Composable () -> Unit = {},
    widgetContainer: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    item(key = key, contentType = "BasicItem") {
        BasicItem(
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
public fun BasicItem(
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
