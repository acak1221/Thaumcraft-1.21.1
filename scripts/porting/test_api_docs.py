#!/usr/bin/env python3
"""The add-on developer guides stay identical to the compiled example add-on, and the API reference to the Javadoc."""
import contextlib
import io
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import api_docs  # noqa: E402

ROOT = api_docs.ROOT


def anchors(path):
    """The anchors GitHub gives the headings of a page, plus explicit <a id> anchors."""
    found, seen = set(), {}
    text = path.read_text(encoding="utf-8")
    for heading in re.findall(r"^#{1,6} (.+)$", text, re.M):
        slug = re.sub(r"[^\w\- ]", "", heading.replace("`", "").strip().lower()).replace(" ", "-")
        count = seen.get(slug, 0)
        seen[slug] = count + 1
        found.add(slug if count == 0 else f"{slug}-{count}")
    found.update(re.findall(r'<a id="([^"]+)"', text))
    return found


def used(paths):
    names = set()
    for path in paths:
        names.update(re.findall(r"<!-- snippet:([a-z0-9-]+) -->", path.read_text(encoding="utf-8")))
    return names


class CommittedGuides(unittest.TestCase):
    def test_the_guides_match_the_example(self):
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()) as errors:
            code = api_docs.run(write=False)
        self.assertEqual(0, code, errors.getvalue())

    def test_every_snippet_is_shown_in_both_languages(self):
        regions = set(api_docs.collect())
        english = used(sorted((api_docs.DOCS / "guide").glob("*.md")))
        russian = used(sorted((api_docs.DOCS / "ru").rglob("*.md")))
        self.assertEqual(regions, english, "every region of the example appears in an English guide")
        self.assertEqual(english, russian, "the Russian guides show the same snippets as the English ones")

    def test_every_english_guide_has_its_russian_page(self):
        english = {path.name for path in (api_docs.DOCS / "guide").glob("*.md")}
        russian = {path.name for path in (api_docs.DOCS / "ru" / "guide").glob("*.md")}
        self.assertEqual(english, russian)

    def test_relative_links_and_anchors_resolve(self):
        broken = []
        for path in api_docs.guides():
            for target, anchor in re.findall(r"\]\(([^)#\s]*)(?:#([^)\s]*))?\)", path.read_text(encoding="utf-8")):
                if "://" in target:
                    continue
                resolved = (path.parent / target).resolve() if target else path
                if not resolved.exists():
                    broken.append(f"{path.relative_to(ROOT)} -> {target}")
                elif anchor and resolved.suffix == ".md" and anchor not in anchors(resolved):
                    broken.append(f"{path.relative_to(ROOT)} -> {target}#{anchor}")
        self.assertEqual([], broken)


class Rendering(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp)

    def source(self, text):
        (self.tmp / "A.java").write_text(text, encoding="utf-8")
        return api_docs.collect(self.tmp)

    def test_regions_of_one_name_are_joined_at_the_deeper_indentation(self):
        regions = self.source(
            "class A {\n"
            "    // [doc:x]\n"
            "    void a() {\n"
            "        one();\n"
            "    // [/doc]\n"
            "        hidden();\n"
            "    // [doc:x]\n"
            "    }\n"
            "    // [/doc]\n"
            "}\n")
        self.assertEqual("void a() {\n    one();\n    // ...\n}", api_docs.render(regions["x"]))

    def test_unclosed_and_nested_regions_are_errors(self):
        with self.assertRaises(api_docs.DocError):
            self.source("// [doc:a]\nx();\n")
        with self.assertRaises(api_docs.DocError):
            self.source("// [doc:a]\n// [doc:b]\n// [/doc]\n")
        with self.assertRaises(api_docs.DocError):
            self.source("// [/doc]\n")

    def test_update_replaces_snippets_and_reports_unknown_names(self):
        text = "<!-- snippet:x -->\nold\n<!-- /snippet -->\n<!-- snippet:missing -->\n<!-- /snippet -->\n"
        updated, problems = api_docs.update(text, {"x": [["int a;"]]})
        self.assertIn("```java\nint a;\n```\n", updated)
        self.assertEqual(["guide: unknown snippet 'missing'"], problems)


@unittest.skipIf(shutil.which("java") is None, "java is not installed")
class Reference(unittest.TestCase):
    def test_the_reference_is_generated_from_the_current_javadoc(self):
        result = subprocess.run(["java", str(ROOT / "scripts" / "porting" / "ApiReference.java"), "--check"],
                                cwd=ROOT, capture_output=True, text=True, timeout=600)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
