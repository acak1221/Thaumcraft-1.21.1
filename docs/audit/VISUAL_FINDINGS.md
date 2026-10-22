# Visual pipeline defect hunt — 2026-10-23

Baseline `e79b8558`, branch `agent/h2visual`. **No client, Gradle, Java compilation, GPU shader compilation or inspected frame.** Fixes below are code-contract fixes, not visual acceptance. Paths are repository-relative. Line numbers refer to the final audit tree unless marked baseline.

## Counts and scope

Counts are inspected objects, not assertions of runtime coverage. A finding belongs to one primary hunt; cross-references are not counted twice.

| Hunt | Checked | PROVEN BY READING | SUSPECTED |
|---|---|---:|---:|
| 1. Registration/reachability | 51 BER implementations (including dispatcher delegates), 56 entity bindings, 29 item-model ancestry chains, 3 core-shader registrations on each loader, 23 client mixin entries; scan and reload callbacks | 6 | 0 |
| 2. Render type / texture | 127 `RenderType.*(...)` call sites enumerated across modern common client; 170 literal PNG references (129 distinct); focused state/format review of scanner, robe, node, beam, architect, mirror, particles and block adapters | 0 | 1 |
| 3. Transforms | 8 chains: scanner GUI, scanner first person, wand, mirror, node jar, aura item, excavation, architect axes | 4 | 0 |
| 4. Clipping/culling | 10 paths: six mirror faces, stabilizer bubble, excavation endpoint, architect cube, node-jar planes | 0 | 0 |
| 5. UV/sprite math | 8 paths: nodes, generic particles, beam scroll/impact, mirror pane, scanner, stairs, crust, radial | 2 | 0 |
| 6. Shaders | 3 core programs + blur program, their actual vertex/fragment stages and consumers | 0 | 1 |
| 7. Mixin contracts | 23 client mixins: 19 common, 1 Fabric, 3 NeoForge; 20 common/Fabric entries checked against vanilla javap, 3 NeoForge entries compared with available vanilla signatures only | 0 | 0 |

**Total: 12 distinct proven defects, 11 fixed, 1 deliberately left open; 2 suspected issues left open.** The existing report names 15 effects in its “waves 4–8” sentence, despite the task's fourteen-feature shorthand. This audit does not turn either count into seen-on-screen evidence.

## 1. Registered at all?

### R1 — PROVEN BY READING — fixed `04ae3e7b`

`common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java:36-87` had **no caller anywhere**. Added the call at `common/src/main/java/thaumcraft/client/ClientSetup.java:190`, reached by both loaders.

All **13 registry calls** were dormant: eldritch dispatcher, eldritch nothing, portal, item mirror, essentia mirror, banner, brainbox, wand pedestal, flux scrubber, ethereal bloom, warded glass owner, legacy wand pedestal, portable hole. Dispatcher delegates account for additional implementations, not additional registrations.

**User sees:** no dynamic renderer for those block entities: missing mirror tunnel/pane, banners, bloom and other device geometry/effects. Static block geometry, where separately supplied, does not prove the BER works.

### R2 — PROVEN BY READING — fixed `0bf886dc`

`common/src/main/resources/assets/thaumcraft/models/item/jar_node.json:2` previously inherited `thaumcraft:block/jar_node`, a normal element model, not `builtin/entity`. Both loaders registered `ItemJarNodeRenderer`, but vanilla `ItemRenderer.render` takes its custom-renderer branch only for `BakedModel.isCustomRenderer()`.

Now inherits `thaumcraft:item/jar`, whose root is `minecraft:builtin/entity`; shared display transforms are retained and the renderer still draws the separate **block** model for its shell, so there is no recursion.

**User sees before fix:** glass jar item without the captured node, regardless of saved aspects.

### R3 — PROVEN BY READING — fixed `cafcd96a`, generator follow-up `cd6c5408`

`common/src/main/resources/assets/thaumcraft/models/item/aura_node.json:2` was `minecraft:item/generated` with a shard layer. The registered `ItemNodeRenderer` could not run. Changed to `builtin/entity` and kept shard as the particle texture. Updated `scripts/porting/convert_assets.py:409-412` so regeneration does not restore the broken gate.

**User sees before fix:** flat shard instead of animated aura-node item.

### R4 — PROVEN BY READING — OPEN, not safe to fake

`common/src/main/resources/assets/thaumcraft/models/item/advanced_node_stabilizer.json:2` is `builtin/entity`. The actual item is registered at `common/src/main/java/thaumcraft/common/config/ThaumcraftNodeDevices.java:60-61`.

Neither loader supplies its item renderer: Fabric registers only the basic block at `fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java:114`; NeoForge does likewise at `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java:136`. Merely registering the basic callback a second time would not fix it: `common/src/main/java/thaumcraft/client/renderers/tile/TileNodeStabilizerRenderer.java:45` explicitly rejects any stack other than the basic stabilizer. Its model path also expressly implements basic lock1, not advanced lock2.

