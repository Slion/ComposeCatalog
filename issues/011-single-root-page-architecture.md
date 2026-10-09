# 011 — Architecture: one root Page; pages are just collections of items

**Status:** completed (2026-10-09) — verified 10/10 on A22 (portrait) and G8X (landscape) via `tools/autotest` (`--orientation current`)
**Priority:** P1 (architecture / API quality)
**Proposed:** 2026-10-09

## Goal

Make the tree uniform:

- A `Catalog` hosts exactly **one root `Page`** — there is no "root level" that is
  anything other than a page.
- A `Page` is just **a collection of items** (any `*Item`/`section`/`card` builders),
  **some of which are page items that lead to opening a new page**.
- Consequence: one content mechanism, one search index, no root-only hacks. Less
  hard coding, more generic building blocks.

Backward compatibility is **not** a constraint (per `issues/README.md`): the public
surface may change freely.

## Today's special cases (what we are removing)

| # | Special case | Where |
|---|--------------|-------|
| 1 | `Catalog(pages: List<Page>)` — the top level is a bare list, not a page | `Catalog.kt` signature |
| 2 | `rootContent: LazyListScope.() -> Unit` + `rootContentVersion` — a second content mechanism that only exists at the root | `Catalog.kt` signature; `CatalogPanes.kt` list pane |
| 3 | Separate search index for root rows: `rootIndex = buildSearchIndex(rootContent)` and a `buildSearchIndex(rootContent)` overload | `Catalog.kt` (~line 356), `SearchIndex.kt` |
| 4 | Root-row search results are a distinct shape: `SearchEntry(page == null)` and a `showRootEntry` path with a **hardcoded** scroll offset `1 + pages.size + entry.index` | `Catalog.kt` `showRootEntry`/`onSearchEntrySelected` |
| 5 | `Page.subPages` separate from `Page.content` — sub-page rows always render at the top of the detail pane, so the search scroll index must be shifted by `page.subPages.size` (hardcoded) | `Page.kt`, `CatalogPanes.kt` detail pane, `Catalog.kt` `onSearchEntrySelected` |
| 6 | `Page.onClick` — a page that is really an action (activity launch); the two-pane auto-open filler must skip `onClick != null` pages | `Page.kt`, `Catalog.kt` `LaunchedEffect(isTwoPane)` |
| 7 | `listRows` is `pages` at the root, else `parent.subPages` — two different sources for the list pane | `Catalog.kt` |
| 8 | Demo: three mechanisms describe one tree — `samplePages()` + `sheetSettingsPage(...)` + `sampleRootContent(...)` — duplicated in `SampleApp` and `SheetSettings` | `demo/SamplePages.kt`, `SampleApp.kt`, `SheetSettings.kt` |

## Target API

```kotlin
// BEFORE
@Composable
fun Catalog(
    title: String,
    pages: List<Page>,
    onBack: () -> Unit = {},
    backEnabled: Boolean = true,
    showBackButton: Boolean = false,
    adaptiveInfo: WindowAdaptiveInfo? = null,
    singlePaneOnly: Boolean = false,
    rootContent: LazyListScope.() -> Unit = {},
    rootContentVersion: Int = 0,
    options: CatalogOptions = CatalogOptions(),
)

public data class Page(
    id: String,
    title: String,
    summary: String? = null,
    icon: @Composable (() -> Unit)? = null,
    subPages: List<Page> = emptyList(),
    onClick: (() -> Unit)? = null,
    contentVersion: Int = 0,
    content: LazyListScope.() -> Unit,
)

// AFTER
@Composable
fun Catalog(
    title: String,
    root: Page,                       // the single root page
    onBack: () -> Unit = {},
    backEnabled: Boolean = true,
    showBackButton: Boolean = false,
    adaptiveInfo: WindowAdaptiveInfo? = null,
    singlePaneOnly: Boolean = false,
    options: CatalogOptions = CatalogOptions(),
)

public data class Page(
    id: String,
    title: String,
    summary: String? = null,
    icon: @Composable (() -> Unit)? = null,
    contentVersion: Int = 0,
    content: LazyListScope.() -> Unit,
)

// Opening a page is a base feature of `item`: pass `page = …` and the row
// navigates into that page. With `onClick` set it performs the action instead
// (e.g. launching an activity) — replacing both the Page.onClick hack and the
// "root row" concept. A page row is styled like any other item: it has no
// dedicated card/row treatment, it just carries the page's title/summary/icon.
public fun LazyListScope.item(
    key: String? = null,
    page: Page? = null,
    title: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    actionIcon: ImageVector? = null,
    summary: String? = null,
    staticSummary: Boolean = true,
    widgetContainer: (@Composable (RowScope.() -> Unit))? = null,
    onClick: (() -> Unit)? = null,
)
```

