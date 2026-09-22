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

import androidx.compose.runtime.Composable
import net.slions.compose.preference.PreferencePage

const val SampleTitle = "Compose Preference"

/**
 * The sample pages: one page per preference type, each exercising the type's various
 * configurations. Card groups and categories are used throughout the pages, so they have no
 * page of their own. Hosted by [net.slions.compose.preference.PreferencePageScreen].
 */
@Composable
fun samplePages(): List<PreferencePage> =
    listOf(
        preferenceRowPage(),
        checkboxPreferencePage(),
        switchPreferencePage(),
        sliderPreferencePage(),
        listPreferencePage(),
        multiSelectListPreferencePage(),
        textFieldPreferencePage(),
        radioButtonPreferencePage(),
        footerPreferencePage(),
        twoTargetPreferencePage(),
        twoTargetIconButtonPreferencePage(),
        twoTargetSwitchPreferencePage(),
    )
