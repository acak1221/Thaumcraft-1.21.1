# Getting started

Thaumcraft 4 Reload keeps the public API of Thaumcraft 4.2.3.5 under `thaumcraft.api`, with the types of
Minecraft 1.21.1. This page sets up an add-on project, shows where the Thaumcraft registrations go and lists the
units every other page relies on. Every Java snippet in these guides is copied from the example add-on
([`examples/example-addon`](../../../examples/example-addon)), which every build of this repository compiles, so
the code you read here is code that compiles against the current API.

Moving an existing 1.7.10 add-on? Read the [porting guide](../PORTING.md) next to this one: it maps the old calls to
the new ones.

## Project setup

The mod is built with Mojang mappings and Java 21 for Minecraft 1.21.1 on Fabric (Loader 0.16.14+, Fabric API) and
NeoForge 21.1, both with Architectury API 13.0.11+.

Install the mod into your local Maven repository from a checkout of this repository:

```bash
./gradlew :fabric:publishToMavenLocal :neoforge:publishToMavenLocal
```

or put the release jar into your project's `libs/`. The `-sources.jar` next to it carries the sources of the shared
code, `thaumcraft.api` among them, so your IDE shows the documented API.

**Fabric (Loom).** Loom remaps the mod to your mappings, so Yarn and Mojang mappings both work.

```gradle
repositories { mavenLocal() }
dependencies {
    modImplementation "thaumcraft:Thaumcraft-fabric:0.1.0-alpha.7"   // or: modImplementation files("libs/Thaumcraft-fabric-0.1.0-alpha.7.jar")
    modImplementation "dev.architectury:architectury-fabric:13.0.11"
}
```

**NeoForge.** NeoForge runs on Mojang names, so the release jar is used as it is:
`implementation "thaumcraft:Thaumcraft-neoforge:0.1.0-alpha.7"`, plus a `[[dependencies.<yourmod>]]` entry for
`modId = "thaumcraft"` in `neoforge.mods.toml`.

The example add-on's `fabric.mod.json` declares its entrypoints (`main` and `thaumcraft` are explained below; `client` is the class that may name client classes, see [client effects](world-and-content.md#client-effects-particles)) and the dependency:

<!-- file:examples/example-addon/src/main/resources/fabric.mod.json -->
```json
{
  "schemaVersion": 1,
  "id": "tc4_example",
  "version": "1.0.0",
  "name": "Thaumcraft 4 Reload example add-on",
  "description": "A small add-on written only against the public Thaumcraft 4 Reload API (thaumcraft.api). Development runs only.",
  "authors": ["Thaumcraft 4 Reload contributors"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.example.tc4addon.ExampleAddon"],
    "client": ["com.example.tc4addon.ExampleAddonClient"],
    "thaumcraft": ["com.example.tc4addon.ExampleAddon"]
  },
  "depends": {
    "fabricloader": ">=0.16.14",
    "minecraft": "1.21.1",
    "thaumcraft": "*"
  }
}
```
<!-- /file -->

## Where the registrations go

Register your items, blocks and block entity types as any 1.21.1 mod does. Register everything that belongs to
Thaumcraft (aspects, object tags, recipes, research, wand parts) in `IThaumcraftAddon.onThaumcraftReady()`:

<!-- snippet:lifecycle -->
```java
public final class ExampleAddon implements ModInitializer, IThaumcraftAddon {
    public static final String MOD_ID = "tc4_example";
    // ...
    /** Fabric "main": only the add-on's own registry objects. Thaumcraft's items may not exist yet. */
    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, id("vis_shard"), VIS_SHARD);
        // ...
    }

    /** The "thaumcraft" entrypoint: every mod's items exist, Thaumcraft's registries are open. Runs on both sides. */
    @Override
    public void onThaumcraftReady() {
        aspects();
        recipesAndResearch();
        wandParts();
        moreContent();
    }
```
<!-- /snippet -->

- **Fabric:** list the class under the `"thaumcraft"` entrypoint, as above. Fabric runs the `main` entrypoints of
  mods in no particular order, so a Thaumcraft item may not exist yet when your `onInitialize` runs.
- **NeoForge:** annotate the class with `@IThaumcraftAddon.Addon` and give it a public constructor without
  arguments. It is called from the main-thread queue of `FMLCommonSetupEvent`.
- **Either loader:** `ThaumcraftApi.whenReady(Runnable)` from your own initializer runs the callback at the same
  moment (at once if that moment has passed). `ThaumcraftApi.isReady()` tells whether it has.

```java
// NeoForge: the same class, found by the annotation instead of an entrypoint.
@IThaumcraftAddon.Addon
public final class MyAddon implements IThaumcraftAddon {
    @Override public void onThaumcraftReady() { /* aspects, tags, recipes, research, wand parts */ }
}
```

The hook runs once, on the main thread, on **both** physical sides, after every mod registered its items and
Thaumcraft registered its own content, and before a world loads. Register the same content on the client and on the
server: research keys, aspects and wand parts are looked up by name on both sides. The order among add-ons is not
specified. The registries of the API are not concurrent: do not touch them from parallel NeoForge mod constructors.

