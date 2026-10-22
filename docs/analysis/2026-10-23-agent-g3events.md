# g3events — event/lifecycle hypothesis — 2026-10-23

Worktree `/home/cyberfox-kali/wt/g3events`, branch `agent/g3events`.
Baseline `d6ae5c78`; first requested status/log command succeeded with clean status.
No other worktree or primary checkout was accessed; no push or NeoForge build/run.

## Result

- Inventoried **106 registration/subscriber sites**, grouped into 54 table rows in `docs/audit/EVENT_FINDINGS.md`.
- Confirmed **one defect**: static dimension-only chunk-regeneration queue outlived its owning world/server. Two already-registered lifecycle callbacks omitted this subsystem's cleanup.
- Fixed instance isolation (`ServerLevel` keys), unload cleanup, and server-selective shutdown fallback.
- Added `EventLifecycleRuntimeChecks.regenerationQueueLifecycle` and a Fabric GameTest entrypoint. It checks world-instance ownership, dispatches registered unload cleanup, and measures the real world-post drain over three ticks: 21 → 11 → 1 → 0. It also directly checks the shutdown fallback.
- No confirmed never-installed modern handler or doubled gameplay tick. No wholesale event rewrite.

## Proof and limitations

Before patch, `readMarker`, `queued`, and `tick` used `level.dimension()` to access one static map and no code removed entries. The next save using the same dimension in the same JVM would drain the previous save's coordinates against the new world's seed/terrain. Regeneration must be enabled to enqueue through the normal marker path; it is disabled by default.

All registrations except the focused runtime case are source-reasoned. Both loaders reach the guarded shared root; client bootstrap is client-only. ChampionHooks' two init call sites share a pre-registration guard. Runic/accessory ticks descend from one living-entity aiStep mixin rather than duplicate event subscriptions. Client disconnect reset repetition is intentional/idempotent, not a doubled rate.

TradeSwaps PRE versus retained 1.12 END is **unresolved**, not confirmed against the authoritative 4.2.3.5 oracle; no phase change made. No two-save reconnect, client visual observation, multiplayer test, or NeoForge execution is claimed. The test deliberately disables regeneration before queued work drains, avoiding terrain mutation; the queue/lifecycle/callback rate, not feature-generation effects, is what it measures.

## Validation

Java 21, isolated `GRADLE_USER_HOME=/home/cyberfox-kali/wt/g3events/.gradle-g3`:

```sh
./gradlew :common:test --tests thaumcraft.common.lib.events.ChunkRegenerationParityTest :fabric:runGameTest -PgameTestFilter=regenerationQueueLifecycle --no-daemon
```

Run launched; result pending at initial draft. Local log `.tmp-g3-gradle.log` is ignored and not committed. `git diff --check` passed after source edits. Final run outcome will replace this pending paragraph before handoff.
