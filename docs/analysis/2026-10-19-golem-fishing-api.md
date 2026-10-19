# Fishing11 — native implementation agreement

Authority: [original task/loot contract](2026-10-19-golem-next-core.md),
[implemented loot helper](2026-10-19-golem-fishing-loot.md).
This agreement is not gameplay/visual acceptance.

## Source and ownership

One actual live golem-owned bobber/cast; no player FishingHook, paid runtime rod,
five-field configuration expansion, fish-entity kills, ghost supplies or XP.
Native source water/air and all quality cells must be loaded and permitted;
baseline accepts actual vanilla flowing/source water, not waterlogged solids.
Use original random probes, quality, chance/counter and exact one-result loot.
Catch authority remains in the private live goal, not in the pure loot generator.

Entity/goal owner adds FISHING11, no GUI/inventory routing/ghost controls, marker
list retained but ignored. `getFishingBobber()` resolves only its actual bobber;
`hasFishingCast()` is display state, not real cargo. `getRightArmTimer()` returns
raw5…0, status8; existing action/status4 remains separate. `startFishingArm()` is
package-private. No actual fishing rod is put in equipment or `getCarriedItem()`.

Bobber public contract for its separate owner:

```java
EntityGolemBobber(EntityType<? extends EntityGolemBobber>, Level);
void setup(EntityGolemBase fisher, BlockPos water);
EntityGolemBase getFisher(); // nullable, actual ID+UUID/level-bound
BlockPos getWaterTarget();
```

Type `ThaumcraftGolems.GOLEM_BOBBER` is transient/noSave. Rejected admission must
not authorize a cast or leave a detached owner; reuse existing narrow rejected-
spawn cleanup where needed. Recall/death/core change/unload/invalid water clean
up the bobber. Restarting does not restore/reroll a completed result.

## Paid output, not another inventory

At a successful valid cast, roll **once** into a private pending result with the
original water output position/once-sampled trajectory and pickup delay20.
Publish through root's existing queue extension:

```java
DeferredItemDrops.enqueueGolemFishingCatch(ServerLevel, BlockPos water,
                                        Vec3 motion, ItemStack actualResult);
```

This trusted-server API computes position `(x+.5,y+1,z+.5)`, appends one existing
Pending and does not flush/call world hooks. Caller clears its actual pending
owner immediately after normal return, before feedback/bobber removal callbacks.
If publication fails beforehand, retain the result/trajectory without rerolling.
Save an unpublished paid output in native entity NBT (not GolemData) if needed;
death/recall must evacuate it before removing the entity. This is bounded one-
result conservation, not a new journal/token/provider or fish transport inventory.
Queue retry owns its stored stack/motion; it never rerolls rarity/enchantments.

Pond/output permission helper is root's
`GolemHarvestPlatform.mayInteract(EntityGolemBase, BlockPos)`: canonical native
world/loaded/border/profile/spawn checks, no fake-player OP/owner borrowing. It
does **not** emit crop-break events or destroy water. Spawn admission remains
native. Addon-specific non-breaking fishing claims are not silently claimed to
be universally supported; do not invoke effectful UseBlock callbacks as a query.

## Root/client integration

Root registers CORE_FISHING, `coreForType(11)`, exact Harvest-centered infusion
and rod component DAMAGE0. No GUI for11. Client adds bobber/line rendering and
raw independent-arm pose; any rod is a source-backed **display**, never equipment
or stock. Model action overrides independent arms, then carry/walk. Native owned
pending output remains separate from display snapshots and ordinary carried items.

Guard/input-damage build is historical evidence for those sources only. Fishing
runtime/client/integration still require their own final actual Fabric compile;
gameplay, protection, loot rates and visuals remain deferred.
