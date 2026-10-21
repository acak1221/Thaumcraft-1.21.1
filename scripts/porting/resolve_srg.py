#!/usr/bin/env python3
"""Resolve the 1.7.10 SRG member names of the decompiled oracle into readable names.

The oracle is a decompilation of the original 1.7.10 JAR, so every vanilla member
carries its SRG name (`field_76367_g`, `func_149712_f`). The 1.7.10 MCP mapping
(`de.oceanlabs.mcp:mcp_stable:12-1.7.10`) turns those back into the readable
vanilla names.

This matters because the SRG-to-MCP mapping CHANGED between 1.7.10 and 1.12: the
1.12 legacy reference in this repository resolves `field_76367_g` to IN_WALL, while
the 1.7.10 mapping resolves it to `cactus`. The oracle is 1.7.10, so the 1.7.10
mapping is the only correct one.

The mapping is a build-time input and is NOT committed. Fetch it with:
    curl -sL -o /tmp/mcp-stable.zip \\
      https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/12-1.7.10/mcp_stable-12-1.7.10.zip
    unzip -o -q /tmp/mcp-stable.zip -d /tmp/mcp-stable

Usage:
    resolve_srg.py field_76367_g func_149712_f
    resolve_srg.py --grep field_763
    resolve_srg.py --coverage
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ORACLE = ROOT.parent / "work/thaumcraft-4.2.3.5-oracle"
DEFAULT_DIR = Path("/tmp/mcp-stable")
DEFAULT_OUT = ROOT / "docs/porting/srg-name-map.json"
SRG = re.compile(r"\b(?:field|func)_\d+_[a-zA-Z]{1,2}\b")


def load(directory: Path) -> dict[str, str]:
    names: dict[str, str] = {}
    for file in ("fields.csv", "methods.csv"):
        path = directory / file
        if not path.exists():
            continue
        with path.open(encoding="utf-8", errors="replace", newline="") as handle:
            for row in csv.reader(handle):
                if len(row) >= 2 and SRG.fullmatch(row[0].strip()):
                    names[row[0].strip()] = row[1].strip()
    return names


def oracle_names(root: Path) -> set[str]:
    found: set[str] = set()
    for path in root.rglob("*.java"):
        found.update(SRG.findall(path.read_text(encoding="utf-8", errors="replace")))
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("names", nargs="*")
    parser.add_argument("--dir", type=Path, default=DEFAULT_DIR)
    parser.add_argument("--grep")
    parser.add_argument("--coverage", action="store_true")
    parser.add_argument("--export", type=Path)
    args = parser.parse_args()

    if not (args.dir / "fields.csv").exists():
        print(f"missing the 1.7.10 MCP mapping in {args.dir}; fetch mcp_stable-12-1.7.10.zip first",
              file=sys.stderr)
        return 2
    names = load(args.dir)

    if args.coverage:
        used = oracle_names(ORACLE)
        resolved = {n for n in used if n in names}
        print(f"oracle members: {len(used)}  resolvable: {len(resolved)} "
              f"({len(resolved) * 100 // max(1, len(used))} %)")
        for name in sorted(used - resolved)[:20]:
            print("  unresolved", name)
        return 0

    if args.export:
        used = sorted(oracle_names(ORACLE))
        table = {n: names[n] for n in used if n in names}
        args.export.parent.mkdir(parents=True, exist_ok=True)
        args.export.write_text(json.dumps(table, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        print(f"wrote {len(table)} resolved names to {args.export}")
        return 0

    if args.grep:
        for name in sorted(n for n in names if args.grep in n):
            print(f"{name} -> {names[name]}")
        return 0

    for name in args.names:
        print(f"{name} -> {names.get(name, 'UNRESOLVED')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
