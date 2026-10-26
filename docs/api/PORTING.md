# Porting a Thaumcraft 4 add-on to Thaumcraft 4 Reload (Minecraft 1.21.1)

Thaumcraft 4 Reload keeps the public API of Thaumcraft 4.2.3.5: every class of `thaumcraft.api` that
1.7.10 add-ons compiled against exists under the same package and name, with the same methods where
1.21.1 allows it. A 1.7.10 add-on is ported, not loaded: its code still has to move to 1.21.1 itself
(items, blocks, rendering, networking). This guide is about the Thaumcraft side of that move: what stays,
what changed and what to call instead. The working example is
[`examples/example-addon`](../../examples/example-addon) (run it with `./gradlew :fabric:runExampleClient`).

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
| `ThaumcraftApi.armorMatThaumium` field | `ThaumcraftApi.armorMatThaumium()` |
| numeric enchantment ids | `ResourceKey<Enchantment>` constants beside the old fields |
| `ConfigItems.itemResource` + meta, `ConfigBlocks.*` | `ItemsTC` / `BlocksTC` / `ItemApi`, see section 4 |
| `Thaumcraft.proxy.getResearchManager()`, `PlayerKnowledge` | `ThaumcraftApiHelper`, see section 4 |
| `ItemWandCasting` methods | `thaumcraft.api.wands.WandHelper`, see section 8 |

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

Tests: GameTest `addonResearchJoinsGameplay` (completion, warp, completed set).

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

<!-- ASPECTS-SECTION -->

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
  `COMPOUND_CRAFTING` (the wand construct diagrams of the original) is not drawn.
- Changing an original entry through its public view works as it did:
  `ResearchCategories.getResearch("NITOR").setSiblings("MY_KEY")` is laid over the original entry.
- An add-on tab follows the six original ones, in registration order, as in the original.
- Item triggers compare the item and its data, the damage of durable items ignored; the ore dictionary
  shortcut of the original has no counterpart, so list every item of a family.

Tests: GameTest `addonResearchJoinsGameplay`; the book was checked in a client with the example add-on
([screenshots](../analysis/2026-10-26-addon-api-ru.md)).

<!-- WANDS-SECTION -->

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
| `IBauble` | the built-in amulet, ring and belt slots | `LivingEntity` |

## 10. What is not public API

- The particle effects of `Thaumcraft.proxy` (sparkles, beams, bolts, wisps) are client internals of
  `thaumcraft.client.fx`; there is no stable facade yet. Use vanilla particles or your own.
- Golems had no API in 1.7.10 and have none now.
- The Thaumcraft 6 shaped classes of `thaumcraft.api` (theorycraft, `ResearchEntry`/`ResearchStage`,
  `ScanningManager`, `IWandFocus`) are not wired into Thaumcraft 4 gameplay; use the TC4 classes above.
- Numeric enchantment ids, numeric dimensions and arbitrary item NBT are 1.7.10 identities; the
  deprecations in the Javadoc name the replacement. The contract details are in [the README](README.md).

## 11. The example add-on

[`examples/example-addon`](../../examples/example-addon) registers an item, object tags, an arcane, a crucible
and an infusion recipe, a research tab with three entries (a parent of the original tree, recipe, aspect
and smelting pages, a hidden entry revealed by scanning, a warp value) and a data-driven recipe page. It
imports nothing but `thaumcraft.api` (`ExampleAddonApiOnlyTest` keeps it so) and is compiled by every
build. `./gradlew :fabric:runExampleClient` starts a development client with it.
