"""The Compose Catalog demo device-UI test suite.

Plain ``test_*(device, ctx)`` functions that raise :class:`AssertionError` on
failure, written against the generic :class:`~autotest.device.Device` contract
plus the :class:`catalog_device.CatalogDevice` helpers. Grouped into named
feature groups; the reserved ``"all"`` group (added by :class:`~autotest.Suite`)
runs every test.

The full-screen host is two-pane only on a wide / unfolded device (smallest width
>= 600dp). ``test_nested_breadcrumb`` needs that layout, so on a narrow device it
skips (recording a ``ctx["notes"]`` entry) rather than failing.
"""
from __future__ import annotations

from autotest import keys


# --- smoke (the cheap default layer) --------------------------------------

def test_smoke_launch(device, ctx):
    device.open_main()
    assert device.foreground_package() == device.package, "sample app is not foreground"
    assert device.has_title("Compose Catalog"), "screen title is missing"
    assert device.has_title("Theme"), "the first page (Theme) is not visible"


# --- nested navigation -----------------------------------------------------

def test_nested_detail_opens(device, ctx):
    device.open_main()
    assert device.tap_title("Nested"), "could not tap the Nested page"
    assert device.has_title("General"), "Nested's 'General' sub-page is not shown"
    assert device.has_title("Advanced"), "Nested's 'Advanced' sub-page is not shown"


def test_nested_deep_and_back(device, ctx):
    device.open_main()
    assert device.tap_title("Nested"), "could not tap the Nested page"
    assert device.tap_title("Advanced"), "could not tap the Advanced sub-page"
    assert device.tap_title("Developer"), "could not tap the Developer sub-page"
    assert device.has_title("Developer"), "the Developer page was not reached"
    device.key(keys.BACK)  # pop Developer -> Advanced
    assert device.has_title("Advanced"), "back did not return to Advanced"
    device.key(keys.BACK)  # pop Advanced -> Nested
    assert device.has_title("General") and device.has_title("Advanced"), \
        "back did not return to Nested's children"


def test_nested_breadcrumb(device, ctx):
    if not device.is_two_pane():
        ctx["notes"].append("skipped breadcrumb: host is single-pane (no breadcrumb bar)")
        return
    device.open_main()
    assert device.tap_title("Nested"), "could not tap the Nested page"
    assert device.tap_title("Advanced"), "could not tap the Advanced sub-page"
    assert device.tap_title("Developer"), "could not tap the Developer sub-page"
    # At the deepest page the list pane shows only [Developer] and the detail shows
    # Developer's own rows, so the ancestors appear only in the breadcrumb trail.
    assert device.has_title("Nested") and device.has_title("Advanced"), \
        "the breadcrumb trail is missing an ancestor at the deepest page"


# --- bottom sheet ----------------------------------------------------------

def test_sheet_launches(device, ctx):
    assert device.open_sheet_inapp(), "could not open the bottom sheet from the app"
    assert device.foreground_package() == device.package, "the app is not foreground"
    assert device.sheet_contains("Compose Catalog"), "the sheet title is missing"


def test_sheet_single_pane(device, ctx):
    assert device.open_sheet_inapp(), "could not open the bottom sheet from the app"
    # A bottom sheet is single-pane: tapping a page navigates (the list is replaced
    # by the detail) rather than selecting a page in a persistent list pane. Nested's
    # sub-pages only exist in the sheet's detail, so finding them in the sheet region
    # proves the navigation happened there.
    assert device.tap_title("Nested"), "could not tap the Nested page in the sheet"
    assert device.sheet_contains("General") and device.sheet_contains("Advanced"), \
        "the sheet did not navigate to Nested's sub-pages"


# --- search ----------------------------------------------------------------

def test_search_finds_nested_page(device, ctx):
    device.open_main()
    device.search("developer")
    assert device.has_title("Developer"), "search did not surface the nested 'Developer' page"


ALL_TESTS = [
    test_smoke_launch,
    test_nested_detail_opens,
    test_nested_deep_and_back,
    test_nested_breadcrumb,
    test_sheet_launches,
    test_sheet_single_pane,
    test_search_finds_nested_page,
]

TEST_DESCRIPTIONS = {
    "test_smoke_launch": "Launch the full-screen host; assert the title and first page show.",
    "test_nested_detail_opens": "Open the Nested page; assert its sub-pages are reachable.",
    "test_nested_deep_and_back": "Drill to the deepest page, then pop back one level at a time.",
    "test_nested_breadcrumb": "Two-pane only: assert the breadcrumb shows the full trail.",
    "test_sheet_launches": "Launch the bottom-sheet host; assert it foregrounds with a title.",
    "test_sheet_single_pane": "Assert the sheet navigates single-pane (list replaced by detail).",
    "test_search_finds_nested_page": "Type a query; assert a nested page is surfaced as a result.",
}

FEATURE_GROUPS = {
    "smoke": [test_smoke_launch],
    "nested": [test_nested_detail_opens, test_nested_deep_and_back, test_nested_breadcrumb],
    "sheet": [test_sheet_launches, test_sheet_single_pane],
    "search": [test_search_finds_nested_page],
}

DEFAULT_GROUP = "smoke"
