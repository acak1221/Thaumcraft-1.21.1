# Enchantments: requests to the Thaumcraft API and to the loader modules

Group Enchantments: the fourteen enchantments, their event handler, the Enchanter's table of costs and rules and the Osmotic
Enchanter. Nothing below was done by reaching into
`thaumcraft.common.*`: every item is either a workaround with the loss written down (the behaviour of the group today is in
`docs/deviations/Enchantments.md`) or a request to the lead. Priorities are the group's own opinion (P1 changes what the
player gets, P3 only hardens something that works today).

The Thaumcraft API itself was enough for this group: `WandHelper` (`isWand`, `isStaff`, `getCap`, `getAllVis`,
`consumeAllVisCrafting`), `ThaumcraftApiHelper.isResearchComplete(Player, key)`, `ThaumcraftApi.ENCHANTMENT_HASTE` and
`ENCHANTMENT_REPAIR`, `BlocksTC.state(field, meta)`, `ItemApi`, `ThaumcraftFX.essentiaTrail` and `ThaumcraftFX.blockRunes` (the
rune effect of the Enchanter) and `ThaumcraftSoundIds` are used as they are. The requests are for the loaders: the group needs
five hooks that Architectury does not offer (sections 1 to 4 and 6) and works around each of them, and two changes in other
groups' files (section 5).

## 1. A damage event whose amount can be changed (loader event, P1)

**Need.** Pounce, Final Strike, Valiance, Focused Strikes and Dispersed Strikes change `LivingHurtEvent.ammount` of 1.7.10,
for any living attacker and any damage that carries an attacker (melee, arrows, thorns).

**Workaround.** `EntityEvent.LIVING_HURT` can only veto, so `AttackBoost` solves the attack formula of `Player.attack` and puts
a transient modifier on `ATTACK_DAMAGE` just before the attack (from `PlayerEvent.ATTACK_ENTITY`). It works for the blows of
players that go through the network event and for the code that calls `AttackBoost.beforeAttack` itself; it does not work for
mobs, arrows or thorns, and it depends on the shape of the 1.21.1 attack formula.

