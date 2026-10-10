"""The Compose Toolkit demo device-UI test suite.

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

import time

from autotest import keys


# --- smoke (the cheap default layer) --------------------------------------

def test_smoke_launch(device, ctx):
    device.open_main(wait=6.0)
    assert device.foreground_package() == device.package, "sample app is not foreground"
    assert device.has_title("Compose Toolkit"), "screen title is missing"
    assert device.has_title("Item"), "the first page (Item) is not visible"


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
    assert device.sheet_contains("Compose Toolkit"), "the sheet title is missing"


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
    time.sleep(2.0)  # let the result list settle before asserting
    assert device.has_title("Developer"), "search did not surface the nested 'Developer' page"


# --- settings activity (a second Catalog) ----------------------------------

def test_settings_activity_opens(device, ctx):
    assert device.open_settings_activity(), "could not open the settings activity"
    assert device.foreground_package() == device.package, "the app is not foreground"
    assert device.in_settings_activity(), "the settings activity's Catalog is not shown"


def test_settings_activity_back_closes(device, ctx):
    assert device.open_settings_activity(), "could not open the settings activity"
    assert device.in_settings_activity(), "the settings activity did not open"
    device.key(keys.BACK)
    assert device.has_title("Compose Toolkit"), \
        "system back at the root of the settings activity did not close it"


def test_settings_search_scope(device, ctx):
    """The settings activity's Catalog has its own search index (theme page only):
    a query that matches only the main Catalog's tree surfaces nothing here, while a
    theme query does."""
    assert device.open_settings_activity(), "could not open the settings activity"
    device.search("nested")
    time.sleep(1.0)  # let the result list settle before asserting
    assert not device.has_title("Nested"), \
        "search leaked the main Catalog's tree into the settings activity's scope"
    device.clear_field()
    device.search("corner")
    time.sleep(2.0)  # let the result list settle before asserting
    assert device.has_title("Corner"), \
        "search did not surface the theme page's 'Corner' row"


ALL_TESTS = [
    test_smoke_launch,
    test_nested_detail_opens,
    test_nested_deep_and_back,
    test_nested_breadcrumb,
    test_sheet_launches,
    test_sheet_single_pane,
    test_search_finds_nested_page,
    test_settings_activity_opens,
    test_settings_activity_back_closes,
    test_settings_search_scope,
]

TEST_DESCRIPTIONS = {
    "test_smoke_launch": "Launch the full-screen host; assert the title and first page show.",
    "test_nested_detail_opens": "Open the Nested page; assert its sub-pages are reachable.",
    "test_nested_deep_and_back": "Drill to the deepest page, then pop back one level at a time.",
    "test_nested_breadcrumb": "Two-pane only: assert the breadcrumb shows the full trail.",
    "test_sheet_launches": "Launch the bottom-sheet host; assert it foregrounds with a title.",
    "test_sheet_single_pane": "Assert the sheet navigates single-pane (list replaced by detail).",
    "test_search_finds_nested_page": "Type a query; assert a nested page is surfaced as a result.",
    "test_settings_activity_opens": "Open the settings activity; assert its Catalog foregrounds with a title.",
    "test_settings_activity_back_closes": "System back at the settings activity's root closes it.",
    "test_settings_search_scope": "The settings activity's search covers only its own (theme) page.",
}

FEATURE_GROUPS = {
    "smoke": [test_smoke_launch],
    "nested": [test_nested_detail_opens, test_nested_deep_and_back, test_nested_breadcrumb],
    "sheet": [test_sheet_launches, test_sheet_single_pane],
    "search": [test_search_finds_nested_page],
    "settings": [
        test_settings_activity_opens,
        test_settings_activity_back_closes,
        test_settings_search_scope,
    ],
}

DEFAULT_GROUP = "smoke"
