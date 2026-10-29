#!/usr/bin/env python3
"""Convert the Thaumic Tinkerer 1.7.10 resources into the add-on's 1.21.1 resource tree.

Source (read only): a checkout of https://github.com/GTNewHorizons/ThaumicTinkerer pinned to
``SOURCE_COMMIT`` (CC BY-NC-SA 3.0). Target: ``addons/thaumic-tinkerer/common/src/main/resources/
assets/thaumictinkerer``. The script writes

* every texture of ``assets/ttinkerer/textures`` (and the duplicate root ``remotePlacer.png`` when it
  is not already produced) byte-for-byte under a mechanically snake_cased name:
  ``items -> item``, ``blocks -> block``, ``enchants -> enchant``, ``gui``/``misc`` unchanged,
  ``model -> models``; ``.png.mcmeta`` files stay next to their png;
* ``lang/<locale>.json`` for every ``.lang`` file (1.21.1 lowercase locale codes). Text is never
  edited. Keys of the ``tile.<name>.name`` / ``item.<name>.name`` / ``enchantment.ttinkerer:<name>``
  families are renamed to the 1.21.1 translation-key convention; every other family keeps its key
  verbatim. A ``%`` that is not a format specifier is written ``%%``;
* the derived research keys the native Thaumonomicon reads (``tc.research_name.<KEY>`` and
  ``tc.research_text.<KEY>``) next to the untouched ``ttresearch.*`` keys. The text key is the
  ``ttresearch.prefix`` (``ttresearch.prefix.kami`` for the keys created with
  ``new KamiResearchItem(`` in the Java source) followed by ``ttresearch.lore.<KEY>``, exactly as
  ``TTResearchItem.getText()`` builds it;
* every per-group fragment ``addons/thaumic-tinkerer/lang-extra/<group>.<lang>.json`` (new 1.21.1-only
  keys, flat string -> string objects) merged into the same ``lang/<lang>.json``: fragments win over the
  converted base, every collision is printed as a warning, an invalid fragment is a hard error;
* ``scripts/porting/tt_assets_map.json`` (provenance, sha256 of every source file, the complete old ->
  new path mapping, the lang key mapping and the Kami key list) and
  ``addons/thaumic-tinkerer/docs/lang-key-families.md`` (the key family report).

Usage:
    tt_assets.py [--source CHECKOUT] [--check] [--allow-unpinned]

``--check`` recomputes everything and fails (exit 1) when the checked-in output differs; it writes
nothing. Without ``--check`` the output is (re)written and files that an earlier run produced but this
run no longer does are removed. Hand-written files elsewhere under ``assets/thaumictinkerer`` are never
touched. Nothing here reads or contains the original Thaumcraft jars.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import struct
import subprocess
import sys
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = Path("/home/user/gtnewhorizons/thaumictinkerer")
SOURCE_REPO = "https://github.com/GTNewHorizons/ThaumicTinkerer"
SOURCE_COMMIT = "28f4def938e80d7a0b6f433c2b84300dfb7c35ce"
LICENSE_NAME = "CC BY-NC-SA 3.0"
GENERATOR = "scripts/porting/tt_assets.py"

OLD_NS = "ttinkerer"
NEW_NS = "thaumictinkerer"
SOURCE_RESOURCES = "src/main/resources"
SOURCE_JAVA = "src/main/java"
LIB_RESEARCH = "thaumic/tinkerer/common/lib/LibResearch.java"
TARGET_RESOURCES = "addons/thaumic-tinkerer/common/src/main/resources"
MAP_PATH = "scripts/porting/tt_assets_map.json"
DOC_PATH = "addons/thaumic-tinkerer/docs/lang-key-families.md"
LANG_EXTRA = "addons/thaumic-tinkerer/lang-extra"

OLD_ASSETS = f"assets/{OLD_NS}"
NEW_ASSETS = f"assets/{NEW_NS}"
OLD_TEXTURES = f"{OLD_ASSETS}/textures"
NEW_TEXTURES = f"{NEW_ASSETS}/textures"
OLD_LANG = f"{OLD_ASSETS}/lang"
NEW_LANG = f"{NEW_ASSETS}/lang"

# Source texture directory -> target directory. Anything else under textures/ fails loudly.
DIR_MAP = {
    "items": "item",
    "blocks": "block",
    "enchants": "enchant",
    "gui": "gui",
    "misc": "misc",
    "model": "models",
}
# Resource-root files that belong to a texture directory (the target name follows the same rule).
ROOT_TEXTURES = {"remotePlacer.png": "gui"}

# Byte-for-byte copies that 1.21.1 wants under a second path (target below textures/ -> converted source below textures/):
# a mob effect icon is read from textures/mob_effect/<effect id>.png, and an armour material layer from
# textures/models/armor/<layer name>_layer_<1|2>.png. The sources stay where the 1.7.10 code expects them.
DERIVED_COPIES = {
    "mob_effect/potion_air.png": "item/potion_aer.png",
    "mob_effect/potion_earth.png": "item/potion_terra.png",
    "mob_effect/potion_fire.png": "item/potion_ignis.png",
    "mob_effect/potion_water.png": "item/potion_aqua.png",
    "models/armor/ichor_layer_1.png": "models/ichor_1.png",
    "models/armor/ichor_layer_2.png": "models/ichor_2.png",
    "models/armor/ichor_gem_layer_1.png": "models/ichor_gem_1.png",
    "models/armor/ichor_gem_layer_2.png": "models/ichor_gem_2.png",
    "models/armor/revealing_layer_1.png": "models/revealing.png",
}

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
PATH_COMPONENT = re.compile(r"^[a-z0-9._-]+$")
KEY_SEGMENT = re.compile(r"^[a-z0-9_]+$")

PREFIX_KEY = "ttresearch.prefix"
KAMI_PREFIX_KEY = "ttresearch.prefix.kami"
RESEARCH_NAME = re.compile(r"^ttresearch\.name\.([A-Za-z0-9_]+)$")
RESEARCH_LORE = re.compile(r"^ttresearch\.lore\.([A-Za-z0-9_]+)$")
FRAGMENT_NAME = re.compile(r"^([a-z0-9_-]+)\.([a-z]{2}_[a-z]{2})\.json$")


class ConversionError(Exception):
    """The source does not convert cleanly; nothing is written."""


# --------------------------------------------------------------------------- names

_ACRONYM = re.compile(r"([A-Z]+)([A-Z][a-z])")
_CAMEL = re.compile(r"([a-z0-9])([A-Z])")
_TRAILING_DIGITS = re.compile(r"([^_0-9])([0-9]+)$")


def snake_case(stem: str) -> str:
    """Mechanical camelCase -> snake_case: ``focusXPDrain`` -> ``focus_xp_drain``.

    Spaces become ``_``; existing underscores stay; a trailing metadata number gets an underscore
    (``darkQuartz0`` -> ``dark_quartz_0``) unless one is already there (``crop_aer_0``).
    """
    text = stem.replace(" ", "_")
    text = _ACRONYM.sub(r"\1_\2", text)
    text = _CAMEL.sub(r"\1_\2", text)
    text = text.lower()
    return _TRAILING_DIGITS.sub(r"\1_\2", text)


def rename_file(name: str) -> str:
    """New file name for ``<stem>.png`` or ``<stem>.png.mcmeta``."""
    for suffix in (".png.mcmeta", ".png"):
        if name.endswith(suffix):
            stem = name[: -len(suffix)]
            break
    else:
        raise ConversionError(f"unexpected file type in textures: {name!r} (only .png and .png.mcmeta)")
    if not stem:
        raise ConversionError(f"empty file stem: {name!r}")
    renamed = snake_case(stem) + suffix
    if not PATH_COMPONENT.match(renamed):
        raise ConversionError(f"{name!r} -> {renamed!r} is not a legal 1.21.1 resource name ([a-z0-9._-])")
    return renamed


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def dumps(document: object) -> str:
    return json.dumps(document, ensure_ascii=False, indent=2) + "\n"


# --------------------------------------------------------------------------- textures

@dataclass(frozen=True)
class Texture:
    old: str       # relative to src/main/resources
    new: str       # relative to the target resources root
    data: bytes


def png_size(data: bytes, label: str) -> tuple[int, int]:
    if data[:8] != PNG_SIGNATURE or data[12:16] != b"IHDR":
        raise ConversionError(f"{label}: not a PNG file")
    return struct.unpack(">II", data[16:24])


def check_animation(label: str, meta: bytes, size: tuple[int, int]) -> int:
    """Validate a .mcmeta against the 1.21.1 AnimationMetadataSection rules; return the frame count.

    A metadata file without an ``animation`` section counts as one frame.
    """
    try:
        document = json.loads(meta.decode("utf-8-sig"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ConversionError(f"{label}: invalid mcmeta JSON: {error}") from error
    if not isinstance(document, dict):
        raise ConversionError(f"{label}: mcmeta is not a JSON object")
    animation = document.get("animation")
    if animation is None:
        return 1
    if not isinstance(animation, dict):
        raise ConversionError(f"{label}: 'animation' is not an object")
    width, height = size
    frame_w, frame_h = animation.get("width"), animation.get("height")
    if frame_w is None and frame_h is None:
        frame_w = frame_h = min(width, height)
    elif frame_h is None:
        frame_h = height
    elif frame_w is None:
        frame_w = width
    if not all(isinstance(v, int) and v > 0 for v in (frame_w, frame_h)) or width % frame_w or height % frame_h:
        raise ConversionError(f"{label}: image {width}x{height} is not a whole number of {frame_w}x{frame_h} frames")
    count = (width // frame_w) * (height // frame_h)
    for frame in animation.get("frames", []):
        index = frame.get("index") if isinstance(frame, dict) else frame
        if not isinstance(index, int) or not 0 <= index < count:
            raise ConversionError(f"{label}: animation frame {frame!r} is outside 0..{count - 1}")
    frametime = animation.get("frametime", 1)
    if not isinstance(frametime, int) or frametime < 1:
        raise ConversionError(f"{label}: invalid frametime {frametime!r}")
    return count


def collect_textures(resources: Path) -> list[Texture]:
    """Every file below assets/ttinkerer/textures plus the root duplicates, renamed and validated."""
    root = resources / OLD_TEXTURES
    if not root.is_dir():
        raise ConversionError(f"missing texture directory {root}")
    found: list[Texture] = []
    files = sorted((p for p in root.rglob("*") if p.is_file()), key=lambda p: p.relative_to(root).as_posix())
    for path in files:
        parts = path.relative_to(root).parts
        if len(parts) == 1:
            new_parts: tuple[str, ...] = ()
        elif len(parts) == 2 and parts[0] in DIR_MAP:
            new_parts = (DIR_MAP[parts[0]],)
        else:
            raise ConversionError(f"textures/{'/'.join(parts)}: directory not covered by the conversion table")
        old = f"{OLD_TEXTURES}/{'/'.join(parts)}"
        new = "/".join((NEW_TEXTURES, *new_parts, rename_file(parts[-1])))
        found.append(Texture(old, new, path.read_bytes()))
    return found


def merge_textures(resources: Path, textures: list[Texture]) -> tuple[list[Texture], dict[str, dict]]:
    """Collision check, validation, and the root duplicates (copied only when not already produced)."""
    owner: dict[str, Texture] = {}
    for texture in textures:
        if texture.new in owner:
            raise ConversionError(
                f"name collision after renaming: {owner[texture.new].old!r} and {texture.old!r} -> {texture.new!r}")
        owner[texture.new] = texture
    duplicates: dict[str, dict] = {}
    for name, directory in sorted(ROOT_TEXTURES.items()):
        path = resources / name
        if not path.is_file():
            continue
        data = path.read_bytes()
        new = f"{NEW_TEXTURES}/{directory}/{rename_file(name)}"
        if new in owner:
            if owner[new].data != data:
                raise ConversionError(
                    f"{name!r} differs from {owner[new].old!r} but both map to {new!r}: refusing to pick one")
            duplicates[name] = {
                "identical_to": owner[new].old,
                "new": new,
                "copied": False,
                "sha256": sha256(data),
            }
        else:
            texture = Texture(name, new, data)
            owner[new] = texture
            textures = [*textures, texture]
    sizes: dict[str, tuple[int, int]] = {}
    for texture in textures:
        if texture.new.endswith(".png"):
            sizes[texture.new] = png_size(texture.data, texture.old)
    for texture in textures:
        if texture.new.endswith(".png.mcmeta"):
            image = texture.new[: -len(".mcmeta")]
            if image not in sizes:
                raise ConversionError(f"{texture.old}: no png next to it after renaming ({image})")
            check_animation(texture.old, texture.data, sizes[image])
    return sorted(textures, key=lambda t: t.new), duplicates


# --------------------------------------------------------------------------- lang

@dataclass
class LangFile:
    name: str
    entries: dict[str, str]
    lines: dict[str, list[int]]

    @property
    def duplicates(self) -> dict[str, list[int]]:
        return {key: numbers for key, numbers in sorted(self.lines.items()) if len(numbers) > 1}


def parse_lang(text: str, name: str) -> LangFile:
    """1.7.10 ``.lang`` syntax: ``#`` comments, blank lines, ``key=value`` split at the first ``=``.

    Values are never trimmed or edited. A repeated key keeps its last value (the first lines are
    recorded so the duplicate can be reported). Any other line is an error.
    """
    text = text.removeprefix("﻿").replace("\r\n", "\n").replace("\r", "\n")
    entries: dict[str, str] = {}
    lines: dict[str, list[int]] = {}
    for number, line in enumerate(text.split("\n"), 1):
        if not line.strip() or line.startswith("#"):
            continue
        if "=" not in line:
            raise ConversionError(f"{name}:{number}: neither comment, blank nor key=value: {line[:60]!r}")
        key, value = line.split("=", 1)
        if not key or key != key.strip():
            raise ConversionError(f"{name}:{number}: key with leading or trailing whitespace: {key!r}")
        entries[key] = value
        lines.setdefault(key, []).append(number)
    return LangFile(name, entries, lines)


_PERCENT = re.compile(r"%%|%(?:\d+\$)?[\d.]*[A-Za-z]|%")


def escape_percent(value: str, where: str = "") -> str:
    """Make a legacy value safe for Minecraft 1.21's translation parser.

    ``%s`` / ``%n$s`` stay. ``%d`` and ``%f`` also stay: the 1.21 Language loader rewrites them to
    ``%s``. An existing ``%%`` stays. Any other ``%`` that is not directly followed by a format letter
    (``30% discount``, ``20%까지``) is a literal percent and becomes ``%%``. A directly attached other
    letter (``%x``, ``%S``) could be a format or text, so that is a hard error for a human to decide.
    """
    def fix(match: re.Match[str]) -> str:
        token = match.group(0)
        if token == "%":
            return "%%"
        if token == "%%" or token[-1] in "sdf":
            return token
        raise ConversionError(f"{where}: ambiguous percent sequence {token!r} in {value[:60]!r}")
    return _PERCENT.sub(fix, value)


@dataclass(frozen=True)
class Family:
    label: str       # key pattern shown in the report
    pattern: re.Pattern[str]
    action: str      # "mapped" or "verbatim"
    target: str      # new key pattern for mapped families, otherwise a short note


def _family(label: str, pattern: str, action: str, target: str) -> Family:
    return Family(label, re.compile(pattern), action, target)


FAMILIES: tuple[Family, ...] = (
    _family("tile.<name>.name", r"^tile\.([^.]+)\.name$", "mapped", f"block.{NEW_NS}.<snake(name)>"),
    _family("item.<name>.name", r"^item\.([^.]+)\.name$", "mapped", f"item.{NEW_NS}.<snake(name)>"),
    _family("enchantment.ttinkerer:<name>", r"^enchantment\.ttinkerer:([A-Za-z0-9]+)$", "mapped",
            f"enchantment.{NEW_NS}.<snake(name)>"),
    _family("tile.* (other: metadata or variant keys)", r"^tile\.", "verbatim", "kept"),
    _family("item.Wand.<component>.<slot>", r"^item\.Wand\.", "verbatim", "kept (wand component names)"),
    _family("item.* (other: metadata or variant keys)", r"^item\.", "verbatim", "kept"),
    _family("itemGroup.*", r"^itemGroup\.", "verbatim", "kept (the creative tab key is chosen by the code)"),
    _family("ttmisc.*", r"^ttmisc\.", "verbatim", "kept"),
    _family("ttwaila.*", r"^ttwaila\.", "verbatim", "kept"),
    _family("ttcomputer.*", r"^ttcomputer\.", "verbatim", "kept"),
    _family("tt.nei.*", r"^tt\.nei\.", "verbatim", "kept"),
    _family("ttresearch.name.<KEY>", r"^ttresearch\.name\.", "verbatim", "kept; copied to tc.research_name.<KEY>"),
    _family("ttresearch.lore.<KEY>", r"^ttresearch\.lore\.", "verbatim",
            "kept; copied with the prefix to tc.research_text.<KEY>"),
    _family("ttresearch.page.<KEY>.<n>", r"^ttresearch\.page\.", "verbatim", "kept (page translation keys)"),
    _family("ttresearch.webpage.*", r"^ttresearch\.webpage\.", "verbatim", "kept"),
    _family("ttresearch.prefix*", r"^ttresearch\.prefix", "verbatim", "kept; prepended to the lore"),
    _family("tc.research_category.*", r"^tc\.research_category\.", "verbatim", "kept"),
    _family("(other)", r"", "verbatim", "kept"),
)
DERIVED_FAMILIES = (
    ("tc.research_name.<KEY>", "derived", "value of ttresearch.name.<KEY>"),
    ("tc.research_text.<KEY>", "derived", "prefix + value of ttresearch.lore.<KEY>"),
)


def classify_key(key: str) -> tuple[Family, str]:
    """The family of a legacy key and its 1.21.1 key (the key itself for verbatim families)."""
    for family in FAMILIES:
        match = family.pattern.match(key)
        if not match:
            continue
        if family.action == "verbatim":
            return family, key
        segment = snake_case(match.group(1))
        if not KEY_SEGMENT.match(segment):
            raise ConversionError(f"{key!r}: {segment!r} is not a legal translation-key segment")
        return family, family.target.replace("<snake(name)>", segment)
    raise AssertionError("the (other) family matches everything")


def derive_research(values: dict[str, str], fallback: dict[str, str], kami_keys: set[str]) -> tuple[dict[str, str], dict]:
    """``tc.research_name.<KEY>`` / ``tc.research_text.<KEY>`` from one locale's converted values.

    ``fallback`` (en_US) supplies a prefix the locale does not define, the way the game falls back to
    en_US for a missing key. A name without lore (or lore without a name) emits only the half that
    exists; the game then falls back to en_US for the other half. Both are reported.
    """
    names = {m.group(1): v for k, v in values.items() if (m := RESEARCH_NAME.match(k))}
    lores = {m.group(1): v for k, v in values.items() if (m := RESEARCH_LORE.match(k))}
    derived: dict[str, str] = {}
    prefix_fallback: set[str] = set()
    for key, value in names.items():
        derived[f"tc.research_name.{key}"] = value
    for key, value in lores.items():
        prefix_key = KAMI_PREFIX_KEY if key in kami_keys else PREFIX_KEY
        if prefix_key in values:
            prefix = values[prefix_key]
        elif prefix_key in fallback:
            prefix = fallback[prefix_key]
            prefix_fallback.add(prefix_key)
        else:
            raise ConversionError(f"ttresearch.lore.{key}: no {prefix_key} in this locale or in en_US")
        derived[f"tc.research_text.{key}"] = prefix + value
    info = {
        "names": len(names),
        "lores": len(lores),
        "name_without_lore": sorted(set(names) - set(lores)),
        "lore_without_name": sorted(set(lores) - set(names)),
        "kami_texts": sorted(set(lores) & kami_keys),
        "prefix_fallback": sorted(prefix_fallback),
    }
    return derived, info


@dataclass
class LocaleResult:
    locale: str
    source: str                       # old relative path of the .lang
    target: str                       # new relative path of the .json
    values: dict[str, str]            # final JSON content (sorted)
    key_map: dict[str, str]           # old key -> new key, renamed keys only
    family_counts: Counter = field(default_factory=Counter)
    family_keys: dict[str, list[str]] = field(default_factory=dict)
    percent: list[tuple[str, str, str]] = field(default_factory=list)   # key, before, after
    duplicates: dict[str, list[int]] = field(default_factory=dict)
    source_keys: int = 0
    research: dict = field(default_factory=dict)
    fragments: list[tuple[str, int]] = field(default_factory=list)      # fragment path, keys in it
    overridden: list[tuple[str, str, str]] = field(default_factory=list)  # key, fragment path, what it replaced


def convert_lang(lang: LangFile, locale: str, source: str, target: str) -> LocaleResult:
    result = LocaleResult(locale, source, target, {}, {})
    result.duplicates = lang.duplicates
    result.source_keys = len(lang.entries)
    owner: dict[str, str] = {}
    for key, value in lang.entries.items():
        family, new_key = classify_key(key)
        if new_key in owner:
            raise ConversionError(f"{lang.name}: {key!r} and {owner[new_key]!r} both become {new_key!r}")
        owner[new_key] = key
        escaped = escape_percent(value, f"{lang.name}: {key}")
        if escaped != value:
            result.percent.append((key, value, escaped))
        result.values[new_key] = escaped
        if new_key != key:
            result.key_map[key] = new_key
        result.family_counts[family.label] += 1
        result.family_keys.setdefault(family.label, []).append(key)
    result.values = dict(sorted(result.values.items()))
    return result


def add_research_keys(results: dict[str, LocaleResult], kami_keys: set[str]) -> None:
    fallback = results["en_us"].values if "en_us" in results else {}
    for locale, result in sorted(results.items()):
        derived, info = derive_research(result.values, fallback, kami_keys)
        clash = sorted(set(derived) & set(result.values))
        if clash:
            raise ConversionError(f"{result.source}: derived research keys already exist: {clash[:3]}")
        result.values = dict(sorted({**result.values, **derived}.items()))
        result.research = info
        result.family_counts["tc.research_name.<KEY>"] = info["names"]
        result.family_counts["tc.research_text.<KEY>"] = info["lores"]


# --------------------------------------------------------------------------- lang fragments

def _no_duplicate_keys(pairs: list[tuple[str, object]]) -> dict[str, object]:
    seen: dict[str, object] = {}
    for key, value in pairs:
        if key in seen:
            raise ValueError(f"duplicate key {key!r}")
        seen[key] = value
    return seen


def read_fragment(path: Path, label: str) -> dict[str, str]:
    """A fragment is a flat JSON object whose values are all strings; anything else is a hard error."""
    try:
        document = json.loads(path.read_text(encoding="utf-8-sig"), object_pairs_hook=_no_duplicate_keys)
    except ValueError as error:  # JSONDecodeError and UnicodeDecodeError are ValueErrors
        raise ConversionError(f"{label}: invalid JSON: {error}") from error
    if not isinstance(document, dict):
        raise ConversionError(f"{label}: must be a flat JSON object of strings")
    for key, value in document.items():
        if not isinstance(value, str):
            raise ConversionError(f"{label}: the value of {key!r} is not a string")
    return document


def fragment_files(root: Path) -> list[Path]:
    directory = root / LANG_EXTRA
    if not directory.is_dir():
        return []
    return sorted((p for p in directory.iterdir() if p.is_file() and p.name.endswith(".json")), key=lambda p: p.name)


def merge_fragments(results: dict[str, LocaleResult], root: Path) -> list[str]:
    """Merge ``lang-extra/<group>.<lang>.json`` into each locale; return the collision warnings.

    Fragments are applied in file-name order after the converted base; a later value wins, so extras win
    over the base and, on a clash between two groups, the later file name wins. Every collision warns.
    """
    by_locale: dict[str, list[tuple[Path, str]]] = {}
    for path in fragment_files(root):
        label = f"{LANG_EXTRA}/{path.name}"
        match = FRAGMENT_NAME.match(path.name)
        if not match:
            raise ConversionError(f"{label}: fragment files are named <group>.<lang>.json (a-z, 0-9, _ and -)")
        language = match.group(2)
        if language not in results:
            raise ConversionError(f"{label}: no base language {language} (have: {', '.join(sorted(results))})")
        by_locale.setdefault(language, []).append((path, label))
    warnings: list[str] = []
    for language, items in sorted(by_locale.items()):
        result = results[language]
        merged = dict(result.values)
        origin: dict[str, str] = {}
        for path, label in items:
            fragment = read_fragment(path, label)
            for key, value in fragment.items():
                if key in merged:
                    replaced = origin.get(key, "the converted .lang base")
                    result.overridden.append((key, label, replaced))
                    warnings.append(f"{label}: key {key!r} overrides {replaced}")
                merged[key] = value
                origin[key] = label
            result.fragments.append((label, len(fragment)))
        result.values = dict(sorted(merged.items()))
    return warnings


# --------------------------------------------------------------------------- Kami research keys

def strip_java_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


_KAMI_SITE = re.compile(r"\bnew\s+KamiResearchItem\s*\(\s*([^,()]+?)\s*,")
_STRING_CONSTANT = re.compile(r"""public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]*)"\s*;""")


