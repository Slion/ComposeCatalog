# 003 — P1: Persistence rewrites all keys on every change

**Status:** done (2026-10-08) · **Area:** `PreferenceFlow.kt`

## Problem

Every state change runs `edit().clear()` + `put*` for **all** keys + `apply()` on the
main thread (`GlobalScope`, process-global holder). So: O(all keys) work per toggle
(`apply()` commits to memory synchronously; bursts force disk commits on main), a
full-store rewrite per frame during `live` slider drags, and an uncancellable,
untestable collector. The global `...LongSupportEnabled` flag makes it worse to reason
about.

## Fix

1. Diff-based writes: only added/changed/removed keys.
2. Coalesce bursts (conflate ~100 ms) — the in-memory flow already confluates reads.
3. Structured writer scope (no `GlobalScope`); `Long` support as a function parameter
   (global flag deprecated).
4. Keep the per-row `flow.map` reads — they scope recomposition to the changed row. Do
   not read `flow.value[key]` directly (would invalidate every row).

## Done

- Toggling one switch = exactly one `put*` (unit test, fake `SharedPreferences`).
- A 30-frame drag = ≤ 5 writes; no main-thread disk work (008).

### Implementation (2026-10-08)

- `createPreferenceFlow` collects the state flow in a supervised writer scope
  (`preferenceFlowWriterScope`, no more `GlobalScope`): `drop(1)` + a 100 ms
  `delay` coalescing window, then `writeDiff(old, new)` — one `put*` per changed key,
  one `remove` per dropped key, `apply()` only when something changed.
- `Long` support is a `longSupport` parameter; the global
  `isDefaultPreferenceFlowAndroidLongSupportEnabled` flag is deprecated and only
  consulted by the default flow. An unsupported type fails the writer fast (the
  in-memory state is not affected).
- All six acceptance tests in `PreferenceFlowTest` (fake `SharedPreferences`, virtual
  time) pass: one-put-per-toggle, diff-only writes, removal-only writes, ≤ 5 writes
  for a 30-frame drag, fail-fast and opt-in for `Long`.
