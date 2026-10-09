"""App profile for the Compose Catalog ``demo`` app.

Subclasses AutoTest's :class:`~autotest.android.device.AndroidDevice` with the
concrete app under test: its package, its two hosts (the full-screen settings
activity and the bottom-sheet activity), and the semantic helpers the tests use
to drive :func:`~net.slions.compose.catalog.Catalog` — open a
host, read the visible titles, drive a nested page, and type into the search
pill.

This is the host-repo layer the AutoTest framework is deliberately app-agnostic
about (see the framework README). Keep all app knowledge here; keep the tests
generic over the :class:`~autotest.device.Device` contract plus these helpers.
"""
from __future__ import annotations

import sys
from pathlib import Path

# Make the framework importable whether it is pip-installed or present as the
# AutoTest submodule at the repo root (the normal case for this repo).
_HERE = Path(__file__).resolve().parent
_FRAMEWORK = _HERE.parent.parent / "AutoTest"
if _FRAMEWORK.is_dir() and str(_FRAMEWORK) not in sys.path:
    sys.path.insert(0, str(_FRAMEWORK))

from autotest.android.device import AndroidDevice  # noqa: E402

# The ``sample`` app is the test fixture for the library's screen behavior.
PACKAGE = "net.slions.compose.catalog.demo"
MAIN = "net.slions.compose.catalog.demo/.MainActivity"


