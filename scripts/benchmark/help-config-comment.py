#!/usr/bin/env python3
"""Render a gradle-profiler benchmark.csv into a Markdown comparison table.

Usage: help-config-comment.py <benchmark.csv> <output.md>

The CSV has one column per scenario. gradle-profiler does not write summary
rows to the CSV (those live in the HTML report), so we read the `scenario`
header row for the titles and average the `measured build #N` rows ourselves to
get the mean total build time in milliseconds.
"""
import csv
import sys

MARKER = "<!-- help-config-benchmark -->"


def main(csv_path: str, out_path: str) -> None:
    header = None
    measured = []
    with open(csv_path, newline="") as f:
        for row in csv.reader(f):
            if not row:
                continue
            if row[0] == "scenario":
                header = row
            elif row[0].startswith("measured build"):
                measured.append(row)

    if header is None or not measured:
        body = f"{MARKER}\n### `help` configuration benchmark\n\nCould not parse benchmark results."
        with open(out_path, "w") as f:
            f.write(body)
        return

    titles = header[1:]
    means = []
    for col in range(1, 1 + len(titles)):
        values = [float(r[col]) for r in measured]
        means.append(sum(values) / len(values))
    by_title = dict(zip(titles, means))

    base = by_title.get("help base")
    pr = by_title.get("help PR")

    lines = [
        MARKER,
        "### `help` configuration benchmark (configuration cache disabled)",
        "",
        "Mean of 5 builds after 2 warm-ups.",
        "",
        "| Scenario | Mean build time |",
        "| --- | --- |",
        f"| Base (`help base`) | {base:.0f} ms |",
        f"| PR (`help PR`) | {pr:.0f} ms |",
    ]

    delta = pr - base
    pct = (delta / base) * 100 if base else 0
    sign = "🔺" if delta > 0 else "✅"
    lines.append(f"| **Difference** | {sign} {delta:+.0f} ms ({pct:+.1f}%) |")

    with open(out_path, "w") as f:
        f.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
