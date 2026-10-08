"""Summarize macrobenchmark Perfetto traces (frame p95 / on-time present rate).

Usage: python tools/bench_trace.py <trace-or-dir> [...]

Finds the trace_processor_shell binary cached by the Perfetto CLI wrapper
(`curl -LO https://get.perfetto.dev/trace_processor`, run once anywhere).
Groups the frames of the benchmarked app's layer and reports, per trace:
frame count, on-time-present %, p95 frame duration, and the jank breakdown.
"""

import glob
import os
import subprocess
import sys

APP_LAYER = "compose.preference.sample"


def find_shell() -> str:
    home = os.path.expanduser("~")
    pattern = os.path.join(home, ".local", "share", "perfetto", "prebuilts", "trace_processor_shell-*")
    hits = sorted(glob.glob(pattern))
    if not hits:
        sys.exit("trace_processor_shell not found; download once: curl -LO https://get.perfetto.dev/trace_processor")
    return hits[-1]


def query(shell: str, trace: str, sql: str) -> list[str]:
    out = subprocess.run(
        [shell, "query", trace, sql],
        check=True,
        capture_output=True,
        text=True,
    ).stdout
    return [s.strip() for s in out.split("\n\n") if s.strip()]


def summarize(shell: str, trace: str) -> None:
    where = f"layer_name LIKE '%{APP_LAYER}%'"
    # One load, three statements; result sets are CSV separated by a blank line.
    sets = query(
        shell,
        trace,
        f"SELECT count(*) FROM actual_frame_timeline_slice WHERE {where}; "
        f"SELECT 100.0 * sum(present_type = 'On-time Present') / count(*) "
        f"FROM actual_frame_timeline_slice WHERE {where}; "
        f"SELECT jt || ':' || c FROM (SELECT coalesce(nullif(jank_type, ''), 'none') AS jt, count(*) AS c "
        f"FROM actual_frame_timeline_slice WHERE {where} GROUP BY 1) ORDER BY c DESC",
    )
    n = int(sets[0].splitlines()[-1])
    if n == 0:
        print(f"{os.path.basename(trace)}: no app frames")
        return
    ontime = float(sets[1].splitlines()[-1])
    p95 = query(
        shell,
        trace,
        f"SELECT round(dur / 1e6, 1) FROM actual_frame_timeline_slice WHERE {where} "
        f"ORDER BY dur ASC LIMIT 1 OFFSET {int(n * 0.95) - 1}",
    )[0].splitlines()[-1]
    jank = " ".join(line.strip('"') for line in sets[2].splitlines()[1:])
    print(f"{os.path.basename(trace)}: frames={n} on-time={ontime:.1f}% p95={p95}ms  {jank}")


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    shell = find_shell()
    traces = []
    for arg in sys.argv[1:]:
        if os.path.isdir(arg):
            traces.extend(sorted(glob.glob(os.path.join(arg, "**", "*.perfetto-trace"), recursive=True)))
        else:
            traces.append(arg)
    for trace in traces:
        summarize(shell, trace)


if __name__ == "__main__":
    main()
