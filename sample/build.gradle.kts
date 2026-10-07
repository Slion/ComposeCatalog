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
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.compose)
}

kotlin {
    // Android-only: the sample app targets the Android platform exclusively.
    android {
        namespace = "net.slions.compose.preference.sample"
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
                implementation(project(":preference"))
                // TODO: Migrate away from deprecated dependency aliases once they have a BOM for
                //  compatible versions.
                implementation(compose.components.resources)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.preview)
                // Google Material Symbols (variable-font based) — provides icons the
                // deprecated material-icons-extended set no longer has, e.g. "colors".
                implementation("dev.vicart:compose-material-symbols:1.1.6")
            }
        }
        commonTest { dependencies { implementation(libs.kotlin.test) } }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.androidx.compose.ui.testManifest)
    androidRuntimeClasspath(compose.uiTooling)
}
