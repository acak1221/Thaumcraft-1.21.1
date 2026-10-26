# Thaumcraft 4 Reload — Minecraft 1.21.1 migration

The user explicitly authorized migration to Minecraft 1.21.1 on 2026-10-15. Java 21, Architectury, Fabric and NeoForge replace the legacy Java 8/Forge 1.12 build. Old no-upgrade constraints apply only to the retained legacy source. Original instructions/build snapshots are under docs/legacy-build/.

## Execution priority — user correction 2026-10-16
- **User correction 2026-10-20:** continue until the entire port is finished; do not end the task after a content checkpoint. Sub-agents and commits/pushes to the existing repository's `work` branch are explicitly authorized. Keep Fabric first, then finish NeoForge. A passing server suite is not full client, multiplayer or survival acceptance.
- **Fabric first:** complete the TC4 gameplay port on Fabric before resuming NeoForge work. Do not develop or run NeoForge builds/tests in parallel with the remaining content work.
- Keep the existing NeoForge module and the shared Architectury/common foundation; do not delete or rewrite that work merely to change priority. NeoForge parity is deferred, not claimed for new Fabric checkpoints.
- Current content validation/build/client runs target Fabric only. Old NeoForge results remain historical evidence of their exact earlier inputs, never evidence for newer common code.
- **Continuous progression (user correction 2026-10-17):** finishing one bounded content group is a checkpoint, not a reason to stop. Record/verify/export the accepted group, then immediately start the next planned Fabric group without waiting for another user confirmation. Pause only for an actual blocker, required authorization, or user stop/correction; never mistake a progress update for task completion.
- **Content over smoke-tool development (user correction 2026-10-18):** stop extending the Bore smoke/audit infrastructure and continue Fabric content. The user authorized a provisional Bore export with final integrated gameplay checks deferred until later. Do not relabel rejected runs as accepted or claim deferred tests passed. Keep the prior accepted package backed up; unfinished Hungry Chest sources must not enter the Bore-only export.

## Source of truth
- TC4 4.2.3.5 original JAR and its private oracle are the only gameplay specification.
- Legacy src/ is the audited 1.12 reference, intentionally not compiled by the modern build.
- OpenTC4 and OpenTC4-Architectury are pinned references only. Architectury ref targets 1.20.1 and is unfinished; never copy its entire source or claim it is working.
- Preserve canonical TC4 values/quirks, research keys, aspect tags. Fix reproducible original crashes/authority defects only with evidence and document each deviation.
- Never modify original/donor JAR or thaumcraft_src. Keep decompilation outside this repository.
- The original JARs (`Thaumcraft-1.7.10-4.2.3.5.jar`, `Thaumcraft-1.12.2-6.1.BETA26.jar`) live only locally at the repository root, ignored by git; their hashes are in docs/porting/source-lock.json. Never commit them: on 2026-10-26 the owner had them removed from the whole history before publishing the repository. Tests and scripts that read them skip or fall back to thaumcraft_src when they are absent.
- Fresh 1.21.1 worlds only; no 1.7/1.12 world migration is authorized.

## Active layout
- common/src: shared gameplay/data, public thaumcraft.api, tests, resources.
- fabric/src and neoforge/src: loader bootstrap only and required platform integrations.
- scripts/porting: deterministic extraction/conversion with provenance.
- docs/MIGRATION_STATUS.md and docs/HANDOFF.md: resumable state, missing units and exact evidence.

## Verification
- Start with git status --short. Read relevant original behavior before gameplay edits.
- Compile the actual target, not source-text stubs. Use Java 21 and the Gradle 8.12.1 wrapper.
- Run meaningful behavioral tests for state transitions and resource preservation.
- Run headless dedicated server smoke for runtime-affecting checkpoints. Client visuals require inspected game evidence, not compile success.
- Use focused gameplay checks for routine content changes. Do not replay the entire long client progression for each feature/fix; reserve full progression runs for shared-system regressions and broader checkpoints. Reused saved inputs must be declared, not presented as acquisition replayed in the current run.
- Never claim build/test/runtime pass unless it ran. Mark implemented/tested/seen/unverified separately.
- A catalogue/model or placeholder is not a completed gameplay unit. Keep all missing units visible.
- Build after final source changes. Preserve package names where practical; document forced API/data-component/ID flattening changes.
- Keep transient tools/logs/cache in .tmp, .gradle21 or ignored run/build folders. No credentials, game saves or original decompile in commits.
- **Owner decision 2026-10-26:** the repository is published and releases go out under the name Thaumcraft 4 Reload. Keep attribution/provenance in THIRD_PARTY_NOTICES.md; publishing does not establish rights to Azanor's material, and original assets/code ownership is not changed by a reference repository's template license.