## Units

Most porting mistakes are unit mistakes. These are the units of the original, kept unchanged:

| Value | Unit |
|---|---|
| Arcane recipe cost (`addArcaneCraftingRecipe`, JSON `"vis"`) | whole vis of the six primals |
| Crucible, infusion and enchantment costs, jars, tubes, phials | essentia units |
| `WandRod`/`StaffRod` capacity | whole vis |
| Vis stored in a wand, `WandHelper.getVis`/`getAllVis`/`getMaxVis`/`addRealVis`/`consumeVis`/`consumeAllVis` | centivis (1 vis = 100) |
| `WandHelper.addVis`, `consumeAllVisCrafting` | whole vis |
| Focus cost (`ItemFocusBasic.getVisCost`) | centivis |
| Focus `getActivationCooldown` | milliseconds of real time |
| `VisNetHandler.drainVis` | centivis |
| `WandCap` discount | multiplier (0.9 = 10 % cheaper) |
| `IVisDiscountGear.getVisDiscount` | percentage points |

## Language keys

Thaumcraft builds its names from translation keys, as in 1.7.10 (now in `assets/<modid>/lang/en_us.json`):

| Key | Used for |
|---|---|
| `tc.research_category.<CATEGORY>` | the tooltip of a research tab |
| `tc.research_name.<KEY>`, `tc.research_text.<KEY>` | the name and the subtitle of a research entry |
| any key you pass to `new ResearchPage(String)` | the text of a page (markup `<BR>`, `<LINE>`, `<IMG>...</IMG>`) |
| `tc.aspect.<tag>`, `tc.aspect.help.<tag>` | the name and the meaning of an aspect |
| `item.Wand.<tag>.rod`, `item.Wand.<tag>.cap` | the words a wand's name is made of |

## The example add-on

[`examples/example-addon`](../../../examples/example-addon) uses only `thaumcraft.api` (the test
`ExampleAddonApiOnlyTest` keeps it so) and shows:

| Feature | Where | Guide |
|---|---|---|
| lifecycle, items, a block with a block entity | `ExampleAddon.java` | this page, [essentia and vis](essentia-and-vis.md) |
| an aspect, object, complex and entity tags | `ExampleAddon.aspects()` | [aspects](aspects.md) |
| arcane, shapeless arcane, crucible and infusion recipes; JSON recipes | `ExampleAddon.recipesAndResearch()`, `data/tc4_example/recipe/` | [recipes](recipes.md) |
| a research tab with nine entries (one of them hidden) | `ExampleAddon` | [research](research.md) |
| a wand rod with an update hook, a wand cap, a focus | `ExampleAddon.wandParts()`, `SparkFocus.java` | [wands and foci](wands-and-foci.md) |
| an essentia container, transport and wand-clickable block | `EssentiaBowlBlock.java`, `EssentiaBowlBlockEntity.java` | [essentia and vis](essentia-and-vis.md) |
| an amulet with discount, runic charge and warp | `OrderAmulet.java` | [gear and baubles](gear-and-baubles.md) |
| a bauble that keeps its right click, and the list of what is worn | `OrderTalisman.java` | [gear and baubles](gear-and-baubles.md#a-bauble-that-keeps-its-right-click) |
| a focus pouch that is also a belt, read by the wand's focus selector | `FocusBelt.java` | [wands and foci](wands-and-foci.md#focus-pouches-ifocuspouch) |
| research points and research completion from an item | `InsightScroll.java` | [research](research.md) |
| a data component that holds aspects (codecs) | `StoredAspects.java`, `ExampleAddon` | [aspects](aspects.md#aspects-in-data-components-data-files-and-packets) |
| copying scans and discovered aspects between players | `ShareTome.java` | [research](research.md#scans-discovered-aspects-and-the-tome-of-sharing) |
| felling a tree with the Axe of the Stream's helpers | `ExampleAddon.onInitialize()` | [world and content](world-and-content.md#felling-trees) |
| warp on crafting, smelting bonus, loot bags | `ExampleAddon.moreContent()` | [world and content](world-and-content.md) |
| Thaumcraft's creatures, effects and sounds by key, never by string | `ExampleAddon.java` (the death hook) | [world and content](world-and-content.md#stable-keys) |
| particles (sparkle, runes, beam) and aspect tags drawn on the client | `ExampleAddonClient.java` | [world and content](world-and-content.md#client-effects-particles), [aspect tags](world-and-content.md#aspect-tags-in-a-screen) |

Run it in a development client:

```bash
./gradlew :fabric:runExampleClient            # add -PwithEmi to see the recipes in EMI
```

`/thaumcraft research grant EXAMPLE_SHARDS` (and the other `EXAMPLE_*` keys) unlocks the entries in a creative world.

## Where to look next

- The [API reference](../reference/README.md): every public type and member, generated from the Javadoc.
- [LEGACY_IDS.md](../LEGACY_IDS.md): the 1.21.1 ids of the original `ConfigItems`/`ConfigBlocks` fields.
- [The contracts](../README.md#contracts) of the API that differ from a straight 1.7.10 reading.
