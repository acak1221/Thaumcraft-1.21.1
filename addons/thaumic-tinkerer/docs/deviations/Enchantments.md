# Enchantments: deviations from Thaumic Tinkerer 1.7.10

Group Enchantments: the fourteen enchantments (Ascent Boost, Flaming Touch, Disintegrate, Dispersed Strikes, Final Strike,
Focused Strikes, Pounce, Quick Draw, Shatter, Shockwave, Slow Fall, Tunnel, Valiance, Vampirism), `ModEnchantmentHandler`,
the Enchanter's table of costs and rules (`EnchantmentManager`), the Osmotic Enchanter (block, block entity, menu, the two
packets, screen, renderer) and the fourteen research entries of the enchantments. Specification: the 1.7.10 sources in
`/home/user/gtnewhorizons/thaumictinkerer` (commit in `NOTICE.md`). Everything not listed here is ported as it was:
research keys, coordinates, aspects and parents, the costs of all 40 table rows, the formulas and their quirks (section 11),
the pillar rules, the recipe and the hardness of the Enchanter. Requests to the API and to the lead are in
`docs/api-requests/Enchantments.md`.

Reading key: **forced** means 1.21.1 cannot do it the old way; **fix** means the original crashed, lost data or was
exploitable; **choice** means a free decision of the port; **limit** means something the port cannot reproduce exactly.

Status: the group was written and compiled (`tools/compile.sh`, with and without `-g`); its GameTests (`EnchantmentsGameTests`,
Fabric, headless server with mock players and the fake player of the Animation Tablet) and its JUnit tests have since been run
(the exact runs are in the report that came with this revision). The effects of the damage, dig, jump, fall and bow enchantments
now go through the loader events of `docs/HOOKS.md` (sections 2, 3, 4, 9 and 13 here); the part that only a client can show is
not seen. Section 12 lists what is unverified.

## 1. The enchantments are data (forced)

| 1.7.10 | 1.21.1 |
|---|---|
| `Enchantment` subclasses with numeric ids from the config (220 to 225 and 166 to 173) | one JSON file per enchantment, `data/thaumictinkerer/enchantment/<name>.json`; identity is the registry key `thaumictinkerer:<name>`. `LibEnchantIDs` keeps the 1.7.10 field names but holds `ResourceKey<Enchantment>`; the ids of the config are gone |
| `EnchantmentMod(id, maxLevel, type)`, weight 0, `canApplyAtEnchantingTable = false` | `max_level` of the file (Ascent Boost 4, Slow Fall 3, Flaming Touch 1, Disintegrate 1, Quick Draw 2, Vampirism 2, the other seven 5), weight 1 (the data cannot say 0; the weight only matters for the random picks the enchantments are out of), **`anvil_cost` 0** (see below) and `effects: {}`; the enchantments are in none of `in_enchanting_table`, `tradeable`, `on_random_loot` (so, as in 1.7.10, only the Enchanter gives them), and they are in `minecraft:treasure` |
| `EnumEnchantmentType` (weapon, digger, bow, armor_legs, armor_feet) and `canApply` overrides | the item tags `data/thaumictinkerer/tags/item/enchantable/<name>.json`: `#minecraft:swords` (Vampirism, Final Strike, Valiance, Focused and Dispersed Strikes), `#minecraft:pickaxes/shovels/axes` (Shatter, Tunnel, Disintegrate), `#minecraft:leg_armor` (Ascent Boost, Pounce), `#minecraft:enchantable/bow` (Quick Draw), explicit boot lists (Slow Fall, Shockwave), explicit axes plus `#minecraft:pickaxes/shovels` (Flaming Touch). Items of Thaumcraft and of this add-on are listed with `required: false`, among them the items the 1.7.10 types accepted and no tag of the base mod lists: the Crimson Blade (`thaumcraft:crimson_sword`, a sword) in the five sword tags, the Primal Crusher (`thaumcraft:primal_crusher`, a tool) in the four digger tags, the leggings of the Crimson Cult in Ascent Boost and Pounce, the boots of the Crimson Cult in Slow Fall and Shockwave |
| `canApplyTogether` of each enchantment | `exclusive_set` of the file, and the rules of `EnchantmentManager` (section 5); both are asked by the Enchanter, only the exclusive sets by an anvil |
| `canApply` excluding the Elemental Axe (Flaming Touch), the Cursed Spirit's Blade (Vampirism), the gem boots (Slow Fall, Shockwave) | the tags simply do not list them (the gem boots are in `#minecraft:foot_armor`, so the boot tags are explicit lists); `EnchantmentManager.allowsItem` repeats the exclusion in code, for a data pack that adds them |

**The anvil (choice).** `EnchantmentMod` called `super(id, 0, type)`: weight 0. The 1.7.10 anvil turns the weight into a cost per
level with a `switch` (as recalled, not re-read: 1 gives 8, 2 gives 4, 5 gives 2, 10 gives 1, anything else 0) and halves it for an
enchanted book with a minimum of 1, so the fourteen cost nothing per level when they come from a tool and 1 per level when they
come from a book.
`anvil_cost` 0 gives the same numbers in 1.21.1 (`max(1, 0 / 2)` for a book). The vanilla anvil of 1.21.1 returns no result when
the sum of the enchantment costs is 0 (read in the decompiled `AnvilMenu.createResult`; the 1.7.10 `ContainerRepair` is not at
hand here, and the review recalls the same rule), so two tools cannot be merged to move one of the fourteen, a book can. The
JUnit test `EnchantmentResourcesTest` asserts the 0 with the real codec, and the GameTest
`theAnvilChargesOnePerLevelForABookAndNothingForAToolOfTheEnchantmentsOfTheAddOn` asks a real `AnvilMenu` (not run).

