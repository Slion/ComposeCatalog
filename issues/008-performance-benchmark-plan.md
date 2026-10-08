# 008 — P0 (enabler): Baseline on the A22 before any fix

**Status:** done (A22 baseline + post-fix recorded 2026-10-08; CI scroll-scenario runs not set up) · **Area:** `:benchmark` module (macrobenchmark 1.2.4, `com.android.test`), CI

## Setup

- Standalone `:benchmark` module (self-instrumenting, non-debuggable, `CompilationMode.None()`).
- Every frame scenario resets state first: `pm clear` + cold launch (`freshRoot`), so the
  fresh-launch frames are part of the metrics. Baseline and post-fix runs share the protocol.
- Devices: A22 (acceptance). **G8X cannot run frame benchmarks** — LG ROM has no
  `/data/misc/perfetto-traces` (no `traced` daemon, `tracing` service) and SELinux blocks the
  app user from any alternative trace-output path. G8X stays the landscape UI-test device.

## Scenarios

| Scenario | Action | Metric |
|---|---|---|
| `ColdStart` | cold launch, 8 iters | time-to-initial-display |
| `RootPageScroll` | freshRoot, 4 list-pane flings | p95 frame, dropped %, janky frames |
| `DetailPageScroll` | freshRoot, open Theme page, 4 detail-pane flings | + detail content-group recomposition count (002) |
| `ToggleRow` | freshRoot, scroll to "Root switch", 10 taps 300 ms apart | frame delta per tap (003) |
| `SearchType` | freshRoot, open Search, `setText` 12-char query | frame cost of the query change (005) |

Perfetto baseline: frametracer + process_stats + art (GC) on `RootPageScroll`, attached
here before fixes land.

## Acceptance (for 001/002)

- A22: p95 frame ≤ 33 ms, dropped < 1 %, no > 8 ms GC on the frame thread.
- `DetailPageScroll`: 0 detail content-group recompositions per frame; `RootPageScroll`:
  no content-builder re-runs during a pure fling.
- CI: emulator runs of the two scroll scenarios with a generous dropped-frame threshold.

## Run command

```powershell
# build + install (once)
.\gradlew :benchmark:assembleDebug
adb install -r benchmark\build\outputs\apk\debug\benchmark-debug.apk
# run all scenarios (A22)
adb -s R58R91GBTZK shell "am instrument -w -e class 'net.slions.compose.preference.benchmark.BenchmarkScenarios' 'net.slions.compose.preference.benchmark/androidx.test.runner.AndroidJUnitRunner'"
```

## Results

Method: `p95 frame` = p95 of the app layer's actual frame timeline duration
(`tools/bench_trace.py` over the recorded Perfetto traces, worst of the iterations);
`Dropped %` = 100 − on-time present (worst iteration). Pre-fix baseline, non-minified
benchmark variant, `CompilationMode.None()`, fresh `pm clear` state per iteration
(fresh-launch frames included).

Baseline (pre-fix), 2026-10-08, A22 (SM-A225F, portrait):

| Scenario | p95 frame | Dropped % | Compose CPU p95 |
|---|---|---|---|
| ColdStart (TTI) | — (median 887 ms, min 780 / max 1035) | — | — |
| RootPageScroll | 29.2 ms | 97.1 % | 21.6 ms |
| DetailPageScroll | 25.8 ms | 100 % | 21.1 ms |
| ToggleRow | 27.9 ms | 98.9 % | 26.6 ms |
| SearchType | 33.5 ms | 99.8 % | 37.4 ms |

Late presents are dominated by Buffer Stuffing (unminified app on a 60 Hz panel);
the App-Deadline-Missed share is the actionable jank.

Post-fix (001 + 002 + 003 in the benchmark APK; 005/006 are behaviour-neutral and
landed after the APK build), 2026-10-08, A22, same protocol:

| Scenario | p95 frame | Dropped % | Compose CPU p95 | Δ p95 frame |
|---|---|---|---|---|
| ColdStart (TTI) | — (median 854 ms, min 777 / max 1,074) | — | — | −33 ms |
| RootPageScroll | 30.8 ms | 95.0 % | 23.2 ms | +1.6 ms |
| DetailPageScroll | 25.9 ms | 99.9 % | 21.5 ms | +0.1 ms |
| ToggleRow | 28.6 ms | 99.5 % | 28.9 ms | +0.7 ms |
| SearchType | 20.6 ms | 97.9 % | 34.2 ms | −12.9 ms |

Reading:

- No fix regressed the frame metrics; deltas on the scroll/toggle scenarios are
  within run-to-run noise. The dropped % stays high because it is dominated by
  Buffer Stuffing (unminified build, 60 Hz panel) — the presentation pipeline the
  fixes don't touch. A minified release build is the lever for the < 1 % dropped
  acceptance, not the 001/002/003 fixes.
- SearchType's −12.9 ms looks like the biggest win, but the search code is
  identical in both APKs (005 landed after the build) — treat as variance.
- One ToggleRow frame had a 3.19 s overrun (system hiccup during the tap
  sequence) — outlier, excluded from the p95.
- The first post-fix coldStart pass measured TTI median 1,225 ms under heavy
  device memory pressure (226 MB free, 1.27 GB swap used, Play Protect dialog
  active); the re-run above is the valid number.
- Macrobenchmark gotcha: every scenario start uninstalls + reinstalls the target
  app (`CompilationMode.None.shouldReset() == true`), which (a) wipes the trace
  output directory of the previous scenario and (b) can trip Play Protect's
  "security check" interstitial, which holds the `pm install` open until
  dismissed ("Don't send" tap).
