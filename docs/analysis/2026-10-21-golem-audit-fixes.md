# Golem and shared-infrastructure audit fixes (a7) — 2026-10-21

Source of truth: `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (TC4 4.2.3.5, the only
gameplay specification). Audit: [2026-10-21-audit-a7-golems.md](2026-10-21-audit-a7-golems.md),
defects 1–9 and the decisions 2–5.

Every number, message and rule below comes from the oracle; the native names and the
1.21.1 API are the only things this port chose.

## Fixed

### 1. The owner protection of a golem (audit defect 1)

Oracle `thaumcraft/common/lib/events/EventHandlerEntity.java`:348-356 — `playerInteract`
cancelled **every** interaction with a golem that has an owner when the interacting player
was not that owner, and answered "You are not my Master!".

* `EntityGolemBase.mobInteract` now refuses the interaction first and sends the original
  message (`tc.golem.notowner`, the key the language file already carried).
* `EntityGolemBase.canConfigure` now refuses a stranger, so the configuration menu, the
  core, the upgrades, the accessories, the bell link, the recall and the dismantling with
  the bell belong to the owner alone.
* The comparison is the original one: the owner's **name** against the player's name.
* New pure rules `GolemOwnership` (`denies`/`allows`), covered by
  `common/src/test/java/thaumcraft/golems/GolemOwnershipTest.java`.

### 2. The ender pearl next to a warded object (audit defect 2)

Oracle `thaumcraft/common/lib/events/EventHandlerEntity.java`:358-379 — `entitySpawns`
scanned the cube from −5 to 5 on every axis around a joining ender pearl for a `TileOwned`
block entity, told the thrower "The magic of a nearby warded object destroys the ender
pearl." and killed the pearl.

* New `thaumcraft.common.lib.ward.WardedPearl`: the same cube, the same `TileOwned` test
  (the arcane door, the arcane pressure plate and the warded glass are the native owned
  tiles), the same message (`tc.warded.enderpearl`, dark purple and italic like the
  original's `§5§o`) and the same death: the pearl is marked dead and leaves the world on
  the next tick, exactly like the original's `setDead()`.
* The 1.21.1 stand-in for the original's `EntityJoinWorldEvent` is the entity admission of
  the level's entity manager, which Architectury publishes as `EntityEvent.ADD`. The rule
  runs on the server only, like the original.
* Chunks that are not loaded are skipped: the original only ever read the loaded world.

### 3. The thaumium golem and the magic damage (audit defect 4, decision 5)

Oracle `thaumcraft/common/entities/golems/EntityGolemBase.java`:1131-1138 —
`if (this.getGolemType() == EnumGolemType.THAUMIUM && ds == DamageSource.field_76376_m)
par2 *= 0.5f;` and, in the same method, `if (ds == DamageSource.field_76367_g) return false;`
— the immunity the traveling trunk shares (oracle `EntityTravelingTrunk.java`:107).

* `DamageSource.field_76376_m` is the 1.7.10 "indirectMagic" source, the one 1.21.1 keeps
  as `DamageTypes.INDIRECT_MAGIC` (the port's own magic projectile damage). A thaumium golem
  now takes half of it; the factor lives in the pure class `GolemDamageRules`.
* `DamageSource.field_76367_g` is the 1.7.10 "inWall" source: the very same line of this
  mod in the 1.12.2 revision names it `DamageSource.IN_WALL` (the shipped legacy
  reference, `src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java`:858),
  so the shared immunity is the in-wall damage, not the cactus damage the trunk carried.
  The golem and the trunk are now both immune to `DamageTypes.IN_WALL`.

### 4. The Guard's target mask (audit defect 3, decision 2)

Oracle `thaumcraft/common/entities/golems/EntityGolemBase.java`:800-831 and 1150-1180 —
the four switches are **negative** bits of the `toggles` byte (bit 1 monsters, 2 animals,
3 players, 4 creepers; a set bit forbids the category) and a fresh Guard has every bit
clear, so a Guard with Order attacks monsters, animals, players and creepers until the
owner switches a category off in the GUI.

* `GolemGuardTargets` is negative again and its default is `0`: nothing is forbidden, the
  original's default. `toOriginalToggles`/`fromOriginalToggles` are the identity of the
  original's byte (the mask is the original's forbidden bits).
* The menu keeps the original button ids 51–54, and the GUI still lights a box while the
  category may be attacked, exactly like the original's `canAttackX()`.
* A Guard without Order still attacks hostile monsters only: the original's Order upgrade
  is what allows animals, players and creepers.

### 5. The Guard's keep distance (audit decision 3)

Oracle `thaumcraft/common/entities/ai/combat/AITarget.java`:31-42 and
`AINearestAttackableTarget.java`:41 — the goal keeps its target while the distance stays
inside `getRange()`, the golem's own range, not the vanilla follow range.

* `GuardGolemGoals.Nearest.canContinueToUse` compares against `golem.getRange()` now.

### 6. The magical log as fuel (audit defect 7)

Oracle `thaumcraft/common/lib/events/EventHandlerWorld.java`:139-147 — the fuel handler
returned 6400 for the alumentum and 400 for `ConfigBlocks.blockMagicalLog`.

* The alumentum's 6400 was already ported. The greatwood log (the original's
  `blockMagicalLog` metadata 0) now burns 400 ticks
  (`ThaumcraftResources.MAGICAL_LOG_BURN_TIME`); the silverwood log keeps the vanilla burn
  time, as in the original.

## Reported only, not changed

* **The chunk regeneration queue (audit defect 6).** The original queued the chunks
  without the `Thaumcraft.regen` tag and regenerated up to ten of them per tick. 1.21.1
  has no chunk data event with that tag, so the rule has no equivalent API. Known gap.
* **The block placement next to an active Outer Lands boss (audit defect 8).** The
  original cancelled a placement when a `EntityThaumcraftBoss` stood within 32 blocks in
  the Outer Lands, in a maze cell of feature 2..5. The native has neither the boss entity
  nor the maze. Known gap, and the maze is out of scope here.
* **The eldritch golem's beam (audit defect 5).** The original drew a pulsing beam from
  the boss's head with `FXBeamGolemBoss` and `PacketFXBeamPulseGolemBoss`. The native
  replaced it with the `EntityGolemOrb` projectile: a deliberate 1.21.1 adaptation that
  keeps the damage, the charge and the cooldown. Left as it is.
* **The client research cache on logout (audit defect 9).** The original's
  `GuiResearchBrowser.completedResearch` was a process-wide map keyed by the player name;
  the native has no such map. The native knowledge lives in the player itself
  (`PlayerKnowledge`), every login snapshot replaces it wholesale (`load()` resets it
  first), and the client configuration flags are restored on disconnect
  (`ThaumcraftSettings.resetClient()` in the Fabric disconnect handler). Nothing is lost,
  so nothing was changed.

## Verification

* `:common:compileJava` and `:common:test` — 1525 JUnit, 0 failures (the guard target
  tests were rewritten for the original's negative mask, `GolemOwnershipTest` is new).
* The Fabric GameTest source set does not compile in this tree for an unrelated reason
  (`ItemBottleTaint.burst` is missing, `EquipmentItemRuntimeChecks.java`:132), so the
  golem GameTest scenarios were updated but not executed here.
