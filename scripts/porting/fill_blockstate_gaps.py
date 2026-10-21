#!/usr/bin/env python3
"""Fill the missing states of the blockstate files of the port.

Minecraft 1.21.1 loads every state of a block and reports a missing model for
each state that the blockstate file does not name. The port splits the original
metadata families into several native blocks (the eldritch family, the airy
family, the cosmetic solids, the opaque family) and every one of those blocks
keeps the wide variant property of its family, so the states that the block
never hosts are reported as missing. The arcane furnace has a fill level of
nineteen steps and only three models, so its other levels are missing too.

The script reads the report of the game (the missing states) and names a model
for every one of them: the nearest existing state of the same block, so the
unreachable variants borrow the model of the block itself and the furnace levels
borrow the model of the nearest filled level.

Usage:
    fill_blockstate_gaps.py --log <latest.log> [--dry-run]
"""
from __future__ import annotations

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path

ASSETS = Path("common/src/main/resources/assets/thaumcraft/blockstates")
# The blocks whose variant property carries only the metadata values of their own group.
NARROW_PROPERTY_BLOCKS = {
    "eldritch", "eldritch_crust", "eldritch_deco", "eldritch_doorway", "eldritch_spawner",
    "eldritch_crystal", "airy", "airy_node", "airy_hole", "airy_unbreakable",
    "cosmetic_solid", "cosmetic_solid_tough", "cosmetic_solid_thaumium",
    "cosmetic_opaque", "cosmetic_opaque_warded",
}
REPORT = re.compile(r"blockstates/(?P<file>[a-z0-9_]+)\.json' missing model for variant: "
                    r"'[a-z0-9_]+:[a-z0-9_]+#(?P<state>[^']+)'")


def parse_log(path: Path) -> dict[str, set[str]]:
    missing: dict[str, set[str]] = defaultdict(set)
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        match = REPORT.search(line)
        if match:
            missing[match.group("file")].add(match.group("state"))
    return missing


def state_key(state: str) -> dict[str, str]:
    out: dict[str, str] = {}
    for part in state.split(","):
        name, _, value = part.partition("=")
        out[name] = value
    return out


def model_of(entry) -> str | None:
    """The model of a state entry; a blockstate may name a list of models."""
    if isinstance(entry, dict):
        model = entry.get("model")
        return model if isinstance(model, str) else None
    if isinstance(entry, list) and entry:
        return model_of(entry[0])
    return None


def model_for(existing: dict[str, dict], state: str) -> str | None:
    """The model of the nearest existing state of the same block."""
    wanted = state_key(state)
    if not existing:
        return None
    # the states that differ only in the fill level borrow the nearest filled level
    others = [key for key in existing if set(state_key(key)) - {"level"} == set(wanted) - {"level"}]
    if others and "level" in wanted:
        level = int(wanted["level"])
        best = min(others, key=lambda key: abs(int(state_key(key)["level"]) - level))
        return model_of(existing[best])
    # the unreachable variants borrow the model of the nearest existing variant
    if "variant" in wanted:
        variant = int(wanted["variant"])
        same = [key for key in existing if set(state_key(key)) == set(wanted)]
        if same:
            best = min(same, key=lambda key: abs(int(state_key(key)["variant"]) - variant))
            return model_of(existing[best])
    first = sorted(existing)[0]
    return model_of(existing[first])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, required=True, help="the log of the game with the missing model reports")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    missing = parse_log(args.log)
    total = 0
    for name, states in sorted(missing.items()):
        # The metadata families carry a narrow variant property now, so their blockstate names
        # exactly their own metadata values and there is nothing to fill.
        if name in NARROW_PROPERTY_BLOCKS:
            continue
        path = ASSETS / f"{name}.json"
        if not path.exists():
            print(f"!! {path} is missing")
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        variants = data.get("variants", {})
        added = 0
        for state in sorted(states):
            if state in variants:
                continue
            model = model_for(variants, state)
            if model is None:
                continue
            variants[state] = {"model": model}
            added += 1
        if added:
            total += added
            data["variants"] = {key: variants[key] for key in sorted(variants, key=lambda k: [int(p) if p.isdigit() else p for p in state_key(k).values()])}
            if not args.dry_run:
                path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print(f"{path}: {added} states named")
    print(f"total {total} states named")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
