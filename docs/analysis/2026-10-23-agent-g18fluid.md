# g18fluid — fluid conservation hypothesis

- Worktree: `/home/cyberfox-kali/wt/g18fluid`; branch `agent/g18fluid`.
- First command succeeded: clean status, baseline `a2b5bf92`. No other worktree or primary checkout accessed.
- Hypothesis confirmed at Fabric API boundaries, not by rewriting the original tank rules.
- Inspected22 entries:20 block variants plus pure bucket and golem cargo. The actual mB tanks are crucible and Spa; seventeen named block variants instead store essentia, XP, node state, inventory/work or fuel/light state.

## Proof before fix

Production baseline with newly authored real-container GameTests:5 executed,4 failed,1 passed (`.tmp/g18-red-exact.log`).

1. Canceled crucible insertion left1000mB instead of0.
2. Canceled crucible extraction left0mB instead of1000.
3. Golem adapter reported0mB inserted into real Spa but committed80 droplets.
4. Golem adapter reported999mB extracted from real Spa but removed80999 droplets,80 too many.

An initial shorthand filter `fluid` was rejected because the existing selector requires exact names. That harness-selection failure is not bug evidence. Gradle used Java21 and private `.gradle-g18` (seeded by copying the ordinary user cache, not another worktree); fresh `fabric/run-gametest/world` before each invocation. NeoForge project configuration occurs in the multi-project build, but no NeoForge compile/runtime task was requested.

## Fix commits

- `36f18e7c`: probe/abort native availability, round to representable whole mB, then commit exactly that amount. Reject a mismatching retry rather than commit an unreportable partial amount. Three focused bridge/boundary tests pass.
- `65c968bc`: shared weakly cached per-crucible `SnapshotParticipant`, nested/outer rollback and retired-owner refusal. Additional nested cross-face assertions cover1mB/2mB and nested commit followed by outer abort.

Combined8-test runtime run passed, including actual golem AI and pure-bucket transactions (`.tmp/g18-green.log`, all8 required tests, build successful). Every whole mB amount1..1000 for crucible and1..5000 for Spa is exercised. Final test expansion adds native subunits down to one droplet, input/output mutability and six-face attempts.

Final result: **all11 required dedicated-server GameTests passed; `:fabric:build` succeeded**,46s (`.tmp/g18-final.log`, lines457/502/512). The final run includes all five new checks and six existing Spa storage, pure bucket storage, golem AI, pure source pickup, Spa component/container and pure flow checks. `git diff --check` is clean. No full-suite/client/NeoForge acceptance is implied.

## Explicit limits

See `docs/audit/FLUID_FINDINGS.md` for all22 rows, exact80/81mB discrepancies and reproduction. Rows2–18 remain reasoned-only for this fluid hypothesis; no essentia conservation proof is claimed. Component-bearing generic golem cargo, arbitrary third-party storage, reentrant direct crucible mutation/save during a native transaction, client visuals and NeoForge parity were not proven. A source pickup is exactly one bucket; total golem cargo capacity is not universally1000mB.

The original Spa direct-call/advertised-face distinction is intentional; only UP automation is refused. Void jar overflow is an original essentia sink, not a fluid conservation bug. No original/donor JAR or gameplay constants changed. No push/release performed.
