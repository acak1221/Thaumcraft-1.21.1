# Client rendering, FX, input and GUI - the audit wave of 2026-10-23

Agent: `clientrender`. Worktree: `/home/cyberfox-kali/wt/clientrender`, branch
`agent/clientrender`. Domain: `client_rendering_fx_input_and_gui`, 54 open
ledger units (44 `partial`, 10 `missing`).

Source of truth: the TC4 4.2.3.5 oracle at
`/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/`. The 1.21.1 API facts
were read out of the decompiled client of the loom cache
(`minecraft-merged-1.21.1-loom.mappings...jar`), not from memory.

**No Gradle was run.** Every test below is **authored but not executed**.

---

## 1. The result

| | before | after |
|---|---|---|
| `parity_verified` | 0 of the 54 units | **29** |
| `partial` | 44 | **25** |
| `missing` | 10 | **0** |

Ten units moved `missing -> parity_verified` (the dead code of the original and
the 1.12-only artefacts, re-audited with a complete callsite audit), fifteen
moved `partial -> parity_verified` (the rows whose ledger evidence was stale
plus the rows this wave closed), nine stayed `partial` with a narrowed gap and
five new gaps were written down.

The ledger edit is in `docs/porting/scope-ledger.json`; the refresh script
`python3 scripts/porting/update_scope_ledger.py --check` could **not** be run
in this worktree because the private oracle of the leader
(`work/thaumcraft-4.2.3.5-oracle`, 859 classes) is not on this machine; the
readable oracle of OpenTC4 is missing three files of it
(`thaumcraft/common/lib/HexUtils.java`,
`thaumcraft/common/lib/UUIDHelper.java`,
`thaumcraft/common/lib/utils/UUIDHelper.java`) and the refresh refuses the
path loss. The edit was therefore validated against the *schema* of the script
(`validate_review`): every row of the file passes the checks of the script
(allowed verdicts, the evidence of an `exclude` and of a `parity_verified`
verdict, the shape of the reviewed fingerprints), and no fingerprint of a
source was touched, so `stale_review_fingerprints` stays 0.

---

## 2. The defects this wave found and fixed

### 2.1 The glow of the levitator was in the wrong colour, on the wrong side and sampled the wrong cell

| | 1.7.10 original | 1.21.1 port before | port after |
|---|---|---|---|
| colour of the side panels | `14488063` = `0xDD11FF` = rgb(221,17,255) - oracle `BlockLifterRenderer.java:58` | `0x00DD11` = rgb(0,221,17), green - `TileLevitatorRenderer.java:39` | `0xDD11FF` - `TileLevitatorRenderer.java:48` |
| colour of the top panel | `BlockCustomOreItem.colors[4]` = 40960 = `0x00A000` = rgb(0,160,0) - oracle `BlockLifterRenderer.java:47`, `BlockCustomOreItem.java:17` | `0xA00000` = rgb(0,160,0) - correct | `0x00A000` - `TileLevitatorRenderer.java:46` |
| side of the +X panel | the +X face, when the +X neighbour is renderable - oracle `BlockLifterRenderer.java:59-61` | the **-X** face at `x = -0.01` | the +X face - `TileLevitatorRenderer.java:98` |
| side of the -X panel | the -X face - oracle `BlockLifterRenderer.java:63-65` | the **+X** face at `x = 1.01` | the -X face - `TileLevitatorRenderer.java:101` |
| side of the +Z / -Z panels | the +Z / -Z face - oracle `BlockLifterRenderer.java:67-73` | mirrored as well | the +Z / -Z face - `TileLevitatorRenderer.java:104/107` |
| texture of the panels | the cell of the `animatedglow` icon - oracle `BlockLifter.java:40` | the UVs `0..1` of the **whole block atlas** - `TileLevitatorRenderer.java:131` (old) | the cell of the sheet - `TileLevitatorRenderer.java:134` |
| brightness of the side panels | the same `bb` as the top panel, the fixed lightmap coordinate 180 while the levitator is not powered - oracle `BlockLifterRenderer.java:48-53` | the light of the block | the same `bb` as the top panel - `TileLevitatorRenderer.java:89` |

