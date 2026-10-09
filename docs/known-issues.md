# Known issues & limitations

A running log of open issues and known limitations in the Compose Catalog library,
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

## Data source

- **No `Long` by default.** The default `SharedStore`-backed flow omits `Long` to match
  AndroidX DataStore's behavior; opt in with `isDefaultPreferenceFlowAndroidLongSupportEnabled`.
  (Documented in the README; tracked here so it is not "fixed" away accidentally.)

## Platform

- **Android only.** iOS/JVM/JS/Wasm targets were dropped to keep the dependency surface small and
  the build fast. Tracked so the surface stays deliberately narrow; re-adding targets is a
  deliberate decision, not a regression.
