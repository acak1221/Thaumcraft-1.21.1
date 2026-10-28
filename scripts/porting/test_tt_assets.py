#!/usr/bin/env python3
"""Thaumic Tinkerer asset conversion: naming rules, collisions, lang keys, research keys, fragments.

Most tests build a tiny synthetic checkout in a temporary directory, so they need neither the real
source checkout nor the original Thaumcraft jars. The committed-output tests read only files inside
this repository. The final test replays the whole conversion against the pinned source checkout and
is skipped when that checkout is absent or at another commit.
"""
import contextlib
import hashlib
import io
import json
import struct
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
import tt_assets as tt  # noqa: E402


def png(width=16, height=16, tag=b""):
    """The smallest thing tt_assets accepts as a PNG: signature, IHDR header, a tag to tell files apart."""
    return (tt.PNG_SIGNATURE + struct.pack(">I", 13) + b"IHDR" + struct.pack(">II", width, height)
            + b"\x08\x06\x00\x00\x00" + b"\0\0\0\0" + tag)


LIB_RESEARCH = """package x;
public final class LibResearch {
    public static final String KEY_WARP = "WARP_GATE";
    public static final String KEY_ICHOR = "ICHOR";
    // public static final String KEY_COMMENTED = "COMMENTED";
}
"""
KAMI_SITES = """package x;
class Foo {
    Object a() {
        return new KamiResearchItem(
                LibResearch.KEY_WARP, new AspectList().add(Aspect.TRAVEL, 2), 1, 2, 3, icon);
    }
    Object b() { return flag ? null : new KamiResearchItem("LITERAL_KEY", tags, 1); }
    // new KamiResearchItem(LibResearch.KEY_COMMENTED, tags)
    /* new KamiResearchItem(LibResearch.KEY_COMMENTED, tags) */
    Object c() { return new TTResearchItem(LibResearch.KEY_ICHOR, tags); }
}
"""
LANG_EN = """# MISC
itemGroup.ThaumicTinkerer=Thaumic Tinkerer

tile.fooBar.name=Foo Bar
item.bazQux.name=Baz = Qux
item.baz.0.name=Variant\r
enchantment.ttinkerer:autoSmelt=Flaming Touch
ttresearch.prefix=[TT]
ttresearch.prefix.kami=[Kami]
ttresearch.name.WARP_GATE=Gate
ttresearch.lore.WARP_GATE=Lore of the gate
ttresearch.name.PLAIN=Plain
ttresearch.lore.PLAIN=Plain lore
ttresearch.page.PLAIN.0=Gives 30% off and %s
ttresearch.webpage.peripherals=first
ttresearch.webpage.peripherals=second
"""
LANG_RU = """ttresearch.prefix=[ТТ]\x20
ttresearch.prefix.kami=[ТТ-К]\x20
ttresearch.name.WARP_GATE=Врата
ttresearch.lore.WARP_GATE=Текст
ttresearch.name.PLAIN=Простое
ttresearch.lore.PLAIN=Простой текст
"""
LANG_GB = "item.bazQux.name=Baz (GB)\n"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data if isinstance(data, bytes) else data.encode("utf-8"))


def make_source(base, textures=None, lang=None, root_files=None, java=True):
    """A synthetic checkout. ``textures`` maps 'items/fooBar.png' to bytes."""
    resources = base / "src/main/resources"
    default = {"items/fooBar.png": png(tag=b"a"), "blocks/crop_aer_0.png": png(tag=b"b"),
               "gui/remotePlacer.png": png(tag=b"c")}
    for relative, data in (default if textures is None else textures).items():
        write(resources / tt.OLD_TEXTURES / relative, data)
    languages = {"en_US": LANG_EN, "ru_RU": LANG_RU, "en_GB": LANG_GB} if lang is None else lang
    for name, text in languages.items():
        write(resources / tt.OLD_LANG / f"{name}.lang", text)
    for name, data in (root_files or {}).items():
        write(resources / name, data)
    if java:
        write(base / tt.SOURCE_JAVA / tt.LIB_RESEARCH, LIB_RESEARCH)
        write(base / tt.SOURCE_JAVA / "x/Foo.java", KAMI_SITES)
    return base


class TempCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name)
        self.source = self.base / "source"
        self.root = self.base / "repo"
        self.root.mkdir()

    def run_main(self, *arguments):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = tt.main([*arguments], root=self.root)
        return code, out.getvalue(), err.getvalue()

    def fragment(self, name, document):
        write(self.root / tt.LANG_EXTRA / name, document if isinstance(document, str) else json.dumps(document))

    def lang(self, locale):
        path = self.root / tt.TARGET_RESOURCES / tt.NEW_LANG / f"{locale}.json"
        return json.loads(path.read_text(encoding="utf-8"))