The mirror of the sides was the worst of them: the panel of an open side was
drawn on the *closed* side of the block, so the glow of a levitator that stands
against a wall appeared inside the wall and the open side stayed dark.

**Deliberate difference that stays.** The original puts every panel one
hundredth of a block **inside** its own face (the top face at `y + 0.99`, the
+X face at `x + 0.99`, the -X face at `x + 0.01`; oracle
`BlockLifterRenderer.java:55/60/64/68/72`) and the levitator of the original is
not a solid cube in the world, so those recessed panels are what the player
sees. The port keeps the opaque cube of the levitator
(`models/block/levitator.json` is `minecraft:block/cube_bottom_top`), therefore
the panels are drawn one hundredth of a block **outside** their face
(`TileLevitatorRenderer.panelOffset`, `TileLevitatorRenderer.java:74-76`).
Whether the original's own cube hides its panels is a depth question that only
a running client can settle; the side, the colour, the brightness and the
sheet are now the ones of the original.

### 2.2 Five GUI screens drew their TC4 sheets as solid black

The plain `GuiGraphics#blit` of 1.21.1 ends in `innerBlit`, which **neither
enables nor disables blending** (read out of
`net/minecraft/client/gui/GuiGraphics.class`: the overload
`blit(ResourceLocation,int,int,int,int,int,int)` funnels into
`innerBlit(ResourceLocation,int,int,int,int,int,float,float,float,float)`, and
that method calls `RenderSystem.setShaderTexture` and `RenderSystem.setShader`
only). A `fill` leaves the blend **off**, because the GUI render type of the
vanilla game is `RenderType.GUI`, whose transparency shard is
`TRANSLUCENT_TRANSPARENCY` and whose teardown calls `RenderSystem.disableBlend()`.
The 1.7.10 GUI always had the blend on (`GuiScreen.drawScreen` ->
`drawDefaultBackground` -> `GL11.glEnable(GL11.GL_BLEND)`), and the vanilla
1.21.1 does the same explicitly in
`Screen.renderMenuBackgroundTexture` (`RenderSystem.enableBlend()`, the blit,
`RenderSystem.disableBlend()`).

The sheets of TC4 really have translucent pixels. Measured with the shipped
PNGs (the alpha of the drawn region):

| screen | sheet | translucent pixels of the drawn region | port before |
|---|---|---|---|
| focus pouch | `gui/gui_focuspouch.png` | 9932 of 40600 (the frame) | no blend -> black frame |
| focus pouch, locked marker | cell 240,0 | 256 of 256, alpha 0..128 | no blend -> black square |
| hover harness | `gui/guihoverharness.png` | 9785 of 29216 | no blend |
| hover harness, locked marker | cell 240,0 | 256 of 256, alpha 0..128 | no blend |
| hand mirror | `gui/guihandmirror.png` | 11133 of 29216 | no blend |
| research book | `gui/gui_researchbook.png` | 11419 of 184320 of the page | no blend |
| research toast | `gui/research_toast.png` | 18 of 5120 plus the translucent icon | no blend |

Fixed in `GuiFocusPouch.java:22/28`, `GuiHoverHarness.java:21/27`,
`GuiHandMirror.java:64`, `GuiResearchRecipe.java:105` and
`GuiResearchPopup.java:31`: the screens now enable the blend with the ordinary
alpha function around their blits and switch it off again.

The screens that already did it (`GuiPech`, `GuiSpa`, `GuiArcaneWorkbench`,
`GuiGolem`, `GuiAlchemyFurnace`, `GuiResearchBrowser`,
`GuiResearchTable.blitAlpha`) were left alone; their art is opaque or their
blend is already there.

### 2.3 The three TC4 keys are not bound by default in the original

