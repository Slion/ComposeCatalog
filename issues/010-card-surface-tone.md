# 010 — Filled cards read brighter than the flat page rows

**Status:** open
**Priority:** P3 (polish)
**Observed:** 2026-10-09, demo app (main activity root card group + Theme page cards)

## What is seen

The `card` / `cardGroup` surfaces in the demo look a step **brighter** than the
plain page rows around them. Notable in two places:

- The root **card group** ("Root page") in the main activity — the cards stand out
  against the flat list behind them.
- The **Theme page** cards (Colors / Texts / Shapes).

This is consistent and *intentional*, not a bug.

## Why

The list-pane **page rows are not cards** — they are plain `Item` rows drawn
directly on `colorScheme.surface` (the window background).

The demo's cards use `CardStyle.Filled`, whose default container is
`CardDefaults.cardColors()` → **`colorScheme.surfaceVariant`**, a lighter tonal
step than `surface` in both the light and dark themes. That is the standard M3
"card sits one tonal step above the surface" hierarchy, so any filled card will
read brighter than the flat rows around it. The root cards and the Theme page
cards therefore match *each other*; they only stand out against the flat rows.

## Options (decide when we revisit)

1. **Same-color fill** — pass
   `cardColor = MaterialTheme.colorScheme.surface` to the relevant `card` /
   `cardGroup` calls. Keeps the shape/division, removes the tonal lift.
   - Scope to decide: root group only, or everywhere (incl. Theme page).
2. **Outlined style** — use `CardStyle.Outlined` (`surface` background + thin
   stroke). Clearly "card" without the brightness.
3. **Leave as-is** — accept the M3 tonal hierarchy as the intended look.

## Notes / open questions

- If we change the demo's cards, consider whether the *library default* for a
  filled card should stay `surfaceVariant` (it is correct M3) — this is a
  demo-styling decision, not a library API change.
- Re-check in both light and dark themes and both orientations before settling.
