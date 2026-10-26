#!/usr/bin/env python3
"""The remaining original ConfigAspects rows and the aspect families of the inference.

The data checks always run. The bytecode replay needs the MCP stable 12-1.7.10 mapping, which is not
committed: TC4_MCP_DIR (default /tmp/mcp-stable) names its folder, and the replay is skipped without it.
"""
import json
import os
import re
import shutil
import unittest
from pathlib import Path

import extract_config_aspects as extract

ROOT = Path(__file__).resolve().parents[2]
MCP = Path(os.environ.get("TC4_MCP_DIR", "/tmp/mcp-stable"))


def aspect_tags():
    source = extract.ASPECT_SOURCE.read_text(encoding="utf-8")
    return set(re.findall(r'public static final Aspect [A-Z_]+ = new Aspect\("(\w+)"', source))


class CommittedData(unittest.TestCase):
    def setUp(self):
        self.document = json.loads(extract.OUT.read_text(encoding="utf-8"))
        self.early = json.loads(extract.EARLY.read_text(encoding="utf-8"))["bindings"]

    def test_rows_and_families_keep_the_jar_order_and_name_real_aspects(self):
        tags = aspect_tags()
        for part in ("rows", "families"):
            lines = [row["original_jar_line"] for row in self.document[part]]
            self.assertEqual(lines, sorted(lines), part)
            for row in self.document[part]:
                self.assertIn(row["registration"], ("direct", "complex"))
                for aspect, amount in row["aspects"].items():
                    self.assertIn(aspect, tags)
                    self.assertGreater(amount, 0)

    def test_no_row_repeats_an_early_id(self):
        early = {i for row in self.early for i in row["modern_ids"]}
        for row in self.document["rows"]:
            self.assertFalse(early & set(row["modern_ids"]), row["original_target"])

    def test_every_family_has_its_tag_file(self):
        for family in self.document["families"]:
            namespace, path = family["tag"].split(":", 1)
            self.assertEqual("thaumcraft", namespace)
            tag = ROOT / "common/src/main/resources/data/thaumcraft/tags/item" / f"{path}.json"
            self.assertTrue(tag.is_file(), tag)
            values = json.loads(tag.read_text(encoding="utf-8"))["values"]
            for value in values:
                if isinstance(value, dict):
                    self.assertFalse(value["required"], f"{tag}: a convention tag must stay optional")

    def test_the_inference_table_names_an_item_once(self):
        table = json.loads((ROOT / "common/src/main/resources/data/thaumcraft/port/aspect_inference.json").read_text(encoding="utf-8"))
        self.assertFalse(set(table["like"]) & set(table["aspects"]))
        for item, parent in table["like"].items():
            self.assertNotEqual(item, parent)
            self.assertNotIn(parent, table["like"], f"{item} -> {parent} must name a resolved item, not another form")
        tags = aspect_tags()
        for item, aspects in table["aspects"].items():
            for aspect, amount in aspects.items():
                self.assertIn(aspect, tags, item)
                self.assertGreater(amount, 0, item)


@unittest.skipUnless((MCP / "fields.csv").is_file() and shutil.which("javap") and extract.JAR.is_file(),
                     "the MCP 1.7.10 mapping, javap or the original JAR is not available")
class BytecodeReplay(unittest.TestCase):
    def test_the_committed_files_are_what_the_jar_gives(self):
        document, tags = extract.build(MCP)
        self.assertEqual(json.loads(extract.OUT.read_text(encoding="utf-8")), document)
        for name, tag in tags.items():
            self.assertEqual(json.loads((extract.TAGS / f"{name}.json").read_text(encoding="utf-8")), tag, name)


if __name__ == "__main__":
    unittest.main()