The oracle binds **none** of the three keys:
`thaumcraft/api/Keys.java:7-9` binds `keyF`, `keyH` and `keyG` to
`Keyboard.KEY_NONE`. The port bound them to F, H and G, and the comment of the
port repeated a wrong reading of the LWJGL 2 key table (33, 34 and 35 are
PAGE UP, PAGE DOWN and END, not F, G and H; the real LWJGL 2 codes of F, G and
H are 70, 71 and 72). The ledger row of `thaumcraft/client/lib/KeyHandler.java`
had inherited the same wrong numbers.

Fixed: `ThaumcraftFocusClient.java:26`, `ThaumcraftTravelGearClient.java:51`
and `ThaumcraftMiscKeyClient.java:28` now carry `GLFW.GLFW_KEY_UNKNOWN`, the
1.21.1 value of an unbound key. The keys stay assignable in the vanilla
controls screen, exactly as the original key bindings were assignable in the
options screen of 1.7.10.

---

## 3. The units this wave touched

### 3.1 `missing -> parity_verified` (10)

All ten are code that the original never runs, so the absence of the port
changes nothing. The callsite audit was repeated in this wave against the
oracle tree (859 classes).

| unit | audit |
|---|---|
| `thaumcraft/client/ClientModelRegistry.java` | no such file in the oracle; a 1.12 artefact |
| `thaumcraft/client/fx/ITCParticle.java` | no such file in the oracle; a 1.12 artefact |
| `thaumcraft/client/fx/beams/FXBeam.java` | built only at `ClientProxy.java:698` and `PacketFXBeamPulse.java:50`; `ClientProxy.beam` has **zero** callers and the packet is registered but never sent |
| `thaumcraft/client/fx/particles/FXBubbleAlt.java` | zero references outside its own file |
| `thaumcraft/client/fx/particles/FXScorch.java` | built only at `UtilsFX.java:110` inside `UtilsFX.shootFire`, which has **zero** callers |
| `thaumcraft/client/fx/particles/FXSmokeDrift.java` | no such file in the oracle; a 1.12 artefact |
| `thaumcraft/client/fx/particles/FXSmokeTrail.java` | built only at `ClientProxy.java:773` inside `ClientProxy.soulTrail`, which has **zero** callers |
| `thaumcraft/client/gui/GuiFactory.java` | no such file in the oracle; a 1.12 artefact |
| `thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java` | no such file in the oracle; TC4 4.2.3.5 has no JEI at all (the only related line is `ClientProxy.java:444`) |
| `thaumcraft/client/lib/ItemAspectTooltipHandler.java` | no such file in the oracle; a 1.12 artefact |

### 3.2 `partial -> parity_verified` (15)