def kami_research_keys(source: Path) -> tuple[dict[str, str], dict[str, str]]:
    """Research keys created with ``new KamiResearchItem(`` -> ``path:line``, and sha256 of the files read."""
    java = source / SOURCE_JAVA
    lib_path = java / LIB_RESEARCH
    if not lib_path.is_file():
        raise ConversionError(f"missing {lib_path}")
    constants = dict(_STRING_CONSTANT.findall(strip_java_comments(lib_path.read_text(encoding="utf-8"))))
    keys: dict[str, str] = {}
    hashes = {f"{SOURCE_JAVA}/{LIB_RESEARCH}": sha256(lib_path.read_bytes())}
    for path in sorted(java.rglob("*.java"), key=lambda p: p.relative_to(java).as_posix()):
        raw = path.read_text(encoding="utf-8")
        if "KamiResearchItem" not in raw:
            continue
        text = strip_java_comments(raw)
        for match in _KAMI_SITE.finditer(text):
            argument = match.group(1)
            line = text.count("\n", 0, match.start()) + 1
            relative = f"{SOURCE_JAVA}/{path.relative_to(java).as_posix()}"
            if argument.startswith('"') and argument.endswith('"'):
                key = argument[1:-1]
            else:
                name = argument.rsplit(".", 1)[-1]
                if name not in constants:
                    raise ConversionError(f"{relative}:{line}: cannot resolve research key {argument!r}")
                key = constants[name]
            if key in keys:
                raise ConversionError(f"research key {key!r} is created twice as Kami ({keys[key]}, {relative}:{line})")
            keys[key] = f"{relative}:{line}"
            hashes[relative] = sha256(path.read_bytes())
    if not keys:
        raise ConversionError("no 'new KamiResearchItem(' site found: wrong source checkout?")
    return dict(sorted(keys.items())), dict(sorted(hashes.items()))


