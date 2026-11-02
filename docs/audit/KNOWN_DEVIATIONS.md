# Known deviations

Every entry here is a behaviour that differs from TC4 4.2.3.5 and is **accepted** rather than
closed. Each has the oracle citation and the reason it cannot or should not be changed. Nothing
here is a guess: an entry exists only after the oracle was read.

The scope ledger marks these in the row notes with `ACCEPTED DEVIATION`.

---

## 1. Node identity: a persisted UUID instead of `dimension:x:y:z`

**Oracle** `TileNode.generateId()` (`thaumcraft/common/tiles/TileNode.java:103-114`) derives the
identity from the position: `dim + ":" + x + ":" + y + ":" + z`.

**Native** `TileNode:63` keeps `scanId = UUID.randomUUID().toString()` and persists it
(`:869-888`).

**Why it stays.** The UUID is bound into accepted evidence — the client acceptance parses it with
`UUID.fromString`
(`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509`)
— and into every saved world. Changing the format invalidates the accepted client and multiplayer
runs.

**Observable consequence.** A node that is moved keeps its identity in the native and changes it in
the original. The scan signature differs in form but is stable per node.

---

## 2. Jar step/break audio on the brain jar and the node jar: fixed

This one was **not** accepted — it was a real defect the audit found and the leader fixed:
`jar_brain` and `jar_node` used `SoundType.GLASS` while the plain jar used the original
`CustomSoundType("jar", 1.0F, 1.0F)`. The oracle sets the sound in the `BlockJar` constructor
(`oracle BlockJar.java:88`), so all three share it. Both now use
`ThaumcraftContent.JAR_SOUND`. Recorded here because the audit named it as a deviation to accept;
it turned out to be fixable.

---

## 3. 1.7.10 font rendering

The original ships a bitmap font with its own glyph metrics. 1.21.1 has no equivalent API for
replacing the font atlas per mod without shipping a full font provider. The port uses the vanilla
font.

**Not verified:** whether the glyph widths match closely enough for the research book layout. No
frame-level comparison has been made.

---

## 4. OptiFine/Iris shader pack integration

The original reads OptiFine's shader-pack state to decide whether to draw certain effects. 1.21.1
has no equivalent hook. The port draws the effects unconditionally.

---

## 5. Particle count per layer

**Resolved in code, not a deviation.** The original evicts the oldest entry at 2000 per layer and
dimension (`oracle ParticleEngine.java:138-196`). An earlier wave recorded this as impossible; the
wave-8 lane implemented it (`Tc4ParticleEngineMixin`, `ParticleLevelAccess`,
`Tc4ParticleWorldRenderMixin`).

**Not verified:** runtime mixin injection and the eviction under load.

---

## 6. What is NOT in this file, and why

Several things were previously reported as impossible limitations and were **disproved** by later
lanes reading the actual vanilla code. They are not deviations:

| Formerly claimed impossible | Actually done |
|---|---|
| The linked mirror's view-dependent texture (`glTexGen`) | Projective core shader, using `rendertype_end_portal` as the demonstrated technique |
| The chunk regeneration queue | `ChunkRegeneration` drains ten chunks per tick with the oracle's seed |
| The scattered-feature node and `auraGen` exclusivity | `AuraGenRules` maps the native structure starts |
| The 2000-particle cap | Implemented in wave 8 |
| The ore overlay's "fast graphics" condition | It is anisotropic filtering, reproduced |

This is the pattern the whole effort kept hitting: an agent wrote "no 1.21.1 counterpart" without
opening the vanilla class, and the claim survived into the ledger. The audit's job is to keep
finding them.

---

## 6a. Original object aspects whose recipe base 1.21.1 no longer has

`ConfigAspects` registers the enchanted golden apple (`golden_apple` metadata 1) as a complex row:
the original adds `praecantatio4 sano8` to the tags generated from its 1.7.10 recipe (eight gold
blocks and an apple). 1.21.1 has no recipe for it, so the generated base is empty and the item has
the additions alone. Every other original row keeps its base (`scripts/porting/extract_config_aspects.py`
accounts for all 326 registrations of the jar).

---

## 7. Unverified, not deviated

These are not differences — they are things **nobody has looked at on a real frame or a real
world**. They must not be reported as verified until someone does:

* The excavation beam in third person and from another player; the floaty lines of the wand
  pedestal and the cultist; the fading halo of bolts and arcs.
* Runtime mixin injection on Fabric. On NeoForge it is now checked: a Mixin audit on a real
  NeoForge client (2026-10-26) applied every pending target without error, after
  `TaintOverlayChunkLayerMixin` was fixed for NeoForge's delegating `SectionCompiler.compile`
  (see `docs/analysis/2026-10-26-neoforge-client-ru.md`).
* Nether wild nodes in a freshly generated Nether.
* The chunk regeneration queue on a real saved world.
* Two physical clients plus a dedicated server on NeoForge. (On Fabric the scripted 37-step run
  passed on a fresh server world and the current code on 2026-10-26, see
  `docs/analysis/2026-10-26-bloom-blend-multiplayer-ru.md`.)

