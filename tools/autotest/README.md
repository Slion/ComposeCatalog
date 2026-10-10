# Compose Toolkit device-UI tests

Device-UI tests for the `sample` app, which doubles as the fixture for the
library's screen behavior. Built on the
[AutoTest](https://github.com/Slion/AutoTest) framework (a git submodule at
[`../../subs/AutoTest`](../../subs/AutoTest)): the framework is app-agnostic, and this
directory holds the **app profile** and the **tests** for the Compose Toolkit
sample.

## Layout

| File | Purpose |
| --- | --- |
| `catalog_device.py` | `CatalogDevice` — the app profile (package, hosts, semantic helpers). |
| `tests.py` | The test functions, their descriptions, and the feature groups. |
| `run.py` | CLI entry point — a thin wrapper over the AutoTest `Runner`. |
| `results/` | Per-device-model + configuration results (generated; git-ignored). |

## Setup

1. Python 3.10+ with `adb` on `PATH`.
2. Init the framework submodule and install the one dependency:

   ```sh
   git submodule update --init subs/AutoTest
   pip install -r tools/autotest/requirements.txt   # PyYAML
   ```

3. Build and install the sample app on the target:

   ```sh
   ./gradlew :demo:assembleDebug
   adb install -r demo/build/outputs/apk/debug/demo-debug.apk
   ```

## Run

```sh
# the cheap default layer (smoke)
python tools/autotest/run.py

# a named group
python tools/autotest/run.py --group nested
python tools/autotest/run.py --group sheet
python tools/autotest/run.py --group search

# everything
python tools/autotest/run.py --group all

# one test by name substring
python tools/autotest/run.py --test search

# a specific device, forcing an orientation, with a live progress notification
python tools/autotest/run.py --group all --device A2VQ024108006964 --orientation portrait --notify

# list the tests and exit
python tools/autotest/run.py --list
```

Groups: `smoke` (default), `nested`, `sheet`, `search`, and the reserved `all`.
Each run records results per device model + configuration (orientation, rotation,
smallest-width) and diffs them against the previous run, so a regression in a test
that used to pass is flagged.

## Notes

- The full-screen host is **two-pane only on a wide / unfolded device**
  (smallest width >= 600dp). `test_nested_breadcrumb` needs that layout, so on a
  narrow device it skips (with a note) rather than failing. Run the `nested` group
  on the unfolded foldable to exercise the breadcrumb.
- The `sheet` group targets the in-activity bottom sheet, which is **always
  single-pane** (`singlePaneOnly`). The full-screen screen stays composed behind
  the sheet's scrim, so these assertions check nodes in the sheet's lower screen
  region (`CatalogDevice.sheet_contains`) rather than global titles. They are
  behavioral (tapping a page replaces the list with the detail) and do not depend
  on device width.
- Tests are self-contained: each opens the host it needs from a clean state
  (`force-stop` + relaunch), so test order does not matter.
- Cross-check what a failing test reveals against
  [`../../docs/known-issues.md`](../../docs/known-issues.md); add new open issues
  there as they surface.