# --------------------------------------------------------------------------- plan

@dataclass
class Plan:
    outputs: dict[str, bytes]               # repo-relative posix path -> content
    mapping: dict[str, str]                 # old (resources-relative) -> new (target-resources-relative)
    textures: list[Texture]
    duplicates: dict[str, dict]
    locales: dict[str, LocaleResult]
    kami_keys: dict[str, str]
    document: dict
    warnings: list[str] = field(default_factory=list)


def build(source: Path, root: Path = ROOT) -> Plan:
    resources = source / SOURCE_RESOURCES
    textures, duplicates = merge_textures(resources, collect_textures(resources))
    lang_dir = resources / OLD_LANG
    if not lang_dir.is_dir():
        raise ConversionError(f"missing lang directory {lang_dir}")
    lang_files = sorted(lang_dir.iterdir(), key=lambda p: p.name)
    hashes: dict[str, str] = {}
    results: dict[str, LocaleResult] = {}
    for path in lang_files:
        if not path.is_file() or path.suffix != ".lang":
            raise ConversionError(f"unexpected entry in lang directory: {path.name}")
        locale = path.stem.lower()
        if locale in results:
            raise ConversionError(f"two lang files map to {locale}.json: {results[locale].source} and {path.name}")
        data = path.read_bytes()
        old = f"{OLD_LANG}/{path.name}"
        hashes[old] = sha256(data)
        lang = parse_lang(data.decode("utf-8"), path.name)
        results[locale] = convert_lang(lang, locale, old, f"{NEW_LANG}/{locale}.json")
    if not results:
        raise ConversionError("no .lang files found")
    kami, java_hashes = kami_research_keys(source)
    add_research_keys(results, set(kami))
    warnings = merge_fragments(results, root)

    by_new = {t.new: t for t in textures}
    derived: dict[str, str] = {}
    for target, source in sorted(DERIVED_COPIES.items()):
        origin = by_new.get(f"{NEW_TEXTURES}/{source}")
        if origin is None:
            raise ConversionError(f"derived copy {target!r}: no converted texture {source!r}")
        new = f"{NEW_TEXTURES}/{target}"
        if new in by_new:
            raise ConversionError(f"derived copy {target!r} collides with a converted texture")
        derived[new] = origin.new

    mapping = {t.old: t.new for t in textures}
    for texture in textures:
        hashes[texture.old] = sha256(texture.data)
    for name, entry in duplicates.items():
        hashes[name] = entry["sha256"]

    outputs: dict[str, bytes] = {}
    if not fragment_files(root):
        outputs[f"{LANG_EXTRA}/.gitkeep"] = b""  # keeps the (still empty) fragment directory in git
    for texture in textures:
        outputs[f"{TARGET_RESOURCES}/{texture.new}"] = texture.data
    for new, source in derived.items():
        outputs[f"{TARGET_RESOURCES}/{new}"] = by_new[source].data
    for result in results.values():
        outputs[f"{TARGET_RESOURCES}/{result.target}"] = dumps(result.values).encode("utf-8")

    lang_key_map = dict(sorted({k: v for r in results.values() for k, v in r.key_map.items()}.items()))
    for old_key, new_key in lang_key_map.items():  # one old key may never map to two new keys
        for r in results.values():
            if old_key in r.key_map and r.key_map[old_key] != new_key:
                raise ConversionError(f"{old_key!r} maps differently in {r.locale}")
    document = {
        "provenance": {
            "source_repo": SOURCE_REPO,
            "source_commit": SOURCE_COMMIT,
            "license": LICENSE_NAME,
            "generator": GENERATOR,
            "source_resources_root": SOURCE_RESOURCES,
            "target_resources_root": TARGET_RESOURCES,
            "source_namespace": OLD_NS,
            "target_namespace": NEW_NS,
            "note": "Texture bytes are copied unchanged. Paths in 'mapping' are relative to the two resource roots.",
        },
        "directory_map": dict(sorted(DIR_MAP.items())),
        "duplicates": dict(sorted(duplicates.items())),
        "derived_copies": dict(sorted(derived.items())),
        "lang_files": {r.source: r.target for _, r in sorted(results.items())},
        "lang_key_map": lang_key_map,
        "kami_research": {"keys": kami, "java_sha256": java_hashes},
        "files": dict(sorted(hashes.items())),
        "mapping": dict(sorted(mapping.items())),
    }
    plan = Plan(outputs, mapping, textures, duplicates, results, kami, document, warnings)
    outputs[MAP_PATH] = dumps(document).encode("utf-8")
    outputs[DOC_PATH] = render_doc(plan).encode("utf-8")
    return plan


