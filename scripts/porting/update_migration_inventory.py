"""Refresh source fingerprints without automatically promoting gameplay status."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
INVENTORY = ROOT / "docs/porting/class-inventory.json"


def source_hash(path: Path) -> str:
    # Git checkouts may use CRLF on Windows. Java content fingerprints are portable.
    text = path.read_bytes().decode("utf-8-sig").replace("\r\n", "\n").replace("\r", "\n")
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def refresh() -> dict:
    data = json.loads(INVENTORY.read_text(encoding="utf-8"))
    data["fingerprint_policy"] = "Java UTF-8 without BOM, CRLF/CR normalized to LF; gameplay status remains explicitly authored."
    legacy = {path.relative_to(ROOT).as_posix(): path for path in (ROOT / "src/main/java").rglob("*.java")}
    entries = data["legacy_units"]
    if set(legacy) != {row["legacy_path"] for row in entries}:
        raise ValueError("Legacy unit set changed; update the authored migration map explicitly")
    for row in entries:
        path = legacy[row["legacy_path"]]
        row["legacy_sha256"] = source_hash(path)
        row["legacy_lines"] = len(path.read_text(encoding="utf-8").splitlines())
        for native in row["native_paths"]:
            if not (ROOT / native).is_file():
                raise ValueError(f"Missing authored native counterpart: {native}")
    previous_roles = {row["path"]: row["role"] for row in data["active_native_sources"]}
    native_files = sorted(path for module in ("common", "fabric", "neoforge")
                          for path in (ROOT / module / "src/main/java").rglob("*.java"))
    sources = []
    for path in native_files:
        name = path.relative_to(ROOT).as_posix()
        role = previous_roles.get(name, "runtime_test_support" if "/testing/" in name else "native_foundation")
        sources.append({"path": name, "role": role, "sha256": source_hash(path)})
    data["active_native_sources"] = sources
    data["counts"] = {"legacy_units": len(entries),
                      **{status: sum(row["status"] == status for row in entries)
                         for status in ("partial", "pending", "complete")},
                      "active_native_java_files": len(sources)}
    return data


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true", help="Refresh hashes/counts, preserving authored statuses")
    args = parser.parse_args()
    data = refresh()
    if args.write:
        INVENTORY.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    elif data != json.loads(INVENTORY.read_text(encoding="utf-8")):
        raise SystemExit("Migration source inventory is stale; run with --write")
    print(json.dumps({"mode": "write" if args.write else "check", **data["counts"]}))


if __name__ == "__main__":
    main()
