# Wave 7 — item / book / tile rendering

Branch: `agent/w7items`. Oracle throughout: byte-exact
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (CFR 0.152).
All `parity_verified` changes below are **source-level closure against that oracle**,
not a claim of inspected frames or executed JUnit. No Gradle, game, server or JUnit
execution was performed. Fabric runtime/mixin application and visual acceptance
belong to the leader. No assets were changed.

## Outcome and acceptance criteria

### GuiResearchRecipe — parity_verified (existing implementation audited)

* Oracle `thaumcraft/common/config/ConfigResearch.java:110-144` specifies three
  sceptres, wood/iron → greatwood/gold → silverwood/thaumium, each with grid
  `" TF", " RT", "T  "`. That is **three** caps, a rod and a primal charm.
* Oracle `ConfigResearch.java:302-305` / `ConfigRecipes.java:417-425` loops over
  **all 48 aspects**, not sixteen. The brief/old ledger's sixteen was incorrect.
  Oracle `thaumcraft/client/gui/GuiResearchRecipe.java:647-655` cycles arrays once
  per second.
* Already implemented in native
  `common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java:471-511,559-567,764-850`:
  special sceptre input assembly plus vis display, and synthetic blank-label /
  eight-unit phial → bound label pages. GUI delegates at
  `common/src/main/java/thaumcraft/client/gui/GuiResearchRecipe.java:127-130`.
  Corrected misleading comments, did not duplicate those implementations.
* **Frame acceptance:** SCEPTRE cycles the three actual material combinations
  once per second with occupied slots 1,2,4,5,6, correct output sceptre and primal
  costs 1/13/81. JARLABEL shows blank label plus filled phial and a matching bound
  label; follow the cycle beyond sixteen entries to confirm it does not truncate.
  Hover the inputs and output; their components must agree with the pictured aspect.

### ItemThaumometerRenderer — parity_verified

* Oracle `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:101-104`
  interpolates arm pitch/yaw then applies `(playerRotation - armRotation) * .1`
  around X/Y. Stable-12 MCP names map field_71164_i / field_71155_g to previous /
  current arm pitch and field_71163_h / field_71154_f to previous / current yaw.
* Contrary to the old blocker, vanilla
  `net/minecraft/client/player/LocalPlayer.java:136-139,622-625` has public
  xBob/xBobO/yBob/yBobO and their same `.5` lag update. Native
  `common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:115-119`
  now uses them through `ThaumometerArmPose.java:7-8`.
* Existing readout at native `ItemThaumometerRenderer.java:87-90,166-190` and
  `ThaumometerReadout.java:102-140` already covers oracle `:155-259`; the original
  derives a name from the scanned stack, not an additional item icon.
* Preserved the leader's SCREEN_SCALE=2.5, YN 248 inventory rotation, and
  `ThaumometerReadout.lightmap` decomposition. Did not undo those landings.
* **Frame acceptance:** while turning quickly in both axes the instrument lags
  with the two .1 rotations and settles when the view stops. A scanned target's
  name/aspects remain physically attached to the screen; unknown targets do not
  reveal known-only aspects. Check an item, living entity and node. Inventory
  scanner orientation and face-filling screen must remain unchanged.

### ItemWandRenderer — parity_verified

* Oracle `thaumcraft/client/renderers/item/ItemWandRenderer.java:51-56,87-113`:
  real wielder, XP 180 model correction, pivot Y=1, third-person Z=33 or
  first-person X=10 then Z=10, -X raise 60 over three elapsed ticks; WAVE divisors
  10/15 amplitude 10, CHARGE divisors .8/.7 amplitude 1.
* Existing `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java:160-164,241-300`
  and `renderers/item/WandUsePose.java:48-96` already express that chain after
  `WandContextPose`. The remaining actual bug was using `Minecraft.player` even
  for a remote player's wand. New `common/src/main/java/thaumcraft/common/mixin/client/WandRenderContextMixin.java:20-31`
  captures the actual LivingEntity from the vanilla ItemRenderer.renderStatic
  overload; `WandRenderContext.java:11-20` preserves nested calls. The use pose
  now takes that Player at `ThaumcraftGearModelsClient.java:245-246`.