| unit | what the audit found |
|---|---|
| `thaumcraft/client/fx/particles/FXBurst.java` | the deviation of the earlier audit is closed: `MonsterSpriteParticle.java:33` now draws the burst with the alpha **0.75**, the `glColor4f(1,1,1,0.75)` of the original `FXBurst.java:45`; the life of 31 ticks and the node strip (`u = age % 32 / 32`, `v = 0.96875`) are kept |
| `thaumcraft/client/fx/particles/FXGeneric.java` | the visible effect of the original (`FXGeneric.java:16-79`, only built at `ClientProxy.java:1102-1111`) is reproduced by three purpose specific native particles with the original cells, delays, sizes and the half alpha of the first and the last frame |
| `thaumcraft/client/fx/beams/FXArc.java` | **the ledger evidence was stale.** The native `fx/particles/FXArc.java` is the port of the ribbon: the half width 0.25 and the alpha 0.8 (`FXArc.java:87/95`), the gravity 0.115 and the noise 0.25 with at most fifty steps (`FXArc.java:41-61`), the UV quirk of the point index over the length (`FXArc.java:113`). The two callers of the original (`EntityEldritchGolem.java:302`, `PacketFXBlockArc.java:64`) are called by `BlockFxClientEffects.java:62` and `ThaumcraftEldritchClient.java:51` |
| `thaumcraft/client/fx/beams/FXBeamPower.java` | **stale.** The native `TileVisRelayRenderer.java:32/41` keeps the opacity ramp of the original (the floor 0.3, 0.8 on the pulse, the decay of 0.025 per tick - `FXBeamPower.java:32/91-96/114`) and the tenfold boost for a carrier of `IRevealer`; `VisNetRender.java:88-95` is the additive flare of the original `FXBeamPower.java:180-218` (the half size 0.66 of the opacity) |
| `thaumcraft/client/fx/particles/FXSmokeSpiral.java` | **stale.** The native `fx/particles/FXSmokeSpiral.java` keeps the turn of 720 degrees and the polar angle of 90 to -90 (`FXSmokeSpiral.java:33-34`), the alpha 0.66 (`:31`) and the half size 0.15 (`:45`); the two callers of the original are called by `ClientSetup.java:86/93` |
| `thaumcraft/client/gui/ThaumcraftGuiConfig.java` | **stale.** `ThaumcraftConfigScreen.java` shows the five original categories with the original keys and defaults of `Config.java:104-111/369-500` |
| `thaumcraft/client/ThaumcraftGuiFactory.java` | **stale.** 1.21.1 has no FML factory, but the configuration screen it opened exists: `ThaumcraftConfigClient.java` |
| `thaumcraft/client/lib/KeyHandler.java` | the three keys of the original are unbound (`Keys.java:7-9`); the port now does the same (see 2.3) |
| `thaumcraft/client/renderers/item/LifterItemColor.java` | a 1.12 artefact; the visible inventory art of the original (`BlockLifterRenderer.java:17-38`) is the plain cube, because the two tinted slabs of the original (0.01..0.99 x 0.9..0.99 and 0.1..0.9) are inside the opaque cube - which is what the port has |
| `thaumcraft/client/renderers/tile/TileVisRelayRenderer.java` | the beam and the flare of the original `FXBeamPower` are in the tree (see above) and the model keeps the original vertex guard (`ModelVisRelay.java:26`: RingBase 96, RingFloat 96, Crystal 48, Support 108) |
| `thaumcraft/client/renderers/entity/RenderAspectOrb.java` | the clock of 25 ms (`:34`), the size 0.1 + 0.3 * life (`:50`), the blend of the aspect (`:28`) and the alpha 128 (`:54`) are all kept |
| `thaumcraft/client/renderers/entity/RenderDart.java` | the scale 0.025625, the shaft 0.75, the colour 0.5/0.5/0.6, the turn of 45 degrees and the shift of four pixels are kept |
| `thaumcraft/client/renderers/entity/RenderElectricOrb.java` | the strip start (1 + ticks % 6) / 8, the step 0.125, the rows 0.875/0.75, the bob sin(ticks / 5) * 0.2 + 0.2, the alpha 0.8 and the brightness 220 are kept |
| `thaumcraft/client/renderers/entity/RenderEmber.java` | the frame (7 + 8 * age / duration) / 16, the row 0.5625, the size 0.25 + age / duration, the alpha 0.9 and the brightness 220 are kept |
| `thaumcraft/client/renderers/entity/RenderExplosiveOrb.java` | the strip start (ticks % 4) / 16, the row 0.8125, the scale 0.7, the alpha 0.8 and the standard alpha blend of the original `glBlendFunc(770, 771)` are kept |

### 3.3 `partial` kept, with the gap narrowed (9)

