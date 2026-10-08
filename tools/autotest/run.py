#!/usr/bin/env python3
"""CLI entry point for the Compose Preference device-UI tests.

A thin wrapper over the AutoTest framework: it builds the sample app's
:class:`~autotest.Suite` and runs it. The framework is imported from the
``subs/AutoTest`` submodule at the repo root (bootstrapped below), and the app
profile / tests live next to this file.

Examples::

    python tools/autotest/run.py                          # default "smoke" group
    python tools/autotest/run.py --group nested
    python tools/autotest/run.py --group all
    python tools/autotest/run.py --test search
    python tools/autotest/run.py --group all --device A2VQ024108006964 --orientation portrait
    python tools/autotest/run.py --list
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
_REPO_ROOT = _HERE.parent.parent
_FRAMEWORK = _REPO_ROOT / "subs" / "AutoTest"
for _p in (str(_HERE), str(_FRAMEWORK)):
    if Path(_p).is_dir() and _p not in sys.path:
        sys.path.insert(0, _p)

from autotest import Runner, Suite  # noqa: E402
from preference_device import PreferenceDevice  # noqa: E402
from tests import ALL_TESTS, DEFAULT_GROUP, FEATURE_GROUPS, TEST_DESCRIPTIONS  # noqa: E402

RESULTS_DIR = _HERE / "results"


def resolve(device: str | None, use_all: bool, package: str | None):
    """Map the CLI device selection to :class:`PreferenceDevice` objects."""
    from autotest.android import adb

    serials = adb.resolve_devices(device, use_all)
    pkg = package or PreferenceDevice.package_id
    return [PreferenceDevice(s, None) for s in serials]


def main() -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--device", help="adb serial; default: the only connected device")
    parser.add_argument("--all", action="store_true", help="run on every connected device")
    parser.add_argument("--package", help="override the app package (default: the sample)")
    parser.add_argument("--test", help="run tests whose name contains this substring")
    parser.add_argument("--group", help=f"run a named group: {', '.join(sorted(FEATURE_GROUPS))}")
    parser.add_argument("--restart", action="store_true", help="restart the app between tests")
    parser.add_argument("--orientation", choices=["portrait", "landscape", "sensor"],
                        help="force an orientation for the run")
    parser.add_argument("--notify", action="store_true",
                        help="post a live progress notification on the device")
    parser.add_argument("--no-save", action="store_true", help="don't persist results")
    parser.add_argument("--list", action="store_true", help="list tests and exit")
    args = parser.parse_args()

    suite = Suite(tests=ALL_TESTS, descriptions=TEST_DESCRIPTIONS,
                  groups=FEATURE_GROUPS, default_group=DEFAULT_GROUP)
    runner = Runner(resolve, suite, results_dir=str(RESULTS_DIR))
    return runner.run(
        device=args.device, use_all=args.all, package=args.package,
        test=args.test, group=args.group, restart=args.restart,
        keep_tabs=False, orientation=args.orientation,
        no_save=args.no_save, notify=args.notify, list=args.list,
    )


if __name__ == "__main__":
    sys.exit(main())
