#!/usr/bin/env python3
"""Refresh a metadata-only original/legacy scope ledger, preserving authored reviews.

The private TC4 oracle is read only. A matching Java path is a lexical relation,
not a semantic verdict; neither hashes nor native links establish parity.
Run --write after refreshing class-inventory.json, then --check. --self-test uses
temporary synthetic sources and does not need or copy the private decompilation.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import tempfile
from collections import Counter
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[2]
ORIGINAL_ALIAS = "tc4_4_2_3_5_oracle"
LEGACY_ALIAS = "legacy_1_12"
INVENTORY_PATH = "docs/porting/class-inventory.json"
LEDGER_PATH = "docs/porting/scope-ledger.json"
HASH_POLICY = "UTF-8 without BOM; CRLF and lone CR normalized to LF; other characters and final newline preserved"
MILESTONES = {
    "M00": "Contracts, registries, state and full scope audit",
    "M01": "Research, scanning and knowledge",
    "M02": "Base content and natural resources",
    "M03": "Wands, nodes and vis",
    "M04": "Recipes and arcane crafting",
    "M05": "Alchemy and essentia production/storage",
    "M06": "Essentia transport",
    "M07": "Infusion",
    "M08": "Foci, equipment, enchantments and repair",
    "M09": "Advanced devices and VisNet",
    "M10": "Golems and their tasks",
    "M11": "Warp, creatures, ecology and world structures",
    "M12": "Outer Lands and Eldritch progression",
    "M13": "Public API and selected compatibility",
    "M14": "Cross-layer fidelity audit",
    "M15": "Packaged multiplayer and survival acceptance",
}
REVIEW_DEFAULT = {
    "disposition": "needs_review",
    "source_semantics": "unreviewed",
    "callsite_audit": "not_started",
    "native_coverage": "not_assessed",
    "owner": None,
    "evidence": [],
    "notes": [],
    "reviewed_sha256": {},
}


def normalized_source(path: Path) -> str:
    return path.read_bytes().decode("utf-8-sig").replace("\r\n", "\n").replace("\r", "\n")


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def json_fingerprint(data: dict) -> str:
    return sha256(json.dumps(data, ensure_ascii=False, sort_keys=True, separators=(",", ":")))


def relative_path(value: str) -> str:
    if not isinstance(value, str) or not value or "\\" in value:
        raise ValueError(f"Invalid portable relative path: {value!r}")
    path = PurePosixPath(value)
    if path.is_absolute() or any(part in (".", "..") for part in value.split("/")) or ":" in value:
        raise ValueError(f"Invalid portable relative path: {value!r}")
    if path.as_posix() != value:
        raise ValueError(f"Noncanonical relative path: {value!r}")
    return value


def collect(root: Path) -> dict[str, Path]:
    if not root.is_dir():
        raise ValueError(f"Missing source root: {root}")
    result = {path.relative_to(root).as_posix(): path for path in sorted(root.rglob("*.java"))}
    if not result:
        raise ValueError(f"Empty Java source corpus: {root}")
    return result


def source_entry(path: Path, alias: str, relative: str) -> dict:
    text = normalized_source(path)
    return {"source_alias": alias, "path": relative, "sha256_lf": sha256(text), "lines": len(text.splitlines())}


def source_layer(path: str) -> str:
    if path.startswith("thaumcraft/api/"):
        return "public_api"
    for marker, layer in (("/client/", "client"), ("/network/", "network"), ("/config/", "configuration"),
                          ("/events/", "events"), ("/items/", "items"), ("/blocks/", "blocks"),
                          ("/tiles/", "block_entities"), ("/container/", "menus"),
                          ("/entities/", "entities"), ("/world/", "world")):
        if marker in path:
            return layer
    return "support"


def queue_for(path: str) -> dict:
    """Advisory path/name routing only; implementations span several milestones."""
    lower = path.lower()
    name = PurePosixPath(lower).stem
    family, milestone = "shared_infrastructure", "M00"
    if "/codechicken/" in lower or lower.startswith("truetyper/"):
        family, milestone = "embedded_support_and_dependencies", "M13"
    elif "/world/dim/" in lower or any(word in name for word in ("eldritch", "cultist", "outer", "labyrinth", "maze")):
        family, milestone = "eldritch_and_outer_lands", "M12"
    elif any(word in name for word in ("research", "thaumonomicon", "thaumometer", "scan", "inkwell", "scribetool", "knowledge")) or "/api/research/" in lower:
        family, milestone = "research_scanning_and_knowledge", "M01"
    elif "/golems/" in lower or "/entities/ai/" in lower and "/pech/" not in lower or any(word in name for word in ("golem", "golemcore", "golembell")):
        family, milestone = "golems_and_tasks", "M10"
    elif any(word in name for word in ("taint", "warp", "champion", "pech", "zombie", "wisp", "spider", "brainyzombie")) or "/potions/" in lower or "/monster/" in lower:
        family, milestone = "warp_creatures_and_ecology", "M11"
    elif any(word in name for word in ("focus", "foci", "focal", "runic", "shield", "enchant", "repair")) or "/armor/" in lower or "/baubles/" in lower or "/equipment/" in lower or "/api/wands/foci/" in lower:
        family, milestone = "foci_equipment_and_enchantments", "M08"
    elif any(word in name for word in ("infusion", "pedestal")):
        family, milestone = "infusion", "M07"
    elif any(word in name for word in ("tube", "reservoir", "essentiatransport", "essentiatransporter")):
        family, milestone = "essentia_transport", "M06"
    elif any(word in name for word in ("crucible", "alembic", "smelter", "bellow", "distill", "jar", "phial", "essence")):
        family, milestone = "alchemy_and_essentia", "M05"
    elif any(word in name for word in ("mirror", "visnet", "nodeconverter", "nodestabilizer", "nodeenerg", "recharge", "bore", "thaumatorium", "furnace", "lamp", "spa", "scrubber", "sensor", "levitat", "warded", "paving", "hungrychest", "arcaneear", "magicbox", "grate", "arcanepressure")) or "/api/visnet/" in lower:
        family, milestone = "advanced_devices_and_visnet", "M09"
    elif "/wands/" in lower or "/nodes/" in lower or any(word in name for word in ("wand", "auranode", "visnode", "node", "aura")):
        family, milestone = "wands_nodes_and_vis", "M03"
    elif "/crafting/" in lower or any(word in name for word in ("recipe", "arcanework", "arcanetable")):
        family, milestone = "recipes_and_arcane_crafting", "M04"
    elif "/api/aspects/" in lower or name in ("aspect", "aspectlist"):
        family, milestone = "aspect_contracts_and_data", "M00"
    elif "/api/" in lower or "/compat/" in lower or "compat" in name:
        family, milestone = "public_api_and_compatibility", "M13"
    elif "/world/" in lower:
        family, milestone = ("world_structures_and_ecology", "M11") if any(word in name for word in ("village", "mound", "hilltop", "totem", "structure")) else ("natural_resources_and_worldgen", "M02")
    elif "/entities/" in lower:
        family, milestone = "creatures_and_projectiles", "M11"
    elif "/items/" in lower or "/blocks/" in lower:
        family, milestone = "base_content_and_small_interactions", "M02"
    elif "/client/" in lower:
        family, milestone = "client_rendering_fx_input_and_gui", "M14"
    elif "/tiles/" in lower or "/container/" in lower:
        family, milestone = "device_and_menu_routing_review", "M09"
    return {"subsystem_family": family, "planned_milestone": milestone,
            "basis": "path_name_heuristic_unreviewed"}


def unique_index(rows: list[dict], key: str, label: str) -> dict[str, dict]:
    result = {}
    for row in rows:
        value = relative_path(row[key])
        if value in result:
            raise ValueError(f"Duplicate {label}: {value}")
        result[value] = row
    return result


def validate_review(review: dict, sources: dict[str, dict | None], path: str) -> None:
    allowed = {
        "disposition": {"needs_review", "port", "adapt", "replace", "exclude"},
        "source_semantics": {"unreviewed", "equivalent", "different", "adapted", "not_applicable"},
        "callsite_audit": {"not_started", "partial", "complete"},
        "native_coverage": {"not_assessed", "missing", "partial", "parity_verified"},
    }
    for key, values in allowed.items():
        if review[key] not in values:
            raise ValueError(f"Invalid authored {key} for {path}: {review[key]!r}")
    if review["owner"] is not None and not isinstance(review["owner"], str):
        raise ValueError(f"Invalid review owner for {path}")
    for key in ("evidence", "notes"):
        if not isinstance(review[key], list) or any(not isinstance(value, str) or not value.strip() for value in review[key]):
            raise ValueError(f"Invalid review {key} for {path}")
    hashes = review["reviewed_sha256"]
    if not isinstance(hashes, dict) or any(side not in sources or sources[side] is None for side in hashes):
        raise ValueError(f"Invalid reviewed source aliases for {path}")
    if any(side not in sources or not isinstance(value, str) or not re.fullmatch(r"[0-9a-f]{64}", value)
           for side, value in hashes.items()):
        raise ValueError(f"Invalid reviewed fingerprints for {path}")
    if review["disposition"] == "exclude" and (review["callsite_audit"] != "complete" or not review["evidence"]):
        raise ValueError(f"Exclusion needs an authored complete callsite audit and evidence: {path}")
    if review["native_coverage"] == "parity_verified" and not review["evidence"]:
        raise ValueError(f"Authored parity verdict needs evidence: {path}")


def refresh(root: Path, original_root: Path, previous: dict | None) -> dict:
    original = collect(original_root)
    legacy = collect(root / "src/main/java")
    inventory = json.loads((root / INVENTORY_PATH).read_text(encoding="utf-8"))
    mappings = unique_index(inventory["legacy_units"], "legacy_path", "legacy inventory path")
    expected_legacy = {"src/main/java/" + path for path in legacy}
    if set(mappings) != expected_legacy:
        raise ValueError("Legacy path set differs from authored class-inventory; update that inventory explicitly first")
    active = unique_index(inventory["active_native_sources"], "path", "active native inventory path")
    for path in active:
        if not path.startswith(("common/src/main/java/", "fabric/src/main/java/", "neoforge/src/main/java/")) or not (root / path).is_file():
            raise ValueError(f"Dangling or non-native inventory source: {path}")
    for row in mappings.values():
        if len(row["native_paths"]) != len(set(row["native_paths"])):
            raise ValueError(f"Duplicate native counterpart for {row['legacy_path']}")
        for path in row["native_paths"]:
            relative_path(path)
            if path not in active or not (root / path).is_file():
                raise ValueError(f"Dangling authored native counterpart: {path}")
    if previous is not None and previous.get("schema_version") != 1:
        raise ValueError("Unsupported scope ledger schema; migrate it explicitly without dropping authored reviews")
    old = unique_index(previous["units"], "canonical_path", "scope ledger path") if previous else {}
    current = set(original) | set(legacy)
    lost = sorted(set(old) - current)
    if lost:
        raise ValueError(f"Scope path loss; retain/migrate its authored row explicitly: {lost[:5]}")
    rows = []
    for path in sorted(current):
        sources = {
            "original": source_entry(original[path], ORIGINAL_ALIAS, path) if path in original else None,
            "legacy": source_entry(legacy[path], LEGACY_ALIAS, "src/main/java/" + path) if path in legacy else None,
        }
        prior = old.get(path)
        if prior:
            for side in sources:
                if prior.get(side) is not None and sources[side] is None:
                    raise ValueError(f"Source-side path loss: {side}:{path}")
        mapping = mappings.get("src/main/java/" + path)
        if mapping and mapping["legacy_sha256"] != sources["legacy"]["sha256_lf"]:
            raise ValueError(f"Legacy fingerprint is stale in class-inventory; refresh that inventory first: {path}")
        review = copy.deepcopy(prior.get("review", {})) if prior else {}
        for key, value in REVIEW_DEFAULT.items():
            review.setdefault(key, copy.deepcopy(value))
        validate_review(review, sources, path)
        anchored = review["reviewed_sha256"]
        review_current = (all(anchored.get(side) == source["sha256_lf"] for side, source in sources.items() if source)
                          if anchored else None)
        # Queue routing is initially heuristic; later authored corrections survive refresh.
        queue = copy.deepcopy(prior["queue"]) if prior else queue_for(path)
        if not isinstance(queue, dict) or queue.get("planned_milestone") not in MILESTONES or not queue.get("subsystem_family"):
            raise ValueError(f"Invalid authored queue routing for {path}")
        rows.append({
            "canonical_path": path,
            "path_relation": "shared_path" if path in original and path in legacy else "original_only" if path in original else "legacy_only",
            **sources,
            "source_layer": source_layer(path),
            "queue": queue,
            "native_counterparts": list(mapping["native_paths"]) if mapping else [],
            "class_inventory_mapping": {"legacy_status": mapping["status"],
                                        "legacy_parity_verified": mapping["parity_verified"],
                                        "basis": mapping["basis"]} if mapping else None,
            "review": review,
            "review_fingerprints_current": review_current,
        })
    relations = Counter(row["path_relation"] for row in rows)
    counts = {"original_units": len(original), "legacy_units": len(legacy), "union_rows": len(rows),
              **{key: relations[key] for key in ("shared_path", "original_only", "legacy_only")},
              "native_linked_rows": sum(bool(row["native_counterparts"]) for row in rows),
              "unique_native_counterparts": len({path for row in rows for path in row["native_counterparts"]}),
              "dispositions": dict(sorted(Counter(row["review"]["disposition"] for row in rows).items())),
              "milestone_queue": dict(sorted(Counter(row["queue"]["planned_milestone"] for row in rows).items())),
              "stale_review_fingerprints": sum(row["review_fingerprints_current"] is False for row in rows)}
    return {
        "schema_version": 1,
        "claim": "Metadata-only scope and audit queue. No automatic gameplay completion or semantic parity claim; M00 remains open.",
        "source_aliases": {
            ORIGINAL_ALIAS: {"role": "private original gameplay specification", "version": "Thaumcraft 4.2.3.5",
                             "resolution": "--original-root; defaults to sibling work/thaumcraft-4.2.3.5-oracle outside the repository",
                             "path_basis": "relative Java source path in private oracle; no source body is copied"},
            LEGACY_ALIAS: {"role": "retained 1.12 reference, not the gameplay specification", "resolution": "repository src/main/java"},
        },
        "fingerprint_policy": HASH_POLICY,
        "path_policy": "One row per union of canonical relative Java paths. shared_path is lexical only; moves/semantic matches require authored evidence, never guessed from basenames.",
        "native_link_policy": "Only explicit native_paths from the authored legacy class-inventory are linked. Its status is retained as legacy mapping metadata, never promoted to original parity or copied into review.",
        "review_policy": "--write preserves authored review and queue fields. No automatic completion, exclusion, TC6 classification or dead-code verdict. Exclusion requires an authored complete callsite audit and evidence; source changes flag anchored reviews stale without erasing them.",
        "queue_policy": "Initial family/milestone routing uses paths/names only and needs review. It is the principal audit queue, not a dependency graph; render/GUI work belongs with each gameplay unit and its later M14 audit.",
        "milestones": MILESTONES,
        "inventory_provenance": {"path": INVENTORY_PATH, "canonical_json_sha256": json_fingerprint(inventory),
                                 "hash_policy": "Parsed JSON serialized with sorted keys, UTF-8, no whitespace; includes authored mappings and recorded fingerprints"},
        "counts": counts,
        "units": rows,
    }


def self_test() -> None:
    """Exercise refresh and its rejection paths without touching workspace sources."""
    with tempfile.TemporaryDirectory(prefix="tc4-scope-ledger-") as temporary:
        base = Path(temporary)
        root, oracle = base / "port", base / "oracle"
        (root / "docs/porting").mkdir(parents=True)
        (root / "src/main/java/demo").mkdir(parents=True)
        (root / "common/src/main/java/demo").mkdir(parents=True)
        (oracle / "demo").mkdir(parents=True)
        (oracle / "demo/Shared.java").write_bytes(b"\xef\xbb\xbfclass Shared {}\r\n")
        (oracle / "demo/Original.java").write_text("class Original {}\n", encoding="utf-8")
        (root / "src/main/java/demo/Shared.java").write_text("class Shared {}\n", encoding="utf-8")
        (root / "src/main/java/demo/Legacy.java").write_text("class Legacy {}\n", encoding="utf-8")
        (root / "common/src/main/java/demo/Shared.java").write_text("class Shared {}\n", encoding="utf-8")
        inventory = {
            "active_native_sources": [{"path": "common/src/main/java/demo/Shared.java"}],
            "legacy_units": [{"legacy_path": "src/main/java/demo/" + name + ".java",
                              "legacy_sha256": sha256(normalized_source(root / "src/main/java/demo" / (name + ".java"))),
                              "native_paths": ["common/src/main/java/demo/Shared.java"] if name == "Shared" else [],
                              "status": "partial" if name == "Shared" else "pending", "parity_verified": False,
                              "basis": "Synthetic authored fixture"} for name in ("Legacy", "Shared")],
        }
        inventory_file = root / INVENTORY_PATH
        inventory_file.write_text(json.dumps(inventory), encoding="utf-8")
        first = refresh(root, oracle, None)
        assert first["counts"]["union_rows"] == 3
        assert [row["path_relation"] for row in first["units"]] == ["legacy_only", "original_only", "shared_path"]
        assert refresh(root, oracle, first) == first
        shared = first["units"][-1]
        assert shared["original"]["sha256_lf"] == shared["legacy"]["sha256_lf"]
        assert shared["review"]["disposition"] == "needs_review"
        shared["review"].update({"disposition": "adapt", "source_semantics": "adapted", "notes": ["Authored note"],
                                 "reviewed_sha256": {side: shared[side]["sha256_lf"] for side in ("original", "legacy")}})
        shared["queue"] = {"subsystem_family": "authored_family", "planned_milestone": "M03", "basis": "authored"}
        reviewed = refresh(root, oracle, first)
        assert reviewed["units"][-1]["review"] == shared["review"]
        assert reviewed["units"][-1]["queue"] == shared["queue"]
        assert reviewed["units"][-1]["review_fingerprints_current"] is True
        (oracle / "demo/Shared.java").write_text("class Shared { int changed; }\n", encoding="utf-8")
        drifted = refresh(root, oracle, reviewed)
        assert drifted["units"][-1]["review"] == shared["review"]
        assert drifted["units"][-1]["review_fingerprints_current"] is False

        def rejects(action, expected: str) -> None:
            try:
                action()
            except ValueError as exception:
                assert expected in str(exception), str(exception)
            else:
                raise AssertionError("Expected rejection: " + expected)

        original_text = (oracle / "demo/Shared.java").read_bytes()
        (oracle / "demo/Shared.java").unlink()
        rejects(lambda: refresh(root, oracle, drifted), "Source-side path loss")
        (oracle / "demo/Shared.java").write_bytes(original_text)
        (oracle / "demo/Original.java").unlink()
        rejects(lambda: refresh(root, oracle, drifted), "Scope path loss")
        (oracle / "demo/Original.java").write_text("class Original {}\n", encoding="utf-8")
        native = root / "common/src/main/java/demo/Shared.java"
        native.unlink()
        rejects(lambda: refresh(root, oracle, drifted), "Dangling")
        native.write_text("class Shared {}\n", encoding="utf-8")
        excluded = copy.deepcopy(drifted)
        excluded["units"][0]["review"]["disposition"] = "exclude"
        rejects(lambda: refresh(root, oracle, excluded), "Exclusion needs")
        duplicate = copy.deepcopy(drifted)
        duplicate["units"].append(copy.deepcopy(duplicate["units"][0]))
        rejects(lambda: refresh(root, oracle, duplicate), "Duplicate scope ledger path")
        nonexistent_source = copy.deepcopy(drifted)
        nonexistent_source["units"][0]["review"]["reviewed_sha256"] = {"original": "0" * 64}
        rejects(lambda: refresh(root, oracle, nonexistent_source), "Invalid reviewed source aliases")
        inventory["legacy_units"][0]["legacy_sha256"] = "0" * 64
        inventory_file.write_text(json.dumps(inventory), encoding="utf-8")
        rejects(lambda: refresh(root, oracle, drifted), "Legacy fingerprint is stale")
        inventory["legacy_units"][0]["legacy_sha256"] = sha256(normalized_source(root / "src/main/java/demo/Legacy.java"))
        inventory["legacy_units"][0]["native_paths"] = ["common/src/main/java/demo/Missing.java"]
        inventory_file.write_text(json.dumps(inventory), encoding="utf-8")
        rejects(lambda: refresh(root, oracle, drifted), "Dangling authored native counterpart")
    print("Scope ledger self-test passed: normalization, union, determinism, review/queue preservation, drift, path loss, dangling links, exclusion and duplicates")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--write", action="store_true", help="Refresh metadata while preserving authored reviews and queue routing")
    mode.add_argument("--check", action="store_true", help="Reject stale metadata without writing (the default)")
    mode.add_argument("--self-test", action="store_true", help="Run synthetic safety/regression checks without the private oracle")
    parser.add_argument("--original-root", type=Path, default=ROOT.parent / "work/thaumcraft-4.2.3.5-oracle",
                        help="Private TC4 Java oracle root; the absolute path is never saved in the ledger")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    ledger = ROOT / LEDGER_PATH
    previous = json.loads(ledger.read_text(encoding="utf-8")) if ledger.exists() else None
    try:
        data = refresh(ROOT, args.original_root, previous)
    except (ValueError, KeyError, UnicodeError) as exception:
        raise SystemExit(str(exception)) from exception
    if args.write:
        ledger.parent.mkdir(parents=True, exist_ok=True)
        ledger.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    elif data != previous:
        raise SystemExit("Scope ledger is missing or stale; refresh class-inventory, then run update_scope_ledger.py --write")
    print(json.dumps({"mode": "write" if args.write else "check", **data["counts"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