* **Frame acceptance:** first-person wand rises across ticks 0..3, keeps the
  focus end up, and WAVE/CHARGE keep oscillating after full raise. In third person
  observe another player using a focus while the local viewer is idle, then swap
  their roles: only the actual using wielder's pose follows their elapsed time.
  GUI, ground and framed wands must not inherit a player's active-use pose.

### TileThaumatoriumRenderer — parity_verified

* Oracle `thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:53-73`:
  (.5,0,.5), XP -90, Z NORTH=270/SOUTH=90/EAST=180/WEST=0. Oracle `:78-99`:
  ordered recipe output selected by player ticks /40, front offset /1.99,
  Y=1.325, scale=.75 and Y facing turns 180/0/90/270.
* New `common/src/main/java/thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:21-62`
  draws the retained ModelThaumatorium OBJ then that preview, registered at
  `common/src/main/java/thaumcraft/client/ThaumcraftAlchemyClient.java:22-23`.
  `BlockThaumatorium.java:39` hides both old model shells; the bottom BER draws
  the entire two-block mesh. Offscreen rendering avoids bottom-only culling.
* `common/src/main/java/thaumcraft/common/tiles/TileThaumatorium.java:88-93`
  resolves the selected job list in saved order without requiring an input item.
  Existing `Recipes` persistence/update-tag delivery already carries that list:
  the old claim that the preview was impossible was incorrect.
* Checked official vanilla 1.7.10 client `bny` (RenderItem), decompiled locally
  under ignored `.tmp/w7items-1710`: `bny.java:75,80-95,137-139,183-195` supplies
  constant fresh-EntityItem bob .1, framed sprite scale .5128205, sprite origin,
  and ordinary 3D block scale 1.25*.25. The new preview avoids applying modern
  item display transforms on top of those, using NONE. Item geometry remains
  the native game's geometry; special legacy block render-type sizing is not
  a new vanilla block-model renderer in this port.
* **Frame acceptance:** form machines in four horizontal facings. Each has one
  detailed original two-block OBJ, correctly upright with front matching the
  output side, and no superimposed cubic shell. Select two formulas (with an
  upgrade); preview outputs switch every 40 player ticks, at the front panel,
  count one, without spinning. Empty the catalyst slot: preview still cycles.
  Clear the formulas: preview disappears. Check a flat output and a block output
  for old frame sizing, and check visibility when only the top half is on screen.

### ItemBowBoneRenderer — partial (held transform fixed, glint remains)

* Oracle `thaumcraft/client/renderers/item/ItemBowBoneRenderer.java:48-64,82-88`
  discards the equip matrix then uses scale 2.6666667, -20/90/-60 turns, inner
  .625 scale with negative Y, and the icon's own 1.5/50/335 transform chain.
* `common/src/main/java/thaumcraft/client/renderers/item/BoneBowPose.java:11-29`
  now preserves that full chain. New
  `common/src/main/java/thaumcraft/common/mixin/client/BoneBowHeldPoseMixin.java:21-34`
  intercepts ItemInHandLayer before the modern equipped matrix, restores the old
  0..1 icon origin and renders with NONE. Native model selection still receives
  the wielder, so the three pull sprites retain native selection.
* **Exact remaining difference:** oracle `ItemBowBoneRenderer.java:89-119`
  uses the 1.7 glint texture, depth EQUAL, blend SRC_COLOR/ONE, RGB
  (.38,.19,.608), texture scale .125, turns -50/10 and 3000/4873-ms scrolls.
  The port still uses native glint. This is implementable with a dedicated
  glint render type/UV mapping; absence of a JSON transform is not an API waiver.
