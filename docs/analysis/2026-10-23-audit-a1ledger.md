# a1ledger — adversarial ledger audit

Baseline c6feeda7; branch agent/a1ledger. Findings: docs/audit/LEDGER_FINDINGS.md.
Counts independently match: 1044 union rows, 1044 parity_verified; original 859, legacy 934, shared 749, original-only 110, legacy-only 185. Excludes 163.
All 163 excludes searched by class and declared methods/fields: 77 zero class hits; 86 nonzero (63 original-bearing, 23 relocated). These are literal absence-test failures, not 86 proven gameplay regressions. ModelBoreEmit falsely called absent; its five-part native geometry exists in ModelArcaneBore.
220 evidence strings / 110 units / 21 families spot-checked; 66 supporting strings wrong/materially incomplete, all corrected. Total changes: 79 evidence strings and 42 notes across 72 rows. Twelve baseline mechanical failures; zero in post-fix selected sample.
Downgrade recommendations only: BlockJar (custom step/break audio replaced with glass), INode and TileNode (original coordinate identity replaced by UUID). JarStepSound itself is an unused duplicate: BlockJar calls CustomStepSound. Reclassify ModelBoreEmit exclude to replacement; no missing geometry claimed. No disposition or coverage field changed.
Checker executed after every successful ledger edit batch: stale_review_fingerprints: 0. Entire counts block and every non-evidence/non-note field unchanged. No Gradle or runtime/visual test run.
Limits: search counts are source identifier matches, not receiver-resolved binary invoke-site audits. Secondary abbreviated citations were not exhaustively parsed. The entire ledger has not been semantically re-proved. Official client SHA-1 verified; MCP fetches blocked 403, so no new vanilla interpretation claimed.
Reproduce searches with scripts/porting/audit_ledger_evidence.py; baseline manifests: docs/audit/LEDGER_SAMPLE.json and LEDGER_EXCLUDE_SEARCHES.json.
