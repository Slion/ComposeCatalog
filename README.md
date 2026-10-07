# Compose Preference

[![Android CI](https://github.com/Slion/ComposePreference/actions/workflows/android.yml/badge.svg)](https://github.com/Slion/ComposePreference/actions/workflows/android.yml)

[Preference](https://developer.android.com/develop/ui/views/components/settings) implementation for [Jetpack Compose](https://developer.android.com/jetpack/compose) [Material 3](https://developer.android.com/jetpack/compose/designsystems/material3).

This is not an officially supported Google product.

> **Android only.** This library targets the Android platform exclusively. It was forked from a
> multiplatform (iOS/JVM/JS/Wasm) project, but those targets have been dropped to keep the
> dependency surface small and the build fast.

## Preview

<p><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="32%" /></p>

## Integration

This project is consumed as a [git submodule](https://git-scm.com/docs/git-submodule) rather than a published Maven artifact. Add the submodule, then wire it into your build as a [composite build](https://docs.gradle.org/current/userguide/composite_builds.html).

1. Add the submodule:

   ```sh
   git submodule add https://github.com/Slion/ComposePreference.git third_party/composepreference
   git submodule update --init --recursive
   ```

2. Include the submodule as a composite build and substitute the `:preference` module for the library coordinate, in your root `settings.gradle.kts`:

   ```kotlin
   includeBuild("third_party/composepreference") {
       dependencySubstitution {
           substitute(module("net.slions.compose.preference:preference")).using(project(":preference"))
       }
   }
   ```

3. Depend on the library in the modules that use it:

   ```kotlin
   implementation("net.slions.compose.preference:preference")
   ```

The composite build ensures the `:preference` module is always used in place of any published artifact.

## Design

There is no official and complete Material 3 UX specification for preference yet, so the UX design of this library mainly comes from the following sources:

- [Material Design 3](https://m3.material.io/)
- [Settings design guidelines](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Android settings design guidelines](https://source.android.com/docs/core/settings/settings-guidelines)
- [AndroidX Preference](https://developer.android.com/jetpack/androidx/releases/preference)
- [AOSP Settings](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/)

## Usage

This library is designed with both extensibility and ease-of-use in mind.

Basic usage of this library involves invoking the `ProvidePreferenceLocals` composable, and then calling the `*Preference` helper functions in a `LazyColumn` composable:

```kotlin
AppTheme {
    ProvidePreferenceLocals {
        // Other composables wrapping the LazyColumn ...
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            switchPreference(
                key = "switch_preference",
                defaultValue = false,
                title = "Switch preference",
                icon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
                summary = { if (it) "On" else "Off" }
            )
        }
    }
}
```

### Preferences

Built-in types of preferences include:

- [`Preference`](preference/src/commonMain/kotlin/Preference.kt)
- [`PreferenceCategory`](preference/src/commonMain/kotlin/PreferenceCategory.kt)
- [`CheckboxPreference`](preference/src/commonMain/kotlin/CheckboxPreference.kt)
- [`FooterPreference`](preference/src/commonMain/kotlin/FooterPreference.kt)
- [`ListPreference`](preference/src/commonMain/kotlin/ListPreference.kt) (supports both alert dialog and dropdown menu)
- [`MultiSelectListPreference`](preference/src/commonMain/kotlin/MultiSelectListPreference.kt)
- [`RadioButtonPreference`](preference/src/commonMain/kotlin/RadioButtonPreference.kt)
- [`SliderPreference`](preference/src/commonMain/kotlin/SliderPreference.kt)
- [`SwitchPreference`](preference/src/commonMain/kotlin/SwitchPreference.kt)
- [`TextFieldPreference`](preference/src/commonMain/kotlin/TextFieldPreference.kt)
- [`TwoTargetIconButtonPreference`](preference/src/commonMain/kotlin/TwoTargetIconButtonPreference.kt)
- [`TwoTargetSwitchPreference`](preference/src/commonMain/kotlin/TwoTargetSwitchPreference.kt)

Each type of built-in preference includes 4 kinds of APIs:

1. A `LazyListScope.*Preference` extension function, which is the easiest way to use preferences in this library, and helps developers to avoid boilerplates like having to specify the key twice for the `LazyColumn` and the `Preference`.
2. A `LazyListScope.*Preference` extension function, which is an overload of the first extension function but accepts `value` and `onValueChange` instead.
3. A `*Preference` composable that takes a `MutableState`, which allows developers to bring in any kind of state they currently have.
4. A `*Preference` composable that takes `value` and `onValueChange`, which allows developers to use the preference without a state and even in non-preference scenarios.

### Theming

The visual appearance of the preferences can be customized by providing a custom [`PreferenceTheme`](preference/src/commonMain/kotlin/PreferenceTheme.kt) with `preferenceTheme` to `ProvidePreferenceLocals` or `ProvidePreferenceTheme`.

Customizable values in the theme include most dimensions, colors and text styles used by the built-in preferences.

### Data source

The data source of the preferences can be customized by providing a custom `MutableStateFlow<Preferences>` to `ProvidePreferenceLocals` or `ProvidePreferenceFlow`.

The [`Preferences`](preference/src/commonMain/kotlin/Preferences.kt) interface defined in this library is similar to the AndroidX DataStore [`Preferences`](https://developer.android.com/reference/kotlin/androidx/datastore/preferences/core/Preferences) class, but:

- It can be implemented by other mechanisms like [`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences), thanks to being a public interface instead of an abstract class with only an internal constructor.
- It doesn't have to be produced and updated via a [`DataStore`](https://developer.android.com/reference/kotlin/androidx/datastore/core/DataStore).
- It doesn't mandate a fixed set of types that an implementation has to support, so that implementations have the flexibility to support much more or less types. The implementations within this library supports most of the types supported by `SharedPreferences` **except for `Long`** by default. You can opt in to `Long` support by setting `isDefaultPreferenceFlowAndroidLongSupportEnabled` to `true`.

The default data source provided by this library (`createDefaultPreferenceFlow()`) is implemented with [`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences), because:

- `SharedPreferences` is available as part of the Android framework, and doesn't require external dependencies like AndroidX DataStore which [bundles its own copy of `protobuf-lite`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:datastore/datastore-preferences-core/build.gradle;l=108;drc=9fd0cda7bb963d41fd25645b0761776caa830ed7).
- `SharedPreferences` can actually be [10x faster](https://stackoverflow.com/q/71601343) than AndroidX DataStore, likely due to its existing optimizations and simple threading and persistence model (XML is simple enough to be faster than Protobuf).
- `SharedPreferences` has a synchronous API, but it is actually async except for the first (un-cached) read, and allows in-memory value change without waiting for the disk write to complete, which is good for the preference use case.
- Existing users of `SharedPreferences` can use this library directly with the default data source.

**There should only be at most one invocation of `createDefaultPreferenceFlow()`**, similar to creating `DataStore` in AndroidX DataStore. It is also only for usage within a single process due to being backed by `SharedPreferences`.

If AndroidX DataStore is considered more appropriate for your use case, e.g. you need multi-process support, you can also create an AndroidX DataStore backed implementation that provides a `MutableStateFlow<Preferences>` on your own.

## Credits

Forked from [zhanghai/ComposePreference](https://github.com/zhanghai/ComposePreference).