**User sees:** invisible advanced stabilizer inventory/held/dropped item. Correct advanced geometry/material implementation remains needed; using basic artwork would conceal rather than fix the parity defect.

### R5 — PROVEN BY READING — fixed `e073ed04`

Fabric called `ScanOverlay.render` at `fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java:139`; no NeoForge rendering caller existed. Added the shared call at `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java:235`.

**User sees before fix:** elemental-pickaxe scan activation on NeoForge produces no ore/fluid overlay.

### R6 — PROVEN BY READING — fixed `f68e974b`

`neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java:188-196` returned its own completed apply future without ever invoking the preparation barrier. Vanilla `SimpleReloadInstance` initializes `preparingListeners` from **all** listeners; its anonymous `PreparationBarrier.wait(T)` removes the listener and completes `allPreparations` only after the set becomes empty. Completing the returned listener future is not that signal.

**User sees before fix:** resource loading cannot finish (initial loading/reload screen remains), so none of the new visuals can be reached. Replaced with `completedFuture(null).thenCompose(barrier::wait).thenRunAsync(...)`.

### Registration checks without a finding

Both loader metadata files load the common mixin config and their respective client configs. NeoForge bootstraps common `ClientSetup` via enqueued client setup; Fabric calls it directly. Three shaders are registered on both loaders with matching formats. All 51 BER implementation constructors have a registry or dispatcher constructor reference; fixing R1 makes the dormant registry reachable. The central entity list has 56 bindings. These checks establish wiring, not actual frames or patched-loader class application.

## 2. Render type vs texture

All 170 literal PNG references resolve in mod resources or the remapped vanilla jar; eight vanilla-namespace references were not misreported as missing mod textures. Computed texture names are **not exhaustively validated** by that literal scan. Atlas consumers inspected use `TextureAtlas.LOCATION_BLOCKS` plus sprite UVs; standalone model/FX sheets use their own resource location. A cutout-authored PNG passed to `entityTranslucent` is not automatically invisible: alpha content and desired blending matter.

### S1 — SUSPECTED — architect's final buffer is not flushed at its world callback

`common/src/main/java/thaumcraft/client/ThaumcraftArchitectClient.java:93-110,133-135,185` queues translucent faces/arrows without ending their batches. The loader callbacks end only `RenderType.lines()` (`fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java:136-139`, NeoForge corresponding marker tail).

Switching render types flushes some non-fixed buffers, but the final architect type may remain queued until another renderer or GUI operation flushes it. This depends on buffer ownership and subsequent draws, so not labelled certain invisibility.

**Possible user-visible result:** last arrow/face missing or drawn later under the wrong matrix/state; intermittent rather than uniformly absent. Needs a frame and buffer-flush tracing. No speculative state change made.

## 3. Transform order

### T1 — PROVEN BY READING — fixed `1773945b`

`common/src/main/java/thaumcraft/client/renderers/tile/TileMirrorRenderer.java:74-100` converted the original 0..1 quad to a centered quad by adding `.5` in **world** coordinates before rotating it. That is not addition in rotated local coordinates.

Baseline NORTH put the plane at z=1.48, not .98. DOWN put it at y=1.48, with z=1..2; SOUTH put it at z=.52, with x=1..2. The other orientations were likewise displaced. **All six planes were wrong**, not just a scale aesthetic.

Restored literal oracle `TileMirrorRenderer:628-647` translation/rotation chains and `UtilsFX:402-405` 0..1 corners. Independent Python matrix evaluation checked all **24 corners** lie on the appropriate .02/.98 face and within the block (at zero instability).

**User sees before fix:** floating/misaligned mirror pane, possibly embedded in neighbouring terrain, instead of covering its frame. R1 previously hid this defect by preventing the renderer from running at all.

### T2 — PROVEN BY READING — fixed `322db81b`

`common/src/main/java/thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java:48` rotated the second node plane about **Z**, while oracle `ItemJarNodeRenderer:70` uses `(0,1,0)`, **Y**. The comment even described Y's tuple as Z. Restored Y.

**User sees before fix, once R2 is repaired:** two coplanar passes instead of three orthogonal planes; node looks doubled/flat and loses coverage from some viewing angles.

### T3 — PROVEN BY READING — fixed `4e49ba29`

`common/src/main/java/thaumcraft/client/ThaumcraftExcavationClient.java:204-207` draws beam length along **local +Y**, but used pitch measured as elevation above XZ. Horizontal target means elevation=0: yaw and spin leave local +Y vertical. Changed pitch to `90 - elevation`.