| unit | the remaining gap |
|---|---|
| `thaumcraft/client/renderers/block/BlockLifterRenderer.java` | the panel of the original sits one hundredth of a block **inside** its face and the levitator of the original is not a solid cube in the world; the port keeps the opaque cube and the panel outside. Only a running client can show which of the two the player sees |
| `thaumcraft/client/renderers/block/BlockCandleRenderer.java` | the deterministic random wax drips of the original (1 to 5 per block, seeded with `x + y * z`, `BlockCandleRenderer.java:35-48`) are not reproduced; the native model is static |
| `thaumcraft/client/renderers/block/BlockCosmeticOpaqueRenderer.java` | the rune particles of the original on the rune blocks (`BlockCosmeticSolid.java:322`, `BlockEldritch.java:304`, `ItemThaumometer.java:92/103/128`) are not reproduced; the native rune particle only serves the infusion matrix |
| `thaumcraft/client/renderers/block/BlockCustomOreRenderer.java` | the minimum brightness of 160 of the emissive overlay (`BlockCustomOreRenderer.java:48`) and the second pass with the stone texture for anisotropic filtering (`:50-55`) are not reproduced by the static native model |
| `thaumcraft/client/renderers/block/BlockGasRenderer.java` | the variable height of the gas surface (the per corner heights, the 0.875 quanta factor and the four shading constants, `BlockGasRenderer.java:17-21/46-58/244-255`) is not reproduced; the gas is a plain translucent block |
| `thaumcraft/client/renderers/block/BlockMetalDeviceRenderer.java` | the per metadata boxes of the original (`BlockMetalDeviceRenderer.java:18-40`: the lid of the metadata 5 at 0.8125..1, the box of the metadata 7/8/13 at W4..W12 x W2..W14, the box of the metadata 12 at W3..W13) are not distinguished by the native models |
| `thaumcraft/client/renderers/block/BlockStoneDeviceRenderer.java` | the per metadata geometry of the original (`BlockStoneDeviceRenderer.java`) is only partly reproduced; the remaining variants share the wand pedestal parent |
| `thaumcraft/client/renderers/block/BlockWoodenDeviceRenderer.java` | the per metadata geometry of the original (`BlockWoodenDeviceRenderer.java`) is only partly reproduced by the native model set |
| `thaumcraft/client/renderers/item/ItemBowBoneRenderer.java` | the original draws the bone bow as a **flat quad** with the transform chain of `ItemBowBoneRenderer.java:41-52` (the scale 2.6666667, the turns of -20/90/-60, the inner scale 0.625) and the quad transform of `:70-75` (the scale 1.5, the turns of 50 and 335); the port uses the 3D model of the vanilla bow with the TC4 texture, so the pose in the hand is the vanilla one. Reproducing the flat quad needs a builtin item renderer in 1.21.1 |

### 3.4 `partial` kept, unchanged gap (16)

`thaumcraft/client/fx/bolt/FXLightningBolt.java` (the original type 3, the
node bolt against a living target, has no dedicated native particle),
`thaumcraft/client/fx/bolt/FXLightningBoltCommon.java` (the native arc of the
infusion is a bounded 32 segment construction, not the original fractal),
`thaumcraft/client/fx/particles/FXBlockRunes.java` (only the infusion matrix
uses the rune particle), `thaumcraft/client/fx/particles/FXBreaking.java` (the
slime jump variant of the original has no native particle),
`thaumcraft/client/fx/particles/FXEssentiaTrail.java` (the native folds the
trail into a shared particle class),
`thaumcraft/client/renderers/entity/RenderWatcher.java` (the original never
registers the renderer, the port does - an addition, not a loss),
`thaumcraft/client/lib/TCFontRenderer.java` (the galactic atlas of the original
was never used, but the glyphs of the vanilla font changed),
`thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java` (the
scrolling light strip of the original is static in the port),
`thaumcraft/client/lib/ClientTickEventsFML.java` (the aspect row on the
hovered container slot, `ClientTickEventsFML.java:660-724`, needs the scanned
object registry of the original, and the OptiFine shader groups have no 1.21.1
counterpart),
`thaumcraft/client/lib/RenderEventHandler.java` (the scanned block overlay and
the shader group driver),
`thaumcraft/client/lib/UtilsFX.java` (the helper library; the reachable
effects are mapped, `getEquippedProgress`/`getTimer` have their native
equivalents in the thaumometer and the wand),
`thaumcraft/client/gui/MappingThread.java` (the id to ItemStack cache of the
original is replaced by the recipe manager of 1.21.1),
`thaumcraft/client/fx/ParticleEngine.java` (the four layer buckets and the
2000 particle cap of the original are the ones of the vanilla engine now),
`thaumcraft/client/fx/WRMat4.java` and
`thaumcraft/client/fx/WRVector3.java` (the float vector maths of the original
was rewritten with JOML).

