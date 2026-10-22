# h3parity handoff — 2026-10-23

Worktree `/home/cyberfox-kali/wt/h3parity`, branch `agent/h3parity`, baseline `e79b8558`.

Found and patched **nine** unreachable original mob aspect rows: thaumic slime; tainted pig, sheep, cow, chicken, villager, creeper; cultist knight and cleric. `EntityAspectBindings.NAMES` omitted their registered native IDs. `describe` therefore retained native names that could never match original `Thaumcraft.*` registrations. Added exact mappings, shared package-private translation, and a nine-mob original-value JUnit regression.

Checked: 69 original entity declarations (116 expanded rows); entire 48-aspect tree and order; 48 crucible recipe research/cost/output-count associations; 56 golem numeric values plus 8 booleans; 3 ordering paths; 20 exclude rows screened. Nine gameplay mismatches, nine source fixes, zero runtime-verified fixes. No claim of full ingredient/research-page audit.

Deliverables: `docs/audit/PARITY_FINDINGS.md`, generated `docs/audit/PARITY_CHECKS.json`, reproducible `scripts/porting/audit_h3parity.py`. Python comparison passed. Baseline map inspection shows all nine missing mappings.

**Compile blocker:** javac against named Minecraft plus `common/build/classes/java/main` and actual sourcepath attempted; exit 1. This worktree lacks common build classes and mapped Architectury dependencies/access-widened Minecraft. Actual changed production class emitted, but dependency errors prevent a passing compile/test claim. Log `.tmp/h3parity/javac.log` (ignored). No Gradle used. JUnit authored, not run. Parent needs prepared-classpath compile/test before acceptance.

Exclude concerns: MaterialAiry/MaterialTaint mobility needs official vanilla verification, not guesses. BlockWardedRenderer is really present in the oracle despite its ledger absence claim; native TileWardedRenderer covers remembered-state rendering. Reservoir inventory is NOT full cube: native model already uses original 2..14 bounds. No newly proven dropped exclude feature; many stale notes describe code that now exists. Ledger left untouched.