**User sees before fix:** beam shoots upward for a horizontal mining target; impact sprite remains at the actual target, visibly disconnected. A vertical-up shot instead points horizontally. Independent matrix evaluation checked the repaired +Y mapping for **1,004 directions**, including up/down/cardinal and seeded random directions.

### T4 — PROVEN BY READING — fixed `e2ccbd20`

`common/src/main/java/thaumcraft/client/ThaumcraftExcavationClient.java:98,132-135` interpolated from `previousSize`, but nothing ever assigned that field. It stayed zero throughout every beam's lifetime. Tick now retains the previous age-derived width before advancing age.

**User sees before fix:** every tick the beam collapses toward zero width/length and regrows with partial tick, even after the startup pulse should be complete. High-FPS continuous beam flickers/saws rather than remaining full-sized.

No unmatched push/pop was found in the eight inspected chains. This is not an exception-safety proof for every client class.

## 4. Clipping and culling

T1 proves out-of-block geometry; T3 proves the wrong spatial axis. They are counted in hunt 3, not counted again here. Repaired mirror chains keep all 24 corners inside the block at zero instability. Stabilizer explicitly opts into offscreen BER drawing and applies bounded distance via a two-block volume; the .9-radius bubble is not silently treated as a one-block-only BER. Architect's cube vertices are in 0..1 blocks, and candle boxes explicitly multiply block coordinates by 16 for `FaceBakery`.

No additional certain culling defect established in the ten inspected paths. Linked mirror deliberately enables culling and chooses vertex winding by axis/direction; correctness of its projected view still requires a frame.

## 5. UV and sprite math

### U1 — PROVEN BY READING — fixed with T1 in `1773945b`

`common/src/main/java/thaumcraft/client/renderers/tile/TileMirrorRenderer.java:99` previously mapped the left/top local corner to U=0 and the right/top to U=1. Oracle `UtilsFX:402-405` does the opposite. Restored the literal UV ordering and oracle -Z normal.

**User sees before fix:** mirror-pane artwork horizontally reversed relative to the original orientation. This is independent of the misplaced plane.

### U2 — PROVEN BY READING — fixed `cef593f4`, incorrect test repaired `56802cd1`

`common/src/main/java/thaumcraft/client/ThaumcraftExcavationClient.java:263` used `sin(-value*.1)` where oracle `FXBeamWand:205` calls `MathHelper.func_76141_d`, **floor**. For age10/partial0 the original gives -1; sine gives approximately -1.15853. Restored `Mth.floor`.

**User sees before fix:** beam texture crawls/oscillates with the wrong phase rather than the original wrapped scroll. Existing `common/src/test/java/thaumcraft/wands/ExcavationBeamRulesTest.java:50-56` actually asserted the wrong sine formula; replaced it with independent numeric -1 and -.2 examples. Java test **not run** under the no-Gradle restriction.

Other inspected sheet dimensions: node atlas 2048² / 32 cells; generic particles 256² / 16 cells; scan screen128²; arrows64²; mirror panes16²; robe sheets256×128; beam2 64². Normalized sprite UV interpolation is appropriate for 1.21.1 `TextureAtlasSprite`; a literal 256 divisor was not blindly assumed for those standalone sheets. No additional proven frame-index error found in the eight paths.

## 6. Core shaders and blur

Read the actual remapped jar's `particle.vsh`, `position_tex_color.vsh`, `particle.json` and post `blit.vsh`, not a different Minecraft version.

| Program | Attributes actually consumed | Declared non-sampler uniforms / samplers | Result |
|---|---|---|---|
| linked_mirror | Position, UV0, Color | 4 / 1 | POSITION_TEX_COLOR matches; PortalQ/PortalTextureMat set before immediate draw; vertex-to-fragment projectedTexture/vertexColor types match |
| node_stabilizer_bubble | Position, UV0, Color | 3 / 1 | Vanilla position_tex_color vertex stage + custom fragment interfaces match |
| monster_lit_particle | Position, UV0, Color, UV2 | 7 / 2 | Vanilla particle vertex stage + custom fragment interfaces match PARTICLE format and lightmap sampler |
| thaumcraft_blur | Position | ProjMat, InSize, OutSize, BlurDir, Radius / DiffuseSampler | Manifest names/types fit both stages; texCoord/oneTexel interfaces match; horizontal/vertical passes supply BlurDir and Radius10 |

Automated declaration/interface-set checks passed for all three core programs. Read blur manifest/stages and post-chain inputs. No missing uniform/interface was demonstrated; **not** a driver compile/link pass. Missing/optimized-out JSON uniforms alone do not establish a GLSL link failure.

Fabric shader namespace support was verified by reading the installed Fabric rendering API sources: `FabricShaderProgram.rewriteAsId` and `ShaderProgramMixin` rewrite namespaced stage IDs. Do **not** incorrectly declare these core shaders broken merely because vanilla `ShaderInstance` uses default-namespace paths. Post effects intentionally live under `assets/minecraft/shaders/program` and use plain pass names.

