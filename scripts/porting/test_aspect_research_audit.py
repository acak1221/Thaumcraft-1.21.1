#!/usr/bin/env python3
"""Independent original-oracle audit; no Minecraft/legacy Java execution required.

TC4_ORACLE overrides the private oracle directory. --write-fixture refreshes the
small, data-only fixture consumed by the native Java behavioral tests.
"""
import copy
import json
import os
from pathlib import Path
import re
import sys
import unittest

import extract_tc4_definitions as extract

ROOT = Path(__file__).resolve().parents[2]
ORACLE = Path(os.environ.get("TC4_ORACLE", "/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle"))
DATA = ROOT / "common/src/main/resources/data/thaumcraft/tc4"
FIXTURE = ROOT / "common/src/test/resources/thaumcraft/aspect_research_oracle.json"
GRAPH_FIELDS = ("category", "column", "row", "parents", "parents_hidden", "siblings")


def closure(rows):
    reached = set()
    while True:
        unlocked = {r["key"] for r in rows
                    if set(r["parents"] + r["parents_hidden"]) <= reached}
        if unlocked <= reached:
            return reached
        reached |= unlocked


def originals():
    aspects = extract.extract_aspects(ORACLE / "thaumcraft/api/aspects/Aspect.java")
    symbols = {r["symbol"]: r["tag"] for r in aspects}
    research = extract.extract_research([ORACLE / "thaumcraft/common/config/ConfigResearch.java"], symbols)
    return aspects, symbols, research


def literal_samples(symbols):
    """Identifiers come from explicit flattening; amounts come ONLY from the oracle.

    Dynamic lists and complex recipe bases are deliberately not treated as literals.
    Verify the line's target too: matching an amount on the wrong item isn't proof.
    """
    source = extract.mask_comments((ORACLE / "thaumcraft/common/config/ConfigAspects.java").read_text())
    registrations = {source.count("\n", 0, match.start()) + 1: args
                     for match, args, _ in extract.calls(source, r"ThaumcraftApi\.registerObjectTag")}
    samples, errors = {}, []
    for row in json.loads((DATA / "early_object_aspects.json").read_text())["bindings"]:
        if row["registration"] != "direct":
            continue
        args = registrations.get(row["original_source_line"])
        if args is None:
            errors.append(f"No direct registration at line {row['original_source_line']}: {row['modern_ids']}")
            continue
        if not re.fullmatch(r"new AspectList\(\)(?:\.add\(Aspect\.[A-Z_]+, \d+\))*", args[-1]):
            continue
        target = row["original_target"]
        if "." in target:
            if target not in args[0]:
                errors.append(f"Wrong oracle target at line {row['original_source_line']}: {row['modern_ids']}: {args[0]}")
                continue
        elif target.startswith("ore_dictionary:") and not args[0].startswith("dyes["):
            if args[0] != json.dumps(target.split(":", 1)[1]):
                errors.append(f"Wrong ore dictionary target: {target}: {args[0]}")
                continue
        amounts = extract.aspect_cost(args[-1], symbols)
        if amounts != row["aspects"]:
            errors.append(f"Tag mismatch: {row['modern_ids']}: oracle={amounts}, native={row['aspects']}")
            continue
        for item in row["modern_ids"]:
            samples[item] = {"aspects": amounts, "source_line": row["original_source_line"]}
    assert not errors, "\n".join(errors)
    return samples


def fixture():
    aspects, symbols, _ = originals()
    return {"source": "TC4 4.2.3.5 private oracle; explicit native identifier flattening only",
            "config_aspects_sha256": extract.canonical_java_source_sha256(ORACLE / "thaumcraft/common/config/ConfigAspects.java"),
            "tags": literal_samples(symbols),
            "combinations": {r["tag"]: [symbols[c] for c in r["components"]]
                             for r in aspects if r["components"]}}


@unittest.skipUnless((Path(os.environ.get("TC4_ORACLE", "/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle")) / "thaumcraft").is_dir(),
                     "the private TC4 oracle is not present; this audit compares against it")
class AspectResearchAudit(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.aspects, cls.symbols, cls.original = originals()
        cls.bundled = json.loads((DATA / "research.json").read_text())["research"]

    def test_all_201_graph_nodes_match_original_and_legacy(self):
        legacy = extract.extract_research(sorted((ROOT / "src/main/java/thaumcraft/common/config/research").glob("ConfigResearch*.java")), self.symbols)
        for rows in (self.bundled, legacy):
            self.assertEqual(201, len(rows))
            by_key = {r["key"]: r for r in rows}
            self.assertEqual({r["key"] for r in self.original}, set(by_key))
            for original in self.original:
                for field in GRAPH_FIELDS:
                    self.assertEqual(original[field], by_key[original["key"]][field], (original["key"], field))
        # Full original page descriptors (not merely the set of text keys).
        for original, bundled in zip(self.original, self.bundled):
            self.assertEqual(original["key"], bundled["key"])
            self.assertEqual(original["pages"], bundled["pages"], original["key"])

    def test_every_node_reachable_and_nonvirtual_positions_unique(self):
        keys = {r["key"] for r in self.bundled}
        self.assertEqual(set(), keys - closure(self.bundled))
        positions = {}
        for row in self.bundled:
            if "Virtual" in row["flags"]:
                continue
            position = (row["category"], row["column"], row["row"])
            self.assertNotIn(position, positions, (positions.get(position), row["key"]))
            positions[position] = row["key"]

    def test_closure_detects_a_missing_parent_and_a_cycle(self):
        missing = copy.deepcopy(self.bundled)
        missing[0]["parents"] = ["DOES_NOT_EXIST"]
        self.assertNotIn(missing[0]["key"], closure(missing))
        cycle = copy.deepcopy(self.bundled)
        cycle[0]["parents"] = [cycle[1]["key"]]
        cycle[1]["parents"] = [cycle[0]["key"]]
        self.assertFalse({cycle[0]["key"], cycle[1]["key"]} & closure(cycle))

    def test_at_least_80_native_literal_tags_match_original(self):
        self.assertGreaterEqual(len(literal_samples(self.symbols)), 80)

    def test_all_combination_components_and_multiplicities_match(self):
        native = extract.extract_aspects(ROOT / "common/src/main/java/thaumcraft/api/aspects/Aspect.java")
        expected = {r["tag"]: r["components"] for r in self.aspects}
        actual = {r["tag"]: r["components"] for r in native}
        self.assertEqual(expected, actual)
        self.assertEqual(42, sum(bool(r["components"]) for r in native))
        self.assertTrue(all(len(r["components"]) == 2 for r in native if r["components"]))

    def test_native_behavior_fixture_is_current(self):
        self.assertEqual(fixture(), json.loads(FIXTURE.read_text()))


if __name__ == "__main__":
    if "--write-fixture" in sys.argv:
        FIXTURE.parent.mkdir(parents=True, exist_ok=True)
        FIXTURE.write_text(json.dumps(fixture(), indent=2) + "\n")
        print(FIXTURE)
    else:
        unittest.main()