class NamingTest(unittest.TestCase):
    def test_snake_case_table(self):
        table = {
            "Levitational Locomotive Relay0": "levitational_locomotive_relay_0",
            "Levitational Locomotive Relay2": "levitational_locomotive_relay_2",
            "Levitational Locomotive1": "levitational_locomotive_1",
            "Mobilization Levitator": "mobilization_levitator",
            "focusXPDrain": "focus_xp_drain",
            "fruit_aer": "fruit_aer",
            "crop_aer_0": "crop_aer_0",
            "fireAir_layer_0": "fire_air_layer_0",
            "aer_condensed": "aer_condensed",
            "emptyTexture": "empty_texture",
            "darkQuartz0": "dark_quartz_0",
            "ichorclothHelmGem": "ichorcloth_helm_gem",
            "ichorAxeGem2": "ichor_axe_gem_2",
            "chiseledDarkQuartz1": "chiseled_dark_quartz_1",
            "placementMirror": "placement_mirror",
            "placementMirror1": "placement_mirror_1",
            "r_enchanting": "r_enchanting",
            "atFacingIndent": "at_facing_indent",
            "ichor1": "ichor_1",
            "ichorGem2": "ichor_gem_2",
            "magnetN": "magnet_n",
            "mobMagnetN": "mob_magnet_n",
            "baneOfAwfulTurds": "bane_of_awful_turds",
            "seeds_perdito": "seeds_perdito",
            "ichor12": "ichor_12",
            "0": "0",
        }
        for stem, expected in table.items():
            self.assertEqual(expected, tt.snake_case(stem), stem)

    def test_snake_case_is_idempotent(self):
        for stem in ("focusXPDrain", "Levitational Locomotive Relay0", "crop_aer_0", "darkQuartz0"):
            once = tt.snake_case(stem)
            self.assertEqual(once, tt.snake_case(once))

    def test_rename_file_keeps_mcmeta_next_to_its_png(self):
        self.assertEqual("fire_air_layer_0.png", tt.rename_file("fireAir_layer_0.png"))
        self.assertEqual("fire_air_layer_0.png.mcmeta", tt.rename_file("fireAir_layer_0.png.mcmeta"))
        self.assertEqual("portal.png.mcmeta", tt.rename_file("portal.png.mcmeta"))
        self.assertEqual("focus_heal_depth.png.mcmeta", tt.rename_file("focusHealDepth.png.mcmeta"))

    def test_rename_file_rejects_what_it_cannot_name(self):
        for name in ("notes.txt", ".png", "Café.png", "a+b.png"):
            with self.assertRaises(tt.ConversionError, msg=name):
                tt.rename_file(name)


class TextureTest(TempCase):
    def test_directories_are_renamed_and_bytes_are_kept(self):
        make_source(self.source, textures={
            "items/focusXPDrain.png": png(tag=b"1"), "blocks/Levitational Locomotive Relay0.png": png(tag=b"2"),
            "enchants/slowFall.png": png(tag=b"3"), "gui/warpGate.png": png(tag=b"4"),
            "misc/atLeft.png": png(tag=b"5"), "model/magnetN.png": png(tag=b"6"), "logo.png": png(tag=b"7")})
        plan = tt.build(self.source, self.root)
        self.assertEqual({
            f"{tt.OLD_TEXTURES}/items/focusXPDrain.png": f"{tt.NEW_TEXTURES}/item/focus_xp_drain.png",
            f"{tt.OLD_TEXTURES}/blocks/Levitational Locomotive Relay0.png":
                f"{tt.NEW_TEXTURES}/block/levitational_locomotive_relay_0.png",
            f"{tt.OLD_TEXTURES}/enchants/slowFall.png": f"{tt.NEW_TEXTURES}/enchant/slow_fall.png",
            f"{tt.OLD_TEXTURES}/gui/warpGate.png": f"{tt.NEW_TEXTURES}/gui/warp_gate.png",
            f"{tt.OLD_TEXTURES}/misc/atLeft.png": f"{tt.NEW_TEXTURES}/misc/at_left.png",
            f"{tt.OLD_TEXTURES}/model/magnetN.png": f"{tt.NEW_TEXTURES}/models/magnet_n.png",
            f"{tt.OLD_TEXTURES}/logo.png": f"{tt.NEW_TEXTURES}/logo.png"}, plan.mapping)
        for texture in plan.textures:
            self.assertEqual(texture.data, plan.outputs[f"{tt.TARGET_RESOURCES}/{texture.new}"])
        self.assertEqual(png(tag=b"1"), plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_TEXTURES}/item/focus_xp_drain.png"])

    def test_name_collision_after_renaming_is_fatal(self):
        for first, second in (("fooBar.png", "foo_bar.png"), ("foo bar.png", "foo_bar.png"),
                              ("FooBar.png", "fooBar.png")):
            with self.subTest(first=first, second=second):
                make_source(self.source, textures={f"items/{first}": png(tag=b"1"), f"items/{second}": png(tag=b"2")})
                with self.assertRaisesRegex(tt.ConversionError, "name collision after renaming"):
                    tt.build(self.source, self.root)
                self.assertEqual([], list(self.root.rglob("*")), "nothing is written before the plan is complete")

    def test_same_name_in_different_directories_is_not_a_collision(self):
        make_source(self.source, textures={"items/repair.png": png(tag=b"1"), "misc/repair.png": png(tag=b"2")})
        plan = tt.build(self.source, self.root)
        self.assertEqual(2, len(plan.textures))

    def test_unknown_directory_unknown_type_and_nesting_are_fatal(self):
        for relative in ("weird/x.png", "items/sub/x.png", "items/readme.txt"):
            with self.subTest(relative=relative):
                make_source(self.source, textures={relative: b"x"})
                with self.assertRaises(tt.ConversionError):
                    tt.build(self.source, self.root)

    def test_a_texture_that_is_not_a_png_is_fatal(self):
        make_source(self.source, textures={"items/fake.png": b"not a png at all, just text.."})
        with self.assertRaisesRegex(tt.ConversionError, "not a PNG"):
            tt.build(self.source, self.root)

    def test_root_duplicate_identical_to_gui_file_is_recorded_not_copied(self):
        data = png(tag=b"same")
        make_source(self.source, textures={"gui/remotePlacer.png": data}, root_files={"remotePlacer.png": data})
        plan = tt.build(self.source, self.root)
        self.assertEqual([f"{tt.OLD_TEXTURES}/gui/remotePlacer.png"], list(plan.mapping))
        entry = plan.duplicates["remotePlacer.png"]
        self.assertEqual(f"{tt.OLD_TEXTURES}/gui/remotePlacer.png", entry["identical_to"])
        self.assertEqual(f"{tt.NEW_TEXTURES}/gui/remote_placer.png", entry["new"])
        self.assertFalse(entry["copied"])
        self.assertEqual(hashlib.sha256(data).hexdigest(), plan.document["files"]["remotePlacer.png"])

    def test_root_duplicate_that_differs_is_fatal(self):
        make_source(self.source, textures={"gui/remotePlacer.png": png(tag=b"1")},
                    root_files={"remotePlacer.png": png(tag=b"2")})
        with self.assertRaisesRegex(tt.ConversionError, "refusing to pick one"):
            tt.build(self.source, self.root)

    def test_root_file_is_copied_when_the_gui_directory_does_not_produce_it(self):
        make_source(self.source, textures={"items/aer.png": png()}, root_files={"remotePlacer.png": png(tag=b"r")})
        plan = tt.build(self.source, self.root)
        self.assertEqual(f"{tt.NEW_TEXTURES}/gui/remote_placer.png", plan.mapping["remotePlacer.png"])
        self.assertEqual({}, plan.duplicates)

    def test_mcmeta_must_sit_next_to_a_png_and_be_a_valid_animation(self):
        animation = json.dumps({"animation": {"frames": [0, 1, {"index": 2, "time": 4}], "frametime": 3}}).encode()
        make_source(self.source, textures={"blocks/portalThing.png": png(16, 48), "blocks/portalThing.png.mcmeta": animation})
        plan = tt.build(self.source, self.root)
        self.assertIn(f"{tt.NEW_TEXTURES}/block/portal_thing.png.mcmeta", plan.mapping.values())
        cases = {
            "orphan": ({"blocks/lonely.png.mcmeta": b"{}"}, "no png next to it"),
            "not json": ({"blocks/a.png": png(), "blocks/a.png.mcmeta": b"{oops"}, "invalid mcmeta JSON"),
            "frame out of range": ({"blocks/a.png": png(16, 32),
                                    "blocks/a.png.mcmeta": b'{"animation":{"frames":[0,5]}}'}, "outside 0..1"),
            "ragged strip": ({"blocks/a.png": png(16, 40), "blocks/a.png.mcmeta": b'{"animation":{}}'},
                             "whole number"),
        }
        for label, (textures, message) in cases.items():
            with self.subTest(label):
                make_source(self.source, textures=textures)
                with self.assertRaisesRegex(tt.ConversionError, message):
                    tt.build(self.source, self.root)

    def test_empty_animation_object_means_square_frames_of_the_image_width(self):
        self.assertEqual(9, tt.check_animation("x", b'{"animation": {}}', (8, 72)))
        self.assertEqual(1, tt.check_animation("x", b"{}", (8, 72)))