# --------------------------------------------------------------------------- report

LOCALE_ORDER = ("en_us", "en_gb")


def ordered_locales(results: dict[str, LocaleResult]) -> list[str]:
    return sorted(results, key=lambda l: (LOCALE_ORDER.index(l) if l in LOCALE_ORDER else len(LOCALE_ORDER), l))


def excerpt(value: str, marker: str = "%", width: int = 14) -> str:
    at = value.find(marker)
    return "..." + value[max(0, at - width): at + width + 1].replace("|", "\\|") + "..."


def row(*cells: object) -> str:
    return "| " + " | ".join(str(c) for c in cells) + " |"


def render_doc(plan: Plan) -> str:
    locales = ordered_locales(plan.locales)
    results = plan.locales
    out: list[str] = []
    add = out.append
    add("# Thaumic Tinkerer lang key families")
    add("")
    add(f"Generated by `{GENERATOR}` from {SOURCE_REPO} at `{SOURCE_COMMIT}` ({LICENSE_NAME}). "
        "Do not edit by hand: `python3 scripts/porting/tt_assets.py --check` fails when this file is stale.")
    add("")
    add("No text was translated or edited. A value changed only where a literal `%` became `%%` (see below).")
    add("")
    add("## Locale files")
    add("")
    add(row("Source", "Target", "Keys in .lang", "Keys in .json", "Renamed keys", "Duplicates", "Escaped `%`",
            "Fragment files", "Fragment keys"))
    add(row(*["---"] * 9))
    for locale in locales:
        r = results[locale]
        add(row(f"`{r.source.rsplit('/', 1)[1]}`", f"`{r.target.rsplit('/', 1)[1]}`", r.source_keys, len(r.values),
                len(r.key_map), len(r.duplicates), len(r.percent), len(r.fragments),
                sum(n for _, n in r.fragments)))
    add("")
    add("## Key families")
    add("")
    add("`mapped` families are renamed to the 1.21.1 convention, `verbatim` families keep the legacy key, "
        "`derived` families are added next to their untouched `ttresearch.*` source keys. "
        "Counts are keys per locale after duplicates were folded.")
    add("")
    add(row("Family", "Action", "New key / note", *locales, "Union"))
    add(row(*["---"] * (3 + len(locales) + 1)))
    labels = [f.label for f in FAMILIES]
    union: dict[str, set[str]] = {label: set() for label in labels}
    for r in results.values():
        for label, keys in r.family_keys.items():
            union[label].update(keys)
    for family in FAMILIES:
        if not union[family.label]:
            continue
        add(row(f"`{family.label}`", family.action, f"`{family.target}`" if family.action == "mapped" else family.target,
                *[results[l].family_counts.get(family.label, 0) for l in locales], len(union[family.label])))
    for label, action, note in DERIVED_FAMILIES:
        counts = [results[l].family_counts.get(label, 0) for l in locales]
        union_keys = set()
        for r in results.values():
            union_keys.update(k for k in r.values if k.startswith(label.split("<")[0]))
        add(row(f"`{label}`", action, note, *counts, len(union_keys)))
    verbatim_families = [f.label for f in FAMILIES if f.action == "verbatim" and union[f.label]]
    add("")
    add(f"Verbatim-kept key families: **{len(verbatim_families)}**.")

    add("")
    add("## Derived research keys")
    add("")
    add("The native Thaumonomicon reads an entry's name and summary only from `tc.research_name.<KEY>` and "
        "`tc.research_text.<KEY>`. They are generated per locale from the untouched `ttresearch.*` keys:")
    add("")
    add("* `tc.research_name.<KEY>` = value of `ttresearch.name.<KEY>`.")
    add("* `tc.research_text.<KEY>` = value of `ttresearch.prefix` (or `ttresearch.prefix.kami` for a Kami "
        "entry) immediately followed by the value of `ttresearch.lore.<KEY>`, with no separator added, "
        "as `TTResearchItem.getText()` builds it. The prefixes keep their own trailing space where a locale "
        "has one (`de_de`, `ko_kr`); `en_us` has none.")
    add("* `ttresearch.page.<KEY>.<n>` stay verbatim: they are the page translation keys.")
    add("* The original adds the prefix only while the config option `tooltipIndicators.enabled` is true "
        "(default true, `ConfigHandler.useTootlipIndicators`). The converter assumes the default; "
        "the option is the code port's decision.")
    add("")
    kami = plan.kami_keys
    add(f"Kami entries (`new KamiResearchItem(` in the Java source, key resolved through `LibResearch`): **{len(kami)}**.")
    add("")
    add(row("Research key", "Created at"))
    add(row("---", "---"))
    for key, where in kami.items():
        add(row(f"`{key}`", f"`{where}`"))
    add("")
    add("Per locale (names / lore entries, mismatches, Kami texts, prefix taken from en_US):")
    add("")
    add(row("Locale", "name keys", "lore keys", "name without lore", "lore without name", "Kami texts", "prefix from en_US"))
    add(row(*["---"] * 7))
    for locale in locales:
        info = results[locale].research

        def cell(items: list[str]) -> str:
            return ", ".join(f"`{i}`" for i in items) if items else "none"
        add(row(f"`{locale}`", info["names"], info["lores"], cell(info["name_without_lore"]),
                cell(info["lore_without_name"]), len(info["kami_texts"]), cell(info["prefix_fallback"])))
    en = results.get("en_us")
    if en:
        missing = sorted(set(kami) - {k.removeprefix("ttresearch.lore.") for k in en.values if k.startswith("ttresearch.lore.")})
        add("")
        add("Kami keys without a `ttresearch.lore.<KEY>` in en_US: " + (", ".join(f"`{k}`" for k in missing) or "none") + ".")

    add("")
    add("## Lang fragments")
    add("")
    add(f"New 1.21.1-only keys live in `{LANG_EXTRA}/<group>.<lang>.json` (flat string objects) and are merged "
        "into `lang/<lang>.json` after the conversion; a fragment key replaces a converted key of the same name "
        "and the script prints a warning for it.")
    add("")
    add(row("Locale", "Fragment", "Keys", "Replaced keys"))
    add(row("---", "---", "---", "---"))
    listed = 0
    for locale in locales:
        for label, count in results[locale].fragments:
            replaced = [k for k, by, _ in results[locale].overridden if by == label]
            add(row(f"`{locale}`", f"`{label}`", count, ", ".join(f"`{k}`" for k in replaced) or "none"))
            listed += 1
    if not listed:
        add(row("(no fragments yet)", "", "", ""))

    add("")
    add("## Verbatim-kept `tile.` / `item.` keys (decide by hand)")
    add("")
    add("These cannot be mapped mechanically (extra metadata or variant segments, or not a `.name` key). "
        "They are in the JSON files under their legacy key.")
    add("")
    add(row("Key", "Locales"))
    add(row("---", "---"))
    verbatim_item_keys: dict[str, list[str]] = {}
    for locale in locales:
        for label in ("tile.* (other: metadata or variant keys)", "item.Wand.<component>.<slot>",
                      "item.* (other: metadata or variant keys)"):
            for key in results[locale].family_keys.get(label, []):
                verbatim_item_keys.setdefault(key, []).append(locale)
    for key, where in sorted(verbatim_item_keys.items()):
        add(row(f"`{key}`", ", ".join(where)))
    if not verbatim_item_keys:
        add(row("(none)", ""))

    add("")
    add("## Mapped `tile.` / `item.` / `enchantment.` keys")
    add("")
    add("Old key -> new key. The name segment is `snake_case` of the legacy name (same function as the "
        "texture names). Whether a registry id exists for each name is the code port's decision; items that "
        "had metadata variants in 1.7.10 (`mobAspect`, `infusedPotion`, `kamiResource`, `*Talisman`, `*Fire`) "
        "keep only their base name here.")
    add("")
    add(row("Old key", "New key", "Locales"))
    add(row("---", "---", "---"))
    where_mapped: dict[str, list[str]] = {}
    for locale in locales:
        for old_key in results[locale].key_map:
            where_mapped.setdefault(old_key, []).append(locale)
    for old_key, new_key in plan.document["lang_key_map"].items():
        add(row(f"`{old_key}`", f"`{new_key}`", ", ".join(where_mapped[old_key])))

    add("")
    add("## Escaped percent signs")
    add("")
    add("A `%` that is not a format specifier (`%s`, `%n$s`, `%d`, `%f`) is written `%%`, which both "
        "`Component.translatable` and `String.format` render as one `%`. A raw `%` there would make the "
        "1.21.1 `TranslatableContents` parser fall back to showing the unformatted string. A consumer that "
        "reads the raw string through `Language.getOrDefault` shows `%%`.")
    add("")
    add(row("Locale", "Key", "Context"))
    add(row("---", "---", "---"))
    changed = 0
    for locale in locales:
        for key, before, after in results[locale].percent:
            add(row(f"`{locale}`", f"`{key}`", f"`{excerpt(before)}` -> `{excerpt(after)}`"))
            changed += 1
    if not changed:
        add(row("(none)", "", ""))

    add("")
    add("## Duplicate keys (last one wins)")
    add("")
    add(row("Locale", "Key", "Lines"))
    add(row("---", "---", "---"))
    dups = 0
    for locale in locales:
        for key, numbers in results[locale].duplicates.items():
            add(row(f"`{locale}`", f"`{key}`", ", ".join(map(str, numbers))))
            dups += 1
    if not dups:
        add(row("(none)", "", ""))

    add("")
    add("## Legacy resource paths inside values")
    add("")
    add("Text is not edited, so these values still name the 1.7.10 path. The new path is where the file "
        "went; the code port must rewrite the reference or the research renderer must understand it.")
    add("")
    add(row("Locale", "Key", "Legacy path", "New path"))
    add(row("---", "---", "---", "---"))
    embedded = re.compile(rf"{OLD_NS}:(textures/[A-Za-z0-9_/.-]+?\.png)")
    found = 0
    for locale in locales:
        for key, value in results[locale].values.items():
            for path in embedded.findall(value):
                new = plan.mapping.get(f"{OLD_ASSETS}/{path}")
                add(row(f"`{locale}`", f"`{key}`", f"`{OLD_NS}:{path}`",
                        f"`{NEW_NS}:{new.removeprefix(NEW_ASSETS + '/')}`" if new else "(no such source file)"))
                found += 1
    if not found:
        add(row("(none)", "", "", ""))

    if "en_us" in results:
        base = {k for keys in results["en_us"].family_keys.values() for k in keys}
        add("")
        add("## Keys that exist in another locale but not in en_US")
        add("")
        add(row("Locale", "Keys"))
        add(row("---", "---"))
        extra = 0
        for locale in locales:
            if locale == "en_us":
                continue
            others = sorted(k for keys in results[locale].family_keys.values() for k in keys if k not in base)
            if others:
                add(row(f"`{locale}`", ", ".join(f"`{k}`" for k in others)))
                extra += 1
        if not extra:
            add(row("(none)", ""))
    add("")
    return "\n".join(out)


