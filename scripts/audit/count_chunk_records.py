#!/usr/bin/env python3
"""Count persisted terrain chunk records (including proto chunks) in a stopped test world.

This reads region location headers, not file sizes or an estimate from FULL chunks.
Entities/POI are deliberately excluded. Run only after the server finishes saving.
"""
import json
from pathlib import Path
import struct
import sys


def count(world: Path) -> dict[str, int]:
    result: dict[str, int] = {}
    for directory in sorted(world.rglob("region")):
        if not directory.is_dir():
            continue
        total = 0
        for region in directory.glob("r.*.*.mca"):
            with region.open("rb") as source:
                header = source.read(4096)
            if len(header) != 4096:
                raise ValueError(f"Incomplete location header: {region}")
            total += sum(value != 0 for value in struct.unpack(">1024I", header))
        result[str(directory.relative_to(world))] = total
    return result


if __name__ == "__main__":
    counts = count(Path(sys.argv[1]))
    print(json.dumps({"terrain_records": counts, "total": sum(counts.values())}, indent=2))
