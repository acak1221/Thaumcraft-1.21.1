# The final audit — what "closed, clean and good code" has to mean

This file is the acceptance contract for the end of the port. It is written **before** the
audit runs, so the audit cannot invent its own standard after seeing the code. Every item is a
command or an inspection with a verifiable answer, not an opinion.

The audit starts only when the scope ledger has no `partial`, no `missing` and no
`not_assessed` unit left. If an item cannot be satisfied, it is reported as a **known
deviation with evidence** — never silently dropped and never relabelled as done.

## 0. The rule that overrides every other item

> The oracle (`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`, byte-exact, 859/859
> fingerprints) and the official 1.7.10 `client.jar` + MCP mapping are the only authority.
> `OpenTC4` is a second opinion. When two sources disagree, the oracle decides — never a
> majority, never a comment in the port, never an agent's summary.

An audit finding is only a finding if it cites `file:line` on both sides (original and native).

---

## 1. Correctness — the ledger must be honest

1.1. **Counts.** `docs/porting/scope-ledger.json` reports 1044 units and every one has
`review.native_coverage == parity_verified` or an explicit `disposition == exclude` with
evidence. Recompute from the file itself, not from a report.

1.2. **No invented contracts.** For every unit closed as `exclude`, re-run the oracle search
the note claims (the class name and its distinctive members) and confirm it still returns
nothing. A `exclude` whose evidence cannot be reproduced is a wrong `exclude`.

1.3. **Fingerprints.** `update_scope_ledger.py --check` reports
`stale_review_fingerprints: 0`, and `counts` was not hand-edited.

1.4. **Every evidence string points at a real line.** Sample at least 100 evidence entries
across families and open the cited `file:line` on both sides. A citation that points past the
end of the file, or at a line that does not say what the note claims, is a finding.

1.5. **`partial` is not allowed to hide a gap.** Any unit the audit cannot close must be
listed in `docs/audit/KNOWN_DEVIATIONS.md` with the vanilla `file:line` that has no
counterpart, and the user is told.

## 2. The four automated gates must be green, from a clean state

Run in this order, one Gradle at a time (the machine OOMs otherwise), and record the **real
output**:

2.1. `./gradlew :common:test` → 0 failures, 0 errors. Record the count.
2.2. `rm -rf fabric/run-gametest/world && ./gradlew :fabric:runGameTest` → 325/325 with **no
`-P` flag** (the 3G default must be enough).
2.3. `./gradlew :neoforge:build` and `:neoforge:runGameTest` → record how many pass; a gap
that is a NeoForge-only limitation is documented, not ignored.
2.4. `python3 scripts/porting/convert_assets.py --verify` → `missing_references: 0`;
`python3 -m unittest discover -s scripts/porting -p 'test_*.py'` → all pass.
2.5. `python3 scripts/porting/extract_tc4_definitions.py --oracle ... --check` → 201 research
entries, 294 text pages, 48 aspects, pinned hashes.

## 3. Code quality — the part nobody has checked yet

This is the half of the audit the waves have not done. Every item is an inspection of the
merged tree, not of an agent's report.

3.1. **Dead code.** For every public static method, rule class and field the waves added,
find at least one non-test caller in `common/src/main`. Anything with no caller is either
wired up or deleted. `grep` is the tool; a rule class that only its own test calls is a
finding.

3.2. **Duplication.** Hunt for the same rule implemented twice: the same numeric constant in
two places, two `packedLight`-style helpers, two copies of the same `Random` replay, two
"is this a source" predicates. One owner per rule.

3.3. **Constants vs magic numbers.** Every number that comes from the oracle should live in a
named constant with the oracle citation next to it. Inline `0.875F`, `248.0F`, `360`,
`0xFFFF` in a renderer is a finding unless it is a purely local arithmetic step.

3.4. **Comments must not lie.** Sample every comment that contains `Oracle`, `original`,
`1.7.10` or a `file:line`, and confirm the claim. The waves have *already* shipped three
comments that asserted the opposite of the oracle (the beam origin, `AuraHandler`, the wand
tip). Assume more remain. A comment that claims dead code, a limitation, or a behaviour
without a citation is a finding.

3.5. **Leftover diagnostics.** No `System.out`, no `printStackTrace`, no `Thaumcraft.LOG.info`
in a per-tick or per-frame path, no `TODO`/`FIXME`/`XXX` without a tracked reason. The debug
instrumentation added during bug hunts (`DIAG-…`) must be gone.

3.6. **Threading and state.** Every `static` mutable field is justified: a registry, a
`ThreadLocal`, or a documented single-thread owner. The client/main split is respected: no
`net.minecraft.client` import in a class that a dedicated server loads (the `javac` gate of
the server tree proves it, but check the intent too).

3.7. **Error handling.** `catch (RuntimeException ignored)` blocks are listed and each is
justified — an ignored exception in a chunk-save or a network path hides data loss. Every
`throw new IllegalStateException` on a resource-loading path must be reachable only on a
genuinely broken install.

3.8. **Resource loading.** Every `getResourceAsStream`/`ResourceLocation` the code composes is
covered by `convert_assets.py --verify`; no path is built by string concatenation that the
verifier cannot see.

3.9. **Tests that assert nothing.** Find tests with no assertion, tests that only assert
`assertNotNull`, and tests that re-implement the production formula instead of citing the
oracle value. A test that copies the implementation cannot fail for the right reason — the
waves produced several of these (a tautological staff-lift assertion, a `Matrix4f`
comparison that only restated the code).

3.10. **Test hygiene.** No test constructs a `Block` (it freezes the registry for the whole
JVM); no test depends on another test's order; no test leaves a world, a chunk ticket or a
mock player behind (the totem scenario used to force 131 072 chunks — check for siblings).

## 4. Repository hygiene

4.1. `git status` is clean; no `.gradle*`, no `run/`, no world, no jar, no log in the tree.
(`git filter-repo` already had to remove a 1.1 GB Gradle home once.)

4.2. Every worktree and `agent/*` branch is removed except the ones still in use.

4.3. `THIRD_PARTY_NOTICES.md` names the oracle, the official jars, the MCP mapping and every
asset whose provenance matters.

4.4. `MODLOG.md`, `docs/HANDOFF.md`, `docs/MIGRATION_STATUS.md` agree with the ledger and with
the last real test run — no inherited numbers.

4.5. `docs/analysis/` has one report per lane, and the reports do not contradict each other
without a recorded arbitration.

## 5. The client and multiplayer acceptance

Compile success is not visual acceptance.

5.1. The client smoke harness reaches `SCRIPT_PASSED` and every frame is judged against the
original. The frames the waves added (thaumometer screen and readout, research graph,
void-robe layers, node item, excavation beam, architect overlay, essence flow, sparkles,
candles, gas, crust, eldritch stairs) must be **seen**, not assumed.

5.2. Two physical clients plus a dedicated server, 37/37 stages, as before.

5.3. The payload channels that were never observed on the wire are either observed or listed
in `KNOWN_DEVIATIONS.md`.

## 6. What the audit must produce

* `docs/audit/REPORT.md` — every item above, with the command and the real output, or the
  inspection and the `file:line` of the finding.
* `docs/audit/FINDINGS.md` — each defect found, with the oracle citation, what was changed,
  and the test that now pins it.
* `docs/audit/KNOWN_DEVIATIONS.md` — everything that stays open, each with the vanilla
  `file:line` that has no counterpart and the reason.
* A green run of every gate in §2 **after** the last finding is fixed. An audit that ends on a
  red gate is not finished.

The audit is allowed to find that a wave was wrong. That is its purpose.
