# Enchantments: requests to the Thaumcraft API and to the loader modules

Group Enchantments: the fourteen enchantments, their event handler, the Enchanter's table of costs and rules and the Osmotic
Enchanter. Nothing below was done by reaching into `thaumcraft.common.*`.

The Thaumcraft API itself was enough for this group: `WandHelper` (`isWand`, `isStaff`, `getCap`, `getAllVis`,
`consumeAllVisCrafting`), `ThaumcraftApiHelper.isResearchComplete(Player, key)`, `ThaumcraftApi.ENCHANTMENT_HASTE` and
`ENCHANTMENT_REPAIR`, `BlocksTC.state(field, meta)`, `ItemApi`, `ThaumcraftFX.essentiaTrail` and `ThaumcraftFX.blockRunes` (the
rune effect of the Enchanter) and `ThaumcraftSoundIds` are used as they are.

The group needed five hooks that Architectury does not offer (sections 1 to 4 and 6) and two changes in other groups' files
(section 5). **The lead has written the loader layer** (`thaumic.tinkerer.common.hooks.TinkererEvents`, `docs/HOOKS.md`) and the
group is wired to it: every request below is **answered**, with what the event gives and what it does not. The workarounds that
the requests described are deleted (`AttackBoost`, `BreakSpeedControl`, `EnchantmentClientHandler`, the airborne tracking, the arrow
speed through `EntityEvent.ADD`); the behaviour of the group is in `docs/deviations/Enchantments.md`.

**What the NeoForge module has to post** (not written yet; `docs/HOOKS.md` sections 3 to 7 give the NeoForge counterpart of each):
`LIVING_DAMAGE`, `BREAK_SPEED`, `LIVING_JUMP`, `LIVING_FALL` and `USE_ITEM_TICK`. Without the poster of an event on NeoForge the
enchantments that listen to it do nothing there: no damage factors and Vampirism (`LIVING_DAMAGE`), no Shatter, Tunnel, Disintegrate
and Flaming Touch speed (`BREAK_SPEED`), no Ascent Boost, no Slow Fall landing nor Shockwave, no Quick Draw. The group needs nothing
else from the loaders.

## 1. A damage event whose amount can be changed (loader event) - ANSWERED: `LIVING_DAMAGE`

**Need.** Pounce, Final Strike, Valiance, Focused Strikes and Dispersed Strikes change `LivingHurtEvent.ammount` of 1.7.10,
for any living attacker and any damage that carries an attacker (melee, arrows, thorns); Vampirism is in the same handler.

**What `LIVING_DAMAGE` gives.** `float onLivingDamage(victim, source, amount)` on the server, for every living entity and whatever dealt
the damage, with the new amount as the result. The group's listener (`ModEnchantmentHandler.onEntityDamaged`) is the sixteen lines
of the original: it works for the blows of players, of the Animation Tablet's fake player, of mobs, for arrows and thorns, which
the workaround (`AttackBoost`, an attribute modifier armed from `PlayerEvent.ATTACK_ENTITY`) could not do, and it does not depend on the
shape of the attack formula.