The names are the lang keys `enchantment.thaumictinkerer.<name>` of the converted 1.7.10 lang files (checked by
`EnchantmentResourcesTest` for en_us and ru_ru). No new lang key was needed, so there is no `lang-extra/enchantments.*.json`.

Consequences kept in mind: Mending, Sweeping Edge, Depth Strider, Soul Speed and the other enchantments of later versions
are not in the table (the table is the 1.7.10 one, section 4), so the Enchanter does not offer them. The numeric ids of
1.7.10 are not migrated; neither are 1.7.10 items or worlds.

## 2. Damage: Pounce, Final Strike, Valiance, Focused and Dispersed Strikes (loader event `LIVING_DAMAGE`)

1.7.10 changed `event.ammount` in `LivingHurtEvent`. Architectury has no such event, so the group first armed the attack of a
player through an attribute modifier (`AttackBoost`). That workaround is gone: `ModEnchantmentHandler.onEntityDamaged` is a
listener of `TinkererEvents.LIVING_DAMAGE` (`docs/HOOKS.md` section 3), which hands the amount to the listener and takes the new
one back. It runs for **every** damage that has a living attacker (`source.getEntity()`, the entity that caused the damage:
the shooter of an arrow, the wearer of thorns), as the original did: the blow of a player, of the Animation Tablet's fake player
(`TinkererFakePlayer.attackEntity`), of a mob with an enchanted item in its hand, an arrow, thorns, a potion. The attacker's
**main hand** is read (the original had no other hand), and an empty hand ends the handler, Pounce included, as in the original.

- **The numbers are the original's**: Pounce `x(1 + .25 L)` when the block under the feet is air (a player's leggings only, as
  in the original; the server's `posY` of 1.7.10 was the height of the feet), Final Strike `x3` when `L > nextInt(19)`,
  Valiance `x(1 + .1 L)` below half health of the attacker, Focused Strikes `/2` then `+= .5 * max(s, 5) * amount * L`,
  Dispersed Strikes `x(1 + .6 L + s/5)` then `/(1 + .25 L + 2.5 s)` (integer division in `s/5`), in this order, with the
  counter `s` of the original (section 11), all in `EnchantmentFormulas.modifyDamage` with the narrowing to `float` after each
  step that the compound assignments of Java did. Final Strike's roll comes from the attacker's own random source instead of a
  fresh `new Random()`; the distribution is the same.
