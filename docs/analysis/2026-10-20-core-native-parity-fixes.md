# Tainted actors: original death-time XP and reload audit

Read-only source-of-truth review: TC4 4.2.3.5 original EntityTaintSpider,
EntityTaintSwarm, EntityTaintSpore/Swarmer, EntityTaintacle/Small; vanilla1.7.10
EntityLivingBase (sv), EntityLiving (sw), ExperienceOrb (sq), compared with
actual mapped1.21.1 LivingEntity/ExperienceOrb and active OriginalTaintExperience.
No original/donor modifications. The scoped helper is active common production code; this independent review adds no further production state.

Confirmed contracts:
- Original sv.aF increments deathTime first and awards only when equal20.
  sv.C calls it BEFORE decrementing recentlyHit. Native LivingEntity.baseTick
  also invokes tickDeath BEFORE decrementing lastHurtByPlayerTime. Therefore
  a real player hit,80 living updates, environmental lethal damage and20 death
  updates still earns XP, while81 living updates does not. Existing fixtures
  use native damage/tickers and retain these positive/negative boundaries.
- Original gates are recent-player OR original always-dropper, should-drop-XP,
  doMobLoot; reward is computed at death20, not snapshotted during die().
  Original equipment bonus is only for positive base and original five slots
  (mainhand, boots, leggings, chest, helmet),1+entityRNG.nextInt(3) for each
  eligible present item. Modern offhand contributes no original bonus.
- Original canonical split is preserved (10 ->7+3;5 ->3+1+1;2 ->1+1).
  Native ExperienceOrb.award would introduce WORLD nextInt(40) immediate merge
  rolls. The opt-in helper instead constructs canonical ordinary-ID native
  orbs, with independent original scatter, and calls native admission once.
- The helper claims deathTime20 before ADD callbacks and fences source tick
  reentry. Its orb rejects playerTouch until it is the actual indexed owner.
  Existing tiny-spider runtime observes real ADD, invokes actual pre-admission
  playerTouch and source tick reentry, and verifies no free/duplicate XP or
  extra WORLD draws. These are runtime assertions, not a seen-client claim.

A proposed production equipment-presence persistence change was CANCELLED
before any source edit. Native equipment drops clear an actual dropped slot,
whereas original equipment remained. However, BOTH original sv.b/a(NBT) and
native LivingEntity.save/load omit recent-player timer/attacker attribution.
A new-instance pending-death reload therefore has zero eligibility in the
original too. Losing the transient equipment-presence cache demonstrates no
actual reward drift for these Taint actors. Do not persist eligibility/reward
or introduce presence NBT just to obtain an invented11..13 XP reload result.

The new regression in TaintacleRuntimeChecks uses actual native guaranteed mainhand drop,
ordinary attributed kill,19 real entity death updates, public saveWithoutId,
retirement, a new instance's public load, same-UUID native admission and the
next real death update. It asserts that saved death/equipment/identity survive,
recent-player attribution does not, and death20 releases no XP. No eligibility,
reward or RNG is forced. Existing positive equipment11..13 and80/81 vectors
remain unchanged. It uses the existing Taintacle runtime wrapper; no new test
harness or production API. The existing native main-XP/equipment/80–81 assertions passed in the fresh local262 run (260PASS/2FAIL). The newly added pending-reload assertion still awaits native replay. The Spider wrapper failed on its earlier player attack fixture, so its later XP/admission assertions have NOT run yet.

## Checkpoint evidence

- Exact fc1f8ff GitHub run39426400166:261 total,255PASS/6FAIL; not accepted.
- Local fresh-world fixes run:918JUnit/84Python PASS;262GameTest,260PASS/2FAIL. Log .tmp/core-native-parity-fixes-runtime.log.
- Remaining fixture failures were real native login spawn protection before spider damage and the restored detached creative Pearl argument. They are corrected by actual player updates and physical hand/inventory custody checks, preserving the damage and all node-state invariants. Final compilation/native replay are tracked in MIGRATION_STATUS.md; this document grants no full acceptance.
- Actual fresh25-chunk forest native decoration produced13Vishroom/4typedManaPods and real weighted Pech admission. Final WORLD_SURFACE/OCEAN_FLOOR maps reflect earlier FEATURES writes, unlike stale WG maps.
- Client visuals, full survival/multiplayer and NeoForge remain unverified for this checkpoint.

Latest local262-case replay: actual Taintacle XP wrapper PASS, including19/20,
equipment/offhand/base0 and80/81 vectors. The tiny-spider wrapper failed on
its earlier attack assertion, so its newer XP/admission tail did NOT run in
this replay. Do not present those assertions as passing server evidence yet.
