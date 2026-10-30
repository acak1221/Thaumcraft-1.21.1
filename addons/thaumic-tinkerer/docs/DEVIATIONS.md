# Deviations from Thaumic Tinkerer 1.7.10: overview

This is the entry point. Each of the eight feature groups has its own document with every difference from the pinned original
(GTNewHorizons/ThaumicTinkerer at `28f4def938e80d7a0b6f433c2b84300dfb7c35ce`, Minecraft 1.7.10) and the evidence for it. This page only
groups the kinds of difference that run through all of them, and lists what was **not** ported. Values that were copied on purpose
(costs, aspects, research positions, formulas, config switches) are not listed anywhere as deviations: they are the same as in the
original, and `docs/PARITY_REPORT.md` compares the research and recipe definitions with the source mechanically.

| Group | Document | Registrar |
|---|---|---|
| Simple blocks (dark quartz, slabs, gases, platform, forcefield, infused farmland and seeds, elemental fires, imbued effects) | [SimpleBlocks](deviations/SimpleBlocks.md) | `content/SimpleBlocks` |
| Tile-entity blocks (Animation Tablet, Remote Placer, Levitational Locomotive and Relay, Golem Connector, Transvector blocks) | [TileBlocks](deviations/TileBlocks.md) | `content/TileBlocks` |
| Machines (Aspectalyzer, Thaumic Restorer, Essentia Funnel, attractors, Tablet of Necromancy, Cursed Spirit's Blade) | [Machines](deviations/Machines.md) | `content/Machines` |
| Basic items (Spellbinding Cloth, talismans, Tome of Knowledge Sharing, Scribing Tools, Helmet of Revealing) | [BasicItems](deviations/BasicItems.md) | `content/BasicItems` |
| Wand foci (seven ordinary foci, Dislocation HUD) | [Foci](deviations/Foci.md) | `content/Foci` |
| Enchantments (fourteen enchantments, Osmotic Enchanter) | [Enchantments](deviations/Enchantments.md) | `content/Enchantments` |
| Kami gear (Ichorium tools and armour, Placement Mirror, Bottomless Pouch, Feline Amulet, Kami foci) | [KamiGear](deviations/KamiGear.md) | `content/KamiGear` |
| Kami world (Bedrock dimension and portal, Celestial Gateway, Soul Hearts, dimensional shards) | [KamiWorld](deviations/KamiWorld.md) | `content/KamiWorld` |

Related: [PORTING_RULES](PORTING_RULES.md) (the rules), [API_GAPS](API_GAPS.md) and [api-requests/](api-requests/) (what the add-on needed
from the Thaumcraft API), [HOOKS](HOOKS.md) (loader events), [lang-key-families](lang-key-families.md) (language keys).

## 1. Differences that every group shares (forced by 1.21.1)

* **Metadata is gone.** A 1.7.10 block or item with several metadata values becomes several blocks, block states or items with
  `snake_case` registry paths (`LibBlockNames`, `LibItemNames`); damage values that carried data become **data components** on the stack.
  The per-group documents have the mapping tables (`SimpleBlocks` and `TileBlocks` section 1, `Machines` section 1, `BasicItems` section 1).
* **Enchantments are data.** In 1.21.1 an enchantment is a JSON entry in a data pack. The fourteen Thaumic Tinkerer enchantments are
  JSON files with empty `effects`; their behaviour is Java code that looks the enchantment up by `ResourceKey<Enchantment>`
  (`Enchantments.md` section 1).
* **Textures and models.** The 1.7.10 textures are converted deterministically by `scripts/porting/tt_assets.py` (the map is
  `scripts/porting/tt_assets_map.json`, with provenance and a `--check` mode); block and item models are written as 1.21.1 JSON.
* **Recipes.** Crafting-table recipes are data files under `data/thaumictinkerer/recipe/`; arcane, crucible and infusion recipes are
  registered through the Thaumcraft API at start-up, as the original did through Thaumcraft 4's API.
* **Random ticks.** `BlockBehaviour.randomTick` does nothing by default in 1.21.1, whereas in 1.7.10 `updateTick` served both the
  scheduled and the random tick. The elemental fires and the gases therefore delegate `randomTick` to `tick` (found by the first GameTest
  run: an old fire never burned out; `SimpleBlocks.md` 2.3 and 2.9).
* **Mock and fake players.** The Animation Tablet and the Remote Placer act through a fake player. Its effects reach other mods only
  through the vanilla paths the fake player really takes (`state.getDestroyProgress`, `attack`).
* **Registry names** are lower-case; the original's mixed-case ids appear only in `LibItemNames`/`LibBlockNames` comments.
* **Config** is JSON (`config/thaumictinkerer.json`, `ConfigHandler`), not a Forge `.cfg`; the switches and defaults are the original's.
  There is a `/maxresearch` debug command (the original's `/maxresearch` equivalent: 99 of every aspect), only when
  `enableDebugCommands` is on.

## 2. Deliberate decisions of the port

* **Research cell of the Remote Placer** moved from (-6,3) to (-8,3) because Thaumcraft 4 Reload's own Magnets entry occupies (-6,3); the
  original relied on Thaumcraft silently dropping one of the two. A research that Thaumcraft drops because its cell is taken is now a
  registration failure (`ThaumicTinkerer.failures()`), so a clash cannot stay unnoticed (`TileBlocks.md`, last section).
* **Cleansing Talisman** removes harmful effects in registry-id order. In 1.7.10 the potion ids were small integers in a map with a fixed
  order; in 1.21.1 the active effects sit in a map keyed by holder identity, so without an explicit order the result would change from run
  to run (`BasicItems.md` section 3).
* **Relay partner out of reach.** A Levitational Relay whose partner lies beyond the search range ends the link at once. The original
  loaded the partner's chunk and cleared the link when it found no relay; the port does not ask unloaded chunks, and a stale link made the
  beam loops walk billions of cells (`TileBlocks.md` 2.3).
* **Wand cap and rod textures** derive their default path from the tag in lower case (a change in the Thaumcraft API: tags such as
  `ICHOR` are legal there, resource paths are not).
* **Licence split.** The port is CC BY-NC-SA 3.0 and lives in its own directory with its own licence file (`NOTICE.md`); the Thaumcraft
  mod stays MIT. The add-on's public-API additions in the mod (for example `IBauble.equipsOnUse`, `IFocusPouch`, `BaublesApi.getWorn`,
  `ThaumcraftApiHelper` scan and discovery helpers, `WarpEffects`) contain no Thaumic Tinkerer code.

## 3. Behaviour that depends on loader events

Architectury has no events for several things the original used Forge events for. The groups first worked around each gap (the
workarounds and what they lose are written down in the per-group documents and in `api-requests/`). `docs/HOOKS.md` describes the loader
layer that closes the gaps (`LIVING_DAMAGE`, `BREAK_SPEED`, `LIVING_JUMP`, `LIVING_FALL`, `USE_ITEM_TICK`, `LIVING_DROPS`, floating ticks,
item renderers). It exists for **Fabric** (Mixins); the NeoForge counterpart is specified there but not written. Section 10 of that document
says, per group, which workaround is replaced by which event; whichever of them is still a workaround in a group document is marked there.

## 4. Not ported (kept visible)

* **Other mods' integrations**: Waila (now Jade; the gateway tooltip and the other providers), NEI (recipe pages for infused seeds and
  necromancy would be EMI/JEI categories), Forge Multipart parts (candle and Nitor parts), EnderStorage page of the Ender Rift focus, Botania,
  Blood Magic, AE2 (`IMovableTile`, passenger handling), Tinkers' Construct repair, GregTech accelerator text, EnderIO planter,
  Electro-Magic Tools hoe.
