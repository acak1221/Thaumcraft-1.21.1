#!/usr/bin/env python3
"""The table of the original TC4 field names -> the native 1.21.1 ids and its generated reference.

The data checks always run. The bytecode replay needs the original JAR, which is not committed:
TC4_ORIGINAL_JAR names it (by default the repository root or the root of the main checkout), and the
replay is skipped without it or without javap.
"""
import contextlib
import io
import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
import extract_legacy_ids as extract  # noqa: E402

JAR = extract.default_jar()


def load():
    return json.loads(extract.OUT.read_text(encoding="utf-8"))


class CommittedData(unittest.TestCase):
    def setUp(self):
        self.document = load()
        self.rows = {(r["field"], r["meta"]): r for r in self.document["rows"]}

    def test_the_reference_is_generated_from_the_table(self):
        self.assertEqual(extract.render_doc(self.document), extract.DOC.read_text(encoding="utf-8"))

    def test_the_static_checks_pass(self):
        self.assertEqual([], extract.static_problems(self.document))

    def test_every_field_has_its_rows_and_one_wildcard(self):
        fields = [f["field"] for f in self.document["fields"]]
        self.assertEqual(len(fields), len(set(fields)))
        self.assertEqual(set(fields), set(extract.MAPPING))
        for field in fields:
            metas = [r["meta"] for r in self.document["rows"] if r["field"] == field]
            self.assertEqual(metas, sorted(metas), field)
            self.assertEqual(len(metas), len(set(metas)), field)
            self.assertEqual(extract.WILDCARD, metas[-1], field)
            self.assertGreater(len(metas), 1, field)

    def test_the_counts_add_up(self):
        counts = self.document["counts"]
        rows = [r for r in self.document["rows"] if r["meta"] != extract.WILDCARD]
        self.assertEqual(counts["rows"], len(rows))
        self.assertEqual(counts["mapped"] + counts["null"], counts["rows"])
        self.assertEqual(counts["wildcards"], counts["fields"])
        self.assertEqual(counts["item_fields"] + counts["block_fields"], counts["fields"])
        for row in rows:
            if row["id"] is None:
                self.assertTrue(row.get("note"), f"{row['field']}:{row['meta']} needs a reason")
            self.assertTrue(row["evidence"], f"{row['field']}:{row['meta']} needs evidence")

    def test_the_script_mapping_is_what_the_table_holds(self):
        # A change of MAPPING must be written back: the JAR is only needed for the evidence.
        for field, spec in extract.MAPPING.items():
            if not isinstance(spec, dict):
                continue
            for meta, entry in spec.items():
                row = self.rows[(field, meta)]
                self.assertEqual(entry["id"], row["id"], f"{field}:{meta}")
                self.assertEqual(entry.get("note"), row.get("note"), f"{field}:{meta}")
                self.assertEqual(entry.get("component"), row.get("component"), f"{field}:{meta}")
                if row["kind"] == "block":
                    self.assertEqual(entry.get("state") or None, row.get("state"), f"{field}:{meta}")
                    held = entry.get("item", extract.SAME)
                    self.assertEqual(entry["id"] if held is extract.SAME else extract.ns(held), row["item"],
                                     f"{field}:{meta}")

    def test_spot_checks(self):
        self.assertEqual("thaumcraft:thaumium_ingot", self.rows[("itemResource", 2)]["id"])
        self.assertEqual("Thaumium Ingot", self.rows[("itemResource", 2)]["name"])
        self.assertEqual("thaumcraft:balance_shard", self.rows[("itemShard", 6)]["id"])
        self.assertEqual("thaumcraft:jar", self.rows[("blockJar", 0)]["id"])
        self.assertEqual("Warded Jar", self.rows[("blockJar", 0)]["name"])
        self.assertEqual("thaumcraft:thaumium_block", self.rows[("blockCosmeticSolid", 4)]["id"])
        self.assertEqual({"variant": "1"}, self.rows[("blockCosmeticSolid", 1)]["state"])
        self.assertEqual("thaumcraft:obsidian_tile", self.rows[("blockCosmeticSolid", 1)]["item"])
        self.assertEqual("minecraft:iron_nugget", self.rows[("itemNugget", 0)]["id"])
        self.assertEqual("Config.foundTinIngot", self.rows[("itemNugget", 18)]["condition"])
        self.assertIsNone(self.rows[("itemWandCap", 4)]["id"])
        self.assertEqual(7, len(self.rows[("itemShard", extract.WILDCARD)]["ids"]))
        self.assertEqual("thaumcraft:inkwell", self.rows[("itemInkwell", extract.WILDCARD)]["id"])

    def test_the_existing_partial_maps_agree(self):
        self.assertEqual([], extract.reconcile(self.document["rows"]))
        icons = extract.icon_map()
        self.assertGreater(len(icons), 150)
        self.assertEqual("thaumium_ingot", icons[("itemResource", 2)])

    def test_check_fails_for_a_stale_reference(self):
        with tempfile.TemporaryDirectory() as temporary:
            stale = Path(temporary) / "LEGACY_IDS.md"
            stale.write_text(extract.DOC.read_text(encoding="utf-8").replace("Warded Jar", "Warded Jar (edited)"),
                             encoding="utf-8")
            with mock.patch.object(extract, "DOC", stale), mock.patch.object(extract, "ROOT", Path(temporary)), \
                    mock.patch.object(sys, "argv", ["extract_legacy_ids.py", "--check", "--doc-only"]), \
                    contextlib.redirect_stdout(io.StringIO()) as out:
                self.assertEqual(1, extract.main())
            self.assertIn("stale", out.getvalue())
            with mock.patch.object(sys, "argv", ["extract_legacy_ids.py", "--check", "--doc-only"]), \
                    contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, extract.main())


@unittest.skipUnless(JAR.is_file() and shutil.which("javap"), "the original JAR or javap is not available")
class BytecodeReplay(unittest.TestCase):
    def test_the_committed_table_is_what_the_jar_gives(self):
        document = extract.build(JAR)
        self.assertEqual(load(), json.loads(extract.dump(document)))
        self.assertEqual(extract.dump(document), extract.OUT.read_text(encoding="utf-8"))

    def test_every_original_field_is_accounted_for(self):
        import legacy_bytecode as bytecode
        classes = bytecode.disassemble(JAR)
        declared = {name for owner in (bytecode.CONFIG_ITEMS, bytecode.CONFIG_BLOCKS)
                    for name, _kind in bytecode.declared_fields(classes[owner])}
        self.assertEqual(declared, {f["field"] for f in load()["fields"]})
        # The interpreter reads the switches of the other-mod metals and the concrete loops.
        method = bytecode.find_method(classes, "thaumcraft/common/items/ItemNugget", bytecode.GET_SUB_ITEMS)[1]
        found = {s.meta: s.condition for s in bytecode.run_subtypes(method, "itemNugget")}
        self.assertEqual({0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 31}, set(found))
        self.assertEqual(("foundCopperIngot",), found[17])
        resource = bytecode.find_method(classes, "thaumcraft/common/items/ItemResource", bytecode.GET_SUB_ITEMS)[1]
        self.assertEqual(set(range(19)) - {5}, {s.meta for s in bytecode.run_subtypes(resource, "itemResource")})


if __name__ == "__main__":
    unittest.main()
