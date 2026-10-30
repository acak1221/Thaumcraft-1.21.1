# Thaumic Tinkerer on `thaumcraft.api`: gap analysis

Read-only analysis of what the 1.7.10 add-on (GTNewHorizons fork, `28f4def938e80d7a0b6f433c2b84300dfb7c35ce`, 334 files)
needs from Thaumcraft, against the public API of Thaumcraft 4 Reload (`common/src/main/java/thaumcraft/api`, 97 files,
guides in `docs/api`). Claims about the mod were checked in its code unless a document is cited; paths are relative to the root of the Thaumcraft 4 Reload repository (<https://github.com/acak1221/Thaumcraft-1.21.1>; the standalone add-on repository has none of `common/src/main/java/thaumcraft`, `docs/api` and `AGENTS.md`) and
`API/` means `common/src/main/java/thaumcraft/api`, `INT/` means `common/src/main/java/thaumcraft/common`.
Nothing was compiled or run (no Gradle, no game); runtime behaviour and rendering are **not verified**.

Verdicts:

- **COVERED**: usable today from `thaumcraft.api` (route named), or from vanilla/Architectury where the need was never
  Thaumcraft's.
- **NEEDS-API (A1..A13)**: the capability exists inside the mod; a small API is proposed in section 2.
- **NOT-IN-PORT (N1..N4)**: the capability does not exist in the port at all.
- **DROP**: no 1.21.1 equivalent or not worth porting (section 1.12); a counterpart is named when there is an obvious one.

Totals: **13 NEEDS-API-ADDITION**, **4 NOT-IN-PORT**, 23 DROP rows. TT imports Thaumcraft in 160 of its 334 files, 116 of
them from `thaumcraft.common`/`thaumcraft.client` (the internal surface that has to be replaced); its direct use of
`ThaumcraftApi` is small (arcane/crucible/infusion recipes, `addWarpToResearch`, one `registerObjectTag`, material
constants, `portableHoleBlackList`, `enchantRepair`), so almost all of the work is the cross-cutting rows of 1.0.

## 1. Needs per feature group

Group names are the content groups of `common/content` (BasicItems, Foci, KamiGear, KamiWorld, Machines, TileBlocks,
SimpleBlocks, Enchantments).

### 1.0 Cross-cutting (every group)

| Need (1.7.10) | Verdict | Route |
|---|---|---|
| `dependencies="after:Thaumcraft"`, `init`/`postInit` registrations | COVERED | `IThaumcraftAddon.onThaumcraftReady()` (Fabric entrypoint `thaumcraft`; NeoForge `@IThaumcraftAddon.Addon`), `ThaumcraftApi.whenReady` (`API/ThaumcraftApi.java:436`). Both sides, main thread, before a world loads. |
| `ConfigItems.*` / `ConfigBlocks.*` plus metadata (226 use sites, 88 files) | COVERED | `ItemApi.getItem(field, meta)`, `ItemApi.getBlock`, `BlocksTC.state(field, meta)`, `ItemsTC.require(id)`. All ~60 distinct field/meta pairs of TT's `new ItemStack(Config…)` expressions resolve in `common/src/main/resources/data/thaumcraft/tc4/legacy_ids.json`; the loop `itemGolemCore:0..11` of `AspectCropLootManager` is in the table too. Notes: `blockAiry:0` = `aura_node`, `:1` = `nitor` block (the Enchanter pillar top), `:4` = `airy[variant=4]` (warding fence); `blockCosmeticSolid:0` = `cosmetic_solid_tough[variant=0]` (obsidian totem, Enchanter pillar), `:1` obsidian tile, `:2` travel, `:3` warding; `blockHole:15` has no item (only a diagram in `ResearchHelper.LeviationaryHelp`); `itemResource:5` (zombie brain) is defunct, TT really means `itemZombieBrain` (resolves). |
| Aspects (`Aspect`, `AspectList`, primals, `Aspect.aspects`, `ResearchManager.getCombinationResult`) | COVERED | `thaumcraft.api.aspects.*`; `Aspect.getCombinationResult(a, b)`, `Aspect.getPrimalAspects()`, public `Aspect.aspects`. Quirk: `AspectList.reduce` keeps a zero key (use `remove`). |
| Research API (`ResearchItem`, `ResearchPage`, `ResearchCategories`) and recipe API | COVERED | see 1.9. |
| `ResearchManager` / `PlayerKnowledge` | COVERED, with gaps | `ThaumcraftApiHelper.isResearchComplete/completeResearch/getCompletedResearch/addAspectPool/getAspectPool/getDiscoveredAspects/getWarp*`. Gaps: scan markers and "discover aspect" (A3), offline players (N2). |
| `ItemWandCasting` | COVERED | `thaumcraft.api.wands.WandHelper`: `isWand/isStaff/isSceptre`, `getFocusItem` (a copy), `setFocus`, `getFocus`, `getAllVis/getVis/getMaxVis`, `addRealVis`, `consumeAllVis`, `consumeAllVisCrafting` (whole vis), `getConsumptionModifier`, `getFocusPotency/Enlarge/Frugal`, `setRod/setCap`. A null player is allowed (no gear). `storeVis(abs)` has no twin: add with `addRealVis`, subtract with `consumeAllVis`. |
| `Thaumcraft.proxy.*` client FX: `sparkle` ×12, `essentiaTrailFx` ×6, `blockSparkle` ×5, `blockRunes` ×5, `wispFX2/3`, `burst`, `particleCount`, `smokeSpiral`, `beamCont` | partly COVERED | `ThaumcraftFX.blockSparkle/essentiaTrail/blockArc/zap/bubbles/visDrain` (server to clients, `API/ThaumcraftFX.java`). The rest has no facade (the guides say so): A8, or TT's own vanilla particles. |
| Sounds `thaumcraft:wand/tool/zap/wind/brain/egidle/key` | COVERED (by id) | All registered (`common/src/main/resources/assets/thaumcraft/sounds.json`, `INT/config/ThaumcraftSounds.java`); not constants of the API: A7. |
| `Config.allowMirrors`, `Config.dialBottom`, `Config.enchRepair/enchHaste` | allowMirrors: NEEDS-API (A5); enchantments: COVERED | `ThaumcraftApi.ENCHANTMENT_REPAIR/HASTE` (`API/ThaumcraftApi.java:137-139`) are `ResourceKey<Enchantment>`; `dialBottom` is only the TC HUD offset: DROP. |
| `thaumcraft.api.*` that changed shape | COVERED | `ThaumcraftApi.armorMatThaumium` field → method `armorMatThaumium()` returns `ArmorMaterial` (wrap with `BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder`, or A7); `toolMatThaumium` is still a field (`Tier`); `portableHoleBlackList` is only a mirror, the block tag `thaumcraft:portable_hole_blacklist` is authoritative (`ThaumcraftApi.java:515`, refreshed at `INT/Thaumcraft.java:261`): test the tag. |

### 1.1 BasicItems (+ Machines/SimpleBlocks items)

| Feature (TT class) | Thaumcraft functionality needed | Verdict | Route / proposal |
|---|---|---|---|
| Spell Cloth (`ItemSpellCloth`, `SpellCloth*`) | `itemResource:7` (enchanted fabric) as ingredient only | COVERED | `ItemApi.getItem("itemResource", 7)`; crafting handler is vanilla. |
| Bright Nitor (`ItemBrightNitor`) | crucible recipe on nitor (`itemResource:1`) | COVERED | `ThaumcraftApi.addCrucibleRecipe(key, result, catalyst, aspects)`. |
| Cleansing Talisman (`ItemCleansingTalisman`) | `IBauble` amulet; `PotionWarpWard` and the five warp potions by `Config.potion*ID`; remove a broken bauble through `BaublesApi`; right-click/sneak-click toggle | `IBauble` COVERED; **equip-on-use clash NEEDS-API A1**; effect ids A7; slot removal A10 (optional) | `API/baubles/IBauble.java`. The port equips every `IBauble` on right click (`INT/lib/baubles/PlayerAccessories.java:45-58`) before the item's own `use` runs. Effects exist as `thaumcraft:warp_ward`, `sun_scorned`, `death_gaze`, `blurred_vision`, `unnatural_hunger`, `infectious_vis_exhaust` (`INT/config/ThaumcraftWarp.java:33-38`, `ThaumcraftFlux.java:57`), but only `PotionFluxTaint`/`PotionVisExhaust` are API. Use `MobEffect.getCategory()` instead of the `isBadEffect` reflection. `hurtAndBreak` empties the stack. |
| XP Talisman (`ItemXPTalisman`) | `IBauble` amulet; right click bottles XP; `itemZombieBrain` ingredient; zap sound; sparkles | COVERED + A1 + A8 | as above. |
| Tome of Sharing (`ItemShareBook`) | read/complete research, copy discovered aspects, copy scanned objects, `scheduleSave` | partly COVERED: A3, A4, N2 | `getCompletedResearch(player)` strips `@` clue markers and `completeResearch` refuses them (`API/internal/ThaumcraftInternalMethods.java`), so scan markers cannot travel (A3); `PlayerKnowledge.addDiscoveredAspect` exists (`INT/lib/research/PlayerKnowledge.java:88`) but is not API (A3); research of an **offline** book owner cannot be read (N2): use the snapshot stored on the book (a deviation). `scheduleSave` is not needed (attachments save themselves). Aspect list on the book needs a codec (A4). |
| Infused Inkwell (`ItemInfusedInkwell`) | `IScribeTools`; durability = ink; recipe parts | COVERED | `API/IScribeTools.java`; the research table slot accepts any `IScribeTools` (`INT/tiles/TileResearchTable.java:163`, `INT/container/ContainerResearchTable.java:69`). Refill recipe is a vanilla recipe with the wildcard damage. |
| Helm of Revealing (`ItemRevealingHelm`) | `IRepairable`, `IRevealer`, `IGoggles`, `IVisDiscountGear`, `IRunicArmor`; Thaumium armour material; goggles + thaumium helmet in recipe | COVERED | `thaumcraft.api.nodes.IRevealer.showNodes`, `thaumcraft.api.IGoggles.showIngamePopups`, `IVisDiscountGear.getVisDiscount(stack, Player, Aspect)`; head slot is read generically (`common/src/main/java/thaumcraft/client/ThaumcraftGogglesClient.java:90`, which also tolerates a `null` `getAspects()`). |
| Blood Sword (`ItemBloodSword`) | `IRepairable`; `toolMatThaumium.getEnchantability()`; thaumium sword in recipe; mob drops | COVERED | `ThaumcraftApi.toolMatThaumium.getEnchantmentValue()` (1.21.1 `Tier`). Drop hook is a loader event, not Thaumcraft. |
| Mob Aspect / Mob Display / Soul Mould (`ItemMobAspect`, `ItemMobDisplay`, `ItemSoulMould`) | one `registerObjectTag(stack, int[] meta, aspects)` per aspect (17 aspects, each in 3 variants: plain, condensed, infused) | COVERED, with a design constraint | Object tags are per item (+damage of durable items), not per component. Either 51 separate items, or implement `IEssentiaContainerItem` on the item: `ObjectAspectResolver.scanTags` adds the container contents to the scan (`INT/lib/research/ObjectAspectResolver.java:134`, guide `docs/api/guide/aspects.md`). |
| Connector (`ItemConnector`) | recognises a golem entity (`EntityGolemBase`) | COVERED (by id) / A7 | entity type `thaumcraft:golem` (`INT/config/ThaumcraftGolems.java:42`). |
| Gas Remover, gas items (`ItemGasRemover`, `ItemGas`) | empty phial `itemEssence:0` as ingredient; wand sound | COVERED | `ItemApi.getItem("itemEssence", 0)` = `thaumcraft:phial_empty`. |

### 1.2 Foci (the ten: Flight, Dislocation, Telekinesis, Smelt, Deflect, Heal, Ender Chest; Kami: Recall, Shadowbeam, XP Drain)

| Need | Verdict | Route / proposal |
|---|---|---|
| Base class `ItemFocusBasic` (`getVisCost`, `isVisCostPerTick`, `getFocusColor`, `getOrnament`, `getFocusDepthLayerIcon`, `getSortingHelper`, `getActivationCooldown`, `onFocusRightClick/onUsingFocusTick/onPlayerStoppedUsingFocus`, `getPossibleUpgradesByRank`) | COVERED | `API/wands/ItemFocusBasic.java`; dispatch `INT/items/wands/foci/WandFocusActions.java:62-90`. Add-on foci keep their own data component patch inside the wand (`FocusData` uses `ItemStack.STRICT_CODEC`, `INT/items/wands/foci/FocusData.java:17-45`). |
| `onFocusRightClick(…, MovingObjectPosition)` | COVERED, shape changed | no hit result is passed: use `player.pick(range, 1F, false)` (the example focus does). Held foci call `player.startUsingItem(hand)` instead of `setItemInUse(stack, MAX_VALUE)`; elapsed ticks = `getUseDuration − count` (Recall). |
| Vis payment (`wand.consumeAllVis(stack, p, cost, doit, false)`) | COVERED | `WandHelper.consumeAllVis(wand, player, cost, doit, false)`; costs are centivis; `doit` is ignored on a client player. |
| Upgrades `frugal`, `potency`, `enlarge` | COVERED | `FocusUpgradeType` is a closed catalogue (ids 0-20); TT uses only these three. Read the level with `WandHelper.getFocusPotency/Frugal/Enlarge(wand)`; **do not** call `focus.getUpgradeLevel(wandStack, …)` (throws, see 3b). |
| **Dislocation**: picked block + tile NBT stored on the focus; `portableHoleBlackList`; allow-mirrors; burst FX | COVERED + A5 + A8 | `WandHelper.getFocusItem` / modify a data component / `WandHelper.setFocus`; blacklist = tag `thaumcraft:portable_hole_blacklist`; amber `itemResource:6`. |
| **Ender Chest**: mirror and portable-hole focus ingredients; allow-mirrors | COVERED + A5 | `ItemApi.getBlock("blockMirror", 0)` = `thaumcraft:mirror`; `itemFocusPortableHole`. GUI is vanilla (`ChestMenu` over the ender chest). EnderStorage branch: DROP. |
| **Flight**: `wind` sound, smoke FX | COVERED + A8 | `getUpgradeLevel(wandStack…)` bug above. |
| **Telekinesis**, **Heal** | COVERED | nothing from Thaumcraft but costs. `isUseItem` → `startUsingItem`. |
| **Smelt**: `BlockUtils.getTargetBlock`, furnace registry, beam FX; arcane recipe from Fire + Excavation foci | COVERED + A8 | vanilla `player.pick` / `RecipeManager`; `itemFocusFire`, `itemFocusExcavation`. |
| **Deflect**: `getFocusEnlarge`; shield of projectiles; Botania/BloodMagic blacklists | COVERED | `WandHelper.getFocusEnlarge(wand)`; `thaumcraft:frost_shard` etc. are `Projectile`s with `getOwner()` (`INT/entities/projectile/EntityFrostShard.java:50`). |
| **Recall** (Kami): Sky Pearl, Warp Gate, `blockMirror` | COVERED | nothing beyond vis payment. |
| **Shadowbeam** (Kami): beam entity, `FXSparkle` subclass | COVERED + A8 | TT owns the entity; particle is TT's. |
| **XP Drain** (Kami): `getAllVis/getMaxVis`, `storeVis`, `Aspect.getPrimalAspects()`, rainbow colour | COVERED | `WandHelper.getAllVis/getMaxVis/addRealVis(wand, aspect, centivis, true)`; `getFocusColor(stack)` may read the client player. |

### 1.3 KamiGear

| Feature (TT class) | Thaumcraft functionality needed | Verdict | Route / proposal |
|---|---|---|---|
| Ichor resources (`ItemKamiResource`, 11 metas), Protoclay, Sky Pearl, Placement Mirror, Cat Amulet | ingredients `itemResource:7/2/14`, `itemWandCap:2`, `itemWandRod:2`; sound `thaumcraft:wand`; Cat Amulet is `IBauble` | COVERED | metas become separate items (TT side). |
| Ichorcloth Rod / Ichor Cap (`RodIchorcloth`, `CapIchor`) | `WandRod("ICHORCLOTH", 1000, stack, 10)` + `setGlowing(true)`, `WandCap("ICHOR", 0.7F, stack, 10)`, texture override | COVERED | `API/wands/WandRod.java`, `WandCap.java:69` (the 4-arg original constructor exists). Tags are not lower-cased (`INT/items/wands/WandMaterials.java:192-235`), so research keys `ROD_ICHORCLOTH` / `CAP_ICHOR` still match `getResearch()`. Capacity is whole vis (1000 → 100000 centivis). Use `setTexture` rather than overriding `getTexture`. Build the item stacks in `onThaumcraftReady`. |
| Ichorcloth armour ×4, Gem armour ×4 (`ItemIchorclothArmor`, `ItemGem*`) | `IVisDiscountGear`, `IWarpingGear`, `IRunicArmor`, `IGoggles`+`IRevealer` (gem helm), `Thaumcraft.instance.entityEventHandler.prevStep` | COVERED | `API/IVisDiscountGear`, `IWarpingGear.getWarp(stack, Player)`, `IRunicArmor`. `prevStep` is Thaumcraft's own step-height restore: in 1.21.1 use an attribute modifier. `ISpecialArmor` has no counterpart in the Thaumcraft API (3a); the add-on's loader event `LIVING_ARMOR_ABSORB` stands in for it (Fabric only; `docs/HOOKS.md` section 14, `docs/deviations/KamiGear.md` section 3). |
| Ichor tools ×8 (`ItemIchor*`, `ToolHandler`) | Tool tier; recipe pieces (elemental tools, excavation/frost foci, native clusters `itemNugget:16/21/31`); **tree felling**: `Utils.isWoodLog`, `BlockUtils.breakFurthestBlock` (Axe Adv., mode 2) | tools COVERED; **felling NEEDS-API A6** | `ThaumcraftApi.toolMatElemental` is a `Tier`; TT's `ICHOR` tier is its own. The two helpers exist at `INT/lib/utils/Utils.java:333`, `INT/lib/utils/BlockUtils.java:224`. |
| Ichor Pouch (`ItemIchorPouch`, `ContainerIchorPouch`, `SlotNoPouches`) | extends `ItemFocusPouch` (117 slots), is a belt `IBauble`, opens its own GUI on right click, must be offered by the wand's focus selector | **NEEDS-API A2 + A1** | `ItemFocusPouch` is internal, fixed to 18 cells (`INT/items/wands/ItemFocusPouch.java:21,26-34`); the selector only knows `ItemFocusPouch` and 18 cells per pouch (`INT/items/wands/FocusSelectionSources.java:15-34`); right-click equip excludes only `ItemFocusPouch` (`PlayerAccessories.java:64`), so an `IBauble` pouch is equipped instead of opened. |
| Block Talisman (`ItemBlockTalisman`) | ring `IBauble`; sneak toggles; uses on blocks | COVERED + A1 | `blockJar:0/3`, `itemFocusPortableHole` in recipes. |
| Kami foci ×3, `KamiResearchItem` | see 1.2 and 1.9 | | |

### 1.4 KamiWorld

| Feature | Thaumcraft functionality needed | Verdict | Route |
|---|---|---|---|
| Bedrock dimension, portal, `TeleporterBedrock`, `OreClusterGenerator` | keep Thaumcraft's ores/aura/vegetation out of a custom dimension | COVERED | `ThaumcraftApi.addDimBlacklist(ResourceKey<Level>, 2)` (`API/ThaumcraftApi.java:55`: 0 or 2 suppress ores and aura, every level suppresses vegetation and structures). The dimension itself is data (`data/<mod>/dimension*`, example: `common/src/main/resources/data/thaumcraft/dimension/outer_lands.json`). |
| Warp Gate (`BlockWarpGate`, `TileWarpGate`, GUIs) | `blockCosmeticSolid:2` (travel paving) in the infusion; wand sound; sparkles | COVERED + A8 | no warp is added by the gate. |
| Soul hearts, shard drop handler, armour toggle | none | COVERED | |

### 1.5 Machines

| Feature (TT class) | Thaumcraft functionality needed | Verdict | Route / proposal |
|---|---|---|---|
| Aspect Analyzer (`BlockAspectAnalyzer`, `TileAspectAnalyzer`, `ContainerAspectAnalyzer`, `GuiAspectAnalyzer`, `SlotLimitedHasAspects`) | aspects of the stack (`getObjectTags` + `getBonusTags`), "has the player scanned this item" (`ScanManager.generateItemHash`, client `getScannedObjects`), `UtilsFX.drawTag` | aspects COVERED; **scan check NEEDS-API A3**; drawing A9 | `ThaumcraftApiHelper.getObjectAspects(stack, level)` already adds container contents and bonuses (`API/ThaumcraftApiHelper.java:148`). The slot filter becomes "`getObjectAspects(stack, level).size() > 0`". The scan check needs `ScanManager.hasBeenScanned(KnowledgeAccess.get(player), "@" + itemId, ITEM)` (`INT/lib/research/ScanManager.java:142`; signature at `INT/items/relics/ItemThaumometer.java:180`). `Aspect.getImage()`/`getColor()` suffice to draw a tag yourself. In the pinned fork the block exists only with ComputerCraft (`shouldRegister`); decide whether to register it always. |
| Repairer (`TileRepairer`) | `IAspectContainer`, `IEssentiaTransport`, `getConnectableTile`, `takeEssentia` | COVERED | `ThaumcraftApiHelper.getConnectableTile(Level, x, y, z, Direction)`; same loop as the example bowl, pulling from tubes/jars (`docs/api/guide/essentia-and-vis.md`). Sparkles A8. |
| Funnel (`TileFunnel`, `RenderTileFunnel`) | filled jar item (`ItemJarFilled`), target jar `TileJarFillable`/`Void`, `aspectFilter`, hopper below | COVERED | filled jar = `ItemJar implements IEssentiaContainerItem` (`getAspects` returns `null` when empty, `setAspects(stack, list)` needs a stack of 1; `INT/items/ItemJar.java`); jar variants `BlocksTC.jar()` / `BlocksTC.voidJar()`; target `TileJar` is an `IAspectSource` (`INT/tiles/TileJar.java`): check `doesContainerAccept(aspect)` (reflects the label), then `addToContainer` (returns the remainder; the void jar returns 0). **Quirks**: `containerContains` always returns 0 (line 89, original quirk), read `getAspects()`; mutators work on the server thread only. The label survives `setAspects(empty)`, so the "swap to an empty jar" branch disappears. Render the jar with the item renderer (TC registers `JarItemRenderer`). |
| Magnet, Mob Magnet (`TileMagnet`, `TileMobMagnet`) | lightning-ring texture of the hover harness | COVERED | texture `thaumcraft:textures/items/lightningring.png`; nothing else. |
| Necromancy Tablet (`TileSummon`) | arcane pedestals with an item; essentia trails; spawn Thaumcraft mobs (`EntityWisp`, `EntityFireBat`, `EntityBrainyZombie`) | COVERED + A7 | `TilePedestal` is a `WorldlyContainer` (`INT/tiles/TilePedestal.java:30`), block `BlocksTC.get("arcane_pedestal")`; `ThaumcraftFX.essentiaTrail(level, consumer, source, aspect)`; entity types `thaumcraft:wisp`, `firebat`, `brainy_zombie` (`INT/config/ThaumcraftMonsters.java:25-31`) by id (the entity classes are `final`). |

### 1.6 TileBlocks

| Feature | Thaumcraft functionality needed | Verdict | Route / proposal |
|---|---|---|---|
| Animation Tablet (`BlockAnimationTablet`, `TileAnimationTablet`, `TabletFakePlayer`) | wand click rotates; golem core `itemGolemCore:100`; `thaumcraft:tool` sound | COVERED | `WandHelper.isWand(stack)`; or implement `IWandable` on the block (`API/wands/IWandable.java`). FakePlayer is a loader matter (3a). |
| Remote Placer (`BlockRPlacer`) | `IWandable`, `ItemWandCasting` check | COVERED | `IWandable.onWandRightClick(level, wand, player, x, y, z, side, event)` (block asked first). Unregistered in the pinned fork (`shouldRegister() == false`). |
| Levitation Locomotive + Relay (`BlockMobilizer*`, `TileEntityMobilizer/Relay`) | recipe parts (`blockLifter` = `thaumcraft:levitator`, shards, greatwood log); a **multiblock diagram page** (`ResearchPage(List)`) | recipes COVERED; **diagram NOT-IN-PORT N1** | `PORTING.md §7`: `COMPOUND_CRAFTING` is not drawn. Use text/image pages. |
| Golem Connector (`BlockGolemConnector`, `TileGolemConnector`) | golem type, core, decoration, health, home, markers (read/write) | **NEEDS-API A11** (only used for ComputerCraft; block exists only with CC in the fork) | internals exist: `INT/entities/golems/EntityGolemBase.java` (`getGolemCore` 338, `getDecoration` 371, `getHomePosition` 408, `setHome` 415, `getMarkers` 653, `setMarkers` 656). The docs state golems have no API. |
| Transvector Interface (`TileTransvectorInterface`, `Block…`) | proxies `IAspectContainer` and `IEssentiaTransport` of the linked block entity | COVERED | both interfaces are honoured for foreign tiles (`INT/lib/essentia/EssentiaTransfers.java:565-677`, `foreignSource`); verify against jars/alembics because stock changes inside a foreign mutation are checked. Item/fluid/RF/EU parts are loader capabilities (3a). |
| Transvector Dislocator (`TileTransvectorDislocator`) | skip `blockAiry:0` (node) and `portableHoleBlackList` | COVERED | `BlocksTC.auraNode()`; tag `thaumcraft:portable_hole_blacklist`. |

### 1.7 SimpleBlocks

| Feature | Thaumcraft functionality needed | Verdict | Route / proposal |
|---|---|---|---|
| Dark Quartz, slabs, stairs; Platform; Forcefield; Camo; Gaseous Light/Shadow; Nitor gas; elemental fire ×6 | none besides recipe items (planks `blockWoodenDevice:6/7`, shards `itemShard:0..6`, `itemEssence`) | COVERED | sparkles A8. Gas/fire items use nothing else. |
| Ward Slab (`BlockWardSlab`, `TileWardedSlab`) | places `blockAiry:4` (warding fence) above itself; runes FX; `blockCosmeticSolid:3` recipe | COVERED + A8 | `ItemApi.legacyBlockState("blockAiry", 4)`. See 3b: the fence tile removes itself unless a warding stone is 1-2 blocks below (`INT/tiles/TileWardingStoneFence.java:20-30`). |
| Travel Slab / Stairs | paving stone of travel model/texture; potion effects on walk | COVERED | model texture `thaumcraft:blocks/paving_stone_travel` (note the `blocks` directory), `thaumcraft:blocks/paving_stone_warding`; base state for the stairs = `BlocksTC.state("blockCosmeticSolid", 2)`. |
| Infused Farmland / Grain / Seeds (`TileInfusedFarmland`, `TileInfusedGrain`, `ItemInfused*`) | `IAspectContainer` (goggles show it), `Aspect.getCombinationResult`, essentia trails, aspect lists on stacks, Hoe of Growth hook | COVERED + A4 | the Hoe of Growth already bone-meals on its own click (`INT/items/equipment/ItemElementalHoe.java`, `ElementalHoeRules`); TT implements `BonemealableBlock`. The block entity must sync its aspects (`getUpdateTag`) for goggles; use `AspectList.writeToNBT/readFromNBT`. Item aspect/tendency data needs codecs: A4. |
| Aspect crop loot (`AspectCropLootManager`) | wisp essence per aspect; golem cores 0-11; void seed `itemResource:17`; clusters; tools; `OreDictionary` name scan | COVERED | `((IEssentiaContainerItem) ItemsTC.require("wisp_essence")).setAspects(stack, new AspectList().add(a, 2))` (`INT/items/ItemWispEssence.java:37`); the ore-name substring scan becomes item-tag lookups (`OreDictionaryNames` resolves old names). |
| Infused potions (`ItemInfusedPotion`, `ModPotions`) | none | COVERED | `MobEffect` + `PotionContents` (TT side). |

### 1.8 Enchantments (14 and `ModEnchantmentHandler`)

| Need | Verdict | Route |
|---|---|---|
| Compatibility with Repair (`ThaumcraftApi.enchantRepair`), exclusions of Unbreaking | COVERED | enchantments are data: `exclusive_set`; Repair is `thaumcraft:repair` (`ThaumcraftApi.ENCHANTMENT_REPAIR`, data `common/src/main/resources/data/thaumcraft/enchantment/repair.json`: exclusive with `minecraft:unbreaking`, supported items tag `#thaumcraft:enchantable/repair`). |
| Auto Smelt must not apply to the Elemental Axe | COVERED | `ItemApi.getItem("itemAxeElemental", 0)` / `supported_items` tag. |
| Enchanter's costs for Haste/Repair, research gating `canEnchantmentBeUsed` | COVERED | `ThaumcraftApi.ENCHANTMENT_HASTE/REPAIR`; `ThaumcraftApiHelper.isResearchComplete(player, key)`. The enchanter must enumerate `Registries.ENCHANTMENT` from the level's registry access (3a). |
| Vampirism `thaumcraft:zap`; focus/final strikes etc. | COVERED | sound id; the rest is Minecraft events. |

### 1.9 Research registration

| Need | Verdict | Route / notes |
|---|---|---|
| `registerCategory`, `new ResearchItem(key, cat, aspects, col, row, complexity, ItemStack/ResourceLocation)`, `setParents/ParentsHidden/Secondary/Concealed/Stub/AutoUnlock/Round/Special`, `setPages`, `registerResearchItem`, `ThaumcraftApi.addWarpToResearch` | COVERED | `API/research/*`. Add-on entries are real research (book, table, purchase, sync) via `INT/lib/research/AddonResearch.java`. Complexity > 3 is clamped to 3 (as in 1.7.10). No TT key collides with a native key (checked when this table was written; it gave the number of keys as 92, which `docs/PARITY_REPORT.md` sections 1 and 5 finds to be an overestimate: the source has 78 distinct research keys). |
| Parents on Thaumcraft keys: `DISTILESSENTIA, ENCHFABRIC, FOCUSEXCAVATION, GOGGLES, GOLEMBELL, INFUSION, JARBRAIN, MIRROR, NITOR, THAUMIUM` | COVERED | all in `tc4/research.json`; categories `BASICS, GOLEMANCY, ARTIFICE, ALCHEMY, THAUMATURGY, ELDRITCH`. |
| `KamiResearchItem` walks `ResearchCategories.researchCategories` for every non-lost, non-virtual entry | COVERED | the public map is filled by `loadNative()`, which `registerCategory`/`addResearch` call first; prefer `ResearchCategories.allResearch()`. |
| Name/text via overridden `getName()/getText()` | COVERED (differently) | the book reads `tc.research_name.<KEY>` / `tc.research_text.<KEY>` from the key only (`INT/lib/research/ResearchDefinition.java:45-46`); overrides are ignored. The lang converter already derives both (`docs/lang-key-families.md`, `derived` rows). Page texts are translation keys. |
| Recipe pages: `ConfigResearch.recipes` | COVERED | keep the recipe objects returned by `ThaumcraftApi.addArcaneCraftingRecipe/addCrucibleRecipe/addInfusionCraftingRecipe` in `TTRecipes`; page = `new ResearchPage(IArcaneRecipe/InfusionRecipe/CrucibleRecipe/InfusionRecipe[])`; JSON crafting-table recipes: `ResearchPage.recipe(ResourceLocation…)`. |
| Compound (multiblock) page | NOT-IN-PORT N1 | see 1.6. |

### 1.10 Commands

| Command | Verdict | Route |
|---|---|---|
| `/maxresearch` (+99 to every aspect) | COVERED | `ThaumcraftApiHelper.addAspectPool(player, aspect, 99)` for each `Aspect.aspects.values()`. |
| `/iskamiunlocked` | COVERED | `getCompletedResearch(player)` vs `ResearchHelper.kamiResearch.parentsHidden`. |
| `/setCropTendency` | COVERED | `player.pick(...)` instead of `BlockUtils.getTargetBlock`. |

### 1.11 Peripherals (ComputerCraft / OpenComputers)

| Peripheral | Verdict | Route / proposal |
|---|---|---|
| Generic `IAspectContainer` and `IEssentiaTransport` peripherals (`PeripheralAspectContainer`, `PeripheralEssentiaTransport`) | COVERED | pure API interfaces; implement with CC:Tweaked (`PeripheralLookup` on Fabric, capability on NeoForge). |
| Arcane Ear note get/set, Brain in a Jar XP, Deconstruction Table aspect, Arcane Bore status | **NEEDS-API A12** | internals: `INT/tiles/TileSensor.java:21` (`getNote`, no setter), `TileJarBrain.java:51` (`getXp`), `TileDeconstructionTable.java:48` (`reward()`), `TileArcaneBore.java:169-170` (`hasFocus/hasPickaxe`, `stats()`). Alternative that needs no API: a CC:Tweaked module in the mod's own `compat/` (same pattern as `compat/jade`). |
| Golem Connector peripheral | A11 | above. |
| OpenComputers drivers | DROP | OC 1 has no 1.21.1 release. |

### 1.12 DROP

| # | 1.7.10 piece | 1.21.1 counterpart |
|---|---|---|
| 1 | OpenComputers drivers (`peripheral/OpenComputers/*`, `OCCompat`) | CC:Tweaked only |
| 2 | Forge Multipart (`multipart/*`: candle and nitor parts) | none |
| 3 | NEI handlers (`client/nei/*`) | EMI/JEI plugins; TC's arcane/crucible/infusion recipes show up already, TT's seed and necromancy lists need an own plugin |
| 4 | EnderIO planter (`EnderIO`, `InfusedSeedsPlanter`) | none |
| 5 | EMT electric hoe (`EMTCompat`) | none |
| 6 | IC2/CoFH energy on the Transvector Interface (`IndustrialcraftUnloadHelper`) | loader energy/fluid/item capabilities, per loader |
| 7 | AE2 `IMovableTile` | none |
| 8 | Baubles Expanded (`IBaubleExpanded`, `BaubleItemHelper`) | TC4 Reload's own four slots |
| 9 | ThaumicBoots omni mode (`@Optional` in `ItemGemBoots`) | none |
| 10 | Waila providers (`TTinkererProvider`, `MagnetProvider`) | Jade (`compat/jade` of the mod shows the pattern) |
| 11 | Tinkers' Construct repair (`TinkersConstructCompat`) | optional later |
| 12 | BloodMagic/Botania deflect blacklists | optional later (entity-type tag) |
| 13 | EnderStorage focus integration | optional later |
| 14 | Forge IMC (`InterModCommsOperations`) | an entity-type/block tag or own API |
| 15 | ASM/mixins/preloader (`mixins/*`, `preloader/*`) | none |
| 16 | CodeChicken `Vector3` bundled in Thaumcraft | `Vec3` |
| 17 | `GuiResearchRecipe` hook for the peripherals page (`GuiResearchPeripheral`, `ClientTickHandler`) | a plain research page |
| 18 | `ConfigResearch.recipes` | `TTRecipes` (already in the skeleton) |
| 19 | Potion id config and `Potion.potionTypes` hack | `MobEffect` registry |
| 20 | CC turtle upgrade `FumeTool`, `FumePeripheral` | CC:Tweaked data-driven turtle upgrade, optional |
| 21 | `SlotResearchTableOverride` (unused) | the research table accepts `IScribeTools` |
| 22 | Update checker (`core/handler/updater/*`) | none |
| 23 | `LibRenderIDs`, ISBRH ids | n/a |

"Ender mirror" does not exist in this source. The nearest are the Ender Chest focus (1.2), which uses the Thaumcraft mirror
block as an ingredient (`blockMirror:0` = `thaumcraft:mirror`), and the Kami Placement Mirror (1.3).

## 2. Prioritised API additions (13)

Signatures are 1.21.1 types. "Backing" is the internal class that already does the job; all backing capabilities exist (A4 is new, thin code over data classes that exist).
P1 = a TT feature is broken or impossible without it; P2 = parity; P3 = polish or optional integration.

| # | Prio | Proposed API | Backing (file) | Needed by | Effort |
|---|---|---|---|---|---|
| A1 | P1 | `thaumcraft.api.baubles.IBauble`: `default boolean equipsOnUse(ItemStack stack, Player player) { return true; }` (hand-equip skipped when false; menu equip unchanged) | `INT/lib/baubles/PlayerAccessories.java:45-58` (RIGHT_CLICK_ITEM) and `freeSlotFor` `:64-67` | XP Talisman (right click bottles XP), Cleansing and Block Talismans (sneak-use), Ichor Pouch (right click opens GUI) | S |
| A2 | P1 | `thaumcraft.api.wands.IFocusPouch { ItemStack[] getInventory(ItemStack pouch); void setInventory(ItemStack pouch, ItemStack[] contents); }` implemented by the native `ItemFocusPouch`; selector, equip-on-use exclusion and nested-pouch checks use the interface; cell stride raised from 18 (or document the first 18 cells as the usable page) | `INT/items/wands/ItemFocusPouch.java` (same method names, `SIZE = 18`), `FocusSelectionSources.java:15-34`, `lib/network/FocusSelectionNetworking.java`, `PlayerAccessories.java:64` | Ichor Pouch (117 slots), `SlotNoPouches` | M (S if capped at 18 cells) |
| A3 | P1 | `ThaumcraftApiHelper`: `boolean hasScannedItem(Player, ItemStack)`; `boolean discoverAspect(ServerPlayer, Aspect)`; `Scans getScans(Player)` / `void addScans(ServerPlayer, Scans)` with `record Scans(Set<String> items, Set<String> entities, Set<String> phenomena)` | `ScanManager.hasBeenScanned` (`:142`, signature `"@" + ITEM id`, `ItemThaumometer.java:180`), `PlayerKnowledge.addDiscoveredAspect` (`:88`), `getScannedItems/Entities/Phenomena`, `scanItem/Entity/Phenomena` (`:129-137`), sync `lib/network/KnowledgeEvents.java:51`; player access `lib/capabilities/KnowledgeAccess.java` | Aspect Analyzer (GUI and `canPlace`), Tome of Sharing | M |
| A4 | P1 | `Aspect.CODEC`, `Aspect.STREAM_CODEC` (by tag, registry-driven so add-on aspects work); `AspectList.CODEC`, `AspectList.STREAM_CODEC` (list of tag/amount) | new thin wrappers; pattern in `INT/items/WispEssenceData.java` | data components of infused seeds/tendencies, Tome of Sharing, mob aspect data, any stack holding aspects | S |
| A5 | P2 | `ThaumcraftApi.isMirrorsAllowed()`, `isWardedStoneAllowed()` (synced value on a client) | `INT/config/ThaumcraftSettings.java:28` (`current()`), `Config.java:124` | gating of Dislocation, Ender Chest, Heal, Recall research; ward slab | S |
| A6 | P2 | `ThaumcraftApiHelper.isWoodLog(Level, BlockPos)`; `ThaumcraftApiHelper.breakFurthestBlock(Level, BlockPos, Block, Player)` | `INT/lib/utils/Utils.java:333`, `INT/lib/utils/BlockUtils.java:224` | Ichor Axe Adv. (tree mode) | S |
| A7 | P2 | stable keys: `thaumcraft.api.potions.WarpEffects` (`ResourceKey<MobEffect>` for warp ward, sun scorned, death gaze, blurred vision, unnatural hunger, infectious vis exhaust, thaumarhia); `thaumcraft.api.entities.ThaumcraftEntities` (`ResourceKey<EntityType<?>>` wisp, firebat, brainy zombie, golem, frost shard); `ThaumcraftMaterials.armorMatThaumiumHolder()` etc.; sound-id constants | `INT/config/ThaumcraftWarp.java:33-38`, `ThaumcraftFlux.java:56-57`, `ThaumcraftMonsters.java:25-31`, `ThaumcraftGolems.java:42`, `ThaumcraftEquipment.java:88-119`, `ThaumcraftSounds.java` | Cleansing Talisman, Necromancy Tablet, Connector, Helm of Revealing. Works today by id strings; this only hardens it | S |
| A8 | P3 | client-only `thaumcraft.api.client.ThaumcraftClientFX`: `sparkle`, `burst`, `wisp`, `blockRunes`, `beam`, `smokeSpiral` (+ server-side senders in `ThaumcraftFX`) | `client/fx/ParticleEngine.java`, `Tc4Particle.java`, `RuneEffects.java`, `BlockSparkleClientEffects.java`, `EssentiaFlowClientEffects.java` | sparkles/runes/beams of foci, ward slab, repairer, enchanter, talismans, warp gate | M |
| A9 | P3 | client-only `thaumcraft.api.client.AspectRenderer.draw(GuiGraphics, Aspect, int x, int y, int amount, int bonus)` | `client/gui/ThaumcraftRecipeDisplay.java:1292` (`drawAspectTag`) | Aspect Analyzer GUI (`UtilsFX.drawTag`) | S |
| A10 | P3 | `thaumcraft.api.baubles.BaublesApi.getWorn(LivingEntity)` → `List<ItemStack>` (read-only) | `INT/lib/baubles/PlayerAccessories.java:26` (`get(player)`) | Cleansing Talisman cleanup, any "is X worn" check | S |
| A11 | P3 | `thaumcraft.api.golems`: read view (type/material, core id, decoration string, health, home pos+facing, markers pos/dim/side/colour) and `setHome`, `setMarkers` | `INT/entities/golems/EntityGolemBase.java:293,338,371,408,415,653,656`, `GolemMarker` | Golem Connector (CC-only) | M |
| A12 | P3 | `thaumcraft.api.machines`: `IArcaneEar { int getNote(); void setNote(int); }`, `IBrainJar { int getXp(); int getXpMax(); }`, `IDeconstructor { Aspect getReward(); }`, `IArcaneBore { boolean hasFocus(); boolean hasPickaxe(); … }` (alternative: CC glue inside the mod's `compat/`) | `INT/tiles/TileSensor.java`, `TileJarBrain.java`, `TileDeconstructionTable.java`, `TileArcaneBore.java` | CC:Tweaked peripherals | M |
| A13 | P3 | `ThaumcraftApiHelper.getBonusObjectTags(ItemStack, AspectList, Level)` | `INT/lib/InternalMethodHandler.java:80` (reachable today through the public field `ThaumcraftApi.internalMethods`) | Aspect Analyzer (optional, `getObjectAspects(stack, level)` already covers it) | S |

NOT-IN-PORT (no API proposal, plan a workaround):

| # | Missing capability | TT need | Workaround |
|---|---|---|---|
| N1 | `ResearchPage.COMPOUND_CRAFTING` is not drawn (`docs/api/PORTING.md §7`); the 1.7.10 constructor shape (list of dims/stacks) is also gone | Levitation Locomotive diagram (`ResearchHelper.LeviationaryHelp`) | text and image pages. |
| N2 | Knowledge lives on the loaded player; there is no read for an offline player by name (`ThaumcraftApiHelper.isResearchComplete(String, …)` works for online players only) | Tome of Sharing "sync from the owner's latest research" | use the snapshot stored on the book (document as a deviation). |
| N3 | `ItemFocusBasic.onFocusBlockStartBreak` is declared but never dispatched (no caller in `INT/`) | none (all TT foci return false) | none. |
| N4 | `IWandable.onUsingWandTick/onWandStoppedUsing` are never called (guide `wands-and-foci.md`) | none | none. |

## 3. Risks

### 3a. 1.7.10 semantics that do not carry over

| Topic | Where in TT | Recommended approach |
|---|---|---|
| Metadata | 33 files use block metadata (orientation, powered, mode); items: Mob Aspect (17×3), Infused Grain/Seeds/Potion per aspect, Kami resources (11), Block/XP Talisman toggle, Dark Quartz | block state properties; one item per variant or a data component; the toggle items use a boolean component. Document the id map per group. |
| Stack NBT | Dislocation focus payload, Tome of Sharing, talismans (XP, count), Placement Mirror, Infused seeds | data components with `Codec` + `StreamCodec`; a focus component survives `WandHelper.setFocus` (whole stack kept, `FocusData`). |
| ISBRH / `IItemRenderer` / `ModelBase` | `RenderMagnet`, `RenderRepairer`, `RenderInfusedCrops`, `RenderWarpGate`; `RenderGenericSeeds`, `RenderMobDisplay`, `RenderPlacementMirror` | JSON models where static; block-entity or item renderers otherwise; `MixinRenderBlocks` is dropped. |
| TESR | Animation Tablet, Enchanter, Funnel, Magnet, Relay, Repairer, Warp Gate | `BlockEntityRenderer`; draw the wand and jar through `ItemRenderer.renderStatic` (Thaumcraft registers their special renderers). |
| `ForgeDirection`, `ChunkCoordinates`, `MovingObjectPosition`, `IIcon` | everywhere | `Direction`, `BlockPos`, `HitResult`; textures converted by `scripts/porting/tt_assets.py`. |
| FakePlayer | `TabletFakePlayer`, `TileRPlacer`, mixin `AccessorItemInWorldManager` | Architectury has none: subclass `ServerPlayer` in common code (dummy connection) or use Fabric API `FakePlayer`/NeoForge `FakePlayer` per loader; `ServerPlayerGameMode` replaces the accessor. The mod's `FakeThaumcraftPlayer` is internal. |
| World providers / dimension ids | `WorldProviderBedrock`, `ChunkProviderBedrock`, `TeleporterBedrock`, golem marker `dim` | datapack dimension + dimension type + generator; `ResourceKey<Level>`; teleport with `ServerPlayer.changeDimension(DimensionTransition)`; add the key to `ThaumcraftApi.addDimBlacklist`. |
| `EnumHelper` (rarity, tool tier, armour material) | `kamiRarity`, `ICHOR` tier, `TT_BLOOD`, `ICHOR` armour | `Rarity` cannot be extended: colour the name in `getName`; implement `Tier`; register an `ArmorMaterial` in `Registries.ARMOR_MATERIAL` (skeleton has `ARMOR_MATERIALS`). `ISpecialArmor` (no damage, `0.0425 × armor` reduction) is ported as armour pieces made without durability (they cannot be damaged, so `UNBREAKABLE` was not needed) plus the add-on's loader event `LIVING_ARMOR_ABSORB`, which replaces the amount the armour takes off (Fabric only; `docs/HOOKS.md` section 14, `docs/deviations/KamiGear.md` sections 3 and 13). The "attribute modifiers or a hurt event" route proposed here earlier was not taken. |
| `GameRegistry` / `OreDictionary` | `addRecipe`, `registerOre`, `AspectCropLootManager`, `EnumOreFrequency` | JSON recipes; tags (`OreDictionaryNames` maps old names to `c:` tags); substring scans of ore names → explicit tag lists. |
| Enchantments | 14 + `ModEnchantments`, `EnchantmentManager` | `data/thaumictinkerer/enchantment/*.json` plus event code reading `EnchantmentHelper.getItemEnchantmentLevel(holder, stack)`; the Enchanter lists `Registries.ENCHANTMENT` via the level's `RegistryAccess`, level range from `Enchantment.getMaxLevel()`, compatibility from `Enchantment.areCompatible`; costs keyed by `ResourceKey<Enchantment>`; keep `LibEnchantIDs` only as names. |
| Potions | `ModPotions`, `DummyPotions`, `PotionEffectHandler*` | `MobEffect` + `Holder`; `MobEffectInstance(holder, duration, amplifier)`. |
| GUI / network | `GuiHandler`, `SimpleNetworkWrapper`, `AbstractPacket` | `MenuType` + screens, `CustomPacketPayload` (rules in `PORTING_RULES.md §2`). |
| Events | `LivingHurt/Update/Jump/Fall/Drops`, `BonemealEvent`, `PlayerInteract`, `BreakEvent` | Architectury events where they exist. The ones it has not (the damage amount, armour absorption, dig speed, jump, fall, item-use tick, death drops) are the add-on's own loader events `TinkererEvents`, posted from Mixins on **Fabric** and used by five groups (`docs/HOOKS.md`; the **NeoForge posters are not written**). `BonemealEvent` has no event: `docs/deviations/SimpleBlocks.md` "Bone meal (forced)". |
| Energy/fluids | `TileTransvectorInterface` (RF, EU, `IFluidHandler`) | per-loader capabilities (NeoForge `Capabilities.*`, Fabric Transfer/energy API); only the essentia/aspect proxy is API. |
| Config/`LoadedMods` | `ConfigHandler`, `LoadedMods` | skeleton `ConfigHandler`; `Platform.isModLoaded`. |

### 3b. Traps in the existing API and port (verified in code)

| Trap | Evidence | Consequence / handling |
|---|---|---|
| `focus.getUpgradeLevel(stack, type)` **throws** `IllegalArgumentException` when `stack` is not a supported focus stack (TT's `ItemFocusFlight` passes the wand) | `INT/items/wands/foci/FocusUpgradeData.java:95-99` | read via `WandHelper.getFocusPotency(wand)` or `getUpgradeLevel(WandHelper.getFocusItem(wand), type)`. |
| TT foci override `canApplyUpgrade(...)` to `true` | `ItemModFocus`, `ItemModKamiFocus`; `API/wands/ItemFocusBasic.java` `applyUpgrade` calls `canApplyUpgrade` | a direct `applyUpgrade` of a type outside `getPossibleUpgradesByRank` would store an upgrade that makes the focus unmountable (`FocusData.isSupportedFocus`, `FocusUpgradeData.supportedForFocus`). Do not override; let the focal manipulator offer upgrades from `getPossibleUpgradesByRank`. |
| Right-click equips every `IBauble` before the item's own `use` | `PlayerAccessories.java:45-58` | A1. |
| Focus pouch is internal, fixed 18 cells | `ItemFocusPouch.java:21-34`, `FocusSelectionSources.java` | A2. |
| `TileJar.containerContains` returns 0; `addToContainer` ignores the label; mutators need the server thread | `INT/tiles/TileJar.java:89,60-68` | check `doesContainerAccept` first, read `getAspects()`. |
| `ItemJar.getAspects` returns `null` for an empty jar; `setAspects` needs `count == 1` and at most one aspect (≤ 64) | `INT/items/ItemJar.java` | null-check; split the stack first. |
| `AspectList.reduce` leaves a zero key | `API/aspects/AspectList.java:139` | TT's `getAspects()[0]` loops (infused grain) may pick an empty entry; use `remove`. |
| Overridden `ResearchItem.getName()/getText()` are never called by the book | `ResearchDefinition.java:45-46` | already solved by the `tc.research_name.*` lang keys. |
| `ResearchCategories.researchCategories` is empty until `loadNative()` | `API/research/ResearchCategories.java` | `registerCategory`/`addResearch`/`allResearch()` load it; TT registers its category first. |
| Every `registerResearchItem()` rebuilds the add-on layer | `AddonResearch.refresh()` | ~150 entries: fine, but register them in one batch on the ready hook. |
| Warding fence (`airy[variant=4]`) destroys itself unless a warding stone is 1-2 blocks below | `TileWardingStoneFence.java:20-30`, `EldritchBlockRules.java:900` | TT's Ward Slab re-places it every 100 ticks as in 1.7.10 (the same flicker); if that is unacceptable give the slab its own push/collision logic. |
| Add-on wand parts must be registered before any wand with them is loaded | `WandData` codec fails with "Unsupported wand rod" | register `ICHORCLOTH`/`ICHOR` on the ready hook, never conditionally per world. |
| `IWandable` is asked before the focus; `onFocusRightClick` gets no hit result | `WandFocusActions.java`, guide | use `player.pick`. |
| Essentia transfers are transactional | `EssentiaTransfers.java:565-677` | Transvector Interface proxying into TC jars needs a GameTest with jars, tubes and alembics. |
| The add-on's API-only test rejects the **text** `thaumcraft.common.` / `thaumcraft.client.` anywhere in a source file, comments and Javadoc included (`TinkererApiOnlyTest`, like `ExampleAddonApiOnlyTest`) | `addons/thaumic-tinkerer/common/src/test/java/thaumic/tinkerer/TinkererApiOnlyTest.java` | reword any ported comment that names an internal class; API methods whose signatures expose internal types (`AuraHelper.node` returns the internal node tile, `WandRod.nativeRod()`, `ResearchPage.corpusPage()`) cannot be named: use `NodeAdapter.of(blockEntity)` / `INode`. |

### 3c. Scope facts of the pinned fork (affect what must be ported)

- Unregistered (`shouldRegister() == false`): the Remote Placer block (`BlockRPlacer`); the ItemBlock helper classes of magnet, warp gate, ward slab, travel slab and dark quartz block/slab are not registered as stand-alone items (their blocks register normally).
- Aspect Analyzer and Golem Connector exist only when ComputerCraft is loaded; all Kami content only when `ConfigHandler.enableKami`.
- `ThaumicTinkererArcaneRecipe` etc. already map onto `ThaumcraftApi`; TT's use of `ThaumcraftApi.addInfusionCraftingRecipe` never passes the `Object[]` NBT-result form (grep), so the restriction of `InfusionRecipe` to named patches does not matter.
- The infusion recipe codecs accept up to 4096 components (`INT/crafting/InfusionComponentPredicate.java:26`); TT's largest recipe has about 13 stacks.

## 4. Not verified

Runtime behaviour (no game or Gradle run), client rendering of jar/wand items inside TT's renderers, NeoForge parity (the mod's own Fabric-first status in `AGENTS.md` applies), the CC:Tweaked integration surface, and whether the port's `docs/api` examples hold for TT-sized recipe sets. The line numbers cited are those of the current checkout (`cb2b435`).
