# Porting a Thaumcraft 4 add-on to Thaumcraft 4 Reload (Minecraft 1.21.1)

Thaumcraft 4 Reload keeps the public API of Thaumcraft 4.2.3.5: every class of `thaumcraft.api` that
1.7.10 add-ons compiled against exists under the same package and name, with the same methods where
1.21.1 allows it. A 1.7.10 add-on is ported, not loaded: its code still has to move to 1.21.1 itself
(items, blocks, rendering, networking). This guide is about the Thaumcraft side of that move: what stays,
what changed and what to call instead. The working example is
[`examples/example-addon`](../../examples/example-addon) (run it with `./gradlew :fabric:runExampleClient`), and the
[guides](README.md#guides) explain each part of the API with compiled code from it.

Everything stated as working here is exercised by a test of this repository; the names are in each section.

## 1. Project setup

The mod is built with Mojang mappings and Java 21 for Minecraft 1.21.1 on Fabric (Loader 0.16.14+, Fabric
API) and NeoForge 21.1, both with Architectury API 13.0.11+.

Install the mod into your local Maven repository from a checkout of this repository:

```bash
./gradlew :fabric:publishToMavenLocal :neoforge:publishToMavenLocal
```

or put the release jar into your project's `libs/`. The `-sources.jar` next to it carries the sources of
the whole shared code, `thaumcraft.api` among it, so your IDE shows the documented API.

**Fabric (Loom).** Loom remaps the mod to your mappings, so Yarn and Mojang mappings both work.

```gradle
repositories { mavenLocal() }
dependencies {
    modImplementation "thaumcraft:Thaumcraft-fabric:0.1.0-alpha.7"   // or: modImplementation files("libs/Thaumcraft-fabric-0.1.0-alpha.7.jar")
    modImplementation "dev.architectury:architectury-fabric:13.0.11"
}
```

`fabric.mod.json`: `"depends": { "thaumcraft": "*" }`.

**NeoForge.** NeoForge runs on Mojang names, so the release jar is used as it is:
`implementation "thaumcraft:Thaumcraft-neoforge:0.1.0-alpha.7"`, plus a `[[dependencies.<yourmod>]]`
entry for `modId = "thaumcraft"` in `neoforge.mods.toml`.

## 2. Where the Thaumcraft registrations go

A 1.7.10 add-on declared `dependencies = "after:Thaumcraft"` and registered aspects, object tags, recipes,
research and wand parts in `init`/`postInit`. 1.21.1 has no such order: Fabric runs the `main`
entrypoints of mods in no particular order, so `ItemsTC.require("thaumium_ingot")` in your initializer can
run before Thaumcraft registered that item. Implement `thaumcraft.api.IThaumcraftAddon`:

```java
public final class MyAddon implements ModInitializer, IThaumcraftAddon {
    @Override public void onInitialize() { /* register your items and blocks as usual */ }
    @Override public void onThaumcraftReady() { /* everything you did in init/postInit */ }
}
```

- **Fabric:** list the class under the `"thaumcraft"` entrypoint: `"entrypoints": {"main": [...], "thaumcraft": ["com.example.MyAddon"]}`.
- **NeoForge:** annotate it with `@IThaumcraftAddon.Addon` (public constructor without arguments). It runs
  from `FMLCommonSetupEvent`'s main-thread queue.
- **Either loader:** `ThaumcraftApi.whenReady(Runnable)` from your own initializer.

The hook runs once, on the main thread, on both physical sides, after every mod registered its items and
Thaumcraft its own content, and before a world loads. Register on both sides: a client and its server must
know the same research keys. Tests: `AddonLifecycleTest`, GameTest `addonHookRanBeforeTheWorld`.

Do not register from parallel NeoForge mod constructors: the registries of the API are not concurrent.

## 3. Cheat sheet: 1.7.10 → 1.21.1

| 1.7.10 | 1.21.1 in this API |
|---|---|
| `World`, `int x, y, z` | `Level`, `BlockPos` (some original int-coordinate forms are kept, e.g. `IWandable`) |
| `EntityPlayer` | `Player` / `ServerPlayer` |
| `TileEntity` | `BlockEntity` (`thaumcraft.api.TileThaumcraft` is still the synced base class) |
| `ForgeDirection` | `Direction` |
| item metadata | separate items; where the API still takes `meta` it is the damage of a damageable item |
| `NBTTagCompound` on stacks | data components; block entities still save `CompoundTag` |
| `OreDictionary` name | item tag; the API accepts the old names, see section 5 |
| `IIcon`, `registerIcons` | JSON models and textures (`assets/<modid>/...`) |
| `.lang` files | `assets/<modid>/lang/en_us.json` with the same keys |
| integer dimension id | `ResourceKey<Level>` (`ThaumcraftApi.addDimBlacklist(ResourceKey, int)`) |
| `Potion` field (`PotionFluxTaint.instance`) | registered `MobEffect`: `PotionFluxTaint.instance()` / `holder()` |
| `DamageSourceThaumcraft.taint` field | `DamageSourceThaumcraft.taint(level)`, `causeTentacleDamage(entity)`... (damage types are data) |
| `ThaumcraftApi.armorMatThaumium` field | `ThaumcraftApi.armorMatThaumium()`; as the argument of an `ArmorItem`, the registry holder `ThaumcraftMaterials.armorMatThaumiumHolder()` (also fortress, void, void robe), see [stable keys](guide/world-and-content.md#stable-keys) |
| `Config.potionWarpWardID` and the six other warp potion ids, `new PotionEffect(id, ...)` | `WarpEffects.WARP_WARD`, `SUN_SCORNED`, `DEATH_GAZE`, `BLURRED_VISION`, `UNNATURAL_HUNGER`, `INFECTIOUS_VIS_EXHAUST`, `THAUMARHIA`; `new MobEffectInstance(WarpEffects.holder(key), duration, amplifier)` |
| `entity instanceof EntityWisp` (`EntityFireBat`, `EntityBrainyZombie`, `EntityGolemBase`, `EntityFrostShard`...) | `ThaumcraftEntities.is(entity, ThaumcraftEntities.WISP)`; `type(key)`, `holder(key)` |
| `"thaumcraft:wand"`, `"thaumcraft:zap"`... sound names in strings | `ThaumcraftSoundIds.WAND`, `ZAP`...; `ThaumcraftSoundIds.event(id)` for `Level.playSound` |
| `Thaumcraft.proxy.sparkle`, `burst`, `wispFX`/`wispFX2`/`wispFX3`, `blockRunes`, `beamCont`, `smokeSpiral`, `essentiaTrailFx`, `particleCount` | `thaumcraft.api.client.ThaumcraftClientFX` (client; the `Level` comes first), see [client effects](guide/world-and-content.md#client-effects-particles) |
| `UtilsFX.drawTag(x, y, aspect, amount, bonus, ...)` | `thaumcraft.api.client.AspectRenderer.draw(graphics, aspect, x, y, amount, bonus)`, see [aspect tags](guide/world-and-content.md#aspect-tags-in-a-screen) |
| numeric enchantment ids | `ResourceKey<Enchantment>` constants beside the old fields |
| `ConfigItems.itemResource` + meta, `ConfigBlocks.*` | `ItemsTC` / `BlocksTC` / `ItemApi`, see section 4 |
| `Thaumcraft.proxy.getResearchManager()`, `PlayerKnowledge` | `ThaumcraftApiHelper`, see section 4 |
| `ItemWandCasting` methods | `thaumcraft.api.wands.WandHelper`, see section 8 |
| `Config.allowMirrors`, `Config.wardedStone` (client copies `CallowMirrors`, `CwardedStone`) | `ThaumcraftApi.isMirrorsAllowed()`, `isWardedStoneAllowed()`, see section 4 |
| `Utils.isWoodLog`, `BlockUtils.breakFurthestBlock` | `ThaumcraftApiHelper.isWoodLog(level, pos)`, `breakFurthestBlock(level, pos, block, player)` and, new, `findFurthestBlock(level, pos, block)`, see section 4 |

## 4. Reaching Thaumcraft content and player knowledge

Items and blocks have 1.21.1 ids under the `thaumcraft` namespace: `ItemsTC.require("thaumium_ingot")`,
`BlocksTC.get("arcane_stone")`. `ItemApi.getItem(name, meta)` / `getBlock(name, meta)` keep the original
calls: a plain or namespaced 1.21.1 id works, and the original `ConfigItems`/`ConfigBlocks` field names with
their metadata resolve to the flattened 1.21.1 item, block or state (table: [LEGACY_IDS.md](LEGACY_IDS.md)).

What add-ons reached through `ResearchManager` or `Thaumcraft.proxy.getPlayerKnowledge()` is on
`ThaumcraftApiHelper`, taking the player instead of a name:

| 1.7.10 | `ThaumcraftApiHelper` |
|---|---|
| `ResearchManager.isResearchComplete(name, key)` | `isResearchComplete(player, key)` (the name form still works for online players) |
| `ResearchManager.completeResearch(player, key)` + packet | `completeResearch(player, key)` (server; pays its warp, syncs) |
| `getResearchForPlayer(name)` | `getCompletedResearch(player)` |
| `completeAspect(player, aspect, amount)` | `addAspectPool(player, aspect, amount)` / `getAspectPool(player, aspect)` |
| `hasDiscoveredAspect`, `getDiscoveredAspects` | same names, player forms added |
| `getWarpPerm/Sticky/Temp/Total(name)` | `getWarpPermanent/Sticky/Temporary/Total(player)` |
| `Thaumcraft.addWarpToPlayer`, `addStickyWarpToPlayer` | `addWarpToPlayer(player, amount, temporary)`, `addStickyWarpToPlayer` |
| `consumeVisFromWand`, `consumeVisFromInventory` | same names |
| `Thaumcraft.proxy.getScannedObjects/Entities/Phenomena`, the hash check of the thaumometer readout | `hasScannedItem(player, stack)`, `getScans(player)` (an immutable `Scans` of the mod's own markers) |
| `ResearchManager.completeScannedObject/Entity/Phenomena` | `addScans(serverPlayer, scans)` (server; returns how many markers were new, syncs once) |
| `PlayerKnowledge.addDiscoveredAspect` | `discoverAspect(serverPlayer, aspect)` (server; quiet: no notice, no points) |
| `Config.allowMirrors`, `Config.wardedStone` | `ThaumcraftApi.isMirrorsAllowed()`, `ThaumcraftApi.isWardedStoneAllowed()` (the value the connected server sent, on a client) |

Scan markers are not research: the clue markers (`"@KEY"` among the completed research) are neither returned by
`getCompletedResearch` nor accepted by `completeResearch`, and `Scans` does not carry them. Read the switches when a
player acts, not when you register research, so that both sides register the same content.

The log walk of the Axe of the Stream is `ThaumcraftApiHelper.isWoodLog(level, pos)` and
`breakFurthestBlock(level, pos, block, player)`, plus `findFurthestBlock(level, pos, block)`, which answers where the next
break would happen without breaking anything. Same walk as 1.7.10 (a greedy walk of the first qualifying cell within two
blocks, boxed to 24/48/24 blocks around the start; it breaks one block per call, drops its loot, wears no tool), with these
differences: server thread only, `false` for a client level, an unloaded start, an air start and a start that is not a log
of `block` (the original broke any start), no chunk is loaded, and the world border, the spawn protection and the game mode
are applied to the cell that breaks. **A claims or land-protection mod is not asked, and no break event fires:** the broken
cell can lie 24 blocks sideways or 48 up or down from the clicked one, so call `findFurthestBlock`, ask your protection about
that cell, and only then `breakFurthestBlock` (checking the clicked position protects nothing). `isWoodLog` is the block
tag `#minecraft:logs` (the plain oak log counts, leaves do not): what the original's "can sustain leaves, or is on the list of
logs" comes to in 1.21.1. See `docs/audit/KNOWN_DEVIATIONS.md` section 9 and
[the guide](guide/world-and-content.md#felling-trees).

Tests: GameTests `addonResearchJoinsGameplay` (completion, warp, completed set), `knowledgeApiScansAndDiscovery`,
`knowledgeApiWoodLogRule`, `knowledgeApiFurthestBlock*`; `ScansApiTest`, `ScanServicesTest`, `ServerSwitchesApiTest`,
`TreeFellingApiTest`.

## 5. Aspects and object tags

`ThaumcraftApi.registerObjectTag` keeps its forms: an `ItemStack`, an item with metadata (the damage of a
damageable item, `32767` the wildcard), a `TagKey<Item>`, and the **1.7.10 ore dictionary name**:
`registerObjectTag("ingotCopper", aspects)`. `OreDictionaryNames` resolves the old names: the names
Thaumcraft 4 itself used keep their original member lists (`thaumcraft:original_ore/*`), a namespaced id is
that tag, and the Forge convention `<kind><Material>` is the `c:` tag (`ingotCopper` → `c:ingots/copper`,
`gemNetherQuartz` → `c:gems/nether_quartz`, `dyeLightBlue` → `c:dyes/light_blue`).

Tags are data: at mod initialization, when 1.7.10 add-ons made these calls, no tag is bound yet. A tag
registration is kept and applied again when a world starts (and on a client joining a server): it fills
every member that has no registration of its own. A registration of the item itself always wins over a
tag. `registerComplexObjectTag` needs the recipes of a world and therefore a running server (call it with
a `ServerLevel`, or from the server lifecycle). Entity tags (`registerEntityTag`) use the original entity
names for vanilla and Thaumcraft entities ("Zombie", "Thaumcraft.Firebat") and the registry id for others
("mymod:my_entity"). Tests: `OreDictionaryNamesTest`, GameTests `oreDictionaryNamesReachTheTagsOfTheWorld`,
`ObjectAspectApiRuntimeChecks`.

**New aspects** use the original constructor, five-argument form with your own texture:

```java
Aspect TEMPUS = new Aspect("tempus", 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
        ResourceLocation.fromNamespaceAndPath("myaddon", "textures/aspects/tempus.png"), 1);
```

The tag is unique and lower case; the texture is a square white or grey glyph (the originals are 32×32),
tinted with the colour; the lang keys are `tc.aspect.<tag>` (the meaning under the name) and
`tc.aspect.help.<tag>`. Register the same aspects in the same order on both sides: a client that lacks one
cannot decode the items that name it. An add-on aspect is stored, synced, scanned, combined at the research
table, carried as essentia in jars, tubes, phials and crystals, and drawn everywhere aspects are drawn. A
primal of your own (no components) is given to players on their first join, but wand, sceptre and amulet vis,
vis relays, shards and infused ores exist for the six original primals only. The full contract is the Javadoc
of `Aspect`. Tests: `AddonAspectTest`, GameTests of `AspectApiRuntimeChecks`.

**Aspects in data.** `Aspect.CODEC` / `Aspect.STREAM_CODEC` (the tag, registry driven, so add-on aspects work; an unknown tag
is an error naming it) and `AspectList.CODEC` / `AspectList.STREAM_CODEC` (`[{"tag": "ignis", "amount": 4}]` in list order,
amounts of at least one) let a data component, a data file or a packet hold aspects. `AspectList` still compares by
identity, so wrap it in a value type for a component ([guide](guide/aspects.md#aspects-in-data-components-data-files-and-packets)).
Tests: `AspectCodecsTest`.

**`getBonusObjectTags`.** The 1.7.10 `getBonusObjectTags(stack, base)` took the base aspects and returned them enriched.
`getBonusObjectTags(stack, bonus, level)` returns the complete aspects of the stack with `bonus` added on top, so the base
must not be passed again: pass `null`. Test: GameTest `knowledgeApiBonusObjectTags`.

## 6. Recipes

`ThaumcraftApi.addArcaneCraftingRecipe`, `addShapelessArcaneCraftingRecipe`, `addCrucibleRecipe`,
`addInfusionCraftingRecipe` and `addInfusionEnchantmentRecipe` are the original calls with the original
units: arcane costs are whole vis of the primal aspects, crucible and infusion costs are essentia. The
recipes survive data pack reloads and appear in EMI and JEI. An ingredient may be an `ItemStack`, an `Item`,
a `Block`, an `Ingredient`, a namespaced tag id or a 1.7.10 ore dictionary name (`"ingotIron"`,
`"logWood"`). An infusion enchantment takes a `Holder<Enchantment>`. Tests: `ProgrammaticRecipeRuntimeChecks`,
`CraftingRecipeApiRuntimeChecks`, `CraftingApiTest`.

The recipe types are data driven as well (`data/thaumcraft/recipe/` of this repository shows the formats),
so an add-on may ship JSON recipes instead and show them in the book with `ResearchPage.recipe(id)`.

## 7. Research

`ResearchCategories.registerCategory`, `new ResearchItem(...)...registerResearchItem()`, `ResearchPage` and
`ThaumcraftApi.addWarpToResearch` are the original calls. An entry registered this way is research of the
game: it is drawn in the Thaumonomicon, the research table writes its notes, a secondary entry is bought
with research points, item, entity and aspect triggers reveal hidden entries when scanned, completion pays
its warp, and the knowledge of a player saves and syncs it.

- Name and text: `tc.research_name.<KEY>`, `tc.research_text.<KEY>`; a category tab: `tc.research_category.<KEY>`.
  Page texts are translation keys and keep the original markup (`<BR>`, `<LINE>`, `<IMG>...</IMG>`).
- Icons: `icon_item` is drawn as an item, `icon_resource` over the whole cell; a category icon over the
  whole tab icon whatever its size; a category background is read with the geometry of the original
  512×512 sheet.
- Pages: text, concealed text, arcane/crucible/infusion/enchantment recipe objects (single or arrays, an
  array cycles once per second), vanilla `Recipe` objects, `ResearchPage(ItemStack)` smelting pages, aspect
  pages, image pages, and `ResearchPage.recipe(ResourceLocation...)` for data-driven recipes.
- `COMPOUND_CRAFTING`, the wand construct diagram of the original, is drawn as TC4 drew it: every layer at once, top layer
  first, in the isometric layout over the ground plate, the vis of the construct below, the item's tooltip on hover. The
  page takes the original list `[AspectList vis, Integer dx, Integer dy, Integer dz, List blocks]`, the blocks in the
  original's order: layer by layer from the top, and in a layer row `dz - 1` down to row 0, each row column `dx - 1` down to
  column 0 (the first block of a layer is drawn at the right corner of its plan). The 1.21.1
  constructor is typed `ResearchPage(List<Recipe<?>>)`, so cast the list: `new ResearchPage((List) Arrays.asList(vis, 5, 1,
  1, blocks))`. A block is an `ItemStack`, an item or block, an ore dictionary name (`String`), an item tag, an `Ingredient`
  or a list of these (shown one per second); `null` draws nothing, and an empty stack (`ItemStack.EMPTY`, `Blocks.AIR`) is the
  original's invisible "Empty Space" block (`blockHole` metadata 15), drawn blank and named on hover. A list of recipe objects
  given to the same constructor is shown like a recipe array. The book does not offer the click-through of the original on
  this page or on any other.
- Changing an original entry through its public view works as it did:
  `ResearchCategories.getResearch("NITOR").setSiblings("MY_KEY")` is laid over the original entry.
- An add-on tab follows the six original ones, in registration order, as in the original.
- Item triggers compare the item and its data, the damage of durable items ignored; the ore dictionary
  shortcut of the original has no counterpart, so list every item of a family.

Tests: GameTest `addonResearchJoinsGameplay`; the book was checked in a client with the example add-on
([screenshots](../analysis/2026-10-26-addon-api-ru.md)).

## 8. Wand parts, foci and the wand itself

`new WandRod(...)`, `new StaffRod(...)` and `new WandCap(...)` are the original constructors and register the
part (capacity in whole vis, craft cost, an `IWandRodOnUpdate` for vis regeneration, the texture):

```java
new WandRod("infernal", 150, new ItemStack(MyItems.INFERNAL_CORE), 12, new MyRodOnUpdate(),
        ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_rod_infernal.png"));
WandCap cap = new WandCap("alchemical", 0.9F, List.of(Aspect.WATER), 0.8F, new ItemStack(MyItems.ALCHEMICAL_CAP), 7);
cap.setTexture(ResourceLocation.fromNamespaceAndPath("myaddon", "textures/models/wand_cap_alchemical.png"));
```

The arcane workbench builds wands, staves and sceptres from the part items once `ROD_<tag>` and `CAP_<tag>`
are researched (register those research entries yourself, as in 1.7.10), the cost is rod cost × cap cost, the
capacity comes from the rod, the discount from the cap (special aspects included), `onUpdate` runs while the
wand is carried, and the name uses `item.Wand.<tag>.rod` / `item.Wand.<tag>.cap`. The wand is drawn with your
textures in the UV layout of the original `ModelWand`, so a 1.7.10 PNG works unchanged. A part registered under
a native tag never replaces the native part. Any `ItemFocusBasic` subclass is a focus: it mounts on a wand and
its hooks, colour and ornament are used.

`thaumcraft.api.wands.WandHelper` is the original `ItemWandCasting` surface: `isWand`, `getRod`, `getCap`,
`getFocus`, `getFocusItem`, `setFocus`, `isStaff`, `isSceptre`, `getVis`/`getAllVis`/`getMaxVis` and
`addRealVis`/`consumeVis`/`consumeAllVis` in centivis (hundredths of vis, as stored), `addVis` and
`consumeAllVisCrafting` in whole vis, `getConsumptionModifier` and the focus upgrade getters. Payment goes
through the native discount and debit rules. Tests: `WandAddonPartsTest`, `WandAddonRenderingTest`,
GameTests of `WandApiRuntimeChecks`; the drawing was checked in a client
([screenshots](../analysis/2026-10-26-addon-wand-parts-ru.md)).

## 9. Interfaces of items, blocks and block entities

Thaumcraft asks every item, block and block entity for these interfaces, so implementing one is enough:

| Interface | Consulted by | 1.21.1 signature change |
|---|---|---|
| `IVisDiscountGear` | wand vis cost, worn armour and baubles | `Player` |
| `IRunicArmor` | runic shielding | – |
| `IWarpingGear` | warp of held and worn gear | `Player` |
| `IGoggles`, `IRevealer` | aspect popups, node reveal | `LivingEntity` |
| `IRepairable`, `IRepairableExtended` | Repair enchantment | `Player` |
| `IArchitect` | architect overlay of foci | `Level`, `Player` |
| `IScribeTools` | research table ink | – |
| `IWandable` | wand right click on a block or block entity | `Level`, `Player` |
| `IInfusionStabiliser` | infusion stability around the altar | `canStabaliseInfusion(Level, BlockPos)` |
| `IEssentiaTransport`, `IAspectContainer`, `IAspectSource` | tubes, golems, infusion, jars | `Direction` |
| `IEssentiaContainerItem` | phials, crystals, jars as items | – |
| `IEldritchMob`, `ITaintedMob` | targeting and taint rules | – |
| `IBauble` | the built-in amulet, ring and belt slots | `LivingEntity`; new `equipsOnUse(stack, player)` (default `true`) keeps an item's right click for itself instead of the hand equip |
| `IFocusPouch` | the wand's focus selector: the foci of every pouch in the inventory and the accessory slots | new; the 1.7.10 `getInventory`/`setInventory` of `ItemFocusPouch`, which was not API, as an interface (up to 128 cells) |

`BaublesApi.getWorn(entity)` is the read-only "is X worn" view of the accessory slots (copies, slot order, no empty
slots); the Baubles mod's `BaublesApi.getBaubles(player)` answered the live inventory, which this port does not
expose. An item that was a bauble **and** a focus pouch in 1.7.10 (Thaumic Tinkerer's Bottomless Pouch extended
`ItemFocusPouch`) implements `IBauble` and `IFocusPouch`; a right click never puts a pouch on, it runs the
pouch's own `use`. Guides: [gear and baubles](guide/gear-and-baubles.md#a-bauble-that-keeps-its-right-click),
[focus pouches](guide/wands-and-foci.md#focus-pouches-ifocuspouch). Tests: `BaublesAndPouchApiTest`,
`FocusSelectionExtendedCellsTest`, GameTests of `BaublesApiRuntimeChecks` and `AddonPouchRuntimeChecks`.

## 10. Golems and machines (`thaumcraft.api.golems`, `thaumcraft.api.machines`)

A 1.7.10 add-on that read a golem or a machine (Thaumic Tinkerer's ComputerCraft and OpenComputers peripherals and its
Golem Connector) used the internal `EntityGolemBase`, `TileSensor`, `TileJarBrain`, `TileDeconstructionTable` and
`TileArcaneBore` and their public fields. Those classes are not API; the same reads and writes are:

| 1.7.10 (internal) | 1.21.1 |
|---|---|
| `entity instanceof EntityGolemBase` | `GolemApi.isGolem(entity)`; `GolemApi.view(entity)` answers an `Optional<GolemView>` snapshot |
| `golem.getGolemType()` | `view.materialId()` (original ordinal), `view.material()` (`GolemMaterial`; `displayName()` is `"Straw"` … `"Thaumium"`) |
| `golem.getCore()` | `view.core()`: -1 (`GolemView.NO_CORE`) or the `ItemGolemCore` damage 0..11 |
| `golem.decoration` | `view.decoration()`: the accessory letters `HGBFRVPM` in the order they were put on |
| `golem.getHealth()`, `golem.posX/posY/posZ` | `view.health()`, `view.position()` |
| `golem.getHomePosition()`, `golem.homeFacing` | `view.home()`, `view.homeFacing()` (a `Direction`; `get3DDataValue()` is the old number) |
| `golem.getMarkers()` (`Marker x, y, z, dim, side, color`) | `view.markers()`: `GolemView.Marker(pos, dimension, side, color)`, a `ResourceKey<Level>` and a `Direction` |
| `golem.setHomeArea(x, y, z, 35); golem.homeFacing = f` | `GolemApi.setHome(entity, pos, facing)` |
| `golem.setMarkers(list)` | `GolemApi.setMarkers(entity, markers)` |
| `TileSensor.note` (read and write) | `IArcaneEar.getNote()`, `IArcaneEar.setNote(int)` |
| `TileJarBrain.xp`, `xpMax` | `IBrainJar.getXp()`, `IBrainJar.getXpMax()` |
| `TileDeconstructionTable.aspect`, `getStackInSlot(0)` | `IDeconstructor.getReward()`, `IDeconstructor.hasItem()` |
| `TileArcaneBore.hasFocus`, `hasPickaxe`, `getStackInSlot(1)`, `gettingPower()`, `area + maxRadius`, `speed` | `IArcaneBore.hasFocus()`, `hasPickaxe()`, `getPickaxe()`, `isPowered()`, `getRadius()`, `getSpeed()` |
| the peripheral's own formulas over the bore's pickaxe | `IArcaneBore.isPickaxeBroken()`, `isWorking()`, `hasNativeClusters()`, `getPickaxeFortune()`, `hasSilkTouch()` |

Ask a block entity with `level.getBlockEntity(pos) instanceof IArcaneEar ear`; Thaumcraft's own four block entities are
the only implementations. What changed:

- **Threads.** A golem or machine of a server level belongs to the server thread. `GolemApi` (reads and writes) and
  `IArcaneEar.setNote` throw `IllegalStateException` on another thread; the machine getters are plain reads to be called
  on the same thread. A ComputerCraft peripheral runs its calls on the main thread (`mainThread = true`).
- **Writes go through the owner's own code.** `setHome` is the golem placer's `setHome` (work in progress that depends on the
  home is dropped, the home is synced and saved); `setMarkers` is the bell's (the inventory cores re-plan; the markers are
  saved, and stay on the server as they did in 1.7.10); `setNote` is the hand tuning's save and sync, without playing the
  note. Both golem writes answer `false` and change nothing when the golem refuses: a coloured marker on a golem without
  the Order upgrade, a list of more than 64 markers or with a `null`, a home outside the world, a removed or unsupported
  golem. The original wrote any value.
- **Values.** `setNote` takes 0..24 and throws `IllegalArgumentException` for another note (the original stored any byte,
  which the ear never heard). A marker's dimension is a `ResourceKey<Level>` instead of a number; markers of other
  dimensions are kept, as before, and the golem works only in its own. The home radius is 32, the radius every
  original golem got on load (the connector's 35 lasted until the next load; nothing in this port reads it). The original
  peripheral's `getRadius` answered the width of the hole, `1 + 2 * IArcaneBore.getRadius()`.

Tests: `GolemAndMachineApiTest`, GameTests of `GolemMachineApiRuntimeChecks` (`golemApi…`, `machineApi…`): every answer
against the real golem or block entity, every write against its state, the vanilla sync packet and a save and reload, and
every refusal. A client receiving the data was not run.

## 11. Effects, and what is not public API

- `ThaumcraftFX` sends the effect packets 1.7.10 add-ons built themselves (`PacketFXBlockSparkle`,
  `PacketFXBlockArc`, `PacketFXBlockZap`, `PacketFXBlockBubble`, `PacketFXEssentiaSource`, `PacketFXVisDrain`)
  from the server, and since 2026-10-28 four more for the client facade (`sparkle`, `burst`, `blockRunes`, `smokeSpiral`).
- The client-side particles of `Thaumcraft.proxy` are `thaumcraft.api.client.ThaumcraftClientFX`: `sparkle`, `burst`, three
  `wisp` forms, `blockRunes`, the continuous `beam`, `smokeSpiral`, `essentiaTrail` and `particleCount`. It draws only on a
  client level (a server level is a no-op, so shared code may call it), checks its arguments on every side, and does not
  provide the homing wisp, `beamPulseFX`, `bolt`, `arcLightning`, `crucibleBubble` or the returned beam object. `AspectRenderer`
  is the client-only `UtilsFX.drawTag` (the amount is an `int`). Both are unit tested and unverified on a frame: [client effects](guide/world-and-content.md#client-effects-particles),
  [aspect tags](guide/world-and-content.md#aspect-tags-in-a-screen). Tests: `ClientFxApiTest`, `ClientFxPayloadTest`,
  `ClientFxParticleRulesTest`, `AspectRendererTest`, `AspectTagLayoutTest`, GameTest `clientFxServerTwinsCheckAndSendOnTheServerThread`.
- Stable keys ([guide](guide/world-and-content.md#stable-keys)): `WarpEffects`, `ThaumcraftEntities`, the armour material
  holders of `ThaumcraftMaterials`/`ThaumcraftApi` and `ThaumcraftSoundIds` replace the numeric potion ids, `instanceof` on
  internal entity classes and sound names in strings. Tests: `StableKeysApiTest`, GameTest `stableKeysResolveToTheRegisteredObjects`.
- Golems had no API in 1.7.10; [section 10](#10-golems-and-machines-thaumcraftapigolems-thaumcraftapimachines) is the one they have now.
- The Thaumcraft 6 shaped classes of `thaumcraft.api` (theorycraft, `ResearchEntry`/`ResearchStage`,
  `ScanningManager`, `IWandFocus`) are not wired into Thaumcraft 4 gameplay; use the TC4 classes above.
- Numeric enchantment ids, numeric dimensions and arbitrary item NBT are 1.7.10 identities; the
  deprecations in the Javadoc name the replacement. The contract details are in [the README](README.md).

## 12. The example add-on

[`examples/example-addon`](../../examples/example-addon) registers items, a block with a block entity, an aspect of its
own, object, complex and entity tags (one by a 1.7.10 ore dictionary name), arcane, shapeless arcane, crucible and
infusion recipes through the API and as JSON files of the native types, a wand rod with an update hook and a wand cap,
a focus, an amulet bauble with a discount, runic charge and warp, a talisman bauble that keeps its sneaking right click
(`IBauble.equipsOnUse`) and reads `BaublesApi.getWorn`, a belt that is also a focus pouch (`IFocusPouch`), an essentia
container that feeds tubes and answers a wand, an item that grants research points from a data component that holds
aspects (the codecs of `AspectList`), a Tome of Sharing that copies scans and discovered aspects between players, a hook
that fells a tree with `isWoodLog`, `findFurthestBlock` and `breakFurthestBlock` (asking its own rule about the cell that breaks), a kill hook that uses the stable keys of a wisp, the warp ward and the wand sound, a client entrypoint
that draws sparkles, runes and a beam while the amethyst core is held and the aspect tags of the held item on the HUD, and a
research tab with nine entries. It imports nothing but
`thaumcraft.api` (`ExampleAddonApiOnlyTest` keeps it so) and is compiled by every build; the guides quote it.
`./gradlew :fabric:runExampleClient` starts a development client with it.
