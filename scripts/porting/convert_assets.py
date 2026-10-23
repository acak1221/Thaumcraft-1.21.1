#!/usr/bin/env python3
"""Copy TC4 art unchanged and generate the small 1.21.1 foundation resource set.

The source and original-reference trees are read only. This is an explicit
identity/model manifest, not a legacy-world or NBT migration tool. No old model,
blockstate, shader or recipe JSON is copied into the modern resource tree.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import struct
import subprocess
from pathlib import Path
from typing import Any

import composed_resources

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "src/main/resources/assets/thaumcraft"
ORIGINAL = ROOT / "thaumcraft_src/assets/thaumcraft"
RESOURCE_ROOT = ROOT / "common/src/main/resources"
ASSETS = RESOURCE_ROOT / "assets/thaumcraft"
BLOCK_ATLAS = RESOURCE_ROOT / "assets/minecraft/atlases/blocks.json"
MANIFEST = ROOT / "docs/porting/resource-manifest.json"
CANONICAL_TEXT_SUFFIXES = frozenset({".mcmeta", ".obj", ".mtl", ".json", ".lang"})
# These original model UV sheets are full-image static sprites, not animation
# strips. Native 1.21.1 AnimationMetadataSection.EMPTY keeps the full W/H and
# Stitcher accepts rectangular sprites. Explicit singles avoid stitching the
# other legacy models/effect sheets; retain the strip guard everywhere else.
STATIC_MODEL_SPRITES = {
    "models/table": (64, 32), "models/restable": (128, 64), "models/worktable": (128, 64),
    "models/bellows": (128, 64), "models/alembic": (64, 64),
    "models/wand_rod_wood": (32, 32), "models/wand_rod_greatwood": (32, 32),
    "models/wand_cap_iron": (32, 32), "models/wand_cap_gold": (32, 32),
    "models/wand_cap_thaumium": (32, 32),
    "models/wand": (32, 32),
    "models/wand_rod_silverwood": (32, 32),
    # The elemental rods and the primal staff core (the item models of wands draw them as 3D shapes), and the trunk spawner's chest.
    "models/wand_rod_obsidian": (32, 32), "models/wand_rod_blaze": (32, 32), "models/wand_rod_ice": (32, 32),
    "models/wand_rod_quartz": (32, 32), "models/wand_rod_bone": (32, 32), "models/wand_rod_reed": (32, 32),
    "models/wand_rod_primal": (48, 64), "models/trunk": (64, 64), "models/wand_cap_void": (32, 32),
    "models/wand_cap_copper": (32, 32),
    "models/infuser": (64, 64),
    "models/wandtable": (128, 64),
}

# Legacy metadata comes from ItemResource.java; the texture names come from the
# existing itemresource_* JSON. The names match the approved flattened registry.
MATERIALS = (
    (0, "alumentum", "alumentum"),
    (1, "nitor", "nitor"),
    (2, "thaumium_ingot", "thaumiumingot"),
    (3, "quick_silver", "quicksilver"),
    (4, "magic_tallow", "tallow"),
    (5, "zombie_brain", "brain"),
    (6, "amber_gem", "amber"),
    (7, "enchanted_fabric", "cloth"),
    (8, "vis_filter", "filter"),
    (9, "knowledge_fragment", "knowledgefragment"),
    (10, "mirrored_glass", "mirrorglass"),
    (11, "tainted_goo", "taint_slime"),
    (12, "taint_tendril", "taint_tendril"),
    (13, "jar_label", "label"),
    (14, "salis_mundus", "dust"),
    (15, "primal_charm", "charm"),
    (16, "void_ingot", "voidingot"),
    (17, "void_seed", "voidseed"),
    (18, "gold_coin", "coin"),
)
# These are the original ItemShard.colors, rather than Aspect.getColor().
PRIMALS = (
    (0, "air", 0xFFFF7E),
    (1, "fire", 0xFF3C01),
    (2, "water", 0x0090FF),
    (3, "earth", 0x00A000),
    (4, "order", 0xEECCFF),
    (5, "entropy", 0x555577),
)

GAPS = [
    {"id": "scanner_mesh", "status": "pending",
     "detail": "Thaumometer icon aliases the original scanner UV atlas byte-for-byte; scanner.obj and hand/screen rendering are not implemented."},
    {"id": "wand_mesh", "status": "pending",
     "detail": "Foundation wand is a static layered original rod/cap icon; dynamic TC4 rod/cap/focus geometry and use poses remain to port."},
    {"id": "crystal_geometry", "status": "implemented_client_unverified",
     "detail": "Seven native tile/builtin item renderers use the original six-piece ModelCrystal geometry, face attachment and positional Java RNG; inspected client visual acceptance remains pending."},
    {"id": "jar_contents", "status": "pending",
     "detail": "Jar uses the existing two-box shell with original face textures; essentia fill, labels, brain/node/void variants need dedicated rendering."},
    {"id": "aura_node_renderer", "status": "pending",
     "detail": "Aura node block has empty geometry; its item uses a diagnostic shard marker. Original node rings, reveal visibility and drain beams are not implemented."},
    {"id": "research_gui", "status": "partial",
     "detail": "Thaumonomicon map and completed text/illustration pages use the original corpus. Item-backed node icons, recipe pages, original line animations, research table and puzzle remain pending; visual parity is not established."},
    {"id": "item_tint_registration", "status": "platform_client_registration_required",
     "detail": "Shard models use tint index 0 and original RGB values. Seven crystal tile/builtin item renderers apply original per-piece colors directly. Filled phials use the aspect component color at tint index 1 while layer 0 stays white. Loader clients must register these providers and jar cutout layers."},
    {"id": "legacy_mesh_formats", "status": "reference_only",
     "detail": "OBJ/MTL text content is preserved with canonical LF newlines; these references are not auto-loaded, and internal paths/legacy formats still need a modern renderer importer."},
    {"id": "shader_and_remaining_model_coverage", "status": "pending",
     "detail": "Legacy shader/program, blockstate, model and recipe JSON are deliberately not bulk-copied. Only approved foundation IDs receive modern models."},
    {"id": "gameplay_parity", "status": "not_established",
     "detail": "Resource coverage is not gameplay or visual parity. The 19 material items are foundation identities, not completed TC4 item behaviors."},
]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical_file_bytes(path: Path) -> bytes:
    data = path.read_bytes()
    if path.suffix.lower() in CANONICAL_TEXT_SUFFIXES:
        return data.replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    return data


def normalized(relative: str) -> str:
    """Produce legal lowercase ResourceLocation paths without touching sources."""
    return re.sub(r"[^a-z0-9/._-]", "_", relative.replace("\\", "/").lower())


def write_bytes(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.is_file() or path.read_bytes() != data:
        path.write_bytes(data)


def write_json(path: Path, value: Any) -> None:
    data = (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    write_bytes(path, data)


def resource_location(identifier: str, default_namespace: str = "minecraft") -> tuple[str, str]:
    if not isinstance(identifier, str) or identifier.count(":") > 1:
        raise ValueError(f"Invalid ResourceLocation: {identifier!r}")
    namespace, path = identifier.split(":", 1) if ":" in identifier else (default_namespace, identifier)
    if not re.fullmatch(r"[a-z0-9._-]+", namespace) or not re.fullmatch(r"[a-z0-9/._-]+", path):
        raise ValueError(f"Invalid ResourceLocation: {namespace}:{path}")
    return namespace, path


def convert_sound_definitions(legacy: dict[str, Any]) -> tuple[dict[str, Any], list[dict[str, str]], list[dict[str, str]]]:
    modern: dict[str, Any] = {}
    key_map = {old_key: normalized(old_key) for old_key in legacy}
    if len(set(key_map.values())) != len(key_map):
        raise ValueError("Sound event IDs collide after lowercase normalization")
    event_map = []
    reference_map = []
    for old_key, definition in legacy.items():
        new_key = key_map[old_key]
        resource_location(f"thaumcraft:{new_key}")
        converted = copy.deepcopy(definition)
        converted["subtitle"] = f"subtitles.thaumcraft.{new_key}"
        event_map.append({"legacy_key": old_key, "modern_key": new_key,
                          "legacy_id": f"thaumcraft:{old_key}", "modern_id": f"thaumcraft:{new_key}"})
        for index, sound in enumerate(converted.get("sounds", [])):
            old_name = sound if isinstance(sound, str) else sound["name"]
            namespace, path = old_name.split(":", 1) if ":" in old_name else ("minecraft", old_name)
            namespace = namespace.lower()
            is_event = isinstance(sound, dict) and sound.get("type") == "event"
            new_path = key_map.get(path, normalized(path)) if is_event and namespace == "thaumcraft" else normalized(path)
            new_name = f"{namespace}:{new_path}"
            resource_location(new_name)
            if isinstance(sound, str):
                converted["sounds"][index] = new_name
            else:
                sound["name"] = new_name
            reference_map.append({"event": f"thaumcraft:{new_key}", "type": "event" if is_event else "file",
                                  "legacy_reference": old_name, "modern_reference": new_name})
        modern[new_key] = converted
    return modern, event_map, reference_map


def create_sounds_json() -> dict[str, Any]:
    source = SOURCE / "sounds.json"
    target = ASSETS / "sounds.json"
    modern, events, references = convert_sound_definitions(json.loads(source.read_text(encoding="utf-8")))
    write_json(target, modern)
    return {"source": source.relative_to(ROOT).as_posix(), "target": target.relative_to(ROOT).as_posix(),
            "source_sha256": sha256(canonical_file_bytes(source)), "source_hash_policy": "canonical_lf",
            "sha256": sha256(canonical_file_bytes(target)), "target_hash_policy": "canonical_lf",
            "role": "sound_event_definitions", "transformation": "lowercase_event_ids_and_resource_references_with_subtitles",
            "event_id_mappings": events, "reference_id_mappings": references}


def source_paths() -> list[Path]:
    """Prefer Git's case-preserving inventory, including identical case aliases."""
    try:
        prefix = SOURCE.relative_to(ROOT).as_posix()
        result = subprocess.run(
            ["git", "-C", str(ROOT), "ls-files", "-z", "--", prefix],
            check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        )
        paths = [ROOT / entry.decode("utf-8") for entry in result.stdout.split(b"\0") if entry]
        if paths:
            return sorted(paths, key=lambda path: path.as_posix())
    except (OSError, subprocess.CalledProcessError):
        pass
    return sorted(path for path in SOURCE.rglob("*") if path.is_file())


