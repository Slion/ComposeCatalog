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

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.plugin.compose)
}

android {
    namespace = "net.slions.compose.preference.sample"
    buildToolsVersion = libs.versions.android.buildTools.get()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Match the app's benchmark build type so :androidSample's benchmark variant
    // can consume this library (see the :benchmark macrobenchmark module).
    buildTypes {
        create("benchmark")
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":preference"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIconsExtended)
    implementation(libs.androidx.compose.ui.toolingPreview)
    // Google Material Symbols (variable-font based) — provides icons the
    // deprecated material-icons-extended set no longer has, e.g. "colors".
    implementation("dev.vicart:compose-material-symbols:1.1.6")
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    debugImplementation(libs.androidx.compose.ui.testManifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