# --------------------------------------------------------------------------- apply / check

def orphans(plan: Plan, root: Path) -> list[str]:
    """Texture outputs an earlier run listed in the checked-in map that this run no longer produces."""
    map_file = root / MAP_PATH
    if not map_file.is_file():
        return []
    try:
        previous = json.loads(map_file.read_text(encoding="utf-8"))
        old = {f"{TARGET_RESOURCES}/{new}" for new in previous.get("mapping", {}).values()}
        old |= {f"{TARGET_RESOURCES}/{e['new']}" for e in previous.get("duplicates", {}).values() if e.get("copied")}
        old |= {f"{TARGET_RESOURCES}/{new}" for new in previous.get("derived_copies", {})}
        old |= {f"{TARGET_RESOURCES}/{new}" for new in previous.get("lang_files", {}).values()}
    except (OSError, ValueError, AttributeError):
        return []
    return sorted(p for p in old - set(plan.outputs) if (root / p).is_file())


def apply(plan: Plan, root: Path) -> dict[str, int]:
    counts = {"written": 0, "unchanged": 0, "removed": 0}
    stale = orphans(plan, root)  # read the previous map before it is overwritten below
    for relative, data in sorted(plan.outputs.items()):
        path = root / relative
        if path.is_file() and path.read_bytes() == data:
            counts["unchanged"] += 1
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        counts["written"] += 1
    for relative in stale:
        path = root / relative
        path.unlink()
        counts["removed"] += 1
        parent = path.parent
        while parent != root / TARGET_RESOURCES and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent
    return counts


