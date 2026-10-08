# 002 — P0: Detail-page scroll recomposes every frame

**Status:** open · **Area:** `PreferencePageScreen.kt` (detail pane)

## Problem

`detailHeaderProgressRaw` is written on every scroll frame (snapshotFlow on
`firstVisibleItemScrollOffset`), and its read lives in the `detailPane` lambda — so the
whole pane re-executes every frame (two-pane mode). `paneFadingEdges` then returns a
fresh `drawWithContent` modifier each time, so the `LazyColumn` is never skipped and its
content builder (all row registrations) re-runs every frame. Any other state read in the
pane lambda has the same effect.

## Fix

1. Move the compact bar and its progress reads into their own child composable, so the
   per-frame float write only re-executes that small scope.
2. Make `paneFadingEdges` a `ModifierNodeElement` (or `remember` the modifier) so the
   `LazyColumn`'s parameters compare equal and it is skipped. Apply to the list pane too
   (001.2).

## Done

- Scrolling the detail pane re-executes only the compact bar; zero content-builder
  re-runs per frame (Compose metrics, 008).
- Two-pane scroll is as smooth as list-pane scroll on the A22 after 001.
