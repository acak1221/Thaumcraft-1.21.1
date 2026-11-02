"""Keep the completed wand presentation reachable after foundation regeneration.

The live selector is a read-only fixture. The actual foundation generator writes
only into a temporary ASSETS directory; original art is neither copied nor read.
"""
from contextlib import redirect_stdout
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import convert_assets as converter
import gen_wand_models


class ConvertWandCompositionReviewTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        original_path = converter.ASSETS / "models/item/wand.json"
        cls.original = json.loads(original_path.read_text(encoding="utf-8"))
        temporary = tempfile.TemporaryDirectory(prefix="tc4-wand-composition-review-")
        cls.addClassCleanup(temporary.cleanup)
        cls.assets = Path(temporary.name)
        output = io.StringIO()
        with patch.object(converter, "ASSETS", cls.assets), redirect_stdout(output):
            converter.create_models()
        cls.generation_output = output.getvalue()
        cls.regenerated = json.loads(
            (cls.assets / "models/item/wand.json").read_text(encoding="utf-8")
        )

    def test_regeneration_keeps_the_builtin_wand_shape_parent(self):
        self.assertEqual("thaumcraft:item/wand_shape", self.original["parent"])
        self.assertEqual(
            self.original["parent"], self.regenerated["parent"],
            "the completed wand renderer must remain reachable through its shape parent",
        )

    def test_regeneration_keeps_every_native_part_and_focus_selector_in_order(self):
        native_properties = {
            "thaumcraft:wand_rod", "thaumcraft:wand_cap", "thaumcraft:wand_focus",
        }

        def native_overrides(model):
            return [entry for entry in model.get("overrides", [])
                    if set(entry.get("predicate", {})) == native_properties]

        original = native_overrides(self.original)
        self.assertTrue(original, "the read-only fixture must contain native combinations")
        self.assertEqual(
            original, native_overrides(self.regenerated),
            "rod/cap/focus combinations and their last-match order must survive regeneration",
        )

    def test_regeneration_keeps_the_addon_part_selector(self):
        def addon_overrides(model):
            return [entry for entry in model.get("overrides", [])
                    if entry.get("predicate", {}).get("thaumcraft:wand_addon") == 1]

        original = addon_overrides(self.original)
        self.assertEqual(1, len(original), "the read-only fixture must route addon parts")
        self.assertEqual(
            original, addon_overrides(self.regenerated),
            "addon wands must keep their builtin renderer selector",
        )

    def test_regeneration_keeps_the_wand_material_texture_slots(self):
        self.assertEqual({"rod", "cap"}, set(self.original["textures"]))
        self.assertEqual(
            self.original["textures"], self.regenerated["textures"],
            "the wand shape consumes material sheets rather than flat item layers",
        )

    def test_an_ordinary_foundation_item_keeps_its_generated_sprite(self):
        alumentum = json.loads(
            (self.assets / "models/item/alumentum.json").read_text(encoding="utf-8")
        )
        self.assertEqual({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "thaumcraft:items/alumentum"},
        }, alumentum)

    def test_foundation_model_generation_keeps_its_silent_output_contract(self):
        self.assertEqual("", self.generation_output)

    def test_wand_generator_cli_keeps_its_count_line_and_explicit_destination(self):
        with tempfile.TemporaryDirectory(prefix="tc4-wand-cli-review-") as temporary:
            models = Path(temporary) / "new/models/item"
            output = io.StringIO()
            with patch.object(gen_wand_models, "MODELS", models), redirect_stdout(output):
                gen_wand_models.main()
            self.assertEqual(f"{len(self.original['overrides'])} overrides\n", output.getvalue())
            generated = json.loads((models / "wand.json").read_text(encoding="utf-8"))
            self.assertEqual(self.original, generated)


if __name__ == "__main__":
    unittest.main()