class CatalogDevice(AndroidDevice):
    """A device bound to the Compose Catalog demo app."""

    #: The sample app's package id (also the default for the run resolver).
    package_id = PACKAGE

    def __init__(self, serial: str, session=None):
        super().__init__(serial, PACKAGE, session)

    # --- lifecycle --------------------------------------------------------
    def open_main(self, wait: float = 4.0) -> None:
        """(Re)launch the full-screen settings host from a clean state."""
        self.force_stop()
        self.start_component(MAIN, wait=wait)

    def open_sheet_inapp(self) -> bool:
        """Open the in-activity bottom sheet via its row. Returns True on success.

        The sheet is hosted in the same activity as the full-screen screen, so it
        is reached through the "Open in a bottom sheet" row on the "Sheet settings"
        page rather than started as a separate activity.
        """
        import time

        self.open_main()
        if not self.tap_title("Sheet settings"):
            return False
        if not self.tap_title("Open in a bottom sheet"):
            return False
        time.sleep(2.0)  # let the sheet expand and its content compose
        return True

    def open_settings_activity(self) -> bool:
        """Start the settings activity (a second catalog) via its root row.

        Unlike the sheet, this is a real activity launch: its catalog has its own
        page tree (the theme page) and therefore its own search scope, while
        sharing the app's store. The root row opens it directly on one tap.
        """
        import time

        self.open_main()
        if not self.tap_title("Settings"):
            return False
        time.sleep(2.0)  # let the new activity launch and compose
        return True

    def in_settings_activity(self) -> bool:
        """True if the settings activity's catalog is on screen.

        The theme page row only exists in that catalog's tree, so finding it
        proves the activity launched (the root row's own "Settings" title
        cannot tell the two apart).
        """
        return self.has_title("Theme")

    # --- UI state (semantic reads) ---------------------------------------
    def visible_titles(self) -> list[str]:
        """All non-empty visible node texts, in hierarchy order."""
        return [n.text.strip() for n in self.nodes() if n.text and n.text.strip()]

    def has_title(self, text: str) -> bool:
        """True if some visible node shows exactly ``text``."""
        return any(t == text for t in self.visible_titles())

    def sheet_contains(self, text: str) -> bool:
        """True if a node with exact ``text`` sits in the lower sheet region.

        The in-activity sheet is an overlay: the full-screen screen stays composed
        behind it, so plain ``has_title`` cannot tell the sheet's content apart
        from what is behind the scrim. The sheet occupies the bottom of the
        screen, so its nodes' top edge is well below the top quarter.
        """
        node = self.find_node_by_text(text)
        if not node or not node.bounds:
            return False
        _, h = self.screen_size()
        return node.bounds[1] > h * 0.25

    def is_two_pane(self) -> bool:
        """Whether the full-screen host is in its two-pane layout.

        The screen splits at the Material 3 "Medium" window size class
        (smallest width >= 600dp). A bottom sheet reports its *host* size and is
        forced single-pane, so this describes the full-screen host only — use the
        behavioral sheet test rather than this for the sheet.
        """
        return self.config().get("smallest_width_dp", 0) >= 600

    # --- navigation -------------------------------------------------------
    def swipe(self, x1: int, y1: int, x2: int, y2: int, ms: int = 250) -> None:
        """A raw on-device swipe (the base ``Device`` contract has no swipe)."""
        import time

        self.transport.shell(
            ["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms)])
        time.sleep(0.6)

    def _find_tappable(self, text: str):
        """First node with exact ``text`` and a non-zero area, else None.

        A freshly composed (but not yet laid-out) node reports bounds of
        ``[0,0][0,0]``; its center is ``(0, 0)``, which is truthy — tapping it
        is a phantom tap in the status-bar corner that changes nothing.
        """
        for node in self.nodes():
            if node.text != text or not node.bounds:
                continue
            x1, y1, x2, y2 = node.bounds
            if x2 > x1 and y2 > y1:
                return node
        return None

    def _tap_visible(self, text: str) -> bool:
        """Tap a node with exact ``text`` if it is currently visible; else False."""
        node = self._find_tappable(text)
        if node is not None:
            self.tap(*node.center)
            return True
        return False

    def tap_title(self, text: str, max_scrolls: int = 12) -> bool:
        """Tap the node whose text is exactly ``text``.

        If it is not immediately visible, scrolls the list pane (the left half in
        two-pane, the full-width list in single-pane) down until it appears, then
        taps. Sub-page rows live at the top of the detail pane and are found on the
        first pass without scrolling.
        """
        if self._tap_visible(text):
            return True
        w, h = self.screen_size()
        x = w // 4
        for _ in range(max_scrolls):
            self.swipe(x, int(h * 0.75), x, int(h * 0.35))
            if self._tap_visible(text):
                return True
        return False

    # --- search -----------------------------------------------------------
    def search(self, query: str) -> None:
        """Focus the list-pane search pill and type ``query``.

        The pill's ``"Search"`` placeholder is not its own accessibility node,
        so the field is targeted by class (the list pane's
        ``android.widget.EditText``); a positional tap near the top of the list
        pane covers a field that is not in the tree yet.
        """
        node = next(
            (n for n in self.nodes()
             if n.cls == "android.widget.EditText" and n.bounds
             and n.bounds[2] > n.bounds[0] and n.bounds[3] > n.bounds[1]),
            None)
        if node is None:
            w, h = self.screen_size()
            self.tap(max(80, w // 4), int(h * 0.14))
        else:
            self.tap(*node.center)
        # The IME may drop or auto-correct a character while the keys are
        # injected ("deveoper" for "developer"); the app searches on the
        # committed field text, so verify it and retype (after clearing) if
        # the query did not land intact.
        self._type_slow(query)
        for _ in range(2):
            committed = next(
                (n.text for n in self.nodes() if n.cls == "android.widget.EditText"),
                "")
            if query in committed:
                return
            self.clear_field()
            self._type_slow(query)

    def _type_slow(self, text: str, per_char_delay: float = 0.2) -> None:
        """Type character by character.

        Bulk ``input text`` injection races aggressive IME prediction (the LG
        IME auto-completes mid-word and drops a character); per-key injection
        with a small gap lands the text intact.
        """
        import time

        for ch in text:
            payload = " " if ch == " " else ch
            self.transport.shell(
                ["shell", "input", "text", f"'{payload}'"])
            time.sleep(per_char_delay)
