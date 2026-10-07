/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.preference.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import net.slions.compose.preference.PreferencePage
import net.slions.compose.preference.PreferencePageScreen
import net.slions.compose.preference.ProvidePreferenceLocals
import net.slions.compose.preference.ProvidePreferenceTheme
import net.slions.compose.preference.preference
import net.slions.compose.preference.windowAdaptiveInfoFor

/**
 * A root-level page row (shown at the top of the settings list) that opens the settings tree
 * inside a [SheetSettingsActivity] bottom sheet. It exists to prove the same tree can be
 * hosted in a small, adaptive surface (a bottom sheet) as well as full-screen: tapping this
 * row launches the sheet activity, where the tree is adapted to the sheet's size.
 */
@Composable
fun sheetSettingsPage(): PreferencePage {
    val context = LocalContext.current
    return PreferencePage(
        id = "sheet_settings",
        title = "Sheet settings",
        summary = "Opens this settings tree inside a bottom sheet.",
    ) {
        preference(
            key = "sheet_settings_row",
            title = "Open in a bottom sheet",
            summary = "The same tree, hosted in a draggable bottom sheet.",
            onClick = {
                context.startActivity(Intent(context, SheetSettingsActivity::class.java))
            },
        )
    }
}

class SheetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProvidePreferenceLocals {
                val (themeValues, writeThemeValues) = rememberSampleThemeValues()
                SampleTheme(darkTheme = effectiveDarkTheme(themeValues.themeMode, isSystemInDarkTheme())) {
                    ProvidePreferenceTheme {
                        SheetSettingsHost(pages = samplePages(themeValues, writeThemeValues))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSettingsHost(pages: List<PreferencePage>) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        DraggableBottomSheet { size ->
            // The settings tree, adapted to the sheet's measured size.
            SheetSettingsScreen(pages, size)
        }
    }
}

/**
 * A simple, self-contained bottom sheet: a rounded panel anchored to the bottom of its
 * container that the user drags (by its handle or anywhere) to change its expand fraction
 * (0 = collapsed, 1 = full height of the container). It reports the content's measured
 * [IntSize] to [content] so the hosted content can adapt to the sheet's current size.
 */
@Composable
private fun DraggableBottomSheet(content: @Composable (IntSize) -> Unit) {
    val density = LocalDensity.current
    var expand by remember { mutableFloatStateOf(0.75f) }
    var containerHeightPx by remember { mutableStateOf(0) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { containerHeightPx = it.height },
    ) {
        // The sheet's height is its expand fraction of the container, in dp.
        val sheetHeight =
            with(density) { (containerHeightPx * expand).toInt().coerceAtLeast(0).toDp() }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .pointerInput(containerHeightPx) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            // Snap to the nearest of peeked / expanded.
                            expand = if (expand > 0.5f) 1f else 0.4f
                        },
                    ) { _, dragAmount ->
                        if (containerHeightPx > 0) {
                            expand = (expand - dragAmount / containerHeightPx).coerceIn(0f, 1f)
                        }
                    }
                },
        ) {
            // The drag handle: a small pill at the top of the sheet.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size(32.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
            )
            // The hosted content fills the rest of the sheet and reports its measured size.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp)
                    .onSizeChanged { contentSize = it },
            ) {
                if (contentSize != IntSize.Zero) {
                    content(contentSize)
                }
            }
        }
    }
}

/**
 * The settings tree adapted to a host measured at [hostSize]: a [WindowAdaptiveInfo] is built
 * for that size (no hinges — the fold does not apply to the sheet's surface) and passed to
 * [PreferencePageScreen.adaptiveInfo], so the screen's single- vs two-pane layout follows the
 * sheet rather than the window.
 */
@Composable
private fun SheetSettingsScreen(pages: List<PreferencePage>, hostSize: IntSize) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // The sheet is a narrow, modal host: it must never split into a list + detail, so force
    // the single-pane layout (the list navigates to the detail and back). Back at the root
    // closes the sheet.
    PreferencePageScreen(
        title = SampleTitle,
        pages = pages,
        adaptiveInfo = windowAdaptiveInfoFor(hostSize, density),
        singlePaneOnly = true,
        onBack = { (context as? android.app.Activity)?.finish() },
    )
}
