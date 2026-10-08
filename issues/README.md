# Improvement plans

Improvement plans for the Compose Preference framework, produced from a full architecture
review on 2026-10-08. Backward compatibility is **not** a constraint: APIs may be changed or
removed freely; each plan says which public surface it touches.

Priority: **P0** = user-visible performance (the Samsung A22 sample-app lag), **P1** =
architecture / API quality, **P2** = hygiene.

| # | Plan | Priority | Area |
|---|------|----------|------|
| [001](001-scroll-jank-root-page.md) | Root-page scroll jank (A22) | P0 | `PreferencePageScreen` rendering |
| [002](002-scroll-jank-detail-page-recomposition.md) | Detail-page per-frame recomposition | P0 | `PreferencePageScreen` state design |
| [003](003-persistence-full-rewrite.md) | Persistence: full rewrite of every key on every change | P1 | `PreferenceFlow.android` |
| [004](004-api-surface-unification.md) | API surface unification (4 styles per type, key overloading) | P1 | public API |
| [005](005-search-index-architecture.md) | Search index: thread-global recorder, rebuild-on-recomposition | P1 | `SearchIndex`, `PreferencePageScreen` |
| [006](006-preferencepagescreen-refactor.md) | Decompose the 1200-line `PreferencePageScreen`; library logging | P1 | `PreferencePageScreen` |
| [007](007-maintainability-cleanup.md) | Dead code, missing tests, CI hardening | P2 | repo |
| [008](008-performance-benchmark-plan.md) | Performance verification: macrobenchmark + Perfetto on A22 | P0 (enabler) | tooling |

Suggested order: **008 first** (baseline on the A22), then **001 + 002** (the reported lag),
then **003 → 005 → 004 → 006**, with **007** interleaved.
