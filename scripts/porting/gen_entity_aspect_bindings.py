#!/usr/bin/env python3
"""Extract TC4's closed entity-aspect data; reads the immutable oracle outside this repository."""
import argparse
import hashlib
import json
import re
from pathlib import Path


def extract(source: Path, aspects: Path) -> dict:
    text = source.read_text(encoding="utf-8-sig").replace("\r\n", "\n").replace("\r", "\n")
    symbols = {row["symbol"]: row["tag"] for row in json.loads(aspects.read_text())["aspects"]}
    entries = []
    for number, line in enumerate(text.splitlines(), 1):
        match = re.search(r'ThaumcraftApi\.registerEntityTag\("([^"\n]+)", (.*), new ThaumcraftApi\.EntityTagsNBT', line)
        if not match:
            continue
        name, expression = match.groups()
        values = {}
        for symbol, amount in re.findall(r'\.add\(Aspect\.([A-Z_]+), (\d+)\)', expression):
            tag = symbols[symbol]
            values[tag] = values.get(tag, 0) + int(amount)
        row = {"original_name": name, "aspects": values, "source_line": number}
        if ".add(tag," in expression:
            row["dynamic_wisp_type"] = True
        condition = re.search(r'EntityTagsNBT\("([^"]+)", \(byte\)(\d+)\)', line)
        if condition:
            row["byte_condition"] = {"name": condition[1], "value": int(condition[2])}
        entries.append(row)
    if not entries or entries[-1]["original_name"] != "Thaumcraft.Golem":
        raise ValueError("Missing original entity registration tail")
    return {"schema_version": 1, "source": "TC4 4.2.3.5 ConfigAspects.registerEntityAspects",
            "source_sha256": hashlib.sha256(text.encode()).hexdigest(), "entries": entries}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--oracle", type=Path, required=True)
    parser.add_argument("--aspects", type=Path, default=Path("common/src/main/resources/data/thaumcraft/tc4/aspects.json"))
    parser.add_argument("--output", type=Path, default=Path("common/src/main/resources/data/thaumcraft/tc4/entity_aspects.json"))
    args = parser.parse_args()
    data = extract(args.oracle / "thaumcraft/common/config/ConfigAspects.java", args.aspects)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    print(f"Extracted {len(data['entries'])} original entity registrations")
