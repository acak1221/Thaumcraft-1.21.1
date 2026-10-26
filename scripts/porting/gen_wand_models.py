#!/usr/bin/env python3
"""Writes the wand's item model selector and any missing composite wand models.

The wand item (assets/thaumcraft/models/item/wand.json) picks one model per rod x cap x focus through the item properties
thaumcraft:wand_rod / wand_cap / wand_focus, whose values come from common/.../items/wands/WandModels.java. A vanilla override list
is searched from its end and the first entry whose every predicate is >= its value wins, so the entries are listed from the least to the
most specific, ordered by (rod, cap, focus): the exact combination is then always the last one that matches.

A staff rod ("<rod>_staff") uses the staff_shape / staff_focus_shape parents and the skin of its plain rod.

Run from the repository root after changing the rods, caps or foci (keep ROD_TAGS / CAP_TAGS in step with WandMaterials.java; the JUnit test
WandModelAssetsTest fails when they differ). Existing model files are kept as they are; only missing ones are created.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / "common/src/main/resources/assets/thaumcraft/models/item"
ROD_TAGS = ["wood", "greatwood", "silverwood", "obsidian", "blaze", "ice", "quartz", "bone", "reed",
            "greatwood_staff", "obsidian_staff", "blaze_staff", "ice_staff", "quartz_staff", "bone_staff", "reed_staff", "silverwood_staff", "primal_staff"]
CAP_TAGS = ["iron", "gold", "thaumium", "void", "copper"]
FOCI = [("", 0.0), ("fire", 0.25), ("excavation", 0.5), ("frost", 0.75), ("warding", 0.8125), ("trade", 0.875), ("portable_hole", 0.9375), ("hellbat", 0.953125), ("pech", 0.96875), ("primal", 0.984375), ("shock", 1.0)]


# Powers of two, like WandModels.ROD_SCALE / CAP_SCALE: the values stay exact binary fractions however many parts are added.
ROD_SCALE = 32
CAP_SCALE = 4


def value(index, scale):
    return index / scale


def is_staff(rod):
    return rod.endswith("_staff")


def base_tag(rod):
    return rod[:-len("_staff")] if is_staff(rod) else rod


def model_name(rod, cap, suffix):
    return f"wand_{rod}_{cap}" + (f"_{suffix}" if suffix else "")


def write_primal_shapes():
    # Both original depth+outer cubes have a dedicated direct-texture renderer.
    # The retained6px depth strip must stay out of the global mipmap4 block atlas.
    for kind in ["wand", "staff"]:
        shape = {"parent": f"thaumcraft:item/{kind}_shape"}
        (MODELS / f"{kind}_primal_shape.json").write_text(json.dumps(shape, indent=2) + "\n", encoding="utf-8", newline="")


def write_addon_model():
    # A wand with any add-on rod or cap (thaumcraft:wand_addon = 1, WandModels.addonValue). Its parts are drawn by the
    # builtin item renderer from the add-on's own textures, so the model only has to reach builtin/entity through the
    # shape chain; the textures are the fallback particle of the shape. One model serves wands, staves and sceptres:
    # the shape's elements are never baked for a builtin model, and the renderer picks the staff geometry itself.
    model = {"parent": "thaumcraft:item/wand_shape",
             "textures": {"rod": "thaumcraft:models/wand_rod_greatwood", "cap": "thaumcraft:models/wand_cap_iron"}}
    (MODELS / "wand_addon.json").write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8", newline="")


def main():
    write_primal_shapes()
    write_addon_model()
    overrides = []
    for r, rod in enumerate(ROD_TAGS):
        for c, cap in enumerate(CAP_TAGS):
            for suffix, focus in FOCI:
                name = model_name(rod, cap, suffix)
                path = MODELS / f"{name}.json"
                if not path.exists():
                    textures = {"rod": f"thaumcraft:models/wand_rod_{base_tag(rod)}", "cap": f"thaumcraft:models/wand_cap_{cap}"}
                    if suffix:
                        textures["focus"] = "thaumcraft:models/wand"
                    shape = ("staff" if is_staff(rod) else "wand") + ("_primal_shape" if suffix == "primal" else "_focus_shape" if suffix else "_shape")
                    model = {"parent": "thaumcraft:item/" + shape, "textures": textures}
                    path.write_text(json.dumps(model, separators=(",", ":")), encoding="utf-8", newline="")
                overrides.append({"predicate": {"thaumcraft:wand_rod": value(r, ROD_SCALE), "thaumcraft:wand_cap": value(c, CAP_SCALE),
                                                "thaumcraft:wand_focus": focus}, "model": f"thaumcraft:item/{name}"})
    # Last, so it wins whenever the wand has an add-on part; a native wand reads 0 and never meets it.
    overrides.append({"predicate": {"thaumcraft:wand_addon": 1.0}, "model": "thaumcraft:item/wand_addon"})
    # The fallback model is the SHAPE, not a flat handheld sprite.
    #
    # Every override above parents to wand_shape (builtin/entity) and supplies #rod/#cap, so the
    # item is drawn by ItemWandRenderer through the builtin item hook. A root model of
    # minecraft:item/handheld is a completely different kind of model: it carries layer0/layer1
    # sprites and no #rod/#cap, so a stack that matched NO override - or whose properties were
    # read before they were registered - fell back to a flat 2D wand picture while every other
    # wand in the game was three dimensional. It also put two incompatible model kinds in one
    # overrides list. The correct fallback is the same wand_shape the overrides use.
    wand = {"parent": "thaumcraft:item/wand_shape",
            "textures": {"rod": "thaumcraft:models/wand_rod_greatwood",
                         "cap": "thaumcraft:models/wand_cap_iron"},
            "overrides": overrides}
    (MODELS / "wand.json").write_text(json.dumps(wand, indent=1) + "\n", encoding="utf-8", newline="")
    print(f"{len(overrides)} overrides")


if __name__ == "__main__":
    main()
