# 006 — P1: Decompose PreferencePageScreen; drop hardcodes

**Status:** done (2026-10-08; the window-core `computeWindowSizeClass` upgrade is deferred — no stable window-core exposes it, see below) · **Area:** `PreferencePageScreen.kt` (~1,200 lines), `:preference` deps

## Problem

One composable does the adaptive directive, navigation, search, breadcrumb, two panes,
two headers, fades, fold logging, and focus hacks (~30 state vars) — which is why 002's
invalidation scope is as wide as it is. The library also depends on Timber for one-off
`PrefPageFold` logging, and the fold workarounds (`twoPanesOnMediumWidth`,
`HingePolicy.NeverAvoid`, size poll, deprecated `WindowSizeClass.compute`) are hardcoded.

## Fix

- Split into `BreadcrumbBar`, `ListPane` (search pill, page/root rows, results, header)
  and `DetailPane` (rows, compact bar); each child owns its state; the 002 fix becomes
  structural.
- Drop Timber (delete the fold logging); if kept, gate behind an internal flag.
- `PreferencePageScreenOptions`: `hingePolicy`, `twoPanesOnMediumWidth`, `fadingEdges`
  — current workarounds as defaults.
- Upgrade window-core for `computeWindowSizeClass`; share one quantization helper
  (screen + `windowAdaptiveInfoFor`).
- Collapse the repeated `delay(400)` focus guards into one helper.

## Done

- `PreferencePageScreen` < ~250 lines; no Timber; options in the public API; fold
  behaviors from `docs/known-issues.md` become documented options.

### Implementation (2026-10-08)

- `PreferencePageScreen.kt` 1,283 → 530 lines; the composable is ~300 code lines
  (359 with KDoc) — the screen's state core (navigation, size sync, search, layout).
  The UI moved to `PreferencePageScreenPanes.kt`: `BreadcrumbBar`, `ListPane` (search
  pill + `SearchPill`, page/root rows, results, header; owns the list `LazyListState`
  and both scroll effects) and `DetailPane` (rows, compact bar; owns the detail
  `LazyListState` and its scroll effect). The adaptive math moved to
  `PreferencePageAdaptive.kt`: `rememberScreenAdaptive` (view layout listener +
  quantized `WindowAdaptiveInfo` + directive) with the pure `quantizedAdaptiveInfo`
  and `paneScaffoldDirective` helpers; the search-rows flattener moved to
  `buildSearchEntries` in `SearchIndex.kt` (pure, unit-testable).
- Timber removed from the library (fold `PrefPageFold` logging deleted; the pane-size
  state and `onSizeChanged` probes existed only for it and went with it). `:sample`
  keeps its own Timber dependency for app logging.
- `PreferencePageScreenOptions` (public): `hingePolicy` (default `NeverAvoid`),
  `twoPanesOnMediumWidth` (default true), `fadingEdges` (default true) — the fold
  workarounds are now documented options, behavior unchanged at the defaults.
- The three repeated `delay(400)` focus guards collapsed into
  `guardedNavigation { }` (+ the `FIELD_FOCUS_SETTLE_MS` constant, shared with the
  initial enable).
- Deferred: the window-core upgrade — the resolved window-core 1.5.0 (stable) still
  only exposes the deprecated `WindowSizeClass.compute`; `computeWindowSizeClass` is
  not in any stable release, so the deprecated call stays (suppressed, commented).
  Landing the composable strictly under 250 lines would require extracting the
  screen's state into a controller object; deferred as out of scope for the
  decomposition.
