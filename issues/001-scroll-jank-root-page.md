# 001 — P0: Root-page scroll jank (A22)

**Status:** open · **Area:** `PreferencePageScreen.kt` (list pane), sample host
**Symptom:** Flinging the root page on a Samsung A22 feels laggy.

Baseline each fix with [008](008-performance-benchmark-plan.md).

## Fixes

1. **Per-frame size poll.** A self-re-posting `Choreographer.FrameCallback` runs every
   frame for the screen's lifetime to read the root view size. Replace with
   `onSizeChanged`/`addOnLayoutChangeListener` (verify the fold-spread case that
   motivated the poll before deleting).

2. **`paneFadingEdges` allocates 2 brushes and draws 2 full-width gradients on every draw
   pass** (both panes). Build brushes in `drawWithCache`/`remember`; make the fade
   theme-optional.

3. **6dp elevation shadows** on the single-pane list header and the detail compact bar —
   replace with a 1dp `outlineVariant` divider or the fade gradient.

4. **Every list row is a full M3 `Card`** (surface + clip + tonal elevation). Render as a
   flat `Surface` (no elevation); keep the first/last rounded corners. Measure first.

5. **Unstable `pages` identity.** The sample passes a fresh list every recomposition, so
   the search index is rebuilt and the `LaunchedEffect(listRows)` scroll-to-selection
   restarts — it can `animateScrollToItem` mid-fling and fight the user's scroll.
   Sample: `remember { }` the pages. Library: key the effect on selected id + row-set
   content; no-op when the row is already visible.

6. **Size-class re-derivation** (`WindowSizeClass.compute`) on any size wobble —
   re-check after item 1 removes the wobble source.

## Done

- A22 top→bottom fling: p95 frame ≤ 33 ms, dropped frames < 1% (008).
- A pure scroll frame performs no state writes and no content-builder re-runs.