* **Computer peripherals** (ComputerCraft / OpenComputers) of the Animation Tablet, Transvector Interface, Golem Connector, Aspectalyzer and
  the attractors. The method tables exist as plain Java; nothing registers them. The `peripherals` research category exists and is empty.
* **Golem API** (Golem Connector data): API request A11/A12 is deferred; the Golem Connector block exists without golem data.
* **Configurable dimension id and disabling the Bedrock dimension** (a datapack dimension has a resource id and is switched off by the
  data pack), and the truncated damage for players without Soul Hearts (`KamiWorld.md` section 9).
* **Armour absorption of Kami armour** (`ISpecialArmor`): needs a hook on `getDamageAfterArmorAbsorb`; not written (`HOOKS.md` section 3).
* **Multiblock diagram page** of the Levitational Locomotive research, EnderStorage page of the Ender Rift research.
* **NeoForge**: the add-on has no NeoForge module yet.

## 5. Not verified

These parts compile and are covered by unit or resource tests, but **have not been seen working in a running game client**: models,
item and block rendering (camouflage, Mob Display, the Tablet's overlays), screens, particles, sounds, HUD (Dislocation overlay), and the
network behaviour of the packets in multiplayer. GameTests run on a dedicated server and cannot show these. The per-group documents list
which individual checks were run, how, and with what result.
