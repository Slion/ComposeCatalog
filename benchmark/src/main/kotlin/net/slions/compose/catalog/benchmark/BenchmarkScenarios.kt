/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.catalog.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiSelector
import android.graphics.Point
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The macro benchmarks of the sample app (see issues/008). The baseline runs with no baseline
 * profile and no compilation, matching the pre-fix state.
 *
 * Every frame scenario first resets the app to a deterministic state ([freshRoot]: `pm clear`
 * + fresh launch), so the root list is at the top with no persisted selection — the restored
 * selection would otherwise open a detail page (single-pane) or auto-scroll the list past the
 * search pill (two-pane). The fresh-launch frames are part of the frame metrics; baseline and
 * post-fix runs share the protocol, so deltas stay comparable.
 */
@RunWith(AndroidJUnit4::class)
class BenchmarkScenarios {

    private val app = "net.slions.compose.catalog.demo"

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Cold launch of the sample app; startup time. */
    @Test
    fun coldStart() =
        rule.measureRepeated(
            packageName = app,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 8,
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        }

    /** Fling the root list top→bottom for ~2 s; p95 frame, dropped %, janky frames. */
    @Test
    fun rootPageScroll() =
        rule.measureRepeated(
            packageName = app,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.WARM,
            iterations = 4,
        ) {
            freshRoot()
            // x = 0.25 is the list pane in both layouts (full width in single-pane, left pane
            // in two-pane).
            fling(times = 4, xRatio = 0.25f)
        }

    /** Same fling on the heaviest page (Theme), in the detail pane. */
    @Test
    fun detailPageScroll() =
        rule.measureRepeated(
            packageName = app,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.WARM,
            iterations = 4,
        ) {
            freshRoot()
            device.findObject(UiSelector().text("Theme")).click()
            device.waitForIdle()
            // x = 0.75 hits the detail pane in both layouts: full-screen in single-pane
            // (portrait) and the right pane of the two-pane layout (landscape).
            fling(times = 4, xRatio = 0.75f)
        }

    /**
     * 10 taps on the root switch — the last row of the root list, below the fold —, 300 ms
     * apart; the per-tap frame delta.
     */
    @Test
    fun toggleRow() =
        rule.measureRepeated(
            packageName = app,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.WARM,
            iterations = 2,
        ) {
            freshRoot()
            scrollToRow("Root switch")
            repeat(10) {
                device.findObject(UiSelector().text("Root switch")).click()
                device.waitForIdle()
                Thread.sleep(300)
            }
        }

    /**
     * Commit a 12-char query to the search pill; the frame cost of the query change (search
     * index lookup + list diff + detail update).
     */
    @Test
    fun searchType() =
        rule.measureRepeated(
            packageName = app,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.WARM,
            iterations = 4,
        ) {
            freshRoot()
            // The pill's "Search" placeholder is not exposed as its own accessibility node
            // (the field's merged semantics absorb it), so target the field directly.
            val field = device.findObject(UiSelector().className("android.widget.EditText"))
            field.click()
            device.waitForIdle()
            field.setText("theme12345")
        }

    /**
     * Reset to a deterministic state: clear any persisted selection/scroll so the app starts at
     * the root list, at the top, then wait until the UI settles.
     */
    private fun MacrobenchmarkScope.freshRoot() {
        device.executeShellCommand("pm clear $app")
        startActivityAndWait()
        device.waitForIdle()
    }

    /**
     * Bring [text] on screen by flinging the list pane (x = 0.25) to the bottom; the root
     * switch is the last row of the root list. Three flings cover the whole list on both
     * test devices. Fails if the row is not visible afterwards.
     */
    private fun MacrobenchmarkScope.scrollToRow(text: String, maxFling: Int = 3) {
        repeat(maxFling) { fling(times = 1, xRatio = 0.25f) }
        check(device.findObject(UiSelector().text(text)).exists()) {
            "row '$text' not found after scrolling"
        }
    }

    /**
     * Repeated fast flings, ~500 ms apart (≈2 s in total), from the lower to the upper part of
     * the screen at [xRatio] of its width. A 100 ms swipe is fast enough for the system to turn
     * it into a fling.
     */
    private fun MacrobenchmarkScope.fling(times: Int, xRatio: Float) {
        val x = (device.displayWidth * xRatio).toInt()
        repeat(times) {
            device.swipe(
                arrayOf(
                    Point(x, (device.displayHeight * 0.8f).toInt()),
                    Point(x, (device.displayHeight * 0.2f).toInt()),
                ),
                100,
            )
            Thread.sleep(500)
        }
    }
}