- **Where the event stands, and what that changes** (`docs/HOOKS.md` section 3). 1.7.10 posted `LivingHurtEvent` in
  `damageEntity`, after the invulnerability frames had taken the last hit off (`damageEntity(source, amount - lastDamage)`) and
  before the armour. The new event comes before the frames compare the hit with the last one, before the shield, the helmet,
  the armour, Protection, Resistance and Absorption. So:
  - a blow inside the frames of the victim arrives with its **whole amount**, the factors multiply that, and vanilla then
    compares the **listener's result** with the last hit (which vanilla stored as the boosted amount, `f a1` for a first raw
    blow `a1`) and deals the difference. 1.7.10 compared the **raw** amounts (`lastDamage = a1`) and handed its event the
    difference `a2 - a1`. For a factor `f` that is the same on both blows (Pounce, Valiance, Focused Strikes, whose counter is
    floored at 5) the two agree when the second raw blow `a2` is above both bars (`a2 > max(a1, f a1)`: both deal
    `f (a2 - a1)`) and when it is below both; **in between they differ, in a band**:
    - `f > 1` and `a1 < a2 <= f a1`: 1.7.10 dealt `f (a2 - a1)`, here the blow is **refused** (it is not posted: it is not above
      the last hit as vanilla stored it). Pounce IV (`f = 2`): a blow of 10 is dealt as 20; a blow of 15 inside the frames
      dealt 10 more in 1.7.10 and deals nothing here; a blow of 25 deals 30 in both.
    - `f < 1` (Dispersed Strikes I at a counter of 5 or more: 0.19 of the blow, and less as the counter grows) and `f a1 < a2 <= a1`: 1.7.10 refused
      the blow before its event, here it is **posted** (vanilla's test compares it with the boosted `f a1`), so the listener
      runs: **the strike counter moves and Vampirism heals** for a blow that then deals nothing, because its result `f a2` is
      not above `f a1` and vanilla refuses it. A counter step and a heal without damage, in that band only.

    With the random Final Strike and with Dispersed Strikes, whose factor moves with the counter, there is no constant `f`: a
    tripled hit raises the bar to `3 a1`, and the plain hits that follow inside its frames are refused where 1.7.10 would have
    let a plain difference through. Both bands are pinned by the GameTest `theFramesCompareTheBoostedAmountsNotTheRawOnes`
    (Pounce IV; Dispersed Strikes I at a counter of 6).
  - a hit that the frames refuse **by its raw amount** (`amount <= lastHurt` inside the frames, `lastHurt` being what vanilla
    stored: the listener's result for the hit before) is **not posted** (the poster applies vanilla's test first), so it does not
    move the counter of Focused and Dispersed Strikes and does not heal with Vampirism. That is what 1.7.10 did too, except in
    the band above for a factor below one. A hit that a shield blocks or a helmet reduces is posted with its full amount and may
    be refused afterwards; the counter and the heal have happened then. (1.7.10 had no shields and no helmet rule; `docs/HOOKS.md` section 3 puts its event after the
    blocking, which for a player victim that blocked with a sword was `(1 + amount) / 2` before the factors, and now it is not
    applied at all. A player victim that holds a sword up is the one case in which the factors of the enchantments meet a
    different amount than in 1.7.10.)
  - the listener never cancels, so no frames are lost; other listeners (Soul Hearts of the KamiWorld group) chain in the order
    of registration, each seeing the amount the one before returned.
- **What the workaround could not do and the event does** (all of them were limits written down here before): the factors
  apply to mobs, arrows, thorns and the Animation Tablet's fake player; there is no dependence on the shape of the 1.21.1 attack
  formula; a critical hit is multiplied like anything else because the event comes after `Player.attack` has worked it out; the
  counter moves when the damage is about to land, not when the attack starts.
- **A sweep is a hit for each entity it reaches** (new in Minecraft 1.9, so no 1.7.10 counterpart; read in the bytecode of
  `Player.attack`, pinned by the GameTest `aSweepOfASwordIsAHitForEachEntityItReaches`). A full-strength swing of a sword by a
  player on the ground (not a critical hit, not a sprint knock-back) hurts the target, then calls `LivingEntity.hurt` with the
  **same `playerAttack` source** once for every other living entity in the target's box grown by a block to each side that is
  within three blocks of the attacker (allies of the attacker and marker armour stands are skipped), and the event is posted for
  each of them. The listener cannot tell a swept entity from the target (the source is the
  same and the event carries nothing else), so it answers for each as for a blow of its own:
  - **Vampirism heals by its level once for each entity hurt** and plays the zap at each (a swing that hurts two pigs heals
    twice, a swing at a lone pig once, as in 1.7.10);
  - the factors multiply the sweep damage of each swept entity (that damage is 1 plus the Sweeping Edge share of the blow, times
    the strength of the swing), **Final Strike rolls its dice separately for each**;
  - **the strike counter follows the last entity of the swing**: the remembered target becomes the last swept entity and the
    counter starts again at 0, so a streak of Focused or Dispersed Strikes on one target of a crowd is broken by every sweep that
    reaches somebody else (1.7.10 had one target, one heal and one counter step for each swing).
  This was kept as it is (**choice**): a filter would need the target of the swing, which neither the damage source nor the event
  carries, and a swept entity is hurt by the sword like the target is.
- **The fake player's health is fixed** (`TinkererFakePlayer.setHealth` does nothing), so Valiance never sees a hurt fake
  player; every other factor works for it (the GameTests use Focused Strikes and Pounce).
- The listener changes the weapon (the counter on its data component) in the server's call. The event is posted on the server
  only, so nothing needs a side test.

## 3. Vampirism (now in the damage listener)

Heals the attacker by the level (any living attacker, as in 1.7.10) and plays `thaumcraft:zap` at the victim, in the same
listener of `LIVING_DAMAGE` as the factors of section 2 and after them, as `onEntityDamaged` of the original did. Once for each
entity the hit reaches: the entities swept by a sword each heal as well (section 2, "A sweep is a hit for each entity it reaches"). The workaround
(`EntityEvent.LIVING_HURT`, posted at the start of `LivingEntity.hurt`, with a list of conditions that guessed whether the blow
would deal damage: not dying, not invulnerable, not inside the frames) is gone: the new event is posted for a hit that the game
goes on to deal, and not for the ones it ignores (dead or invulnerable victim) or refuses by their raw amount (the frames), as
1.7.10's event was (with the band of section 2, where a factor below one posts a blow that 1.7.10 refused: it heals there). A hit that a later listener cancels, or that a shield blocks, still heals (all the handlers of 1.7.10 ran as well).

## 4. Movement and shooting enchantments (loader events `LIVING_JUMP`, `LIVING_FALL`, `USE_ITEM_TICK`)

All four are listeners of `TinkererEvents` (`docs/HOOKS.md` sections 5 to 7), registered once on both sides: Slow Fall, Shockwave and Quick Draw by
`ModEnchantments.initEnchantments()`, which `Enchantments.register()` calls, and Ascent Boost's by `ModEnchantments.initJump()`, which `Enchantments.ready()`
calls (see the Ascent Boost bullet). The events that run on both sides run their listener on both, so the rule of every listener that changes a
number both sides compute is the same on both; the one with an effect on the world (Shockwave) acts on the server only.

- **Ascent Boost** (`LIVING_JUMP`). The motion of the jump, set just before the event, is multiplied by `(L + 2) / 2` (the
  highest level on the four armor slots, not while sneaking, players only), as `onPlayerJump` did. The client handler that
  guessed the jump from the tick of the local player (`EnchantmentClientHandler`) is deleted; the server's player gets the
  boost too, from the same event (a real server player jumps from the movement packet, `HOOKS.md` section 5), so the server does
  not expect a lower jump than the client makes. As HOOKS.md says, the server also posts it for any other upward take-off of a player
  that the client reports as leaving the ground. **Order against the Gem Boots**: the listener is registered in the ready phase
  (`Enchantments.ready()` → `ModEnchantments.initJump()` → `ModEnchantmentHandler.registerJump()`), after every group's `register()`, so it runs
  after the Gem Boots' listener, which `KamiGear.register()` registers; that is the order of 1.7.10 (boots in `preInit`, `ModEnchantmentHandler` in
  `init`): the boots add their 0.3 and Ascent Boost multiplies the sum, 1.08 for Ascent Boost I and 1.80 for III (`docs/deviations/KamiGear.md` section 3;
  the GameTest `gemBootsAndAscentBoostLeggingsComposeTheirJumps` pins the numbers). The first wiring registered it in `register()`, before KamiGear's, which
  reversed the order.