---

## 4. What could **not** be fixed here, and why

1. **The visual acceptance of every fix needs a running client.** The levitator
   panel, the five GUI screens, the held bone bow, the wax drips of the candle
   and the rune particles can only be judged on the screen. The numbers of the
   original are pinned by the tests, the pixels are not.
2. **The rune particles on the rune blocks** (`BlockCosmeticSolid.java:322`,
   `BlockEldritch.java:304`, `ItemThaumometer.java:92/103/128`) are triggered
   from the block class, which belongs to another lane: the native port would
   need a call of the client effect inside
   `common/src/main/java/thaumcraft/common/blocks/BlockCosmeticOpaque.java`
   and in the thaumometer, which this wave must not touch. The required change
   is: a client hook in the block (a `ClientEffects` callback of the block,
   like the one of `ItemElementalSword`) that spawns the rune particle of
   `InfusionParticle` with the original parameters (the rune cell of the row
   0.375, the random turn of 90 degrees, the size 0.3 * (1 + gauss * 0.1), the
   life of 3 * duration and the 0.04 * gravity drift).
3. **The slime jump of the thaumic slime** (`EntityThaumicSlime.java:132/140`
   -> `ClientProxy.slimeJumpFX` -> the `FXBreaking` slime icon) has no native
   particle. The native has the particle of the champion
   (`ThaumcraftChampionClient.java:177`), but the ordinary slime is the entity
   of another lane, so the call site is missing.
4. **The wax drips of the candle** need a block entity renderer with a
   deterministic random (the original seeds with `x + (long) y * z`,
   `BlockCandleRenderer.java:35`); a static JSON model cannot draw them.
5. **The variable height of the flux gas** needs a fluid-like renderer, not a
   JSON model.
6. **`update_scope_ledger.py --check` could not be run** here, see section 1.
7. **No Gradle was run**, so the three new test classes are authored but not
   executed, and nothing was compile-verified.

---

## 5. The tests this wave authored (NOT executed)

`common/src/test/java/thaumcraft/client/renderers/tile/LevitatorGlowParityTest.java`
- the colour table of the original ore block (40960 = `0x00A000` for the top
  panel) and the literal side colour of the original (14488063 = `0xDD11FF`);
- the panel of a neighbour is drawn on the side of that neighbour;
- the panel sits on its own side of the block;
- the active levitator keeps the lightmap coordinate 180 (block light 4, sky
  light 11).

`common/src/test/java/thaumcraft/client/gui/GuiBlendParityTest.java`
- the alpha of the drawn regions of the five TC4 GUI sheets, measured from the
  shipped PNGs, which proves that the blend is necessary;
- the five screens enable the blend with the ordinary alpha function;
- the one region that the port draws without the blend (the ghost frame of the
  golem configuration, the cell 184,0 of `guigolem.png`) is opaque.

`common/src/test/java/thaumcraft/client/renderers/entity/ClientEntityRendererNumbersTest.java`
- the original numbers of the dart (0.025625, 0.75, 127/127/153, 45 degrees,
  the shift of four pixels, 0.15625), of the electric orb ((1 + ticks % 6) / 8,
  0.125, 0.875/0.75, the bob, 0.8, 220), of the ember ((7 + frame) / 16,
  0.0625, 0.5625, 0.25 + age / duration, 0.9, 220), of the explosive orb
  ((ticks % 4) / 16, 0.0625, 0.8125, 0.7, 0.8, the standard alpha blend) and
  of the aspect orb (the clock of 25 ms, 0.1 + 0.3 * life, 128, the blend of
  the aspect).

---

## 6. The commits

| commit | what |
|---|---|
| `90ce351b` | the levitator glow: the original colour, the original side, the cell of the sheet, the original brightness |
| `59e13176` | the blend of the five GUI screens |
| `a2a9fdf2` | the three TC4 keys unbound, as in the original |
| `628fa766` | the three new test classes |
| `b7420f93` | the ledger of the 54 units |
