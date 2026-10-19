#!/usr/bin/env python3
"""Extract TC4 definitions without loading or executing the legacy Java runtime.

Balanced argument parsing preserves nested item/page expressions as provenance,
not executable modern recipes. The original decompilation is the data oracle.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

RESEARCH_HASH = "397d620877d429c49d4db6ae65552a681ac5b26be5d60cd1e95e5e28cae9f40e"
PAGE_HASH = "66a10cd39a47e78472134a35d3b7b4e1b78618996ac6c87907aeb63303d4bf38"
SOURCE_HASH_POLICY = "Java source decoded as UTF-8 without BOM; CRLF and lone CR normalized to LF; other characters and final newline preserved"


def canonical_java_source_sha256(path: Path) -> str:
    source = path.read_bytes().decode("utf-8-sig").replace("\r\n", "\n").replace("\r", "\n")
    return hashlib.sha256(source.encode("utf-8")).hexdigest()


def mask_comments(source: str) -> str:
    out = list(source)
    i = 0
    quote = None
    while i < len(source):
        ch = source[i]
        if quote:
            if ch == "\\":
                i += 2
                continue
            if ch == quote:
                quote = None
        elif ch in ('"', "'"):
            quote = ch
        elif source.startswith("//", i) or source.startswith("/*", i):
            block = source.startswith("/*", i)
            end = source.find("*/", i + 2) + 2 if block else source.find("\n", i + 2)
            if end < i:
                end = len(source)
            for j in range(i, end):
                if source[j] != "\n":
                    out[j] = " "
            i = end
            continue
        i += 1
    return "".join(out)


def closing(source: str, start: int) -> int:
    pairs = {"(": ")", "[": "]", "{": "}"}
    stack = [pairs[source[start]]]
    quote = None
    i = start + 1
    while i < len(source):
        ch = source[i]
        if quote:
            if ch == "\\":
                i += 2
                continue
            if ch == quote:
                quote = None
        elif ch in ('"', "'"):
            quote = ch
        elif ch in pairs:
            stack.append(pairs[ch])
        elif ch in ")]}":
            if not stack or ch != stack.pop():
                raise ValueError(f"Mismatched Java delimiter at {i}")
            if not stack:
                return i
        i += 1
    raise ValueError(f"Unclosed Java expression at {start}")


def arguments(source: str) -> list[str]:
    parts, start, i = [], 0, 0
    while i < len(source):
        ch = source[i]
        if ch in "([{":
            i = closing(source, i) + 1
            continue
        if ch in ('"', "'"):
            quote = ch
            i += 1
            while i < len(source):
                if source[i] == "\\":
                    i += 2
                    continue
                if source[i] == quote:
                    break
                i += 1
        elif ch == ",":
            parts.append(source[start:i].strip())
            start = i + 1
        i += 1
    if source[start:].strip():
        parts.append(source[start:].strip())
    return parts


def java_string(value: str) -> str:
    return json.loads(value.strip())


def compact(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def calls(source: str, prefix: str):
    pattern = re.compile(prefix + r"\s*\(")
    for match in pattern.finditer(source):
        start = source.index("(", match.start())
        end = closing(source, start)
        yield match, arguments(source[start + 1:end]), end


def lexical_conditions(source: str, position: int) -> list[str]:
    # Preserve enclosing Java conditions verbatim. Their modern config/tag
    # meaning is a later explicit mapping, never guessed by this extractor.
    stack, boundary, i = [], 0, 0
    while i < position:
        ch = source[i]
        if ch in ('"', "'"):
            quote = ch
            i += 1
            while i < position:
                if source[i] == "\\":
                    i += 2
                    continue
                if source[i] == quote:
                    break
                i += 1
        elif ch == "{":
            header = compact(source[boundary:i])
            stack.append(header if re.match(r"(?:if|else|for|while|switch)\b", header) else None)
            boundary = i + 1
        elif ch == "}":
            if stack:
                stack.pop()
            boundary = i + 1
        elif ch == ";":
            boundary = i + 1
        i += 1
    return [condition for condition in stack if condition]


def extract_aspects(path: Path) -> list[dict]:
    source = mask_comments(path.read_text(encoding="utf-8-sig"))
    rows = []
    pattern = r"public\s+static\s+(?:final\s+)?Aspect\s+([A-Z_]+)\s*=\s*new\s+Aspect"
    for match, args, end in calls(source, pattern):
        symbol = match.group(1)
        tag, color = java_string(args[0]), int(args[1], 0)
        primal = args[2].startswith('"')
        components = [] if primal else re.findall(r"\b[A-Z][A-Z_]+\b", args[2].removeprefix("new Aspect[]"))
        rows.append({"symbol": symbol, "tag": tag, "color": color,
                     "components": components, "chatcolor": java_string(args[2]) if primal else None,
                     "blend": int(args[3], 0) if len(args) > 3 else 1,
                     "image": f"thaumcraft:textures/aspects/{tag}.png",
                     "source_line": source.count("\n", 0, match.start()) + 1})
    if len(rows) != 48 or sum(not row["components"] for row in rows) != 6:
        raise ValueError("Expected the original 48 aspects including six primals")
    symbols = {row["symbol"] for row in rows}
    if any(set(row["components"]) - symbols for row in rows):
        raise ValueError("Aspect references an unknown component")
    return rows


def aspect_cost(expression: str, symbols: dict[str, str]) -> dict[str, int]:
    costs = {}
    for match in re.finditer(r"\.add\(\s*Aspect\.([A-Z_]+)\s*,\s*(\d+)\s*\)", expression):
        tag = symbols[match.group(1)]
        costs[tag] = costs.get(tag, 0) + int(match.group(2))
    if "new AspectList" not in expression and not costs:
        raise ValueError(f"Unresolved research aspect expression: {expression}")
    return costs


def parse_pages(expression: str) -> list[dict]:
    pages = []
    for _, args, _ in calls(expression, r"new\s+ResearchPage"):
        if len(args) not in (1, 2) or len(args) == 2 and not all(arg.startswith('"') for arg in args):
            raise ValueError(f"Unexpected ResearchPage arguments: {args}")
        required = java_string(args[0]) if len(args) == 2 else None
        raw = args[-1]
        recipe = re.search(r'recipes\.get\("([^\"]+)"\)', raw)
        text = java_string(raw) if raw.startswith('"') else None
        pages.append({"kind": "text" if text else "recipe_reference" if recipe else "legacy_expression",
                      "text_key": text, "required_research": required,
                      "recipe_key": recipe.group(1) if recipe else None,
                      "java_expression": compact(", ".join(args))})
    return pages


def extract_research(paths: list[Path], symbols: dict[str, str]) -> list[dict]:
    rows, warps = [], {}
    for path in paths:
        source = mask_comments(path.read_text(encoding="utf-8-sig"))
        for _, args, _ in calls(source, r"ThaumcraftApi\.addWarpToResearch"):
            warps[java_string(args[0])] = int(args[1], 0)
        for match, args, end in calls(source, r"new\s+ResearchItem"):
            key, category = java_string(args[0]), java_string(args[1])
            stop = source.index(";", end)
            suffix = source[end + 1:stop]
            methods = {}
            # A fluent invocation can contain nested calls. Skip its complete
            # balanced arguments before looking for the next fluent invocation.
            cursor = 0
            while cursor < len(suffix):
                method = re.search(r"\.(set\w+|registerResearchItem)\s*\(", suffix[cursor:])
                if not method:
                    break
                opening = suffix.index("(", cursor + method.start())
                close = closing(suffix, opening)
                methods[method.group(1)] = suffix[opening + 1:close]
                cursor = close + 1
            flags = [name[3:] for name, arg in methods.items() if name.startswith("set") and not arg.strip()]
            full = len(args) == 7
            if not full and len(args) != 2:
                raise ValueError(f"Unsupported ResearchItem constructor for {key}: {args}")
            if not full:
                flags.append("Virtual")
            def strings(name):
                return [java_string(arg) for arg in arguments(methods.get(name, ""))]
            pages = parse_pages(methods.get("setPages", ""))
            row = {"key": key, "category": category,
                   "column": int(args[3]) if full else 0, "row": int(args[4]) if full else 0,
                   "complexity": max(1, min(3, int(args[5]))) if full else 0,
                   "aspect_cost": aspect_cost(args[2], symbols) if full else {},
                   "flags": sorted(set(flags)), "parents": strings("setParents"),
                   "parents_hidden": strings("setParentsHidden"), "siblings": strings("setSiblings"),
                   "aspect_triggers": [symbols[symbol] for symbol in re.findall(r"Aspect\.([A-Z_]+)", methods.get("setAspectTriggers", ""))],
                   "entity_triggers": strings("setEntityTriggers"),
                   "item_trigger_expressions": [compact(v) for v in arguments(methods.get("setItemTriggers", ""))],
                   "icon_expression": compact(args[6]) if full else None,
                   "pages": pages, "conditions": lexical_conditions(source, match.start()),
                   "warp": 0,
                   "source": {"file": path.name, "line": source.count("\n", 0, match.start()) + 1,
                              "expression_sha256": hashlib.sha256(compact(source[match.start():stop]).encode()).hexdigest()}}
            if "setComplexity" in methods:
                row["complexity"] = int(methods["setComplexity"])
            rows.append(row)
    for row in rows:
        row["warp"] = warps.get(row["key"], 0)
    if len({row["key"] for row in rows}) != len(rows):
        raise ValueError("Duplicate research key in donor")
    return sorted(rows, key=lambda row: row["key"])


def fingerprint(lines: set[str]) -> str:
    return hashlib.sha256(("\n".join(sorted(lines)) + "\n").encode()).hexdigest()


def corpus(rows: list[dict]) -> dict:
    pairs = {f'{row["key"]}|{row["category"]}' for row in rows}
    # Retain the existing 294-key fingerprint's precise definition: first-string
    # text constructors. Fifteen additional gated text pages are exported too.
    pages = {page["text_key"] for row in rows for page in row["pages"] if page["text_key"] and not page["required_research"] and page["text_key"].startswith("tc.research_page.")}
    return {"research_count": len(rows), "research_key_category_sha256": fingerprint(pairs),
            "text_page_count": len(pages), "text_page_sha256": fingerprint(pages)}


def extract_object_bindings(path: Path, symbols: dict[str, str]) -> list[dict]:
    source = mask_comments(path.read_text(encoding="utf-8-sig"))
    renames = {
        "Blocks.GRASS": ["grass_block"], "Blocks.LOG": ["oak_log"],
        "Blocks.LOG2": ["acacia_log"], "Blocks.PLANKS": ["oak_planks"],
        "Blocks.LEAVES": ["oak_leaves"], "Blocks.LEAVES2": ["acacia_leaves"],
        "Blocks.SAPLING": ["oak_sapling"], "Blocks.QUARTZ_ORE": ["nether_quartz_ore"],
        "Blocks.MELON_BLOCK": ["melon"], "Blocks.WATERLILY": ["lily_pad"],
        "Blocks.YELLOW_FLOWER": ["dandelion"], "Blocks.RED_FLOWER": ["poppy"],
        "Blocks.DEADBUSH": ["dead_bush"], "Blocks.SNOW": ["snow_block"],
        "Blocks.WOOL": ["white_wool"], "Blocks.LIT_FURNACE": ["furnace"],
        "Blocks.LIT_REDSTONE_LAMP": ["redstone_lamp"],
        "Items.FISH": ["cod"], "Items.COOKED_FISH": ["cooked_cod"],
        "Items.SPECKLED_MELON": ["glistering_melon_slice"],
        "Items.RECORD_FAR": ["music_disc_far"], "Items.DYE": ["black_dye"],
    }
    removed = {"Blocks.WATER", "Blocks.FLOWING_WATER", "Blocks.LAVA", "Blocks.FLOWING_LAVA"}
    rows = []
    for match, args, _ in calls(source, r"ThaumcraftApi\.(registerObjectTag|registerComplexObjectTag)"):
        target, aspects_expr = args[0], args[-1]
        stack = list(calls(target, r"new\s+ItemStack"))
        symbol, meta = None, None
        if stack:
            stack_args = stack[0][1]
            symbol = stack_args[0].strip()
            raw_meta = stack_args[2] if len(stack_args) > 2 else "0"
            meta = int(raw_meta) if re.fullmatch(r"-?\d+", raw_meta) else "wildcard" if raw_meta == "OreDictionary.WILDCARD_VALUE" else None
        resolved, operations, ids = True, {}, []
        if not re.match(r"^new AspectList\(\)", aspects_expr):
            resolved = False
        for method in re.finditer(r"\.(add|remove|merge)\s*\(", aspects_expr):
            opening = aspects_expr.index("(", method.start())
            close = closing(aspects_expr, opening)
            op_args = arguments(aspects_expr[opening + 1:close])
            aspect = re.fullmatch(r"Aspect\.([A-Z_]+)", op_args[0]) if op_args else None
            if not aspect or len(op_args) > 2 or len(op_args) == 2 and not re.fullmatch(r"\d+", op_args[1]):
                resolved = False
                continue
            tag = symbols[aspect.group(1)]
            amount = int(op_args[1]) if len(op_args) == 2 else None
            op = method.group(1)
            if op == "add" and amount is not None:
                operations[tag] = operations.get(tag, 0) + amount
            elif op == "merge" and amount is not None:
                operations[tag] = max(operations.get(tag, 0), amount)
            elif op == "remove":
                if amount is None:
                    operations.pop(tag, None)
                else:
                    left = operations.get(tag, 0) - amount
                    if left > 0:
                        operations[tag] = left
                    else:
                        operations.pop(tag, None)
            else:
                resolved = False
        if symbol and re.fullmatch(r"(?:Items|Blocks)\.[A-Z_]+", symbol) and symbol not in removed and meta is not None:
            if symbol == "Blocks.STONE" and meta in (1, 3, 5):
                ids = [{1: "granite", 3: "diorite", 5: "andesite"}[meta]]
            elif symbol == "Blocks.TALLGRASS" and meta == 1:
                ids = ["short_grass", "fern"] if len(args) == 3 else ["short_grass"]
            elif symbol == "Blocks.DOUBLE_PLANT" and meta == "wildcard":
                ids = ["sunflower", "lilac", "tall_grass", "large_fern", "rose_bush", "peony"]
            elif symbol == "Blocks.RED_FLOWER" and meta == "wildcard":
                ids = ["poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy"]
            elif symbol == "Items.COAL" and meta == 1:
                ids = ["charcoal"]
            elif symbol == "Items.DYE" and meta not in (0, "wildcard"):
                ids = []
            elif meta in (0, "wildcard"):
                ids = renames.get(symbol, [symbol.split(".")[1].lower()])
        rows.append({"registration": match.group(1), "legacy_target": symbol or compact(target),
                     "legacy_meta": meta, "modern_ids": ["minecraft:" + item for item in ids],
                     "id_validation": "requires_target_registry_check" if ids else "unmapped",
                     "aspects": operations if resolved and match.group(1) == "registerObjectTag" else None,
                     "literal_partial_aspects": operations,
                     "aspect_expression": compact(aspects_expr),
                     "target_expression": compact(target),
                     "conditions": lexical_conditions(source, match.start()),
                     "source_line": source.count("\n", 0, match.start()) + 1})
    return rows


def main() -> None:
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--oracle", type=Path, default=root.parent / "work/thaumcraft-4.2.3.5-oracle")
    parser.add_argument("--output-dir", type=Path, default=root / "common/src/main/resources/data/thaumcraft/tc4")
    parser.add_argument("--check", action="store_true", help="Verify existing generated files without writing")
    args = parser.parse_args()
    aspect_path = args.oracle / "thaumcraft/api/aspects/Aspect.java"
    research_path = args.oracle / "thaumcraft/common/config/ConfigResearch.java"
    aspects = extract_aspects(aspect_path)
    symbols = {row["symbol"]: row["tag"] for row in aspects}
    research = extract_research([research_path], symbols)
    invariant = corpus(research)
    if invariant != {"research_count": 201, "research_key_category_sha256": RESEARCH_HASH,
                     "text_page_count": 294, "text_page_sha256": PAGE_HASH}:
        raise ValueError(f"Original research corpus changed: {invariant}")
    native_paths = sorted((root / "src/main/java/thaumcraft/common/config/research").glob("ConfigResearch*.java"))
    native = extract_research(native_paths, symbols)
    if corpus(native) != invariant:
        raise ValueError("Native 1.12 research corpus differs from the pinned original")
    metadata_fields = ["category", "column", "row", "complexity", "aspect_cost", "flags",
                       "parents", "parents_hidden", "siblings", "aspect_triggers", "entity_triggers", "warp"]
    native_by_key = {row["key"]: row for row in native}
    metadata_differences = {row["key"]: [field for field in metadata_fields
        if row[field] != native_by_key[row["key"]][field]] for row in research}
    metadata_differences = {key: fields for key, fields in metadata_differences.items() if fields}
    if metadata_differences:
        raise ValueError(f"Native research metadata differs from the original: {metadata_differences}")
    keys = {row["key"] for row in research}
    if any(set(row["parents"] + row["parents_hidden"] + row["siblings"]) - keys for row in research):
        raise ValueError("Research references unknown keys")
    provenance = {"source": "Thaumcraft-1.7.10-4.2.3.5.jar CFR source", "schema_version": 1,
                  "source_hash_policy": SOURCE_HASH_POLICY,
                  "source_sha256": {"Aspect.java": canonical_java_source_sha256(aspect_path),
                                    "ConfigResearch.java": canonical_java_source_sha256(research_path)},
                  "native_reference_commit": "4bc68454b1063cdc1255f833dd537e0fd4fc341d",
                  "native_corpus_checked": True, "native_metadata_fields_checked": metadata_fields}
    bindings_path = root / "src/main/java/thaumcraft/common/config/ConfigAspects.java"
    bindings = extract_object_bindings(bindings_path, symbols)
    products = {"aspects.json": {**provenance, "aspects": aspects},
                "research.json": {**provenance, "invariants": invariant,
                                  "expression_policy": "Legacy expressions are provenance only; they are not modern executable recipes or item ids.",
                                  "research": research},
                "object_aspects.json": {"schema_version": 1,
                    "source": "Audited native Forge 1.12.2 direct ConfigAspects registrations",
                    "source_hash_policy": SOURCE_HASH_POLICY,
                    "source_sha256": canonical_java_source_sha256(bindings_path),
                    "source_commit": "4bc68454b1063cdc1255f833dd537e0fd4fc341d",
                    "policy": "Only resolved literal aspect lists may be used. Check mapped ids in the target registry. Unmapped or dynamic expressions are not guessed.",
                    "bindings": bindings}}
    for name, payload in products.items():
        path = args.output_dir / name
        content = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
        if args.check:
            if not path.exists() or path.read_text(encoding="utf-8") != content:
                raise ValueError(f"Generated data is stale: {path}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8", newline="\n")
    print(json.dumps({"aspects": len(aspects), **invariant, "mode": "check" if args.check else "write"}))


if __name__ == "__main__":
    main()
