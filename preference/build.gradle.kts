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
    alias(libs.plugins.android.kotlinMultiplatformLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApi()

    // Android-only: this library targets the Android platform exclusively.
    android {
        namespace = "net.slions.compose.preference"
        buildToolsVersion = libs.versions.android.buildTools.get()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard-rules.pro")
            }
        }
        packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    }

    sourceSets {
        commonMain {
            dependencies {
                // TODO: Migrate away from deprecated dependency aliases once they have a BOM for
                //  compatible versions.
                implementation(compose.components.resources)
                implementation(compose.material3)
                // The extended artifact transitively provides the core icon set
                // (androidx.compose.material.icons.Icons).
                implementation(compose.materialIconsExtended)
                implementation(libs.jetbrains.androidx.lifecycle.runtimeCompose)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                // Exposed with api (not implementation): WindowAdaptiveInfo is part of the
                // library's public API (PreferencePageScreen.adaptiveInfo), so consumers must
                // have it on their compile classpath to pass a value.
                api(libs.androidx.material3.adaptive)
                api(libs.androidx.material3.adaptive.layout)
                api(libs.androidx.material3.adaptive.navigation)
                implementation(libs.timber)
            }
        }
        commonTest { dependencies { implementation(libs.kotlin.test) } }
    }
}