- **Slow Fall** has two parts, as `onEntityUpdate` had one block. (1) *The motion and the cloud*: on the client that simulates
  the fall of its own player, `TickEvent.PLAYER_PRE` divides the vertical motion by `1 + .33 L` (the float literal of the
  original) and spawns the cloud while the player falls faster than `-0.0784`, for 2.9 blocks or more, not sneaking, with the
  highest level on the armor; this is the `LivingUpdateEvent` of 1.7.10 on a real per-tick event, not a workaround. (2) *The fall
  distance*, on both sides, in `LIVING_FALL`: the original lowered it **every tick** of the fall
  (`fallDistance = Math.max(2.9F, fallDistance - L / 3F)`), which ends, however long the fall, at the floor of 2.9, a fall that
  does no damage. The event comes **once, at the landing**, so the listener applies that end result at once: a landing of 2.9
  blocks or more, for a player with Slow Fall on the armor who does not sneak at the landing, is a landing of 2.9
  (`EnchantmentFormulas.slowFallLanding`). The single step `max(2.9, d - L/3)` would leave a fall of 100 blocks at 99 and kill
  the wearer, which is why it was not taken. The old server-side tick rule (`PLAYER_PRE`, fall distance only) and the client's
  own per-tick reduction of the distance are gone; the event is the one place that touches the distance. **Differences** (by a
  reading of the formulas, section 12): a fall of 3 to 4 blocks, which the original may have left at 3.0 to 3.5 because the player
  landed before the rule had run for many ticks, does no damage here; the level of the enchantment no longer matters for the distance (it did only in how fast the floor was
  reached); sneaking matters at the moment of the landing; the unreduced distance still goes to `Block.fallOn` (the chance of
  trampling farmland) as it is the argument of that call, the event only lowers what `causeFallDamage` works the damage out
  from. As for every `LIVING_FALL` listener, a rider of a horse, donkey, mule or llama is not served (`HOOKS.md` section 6).
- **Shockwave** (`LIVING_FALL`, server only). A player whose **boots** carry it, landing from more than three blocks (the
  distance the event carries, after Slow Fall's listener, which is registered first because its effect in the original had been
  done tick by tick before the landing), hurts every other living entity in the box of 10 blocks to every side by `.1 L d` fall damage. The
  listener tests `level.isClientSide` and does nothing on the client (the event is posted there too). A player that can fly
  never reaches the event (`Player.causeFallDamage` refuses first), as `EntityPlayer.fall` did not run for it in 1.7.10. The
  tracking of the last airborne tick of every server player and the guess about water and teleports are gone: a landing in
  water or by a teleport is not posted by the game in the first place. The passenger of a vehicle that lands is posted with the
  vehicle's distance, as in 1.7.10.
  - **Order against the listeners of other groups (limit, read, not run).** The chain of an event runs in the order of
    registration. Inside this group it is Slow Fall, then Shockwave (so the wave sees a fall of 2.9 blocks, never more, on boots
    that carry both: the GameTest `shockwaveHurtsTheCreaturesAroundALanding` puts both on one pair of boots, a combination that
    the Enchanter and the exclusive sets forbid, and sees no wave from ten blocks). Shockwave's `LOW` priority of 1.7.10 (after
    the handlers of every other mod) is **not reproduced against other groups**: Slow Fall's and Shockwave's listeners are registered in
    `Enchantments.register()` (through `ModEnchantments.initEnchantments()`) and the Gem Boots' `LIVING_FALL` listener (it sets the distance to
    0) in `KamiGear.register()`, and `ContentGroups.ALL` runs Enchantments before KamiGear, so the boots' listener comes after both of ours and Shockwave
    sees the distance before the boots zero it. (Only Ascent Boost's jump listener was moved to `Enchantments.ready()`, so that the boots come first
    for the jump, see the Ascent Boost bullet above; the fall listeners were not moved.) `docs/deviations/KamiGear.md` describes the same order. There is no live effect
    through the Enchanter or an anvil, because the boot tags of Slow Fall and Shockwave do not list the Gem Boots (section 1); a
    stack with both, made by a command or another mod, would blast. Closing it would take a later registration phase for Slow Fall and
    Shockwave or another order of the groups in `ContentGroups`, which is the lead's; nothing is done because the case has no live effect.
- **Quick Draw** (`USE_ITEM_TICK`). The draw clock is the entity's own `useItemRemaining`, which the bow pose, the HUD and
  the power of the arrow (`releaseUsing`) read, so the rule of the original changes the real thing: per tick
  `speedup = L / 6`, plus one when the clock is a multiple of `6 - L % 6`, and the clock becomes `Math.max(2, clock - speedup)`.
  The arrow-speed workaround (`EntityEvent.ADD`) and its two limits (the bow did not look drawn faster; a draw shorter than
  the minimum power released no arrow) are gone. **Where in the tick**: 1.7.10 ran this after the decrement of the tick, the
  event comes before it, so the rule is applied to `remaining - 1`, the count the decrement is about to leave, and one is added
  back (`EnchantmentFormulas.quickDrawRemaining`); the clock then goes through the same values as the original's (a JUnit test
  compares 400 ticks of both, the GameTest compares a real draw with a literal copy of the original handler). The level is that
  of the bow in use (in 1.7.10 the item in the hand, which was the item in use, so a bow drawn from the off hand is one more
  case that the original could not have); only bows (`BowItem`, which Thaumcraft's Bone Bow is too: it reads the same clock to
  release itself, so the shortened draw reaches its auto release as `itemInUseCount` did in 1.7.10; read in `ItemBowBone`, not
  tried), only players. A client runs the rule for its own copy of the player: it only reads the enchantments of the stack in
  use, which both sides have.

## 5. Rules of the Enchanter (choice)