### S2 — SUSPECTED visual consequence — relay flare ignores its supplied lightmap

`common/src/main/java/thaumcraft/client/renderers/VisNetRender.java:107-115` builds a PARTICLE-format flare with per-vertex light but selects the shared **bubble** shader, whose format is POSITION_TEX_COLOR and whose stages do not read UV2/Sampler2. Those first three attribute names overlap, so this does **not** prove a link failure or invisible quad. It does prove the flare's supplied light cannot affect its sampled colour.

**Possible user-visible result:** relay flare too bright/unlit compared with surrounding light, especially in darkness. Whether that is the intended unlit look or a meaningful mismatch needs the original/ported frame comparison; no speculative shader substitution made.

## 7. Mixins and vanilla contracts

Used `javap -p -c -l -s -classpath <remapped-1.21.1-jar>` on target classes. All 20 common/Fabric client entries have their named methods or accessor fields in the jar, and inspected callback descriptors match their vanilla signatures. Explicit descriptors for wand `renderStatic`, vanilla `tesselateBlock`, and armour six-argument entry match erasure. In particular:

* `TableOutlineMixin`: `renderHitOutline` invokes exactly `BlockState.getShape(BlockGetter,BlockPos,CollisionContext):VoxelShape`.
* `WandFocusKeyMixin`: the offhand field is followed by `KeyMapping.consumeClick():boolean` inside `handleKeybinds`; the drop-field slice boundary exists.
* `FabricRobeArmorLayerMixin`: `renderArmorPiece` invokes `ItemStack.is(TagKey):boolean`; this redirect remains Fabric-only.
* `TaintOverlayChunkLayerMixin`: `SectionCompiler.compile` invokes `PoseStack.popPose()` at bytecode300 after `renderBatched`; its local-variable table at that point contains exactly three BlockPos locals, with the current position at ordinal2, plus the requested world/pack/state/pose/map locals. Shadow `getOrBeginLayer(Map,SectionBufferBuilderPack,RenderType):BufferBuilder` exists.
* `Tc4ParticleWorldRenderMixin`: vanilla `renderLevel(DeltaTracker,boolean,Camera,GameRenderer,LightTexture,Matrix4f,Matrix4f):void` matches, and vanilla pops its model-view stack before the tail pass restores the view.
* Screen tooltip tail methods, texture animation cycle, particle add/setLevel/close/crack, dimension effects, fog setup, biome grass/foliage and item-colour accessors all exist with the inspected argument/field types.

The 19 common client entries are: Tc4ParticleEngineMixin, ParticleLevelAccess, Tc4ParticleWorldRenderMixin, TaintOverlayChunkLayerMixin, OreAnisotropyMixin, PositionBlockModelMixin, WandRenderContextMixin, BoneBowHeldPoseMixin, OuterLandsDimensionEffectsMixin, TableOutlineMixin, WandFocusKeyMixin, MinecraftItemColorsAccess, WarpFogMixin, WarpPostEffectsMixin, WardedHitParticleMixin, ResearchPopupScreenMixin, Tc4BiomeColorMixin, ContainerScreenAspectRowMixin, AbstractContainerScreenAccess.

**Limit, not a fabricated success:** the permitted jar is vanilla/Fabric-remapped, not NeoForge's patched Minecraft. NeoForgePositionBlockModelMixin's ModelData overload is necessarily absent there; that absence is not proof of a broken NeoForge mixin. NeoForgeGearArmorMixin's vanilla-erased six-argument method and NeoForgePureFluidMixin's two method signatures exist in vanilla, but their patched classes and common mixin local/anchor stability still require leader validation. No NeoForge-patched javap or runtime application was performed. Gameplay/server and smoke-only mixins are outside these 23 visual entries.

## Validation actually run

* Initial worktree status clean; baseline confirmed. No other worktree or main checkout accessed.
* `python3 -m unittest discover -s scripts/porting -p 'test_convert_assets.py'`: **58 passed**, 3.707s.
* 29 item-model inheritance chains resolve to `builtin/entity` after fixes; the advanced item's missing callback remains separately open.
* 170 literal PNG references exist in mod resources or the remapped vanilla jar; 3 core program interface checks passed.
* Independent Python calculations: 24 mirror corners on correct block faces; 1,004 beam directions map local +Y to the target. Width-assignment and floor-wrap source checks passed.
* Javap outputs retained locally under ignored `.tmp/h2visual/`; this report records their target/contract conclusions, not an invented injection run.
* **No Gradle, client, compiled Java tests, shader driver or visual acceptance.** Leader must compile and inspect both-loader behaviour, especially now-reachable R1 renderers and advanced stabilizer R4.
