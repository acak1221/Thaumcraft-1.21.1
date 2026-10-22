#!/usr/bin/env python3
"""Read-only independent oracle comparisons; no Gradle or game launch.

Run from the repository root with the private 4.2.3.5 oracle path as argv[1].
This deliberately does not treat legacy-derived aspect data as original evidence.
"""
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
ORACLE = Path(sys.argv[1])
JAVA = ROOT / "common/src/main/java"
DATA = ROOT / "common/src/main/resources/data/thaumcraft"


def declarations(text):
    return {
        name: (tag, int(color, 0), re.findall(r"\b[A-Z_]+\b", tail))
        for name, tag, color, tail in re.findall(
            r'public static final Aspect (\w+) = new Aspect\("([^"]+)", '
            r'(0x[\da-fA-F]+|\d+), (.*)\);', text)
    }


original = declarations((ORACLE / "thaumcraft/api/aspects/Aspect.java").read_text())
native = declarations((JAVA / "thaumcraft/api/aspects/Aspect.java").read_text())
assert len(original) == len(native) == 48
assert original == native, "Aspect tag/colour/parent/blend declaration differs"
assert list(original) == list(native), "Aspect registration order differs"
tags = {name: value[0] for name, value in original.items()}


def amounts(line):
    result = {}
    for name, count in re.findall(r'\.(?:add|merge)\(Aspect\.(\w+), (\d+)\)', line):
        tag = tags[name]
        result[tag] = result.get(tag, 0) + int(count)
    return result


aspect_lines = (ORACLE / "thaumcraft/common/config/ConfigAspects.java").read_text().splitlines()
entities = json.loads((DATA / "tc4/entity_aspects.json").read_text())["entries"]
entity_source = (JAVA / "thaumcraft/common/lib/research/EntityAspectBindings.java").read_text()
name_map = dict(re.findall(r'Map.entry\("([^"]+)", "([^"]+)"\)', entity_source))
entity_results = []
for row in entities:
    line_number = row["source_line"]
    source = aspect_lines[line_number - 1]
    assert '"' + row["original_name"] + '"' in source
    assert row["aspects"] == amounts(source), row
    if "byte_condition" in row:
        condition = row["byte_condition"]
        assert f'"{condition["name"]}", (byte){condition["value"]}' in source
    entity_results.append({"name": row["original_name"], "oracle_line": line_number})
assert len(entities) == 69
assert not ({row["original_name"] for row in entities} - set(name_map.values())), "Unreachable entity tags"

# Pin the concrete native IDs too: testing forOriginal alone missed this defect.
expected_names = {
    "thaum_slime": "ThaumSlime", "tainted_pig": "TaintedPig",
    "tainted_sheep": "TaintedSheep", "tainted_cow": "TaintedCow",
    "tainted_chicken": "TaintedChicken", "tainted_villager": "TaintedVillager",
    "taint_creeper": "TaintedCreeper", "cultist_knight": "CultistKnight",
    "cultist_cleric": "CultistCleric",
}
for identifier, original_name in expected_names.items():
    assert name_map["thaumcraft:" + identifier] == "Thaumcraft." + original_name

recipe_lines = (ORACLE / "thaumcraft/common/config/ConfigRecipes.java").read_text().splitlines()
literal_recipes = []
for number, source in enumerate(recipe_lines, 1):
    if "addCrucibleRecipe" not in source or '"BalancedShard_' in source:
        continue
    key, research = re.search(r'recipes.put\("([^"]+)", ThaumcraftApi.addCrucibleRecipe\("([^"]+)"', source).groups()
    # Count is the second argument of the first (output) ItemStack, defaulting to 1.
    stack_args = re.search(r'new ItemStack\(([^)]+)\)', source).group(1).split(",")
    count = int(stack_args[1]) if len(stack_args) > 1 else 1
    literal_recipes.append((number, key, research, count, amounts(source)))

recipes = []
for path in sorted((DATA / "recipe/crucible").glob("*.json")):
    row = json.loads(path.read_text())
    if path.stem.startswith("balanced_shard_"):
        order = ["aer", "ignis", "aqua", "terra", "ordo", "perditio"]
        assert row["aspects"] == {tag: 2 for i, tag in enumerate(order) if i != row["source_order"]}
        assert row["research"] == "CRUCIBLE" and row["result"]["count"] == 1
        number, key = 113, "BalancedShard_" + str(row["source_order"])
    else:
        candidates = [r for r in literal_recipes if r[2] == row["research"] and r[4] == row["aspects"]]
        assert candidates, (path, "no original cost/research match")
        # Preserve disambiguation for the several shared duplication/golem costs.
        if row["source_order"] >= 159:
            candidates = [r for r in candidates if r[0] == row["source_order"]]
        else:
            # First wave uses the original unconditional order, including expanded balanced shards.
            unconditional = [r for r in literal_recipes if r[1] not in {
                "PureTin", "PureSilver", "PureLead", "TransTin", "TransSilver", "TransLead"}]
            candidates = [r for r in candidates if unconditional.index(r) + 6 == row["source_order"]]
        assert len(candidates) == 1, path
        number, key, _, count, _ = candidates[0]
        assert row["result"]["count"] == count, path
    assert "catalyst" in row, path
    recipes.append({"native": str(path.relative_to(ROOT)), "oracle_line": number, "key": key})
assert len(recipes) == 48


def golem_rows(text):
    return [(name, fields.lower().replace(" ", "")) for name, fields in
            re.findall(r'^\s+([A-Z]+)\(([^)]+)\)[,;]', text, re.M)]


golem_path = "thaumcraft/common/entities/golems/EnumGolemType.java"
golems = golem_rows((ORACLE / golem_path).read_text())
assert len(golems) == 8
assert golems == golem_rows((JAVA / golem_path).read_text())

print(json.dumps({
    "aspect_declarations": 48, "compound_recipes": 42, "primal_aspects": 6,
    "entity_declarations": entity_results, "crucible_recipe_field_checks": recipes,
    "golem_numeric_values": 56, "golem_boolean_values": 8,
    "native_entity_names_regression": expected_names,
    "limits": "Static comparisons only. Recipe ingredient identities, research page text, and runtime scans are not certified."
}, indent=2))
