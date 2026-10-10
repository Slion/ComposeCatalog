# Known issues & limitations

A running log of open issues and known limitations in the Compose Toolkit library,
ordered roughly by impact. Each entry says where the problem shows up and the current
workaround, so it can be cross-checked against the
[device-UI tests](../tools/autotest/README.md). When a fix lands, move the entry to a
short "resolved" note or delete it.

## Screen / adaptive layout

- **Medium-width two-pane boundary.** The adaptive directive only goes two-pane at the
  720dp "Expanded" width class by default. Unfolded foldables sit right on that boundary
  (≈719dp → "Medium") and would stay single-pane in both orientations.
  **Workaround (in place):** `Catalog` uses
  `calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth`.
  *Open:* confirm the breakpoint on more devices; a per-device `swNNN` override may be needed.

- **Fold half-state divider.** The default `HingePolicy.AvoidSeparating` inserts the fold as
  an excluded bound, so in half-folded portrait the scaffold snaps the pane divider onto the
  crease and the list pane grows past 50%.
  **Workaround (in place):** `HingePolicy.NeverAvoid` keeps both `preferredWidth(0.5f)` panes
  exactly half in every fold state.
  *Open:* validate on additional hinge devices (only Honor Magic V2 verified so far).

- **Search result for a multi-row card scrolls to the card, not the row.** A `card`
  is a single lazy item holding many rows, so the recorded search entries share one lazy index;
  selecting such a result scrolls to the card.
  *Open:* per-row targeting inside cards.

- **Stale pane layout on a non-recreating resize.** material3-adaptive's
  `ThreePaneScaffoldState` is a sealed class whose transition value only moves on
  navigation operations or an explicit (suspend) snap — never from a directive change —
  so a host that resizes without recreating its activity (rotation with
  `configChanges="orientation|screenSize"`, or a fold spreading/folding in place) draws
  the pre-resize layout (e.g. the two panes) in the new window size for a frame or two
  before `Catalog`'s sync effect collapses it. The library cannot clamp the value
  during composition (the state is sealed, the snap is suspend).
  **Workaround (in place for the demo):** the demo activities declare no `configChanges`,
  so a rotation recreates the activity and the fresh composition starts at the correct
  partition count (all screen state is `rememberSaveable`, including the navigator's
  destination history).
  *Open:* hosts that must not recreate (foldables in place) still see the brief flash.

## Nested pages

- **Breadcrumb is two-pane only.** The trail bar is shown only when both panes are visible; in
  single-pane the back arrow is the only affordance. Intentional, but worth confirming it matches
  user expectation (a collapsed trail or a `>` chevron on the title could help).

- **List-pane scroll on back (two-pane).** Going back from a nested page must scroll the list
  pane to the newly-selected page item. Implemented with a `LaunchedEffect(selectedPageId, ...)`
  that animates to the page's index.
  *Open:* verify the animation does not fight a user-initiated fling, and that it is a no-op when
  the page is already on screen.

## Bottom sheet / dialog hosting

- **Host-measured adaptive info.** A bottom sheet must build its own `WindowAdaptiveInfo`
  (no hinges) and pass it via `adaptiveInfo`; the screen otherwise follows the window, not the
  sheet.
  *Open:* lift the "measure host → build `WindowAdaptiveInfo`" helper into the library so each
  host does not re-derive it.

- **`singlePaneOnly` is a hard cap.** It forces `maxHorizontalPartitions = 1`, so a wide sheet can
  never show two panes. Intentional for sheets/dialogs; not meant for full-screen hosts.

- **Resolved: sheet as a separate activity.** The sample's bottom sheet used to be a
  `SheetSettingsActivity`, which (a) forked the theme — each activity built its own preference
  flow, so opening/closing the sheet reset the theme — and (b) couldn't be launched over adb
  (`exported="false"` with a dead `MAIN`/`LAUNCHER` filter). It is now hosted in the current
  activity as an in-activity overlay (see `SheetSettings.kt` in the sample), reads the same
  shared preference flow, and the device-UI test reaches it through the "Sheet settings" →
  "Open in a bottom sheet" row.

## Search

- **Result icon top-aligned on rows with a wrapped supporting line (won't fix).** A search
  result whose supporting text wraps to several lines (e.g. a long page summary in the narrow
  list pane) is a three-line item for material3's `ListItem`, and material3's measure policy
  intentionally top-aligns the leading icon on three-line items
  (`isSupportingMultiline` → `place(…, y = topPadding)` instead of centering) — that is the
  Material 3 spec for three-line list items. The icon itself sizes fine (24dp `Icon`), and
  rows with a one-line supporting text stay centered; only the wrapped rows differ, and the
  placement happens on the slot by the layout, so it cannot be adjusted from inside
  `leadingContent`.
  **Decision:** keep the material3 default (won't fix for now). Revisit only if it reads as
  misaligned in practice; the alternative would be clamping the supporting line to one line
  in result rows (loses long summaries).

## Data source

- **No `Long` by default.** The default `SharedStore`-backed flow omits `Long` to match
  AndroidX DataStore's behavior; opt in with `isDefaultPreferenceFlowAndroidLongSupportEnabled`.
  (Documented in the README; tracked here so it is not "fixed" away accidentally.)

## Platform

- **Android only.** iOS/JVM/JS/Wasm targets were dropped to keep the dependency surface small and
  the build fast. Tracked so the surface stays deliberately narrow; re-adding targets is a
  deliberate decision, not a regression.