### 7.1 Now seen on a real frame (2026-10-23)

These were in the list above and have now actually been looked at. What the frames show is
recorded here so the claim is bounded by what was seen, not by a test count:

* **Research graph background** — the category sheet is the original purple nebula, not parchment,
  and it now covers the pane continuously with no crop seams. The 1:2 texel density was verified
  numerically: four source texels match the captured PNG at exactly 2×2 GUI pixels. See
  `docs/analysis/2026-10-23-agent-visualrun.md`.
* **Void robe** — worn on the body in third person, front and back: black-faced purple hood, layered
  pointed shoulder plates, long robe, split side panels, diagonal back accessory and belt pouches.
  No missing-texture checkerboard. Both frames are dark, and the floor shows hard rectangular black
  regions whose cause is **not** established; not patched speculatively.
* **Node item and jar node** — the stabilizer draws as a held gold-rimmed 3D item; the placed jar
  shows its glowing contents; the aura node draws as a bounded orb within the view.
* **Candles, gas surface, crust, eldritch stairs** — one frame carries all four: a lit candle with
  smoke, a purple crust block, dark two-step eldritch stairs and a lavender gas cell in glass.
* **The mirror** — a real native bind and place produced authoritative reciprocal links, and both
  faces show the dark purple speckled projected field with its inset border, not a blank plane.
  Static frames do not establish projective motion parity.
* **Radial focus menu** — opens on held F with the focus list, the wand label and Remove/Cancel.
* **Eldritch obelisk** — renders coherently (coherent housing, pointed caps) and no longer crashes
  the client. The tunnel effect inside it is **not** readable as a receding passage; it reads as
  faint cyan marks on dark inset faces. Left open as a real rendering gap.
* **Shimmerleaf and cinderpearl** — both decorative plants draw their crossed-quad models, the
  shimmerleaf emits its blue wisp and the cinderpearl its flame/smoke while standing on sand.


### 7.2 Now seen on a real frame (2026-10-26)

* **Architect overlay** — it was **not drawn at all**: the port drew the area with the depth test of
  an entity, exactly on the faces of the blocks it marks, so the terrain hid it
  (`docs/analysis/images/2026-10-26-architect-before.png`). The original bytecode of
  `REHWandHandler.drawOverlayBlock` switches the depth test, depth writes and culling off, blends
  `SRC_ALPHA, ONE`, grows the cube to `-0.001..1.001` and sets the texture mode `GL_ADD`; the port
  now does the same (core shader `thaumcraft:architect_overlay` for `GL_ADD`). A real Fabric frame
  shows the 7x7 warded-glass shell of an Equal Trade area and the axis arrows
  (`docs/analysis/images/2026-10-26-architect-after.png`). Not reproduced: the lightmap value
  `200` of the original tessellator; the area is drawn at full brightness. NeoForge registers the
  same shader but was not looked at in a client.
* **Excavation beam, first person** — on a real Fabric frame the beam runs almost along the view,
  so it reads as a short green streak from the wand tip toward the target, with a green glint at
  the impact and crack stages on the block. The full ribbon length, third person and another
  player's view are still unseen.

### 7.3 Now seen on a real frame (2026-10-26, round 2)

See `docs/analysis/2026-10-26-visual-checks-round2-ru.md` for every frame. Seen and found wrong,
then fixed against the original bytecode: the thaumometer amounts (all drawn at one point), the
held bone bow (2.5 blocks, detached from the arm), every late world effect on NeoForge (drawn
without the camera rotation), the warded block runes (`GL_ADD`), the primal arrow trails (wrong
colours, depth writes), the eldritch orb rays (depth writes), node items (drawn through walls).
Added: the sceptre and primal staff runes. Seen and in order: the thaumometer screen and scan,
the block sparkles, the projective mirror (no shader error), the infusion essentia streams.
Still different, not fixed: the deconstruction table ghost item is drawn solid (original additive,
alpha 0.75). (The eldritch portal depth writes and the cut-out jar and alembic labels listed here
before are fixed, see 7.4; the golem markers, see 7.5.)

### 7.4 Now seen on a real frame (2026-10-26, round 3)

See `docs/analysis/2026-10-26-bloom-blend-multiplayer-ru.md`. Fixed against the original bytecode
and seen on a Fabric client: the ethereal bloom (it had no ticker at all, so it neither grew nor
cleansed taint), the thaumic slime outer shell (missing, an invented 0.7 inner body drawn instead),
the additive bloom crystal, the see-through ice and quartz wand rods, the additive fading primal
arrow shaft, blended aspect icons on jars and alembics, the research table inkwell and parchment,
and the eldritch portal without depth writes. The bloom now restores each quart of a column to the
biome its generator gives it; 1.7.10 had one biome per column, 1.21.1 has one per quart height.

### 7.5 Golem bell marks (2026-10-26)

