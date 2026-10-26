#!/usr/bin/env python3
"""Keep the NeoForge GameTest catalogue in step with the Fabric one.

The Fabric catalogue (fabric/src/gameTest/.../ThaumcraftGameTests.java) is the authored list. The
NeoForge catalogue wraps the same shared checks. Before 2026-10-25 it had drifted to 307 of 469
wrappers, and one stale body no longer compiled.

    --check  (default) every Fabric wrapper has a NeoForge wrapper of the same name, unless its body
             uses a Fabric-native fixture (FABRIC_ONLY_FIXTURES); NeoForge has no wrapper Fabric lacks.
    --write  regenerate the NeoForge catalogue: Fabric order, comments and annotations, translated to
             the NeoForge template. A NeoForge body that differs from the Fabric one (networked test
             players, knowledge-capturing sessions) is kept, as are the NeoForge helper methods.

Deterministic and offline. It reads and writes only the two catalogue files.
"""
import argparse
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
FABRIC = Path("fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java")
NEOFORGE = Path("neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTests.java")
# Fixtures that drive a Fabric-only API (Transfer API storages, Fabric loot events, the Fabric
# protection probe). Their NeoForge counterparts are capabilities and events with no shared fixture.
FABRIC_ONLY_FIXTURES = (
    "BoreProtectionProbe",
    "LootSourceRegressionChecks",
    "SpaFluidStorageRuntimeChecks",
    "FluidConservationRuntimeChecks",
    "PureBucketStorageRuntimeChecks",
)
# NeoForge bodies that no longer compile against the shared checks; the Fabric body replaces them.
STALE_NEOFORGE = frozenset({"magicalFlowersGrowHarvestAndRefine"})
NEOFORGE_HELPERS = ("notePlayer", "purchaseSession")
_SIGNATURE = re.compile(r"\s*((?:@\w+(?:\([^)]*\))?\s*)*)public void (\w+)\(GameTestHelper (\w+)\)\s*")


def _match(src, start, open_c, close_c):
    """Index of the bracket closing the one at ``start``; string literals are skipped."""
    depth = 0
    in_str = escaped = False
    for j in range(start, len(src)):
        ch = src[j]
        if in_str:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_str = False
        elif ch == '"':
            in_str = True
        elif ch == open_c:
            depth += 1
        elif ch == close_c:
            depth -= 1
            if depth == 0:
                return j
    raise ValueError("unbalanced %s%s from offset %d" % (open_c, close_c, start))


def methods(src):
    """Every @GameTest wrapper: annotation, extra annotations, name, parameter and body."""
    out = []
    for found in re.finditer(r"@GameTest\(", src):
        a0 = found.start()
        a1 = _match(src, found.end() - 1, "(", ")") + 1
        sig = _SIGNATURE.match(src, a1)
        if not sig:
            raise ValueError("no wrapper signature after " + src[a0:a1])
        b0 = sig.end()
        if src[b0] != "{":
            raise ValueError("wrapper %s has no body" % sig.group(2))
        b1 = _match(src, b0, "{", "}") + 1
        out.append(dict(ann=src[a0:a1], extra=sig.group(1), name=sig.group(2), param=sig.group(3),
                        body=src[b0:b1], start=src.rfind("\n", 0, a0) + 1, end=b1))
    return out


def fabric_only(method):
    return any(re.search(r"\b" + fixture + r"\b", method["body"]) for fixture in FABRIC_ONLY_FIXTURES)


def check(fabric_src, neoforge_src):
    """Problems as sentences; empty when the catalogues agree."""
    fabric = methods(fabric_src)
    neoforge = {m["name"] for m in methods(neoforge_src)}
    expected = [m["name"] for m in fabric if not fabric_only(m)]
    problems = ["NeoForge lacks the Fabric wrapper " + name for name in expected if name not in neoforge]
    authored = {m["name"] for m in fabric}
    problems += ["NeoForge has a wrapper Fabric lacks: " + name for name in sorted(neoforge - authored)]
    problems += ["NeoForge wraps the Fabric-only fixture test " + m["name"]
                 for m in fabric if fabric_only(m) and m["name"] in neoforge]
    return problems


