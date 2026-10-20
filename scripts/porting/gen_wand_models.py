#!/usr/bin/env python3
"""Writes the wand's item model selector and any missing composite wand models.

The wand item (assets/thaumcraft/models/item/wand.json) picks one model per rod x cap x focus through the item properties
thaumcraft:wand_rod / wand_cap / wand_focus, whose values come from common/.../items/wands/WandModels.java. A vanilla override list
is searched from its end and the first entry whose every predicate is >= its value wins, so the entries are listed from the least to the
most specific, ordered by (rod, cap, focus): the exact combination is then always the last one that matches.

Run from the repository root after changing the rods, caps or foci (keep ROD_TAGS / CAP_TAGS in step with WandMaterials.java; the JUnit test
WandModelAssetsTest fails when they differ). Existing model files are kept as they are; only missing ones are created.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / "common/src/main/resources/assets/thaumcraft/models/item"
ROD_TAGS = ["wood", "greatwood", "silverwood", "obsidian", "blaze", "ice", "quartz", "bone", "reed"]
CAP_TAGS = ["iron", "gold", "thaumium"]
FOCI = [("", 0.0), ("fire", 0.25), ("excavation", 0.5), ("frost", 0.75), ("shock", 1.0)]


def value(index, count):
    return index / (count - 1)


def model_name(rod, cap, suffix):
    return f"wand_{rod}_{cap}" + (f"_{suffix}" if suffix else "")


def main():
    overrides = []
    for r, rod in enumerate(ROD_TAGS):
        for c, cap in enumerate(CAP_TAGS):
            for suffix, focus in FOCI:
                name = model_name(rod, cap, suffix)
                path = MODELS / f"{name}.json"
                if not path.exists():
                    textures = {"rod": f"thaumcraft:models/wand_rod_{rod}", "cap": f"thaumcraft:models/wand_cap_{cap}"}
                    if suffix:
                        textures["focus"] = "thaumcraft:models/wand"
                    model = {"parent": "thaumcraft:item/" + ("wand_focus_shape" if suffix else "wand_shape"), "textures": textures}
                    path.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8", newline="")
                overrides.append({"predicate": {"thaumcraft:wand_rod": value(r, len(ROD_TAGS)), "thaumcraft:wand_cap": value(c, len(CAP_TAGS)),
                                                "thaumcraft:wand_focus": focus}, "model": f"thaumcraft:item/{name}"})
    wand = {"parent": "minecraft:item/handheld",
            "textures": {"layer0": "thaumcraft:items/wand_rod_greatwood", "layer1": "thaumcraft:items/wand_cap_iron"},
            "overrides": overrides}
    (MODELS / "wand.json").write_text(json.dumps(wand, indent=1) + "\n", encoding="utf-8", newline="")
    print(f"{len(overrides)} overrides")


if __name__ == "__main__":
    main()
