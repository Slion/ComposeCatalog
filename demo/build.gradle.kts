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
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.plugin.compose)
}

android {
    namespace = "net.slions.compose.toolkit.demo"
    buildToolsVersion = libs.versions.android.buildTools.get()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "net.slions.compose.toolkit.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    signingConfigs {
        create("release") {
            storeFile = System.getenv("STORE_FILE")?.let { rootProject.file(it) }
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
        // Non-debuggable, non-minified build for macrobenchmarking (see :benchmark):
        // representative frame timing without R8, signed with the local debug key so it
        // builds without the release keystore env vars.
        create("benchmark") {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":lib"))
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