def source_revision() -> str | None:
    try:
        return subprocess.check_output(
            ["git", "-C", str(ROOT), "log", "-1", "--format=%H", "--", SOURCE.relative_to(ROOT).as_posix()],
            stderr=subprocess.DEVNULL
        ).decode("ascii").strip() or None
    except (OSError, subprocess.CalledProcessError):
        return None


def copy_art() -> list[dict[str, Any]]:
    original_files = {
        path.relative_to(ORIGINAL).as_posix().casefold(): path
        for path in ORIGINAL.rglob("*") if path.is_file()
    }
    copied: list[dict[str, Any]] = []
    targets: dict[str, str] = {}
    for path in source_paths():
        relative = path.relative_to(SOURCE).as_posix()
        if not (relative.startswith("textures/") or relative.startswith("sounds/")):
            continue
        target_relative = normalized(relative)
        is_text = path.suffix.lower() in CANONICAL_TEXT_SUFFIXES
        data = canonical_file_bytes(path)
        digest = sha256(data)
        if target_relative in targets and targets[target_relative] != digest:
            raise ValueError(f"Normalization collides with different bytes: {relative}")
        targets[target_relative] = digest
        write_bytes(ASSETS / target_relative, data)
        original = original_files.get(relative.casefold())
        original_digest = sha256(canonical_file_bytes(original)) if original else None
        record = {
            "source": path.relative_to(ROOT).as_posix(),
            "target": (ASSETS / target_relative).relative_to(ROOT).as_posix(),
            "sha256": digest,
            "original_source": original.relative_to(ROOT).as_posix() if original else None,
            "original_sha256": original_digest,
            "matches_original_bytes": digest == original_digest if not is_text else None,
            "path_normalized": relative != target_relative,
        }
        if is_text:
            record.update({"transformation": "newline_only_lf", "source_sha256": digest,
                           "source_hash_policy": "canonical_lf", "target_hash_policy": "canonical_lf",
                           "original_hash_policy": "canonical_lf",
                           "matches_original_canonical_text": digest == original_digest})
        copied.append(record)
    copied.append(create_sounds_json())
    # TC4's scanner uses a mesh UV atlas, not an independent inventory icon.
    # Keep the bytes original and label the temporary foundation representation.
    scanner = SOURCE / "textures/models/scanner.png"
    alias = ASSETS / "textures/items/thaumometer.png"
    write_bytes(alias, scanner.read_bytes())
    copied.append({
        "source": scanner.relative_to(ROOT).as_posix(),
        "target": alias.relative_to(ROOT).as_posix(),
        "sha256": sha256(scanner.read_bytes()),
        "role": "temporary_scanner_atlas_icon_alias",
        "visual_parity": False,
    })
    return copied