Notes:

- `page` is **a parameter of the existing `item` builder**, not a separate
  `subPage` builder (an earlier draft added `LazyListScope.subPage`; it was folded
  into `item` so opening a page is a base capability of any item). It can sit
  anywhere in a page's `content` in any order, next to regular items and card
  groups.
- A page row renders through the **same `BasicItem`** as every other row — no forced
  card, no selection highlight, no dedicated row style ("generic, not specific").
  The only affordances are the defaults: title/summary/icon fall back to the page's
  own values, and the trailing action icon defaults to a chevron (`open_in_new` when
  `onClick` is set).
- `Page.onClick` is removed from `Page`; the action belongs to the `item` that
  references the page.
- `Page.subPages` is removed; the children of a page are exactly its `item(page = …)`
  rows, discovered by walking `content` (`Page.structure` / `childPages`).

## What falls out for free

1. **One search index.** `buildSearchIndex(root)` walks the single tree (page items
   included); the `buildSearchIndex(rootContent)` overload, `rootIndex`, and the
   `SearchEntry(page == null)` shape all disappear. Every entry has a page.
2. **No index arithmetic.** Entry indices are relative to their page's own lazy list
   (page items are just rows in that list), so `1 + pages.size + entry.index` and
   `it.index + page.subPages.size` are gone; the only remaining constant is "item 0
   is the search pill", which stays.
3. **One list-pane source.** The list pane renders the current parent page's `content`
   directly (root included); `isAtRootLevel` becomes `pagePath.size == 1`.
4. **No action-page special case.** The two-pane auto-open filler picks the first
   *page row without `onClick`* (falling back to the first row).
5. **Demo: one tree, one builder.** `samplePages()` + `sampleRootContent()` merge into
   a single `sampleRootPage(…): Page`; `SampleApp` and `SheetSettings` both pass it as
   `root`; the sheet stays a separate host that hosts the same root page in a bottom
   sheet, opened from a single-tap action row. The settings activity's catalog becomes
   `Catalog(title = "Settings", root = Page { item(page = themePage()) })`.

## Affected code

- **lib:** `Catalog.kt` (signature, navigation state, search wiring), `CatalogPanes.kt`
  (list pane rows, detail pane sub-page block removed, `PageRow` removed),
  `Page.kt` (model, `walkPages`/`findPage`/`findPagePath` walk items, `searchPages`),
  `SearchIndex.kt` (index over page items; drop root overload), `Item.kt` (`page` param
  on `item`; the earlier `SubPage.kt` draft was folded in and deleted).
- **demo:** `SamplePages.kt` (one `rootPage`), `SampleApp.kt`, `SheetSettings.kt`,
  `SettingsActivity.kt`, every page file using `subPages` (`NestedPages.kt` first).
- **tests:** `lib/src/test` (`PageTest`, `SearchIndexTest` walk the new model);
  `tools/autotest` (UI behavior is unchanged; the settings-activity helper may need
  the new row shape).

## Rollout (as executed)

1. **Migrate lib internals:** `Catalog(root = …)`; tree walking over items
   (`Page.structure` / `childPages`); `item(page = …)`; drop `pages`, `rootContent`,
   `rootContentVersion`, `Page.subPages`, `Page.onClick`, and the root search overloads
   in one pass (no deprecated sugar — backward compatibility is not a constraint).
2. **Migrate the demo** to one `sampleRootPage` per host; delete `samplePages`/
   `sampleRootContent`; settings activity and sheet wired to it.
3. **Rewrite unit tests** (`PageTest`, `SearchIndexTest`) against the root model.
4. **Verify on both devices** (`tools/autotest`, group `all`, `--orientation current`):
   10/10 on A22 (portrait) and G8X (landscape).

## Risks / open questions

- **Two-pane auto-open at the root:** today the filler opens the first *top-level
  page*; with a root page the detail pane at the root level shows the root page's
  first child — decide the exact rule (first `subPage` without `onClick`).
- **`content` still cannot read composition** (it runs during index build). If the
  new demo wants the `open_in_new` icon themed from the composition, keep the
  existing "capture before building the page" rule — no API change needed.
- **Search-result supporting text** ("Theme > Colors" trail) is derived from the
  page path; with a single root the trail gains the root segment — decide whether
  the root's title appears in trails (probably: keep current display, root is
  implicit).
- **State restoration:** `pagePath` is a list of ids — unchanged, but the root id is
  now explicit; `rememberSaveable` state survives the migration.