**Proposed.** A common event `LivingDamage { float onDamage(LivingEntity victim, DamageSource source, float amount) }` returning
the new amount (a cancel is an amount of zero), posted after the invulnerability test and before armor, with the backends
`LivingDamageEvent.Pre` (NeoForge, `setNewDamage`) and a `@ModifyVariable` on the amount of `LivingEntity.actuallyHurt`
(Fabric; Fabric API's `ServerLivingEntityEvents.ALLOW_DAMAGE` can only veto). With it `AttackBoost` is replaced by the
sixteen lines of the original.

## 2. A break-speed event (loader event, P1)

**Need.** Shatter, Tunnel, Disintegrate and Flaming Touch change `PlayerEvent.BreakSpeed.newSpeed` and cancel it (1.7.10).

**Workaround.** `BreakSpeedControl` keeps a transient modifier on the `BLOCK_BREAK_SPEED` attribute of the player that fits
the block under the crosshair, updated once per tick, and vetoes the break in `BlockEvent.BREAK`. The costs: one tick of lag
when the crosshair changes the kind of block, a fixed factor of 1024 for "at once", and the speed of every other mod's
modifier multiplies with ours.

**Proposed.** `PlayerEvent.BREAK_SPEED { EventResult onBreakSpeed(Player player, BlockState state, @Nullable BlockPos pos,
FloatRef speed) }` (cancel = the block cannot be dug); NeoForge `PlayerEvent.BreakSpeed`, Fabric a mixin at the end of
`Player.getDestroySpeed(BlockState)` (no position there; `Player.getDestroySpeed` is called from `BlockState.getDestroyProgress`,
which has it, so the mixin belongs one level up if the position is wanted).

## 3. Jump event (loader event, P2)

**Need.** Ascent Boost multiplies the vertical motion of a jump (`LivingJumpEvent`, on both sides).

**Workaround.** `EnchantmentClientHandler` detects a jump on the client (a tick that starts on the ground with the jump key
held and ends in the air moving up) and adds the rest of the multiplied jump afterwards. It sees only the key jumps of the local
player; a jump made some other way (from a mount, by a command) is not boosted, and nothing happens for other players' clients.

**Proposed.** `EntityEvent.LIVING_JUMP { void onJump(LivingEntity entity) }`, NeoForge `LivingEvent.LivingJumpEvent`, Fabric a
mixin at the end of `LivingEntity.jumpFromGround`.

## 4. Fall event (loader event, P2)

**Need.** Shockwave reacts to `LivingFallEvent` (distance greater than 3 blocks); Slow Fall could also use it to reduce the
distance.

**Workaround.** `ModEnchantmentHandler.onPlayerPost` remembers the fall distance of the last tick in the air of every server
player and treats the first tick on the ground as the landing. It is exact for walking and jumping falls, and it does not
react to a fall that another mod handles by setting the distance to zero in the same tick.

**Proposed.** `EntityEvent.LIVING_FALL { EventResult onFall(LivingEntity entity, float distance, float damageMultiplier) }`,
NeoForge `LivingFallEvent`, Fabric a mixin at the head of `LivingEntity.causeFallDamage`.

## 5. The Animation Tablet's fake player: attack and dig (shared files, to the lead, P1)

**Need.** The Animation Tablet attacks and digs with a fake player that is never ticked. In 1.7.10 both went through events the
enchantments hook: `attackTargetEntityWithCurrentItem` posted `LivingHurtEvent` (Valiance, Final Strike, Focused and Dispersed
Strikes) and `ItemInWorldManager`'s dig progress posted `PlayerEvent.BreakSpeed` (Shatter, Tunnel, Disintegrate, Flaming Touch).
In the port neither is posted for the fake player: `PlayerEvent.ATTACK_ENTITY` is a network event and the per-tick
`BreakSpeedControl.update` is a player tick, so a tablet with such a weapon hits at the base damage and a tablet with such a tool
digs at the plain speed of the tool (the veto of `BlockEvent.BREAK` already applies to it). Nothing outside the enchantment
package calls `AttackBoost` or `BreakSpeedControl` today.

**What exists.** `AttackBoost.beforeAttack(Player, Entity)` / `afterAttack(Player)` (safe for a player without enchantments)
and `BreakSpeedControl.updateFor(ServerPlayer, BlockPos target)` (fits the dig speed to the block at the position, with no
crosshair and no lag; safe without enchantments).

**Patch 1: `common/core/fakeplayer/TinkererFakePlayer.java`, `attackEntity`** (after the attack strength is set, so the factor is
worked out for a charged swing):

```java
public void attackEntity(Entity target) {
    refreshHeldItemModifiers();
    this.attackStrengthTicker = CHARGED;
    AttackBoost.beforeAttack(this, target);
    try {
        this.attack(target);
    } finally {
        AttackBoost.afterAttack(this);
    }
}
```
(import `thaumic.tinkerer.common.enchantment.AttackBoost`).

**Patch 2: `common/block/tile/tablet/TileAnimationTablet.java`**, the line `BreakSpeedControl.updateFor(player, pos);` right before each
`state.getDestroyProgress(player, level, pos)`: in `startBreaking` (before the `if`) and in `updateBlockRemoving` (before
`float progress = ...`); import `thaumic.tinkerer.common.enchantment.BreakSpeedControl`. (The Remote Placer does not dig or
attack, so `TileRPlacer` needs nothing.)

**Checks.** `EnchantmentsRuntimeChecks.aFakePlayerAttackGetsTheEnchantmentsOfItsSword` fails until patch 1 is in (its Fabric
wrapper is `required = false`; make it required afterwards). `aPlayerThatIsNeverTickedDigsAtTheSpeedOfItsEnchantmentsAfterUpdateFor`
covers `updateFor` itself and passes without the patches; a test that lets a real tablet dig with a Disintegrate shovel (the
dirt in front gone within a few ticks of the first hit) belongs to the group that owns the tablet once patch 2 is in. Neither
patch was applied or run by this group: they touch another group's files.

## 6. A bow draw clock (loader event, P3)

**Need.** Quick Draw takes extra ticks off `itemInUseCount` while a bow is drawn, so the draw is faster and the bow looks drawn
sooner.

**Workaround.** The arrow gets the speed of the longer draw when it is created (`EntityEvent.ADD`), computed with the original
per-tick simulation. The bow does not look drawn any faster, and a draw shorter than the game's minimum power (0.1) releases no
arrow, because the game decides that before the arrow exists.

**Proposed.** `LivingEntityEvents.USE_ITEM_TICK { int onTick(LivingEntity entity, ItemStack stack, int remainingTicks) }` returning
the new count (NeoForge `LivingEntityUseItemEvent.Tick#setDuration`, Fabric a mixin at the end of
`LivingEntity.updateUsingItem`). Only `getTicksUsingItem` reads it back, so the bow pose follows.

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