`EnchantmentManager.rules` holds the `canApplyTogether` rules of the fourteen enchantments as they were (Slow Fall: not
Feather Falling; Flaming Touch and Disintegrate: only Unbreaking and Repair; Dispersed and Focused Strikes: Unbreaking,
Repair, Knockback and Looting; Final Strike and Valiance: Unbreaking, Repair, Sharpness and Smite; Quick Draw: not Punch;
Shatter: not Efficiency and Disintegrate; Tunnel: not Shatter and Efficiency; Shockwave: not Feather Falling and Slow Fall;
Vampirism: not Fire Aspect and Knockback). 1.7.10 asked both enchantments of every pair, so a rule of an enchantment that is
already chosen also forbids a new one; `rulesForbid` does the same. The exclusive sets of the JSON files say the same for an
anvil, and `EnchantmentResourcesTest` checks that the two agree for every pair of enchantments that fit the same kind of item
(swords, tools, boots, bows, leggings), Mending and Sweeping Edge included although the table does not list them.

The table has the 40 rows of `EnchantmentManager.initEnchantmentData` (24 vanilla enchantments, Haste and Repair of
Thaumcraft, the fourteen of this group), with their costs `level 1 aspects x level x (1 + .2 level)`, cut to whole numbers
per aspect, and their research. The research of the TT enchantments from Focused Strikes to Pounce in the table is
`TTENCH_VAMPIRISM` in the original, not the entry of their own name; this was kept (it is a copy-and-paste habit of 1.7.10).
Lure's and Luck of the Sea's `BEAST` cost is never charged because the Enchanter only collects the six primals, as before.

**Which items (choice, fidelity).** 1.7.10 asked `enchant.canApply(stack) && enchant.type.canEnchantItem(item)`: the narrow type
decided, not the wider overrides that let an anvil put Sharpness on an axe, Efficiency on shears or Thorns on any armor. The
Enchanter asks `Enchantment.isPrimaryItem` (the `primary_items` of the vanilla data: swords for Sharpness, Smite, Bane of
Arthropods and Fire Aspect, chest armor for Thorns; for the others, and for the fourteen, which have no primary items, the
supported items), and `EnchantmentManager.allowsItem` additionally keeps Efficiency, Fortune and Silk Touch off hoes and shears
(`ItemHoe` and `ItemShears` were no `ItemTool`; the 1.21.1 tags of these three list them). An anvil still follows the plain
`supported_items` of the data, as the 1.7.10 anvil followed the wider overrides. The vanilla enchantments reach the Cursed
Spirit's Blade (an `ItemSword` of 1.7.10 with Thaumium enchantability, it took every weapon enchantment but Vampirism), the
Crimson Blade and the Primal Crusher through `data/minecraft/tags/item/enchantable/{sword,sharp_weapon,fire_aspect,mining,mining_loot,durability}.json`
(optional entries; the blade stays out of `#minecraft:swords`, which the tag of Vampirism holds). The armor of the Crimson Cult
is in none of the vanilla armor tags of the base mod, so the vanilla armor enchantments do not go on it; that is a gap in
Thaumcraft's tags, not in this group's (the fourteen are listed explicitly, see section 1).

**Button order (choice).** The original listed the buttons in the iteration order of a `HashMap<Integer, ...>` keyed by the
enchantment ids, which for the small ids of 1.7.10 was close to ascending id. The port lists them in the order of the table
(vanilla armor, sword, tool and bow enchantments, Lure, Luck of the Sea, Haste, Repair, then the fourteen). At most sixteen are
shown, in one or two rows of eight, as before.

## 6. The Osmotic Enchanter

- **Block.** `BaseEntityBlock`, hardness 5.0, blast resistance 1200 (the 2000 of 1.7.10 in today's scale, the number of the
  enchanting table), stone sound, needs
  a pickaxe of any tier (`mineable/tile_blocks/pickaxe`, no `needs_*` tag), drops itself and drops the tool and the wand it
  holds when broken. The box is 12 pixels high like the enchanting table (`setBlockBounds(0, 0, 0, 1, .75, 1)`), the model is
  that box with the three original textures (`textures/block/enchanter_0..2.png`), `noOcclusion`.
- **Opening.** A right click opens the menu with or without an item in the hand, as `onBlockActivated` did. The screen is a
  `MenuType` (`ContainerEnchanter`, an extended menu that carries the position) instead of a GUI id.
