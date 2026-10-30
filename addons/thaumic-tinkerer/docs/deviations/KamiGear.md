# KamiGear: deviations from Thaumic Tinkerer 1.7.10

Group KamiGear: Ichor resources, the Ichorcloth wand rod and cap, the eight Ichorium tools, the eight Ichorcloth armour pieces,
the Feline Amulet, the Bottomless Pouch, the Black Hole Ring, the Worldshaper's Looking Glass (Placement Mirror), the Protoclay,
the Celestial Pearl and the Foci of Shadowbeam, Experience Drain and Celestial Recall. The pinned 1.7.10 source is
GTNewHorizons/ThaumicTinkerer at `28f4def9...`. Only what differs from that source is listed; everything else (numbers, costs,
recipes, research keys and positions, aspects, tooltips, sounds, config use) is ported as it is.

How this was checked (honestly): the research entries (key, aspects, column, row, complexity, warp, parents) and all 30
recipes (instability, aspects) were compared with the 1.7.10 source by script, the ingredient lists of a dozen recipes by hand;
the add-on compiles with `tools/compile.sh` (also with `-g`, which adds the GameTest sources); the pure logic and the data files are
covered by 31 JUnit tests (KamiGear's 30 and `TinkererApiOnlyTest`) that were run with the real JUnit Jupiter launcher from the
Gradle cache, without Gradle (since the loader events of section 13 the group has 34 JUnit tests and 36 GameTests, the numbers below
are those of the first round). The 30 GameTests (`KamiGearRuntimeChecks`) were first run on a real Fabric dedicated server in the
integration round (`-PtinkererGameTestFilter='kamigeargametests.*'`): 23 passed, 7 failed, and all 7 were wrong expectations or
set-ups of the checks, not defects of the implementation (the mock player of `GameTestHelper.makeMockServerPlayerInLevel` is in
Creative mode, so the checks now set Survival; `GameTestHelper.relativePos` is not the inverse of `absolutePos` for an unrotated
test; a pouch menu is opened for the *selected* hotbar slot; the target of a piglin is its `ATTACK_TARGET` brain memory, which
`AbstractPiglin.getTarget` reads, not the field that `Mob.setTarget` writes). After the corrections all 30 pass. Nothing
here has been seen in a running client.

## 1. Identity: registry paths, components, models

| 1.7.10 | 1.21.1 |
|---|---|
| `kamiResource` metadata 0..7 | eight items, in the original order: `ichor`, `ichor_cloth`, `ichorium`, `ichor_nugget`, `ichor_cap`, `ichorcloth_rod`, `nether_shard`, `ender_shard` (`LibItemNames.KAMI_RESOURCE_NAMES`) |
| armour `ichorcloth{Helm,Chest,Legs,Boots}`, `...Gem` | `ichorcloth_helm`, `_chest`, `_legs`, `_boots` and the same with `_gem` |
| tools `ichorPick/Shovel/Axe/Sword` and `...Gem` | `ichor_pick`, `ichor_shovel`, `ichor_axe`, `ichor_sword` and the same with `_gem` |
| `catAmulet`, `ichorPouch`, `blockTalisman`, `placementMirror`, `protoclay`, `skyPearl` | `cat_amulet`, `ichor_pouch`, `block_talisman`, `placement_mirror`, `protoclay`, `sky_pearl` |
| `focusShadowbeam`, `focusXPDrain`, `focusRecall` | `focus_shadowbeam`, `focus_xp_drain`, `focus_recall` |
| damage value 0/1/2 of an awakened tool (mode) | component `thaumictinkerer:kami_tool_mode` (0..2). The item model predicate is `thaumictinkerer:kami_mode` = mode / 2 (0, 0.5, 1) because predicates are clamped to 0..1 |
| damage value 1 of an awakened armour piece (switched off) | component `kami_piece_disabled` (boolean) |
| ring: `blockName`, `blockMeta`, `blockCount`; damage value 1 (active) | components `kami_talisman_store` `{block, count}` and `kami_talisman_active`; model predicate `thaumictinkerer:kami_active` |
| mirror: `blockName`, `blockMeta`, `size` | components `kami_mirror_state` (the whole block state: metadata no longer exists) and `kami_mirror_size` |
| pearl: `x`, `y`, `z`, `dim` | component `kami_pearl_target` `{dimension, pos}` |
| pouch: tag `Inventory` | component `kami_pouch_contents` (`ItemContainerContents`, 117 cells) |

The wand parts keep their 1.7.10 tags `ICHOR` (cap) and `ICHORCLOTH` (rod), so the research keys `CAP_ICHOR` and `ROD_ICHORCLOTH` and
the language keys `item.Wand.ICHOR.cap` / `item.Wand.ICHORCLOTH.rod` are those of the original. The 1.7.10 constructors
`CapIchor(String)` and `RodIchorcloth(String)` (a "patch for changing the name" that registered a second cap and rod from the
metadata 10 and 9, which do not exist) are not ported.

The creative-tab wand with the Ichor rod and cap and 100000 centivis of every primal is registered with `TTRegistry.addTabStack`.

### Armour textures (copied, recorded)

`scripts/porting/tt_assets.py` converts the 1.7.10 armour sheets to `textures/models/ichor_1.png`, `ichor_2.png`, `ichor_gem_1.png`
and `ichor_gem_2.png`. 1.21.1 names the worn layers of an armour material by path
(`textures/models/armor/<material>_layer_1.png` for the helmet, chestplate and boots, `_layer_2` for the leggings), so the four
sheets were **copied by file copy** (bytes identical, checked by `KamiGearResourcesTest`) to:

| Source (converted) | Copy |
|---|---|
| `assets/thaumictinkerer/textures/models/ichor_1.png` | `.../textures/models/armor/ichor_layer_1.png` |
| `.../models/ichor_2.png` | `.../models/armor/ichor_layer_2.png` |
| `.../models/ichor_gem_1.png` | `.../models/armor/ichor_gem_layer_1.png` |
| `.../models/ichor_gem_2.png` | `.../models/armor/ichor_gem_layer_2.png` |

The converter was **not** changed: its table covers one directory level and a generated subdirectory would need changes in its
map, its check and its documentation. The copies are hand-written files, which the converter never touches. The sheet
`ichor_gem_layer_1.png` is also the texture of the wings (two regions of it, as in 1.7.10), drawn by `WingsLayer`.

## 2. Rarity

`TTCommonProxy.kamiRarity` was an `EnumRarity` added by `EnumHelper` (light purple, "Kami"). `Rarity` is a fixed enum in
1.21.1, so every Kami item has `Rarity.EPIC`, which is light purple as well; the two shards stay common as in 1.7.10. The
Shadowbeam and the Experience Drain keep the Kami name colour through `ItemModKamiFocus.getName`.

## 3. Armour

- **Absorption: `ISpecialArmor` is the loader event `LIVING_ARMOR_ABSORB`** (request K1 of `docs/api-requests/KamiGear.md`, answered;
  the event itself is described in section 13). Every piece takes `0.0425 x its armour points` of a blow, flat (by the material's
  array 3/8/6/3 indexed by armour type: helmet and boots 12.75%, chestplate 34%, leggings 25.5%, the set 85%; see the open point on the
  index below), and never wears. `IchorAbsorption.onLivingArmorAbsorb`, registered by
  `KamiArmorHandler.register()`, **replaces** the amount that the 1.21.1 formula takes off: for a wearer of the pieces it is
  `damage x (the summed ratios of the worn Ichorcloth pieces)` plus the share that the 1.21.1 formula gives to the armour that is **not**
  Ichorcloth (the entity's armour value less the points of the Ichorcloth pieces, with its toughness), never more than the blow. 1.7.10's
  `ArmorProperties.ApplyArmor` added the ratios of all pieces of one priority (a plain `ItemArmor` gave `damageReduceAmount / 25`, 4% a
  point, an `ISpecialArmor` what it returned) and took the sum off the blow; the sum is the same here, with the 1.21.1 formula standing in
  for the 4% of the other armour. For an entity that wears no Ichorcloth the listener returns the amount it was given, so any other
  armour is exactly vanilla's. Blow of 10 (checked by the GameTests): the full set lets 1.5 through where the formula alone, on the same
  twenty armour points, lets 4.0; a blow of 40 lets 6.0 through, the formula alone 33.6; a helmet alone 8.725; chestplate and leggings
  4.05; an Ichorcloth chestplate with iron helmet, leggings and boots (9 points, 1.6 of 10 under the formula) 5.0.
  - Kept as the original had it: the awakened pieces absorb the same 85% whatever their own switches and the master switch say (`getProperties`
    looked at neither); a piece counts only in the slot it is made for; the pieces never wear. They have no durability (`UNBREAKABLE` is not
    needed: an `ArmorItem` made without `Properties.durability` cannot be damaged, so the game's own wear call `hurtArmor` does nothing to
    them while it still wears iron or diamond beside them; the GameTest shows both).
  - **Open point: the index that 1.7.10 gave `getProperties` and `getArmorDisplay` (not verifiable here, not carried over).** The original reads
    `getArmorMaterial().getDamageReductionAmount(slot)` from an array laid out by *armour type* (helmet 0, chestplate 1, leggings 2, boots 3: 3/8/6/3).
    As far as I remember Forge 1.7.10 (not read: **there is no Forge 1.7.10 source or jar in this environment**), `ArmorProperties.ApplyArmor` and
    `ForgeHooks.getTotalArmorValue` pass the index *in `armorInventory`* as `slot` (boots 0, leggings 1, chestplate 2, helmet 3, which is also the order the
    original's own code uses: `getCurrentArmor(0)` for the boots, `getCurrentArmor(3 - armorType)` for the others). If that is right, 1.7.10 gave the boots 3,
    the leggings **8**, the chestplate **6** and the helmet 3 points, for the absorption and for the HUD alike: chestplate and leggings swapped, so a chestplate
    alone took 25.5% and leggings alone 34%, the reverse of the numbers above. The full set (20 points, 85%), a helmet alone and boots alone are the same
    either way; only partial sets (chestplate or leggings without the other) and the points the tooltip lists per piece differ. The port **keeps the
    intended assignment** (chestplate 8, leggings 6, as the material's array says): the swap looks like an oversight of the original, not a design, and it
    cannot be proved from here. If it is shown to be right and wanted, swap `CHESTPLATE` and `LEGGINGS` in `KamiArmorMaterials.create` (the HUD, the tooltip
    and the listener read the same numbers) and the 34% / 25.5% / 4.05 / 5.0 expectations of `KamiGearLogicTest` and of the GameTests.
  - **Damage that bypasses armour** (`DamageTypeTags.BYPASSES_ARMOR`: starvation, drowning, magic, falling, the void...) is not reduced, and the
    event is not posted for it. The original `getProperties` has no look at the source, and Forge's `ApplyArmor` asked `isUnblockable()`
    only for plain `ItemArmor`; whether the player's damage path of Forge 1.7.10 called `ApplyArmor` at all for unblockable damage could not be
    checked (**there is no Forge 1.7.10 source or jar in this environment**: what is said here about `ApplyArmor`, its ratio sum and its priorities is
    from its documented behaviour and from how Thaumcraft's own port describes it, `thaumcraft.common.lib.MagicWard`), so the original may have
    reduced those hits by 85% too. The port follows request K1 (no reduction); the one place to change if that is shown to be wrong is the
    `BYPASSES_ARMOR` test in `TinkererEvents.postLivingArmorAbsorb`.
  - **Mobs.** The event is posted for every living entity and the listener answers for any wearer of the pieces. Forge 1.7.10 consulted
    `ISpecialArmor` from the player's damage path (to the best of my knowledge, not verified, see above); 1.21.1 lets mobs wear armour like
    players, so a zombie in the set takes the 85% too (plus the vanilla share of its own base armour, 2 points).
  - **The armour points and the toughness of the materials are a display only.** The materials `ichor` and `ichor_gem` keep the original's 3/8/6/3
    armour points and enchantability 20: those are what 1.7.10's `getArmorDisplay` showed (twenty points, ten icons, for the set) and what the
    1.21.1 HUD and the tooltip ("+8 Armor") show now. They **never stack** with the 85%, because the listener replaces the formula's amount instead of
    adding to it: with twenty points on the HUD a blow of 10 leaves 1.5, not 4.0 (nor any sum of the two), and a blow of 40 leaves 6.0, not 33.6. The
    **toughness of 5 per piece is gone** (it was the stand-in for the missing absorption; the tooltip listed an "Armor Toughness" the original never had, and it would have counted in
    the share of any other armour worn with the pieces); the pieces have none, as in 1.7.10. Without a poster of
    the event (NeoForge until its module exists) the pieces are plain 3/8/6/3 armour: 60% of a blow of 10, 16% of a blow of 40.
  - **Order with Thaumcraft's own ward pass.** Thaumcraft's `LivingEntityMagicWardMixin` wraps the same method for the priority-1 pieces of the
    original's `ISpecialArmor` (the void robes and the Fortress armour: their share comes off the damage first, then the ordinary armour sees what
    is left), which is the order of `ApplyArmor`. The add-on's `LivingEntityArmorMixin` has priority 900 so that it is applied first and is the inner
    wrapper: the event gets the damage that is left after the ward pass (the GameTest of the event shows it with a Fortress chestplate: a pig is
    posted with 6.5 of an explosion of 10). With the default priority the order would be that of the loading of the two mixin configs.
  - **Known corner.** The share of the *other* armour is worked out from the entity's armour value less the Ichorcloth points. When Thaumcraft's
    ward pieces are worn together with Ichorcloth, Thaumcraft leaves a ward piece out of the formula's armour value for a hit it took a share of, and
    the listener does not repeat that adjustment (it cannot: it is Thaumcraft's internal), so such a piece also counts in the formula's share.
  - **Needs a poster on NeoForge.** The group needs the NeoForge poster of `LIVING_ARMOR_ABSORB` (section 13), of `LIVING_JUMP` and of `LIVING_FALL`; without
    them the Gem Boots do not jump higher and do not stop fall damage, and the Ichorcloth is plain armour.
- **Gregtech hazard protection and the Thaumic Boots mod** (`IHazardProtector`, `IBoots`, optional in the pinned fork) are not ported:
  neither mod exists for 1.21.1.
- **The toggle key** "Toggle Awakened Armor" (category "Thaumic Tinkerer") is bound to **no key**, as `Keyboard.KEY_NONE` in the
  pinned source, although the Thaumonomicon pages still say "Pressing U". The player binds it in the controls screen. It switches the
  master flag of the player: the client sets its own copy and tells the server (`PacketToggleArmor`), the server saves it with
  the world (`SavedData`) and tells the client on login (`PacketArmorStatus`); the piece-wise switch is the sneak-click.
- **Cowl of the Abyssal Depths.** Unchanged, including the 1.7.10 quirks: lava gives unlimited breath and Blindness (the code
  names `Potion.blindness`); the food regeneration ignores both switches.
- **Boots of the Horizontal Shield.** The jump and the fall are loader events (`LIVING_JUMP`, `LIVING_FALL`, section 13), the two handlers of the
  original's `EventHandler`: `ItemGemBoots.onLivingJump` (`onPlayerJump`) adds 0.3 to the vertical motion after the jump for a player whose Gem Boots
  are on the feet with the master switch and the piece switched on, and `ItemGemBoots.onLivingFall` (`onFall`) sets the fall distance to 0 for every
  player who wears the boots on the feet, **whatever the switches say** (the original asked for nothing else). The jump is `motionY += 0.3` as in
  1.7.10, added to what the jump gave, so the block's jump factor (honey) and Jump Boost act on the jump alone, and a mob in the boots does not
  jump higher (the original asked for a player). The attribute modifier `JUMP_STRENGTH` +0.3 of the first port, which scaled with the jump factor,
  is gone, and so is the zeroing of the fall distance for every wearer on every tick; what stays of the tick is the original's `tickPlayer` line:
  `fallDistance = 0` every tick while the boots are switched on. Both listeners change a result that both sides compute, by the same rule (the side's
  own copy of the master flag, synchronised by `PacketToggleArmor` and `PacketArmorStatus`; the piece's switch is in the stack), and touch nothing in
  the world. The step height is still an attribute modifier that the server sets (`STEP_HEIGHT` +0.4, -0.1 while sneaking); the walking boost
  (speed 0.075, a quarter in water, half while sneaking) is added to the **local player's** motion on the client, because a 1.21.1 server cannot push
  a client-driven player. As in the original, the speed boost and the step height while moving ignore the disable switches. Dirt turns to grass
  under the wearer only while the piece is switched on and `shouldKamiBootsDoGrass` is on. The Thaumic Boots mod's tags `jump`, `speed`, `omni` and
  `inertiacanceling` are not ported (that mod does not exist for 1.21.1): the jump modifier is 1.
  - **Order against Ascent Boost: reversed, not closable inside the group, pinned by a GameTest.** Both are `LIVING_JUMP` listeners
    and run in the order they were registered. 1.7.10 built the Gem Boots in `TTRegistry.preInit` (the constructor chain calls `registerEvents()`, which
    registers `ItemGemBoots.EventHandler`) and registered `ModEnchantmentHandler` from `ModEnchantments.initEnchantments()`, called by `TTCommonProxy.init`;
    both handlers are at normal priority, and Forge runs equal priorities in registration order, so the boots came first: `motionY = (0.42 + 0.3) x factor`,
    with Ascent Boost's `factor = (level + 2) / 2` (the highest level on the four armour slots, not while sneaking): **1.08 for Ascent Boost I, 1.80 for III**.
    Here `ThaumicTinkerer.init()` runs the groups' `register()` in the order of `ContentGroups.ALL`, and the Enchantments group registers its listener
    (`ModEnchantments.initEnchantments()` in `Enchantments.register()`) before KamiGear registers the boots' (`KamiGear.register()`, `KamiArmorHandler.register()`),
    so Ascent Boost multiplies the plain jump and the boots add 0.3 after it: `0.42 x factor + 0.3`, **0.93 for I and 1.35 for III**. Ascent Boost is a leg
    enchantment (leggings of any kind, also the Ichorcloth ones), so Gem Boots with Ascent Boost leggings is an ordinary combination, and it jumps lower
    than in 1.7.10 (the GameTest `gemBootsAndAscentBoostLeggingsComposeTheirJumps` pins the numbers above; both effects arrive, each under its own conditions).
    The boots' listener cannot be moved ahead of Enchantments' from inside this group (both register in the `register()` phase and the groups run in the
    order of `ContentGroups.ALL`; the files that decide it are the Enchantments group's and the lead's). Request to the lead, three ways, the first matching the
    original's own order (`preInit` before `init`): (1) `Enchantments.register()` calls `ModEnchantments.initEnchantments()` (which only registers the event
    handler) from `Enchantments.ready()` instead, the phase after every `register()`; (2) `ContentGroups.ALL` with KamiGear before Enchantments (also changes the
    registration order of the groups' items); (3) a priority for the listeners of `TinkererEvents`. After it, the two numbers in the check become 1.08 and 1.80
    and this paragraph goes.
  - **Order against Shockwave and Slow Fall: immaterial.** Shockwave on the Gem Boots would blast with the real distance here (it ran at LOW priority, after the
    boots, in 1.7.10 and saw 0), but that case cannot arise: the Gem Boots cannot carry either enchantment, in the original
    (`EnchantmentShockwave.canApply` is false for `ItemGemBoots`, `EnchantmentSlowFall.canApply` is `!(item instanceof ItemGemBoots)`) and here (the item
    tags `enchantable/shockwave` and `enchantable/slow_fall` do not list `ichorcloth_boots_gem`, and `EnchantmentManager.allowsItem` repeats the exclusion in code;
    `docs/deviations/Enchantments.md` section 1). Shockwave reads only the feet slot, so it could meet the Gem Boots only by `/enchant` or a data pack, and
    then the order of the `LIVING_FALL` listeners (Slow Fall, Shockwave, then the boots') would show; nothing is done about that. (The comment in
    `ModEnchantmentHandler.register()` that blames the group order for the Shockwave case is in the Enchantments group's file and was not edited.)
- **Leggings of the Burning Mantle.** Unchanged; the light feature (config `shouldKamiLegsDoLightFeature`, off by default) places
  `nitor_gas` with the integer state property `level` = 1 on the player and level 0 on the five cells in front (the 1.7.10 code
  had put the metadata back to 0 before the loop). The gas block removes itself when no carrier is near (SimpleBlocks group).
- **Robes of the Stratosphere.** Flight is `Abilities.mayfly`, granted and taken away by the server every tick for players the
  armour has granted it (and only for them: a creative player keeps theirs); the projectile deflection is the Kami copy of the
  Distortion rule (the Foci group owns the focus). Projectiles that did not move during their last tick (an arrow in a block
  keeps its hit vector in 1.21.1, where 1.7.10's had no motion left) are not pushed; a projectile that spawned this tick counts as
  not having moved and is looked at one tick later. The six white sparkles per deflected projectile of the original are sent by
  `ThaumcraftFX.sparkle` (the server twin of the client sparkle: request A8, now in the API). The wings are a render layer
  (`WingsLayer`, the model `ModelWings`), added to the player renderers by the loader glue (`docs/api-requests/KamiGear.md`
  section 4). The flap animation resets the wings to the rest pose when the wearer stops flying (1.7.10 left the last angle).

## 4. Tools

- **No durability.** `ToolMaterial ICHOR` (harvest level 4, durability -1, efficiency 10, damage 5, enchantability 25) is the tier
  `IchorTier`: nothing is incorrect for drops (the netherite rule), and the tools carry the `UNBREAKABLE` component with the
  tooltip hidden. `Item.isEnchantable` is overridden to true because an unbreakable item would otherwise refuse enchantments (1.7.10
  allowed them). The tools are in the vanilla item tags `pickaxes`/`shovels`/`axes`/`swords` through the add-on's own copy of those
  tag files (optional entries, so the tags still load with Kami off).
- **Attack damage** is the 1.7.10 one: the weapon modifier is the tool's base (pickaxe 2, shovel 1, axe 3, sword 4, passed to
  `createAttributes`) plus the tier's 5, so a hit with the player's own 1 deals 8, 7, 9 and 10 (the vanilla 1.21.1 bases 1, 1.5, 5, 3
  are not used). The attack speeds are the vanilla ones, because 1.7.10 had no attack cooldown (see the sword below).
- **`Material` lists become predicates over block tags** (`ToolHandler.MATERIALS_SHOVEL/PICK/AXE`): the shovel list is
  `#minecraft:mineable/shovel`; the pickaxe list is `#minecraft:mineable/pickaxe`, bedrock, glass-sounding blocks and the shovel list
  (the 1.7.10 list is the concatenation); the axe list is `#minecraft:mineable/axe`, leaves, plants (bush blocks), sugar cane and
  cocoa. The Protoclay keeps the original order, so dirt counts as `pick`.
- **`onBlockStartBreak` is `BlockEvent.BREAK`** (Architectury). The extra blocks of the square, line and column modes are broken
  with `ServerPlayerGameMode.destroyBlock`, which gives them the player's normal break (drops, experience, claims and other
  mods' break events) instead of the hand-made copy of `ItemInWorldManager.tryHarvestBlock`; a thread-local flag keeps those inner
  breaks from starting the mode again. `destroyBlock` asks only for the game mode's restrictions, so every extra block is first
  checked with `Level.mayInteract` (spawn protection, world border), which vanilla asks only for the block the player hits; a
  block it refuses is left standing (1.7.10 had no such protection). The items on the ground in the box are moved to the player
  as before, and once more at the end of the server tick (`TickEvent.SERVER_POST`), because the clicked block is part of the
  box and the break in progress drops its items only after `BlockEvent.BREAK` returns (1.7.10 broke it inside the loop).
- **Blocks that cannot be broken at all** (hardness -1: end portal frames, barriers...) are left alone by the extra breaks; the pinned
  fork would have destroyed them (it only filtered by material). Bedrock is broken only in the Bedrock dimension and not in the top two layers, as before.
- **Column mode** (Shovel): the pinned fork lost the "same block" filter (`removeBlocksInIteration(..., block, ...)` is called with the
  parameter and ignores it in the loop; the neighbouring method `removeBlockWithDrops` still has it). The filter is restored: the
  column takes only blocks of the kind that was broken, 8 below to 7 above.
- **Bedrock (left click).** 1.7.10 has two paths: `onBlockStartBreak` (which survival never reaches for a block that cannot be
  broken) and `KamiDimensionHandler.onInteract`, which calls `onBlockStartBreak` of the awakened pickaxe on every left click
  on bedrock. The second one is what makes it work in survival, and it is ported **here**, not in the KamiWorld group:
  `ItemIchorPickAdv.onLeftClickBlock` (`InteractionEvent.LEFT_CLICK_BLOCK`, registered by `KamiGear`) does the same call. KamiWorld
  must not port `KamiDimensionHandler` again. Sneaking: the clicked bedrock becomes the portal block (Overworld, lowest five layers;
  top two layers of the Bedrock dimension) or air (rest of the Bedrock dimension). Not sneaking, in square or line mode: the blocks
  around break, and the bedrock itself too where the rules allow (the Bedrock dimension below its top two layers; 1.7.10 included
  the clicked block in the loops). The click is cancelled afterwards (the bedrock has nothing left to break; in creative the
  normal break would repeat the mode). Differences: the portal is made in the Overworld only (1.7.10: in any "surface world"),
  and the bedrock dimension counts as available when the portal block is registered (1.7.10: `bedrockDimensionID != 0`).
  Mining speed on bedrock is the maximum, as in 1.7.10.
- **Sneaking and the vanilla abilities.** The awakened pickaxe, shovel, axe and sword cycle the mode on a sneak-click (`use`, as 1.7.10
  `onItemRightClick`). In 1.21.1 the shovel (flattening grass to a path) and the axe (stripping logs, scraping copper) have a vanilla
  `useOn` that runs before `use` and would swallow a sneak-click on a block, so the awakened shovel and axe pass `useOn` while
  sneaking. Those two vanilla abilities themselves are new (1.7.10 had neither) and are inherited by all four shovels and axes.
- **Tree mode** is the 1.7.10 loop over Thaumcraft's own two helpers (now API, request A6): while the clicked cell is not air, break
  the furthest block of the walk that starts there. Their rules apply: oak logs are not logs for them (the original's rule), no tool
  wear, no protection check (spawn protection included) and no break event for the felled logs, a walk limited to 24 blocks sideways and 48 up or down. Two
  small differences: the loop stops after 1024 blocks or when the helper breaks nothing (1.7.10 would loop forever if a break was
  vetoed), and the gathered items keep their normal pickup delay (1.7.10 added 20 ticks to their age, which has no counterpart).
- **Sword, area mode.** 1.7.10 had no attack cooldown; without a fast attack speed every hit after the first would deal a fifth
  of its damage, so the attack speed gets a temporary huge modifier while the area strike runs. The mode is started from
  `PlayerEvent.ATTACK_ENTITY`, before the hit, like `onLeftClickEntity`. Soul mode gives the target Resistance II for one tick and
  asks the Soul Heart hook (`KamiHooks.soulHeartSink`, see `docs/api-requests/KamiGear.md` section 5).

## 5. Black Hole Ring

- **No loss when the container refuses.** 1.7.10 took the blocks out of the ring before asking the slot whether it accepts them and lost them when
  it did not; here nothing leaves the ring unless the slot accepts the stack (`canPlaceItem`, and `canPlaceItemThroughFace` for a sided
  container). A failed placement keeps the block in the ring (1.7.10 had taken it).
- **Stack size.** The stack the worn ring hands out is `min(64, the item's own maximum)`; 1.7.10 hard-coded 64, which gave a
  slot of 64 ender pearls or signs. A container still gets the item's own maximum, as before.
- **Binding.** An empty ring binds to the block it is clicked on only if that block has an item (not to water, fire, a portal...); 1.7.10
  also bound to blocks without one, which left a ring that could neither fill a container nor place anything.
- **The cursor condition of the worn refill** keeps its 1.7.10 shape: the ring hands out a stack when the cursor is empty, holds the
  bound item, or holds an item whose damage differs from the ring's (`getDamageValue() != 0`, the block metadata being 0 now).
- **Overflow.** The count saturates at `Integer.MAX_VALUE` ("over 2 billion"); 1.7.10 let the int overflow.
- **Worn merge.** `onWornTick` is the 1.7.10 algorithm (every stack of the bound block in the 36 main slots is merged into the
  first one found, which a later, larger stack replaces only while the first is outside the hotbar; the target is topped up from the
  ring; with none in the inventory and a slot free, the ring hands out a stack), with `isSameItemSameComponents` instead of
  metadata. It runs for the ring in a ring slot of the Baubles menu, on the server.
- **Equip on use.** `equipsOnUse` is false: a sneak-click toggles the ring and the ring is put on in the Baubles menu (request A1,
  now API).

## 6. Worldshaper's Looking Glass

- The square, the cost and the range (5 blocks) are those of 1.7.10 (`squareOffsets` is the original arithmetic as a pure method, tested for every side, pitch, rotation and size 3 to 11).
- **Protection.** Every placement asks `Level.mayInteract` and `Player.mayBuild` (spawn protection, world border and adventure mode);
  1.7.10 placed wherever the square reached. Blocks outside the build height are skipped.
- **Payment.** The blocks must be in the inventory or in Black Hole Rings in it; a creative player always has them and the
  placing takes nothing from them (1.7.10 `hasBlocks`: `if (player.capabilities.isCreativeMode) return true`), the ghost blocks included.
  The inventory is paid first, then the rings; the first ring that really gives a block pays (1.7.10 stopped at the first ring bound
  to the block, empty or not, which made the blocks free when a bound empty ring came first).
- **Sound.** The placing sound is played by the server for everyone near, the player included (`KamiHooks.broadcastThaumcraftSound`);
  `Level.playSound(player, ...)` on a server level leaves that player out.
- **Reach.** The server aims with the reach of the player (`blockInteractionRange`, 1.7.10: the game mode's reach), the client with
  5 blocks, so the preview can show a square at the very edge that the server does not place, as before.
- **Choosing the block.** 1.7.10 took a block with render type 0: cubes, slabs, glass, leaves, furnaces, snow layers, and also
  carpets, pressure plates, buttons, trapdoors, cake; not stairs, fences, walls, doors, beds, torches, panes, ladders. 1.21.1 has no
  render type and nearly every block is `RenderShape.MODEL`, so `ItemPlacementMirror.canRemember` takes a block *that has an
  item* whose shape is the full cube, or a slab or a layer of snow. That keeps out everything the original kept out, but
  also the flat render-type-0 blocks that are not slabs or snow (carpets, plates, buttons, trapdoors, cake), which 1.7.10 took.
- **Ghost blocks.** The translucent blocks are drawn from the loader's after-world hook (`PlacementMirrorPredictionRenderer.render`, alpha 0.6, only
  while the player holds the mirror and has the blocks; config `showPlacementMirrorBlocks`). Not seen in a running client.

## 7. Bottomless Pouch

- 13 x 9 cells; opens on a right click in either hand (`MenuRegistry.ofExtended`; the opening packet carries the hand); the
  cells are saved into the pouch whenever one changes, and again when the menu closes. The menu holds the stack it was opened for
  (the 1.7.10 `pouch` field) and never looks at the hand again, so selecting another hotbar slot while it is open (a client may send
  that) cannot redirect the writes into a second pouch. The pouch's own slot cannot be clicked, moved with its hotbar key (or the
  off hand key) or shift-clicked, and the menu closes when that stack leaves its inventory slot (1.7.10's duplication hole) or when
  something else writes the pouch's contents behind the open menu (the wand's focus selector does; the menu then does not write
  its stale cells over it). Pouches (Thaumcraft's and this one) cannot be put in. The dimensions of `ConfigHandler.forbiddenDimensions` keep it shut.
- It is an `IFocusPouch` (request A2, now API) and a belt `IBauble`, so the wand's focus selector offers the foci in it. The selector
  lists at most the first 128 cells (the pouch has 117).
- The screen title key `container.thaumictinkerer.ichor_pouch` is new (the screen draws no title, as in 1.7.10).

## 8. Feline Amulet

1.7.10 rewrote the goals of every mob around the wearer once and for good, which made a creeper's "avoid ocelots" goal avoid players
from then on and every "nearest attackable player" goal target an ender crystal instead; the effect survived the amulet being taken off
and was lost on a reload. 1.21.1 goals are final fields, so the amulet applies the effect again **every tick while worn** and the effect
ends with it. No permanent change to any mob. Scope, narrowed to what the 1.7.10 goals did:

- **Creepers** within 10 x 4 blocks lose their target and their fuse (a creeper that was already swelling stops: 1.7.10 set
  `timeSinceIgnited = 2`) and walk away from the wearer (6 blocks, speed 1.0, 1.2 when closer than 7), whoever they were after.
- **Other mobs** within the same box lose the wearer as their target only if they are after the wearer *without being provoked*: a mob
  that the wearer hurt in the last 100 ticks keeps its revenge (1.7.10's goals only stopped mobs from *finding* a player; a mob that
  already had a target, or struck back, went on). Left alone: neutral mobs (`NeutralMob`: wolves, bees, polar bears, zombified piglins...),
  the Wither and Endermen (their targeting was not a "nearest attackable player" goal), and every mob that thinks with a brain (Warden,
  piglins, hoglins, villagers...: they did not exist, and the amulet must not read a brain memory a mob does not have, which throws).
  A goal swap cannot be limited to one player either, so 1.7.10 made those mobs ignore every player; only the wearer is spared here.

## 9. Foci

- **Shadowbeam.** The cost per tick (`visShadowbeam*`), the damage (`baseDamageShadowbeam`, +20% of the base per potency level, truncated),
  the 300 steps of 0.15 blocks, the entity margin 0.3 and the mirror rule are those of 1.7.10; an entity on the way takes the hit and
  the beam goes on, a block reflects it (a Nether portal does not: an `EntityThrowable` only "enters" it). The beam is simulated by `ShadowBeam` (a pure
  function of the world, no entity); the **visuals** are a packet with the path to the players within 64 blocks and black dust particles
  along it, where 1.7.10 spawned a custom black `FXSparkle` subclass per step (request K3). The hum (`thaumcraft:brain`) is played by
  the server for everyone near, the shooter included.
- **Experience Drain.** 5 vis (+20% per potency) per tick for 15 experience points (-1 per frugal level, scaled to what fits);
  `getExperienceForLevel` and `getLevelForExperience` use the **1.21.1 experience curve** (7 + 2 per level up to 15, and so on), not the
  1.7.10 one of the original helper (17 per level up to 15, `1.5 L^2 - 29.5 L + 360` up to 30...), because the experience bar of
  a 1.21.1 player follows the new curve; the cost stays 15 experience *points*.
  The focus colour runs through the rainbow on the client: the icon is a grey texture, tinted by an item colour handler
  (`KamiGearClient.registerItemColors`, 1.7.10 `getColorFromItemStack`). The tooltips reuse Thaumcraft's own keys `item.Focus.cost2` (foci)
  and `tc.visdiscount` (armour).
- **Celestial Recall.** Held for three seconds, with an attuned pearl in the hotbar, teleports to that pearl's gateway of the
  same dimension for 40 vis each of Aer, Terra and Ordo, through `KamiHooks.warpGateTeleporter` (the gateway belongs to the KamiWorld
  group). The research is registered only while Thaumcraft's mirrors are allowed (`ThaumcraftApi.isMirrorsAllowed()`), as in 1.7.10.
  The two jump sounds and the 20 white sparkles before and after the jump of `TileWarpGate.teleportPlayer` belong to the gateway code
  that the hook calls: the default hook (`KamiHooks.teleportToWarpGate`, used only while KamiWorld has not replaced it) plays both
  and sends the sparkles (`ThaumcraftFX.sparkle`); the KamiWorld group's `TileWarpGate.teleportPlayer` plays the sounds and **sends no
  sparkles yet** (see the report: a request to that group).
- **`canApplyUpgrade`.** The original Kami foci answered true for every upgrade; the port keeps the base rule (an upgrade must be
  one the focus lists in `getPossibleUpgradesByRank`, see `docs/API_GAPS.md` 3b). The Shadowbeam and the Experience Drain list
  frugal and potency on every rank and the Recall frugal only, which is what the focal manipulator offered before, so the only
  difference is that an API caller cannot force another upgrade.

## 10. Client

- **HUD message.** The mode of a tool, the size of the mirror and the state of an armour piece are drawn above the hotbar by
  `ToolModeHUDHandler` (`ClientGuiEvent.RENDER_HUD`): 400 ticks, fading out, in the 1.7.10 rainbow colour (`cos(ticks / 250)`).
- **Wings and ghost blocks** need the two loader hooks (`docs/api-requests/KamiGear.md` section 4); Fabric has them
  (`fabric/.../client/KamiGearFabricClient.java`), which `KamiGearClient.register()` starts by name on Fabric (`Platform.isFabric()`),
  so `fabric.mod.json` needs no entry (listing the class as a `client` entrypoint as well is harmless: it registers once). NeoForge has none yet.
- **Network.** The two S2C payload types (`armor_status`, `shadow_beam`) are registered by their client receivers
  (`KamiGearClient`); `KamiGear.registerNetwork()` registers them itself only on a dedicated server (`Platform.getEnvironment() ==
  Env.SERVER`), because Fabric's payload registry throws when a client registers a type twice. `PacketToggleArmor` is C2S and is
  registered on both sides.
- **Models.** The 33 items have JSON item models (`assets/thaumictinkerer/models/item/<path>.json`); the awakened tools and the ring have the
  override models of their modes/state. Nothing was looked at in a running client.

## 11. Deliberately not carried over

- `GameRegistry.addShapelessRecipe(9 x nugget <- ichorium)` is a data recipe: `data/thaumictinkerer/recipe/ichor_nuggets.json`. It is
  not conditional on `enableKami`: with the option off `ichorium` does not exist and the game logs **one recipe parse error** at data
  load (the tag files are optional entries and cause none). A load condition would be loader-specific
  (`fabric:load_conditions` / `neoforge:conditions`) and could not be tried here.
- The item class `ItemKamiBase` is `KamiItemBase`; `ItemKamiResource.getRarity` per damage value is the per-item rarity above.
- The creative tab entry of the 1.7.10 mod's `ichor wand` is the tab stack above.

## 12. The research a Kami entry requires (decision, found by the parity check)

`KamiResearchItem.setPages` of 1.7.10 made every Kami entry require "every other research" that was registered at the moment its
`getResearchItem()` ran (Thaumcraft's five tabs and the add-on's tab, without lost and virtual entries, Kami entries, `MINILITH*` and
`*KAMI`). `ResearchHelper.initResearch` runs before `TTRegistry.postInit`, and `postInit` calls `getResearchItem()` of every item before
that of any block. So the 23 item-owned Kami entries never required a research that a block registers (Animation Tablet, Enchanter,
Funnel, Magnets, the elemental fires, ...), and which of the item-owned research they required depended on the iteration order of the
registry's `HashMap`; `WARP_GATE` (a block) required all item-owned research and the block-owned ones registered before it.
The port collects the requirements in `KamiResearchItem.registerResearch()`, after all other research is registered, so every Kami entry
requires all of them: it is never available earlier than in 1.7.10, and the list is the same on every start. `docs/PARITY_REPORT.md`
section 3 and `known_differences.json` carry this difference; the parity test checks the port's list against exactly this rule.

## 13. The loader events of the group (and the new `LIVING_ARMOR_ABSORB`, text for `docs/HOOKS.md`)

The group used to work around three missing Forge events (section 3 says how) and now listens to the loader events of `docs/HOOKS.md`. All three
are registered once, from `KamiArmorHandler.register()` (which `KamiGear.register()` calls), and answer only for their own items:

| Event | Listener | What it does | Sides |
|---|---|---|---|
| `LIVING_JUMP` | `ItemGemBoots.onLivingJump` | a player in the switched-on Gem Boots: vertical motion + 0.3 | both (changes the motion both compute, by one rule) |
| `LIVING_FALL` | `ItemGemBoots.onLivingFall` | a player in the Gem Boots (switched on or off): fall distance 0 | both (changes the distance both compute; no world effect) |
| `LIVING_ARMOR_ABSORB` | `IchorAbsorption.onLivingArmorAbsorb` | a wearer of Ichorcloth pieces: the armour takes `0.0425 x points` per piece plus the other armour's share | server (the armour step runs only there) |

**The group needs the NeoForge poster of each of these three events** (`TinkererEvents.postLivingJump`, `postLivingFall`, `postLivingArmorAbsorb`); the first two are in
`docs/HOOKS.md` sections 5 and 6, the third is new and described here.

### `LIVING_ARMOR_ABSORB`

```java
float onLivingArmorAbsorb(LivingEntity victim, DamageSource source, float damage, float absorbed)   // the amount the armour takes off
```

*Posted* on the **server** from `LivingEntity.getDamageAfterArmorAbsorb`, the armour step of `actuallyHurt` (`Player.actuallyHurt` calls the same method),
**after** vanilla has worn the armour pieces (`hurtArmor`) and worked out what its formula takes off (`damage - CombatRules.getDamageAfterAbsorb(...)`), and
**before** the magic step (Resistance, the Protection enchantments) and Absorption. `damage` is what comes to the armour step: the hit after `LIVING_DAMAGE`, the shield and the
invulnerability frames; `absorbed` is vanilla's amount, or what the listeners before returned. It is the position at which 1.7.10 Forge asked an
`ISpecialArmor` piece for its `ArmorProperties`, and the listener has the same power: it **replaces the amount the armour absorbs**. The damage that goes on is `damage - result`.

*The rules* (all in `TinkererEvents.postLivingArmorAbsorb`, the one place both loaders share):

* **Not posted** for a source of the tag `bypasses_armor` (the game does not reduce such a hit by armour) nor for a damage that is not positive and finite. The same for a
  hit that never reaches the armour step (cancelled by `LIVING_DAMAGE`, refused by the invulnerability frames, an invulnerable entity).
* The result is held between `0` and `damage` (a listener cannot make the armour heal or take more than the blow); a NaN is ignored (the value stays what it was).
* Listeners run in the order they were registered and each sees the last one's result in `absorbed`: a listener that *replaces* the value (Ichorcloth) overrides the ones
  before it, one that *adjusts* it (a bonus on top) composes. With nobody listening, or with a result equal to `absorbed`, the game gets its own value back **bit for bit**
  (no rounding of `damage - (damage - x)`, no NaN for an infinite blow): `TinkererEvents.damageAfterArmorAbsorb`.
* Posted for every living entity, so a listener answers only for the entities it means. It has no world effect.
* Its place relative to Thaumcraft's ward pass (`LivingEntityMagicWardMixin`, the priority-1 pieces of the original's `ISpecialArmor`): after it, see section 3.

*Fabric*: `LivingEntityArmorMixin`, a `@WrapMethod` on `getDamageAfterArmorAbsorb` with `priority = 900` (inner to Thaumcraft's wrapper; see section 3), registered in
`thaumictinkerer.mixins.json` under the usual `defaultRequire: 1`. It runs the original and hands its value to `TinkererEvents.damageAfterArmorAbsorb`, which computes `absorbed = damage - vanilla`, asks `postLivingArmorAbsorb` and
returns `vanilla` itself when the poster returned `absorbed` unchanged (compared **bit for bit**, so that an infinite blow, whose `absorbed` is `Infinity - Infinity` = NaN and never `==`
to itself, still gets the game's infinity and not a NaN health), `damage - result` otherwise. (Nothing in the add-on deals an infinite blow, but other mods can.) NeoForge needs no such step: its reduction modifier takes the amount directly.

*NeoForge* (read from the 21.1.259 jars in the Gradle cache, not run): there is no event with this exact shape, but the equivalent exists. `LivingIncomingDamageEvent#addReductionModifier(DamageContainer.Reduction.ARMOR, IReductionFunction)`:
the patched `LivingEntity.actuallyHurt` and `Player.actuallyHurt` call `DamageContainer.setReduction(ARMOR, newDamage - getDamageAfterArmorAbsorb(source, newDamage))` after `hurtArmor` and the formula; `setReduction`
runs the added functions in insertion order (each gets the container and the reduction so far and returns the new one) and then subtracts the result from the container's damage.
The handler that the NeoForge module needs (it already handles `LivingIncomingDamageEvent` for `LIVING_DAMAGE`) adds on every event:
`event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> TinkererEvents.postLivingArmorAbsorb(event.getEntity(), container.getSource(), container.getNewDamage(), reduction))`.
`container.getNewDamage()` is the damage that comes to the armour step at that moment. **Note**: the Javadoc of `DamageContainer#addModifier` says that a function does not run when vanilla's reduction is not triggered,
but the patched `actuallyHurt` calls `setReduction(ARMOR, ...)` unconditionally (also for a source that bypasses armour, with a reduction of 0), so the functions do run for such sources; the poster's `bypasses_armor` test is therefore needed on NeoForge too, and it
is in the poster. NeoForge also has `ArmorHurtEvent` (the wear of the pieces) and `LivingShieldBlockEvent`; neither changes the amount, so neither is used.

*Tests*: `TinkererEventsTest` (chain order, a lone listener is clamped like a chain, NaN, bypass source, no damage, bit-for-bit pass-through, and the step: `armorStepGivesBackTheGamesOwnFloatUnlessAListenerChangedTheAmount`,
`armorStepOfABlowThatIsNotFiniteIsTheGamesOwnValueNotNaN`), `HooksRuntimeChecks.livingArmorAbsorbHandsBackTheGamesOwnFloatAndSurvivesAnInfiniteBlow` (a real `hurt` of a real pig: the health after a blow whose
round trip differs from vanilla's float in the last place is vanilla's exactly, with and without a listener that returns what it is given; an infinite blow kills, it leaves no NaN health and is not posted) and
`HooksRuntimeChecks.livingArmorAbsorbIsPostedFromTheArmourStepAndReplacesWhatArmourTakesOff`
(a pig in 20 armour points and a player: the listener sees the damage after `LIVING_DAMAGE` and vanilla's amount, can leave it alone, halve the blow, push past the blow or below nothing, answer NaN; chains; starvation and magic are not
posted and not reduced; a bystander is left as vanilla; a pig in a Fortress chestplate is posted after Thaumcraft's ward pass). The group's own checks: section 14.

## 14. Tests of the loader-event part

* JUnit, `KamiGearLogicTest`: the ratio of each piece, 85% for the set of any blow, partial sets, the sum with other armour and its cap at the blow.
* GameTests, `KamiGearRuntimeChecks` (real pieces on mock players, a new player for every hit because the Soul Hearts of the KamiWorld group keep the 1.7.10 invulnerability
  frames of each server player by game tick, and a wait of 65 ticks for the spawn invulnerability of a new player):
  * `gemBootsRaiseTheJumpOnlyWhileWornAndSwitchedOn`: 0.42 against 0.72; piece off, master switch off, plain Ichorcloth boots, Gem Boots on the head, a pig in the boots, a bare player: 0.42; on
    honey (0.21) the 0.3 comes on top (0.51); the jump strength attribute is untouched; the jump key's own path (`setJumping` and a tick) carries the wearer 0.72 up.
  * `gemBootsLandWithoutFallDamageWhateverTheSwitchesSay`: a landing of 10 blocks costs a bare player 7, the wearer 0 (piece off, master off too), plain boots and Gem Boots on the head 7; a listener registered
    after the boots' sees distance 0.
  * `gemBootsAndAscentBoostLeggingsComposeTheirJumps`: Gem Boots with diamond leggings enchanted Ascent Boost I (0.93) and III (1.35), the order of the listeners as it is today (see section 3: 1.7.10 gave 1.08 and
    1.80); Ascent Boost alone 0.63; sneaking, only the boots (0.72); boots switched off, Ascent Boost alone (1.05); no leggings, the boots alone.
  * `ichorclothSetTakesEightyFivePercentOfBlowsAndLeavesBypassingDamageAlone`, `ichorclothPartialAndMixedSetsKeepTheOtherArmourAndLeaveOtherWearersAlone` (with a diamond chestplate, toughness 2, beside Ichorcloth leggings:
    5.85 of 10, where the same set without the toughness would let 6.25 through),
    `ichorclothPiecesShowTheOriginalArmourAndNeverWear`: the numbers of section 3 (blows of 1, 10, 40; starvation, magic, fall, drowning; both sets; switches; partial, mixed, iron, wrong slot, bare, a zombie; another
    listener sees 8.5 and its extra point composes; the pieces keep damage 0 while iron wears; 3/8/6/3 and no toughness in the attributes).
* Not run: the same events on a client (the jump of the local player and the landing sound), and a real flight and landing of a Gem Boots wearer (the landing is driven through `Block.fallOn`, the game's own handler, not through a fall).