def model(path: str, value: dict[str, Any]) -> None:
    write_json(ASSETS / "models" / f"{path}.json", value)


def item_model(item_id: str, *textures: str, handheld: bool = False) -> None:
    model(f"item/{item_id}", {
        "parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
        "textures": {f"layer{index}": f"thaumcraft:{texture}" for index, texture in enumerate(textures)},
    })


def legacy_mapping(old_id: str, metadata: int, new_id: str, textures: list[str],
                   keys: list[str], registry: str = "item", tint: int | None = None,
                   visual_status: str = "original_sprite") -> dict[str, Any]:
    entry: dict[str, Any] = {
        "legacy_id": f"thaumcraft:{old_id}", "legacy_metadata": metadata,
        "modern_id": f"thaumcraft:{new_id}", "registry": registry,
        "textures": [f"thaumcraft:{texture}" for texture in textures],
        "legacy_translation_keys": keys, "visual_status": visual_status,
    }
    if tint is not None:
        entry["tint_index"] = 0
        entry["tint_rgb"] = f"#{tint:06X}"
    return entry


def create_jar_models() -> list[dict[str, Any]]:
    """Regenerate ONLY jar presentations; never overwrite unrelated content models.

    Body/neck bounds and textures follow original BlockJarRenderer:48-72.
    Builtin item transforms are the native1.21.1 block/block display defaults;
    actual contents/labels are rendered from the stack by JarItemRenderer.
    """
    jar_faces = {
        face: {"texture": "#" + ("bottom" if face == "down" else "top" if face == "up" else "side")}
        for face in ("down", "up", "north", "south", "west", "east")
    }
    neck_faces = {face: {"texture": "#" + ("top" if face in ("down", "up") else "side")}
                  for face in jar_faces}
    display = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [.625, .625, .625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [.25, .25, .25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [.5, .5, .5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.375, .375, .375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [.4, .4, .4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [.4, .4, .4]},
    }
    mappings = []
    for metadata, native_id, suffix in ((0, "jar", ""), (3, "jar_void", "_void")):
        textures = [f"blocks/jar_side{suffix}", "blocks/jar_bottom", f"blocks/jar_top{suffix}"]
        model(f"block/{native_id}", {
            "ambientocclusion": False,
            "textures": {"particle": f"thaumcraft:{textures[0]}", "side": f"thaumcraft:{textures[0]}",
                         "bottom": f"thaumcraft:{textures[1]}", "top": f"thaumcraft:{textures[2]}"},
            "elements": [{"from": [3, 0, 3], "to": [13, 12, 13], "faces": jar_faces},
                         {"from": [5, 12, 5], "to": [11, 14, 11], "faces": neck_faces}],
        })
        model(f"item/{native_id}", {"parent": "minecraft:builtin/entity", "gui_light": "side", "display": display})
        write_json(ASSETS / "blockstates" / f"{native_id}.json", {"variants": {"": {"model": f"thaumcraft:block/{native_id}"}}})
        mappings.append(legacy_mapping("blockjar", metadata, native_id, textures,
                                       [f"tile.thaumcraft.jar.{metadata}.name"], registry="block_and_item",
                                       visual_status="existing_two_box_shell"))
    return mappings


def create_crystal_models() -> list[dict[str, Any]]:
    """Regenerate only seven builtin crystal presentations; actual geometry belongs to TileCrystalRenderer."""
    mappings: list[dict[str, Any]] = []
    display = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [.625, .625, .625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [.25, .25, .25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [.5, .5, .5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [.375, .375, .375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [.4, .4, .4]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [.4, .4, .4]},
    }
    for metadata, primal, tint in (*PRIMALS, (6, "mixed", None)):
        crystal_id = f"crystal_{primal}"
        model(f"block/{crystal_id}", {"ambientocclusion": False, "textures": {"particle": "thaumcraft:blocks/crystal"}, "elements": []})
        model(f"item/{crystal_id}", {"parent": "minecraft:builtin/entity", "gui_light": "side", "display": display,
                                     "textures": {"particle": "thaumcraft:blocks/crystal"}})
        write_json(ASSETS / "blockstates" / f"{crystal_id}.json", {"variants": {"": {"model": f"thaumcraft:block/{crystal_id}"}}})
        mappings.append(legacy_mapping("blockcrystal", metadata, crystal_id, ["models/crystal", "blocks/crystal"],
                                       [f"tile.thaumcraft.crystal.{metadata}.name"], registry="block_and_item", tint=tint,
                                       visual_status="native_six_piece_renderer_client_unverified"))
    return mappings


def create_models() -> list[dict[str, Any]]:
    mappings: list[dict[str, Any]] = []
    for metadata, item_id, legacy_name in MATERIALS:
        textures = [f"items/{legacy_name}"]
        if item_id == "jar_label":
            textures.append("items/label_over")
        item_model(item_id, *textures)
        keys = [f"item.thaumcraft.resource.{legacy_name}.name", f"item.thaumcraft.resource.{metadata}.name"]
        if item_id == "zombie_brain":
            keys.insert(0, "item.thaumcraft.zombie_brain.name")
        mappings.append(legacy_mapping("itemresource", metadata, item_id, textures, keys))
    crystal_mappings = create_crystal_models()
    for metadata, primal, tint in PRIMALS:
        shard_id = f"{primal}_shard"
        item_model(shard_id, "items/shard")
        mappings.append(legacy_mapping("itemshard", metadata, shard_id, ["items/shard"],
                                       [f"item.thaumcraft.shard.{metadata}.name"], tint=tint))
        mappings.append(crystal_mappings[metadata])
    mappings.append(crystal_mappings[6])
    item_model("balance_shard", "items/shard_balanced")
    mappings.append(legacy_mapping("itemshard", 6, "balance_shard", ["items/shard_balanced"],
                                   ["item.thaumcraft.shard.6.name"]))
    item_model("thaumometer", "items/thaumometer")
    mappings.append(legacy_mapping("itemthaumometer", 0, "thaumometer", ["items/thaumometer"],
                                   ["item.thaumcraft.thaumometer.name"], visual_status="temporary_scanner_atlas_icon"))
    item_model("thaumonomicon", "items/thaumonomicon")
    mappings.append(legacy_mapping("itemthaumonomicon", 0, "thaumonomicon", ["items/thaumonomicon"],
                                   ["item.thaumcraft.thaumonomicon.0.name"], visual_status="original_item_sprite"))
    item_model("inkwell", "items/inkwell")
    mappings.append(legacy_mapping("iteminkwell", 0, "inkwell", ["items/inkwell"],
                                   ["item.ItemInkwell.name", "item.thaumcraft.inkwell.name"]))
    mappings.append(legacy_mapping("blocktable", 0, "table", ["models/table", "blocks/woodplain"],
                                   ["tile.thaumcraft.table.0.name"], registry="block_and_item",
                                   visual_status="original_model_boxes_and_uv_native_ber"))
    research_table = legacy_mapping("blocktable", 2, "research_table", ["models/restable", "blocks/woodplain"],
                                   ["tile.thaumcraft.table.2.name"],
                                   visual_status="flattened_pick_presentation_places_base_table")
    research_table["legacy_metadata_aliases"] = {"min": 2, "max": 9}
    mappings.append(research_table)
    for metadata, native_id, base, overlay, name in (
            (0, "research_note", "researchnotes", "researchnotesoverlay", "item.researchnotes.name"),
            (64, "discovery", "discovery", "discoveryoverlay", "item.discovery.name")):
        textures = [f"items/{base}", f"items/{overlay}"]
        item_model(native_id, *textures)
        entry = legacy_mapping("itemresearchnotes", metadata, native_id, textures, [name])
        entry.update({"untinted_layers": [0], "tint_index": 1,
                      "tint_source": "ResearchNoteData.color (presentation independent of complete)",
                      "default_tint_rgb": "#999999"})
        mappings.append(entry)
    item_model("wand", "items/wand_rod_greatwood", "items/wand_cap_iron", handheld=True)
    mappings.append(legacy_mapping("wandcasting", 0, "wand", ["items/wand_rod_greatwood", "items/wand_cap_iron"],
                                   ["item.thaumcraft.wand_casting.name"], visual_status="foundation_static_layered_icon"))

    # The historical44-entry foundation identity manifest is not a catalogue of
    # every later content unit. Void metadata3 is tracked in its bounded contract.
    mappings.extend(create_jar_models()[:1])
    model("block/aura_node", {"textures": {"particle": "thaumcraft:blocks/empty"}, "elements": []})
    write_json(ASSETS / "blockstates/aura_node.json", {"variants": {"": {"model": "thaumcraft:block/aura_node"}}})
    # Both loader registrations are reached only by a builtin/entity model.
    model("item/aura_node", {"parent": "minecraft:builtin/entity",
                             "textures": {"particle": "thaumcraft:items/shard"}})
    mappings.append(legacy_mapping("blockairy", 0, "aura_node", ["blocks/empty", "items/shard"],
                                   ["tile.thaumcraft.airy.0.name"], registry="block_and_item",
                                   visual_status="empty_block_diagnostic_item_marker"))

    # ItemEssence's original models keep the glass untinted in layer 0 and put
    # animated aspect-colored essentia in layer 1. Native IDs replace metadata.
    item_model("phial_empty", "items/phial")
    empty_phial = legacy_mapping("itemessence", 0, "phial_empty", ["items/phial"],
                                 ["item.thaumcraft.essence.0.name"])
    empty_phial["untinted_layers"] = [0]
    mappings.append(empty_phial)
    item_model("phial_filled", "items/phial", "items/essence")
    filled_phial = legacy_mapping("itemessence", 1, "phial_filled", ["items/phial", "items/essence"],
                                  ["item.thaumcraft.essence.1.name"])
    filled_phial.update({
        "untinted_layers": [0], "tint_index": 1,
        "tint_source": "PhialData aspect tag -> Aspect.getColor()",
        "default_tint_rgb": "#FFFFFF", "fixed_essentia_amount": 8,
        "legacy_model_metadata_aliases": {"min": 1, "max": 63},
    })
    mappings.append(filled_phial)
    return mappings


def read_lang(path: Path) -> tuple[dict[str, str], list[str]]:
    entries: dict[str, str] = {}
    duplicates: list[str] = []
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        if not raw.strip() or raw.lstrip().startswith("#") or "=" not in raw:
            continue
        key, value = raw.split("=", 1)
        key = key.strip()
        if key in entries:
            duplicates.append(key)
        # Preserve the text after the first '=' exactly, including TC4 markup.
        entries[key] = value
    return entries, duplicates


def convert_languages(mappings: list[dict[str, Any]]) -> list[dict[str, Any]]:
    reports: list[dict[str, Any]] = []
    for locale in ("en_us", "ru_ru"):
        source = SOURCE / "lang" / f"{locale}.lang"
        entries, duplicates = read_lang(source)
        original_count = len(entries)
        subtitles = json.loads((ROOT / "scripts/porting/sound-subtitles.json").read_text(encoding="utf-8"))
        for sound, translations in subtitles.items():
            entries[f"subtitles.thaumcraft.{sound}"] = translations[locale]
        # Other porting tasks may add native GUI/tooltip keys. Regeneration owns
        # original keys and this manifest's name aliases, not unrelated additions.
        target = ASSETS / "lang" / f"{locale}.json"
        retained_keys = 0
        if target.is_file():
            existing = json.loads(target.read_text(encoding="utf-8"))
            for key, value in existing.items():
                if key not in entries:
                    entries[key] = value
                    retained_keys += 1
        missing: list[str] = []
        for entry in mappings:
            new_id = entry["modern_id"].split(":", 1)[1]
            value = next((entries[key] for key in entry["legacy_translation_keys"] if key in entries), None)
            if value is None:
                missing.append(new_id)
                continue
            modern_key = ("block" if entry["registry"] == "block_and_item" else "item") + f".thaumcraft.{new_id}"
            entries[modern_key] = value
            if entry["registry"] == "block_and_item":
                entries[f"item.thaumcraft.{new_id}"] = value
        write_json(target, entries)
        reports.append({"locale": locale, "source": source.relative_to(ROOT).as_posix(),
                        "source_sha256": sha256(canonical_file_bytes(source)), "source_hash_policy": "canonical_lf",
                        "legacy_keys": original_count,
                        "output_keys": len(entries), "retained_existing_native_keys": retained_keys,
                        "duplicate_source_keys": duplicates,
                        "missing_name_aliases": missing})
    return reports


def create_block_atlas() -> dict[str, Any]:
    """Extend vanilla's merged atlas without renaming any legacy texture path.

    Since 1.19.3 only textures/block and textures/item are included by default.
    Directory sources preserve each discovered resource's original namespace.
    https://feedback.minecraft.net/hc/en-us/articles/11280166737293
    """
    sources = []
    if BLOCK_ATLAS.is_file():
        for source in json.loads(BLOCK_ATLAS.read_text(encoding="utf-8"))["sources"]:
            source = copy.deepcopy(source)
            # Repair this converter's previous invalid artifact. Verification
            # below never accepts the flat pre-fix schema as native-valid.
            if source["type"].removeprefix("minecraft:") == "filter" and "pattern" not in source:
                pattern = {key: source.pop(key) for key in ("namespace", "path") if key in source}
                if not pattern:
                    raise ValueError("Atlas filter requires a pattern object")
                source["pattern"] = pattern
            sources.append(source)
    for folder in ("items", "blocks"):
        entry = {"type": "minecraft:directory", "source": folder, "prefix": f"{folder}/"}
        if entry not in sources:
            sources.append(entry)
    static_models = []
    for relative, dimensions in STATIC_MODEL_SPRITES.items():
        png = ASSETS / "textures" / f"{relative}.png"
        actual = struct.unpack("!II", png.read_bytes()[16:24])
        if actual != dimensions or Path(str(png) + ".mcmeta").exists():
            raise ValueError(f"Static model sprite requires its original full-image frame: {relative}, {actual}")
        entry = {"type": "minecraft:single", "resource": f"thaumcraft:{relative}"}
        if entry not in sources:
            sources.append(entry)
        static_models.append({"sprite": f"thaumcraft:{relative}", "width": actual[0], "height": actual[1],
                              "frame_contract": "Native 1.21.1 no-animation metadata uses the full image width/height.",
                              "texture": png.relative_to(ROOT).as_posix(), "sha256": sha256(png.read_bytes()),
                              "transformation": "none; original PNG and model UV coordinates retained"})
    referenced = {texture for path in (ASSETS / "models").rglob("*.json")
                  for texture in json.loads(path.read_text(encoding="utf-8")).get("textures", {}).values()
                  if not texture.startswith("#")}
    excluded = []
    for folder in ("items", "blocks"):
        for png in sorted((ASSETS / "textures" / folder).rglob("*.png")):
            width, height = struct.unpack("!II", png.read_bytes()[16:24])
            frame_width, frame_height = atlas_frame_size(png)
            has_metadata = Path(str(png) + ".mcmeta").is_file()
            legacy_strip = width != height and not has_metadata
            limits_mip_four = frame_width % 16 != 0 or frame_height % 16 != 0
            if not legacy_strip and not limits_mip_four:
                continue
            relative = png.relative_to(ASSETS / "textures").with_suffix("").as_posix()
            if f"thaumcraft:{relative}" in referenced:
                raise ValueError(f"Active model sprite requires dedicated atlas handling: thaumcraft:{relative}")
            # Preserve legacy direct-render effect sprites, but do not let an
            # unused 6px/8px animation reduce the entire block atlas mip level.
            entry = {"type": "minecraft:filter",
                     "pattern": {"namespace": "thaumcraft", "path": re.escape(relative)}}
            if entry not in sources:
                sources.append(entry)
            reason = ("Unused legacy animation frames would reduce global mipmap level below 4; pending dedicated renderer, bytes retained."
                      if limits_mip_four else
                      "Legacy non-square direct-render strip without animation metadata; bytes retained outside block atlas.")
            excluded.append({"sprite": f"thaumcraft:{relative}", "width": width, "height": height,
                             "frame_width": frame_width, "frame_height": frame_height, "reason": reason})
    write_json(BLOCK_ATLAS, {"sources": sources})
    return {"file": BLOCK_ATLAS.relative_to(ROOT).as_posix(), "sources": sources,
            "excluded_legacy_strips": excluded,
            "static_model_sprites": static_models,
            "mipmap_level_preserved": 4,
            "purpose": "Merged vanilla block/item atlas supplement for preserved plural legacy directories."}


def atlas_frame_size(png: Path) -> tuple[int, int]:
    width, height = struct.unpack("!II", png.read_bytes()[16:24])
    metadata = Path(str(png) + ".mcmeta")
    animation = json.loads(metadata.read_text(encoding="utf-8")).get("animation") if metadata.is_file() else None
    if not isinstance(animation, dict):
        return width, height
    if "width" in animation:
        return animation["width"], animation.get("height", height)
    if "height" in animation:
        return width, animation["height"]
    frame = min(width, height)
    return frame, frame


def block_atlas_sprites() -> dict[str, Path]:
    """Resolve local sprites using default directories plus the merged supplement.

    This checks atlas inclusion, not merely file existence. Only the source
    types used here (directory/single/filter) are simulated; vanilla core
    entries are represented by their default item/block directory sources.
    """
    textures = ASSETS / "textures"
    sources = [{"type": "directory", "source": name, "prefix": f"{name}/"}
               for name in ("item", "block")]
    if BLOCK_ATLAS.is_file():
        sources.extend(json.loads(BLOCK_ATLAS.read_text(encoding="utf-8"))["sources"])
    sprites: dict[str, Path] = {}
    for source in sources:
        resource_location(source["type"])
        source_type = source["type"].removeprefix("minecraft:")
        if source_type == "directory":
            if not isinstance(source.get("source"), str) or not isinstance(source.get("prefix"), str):
                raise ValueError("Atlas directory requires source and prefix strings")
            directory = textures / source["source"]
            for png in directory.rglob("*.png"):
                sprite_path = source["prefix"] + png.relative_to(directory).with_suffix("").as_posix()
                sprites[f"thaumcraft:{sprite_path}"] = png
        elif source_type == "single":
            resource = source["resource"]
            domain, resource_path = resource_location(resource)
            resource_location(source.get("sprite", resource))
            if domain == "thaumcraft":
                sprites[source.get("sprite", resource)] = textures / f"{resource_path}.png"
        elif source_type == "filter":
            pattern = source.get("pattern")
            if not isinstance(pattern, dict) or "namespace" in source or "path" in source:
                raise ValueError("Atlas filter requires nested pattern object; flat namespace/path is invalid")
            namespace = re.compile(pattern.get("namespace", ".*"))
            path_pattern = re.compile(pattern.get("path", ".*"))
            sprites = {sprite: png for sprite, png in sprites.items()
                       if not (namespace.fullmatch(sprite.split(":", 1)[0])
                               and path_pattern.fullmatch(sprite.split(":", 1)[1]))}
        else:
            raise ValueError(f"Atlas verification does not support source type {source_type}")
    for sprite in sprites:
        resource_location(sprite)
    return sprites


def blockstate_models(data: dict[str, Any], context: str) -> list[str]:
    """Resolve both vanilla blockstate forms, including weighted arrays inside multipart apply."""
    result: list[str] = []
    def applications(value: Any, location: str) -> None:
        values = value if isinstance(value, list) else [value]
        if not values:
            raise ValueError(f"{context} {location} has no model applications")
        for entry in values:
            if not isinstance(entry, dict) or not isinstance(entry.get("model"), str):
                raise ValueError(f"{context} {location} requires a model or a list of model objects")
            result.append(entry["model"])
    if "variants" in data:
        if not isinstance(data["variants"], dict):
            raise ValueError(f"{context} variants must be an object")
        for variant, value in data["variants"].items():
            applications(value, f"variant {variant!r}")
    if "multipart" in data:
        if not isinstance(data["multipart"], list):
            raise ValueError(f"{context} multipart must be an array")
        for index, part in enumerate(data["multipart"]):
            if not isinstance(part, dict) or "apply" not in part:
                raise ValueError(f"{context} multipart[{index}] requires apply")
            applications(part["apply"], f"multipart[{index}].apply")
    if "variants" not in data and "multipart" not in data:
        raise ValueError(f"{context} requires variants or multipart")
    return result


def validate_resources(*, include_composed: bool = True) -> dict[str, int]:
    """Validate output; tiny model-only test fixtures may omit the Java contract."""
    missing: list[str] = []
    texture_refs = 0
    atlas_refs = 0
    model_refs = 0
    sprites = block_atlas_sprites()
    for sprite, png in sprites.items():
        if not png.is_file():
            missing.append(f"Missing atlas sprite file {sprite}")
            continue
        frame_width, frame_height = atlas_frame_size(png)
        expected_static_frame = STATIC_MODEL_SPRITES.get(sprite.removeprefix("thaumcraft:"))
        if expected_static_frame is not None:
            dimensions = struct.unpack("!II", png.read_bytes()[16:24])
            if dimensions != expected_static_frame or Path(str(png) + ".mcmeta").exists():
                missing.append(f"Static model sprite {sprite} violates original full-image frame {expected_static_frame}")
        if frame_width % 16 != 0 or frame_height % 16 != 0:
            missing.append(f"Atlas sprite {sprite} ({frame_width}x{frame_height}) lowers global mipmap level below 4")
    for path in sorted((ASSETS / "models").rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        parent = data.get("parent", "")
        if parent:
            resource_location(parent)
        if parent.startswith("thaumcraft:"):
            model_refs += 1
            relative = parent.split(":", 1)[1]
            if not (ASSETS / "models" / f"{relative}.json").is_file():
                missing.append(f"{path.relative_to(ROOT)} parent {parent}")
        for override in data.get("overrides", []):
            model = override.get("model")
            if not isinstance(model, str) or not model.startswith("thaumcraft:"):
                continue
            model_refs += 1
            relative = model.split(":", 1)[1]
            if not (ASSETS / "models" / f"{relative}.json").is_file():
                missing.append(f"{path.relative_to(ROOT)} override {model}")
        for texture in data.get("textures", {}).values():
            if texture.startswith("#"):
                continue
            resource_location(texture)
            if not texture.startswith("thaumcraft:"):
                missing.append(f"{path.relative_to(ROOT)} texture has unexpected namespace {texture}")
                continue
            texture_refs += 1
            png = sprites.get(texture)
            if png is None:
                missing.append(f"Unstitched atlas texture {texture} in {path.relative_to(ROOT)}")
                png = ASSETS / "textures" / (texture.split(":", 1)[1] + ".png")
            else:
                atlas_refs += 1
            if not png.is_file():
                missing.append(f"{path.relative_to(ROOT)} texture {texture}")
                continue
            content = png.read_bytes()
            if content[:8] != b"\x89PNG\r\n\x1a\n":
                missing.append(f"Unreadable PNG header: {png.relative_to(ROOT)}")
                continue
            width, height = struct.unpack("!II", content[16:24])
            if width != height and not Path(str(png) + ".mcmeta").is_file() and texture.removeprefix("thaumcraft:") not in STATIC_MODEL_SPRITES:
                missing.append(f"Non-square unanimated atlas texture: {texture} {width}x{height}")
    for path in sorted((ASSETS / "blockstates").glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for model_id in blockstate_models(data, str(path.relative_to(ROOT))):
            model_refs += 1
            resource_location(model_id)
            relative = model_id.split(":", 1)[1]
            if not (ASSETS / "models" / f"{relative}.json").is_file():
                missing.append(f"{path.relative_to(ROOT)} model {model_id}")
    sound_refs = 0
    sound_events = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8"))
    sound_event_refs = 0
    for event_key, event in sound_events.items():
        resource_location(f"thaumcraft:{event_key}")
        for sound in event.get("sounds", []):
            name = sound if isinstance(sound, str) else sound["name"]
            domain, relative = resource_location(name)
            if isinstance(sound, dict) and sound.get("type") == "event":
                sound_event_refs += 1
                if domain == "thaumcraft" and relative not in sound_events:
                    missing.append(f"Undefined sound event reference {name}")
                continue
            if domain != "thaumcraft":
                continue
            sound_refs += 1
            if not (ASSETS / "sounds" / f"{relative}.ogg").is_file():
                missing.append(f"Sound {name}")
    if missing:
        raise ValueError("Resource validation failed:\n" + "\n".join(missing))
    checks = {"checked_model_texture_refs": texture_refs, "checked_local_model_refs": model_refs,
            "checked_atlas_sprite_refs": atlas_refs, "atlas_sprites_available": len(sprites),
            "atlas_mipmap_level_guard": 4,
            "checked_sound_refs": sound_refs, "checked_sound_event_ids": len(sound_events),
            "checked_sound_event_refs": sound_event_refs, "missing_references": 0}
    if include_composed:
        checks.update(composed_resources.validate(ROOT, RESOURCE_ROOT, sprites))
    return checks


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verify", action="store_true", help="Validate existing output without writing it")
    args = parser.parse_args()
    if args.verify:
        print(json.dumps(validate_resources(), sort_keys=True))
        return
    if not SOURCE.is_dir():
        raise SystemExit(f"Asset source not found: {SOURCE}")
    copied = copy_art()
    mappings = create_models()
    languages = convert_languages(mappings)
    if any(entry["missing_name_aliases"] for entry in languages):
        raise ValueError(f"Missing foundation language names: {languages}")
    atlas = create_block_atlas()
    write_json(RESOURCE_ROOT / "pack.mcmeta", {
        "pack": {"pack_format": 34,
                 "supported_formats": {"min_inclusive": 34, "max_inclusive": 48},
                 "description": "Thaumcraft 4 resources for Minecraft 1.21.1"}
    })
    checks = validate_resources()
    manifest = {
        "schema_version": 1, "target_minecraft": "1.21.1", "resource_pack_format": 34,
        "data_pack_format": 48,
        "pack_metadata_purpose": "One mod pack exposes 1.21.1 client assets (format 34) and server data (format 48); inclusive metadata range covers both pack types, not additional Minecraft versions.",
        "source_revision": source_revision(),
        "source_revision_policy": "Last commit touching the legacy asset source tree, independent of modern port checkpoints.",
        "text_provenance_hash_policy": "Canonical LF newlines for .mcmeta/.obj/.mtl/.json/.lang; PNG/OGG and other binary bytes are untouched.",
        "source_root": SOURCE.relative_to(ROOT).as_posix(),
        "original_reference_root": ORIGINAL.relative_to(ROOT).as_posix(),
        "purpose": "Source identity trace and foundation asset coverage; not legacy save migration.",
        "visual_parity_validated": False,
        "copied_files": copied, "identity_mappings": mappings,
        "languages": languages, "block_atlas": atlas,
        "sound_events": {key: value for key, value in next(entry for entry in copied if entry.get("role") == "sound_event_definitions").items()
                         if key in ("event_id_mappings", "reference_id_mappings", "source_sha256", "sha256",
                                    "source_hash_policy", "target_hash_policy")},
        "validation": checks, "gaps": GAPS,
    }
    write_json(MANIFEST, manifest)
    print(json.dumps({
        "copied_entries": len(copied), "unique_copied_targets": len({entry["target"] for entry in copied}),
        "foundation_item_models": len(mappings),
        "foundation_block_models": len(list((ASSETS / "models/block").glob("*.json"))),
        "foundation_blockstates": len(list((ASSETS / "blockstates").glob("*.json"))),
        "identity_mappings": len(mappings), "languages": len(languages), **checks,
    }, sort_keys=True))


if __name__ == "__main__":
    main()
