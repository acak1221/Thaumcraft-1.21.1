#!/usr/bin/env python3
"""Keeps the code of the add-on developer guides identical to the compiled example add-on.

The guides under docs/api (not the generated reference) never hold hand-written copies of example code. A guide asks
for a region of the example add-on and the region is copied in:

    <!-- snippet:focus -->
    ```java
    ...replaced...
    ```
    <!-- /snippet -->

    <!-- file:examples/example-addon/src/main/resources/data/tc4_example/recipe/spark_focus.json -->
    ```json
    ...replaced...
    ```
    <!-- /file -->

A region is marked in the example's Java sources by a line ``// [doc:name]`` and the next line ``// [/doc]``. Regions of
the same name are joined in source order with a ``// ...`` line, so a snippet can skip the lines between them. The marker
lines themselves never appear in a guide, and the common indentation is removed.

    python3 scripts/porting/api_docs.py --write   # rewrite the guides
    python3 scripts/porting/api_docs.py --check   # exit 1 when a guide is stale or names an unknown region
"""
import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCES = ROOT / "examples" / "example-addon" / "src" / "main" / "java"
DOCS = ROOT / "docs" / "api"
SKIP = {DOCS / "reference"}

OPEN = re.compile(r"^\s*// \[doc:([a-z0-9-]+)\]\s*$")
CLOSE = re.compile(r"^\s*// \[/doc\]\s*$")
BLOCK = re.compile(r"(<!-- (snippet|file):([^ ]+) -->\n)(.*?)(<!-- /\2 -->)", re.S)
FENCES = {".java": "java", ".json": "json", ".gradle": "groovy", ".toml": "toml"}


class DocError(Exception):
    pass


def collect(sources=SOURCES):
    """name -> list of regions (each a list of lines), in file and line order."""
    regions = {}
    for path in sorted(sources.rglob("*.java")):
        current, lines = None, []
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            opened, closed = OPEN.match(line), CLOSE.match(line)
            if opened:
                if current is not None:
                    raise DocError(f"{path}:{number}: region '{opened.group(1)}' opens inside '{current}'")
                current, lines = opened.group(1), []
            elif closed:
                if current is None:
                    raise DocError(f"{path}:{number}: '// [/doc]' without an open region")
                regions.setdefault(current, []).append(lines)
                current = None
            elif current is not None:
                lines.append(line)
        if current is not None:
            raise DocError(f"{path}: region '{current}' is never closed")
    return regions


def dedent(lines):
    indents = [len(line) - len(line.lstrip()) for line in lines if line.strip()]
    cut = min(indents) if indents else 0
    return [line[cut:] if line.strip() else "" for line in lines]


def render(parts):
    joined = []
    for index, part in enumerate(parts):
        while part and not part[0].strip():
            part = part[1:]
        while part and not part[-1].strip():
            part = part[:-1]
        if index > 0 and part:
            # The skipped lines sit at the deeper of the two indentations around them.
            indent = max(len(line) - len(line.lstrip()) for line in (joined[-1] if joined else "", part[0]))
            joined.append(" " * indent + "// ...")
        joined.extend(part)
    return "\n".join(dedent(joined))


def fenced(language, text):
    return f"```{language}\n{text.rstrip()}\n```\n"


def update(text, regions, where="guide"):
    problems = []

    def replace(match):
        head, kind, name, _, tail = match.groups()
        if kind == "snippet":
            if name not in regions:
                problems.append(f"{where}: unknown snippet '{name}'")
                return match.group(0)
            return head + fenced("java", render(regions[name])) + tail
        path = ROOT / name
        if not path.is_file() or ROOT not in path.resolve().parents:
            problems.append(f"{where}: missing file '{name}'")
            return match.group(0)
        return head + fenced(FENCES.get(path.suffix, ""), path.read_text(encoding="utf-8")) + tail

    return BLOCK.sub(replace, text), problems


def guides(docs=DOCS):
    return [path for path in sorted(docs.rglob("*.md")) if not any(skip in path.parents for skip in SKIP)]


def run(write):
    regions = collect()
    stale, problems = [], []
    for path in guides():
        before = path.read_text(encoding="utf-8")
        after, found = update(before, regions, str(path.relative_to(ROOT)))
        problems += found
        if after != before:
            stale.append(path)
            if write:
                path.write_text(after, encoding="utf-8")
    for problem in problems:
        print(problem, file=sys.stderr)
    names = ", ".join(str(path.relative_to(ROOT)) for path in stale)
    if write:
        print(f"written: {len(stale)} guide(s){': ' + names if stale else ''}")
    elif stale:
        print(f"stale: {names}; run python3 scripts/porting/api_docs.py --write", file=sys.stderr)
    else:
        print(f"up to date: {len(guides())} guides, {len(regions)} snippets")
    return 1 if problems or (stale and not write) else 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        return run(args.write)
    except DocError as error:
        print(error, file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
