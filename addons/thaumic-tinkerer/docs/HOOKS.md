# Loader hooks of the Thaumic Tinkerer add-on

Architectury has no events for several things that the 1.7.10 mod did through Forge events. The feature groups first worked
around each gap and wrote the loss down (`docs/api-requests/*.md`, `docs/deviations/*.md`). This is the loader layer that closes
the gaps: **real events**, posted from the vanilla code paths themselves, and two small registries. The five groups that needed
them (Enchantments, KamiWorld, KamiGear, Machines, Foci) **are rewired** to them and their workarounds are deleted; section 10 says
per group which workaround went, which event replaced it and which check pins it. The posters exist for **Fabric** (Mixin). **None of
the NeoForge posters is written** (section 9 lists what each of them must do): on NeoForge every effect that depends on an event would not happen.
Nothing of the add-on has been built or run on NeoForge, so that is a consequence read from the code, not an observation.

How the words are used: *implemented* means the code exists; a *check* is a JUnit test or a GameTest in the source that was written for the thing it
is named for, and a check counts as *run* only where a document or a results file says so (section 12, "What ran", says what that is for the hooks); *read*
means taken from the 1.21.1 or NeoForge bytecode, or from the pinned 1.7.10 source of Thaumic Tinkerer, by reading, not run; *recalled* means from memory
of Forge or Minecraft 1.7.10, whose source and jars are not on this machine; *unverified* is everything else, in particular every effect on a client (no
check runs one, section 12).

| What | Where |
|---|---|
| The events and the rules of every one | `common/.../thaumic/tinkerer/common/hooks/TinkererEvents.java` (loader neutral, Architectury `Event`s) |
| Access to the flying-kick counter | `common/.../common/hooks/FloatingTicks.java` |
| Built-in item renderer registry (client) | `common/.../thaumic/tinkerer/client/hooks/ItemRenderers.java` |
| Fabric posters (Mixin) | `fabric/src/main/java/thaumic/tinkerer/fabric/mixin/*`, config `fabric/src/main/resources/thaumictinkerer.mixins.json`, listed under `"mixins"` of `fabric/src/main/resources/fabric.mod.json` |
| Fabric helper of the drop capture, Fabric item renderer backend | `fabric/.../fabric/hooks/DropCapture.java`, `fabric/.../fabric/client/FabricItemRenderers.java` |
| NeoForge posters | **not written**; there is no NeoForge build file of the add-on either (section 9) |
| Runtime tests | `common/src/gameTest/.../testing/HooksRuntimeChecks.java`, Fabric wrappers `fabric/src/gameTest/.../testing/HooksGameTests.java`; JUnit `common/src/test/.../hooks/` |

## 1. Overview