- **Slots.** `SlotTool` (one non-stacking damageable item, no book; locked while it works) and `SlotWand` (a wand, not a
  staff, whose cap's base cost modifier is at most one) are the 1.7.10 slots. The block entity is a `WorldlyContainer` with no
  accessible face, so hoppers never touch it. Shift-click follows `transferStackInSlot`: books are refused, a wand goes to the
  wand slot, a tool to the tool slot, anything from the two slots goes to the inventory.
- **Choice of enchantments is kept on the server and sent with the block entity update packet.** The keys changed:
  `enchants` is a list of enchantment ids as strings (1.7.10 `enchantsIntArray` held numeric ids), `levelsIntArray`,
  `totalAspects`, `currentAspects`, `working` and `Items` keep their names; the slot list is the `ContainerHelper` format with
  1.21.1 stacks. A tag where the two lists differ in length is mended on loading (levels are cut or filled with 1).
- **The work runs on the server only (forced).** 1.7.10 ran `updateEntity` on both sides, so the client counted
  `currentAspects` itself, with its own random choice of aspect, and played the particles. The port ticks on the server
  and sends the state every other tick of the work, and at once for the last step, so the bars on the screen move in steps
  of two ticks.
- **Vis.** One whole vis per tick from the wand: an aspect that the job still needs and that the wand holds at least 100 of
  (that is one vis) is chosen at random, `WandHelper.consumeAllVisCrafting(wand, null, 1 of the aspect, true)` pays
  it (with the discount of the wand's cap, as the original's private copy of the method did), and `currentAspects` grows
  by one. The original ignored a failed payment; the port counts the unit only if the payment succeeded (it cannot fail with
  a slot that only takes caps that do not raise the cost). The pillar used for the effect is the one with the index of the
  chosen aspect among the aspects that could be paid, a quirk of the original that was kept.
- **Pillars.** Within 4 blocks horizontally, at the height of the table: two to twelve Obsidian Totems then a Nitor; the
  first six found in the order x then z. A field of fewer than six stops the work and loses the vis paid so far (the
  vis taken from the wand stays taken).
- **Effects (A8).** The essentia trail is `ThaumcraftFX.essentiaTrail` from the pillar to the table; the two magic runes of
  `Thaumcraft.proxy.blockRunes` are `ThaumcraftFX.blockRunes` (the server twin of the client call that 1.7.10 made, since the work runs
  on the server) at the same points (`pillar x, top - 0.75, pillar z` and `table x, y + 0.25, table z`) with the same purple
  (`0.3 + rand * 0.7` red and blue, no green), duration 15 and gravity `rand / 8`; the
  sounds are `thaumcraft:egidle` (one in five of the effects, at the pillar) and `thaumcraft:wand` when the job is done.
- **Packets (fix / hardening).** The payloads carry the block position and the enchantment's registry id. The server
  resolves the block entity only if the sender has the menu of exactly that enchanter open (the original trusted any
  position), refuses an enchantment that is not in the table, refuses to add to a tool that is enchanted already, and `start`
  needs a tool in the slot and clears `currentAspects` (the original kept the vis of a stopped job, so a job could be
  started half paid). Level changes clamp to `[1, max level]` of the registry's data as before.
- **Research gate.** `canEnchantmentBeUsed` is `ThaumcraftApiHelper.isResearchComplete(player, key)` for the table's research,
  on the server when the packet arrives and on the client when the buttons are listed.
- **Recipe and research** are the original's: the infusion recipe (instability 15, Magic 50, Energy 20, Eldritch 20, Void 20,
  Mind 10; enchanting table in the middle, five Obsidian Tiles, two Thaumium Ingots, Spellbinding Cloth) and the entry
  `ENCHANTER` (5, 4), complexity 5, parent `SPELL_CLOTH`, pages 0, 1, 2 and the recipe.

## 7. The screen

`GuiEnchanting` and its four buttons follow `GuiEnchanting` and `GuiButton*` of 1.7.10 pixel for pixel (positions, the frames to
the right of the screen, the level buttons, the six aspect bars drawn with the aspect colour and their tooltips with
`current/total`). Differences:

- **Buttons are rebuilt when something changes** (the tool, the choice, the offered list, `working`), where 1.7.10 rebuilt them
  every tick. The list of offered enchantments depends on the player's research, which is read on the client.
- **Icons** are the 1.7.10 textures `textures/enchant/*.png`, 32 by 32 (35 files) or 512 by 512 (8 files, the ones of this
  add-on); the original drew either onto 16 by 16 with the texture coordinates 0 to 1. `EnchantIcons` reads the width from the
  PNG header and does the same; the sizes are forgotten when the resources reload (F3+T, a resource pack with icons of another
  size), by a reload listener that `EnchantmentsClient` registers.
- **Names and tooltips** use the description component of the data (`enchantment.thaumictinkerer.<name>`; the vanilla and
  Thaumcraft enchantments their own) and the 1.7.10 keys `ttmisc.startEnchant`, `ttmisc.baseCost`, `ttmisc.clickToRemove`.
  The level numeral is `enchantment.level.<n>`, which exists up to 10 in the vanilla lang files.

## 8. Renderer

`RenderTileEnchanter` places the tool and the wand with the transforms of the 1.7.10 renderer (the tool lying on the table
at 0.7 scale, the wand tilted and floating with `cos(time/1000)` and `cos(time/500)`). The item renderer draws the model of
each item; 1.7.10 extruded the flat icon of the item with `ItemRenderer.renderItemIn2D`. The offsets were carried over by
arithmetic, not by looking at the result: they may need a correction when the first screenshot exists.

## 9. Mining: Shatter, Tunnel, Disintegrate, Flaming Touch (loader event `BREAK_SPEED`)

1.7.10 changed `event.newSpeed` in `PlayerEvent.BreakSpeed` and cancelled it for blocks the tool must not break. The group first
kept a transient modifier on the player's `BLOCK_BREAK_SPEED` attribute that fitted the block under the crosshair
(`BreakSpeedControl`, once per tick, with a one-tick lag and a `updateFor` call that the Animation Tablet had to make). That is
gone: `ModEnchantmentHandler.onGetHarvestSpeed` is a listener of `TinkererEvents.BREAK_SPEED` (`docs/HOOKS.md` section 4), which the
game posts inside `BlockState.getDestroyProgress(player, level, pos)` with the speed `Player.getDestroySpeed` worked out, on **both
sides** (the server's decision and the client's prediction use the same rule and the same numbers: the tool in the main hand, the
block's hardness, the direction on the tool and the player's yaw) and for a player that is never ticked, so **the Animation
Tablet's fake player digs with the enchantments with nothing called by the Tablet** (`docs/api-requests/Enchantments.md` section 5,
patches 1 and 2, are not needed). No crosshair, no lag, no attribute, nothing of other mods' modifiers multiplied in.

- **Factors** are the original's, in its order: Shatter `x3L` on blocks of hardness above 20 and `x.8` on the others; Tunnel
  `x(1 + .2 L)` when the player's yaw is within 50 degrees of the yaw remembered on the tool at the last break and `x.3` when it
  is not (nothing without a remembered yaw); Disintegrate on blocks of hardness 1.5 or less, and Flaming Touch on "wood", make
  the speed `Float.MAX_VALUE` (the block breaks at once, any block of them, any tool; the factor 1024 of the workaround is gone),
  and any other block cannot be dug with that tool: `EnchantmentFormulas.breakSpeed`, with the narrowing to `float` after each step.
