"""Regression checks for attribution in final loader JARs."""
from __future__ import annotations

import hashlib
from io import BytesIO
import json
import unittest
import warnings
from zipfile import ZipFile

import record_verification as recorder


class PackagedNoticesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.notices = {name: recorder.canonical_notice_bytes(recorder.ROOT / name)
                        for name in ("THIRD_PARTY_NOTICES.md", "LICENSE")}

    def check_archive(self, entries: list[tuple[str, bytes]]) -> dict[str, str]:
        stream = BytesIO()
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)  # Duplicate entry is an intentional corrupt fixture.
            with ZipFile(stream, "w") as archive:
                for name, content in entries:
                    archive.writestr(name, content)
        stream.seek(0)
        with ZipFile(stream) as archive:
            return recorder.validate_packaged_notices(archive)

    def test_current_notices_are_accepted_and_fingerprinted(self) -> None:
        self.assertEqual(self.check_archive(list(self.notices.items())),
                         {name: hashlib.sha256(content).hexdigest() for name, content in self.notices.items()})

    def test_missing_notice_is_rejected(self) -> None:
        for missing in self.notices:
            with self.subTest(missing=missing), self.assertRaisesRegex(ValueError, missing):
                self.check_archive([(name, content) for name, content in self.notices.items() if name != missing])

    def test_duplicate_notice_is_rejected(self) -> None:
        for duplicate in self.notices:
            with self.subTest(duplicate=duplicate), self.assertRaisesRegex(ValueError, "exactly one"):
                self.check_archive(list(self.notices.items()) + [(duplicate, self.notices[duplicate])])

    def test_stale_notice_is_rejected(self) -> None:
        for stale in self.notices:
            entries = [(name, b"outdated notice\n" if name == stale else content)
                       for name, content in self.notices.items()]
            with self.subTest(stale=stale), self.assertRaisesRegex(ValueError, "stale"):
                self.check_archive(entries)

    def test_windows_line_endings_are_rejected_in_artifact(self) -> None:
        for noncanonical in self.notices:
            entries = [(name, content.replace(b"\n", b"\r\n") if name == noncanonical else content)
                       for name, content in self.notices.items()]
            with self.subTest(noncanonical=noncanonical), self.assertRaisesRegex(ValueError, "noncanonical"):
                self.check_archive(entries)


class PackagedIsolationTest(unittest.TestCase):
    def check_archive(self, entries: list[tuple[str, bytes]]) -> dict:
        stream = BytesIO()
        with ZipFile(stream, "w") as archive:
            for name, content in entries:
                archive.writestr(name, content)
        stream.seek(0)
        with ZipFile(stream) as archive:
            return recorder.validate_packaged_isolation(archive)

    def test_clean_archive_is_accepted(self) -> None:
        result = self.check_archive([
            ("thaumcraft/common/Thaumcraft.class", b"production"),
            ("fabric.mod.json", json.dumps({"entrypoints": {"main": ["thaumcraft.fabric.ThaumcraftFabric"]}}).encode())])
        self.assertTrue(all(result.values()))

    def test_helpers_wrappers_client_harness_and_templates_are_rejected(self) -> None:
        for name in ("thaumcraft/common/testing/TableChecks.class",
                     "thaumcraft/fabric/testing/ThaumcraftGameTests.class",
                     "thaumcraft/neoforge/testing/NetworkedGameTestPlayer$Connection.class",
                     "thaumcraft/fabric/clienttest/BookClientSmoke.class",
                     "thaumcraft/clienttest/SharedClientSmoke.class",
                     "data/thaumcraft/structure/empty.nbt", "data/thaumcraft/structures/empty.nbt",
                     "data/thaumcraft_gametest/structure/fixture.nbt"):
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, "Test support leaked"):
                self.check_archive([(name, b"fixture")])

    def test_production_reference_to_absent_test_helper_is_also_rejected(self) -> None:
        for reference in (b"thaumcraft/common/testing/ResearchChecks",
                          b"net/minecraft/gametest/framework/GameTest",
                          b"net/neoforged/neoforge/event/RegisterGameTestsEvent"):
            with self.subTest(reference=reference), self.assertRaisesRegex(ValueError, "references a test"):
                self.check_archive([("thaumcraft/neoforge/ThaumcraftNeoForge.class", b"constant pool " + reference)])

    def test_fabric_test_entrypoint_is_rejected_even_without_test_classes(self) -> None:
        with self.assertRaisesRegex(ValueError, "fabric-gametest entrypoint"):
            self.check_archive([("fabric.mod.json", b'{"entrypoints":{"fabric-gametest":["missing.Fixture"]}}')])

    def test_test_mod_metadata_is_not_allowed_in_production(self) -> None:
        for name in ("fabric.mod.json", "META-INF/neoforge.mods.toml"):
            content = b'{"id":"thaumcraft_client_smoke"}' if name.endswith("json") else b'modId="thaumcraft_gametest"'
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, "Test mod metadata"):
                self.check_archive([(name, content)])


if __name__ == "__main__":
    unittest.main()
