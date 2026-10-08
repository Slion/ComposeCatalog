# 007 — P2: Cleanup (dead code, tests, CI)

**Status:** open

## Dead code

- `desktopSample/` — not in `settings.gradle.kts`; delete.
- `sample/…/SheetSettingsActivity.kt` — replaced by the in-activity sheet; not in the
  manifest; delete.
- `ScrollIndicators.kt` — custom indicators used only by the list dialogs; keep or drop.
- `SampleAppPreview` previews a composable that needs a window; fix or delete.
- `kotlin-js-store/`, `jitpack.yml` — leftovers of the dropped targets; reconcile.
- binary-compatibility-validator — **done**: every task skips (no KLib targets in an
  Android-only module) and the tracked `api/android/preference.api` was a stale old-package
  dump; plugin + dump removed.

## Tests (none exist; `commonTest` is empty)

`Preferences` map ops; the diff-based writer (003); `walkPages`/`findPage`/
`findPagePath`; `searchPreferencePages`; `headerProgress` (002);
`SampleThemeValues` round-trip.

## CI (`android.yml` = `./gradlew build` only)

`checkout@v4`; explicit `check` + `lint`; Gradle + dependency caching; macrobenchmark
smoke on the cheapest emulator (008); document `tools/autotest` as a pre-merge step.

## Docs

Update `README` (003, 004); move resolved `docs/known-issues.md` entries (005, 006);
verify the `subs/AutoTest` submodule path in CI/docs (moved 2026-10-08).

## Done

- No references to deleted files; `clean build` green; unit tests < 2 min in CI.
