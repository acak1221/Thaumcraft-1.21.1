#!/usr/bin/env python3
"""Read-only reproducible ledger audit; does not certify semantic parity."""
import collections
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ORACLE = Path('/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle')
OUT = ROOT / '.tmp/a1ledger'
OUT.mkdir(parents=True, exist_ok=True)
ledger = json.loads((ROOT / 'docs/porting/scope-ledger.json').read_text())
units = ledger['units']
corpus = {str(p.relative_to(ORACLE)): p.read_text() for p in sorted(ORACLE.rglob('*.java'))}

def hits(term):
    pattern = re.compile(r'\b' + re.escape(term) + r'\b')
    return [(p, i, s.strip()) for p, t in corpus.items() for i, s in enumerate(t.splitlines(), 1) if pattern.search(s)]

counts = dict(original_units=sum(bool(u['original']) for u in units),
              legacy_units=sum(bool(u['legacy']) for u in units), union_rows=len(units),
              **collections.Counter(u['path_relation'] for u in units),
              native_linked_rows=sum(bool(u['native_counterparts']) for u in units),
              unique_native_counterparts=len({p for u in units for p in u['native_counterparts']}),
              dispositions=dict(sorted(collections.Counter(u['review']['disposition'] for u in units).items())),
              milestone_queue=dict(sorted(collections.Counter(u['queue']['planned_milestone'] for u in units).items())),
              stale_review_fingerprints=sum(u['review_fingerprints_current'] is False for u in units))
assert counts == ledger['counts'], (counts, ledger['counts'])
(OUT / 'counts.json').write_text(json.dumps(dict(counts=counts, coverage=dict(collections.Counter(u['review']['native_coverage'] for u in units))), indent=2)+'\n')

excludes = []
for u in units:
    if not any(u.get(k, {}).get('disposition') == 'exclude' for k in ('queue', 'review')):
        continue
    name = Path(u['canonical_path']).stem
    source = ROOT / u['legacy']['path'] if u['legacy'] else ORACLE / u['original']['path']
    text = source.read_text()
    # Java declared method identifiers, excluding constructors and inherited overrides
    declared = set(re.findall(r'\b(?:public|protected|private)\s+(?:(?:static|final|abstract|synchronized|default)\s+)*(?:[\w<>\[\].?,]+\s+)\s*(\w+)\s*\(', text))
    declared.update(re.findall(r'\b(?:public|protected|private)\s+(?:(?:static|final|volatile|transient)\s+)*[\w<>\[\].?,]+\s+(\w+)\s*(?:=|;)', text))
    if re.search(r'\binterface\s+' + re.escape(name) + r'\b', text):
        declared.update(re.findall(r'^\s*(?:public\s+)?[\w<>\[\].?,]+\s+(\w+)\s*\([^;{}]*\)\s*;', text, re.M))
    members = sorted(declared - {name})
    # Search every declared member, not a cherry-picked subset.
    searches = {s: hits(s) for s in [name] + members}
    # Also replay zero-hit distinctive identifiers explicitly quoted in the evidence.
    claim = ' '.join(u['review']['evidence'] + u['review']['notes'])
    for token in re.findall(r'\b[A-Za-z_]\w*(?:\(\))?', claim):
        token = token.removesuffix('()')
        if (re.search(r'[a-z][A-Z]', token) and token in text and token not in searches):
            searches[token] = hits(token)
    excludes.append(dict(unit=u['canonical_path'], relation=u['path_relation'],
                         searches={s: dict(lines=len(h), files=len({v[0] for v in h}), external_lines=sum(p != u['canonical_path'] for p, _, _ in h)) for s, h in searches.items()},
                         class_hits=searches[name], notes=u['review']['notes'], evidence=u['review']['evidence']))
(OUT / 'excludes.json').write_text(json.dumps(excludes, indent=2)+'\n')

citation = re.compile(r'((?:(?:common|fabric|neoforge)/src/[^\s,;()]+|src/main/java/[^\s,;()]+|thaumcraft/[^\s,;()]+|truetyper/[^\s,;()]+)\.java):(\d+(?:(?:-|,|/)\d+)*)')
def refs(entry):
    result = []
    for m in citation.finditer(entry):
        path, nums = m.groups()
        side = 'native' if path.startswith(('common/', 'fabric/', 'neoforge/')) else 'legacy' if path.startswith('src/') else 'oracle'
        f = (ORACLE if side == 'oracle' else ROOT) / path
        lines = f.read_text().splitlines() if f.is_file() else []
        ns = []
        for part in re.split(r'[,/]', nums):
            a, *b = part.split('-')
            ns.extend(range(int(a), int(b[0])+1) if b else [int(a)])
        actual = [(n, lines[n-1] if 0 < n <= len(lines) else '<PAST EOF>' if lines else '<MISSING FILE>') for n in ns]
        bad = [n for n, s in actual if s.startswith(('<PAST EOF>', '<MISSING FILE>')) or (len(ns) == 1 and s.strip() in ('', '{', '}', '};'))]
        result.append(dict(side=side, path=path, nums=nums, actual=actual, structural_bad=bad))
    return result

groups = collections.defaultdict(list)
for u in units:
    ev = [(i, e, refs(e)) for i, e in enumerate(u['review']['evidence'])]
    oracle = next((v for v in ev if any(r['side'] == 'oracle' and (ORACLE / r['path']).is_file() for r in v[2])), None)
    native = next((v for v in ev if any(r['side'] == 'native' for r in v[2])), None)
    if oracle and native:
        groups[u['queue']['subsystem_family']].append((u, oracle, native))
sample = []
for family, rows in sorted(groups.items()):
    # six evenly spread rows per family; all rows if the family is smaller
    for n in sorted({round(i*(len(rows)-1)/max(1, min(6, len(rows))-1)) for i in range(min(6, len(rows)))}):
        u, a, b = rows[n]
        for i, e, rr in (a, b):
            sample.append(dict(unit=u['canonical_path'], family=family, index=i, claim=e, refs=rr))
(OUT / 'sample.json').write_text(json.dumps(sample, indent=2)+'\n')
with (OUT / 'sample.txt').open('w') as out:
    for i, s in enumerate(sample, 1):
        out.write(f"{i}. {s['family']} | {s['unit']} | evidence[{s['index']}] {s['claim']}\n")
        for r in s['refs']:
            actual = r['actual']
            # Complete requested ranges are retained in sample.json; summarize long ranges here.
            shown = actual if len(actual) <= 4 else actual[:2] + actual[-2:]
            out.write(f"  {r['side']} {r['path']}:{r['nums']} BAD={r['structural_bad']} " + ' / '.join(f'{n}: {v.strip()}' for n, v in shown) + '\n')
print(json.dumps(dict(counts=counts, excludes=len(excludes), sampled_entries=len(sample), sampled_units=len(sample)//2, families=dict(collections.Counter(s['family'] for s in sample)), structural_failures=sum(any(r['structural_bad'] for r in s['refs']) for s in sample)), indent=2))