The golem bell now draws the original marks instead of line boxes: the home mark, the face marks
(rainbow or Order colour), the dashed outline of a marked air block and the wobbling script ribbon
from the golem's head to every marker (`docs/analysis/2026-10-26-unwired-tickers-golem-marks-ru.md`).
Like the architect overlay they are drawn at full brightness; the original tessellator's lightmap
brightness 200 is not reproduced. Seen on a Fabric client only.

---

## 8. Improvements adopted from TC4Tweaks (settings, on by default)

The user asked for the quality-of-life improvements of TC4Tweaks, the 1.7.10 companion mod of
TC4 (<https://github.com/Glease/TC4Tweaks>, AGPL-3.0). Each one is a setting in the `Tweaks`
tab of the configuration screen (`config/thaumcraft.properties`), on by default like in
TC4Tweaks; switched off, the original rule applies. No TC4Tweaks code was copied: it patched
1.7.10 bytecode, and each idea is re-implemented on the port's code
(`thaumcraft.common.config.ThaumcraftTweaks`). `TweakRuntimeChecks` drives every one of them
twice, with the setting and with the original rule; the three client settings were checked in a
real client (`docs/analysis/2026-10-26-tc4tweaks-wave2-ru.md`).

| Setting (key) | Original TC4 4.2.3.5 (oracle bytecode) | With the setting |
|---|---|---|
| `more_randomized_loot` | `Utils.generateLoot` enchants the pooled `Items.book` in place, once, so every later book repeats that enchantment until the server restarts; `ThaumcraftLoot` charges the loot vis amulet once per run | Every book from a loot bag, urn or crate is enchanted on its own draw and the pool keeps its plain book; every loot vis amulet gets its own charge with the original spread (0–4 vis per primal) |
| `earth_shock_targets` (2) | `EntityShockOrb` hurts `getEntitiesWithinAABB(Entity.class, …)`, and the shock field (`BlockAiry.onEntityCollidedWithBlock`) hurts any entity, so dropped items and experience orbs are destroyed | 2: only living entities; 1: everything except items and experience orbs; 0: the original. The field still slows every entity |
| `infusion_keeps_item_data` | `TileInfusionMatrix` copies `InfusionRecipe.getRecipeOutput(input)`, so an enchanted or named centre item loses its enchantments and name | When the centre and the result are both armour or tools, every component the centre item changes is kept on the result unless the recipe's result sets that component itself; the components that define the kind of item are never copied and carried wear never breaks the result. Applies to data recipes and plain add-on recipes, not to recipe subclasses that compute their own result |
| `filled_container_to_hand` | `ItemEssence` gives the filled phial with `InventoryPlayer.addItemStackToInventory`, the first free slot when no stack has room | When the last empty phial (or, at the alembic, the last empty jar) in hand is filled and no stack of the filled item has room, the filled one stays in that hand |
| `research_search` (client) | The Thaumonomicon has no search | A search box on the top border finds completed research by name or key, and by the items their readable recipe pages make (not the sample items of enchantment and runic infusions); a result opens the research. Research the player has not completed is never listed |
| `research_counter` (client) | No counter | The top border shows the completed research of the open category out of all of it, or, while some is still hidden, out of what the player can see, marked with a red question mark |
| `notification_clear_button` (client) | Notifications in the corner can only expire | Open chat shows a Clear button above the notification stack while it has rows |
| (always, client) | The research table pages its aspect palette with two arrows | The mouse wheel over the palette pages it as well; a pure input addition with no setting |
| `research_remember_page` (client, the port's own) | The inventory key on a research page goes back to the map (`GuiResearchRecipe.keyTyped`); the book always reopens on the map | The inventory key on a research page closes the book and the next opening shows that spread again; Escape, Back and Backspace still go back to the map and forget it |
| `research_keyboard` (client, the port's own) | Only the inventory key and Escape | On a page: the arrow and page keys turn the spreads, Home/End, Backspace to the map. On the map: the arrows move it one cell, Tab and Shift+Tab change the category |
| `research_bookmarks` (client, the port's own) | No bookmarks | A ribbon on a research page bookmarks it (up to nine); the completed bookmarked research is listed right of the map: click opens it, right-click removes it. Kept in `config/thaumcraft-bookmarks.txt` on this computer |
| `aspect_inference` (server, synced to the client, the port's own) | An item has aspects only from a `ConfigAspects` registration (the ore dictionary rows reach every member of a name), an API registration or crafting, crucible, arcane and infusion recipes (`ThaumcraftCraftingManager.generateTags`); an item made by none of them has none | An item without a registration takes the original row of the last aspect family whose tag holds it, before its recipes, as the original ore dictionary registrations did (the ore dictionary names and wildcard families as `thaumcraft:aspect_family/*` tags with the 1.21.1 members and optional `c:` convention tags; a complex family row adds to the recipe base). An item that the TC4 rules still leave with nothing takes, in order: the port's own table (`data/thaumcraft/port/aspect_inference.json`: a form of another item, or a raw material's list), the plain block of a waxed or weathered copper block, stonecutting, smithing and cooking recipes, three quarters of what a raw material cooks into, one point of the largest aspect of its richest recipe, and for a spawn egg the aspects of its creature. Off: exactly the TC4 rules |
| (always, the port's own) | TC4 has no accessory slots: it implements the API of the separate Baubles mod, whose own menu (key B) holds the amulet, the two rings and the belt; using a bauble from the hand does nothing | The four slots are the port's (`PlayerAccessories`). The menu opens with a key (B by default, under Inventory in the controls) or the B button beside the player in the survival and the creative inventory; an empty slot shows a faded mundane amulet, ring or belt and names its kind. Using a bauble from the hand puts one item into the first free slot of its kind (a creative player keeps the stack); it never replaces a worn one, a spectator puts nothing on, and a focus pouch (any `IFocusPouch`) still opens; an item opts out of the hand equip with `IBauble.equipsOnUse` (section 9) |

Not adopted, because the port already covers them or they do not apply to 1.21.1: the packet
exploit fixes for research completion and aspect combination (the port validates both on the
server), golems picking up dead item entities (checked), the decant golem's unbounded fluid search
(bounded), the mana bean replant fix (matched by full item components), saved player data file
names, and the 1.7.10 crash workarounds for renderers and world generation.

## 9. Add-on API (2026-10-26)

The public API was completed so that 1.7.10 add-ons port quickly ([`docs/api/PORTING.md`](../api/PORTING.md)).
Where the 1.7.10 behaviour could not or should not be kept, this is what an add-on gets instead:

| Area | Original TC4 4.2.3.5 | Thaumcraft 4 Reload |
|---|---|---|
| When to register | `dependencies = "after:Thaumcraft"`, then `init`/`postInit` | `IThaumcraftAddon` (Fabric entrypoint `thaumcraft`, NeoForge `@IThaumcraftAddon.Addon`) or `ThaumcraftApi.whenReady`, run once after every mod registered its items |
| Ore dictionary names | `OreDictionary` entries, populated before `postInit` | `OreDictionaryNames`: the names TC4 used are its `thaumcraft:original_ore/*` tags, the Forge convention is the `c:` tag. A tag registration of object aspects is kept and applied again when tags are bound (world start, client join), filling only items without a registration of their own |
| Research of add-ons | One registry read by everything | Add-on entries and changes to original entries through their public view are laid over the bundled corpus (`AddonResearch`); the icon, conditions and item triggers of an original entry are never taken from its view |
| IMAGE research pages | `GuiResearchRecipe.drawPage` has no IMAGE branch: the page is blank | The image is drawn over the page width with its caption |
| ASPECTS research pages | Hovering an aspect shows a grid of scanned items holding it | The hover shows the name and the description of the aspect |
| COMPOUND_CRAFTING pages | The wand construct diagrams: `ResearchPage(List)` with the list `[AspectList, dx, dy, dz, cells]` | Drawn for add-ons like the book's own construct pages (2026-11-01); the 1.21.1 constructor is typed `ResearchPage(List<Recipe<?>>)`, so the original list goes through a raw `List` cast; the original "Empty Space" block has no item, an empty stack or air stands for it; a list of another shape is not shown |
| Item triggers of add-on research | `areItemStacksEqual(trigger, scanned, true, true, false)`, ore dictionary shortcut included | Same item and same data, durable damage ignored; no ore dictionary shortcut |
| Smelting pages | Output from the global furnace registry at construction | Output from the client world's recipe manager when drawn |
| `ConfigItems`/`ConfigBlocks` names | Static fields | `ItemApi`/`ItemsTC`/`BlocksTC` resolve the original names and metadata through `data/thaumcraft/tc4/legacy_ids.json` ([LEGACY_IDS.md](../api/LEGACY_IDS.md)); `getItem`/`getBlock` return null instead of throwing for a malformed name |
| Wand parts | A part registered under a native tag replaced it; API capacities in whole vis | Native parts are never replaced and setters on their API views do not change gameplay; capacities are whole vis in the API, add-on capacity 0..143165, assembly cost at most 32767 |
| `WandHelper` (was `ItemWandCasting`) | Negative amounts drained or refilled; client-side payment debited | Negative amounts do nothing; client-side payment answers without changing the stack; `getFocusItem` is an empty stack instead of null |
| Add-on foci | Any `ItemFocusBasic` | Any `ItemFocusBasic` (the port's closed list of ten native foci was removed) |
| Add-on aspects | Single-threaded registry; caches of `getAllAspects` kept forever | Registration synchronised; the caches rebuild when the registry changes; `getName()` lowercases with `Locale.ROOT`; late wisp scan rows and creative-tab phials are added when the aspect appears |
| Add-on primal aspects | Wands, sceptres, amulets and vis relays used the live primal list | They stay limited to the six original primals (a gap, not a choice: see `docs/analysis/2026-10-26-addon-aspects-ru.md`) |
| `ThaumcraftApiHelper` knowledge services, `ThaumcraftFX`, `ResearchPage.recipe(ids)` | Not in the API (add-ons used `thaumcraft.common`) | Added; not 1.7.10 members |
| `IBauble.equipsOnUse`, `BaublesApi.getWorn`, `IFocusPouch` | The Baubles mod had no hand equip and `BaublesApi.getBaubles(player)` answered the live inventory; the focus selector found a pouch as a subclass of the internal `ItemFocusPouch` and read every cell of its `getInventory` | Added; not 1.7.10 members. `equipsOnUse` (default `true`) lets one item keep its right click from the port's own hand equip; `getWorn` is a read-only list of copies; the selector reads any `IFocusPouch` and offers foci from, and returns a focus into, the first 128 cells of a pouch (the original had no cap; no known pouch has more than 117 cells) |
| `TileThaumcraft` update data | `getDescriptionPacket` carried the custom NBT, with the chunk and after `markBlockForUpdate` | The same data with the chunk (`getUpdateTag`) and in the update packet, plus the key `thaumcraft:update`: a 1.21.1 client drops an update whose tag is empty, which would leave an emptied add-on container full on screen (2026-10-26) |
| Add-on essentia sources that pay more than asked | A tube credited one unit whatever the source's `takeEssentia` returned; a surplus was lost | Native receivers still ask for one unit; a surplus goes back to the source through its `addEssentia` (2026-10-26) |
| `IWandable.onUsingWandTick` / `onWandStoppedUsing` | Called for an object an add-on marked with the internal `ItemWandCasting.setObjectInUse` | Not called: the port has no counterpart of `setObjectInUse` (a gap) |
| Phials as `IEssentiaContainerItem` | `ItemEssence` implemented it (contents in NBT) | Empty and filled phials are two items with a native component and do not implement it (a gap); jars, crystals, wisp essence and mana beans do |
| Scan markers (`hasScannedItem`, `getScans`, `addScans`), `discoverAspect`, codecs of `Aspect`/`AspectList`, `isMirrorsAllowed`/`isWardedStoneAllowed` (2026-10-28) | Reached through `thaumcraft.common` (`Thaumcraft.proxy.getScannedObjects`, `ResearchManager.completeScanned*`, `PlayerKnowledge.addDiscoveredAspect`, `Config.allowMirrors`/`wardedStone`); a scanned object was a numeric hash of item and metadata | Added to the API; not 1.7.10 members. Scan markers are the port's native keys (`"@" + registry id`, `"#"` for an advanced scan), meant to travel unchanged between players of this port; research clue markers (`"@KEY"` among the completed research) are not part of `Scans`. `AspectList` still compares by identity |
| `ThaumcraftApiHelper.getBonusObjectTags(stack, bonus, level)` (2026-10-28) | `getBonusObjectTags(stack, base)` returned `base` enriched with the contents of containers and the equipment and enchantment bonuses | Returns the complete aspects of the stack (base, contents, bonuses, culled to six) with `bonus` added on top; passing the base list counts it twice. The level-free form still only copies its argument |
| `ThaumcraftApiHelper.isWoodLog` / `findFurthestBlock` / `breakFurthestBlock` (2026-10-28) | `Utils.isWoodLog` accepted a non-air block that `canSustainLeaves` (a log does, a leaf block does not, the plain oak log included) or that was on the elemental axe's list of ore dictionary logs (reading of the oracle in `docs/analysis/2026-10-19-golem-lumber.md`, U:202-211, and of the audited 1.12 `Utils.java:282-300`); `BlockUtils.breakFurthestBlock` ran on either side, took any start block (the Axe of the Stream called `isWoodLog` on it first), harvested the end of its greedy walk with no check of the world border, the spawn protection, the game mode or any permission, read (and so generated) unloaded chunks, kept the walk in static fields and answered true for air | `isWoodLog` is the block tag `#minecraft:logs`: the 1.21.1 meaning of both conditions (every log, wood, stripped log and stem, the plain oak log included, no leaf block). **The oracle jar was not available when this was settled**; the rule rests on the two readings above, and a different reading of the original is fixed in `Utils.isWoodLogState` alone (pinned by `TreeFellingApiTest` and the GameTest `knowledgeApiWoodLogRule`). The port's own Axe of the Stream deliberately fells the narrower `TreeLogTraversal` set. The walk is the original one (shared with that axe in `StreamAxeGeometry`). `breakFurthestBlock` deviates, as a public helper that a client's packet can reach (authority defects): false for a client level, an unloaded start, an air start and a start that is not a log of `block`; the world border, the spawn protection (`mayInteract`) and the game mode are applied to the cell that is broken, which can lie 24 blocks sideways or 48 up or down from the clicked one; no chunk is loaded (an unloaded cell is no link of the walk and gets no delayed tick); `IllegalStateException` off the server thread; no static state (reentrant); a null player is allowed (the block's `playerWillDestroy`, which dereferences the player for blocks that piglins guard, is skipped). `findFurthestBlock` is new: the end cell without breaking, so a claims mod can be asked about it. As in the original: no tool wear, no exhaustion, no loader break event and no claims or land-protection mod is asked (the caller does, through `findFurthestBlock`) |
| Stable keys, `ThaumcraftClientFX`, `AspectRenderer` and the server twins `ThaumcraftFX.sparkle`/`burst`/`blockRunes`/`smokeSpiral` (2026-10-28) | Reached through `thaumcraft.common` and `thaumcraft.client`: the potion ids of `Config`, `instanceof` on entity classes, sound names in strings, `Thaumcraft.proxy.sparkle`/`burst`/`wispFX*`/`blockRunes`/`beamCont`/`smokeSpiral`/`essentiaTrailFx`/`particleCount` (a `CommonProxy` no-op on a server) and `UtilsFX.drawTag` | Added to the API; not 1.7.10 members. The proxy methods keep their behaviour with a `Level` first and checked arguments (`IllegalArgumentException` for a non-finite number or an out-of-range type, even on a server); a server level or `null` is a no-op. Of the proxy only the effects above are offered (no homing wisp, `beamPulseFX`, `bolt`, `arcLightning`, `crucibleBubble`); the primal wisp types are 0 to 5; `beamCont` returns no object (the player form starts at `wandTip(player)`, the point the original and the excavation focus use, the same for every viewer); `sparkle(x, y, z, color)` uses size 1.5 from the audited legacy source (the 1.7.10 jar was not available). `blockRunes` keeps the original half-block offset on every axis. A colour component is clamped to 0 to 1 (the original packed it into a byte unchecked, so a component above 1 spilled into the next channel; the wisp keeps the original's red == 0 drawn as 1, so a red of 0 or less draws as 1); `smokeSpiral` draws one puff per call, as the original (the ring is 5 to 33 calls with random `start` angles). The glint of a beam (`impact`) is counted down by the end-of-tick step of Thaumcraft, so a caller that runs before that step (an item tick, the start of the client tick) sees the original's look and one that runs after it sees the full value. `AspectRenderer.draw` takes an `int` amount where the original `drawTag` took a `float` and showed up to two decimals (the audited legacy source), and starts the number at `y + 11` where that source has `y + 11.5`. `armorMatSpecial()` and `armorMatVoidFortress()` stay plain, unregistered objects holding the original defences and enchantability (the durability factor 25 and 18 is a property of the item in 1.21.1, not of the object; no key, no holder; `thaumcraft:special` is registered but is the goggles' adapter, a different object). The drawn particles and the tag were **not seen on a frame** |

Details and evidence: `docs/analysis/2026-10-26-addon-api-ru.md`, `-addon-wand-parts-ru.md`,
`-addon-aspects-ru.md`, `-addon-legacy-ids-ru.md`.

## 10. Review fixes of 2026-11-01: runic HUD synchronization and potion scan keys

The owner requested fixes for the repository review. Original `EventHandlerRunic:144-146`
only sent a shield update when charge changed, so changing the maximum without changing charge
left the client HUD stale. The port now synchronizes both charge and maximum, including during
recharge delay and when no vis is available. Charge amounts, costs and timers retain their rules.
This is an intentional correction of an original presentation defect.

Potion scan identities now retain the registered potion subtype which 1.7.10 represented as
metadata. Ordinary item markers are unchanged; named potions append `[potion=namespace:id]`.
The old generic potion marker carries no subtype information and remains the water/unspecified
marker. Other named potions can be scanned separately again. Custom names, colours and custom
effects do not create separate identities, matching the original treatment of NBT.

Implementation, regressions and verification limits: [review fixes](../analysis/2026-11-01-review-fixes.md).

## 11. Second review fixes of 2026-11-01: resource custody

The owner requested correction of all 23 findings from the second review. The original hand
mirror transported before debiting its menu; the original traveling trunk spawned its contents
before clearing them and retired after an unchecked spawner drop; the original infernal furnace
did not retain rejected output entities. Native entity admission callbacks can cancel or reenter
these operations. The port transfers paid output into saved custody before invoking those callbacks,
retries rejected item/experience delivery, and preserves accepted-delivery receipts. This deliberately
corrects inherited resource-authority defects; recipe quantities, XP calculation, mirror trajectory,
trunk behaviour and furnace timing retain their existing rules.

The Earthmover pays the number of placement items represented by a modern block state (including
two slabs for a double slab). The Primal Crusher uses the ordinary native player harvest path for
adjacent blocks, including protection callbacks, tool enchantments, drops and wear. This adapts
the original item/metadata and tool-harvest semantics to Minecraft 1.21.1.

Implementation, regressions and verification limits: [second review fixes](../analysis/2026-11-01-review2-fixes.md).

## 12. Node jar completion (2026-11-01)

The owner explicitly requested a working capture, release and visible shrink animation. The
original `TileJarRenderer:107–115` applied its animation scale after drawing, so it changed no
vertices. The port deliberately makes the one-second transition visible: glass shell 3→1, node
1→0.7. The settled node remains visible without goggles, as in the original.

Original quantities, research, activation on glass, vis modifiers and the 75% quality degradation
are preserved. Modern wooden slab tags represent the original wooden slab; double slabs are not
a single lid item. Capture/release apply world bounds, loaded-chunk checks, player permissions
and common/native protection vetoes. They do not generate unloaded chunks. Fabric's placement
veto is the common event; the ordinary clicked-block use veto is not replayed as a synthetic click.

The original resource handoff preceded neither a checked native replacement nor safe failure
recovery. The port pays before native LOAD/UNLOAD, retires the source, supplies a filled native
replacement once, and reconciles native admission before rollback. Item data includes the required
1.21.1 block-entity id. An immediately harvested admitted target remains consumed. A rejected
transfer restores the complete source and payment; a conflicting foreign block is preserved and
unrestorable owned resources enter saved item custody. This corrects inherited loss/duplication
and permission defects. It does not provide atomic multi-chunk disk checkpoints during power loss.

Implementation and verification limits: [node jar completion](../analysis/2026-11-01-node-jar.md).

## 13. Thaumometer and complete bundled book audit (2026-11-01)

The owner requested a live texture check and then every recipe and icon in the Thaumonomicon.
The source comparison covers all201 research entries,591 source descriptors and308 original named
recipe keys; default live coverage is192 nonvirtual enabled research entries and576 source
descriptors (303 text,273 recipe). Seven optional Silver/Lead/Tin entries and their15 source pages
remain disabled without native items. Two virtual icons and the disabled CAP_silver are intentionally
empty; they are classified rather than replaced with invented content.

The eight corrected recipe definitions restore original ingredients, quantities and aspects.
Original wildcard armour wear and BottleTaint's NBT-agnostic catalyst matcher are preserved.
Potion contents, original banner colours and item prototype components were checked independently.

Original item sprites, layers and major inventory model parts now render through native models and
shared static BER parts. Modern display transforms explicitly replace the old Forge inventory helper;
no fake block entities/worlds are used. The existing three modern layer-plan construct displays remain;
six formerly omitted original construct diagrams are restored. This is presentation adaptation, not
a new way to craft completed multiblocks.

Native styled-component spans fix the 1.21.1 split/reorder reset disagreement without rewriting
translations. The scanner's lit additive shader is reused through its existing reload registration.
The original main-hand node-sight rule remains; this audit does not extend offhand node sight.

Actual Fabric EN/RU client passes render every default book descriptor and recipe variant, all
research/aspect icons, item prototypes and new world meshes, before and after resource reload.
The supplied knowledge/items and copied1.21.1 save are declared fixtures. NeoForge client, multiplayer
and survival acquisition are not replayed. Add-on-only ASPECTS/IMAGE/compound page limitations in
section9 are unchanged; this checkpoint covers the bundled book. No fresh direct comparison with
the absent original JAR is claimed; the canonical Java oracle, retained assets and locked hashes
provide the source evidence.

Exact coverage, fixes and selected native frames: [full audit](../analysis/2026-11-01-thaumometer-book-review.md).
Machine-readable complete recipe/icon records: [audit manifest](../porting/thaumonomicon-visual-audit.json).

## 14. Native recipe transfer — owner request 2026-11-02

The owner explicitly requested recipe autofill in base Thaumcraft rather than a storage addon.
This adds a Thaumonomicon `+` and a shared server transfer path for the existing optional EMI
integration. Shift requests the largest complete arrangement that fits; only existing matrix and
main inventory/hotbar ingredients move. Recipes, research requirements, output quantities and
wand vis payment remain governed by the native crafting path. Autofill itself spends no vis.

A standalone book may open the nearest loaded native arcane workbench within the actor's actual
block interaction reach. Current menu/actor/block validity and the full expected inventory state
are checked on the server. Unsupported machine/multiblock pages do not gain a workbench button.
The public transfer-target API is independent of storage mods; no addon is bundled or changed.

Focused Fabric native tests and a real EN/RU client with EMI cover the supplied finite fixture;
this is not a fresh full survival/multiplayer or NeoForge acceptance. Exact receipts and limitations:
[recipe transfer report](../analysis/2026-11-02-recipe-transfer.md).

## 15. Crucible HUD and node siphon presentation — 2026-11-02

The missing original IAspectContainer surface on native TileCrucible is restored with its
original inert mutation/query methods; native physical alchemy and the existing copied aspect
snapshot remain unchanged. Worn IGoggles controls the popup; undiscovered aspects stay unknown.

A narrow ItemInHandRenderer hook ignores only changing vis for otherwise identical native
wands. It preserves ordinary equip transitions for material/focus/name/other component changes.
Actual use sessions and node payment are unchanged. Original node drainer/color presentation
uses a visual-only S2C with short-lived client state and the retained wispy FloatyLine renderer;
it is independent of goggles and cannot refund/retry gameplay payment on delivery failure.

Focused native/client evidence and exact artifact inputs are recorded in the
[wand/crucible report](../analysis/2026-11-02-wand-crucible.md). This is Fabric presentation
acceptance with a finite supplied fresh-world fixture, not new survival/multiplayer/NeoForge coverage.

## 16. Deepslate infused ores — owner request 2026-11-02

Six new host variants extend the original crystal-bearing stone into Minecraft1.21.1's deep
underground. Original TC4 did not contain deepslate. Native deepslate side/end sprites are
combined with the retained animated crystal overlay and six tints; source PNGs stay unchanged.
The opaque host skips the old inset stone pass and separates overlay depth slightly.

The original stone pass remains, with host-aware transition targets. An additional eight
size-six veins sample the negative world-height interval, use vanilla deepslate ore targets,
and share the existing infused-stone toggle and biome/dimension/loaded/write constraints.
This deliberately adds modern-depth supply rather than claiming original seed/quantity parity.
Old chunks are not automatically converted; explicit existing regeneration is separate.
Shard/Fortune/XP behavior is retained, Silk Touch keeps the new host block, and scanning,
ore-tag integrations and research-table bonuses recognise both variants. Physical deepslate
hardness/resistance/sound apply. Cinnabar/amber are unchanged.

Exact Fabric native/client/production inputs, selected frames and limits:
[deepslate report](../analysis/2026-11-02-deepslate-ore.md). Othermodernadaptation suggestions
are recorded separately and were not implemented in this checkpoint.

## 17. Modern Minecraft adaptations — owner request 2026-11-02

The owner approved seven additions for Minecraft 1.21.1 after the alpha.16 review. These are
modern extensions, not original TC4 values: nine entity aspect tables, five Harvest crop groups,
modern exploration loot in 14 vanilla tables, Greatwood/Silverwood building families (20 blocks,
16 items and 16 crafting recipes), five stonecutting recipes, custom-model armour trim rendering,
and 12 enchantment infusion recipes with two research branches.

The canonical 201 research entries and original 24 enchantment infusion recipes are unchanged.
`MODERNINFUSION` and `DEEPINFUSION` are registered beside the original corpus, giving 203 live
research entries. Modern enchantment ingredients, aspect costs and instability are new balance
choices. Native enchantment applicability, maximum levels, incompatibilities and actual infusion
payment remain in the existing engine. The advanced branch also requires the hidden original
`ELDRITCHMINOR` parent.

Modern entity rows seed the existing public registry once; later addon overrides, explicit null
tags and removal remain authoritative. Scanning uses the existing knowledge prerequisites,
20-tick session, awards and repeat prevention. The crop whitelist adds beetroot, torchflower,
pitcher crop, sweet berries and glow berries. Berry picking preserves the plant. Mature native
torchflowers and pitcher plants do not drop seeds/pods: Order spends real supplied seed/pod
items found within the original six-block AABB search, and leaves harvested cells empty when
none are available. Structure loot leaves existing vanilla pools and the original ten chest
categories in place. Archaeology adds one weighted fragment entry to its existing first pool,
while trial vaults receive only a top-level bonus pool.

The preliminary review incorrectly said ordinary magical armour lacked native trim admission.
It already supported native smithing and ordinary trim rendering. The new visible trim layer
addresses the custom fortress and void-robe models, preserving their native materials and the
existing dyed cloth, mask and goggles layers. The passing native smithing check covers 17 armour
pieces and verifies payment, component preservation, persistence and refused invalid/duplicate trims.

Native sign board/support geometry now uses bounded per-face UVs for the magical plank block
sprite, rather than sampling a 64x32 entity unwrap through a block sprite. Native hanging-sign
chain geometry keeps its separate alpha sheet and attachment visibility. Door items render both
halves. Two rendered-vertex UV checks pass in the final common suite; visual acceptance remains
separate from these assertions.

Final Fabric build and production/test isolation pass; common JUnit reports 2874 tests with
zero failures/errors and five existing skips. The final focused native receipt passes all 11
checks. The ordinary production server runs the final JAR for 50 measured ticks, saves and stops
normally with exit 0. Earlier failed native fixtures and the first client probe (stage-24 timeout)
are declined receipts, not partial passes. The final EN/RU client/reload script passes and captures
25 frames with Gradle PASS. All 25 final client4 frames have been inspected by the root and three
reviewers: wood/inventory, both native sign editors, armour layers, all 12 new recipe pages,
timed Frog scan and the crop fixture pass. Client2/3 also completed SCRIPT_PASSED; their later
replacements only improve presentation visibility, removing covering tooltips and moving the
held instrument away from the crop fixture. The final accepted run is
`modern-adaptations-2026-11-02T10-34-32.410750373Z`; its production JAR SHA remains unchanged.
No fresh survival acquisition, multiplayer or NeoForge acceptance is claimed.
Exact inputs, all frame hashes and limits: [receipt](../porting/modern-adaptations.json),
[report and gallery](../analysis/2026-11-02-modern-adaptations.md).
