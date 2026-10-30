# Loader hooks of the Thaumic Tinkerer add-on

Architectury has no events for several things that the 1.7.10 mod did through Forge events. The feature groups first worked
around each gap and wrote the loss down (`docs/api-requests/*.md`, `docs/deviations/*.md`). This is the loader layer that closes
the gaps: **real events**, posted from the vanilla code paths themselves, and two small registries. The groups are **not yet
rewired** to them; the rewiring is a separate step (section 10 says per event who does what).

| What | Where |
|---|---|
| The events and the rules of every one | `common/.../thaumic/tinkerer/common/hooks/TinkererEvents.java` (loader neutral, Architectury `Event`s) |
| Access to the flying-kick counter | `common/.../common/hooks/FloatingTicks.java` |
| Built-in item renderer registry (client) | `common/.../thaumic/tinkerer/client/hooks/ItemRenderers.java` |
| Fabric posters (Mixin) | `fabric/src/main/java/thaumic/tinkerer/fabric/mixin/*`, config `fabric/src/main/resources/thaumictinkerer.mixins.json`, listed under `"mixins"` of `fabric/src/main/resources/fabric.mod.json` |
| Fabric helper of the drop capture, Fabric item renderer backend | `fabric/.../fabric/hooks/DropCapture.java`, `fabric/.../fabric/client/FabricItemRenderers.java` |
| NeoForge posters | **not written** (`neoforge.gradle` compiles the common tree; section 9) |
| Runtime tests | `common/src/gameTest/.../testing/HooksRuntimeChecks.java`, Fabric wrappers `fabric/src/gameTest/.../testing/HooksGameTests.java`; JUnit `common/src/test/.../hooks/` |

## 1. Overview

