# BasicItems: requests to the Thaumcraft API and to the other groups

Group BasicItems: Spellbinding Cloth, Talisman of Remedium, Talisman of Withhold, Tome of Knowledge Sharing, Infused
Scribing Tools, Helmet of Revealing, `ExperienceHelper`, `ItemNBTHelper`, `MiscHelper`, `HUDHandler`, `ClientTickHandler`.
The numbers `A1`..`A13` and `N1`..`N4` are those of `docs/API_GAPS.md` section 2. Nothing in the group reaches into
`thaumcraft.common.*` or `thaumcraft.client.*` (the test `TinkererApiOnlyTest` checks it): each item below is either already
answered by the API or a named workaround with the loss written down. Priorities are the group's own opinion (P1 changes
what the player sees, P3 only hardens something that works today).

## 0. Requests that the API now answers (verified in the source of 2026-10-28, used by the group)

| Request | Answer in the API | Used in |
|---|---|---|
| A1 `IBauble.equipsOnUse(ItemStack, Player)` | `thaumcraft.api.baubles.IBauble.equipsOnUse` | `ItemXPTalisman` (the click bottles and toggles) answers `false`; `ItemCleansingTalisman` answers `!player.isShiftKeyDown()` |
| A3 `discoverAspect`, `getCompletedResearch`, `completeResearch` | `ThaumcraftApiHelper` | `ItemShareBook` (snapshot and hand-over of research and aspects) |
| A4 `Aspect.CODEC` / `AspectList.CODEC` | `thaumcraft.api.aspects` | not needed: the tome stores the tags as strings (a sorted list of tags, as 1.7.10 did) |
| A7 `WarpEffects`, `ThaumcraftSoundIds` | `thaumcraft.api.potions.WarpEffects`, `thaumcraft.api.ThaumcraftSoundIds` | `ItemCleansingTalisman` (the five effects that cost ten points, the warp ward, `WAND`), `ItemXPTalisman` (`ZAP`) |
| A10 `BaublesApi.getWorn` | `thaumcraft.api.baubles.BaublesApi` | not needed (the 1.7.10 code removed a broken talisman from the first Baubles slot; `hurtAndBreak` empties the live stack the slot holds) |
| `IScribeTools`, `IGoggles`, `IRevealer`, `IVisDiscountGear`, `IRunicArmor`, `IRepairable` | `thaumcraft.api` | `ItemInfusedInkwell`, `ItemRevealingHelm` |
| `ThaumcraftApiHelper` legacy ids (`ItemApi.getItem`) | `docs/api/LEGACY_IDS.md` | every recipe of the group (`itemResource:1/2/3/7`, `itemZombieBrain`, `itemInkwell`, `itemShard:0`, `blockJar`, `itemGoggles`, `itemHelmetThaumium`) |

## R1. Research clue markers for the Tome of Knowledge Sharing (P3)

**Need.** The 1.7.10 tome copied the whole list `ResearchManager.getResearchForPlayer(name)`, which also held the `@KEY`
entries that a scan wrote to reveal a hidden research entry. A research that the giver had only revealed (not completed)
arrived revealed at the receiver.

**What the add-on does today.** It copies the completed research (`ThaumcraftApiHelper.getCompletedResearch`, which leaves the
`@KEY` markers out) and the discovered aspects. A research that was completed arrives completed, with its parents and
siblings; a research that was only revealed does not arrive revealed.

**Proposed signature.**
`Set<String> ThaumcraftApiHelper.getResearchClues(Player player)` (the keys without the `@`) and
`int ThaumcraftApiHelper.addResearchClues(ServerPlayer player, Set<String> keys)` (server, quiet, syncs once, returns how many were
new, ignores unknown keys), as the scan markers have `getScans` / `addScans`.

**Backed by.** `KnowledgeAccess.get(player).isResearchComplete("@" + key)`, `ResearchClues` (the clue draw) and the knowledge
sync (`KnowledgeEvents.sync`); the example add-on's tome of sharing copies scans with the existing pair.

**Stub in the add-on.** None needed: `ItemShareBook.snapshot` / `load` are the two places to extend (one line each).

## R2. Client particles of Thaumcraft for the Talisman of Withhold (A8, P3)