class LangParseTest(unittest.TestCase):
    def test_comments_blank_lines_carriage_returns_and_equals_in_values(self):
        lang = tt.parse_lang("# top\r\n\r\na.b=one = two\r\nc=\r\n   \r\nd.e=trailing \r\n", "t.lang")
        self.assertEqual({"a.b": "one = two", "c": "", "d.e": "trailing "}, lang.entries)

    def test_a_bom_is_dropped_and_a_lone_cr_ends_a_line(self):
        lang = tt.parse_lang("﻿a=1\rb=2", "t.lang")
        self.assertEqual({"a": "1", "b": "2"}, lang.entries)

    def test_duplicate_keys_keep_the_last_value_and_are_reported(self):
        lang = tt.parse_lang("a=1\nb=2\na=3\n# c\na=4\n", "t.lang")
        self.assertEqual("4", lang.entries["a"])
        self.assertEqual({"a": [1, 3, 5]}, lang.duplicates)

    def test_a_line_without_equals_and_a_padded_key_are_fatal(self):
        with self.assertRaisesRegex(tt.ConversionError, "t.lang:2"):
            tt.parse_lang("a=1\njust text\n", "t.lang")
        with self.assertRaisesRegex(tt.ConversionError, "whitespace"):
            tt.parse_lang(" a=1\n", "t.lang")


