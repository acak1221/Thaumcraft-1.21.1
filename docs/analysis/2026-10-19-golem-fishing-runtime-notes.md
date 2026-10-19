# Fishing11 runtime — FINAL owned-source handoff, 2026-10-19

**Implemented, not compiled or gameplay-accepted.** Root must compile the actual
Fabric inputs after this handoff and the independent bobber/client FINALS. The
reported Guard/input-damage/loot-helper build and nine existing data JUnit results
precede these runtime edits; they do not validate Fishing11.

## Owned files and authority

Only these files were authored for this slice:

- `common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java`
- new same-package `FishingGolemGoals.java`
- this new note. No additional pending-output helper was needed.

Authority is original `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/entities/ai/interact/AIFish.java`
(`F` below), the FINAL [next-core contract](2026-10-19-golem-next-core.md),
[loot handoff](2026-10-19-golem-fishing-loot.md), and
[agreed API](2026-10-19-golem-fishing-api.md). Bobber physics/rendering was not
duplicated; its owner's WIP and the loot/client/queue implementations were not
opened for this work. Existing crop/material/inventory/Guard helpers are unchanged,
including root's actual-`Block.class` melon correction in `GolemCrops.java`.

## Runtime and original-source anchors

- `FishingGolemGoals` is one priority2 MOVE/LOOK goal. Eligible idle probing is
  explicitly every fifth server tick, independent of selector admission cadence.
  Radius is `ceil(range/2)=8`; at most16 golem-RNG X/Y/Z probes select the first
  loaded actual `Blocks.WATER`/air-above candidate, home±8 X/Z and ±4 Y
  (F:67–104,165–178). Vanilla flowing states qualify; waterlogged solids do not.
  No walking-to-water, full search-region loading, pond sorting or marker fishing.
- Start quality uses only four eligible horizontal water/air neighbors, their
  sky-above observations, and independent depth1…3 cells. The existing
  `GolemFishingLoot.Pond(A,S,D).quality()` supplies the formula (F:86–101).
  All observed cells, including the actual water-center output's air block, must
  be loaded, in height/border, unchanged and permitted through root's
  `GolemHarvestPlatform.mayInteract(golem,pos)`.
- Root integration corrected the count RNG to **`300 + server.random.nextInt(200)`**,
  matching F:161; the earlier worker instruction incorrectly requested golem RNG.
  Probe RNG remains golem RNG; probability, loot and trajectory RNG remain world
  RNG. Modern spawn/FX adaptations still preclude seed-for-seed whole-cast claims.
- `requiresUpdateEveryTick()` is true. `activeTick` performs the literal
  `count-- > 0` continuation rule after the initial start update; it is not in
  modern selector cleanup, which can run at a lower cadence. Each eligible active
  update rolls `server.random.nextFloat() < quality + strength*.00015F`
  (F:107–162). Timeout pays nothing; no bite timer, Lure or guaranteed catch.
- On success only, re-sample Pond for **category**, not replacement cast quality.
  Revalidate worker, exact admitted bobber, source state, sampled cells, source/
  actual-output permissions and native clear line before calling
  `GolemFishingLoot.rollCatch(server,server.random,pond)` exactly once. A private
  attempt latch precedes the permission callbacks, so cancellation cannot reopen
  the same cast for another roll. The helper's documented native enchantment gaps
  remain unchanged (F:181–211).
- After loot generation, F:132–139 is retained literally: **four world
  `nextFloat()` draws**, X draws before Z, offsets
  `(gx+U1−U2−x+.5, gy−y+1, gz+U3−U4−z+.5)`, velocity
  `(.1dx,.1dy+.08*sqrt(length(delta)),.1dz)`. Both legacy square-root float
  roundings are retained. No normalized `.35` axe throw, Gaussian replacement or
  re-sampled retry motion. Queue output is `(x+.5,y+1,z+.5)`, delay20.

## Bobber authority, lifecycle and client API

- Admission comes only from the real worker: fresh constructor/setup, existing
  rejected-spawn snapshot, native `addFreshEntity`, **true return plus current
  same-level ID and UUID identity proofs** for golem and bobber, and
  `bobber.getFisher()==golem` / matching `getWaterTarget()`. False/exceptional
  admission never authorizes work. Cleanup discards the exact source object and
  uses only that attempt's rejected reservation; it never rolls back a foreign
  callback or removes a replacement lookup entity. In-progress add/removal
  callbacks cannot release the attempt recursively; failed cleanup retains the
  reference and blocks another cast until retried.
- Source water/air exact states and fresh native COLLIDER/Fluid.NONE cast-line
  clearance are checked during the cast and at generation. Only actual ray cells
  plus a one-cell X/Z collision-shape halo are guarded; nothing force-loads the
  whole17×17 search region. Continuous source/obstruction checks, loaded/claim
  checks and failure-closed admission are disclosed native safety adaptations.
- Pause/ineligible work, timeout, obstruction, changed source/home/core, goal stop,
  recall and death cancel transient cast authority and clear display IDs. Native
  removal also cancels. Native chunk-unload paths that directly remove the owner
  must be covered by the independent bobber's ID+UUID owner-invalid cleanup;
  `getFisher()` must not keep a detached/unloaded/removed owner alive.
