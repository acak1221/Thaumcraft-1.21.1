#!/usr/bin/env python3
"""Crop TC4's retained 18x18 potion atlas cells into 1.21's named mob-effect sprites.

Requires Pillow. Input is retained immutable TC4 artwork; no donor/generated image is used.
Positions are the original Potion*.setIconIndex coordinates plus GUIInventory's
vanilla y198 icon origin, not guessed from display colors.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "thaumcraft_src/assets/thaumcraft/textures/misc/potions.png"
TARGET = ROOT / "common/src/main/resources/assets/thaumcraft/textures/mob_effect"
CELLS = {"warp_ward": (3, 2), "death_gaze": (4, 2), "blurred_vision": (5, 2),
         "sun_scorned": (6, 2), "thaumarhia": (7, 2), "unnatural_hunger": (7, 1)}


def main():
    TARGET.mkdir(parents=True, exist_ok=True)
    with Image.open(SOURCE) as atlas:
        for name, (x, y) in CELLS.items():
            atlas.crop((18*x, 198+18*y, 18*(x+1), 198+18*(y+1))).save(TARGET / f"{name}.png")


if __name__ == "__main__":
    main()
