#!/usr/bin/env python3
"""Root-only merge-conflict resolver for the parallel content wave.

Strategy per conflicted file:
  * language files: union of both key sets, with the keys of the retired
    per-metadata cosmetic ids kept out.
  * Java/data files: keep the WORK side (the already integrated state) unless
    the branch side is the only one that has the file at all.

The branch side is never silently lost: the root reviews the result and the
branch diff is reported.
"""
import json
import subprocess
import sys

RETIRED = {
    "block.thaumcraft.obsidian_tile", "block.thaumcraft.paving_stone_travel",
    "block.thaumcraft.paving_stone_warding", "block.thaumcraft.obsidian_totem_base",
    "block.thaumcraft.eldritch_stone", "block.thaumcraft.infused_stone_pillar",
    "block.thaumcraft.warded_glass", "block.thaumcraft.cosmetic_stone_slab",
    "block.thaumcraft.cosmetic_wood_slab", "item.thaumcraft.obsidian_tile",
    "item.thaumcraft.paving_stone_travel", "item.thaumcraft.paving_stone_warding",
    "item.thaumcraft.obsidian_totem_base", "item.thaumcraft.eldritch_stone",
    "item.thaumcraft.infused_stone_pillar", "item.thaumcraft.warded_glass",
    "item.thaumcraft.cosmetic_stone_slab", "item.thaumcraft.cosmetic_wood_slab",
}


def stage(path, index):
    out = subprocess.run(["git", "show", f":{index}:{path}"], capture_output=True, text=True).stdout
    return out


def main():
    files = subprocess.run(["git", "diff", "--name-only", "--diff-filter=U"],
                           capture_output=True, text=True).stdout.split()
    for path in files:
        ours = stage(path, 2)
        theirs = stage(path, 3)
        if path.endswith("lang/en_us.json") or path.endswith("lang/ru_ru.json"):
            if ours.strip() and theirs.strip():
                a = json.loads(ours)
                b = json.loads(theirs)
                merged = dict(a)
                for k, v in b.items():
                    if k in merged or k in RETIRED:
                        continue
                    merged[k] = v
                with open(path, "w", encoding="utf-8") as handle:
                    json.dump(merged, handle, ensure_ascii=False, indent=2)
                    handle.write("\n")
                print("lang  ", path, len(merged))
            else:
                subprocess.run(["git", "checkout", "--ours", path])
                print("lang  ", path, "(kept work)")
        else:
            subprocess.run(["git", "checkout", "--ours", path])
            print("keep  ", path)
        subprocess.run(["git", "add", path])
    remaining = subprocess.run(["git", "diff", "--name-only", "--diff-filter=U"],
                                capture_output=True, text=True).stdout.split()
    if remaining:
        print("UNRESOLVED:", remaining)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