class PercentTest(unittest.TestCase):
    def test_escape_table(self):
        table = {
            "30% discount": "30%% discount",
            "20%까지": "20%%까지",
            "в 20% на": "в 20%% на",
            "100%": "100%%",
            "% d": "%% d",
            "50%% and 50% ": "50%% and 50%% ",
            "Current Pattern: %s": "Current Pattern: %s",
            "Bound to %s's research.": "Bound to %s's research.",
            "%1$s and %2$s": "%1$s and %2$s",
            "%d items, %.2f": "%d items, %.2f",
            "no percent": "no percent",
            "": "",
        }
        for value, expected in table.items():
            self.assertEqual(expected, tt.escape_percent(value), value)

    def test_escaping_twice_changes_nothing(self):
        once = tt.escape_percent("30% discount, 50%% off, %s")
        self.assertEqual(once, tt.escape_percent(once))

    def test_an_attached_unknown_letter_is_a_human_decision(self):
        for value in ("%x", "50%natural", "%S"):
            with self.assertRaisesRegex(tt.ConversionError, "ambiguous percent"):
                tt.escape_percent(value, "t.lang: key")

    def test_percent_is_escaped_in_the_converted_json(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            make_source(base / "source")
            plan = tt.build(base / "source", base / "repo")
        en = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_us.json"])
        self.assertEqual("Gives 30%% off and %s", en["ttresearch.page.PLAIN.0"])


class KeyMappingTest(unittest.TestCase):
    def test_classification_table(self):
        mapped = {
            "tile.darkQuartz.name": ("tile.<name>.name", "block.thaumictinkerer.dark_quartz"),
            "tile.Levitational Locomotive Relay.name":
                ("tile.<name>.name", "block.thaumictinkerer.levitational_locomotive_relay"),
            "tile.Levitational Locomotive.name": ("tile.<name>.name", "block.thaumictinkerer.levitational_locomotive"),
            "item.focusXPDrain.name": ("item.<name>.name", "item.thaumictinkerer.focus_xp_drain"),
            "item.ichorclothHelmGem.name": ("item.<name>.name", "item.thaumictinkerer.ichorcloth_helm_gem"),
            "enchantment.ttinkerer:autoSmelt":
                ("enchantment.ttinkerer:<name>", "enchantment.thaumictinkerer.auto_smelt"),
        }
        for key, (label, expected) in mapped.items():
            family, new_key = tt.classify_key(key)
            self.assertEqual((label, "mapped", expected), (family.label, family.action, new_key), key)

    def test_everything_else_keeps_its_key_verbatim(self):
        verbatim = {
            "item.mobAspect.condensed.name": "item.* (other: metadata or variant keys)",
            "item.infusedPotion.Aer.name": "item.* (other: metadata or variant keys)",
            "item.Wand.ICHOR.cap": "item.Wand.<component>.<slot>",
            "tile.foo.0.name": "tile.* (other: metadata or variant keys)",
            "item.foo": "item.* (other: metadata or variant keys)",
            "itemGroup.ThaumicTinkerer": "itemGroup.*",
            "ttmisc.light0": "ttmisc.*",
            "ttwaila.currentTool": "ttwaila.*",
            "ttcomputer.dissipator": "ttcomputer.*",
            "tt.nei.chance": "tt.nei.*",
            "ttresearch.name.PLAIN": "ttresearch.name.<KEY>",
            "ttresearch.lore.PLAIN": "ttresearch.lore.<KEY>",
            "ttresearch.page.PLAIN.0": "ttresearch.page.<KEY>.<n>",
            "ttresearch.page.FOCUS_ENDER_CHEST.ES": "ttresearch.page.<KEY>.<n>",
            "ttresearch.webpage.peripherals": "ttresearch.webpage.*",
            "ttresearch.prefix.kami": "ttresearch.prefix*",
            "tc.research_category.TT_CATEGORY": "tc.research_category.*",
            "container.thing": "(other)",
            "potion.thing": "(other)",
        }
        for key, label in verbatim.items():
            family, new_key = tt.classify_key(key)
            self.assertEqual((label, "verbatim", key), (family.label, family.action, new_key), key)

    def test_two_old_keys_for_one_new_key_are_fatal(self):
        lang = tt.parse_lang("item.fooBar.name=A\nitem.foo_bar.name=B\n", "t.lang")
        with self.assertRaisesRegex(tt.ConversionError, "both become"):
            tt.convert_lang(lang, "en_us", "a", "b")

    def test_convert_lang_sorts_keys_and_records_renames(self):
        lang = tt.parse_lang("zeta=1\ntile.fooBar.name=Foo\nalpha=2\n", "t.lang")
        result = tt.convert_lang(lang, "en_us", "a", "b")
        self.assertEqual(["alpha", "block.thaumictinkerer.foo_bar", "zeta"], list(result.values))
        self.assertEqual({"tile.fooBar.name": "block.thaumictinkerer.foo_bar"}, result.key_map)


class ResearchKeyTest(TempCase):
    def test_name_text_prefix_and_kami_prefix(self):
        values = {
            "ttresearch.prefix": "[TT] ", "ttresearch.prefix.kami": "[TTKami] ",
            "ttresearch.name.PLAIN": "Plain", "ttresearch.lore.PLAIN": "plain lore",
            "ttresearch.name.WARP_GATE": "Gate", "ttresearch.lore.WARP_GATE": "gate lore",
            "ttresearch.page.PLAIN.0": "page",
        }
        derived, info = tt.derive_research(values, {}, {"WARP_GATE"})
        self.assertEqual({
            "tc.research_name.PLAIN": "Plain", "tc.research_text.PLAIN": "[TT] plain lore",
            "tc.research_name.WARP_GATE": "Gate", "tc.research_text.WARP_GATE": "[TTKami] gate lore"}, derived)
        self.assertEqual(["WARP_GATE"], info["kami_texts"])
        self.assertEqual(([], []), (info["name_without_lore"], info["lore_without_name"]))

    def test_no_separator_or_trimming_is_added(self):
        derived, _ = tt.derive_research({"ttresearch.prefix": "[TT]", "ttresearch.lore.A": " x ",
                                         "ttresearch.name.A": "N"}, {}, set())
        self.assertEqual("[TT] x ", derived["tc.research_text.A"])

    def test_mismatched_name_and_lore_emit_only_what_exists_and_are_reported(self):
        values = {"ttresearch.prefix": "[TT]", "ttresearch.name.ONLY_NAME": "n", "ttresearch.lore.ONLY_LORE": "l"}
        derived, info = tt.derive_research(values, {}, set())
        self.assertEqual({"tc.research_name.ONLY_NAME": "n", "tc.research_text.ONLY_LORE": "[TT]l"}, derived)
        self.assertEqual(["ONLY_NAME"], info["name_without_lore"])
        self.assertEqual(["ONLY_LORE"], info["lore_without_name"])

    def test_a_missing_prefix_falls_back_to_en_us_and_is_reported(self):
        fallback = {"ttresearch.prefix": "[TT]", "ttresearch.prefix.kami": "[K]"}
        derived, info = tt.derive_research({"ttresearch.lore.A": "x", "ttresearch.lore.B": "y"}, fallback, {"B"})
        self.assertEqual({"tc.research_text.A": "[TT]x", "tc.research_text.B": "[K]y"}, derived)
        self.assertEqual(["ttresearch.prefix", "ttresearch.prefix.kami"], info["prefix_fallback"])
        with self.assertRaisesRegex(tt.ConversionError, "no ttresearch.prefix"):
            tt.derive_research({"ttresearch.lore.A": "x"}, {}, set())

    def test_page_keys_stay_verbatim_and_derived_keys_are_added(self):
        make_source(self.source)
        plan = tt.build(self.source, self.root)
        en = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_us.json"])
        ru = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/ru_ru.json"])
        self.assertEqual("Gives 30%% off and %s", en["ttresearch.page.PLAIN.0"])
        self.assertEqual("Gate", en["ttresearch.name.WARP_GATE"])
        self.assertEqual("Gate", en["tc.research_name.WARP_GATE"])
        self.assertEqual("[Kami]Lore of the gate", en["tc.research_text.WARP_GATE"])
        self.assertEqual("[TT]Plain lore", en["tc.research_text.PLAIN"])
        self.assertEqual("[ТТ-К] Текст", ru["tc.research_text.WARP_GATE"])
        self.assertEqual("[ТТ] Простой текст", ru["tc.research_text.PLAIN"])
        gb = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_gb.json"])
        self.assertEqual({"item.thaumictinkerer.baz_qux": "Baz (GB)"}, gb)

    def test_a_source_that_already_has_a_derived_key_is_fatal(self):
        make_source(self.source, lang={"en_US": LANG_EN + "tc.research_name.PLAIN=x\n"})
        with self.assertRaisesRegex(tt.ConversionError, "already exist"):
            tt.build(self.source, self.root)


class KamiExtractionTest(TempCase):
    def test_keys_come_from_new_kami_research_item_sites_only(self):
        make_source(self.source)
        keys, hashes = tt.kami_research_keys(self.source)
        self.assertEqual(["LITERAL_KEY", "WARP_GATE"], list(keys), "ICHOR is a plain TTResearchItem, COMMENTED is a comment")
        self.assertTrue(keys["WARP_GATE"].endswith("x/Foo.java:4"), keys["WARP_GATE"])
        self.assertTrue(keys["LITERAL_KEY"].endswith("x/Foo.java:7"), keys["LITERAL_KEY"])
        self.assertEqual({f"{tt.SOURCE_JAVA}/{tt.LIB_RESEARCH}", f"{tt.SOURCE_JAVA}/x/Foo.java"}, set(hashes))

    def test_unresolvable_constant_duplicate_and_empty_source_are_fatal(self):
        make_source(self.source)
        write(self.source / tt.SOURCE_JAVA / "x/Bad.java", "class B { Object o = new KamiResearchItem(LibResearch.KEY_NOPE, t); }")
        with self.assertRaisesRegex(tt.ConversionError, "cannot resolve"):
            tt.kami_research_keys(self.source)
        (self.source / tt.SOURCE_JAVA / "x/Bad.java").unlink()
        write(self.source / tt.SOURCE_JAVA / "x/Dup.java", 'class D { Object o = new KamiResearchItem("WARP_GATE", t); }')
        with self.assertRaisesRegex(tt.ConversionError, "created twice"):
            tt.kami_research_keys(self.source)
        (self.source / tt.SOURCE_JAVA / "x/Dup.java").unlink()
        (self.source / tt.SOURCE_JAVA / "x/Foo.java").unlink()
        with self.assertRaisesRegex(tt.ConversionError, "no 'new KamiResearchItem"):
            tt.kami_research_keys(self.source)


class FragmentTest(TempCase):
    def build(self):
        make_source(self.source)
        return tt.build(self.source, self.root)

    def test_no_fragment_changes_nothing_and_keeps_the_directory_in_git(self):
        plan = self.build()
        self.assertEqual([], plan.warnings)
        self.assertEqual(b"", plan.outputs[f"{tt.LANG_EXTRA}/.gitkeep"])
        en = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_us.json"])
        self.assertNotIn("menu.thaumictinkerer.thing", en)

    def test_fragments_are_merged_per_language_sorted_and_win_with_a_warning(self):
        self.fragment("simple_blocks.en_us.json", {"menu.thaumictinkerer.thing": "Thing",
                                                   "block.thaumictinkerer.foo_bar": "Foo Bar (extra)"})
        self.fragment("tools.en_us.json", {"a.first": "1"})
        self.fragment("simple_blocks.ru_ru.json", {"menu.thaumictinkerer.thing": "Штука"})
        plan = self.build()
        en = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_us.json"])
        ru = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/ru_ru.json"])
        gb = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_gb.json"])
        self.assertEqual("Thing", en["menu.thaumictinkerer.thing"])
        self.assertEqual("Foo Bar (extra)", en["block.thaumictinkerer.foo_bar"], "extras win over the base")
        self.assertEqual("1", en["a.first"])
        self.assertEqual("Штука", ru["menu.thaumictinkerer.thing"])
        self.assertNotIn("menu.thaumictinkerer.thing", gb, "a fragment only touches its own language")
        self.assertEqual(list(en), sorted(en))
        self.assertEqual(1, len(plan.warnings))
        self.assertIn("simple_blocks.en_us.json", plan.warnings[0])
        self.assertIn("block.thaumictinkerer.foo_bar", plan.warnings[0])
        self.assertNotIn(f"{tt.LANG_EXTRA}/.gitkeep", plan.outputs, "a populated directory needs no placeholder")
        doc = plan.outputs[tt.DOC_PATH].decode("utf-8")
        self.assertIn("| `en_us` | `addons/thaumic-tinkerer/lang-extra/simple_blocks.en_us.json` | 2 | "
                      "`block.thaumictinkerer.foo_bar` |", doc)

    def test_fragments_can_replace_a_derived_research_key(self):
        self.fragment("research.en_us.json", {"tc.research_text.PLAIN": "Hand written"})
        plan = self.build()
        en = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_us.json"])
        self.assertEqual("Hand written", en["tc.research_text.PLAIN"])
        self.assertEqual(1, len(plan.warnings))

    def test_a_clash_between_two_fragments_is_a_warning_and_the_later_file_name_wins(self):
        self.fragment("a_group.en_us.json", {"k": "from a"})
        self.fragment("b_group.en_us.json", {"k": "from b"})
        plan = self.build()
        en = json.loads(plan.outputs[f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_us.json"])
        self.assertEqual("from b", en["k"])
        self.assertEqual(1, len(plan.warnings))
        self.assertIn("overrides", plan.warnings[0])
        self.assertIn("a_group.en_us.json", plan.warnings[0])

    def test_invalid_fragments_are_hard_errors(self):
        bad = {
            "broken.en_us.json": ("{not json", "invalid JSON"),
            "list.en_us.json": ("[1, 2]", "flat JSON object"),
            "nested.en_us.json": ('{"a": {"b": "c"}}', "not a string"),
            "number.en_us.json": ('{"a": 1}', "not a string"),
            "null.en_us.json": ('{"a": null}', "not a string"),
            "dup.en_us.json": ('{"a": "1", "a": "2"}', "duplicate key"),
            "no_language.json": ("{}", "<group>.<lang>.json"),
            "Upper.en_us.json": ("{}", "<group>.<lang>.json"),
            "group.xx_xx.json": ("{}", "no base language"),
        }
        for name, (text, message) in bad.items():
            with self.subTest(name):
                for stale in (self.root / tt.LANG_EXTRA).glob("*") if (self.root / tt.LANG_EXTRA).exists() else ():
                    stale.unlink()
                self.fragment(name, text)
                make_source(self.source)
                with self.assertRaisesRegex(tt.ConversionError, message):
                    tt.build(self.source, self.root)

    def test_non_json_files_in_the_directory_are_ignored(self):
        write(self.root / tt.LANG_EXTRA / "README.md", "notes")
        write(self.root / tt.LANG_EXTRA / ".gitkeep", b"")
        plan = self.build()
        self.assertEqual([], plan.warnings)
        self.assertIn(f"{tt.LANG_EXTRA}/.gitkeep", plan.outputs)

    def test_check_includes_the_fragments(self):
        make_source(self.source)
        self.assertEqual(0, self.run_main("--source", str(self.source), "--allow-unpinned")[0])
        self.assertEqual(0, self.run_main("--source", str(self.source), "--allow-unpinned", "--check")[0])
        self.fragment("new_group.en_us.json", {"menu.thaumictinkerer.thing": "Thing"})
        code, out, _ = self.run_main("--source", str(self.source), "--allow-unpinned", "--check")
        self.assertEqual(1, code)
        self.assertIn("differs: " + f"{tt.TARGET_RESOURCES}/{tt.NEW_LANG}/en_us.json", out)
        code, _, err = self.run_main("--source", str(self.source), "--allow-unpinned")
        self.assertEqual(0, code)
        self.assertEqual("", err, "no collision, no warning")
        self.assertEqual("Thing", self.lang("en_us")["menu.thaumictinkerer.thing"])
        self.assertEqual(0, self.run_main("--source", str(self.source), "--allow-unpinned", "--check")[0])

    def test_a_collision_is_printed_as_a_warning_on_stderr(self):
        make_source(self.source)
        self.fragment("x.en_us.json", {"tile.fooBar.name": "unrelated", "block.thaumictinkerer.foo_bar": "Extra"})
        code, _, err = self.run_main("--source", str(self.source), "--allow-unpinned")
        self.assertEqual(0, code)
        self.assertIn("warning:", err)
        self.assertIn("block.thaumictinkerer.foo_bar", err)
        self.assertEqual("Extra", self.lang("en_us")["block.thaumictinkerer.foo_bar"])


class ApplyAndCheckTest(TempCase):
    def test_write_then_check_then_detect_every_kind_of_drift(self):
        make_source(self.source)
        args = ("--source", str(self.source), "--allow-unpinned")
        code, out, _ = self.run_main(*args)
        self.assertEqual(0, code)
        self.assertIn("written", out)
        self.assertEqual(0, self.run_main(*args, "--check")[0])
        code, out, _ = self.run_main(*args)
        self.assertIn("written 0, unchanged", out, "a second run rewrites nothing")

        target = self.root / tt.TARGET_RESOURCES / tt.NEW_TEXTURES / "item/foo_bar.png"
        self.assertEqual(png(tag=b"a"), target.read_bytes())
        target.write_bytes(b"tampered")
        code, out, _ = self.run_main(*args, "--check")
        self.assertEqual(1, code)
        self.assertIn(f"differs: {tt.TARGET_RESOURCES}/{tt.NEW_TEXTURES}/item/foo_bar.png", out)
        self.assertEqual(b"tampered", target.read_bytes(), "--check never writes")
        target.unlink()
        code, out, _ = self.run_main(*args, "--check")
        self.assertEqual(1, code)
        self.assertIn("missing:", out)
        self.assertEqual(0, self.run_main(*args)[0])
        self.assertEqual(png(tag=b"a"), target.read_bytes())

        (self.root / tt.MAP_PATH).write_text("{}", encoding="utf-8")
        self.assertEqual(1, self.run_main(*args, "--check")[0])

    def test_files_an_earlier_run_produced_and_this_run_does_not_are_removed(self):
        make_source(self.source)
        args = ("--source", str(self.source), "--allow-unpinned")
        self.run_main(*args)
        stale = self.root / tt.TARGET_RESOURCES / tt.NEW_TEXTURES / "item/foo_bar.png"
        hand_written = self.root / tt.TARGET_RESOURCES / tt.NEW_ASSETS / "models/item/keep_me.json"
        write(hand_written, "{}")
        (self.source / "src/main/resources" / tt.OLD_TEXTURES / "items/fooBar.png").unlink()
        code, out, _ = self.run_main(*args, "--check")
        self.assertEqual(1, code)
        self.assertIn("stale", out)
        self.assertTrue(stale.exists())
        self.run_main(*args)
        self.assertFalse(stale.exists())
        self.assertTrue(hand_written.exists(), "files the converter never produced are left alone")
        self.assertEqual(0, self.run_main(*args, "--check")[0])

    def test_a_failed_conversion_exits_2_and_writes_nothing(self):
        make_source(self.source, textures={"items/fooBar.png": png(), "items/foo_bar.png": png(tag=b"x")})
        code, _, err = self.run_main("--source", str(self.source), "--allow-unpinned")
        self.assertEqual(2, code)
        self.assertIn("name collision", err)
        self.assertEqual([], list(self.root.rglob("*")))

    def test_the_output_is_deterministic(self):
        make_source(self.source)
        first = tt.build(self.source, self.root).outputs
        second = tt.build(self.source, self.root).outputs
        self.assertEqual(first, second)
        self.assertEqual(sorted(first), list(sorted(first)))

    def test_map_document_has_provenance_hashes_and_sorted_mapping(self):
        make_source(self.source)
        document = json.loads(tt.build(self.source, self.root).outputs[tt.MAP_PATH])
        provenance = document["provenance"]
        self.assertEqual(tt.SOURCE_COMMIT, provenance["source_commit"])
        self.assertEqual(tt.SOURCE_REPO, provenance["source_repo"])
        self.assertEqual("CC BY-NC-SA 3.0", provenance["license"])
        self.assertEqual("scripts/porting/tt_assets.py", provenance["generator"])
        self.assertEqual(list(document["mapping"]), sorted(document["mapping"]))
        self.assertEqual({"LITERAL_KEY", "WARP_GATE"}, set(document["kami_research"]["keys"]))
        self.assertEqual(hashlib.sha256(png(tag=b"a")).hexdigest(),
                         document["files"][f"{tt.OLD_TEXTURES}/items/fooBar.png"])
        self.assertIn(f"{tt.OLD_LANG}/en_US.lang", document["files"])
        self.assertEqual({"tile.fooBar.name": "block.thaumictinkerer.foo_bar"},
                         {k: v for k, v in document["lang_key_map"].items() if k.startswith("tile.")})

    def test_the_doc_lists_verbatim_item_keys_duplicates_and_percent_escapes(self):
        make_source(self.source)
        doc = tt.build(self.source, self.root).outputs[tt.DOC_PATH].decode("utf-8")
        for expected in ("`item.baz.0.name`", "`ttresearch.webpage.peripherals`", "30%% off", "`tc.research_text.<KEY>`",
                         "`WARP_GATE`", "`item.bazQux.name` | `item.thaumictinkerer.baz_qux`"):
            self.assertIn(expected, doc)


class PinTest(unittest.TestCase):
    def completed(self, stdout):
        return mock.Mock(stdout=stdout + "\n")

    def test_matching_commit_passes(self):
        with mock.patch.object(tt.subprocess, "run", return_value=self.completed(tt.SOURCE_COMMIT)):
            tt.verify_pin(Path("/nowhere"))

    def test_other_commit_and_missing_git_are_fatal(self):
        with mock.patch.object(tt.subprocess, "run", return_value=self.completed("0" * 40)):
            with self.assertRaisesRegex(tt.ConversionError, "pinned to"):
                tt.verify_pin(Path("/nowhere"))
        with mock.patch.object(tt.subprocess, "run", side_effect=FileNotFoundError("git")):
            with self.assertRaisesRegex(tt.ConversionError, "cannot read the commit"):
                tt.verify_pin(Path("/nowhere"))


class CommittedOutputTest(unittest.TestCase):
    """The checked-in tree, without the source checkout."""

    @classmethod
    def setUpClass(cls):
        cls.document = json.loads((tt.ROOT / tt.MAP_PATH).read_text(encoding="utf-8"))
        cls.resources = tt.ROOT / tt.TARGET_RESOURCES

    def test_provenance_header(self):
        provenance = self.document["provenance"]
        self.assertEqual(tt.SOURCE_COMMIT, provenance["source_commit"])
        self.assertEqual("https://github.com/GTNewHorizons/ThaumicTinkerer", provenance["source_repo"])
        self.assertEqual("CC BY-NC-SA 3.0", provenance["license"])

    def test_mapping_is_sorted_injective_legal_and_bytes_match_the_recorded_hashes(self):
        mapping = self.document["mapping"]
        self.assertEqual(list(mapping), sorted(mapping))
        self.assertEqual(len(mapping), len(set(mapping.values())), "two sources share one target")
        for old, new in mapping.items():
            for component in new.split("/"):
                self.assertRegex(component, tt.PATH_COMPONENT, new)
            self.assertTrue(new.startswith(tt.NEW_TEXTURES + "/"), new)
            target = self.resources / new
            self.assertTrue(target.is_file(), new)
            self.assertEqual(self.document["files"][old], hashlib.sha256(target.read_bytes()).hexdigest(), new)

    def test_every_texture_on_disk_is_in_the_mapping(self):
        on_disk = {p.relative_to(self.resources).as_posix() for p in (self.resources / tt.NEW_TEXTURES).rglob("*")
                   if p.is_file()}
        self.assertEqual(set(self.document["mapping"].values()), on_disk)

    def test_each_mcmeta_sits_next_to_its_png(self):
        new = set(self.document["mapping"].values())
        for path in new:
            if path.endswith(".png.mcmeta"):
                self.assertIn(path[: -len(".mcmeta")], new)

    def test_duplicate_root_texture_is_identical_to_its_gui_twin(self):
        entry = self.document["duplicates"]["remotePlacer.png"]
        self.assertEqual(self.document["files"][entry["identical_to"]], entry["sha256"])
        self.assertEqual(self.document["mapping"][entry["identical_to"]], entry["new"])
        self.assertFalse(entry["copied"])

    def test_lang_files_are_sorted_flat_strings_without_raw_percent(self):
        self.assertEqual(7, len(self.document["lang_files"]))
        for new in self.document["lang_files"].values():
            data = json.loads((self.resources / new).read_text(encoding="utf-8"))
            self.assertEqual(list(data), sorted(data), new)
            for key, value in data.items():
                self.assertIsInstance(value, str, f"{new}: {key}")
                self.assertEqual(value, tt.escape_percent(value), f"{new}: {key}")
            self.assertFalse([k for k in data if k.startswith(("tile.", "item.")) and k.endswith(".name")
                              and k.count(".") == 2], f"{new}: unconverted tile./item. names")

    def fragment_keys(self, locale):
        keys = set()
        for path in (tt.ROOT / tt.LANG_EXTRA).glob(f"*.{locale}.json"):
            keys.update(json.loads(path.read_text(encoding="utf-8")))
        return keys

    def test_derived_research_keys_follow_the_original_get_text(self):
        kami = set(self.document["kami_research"]["keys"])
        self.assertEqual(25, len(kami))
        self.assertTrue({"WARP_GATE", "ICHOR", "FOCUS_RECALL", "ICHORCLOTH_ARMOR"} <= kami)
        self.assertFalse({"DARK_QUARTZ", "INTERFACE", "MAGNETS"} & kami)
        for new in self.document["lang_files"].values():
            locale = new.rsplit("/", 1)[1][: -len(".json")]
            data = json.loads((self.resources / new).read_text(encoding="utf-8"))
            overridden = self.fragment_keys(locale)
            names = [k.removeprefix("ttresearch.name.") for k in data if k.startswith("ttresearch.name.")]
            lores = [k.removeprefix("ttresearch.lore.") for k in data if k.startswith("ttresearch.lore.")]
            for key in names:
                if f"tc.research_name.{key}" not in overridden:
                    self.assertEqual(data[f"ttresearch.name.{key}"], data[f"tc.research_name.{key}"], f"{locale} {key}")
            for key in lores:
                prefix = data["ttresearch.prefix.kami" if key in kami else "ttresearch.prefix"]
                if f"tc.research_text.{key}" not in overridden:
                    self.assertEqual(prefix + data[f"ttresearch.lore.{key}"], data[f"tc.research_text.{key}"],
                                     f"{locale} {key}")
            derived_names = {k.removeprefix("tc.research_name.") for k in data if k.startswith("tc.research_name.")}
            derived_texts = {k.removeprefix("tc.research_text.") for k in data if k.startswith("tc.research_text.")}
            self.assertEqual(set(names), derived_names - {k.removeprefix("tc.research_name.") for k in overridden
                                                            if k.startswith("tc.research_name.")}, locale)
            self.assertEqual(set(lores), derived_texts - {k.removeprefix("tc.research_text.") for k in overridden
                                                            if k.startswith("tc.research_text.")}, locale)

    def test_known_values(self):
        en = json.loads((self.resources / tt.NEW_LANG / "en_us.json").read_text(encoding="utf-8"))
        self.assertEqual("[TTKami]Nexus of destinations", en["tc.research_text.WARP_GATE"])
        self.assertEqual("[TT]Shadow of the Day", en["tc.research_text.DARK_QUARTZ"])
        self.assertEqual("Celestial Gateway", en["block.thaumictinkerer.warp_gate"])
        self.assertIn("30%% discount", en["ttresearch.page.CAP_ICHOR.0"])
        self.assertIn("ttresearch.page.INTERFACE.0", en)
        self.assertTrue((self.resources / tt.NEW_TEXTURES / "item/focus_xp_drain.png").is_file())
        self.assertTrue((self.resources / tt.NEW_TEXTURES / "block/levitational_locomotive_relay_0.png").is_file())


class PinnedSourceTest(unittest.TestCase):
    """The checked-in output is exactly what the script produces from the pinned checkout."""

    def test_check_against_the_pinned_checkout(self):
        if not tt.DEFAULT_SOURCE.is_dir():
            self.skipTest("Thaumic Tinkerer source checkout not present")
        try:
            tt.verify_pin(tt.DEFAULT_SOURCE)
        except tt.ConversionError as error:
            self.skipTest(str(error))
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = tt.main(["--check"])
        self.assertEqual(0, code, out.getvalue())


if __name__ == "__main__":
    unittest.main()