| Event | 1.7.10 original | Sides | Fabric posting point (Mojang names) | NeoForge counterpart | Used by |
|---|---|---|---|---|---|
| `LIVING_DAMAGE` | `LivingHurtEvent` (`ammount`) | server | `LivingEntity.hurt`, just before the first `isSleeping()` call, i.e. right after the four early `return false` checks | `LivingIncomingDamageEvent` | Enchantments (Pounce, Final Strike, Valiance, Focused and Dispersed Strikes, Vampirism; the former `AttackBoost` is deleted), KamiWorld (Soul Hearts: `SoulHeartHandler.onLivingDamage`) |
| `LIVING_ARMOR_ABSORB` (section 14) | `ISpecialArmor` (`ArmorProperties`, asked inside Forge's `ArmorProperties.ApplyArmor`) | server | `LivingEntity.getDamageAfterArmorAbsorb`, wrapped (`LivingEntityArmorMixin`, priority 900) | `LivingIncomingDamageEvent#addReductionModifier(DamageContainer.Reduction.ARMOR, ...)` | KamiGear (Ichorcloth: `IchorAbsorption`) |
| `BREAK_SPEED` | `PlayerEvent.BreakSpeed` (`newSpeed`, cancel) | **both** | `BlockBehaviour.getDestroyProgress`, wrapping the call of `Player.getDestroySpeed(BlockState)` | `PlayerEvent.BreakSpeed` (`setNewSpeed`, cancel; its position is `getPosition()`) | Enchantments (Shatter, Tunnel, Disintegrate, Flaming Touch; the former `BreakSpeedControl` is deleted), TileBlocks (the Animation Tablet's fake player gets the event with no change in that group) |
| `LIVING_JUMP` | `LivingJumpEvent` | both | `LivingEntity.jumpFromGround`, `TAIL` | `LivingEvent.LivingJumpEvent` | Enchantments (Ascent Boost), KamiGear (Gem Boots' jump) |
| `LIVING_FALL` | `LivingFallEvent` (`distance`, cancel) | both | `LivingEntity.causeFallDamage`, `HEAD` | `LivingFallEvent` (`setDistance`, `setDamageMultiplier`, cancel) | Enchantments (Shockwave, Slow Fall), KamiGear (Gem Boots set the distance to 0) |
| `USE_ITEM_TICK` | `EntityPlayer.itemInUseCount` (written every tick by the Quick Draw handler) | both | `LivingEntity.updateUsingItem`, `HEAD` | `LivingEntityUseItemEvent.Tick` (`setDuration`) | Enchantments (Quick Draw; the former arrow-speed workaround is deleted) |
| `LIVING_DROPS` | `LivingDropsEvent` (`drops`, cancel) | server | `LivingEntity.dropAllDeathLoot` wrapped (and `Fox.dropAllDeathLoot`); capture in `Entity.spawnAtLocation(ItemStack, float)` and `ServerPlayer.drop(ItemStack, boolean, boolean)` | `LivingDropsEvent` (`getDrops()`, cancel) | Machines (Cursed Spirit's Blade), KamiWorld (`DimensionalShardDropHandler`) |
| `FloatingTicks` | `NetHandlerPlayServer.floatingTickCount` | server | accessor mixin on `ServerGamePacketListenerImpl.aboveGroundTickCount` | an access transformer or the same mixin | Foci (Uprising) |
| `ItemRenderers` | `MinecraftForgeClient.registerItemRenderer` | client | Fabric API `BuiltinItemRendererRegistry` | `RegisterClientExtensionsEvent` + `IClientItemExtensions` | Machines (Mob Display) |

`LIVING_ARMOR_ABSORB` was added after sections 3 to 13 had been numbered, and the group documents and the code comments quote those numbers, so its
section is appended as section 14 (it belongs with sections 3 to 8). Section 15 lists improvements that nobody has written.

## 2. Rules that hold for every event

* A listener is registered with `TinkererEvents.X.register(listener)` and removed with `unregister(listener)` (Architectury's `Event`).
  Registration is not thread safe: do it at mod start-up (the groups' `register()` or `ready()`), or on the server thread while nothing is posting.
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
* **Order across the groups.** The order of registration is made by the phases of `ThaumicTinkerer`: `init()` runs the `register()` of every
  group in the order of `ContentGroups.ALL` (SimpleBlocks, TileBlocks, Machines, BasicItems, Foci, Enchantments, KamiGear, KamiWorld), then
  `onThaumcraftReady()` runs every `ready()` (`ContentGroup` javadoc, `ThaumicTinkerer.init` and `onThaumcraftReady`). So a listener registered
  in the `register()` of a group comes before one registered in the `register()` of a later group, and every listener registered in a `ready()`
  comes after every one registered in a `register()`. There are no priorities (Forge's `EventPriority.LOW` of Shockwave has no counterpart), and
  KamiGear and KamiWorld register nothing while `ConfigHandler.enableKami` is off. What the groups rely on today; each line is a choice of
  where a group registers and of the order of `ContentGroups.ALL`, which the event layer does not guarantee. The 1.7.10 column was **read** in
  the pinned source (`TTCommonProxy.init`, the constructors of `ItemBloodSword` and of `ItemIchorclothArmorAdv`, the `@SubscribeEvent` priorities
  of `ModEnchantmentHandler`); the Forge rule that equal priorities run in registration order is recalled, not read.

  | Event | Order of the listeners here | 1.7.10 | What pins it |
  |---|---|---|---|
  | `LIVING_DAMAGE` | Enchantments (`Enchantments.register()`: `ModEnchantmentHandler.onEntityDamaged`), then KamiWorld's Soul Hearts (`KamiWorld.register()`: `SoulHeartHandler.onLivingDamage`) | the same: `init` calls `ModEnchantments.initEnchantments()` first and registers `SoulHeartHandler` after it | stated, **not pinned**: no check puts both listeners on one hit (searched for in the Enchantments and KamiWorld checks, not found). What follows from it: section 3 ("Other listeners and the Soul Hearts' frames") and `deviations/KamiWorld.md` section 5 |
  | `LIVING_DROPS` | Machines (`Machines.register()`: `ItemBloodSword.onLivingDrops`, the Cursed Spirit's Blade), then KamiWorld (`KamiWorld.register()`: `DimensionalShardDropHandler.onLivingDrops`, the shards) | the same: the blade registers its handler in its constructor (`preInit`), the shard handler is registered in `init` | GameTest `aReapedEndermanAndZombifiedPiglinStillRollTheirDimensionalShard` (`MachinesRuntimeChecks`). It depends on `ContentGroups.ALL` having Machines before KamiWorld **and** on both registering in `register()`, not in `ready()`: the blade clears the list, so a blade registered after the shard handler would delete the shard. `deviations/Machines.md` section 8 records that moving the blade's registration into `ready()` made exactly this check fail |
  | `LIVING_JUMP` | the Gem Boots (`KamiGear.register()` → `KamiArmorHandler.register()`: `ItemGemBoots.onLivingJump`), then Ascent Boost (`Enchantments.ready()` → `ModEnchantments.initJump()` → `ModEnchantmentHandler.registerJump()`) | the same: the boots' handler in `preInit`, `ModEnchantmentHandler` in `init`, both at normal priority, so `motionY = (0.42 + 0.3) × factor`: **1.08** for Ascent Boost I and **1.80** for III | GameTest `gemBootsAndAscentBoostLeggingsComposeTheirJumps` (`KamiGearRuntimeChecks`), whose expectations are 1.08 and 1.80. The first wiring registered Ascent Boost in `register()`, before KamiGear, and gave 0.93 and 1.35; moving its registration into `ready()` fixed that |
  | `LIVING_FALL` | Slow Fall, then Shockwave (both `Enchantments.register()` → `ModEnchantments.initEnchantments()`), then the Gem Boots (`KamiGear.register()`: `ItemGemBoots.onLivingFall`) | the boots' handler (normal priority) first; Shockwave at `EventPriority.LOW` after every other handler of the event; Slow Fall was not a fall listener | Slow Fall before Shockwave: `shockwaveHurtsTheCreaturesAroundALanding` (both on one pair of boots). Shockwave's `LOW` is **not reproduced against other groups** (the boots' listener sets the distance to 0 after Shockwave has looked at it); there is no live effect, because the boot tags of Slow Fall and Shockwave do not list the Gem Boots (`deviations/Enchantments.md` section 1), so the Enchanter and the anvil cannot put them together; a stack made by a command or a data pack would show it. Not pinned |

## 3. `LIVING_DAMAGE`

```java
float onLivingDamage(LivingEntity victim, DamageSource source, float amount)   // the new amount; <= 0 or NaN cancels
```

*Posted* on the server from `LivingEntity.hurt`, after the checks that make an entity ignore a hit and **before** anything reduces
the amount: before a shield blocks, before the extra damage of freezing and the helmet's share, before the invulnerability frames
compare the hit with the last one, before armour, Protection, Resistance and Absorption. For every living entity, whatever dealt
the damage (blow, arrow, thorns, fall, fire). For a player the amount is already scaled by the difficulty (`Player.hurt` does it
before `super.hurt`). A hit of zero damage (a snowball) is not posted.

*Not posted for a hit the invulnerability frames refuse.* While the frames of an earlier hit run (`invulnerableTime > 10`) vanilla
throws away a hit that is not stronger than the last one (`amount <= lastHurt`) unless the source is in the damage-type tag
`bypasses_cooldown` (empty in vanilla 1.21.1). The Fabric poster makes the same test first (`TinkererEvents.framesRefuse(victim, source,
amount, lastHurt)`, the one rule for both loaders) and posts only a hit that the game goes on to deal. A listener with a side effect
(Soul Hearts spending points) is therefore not charged for the lava, fire or cactus hit that vanilla would have refused: the 1.7.10 event
came after that test as well (recalled, not read). The test uses the amount as the event sees it, so the later steps of `hurt` can still decide differently
in two corners: a hit that a shield blocks or a helmet reduces (`* 0.75`, falling blocks) is posted with its full amount and may be
refused afterwards; and a freezing hit on a strider, blaze or magma cube (the game multiplies it by five before it compares) is always
posted. The GameTest `livingDamageIsNotPostedForHitsTheInvulnerabilityFramesRefuse` pins this (the bypass branch only if a data pack
puts a damage type in the tag; none is in vanilla). **A refused hit posts nothing, so a listener cannot count it**: one that must act once
per frames period (the Soul Hearts) cannot lean on the game's frames, which hide the refused hits from it, and has to keep frames of its own
(below).

*A listener may* change the amount (the next listener sees it) or cancel by returning zero. A cancel makes `hurt` return `false` at
once, **as for an invulnerable entity**: no damage, no hurt animation or sound, no knockback, no invulnerability frames, no last
damage source. The listeners after a cancel are not called.

*A cancel starts no frames, so the same hit comes again as soon as its source repeats it.* Lava, fire ticks and cactus call `hurt` every tick
and vanilla lets one in about ten through because the first hit's frames refuse the rest; after a cancelled hit there are no frames, and the
next tick's hit is posted again. A listener that cancels with a side effect (Soul Hearts taking a covered hit off the pool and returning
zero) is called for every repeat and would drain the pool about ten times faster than 1.7.10, where `hurtResistantTime` was set before the
event (recalled, not read). Returning the uncovered rest and letting the game's frames count does not work alone: a fully covered hit has no rest (it must be
cancelled), and even a partly covered one comes back on the next tick, because the game's frames compare it with the *rest*. **The Soul Hearts
therefore keep the frames of the original themselves**, per player (`SoulHeartHandler.Frames`, in memory: the game tick of the hit that began
them, the strongest raw amount so far, and the amount last handed to the game), charge the pool by the rule of the original inside and outside
them, and hand the game `handed + rest` **only while the game's own frames run** (`invulnerableTime > 10`, the source not in `bypasses_cooldown`)
and `rest` alone when they do not, so that the game, which turns the amount it is handed into damage by its own rule (the amount minus its
`lastHurt` inside its frames, the whole amount outside them), deals exactly the rest whichever frames end first. The group's full account, the
numbers and the corners it cannot close are in `deviations/KamiWorld.md` section 5. Checks: the GameTests `soulHeartsAreChargedOncePerTenTicksByLava`
(real lava), `soulHeartsKeepTheFramesOfTheOriginal` (every case by tick) and `soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater`
(the game's frames begin after the pool's and end before or after them); the header and section 10 of `deviations/KamiWorld.md` record that the
KamiWorld GameTests passed (20 of 20 after its review round, run together with the `hooksgametests.*` of that day).

*A sweep attack posts once per swept entity.* A sword's sweeping attack (Minecraft 1.9 and later; 1.7.10 had none) hurts the target and then
calls `LivingEntity.hurt` with the **same `playerAttack` source** once for every other living entity next to the target, so the event is posted for
each of them with that source and nothing that tells a swept entity from the target. A listener with an effect per hit (Vampirism's heal, Final
Strike's dice, the strike counter) acts once per entity, and the strike counter follows the last entity of the swing. Check: GameTest
`aSweepOfASwordIsAHitForEachEntityItReaches` (the bytecode of `Player.attack` was read; `deviations/Enchantments.md` section 2, which records no
run of it; the results file of section 12 lists the case without a failure).

*The frames compare what the listener returned.* Vanilla stores the listener's result as `lastHurt`, so for a listener that multiplies by a
constant factor `f` the frames compare the **boosted** amounts. Take a first blow `a1` and a second `a2` inside the frames. Where `a2` is above
both bars (`a2 > max(a1, f·a1)`) both games deal `f·(a2 - a1)`, and where it is below both both refuse it. **In between there are two bands
where 1.7.10 and this event differ**: (1) for `f > 1` and `a1 < a2 <= f·a1` 1.7.10 dealt `f·(a2 - a1)` (it compared the raw amounts and gave
its event `a2 - a1`) and here the blow is refused and not posted (Pounce IV, `f = 2`: a blow of 10 deals 20; a blow of 15 inside the frames
dealt 10 more in 1.7.10 and deals nothing here; a blow of 25 deals 30 in both); (2) for `f < 1` and `f·a1 < a2 <= a1` 1.7.10 refused the blow
before its event, and here it is posted (vanilla compares it with the boosted `f·a1`), the listener runs and its side effects happen (a
counter step, a heal), and vanilla then refuses it because `f·a2` is not above `f·a1` (Dispersed Strikes I at a counter of 5 or more). With a
factor that is not constant (Final Strike rolls a new one per hit, Dispersed Strikes moves with its counter) there is no such band formula. A check of both bands on
the 1.21.1 side exists, the GameTest `theFramesCompareTheBoostedAmountsNotTheRawOnes` (Pounce IV; Dispersed Strikes I at a counter of 6); the results file of
section 12 lists it without a failure.
**Unverified**: the 1.7.10 side of both bands. The Minecraft 1.7.10 source is not on this machine; it is taken from the first bullet of the
differences below and recalled from `EntityLivingBase.attackEntityFrom` (`deviations/Enchantments.md` sections 2 and 12).

*Other listeners and the Soul Hearts' frames.* The chain runs in the order of registration across the groups: the Enchantments' listener is
earlier than the Soul Hearts', as in 1.7.10 (section 2). Because the poster filters only the *game's* refusals, a hit that the **Soul Hearts'
frames** refuse (a repeat inside ten ticks of a covered hit, which began no game frames, or a weaker one after a partly covered hit) is still
posted, so the Enchantments' listener has already run for it (Vampirism heals the attacker, the strike counter advances) when the pool refuses
the hit; in 1.7.10 that hit was refused before any event. A hook that asks the pool's frames before any listener runs would close it (section 15).

*Differences from Forge's `LivingHurtEvent` of 1.7.10* (**recalled, not read**: neither the Minecraft 1.7.10 source nor a Forge 1.7.10 jar is on this machine,
so what the bullets say about 1.7.10 rests on memory of `EntityLivingBase.attackEntityFrom` and `damageEntity` and on the Thaumic Tinkerer source that listens to
the event, as `deviations/Enchantments.md` section 12 says; the 1.21.1 side of each bullet was read in the bytecode)

* 1.7.10 posted it in `damageEntity`, after the sword block of a player victim (there was no shield item, but a player who held a sword up took
  `(1 + amount) / 2` before the event) and after the invulnerability frames had subtracted the last
  hit (`damageEntity(source, amount - lastDamage)`); here the event comes first and the frames compare the **listener's** result with
  the last hit. The test pins it down: a listener that halves turns 10 and then 8 into 5 and 4, and the second hit is refused, where
  vanilla would let 3 of it through. This is NeoForge's order as well (`LivingIncomingDamageEvent`).
* A cancel in 1.7.10 only skipped `damageEntity`: knockback and the hurt animation still happened. Here a cancel is a refusal of the
  whole hit.
* 1.7.10 passed the amount after that sword block; 1.21.1 has no sword block, and a hit that a shield will block is posted with its full amount.
* 1.7.10 compared the frames with the *raw* amount and posted the event with the difference (`amount - lastDamage`); here the event gets the whole
  amount of a hit that goes on to be dealt, and the frames compare the *listener's result* with the next hit.
* The armour of KamiGear (`ISpecialArmor`, `docs/api-requests/KamiGear.md` section 1) is **not** served by this event: it sat
  *after* the event in 1.7.10 (inside the armour calculation). It has an event of its own, `LIVING_ARMOR_ABSORB`, posted from
  `LivingEntity.getDamageAfterArmorAbsorb` (section 14).

*NeoForge*: a handler of `LivingIncomingDamageEvent` calls `TinkererEvents.postLivingDamage(entity, event.getSource(), event.getAmount())`,
`event.setAmount(result)` and `event.setCanceled(true)` when the result is zero (the event is posted by `CommonHooks.onEntityIncomingDamage`
at the same place of `hurt`). NeoForge's event is posted *before* the frames test too and has no filter of its own, so the handler must first call
`TinkererEvents.framesRefuse(entity, event.getSource(), event.getAmount(), lastHurt)` and return without posting when it is true; `lastHurt` is a protected field of
`LivingEntity` there as well, so the NeoForge module needs an access transformer or an accessor mixin for it.

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
overrides `getDestroyProgress` without calling `super`. In vanilla 1.21.1 those are `BambooStalkBlock` and `BambooSaplingBlock`: they return
`1.0F` (an instant break) for anyone with a sword in the main hand without asking `Player.getDestroySpeed`, so a "cannot dig" listener (Shatter and
the like) cannot stop a sword cutting bamboo. Thaumcraft's overrides call `super`.

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
jump with the key on the client (`aiStep` → `jumpFromGround`), mobs with their jump control. **A real server player does not jump from the key:**
the server's `jumping` flag is set from input only while the player is a passenger, and the server learns of a jump from the movement packet:
`ServerGamePacketListenerImpl.handleMovePlayer` calls `player.jumpFromGround()` whenever the player was on the ground, the packet says it is
not and the move is upwards (read from the 1.21.1 bytecode, not run). So on the server the event fires for any upward take-off of that kind
(a launch from a piston or a wind charge as well as the key), as it did in 1.7.10, where the server also took the jump from the client's
packet. Not posted when the jump power is zero. The server player and the client's copy each run it; a boost has to be the same on both. A
mounted horse's jump, a slime's hop and swimming up do not use the method and do not post. It cannot be cancelled. (The GameTest drives the
`aiStep` path with `setJumping(true)`; both paths end in the same `jumpFromGround`, which is where the mixin sits.)

*NeoForge*: `LivingEvent.LivingJumpEvent` (`CommonHooks.onLivingJump`, same position) → `TinkererEvents.postLivingJump(entity)`.
*Fabric*: `LivingEntityMotionMixin`, `@Inject` at `TAIL` (the early `return` for no jump power is not matched by `TAIL`).

*Order against the other listener*: the Gem Boots' listener (KamiGear, registered in `register()`) runs before Ascent Boost's (Enchantments,
registered in `ready()`), as in 1.7.10, so the boots add their 0.3 and Ascent Boost multiplies the sum: `(0.42 + 0.3) × factor`, 1.08 for
Ascent Boost I and 1.80 for III (section 2 has the table and the check that pins it).

## 6. `LIVING_FALL`

```java
EventResult onLivingFall(LivingEntity entity, FallData fall)   // fall.getDistance()/setDistance, getDamageMultiplier()/setDamageMultiplier
```

*Posted* on **both sides** at the head of `LivingEntity.causeFallDamage`, where vanilla works the damage out from the distance and the
multiplier of the block landed on. 1.7.10's `LivingFallEvent` was posted from `EntityLivingBase.fall` on both sides as well, and
NeoForge's has no side check, so this is not a server-only event: the local player plays the sound of a hard fall from its own copy
of the landing, so a listener that takes the damage away must run on the client too. **A listener with an effect on the world
(Shockwave's blast) must act only when `!entity.level().isClientSide`.** Posted for every living entity that lands with a fall
distance and goes through `LivingEntity.causeFallDamage`, and for the passengers of a landing vehicle that does (with the distance the vehicle's
event left). **Not for horses, donkeys, mules, skeleton and zombie horses and llamas:** `AbstractHorse` and `Llama` override `causeFallDamage`, work
the damage out themselves (`calculateFallDamage`) and call `hurt` on themselves and `Entity.hurt` on their indirect passengers without ever calling
`super`, so neither the mount nor its rider is posted, and Slow Fall or Gem Boots on a rider do not shorten the mount's fall (the rider's damage still
goes through `LIVING_DAMAGE`). 1.7.10 was no better for the rider (the event named the horse); NeoForge's patched `AbstractHorse` and `Llama` have no
`onLivingFall` call either, so both loaders agree. A mixin on those two methods would close it on Fabric; it has not been written.

*A listener may* change the distance and the damage multiplier, or cancel with an interrupting `EventResult`
(`EventResult.interruptFalse()` by convention): `causeFallDamage` returns `false` at once, with no damage, no sound and no fall for
the passengers. The chain stops at the first interrupt.

*NeoForge*: `LivingFallEvent` (`CommonHooks.onLivingFall`, same method) → `TinkererEvents.postLivingFall(entity, new FallData(event.getDistance(),
event.getDamageMultiplier()))`, then `event.setDistance`/`setDamageMultiplier` or `setCanceled(true)`.
*Fabric*: `LivingEntityMotionMixin`, `@Inject(cancellable)` at `HEAD` with two `@Local(argsOnly)` `LocalFloatRef`s.

*Order against the other listeners*: Slow Fall, then Shockwave (Enchantments), then the Gem Boots (KamiGear). Shockwave's `LOW` priority of
1.7.10, which put it after the boots, is not reproduced against other groups; there is no live effect (section 2).

## 7. `USE_ITEM_TICK`

```java
int onUseItemTick(LivingEntity entity, ItemStack stack, int remaining)   // the new remaining ticks; the poster raises anything below 1 to 1
```

*Posted* on both sides at the start of every tick of `LivingEntity.updateUsingItem`, for an entity that holds the use of an item (a
drawn bow, food being eaten, a raised shield). The value is the entity's own `useItemRemaining`, so everything that reads it afterwards
agrees with the listener: the `onUseTick` of the item that tick, the decrement and the completion test, `getTicksUsingItem` (which the
bow's pose and the HUD follow), and `releaseUsing(…, timeLeft)`, where `BowItem` works the arrow's power out from
`useDuration - timeLeft`. Quick Draw takes extra ticks off each tick (`remaining - speedup`), the 1.7.10 `itemInUseCount` rule; the
`Math.max(2, …)` floor of the original is the listener's to apply. When the use was counting down (`remaining >= 1`) a result below 1 is raised
to 1 by the poster, so the use ends on this tick as vanilla's does at zero, and cannot go negative. **With no listener, or a listener that changes
nothing, the poster returns `remaining` untouched**: `Item.getUseDuration` is 0 for everything that is not food, and a use that starts at 0 counts
0, -1, -2 and never completes in vanilla (a hold-to-use item of another mod relies on that), so the clamp must not apply to it. It cannot be cancelled. The client runs it for its own copy, so Quick
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
`die` and `ServerPlayer.die` call; the fox calls it after dropping its mouth item): **after** the loot table, the custom loot and the equipment (and for a player the
inventory) have produced their item entities, **before** any is in the world. `drops` is a live mutable list: remove, replace, add
(`new ItemEntity(level, x, y, z, stack)`), clear. What is left is added to the level in order. `lootingLevel` is the level of Looting
on the weapon of `source.getEntity()` (0 without a living attacker); `recentlyHit` is `lastHurtByPlayerTime > 0` (the loot tables' "killed by
player" condition). An interrupting result cancels the drops (none of the list is spawned). The chain stops at the first interrupt.

*What is captured*: the item entities the **victim itself** spawns during `dropAllDeathLoot` through `Entity.spawnAtLocation(ItemStack, float)`
(all other `spawnAtLocation` overloads end there: loot table, equipment) and a dying player's inventory (`ServerPlayer.drop`). Experience orbs
are not item entities and are not in the list (as in 1.7.10: `event.drops` held items); an item that some other mod spawns somewhere else
with `addFreshEntity`, or an entity that another entity spawns, is not captured. A drop by hand (the drop key) is not affected: no capture is open.
When nobody listens the original runs untouched (`TinkererEvents.hasLivingDropsListeners()`).

*The fox*: `Fox.dropAllDeathLoot` drops the item in the fox's mouth with `spawnAtLocation` **before** it calls `Animal.dropAllDeathLoot`, so the wrapper
on `LivingEntity.dropAllDeathLoot` alone would start its capture too late and the item would enter the world at once, unseen. `FoxDropCaptureMixin`
wraps the fox's override, begins the capture of that fox around it, and the living-entity wrapper takes the open capture of the same victim over
(it ends it before it posts, so anything a listener spawns goes straight into the world). The mouth item is then in the list like any other drop, as
in NeoForge, whose patched `Fox` moves that drop into `dropEquipment()` inside the captured section. The GameTest has a control (a fox killed with
nobody listening drops the emerald) and the listener case. Any other vanilla override that spawns before `super` would need the same wrap; the only
one in 1.21.1 that was found by reading the `dropAllDeathLoot` overrides is the fox.

*NeoForge*: `LivingDropsEvent` (`CommonHooks.onLivingDrops`; NeoForge does the capture itself with `Entity.captureDrops`, and passes a
`Collection`, a list in practice) → `TinkererEvents.postLivingDrops(entity, event.getSource(), new ArrayList<>(event.getDrops()), TinkererEvents.lootingLevel(level, source),
event.isRecentlyHit())`, then replace the contents of `event.getDrops()` and cancel on an interrupt.

*Fabric*: `LivingEntityDropsMixin` (`@WrapMethod` around `dropAllDeathLoot`), `EntityDropCaptureMixin` and `ServerPlayerDropCaptureMixin`
(`@WrapOperation` on their `Level.addFreshEntity` call → `DropCapture.offer`), `FoxDropCaptureMixin` (begins the capture around `Fox.dropAllDeathLoot`,
see above), `DropCapture` (a per-thread, per-entity, nesting capture).

*Order*: the Cursed Spirit's Blade (Machines) before the dimensional shards (KamiWorld), both registered in `register()`; the blade clears the list,
so the order is what keeps the shard of an Enderman or a Zombified Piglin that the blade reaped (section 2 has the check that pins it).

## 9. `FloatingTicks`, `ItemRenderers`, and the NeoForge plan

**`FloatingTicks`** (`common/.../hooks/FloatingTicks.java`): `get(player)`, `set(player, ticks)`, `reset(player)`, `isAvailable(player)`
and `FloatingTicks.RESET`, a `Consumer<ServerPlayer>` of the type `FociHooks.setFloatingTickReset` takes. It reads and writes
`ServerGamePacketListenerImpl.aboveGroundTickCount` through the interface `FloatingTicks.Access` that the Fabric mixin
`ServerGamePacketListenerFloatingMixin` makes the connection class implement (no reflection). A player without a connection gives `-1` /
`false`, and `reset` does nothing. The Foci group uses it: `Foci.register()` calls `FociHooks.setFloatingTickReset(FloatingTicks.RESET)` and
the group's reflection stop-gap is deleted (`deviations/Foci.md` section 2). NeoForge (**not written**): the same interface on the same class
with a mixin, or an access transformer for the field and a tiny `Access` implementation that cannot be done by transformer; the mixin is the
simpler choice and works on both loaders. Without it `isAvailable` is false there and the reset does nothing.

**`ItemRenderers`** (`client/hooks/ItemRenderers.java`): `register(Item, Renderer)` with
`Renderer.render(ItemStack, ItemDisplayContext, PoseStack, MultiBufferSource, int light, int overlay)`; `RenderMobDisplay::render`
fits. Registrations wait for the backend and are handed to it by `setBackend`; a second renderer for the same item is an
`IllegalStateException`. Fabric: `FabricItemRenderers.install()` (called first thing by `ThaumicTinkererFabricClient`) registers each with
`BuiltinItemRendererRegistry.INSTANCE.register(item, DynamicItemRenderer)` (`fabric-rendering-v1` 5.2.1 in the Gradle cache). The Machines group
uses it: `MachinesClient.register()` calls `ItemRenderers.register(TTRegistry.item("mob_display"), RenderMobDisplay::render)` as its first statement,
and the group's own Fabric glue `MachinesFabricClient` is deleted (two registrations of one item are an error). A JUnit check exists,
`MachinesClientWiringTest` (the helper, the queue and the wiring in the source; no document records a run of it); the Fabric backend was run once on a client: the client harness
(`CLIENT_VERIFICATION.md`, 2026-10-30) drew a Mob Display for every mob in the item grid through it, English and Russian (its first picture showed the mobs too big and pitched 90 degrees;
fixed in `RenderMobDisplay`, see `deviations/Machines.md` section 7). Not seen: the item in a hand, on the ground, in an item frame, its break particles, a GPU.
NeoForge (**not written**): the backend collects the items and renderers, and a `RegisterClientExtensionsEvent` handler on the mod bus does
`event.registerItem(new IClientItemExtensions() { getCustomRenderer() → a BlockEntityWithoutLevelRenderer whose renderByItem(stack, ctx, pose,
buffers, light, overlay) calls the Renderer }, item)` for each entry. Without it the Mob Display is invisible there.

**NeoForge: what the posters must do. None of this is written.** The add-on has no NeoForge module yet (Fabric first, `AGENTS.md`) and no NeoForge build
file: `neoforge/build.gradle`, `build.gradle` and `settings.gradle` do not name the add-on, so its common tree has not been compiled against NeoForge. The
`TinkererApiOnlyTest` already lets a neoforge tree import `net.neoforged.` and `org.objectweb.`. The module
will need a class subscribed to `NeoForge.EVENT_BUS` (and one on the mod bus, and a mixin) that maps the NeoForge events onto the `TinkererEvents.post…`
methods; all the rules live in `TinkererEvents`, so only the field mapping is loader code. What sections 3 to 8 and 14 say about NeoForge's own
events was read in NeoForge's patched jars by the authors of those sections (section 14 names the 21.1.259 jars in the Gradle cache); **none of it
was run**. Until the posters exist, **every effect that depends on an event would not happen on NeoForge** (section 10 lists them per group; none of it was
observed, because the add-on has never been started there). Per event:

| Event / registry | What the NeoForge side must do | Details |
|---|---|---|
| `LIVING_DAMAGE` | a handler of `LivingIncomingDamageEvent`: first `TinkererEvents.framesRefuse(entity, event.getSource(), event.getAmount(), lastHurt)`, and return without posting when it is true (NeoForge's event is posted before the frames test and filters nothing); then `postLivingDamage(...)`, `event.setAmount(result)`, and `event.setCanceled(true)` when the result is zero. `lastHurt` is a protected field of `LivingEntity` there as well: the module needs an access transformer or an accessor mixin for it (a public reader would also serve the Soul Hearts, section 15) | section 3 |
| `LIVING_ARMOR_ABSORB` | in the same handler: `event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> TinkererEvents.postLivingArmorAbsorb(event.getEntity(), container.getSource(), container.getNewDamage(), reduction))`. No `damageAfterArmorAbsorb` step is needed (the modifier takes the amount directly). Keep the poster's own `bypasses_armor` test: the patched `actuallyHurt` calls the ARMOR reduction for such sources too | section 14 |
| `BREAK_SPEED` | a handler of `PlayerEvent.BreakSpeed` mapping `getNewSpeed()` and `getPosition()` to `postBreakSpeed(...)` and `setNewSpeed(result)`, cancelling at zero. It must run on **both sides** and also for a player that is never ticked and never in the level (the Animation Tablet's `TinkererFakePlayer`, which asks `state.getDestroyProgress(fake, level, pos)`); NeoForge posts it from `Player.getDigSpeed`, which `getDestroyProgress` calls (read, not run on NeoForge), and the handler must not filter by side or by tick | section 4 |
| `LIVING_JUMP` | a handler of `LivingEvent.LivingJumpEvent` calling `postLivingJump(entity)` | section 5 |
| `LIVING_FALL` | a handler of `LivingFallEvent` calling `postLivingFall(entity, new FallData(event.getDistance(), event.getDamageMultiplier()))`, then `setDistance` and `setDamageMultiplier`, or `setCanceled(true)` | section 6 |
| `USE_ITEM_TICK` | a handler of `LivingEntityUseItemEvent.Tick` calling `postUseItemTick(entity, stack, event.getDuration())` and `event.setDuration(result)`; check that NeoForge's duration ends in `useItemRemaining` | section 7 |
| `LIVING_DROPS` | a handler of `LivingDropsEvent` calling `postLivingDrops(entity, source, new ArrayList<>(event.getDrops()), TinkererEvents.lootingLevel(level, source), event.isRecentlyHit())`, then replacing the contents of `event.getDrops()` and cancelling on an interrupt. No capture mixins: NeoForge captures the drops itself | section 8 |
| `FloatingTicks` | the `Access` interface on `ServerGamePacketListenerImpl` by a mixin (the same mixin works on both loaders) | above |
| `ItemRenderers` | a backend given to `ItemRenderers.setBackend` and a `RegisterClientExtensionsEvent` handler that registers an `IClientItemExtensions` per item | above |

**Other loader glue that NeoForge needs and that is not an event of this layer. None of it is written for NeoForge** (the groups' own loader code; the
Fabric side is named where it exists):

* **Wings and ghost blocks of the Kami gear.** Fabric: `fabric/.../client/KamiGearFabricClient.java`, started by name from `KamiGearClient.register()`: the
  Robes of the Stratosphere's wings layer on every player renderer and the Worldshaper's Looking Glass's ghost blocks after the translucent terrain.
  NeoForge: `EntityRenderersEvent.AddLayers` and `RenderLevelStageEvent` (`api-requests/KamiGear.md` section 4, `deviations/KamiGear.md` section 10).
* **Fluids through the Transvector Interface.** Fabric: `fabric/.../TileBlocksFabric.java` (`FluidStorage.SIDED`), reached by name from `TileBlocks.ready()`.
  NeoForge: the same with `Capabilities.FluidHandler.BLOCK`, rated P1 in `api-requests/TileBlocks.md` section 6. Energy through the Interface is written for
  **neither** loader (the same section).
* **Flammability of other mods' blocks for the elemental fires.** A platform hook (`api-requests/SimpleBlocks.md` section 3, P3), written for **neither**
  loader: the fires burn the vanilla blocks only.
* **The special effects of the Bedrock dimension** (the constant fog of 1.7.10; `api-requests/KamiWorld.md` section 3, optional): not requested and written for
  **neither** loader (Fabric `DimensionRenderingRegistry`, NeoForge `RegisterDimensionSpecialEffectsEvent`).

## 10. Wiring the groups (what each group did; Fabric)

The five groups that needed an event were moved from their workarounds to the events above. Each entry says which workaround was removed, which
event replaces it and which check pins the replacement; the checks are GameTests of the group unless a JUnit test is named. A check named here exists in
the source under that name and was written for the thing it is named for. **Whether and when it ran is not the same for every group**, and this section does
not repeat it: KamiWorld (header and section 10 of its document), Foci (header) and, for its first round only, KamiGear (header) record runs with
numbers; the Enchantments document says that its GameTests and JUnit tests ran and refers to a report that is not in the repository; the Machines and
BasicItems documents record no result of a run of their checks (`Machines.md` says what was checked, without a result). For those three groups the only record is the results file described in section 12 ("What ran"), which
lists their GameTests without a failure and says nothing about JUnit. The client side of every event is **unverified** (section 12), and on NeoForge
none of this is written (section 9). What would not happen there is listed per group.

* **Enchantments** (`deviations/Enchantments.md` sections 2 to 4, 9 and 13; registered by `Enchantments.register()` → `ModEnchantments.initEnchantments()`,
  and by `Enchantments.ready()` → `ModEnchantments.initJump()`, section 2).
  * `LIVING_DAMAGE` replaced `AttackBoost` (an attribute modifier armed from `PlayerEvent.ATTACK_ENTITY`) and the conditions that Vampirism's
    `EntityEvent.LIVING_HURT` handler used to guess whether a blow would land. Pounce, Final Strike, Valiance, Focused and Dispersed Strikes and
    Vampirism now also work for the Animation Tablet's fake player, mobs, arrows and thorns. Pinned by `valianceMultipliesTheWholeAttackAtLowHealth`,
    `theStrikeCounterIsKeptOnTheWeapon`, `pounceDependsOnTheBlockUnderTheFeet`, `aFakePlayerAttackGetsTheEnchantmentsOfItsSword`,
    `aSweepOfASwordIsAHitForEachEntityItReaches` and `theFramesCompareTheBoostedAmountsNotTheRawOnes`.
  * `BREAK_SPEED` replaced `BreakSpeedControl` (a transient modifier on the player's `BLOCK_BREAK_SPEED`, set once per tick from the block under the
    crosshair, with a one-tick lag, a fixed 1024 for "at once" and a veto in `BlockEvent.BREAK`) and the `updateFor` call that the Animation Tablet
    had to make; patches 1 and 2 of `api-requests/Enchantments.md` section 5 are not needed. Pinned by `desintegrateAndFlamingTouchChangeTheDigSpeedOfTheBlock`,
    `shatterAndTunnelScaleTheDigSpeed`, `aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn`,
    `aPlayerThatIsNeverTickedDigsAtTheSpeedOfItsEnchantments` and `theTabletsFakePlayerDigsAndHitsWithTheEnchantments`.
  * `LIVING_JUMP` replaced the client handler that guessed a jump from the tick of the local player (`EnchantmentClientHandler`) for Ascent Boost.
    Pinned by `ascentBoostMultipliesTheJumpOfAPlayer` and, against the Gem Boots, `gemBootsAndAscentBoostLeggingsComposeTheirJumps` (KamiGear).
  * `LIVING_FALL` replaced the tracking of the airborne ticks of every server player (`onPlayerPost`), Shockwave's guesses about water and teleports,
    and Slow Fall's per-tick reduction of the distance (the landing rule `EnchantmentFormulas.slowFallLanding` stands in: a decision the owner may
    want to change, `DEVIATIONS.md` section 2). Pinned by `slowFallMakesALandingAFallOfTwoPointNineBlocks` and `shockwaveHurtsTheCreaturesAroundALanding`.
  * `USE_ITEM_TICK` replaced the arrow-speed workaround through `EntityEvent.ADD` for Quick Draw. Pinned by `quickDrawTakesTicksOffTheDrawClockOfABow`.
  * Kept on Architectury, not an event of this layer: `BlockEvent.BREAK` (Tunnel's remembered direction, the extra wear) and `TickEvent.PLAYER_PRE`
    (Slow Fall's motion and cloud on the local client). The only other users of Architectury's `EntityEvent.LIVING_HURT` in the add-on are the Cursed Spirit's
    Blade (Machines) and the Imbued effects (SimpleBlocks), both below.
  * Without NeoForge posters (nothing was run there): no damage factors and no Vampirism, no Shatter, Tunnel, Disintegrate or Flaming Touch, no Ascent
    Boost, no Slow Fall landing, no Shockwave, no Quick Draw.
* **KamiWorld** (`deviations/KamiWorld.md` sections 5, 6 and 10; `KamiWorld.register()` → `SoulHeartHandler.register()`, `DimensionalShardDropHandler.register()`).
  * `LIVING_DAMAGE` (the Soul Hearts, `SoulHeartHandler.onLivingDamage`, `ServerPlayer` victims only) replaced the Architectury `LIVING_HURT` fallback
    (cancel the hit, deal the rest again as a new hit with the difficulty scaling undone, players without hearts left alone) with its `exactDamageHook`,
    `reissuing` and `beforeDifficultyScaling`: all deleted, there is no second path. The group keeps the frames of the original itself (section 3). Pinned
    by `soulHeartsAbsorbDamageBeforeHealth`, `soulHeartsKeepTheFramesOfTheOriginal`, `soulHeartsAgreeWithTheOriginalWhenTheGamesFramesBeginLater`,
    `soulHeartsAreChargedOncePerTenTicksByLava` and, for the rule on its own, `soulHeartsExactRuleCutsTheFractionOfEveryHit`.
  * `LIVING_DROPS` (`DimensionalShardDropHandler.onLivingDrops`) replaced the loot-table pools added to `minecraft:entities/enderman` and
    `minecraft:entities/zombified_piglin` through `LootEvent.MODIFY_LOOT_TABLE` (deleted with their checks). Pinned by
    `shardDropsFollowTheConditionsOfTheOriginal` and `shardDropsComeFromRealDeathsWithTheChanceOfTheOriginal`.
  * Without NeoForge posters: the Soul Hearts would not act at all (the Architectury fallback, written for both loaders, is gone; it was never run on
    NeoForge), and there would be no dimensional shard drops.
* **KamiGear** (`deviations/KamiGear.md` sections 3, 13 and 14; `KamiGear.register()` → `KamiArmorHandler.register()`).
  * `LIVING_JUMP` and `LIVING_FALL` for the Gem Boots replaced an attribute modifier (`JUMP_STRENGTH` + 0.3, which scaled with the block's jump factor)
    and the zeroing of the fall distance of every wearer on every tick. Pinned by `gemBootsRaiseTheJumpOnlyWhileWornAndSwitchedOn`,
    `gemBootsLandWithoutFallDamageWhateverTheSwitchesSay` and `gemBootsAndAscentBoostLeggingsComposeTheirJumps` (the order of section 2).
  * `LIVING_ARMOR_ABSORB` (section 14) replaced the stand-in of a toughness of 5 per piece that the first port gave the Ichorcloth for want of
    `ISpecialArmor`: `IchorAbsorption`. Pinned by `ichorclothSetTakesEightyFivePercentOfBlowsAndLeavesBypassingDamageAlone`,
    `ichorclothPartialAndMixedSetsKeepTheOtherArmourAndLeaveOtherWearersAlone`, `ichorclothPiecesShowTheOriginalArmourAndNeverWear`, JUnit
    `KamiGearLogicTest`, and the event's own checks in `HooksRuntimeChecks` (section 12).
  * Without NeoForge posters: the Gem Boots would not jump higher, and switched-off Gem Boots would no longer spare the wearer a fall (the `LIVING_FALL`
    listener); switched-on boots would still zero the fall distance every tick (`ItemGemBoots.tick`, which stays). The Ichorcloth would be plain 3/8/6/3 armour.
* **Machines** (`deviations/Machines.md` sections 7, 8 and 12; `Machines.register()` → `ItemBloodSword.registerEvents()`, `MachinesClient.register()`).
  * `LIVING_DROPS` (`ItemBloodSword.onLivingDrops`, the Cursed Spirit's Blade) replaced the note-and-refuse workaround (a note made on `LIVING_DEATH`, a
    filter on `EntityEvent.ADD` and a spawn at the end of the level tick). Pinned by `anActivatedBladeReapsTheSoulAspectsOfTheMobItKills`,
    `anActivatedBladeClearsTheEquipmentToo`, `aRealSwingOfTheActivatedBladeReapsAndHurtsItsWielder` and, for the order against the shards,
    `aReapedEndermanAndZombifiedPiglinStillRollTheirDimensionalShard` (section 2).
  * `ItemRenderers.register` (the Mob Display) replaced the group's own Fabric glue `MachinesFabricClient` and the reflection that reached it. Check: the
    JUnit test `MachinesClientWiringTest`; that the mob is drawn is unverified (section 9).
  * Kept on Architectury, not an event of this layer: the blade's `LivingAttackEvent` handler stays on `EntityEvent.LIVING_HURT`
    (`ItemBloodSword.onDamageTaken`, `deviations/Machines.md` section 8, "Events").
  * Without NeoForge posters: the blade would not reap (a mob it kills would drop its loot) and the Mob Display would be invisible.
* **Foci** (`deviations/Foci.md` section 2; `Foci.register()` → `FociHooks.setFloatingTickReset(FloatingTicks.RESET)`): `FloatingTicks` replaced the
  reflection stop-gap that wrote the kick counter of the connection (two field names and a warning when neither was found), now deleted. Pinned by
  `flightPutsTheKickCounterOfTheConnectionBackToZero` and `flightResetOfTheStartUpClearsTheRealCounterOfTheCaster` (Foci), `floatingTicksReadAndResetTheRealCounter`
  (Hooks) and the JUnit test `FociLogicTest.theFlightKickResetIsWhatIsInstalledAndNullRemovesIt`. Without a NeoForge mixin the reset would do
  nothing, and a player who keeps launching himself with the Uprising on a server without `allow-flight` could be kicked after four seconds of climbing.
* **SimpleBlocks, TileBlocks and BasicItems** never needed an event of this layer that was missing, and do not use it. SimpleBlocks still has one
  Architectury `EntityEvent.LIVING_HURT` listener, the Imbued effects' stand-in for 1.7.10's `LivingAttackEvent` (`PotionEffectHandler.onLivingHurt`; whether
  it fires before the invulnerability checks, as the attack event did, is marked unverified in `deviations/SimpleBlocks.md`, bullet "The hit (forced)").
  TileBlocks needed nothing new: the
  Animation Tablet's fake player already calls `state.getDestroyProgress` and `attackEntity`, which are the posting paths of `BREAK_SPEED` and
  `LIVING_DAMAGE`, so the Enchantments reach it with no call from that group (pinned from the Enchantments side by
  `theTabletsFakePlayerDigsAndHitsWithTheEnchantments`).

## 11. Fabric build wiring

The ten mixins of `thaumictinkerer.mixins.json` (`fabric/src/main/java/thaumic/tinkerer/fabric/mixin`), and what each posts:

| Mixin | Target and injector | Posts |
|---|---|---|
| `LivingEntityHurtMixin` | `LivingEntity.hurt`, `@Inject(cancellable)` at the `INVOKE` of `isSleeping()`, `@Shadow lastHurt` | `LIVING_DAMAGE` (section 3) |
| `LivingEntityArmorMixin` | `LivingEntity.getDamageAfterArmorAbsorb`, `@WrapMethod`, **priority 900** | `LIVING_ARMOR_ABSORB` (section 14) |
| `BlockBehaviourBreakSpeedMixin` | `BlockBehaviour.getDestroyProgress`, `@WrapOperation` on `Player.getDestroySpeed` | `BREAK_SPEED` (section 4) |
| `LivingEntityMotionMixin` | `LivingEntity.jumpFromGround` at `TAIL`; `LivingEntity.causeFallDamage` at `HEAD`, cancellable | `LIVING_JUMP`, `LIVING_FALL` (sections 5, 6) |
| `LivingEntityUseItemMixin` | `LivingEntity.updateUsingItem` at `HEAD`, `@Shadow useItemRemaining` | `USE_ITEM_TICK` (section 7) |
| `LivingEntityDropsMixin`, `EntityDropCaptureMixin`, `ServerPlayerDropCaptureMixin`, `FoxDropCaptureMixin` | `dropAllDeathLoot` (and the fox's override) wrapped; `Entity.spawnAtLocation(ItemStack, float)` and `ServerPlayer.drop(ItemStack, boolean, boolean)` captured; `@Shadow lastHurtByPlayerTime` | `LIVING_DROPS` (section 8) |
| `ServerGamePacketListenerFloatingMixin` | makes `ServerGamePacketListenerImpl` implement `FloatingTicks.Access`, `@Shadow aboveGroundTickCount` | `FloatingTicks` (section 9) |

`HooksResourcesTest` checks that the config lists exactly the mixin classes of that package.

* The mixins are compiled with the add-on's source set `thaumicTinkerer`. Architectury Loom 1.11 puts every source set that exists when it initialises on the
  Mixin annotation processor, each with its own refmap `<sourceSet>-<archivesName>-<project>-refmap.json` (here
  `fabric/build/classes/java/thaumicTinkerer/thaumicTinkerer-Thaumcraft-fabric-fabric-refmap.json`), and the processor also knows the MixinExtras
  annotations. **Do not call `loom.mixin.add(sourceSet, name)` for the add-on**: the first explicit `add` switches that default off for every other source
  set, Thaumcraft's own included. `fabric.gradle` checks after `compileThaumicTinkererJava` that the refmap exists and names every mixin that has an
  injector (`GradleException` otherwise).
* The release path (`remapThaumicTinkererJar`) adds `"refmap"` to `thaumictinkerer.mixins.json` in the jar (Loom's `MixinRefmapService` does it for the
  configs listed in `fabric.mod.json`), and the development run reads the mixins by their Mojang names without a refmap, like the Thaumcraft mod's own.
* `@Shadow` fields (`lastHurt`, `useItemRemaining`, `lastHurtByPlayerTime`, `aboveGroundTickCount`) have no refmap entry, as in the mod's own mixins; Loom's remap of the jar renames them in the
  mixin classes.
* **Checked on a built jar** (`./gradlew :fabric:remapThaumicTinkererJar`, `fabric/build/libs/ThaumicTinkerer-fabric-<version>.jar`): it contains
  `thaumicTinkerer-Thaumcraft-fabric-fabric-refmap.json`, its `thaumictinkerer.mixins.json` carries `"refmap": "thaumicTinkerer-Thaumcraft-fabric-fabric-refmap.json"`, and in the
  remapped mixin classes the shadowed field is `field_6222` (intermediary). That jar was inspected, not started in a production environment. This bullet was written when
the config held nine mixins (before `LivingEntityArmorMixin`); no later inspection of a built jar is recorded in this document.
* MixinExtras 0.4.1 comes with Fabric Loader 0.16.14 (the add-on does not shade it). All mixins use `injectors.defaultRequire: 1` and `required: true`: a
  Minecraft change that moves a target stops the game at start-up with the mixin's name instead of silently losing a hook.
* The `TinkererApiOnlyTest` lets the Fabric tree import `org.spongepowered.` and `com.llamalad7.` besides `net.fabricmc.`; `thaumcraft.common.` and
  `thaumcraft.client.` stay forbidden everywhere.

## 12. Tests

**What ran** (only what a document or a file says; everything else is *not recorded*, which is not the same as failed):

* *GameTests, a results file.* `fabric/build/tinkerer-gametest-results.xml` (written by `:fabric:runTinkererGameTest`, time stamp 2026-10-30 10:29) lists
  298 cases and no `failure`, `error` or `skipped` element: simpleblocks 48, tileblocks 34, machines 47, basicitems 19, foci 36, enchantments 34, kamigear 36,
  kamiworld 20, hooks 14, parity 7, core 3. Its 14 `hooksgametests` cases are the 14 `@GameTest` methods of `HooksGameTests`, among them the two of
  `LIVING_ARMOR_ABSORB`, and it lists `gemBootsAndAscentBoostLeggingsComposeTheirJumps`. The console log of that run was not read, and a results file does not
  say which tree it ran: when this was written the only sources of the add-on's `main`, `test` and `gameTest` trees that were newer than it were
  `ItemGemBoots.java` (a comment) and the build script `fabric.gradle`. That is a statement about file time stamps, not a record of the run.
* *GameTests, the groups' documents.* `deviations/KamiWorld.md` records that its GameTests passed together with the 12 `hooksgametests.*` of its hooks round (31 of 31) and, with its
  20 checks, the 13 of its review round (33 of 33); the mutation checks below were made by hand on the checks that existed then. No document
  records a run of the two `LIVING_ARMOR_ABSORB` GameTests; the results file above is their only trace.
* *JUnit.* No document records a run of `TinkererEventsTest` or `HooksResourcesTest`. `docs/PARITY_REPORT.md` section 2 records the parity tests, `TinkererApiOnlyTest`
  and the 30 `kamigear.*` tests as passed.
* *Not run at all:* every client side (below).

JUnit (`./gradlew :fabric:tinkererTest`): `TinkererEventsTest` (chain order, zero/negative/NaN cancel, a lone listener, no listener, the clamp of the use
tick, drops listeners and `hasLivingDropsListeners`, `ItemRenderers` queue and duplicate, and the armour event: chain order, a lone listener clamped like a chain,
NaN, a source that bypasses armour, no damage, the bit-for-bit pass-through, `armorStepGivesBackTheGamesOwnFloatUnlessAListenerChangedTheAmount` and
`armorStepOfABlowThatIsNotFiniteIsTheGamesOwnValueNotNaN`), `HooksResourcesTest` (the mixin config lists exactly the
mixin classes, `fabric.mod.json` lists the config, no class names a mixin class, handler prefix, common code does not import loader code).

GameTest (`HooksGameTests`, 14 methods, run with `-PtinkererGameTestFilter='hooksgametests.*'`), each registering its listeners inside the check, answering only for its own
entities and taking them away in a `finally` (or, where a check waits for ticks, in a `guarded` cleanup that also runs when the part before the wait fails, since a failed
GameTest does not run the callbacks it still has pending):

| Check | What it proves |
|---|---|
| `livingDamageIsPostedBeforeArmourAndChainsInOrder` | a zombie's blow on a pig with 20 armour points: the listener sees 10 (not the armoured 4), the armour then works on the halved/chained amount, order (10·2)−3 = 17 vs (10−3)·2 = 14, source carries the zombie |
| `livingDamageCancelIsLikeInvulnerableAndFramesFollowTheListener` | a cancelled hit leaves health, frames, hurt time, knockback and last damage source exactly as an invulnerable pig does; the frames compare the listener's result |
| `livingDamageIsNotPostedForHitsTheInvulnerabilityFramesRefuse` | a counting listener: a first hit is posted; a weaker and an equal hit in the frames are refused by the game and **not posted**; a stronger one is posted with its own amount and deals the difference; after the frames a weak hit is posted; a cancelled hit starts no frames and its repeat is posted again; a freezing hit on a blaze is posted although 3 is not above the last 10 (the game compares 15); a source of the `bypasses_cooldown` tag is posted in the frames if one exists |
| `livingDamageSeesAFakePlayersBlowAndAZombiesBlowOnAPlayer` | `TinkererFakePlayer.attackEntity` (diamond sword) and a zombie against a mock server player (difficulty-scaled amount) |
| `livingDamageSeesAnArrow` | a real arrow flight and hit, shooter in the source |
| `breakSpeedIsPostedForAServerPlayerAndAFakePlayer` | `state.getDestroyProgress` for a mock server player and a never-ticked fake player: factor, position, state, order of two listeners, cannot dig = 0, and a real `ServerPlayerGameMode` dig start that breaks the dirt at once with a ×1000 listener |
| `livingJumpIsPostedAfterTheJumpMotionIsApplied` | a pig's `jumpFromGround` (listener sees 0.42 and doubles it) and a player's jump through `setJumping(true)` and `doTick` (the `aiStep` path): the listener sees 0.42, the player rises 0.84 in that tick and keeps (0.84 - 0.08) * 0.98 of vertical motion, not the 0.3332 of a plain jump |
| `livingFallIsPostedOnLandingAndCanHalveOrCancel` | four pigs really fall 10.5 blocks (the harness's barrier ceiling at relative y = 9 is opened for the columns): the listener sees the distance vanilla counts (about 9.7: the movement of the landing tick is not in it), the untouched pig takes the damage of that distance, the halved one the damage of half of it, the cancelled one nothing (no frames), the one whose damage multiplier the listener sets to 0.5 the damage of `ceil((distance - safe) * 0.5)` (the multiplier seen is the stone's 1); on the server; a player's `causeFallDamage` cancelled |
| `useItemTickExtendsABowDraw` | two mock players draw a bow for 12 ticks: the draw clock reads 3× with the listener, the listener always gets the bow that is in use (`stack == getUseItem()`), and the released arrow flies at 3.0 against under 2.0 |
| `livingArmorAbsorbIsPostedFromTheArmourStepAndReplacesWhatArmourTakesOff` | a pig in 20 armour points and a player: the listener sees the damage after `LIVING_DAMAGE` and vanilla's amount, can leave it alone, halve the blow, push past the blow or below nothing, answer NaN; chains; starvation and magic are not posted and not reduced; a bystander is left as vanilla; a pig in a Fortress chestplate is posted after Thaumcraft's ward pass (what is left after it) |
| `livingArmorAbsorbHandsBackTheGamesOwnFloatAndSurvivesAnInfiniteBlow` | a real `hurt` of a real pig: the health after a blow whose round trip differs from vanilla's float in the last place is vanilla's exactly, with and without a listener that returns what it is given; an infinite blow kills, leaves no NaN health and is not posted |
| `livingDropsAreEditedBeforeTheyEnterTheWorld` | a pig killed by the fake player with Looting III: sees pork, level 3, recently hit, the fake player as the attacker, nothing in the world yet; add a diamond / remove the pork / clear / cancel; experience orbs unaffected; a pig killed by plain damage: looting 0, not recently hit, no attacker; a pig that has a bystander spawn an emerald during its death: the list holds its own gold and pork and not the emerald, which is in the world at once; a fox's mouth item (spawned before `super`) is in the list, with a control that it drops with nobody listening; the listener count is back to what it was before; plain again afterwards |
| `livingDropsCoverAPlayersInventoryButNotADropByHand` | `ServerPlayer.die` offers the inventory and a cleared list drops nothing; a drop by hand is immediate |
| `floatingTicksReadAndResetTheRealCounter` | the accessor against an independent reflective read of the private field on a real connection, the connection's own `tick()` resetting the same counter, no connection, the fake player's connection |

**Mutation checks** (done by hand once, after the review fixes; the sources were changed, the GameTests run, the sources put back): with `framesRefuse` always false
`livingDamageIsNotPostedForHitsTheInvulnerabilityFramesRefuse` failed ("neither was posted: [10.0, 8.0, 10.0]"); with the `LIVING_JUMP` injection at `HEAD` instead of `TAIL`
the jump check failed (the listener saw 0.0); with the damage-multiplier write-back of `LIVING_FALL` removed the fall check failed; with the `ItemStack` of `USE_ITEM_TICK`
replaced by `ItemStack.EMPTY` the bow check failed (the last two failed in a delayed callback, whose message is not in the server log, so only the name of the failing test is
known); with `recentlyHit` fixed to `true` the drops check failed ("and no player hit the pig lately"); with the owner guard of `DropCapture.offer` removed the same check failed
("the bystander's emerald is not [in the list]"); with `FoxDropCaptureMixin` taken out of the config it failed at "the fox's mouth item is in the list: []". Each mutation
failed exactly the check that owns it. Not mutation-tested: the rest of the assertions (they were only shown to pass), and the two checks of
`LIVING_ARMOR_ABSORB`, which were added after these mutations (none is recorded for them in this document or in `deviations/KamiGear.md` section 13).

**Not run, because a server cannot**: that the same events are posted on the client (the dig prediction, the jump, the bow draw and the fall of the local player).
The posting code is the vanilla classes' own, shared by both sides; `HOOKS.md` relies on that. A client run with a listener would show it.

## 13. Where the events are not posted (vanilla 1.21.1, read from the bytecode)

One list, so that a group author does not assume full coverage. Items 1 and 3 are the same on NeoForge (checked in its patched jar: no `onLivingFall` in `AbstractHorse`
or `Llama`, bamboo asks `canPerformAction(SWORD_DIG)` and returns 1 without `getDigSpeed`); item 4 was not looked up in NeoForge. The Fabric mixins could close them, none is
written because no group needs them yet. (The fox's mouth item, which a first reading of the wrapped method would have put here, **is** covered: section 8.)

| # | Gap | Effect | Event |
|---|---|---|---|
| 1 | `AbstractHorse.causeFallDamage` and `Llama.causeFallDamage` never call `super` | no `LIVING_FALL` for horses, donkeys, mules, skeleton/zombie horses, llamas; the rider is hurt through `Entity.hurt` (still `LIVING_DAMAGE`) and gets no event of its own | `LIVING_FALL` |
| 2 | An entity of another mod that spawns items before `super.dropAllDeathLoot` (the fox does and is handled) | those items enter the world at once | `LIVING_DROPS` |
| 3 | `BambooStalkBlock` and `BambooSaplingBlock` return `1.0F` for a sword without `super` | a "cannot dig" listener cannot stop a sword cutting bamboo; no event is posted for it | `BREAK_SPEED` |
| 4 | `ArmorStand.hurt` does not call `LivingEntity.hurt` | no event for armour stands | `LIVING_DAMAGE` |
| 5 | A real server player's jump comes from `handleMovePlayer`, not from the key | the event also fires for other upward take-offs of a player on the server | `LIVING_JUMP` |
| 6 | Hits the invulnerability frames certainly refuse are not posted; a covered hit that a listener cancels starts no frames | see section 3 | `LIVING_DAMAGE` |
| 7 | Shield-blocked and helmet-reduced hits are posted with the full amount | the game's later steps may still refuse them | `LIVING_DAMAGE` |
| 8 | Client side of every "both sides" event is not run by a test | the dig prediction, the jump, the fall and the bow of the local player rely on the vanilla code being shared | all |
| 9 | A source of the tag `bypasses_armor` (starvation, drowning, magic, falling, the void...) never gets a share from the armour step | `LIVING_ARMOR_ABSORB` is not posted for it, so the Ichorcloth does not reduce it; whether Forge 1.7.10 reduced such a hit by 85% as well is **unverified** (`deviations/KamiGear.md` section 3) | `LIVING_ARMOR_ABSORB` |
| 10 | The wear of the armour pieces (`hurtArmor`) has happened before the event is posted | a listener cannot change or undo it; a piece without durability does not wear | `LIVING_ARMOR_ABSORB` |

## 14. `LIVING_ARMOR_ABSORB`

(Appended after section 13 because the group documents and the code comments quote the numbers of sections 3 to 13; it belongs with sections 3 to 8. The
KamiGear group wrote the text in `deviations/KamiGear.md` sections 13 and 14; this is that text, checked against `TinkererEvents` and
`LivingEntityArmorMixin`.)

```java
float onLivingArmorAbsorb(LivingEntity victim, DamageSource source, float damage, float absorbed)   // the amount the armour takes off
```

*Posted* on the **server** from `LivingEntity.getDamageAfterArmorAbsorb`, the armour step of `actuallyHurt` (`Player.actuallyHurt` calls the same method),
**after** vanilla has worn the armour pieces (`hurtArmor`) and worked out what its formula takes off (`damage - CombatRules.getDamageAfterAbsorb(...)`), and
**before** the magic step (Resistance, the Protection enchantments) and Absorption. `damage` is what comes to the armour step: the hit after
`LIVING_DAMAGE`, the shield and the invulnerability frames; `absorbed` is vanilla's amount, or what the listeners before returned. It is the position at which
Forge 1.7.10 asked an `ISpecialArmor` piece for its `ArmorProperties` (from the documented behaviour of `ArmorProperties.ApplyArmor`; **unverified**: no Forge
1.7.10 source or jar is on this machine), and the listener has the same power: it **replaces the amount the armour absorbs**. The damage that goes on is
`damage - result`. Posted for every living entity, player or mob, so a listener answers only for the entities it means (the pieces its item classes
wear); it has no world effect and needs no side test, because the armour step runs on the server only.

*A listener may* return another amount (`absorbed` to leave it alone). The rules are in `TinkererEvents.postLivingArmorAbsorb`, the one place both
loaders share:

* **Not posted** for a source of the tag `bypasses_armor` (the game does not reduce such a hit by armour) nor for a damage that is not positive and finite.
  The same for a hit that never reaches the armour step (cancelled by `LIVING_DAMAGE`, refused by the invulnerability frames, an invulnerable entity).
* The result is held between `0` and `damage` (a listener cannot make the armour heal, or take more than the blow); a NaN is ignored (the value stays what
  it was).
* Listeners run in the order they were registered and each sees the last one's result in `absorbed`: a listener that *replaces* the value (Ichorcloth)
  overrides the ones before it, one that *adjusts* it (a bonus on top) composes. With nobody listening, or with a result equal to `absorbed`, the game gets
  its own value back **bit for bit** (no rounding of `damage - (damage - x)`, no NaN for an infinite blow): `TinkererEvents.damageAfterArmorAbsorb`.

*Differences from `ISpecialArmor`* (what this event is not): there is no per-piece `getProperties` / `damageArmor` call, no priority levels of its own (the
one Thaumcraft pass of priority 1, the wards, runs before the event, see below), no `AbsorbMax`, and no hook on the wear of other armour. The share of the
armour that a listener leaves alone is the 1.21.1 formula's, not 1.7.10's 4% a point. A hit that bypasses armour is not posted; whether Forge applied
`ISpecialArmor` to such hits could not be checked, and the one place to change it is the `BYPASSES_ARMOR` test in `postLivingArmorAbsorb`
(`api-requests/KamiGear.md` section 1, `deviations/KamiGear.md` section 3).

*Order against Thaumcraft's ward pass*: Thaumcraft's `LivingEntityMagicWardMixin` wraps the same method for the priority-1 pieces of the original's
`ISpecialArmor` (the void robes and the Fortress armour: their share comes off first, then the ordinary armour sees what is left). `LivingEntityArmorMixin`
has `priority = 900`, below the default, so it is applied first and is the **inner** wrapper: the event gets the damage that is left after the ward pass. With
the priority raised to 1100 the wrapper would be the outer one, the event would see the whole blow and a listener that replaces the amount would throw the
ward's share away. Check: the Fortress chestplate case of `livingArmorAbsorbIsPostedFromTheArmourStepAndReplacesWhatArmourTakesOff` (a pig is posted with
6.5 of an explosion of 10). With the default priority the order would be that of the loading of the two mixin configs.

*Fabric*: `LivingEntityArmorMixin`, a `@WrapMethod` on `getDamageAfterArmorAbsorb`, registered in `thaumictinkerer.mixins.json` under the usual
`defaultRequire: 1`. It runs the original (the wear included) and hands its value to `TinkererEvents.damageAfterArmorAbsorb`, which computes
`absorbed = damage - vanilla`, asks `postLivingArmorAbsorb`, and returns `vanilla` itself when the poster returned `absorbed` unchanged (compared bit for bit,
so that an infinite blow, whose `absorbed` is `Infinity - Infinity` = NaN and never `==` to itself, still gets the game's infinity and not a NaN health), and
`damage - result` otherwise. Nothing in the add-on deals an infinite blow, but other mods can.

*NeoForge* (**not written**; read from the 21.1.259 jars in the Gradle cache, not run): there is no event of this exact shape, but the equivalent exists:
`LivingIncomingDamageEvent#addReductionModifier(DamageContainer.Reduction.ARMOR, IReductionFunction)`. The patched `LivingEntity.actuallyHurt` and
`Player.actuallyHurt` call `DamageContainer.setReduction(ARMOR, newDamage - getDamageAfterArmorAbsorb(source, newDamage))` after `hurtArmor` and the
formula; `setReduction` runs the added functions in insertion order (each gets the container and the reduction so far and returns the new one) and then
subtracts the result from the container's damage. The handler that the NeoForge module needs for `LIVING_DAMAGE` (section 3) adds on every event:

```java
event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) ->
        TinkererEvents.postLivingArmorAbsorb(event.getEntity(), container.getSource(), container.getNewDamage(), reduction));
```

`container.getNewDamage()` is the damage that comes to the armour step at that moment. **Keep the poster's `bypasses_armor` test**: the Javadoc of
`DamageContainer#addModifier` says that a function does not run when vanilla's reduction is not triggered, but the patched `actuallyHurt` calls
`setReduction(ARMOR, ...)` unconditionally, also for a source that bypasses armour (with a reduction of 0), so the functions do run for such sources, and the
test in `postLivingArmorAbsorb` is what keeps them out. NeoForge also has `ArmorHurtEvent` (the wear of the pieces) and `LivingShieldBlockEvent`; neither changes
the amount, so neither is used.

*Used by*: KamiGear, `IchorAbsorption.onLivingArmorAbsorb`, registered by `KamiArmorHandler.register()`: every Ichorcloth piece takes `0.0425 × its armour points`
of a blow, flat, plus the formula's share for the armour that is not Ichorcloth (`deviations/KamiGear.md` sections 3 and 13). **Open point, unverified**: the
slot index that 1.7.10 passed to `getProperties` and `getArmorDisplay` (chestplate and leggings may have been swapped; `deviations/KamiGear.md` section 3).

*Tests*: JUnit `TinkererEventsTest` and the two GameTests of the event (section 12), and the checks of the KamiGear group that use the three events it
listens to (text of `deviations/KamiGear.md` section 14; they build real pieces on mock players, use a new player for every hit because the Soul Hearts keep the
1.7.10 frames of each server player by game tick, and wait 65 ticks for the spawn invulnerability of a new player):

* JUnit `KamiGearLogicTest`: the ratio of each piece, 85% for the set of any blow, partial sets, the sum with other armour and its cap at the blow.
* `ichorclothSetTakesEightyFivePercentOfBlowsAndLeavesBypassingDamageAlone`, `ichorclothPartialAndMixedSetsKeepTheOtherArmourAndLeaveOtherWearersAlone` (with a
  diamond chestplate, toughness 2, beside Ichorcloth leggings: 5.85 of 10, where the same set without the toughness would let 6.25 through) and
  `ichorclothPiecesShowTheOriginalArmourAndNeverWear`: the numbers of `deviations/KamiGear.md` section 3 (blows of 1, 10 and 40; starvation, magic, fall and
  drowning; both sets; the switches; partial, mixed, iron, wrong slot, bare and a zombie; another listener sees 8.5 and its extra point composes; the pieces keep
  damage 0 while iron wears; 3/8/6/3 and no toughness in the attributes). A blow of 10 against the full set leaves 1.5 where the formula alone, on the same twenty
  armour points, leaves 4.0.
* `gemBootsRaiseTheJumpOnlyWhileWornAndSwitchedOn` (0.42 against 0.72; piece off, master switch off, plain Ichorcloth boots, Gem Boots on the head, a pig in the
  boots and a bare player: 0.42; on honey the 0.3 comes on top; the jump key's own path carries the wearer 0.72 up),
  `gemBootsLandWithoutFallDamageWhateverTheSwitchesSay` (a landing of 10 blocks costs a bare player 7 and the wearer 0, switched off too; a listener registered
  after the boots' sees distance 0) and `gemBootsAndAscentBoostLeggingsComposeTheirJumps` (section 2).

Not run: the same events on a client (the jump of the local player and the landing sound), and a real flight and landing of a Gem Boots wearer (the landing is
driven through `Block.fallOn`, the game's own handler, not through a fall).

## 15. Possible improvements (none of them is done)

Each is an idea that came out of the work on the groups; none is written, and no group is blocked by it today.

* **A refusal test that `framesRefuse` consults before the chain** (`api-requests/KamiWorld.md` section 1, optional): a way for a listener to register
  `(victim, source, amount) -> boolean`, for instance `TinkererEvents.FRAMES_REFUSE`, asked with the raw amount before any listener runs. The Soul Hearts
  would register theirs (the player's own frames running and `amount <= lastDamage`). It would stop the listeners that come earlier in the chain (the
  Enchantments' Vampirism and strike counter) from running for a hit that the pool's frames refuse (section 3, "Other listeners and the Soul Hearts' frames"), and
  it would give back the comparison with the raw `lastDamage` of 1.7.10 where the group now compares the amount that earlier listeners left. The other way is
  for the Enchantments group to guard its side effects.
* **A public reader of the entity's `lastHurt`** in `TinkererEvents` (`api-requests/KamiWorld.md` section 1, optional): the posters already read it for
  `framesRefuse` (Fabric has the `@Shadow`, NeoForge needs an accessor anyway). The Soul Hearts would read it instead of tracking the amount they handed
  back, and the corner cases of `deviations/KamiWorld.md` section 5 that come from a shield, a helmet against a falling block or a later listener changing
  the amount would go.
* **A way to keep a hit alive with zero damage** (frames, knockback and hurt animation, no armour wear, no health change; mentioned in
  `api-requests/KamiWorld.md` section 1, which says it was not requested): it would remove the differences of a covered hit (no knockback, no `lastHurtByMob`,
  a sleeper not woken, a shield not used).
* **Priorities for the listeners** (a priority argument on `register`, or a documented order of the groups): the orders of section 2 are now a property of
  where each group registers. A priority would let Shockwave have its `LOW` of 1.7.10 again (`api-requests/Enchantments.md` section 4).
* **Mixins for the gaps of section 13** (horses and llamas for `LIVING_FALL`, the armour stand for `LIVING_DAMAGE`): written for nobody, because no group needs
  them yet.