**Need.** 1.7.10 spawned a green wisp (`Thaumcraft.proxy.wispFX`, size `xp / 5`, colour 0.1/0.9/0.1) at every absorbed orb and
six sparkles (`sparkle(x, y, z, 3)`) at the player when a bottle is filled.

**What the add-on does today.** A green vanilla dust particle at the orb (client side of `onWornTick`) and six happy-villager
particles at the player (`ServerLevel.sendParticles`). Same moments and places, other look.

**Proposed.** The client-only facade of A8, `thaumcraft.api.client.ThaumcraftClientFX.sparkle(Level, x, y, z, int colorIndex)` and
`wisp(Level, x, y, z, float size, float r, float g, float b)`, with a server sender in `ThaumcraftFX`.

## R3. The Thaumcraft wand dial position for the Focus of Dislocation overlay (P3)

**Need.** `HUDHandler` of 1.7.10 put the overlay at the left edge, at `y = 50` or `height - 70` when Thaumcraft's `Config.dialBottom`
was on (the dial of the wand's own HUD is at the other end).

**What the add-on does today.** Always `y = 50`. If the port's own wand HUD sits there, the two overlap.

**Proposed.** `ThaumcraftApi.isWandDialAtBottom()` (client value, as `isMirrorsAllowed` is on the client), or a note in the guide
that the port's wand HUD never uses the left edge.

## R4. `IRepairable` needs a tag in 1.21.1: document it (docs, P2)

**Need.** In 1.21.1 the Repair enchantment is available for the items of the tag `thaumcraft:enchantable/repair`
(`data/thaumcraft/enchantment/repair.json` `supported_items`); an `IRepairable` item that is not in the tag cannot get the
enchantment from the table, the anvil or the infusion. The guide (`gear-and-baubles.md`) says an item "that implements it mends
itself" but not that the tag has to list it.

**What the add-on does.** `data/thaumcraft/tags/item/enchantable/repair.json` lists `thaumictinkerer:revealing_helm` (this
group) and `thaumictinkerer:blood_sword` (the Machines group's `IRepairable` item).

**Proposed.** One sentence in the guide, or (better) the mod adds every item that implements `IRepairable` to the tag at
`TagsUpdatedEvent` / server start, so that an add-on does not need the data file.

## S. Contracts with the other groups and shared files (for the lead)

- **Focus of Dislocation overlay (group Foci).** Both this group (`HUDHandler`, contract `IDislocationHudSource`) and the Foci
  group (`DislocationHUDHandler`, registered by `FociClient`) ported the 1.7.10 `HUDHandler.drawDislocationFocusHUD`. Mine is
  inert (it needs a source that nobody gives it) and the Foci group's draws, so nothing is drawn twice today. Pick one: (a)
  delete `HUDHandler` here and its line in `BasicItemsClient.register()` (smaller), or (b) delete the Foci group's handler and
  let `ItemFocusDislocation` implement `thaumic.tinkerer.common.item.IDislocationHudSource` (`pickedBlock(wand, focus)` is the stack
  of the picked block or empty, `hasExtraContents(wand, focus)` is "the carried block has block-entity data").
- **Tag files that two groups may want** (merge, do not overwrite): `data/thaumcraft/tags/item/enchantable/repair.json` (helm,
  blood sword), `data/minecraft/tags/item/enchantable/head_armor.json` (helm) and `data/minecraft/tags/item/enchantable/durability.json`
  (helm, Talisman of Remedium). The Kami helmets are in `data/minecraft/tags/item/head_armor.json` (KamiGear) and need nothing
  more.
- **Constants.** `LibResources.MODEL_REVEALING_HELM` (`textures/models/revealing.png`) is no longer used by anything: the worn layer
  of the helmet is `textures/models/armor/revealing_layer_1.png`.
- **Recipe serializers.** `BasicItems.RECIPE_SERIALIZERS` is a `DeferredRegister` of this group (called from
  `BasicItems.register()`); `TTRegistry` needs no change. If the lead prefers one place for all registries, move the field and the
  `register()` call.
- **ClientTickHandler.** `ToolModeHUDHandler` (KamiGear) ticks itself; `ClientTickHandler.register()` only counts
  (`ClientTickHandler.elapsedTicks`) and must not call it.
