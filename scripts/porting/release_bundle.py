# SPDX-License-Identifier: MIT
"""Prepare release assets from the repository's existing fresh-evidence checks."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

import checkpoint_evidence

ROOT = Path(__file__).resolve().parents[2]
ORDER_CASES = (
    'waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems',
    'infusionComponentPredicateSession', 'advancedFurnacePreservesCustodyAcrossRealChunkUnload',
    'embersFlyDamageIgniteAndExpire', 'tailreviewgametests.tailreplantseedcustody',
    'pechOriginalEntityUsesNativeGameplay',
    'champion_promotion_uses_the_original_bonuses',
)


def properties(path):
    return dict(line.split('=', 1) for raw in path.read_text(encoding='utf-8-sig').splitlines()
                if (line := raw.strip()) and not line.startswith('#') and '=' in line)


def metadata(version, tag='', publish=False):
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?', version):
        raise ValueError('mod_version must be a semantic version')
    if tag and tag != 'v' + version:
        raise ValueError(f'Tag {tag!r} does not match mod_version={version}')
    if publish and not tag:
        raise ValueError('Publishing requires an existing version tag')
    return dict(version=version, tag=tag, publish=str(publish).lower(),
                prerelease=str('-' in version.split('+', 1)[0]).lower())


def commit():
    return subprocess.check_output(['git', '-C', str(ROOT), 'rev-parse', 'HEAD'], text=True).strip()


def check_order(path, started_ns):
    checkpoint_evidence.fresh_output(path, started_ns)
    rows = json.loads(path.read_text(encoding='utf-8'))
    if not isinstance(rows, list) or {row['case'] for row in rows} != {'full', *ORDER_CASES} or len(rows) != len(ORDER_CASES) + 1:
        raise ValueError('Full and isolated order checks are incomplete')
    for row in rows:
        if row['exit'] != 0 or row['failures'] or row['tests'] <= 0:
            raise ValueError(f'Unsuccessful order check: {row}')
        report = path.parent / (row['case'] + '.xml')
        checkpoint_evidence.fresh_output(report, started_ns)
        actual = checkpoint_evidence.validate_test_report(report)
        if actual['tests'] != row['tests'] or row['case'] != 'full' and row['tests'] != 1:
            raise ValueError('Order check summary does not match its XML')
    return rows


def prepare():
    props = properties(ROOT / 'gradle.properties')
    snapshot = json.loads((ROOT / '.tmp/release-inputs.json').read_text(encoding='utf-8'))
    started_ns = checkpoint_evidence.validate_snapshot(ROOT, snapshot)
    report_path = ROOT / 'docs/porting/verification.json'
    checkpoint_evidence.fresh_output(report_path, started_ns)
    report = json.loads(report_path.read_text(encoding='utf-8'))
    if report['mod_version'] != props['mod_version'] or set(report['loaders_checked']) != {'fabric', 'neoforge'}:
        raise ValueError('Verification does not cover this version and both loaders')
    order = check_order(ROOT / '.tmp/order-verification/summary.json', started_ns)
    neo_reports = sorted((ROOT / 'neoforge/build/test-results/test').glob('TEST-*.xml'))
    if not neo_reports:
        raise ValueError('NeoForge JUnit reports are missing')
    neo_junit = dict(tests=0, failures=0, errors=0, skipped=0)
    for path in neo_reports:
        checkpoint_evidence.fresh_output(path, started_ns)
        summary = checkpoint_evidence.validate_test_report(path)
        for key in neo_junit:
            neo_junit[key] += summary[key]
    output = ROOT / '.tmp/release-files'
    if output.exists() and any(output.iterdir()):
        raise ValueError('Release directory contains stale files')
    output.mkdir(parents=True, exist_ok=True)
    artifacts = []
    for loader in ('fabric', 'neoforge'):
        for sources in (False, True):
            suffix = '-sources' if sources else ''
            name = f"Thaumcraft-{loader}-{props['mod_version']}{suffix}.jar"
            path = ROOT / loader / 'build/libs' / name
            checkpoint_evidence.fresh_output(path, started_ns)
            with zipfile.ZipFile(path) as archive:
                if archive.testzip() is not None:
                    raise ValueError(f'Corrupt jar: {name}')
                if sources:
                    if not any(n.endswith('.java') for n in archive.namelist()) or any(n.endswith('.class') for n in archive.namelist()):
                        raise ValueError(f'Invalid sources archive: {name}')
                else:
                    audited = next(a for a in report['artifacts'] if a['path'] == path.relative_to(ROOT).as_posix())
                    if audited['sha256'] != hashlib.sha256(path.read_bytes()).hexdigest():
                        raise ValueError(f'Production jar changed after verification: {name}')
            shutil.copyfile(path, output / name)
            artifacts.append(dict(loader=loader, kind='sources' if sources else 'mod', file=name,
                                  bytes=path.stat().st_size, sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
    info = dict(version=props['mod_version'], source_commit=commit(), minecraft=props['minecraft_version'], java=21,
                artifacts=artifacts, junit=report['junit'], neoforge_junit=neo_junit, gametests=report['gametests'],
                order_checks=order, assets=report['assets'], inputs_unchanged=report['build']['inputs_unchanged'],
                verification_sha256=hashlib.sha256(report_path.read_bytes()).hexdigest(),
                limitations=['Original-JAR oracle tests may skip only as allowed by checkpoint_evidence.py; skips are listed above.',
                             'This workflow does not run physical client or remote multiplayer acceptance.'])
    (output / 'BUILD_INFO.json').write_text(json.dumps(info, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (output / 'SHA256SUMS.txt').write_text(''.join(f"{a['sha256']}  {a['file']}\n" for a in artifacts), encoding='ascii')
    notes_path = ROOT / 'docs' / f"RELEASE_NOTES_{props['mod_version']}.md"
    notes = notes_path.read_text(encoding='utf-8') if notes_path.exists() else f"# Thaumcraft 4 Reload {props['mod_version']}\n"
    notes += ('\n\nMinecraft 1.21.1, Java 21. Install the mod jar for your loader; sources jars are for development. '
              'Requires Architectury API 13.0.11+ and, on Fabric, Fabric API and Fabric Loader 0.16.14+; '
              'NeoForge build targets 21.1.259+. Use a new world.\n\n'
              f"Common JUnit: {report['junit']['tests']} total, {report['junit']['skipped']} documented oracle skips. "
              f"NeoForge JUnit: {neo_junit['tests']}. Fabric GameTests: {report['gametests']['fabric']['tests']}; "
              f"NeoForge GameTests: {report['gametests']['neoforge']['tests']}. "
              'Full/isolated Fabric checks, asset checks and production archive audits passed. '
              'Source commits and exact results are in BUILD_INFO.json. Client and remote multiplayer checks are not run by this workflow.\n\n'
              'Thaumcraft was created by Azanor; see THIRD_PARTY_NOTICES.md for attribution and rights. '
              'Release automation configured with OpenAI Codex (GPT-6).\n')
    (output / 'RELEASE_NOTES.md').write_text(notes, encoding='utf-8')
    checkpoint_evidence.validate_snapshot(ROOT, snapshot)
    print(json.dumps({k: info[k] for k in ('version', 'source_commit', 'junit', 'neoforge_junit', 'artifacts')}, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['metadata', 'prepare'])
    parser.add_argument('--tag', default='')
    parser.add_argument('--publish', choices=['true', 'false'], default='false')
    parser.add_argument('--github-output', type=Path)
    args = parser.parse_args()
    if args.command == 'prepare':
        prepare()
    else:
        result = metadata(properties(ROOT / 'gradle.properties')['mod_version'], args.tag, args.publish == 'true')
        result['commit'] = commit()
        print(json.dumps(result))
        if args.github_output:
            with args.github_output.open('a', encoding='utf-8') as stream:
                stream.writelines(f'{key}={value}\n' for key, value in result.items())


if __name__ == '__main__':
    main()
