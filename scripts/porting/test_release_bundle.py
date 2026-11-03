# SPDX-License-Identifier: MIT
import json
from pathlib import Path
import tempfile
import unittest

import release_bundle as release


class ReleaseBundleTests(unittest.TestCase):
    def test_tag_matches_the_version_and_publish_requires_a_tag(self):
        with self.assertRaises(ValueError):
            release.metadata('0.1.0-alpha.17', 'v0.1.0-alpha.13', True)
        with self.assertRaises(ValueError):
            release.metadata('0.1.0-alpha.17', publish=True)
        self.assertEqual(release.metadata('0.1.0-alpha.17')['publish'], 'false')
        self.assertEqual(release.metadata('0.1.0-alpha.17', 'v0.1.0-alpha.17', True)['publish'], 'true')

    def test_prerelease_comes_from_the_version_not_the_build_suffix(self):
        self.assertEqual(release.metadata('1.0.0+build-with-hyphens')['prerelease'], 'false')
        self.assertEqual(release.metadata('1.0.0-rc.1')['prerelease'], 'true')

    def test_an_incomplete_order_check_is_not_publishable(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'summary.json'
            path.write_text(json.dumps([dict(case='full', exit=0, tests=656, failures=[])]))
            with self.assertRaises(ValueError):
                release.check_order(path, 1)

    def test_order_summary_cannot_hide_a_failed_report(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'summary.json'
            rows = []
            for case in ('full', *release.ORDER_CASES):
                rows.append(dict(case=case, exit=0, tests=1, failures=[]))
                (path.parent / (case + '.xml')).write_text(
                    '<testsuite tests="1"><testcase name="scenario"><failure/></testcase></testsuite>')
            path.write_text(json.dumps(rows))
            with self.assertRaises(ValueError):
                release.check_order(path, 1)


if __name__ == '__main__':
    unittest.main()
