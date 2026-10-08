# 004 — P1: Unify the API surface

**Status:** open · **Area:** public API of `:preference`, README, sample

No backward-compatibility constraint.

## Problem

Four overlapping styles per type (stateful builder, value builder, `MutableState`
composable, stateless composable); `key` means store-key in one overload and
lazy-list-key in the adjacent one; `staticSummary` and `rememberState` parameters leak
the search/state machinery into every row's API; keys are untyped end to end.

## Target surface

- One stateless composable per type: `SwitchPreference(value, onValueChange, …)`.
- One `LazyListScope` builder per type with the same external-state shape.
- `rememberPreference(key, defaultValue)` (renamed from `rememberPreferenceState`) for
  callers who want a stored value, used with either of the above.
- Drop: the `MutableState` overloads, `staticSummary`, `rememberState`.
- Keep `BasicPreference`/`Preference`/`PreferenceCategory` as the extension points.
- Stretch (defer): DataStore-style typed keys.

## Done

- One composable + one builder per type; README's "4 kinds of APIs" section replaced;
  sample compiles against the new surface.
