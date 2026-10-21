#!/usr/bin/env python3
"""Rebuild the TC4 mound template from the 4.2.3.5 oracle and print its checksum.

The oracle of Thaumcraft 4.2.3.5 writes the mound with 2441 explicit setBlock calls
(thaumcraft/common/lib/world/WorldGenMound.java:63-2549). The native 1.21.1 port keeps
that shape in a layer table (common/src/main/java/thaumcraft/common/lib/world/
WorldGenMoundTemplate.java). This script rebuilds the table from the oracle so that the
equality of the two can be checked at any time:

    python3 scripts/porting/extract_mound_template.py [--print]

The default output is the checksum of the canonical form of the table and the number of
blocks. With --print the sixteen layers are written as the Java string literals of the
native class.

Oracle root: /home/Acak/work/thaumcraft-4.2.3.5-oracle (override with TC4_ORACLE).
"""
from __future__ import annotations

import argparse
import hashlib
import os
import re
import sys

ORACLE = os.environ.get(
    "TC4_ORACLE", "/home/Acak/work/thaumcraft-4.2.3.5-oracle")
SOURCE = os.path.join(ORACLE, "thaumcraft/common/lib/world/WorldGenMound.java")

SIZE = 19
LAYERS = 16

# The 1.7.10 blocks of the oracle and the character of the native table.
PLAIN = {
    "field_150346_d": "D",   # dirt
    "field_150347_e": "C",   # cobblestone
    "field_150349_c": "G",   # grass
    "field_150350_a": "A",   # air
    "field_150341_Y": "M",   # mossy cobblestone
    "field_150411_aY": "I",  # iron bars
    "field_150446_ar": "0",  # stone stairs, metadata zero
}
# The blocks the oracle writes with a metadata.
META = {
    ("field_150350_a", 0): "A",    # air
    ("field_150329_H", 1): "T",    # tall grass
    ("field_150417_aV", 3): "B",   # chiselled stone bricks
    ("field_150468_ap", 5): "L",   # ladder
}
# The blocks the oracle writes outside of the table.
CONDITIONAL = {
    # The chest of the loot room is a trapped chest in one of three rolls and a plain
    # chest otherwise; the mob spawners carry their mob type and are written by the code.
    (10, 1, 9),
    # The tunder the trapped chest of the loot room, one block below the base.
    (10, -1, 9),
    (4, 5, 4),
    (4, 5, 14),
}

BLOCK = r"(?:\(Block\))?(?:Blocks|ConfigBlocks)\.(\w+)"
SET = re.compile(
    r"func_147449_b\(i ([+-]) (\d+), j ([+-]) (-?\d+), k ([+-]) (-?\d+), " + BLOCK + r"\);")
SET_META = re.compile(
    r"func_147465_d\(i ([+-]) (\d+), j ([+-]) (-?\d+), k ([+-]) (-?\d+), " + BLOCK + r", (-?\d+), 3\);")


def _offset(sign: str, value: str) -> int:
    return int(value) if sign == "+" else -int(value)


def read_table(path: str = SOURCE) -> list[str]:
    """The layer table of the oracle, one flat string per layer."""
    if not os.path.exists(path):
        raise SystemExit("the oracle is not at " + path + "; set TC4_ORACLE")
    layers = [list("." * (SIZE * SIZE)) for _ in range(LAYERS)]

    def put(x: int, y: int, z: int, symbol: str) -> None:
        if not (0 <= x < SIZE and 0 <= z < SIZE and 0 <= y < LAYERS):
            raise SystemExit(f"the oracle writes outside of the template at {x},{y},{z}")
        index = z * SIZE + x
        if layers[y][index] != ".":
            raise SystemExit(f"the oracle writes {x},{y},{z} twice")
        layers[y][index] = symbol

    for line in open(path, encoding="utf-8").read().split("\n"):
        # The two loot containers of the oracle are written with a roll of the world
        # random and are therefore not part of the fixed shape.
        if "field_73012_v" in line:
            continue
        if "func_147449_b" in line:
            match = SET.search(line)
            if not match:
                raise SystemExit("unread setBlock call: " + line.strip())
            x = _offset(match.group(1), match.group(2))
            y = _offset(match.group(3), match.group(4))
            z = _offset(match.group(5), match.group(6))
            block = match.group(7)
            if (x, y, z) in CONDITIONAL:
                continue
            put(x, y, z, PLAIN[block])
        elif "func_147465_d" in line:
            match = SET_META.search(line)
            if not match:
                raise SystemExit("unread setBlockWithMetadata call: " + line.strip())
            x = _offset(match.group(1), match.group(2))
            y = _offset(match.group(3), match.group(4))
            z = _offset(match.group(5), match.group(6))
            block = match.group(7)
            meta = int(match.group(8))
            if (x, y, z) in CONDITIONAL:
                continue
            if block == "field_150446_ar":
                put(x, y, z, str(meta))
            else:
                put(x, y, z, META[(block, meta)])
    return ["".join(layer) for layer in layers]


def canonical(layers: list[str]) -> str:
    return "\n".join(layers) + "\n"


def checksum(layers: list[str]) -> str:
    return hashlib.sha256(canonical(layers).encode("utf-8")).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--print", dest="show", action="store_true",
                        help="print the sixteen layers as the Java string literals")
    parser.add_argument("--source", default=SOURCE, help="the WorldGenMound.java of the oracle")
    arguments = parser.parse_args()

    layers = read_table(arguments.source)
    blocks = sum(1 for layer in layers for cell in layer if cell != ".")
    print("oracle  : " + arguments.source)
    print("layers  : " + str(len(layers)))
    print("blocks  : " + str(blocks))
    print("sha256  : " + checksum(layers))
    if arguments.show:
        for index, layer in enumerate(layers):
            print(f"        // layer {index}")
            for row in range(SIZE):
                print(f'        "{layer[row * SIZE:(row + 1) * SIZE]}",')
    return 0


if __name__ == "__main__":
    sys.exit(main())