* **Frame acceptance for implemented fix:** third-person idle and full-draw
  right-hand bone bow uses the bespoke large diagonal pose, not a vanilla bow
  attached through the usual hand-item rotations. Check all three pulling
  sprites and an animated humanoid; enchantment still visibly uses native glint
  and is deliberately not accepted as original parity. Left/offhand rendering
  is an added modern context, not an original 1.7.10 parity assertion.

### TileMirrorRenderer — partial (documented, not falsely waived)

* Oracle `thaumcraft/client/renderers/tile/TileMirrorRenderer.java:85-114`
  and its facing variants generate eye-linear S/T/R and object-linear Q planes
  with projection/texture matrices. At `:57` it resets Random(31100L) each draw.
* Vanilla 1.21.1 `assets/minecraft/shaders/core/rendertype_entity_translucent.vsh:8,29-36`
  consumes explicit UV0; there is no fixed-function texgen state to enable.
  This is the precise missing **API**, not proof that the effect is impossible.
* Counterexample: vanilla `rendertype_end_portal.vsh:13-15` computes projected
  coordinates, and `rendertype_end_portal.fsh:39-60` transforms them and uses
  textureProj for view-dependent layers. Those are the nearest vanilla design
  to reproduce the original in a dedicated shader. Its colors/time/scale are
  not themselves TC4's values, so blindly using it is also not parity.
* Current closest **existing** port is native
  `common/src/main/java/thaumcraft/client/renderers/tile/TileMirrorRenderer.java:57-68`
  plus `renderers/models/ModelTunnelField.java:78-122`: shrinking quads at depth
  with explicit UVs. It has the original textures and additive ONE/ONE inner
  layers through RenderType.eyes; the old claim that those use ordinary alpha
  is stale. Its projection is not the oracle's, and its Random is not reset per
  face. No closest-possible reproduction proof exists; it stays partial.
* **Frame acceptance for eventual closure:** linked item and essentia mirrors,
  viewed from centre and then obliquely, must show texture parallax attached to
  the eye projection rather than the same receding flat quads. Inner additive
  layers must remain bright; retain correct unlinked pane, instability jitter
  and six face orientations. No such frame was inspected in this lane.

## Verification and handoff

* Whole `common/src/main/java`: **1,418 sources, javac exit 0** after final
  production edits. Command: `javac -J-Xmx1500m @.tmp/w7items-javac.args`.
  Java 21, `-proc:none --release 21`, output `.tmp/w7items-javac`, log
  `.tmp/w7items-javac.log`. Classpath includes the named remapped 1.21.1 Loom
  minecraft-merged JAR, `common/build/classes/java/main` (absent in this fresh
  worktree; all main sources were compiled instead), module JARs from
  `~/.gradle-leader/caches/modules-2`, and existing temporary remapped
  Architectury / access-widened Minecraft support from `/tmp/opencode`.
  Bare non-widened vanilla first rejected the port's existing protected access;
  the final run uses the same widened API needed by the actual build.
* Authored **five** pure JUnit 5 cases in
  `common/src/test/java/thaumcraft/client/RemainingItemTilePoseParityTest.java`:
  bone-bow constants, scanner interpolation, OBJ/preview facing values, 40-tick
  preview cycling, three-tick wand raise/pivot. No Block constructed.
  **Compile checked, NOT executed** (`.tmp/w7items-test-javac.log`, exit 0).
  Existing array-page/wand tests were not authored here; corrected one existing
  array-page comment from two caps to three. No claim they were executed.
* Scope ledger `--check --original-root ...`: **stale_review_fingerprints: 0**.
  Only the six owned reviews changed; `counts` untouched. Older evidence remains
  historical; the w7 notes explicitly supersede stale blockers.
* No asset verification required: no assets changed. No Gradle invoked.
* Remaining blockers are the original bow glint and mirror projected-coordinate
  shader, plus the leader's actual Fabric runtime/mixin/frame acceptance of all
  implemented changes. No public push, other worktree, or leader tree touched.
