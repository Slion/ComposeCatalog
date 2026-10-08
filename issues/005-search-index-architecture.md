# 005 — P1: Search index: global recorder + rebuild on recomposition

**Status:** done (minimum fallback; the full row-descriptor model is deferred) · **Area:** `SearchIndex.kt`, `PreferencePage.kt`, `PreferencePageScreen.kt`

## Problem

- `SearchIndexer` is a process-global `object` with a mutable collector that every builder
  touches — not thread-safe, and couples builders to the index walk.
- The index is rebuilt on the composition thread whenever `pages` identity changes (the
  sample does this every recomposition — 001.5); `contentVersion`/`rootContentVersion`
  (added) only signal *runtime* changes.
- `lowercase()` on every entry per keystroke.
- Multi-row cards are one lazy item, so search can't scroll to a row (`docs/known-issues.md`).

## Fix

Adopt a row-descriptor model: page content builds a list of row descriptors; the list
renders them 1:1 and the search index *is* that list — no separate walk, no global
recorder, sync by construction. Cards become per-row lazy items (fixes the row-scroll
issue). Also drops the need for `staticSummary` (004).

Minimum fallback: an explicit per-walk recorder (no global), precomputed lowercase in
the index, index cached on a structural key.

## Done (minimum fallback, 2026-10-08)

- Per-walk recorder: `SearchIndexer.withCollector` scopes one `SearchIndexRecorder` per
  `buildSearchIndex` walk (previous collector saved/restored); builders no-op outside a
  walk. The index path is a plain function over a recording `LazyListScope` — no
  composition, unit-tested in `SearchIndexTest`.
- Index cached on a structural key in `PreferencePageScreen`:
  `remember(pages, pages.walkPages().map { it.contentVersion })` (+ `rootContentVersion`
  for root rows) — a host rebuilding its page list on unrelated recompositions (001.5)
  no longer rebuilds the index.
- Lowercase precomputed once per entry at index-build time
  (`SearchIndexEntry.titleLowercase`/`summaryLowercase`); search filters never re-lowercase.

Deferred (needs the row-descriptor model): per-row lazy items for multi-row cards
(search can scroll to and key-highlight the card's row, not a sub-row), and dropping
`staticSummary` (004).
