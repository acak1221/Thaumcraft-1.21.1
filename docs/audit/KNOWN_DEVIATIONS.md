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

Not adopted, because the port already covers them or they do not apply to 1.21.1: the packet
exploit fixes for research completion and aspect combination (the port validates both on the
server), golems picking up dead item entities (checked), the decant golem's unbounded fluid search
(bounded), the mana bean replant fix (matched by full item components), saved player data file
names, and the 1.7.10 crash workarounds for renderers and world generation.