**What it does not give (written down in `deviations/Enchantments.md` section 2).** The amount is the one *before* the shield,
the helmet, the invulnerability frames and the armour; 1.7.10's event came after the frames had subtracted the last hit, so the
factors multiply the whole amount of a stronger hit that arrives inside the frames and vanilla subtracts afterwards (the same
damage for a constant factor, a different one with Final Strike's dice). A hit that a shield blocks or a helmet reduces is posted
with its full amount. A hit that the frames refuse by its raw amount is not posted (as in 1.7.10 it did not reach the event). But the
frames compare the *listener's result* with the next hit, where 1.7.10 compared the raw amounts, so a second blow inside the frames has a
**band** in which the two differ: with a factor above one, a blow above the first raw blow but not above its boosted amount is refused
here and was dealt there; with a factor below one, a blow that 1.7.10 refused before its event is posted here (the counter moves,
Vampirism heals) and then refused by vanilla, so it deals nothing (`deviations/Enchantments.md` section 2, pinned by a GameTest).
**A sweep of a sword** (no 1.7.10 counterpart) hurts every swept entity with the same damage source as the blow, so the event is posted
once for each of them, and the listener, which cannot tell a swept entity from the target (the event carries nothing for that), heals
Vampirism, rolls Final Strike and steps the strike counter for each (`deviations/Enchantments.md` section 2, pinned by a GameTest).
Armour stands are not covered (their `hurt` does not reach `LivingEntity.hurt`); nothing of the group depends on that.

## 2. A break-speed event (loader event) - ANSWERED: `BREAK_SPEED`

**Need.** Shatter, Tunnel, Disintegrate and Flaming Touch change `PlayerEvent.BreakSpeed.newSpeed` and cancel it (1.7.10).

**What `BREAK_SPEED` gives.** `float onBreakSpeed(player, state, pos, speed)` on both sides, inside `BlockState.getDestroyProgress`, a result of
zero or less being "cannot dig". Right place, right moment: no crosshair, no tick of the player, no lag. The workaround
(`BreakSpeedControl`, a transient modifier on `BLOCK_BREAK_SPEED`, one tick behind, a fixed 1024 for "at once", multiplying with every
other mod's modifiers) is deleted; "at once" is `Float.MAX_VALUE` as in the original.

**What it does not give.** The cancel is a progress of exactly zero, not 1.7.10's negative one (no difference to the player). Not posted
for creative mode's instant break (the original did not ask either), for unbreakable blocks, and for the bamboo cut by a sword
(`HOOKS.md` section 13, not a tool of the group). The wear of the tool (`damageItem(1)` on every event in 1.7.10) is the group's own
rule, once per block at `BlockEvent.BREAK` (`deviations/Enchantments.md` section 9): the event is also asked by the client and by
everything that wants a dig time.

## 3. Jump event (loader event) - ANSWERED: `LIVING_JUMP`

**Need.** Ascent Boost multiplies the vertical motion of a jump (`LivingJumpEvent`, on both sides).

**What `LIVING_JUMP` gives.** `void onLivingJump(entity)` on both sides at the end of `jumpFromGround`, with the motion of the jump
already applied. The group's listener multiplies it (players only, the highest level on the armor, not while sneaking). The
client handler that detected the jump from the tick of the local player (`EnchantmentClientHandler`) is deleted; other players'
jumps and the jumps from a mount or a command are no longer missed.

**What it does not give.** It is the server's jump too, which a real server player makes from the movement packet, so the event also
fires for other upward take-offs that the client reports (`HOOKS.md` section 5); the rule is the same on both sides, so the server's
expected motion agrees with the client's.

## 4. Fall event (loader event) - ANSWERED: `LIVING_FALL`

**Need.** Shockwave reacts to `LivingFallEvent` (distance greater than 3 blocks); Slow Fall could also use it to reduce the distance.

**What `LIVING_FALL` gives.** `onLivingFall(entity, FallData)` on both sides at the head of `causeFallDamage`, with the distance and the
damage multiplier changeable and a cancel. Shockwave uses the distance (server only; it tests `isClientSide` itself), Slow Fall changes
it (both sides, the same rule). The tick-by-tick tracking of airborne players (`ModEnchantmentHandler.onPlayerPost`) is deleted.

**What it does not give.** Not posted for horses, donkeys, mules, skeleton and zombie horses and llamas, nor for their riders
(`HOOKS.md` section 6, 13): no Shockwave and no Slow Fall for a rider. It comes **once, at the landing**, while Slow Fall's original
acted every tick of the fall (and divided the motion): the motion and the cloud stay a per-tick client listener, and the distance is
the end result of the ticks, 2.9 (`deviations/Enchantments.md` section 4). Listeners of one event run in the order of registration and
the event has no priority, so Shockwave's `LOW` priority of 1.7.10 (after every other handler) is not reproduced against other groups:
this group registers Slow Fall, then Shockwave, and KamiGear registers the Gem Boots' listener (distance 0) after both
(`ContentGroups.ALL`; `deviations/KamiGear.md` says the same). No live effect, since Shockwave cannot be put on the Gem Boots; an ordered
registration (a priority argument, or the order of the groups) would close it.

## 5. The Animation Tablet's fake player: attack and dig - NOT NEEDED any more (the events are posted for it)

**Need.** The Animation Tablet attacks and digs with a fake player that is never ticked. In 1.7.10 both went through events the
enchantments hook: `attackTargetEntityWithCurrentItem` posted `LivingHurtEvent` and `ItemInWorldManager`'s dig progress posted
`PlayerEvent.BreakSpeed`. The group asked for two patches in other groups' files: patch 1 in
`TinkererFakePlayer.attackEntity` (bracket the attack with `AttackBoost.beforeAttack/afterAttack`) and patch 2 in
`TileAnimationTablet` (call `BreakSpeedControl.updateFor(player, pos)` before each `getDestroyProgress`).

**Answer.** Both patches are **moot**: `LIVING_DAMAGE` is posted from `LivingEntity.hurt`, which the fake player's `Player.attack` reaches, and
`BREAK_SPEED` is posted from `BlockState.getDestroyProgress`, which the Tablet already calls for its fake player (never ticked, never in the
level; the event is posted with the real position, `HOOKS.md` section 4). `AttackBoost` and `BreakSpeedControl` do not exist any more, so
neither patch could be applied; no file of another group was touched. The checks are now real and required:
`aFakePlayerAttackGetsTheEnchantmentsOfItsSword` (was `required = false`), `aPlayerThatIsNeverTickedDigsAtTheSpeedOfItsEnchantments` (was
"after `updateFor`") and the cross-check `theTabletsFakePlayerDigsAndHitsWithTheEnchantments`, which lets a `TinkererFakePlayer` dig with Shatter
and Tunnel and hit with Pounce. The fake player's health is fixed, so Valiance cannot be tried with it; the tests use Focused Strikes and Pounce.

## 6. A bow draw clock (loader event) - ANSWERED: `USE_ITEM_TICK`

**Need.** Quick Draw takes extra ticks off `itemInUseCount` while a bow is drawn, so the draw is faster and the bow looks drawn
sooner.

**What `USE_ITEM_TICK` gives.** `int onUseItemTick(entity, stack, remaining)` on both sides at the start of every tick of the use, the result being the
entity's own count, which the bow's pose, the HUD and the arrow's power (`releaseUsing`) read. The arrow-speed workaround through `EntityEvent.ADD` and
its two limits (the bow did not look drawn faster; a draw under the game's minimum power released no arrow) are deleted.

**What it does not give.** The event comes before the decrement of the tick, 1.7.10's handler ran after it: the listener applies the rule to
`remaining - 1` and adds the one back, so the clock goes through the original's values (`deviations/Enchantments.md` section 4). The
`Math.max(2, ...)` floor is the listener's.

## 7. Typed access to the Enchanter's pillar blocks (P3)

**Need.** The Enchanter looks for columns of two to twelve Obsidian Totems under a Nitor.

**Workaround.** `BlocksTC.state("blockCosmeticSolid", 0)` (the totem) and `BlocksTC.state("blockAiry", 1)` (the Nitor), both
resolved by the legacy field and metadata table, cached on first use. The totem is compared as a block state, the Nitor as a
block. This breaks if the table loses those two rows, with no compile-time warning.

**Proposed.** `BlocksTC.obsidianTotem()` returning the block state and `BlocksTC.nitor()` returning the block, or the block tags
`thaumcraft:obsidian_totem` and `thaumcraft:nitor` (tags would also let a resource pack add pillar blocks).

## 8. Not requested, for the record

- **Applied Energistics 2 `IMovableTile`** of the Enchanter (`prepareToMove` / `doneMoving`) is not ported (no AE2 in the 1.21.1
  build).
- **Enchantments of Thaumcraft and of later Minecraft versions.** The Enchanter's table is the 1.7.10 one: the 24 vanilla
  enchantments of that version, Haste, Repair and the fourteen of this group. Mending, Sweeping Edge, Depth Strider, Soul Speed
  and the rest are not offered. An API that lets Thaumcraft or another add-on register a row (key, icon, level-1 cost, research,
  rules) would remove the closed table; `EnchantmentManager` has the shape for it (`registerExponentialCostData` is the only
  way in and is private today).
- **The anvil.** The exclusive sets of the data say the same as the Enchanter's rules (tested), so the anvil refuses the
  combinations the Enchanter refuses. Whether the creative inventory lists enchanted books of the fourteen was not checked.