def _norm(body):
    return re.sub(r"\s+", " ", body).strip()


def _comments_before(src, found, index):
    prev_end = found[index - 1]["end"] if index else src.index("{", src.index("class ThaumcraftGameTests")) + 1
    return [line for line in src[prev_end:found[index]["start"]].splitlines() if line.strip().startswith("//")]


def regenerate(fabric_src, neoforge_src):
    """The NeoForge catalogue text and a summary of what each wrapper came from."""
    fabric = methods(fabric_src)
    existing = {m["name"]: m for m in methods(neoforge_src)}
    summary = dict(skipped=[], translated=[], networked=[], kept=[])
    parts = []
    for index, method in enumerate(fabric):
        name = method["name"]
        if fabric_only(method):
            summary["skipped"].append(name)
            continue
        ann = method["ann"].replace("template = FabricGameTest.EMPTY_STRUCTURE",
                                    'template = "empty", templateNamespace = "thaumcraft"')
        if "FabricGameTest" in ann:
            raise ValueError("untranslated Fabric annotation on " + name)
        body, param = method["body"], method["param"]
        old = existing.get(name)
        if old is not None and name not in STALE_NEOFORGE:
            renamed = body.replace(param, "test") if param != "test" else body
            networked = renamed.replace("test.makeMockServerPlayerInLevel()", "NetworkedGameTestPlayer.create(test)")
            if _norm(renamed) == _norm(old["body"]):
                summary["translated"].append(name)
            elif _norm(networked) == _norm(old["body"]):
                body = re.sub(r"(\w+)\.makeMockServerPlayerInLevel\(\)", r"NetworkedGameTestPlayer.create(\1)", body)
                summary["networked"].append(name)
            else:
                body, param = old["body"], old["param"]
                summary["kept"].append(name)
        else:
            summary["translated"].append(name)
        text = "".join(line + "\n" for line in _comments_before(fabric_src, fabric, index))
        text += "    " + ann + "\n"
        if method["extra"].strip():
            text += "    " + method["extra"].strip() + "\n"
        text += "    public void %s(GameTestHelper %s) %s\n" % (name, param, body)
        parts.append(text)

    class_at = neoforge_src.index("public final class ThaumcraftGameTests")
    header = neoforge_src[:neoforge_src.index("{", class_at) + 1] + "\n"
    helpers = []
    for helper in NEOFORGE_HELPERS:
        found = re.search(r"\n(    private static [^\n]*\b" + helper + r"\(GameTestHelper test\) \{.*?\n    \})\n",
                          neoforge_src, re.S)
        if not found:
            raise ValueError("NeoForge helper %s not found" % helper)
        helpers.append(found.group(1))
    imports = []
    for line in re.findall(r"^import [^\n]+;$", neoforge_src, re.M) + re.findall(r"^import [^\n]+;$", fabric_src, re.M):
        if "net.fabricmc" in line or "thaumcraft.fabric." in line or line in imports:
            continue
        imports.append(line)
    bare = re.sub(r"^import [^\n]+;\n", "", header, flags=re.M)
    package_end = bare.index(";") + 1
    out = bare[:package_end] + "\n\n" + "\n".join(sorted(imports)) + "\n\n" + bare[package_end:].lstrip("\n")
    out = out.rstrip() + "\n" + "\n".join(parts) + "\n" + "\n\n".join(helpers) + "\n}\n"
    return out, summary


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--write", action="store_true", help="regenerate the NeoForge catalogue")
    args = parser.parse_args(argv)
    fabric_src = (ROOT / FABRIC).read_text(encoding="utf-8")
    neoforge_src = (ROOT / NEOFORGE).read_text(encoding="utf-8")
    if args.write:
        text, summary = regenerate(fabric_src, neoforge_src)
        (ROOT / NEOFORGE).write_text(text, encoding="utf-8")
        print({key: len(value) for key, value in summary.items()}, "skipped:", summary["skipped"])
        neoforge_src = text
    problems = check(fabric_src, neoforge_src)
    for problem in problems:
        print(problem, file=sys.stderr)
    if not problems:
        print("NeoForge GameTest catalogue matches Fabric (%d wrappers)" % len(methods(neoforge_src)))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
