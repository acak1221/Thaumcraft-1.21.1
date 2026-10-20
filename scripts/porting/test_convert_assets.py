"""Meaningful file-level checks for the modern foundation asset conversion."""
from __future__ import annotations

import importlib.util
import json
import re
import struct
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("convert_assets", Path(__file__).with_name("convert_assets.py"))
converter = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(converter)


class FoundationAssetsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = json.loads(converter.MANIFEST.read_text(encoding="utf-8"))

    def test_every_generated_texture_parent_blockstate_and_sound_resolves(self) -> None:
        checks = converter.validate_resources()
        self.assertEqual(0, checks["missing_references"])
        self.assertGreater(checks["checked_model_texture_refs"], 36)
        self.assertEqual(checks["checked_model_texture_refs"], checks["checked_atlas_sprite_refs"])
        self.assertEqual(111, checks["checked_sound_refs"])

    def test_reused_asset_bytes_match_source_and_manifest(self) -> None:
        for entry in self.manifest["copied_files"]:
            source = converter.ROOT / entry["source"]
            target = converter.ROOT / entry["target"]
            if entry.get("transformation"):
                self.assertEqual(entry["source_sha256"], converter.sha256(converter.canonical_file_bytes(source)))
                if entry["transformation"] == "newline_only_lf":
                    self.assertEqual(converter.canonical_file_bytes(source), converter.canonical_file_bytes(target))
            else:
                self.assertEqual(source.read_bytes(), target.read_bytes(), entry["target"])
            self.assertEqual(entry["sha256"], converter.sha256(converter.canonical_file_bytes(target)), entry["target"])

    def test_language_conversion_preserves_every_legacy_value_and_markup(self) -> None:
        for locale in ("en_us", "ru_ru"):
            legacy, _ = converter.read_lang(converter.SOURCE / "lang" / f"{locale}.lang")
            modern = json.loads((converter.ASSETS / "lang" / f"{locale}.json").read_text(encoding="utf-8"))
            for key, value in legacy.items():
                self.assertEqual(value, modern[key], f"{locale}: {key}")
            self.assertTrue(any("<BR>" in value for value in modern.values()))

    def test_foundation_ids_have_models_and_both_language_names(self) -> None:
        mappings = self.manifest["identity_mappings"]
        self.assertEqual(44, len(mappings))
        self.assertEqual(44, len({entry["modern_id"] for entry in mappings}))
        for entry in mappings:
            item_id = entry["modern_id"].split(":", 1)[1]
            self.assertTrue((converter.ASSETS / "models/item" / f"{item_id}.json").is_file(), item_id)
            prefix = "block" if entry["registry"] == "block_and_item" else "item"
            for locale in ("en_us", "ru_ru"):
                lang = json.loads((converter.ASSETS / "lang" / f"{locale}.json").read_text(encoding="utf-8"))
                self.assertIn(f"{prefix}.thaumcraft.{item_id}", lang)

    def test_explicit_metadata_coverage_and_original_tints(self) -> None:
        mappings = self.manifest["identity_mappings"]
        self.assertEqual(set(range(19)), {entry["legacy_metadata"] for entry in mappings
                                        if entry["legacy_id"] == "thaumcraft:itemresource"})
        self.assertEqual(set(range(7)), {entry["legacy_metadata"] for entry in mappings
                                       if entry["legacy_id"] == "thaumcraft:itemshard"})
        tint_map = {entry["modern_id"]: entry.get("tint_rgb") for entry in mappings}
        self.assertEqual("#0090FF", tint_map["thaumcraft:water_shard"])
        self.assertEqual("#555577", tint_map["thaumcraft:crystal_entropy"])

    def test_legal_lowercase_paths_and_no_inherited_legacy_shader_json(self) -> None:
        for path in converter.ASSETS.rglob("*"):
            if path.is_file():
                relative = path.relative_to(converter.ASSETS).as_posix()
                self.assertRegex(relative, r"^[a-z0-9/._-]+$")
        # Explicit native adapters only. Warp shaders have a separate deterministic
        # GLSL150 conversion; none are bulk-copied into Minecraft's namespace.
        self.assertEqual({"core/node_stabilizer_bubble.json", "core/node_stabilizer_bubble.fsh", "core/monster_lit_particle.json", "core/monster_lit_particle.fsh",
                          "post/warp_blurred.json", "post/warp_death_gaze.json", "post/warp_hunger.json", "post/warp_sun_scorned.json",
                          "program/bloom_color.json", "program/bloom_color.fsh", "program/bloom2.json", "program/bloom2.fsh",
                          "program/bloom_vertex_base.vsh", "program/color_convolve2.json", "program/color_convolve2.fsh"},
                         {path.relative_to(converter.ASSETS / "shaders").as_posix()
                          for path in (converter.ASSETS / "shaders").rglob("*") if path.is_file()})
        self.assertFalse((converter.RESOURCE_ROOT / "assets/minecraft/shaders").exists())
        pack = json.loads((converter.RESOURCE_ROOT / "pack.mcmeta").read_text(encoding="utf-8"))
        self.assertEqual(34, pack["pack"]["pack_format"])
        self.assertEqual({"min_inclusive": 34, "max_inclusive": 48}, pack["pack"]["supported_formats"])

    def test_monster_lit_particle_keeps_native_lightmap_and_low_alpha_contract(self) -> None:
        shader_root = converter.ASSETS / "shaders/core"
        shader = json.loads((shader_root / "monster_lit_particle.json").read_text())
        self.assertEqual("minecraft:particle", shader["vertex"])
        self.assertEqual("thaumcraft:monster_lit_particle", shader["fragment"])
        self.assertEqual({"Sampler0", "Sampler2"}, {sampler["name"] for sampler in shader["samplers"]})
        self.assertTrue({"ModelViewMat", "ProjMat", "ColorModulator", "FogStart", "FogEnd", "FogColor", "FogShape"}
                        <= {uniform["name"] for uniform in shader["uniforms"]})
        fragment = (shader_root / "monster_lit_particle.fsh").read_text()
        self.assertTrue(fragment.startswith("#version 150"))
        self.assertIn("color.a <= 0.003921569", fragment)
        self.assertIn("linear_fog(color, vertexDistance", fragment)
        self.assertFalse((shader_root / "monster_lit_particle.vsh").exists())

    def test_warp_post_programs_are_reproducible_glsl150_conversions_with_resolved_links(self) -> None:
        spec = importlib.util.spec_from_file_location("warp_shader_converter", Path(__file__).with_name("convert_warp_shaders.py"))
        warp = importlib.util.module_from_spec(spec)
        assert spec.loader is not None
        spec.loader.exec_module(warp)
        shader_root = converter.ASSETS / "shaders"
        with tempfile.TemporaryDirectory() as tmp, patch.object(warp, "TARGET", Path(tmp)):
            warp.main()
            for produced in Path(tmp).rglob("*"):
                if produced.is_file():
                    native = shader_root / produced.relative_to(tmp)
                    self.assertEqual(produced.read_bytes(), native.read_bytes(), f"stale converted shader: {native}")
        for stage in (shader_root / "program").glob("*sh"):
            text = stage.read_text()
            self.assertTrue(text.startswith("#version 150"), stage)
            self.assertNotRegex(text, r"\b(attribute|varying|gl_FragColor|texture2D)\b")
            if stage.suffix == ".fsh": self.assertIn("out vec4 fragColor;", text)
        bloom = (shader_root / "program/bloom_color.fsh").read_text()
        self.assertLess(bloom.index("void main()"), bloom.index("vec4 averageColor="), "uniform texture reads belong inside main in core GLSL")
        builtins = {"minecraft:box_blur", "minecraft:blit", "minecraft:sobel", "minecraft:color_convolve"}
        for post in (shader_root / "post").glob("warp_*.json"):
            data = json.loads(post.read_text()); targets = {"minecraft:main", *data["targets"]}
            for step in data["passes"]:
                self.assertIn(step["intarget"], targets); self.assertIn(step["outtarget"], targets)
                if step["name"] in builtins: continue
                self.assertRegex(step["name"], r"^thaumcraft:[a-z0-9_]+$")
                program = json.loads((shader_root / "program" / (step["name"].split(":")[1] + ".json")).read_text())
                for kind, extension in (("vertex", ".vsh"), ("fragment", ".fsh")):
                    namespace, name = program[kind].split(":")
                    self.assertIn(namespace, {"minecraft", "thaumcraft"})
                    if namespace == "thaumcraft": self.assertTrue((shader_root / "program" / (name + extension)).is_file(), program[kind])

    def test_warp_effect_icons_use_original_gui_y198_origin_and_visible_pixels(self) -> None:
        from PIL import Image
        cells = {"warp_ward": (3, 2), "death_gaze": (4, 2), "blurred_vision": (5, 2),
                 "sun_scorned": (6, 2), "thaumarhia": (7, 2), "unnatural_hunger": (7, 1)}
        with Image.open(converter.ROOT / "thaumcraft_src/assets/thaumcraft/textures/misc/potions.png") as source:
            source = source.convert("RGBA")
            for name, (x, y) in cells.items():
                with Image.open(converter.ASSETS / f"textures/mob_effect/{name}.png") as native:
                    native = native.convert("RGBA")
                    self.assertEqual((18, 18), native.size, name)
                    self.assertGreater(sum(native.getchannel("A").histogram()[1:]), 30, f"invisible effect icon: {name}")
                    expected = source.crop((18*x, 198+18*y, 18*(x+1), 198+18*(y+1)))
                    self.assertEqual(expected.tobytes(), native.tobytes(), f"wrong retained atlas cell: {name}")

    def test_visual_gaps_are_retained_instead_of_claiming_parity(self) -> None:
        self.assertFalse(self.manifest["visual_parity_validated"])
        gaps = {entry["id"] for entry in self.manifest["gaps"]}
        self.assertTrue({"scanner_mesh", "aura_node_renderer", "research_gui", "item_tint_registration"} <= gaps)

    def test_phials_keep_original_layers_and_dynamic_tint_contract(self) -> None:
        for native_id, old_model in (("phial_empty", "itemessence_empty"), ("phial_filled", "itemessence")):
            legacy = json.loads((converter.SOURCE / "models/item" / f"{old_model}.json").read_text(encoding="utf-8"))
            modern = json.loads((converter.ASSETS / "models/item" / f"{native_id}.json").read_text(encoding="utf-8"))
            self.assertEqual(legacy["textures"], modern["textures"])
        entries = {entry["modern_id"]: entry for entry in self.manifest["identity_mappings"]}
        empty = entries["thaumcraft:phial_empty"]
        filled = entries["thaumcraft:phial_filled"]
        self.assertEqual(0, empty["legacy_metadata"])
        self.assertEqual(1, filled["legacy_metadata"])
        self.assertEqual([0], filled["untinted_layers"])
        self.assertEqual(1, filled["tint_index"])
        self.assertEqual(8, filled["fixed_essentia_amount"])
        self.assertEqual("#FFFFFF", filled["default_tint_rgb"])
        self.assertIn("Aspect.getColor()", filled["tint_source"])

    def test_void_jar_shell_keeps_geometry_and_retained_original_texture_bytes(self) -> None:
        normal = json.loads((converter.ASSETS / "models/block/jar.json").read_text())
        void = json.loads((converter.ASSETS / "models/block/jar_void.json").read_text())
        self.assertEqual(normal["elements"], void["elements"])
        self.assertEqual("thaumcraft:blocks/jar_side_void", void["textures"]["side"])
        self.assertEqual("thaumcraft:blocks/jar_top_void", void["textures"]["top"])
        self.assertEqual(normal["textures"]["bottom"], void["textures"]["bottom"])
        for name in ("jar_side_void", "jar_top_void", "jar_bottom"):
            relative = f"textures/blocks/{name}.png"
            self.assertEqual((converter.SOURCE / relative).read_bytes(), (converter.ASSETS / relative).read_bytes())

    def test_jar_items_declare_component_renderer_and_native_display(self) -> None:
        for name in ("jar", "jar_void"):
            item = json.loads((converter.ASSETS / f"models/item/{name}.json").read_text())
            self.assertEqual("minecraft:builtin/entity", item["parent"])
            self.assertEqual("side", item["gui_light"])
            self.assertEqual([30, 225, 0], item["display"]["gui"]["rotation"])
            self.assertEqual([.625] * 3, item["display"]["gui"]["scale"])
            self.assertEqual([.4] * 3, item["display"]["firstperson_righthand"]["scale"])

    def test_valve_and_resonator_use_original_inputs_and_one_shared_research_gate(self) -> None:
        recipes = converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane"
        valve = json.loads((recipes / "essentia_valve.json").read_text())
        self.assertEqual("thaumcraft:arcane_shapeless", valve["type"])
        self.assertNotIn("pattern", valve)
        self.assertEqual([{"item": "thaumcraft:essentia_tube"}, {"item": "minecraft:lever"}], valve["ingredients"])
        self.assertEqual({"id": "thaumcraft:essentia_valve", "count": 1}, valve["result"])
        self.assertEqual({"aqua": 5, "ordo": 5}, valve["vis"])
        resonator = json.loads((recipes / "resonator.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", resonator["type"])
        self.assertEqual(["I I", "INI", " S "], resonator["pattern"])
        self.assertEqual(4, sum(row.count("I") for row in resonator["pattern"]))
        self.assertEqual({"item": "minecraft:quartz"}, resonator["key"]["N"])
        self.assertEqual({"tag": "c:rods/wooden"}, resonator["key"]["S"])
        self.assertEqual({"id": "thaumcraft:resonator", "count": 1}, resonator["result"])
        self.assertEqual({"aqua": 5, "aer": 5}, resonator["vis"])
        self.assertEqual("TUBES", valve["research"])
        self.assertEqual(valve["research"], resonator["research"])

    def test_valve_and_resonator_models_use_retained_original_art_and_native_item_hook(self) -> None:
        valve = json.loads((converter.ASSETS / "models/item/essentia_valve.json").read_text())
        self.assertEqual("minecraft:builtin/entity", valve["parent"])
        self.assertEqual([30, 225, 0], valve["display"]["gui"]["rotation"])
        resonator = json.loads((converter.ASSETS / "models/item/resonator.json").read_text())
        self.assertEqual("minecraft:item/generated", resonator["parent"])
        self.assertEqual("thaumcraft:items/resonator", resonator["textures"]["layer0"])
        for relative in ("textures/blocks/pipe_valve.png", "textures/models/valve.png", "textures/items/resonator.png"):
            self.assertEqual((converter.SOURCE / relative).read_bytes(), (converter.ASSETS / relative).read_bytes())
        sprites = converter.block_atlas_sprites()
        self.assertIn("thaumcraft:blocks/pipe_valve", sprites)
        for locale in ("en_us", "ru_ru"):
            lang = json.loads((converter.ASSETS / f"lang/{locale}.json").read_text())
            self.assertEqual(lang["tile.thaumcraft.tube.1.name"], lang["block.thaumcraft.essentia_valve"])
            self.assertEqual(lang["item.thaumcraft.resonator.name"], lang["item.thaumcraft.resonator"])

    def test_later_tube_upgrades_use_real_shapeless_filter_gate_and_original_costs(self) -> None:
        recipes = converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane"
        expected = {
            "essentia_filter": ({"item": "thaumcraft:vis_filter"}, {"aqua": 5, "ordo": 16}),
            "essentia_restrict": ({"tag": "thaumcraft:original_stone"}, {"aqua": 5, "terra": 16}),
            "essentia_oneway": ([{"tag": "thaumcraft:blue_dyes"}, {"tag": "c:dyes/blue"}], {"aqua": 5, "ordo": 8, "perditio": 8}),
        }
        for name, (second, cost) in expected.items():
            recipe = json.loads((recipes / f"{name}.json").read_text())
            self.assertEqual("thaumcraft:arcane_shapeless", recipe["type"])
            self.assertNotIn("pattern", recipe)
            self.assertEqual("TUBEFILTER", recipe["research"])
            self.assertEqual([{"item": "thaumcraft:essentia_tube"}, second], recipe["ingredients"])
            self.assertEqual({"id": f"thaumcraft:{name}", "count": 1}, recipe["result"])
            self.assertEqual(cost, recipe["vis"])
        tags = converter.RESOURCE_ROOT / "data/thaumcraft/tags/item"
        self.assertEqual(["minecraft:stone"], json.loads((tags / "original_stone.json").read_text())["values"])
        self.assertEqual({"minecraft:lapis_lazuli", "minecraft:blue_dye"}, set(json.loads((tags / "blue_dyes.json").read_text())["values"]))

    def test_later_tube_models_and_names_reuse_original_distinct_variant_art(self) -> None:
        sprites = converter.block_atlas_sprites()
        for name, metadata, particle in (("essentia_filter", 3, "pipe_filter"),
                                         ("essentia_restrict", 5, "pipe_restrict"),
                                         ("essentia_oneway", 6, "pipe_1")):
            item = json.loads((converter.ASSETS / f"models/item/{name}.json").read_text())
            self.assertEqual("minecraft:builtin/entity", item["parent"])
            self.assertEqual("side", item["gui_light"])
            self.assertEqual([30, 225, 0], item["display"]["gui"]["rotation"])
            self.assertEqual([.625] * 3, item["display"]["gui"]["scale"])
            block = json.loads((converter.ASSETS / f"models/block/{name}.json").read_text())
            self.assertEqual([], block["elements"])
            self.assertEqual(f"thaumcraft:blocks/{particle}", block["textures"]["particle"])
            for locale in ("en_us", "ru_ru"):
                lang = json.loads((converter.ASSETS / f"lang/{locale}.json").read_text())
                self.assertEqual(lang[f"tile.thaumcraft.tube.{metadata}.name"], lang[f"block.thaumcraft.{name}"])
        for name in ("pipe_filter", "pipe_filter_core", "pipe_restrict", "pipe_oneway"):
            relative = f"textures/blocks/{name}.png"
            self.assertEqual((converter.SOURCE / relative).read_bytes(), (converter.ASSETS / relative).read_bytes())
            self.assertIn(f"thaumcraft:blocks/{name}", sprites)

    def test_later_tube_loot_is_one_plain_own_item_not_copied_buffer_or_filter(self) -> None:
        for name in ("essentia_filter", "essentia_restrict", "essentia_oneway"):
            loot = json.loads((converter.RESOURCE_ROOT / f"data/thaumcraft/loot_table/blocks/{name}.json").read_text())
            self.assertEqual("minecraft:block", loot["type"])
            self.assertEqual([{"rolls": 1,
                               "entries": [{"type": "minecraft:item", "name": f"thaumcraft:{name}"}],
                               "conditions": [{"condition": "minecraft:survives_explosion"}]}], loot["pools"])

    def test_buffer_uses_original_shaped_phials_and_upgraded_tubes_under_primary_gate(self) -> None:
        recipe = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane/essentia_buffer.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", recipe["type"])
        self.assertEqual("CENTRIFUGE", recipe["research"])
        self.assertEqual(["PVP", "T T", "PRP"], recipe["pattern"])
        self.assertEqual({"P": {"item": "thaumcraft:phial_empty"},
                          "V": {"item": "thaumcraft:essentia_valve"},
                          "T": {"item": "thaumcraft:essentia_tube"},
                          "R": {"item": "thaumcraft:essentia_restrict"}}, recipe["key"])
        self.assertEqual({"id": "thaumcraft:essentia_buffer", "count": 1}, recipe["result"])
        self.assertEqual({"aqua": 5, "ordo": 5}, recipe["vis"])

    def test_buffer_models_names_and_plain_drop_retain_own_original_identity(self) -> None:
        name = "essentia_buffer"
        item = json.loads((converter.ASSETS / f"models/item/{name}.json").read_text())
        self.assertEqual("minecraft:builtin/entity", item["parent"])
        self.assertEqual("side", item["gui_light"])
        self.assertEqual([30, 225, 0], item["display"]["gui"]["rotation"])
        block = json.loads((converter.ASSETS / f"models/block/{name}.json").read_text())
        self.assertEqual([], block["elements"])
        self.assertEqual("thaumcraft:blocks/pipe_buffer", block["textures"]["particle"])
        self.assertIn("thaumcraft:blocks/pipe_buffer", converter.block_atlas_sprites())
        relative = "textures/blocks/pipe_buffer.png"
        self.assertEqual((converter.SOURCE / relative).read_bytes(), (converter.ASSETS / relative).read_bytes())
        for locale in ("en_us", "ru_ru"):
            lang = json.loads((converter.ASSETS / f"lang/{locale}.json").read_text())
            self.assertEqual(lang["tile.thaumcraft.tube.4.name"], lang[f"block.thaumcraft.{name}"])
        loot = json.loads((converter.RESOURCE_ROOT / f"data/thaumcraft/loot_table/blocks/{name}.json").read_text())
        self.assertEqual([{"rolls": 1,
                           "entries": [{"type": "minecraft:item", "name": f"thaumcraft:{name}"}],
                           "conditions": [{"condition": "minecraft:survives_explosion"}]}], loot["pools"])

    def test_construct_and_centrifuge_keep_distinct_original_shaped_dependencies(self) -> None:
        recipes = converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane"
        construct = json.loads((recipes / "alchemical_construct.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", construct["type"])
        self.assertEqual("DISTILESSENTIA", construct["research"])
        self.assertEqual(["VTF", "TWT", "FTV"], construct["pattern"])
        self.assertEqual(4, "".join(construct["pattern"]).count("T"))
        self.assertEqual({"V": {"item": "thaumcraft:essentia_valve"},
                          "T": {"item": "thaumcraft:essentia_tube"},
                          "F": {"item": "thaumcraft:vis_filter"},
                          "W": {"item": "thaumcraft:greatwood_planks"}}, construct["key"])
        self.assertEqual({"id": "thaumcraft:alchemical_construct", "count": 1}, construct["result"])
        self.assertEqual({"aqua": 5, "ordo": 5}, construct["vis"])
        centrifuge = json.loads((recipes / "centrifuge.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", centrifuge["type"])
        self.assertEqual("CENTRIFUGE", centrifuge["research"])
        self.assertEqual([" T ", "ACP", " T "], centrifuge["pattern"])
        self.assertEqual({"T": {"item": "thaumcraft:essentia_tube"},
                          "A": {"item": "thaumcraft:alembic"},
                          "C": {"item": "thaumcraft:alchemical_construct"},
                          "P": {"item": "minecraft:piston"}}, centrifuge["key"])
        self.assertEqual({"id": "thaumcraft:centrifuge", "count": 1}, centrifuge["result"])
        self.assertEqual({"aqua": 5, "ordo": 5, "perditio": 5}, centrifuge["vis"])

    def test_construct_cube_and_centrifuge_native_model_retain_separate_original_art(self) -> None:
        construct = json.loads((converter.ASSETS / "models/block/alchemical_construct.json").read_text())
        self.assertEqual("minecraft:block/cube_all", construct["parent"])
        self.assertEqual({"all": "thaumcraft:blocks/alchemyblock"}, construct["textures"])
        self.assertEqual({"parent": "thaumcraft:block/alchemical_construct"}, json.loads((converter.ASSETS / "models/item/alchemical_construct.json").read_text()))
        centrifuge = json.loads((converter.ASSETS / "models/block/centrifuge.json").read_text())
        self.assertEqual([], centrifuge["elements"])
        self.assertEqual("thaumcraft:blocks/pipe_1", centrifuge["textures"]["particle"])
        item = json.loads((converter.ASSETS / "models/item/centrifuge.json").read_text())
        self.assertEqual("minecraft:builtin/entity", item["parent"])
        self.assertEqual("side", item["gui_light"])
        for relative, dimensions in (("blocks/alchemyblock", (32, 32)), ("models/centrifuge", (64, 32))):
            texture = f"textures/{relative}.png"
            data = (converter.ASSETS / texture).read_bytes()
            self.assertEqual((converter.SOURCE / texture).read_bytes(), data)
            self.assertEqual(dimensions, struct.unpack(">II", data[16:24]))
        self.assertIn("thaumcraft:blocks/alchemyblock", converter.block_atlas_sprites())
        for locale in ("en_us", "ru_ru"):
            lang = json.loads((converter.ASSETS / f"lang/{locale}.json").read_text())
            self.assertEqual(lang["tile.thaumcraft.metal_device.9.name"], lang["block.thaumcraft.alchemical_construct"])
            self.assertEqual(lang["tile.thaumcraft.tube.2.name"], lang["block.thaumcraft.centrifuge"])

    def test_construct_and_centrifuge_drop_only_one_plain_own_item(self) -> None:
        for name in ("alchemical_construct", "centrifuge"):
            loot = json.loads((converter.RESOURCE_ROOT / f"data/thaumcraft/loot_table/blocks/{name}.json").read_text())
            self.assertEqual("minecraft:block", loot["type"])
            self.assertEqual([{"rolls": 1,
                               "entries": [{"type": "minecraft:item", "name": f"thaumcraft:{name}"}],
                               "conditions": [{"condition": "minecraft:survives_explosion"}]}], loot["pools"])

    def test_crystallizer_recipe_uses_dispenser_and_balanced_shards_not_quartz_or_diamond(self) -> None:
        recipe = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane/essentia_crystallizer.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", recipe["type"])
        self.assertEqual("ESSENTIACRYSTAL", recipe["research"])
        self.assertEqual(["IDI", "QCQ", "WTW"], recipe["pattern"])
        self.assertEqual({"I": {"tag": "thaumcraft:iron_ingots"},
                          "D": {"item": "minecraft:dispenser"},
                          "Q": {"item": "thaumcraft:balance_shard"},
                          "C": {"item": "thaumcraft:alchemical_construct"},
                          "W": {"tag": "minecraft:planks"},
                          "T": {"item": "thaumcraft:essentia_tube"}}, recipe["key"])
        self.assertEqual({"id": "thaumcraft:essentia_crystallizer", "count": 1}, recipe["result"])
        self.assertEqual({"aqua": 5, "terra": 15, "ordo": 5}, recipe["vis"])
        iron = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/tags/item/iron_ingots.json").read_text())
        self.assertEqual({"replace": False, "values": ["minecraft:iron_ingot"]}, iron)
        planks = json.loads((converter.RESOURCE_ROOT / "data/minecraft/tags/item/planks.json").read_text())
        self.assertIn("thaumcraft:greatwood_planks", planks["values"])
        self.assertIn("thaumcraft:silverwood_planks", planks["values"])

    def test_crystallizer_and_crystal_item_keep_distinct_original_models_and_art(self) -> None:
        block = json.loads((converter.ASSETS / "models/block/essentia_crystallizer.json").read_text())
        self.assertEqual([], block["elements"])
        self.assertEqual("thaumcraft:blocks/pipe_1", block["textures"]["particle"])
        item = json.loads((converter.ASSETS / "models/item/essentia_crystallizer.json").read_text())
        self.assertEqual("minecraft:builtin/entity", item["parent"])
        self.assertEqual("side", item["gui_light"])
        crystal = json.loads((converter.ASSETS / "models/item/crystal_essence.json").read_text())
        self.assertEqual({"parent": "minecraft:item/generated", "textures": {"layer0": "thaumcraft:items/crystalessence"}}, crystal)
        for relative, dimensions in (("models/crystalizer", (64, 64)), ("models/vis_relay", (32, 32)), ("items/crystalessence", (16, 16))):
            texture = f"textures/{relative}.png"
            data = (converter.ASSETS / texture).read_bytes()
            self.assertEqual((converter.SOURCE / texture).read_bytes(), data)
            self.assertEqual(dimensions, struct.unpack(">II", data[16:24]))
        for name in ("crystalizer", "vis_relay"):
            relative = f"textures/models/{name}.obj"
            # Established conversion policy canonicalizes OBJ text newlines;
            # binary PNG checks above remain byte-for-byte, without resizing.
            self.assertEqual(converter.canonical_file_bytes(converter.SOURCE / relative),
                             (converter.ASSETS / relative).read_bytes())

    def test_crystallizer_plain_drop_and_native_language_keys_do_not_alias_shards(self) -> None:
        loot = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/loot_table/blocks/essentia_crystallizer.json").read_text())
        self.assertEqual("minecraft:block", loot["type"])
        self.assertEqual([{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "thaumcraft:essentia_crystallizer"}],
                           "conditions": [{"condition": "minecraft:survives_explosion"}]}], loot["pools"])
        for locale, item_name in (("en_us", "Crystallized Essence"), ("ru_ru", "Кристаллизированная эссенция")):
            lang = json.loads((converter.ASSETS / f"lang/{locale}.json").read_text())
            self.assertEqual(lang["tile.thaumcraft.tube.7.name"], lang["block.thaumcraft.essentia_crystallizer"])
            self.assertEqual(item_name, lang["item.thaumcraft.crystal_essence"])

    def test_node_stabilizer_keeps_literal_quartz_blocks_stone_bricks_and_nitor_item(self) -> None:
        recipe = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane/node_stabilizer.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", recipe["type"])
        self.assertEqual("NODESTABILIZER", recipe["research"])
        self.assertEqual([" G ", "QPQ", "SNS"], recipe["pattern"])
        self.assertEqual({"G": {"item": "minecraft:gold_ingot"},
                          "Q": {"item": "minecraft:quartz_block"},
                          "P": {"item": "minecraft:piston"},
                          "S": {"item": "thaumcraft:arcane_stone_bricks"},
                          "N": {"item": "thaumcraft:nitor"}}, recipe["key"])
        self.assertEqual({"id": "thaumcraft:node_stabilizer", "count": 1}, recipe["result"])
        self.assertEqual({"aqua": 32, "terra": 32, "ordo": 32}, recipe["vis"])

    def test_node_stabilizer_retains_its_bounded_obj_groups_and_original_art(self) -> None:
        relative = "textures/models/node_stabilizer.obj"
        data = (converter.ASSETS / relative).read_bytes()
        self.assertEqual(converter.canonical_file_bytes(converter.SOURCE / relative), data)
        group = ""
        faces = {}
        for line in data.decode().splitlines():
            fields = line.split()
            if not fields:
                continue
            if fields[0] == "g":
                group = " ".join(fields[1:])
            elif fields[0] == "f":
                faces[group] = faces.get(group, 0) + 1
        self.assertEqual({"lock": 28, "piston": 12}, faces)
        for relative, dimensions in (("textures/models/node_stabilizer.png", (32, 32)),
                                     ("textures/models/node_stabilizer_over.png", (32, 32)),
                                     ("textures/misc/node_bubble.png", (256, 256))):
            data = (converter.ASSETS / relative).read_bytes()
            self.assertEqual((converter.SOURCE / relative).read_bytes(), data)
            self.assertEqual(dimensions, struct.unpack(">II", data[16:24]))

    def test_node_stabilizer_has_its_own_native_item_drop_and_scope_notice(self) -> None:
        item = json.loads((converter.ASSETS / "models/item/node_stabilizer.json").read_text())
        self.assertEqual("minecraft:builtin/entity", item["parent"])
        self.assertEqual("side", item["gui_light"])
        block = json.loads((converter.ASSETS / "models/block/node_stabilizer.json").read_text())
        self.assertEqual([], block["elements"])
        self.assertEqual("thaumcraft:blocks/al_furnace_side", block["textures"]["particle"])
        loot = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/loot_table/blocks/node_stabilizer.json").read_text())
        self.assertEqual("minecraft:block", loot["type"])
        self.assertEqual([{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "thaumcraft:node_stabilizer"}],
                           "conditions": [{"condition": "minecraft:survives_explosion"}]}], loot["pools"])
        pickaxe = json.loads((converter.RESOURCE_ROOT / "data/minecraft/tags/block/mineable/pickaxe.json").read_text())
        self.assertIn("thaumcraft:node_stabilizer", pickaxe["values"])
        for locale in ("en_us", "ru_ru"):
            lang = json.loads((converter.ASSETS / f"lang/{locale}.json").read_text())
            self.assertEqual(lang["tile.thaumcraft.stone_device.9.name"], lang["block.thaumcraft.node_stabilizer"])
            notice = lang["tooltip.thaumcraft.node_stabilizer.scope"]
            self.assertEqual({"en_us": "Ordinary or energized nodes; special-node behaviours and the advanced stabilizer are not yet implemented.",
                              "ru_ru": "Обычные и возбуждённые узлы; особые типы и улучшенный стабилизатор ещё не перенесены."}[locale], notice)

    def test_stabilizer_bubble_uses_the_original_low_alpha_threshold_without_copying_a_vertex_shader(self) -> None:
        shader = json.loads((converter.ASSETS / "shaders/core/node_stabilizer_bubble.json").read_text())
        self.assertEqual("minecraft:position_tex_color", shader["vertex"])
        self.assertEqual("thaumcraft:node_stabilizer_bubble", shader["fragment"])
        self.assertEqual([{"name": "Sampler0"}], shader["samplers"])
        self.assertEqual(["ModelViewMat", "ProjMat", "ColorModulator"], [row["name"] for row in shader["uniforms"]])
        self.assertEqual([16, 16, 4], [row["count"] for row in shader["uniforms"]])
        fragment = (converter.ASSETS / "shaders/core/node_stabilizer_bubble.fsh").read_text()
        self.assertIn("if (color.a <= 0.003921569) discard;", fragment)
        self.assertIn("texture(Sampler0, texCoord0) * vertexColor", fragment)
        self.assertIn("fragColor = color * ColorModulator;", fragment)
        self.assertFalse((converter.ASSETS / "shaders/core/node_stabilizer_bubble.vsh").exists())

    def test_vispower_recipes_keep_literal_blocks_balanced_shard_and_two_relay_yield(self) -> None:
        transducer = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane/node_transducer.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", transducer["type"])
        self.assertEqual("VISPOWER", transducer["research"])
        self.assertEqual(["RCR", "ISI", "RAR"], transducer["pattern"])
        self.assertEqual({"R": {"item": "minecraft:redstone_block"}, "C": {"item": "minecraft:comparator"},
                          "I": {"item": "minecraft:iron_ingot"}, "S": {"item": "thaumcraft:node_stabilizer"},
                          "A": {"item": "thaumcraft:nitor"}}, transducer["key"])
        self.assertEqual({"id": "thaumcraft:node_transducer", "count": 1}, transducer["result"])
        self.assertEqual({"ignis": 32, "aer": 32, "perditio": 32}, transducer["vis"])
        relay = json.loads((converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane/vis_relay.json").read_text())
        self.assertEqual("thaumcraft:arcane_shaped", relay["type"])
        self.assertEqual("VISPOWER", relay["research"])
        self.assertEqual([" I ", "ISI", " I "], relay["pattern"])
        self.assertEqual({"I": {"item": "minecraft:iron_ingot"}, "S": {"item": "thaumcraft:balance_shard"}}, relay["key"])
        self.assertEqual({"id": "thaumcraft:vis_relay", "count": 2}, relay["result"])
        self.assertEqual({"ignis": 8, "ordo": 8}, relay["vis"])

    def test_transducer_relay_and_energized_source_retain_exact_original_art_not_placeholder_blocks(self) -> None:
        for relative, dimensions in (("models/node_converter", (32, 32)), ("models/node_converter_over", (32, 32)),
                                     ("models/vis_relay", (32, 32)), ("items/lightningringv", (4096, 256))):
            texture = f"textures/{relative}.png"
            data = (converter.ASSETS / texture).read_bytes()
            self.assertEqual((converter.SOURCE / texture).read_bytes(), data)
            self.assertEqual(dimensions, struct.unpack(">II", data[16:24]))
        groups = {}
        group = ""
        data = (converter.ASSETS / "textures/models/vis_relay.obj").read_bytes()
        self.assertEqual(converter.canonical_file_bytes(converter.SOURCE / "textures/models/vis_relay.obj"), data)
        for line in data.decode().splitlines():
            fields = line.split()
            if not fields:
                continue
            if fields[0] == "g":
                group = " ".join(fields[1:])
            elif fields[0] == "f":
                groups[group] = groups.get(group, 0) + 1
        self.assertEqual({"Crystal": 16, "RingFloat": 32, "RingBase": 32, "Support": 36}, groups)
        # Original item strip stays OUT of the block atlas; native effects bind
        # its whole resource directly, without a fabricated .mcmeta animation.
        self.assertNotIn("thaumcraft:items/lightningringv", converter.block_atlas_sprites())

    def test_power_devices_have_real_native_items_but_no_portable_energized_node(self) -> None:
        for native_id in ("node_transducer", "vis_relay"):
            item = json.loads((converter.ASSETS / f"models/item/{native_id}.json").read_text())
            self.assertEqual("minecraft:builtin/entity", item["parent"])
            loot = json.loads((converter.RESOURCE_ROOT / f"data/thaumcraft/loot_table/blocks/{native_id}.json").read_text())
            self.assertEqual([{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"thaumcraft:{native_id}"}],
                               "conditions": [{"condition": "minecraft:survives_explosion"}]}], loot["pools"])
        self.assertFalse((converter.ASSETS / "models/item/energized_node.json").exists())
        self.assertEqual({"type": "minecraft:block", "pools": []}, json.loads(
            (converter.RESOURCE_ROOT / "data/thaumcraft/loot_table/blocks/energized_node.json").read_text()))
        relay_state = json.loads((converter.ASSETS / "blockstates/vis_relay.json").read_text())
        self.assertEqual({"variants": {"": {"model": "thaumcraft:block/vis_relay"}}}, relay_state)
        for locale in ("en_us", "ru_ru"):
            lang = json.loads((converter.ASSETS / f"lang/{locale}.json").read_text())
            for native_id, legacy in (("node_transducer", "stone_device.11"), ("energized_node", "airy.5"), ("vis_relay", "metal_device.14")):
                self.assertEqual(lang[f"tile.thaumcraft.{legacy}.name"], lang[f"block.thaumcraft.{native_id}"])
            self.assertIn("VisNet", lang["tooltip.thaumcraft.crystallizer.visnet"])

    def test_exceptional_ninth_flux_quantum_has_render_coverage_without_changing_normal_capacity(self) -> None:
        for phase in ("gas", "goo"):
            states = json.loads((converter.ASSETS / f"blockstates/flux_{phase}.json").read_text())["variants"]
            # Retained full-height wildcard covers9 actual states. Nine exact
            # false selectors plus that wildcard cover18, NOT18 JSON selectors.
            self.assertEqual({"full_height=true"} | {f"full_height=false,level={level}" for level in range(9)}, set(states))
            rotation = {"x": 180} if phase == "gas" else {}
            # The retained FULL cube has no ceiling-height offset and was never
            # rotated. Only the partial-height gas variants need inversion.
            self.assertEqual({"model": f"thaumcraft:block/flux_{phase}_full"}, states["full_height=true"])
            for level in range(9):
                self.assertEqual({"model": f"thaumcraft:block/flux_{phase}_{level}", **rotation}, states[f"full_height=false,level={level}"])
        goo = json.loads((converter.ASSETS / "models/block/flux_goo_8.json").read_text())
        self.assertEqual("minecraft:block/block", goo["parent"])
        self.assertFalse(goo["ambientocclusion"])
        self.assertEqual({"particle": "#all", "all": "thaumcraft:blocks/fluxgoo"}, goo["textures"])
        element, = goo["elements"]
        self.assertEqual([0, 0, 0], element["from"])
        self.assertEqual([16, 15.75, 16], element["to"])
        for face in ("north", "south", "west", "east"):
            self.assertEqual({"texture": "#all", "uv": [0, 0.125, 8, 8], "cullface": face}, element["faces"][face])
        self.assertEqual({"parent": "thaumcraft:block/flux_goo_8", "textures": {"all": "thaumcraft:blocks/fluxgas"}},
                         json.loads((converter.ASSETS / "models/block/flux_gas_8.json").read_text()))
        for phase in ("gas", "goo"):
            self.assertEqual((converter.SOURCE / f"textures/blocks/flux{phase}.png").read_bytes(),
                             (converter.ASSETS / f"textures/blocks/flux{phase}.png").read_bytes())

    def test_jar_only_regeneration_is_bounded_and_matches_authored_outputs(self) -> None:
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            assets = Path(temporary)
            with patch.object(converter, "ASSETS", assets):
                mappings = converter.create_jar_models()
            self.assertEqual([0, 3], [entry["legacy_metadata"] for entry in mappings])
            generated = [p for p in assets.rglob("*") if p.is_file()]
            self.assertEqual(6, len(generated))
            for path in generated:
                relative = path.relative_to(assets)
                self.assertEqual(path.read_bytes(), (converter.ASSETS / relative).read_bytes(), str(relative))

    def test_language_regeneration_retains_unowned_native_keys(self) -> None:
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            source = Path(temporary) / "source"
            assets = Path(temporary) / "assets"
            (source / "lang").mkdir(parents=True)
            (assets / "lang").mkdir(parents=True)
            for locale in ("en_us", "ru_ru"):
                (source / "lang" / f"{locale}.lang").write_text("legacy.key=original\n", encoding="utf-8")
                (assets / "lang" / f"{locale}.json").write_text(
                    json.dumps({"screen.thaumcraft.example": "native GUI text", "legacy.key": "stale"}), encoding="utf-8")
            with patch.object(converter, "SOURCE", source), patch.object(converter, "ASSETS", assets):
                converter.convert_languages([])
            for locale in ("en_us", "ru_ru"):
                result = json.loads((assets / "lang" / f"{locale}.json").read_text(encoding="utf-8"))
                self.assertEqual("native GUI text", result["screen.thaumcraft.example"])
                self.assertEqual("original", result["legacy.key"])

    def test_file_existence_is_insufficient_without_atlas_inclusion(self) -> None:
        self.assertTrue((converter.ASSETS / "textures/items/shard.png").is_file())
        self.assertTrue((converter.ASSETS / "textures/blocks/jar_side.png").is_file())
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            missing_atlas = Path(temporary) / "missing-atlas.json"
            with patch.object(converter, "BLOCK_ATLAS", missing_atlas):
                with self.assertRaisesRegex(ValueError, "Unstitched atlas texture thaumcraft:items/shard"):
                    converter.validate_resources()

    def test_atlas_directory_prefixes_resolve_original_sprite_names(self) -> None:
        sprites = converter.block_atlas_sprites()
        self.assertEqual(converter.ASSETS / "textures/items/shard.png", sprites["thaumcraft:items/shard"])
        self.assertEqual(converter.ASSETS / "textures/blocks/jar_side.png", sprites["thaumcraft:blocks/jar_side"])
        self.assertNotIn("thaumcraft:items/lightningringv", sprites)
        self.assertTrue((converter.ASSETS / "textures/items/lightningringv.png").is_file())
        # The directory's physical texture files still exist with an incorrect
        # prefix, but the model's original resource identifiers no longer map.
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            wrong_atlas = Path(temporary) / "wrong-prefix.json"
            wrong_atlas.write_text(json.dumps({"sources": [
                {"type": "directory", "source": "items", "prefix": "incorrect/"},
                {"type": "directory", "source": "blocks", "prefix": "blocks/"}
            ]}), encoding="utf-8")
            with patch.object(converter, "BLOCK_ATLAS", wrong_atlas):
                with self.assertRaisesRegex(ValueError, "Unstitched atlas texture thaumcraft:items/shard"):
                    converter.validate_resources()

    def test_table_uv_sheets_are_explicit_static_full_image_sprites(self) -> None:
        sprites = converter.block_atlas_sprites()
        atlas = json.loads(converter.BLOCK_ATLAS.read_text(encoding="utf-8"))
        singles = {source["resource"] for source in atlas["sources"] if source["type"] == "minecraft:single"}
        for relative, dimensions in (("models/table", (64, 32)), ("models/restable", (128, 64))):
            sprite = f"thaumcraft:{relative}"
            png = sprites[sprite]
            self.assertIn(sprite, singles)
            self.assertEqual(dimensions, converter.atlas_frame_size(png))
            self.assertFalse(Path(str(png) + ".mcmeta").exists())
            self.assertEqual((converter.SOURCE / "textures" / f"{relative}.png").read_bytes(), png.read_bytes())
        # Explicitly approving the two model sheets must not admit legacy FX
        # strips such as the depth sprites checked elsewhere in this suite.
        self.assertNotIn("thaumcraft:items/lightningringv", sprites)

    def test_static_table_frame_rejects_accidental_animation_slicing(self) -> None:
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            png = Path(temporary) / "table.png"
            png.write_bytes((converter.ASSETS / "textures/models/table.png").read_bytes())
            Path(str(png) + ".mcmeta").write_text(
                json.dumps({"animation": {"width": 32, "height": 32, "frames": [0]}}), encoding="utf-8")
            sprites = converter.block_atlas_sprites()
            sprites["thaumcraft:models/table"] = png
            with patch.object(converter, "block_atlas_sprites", return_value=sprites):
                with self.assertRaisesRegex(ValueError, "Static model sprite thaumcraft:models/table violates original full-image frame"):
                    converter.validate_resources()

    def test_explicit_atlas_inclusion_does_not_approve_unknown_legacy_strips(self) -> None:
        with patch.object(converter, "STATIC_MODEL_SPRITES", {}):
            with self.assertRaisesRegex(ValueError, "Non-square unanimated atlas texture: thaumcraft:models/table"):
                converter.validate_resources()

    def test_flat_filter_schema_is_rejected_even_when_directory_sources_are_valid(self) -> None:
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            flat_atlas = Path(temporary) / "flat-filter.json"
            flat_atlas.write_text(json.dumps({"sources": [
                {"type": "minecraft:directory", "source": "items", "prefix": "items/"},
                {"type": "minecraft:filter", "namespace": "thaumcraft", "path": "items/lightningringv"}
            ]}), encoding="utf-8")
            with patch.object(converter, "BLOCK_ATLAS", flat_atlas):
                with self.assertRaisesRegex(ValueError, "requires nested pattern"):
                    converter.block_atlas_sprites()
        atlas = json.loads(converter.BLOCK_ATLAS.read_text(encoding="utf-8"))
        filters = [source for source in atlas["sources"] if source["type"] == "minecraft:filter"]
        self.assertTrue(filters)
        for source in filters:
            self.assertIsInstance(source.get("pattern"), dict)
            self.assertNotIn("namespace", source)
            self.assertNotIn("path", source)

    def test_sound_event_ids_are_native_valid_and_trace_original_names(self) -> None:
        source = json.loads((converter.SOURCE / "sounds.json").read_text(encoding="utf-8"))
        target = json.loads((converter.ASSETS / "sounds.json").read_text(encoding="utf-8"))
        self.assertEqual(len(source), len(target))
        self.assertEqual(source["runicShieldEffect"], target["runicshieldeffect"])
        self.assertEqual(source["runicShieldCharge"], target["runicshieldcharge"])
        self.assertNotIn("runicShieldEffect", target)
        for event_key, definition in target.items():
            self.assertRegex(event_key, r"^[a-z0-9/._-]+$")
            for sound in definition.get("sounds", []):
                reference = sound if isinstance(sound, str) else sound["name"]
                self.assertRegex(reference, r"^[a-z0-9._-]+:[a-z0-9/._-]+$")
        mapping = {entry["legacy_id"]: entry["modern_id"] for entry in self.manifest["sound_events"]["event_id_mappings"]}
        self.assertEqual("thaumcraft:runicshieldeffect", mapping["thaumcraft:runicShieldEffect"])

    def test_unused_depth_effect_sprites_do_not_lower_global_mipmap_level(self) -> None:
        sprites = converter.block_atlas_sprites()
        excluded = {entry["sprite"] for entry in self.manifest["block_atlas"]["excluded_legacy_strips"]}
        for effect in ("focus_primal_depth", "focus_pech_depth", "focus_portablehole_depth", "focus_warding_depth"):
            sprite = f"thaumcraft:items/{effect}"
            self.assertNotIn(sprite, sprites)
            self.assertIn(sprite, excluded)
            self.assertEqual((converter.SOURCE / f"textures/items/{effect}.png").read_bytes(),
                             (converter.ASSETS / f"textures/items/{effect}.png").read_bytes())
        # Check the actual PNG dimensions independently of the converter's
        # selection rule: every currently included image supports mip level 4.
        for sprite, png in sprites.items():
            width, height = struct.unpack("!II", png.read_bytes()[16:24])
            self.assertEqual(0, width % 16, sprite)
            self.assertEqual(0, height % 16, sprite)

    def test_verify_rejects_global_mipmap_drop_even_for_unreferenced_sprite(self) -> None:
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            old_atlas = Path(temporary) / "unfiltered-depth.json"
            old_atlas.write_text(json.dumps({"sources": [
                {"type": "minecraft:directory", "source": "items", "prefix": "items/"},
                {"type": "minecraft:directory", "source": "blocks", "prefix": "blocks/"},
                {"type": "minecraft:filter", "pattern": {"namespace": "thaumcraft", "path": "items/lightningringv"}}
            ]}), encoding="utf-8")
            with patch.object(converter, "BLOCK_ATLAS", old_atlas):
                with self.assertRaisesRegex(ValueError, "focus_primal_depth.*lowers global mipmap level"):
                    converter.validate_resources()

    def test_focal_manipulator_and_real_charm_dependency_keep_literal_original_recipes(self) -> None:
        recipes = converter.RESOURCE_ROOT / "data/thaumcraft/recipe/arcane"
        charm = json.loads((recipes / "primal_charm.json").read_text())
        self.assertEqual({
            "type": "thaumcraft:arcane_shaped", "research": "BASICARTIFACE",
            "pattern": ["123", "ISI", "456"],
            "key": {"1": {"item": "thaumcraft:air_shard"}, "2": {"item": "thaumcraft:fire_shard"},
                    "3": {"item": "thaumcraft:water_shard"}, "4": {"item": "thaumcraft:earth_shard"},
                    "5": {"item": "thaumcraft:order_shard"}, "6": {"item": "thaumcraft:entropy_shard"},
                    "I": {"item": "minecraft:gold_ingot"}, "S": {"item": "thaumcraft:balance_shard"}},
            "result": {"id": "thaumcraft:primal_charm", "count": 1},
            "vis": dict.fromkeys(("aer", "terra", "ignis", "aqua", "ordo", "perditio"), 25)
        }, charm)
        machine = json.loads((recipes / "focal_manipulator.json").read_text())
        self.assertEqual({
            "type": "thaumcraft:arcane_shaped", "research": "FOCALMANIPULATION",
            "pattern": ["IQI", "SPS", "GTG"],
            "key": {"I": {"item": "minecraft:iron_ingot"}, "Q": {"item": "thaumcraft:arcane_stone_brick_slab"},
                    "S": {"item": "thaumcraft:arcane_stone"}, "P": {"item": "thaumcraft:primal_charm"},
                    "G": {"item": "minecraft:gold_ingot"}, "T": {"item": "thaumcraft:table"}},
            "result": {"id": "thaumcraft:focal_manipulator", "count": 1},
            "vis": dict.fromkeys(("aer", "terra", "ignis", "aqua", "ordo", "perditio"), 32)
        }, machine)

    def test_focal_table_sheet_and_upgrade_icons_retain_original_full_image_contract(self) -> None:
        sprites = converter.block_atlas_sprites()
        sheet = converter.ASSETS / "textures/models/wandtable.png"
        self.assertEqual(sheet, sprites["thaumcraft:models/wandtable"])
        self.assertEqual((128, 64), converter.STATIC_MODEL_SPRITES["models/wandtable"])
        self.assertEqual((128, 64), converter.atlas_frame_size(sheet))
        for name, dimensions in [("models/wandtable", (128, 64)), ("gui/gui_wandtable", (256, 256))] + [
                ("foci/" + name, (32, 32)) for name in ("frugal", "potency", "treasure", "enlarge")]:
            target = converter.ASSETS / "textures" / (name + ".png")
            self.assertEqual((converter.ORIGINAL / "textures" / (name + ".png")).read_bytes(), target.read_bytes())
            self.assertEqual(dimensions, struct.unpack("!II", target.read_bytes()[16:24]))
        for locale in ("en_us", "ru_ru"):
            language = json.loads((converter.ASSETS / "lang" / (locale + ".json")).read_text())
            self.assertEqual(language["tile.thaumcraft.stone_device.13.name"], language["block.thaumcraft.focal_manipulator"])
            self.assertIn("tc.port.focus.upgrades.partial", language)
            self.assertIn("tc.port.focus.upgrades.invalid", language)

    def test_bore_literal_recipes_retain_primary_gate_cost_order_and_pristine_components(self) -> None:
        # Original4.2.3.5 ConfigRecipes255/375, not a gameplay/runtime proof.
        recipes = converter.RESOURCE_ROOT / "data/thaumcraft/recipe"
        self.assertEqual({
            "type": "thaumcraft:arcane_shaped", "research": "ARCANEBORE",
            "pattern": ["WIW", "IDI", "WIW"],
            "key": {"W": {"item": "thaumcraft:greatwood_planks"}, "I": {"item": "minecraft:iron_ingot"},
                    "D": {"item": "minecraft:dispenser"}},
            "result": {"id": "thaumcraft:arcane_bore_base", "count": 1}, "vis": {"aer": 10, "ordo": 10}
        }, json.loads((recipes / "arcane/arcane_bore_base.json").read_text()))
        top = json.loads((recipes / "infusion/arcane_bore.json").read_text())
        self.assertEqual({
            "type": "thaumcraft:infusion_stack", "research": "ARCANEBORE", "input": {"item": "minecraft:piston"},
            "components": [{"item": item} for item in ["thaumcraft:greatwood_planks", "thaumcraft:greatwood_planks",
                "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:diamond_pickaxe", "minecraft:diamond_shovel",
                "thaumcraft:air_shard", "thaumcraft:earth_shard"]],
            "component_predicates": [{"component": 4, "damage": 0}, {"component": 5, "damage": 0}],
            "aspects": {"potentia": 16, "perfodio": 32, "machina": 32, "vacuos": 16, "motus": 16},
            "instability": 4, "result": {"id": "thaumcraft:arcane_bore", "count": 1}
        }, top)
        self.assertEqual(["potentia", "perfodio", "machina", "vacuos", "motus"], list(top["aspects"]))
        self.assertEqual(112, sum(top["aspects"].values()))

    def test_bore_preserved_sheets_names_and_builtin_mesh_dispatch_resources(self) -> None:
        # Physical PNG sizes differ from logical128x64 ModelPart UV coordinates.
        # Bytes/resources only: this does not inspect rendered client pixels.
        expected = {
            "models/bore": ((256, 128), "d1527e8c995e3e576a37200bc334e77f35f628515a678565d811fb618e7ed71f"),
            "models/jar": ((64, 32), "1db2b38056e136bf978d5f6d2eefe63ae6023a0024a04e88559510d8f5006158"),
            "gui/gui_arcanebore": ((256, 256), "55d92e67bc3583cc18d7c1ee80345ddb51fc5ef0fece41c132e8c90171108de5"),
            "misc/vortex": ((256, 256), "7c94144a1ffdafe72e8feec17a872c4de32da327e1922b3ebafbbbf9e7aa2429"),
            "misc/beam1": ((32, 32), "5ab71394678e02d0dbfc369974f85850c2457e6ddba0d8f02918f2bcfaf37469"),
            "misc/beam2": ((64, 64), "1c1ebcab25eaaef7d14301e7d82d5cba92184dde8745dd027236aa2beb83113c")
        }
        for path, (size, digest) in expected.items():
            data = (converter.ASSETS / "textures" / (path + ".png")).read_bytes()
            self.assertEqual(size, struct.unpack("!II", data[16:24]), path)
            self.assertEqual(digest, converter.sha256(data), path)
        for locale in ("en_us", "ru_ru"):
            lang = json.loads((converter.ASSETS / "lang" / (locale + ".json")).read_text())
            for meta, item in [(4, "arcane_bore_base"), (5, "arcane_bore")]:
                original = lang[f"tile.thaumcraft.wooden_device.{meta}.name"]
                self.assertEqual(original, lang[f"block.thaumcraft.{item}"])
                self.assertEqual(original, lang[f"item.thaumcraft.{item}"])
            self.assertEqual(lang["block.thaumcraft.arcane_bore"], lang["container.thaumcraft.arcane_bore"])
        for item in ("arcane_bore_base", "arcane_bore"):
            model = json.loads((converter.ASSETS / "models/item" / (item + ".json")).read_text())
            self.assertEqual("builtin/entity", model["parent"])
            self.assertEqual({"particle": "thaumcraft:blocks/woodplain"}, model["textures"])
            self.assertEqual([], json.loads((converter.ASSETS / "models/block" / (item + ".json")).read_text())["elements"])

    def test_sound_conversion_rewrites_typed_event_references_and_rejects_collisions(self) -> None:
        sample = {"runicShieldEffect": {"sounds": ["thaumcraft:shield1"]},
                  "reference": {"sounds": [{"name": "thaumcraft:runicShieldEffect", "type": "event", "weight": 2}]}}
        converted, _, _ = converter.convert_sound_definitions(sample)
        self.assertEqual("thaumcraft:runicshieldeffect", converted["reference"]["sounds"][0]["name"])
        self.assertEqual(2, converted["reference"]["sounds"][0]["weight"])
        self.assertEqual("thaumcraft:runicShieldEffect", sample["reference"]["sounds"][0]["name"])
        with self.assertRaisesRegex(ValueError, "collide"):
            converter.convert_sound_definitions({"Same": {"sounds": []}, "same": {"sounds": []}})
        for invalid in ("thaumcraft:runicShieldEffect", "invalid namespace:file", "thaumcraft:", "a:b:c"):
            with self.assertRaises(ValueError):
                converter.resource_location(invalid)

    def test_lang_parser_preserves_equals_spacing_and_last_duplicate(self) -> None:
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            source = Path(temporary) / "sample.lang"
            source.write_text("# comment\nkey= a=b <BR>\nother=first\nother=last\n", encoding="utf-8")
            parsed, duplicates = converter.read_lang(source)
            self.assertEqual(" a=b <BR>", parsed["key"])
            self.assertEqual("last", parsed["other"])
            self.assertEqual(["other"], duplicates)

    def test_lf_and_crlf_text_inputs_produce_identical_outputs_and_provenance_hashes(self) -> None:
        lf = b'{\n  "animation": {"frametime": 2}\n}\n'
        crlf = lf.replace(b"\n", b"\r\n")
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            base = Path(temporary)
            for suffix in (".mcmeta", ".obj", ".mtl", ".json", ".lang"):
                unix_source = base / ("unix" + suffix)
                windows_source = base / ("windows" + suffix)
                unix_target = base / ("unix-copy" + suffix)
                windows_target = base / ("windows-copy" + suffix)
                unix_source.write_bytes(lf)
                windows_source.write_bytes(crlf)
                converter.write_bytes(unix_target, converter.canonical_file_bytes(unix_source))
                converter.write_bytes(windows_target, converter.canonical_file_bytes(windows_source))
                self.assertEqual(lf, unix_target.read_bytes())
                self.assertEqual(lf, windows_target.read_bytes())
                self.assertEqual(converter.sha256(lf), converter.sha256(converter.canonical_file_bytes(windows_source)))
                self.assertEqual(crlf, windows_source.read_bytes(), "Source tree must remain read only")

    def test_png_and_ogg_embedded_crlf_bytes_are_never_normalized(self) -> None:
        with tempfile.TemporaryDirectory(dir=converter.ROOT / ".tmp") as temporary:
            for suffix, data in ((".png", b"\x89PNG\r\n\x1a\n\r\npayload"), (".ogg", b"OggS\r\nbinary\r\n")):
                source = Path(temporary) / ("original" + suffix)
                target = Path(temporary) / ("copy" + suffix)
                source.write_bytes(data)
                converter.write_bytes(target, converter.canonical_file_bytes(source))
                self.assertEqual(data, target.read_bytes())
                self.assertEqual(converter.sha256(data), converter.sha256(converter.canonical_file_bytes(source)))


if __name__ == "__main__":
    unittest.main()