- **"Cannot be broken"** is the cancelled event of the original. The contract of the event makes it a speed of zero, so the
  progress is **exactly zero** (1.7.10's `-1` speed gave a negative progress): the block never breaks and the crack does not grow,
  on both sides, in the server's dig ticks, on the finishing packet, and for the Animation Tablet, which breaks a block only when
  the progress reaches 1. The veto the workaround had in `BlockEvent.BREAK` (to refuse a break whose progress the modifier had
  not caught, and a client that claims to have finished) is **removed**: the vanilla server checks the finishing packet against
  the progress, which now says zero. What is not refused any more is a break that does not ask for the progress at all: a direct
  call of `ServerPlayerGameMode.destroyBlock` by some other mod, which the original did not refuse either. Creative players never
  ask the progress, so they are never refused, as in 1.7.10. Not posted for the bamboo cut by a sword (`HOOKS.md` section 13),
  which is not a tool of this group.
- **Wear.** 1.7.10 called `damageItem(1)` every time the speed event fired for a block that was theirs, that is every tick of the
  dig, which with a speed of `MAX_VALUE` is once or twice per block. The new event is posted on the client too, for the crack, for
  a dig-time display, for anything that asks, and wear there would be wrong, so the group keeps its own rule: **one more point of
  wear per block broken** with Disintegrate or Flaming Touch, taken in `BlockEvent.BREAK` (server), besides the point that breaking
  a block costs a pickaxe anyway. The GameTest `aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn` measures two points
  for a Disintegrate pickaxe and one for a plain one, through the real first click (`handleBlockBreakAction`).
- **Tunnel's direction** is remembered at the break (`BlockEvent.BREAK`, the same event as in 1.7.10), in the custom data of the
  tool; it reaches the client with the item, so the client's prediction has it.
- **Hardness** is read at the block's own position (the original asked `getBlockHardness(world, 0, 0, 0)` for Shatter, which for
  almost every block is the same number; a poster that does not know the position gives `BlockPos.ZERO`, as the original did).
- **"Wood"** (`Material.wood`) is a block with a wooden sound type (wood, nether wood, cherry wood, bamboo wood, stem) or one of
  the vanilla tags for logs, planks, wooden fences, doors, trapdoors, slabs, stairs and signs. 1.7.10's wood also held the
  bookshelf, chests, the crafting table and jukebox (all have the wooden sound).
- **Tunnel's yaw difference is wrapped** to 0..180 degrees. 1.7.10's `rotationYaw` was unbounded and the original compared the
  raw numbers; the 1.21.1 server stores the player's yaw wrapped to [-180, 180], where a raw difference would call two nearly
  equal yaws on either side of the seam 360 degrees apart (fix).
- **A listener of the dig speed on both sides** reads the yaw of the player, which the server has from the rotation packets and the
  client from its camera: they can differ by the last unsent turn, so near the 50 degrees of Tunnel the client's crack and the
  server's decision may disagree for a tick. Not seen.

## 10. Research entries

The fourteen entries are registered through `ResearchHelper.queue` from the group's `ready()`: secondary, complexity 2, one
text page `"0"`, at the coordinates, with the aspects and the parents of 1.7.10 `ResearchHelper.initResearch` (Ascent Boost
6/2, Slow Fall 7/3, Flaming Touch 7/5, Disintegrate 6/6, Quick Draw 4/6, Vampirism 3/5, Focused Strikes 2/7, Dispersed Strikes
1/6, Final Strike 0/8 with two parents, Pounce 7/0, Shatter 5/8, Shockwave 9/2, Tunnel 9/6, Valiance 1/4). The Enchanter's
own entry comes with its block (section 6). The `PERIPHERALS` entry of the same 1.7.10 method belongs to the Machines group.

## 11. Values checked against the original on purpose

- Strike counter: a new target (or the first) gives 0; the same target gives `max(counter + 1, 5)`, so 5 from the second blow on
  (the original wrote `max` where `min` was meant; kept). Keys `TTEnchantLastTarget`, `TTEnchantLastTarget2` (the two halves
  of the UUID), `TTEnchantSuccessiveStrike` on the weapon, in its custom data.
- Focused Strikes divides by two *before* adding; the added term uses `max(counter, 5)`.
- Final Strike's roll is `nextInt(19)`; level 5 wins 5 of 19.
- Slow Fall: threshold `2.9` as a `double` comparison (`fallDistance >= 2.9`) in the landing rule and in the client's per-tick
  condition, the floor `2.9F` that the original's per-tick `max(2.9F, d - L/3F)` ends at (the landing result), the motion limit
  `-0.0784000015258789`, the divisor `1 + L * 0.33` (the float literal of the original).
- Shockwave: `d > 3`, box of 10 to each side, damage `.1 L d`, the faller excluded.
- Ascent Boost: `(L + 2) / 2`, not while sneaking.
- Quick Draw: `speedup = L / 6`, plus one when the clock is a multiple of `6 - L % 6`, never below 2, on the count that is left
  after the decrement of the tick (the event comes before it: `remaining - 1`, one added back); the clock starts at 72000.
- Disintegrate: hardness `<= 1.5`; Flaming Touch: wood only; both make the speed `Float.MAX_VALUE`, and any other block a speed
  of zero; Tunnel: 50 degrees, `1 + .2 L` and `.3`; Shatter: hardness above 20 gives `3 L`, otherwise `.8`.
- Costs: `i * (1 + i * 0.2)` times the level-1 aspects; the amounts are cut to integers per aspect.
- The Enchanter's blast resistance and hardness: 5.0 and 1200 (2000 in 1.7.10).

## 12. What is not verified

