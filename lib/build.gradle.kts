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
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

android {
    namespace = "net.slions.compose.toolkit"
    buildToolsVersion = libs.versions.android.buildTools.get()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Match the app's benchmark build type so the :benchmark variant resolves this
    // library (see the :benchmark macrobenchmark module).
    buildTypes {
        create("benchmark")
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kotlin {
    compilerOptions { explicitApi() }
}

dependencies {
    implementation(libs.androidx.compose.material3)
    // The extended artifact transitively provides the core icon set
    // (androidx.compose.material.icons.Icons).
    implementation(libs.androidx.compose.materialIconsExtended)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.activity.compose)
    // Exposed with api (not implementation): WindowAdaptiveInfo is part of the
    // library's public API (PreferencePageScreen.adaptiveInfo), so consumers must
    // have it on their compile classpath to pass a value.
    api(libs.androidx.material3.adaptive)
    api(libs.androidx.material3.adaptive.layout)
    api(libs.androidx.material3.adaptive.navigation)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