def check(plan: Plan, root: Path) -> list[str]:
    problems: list[str] = []
    for relative, data in sorted(plan.outputs.items()):
        path = root / relative
        if not path.is_file():
            problems.append(f"missing: {relative}")
        elif path.read_bytes() != data:
            problems.append(f"differs: {relative}")
    problems.extend(f"stale (no longer produced): {relative}" for relative in orphans(plan, root))
    return problems


def verify_pin(source: Path) -> None:
    try:
        completed = subprocess.run(["git", "-C", str(source), "rev-parse", "HEAD"], capture_output=True, text=True,
                                   check=True)
    except (OSError, subprocess.CalledProcessError) as error:
        raise ConversionError(f"cannot read the commit of {source} ({error}); pass --allow-unpinned to skip") from error
    head = completed.stdout.strip()
    if head != SOURCE_COMMIT:
        raise ConversionError(f"{source} is at {head}, the conversion is pinned to {SOURCE_COMMIT}; "
                              "pass --allow-unpinned to convert anyway")


def summary(plan: Plan) -> list[str]:
    per_dir = Counter()
    animated = 0
    for texture in plan.textures:
        parts = texture.new.removeprefix(NEW_TEXTURES + "/").split("/")
        per_dir[parts[0] if len(parts) > 1 else "(root)"] += 1
        animated += texture.new.endswith(".png.mcmeta")
    lines = [f"textures copied: {len(plan.textures)} ({animated} .mcmeta) " +
             ", ".join(f"{d}={n}" for d, n in sorted(per_dir.items()))]
    if plan.duplicates:
        lines.append("duplicates not copied: " + ", ".join(
            f"{name} (identical to {entry['identical_to']})" for name, entry in plan.duplicates.items()))
    for locale in ordered_locales(plan.locales):
        r = plan.locales[locale]
        lines.append(f"lang {locale}.json: {len(r.values)} keys ({r.source_keys} in .lang, {len(r.key_map)} renamed, "
                     f"{len(r.percent)} escaped, {len(r.duplicates)} duplicate)")
    lines.append(f"Kami research keys: {len(plan.kami_keys)}")
    return lines


def main(argv: list[str] | None = None, root: Path = ROOT) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE, help="Thaumic Tinkerer checkout (read only)")
    parser.add_argument("--check", action="store_true", help="verify the checked-in output, write nothing")
    parser.add_argument("--allow-unpinned", action="store_true", help=f"do not require commit {SOURCE_COMMIT[:12]}")
    args = parser.parse_args(argv)
    try:
        if not args.allow_unpinned:
            verify_pin(args.source)
        plan = build(args.source, root)
    except ConversionError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    for warning in plan.warnings:
        print(f"warning: {warning}", file=sys.stderr)
    for line in summary(plan):
        print(line)
    if args.check:
        problems = check(plan, root)
        for problem in problems[:50]:
            print(problem)
        if len(problems) > 50:
            print(f"... and {len(problems) - 50} more")
        print("check: " + (f"FAILED ({len(problems)} problems)" if problems else f"ok ({len(plan.outputs)} files)"))
        return 1 if problems else 0
    counts = apply(plan, root)
    print(f"written {counts['written']}, unchanged {counts['unchanged']}, removed {counts['removed']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
