# Enchantments: requests to the Thaumcraft API and to the loader modules

Group Enchantments: the fourteen enchantments, their event handler, the Enchanter's table of costs and rules and the Osmotic
Enchanter. The numbers `A7` and `A8` are those of `docs/API_GAPS.md` section 2. Nothing below was done by reaching into
`thaumcraft.common.*`: every item is either a workaround with the loss written down (the behaviour of the group today is in
`docs/deviations/Enchantments.md`) or a request to the lead. Priorities are the group's own opinion (P1 changes what the
player gets, P3 only hardens something that works today).

The Thaumcraft API itself was enough for this group: `WandHelper` (`isWand`, `isStaff`, `getCap`, `getAllVis`,
`consumeAllVisCrafting`), `ThaumcraftApiHelper.isResearchComplete(Player, key)`, `ThaumcraftApi.ENCHANTMENT_HASTE` and
`ENCHANTMENT_REPAIR`, `BlocksTC.state(field, meta)`, `ItemApi`, `ThaumcraftFX.essentiaTrail` and `ThaumcraftSoundIds` are
used as they are. The requests are for the loaders: the group needs five hooks that Architectury does not offer (sections 1
to 4 and 6) and works around each of them, and one change in another group's file (section 5).

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

## 5. The Animation Tablet's fake player and the attack factor (to the lead, P2)

**Need.** The Animation Tablet attacks with a fake player. Enchantments of damage (section 1) are armed by
`PlayerEvent.ATTACK_ENTITY`, which the fake player's `attack` does not post.

**What exists.** `thaumic.tinkerer.common.enchantment.AttackBoost.beforeAttack(Player attacker, Entity target)` and
`AttackBoost.afterAttack(Player attacker)`. Wrap the tablet's `fakePlayer.attack(target)` as
`AttackBoost.beforeAttack(fakePlayer, target); try { fakePlayer.attack(target); } finally { AttackBoost.afterAttack(fakePlayer); }`.
Both are safe to call for a player without enchantments. This is a change in the Machines or TileBlocks group's file, so it
is a request, not something this group did. Without it a tablet's attacks run at the base damage (no Valiance, Pounce, Final,
Focused or Dispersed Strikes, as if the sword had none of them).

## 6. A bow draw clock (loader event, P3)

**Need.** Quick Draw takes extra ticks off `itemInUseCount` while a bow is drawn, so the draw is faster and the bow looks drawn
sooner.

**Workaround.** The arrow gets the speed of the longer draw when it is created (`EntityEvent.ADD`), computed with the original
per-tick simulation. The bow does not look drawn any faster, and a draw shorter than the game's minimum power (0.1) releases no
arrow, because the game decides that before the arrow exists.

**Proposed.** `LivingEntityEvents.USE_ITEM_TICK { int onTick(LivingEntity entity, ItemStack stack, int remainingTicks) }` returning
the new count (NeoForge `LivingEntityUseItemEvent.Tick#setDuration`, Fabric a mixin at the end of
`LivingEntity.updateUsingItem`). Only `getTicksUsingItem` reads it back, so the bow pose follows.

## 7. Client sparkles and runes (A8, P3)

**Need.** The Enchanter draws two runes (`Thaumcraft.proxy.blockRunes`) at the pillar's top and above the table for every vis it
takes.

**Workaround.** Vanilla `ENCHANT` particles sent by the server (`ServerLevel.sendParticles`) around the same two points, and
`ThaumcraftFX.essentiaTrail` for the stream from the table to the pillar. The glyphs of Thaumcraft's rune effect are not shown.

**Proposed.** `thaumcraft.api.client.ThaumcraftClientFX.blockRunes(Level, double x, double y, double z, float r, float g, float b,
int life, float gravity)` as in A8, with a server-side sender in `ThaumcraftFX` (the Enchanter works on the server only).

## 8. Typed access to the Enchanter's pillar blocks (P3)

**Need.** The Enchanter looks for columns of two to twelve Obsidian Totems under a Nitor.

**Workaround.** `BlocksTC.state("blockCosmeticSolid", 0)` (the totem) and `BlocksTC.state("blockAiry", 1)` (the Nitor), both
resolved by the legacy field and metadata table, cached on first use. The totem is compared as a block state, the Nitor as a
block. This breaks if the table loses those two rows, with no compile-time warning.

**Proposed.** `BlocksTC.obsidianTotem()` returning the block state and `BlocksTC.nitor()` returning the block, or the block tags
`thaumcraft:obsidian_totem` and `thaumcraft:nitor` (tags would also let a resource pack add pillar blocks).

## 9. Not requested, for the record

- **Applied Energistics 2 `IMovableTile`** of the Enchanter (`prepareToMove` / `doneMoving`) is not ported (no AE2 in the 1.21.1
  build).
- **Enchantments of Thaumcraft and of later Minecraft versions.** The Enchanter's table is the 1.7.10 one: the 24 vanilla
  enchantments of that version, Haste, Repair and the fourteen of this group. Mending, Sweeping Edge, Depth Strider, Soul Speed
  and the rest are not offered. An API that lets Thaumcraft or another add-on register a row (key, icon, level-1 cost, research,
  rules) would remove the closed table; `EnchantmentManager` has the shape for it (`registerExponentialCostData` is the only
  way in and is private today).
- **The anvil.** The exclusive sets of the data say the same as the Enchanter's rules (tested), so the anvil refuses the
  combinations the Enchanter refuses. Whether the creative inventory lists enchanted books of the fourteen was not checked.