| Event | 1.7.10 original | Sides | Fabric posting point (Mojang names) | NeoForge counterpart | Will be used by |
|---|---|---|---|---|---|
| `LIVING_DAMAGE` | `LivingHurtEvent` (`ammount`) | server | `LivingEntity.hurt`, just before the first `isSleeping()` call, i.e. right after the four early `return false` checks | `LivingIncomingDamageEvent` | Enchantments (Pounce, Final Strike, Valiance, Focused and Dispersed Strikes: replaces `AttackBoost`), KamiWorld (Soul Hearts: `SoulHeartHandler.modifyIncomingDamage`) |
| `BREAK_SPEED` | `PlayerEvent.BreakSpeed` (`newSpeed`, cancel) | **both** | `BlockBehaviour.getDestroyProgress`, wrapping the call of `Player.getDestroySpeed(BlockState)` | `PlayerEvent.BreakSpeed` (`setNewSpeed`, cancel; its position is `getPosition()`) | Enchantments (Shatter, Tunnel, Disintegrate, Flaming Touch: replaces `BreakSpeedControl`), TileBlocks (the Animation Tablet's fake player) |
| `LIVING_JUMP` | `LivingJumpEvent` | both | `LivingEntity.jumpFromGround`, `TAIL` | `LivingEvent.LivingJumpEvent` | Enchantments (Ascent Boost), KamiGear (Gem Boots' jump) |
| `LIVING_FALL` | `LivingFallEvent` (`distance`, cancel) | both | `LivingEntity.causeFallDamage`, `HEAD` | `LivingFallEvent` (`setDistance`, `setDamageMultiplier`, cancel) | Enchantments (Shockwave, Slow Fall), KamiGear (Gem Boots set the distance to 0) |
| `USE_ITEM_TICK` | `EntityPlayer.itemInUseCount` (written every tick by the Quick Draw handler) | both | `LivingEntity.updateUsingItem`, `HEAD` | `LivingEntityUseItemEvent.Tick` (`setDuration`) | Enchantments (Quick Draw) |
| `LIVING_DROPS` | `LivingDropsEvent` (`drops`, cancel) | server | `LivingEntity.dropAllDeathLoot` wrapped; capture in `Entity.spawnAtLocation(ItemStack, float)` and `ServerPlayer.drop(ItemStack, boolean, boolean)` | `LivingDropsEvent` (`getDrops()`, cancel) | Machines (Cursed Spirit's Blade), KamiWorld (`DimensionalShardDropHandler`) |
| `FloatingTicks` | `NetHandlerPlayServer.floatingTickCount` | server | accessor mixin on `ServerGamePacketListenerImpl.aboveGroundTickCount` | an access transformer or the same mixin | Foci (Uprising) |
| `ItemRenderers` | `MinecraftForgeClient.registerItemRenderer` | client | Fabric API `BuiltinItemRendererRegistry` | `RegisterClientExtensionsEvent` + `IClientItemExtensions` | Machines (Mob Display) |

## 2. Rules that hold for every event

* A listener is registered with `TinkererEvents.X.register(listener)` and removed with `unregister(listener)` (Architectury's `Event`).
  Registration is not thread safe: do it at mod start-up (the groups' `ready()`), or on the server thread while nothing is posting.
* Listeners run in the order they were registered. An event that changes a value hands the value from one listener to the next, so a
  multiplication registered before an addition gives another result than the other order. The chain stops at the first cancel.
* A lone listener is the invoker itself in Architectury, so the **poster** (`TinkererEvents.post...`) does the normalising
  (zero, negative and NaN mean cancel). Listeners never call the `post...` methods; the loader modules do.
* A listener that throws brings the exception into the game code that posted the event, as with Forge. Do not throw.
* Events that run on both sides run the listener on both sides. A listener that changes the **result of a calculation both sides do**
  (the dig speed, the jump, the bow draw) must change it by the same rule on both, or the client's prediction drifts from the server.
  A listener with an **effect on the world** (damage to others, drops) must test `level.isClientSide` where the event is not server
  only.
* **In tests and in code that outlives a call, answer only for your own entities.** Listeners are global.

## 3. `LIVING_DAMAGE`

```java
float onLivingDamage(LivingEntity victim, DamageSource source, float amount)   // the new amount; <= 0 or NaN cancels
```

*Posted* on the server from `LivingEntity.hurt`, after the checks that make an entity ignore a hit and **before** anything reduces
the amount: before a shield blocks, before the extra damage of freezing and the helmet's share, before the invulnerability frames
compare the hit with the last one, before armour, Protection, Resistance and Absorption. For every living entity, whatever dealt
the damage (blow, arrow, thorns, fall, fire). For a player the amount is already scaled by the difficulty (`Player.hurt` does it
before `super.hurt`). A hit of zero damage (a snowball) is not posted.

*A listener may* change the amount (the next listener sees it) or cancel by returning zero. A cancel makes `hurt` return `false` at
once, **as for an invulnerable entity**: no damage, no hurt animation or sound, no knockback, no invulnerability frames, no last
damage source. The listeners after a cancel are not called.

*Differences from Forge's `LivingHurtEvent` of 1.7.10*

* 1.7.10 posted it in `damageEntity`, after the shield (there was none) and after the invulnerability frames had subtracted the last
  hit (`damageEntity(source, amount - lastDamage)`); here the event comes first and the frames compare the **listener's** result with
  the last hit. The test pins it down: a listener that halves turns 10 and then 8 into 5 and 4, and the second hit is refused, where
  vanilla would let 3 of it through. This is NeoForge's order as well (`LivingIncomingDamageEvent`).
* A cancel in 1.7.10 only skipped `damageEntity`: knockback and the hurt animation still happened. Here a cancel is a refusal of the
  whole hit.
* 1.7.10 passed the amount after shield blocking; here a hit that a shield will block is posted with its full amount.
* The armour of KamiGear (`ISpecialArmor`, `docs/api-requests/KamiGear.md` section 1) is **not** served by this event: it sat
  *after* the event in 1.7.10 (inside the armour calculation). It still needs a hook on `getDamageAfterArmorAbsorb`. Not written.

*NeoForge*: a handler of `LivingIncomingDamageEvent` calls `TinkererEvents.postLivingDamage(entity, event.getSource(), event.getAmount())`,
`event.setAmount(result)` and `event.setCanceled(true)` when the result is zero (the event is posted by `CommonHooks.onEntityIncomingDamage`
at the same place of `hurt`).

*Fabric*: `LivingEntityHurtMixin`, an `@Inject(cancellable)` at the `INVOKE` of `isSleeping()` with a `@Local(argsOnly)` `LocalFloatRef` of
the amount (MixinExtras). Every override of `hurt` that reaches `LivingEntity.hurt` through `super` is covered (players: `ServerPlayer.hurt`
→ `Player.hurt` → here). An entity whose `hurt` does not call `super` (the armour stand) is not.

## 4. `BREAK_SPEED`

```java
float onBreakSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed)   // the new speed; <= 0 or NaN: cannot dig
```

*Posted* on **both sides** by `BlockBehaviour.getDestroyProgress(state, player, level, pos)`, which is what
`BlockState.getDestroyProgress(player, level, pos)` calls: every tick of a server player's dig, the client's prediction of the same
dig (the crack and the moment the client thinks the block is gone come from this number, so a server-only event would mispredict), the
start of a dig (`ServerPlayerGameMode.handleBlockBreakAction`), and anything else that asks. That includes a `ServerPlayer` that is
never ticked and never in the level: the Animation Tablet's `TinkererFakePlayer` calls `state.getDestroyProgress(fake, level, pos)`
and the event is posted for it with the real position. The speed is what `Player.getDestroySpeed` worked out (tool, Efficiency,
Haste, Mining Fatigue, water, air) and goes into `speed / hardness / 30` (`/ 100` when the tool does not harvest the block). The position is
never null from this poster; the parameter is `@Nullable` for posters that do not know it.

*A listener may* change the speed or say that the player cannot dig the block by returning zero or less: the progress is then zero
(the block never breaks, the crack does not grow; it is the same on both sides).

*Not posted* for a block that cannot be dug (hardness -1), for the instant breaking of creative mode, or for a block class that
overrides `getDestroyProgress` without calling `super` (none in vanilla; Thaumcraft's overrides call it).

*Differences*: Forge's event returned `-1` on cancel, which gave a **negative** progress; here "cannot dig" is a progress of exactly
zero. The 1.7.10 event also fired for the wear of the tool (`damageItem(1)` every tick it fired); that part is the Enchantments group's
(`docs/deviations/Enchantments.md` section on the wear).

*NeoForge*: `PlayerEvent.BreakSpeed` is posted by `EventHooks.getBreakSpeed(player, state, speed, pos)` from `Player.getDigSpeed(state, pos)`,
which `BlockBehaviour.getDestroyProgress` calls with the position: a handler maps `getNewSpeed()`/`getPosition()`
(`Optional<BlockPos>`) to `TinkererEvents.postBreakSpeed(...)` and `setNewSpeed(result)`, cancelling at zero. It is posted on both sides there
as well.

*Fabric*: `BlockBehaviourBreakSpeedMixin`, `@WrapOperation` on the `Player.getDestroySpeed(BlockState)` call inside `getDestroyProgress`, with
the position from the method's own arguments.

## 5. `LIVING_JUMP`

```java
void onLivingJump(LivingEntity entity)
```

*Posted* on both sides at the end of `LivingEntity.jumpFromGround`, after the vertical motion (and the sprint push) were applied and
`hasImpulse` was set: the entity's delta movement holds the jump and the listener may change it (`entity.setDeltaMovement`). Players
jump with the key (`aiStep` → `jumpFromGround`), mobs with their jump control. Not posted when the jump power is zero. The server player and
the client's copy each run it; a boost has to be the same on both. A mounted horse's jump, a slime's hop and swimming up do not use
the method and do not post. It cannot be cancelled.

*NeoForge*: `LivingEvent.LivingJumpEvent` (`CommonHooks.onLivingJump`, same position) → `TinkererEvents.postLivingJump(entity)`.
*Fabric*: `LivingEntityMotionMixin`, `@Inject` at `TAIL` (the early `return` for no jump power is not matched by `TAIL`).

## 6. `LIVING_FALL`

```java
EventResult onLivingFall(LivingEntity entity, FallData fall)   // fall.getDistance()/setDistance, getDamageMultiplier()/setDamageMultiplier
```

*Posted* on **both sides** at the head of `LivingEntity.causeFallDamage`, where vanilla works the damage out from the distance and the
multiplier of the block landed on. 1.7.10's `LivingFallEvent` was posted from `EntityLivingBase.fall` on both sides as well, and
NeoForge's has no side check, so this is not a server-only event: the local player plays the sound of a hard fall from its own copy
of the landing, so a listener that takes the damage away must run on the client too. **A listener with an effect on the world
(Shockwave's blast) must act only when `!entity.level().isClientSide`.** Posted for every living entity that lands with a fall
distance, and for the passengers of a landing vehicle (with the distance the vehicle's event left).

*A listener may* change the distance and the damage multiplier, or cancel with an interrupting `EventResult`
(`EventResult.interruptFalse()` by convention): `causeFallDamage` returns `false` at once, with no damage, no sound and no fall for
the passengers. The chain stops at the first interrupt.

*NeoForge*: `LivingFallEvent` (`CommonHooks.onLivingFall`, same method) → `TinkererEvents.postLivingFall(entity, new FallData(event.getDistance(),
event.getDamageMultiplier()))`, then `event.setDistance`/`setDamageMultiplier` or `setCanceled(true)`.
*Fabric*: `LivingEntityMotionMixin`, `@Inject(cancellable)` at `HEAD` with two `@Local(argsOnly)` `LocalFloatRef`s.

## 7. `USE_ITEM_TICK`

```java
int onUseItemTick(LivingEntity entity, ItemStack stack, int remaining)   // the new remaining ticks; the poster raises anything below 1 to 1
```

*Posted* on both sides at the start of every tick of `LivingEntity.updateUsingItem`, for an entity that holds the use of an item (a
drawn bow, food being eaten, a raised shield). The value is the entity's own `useItemRemaining`, so everything that reads it afterwards
agrees with the listener: the `onUseTick` of the item that tick, the decrement and the completion test, `getTicksUsingItem` (which the
bow's pose and the HUD follow), and `releaseUsing(…, timeLeft)`, where `BowItem` works the arrow's power out from
`useDuration - timeLeft`. Quick Draw takes extra ticks off each tick (`remaining - speedup`), the 1.7.10 `itemInUseCount` rule; the
`Math.max(2, …)` floor of the original is the listener's to apply. A result below 1 is raised to 1 by the poster, so the use ends on
this tick as vanilla's does at zero, and cannot go negative. It cannot be cancelled. The client runs it for its own copy, so Quick
Draw must compute the same on both sides (it only reads the stack's enchantments).

*NeoForge*: `LivingEntityUseItemEvent.Tick` (`EventHooks.onItemUseTick`, same place) → `TinkererEvents.postUseItemTick(entity, stack, event.getDuration())`
→ `event.setDuration(result)`. (NeoForge applies the duration to its own variable of the method; check that it ends in `useItemRemaining`
before relying on it, the 1.21.1 patch does.)
*Fabric*: `LivingEntityUseItemMixin`, `@Inject` at `HEAD` with a `@Shadow` of `useItemRemaining`.

## 8. `LIVING_DROPS`

```java
EventResult onLivingDrops(LivingEntity victim, DamageSource source, List<ItemEntity> drops, int lootingLevel, boolean recentlyHit)
```

*Posted* on the server when a living entity has died and its death loot is worked out (`LivingEntity.dropAllDeathLoot`, which
`die`, `ServerPlayer.die` and the fox call): **after** the loot table, the custom loot and the equipment (and for a player the
inventory) have produced their item entities, **before** any is in the world. `drops` is a live mutable list: remove, replace, add
(`new ItemEntity(level, x, y, z, stack)`), clear. What is left is added to the level in order. `lootingLevel` is the level of Looting
on the weapon of `source.getEntity()` (0 without a living attacker); `recentlyHit` is `lastHurtByPlayerTime > 0` (the loot tables' "killed by
player" condition). An interrupting result cancels the drops (none of the list is spawned). The chain stops at the first interrupt.

*What is captured*: the item entities the **victim itself** spawns during `dropAllDeathLoot` through `Entity.spawnAtLocation(ItemStack, float)`
(all other `spawnAtLocation` overloads end there: loot table, equipment) and a dying player's inventory (`ServerPlayer.drop`). Experience orbs
are not item entities and are not in the list (as in 1.7.10: `event.drops` held items); an item that some other mod spawns somewhere else
with `addFreshEntity`, or an entity that another entity spawns, is not captured. A drop by hand (the drop key) is not affected: no capture is open.
When nobody listens the original runs untouched (`TinkererEvents.hasLivingDropsListeners()`).

*NeoForge*: `LivingDropsEvent` (`CommonHooks.onLivingDrops`; NeoForge does the capture itself with `Entity.captureDrops`, and passes a
`Collection`, a list in practice) → `TinkererEvents.postLivingDrops(entity, event.getSource(), new ArrayList<>(event.getDrops()), TinkererEvents.lootingLevel(level, source),
event.isRecentlyHit())`, then replace the contents of `event.getDrops()` and cancel on an interrupt.

*Fabric*: `LivingEntityDropsMixin` (`@WrapMethod` around `dropAllDeathLoot`), `EntityDropCaptureMixin` and `ServerPlayerDropCaptureMixin`
(`@WrapOperation` on their `Level.addFreshEntity` call → `DropCapture.offer`), `DropCapture` (a per-thread, per-entity, nesting capture).

## 9. `FloatingTicks`, `ItemRenderers`, and the NeoForge plan

**`FloatingTicks`** (`common/.../hooks/FloatingTicks.java`): `get(player)`, `set(player, ticks)`, `reset(player)`, `isAvailable(player)`
and `FloatingTicks.RESET`, a `Consumer<ServerPlayer>` of the type `FociHooks.setFloatingTickReset` takes. It reads and writes
`ServerGamePacketListenerImpl.aboveGroundTickCount` through the interface `FloatingTicks.Access` that the Fabric mixin
`ServerGamePacketListenerFloatingMixin` makes the connection class implement (no reflection). A player without a connection gives `-1` /
`false`, and `reset` does nothing. The Foci group's reflection stop-gap keeps working until it calls
`FociHooks.setFloatingTickReset(FloatingTicks.RESET)`; this was **not** done here, because it changes the group.
NeoForge: the same interface on the same class with a mixin, or an access transformer for the field and a tiny `Access` implementation
that cannot be done by transformer; the mixin is the simpler choice and works on both loaders.

**`ItemRenderers`** (`client/hooks/ItemRenderers.java`): `register(Item, Renderer)` with
`Renderer.render(ItemStack, ItemDisplayContext, PoseStack, MultiBufferSource, int light, int overlay)`; `RenderMobDisplay::render`
fits. Registrations wait for the backend and are handed to it by `setBackend`; a second renderer for the same item is an
`IllegalStateException`. Fabric: `FabricItemRenderers.install()` (called first thing by `ThaumicTinkererFabricClient`) registers each with
`BuiltinItemRendererRegistry.INSTANCE.register(item, DynamicItemRenderer)` (`fabric-rendering-v1` 5.2.1 in the Gradle cache). The Machines group
still registers its own through `MachinesFabricClient`; when it moves to `ItemRenderers.register(TTRegistry.item("mob_display"), RenderMobDisplay::render)`
in `MachinesClient.register()`, `MachinesFabricClient` goes (both registering the same item is an error).
NeoForge (to write): the backend collects the items and renderers, and a `RegisterClientExtensionsEvent` handler on the mod bus does
`event.registerItem(new IClientItemExtensions() { getCustomRenderer() → a BlockEntityWithoutLevelRenderer whose renderByItem(stack, ctx, pose,
buffers, light, overlay) calls the Renderer }, item)` for each entry.

**NeoForge plan** (deferred, `AGENTS.md`: Fabric first): a `thaumic.tinkerer.neoforge` class subscribed to `NeoForge.EVENT_BUS` that maps the
NeoForge events of the table in section 1 onto the `TinkererEvents.post…` methods as sketched in sections 3 to 8 (all the rules live in
`TinkererEvents`, so only the field mapping is loader code), the `FloatingTicks` mixin, and the `ItemRenderers` backend. The
`TinkererApiOnlyTest` already lets the neoforge tree import `net.neoforged.` and `org.objectweb.`.

## 10. Wiring the groups (who does what; none of this is done)

* **Enchantments**: `LIVING_DAMAGE` replaces `AttackBoost` (sixteen lines, as `docs/api-requests/Enchantments.md` section 1), also for
  the fake player and for mobs, arrows and thorns; `BREAK_SPEED` replaces `BreakSpeedControl` (and its once-per-tick attribute modifier, and the
  `updateFor` call the Tablet needed: `docs/api-requests/Enchantments.md` section 5 patches 1 and 2 stop being needed; the "required = false" wrappers
  of those two checks can become required); `LIVING_JUMP` for Ascent Boost; `LIVING_FALL` for Shockwave (server only!) and Slow Fall;
  `USE_ITEM_TICK` for Quick Draw (the arrow-speed workaround through `EntityEvent.ADD` goes).
* **KamiWorld**: `LIVING_DAMAGE` with `SoulHeartHandler.modifyIncomingDamage` (for `ServerPlayer` victims), then `SoulHeartHandler.exactDamageHook = true`;
  `LIVING_DROPS` for `DimensionalShardDropHandler`.
* **KamiGear**: `LIVING_JUMP` and `LIVING_FALL` for the Gem Boots. (The armour absorption K1 needs a hook of its own, section 3.)
* **Machines**: `LIVING_DROPS` for the Cursed Spirit's Blade (its note-and-refuse workaround goes); `ItemRenderers.register` for the Mob Display.
* **Foci**: `FociHooks.setFloatingTickReset(FloatingTicks.RESET)` from the group's start-up.
* **TileBlocks**: nothing new: the Animation Tablet's fake player already calls `state.getDestroyProgress` and `attackEntity`, which are the posting paths.

## 11. Fabric build wiring

* The mixins are compiled with the add-on's source set `thaumicTinkerer`. Architectury Loom 1.11 puts every source set that exists when it initialises on the
  Mixin annotation processor, each with its own refmap `<sourceSet>-<archivesName>-<project>-refmap.json` (here
  `fabric/build/classes/java/thaumicTinkerer/thaumicTinkerer-Thaumcraft-fabric-fabric-refmap.json`), and the processor also knows the MixinExtras
  annotations. **Do not call `loom.mixin.add(sourceSet, name)` for the add-on**: the first explicit `add` switches that default off for every other source
  set, Thaumcraft's own included. `fabric.gradle` checks after `compileThaumicTinkererJava` that the refmap exists and names every mixin that has an
  injector (`GradleException` otherwise).
* The release path (`remapThaumicTinkererJar`) adds `"refmap"` to `thaumictinkerer.mixins.json` in the jar (Loom's `MixinRefmapService` does it for the
  configs listed in `fabric.mod.json`), and the development run reads the mixins by their Mojang names without a refmap, like the Thaumcraft mod's own.
* `@Shadow` fields (`useItemRemaining`, `lastHurtByPlayerTime`, `aboveGroundTickCount`) have no refmap entry, as in the mod's own mixins; Loom's remap of the jar renames them in the
  mixin classes.
* **Checked on a built jar** (`./gradlew :fabric:remapThaumicTinkererJar`, `fabric/build/libs/ThaumicTinkerer-fabric-<version>.jar`): it contains
  `thaumicTinkerer-Thaumcraft-fabric-fabric-refmap.json`, its `thaumictinkerer.mixins.json` carries `"refmap": "thaumicTinkerer-Thaumcraft-fabric-fabric-refmap.json"`, and in the
  remapped mixin classes the shadowed field is `field_6222` (intermediary). That jar was inspected, not started in a production environment.
* MixinExtras 0.4.1 comes with Fabric Loader 0.16.14 (the add-on does not shade it). All mixins use `injectors.defaultRequire: 1` and `required: true`: a
  Minecraft change that moves a target stops the game at start-up with the mixin's name instead of silently losing a hook.
* The `TinkererApiOnlyTest` lets the Fabric tree import `org.spongepowered.` and `com.llamalad7.` besides `net.fabricmc.`; `thaumcraft.common.` and
  `thaumcraft.client.` stay forbidden everywhere.

## 12. Tests

JUnit (`./gradlew :fabric:tinkererTest`): `TinkererEventsTest` (chain order, zero/negative/NaN cancel, a lone listener, no listener, the clamp of the use
tick, drops listeners and `hasLivingDropsListeners`, `ItemRenderers` queue and duplicate), `HooksResourcesTest` (the mixin config lists exactly the
mixin classes, `fabric.mod.json` lists the config, no class names a mixin class, handler prefix, common code does not import loader code).

GameTest (`HooksGameTests`, run with `-PtinkererGameTestFilter='hooksgametests.*'`), each registering its listeners inside the check, answering only for its own
entities and taking them away in a `finally`:

| Check | What it proves |
|---|---|
| `livingDamageIsPostedBeforeArmourAndChainsInOrder` | a zombie's blow on a pig with 20 armour points: the listener sees 10 (not the armoured 4), the armour then works on the halved/chained amount, order (10·2)−3 = 17 vs (10−3)·2 = 14, source carries the zombie |
| `livingDamageCancelIsLikeInvulnerableAndFramesFollowTheListener` | a cancelled hit leaves health, frames, hurt time, knockback and last damage source exactly as an invulnerable pig does; the frames compare the listener's result |
| `livingDamageSeesAFakePlayersBlowAndAZombiesBlowOnAPlayer` | `TinkererFakePlayer.attackEntity` (diamond sword) and a zombie against a mock server player (difficulty-scaled amount) |
| `livingDamageSeesAnArrow` | a real arrow flight and hit, shooter in the source |
| `breakSpeedIsPostedForAServerPlayerAndAFakePlayer` | `state.getDestroyProgress` for a mock server player and a never-ticked fake player: factor, position, state, order of two listeners, cannot dig = 0, and a real `ServerPlayerGameMode` dig start that breaks the dirt at once with a ×1000 listener |
| `livingJumpIsPostedAfterTheJumpMotionIsApplied` | a pig's `jumpFromGround` (listener sees 0.42 and doubles it) and a player's jump key through `doTick` |
| `livingFallIsPostedOnLandingAndCanHalveOrCancel` | three pigs really fall 10.5 blocks (the harness's barrier ceiling at relative y = 9 is opened for the columns): the listener sees the distance vanilla counts (about 9.7: the movement of the landing tick is not in it), the untouched pig takes the damage of that distance, the halved one the damage of half of it, the cancelled one nothing (no frames); on the server; a player's `causeFallDamage` cancelled |
| `useItemTickExtendsABowDraw` | two mock players draw a bow for 12 ticks: the draw clock reads 3× with the listener, and the released arrow flies at 3.0 against under 2.0 |
| `livingDropsAreEditedBeforeTheyEnterTheWorld` | a pig killed by the fake player with Looting III: sees pork, level 3, recently hit, nothing in the world yet; add a diamond / remove the pork / clear / cancel; experience orbs unaffected; plain again afterwards |
| `livingDropsCoverAPlayersInventoryButNotADropByHand` | `ServerPlayer.die` offers the inventory and a cleared list drops nothing; a drop by hand is immediate |
| `floatingTicksReadAndResetTheRealCounter` | the accessor against an independent reflective read of the private field on a real connection, the connection's own `tick()` resetting the same counter, no connection, the fake player's connection |

**Not run, because a server cannot**: that the same events are posted on the client (the dig prediction, the jump, the bow draw and the fall of the local player).
The posting code is the vanilla classes' own, shared by both sides; `HOOKS.md` relies on that. A client run with a listener would show it.
