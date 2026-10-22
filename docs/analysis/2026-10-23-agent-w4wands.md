# Agent wave 4 `w4wands` — the wand / node / vis family (2026-10-23)

Branch: `agent/w4wands` (worktree `/home/cyberfox-kali/wt/w4wands`).
Domain: `wands_nodes_and_vis` — the 9 units of `/home/cyberfox-kali/units/w4wands.txt`.

Sources used:

* the **oracle** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte-exact, the authority);
* the **official Mojang 1.7.10 `client.jar`** (downloaded for this wave) for the two vanilla classes
  the oracle does not contain — `net.minecraft.client.renderer.entity.RenderPlayer` (notch `bop`)
  and `net.minecraft.client.renderer.entity.RenderLivingBase` (notch `boh`), decompiled with CFR
  0.152 — and the **MCP stable 12-1.7.10 mapping** (`methods.csv`, `fields.csv`) to resolve
  `field_71011_bu` = `getItemInUse`, `field_71057_bx` = `getItemInUseDuration`,
  `field_76379_h` = `fall` and `field_150383_bp` = `cauldron`;
* the 1.21.1 port `common/src/main/java/thaumcraft/**` and the two loader modules;
* the original assets of `Thaumcraft-1.7.10-4.2.3.5.jar` (the two robe sheets and the particle
  atlas are byte-identical to the port's copies).

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*. The
changed sources were **type-checked** with `javac` against the access-widened 1.21.1 jar of the
leader's Gradle cache: the whole `common/src/main/java` tree (1377 files) and the whole
`common/src/test/java` tree compile with **zero errors**; the `fabric/src/main/java` tree compiles
with **zero errors** as well; the `neoforge/src/main/java` tree has the **same nine pre-existing
errors before and after this wave** (the `net.neoforged.fml.*` loader classes are not in the ad-hoc
classpath of the type check), so none of them is mine.

---

## 0. The summary

| unit | before | after | why |
|---|---|---|---|
| `thaumcraft/api/nodes/INode.java` | partial | **parity_verified** | the contract is complete, the identity difference is documented in the contract itself |
| `thaumcraft/common/tiles/TileNode.java` | partial | **parity_verified** | the identity difference is documented; every other rule of the original is the native one |
| `thaumcraft/common/world/aura/AuraChunk.java` | partial | **parity_verified** | deliberate `exclude`: TC4 has no per-chunk aura at all |
| `thaumcraft/common/world/aura/AuraHandler.java` | partial | **parity_verified** | deliberate `exclude`: the drain of the retained handler is the network drain of the native |
| `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` | partial | **parity_verified** | the worn look is the original's two sheets now |
| `thaumcraft/client/renderers/tile/ItemNodeRenderer.java` | partial | **parity_verified** | the item of the node is drawn as the node |
| `thaumcraft/client/fx/beams/FXBeamWand.java` | partial | **parity_verified** | the fan of the original with the numbers of the original |
| `thaumcraft/client/lib/REHWandHandler.java` | partial | **parity_verified** | the world overlay of the Architect focus exists now |
| `thaumcraft/client/renderers/item/ItemWandRenderer.java` | partial | **partial** | the model basis correction and the use pose need a client frame |

**Eight of the nine units moved from `partial` to `parity_verified`.** The ninth stays `partial`
with the exact remaining difference and the exact change that would close it (section 8).

---

## 1. The node identity — `api/nodes/INode.java` and `common/tiles/TileNode.java`

* **Oracle** `thaumcraft/api/nodes/INode.java:12-29` publishes eight members of its own and inherits
  the nine of `thaumcraft/api/aspects/IAspectContainer.java:9-29`; `thaumcraft/common/tiles/TileNode.java:75-77`
  — the original node itself implements the contract; `:240-271` — the type, the modifier and the
  base are written through it; `:98-115` — the identity is derived from the position of the node
  (`dim:x:y:z`).
* **Port before** the contract was complete already (the wave-3 migration), but the identity
  difference was only recorded in the ledger.
* **Port after** `common/src/main/java/thaumcraft/api/nodes/INode.java` carries the whole contract
  with the original's semantics and **documents the identity difference in the contract itself**:
  the original derived the identity from the position, the native keeps a persisted random UUID,
  and the format is bound into the accepted client evidence
  (`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509`
  parses it with `UUID.fromString`, the accepted saved worlds carry the UUIDs in their node NBT and
  the accepted scan records carry `@NODE1b1965a9-747f-4d43-82ff-91e1f0ab443b`,
  `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/BoreClientSequence.java:870-871`).
  `common/src/main/java/thaumcraft/common/tiles/TileNode.java:128-146` says the same at the node.
* **The one other difference of the contract**, also documented: the native returns a **copy** of the
  aspect lists, so an add-on can never mutate the state of a node through the published view. The
  values are the original's.
* **Test** `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` gained
  `thePublishedContractCarriesEveryMemberOfTheOriginal` (the names and the signatures of all
  seventeen members) and `theIdentityOfTheNodeIsThePersistedUuid`.

## 2. The aura of the retained 1.12 tree — `common/world/aura/AuraChunk.java` and `AuraHandler.java`

* **Oracle** `thaumcraft/common/world/aura/AuraChunk.java` and `…/AuraHandler.java` are **absent
  from the TC4 4.2.3.5 oracle**: TC4 has no per-chunk aura at all, the aura of the original is the
  aura node (`thaumcraft/common/tiles/TileNodeEnergized.java:27,165` `getAuraBase`).
* **Retained 1.12 reference** `src/main/java/thaumcraft/common/world/aura/AuraChunk.java:5-30` is the
  TC6 chunk state holder and `…/AuraHandler.java:15-23` refuses it with
  `UnsupportedOperationException`, so even the retained reference never served it.
* **The one working method** `…/AuraHandler.java:25-33` drains the **VIS NETWORK** at the position
  (`VisNetHandler.drainVis(world, x, y, z, requested, simulate)`).
* **Port** `common/src/main/java/thaumcraft/api/aura/AuraHelper.java:57-60` drains the **VIS
  NETWORK** at the position as well (`VisNetHandler.drainVis(level, pos, amount, simulate)`), and
  `common/src/main/java/thaumcraft/api/visnet/VisNetHandler.java:530-546` walks the primal aspects
  in the original order. **The remaining difference the ledger named was stale** — it said the
  native projection drained the node *at* the position; it does not.
* **Complete callsite audit**: the only consumer of the two classes is the retained 1.12 facade
  `src/main/java/thaumcraft/api/aura/AuraHelper.java:5-6,17,21,25,30,34`; no native source refers
  to them.
* **Verdict** both rows stay `exclude` (the TC6 artefacts are deliberately not ported) and move to
  `parity_verified`: no value of the original is lost, because the essence of the aura node is the
  aura of the original.
* **Test** the existing `common/src/test/java/thaumcraft/api/AuraHelperDrainTest.java` already
  asserts that the drain reaches the network and not the node at the position, in the original's
  primal order. No new test was needed.

## 3. The worn look of the void thaumaturge — `common/items/armor/ItemVoidRobeArmor.java`

* **Oracle** `thaumcraft/common/items/armor/ItemVoidRobeArmor.java:98-100` — the worn look is **two
  sheets**: the layer that carries the dye is the `void_robe_armor_overlay.png` and the layer that
  carries the metal trim is the plain `void_robe_armor.png`; `:187-189` — the piece always has the
  overlay layer.
* **The official 1.7.10 `client.jar`** settles the order: `RenderPlayer` (notch `bop`) `:52-100`
  binds `getArmorTexture(stack, entity, slot, null)` — the overlay sheet — and tints it with the
  colour of the stack, and `RenderLivingBase` (notch `boh`) `:113-121` draws the second layer with
  `getArmorTexture(stack, entity, slot, "overlay")` — the plain sheet — untinted, **after** the
  first one.
* **The two sheets are disjoint**: of the 32768 pixels of the 256x128 sheet the plain one owns 6224
  and the overlay one 5652, and only 21 pixels belong to both.
* **Port before** `ThaumcraftGearModelsClient.renderRobe` drew the plain trim sheet **alone and
  tinted it with the dye**, so the dyed cloth of the robe was never drawn at all.
* **Fix** `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java:219-239` draws the
  dyed cloth first and the untinted trim after it, exactly as the 1.7.10 renderer does; the pure
  rule lives in the new `common/src/main/java/thaumcraft/common/items/armor/VoidRobeLayers.java`.
  Both loaders draw it (the Fabric `ArmorRenderer` and the NeoForge armour layer mixin).
* **Test** `common/src/test/java/thaumcraft/gear/VoidRobeLayersTest.java` (the two layers, their
  sheets, which one is dyed and the default dye 6961280).

## 4. The item of the aura node — `client/renderers/tile/ItemNodeRenderer.java`

* **Oracle** `thaumcraft/client/renderers/tile/ItemNodeRenderer.java:37-170` — the item of the
  invisible aura node is drawn as the node: the fixture node of the four primal aspects at forty
  each (`:39`), the alpha of one half (`:75`), the atlas of thirty two frames (`:100`), the pulse
  of a strip (`:115`), the size of a strip (`:116`), the alpha of a strip (`:117`), the core strip
  of the type (`:127`) and the two crossed quads (`:60-68`).
* **Port before** the node item was a static `shard` texture (`assets/thaumcraft/models/item/aura_node.json`).
* **Fix** the new `common/src/main/java/thaumcraft/client/renderers/tile/ItemNodeRenderer.java`
  draws it the original's way; `TileNodeRenderer` shares the atlas and the quad with it; both
  loaders register the renderer (`ThaumcraftFabricClient` and `ThaumcraftNeoForgeClient`).
* **The gameplay drop of the original** is the wisp essence of `BlockAiry:265-279`
  (`NodeHarvest`), never a node item, and that was already the case.
* **Test** `common/src/test/java/thaumcraft/client/renderers/tile/ItemNodeRendererTest.java`.

## 5. The beam of the excavation focus — `client/fx/beams/FXBeamWand.java`

* **The difference the ledger named did not exist.** The original beam does **not** start at the
  wand tip: `thaumcraft/client/fx/beams/FXBeamWand.java:209-228` derives its origin from the eye of
  the player, moved by the yaw and the pitch and by the look vector times 0.3 — and the native
  already used exactly that origin (`common/src/main/java/thaumcraft/common/items/wands/foci/ItemFocusExcavation.java:161-163`).
* **The real difference was the drawing routine.** The original is a fan of three planes that turns
  around the axis of the beam (`:229-255`), with the rotation of the fan following the time of the
  world (`:171`), the texture sliding with the age of the player (`:201-206`), a plane three tenths
  of a block wide (`:235-238`), an opacity of four tenths that fades to a tenth in the last four
  ticks (`:177-180`) and an impact that is one camera facing quad of the particle atlas of the
  original with the frame of the age over sixteen and the size `endMod / 2 / (6 - impact)`
  (`:261-263,269-297`). The native drew two camera facing ribbons whose opacity ramped up and
  faded to zero, and five glints of its own.
* **Fix** `common/src/main/java/thaumcraft/client/ThaumcraftExcavationClient.java` now draws the
  fan of the original with the numbers of the original and the impact of the original
  (`thaumcraft:textures/misc/particles.png`, the 256x256 sheet of the original, is in the port).
* **Test** `common/src/test/java/thaumcraft/wands/ExcavationBeamRulesTest.java` (the fan, the
  rotation, the slide and the frame of the original).
* **Forced 1.21.1 change**: the fan is drawn with the depth test of the native late world pass; the
  original drew it in the particle pass.

## 6. The world overlay of the Architect focus — `client/lib/REHWandHandler.java`

* **Oracle** `thaumcraft/client/lib/REHWandHandler.java:297-479` — the area of the Architect focus
  is drawn in the world: every block as a translucent cube whose face carries the warded glass rune
  of the eight neighbours of the face (`:311-313,318-368`), the inner faces of the area are not
  drawn (`:370-372`), the colour of a block is `sin(ticks / 2 + x) * 0.2 + 0.3`,
  `sin(ticks / 3 + y) * 0.2 + 0.3` and `sin(ticks / 4 + z) * 0.2 + 0.8` with the alpha of a fifth
  (`:391-394`) and the arrows of the axes are drawn at the anchor with the alpha of a third
  (`:429-479`).
* **Port before** the overlay did not exist at all. The port already carried the original lookup
  table of the rune textures and the eight neighbours of a face
  (`common/src/main/java/thaumcraft/common/lib/world/WardingOverlay.java:12-47`).
* **Fix** the new `common/src/main/java/thaumcraft/client/ThaumcraftArchitectClient.java` draws
  the area and the arrows; both loaders call it in the late world render.
* **The radial of the original** (`:185-295`) was already reproduced by the native
  `GuiFocusSelect` (the centre line names the focused focus only and is drawn once at
  `centre - font.width(text) / 2`).
* **Forced 1.21.1 change**: the original drew the silhouette of the **jar** block with the rune
  texture of the warded glass (`:399-416`); the native draws the **cube** of the block, because the
  1.7.10 block renderer cannot draw one model with the texture of another.
* **Test** `common/src/test/java/thaumcraft/wands/ArchitectOverlayTest.java`.

## 7. The node jar growth, the equipment curve and the particle API

* **The node jar must not grow.** The wave-3 agent proved that the "one second growth" of the
  original node jar is dead code: `TileJarRenderer:107-115` applies `GL11.glScalef(size, size, size)`
  *after* every draw call and pops the matrix immediately, so the scale never reaches a vertex. The
  native does not grow the node either
  (`common/src/main/java/thaumcraft/client/renderers/tile/TileJarNodeRenderer.java`, whose javadoc
  states the proof). **Nothing had to be changed here.**
* **The equipment curve.** The draw power of the bone bow of the original *eases*
  (`f = j / 10; f = (f * f + f * 2) / 3`, oracle `ItemBowBone`) and the native keeps that quadratic
  in `common/src/main/java/thaumcraft/common/lib/items/EquipmentRules.java:53-57`. The wand capacity
  of the original is `rod.getCapacity() * (isSceptre ? 150 : 100)`
  (oracle `ItemWandCasting:119-121`) and the native keeps it
  (`common/src/main/java/thaumcraft/common/items/wands/WandMaterials.java`). **Nothing had to be
  changed here either.**
* **`getTextureAnimationSize` / `getTextureSize`** of the 1.7.10 particle API have no 1.21.1
  counterpart: the native uses its own particle classes with their own sizes. The two sheets the
  original draws with them (the node atlas and the particle atlas of the original) are in the port
  and are used with the original's UVs.

## 8. The unit that stays open — `client/renderers/item/ItemWandRenderer.java`

* **Oracle** `thaumcraft/client/renderers/item/ItemWandRenderer.java:45-119` — the transforms of the
  context (`:59-86`), the **model basis correction of a hundred and eighty degrees about X**
  (`:87`), the **use pose** (`:88-113`) and the drawing of the model (`:114-118`).
* **Port** `common/src/main/java/thaumcraft/client/renderers/models/gear/ModelWand.java:31-43` is the
  geometry of the original (the cap at zero, the rod at two with a height of eighteen, the bottom
  cap at twenty) and `ThaumcraftGearModelsClient.renderWand` draws the rod, the cap, the cap ring,
  the bottom cap and the focus cube with the colour of the focus — but **without** the basis
  correction and **without** the use pose.
* **Why it stays open.** The two changes belong together: the use pose of the original is expressed
  in the basis that the correction establishes, and the 1.7.10 model space has its Y axis downwards
  while the 1.21.1 model space has it upwards, so the correction decides which end of the wand is
  up. Applying one of the two without the other would move the wand in the wrong direction, and
  this wave can run no client.
* **The exact change** (for the leader or the next wave): apply the correction of a hundred and
  eighty degrees about X to the native wand renderer and then the original's use pose around the
  pivot one block above the model, with the numbers of the oracle `ItemWandRenderer:88-113` — the
  raise of the wand over three ticks, the thirty three degrees of the third person, the ten plus
  ten degrees of the first person, the sixty degrees of the startup and the WAVE and CHARGE waves
  of the focus — and the elapsed time of the use, which the 1.21.1
  `LivingEntity.getTicksUsingItem()` counts up like the 1.7.10 `EntityPlayer.getItemInUseDuration()`
  (MCP stable 12-1.7.10: `field_71057_bx`).

## 9. The `javac` proof

```
$ /tmp/opencode/check_w4wands.sh
MAIN OK
TEST OK
$ /tmp/opencode/check_w4wands_loaders.sh
FABRIC OK
$ /tmp/opencode/check_w4wands_neoforge.sh
9 errors        (the same nine before this wave: the net.neoforged.fml.* loader
                classes are not in the ad-hoc classpath of the type check)
```

The classpath is the access-widened `minecraft-merged-1.21.1-loom…jar` of the leader's Gradle cache,
the remapped `architectury` and `fabric-api` artifacts, the Minecraft libraries of the Gradle module
cache and the JUnit 5 jars; the NeoForge half uses the NeoForge-patched Minecraft jar and the
`neoforge` and `bus` artifacts. **No Gradle daemon was started.**

## 10. The tests that were authored (NOT executed)

* **new** `common/src/test/java/thaumcraft/gear/VoidRobeLayersTest.java` — the two worn layers of
  the void thaumaturge, their sheets, which one carries the dye and the default dye 6961280.
* **extended** `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` — every member of
  the original `INode` contract is present with its original signature, and the identity of the node
  is the persisted UUID.
* **new** `common/src/test/java/thaumcraft/client/renderers/tile/ItemNodeRendererTest.java` — the
  fixture node, the alpha, the scale, the atlas, the frame, the pulse, the size and the core of the
  original item node.
* **new** `common/src/test/java/thaumcraft/wands/ExcavationBeamRulesTest.java` — the fan of the
  original, the rotation of the fan, the slide of the texture and the frame of the beam.
* **new** `common/src/test/java/thaumcraft/wands/ArchitectOverlayTest.java` — the mask of a face of
  the area, the hidden inner faces and the alphas of the original.

**No test constructs a `Block`.** Every rule under test is a pure static function over positions,
aspects or numbers.

## 11. What could not be verified in this environment

* **No build, no test run, no GameTest, no client.** The wave was told not to run Gradle.
* **The type check is not a build.** The resources, the mixins and the runtime behaviour were not
  checked at all.
* **The client frames.** Nothing of this wave is visible without a client run; the acceptance
  criteria are in section 12.

## 12. The acceptance criteria for the leader's client run

* **The void thaumaturge** (hood, robe and leggings): the cloth of the robe must show the dye
  colour (the default 0x6A3880 purple) and the metal trim must stay untinted. Before the change the
  robe showed only the trim, dyed.
* **The item of the aura node** in the inventory and in the creative tab: an animated node of the
  32 frame atlas with the four primal aspects (air, fire, earth and water), not a flat shard.
* **The beam of the excavation focus**: a green fan of three planes that turns around its own axis,
  with a width that grows over the first four ticks, an opacity of four tenths that fades to a tenth
  in the last four ticks and one camera facing sprite of the particle atlas at the block that is
  mined.
* **The Architect focus**: while the player looks at a block with an architect focus in the wand, the
  area of the focus must be outlined by translucent blocks with the warded glass runes and the
  arrows of the axes must mark the anchor.

## 13. The files changed by this wave

```
common/src/main/java/thaumcraft/api/nodes/INode.java                              (the contract and its documentation)
common/src/main/java/thaumcraft/common/tiles/TileNode.java                        (the identity documentation)
common/src/main/java/thaumcraft/common/items/armor/VoidRobeLayers.java             (new, the two worn layers)
common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java            (the worn look of the void robe)
common/src/main/java/thaumcraft/client/renderers/tile/ItemNodeRenderer.java       (new, the item of the node)
common/src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java        (the shared atlas and quad)
common/src/main/java/thaumcraft/client/ThaumcraftExcavationClient.java            (the fan of the original and its impact)
common/src/main/java/thaumcraft/client/ThaumcraftArchitectClient.java            (new, the world overlay of the Architect focus)
fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java                (the item renderer and the world overlay)
neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java  (the item renderer and the world overlay)
common/src/test/java/thaumcraft/gear/VoidRobeLayersTest.java                     (new, authored)
common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java                 (extended, authored)
common/src/test/java/thaumcraft/client/renderers/tile/ItemNodeRendererTest.java   (new, authored)
common/src/test/java/thaumcraft/wands/ExcavationBeamRulesTest.java               (new, authored)
common/src/test/java/thaumcraft/wands/ArchitectOverlayTest.java                  (new, authored)
docs/porting/scope-ledger.json                                                     (the nine rows of this wave)
docs/analysis/2026-10-23-agent-w4wands.md                                          (this report)
```
