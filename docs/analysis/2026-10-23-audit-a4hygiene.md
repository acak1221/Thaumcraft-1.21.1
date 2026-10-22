# a4hygiene — repository artefact audit

Base `c6feeda7`, branch `agent/a4hygiene`, only worktree `wt/a4hygiene` changed.
Full checklist findings and exact report arbitration:
[`docs/audit/REPO_FINDINGS.md`](../audit/REPO_FINDINGS.md).
Complete directory inventory: [`ANALYSIS_INVENTORY.txt`](../audit/ANALYSIS_INVENTORY.txt).

## Measured, not assumed

* Initial status clean. Worktree `.git` is 4K; shared Git storage **91M**.
* No Gradle home, generated world/run/log/build JAR found in this worktree.
  Removed only tracked empty `.tmp/.gitkeep`.
* Still tracked: original TC4 JAR 12,565,807 bytes; TC6 reference JAR 11,360,786;
  required wrapper JAR. Protected originals were not deleted to fake a clean tree.
* 21 PNGs over 300KB (seven original assets in three retained trees); largest
  is `nodes.png`, 905,526 bytes. No asset bytes were changed.
* `main`, `work` plus four active audit branches/worktrees. Cleanup belongs to
  leader, not this lane. No branch deletion or history rewrite performed.
* 16 pre-existing individual output notices checked: 0 wrong hashes or phantom
  outputs. Five newly enumerated outputs checked too: total **21**. The complete
  1060 manifest target hashes match, including all 111 sounds. Independently
  checked official client SHA1/SHA256, source atlas hashes and three pixel crops.
* Fixed missing particle/scanner/bobber/MCP/sound provenance and made output paths
  and hashes explicit. **No extracted font exists**; added a limitation, not a
  fictional attribution entry. Removed stale “one grep hit” CodeChicken assertion.
* Python recomputation: **1044/1044 parity_verified**, no other coverage status.
  Both status docs now lead with 1044/0 instead of 1017/27; MODLOG has the missing
  current checkpoint. Old 976, 969, 945 totals and all older sections are expressly
  historical. Old test counts are preserved as their actual earlier measurements.

## Arbitration and open work

Mirror, chunk replay, scattered/aura flag, particle cap and ore-pass condition
have later code reports that supersede earlier impossibility/wrong-setting claims.
Ore gate is **anisotropicFiltering > 1**, not particle setting or Fast/Fancy.
Stair per-face and model mirror-axis disagreements are also explicitly arbitrated.
Detailed old/new report anchors are in REPO_FINDINGS §4.5.

**Unresolved:** old-vs-modern font glyph equivalence; no legacy font asset exists.
New code-closure claims are also **not final acceptance**: wave-8 reports explicitly
say their tests/frames were not executed. The older recorded 2384 JUnit / 325
Fabric / 90 Python and wave-3 66/66 frames / two-client 37/37 do not certify later
inputs. a3tests owns fresh integrated gates; NeoForge visual acceptance is still
not established. No Gradle, Java build, JUnit, GameTest or game ran in this lane.

## Audit-only validation

Python checked ledger values, hashes, existence, original ZIP byte identity,
newline-only scanner adaptation and pixel crops. Git diff whitespace validation
is required before commit. No production code, ledger verdict, original JAR,
retained original asset, branch or other worktree was changed.