- The GameTests of the group and the JUnit tests of `EnchantmentFormulasTest` (and the others) ran on Fabric; they show the
  **server side** of every event. **Not run, because a server cannot:** the client's copy of the events this group listens to,
  which `docs/HOOKS.md` section 12 lists for all of them: the client's prediction of the dig with Shatter, Tunnel, Disintegrate
  and Flaming Touch, the client's own jump with Ascent Boost, the motion and the cloud of Slow Fall on the local player (the
  `PLAYER_PRE` listener, which is client-only and so has no check at all) and the thud of a landing, the pose and HUD of a bow
  under Quick Draw, and the `isClientSide` branch of Shockwave. The listeners are plain code on shared events, with the same rule
  on both sides; a run of a real client with the enchantments would show it.
- The per-tick Slow Fall of 1.7.10 against the landing rule of section 4 was compared by reading the formulas, not by running
  the original (its jar is not run here): the claim that a long fall ends at 2.9 there is arithmetic, and the claim that a short
  fall of 3 to 3.5 blocks may keep some of its damage there is too.
- The 1.7.10 side of the two bands of the invulnerability frames (section 2: `EntityLivingBase.attackEntityFrom` compares the raw
  amounts and passes the difference to `damageEntity`) is taken from `docs/HOOKS.md` section 3 and recalled from that method (the
  Minecraft 1.7.10 sources are not at hand here), and the one-target swing of 1.7.10 is a fact of its version (no sweep); neither was
  run. The 1.21.1 side of both is run by `theFramesCompareTheBoostedAmountsNotTheRawOnes` and `aSweepOfASwordIsAHitForEachEntityItReaches`. A sweep with a Sweeping Edge
  sword (the share of the blow in the sweep damage), with Final Strike, and the heal of a swing at three or more entities were not tried.
- Nothing about the screen, the renderer, the particles and sounds, and the multiplayer behaviour of the packets has been seen.
- **Checks that changed with the wiring** (`EnchantmentsRuntimeChecks`), all of them because they drove the deleted workaround: the
  arming of the attack by `PlayerEvent.ATTACK_ENTITY` is gone with its subject, and its job (a blow of a player arrives multiplied) is
  in `valianceMultipliesTheWholeAttackAtLowHealth`, `theStrikeCounterIsKeptOnTheWeapon` and `pounceDependsOnTheBlockUnderTheFeet`,
  which now hit with real `hurt` and `attack` calls; the dig checks no longer set a crosshair or call `update`/`updateFor`
  (`desintegrateAndFlamingTouchChangeTheDigSpeedOfTheBlock`, `aPlayerThatIsNeverTickedDigsAtTheSpeedOfItsEnchantments`); the
  Slow Fall and Quick Draw checks land and draw for real (`slowFallMakesALandingAFallOfTwoPointNineBlocks`,
  `quickDrawTakesTicksOffTheDrawClockOfABow`); `aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn` proves the refusal on
  the real dig path (first click, ticks, finishing packet) with a plain pickaxe as the control, because a direct `destroyBlock` is no
  longer refused (section 9). `aFakePlayerAttackGetsTheEnchantmentsOfItsSword` is required now. The checks of the Enchanter, the data
  and the table are unchanged.
- **Checks added or tightened in the review round.** `aSweepOfASwordIsAHitForEachEntityItReaches` and
  `theFramesCompareTheBoostedAmountsNotTheRawOnes` pin what section 2 says about the sweep and about the two bands of the
  invulnerability frames (both run the real `Player.attack` / `LivingEntity.hurt` paths; the bytecode of `Player.attack` was read,
  the original was not run). `shockwaveHurtsTheCreaturesAroundALanding` clears the frames and the health of the pigs before every
  landing and ends with a positive control in the same state (before, the pigs of the refusals still had the frames of the first
  wave, and the game refused the small wave whatever the listener did: the check passed with the distance ignored), and puts Slow
  Fall and Shockwave on one pair of boots (the order of registration). `shatterAndTunnelScaleTheDigSpeed` breaks a block with a plain
  pickaxe and with a Shatter one and looks for a remembered direction. `aToolOfDisintegrateRefusesTheOtherBlocksAndWearsDoubleOnItsOwn`
  breaks cobblestone with a Disintegrate pickaxe and dirt with a Flaming Touch axe by a direct call and measures the plain point of
  wear (the extra point only for the blocks that are theirs).
- The JUnit tests of the other enchantment classes (`EnchantmentManagerTest`, `EnchantmentResourcesTest`,
  `EnchanterPacketsAndIconsTest`) test the table, the data and the packets.

## 13. What the group needs from the loaders

Fabric posts all five events (the mixins of `docs/HOOKS.md`). **NeoForge has no poster yet**, so on NeoForge the group needs the
poster of each event it uses, as `docs/HOOKS.md` sections 3 to 7 describe them, or its enchantments do nothing there:

| Event | What the group does with it | Without the poster |
|---|---|---|
| `LIVING_DAMAGE` | Pounce, Final Strike, Valiance, Focused and Dispersed Strikes, Vampirism | no damage factors, no heal |
| `BREAK_SPEED` | Shatter, Tunnel, Disintegrate, Flaming Touch | the tools dig at their plain speed and break anything |
| `LIVING_JUMP` | Ascent Boost | no boost |
| `LIVING_FALL` | Slow Fall (distance), Shockwave (blast, server only) | no Slow Fall landing, no wave |
| `USE_ITEM_TICK` | Quick Draw | the bow draws at its plain speed |

The group also keeps three Architectury listeners that exist on both loaders: `BlockEvent.BREAK` (Tunnel's remembered direction,
the wear of Disintegrate and Flaming Touch) and `TickEvent.PLAYER_PRE` (the motion and cloud of Slow Fall on the local client).