- Public `EntityGolemBase.getFishingBobber()` is nullable and typed. It checks
  synchronized bobber ID **and UUID**, level/current identities and native fisher;
  server resolution additionally requires this worker's exact admitted bobber.
  `hasFishingCast()` is display state only. Neither is reward authority.
- Package-private `startFishingArm()` uses raw right-arm5…0/status8, guarded by
  timer0 as in original `EntityGolemBase:1028–1052`. Start/catch feedback uses it;
  left arm remains0 and Guard/Harvest action6/status4 is unchanged. Native callback
  paths run under the entity's busy guard. No rod is generated in equipment,
  returned as cargo, or substituted by `getCarriedItem()`.

## One paid output, never cargo or a second reward

- Private `Catch(dimension,water,motion,stack)` owns at most one actual result.
  Cancellation, goal resets, changing home/core and failed publication **do not
  clear it**. Pending stock blocks new work but never enters carry space, filters,
  stock counts, menus, home deposits or ordinary directed throws.
- Root's agreed `DeferredItemDrops.enqueueGolemFishingCatch(...)` must append one
  existing Pending without flush/world hooks. On normal return, the very next
  operation is raw `pending=null`, before arm/audio/particles/bobber removal.
  Failure before publication retains the same native stack and trajectory; retries
  do not regenerate loot. After publication, only the existing queue owns retry.
- Generation/initial commit rechecks source and output permissions. An already
  paid catch may subsequently evacuate at its original source position even if
  water dries or the worker dies, pauses, changes core or transfers dimension.
  That is recovery of owned output, not authorization to fish unknown/dry water.
  A removed transfer source is refused publication by native identity checks.
- Native entity NBT alone stores unpublished `FishingCatch` with exact keys
  `Dimension` (string), `Water` (int[3]), `Motion` (three doubles), `Stack` (native
  stack compound). Nothing is added to five-field `GolemData`; cast target,
  countdown, chance, bobber and arm timer are not saved.
- Save attempts evacuation **before** capturing this row; on failure it serializes
  the same output. Death/recall/destructive removal refuse deletion if evacuation
  fails. Unload/transfer removal does not publish again after an already serialized
  source image: the saved/copied entity retains any unpublished row instead.
  Busy/entity reload refuses to overwrite existing pending ownership or publish
  and then re-import the same row. No new journal/token/version or cross-store
  crash-atomicity mechanism was introduced.
- Malformed/future rows, nonfinite motion, unknown fields or lossy stack decoding
  retain the original raw NBT and freeze work/recall/flattening, following the
  existing unread-cargo pattern. No silent normalization or generic fallback drop.
- Ordinary recall keeps Fishing11/material/name/markers; dismantle uses root's
  `coreForType(11)`. Guard's foreign-equipment recall refusal remains, and Fishing
  now shares it, including a fresh check after bobber-removal callbacks. No real
  equipment is purged or converted into supplies.

## Root integration required before the actual Fabric build

1. `GolemData.FISHING=11`, supported baseline six EMPTY filters, six zero counts,
   false Fill mode. `CORE_FISHING`, nullable `coreForType(11)`, pristine
   Harvest-centered **CoreFishing / COREFISHING** infusion/research/assets remain
   root-owned. Installation refuses bad baseline controls before consuming a core.
   Runtime enables11 but excludes it from menus, inventory AI and home deposit;
   marker data survives inertly. No schema expansion or new ghost controls.
2. Register transient/noSave `ThaumcraftGolems.GOLEM_BOBBER` with the agreed
   constructor/setup/getFisher/getWaterTarget API. `getFisher()` must resolve its
   actual native owner independently **before** the golem's display IDs are
   installed; do not make initial admission depend on a reverse display lookup.
   Physics, standalone owner-invalid cleanup and client line/rod display remain
   the independent bobber/client owners' work, not implemented here.
3. Provide `GolemHarvestPlatform.mayInteract(EntityGolemBase,BlockPos)` as agreed:
   native loaded/world/border/profile/spawn authority only, not break callbacks,
   effectful UseBlock queries, owner impersonation, OP or mobGriefing bypasses.
4. Provide the agreed **void, no-flush/no-callback**
   `DeferredItemDrops.enqueueGolemFishingCatch(ServerLevel,BlockPos,Vec3,ItemStack)`.
   It must not publish and then throw as though ownership never transferred.
5. The existing `RejectedEntitySpawn` was ItemEntity-only at the inspected
   checkpoint. Root must generalize/overload its **existing narrow**
   `snapshotBefore(ServerLevel,Entity)` and
   `releaseRejected(ServerLevel,Entity,Snapshot,boolean)` for this actual bobber;
   retain `Snapshot.knownBefore()` and exact attempt/manager/identity checks.
   No duplicate cleanup framework was added to this worker.

Seven materials (0,1,2,3,5,6,7), health/armor/regen/attributes, safe-home behavior,
remote bell-link delegation, real cargo, raw directed-throw detach and the existing
Guard/Harvest/Sorting policies remain in place. Fishing consumes no water/fish/
rod/bait, awards no XP/player credit and performs no collection; player/Gather
collect the genuine queued ground result. Upgrades, universal addon nonbreaking
claim support, rates, cancellation/protection behavior and client visuals remain
explicit gaps/unverified. No Gradle, Java, tests, game, Python, saves, agents,
fixtures, hashes, proof manifests or smoke tooling were run/created for this work.
