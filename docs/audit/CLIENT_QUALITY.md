# Client code-quality audit — 2026-10-23

**Verdict: not review-ready.** This is a static maintainability audit, not visual acceptance and not a claim that every listed concern is an observed rendering defect.

Baseline `3340abe7`, branch `agent/c4client`. Scope: **336 Java files / 188 renderer files**, plus production registration references in common/Fabric/NeoForge and the asset verifier. Paths in tables are relative to `common/src/main/java/thaumcraft/client/` unless explicitly prefixed. H = high (frame-state/exception isolation), M = medium (ownership, allocation, provenance), L = low (maintainability), I = inventory/explicit contract, not a defect. Counts are static sites, not runtime incident counts; findings across checklist items overlap.

Method: comment/string-masked push/pop inventory, Java 21 compiler-tree parse of every client source, inspection of enclosing scopes, restoration implementations, allocation loop bounds, registration/name references and resource loading. The transform audit checks **review traceability**, not equivalence to a newly read private oracle. Existing citations are evidence pointers, not independently verified parity. No Gradle or game was run.

## 1. PoseStack discipline

**Inspected: 242 pushes and 242 pops in 108 files. Findings: 85 exception-exposure sites at baseline; 5 fixed, 80 left. Zero unmatched lexical pushes/pops and zero identified normal-return bypasses.** Do not misreport that zero as exception safety. Baseline: 82 scopes without finally, two throwing setup gaps before finally protection, and one cleanup-before-pop gap. Another assignment-only setup gap in `RenderEldritchWarden` is harmless and not counted.

Fixed the five scopes in `LegacyBlockQuads.render`, `RenderFrostShard.render`, both `TileArcaneWorkbenchRenderer.render` scopes and `TileTubeBufferRenderer.render`. No successful-path vertex, texture or transform order changed.

### Complete push/pop audit table (post-fix lines)

Each entry is `push → pop`. OK means immediate try/finally; FIX means newly protected; H means no finally; H-gap means a potentially throwing operation before the try or before pop during cleanup. ModelPart internals and native library pushes are outside this source inventory. Restore calls themselves can fail under a broken GL context; no source-level finally can promise recovery from a dead context.

| File | Every push → pop | Result |
|---|---|---|
| `BoreClientEffects.java` | 67 → 76 OK | Protected |
| `ThaumcraftArchitectClient.java` | 95 → 99 OK; 106 → 110 OK; 186 → 213 OK; 191 → 196 OK; 199 → 205 OK; 218 → 225 OK | Protected |
| `ThaumcraftExcavationClient.java` | 170 → 182 OK; 200 → 218 OK | Protected |
| `ThaumcraftGearModelsClient.java` | 157 → 224 OK; 167 → 174 OK; 175 → 204 OK; 182 → 186 OK; 187 → 192 OK; 196 → 200 OK; 215 → 222 OK | Protected |
| `ThaumcraftGogglesClient.java` | 154 → 157 H; 167 → 192 H; 180 → 190 H; 200 → 205 H | H: 4 exposed scopes |
| `ThaumcraftGolemClient.java` | 60 → 66 OK | Protected |
| `ThaumcraftRunicClient.java` | 62 → 67 OK | Protected |
| `ThaumcraftShockClient.java` | 168 → 172 H | H: 1 exposed scopes |
| `ThaumcraftTravelGearClient.java` | 88 → 92 OK | Protected |
| `gui/AdvancedFurnaceBookDiagram.java` | 24 → 50 H | H: 1 exposed scopes |
| `gui/GuiArcaneBore.java` | 37 → 52 OK | Protected |
| `gui/GuiArcaneWorkbench.java` | 45 → 50 H; 61 → 67 H | H: 2 exposed scopes |
| `gui/GuiFocalManipulator.java` | 212 → 224 OK; 262 → 280 OK; 289 → 294 OK | Protected |
| `gui/GuiFocusSelect.java` | 321 → 331 H-gap; 444 → 449 H | H: 2 exposed scopes |
| `gui/GuiGolem.java` | 139 → 144 OK; 161 → 168 OK; 173 → 178 OK; 184 → 189 OK; 199 → 204 OK; 207 → 215 OK; 224 → 229 OK | Protected |
| `gui/GuiResearchPopup.java` | 24 → 56 H-gap (disableBlend before pop); 41 → 43 OK | H: 1 exposed scopes |
| `gui/GuiResearchTable.java` | 79 → 82 H; 266 → 276 H; 269 → 273 H; 281 → 285 H; 553 → 557 H; 582 → 593 H | H: 6 exposed scopes |
| `gui/GuiThaumatorium.java` | 33 → 35 H | H: 1 exposed scopes |
| `gui/GuiTravelingTrunk.java` | 35 → 39 OK | Protected |
| `gui/HoveredSlotAspectRow.java` | 102 → 128 H-gap; 172 → 178 OK; 205 → 222 OK | H: 1 exposed scopes |
| `gui/RunicAugmentationDisplay.java` | 32 → 83 H; 74 → 79 H; 100 → 104 H | H: 3 exposed scopes |
| `gui/ThaumatoriumBookDiagram.java` | 23 → 42 H | H: 1 exposed scopes |
| `gui/ThaumcraftRecipeDisplay.java` | 704 → 752 H; 776 → 809 H; 827 → 844 H; 855 → 891 H; 903 → 956 H; 947 → 952 H; 975 → 1018 H; 1009 → 1014 H; 1036 → 1078 H; 1069 → 1074 H; 1089 → 1093 H | H: 11 exposed scopes |
| `lib/PlayerNotifications.java` | 117 → 122 OK; 124 → 129 OK; 134 → 141 OK; 147 → 152 OK | Protected |
| `lib/ScanOverlay.java` | 124 → 135 OK; 153 → 164 OK | Protected |
| `renderers/VisNetRender.java` | 87 → 87 OK; 92 → 92 OK; 109 → 113 OK | Protected |
| `renderers/block/LegacyBlockQuads.java` | 94 → 106 FIX | Protected |
| `renderers/entity/RenderAspectOrb.java` | 20 → 29 OK | Protected |
| `renderers/entity/RenderCultist.java` | 54 → 67 OK | Protected |
| `renderers/entity/RenderCultistPortal.java` | 69 → 93 OK | Protected |
| `renderers/entity/RenderDart.java` | 29 → 58 OK | Protected |
| `renderers/entity/RenderEldritchOrb.java` | 21 → 43 OK; 44 → 51 OK | Protected |
| `renderers/entity/RenderEldritchWarden.java` | 77 → 106 OK; 96 → 103 OK (assignment before try) | Protected |
| `renderers/entity/RenderElectricOrb.java` | 38 → 54 OK | Protected |
| `renderers/entity/RenderEmber.java` | 39 → 46 H | H: 1 exposed scopes |
| `renderers/entity/RenderExplosiveOrb.java` | 33 → 47 OK | Protected |
| `renderers/entity/RenderFallingTaint.java` | 20 → 20 OK | Protected |
| `renderers/entity/RenderFocusOrb.java` | 28 → 34 H | H: 1 exposed scopes |
| `renderers/entity/RenderFrostShard.java` | 41 → 56 FIX | Protected |
| `renderers/entity/RenderGolemBase.java` | 114 → 125 OK; 153 → 162 OK; 171 → 182 OK; 230 → 243 OK; 266 → 272 OK; 287 → 314 OK | Protected |
| `renderers/entity/RenderGolemBobber.java` | 43 → 56 OK | Protected |
| `renderers/entity/RenderPech.java` | 28 → 39 OK; 45 → 45 OK | Protected |
| `renderers/entity/RenderPrimalArrow.java` | 42 → 73 OK; 81 → 95 OK | Protected |
| `renderers/entity/RenderPrimalOrb.java` | 26 → 43 OK; 45 → 55 OK | Protected |
| `renderers/entity/RenderSpecialItem.java` | 48 → 54 OK; 64 → 81 OK | Protected |
| `renderers/entity/RenderThaumicSlime.java` | 46 → 53 OK | Protected |
| `renderers/entity/RenderTravelingTrunk.java` | 76 → 87 OK | Protected |
| `renderers/entity/RenderWatcher.java` | 89 → 113 OK | Protected |
| `renderers/entity/RenderWisp.java` | 24 → 32 OK | Protected |
| `renderers/item/ItemBannerRenderer.java` | 66 → 85 OK | Protected |
| `renderers/item/ItemThaumometerRenderer.java` | 59 → 98 OK; 83 → 95 OK; 133 → 151 OK; 138 → 147 OK; 215 → 228 OK | Protected |
| `renderers/item/PrimalWandFocusRenderer.java` | 37 → 42 OK | Protected |
| `renderers/item/ThaumometerReadout.java` | 107 → 141 OK; 116 → 123 OK; 129 → 138 OK; 158 → 189 OK; 180 → 186 OK | Protected |
| `renderers/models/ModelGolem.java` | 146 → 157 OK | Protected |
| `renderers/models/ModelTaintSporeSwarmer.java` | 23 → 23 OK | Protected |
| `renderers/models/ModelTunnelField.java` | 97 → 104 OK; 115 → 120 OK | Protected |
| `renderers/models/entities/ModelGolemAccessories.java` | 176 → 180 OK | Protected |
| `renderers/models/entities/ModelTaintSeed.java` | 87 → 95 OK | Protected |
| `renderers/models/entities/ModelTaintacle.java` | 49 → 51 OK | Protected |
| `renderers/models/gear/HarnessArmorRenderer.java` | 53 → 56 OK; 57 → 68 OK | Protected |
| `renderers/tile/ItemJarNodeRenderer.java` | 44 → 52 H | H: 1 exposed scopes |
| `renderers/tile/ItemNodeRenderer.java` | 70 → 90 OK; 78 → 89 OK | Protected |
| `renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java` | 31 → 56 OK; 36 → 36 H; 38 → 38 H; 41 → 42 H; 43 → 44 H; 48 → 54 H; 50 → 52 H | H: 6 exposed scopes |
| `renderers/tile/TileAlembicRenderer.java` | 36 → 64 H; 38 → 55 H | H: 2 exposed scopes |
| `renderers/tile/TileArcaneBoreBaseRenderer.java` | 30 → 38 OK | Protected |
| `renderers/tile/TileArcaneBoreRenderer.java` | 41 → 72 OK; 46 → 51 OK; 53 → 57 OK; 58 → 62 OK; 66 → 70 OK | Protected |
| `renderers/tile/TileArcaneLampRenderer.java` | 21 → 26 OK; 28 → 33 OK | Protected |
| `renderers/tile/TileArcaneWorkbenchRenderer.java` | 24 → 30 FIX; 36 → 43 FIX | Protected |
| `renderers/tile/TileBannerRenderer.java` | 75 → 107 OK; 120 → 140 OK | Protected |
| `renderers/tile/TileBellowsRenderer.java` | 22 → 38 H; 30 → 34 H; 35 → 35 H; 36 → 36 H | H: 4 exposed scopes |
| `renderers/tile/TileBrainboxRenderer.java` | 37 → 48 OK | Protected |
| `renderers/tile/TileCentrifugeRenderer.java` | 56 → 63 H | H: 1 exposed scopes |
| `renderers/tile/TileChestHungryRenderer.java` | 65 → 76 OK | Protected |
| `renderers/tile/TileCrucibleRenderer.java` | 60 → 69 H | H: 1 exposed scopes |
| `renderers/tile/TileCrystalRenderer.java` | 44 → 47 OK; 53 → 65 OK | Protected |
| `renderers/tile/TileDeconstructionTableRenderer.java` | 26 → 27 H; 29 → 30 H; 33 → 34 H; 37 → 43 H | H: 4 exposed scopes |
| `renderers/tile/TileEldritchCapRenderer.java` | 51 → 58 OK; 70 → 86 OK; 74 → 82 OK | Protected |
| `renderers/tile/TileEldritchCrabSpawnerRenderer.java` | 42 → 48 OK | Protected |
| `renderers/tile/TileEldritchCrystalRenderer.java` | 41 → 51 OK | Protected |
| `renderers/tile/TileEldritchLockRenderer.java` | 45 → 71 OK; 49 → 67 OK; 55 → 63 OK; 76 → 86 OK; 90 → 105 OK | Protected |
| `renderers/tile/TileEldritchNothingRenderer.java` | 48 → 53 OK | Protected |
| `renderers/tile/TileEldritchObeliskRenderer.java` | 71 → 80 OK; 84 → 98 OK; 88 → 94 OK; 102 → 108 OK; 110 → 116 OK | Protected |
| `renderers/tile/TileEldritchPortalRenderer.java` | 81 → 94 OK | Protected |
| `renderers/tile/TileEssentiaCrystalizerRenderer.java` | 33 → 41 H; 37 → 40 H | H: 2 exposed scopes |
| `renderers/tile/TileEssentiaLampRenderer.java` | 21 → 23 OK; 25 → 27 OK | Protected |
| `renderers/tile/TileEtherealBloomRenderer.java` | 63 → 80 OK; 83 → 89 OK; 96 → 111 OK; 101 → 107 OK; 114 → 130 OK; 120 → 126 OK; 133 → 149 OK; 138 → 145 OK | Protected |
| `renderers/tile/TileFluxScrubberRenderer.java` | 38 → 48 OK | Protected |
| `renderers/tile/TileFocalManipulatorRenderer.java` | 29 → 34 OK; 43 → 49 OK | Protected |
| `renderers/tile/TileHoleRenderer.java` | 42 → 47 OK | Protected |
| `renderers/tile/TileInfusionMatrixRenderer.java` | 39 → 71 H; 49 → 67 H; 86 → 103 H | H: 3 exposed scopes |
| `renderers/tile/TileInfusionPillarRenderer.java` | 40 → 45 H | H: 1 exposed scopes |
| `renderers/tile/TileJarBrainRenderer.java` | 52 → 60 H; 66 → 71 H | H: 2 exposed scopes |
| `renderers/tile/TileJarRenderer.java` | 33 → 43 H; 67 → 75 H | H: 2 exposed scopes |
| `renderers/tile/TileMagicWorkbenchChargerRenderer.java` | 76 → 92 OK; 80 → 84 OK | Protected |
| `renderers/tile/TileManaPodRenderer.java` | 32 → 42 OK; 37 → 37 OK | Protected |
| `renderers/tile/TileMirrorRenderer.java` | 53 → 57 OK; 72 → 107 OK | Protected |
| `renderers/tile/TileNodeConverterRenderer.java` | 28 → 30 OK; 29 → 29 OK | Protected |
| `renderers/tile/TileNodeEnergizedRenderer.java` | 26 → 33 OK | Protected |
| `renderers/tile/TileNodeRenderer.java` | 61 → 65 H; 141 → 146 H; 155 → 159 H; 197 → 236 H; 214 → 218 H | H: 5 exposed scopes |
| `renderers/tile/TileNodeStabilizerRenderer.java` | 48 → 50 OK; 49 → 49 OK; 57 → 61 OK | Protected |
| `renderers/tile/TilePedestalRenderer.java` | 29 → 38 H | H: 1 exposed scopes |
| `renderers/tile/TileResearchTableRenderer.java` | 29 → 68 H; 39 → 46 H; 49 → 56 H; 63 → 66 H | H: 4 exposed scopes |
| `renderers/tile/TileTableRenderer.java` | 23 → 28 H | H: 1 exposed scopes |
| `renderers/tile/TileThaumatoriumRenderer.java` | 23 → 28 H; 34 → 58 H | H: 2 exposed scopes |
| `renderers/tile/TileTubeBufferRenderer.java` | 73 → 82 FIX | Protected |
| `renderers/tile/TileTubeRenderer.java` | 138 → 152 H; 156 → 169 H | H: 2 exposed scopes |
| `renderers/tile/TileVisRelayRenderer.java` | 47 → 50 OK; 48 → 48 OK | Protected |
| `renderers/tile/TileWandPedestalRenderer.java` | 44 → 52 OK | Protected |

`gui/GuiFocusSelect.java:321-331` starts protection only after transform/blend/colour setup and runs `setColor` before pop. `gui/HoveredSlotAspectRow.java:102-129` starts protection after depth/transform setup and calls `flush` before pop. `gui/GuiResearchPopup.java:54-56` calls `disableBlend` before pop. These are not equivalent to an immediate, cleanup-isolated finally. Protect setup too; nest flush/colour cleanup inside a finally whose own finally pops. The remaining H rows require the same mechanical isolation, preferably family-by-family with focused tests rather than a bulk formatter rewrite.

## 2. Render-state discipline

**Inspected: 374 mutating `RenderSystem` call sites in 45 files**, including restore calls. **38 file-level state-ownership findings**, four snapshot-owning implementations, two RenderType-managed implementations and one documented flat-GUI postcondition. These are not 374 independent leaks. No runtime state-leak count is available. Snapshot restoration and resetting to white/default blending/depth-write=true are deliberately distinguished.

The following table lists **every explicit mutating call**, including restore branches, so the count can be checked. `name@line` may repeat at the same line. Native RenderType setup/clear callbacks reset to engine conventions; that is a lifecycle contract, not preservation of arbitrary incoming GL state. Direct particle and GUI methods should not silently rely on that contract. Indirect `graphics.setColor` calls also matter: its restoration is discussed below even though it is not included in the 374 direct-call total.

| File | All mutating calls (change and restore) | Restore and review result |
|---|---|---|
| `BoreClientEffects.java` | setShaderColor@240; enableDepthTest@240; depthFunc@240; disableCull@240; depthMask@241; disableBlend@242; enableBlend@243; blendFunc@243; setShader@264; setShaderTexture@265; setShaderColor@266; blendFuncSeparate@267; enableBlend@268; disableBlend@268; depthMask@269; depthFunc@269; enableDepthTest@270; disableDepthTest@270; enableCull@271; disableCull@271 | I: NativeState captures shader, textures 0–2, colour, blend factors/enable, depth func/test/write, cull; setup failure and draw finally restore (256–271). |
| `CrystallizerClientEffects.java` | setShader@49; setShaderTexture@49; enableBlend@49; defaultBlendFunc@49; depthMask@49; setShader@49; setShaderTexture@49; defaultBlendFunc@49; disableBlend@49; depthMask@49 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `ThaumcraftWarpClient.java` | enableBlend@112; blendFuncSeparate@113; disableDepthTest@115; depthMask@115; depthMask@117; enableDepthTest@117; defaultBlendFunc@118; disableBlend@118; setShaderFogStart@123; setShaderFogEnd@123; setShaderFogShape@123 | H: overlay 112–118 restores hardcoded depth-write/test/blend/white only on success, no finally. Fog 123 intentionally sets pipeline output; it must NOT be immediately undone like a local helper. |
| `fx/FloatyLine.java` | depthMask@226 | I: setup depthMask at 226 is cleared through COLOR_WRITE.clearRenderState at 230. Engine default-write lifecycle, not arbitrary incoming snapshot; setup failure has no local unwind. |
| `fx/particles/BlockRuneParticle.java` | setShader@119; setShaderTexture@120; enableBlend@121; blendFunc@122; disableCull@123; depthMask@124; setShader@130; setShaderTexture@131; defaultBlendFunc@132; disableBlend@133; enableCull@134; depthMask@135 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values; cull reset to enabled. |
| `fx/particles/BlockSparkleParticle.java` | setShader@135; setShaderTexture@136; enableBlend@137; blendFunc@139; disableCull@140; depthMask@141; setShader@149; setShaderTexture@150; defaultBlendFunc@151; disableBlend@152; enableCull@153; depthMask@154 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values; cull reset to enabled. |
| `fx/particles/BoreDigSparkleParticle.java` | setShader@163; setShaderTexture@164; enableBlend@165; blendFunc@166; depthMask@167; setShader@172; setShaderTexture@173; defaultBlendFunc@174; disableBlend@175; depthMask@176 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/CrystalSparkParticle.java` | setShader@51; setShaderTexture@51; enableBlend@52; blendFunc@52; depthMask@52; setShader@54; setShaderTexture@54; defaultBlendFunc@54; disableBlend@54; depthMask@54 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/EssentiaTrailParticle.java` | setShader@130; setShaderTexture@131; enableBlend@132; blendFunc@133; disableCull@134; depthMask@135; setShader@141; setShaderTexture@142; defaultBlendFunc@143; disableBlend@144; enableCull@145; depthMask@146 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values; cull reset to enabled. |
| `fx/particles/FXArcSparkle.java` | setShader@75; setShaderTexture@76; enableBlend@77; blendFunc@78; depthMask@79; setShader@84; setShaderTexture@85; defaultBlendFunc@86; disableBlend@87; depthMask@88 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/FXSmokeSpiral.java` | setShader@131; setShaderTexture@132; enableBlend@133; blendFunc@134; depthMask@135; setShader@144; setShaderTexture@145; defaultBlendFunc@146; disableBlend@147; depthMask@148 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/FXVent.java` | setShader@92; setShaderTexture@93; enableBlend@94; defaultBlendFunc@95; depthMask@96; setShader@102; setShaderTexture@103; defaultBlendFunc@104; disableBlend@105; depthMask@106 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/InfusionParticle.java` | setShader@281; setShaderTexture@282; enableBlend@283; blendFunc@284; disableCull@286; depthMask@287; setShader@297; setShaderTexture@298; defaultBlendFunc@299; disableBlend@300; enableCull@301; depthMask@302 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values; cull reset to enabled. |
| `fx/particles/ResourceParticle.java` | setShader@179; setShaderTexture@180; enableBlend@181; blendFunc@182; depthMask@184; setShader@189; setShaderTexture@190; defaultBlendFunc@191; disableBlend@192; depthMask@193 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/SlurryBubbleParticle.java` | setShader@50; setShaderTexture@50; setShaderColor@50; enableBlend@51; defaultBlendFunc@51; enableDepthTest@51; depthFunc@51; depthMask@51; disableCull@51; setShader@54; setShaderTexture@54; setShaderColor@54; blendFuncSeparate@55; enableBlend@55; disableBlend@55; enableDepthTest@56; disableDepthTest@56; depthFunc@56; depthMask@56; enableCull@56; disableCull@56 | I: captures all explicitly changed state and restores saved values in finally (46–56). |
| `fx/particles/WarpGuardianParticle.java` | setShader@69; setShaderTexture@69; enableBlend@70; blendFunc@70; depthMask@71; setShader@75; setShaderTexture@75; defaultBlendFunc@76; disableBlend@76; depthMask@76 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/WarpOrbParticle.java` | setShader@62; setShaderTexture@62; enableBlend@63; blendFunc@63; depthMask@64; setShader@68; setShaderTexture@68; defaultBlendFunc@69; disableBlend@69; depthMask@69 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values. |
| `fx/particles/WarpSonicParticle.java` | setShader@79; setShaderTexture@80; enableBlend@81; blendFunc@81; disableCull@82; depthMask@82; setShader@88; setShaderTexture@88; defaultBlendFunc@89; disableBlend@89; enableCull@89; depthMask@89 | M: shader and texture 0 saved/restored in finally; blend factors/enable and depth write reset to default/off/true, not incoming values; cull reset to enabled. |
| `gui/AdvancedFurnaceBookDiagram.java` | enableBlend@44; disableBlend@45 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiAlchemyFurnace.java` | enableBlend@23; defaultBlendFunc@24; disableBlend@35 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiArcaneBore.java` | enableBlend@28; defaultBlendFunc@29; disableBlend@35 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiArcaneWorkbench.java` | enableBlend@30; disableBlend@52 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiDeconstructionTable.java` | enableBlend@22; disableBlend@33 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiFocalManipulator.java` | enableBlend@179; defaultBlendFunc@180; defaultBlendFunc@202; disableBlend@203; enableBlend@260; blendFunc@261; defaultBlendFunc@282; disableBlend@283 | M: finally blocks restore default blending/off and white, not incoming blend factors/enable/colour; additive halo setup also precedes protected drawing. |
| `gui/GuiFocusPouch.java` | enableBlend@22; defaultBlendFunc@22; disableBlend@24; enableBlend@28; defaultBlendFunc@28; disableBlend@30 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiFocusSelect.java` | enableBlend@324; defaultBlendFunc@325; disableBlend@332 | M: default/off/white reset, not incoming state; setup and cleanup-before-pop are not fully protected. |
| `gui/GuiGolem.java` | enableBlend@48; defaultBlendFunc@49; disableBlend@104 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiHandMirror.java` | enableBlend@64; defaultBlendFunc@65; disableBlend@69 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiHoverHarness.java` | enableBlend@21; defaultBlendFunc@21; disableBlend@23; enableBlend@27; defaultBlendFunc@27; disableBlend@29 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiPech.java` | enableBlend@20; defaultBlendFunc@20; disableBlend@21 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiResearchBrowser.java` | disableBlend@166; enableBlend@324; defaultBlendFunc@325 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiResearchPopup.java` | enableBlend@34; defaultBlendFunc@35; enableBlend@50; defaultBlendFunc@51; disableBlend@55; enableDepthTest@57; disableDepthTest@57 | M: shader colour and depth captured/restored (23/56–57); blend enable/factors are reset, not captured, and disableBlend precedes pop. |
| `gui/GuiResearchRecipe.java` | enableBlend@108; defaultBlendFunc@109; enableBlend@129; defaultBlendFunc@130; disableBlend@140 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiResearchTable.java` | enableBlend@586; blendFunc@587; defaultBlendFunc@591; disableBlend@592; enableBlend@608; defaultBlendFunc@609; disableBlend@614 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiSpa.java` | enableBlend@37; defaultBlendFunc@37; disableBlend@51; setShader@57; setShaderTexture@57 | H: background resets blend/colour only on success (37–51); fluid helper changes shader/texture at 57 without saving/restoring either. |
| `gui/GuiThaumatorium.java` | enableBlend@29; disableBlend@32 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/GuiTravelingTrunk.java` | enableBlend@44; defaultBlendFunc@45; disableBlend@54 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/HoveredSlotAspectRow.java` | disableDepthTest@106; enableDepthTest@129; disableDepthTest@129; enableBlend@235; defaultBlendFunc@236; disableBlend@241 | M: depth is captured/restored (101/129); blend factors and enable in icon helper reset to defaults/off (235–241), colour to white; setup and cleanup gaps also appear in item 1. |
| `gui/ResearchIconDisplay.java` | disableDepthTest@53 | I: 53 intentionally establishes flat-GUI depth-off postcondition after item/decoration flush. Caller must own final depth restoration. Not labelled an accidental unpaired toggle. |
| `gui/RunicAugmentationDisplay.java` | enableBlend@39; disableBlend@42; enableBlend@69; disableBlend@73 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/ThaumatoriumBookDiagram.java` | enableBlend@36; disableBlend@37 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `gui/ThaumcraftRecipeDisplay.java` | enableBlend@707; disableBlend@710; enableBlend@738; disableBlend@750; enableBlend@779; disableBlend@782; enableBlend@796; disableBlend@808; enableBlend@830; disableBlend@833; enableBlend@882; disableBlend@886; enableBlend@910; disableBlend@913; enableBlend@942; disableBlend@946; enableBlend@985; disableBlend@987; enableBlend@1004; disableBlend@1008; enableBlend@1041; disableBlend@1045; enableBlend@1063; disableBlend@1068 | M: blend enable/factors reset to defaults/off rather than captured; some paths restore only on normal completion. White-colour resets, where present, are not incoming-colour restoration. |
| `renderers/VisNetRender.java` | setShader@72; setShaderTexture@72; setShaderColor@72; enableBlend@72; blendFunc@72; enableDepthTest@72; disableDepthTest@72; depthFunc@72; depthMask@72; enableCull@72; disableCull@72; setShader@123; setShaderTexture@123; setShaderColor@123; blendFuncSeparate@123; enableBlend@123; disableBlend@123; depthMask@123; depthFunc@123; enableDepthTest@123; disableDepthTest@123; enableCull@123; disableCull@123; setShaderColor@129; enableDepthTest@129; depthFunc@129; depthMask@129; disableCull@129; enableBlend@129; defaultBlendFunc@129; disableBlend@129 | I: NativeState + try-with-resources; setup failure unwinds. Capture 120–122, restore 123, draws 71–74/80–83 and model 128–130. |
| `renderers/item/BoneBowGlint.java` | enableBlend@25; blendFunc@26; disableBlend@27; defaultBlendFunc@27; setShaderColor@35; setShaderColor@41 | I: blend setup/clear are paired shard callbacks (25–27); shader colour is saved/restored (34–41). Blend defaults are a RenderType contract; do not use as a standalone saved-state helper. |
| `renderers/tile/TileNodeStabilizerRenderer.java` | setShader@57; setShaderTexture@57; setShaderColor@57; enableBlend@57; blendFunc@57; enableDepthTest@57; depthFunc@57; depthMask@57; disableCull@57; setShader@61; setShaderTexture@61; setShaderColor@61; blendFuncSeparate@61; enableBlend@61; disableBlend@61; depthMask@61; depthFunc@61; enableDepthTest@61; disableDepthTest@61; enableCull@61; disableCull@61 | I: immediate bubble captures changed state at 56, restores in finally 61; model RenderType uses engine setup/clear contract (74–75). |

Important distinction: the particle finally blocks generally **do** re-enable depth writing; the finding is failure to restore the caller's prior false/true value and blend/cull state, not evidence that they leave depth writing permanently off. `PlayerNotifications.renderHud`, `ThaumcraftRunicClient.renderHud` and `GuiResearchBrowser` capture shader colour for indirect `GuiGraphics.setColor`; that is better than the white-only convention, but does not prove their callees preserve all render state. Do not replace all GUI resets with GL reads without considering queued GuiGraphics draws and flush ownership. No render-state behavior was changed in this lane.

## 3. Allocation on frame paths

**Inspected: all 336 files / 2,256 declared methods and constructors; 1,053 `new` class nodes, 2,030 array nodes (including nested initializers), 307 lambdas.** Those are syntax totals, NOT frame counts. Constructors, model baking and OBJ parsing were separated from actual draws. **20 prioritized frame-allocation findings** follow. Source-level object counts are before escape analysis; capturing lambdas are potential closure allocations, not guaranteed heap allocations. Zero-argument `List.of()` is shared, and is not counted as an allocation. Component factories, enum `values()` clones, iterator/buffer internals, Axis quaternion allocations and native PoseStack internals are excluded unless explicitly called out. Thus numbers are lower bounds, not a profiler result.

Frame count is `draw invocations in the frame × row formula`; the visible population and active faces are not constant. These are the worst recurring geometry hot spots found by source review, not a claim of globally ranked measured GC cost.

| # | Source | Explicit objects/arrays per active draw (hence per frame per visible instance) | Severity / review action |
|---|---|---|---|
| 1 | `renderers/tile/LinkedMirrorField.java:32-68`, `MirrorProjection.java:18`, `renderers/VisNetRender.java:119-123` | 151 local objects/arrays + 16 texture matrices + 48 NativeState objects/arrays = **215**, plus up to 48 capturing closures. 16 layers, nine temporary arrays per layer. | M: eliminate per-layer corner/vector arrays; preserve layer RNG order and projective algebra. |
| 2 | `renderers/models/ModelTunnelField.java:83-102,163-164` | **160 arrays per near face** (16 × two five-array 2D tables); up to 960 for six faces. | M: emit scalar corners/UVs; do not unify away deliberate far/near behavior. |
| 3 | `renderers/tile/TileTubeRenderer.java:65-98,238-260` | **3 + 32(a+c)** arrays; a=0..3 connected axes, c=1 ordinary core or 2 filter passes: **35..163**, before valve/one-way extras. | M: six five-array face tables + two bounds per box; scalar or baked bounded geometry. |
| 4 | `renderers/tile/TileTubeBufferRenderer.java:48-69,107-118` | **3 + 32(a+1) = 35..131** arrays, a=0..3. | M: same duplicated array-heavy box builder as tubes. |
| 5 | `renderers/entity/RenderPrimalOrb.java:28-53` | **89** local objects/arrays: Random 1 + 12 × (4 end arrays + 3 origin arrays) + 4 Vec3; +3 state objects/arrays and up to 3 closures for billboard. | M: stack/scalar ray endpoints; preserve seeded sequence. |
| 6 | `renderers/tile/TileWardedGlassRenderer.java:49-53,109-119` | **72** arrays/ResourceLocations: six × (mask 1 + corners 5 + UV 5 + parsed ID 1), plus neighbor BlockPos factory results. | M: preparse the 47 IDs; do not cache neighbor masks across world changes. |
| 7 | `renderers/tile/TileWardedRenderer.java:48-52,64-71` | **13f** objects/arrays, f=visible faces 0..6: ten face arrays + three NativeState objects/arrays per face; up to **78**, plus 3f closures. | M: duplicated face/UV builder; transaction granularity should be documented. |
| 8 | `ThaumcraftArchitectClient.java:133-134,229-238` | **11f**, f=drawn overlay faces: ten face arrays + ResourceLocation; up to **66 per six-face overlay block**. | M: shares warded-face boilerplate but different winding/UV policy. |
| 9 | `renderers/tile/TileBrainboxRenderer.java:55-69` | **60 arrays per six-face box** (10 per face). | M: emit vertices without allocating corner/UV matrices. |
| 10 | `renderers/entity/RenderEldritchOrb.java:23-34` | **49**: one Random + twelve four-array end tables. | M: scalar ray emission; preserve six rotations per fan and seeded Random semantics. |
| 11 | `renderers/item/PrimalWandFocusRenderer.java:36,47-52,70` | **31**: six five-array face tables plus one Depth record. | M: two immutable depth presets, scalar face emission. |
| 12 | `renderers/tile/TileResearchTableRenderer.java:48-55` | **30 arrays** for six parchment quads, regardless of tools/note presence. | M: immutable unit-quad data rather than recreated arrays. |
| 13 | `renderers/entity/RenderSpecialItem.java:60-75` | **21**: one Random + five four-array endpoint tables per star. | M: preserve reset seed 245, avoid temporary nested arrays. |
| 14 | `renderers/VisNetRender.java:100-102` | About **20** objects/arrays on a nondegenerate beam path: nine vector-building operations, eight endpoint offsets and three saved-state objects/arrays; up to three closures. | M: scalar vector arithmetic; transitive vector counts depend on normalize fast paths. |
| 15 | `renderers/entity/RenderGolemBobber.java:71-74` | **17 explicit new Vec3**, plus up to 17 normalize results when rope drawn. | M: no need to create a tangent vector per vertex just to obtain three normalized scalars. |
| 16 | `gui/AdvancedFurnaceBookDiagram.java:30-42` | **17 ItemStacks** for fixed formation cells + nonempty tag-list storage and hover objects/components. | M: precompute immutable display templates, never reuse mutable inventory stacks. |
| 17 | `renderers/tile/TileBannerRenderer.java:131-132` | **10 arrays** per displayed aspect tag. | M: fixed icon corner/UV data should not be rebuilt. |
| 18 | `renderers/tile/TileLevitatorRenderer.java:137-155` | **2f arrays**, f=0..5 panels; up to **10**. | M: two selected flat arrays per panel, not all switch branches. |
| 19 | `renderers/VisNetRender.java:91-95,119-123` | **7 objects/arrays per facing quad**: four Vec3 + state object + texture/color arrays; up to three closures. | M: scalar quad helper; avoid allocating a vector only to unpack it. |
| 20 | `gui/GuiResearchTable.java:525-538` | **3 + h** objects per drawRunes call (Pixel, Rune, ArrayList, h Hex objects for traversed neighbors), before string/component factories. | M: avoid transient key wrappers during every puzzle-cell draw. |

Not per-frame and therefore not dishonestly included above: `renderers/block/GasWorldQuads.java:20-88` and `renderers/fluid/PureFluidRender.java:91-140` allocate heavily during chunk mesh rebuild, while model constructors/OBJ parsers allocate on load/reload. `PureFluidRender` even creates `new float[4][2]` then replaces all four child arrays; `LegacyBlockQuads.java:102-103` allocates two arrays per emitted quad when its render hook is invoked. These are additional lifecycle-specific optimization candidates, not measured frame hotspots. Changing seeded Random to a different PRNG would change the drawing and is not an acceptable cheap optimization.

## 4. Class size and duplication

**Inspected: 336 file sizes / all 188 renderer files. 22 files exceed 250 lines: 6 inside renderers, 16 elsewhere. Findings: 20 size-review candidates plus 5 duplicated-family concerns = 25.** Two large generated mesh tables are explicit data exceptions, not recommendations to hand-split coordinates. Size alone is a reviewability warning, not a correctness failure.

| File | Lines | Severity / role |
|---|---:|---|
| `BoreClientEffects.java:1` | 274 | L: review decomposition by lifecycle/responsibility |
| `ClientSetup.java:1` | 263 | L: review decomposition by lifecycle/responsibility |
| `ThaumcraftArchitectClient.java:1` | 251 | L: review decomposition by lifecycle/responsibility |
| `ThaumcraftExcavationClient.java:1` | 317 | L: review decomposition by lifecycle/responsibility |
| `ThaumcraftGearModelsClient.java:1` | 460 | L: review decomposition by lifecycle/responsibility |
| `ThaumcraftGogglesClient.java:1` | 273 | L: review decomposition by lifecycle/responsibility |
| `fx/InfusionClientEffects.java:1` | 362 | L: review decomposition by lifecycle/responsibility |
| `fx/bolt/WispLightningShape.java:1` | 357 | L: review decomposition by lifecycle/responsibility |
| `fx/particles/InfusionParticle.java:1` | 322 | L: review decomposition by lifecycle/responsibility |
| `gui/GuiFocalManipulator.java:1` | 347 | L: review decomposition by lifecycle/responsibility |
| `gui/GuiFocusSelect.java:1` | 464 | L: review decomposition by lifecycle/responsibility |
| `gui/GuiGolem.java:1` | 329 | L: review decomposition by lifecycle/responsibility |
| `gui/GuiResearchBrowser.java:1` | 507 | L: review decomposition by lifecycle/responsibility |
| `gui/GuiResearchTable.java:1` | 617 | L: review decomposition by lifecycle/responsibility |
| `gui/ResearchIconDisplay.java:1` | 356 | L: review decomposition by lifecycle/responsibility |
| `gui/ThaumcraftRecipeDisplay.java:1` | 1131 | L: review decomposition by lifecycle/responsibility |
| `renderers/entity/RenderGolemBase.java:1` | 365 | L: review decomposition by lifecycle/responsibility |
| `renderers/entity/ThaumcraftEntityRenderers.java:1` | 353 | L: review decomposition by lifecycle/responsibility |
| `renderers/models/ModelCrabVent.java:1` | 893 | I: generated mesh table; keep provenance/generator, not manual method splitting |
| `renderers/models/ModelEldritchCrystal.java:1` | 563 | I: generated mesh table; keep provenance/generator, not manual method splitting |
| `renderers/tile/TileNodeRenderer.java:1` | 296 | L: review decomposition by lifecycle/responsibility |
| `renderers/tile/TileTubeRenderer.java:1` | 284 | L: review decomposition by lifecycle/responsibility |

Five concrete duplication findings (not a mandate to homogenize original quirks):

1. **M:** `renderers/tile/TileTubeRenderer.java:238-283` vs `TileTubeBufferRenderer.java:107-132`: same six-face array construction, sprite-relative coordinates and winding with tint/signature drift. Factor a scalar emitter only after retaining each face policy.
2. **M:** `ThaumcraftArchitectClient.java:229-251`, `renderers/tile/TileWardedRenderer.java:60-76` and `TileWardedGlassRenderer.java:105-127`: three unit-cube face builders with intentionally/non-obviously different UV ordering. Common geometry data could carry an explicit UV policy instead of another copied switch.
3. **M:** `renderers/VisNetRender.java:119-123`, `BoreClientEffects.java:255-271`, `fx/particles/SlurryBubbleParticle.java:46-56` and `renderers/tile/TileNodeStabilizerRenderer.java:56-61`: repeated GL snapshots, while many particles use weaker default resets. A draw-time state transaction needs one clearly owned implementation, not independent partial copies.
4. **L:** `renderers/block/LegacyBlockQuads.java:31-34`, `renderers/tile/TileTubeRenderer.java:229-230`, `TileTubeBufferRenderer.java:108`, `TileBrainboxRenderer.java:34-35`: repeated atlas lookup/path construction. Hoist identifiers, not TextureAtlasSprite objects across reload without invalidation.
5. **L:** `renderers/entity/RenderPrimalOrb.java:22`, `renderers/tile/TileNodeStabilizerRenderer.java:49,60`, `TileNodeConverterRenderer.java:27-29`, `TileTubeBufferRenderer.java:80`: repeated primal palettes, white/full-alpha and device-status RGB literals. Share semantic original palettes only; similarly colored but intentionally distinct 0xFF3333/0xFF4444 or rounded 8-bit tints must not be merged by visual guesswork.

## 5. Transform chains and oracle traceability

**Inspected: 850 lexical transform-like calls; 767 direct PoseStack translate/mulPose/scale operations in 199 method/constructor scopes.** The other 83 are vector/matrix/animation math or non-PoseStack operations, not silently counted as stack transforms. **113 scopes lack a local file:line-shaped oracle anchor (M).** An anchor within the method or its directly attached comment is a trace candidate, not proof that every branch/order is documented. A class-level oracle range is useful but does not establish a particular helper's multiplication order. Those citations were not revalidated against a newly opened private oracle in this lane.

Table contains every direct operation in source order (`T` translate, `R` mulPose, `S` scale); same-line entries are separate operations. A method can contain branches/nested scopes, so this is an operation ledger, not a claim that all its entries execute sequentially. M rows are actionable provenance findings, not assertions that the drawing is wrong.

| Scope (file:declaration line) | Every operation @ line, source order | Local anchor / result |
|---|---|---|
| `BoreClientEffects.java:66` `vortex` | T@69, R@69, R@70, R@70, S@70 | I: trace candidate Vec3 |
| `FocalManipulatorClientEffects.java:67` `render` | T@69, R@72 | I: trace candidate GL11 |
| `ThaumcraftArchitectClient.java:217` `arrow` | S@220 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `ThaumcraftArchitectClient.java:179` `drawArchitectAxis` | R@189, R@194, R@201, R@203, R@208, R@210 | I: trace candidate REHWandHandler:429-479, REHWandHandler:438-440, REHWandHandler:450 |
| `ThaumcraftArchitectClient.java:72` `render` | T@97, T@108 | I: trace candidate REHWandHandler:298-306, REHWandHandler:305, REHWandHandler:310 |
| `ThaumcraftExcavationClient.java:196` `drawBeam` | R@204, R@207, R@209, R@212 | I: trace candidate FXBeamWand:201-206, FXBeamWand:229-255, FXBeamWand:230-234, FXBeamWand:239, FXBeamWand:246, Vec3 |
| `ThaumcraftExcavationClient.java:152` `render` | T@172 | I: trace candidate FXBeamWand:173-176, FXBeamWand:177-180, FXBeamWand:229-255, FXBeamWand:261-263, Vec3 |
| `ThaumcraftGearModelsClient.java:278` `applyWandFocusWave` | R@284, R@285, R@287, R@288 | I: trace candidate ItemWandCasting.java:550, ItemWandRenderer:101-111 |
| `ThaumcraftGearModelsClient.java:241` `applyWandUsePose` | T@254, R@257, R@258, R@261, R@264, T@267 | I: trace candidate ItemWandRenderer:100, ItemWandRenderer:112, ItemWandRenderer:88, ItemWandRenderer:88-113, ItemWandRenderer:93, ItemWandRenderer:95, ItemWandRenderer:97-98 |
| `ThaumcraftGearModelsClient.java:149` `renderWand` | R@162, T@166, T@170, S@171, S@177, S@178, S@184, T@189, S@190, T@195, S@198, T@201, T@218, S@219, S@220 | I: trace candidate ItemWandRenderer.java:59-86, ItemWandRenderer.java:87, ItemWandRenderer.java:88-113, ItemWandRenderer:59-86, ItemWandRenderer:87, ItemWandRenderer:88-113, ModelWand83-243, ModelWand:126-134, ModelWand:173-181, ModelWand:95-98 |
| `ThaumcraftGogglesClient.java:195` `drawNote` | T@201, R@202, S@203 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `ThaumcraftGogglesClient.java:162` `drawTags` | T@168, R@173, S@174, T@181, S@182, S@187 | I: trace candidate Native1 |
| `ThaumcraftGogglesClient.java:134` `renderRevealedNodes` | T@155 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `ThaumcraftGolemClient.java:39` `renderMarkers` | T@63 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `ThaumcraftRunicClient.java:53` `renderHud` | T@64, S@64 | I: trace candidate Original10 |
| `ThaumcraftShockClient.java:156` `render` | T@169 | I: trace candidate Vec3 |
| `ThaumcraftTravelGearClient.java:85` `renderHarnessEffects` | R@89, T@89, T@90, R@91, T@91 | I: trace candidate Vec3 |
| `VisNetClientEffects.java:57` `pose` | T@57 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `VisNetClientEffects.java:65` `render` | R@65 | I: trace candidate GL11 |
| `fx/particles/BlockRuneParticle.java:140` `draw` | T@143, R@145, R@146, T@147 | I: trace candidate FXBlockRunes:51-78, Vec3 |
| `fx/particles/FXArc.java:90` `render` | T@94 | I: trace candidate FXArc:112-121, GL11 |
| `fx/particles/FXSmokeSpiral.java:119` `render` | R@126 | I: trace candidate FXSmokeSpiral:54-57, FXSmokeSpiral:65-68 |
| `fx/particles/FXSmokeSpiral.java:93` `spiral` | T@100 | I: trace candidate FXSmokeSpiral:45-53, FXSmokeSpiral:60, Vec3 |
| `fx/particles/FocusFrostSparkleParticle.java:42` `render` | T@44, R@44 | I: trace candidate GL11 |
| `fx/particles/HarnessBoltParticle.java:32` `render` | T@36 | I: trace candidate GL11, Vec3 |
| `fx/particles/InfusionArcParticle.java:82` `render` | T@95 | I: trace candidate GL11, Vec3 |
| `fx/particles/InfusionParticle.java:306` `renderRune` | T@309, R@310, R@311, T@312 | I: trace candidate Vec3 |
| `fx/particles/MonsterSpriteParticle.java:91` `render` | T@99, R@99 | I: trace candidate GL11 |
| `fx/particles/RunicHitParticle.java:63` `render` | T@67, R@68, R@68, S@69 | I: trace candidate GL11 |
| `fx/particles/ThaumcraftBoltParticle.java:67` `render` | T@78 | I: trace candidate GL11, Vec3 |
| `fx/particles/TypeSparkleParticle.java:120` `render` | T@123, R@124 | I: trace candidate GL11 |
| `fx/particles/WardHitParticle.java:44` `render` | T@48, R@49, R@50, T@51, R@51, T@52 | I: trace candidate GL11 |
| `fx/particles/WarpSonicParticle.java:70` `render` | T@74, R@75, R@75, T@76, S@77 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `fx/particles/WispBoltParticle.java:39` `render` | T@45 | I: trace candidate GL11, Vec3 |
| `gui/AdvancedFurnaceBookDiagram.java:18` `render` | T@24, S@24 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiArcaneBore.java:26` `renderBg` | T@39, S@40 | I: trace candidate Data0 |
| `gui/GuiArcaneWorkbench.java:57` `render` | T@62, S@63 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiArcaneWorkbench.java:29` `renderBg` | T@46, S@47 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiFocalManipulator.java:211` `drawAspectAmounts` | T@214, S@215 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiFocalManipulator.java:257` `renderLabels` | T@264, T@291, S@292 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiFocusSelect.java:318` `drawRing` | T@322, R@323 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiFocusSelect.java:441` `renderWidget` | T@445, S@446 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiGolem.java:135` `renderLabels` | S@141, S@163, T@175, S@176, T@186, S@187, T@201, S@202, T@209, S@210, T@226, S@227 | I: trace candidate GuiGolem:102-114, GuiGolem:185-188, GuiGolem:228-235, GuiGolem:380-390, GuiGolem:88-91, Original80–91 |
| `gui/GuiResearchPopup.java:18` `renderHud` | T@42, S@42 | I: trace candidate GL11 |
| `gui/GuiResearchTable.java:262` `drawComponents` | T@267, T@270, S@271 | I: trace candidate RESEARCHER1 |
| `gui/GuiResearchTable.java:552` `drawRune` | T@554, R@555 | I: trace candidate GuiResearchTable.java:517-540 |
| `gui/GuiResearchTable.java:578` `line` | T@583, R@584 | I: trace candidate GuiResearchTable.java:385-405 |
| `gui/GuiResearchTable.java:73` `render` | T@80 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiResearchTable.java:279` `smallNumber` | T@282, S@283 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiThaumatorium.java:17` `renderBg` | T@33, S@33 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/GuiTravelingTrunk.java:32` `renderLabels` | S@37 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/HoveredSlotAspectRow.java:201` `amount` | T@207, S@208 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/HoveredSlotAspectRow.java:171` `background` | T@174, S@175 | I: trace candidate ClientTickEventsFML:700-708 |
| `gui/HoveredSlotAspectRow.java:82` `render` | T@105 | I: trace candidate ClientTickEventsFML:301-309, ClientTickEventsFML:685-687, ClientTickEventsFML:688-690, ClientTickEventsFML:693-724, ClientTickEventsFML:710, ClientTickEventsFML:712-719, GL11 |
| `gui/RunicAugmentationDisplay.java:98` `drawCenteredFit` | T@101, S@102 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/RunicAugmentationDisplay.java:22` `render` | T@33, S@34, T@75, S@76 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/ThaumatoriumBookDiagram.java:17` `render` | T@23, S@23 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/ThaumcraftRecipeDisplay.java:1087` `drawCenteredFit` | T@1090, S@1091 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/ThaumcraftRecipeDisplay.java:678` `render` | T@705, S@706 | I: trace candidate GuiResearchRecipe:503-773, TC4 |
| `gui/ThaumcraftRecipeDisplay.java:1029` `renderCrucible` | T@1037, S@1038, T@1070, S@1071 | I: trace candidate GuiResearchRecipe:776-877 |
| `gui/ThaumcraftRecipeDisplay.java:961` `renderEnchantmentInfusion` | T@976, S@977, T@1010, S@1011 | I: trace candidate GuiResearchRecipe:1112-1235 |
| `gui/ThaumcraftRecipeDisplay.java:896` `renderInfusion` | T@904, S@905, T@948, S@949 | I: trace candidate GuiResearchRecipe:941-1105 |
| `gui/ThaumcraftRecipeDisplay.java:849` `renderInfusionAltar` | T@856, S@857 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `gui/ThaumcraftRecipeDisplay.java:764` `renderSceptre` | T@777, S@778 | I: trace candidate GuiResearchRecipe.java:503-639 |
| `gui/ThaumcraftRecipeDisplay.java:820` `renderSynthetic` | T@828, S@829 | I: trace candidate GuiResearchRecipe.java:642-773 |
| `lib/PlayerNotifications.java:97` `renderHud` | T@119, S@120, T@126, T@136, S@138, T@149, S@149 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `lib/ScanOverlay.java:151` `drawFaces` | R@155, T@157 | I: trace candidate Vector3 |
| `lib/ScanOverlay.java:111` `render` | T@126, R@131 | I: trace candidate Vec3 |
| `renderers/VisNetRender.java:86` `billboard` | R@87, R@87 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/VisNetRender.java:91` `facingQuad` | R@92 | I: trace candidate GL11, UtilsFX153-191, Vec3 |
| `renderers/VisNetRender.java:107` `flare` | T@109, R@109 | I: trace candidate GL11, Vec3 |
| `renderers/block/LegacyBlockQuads.java:91` `render` | T@96 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderAspectOrb.java:15` `render` | R@22, S@22 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderBrainyZombie.java:36` `scale` | S@37 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderCultist.java:53` `render` | T@63 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderCultist.java:49` `scale` | S@50 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderCultistPortal.java:47` `render` | T@73 | I: trace candidate RenderCultistPortal.java:64, RenderCultistPortal.java:93-108, Vector3 |
| `renderers/entity/RenderDart.java:28` `render` | R@31, R@32, R@34, R@35, S@36, T@37, R@51 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderEldritchGolem.java:28` `scale` | S@29 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderEldritchOrb.java:20` `render` | R@26, R@27, R@28, R@29, R@30, R@31, R@46 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderEldritchWarden.java:76` `render` | S@81, S@82, T@83, S@99 | I: trace candidate ModelEldritchGuardian:467-479 |
| `renderers/entity/RenderElectricOrb.java:37` `render` | R@44, R@45, S@48 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderEmber.java:30` `render` | R@40 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderExplosiveOrb.java:32` `render` | R@39, R@40, S@41 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderFallingTaint.java:18` `render` | T@20 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderFireBat.java:17` `scale` | S@17 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderFireBat.java:18` `setupRotations` | T@18 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderFocusOrb.java:23` `render` | R@28 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderFrostShard.java:37` `render` | R@43, R@44, S@45 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:170` `emptyIcon` | R@173, T@174, S@175 | I: trace candidate RenderGolemBase:133-139 |
| `renderers/entity/RenderGolemBase.java:107` `render` | R@116, T@117, S@118, R@119, T@122, S@123 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:142` `render` | R@155, T@156, S@157, T@159, S@160 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:227` `render` | R@232, T@233, R@234, R@235, S@236, T@239 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:263` `render` | S@268, T@269, R@270 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:280` `render` | S@290, T@292, R@293, T@295, S@298, T@300, T@304, T@306, T@308 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:88` `scale` | T@90 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:93` `setupRotations` | R@99 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBase.java:317` `turnFlatItem` | R@318, R@319, R@320, R@321 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderGolemBobber.java:40` `render` | S@45, R@46, R@47 | I: trace candidate R:75–77, U0, U1, V0, V1, Vec3 |
| `renderers/entity/RenderMindSpider.java:26` `scale` | S@26 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderPech.java:27` `render` | T@30, T@31, R@31, R@31, S@31, T@32, R@32, S@32, R@32, R@32, T@34, R@35, T@35, T@36, S@36, R@36, R@36, T@37, S@37, R@37, R@37, R@37 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderPech.java:45` `render` | T@45 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderPrimalArrow.java:78` `drawTrail` | R@83 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderPrimalArrow.java:40` `render` | R@46, R@47, R@49, R@50, S@51, T@52, R@66 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderPrimalOrb.java:25` `render` | R@30, R@30, R@31, R@31, R@32, R@32, R@47 | I: trace candidate GL11, Vec3 |
| `renderers/entity/RenderSpecialItem.java:59` `drawStar` | R@66, R@67, R@68, R@69, R@70, R@71 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderSpecialItem.java:47` `render` | T@52 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderTaintCrawler.java:44` `scale` | S@45 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderTaintCreeper.java:32` `scale` | S@40 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderTaintSpider.java:17` `scale` | S@17 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderTaintSpore.java:19` `scale` | S@19 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderTaintVillager.java:26` `scale` | S@27 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderThaumicSlime.java:43` `render` | S@48, T@49 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderThaumicSlime.java:30` `scale` | S@34 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderTravelingTrunk.java:71` `render` | T@79, S@80 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderTravelingTrunk.java:52` `scale` | S@54, T@55 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/entity/RenderWatcher.java:76` `drawBeam` | T@91, R@92, R@93 | I: trace candidate Vec3 |
| `renderers/entity/RenderWisp.java:20` `render` | T@26, R@26 | I: trace candidate GL11 |
| `renderers/item/BoneBowPose.java:11` `apply` | R@12, R@13, R@14, S@15, T@16, T@17, R@18, S@19, R@20, R@21, T@22, S@23, R@24, R@25, T@26 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/item/ItemBannerRenderer.java:50` `render` | T@71, S@72, T@75, T@77, T@78, R@79, T@80 | I: trace candidate ItemBannerRenderer:27-32, ItemBannerRenderer:43-50, TileBannerRenderer:31-74 |
| `renderers/item/ItemThaumometerRenderer.java:109` `firstPerson` | T@115, R@116, R@118, R@120, T@126, R@129, T@130, R@131, S@135, T@140, R@141, R@142, R@143, R@144, R@154, T@155, S@156 | I: trace candidate ItemThaumometerRenderer:107, ItemThaumometerRenderer:113-131, ItemThaumometerRenderer:132-135, ItemThaumometerRenderer:98-135, ItemThaumometerRenderer:99-100 |
| `renderers/item/ItemThaumometerRenderer.java:57` `render` | S@65, T@67, R@68, R@69, R@75, R@76, R@77, T@85, R@86, R@87 | I: trace candidate ItemThaumometerRenderer:136-146, ItemThaumometerRenderer:141-145, ItemThaumometerRenderer:148-149, ItemThaumometerRenderer:150-154, ItemThaumometerRenderer:155-259 |
| `renderers/item/ItemThaumometerRenderer.java:204` `screen` | S@217 | I: trace candidate ItemThaumometerRenderer:154, UtilsFX:761 |
| `renderers/item/PrimalWandFocusRenderer.java:35` `renderDepthLayer` | T@39, S@40 | I: trace candidate ModelWand:173-181 |
| `renderers/item/ThaumometerReadout.java:152` `icons` | S@160, S@182 | I: trace candidate ItemThaumometerRenderer.java:221-239 |
| `renderers/item/ThaumometerReadout.java:103` `render` | T@110, S@119, T@132, S@134 | I: trace candidate ItemThaumometerRenderer:164, ItemThaumometerRenderer:191-203, ItemThaumometerRenderer:243-258 |
| `renderers/item/WandContextPose.java:88` `apply` | T@92, S@96, R@97, T@98, T@99, T@104, S@105, T@106, T@110, S@111 | I: trace candidate ItemWandRenderer:59-61, ItemWandRenderer:59-86, ItemWandRenderer:64-71, ItemWandRenderer:72-79, ItemWandRenderer:80-86 |
| `renderers/models/ModelGolem.java:135` `renderToBuffer` | S@150 | I: trace candidate Original91–94 |
| `renderers/models/ModelTaintSporeSwarmer.java:22` `renderToBuffer` | T@23, S@23, T@23 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/models/ModelTunnelField.java:125` `face` | T@129, R@130, T@133, R@134, T@137, R@138, T@141, T@144, R@145, T@148, R@149 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/models/ModelTunnelField.java:72` `renderFace` | R@101 | I: trace candidate GL_TEXTURE_2, TileMirrorRenderer:70-71, TileMirrorRenderer:74-77 |
| `renderers/models/entities/ModelGolemAccessories.java:175` `renderToBuffer` | S@178 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/models/entities/ModelTaintSeed.java:86` `renderToBuffer` | T@91, S@93 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/models/entities/ModelTaintacle.java:48` `renderToBuffer` | T@50, S@50 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/models/gear/HarnessArmorRenderer.java:51` `render` | R@54, S@58, R@58, R@59, T@60 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/ItemJarNodeRenderer.java:36` `render` | T@45, R@48, R@50 | I: trace candidate ItemJarNodeRenderer:68-73, ItemJarNodeRenderer:75-83 |
| `renderers/tile/ItemNodeRenderer.java:67` `render` | T@75, T@80, S@81, R@84, R@87 | I: trace candidate ItemNodeRenderer:50-54, ItemNodeRenderer:60-68, ItemNodeRenderer:64, ItemNodeRenderer:66 |
| `renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java:30` `draw` | T@33, R@33, R@36, T@38, R@38, R@41, R@41, T@41, S@41, T@42, R@43, R@43, T@43, S@43, T@44, T@48, R@50, R@50, R@50, T@50, T@52 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileAlembicRenderer.java:32` `render` | T@37, R@41, R@42, R@59 | I: trace candidate EAST180, NORTH270, SOUTH90, WEST0 |
| `renderers/tile/TileArcaneBoreBaseRenderer.java:29` `renderModel` | T@32, R@36 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileArcaneBoreRenderer.java:39` `renderModel` | T@43, R@44, R@48, T@49, R@52, R@55, T@55, R@60, T@60, R@68, T@68, S@68 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileArcaneLampRenderer.java:18` `render` | T@23, T@30 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileArcaneLampRenderer.java:35` `turn` | T@37, R@37, T@38, R@38, R@39, R@40, R@41 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileArcaneWorkbenchRenderer.java:23` `render` | T@26, R@27, T@38, R@39, R@40 | I: trace candidate TileArcaneWorkbenchRenderer:46-58 |
| `renderers/tile/TileBannerRenderer.java:70` `draw` | T@78, R@79, R@82, R@83, T@89 | I: trace candidate TileBannerRenderer:41-43, TileBannerRenderer:44-48, TileBannerRenderer:49-53, TileBannerRenderer:55-58, TileBannerRenderer:60-62 |
| `renderers/tile/TileBannerRenderer.java:119` `drawTag` | T@122, S@123, R@124, R@125 | I: trace candidate TileBannerRenderer:64-72 |
| `renderers/tile/TileBellowsRenderer.java:19` `render` | T@23, T@25, R@25, T@26, R@26, R@27, R@28, R@29, T@31, S@32, T@35, T@36 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileCentrifugeRenderer.java:55` `renderModel` | T@56, R@61 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileChestHungryRenderer.java:54` `renderStatic` | T@67, S@68, T@69, R@70, T@71 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileCrucibleRenderer.java:42` `render` | T@61, R@62 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileCrystalRenderer.java:49` `render` | T@55, R@56, R@57, R@57, S@58 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileCrystalRenderer.java:41` `renderItem` | T@45, R@45, T@45 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileDeconstructionTableRenderer.java:25` `render` | T@26, R@26, T@29, S@29, T@33, R@33, T@37, R@37, R@37, S@37 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEldritchCapRenderer.java:47` `render` | T@54, R@55 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEldritchCapRenderer.java:66` `renderEyes` | T@72, R@76, T@77, R@78, R@79 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEldritchCrabSpawnerRenderer.java:41` `render` | T@44 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEldritchCrystalRenderer.java:72` `orientation` | R@74, R@75, R@76, R@77, R@78 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEldritchCrystalRenderer.java:40` `render` | T@43, T@45, R@47 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEldritchLockRenderer.java:110` `around` | R@112, R@113, R@114 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEldritchLockRenderer.java:39` `render` | T@47, T@57, S@60, T@78, R@79, R@80, R@81, T@92, S@93 | I: trace candidate Vec3 |
| `renderers/tile/TileEldritchObeliskRenderer.java:60` `render` | T@73, S@74, T@86, R@90, T@91, T@104, R@105, T@112, R@113 | I: trace candidate Vec3 |
| `renderers/tile/TileEldritchPortalRenderer.java:59` `render` | T@86 | I: trace candidate LevelRenderer:1232-1275, TileEldritchPortalRenderer:44-52, TileEldritchPortalRenderer:57-59, TileEldritchPortalRenderer:76-81, Vector3 |
| `renderers/tile/TileEssentiaCrystalizerRenderer.java:32` `render` | T@33, R@34, R@34, R@34, R@34, R@34, T@35, S@37, R@40, T@40, R@40 | I: trace candidate LOW16 |
| `renderers/tile/TileEssentiaLampRenderer.java:18` `render` | T@22, T@26 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEssentiaLampRenderer.java:29` `turn` | T@31, R@31, T@32, R@32, R@33, R@34, R@35 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileEtherealBloomRenderer.java:51` `render` | T@65, R@66, T@85, S@86, T@98, R@99, S@103, R@104, T@116, R@117, R@118, S@122, R@123, T@135, R@136, T@140, S@141, R@142 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileFluxScrubberRenderer.java:53` `orientation` | R@56, R@57 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileFluxScrubberRenderer.java:31` `render` | T@40, T@42, T@45 | I: trace candidate TileFluxScrubberRenderer:38-39 |
| `renderers/tile/TileFocalManipulatorRenderer.java:28` `render` | T@31, R@32, T@45, R@46, S@47 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileInfusionMatrixRenderer.java:79` `halo` | T@87, R@89, R@90, R@91, R@92, R@93, R@94 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileInfusionMatrixRenderer.java:32` `render` | T@40, R@41, R@42, R@43, T@56, R@57, R@58, R@59, S@60 | I: trace candidate TC4, TileRunicMatrixRenderer:103-175 |
| `renderers/tile/TileInfusionPillarRenderer.java:30` `render` | T@41, R@42, R@43 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileJarBrainRenderer.java:82` `applyTurn` | R@83, R@84 | I: trace candidate TileJarRenderer.java:166-167 |
| `renderers/tile/TileJarBrainRenderer.java:51` `render` | T@55, R@56 | I: trace candidate TileJarRenderer:173-174, TileJarRenderer:62-63 |
| `renderers/tile/TileJarBrainRenderer.java:64` `renderBrain` | T@67, S@69 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileJarRenderer.java:66` `label` | T@68, R@70 | I: trace candidate X180 |
| `renderers/tile/TileJarRenderer.java:30` `renderContents` | T@34, R@36 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileMagicWorkbenchChargerRenderer.java:75` `renderModel` | T@78, R@78, R@78, R@82, T@82, R@83 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileManaPodRenderer.java:27` `render` | T@33, R@33, T@37, S@37, S@39 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileMirrorRenderer.java:69` `pane` | T@76, R@77, R@80, T@82, T@84, R@85, T@88, R@89, R@92, T@95 | I: trace candidate UtilsFX:402-405 |
| `renderers/tile/TileNodeConverterRenderer.java:26` `renderModel` | T@28, R@28, R@29, R@29, T@29 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileNodeEnergizedRenderer.java:22` `render` | T@26 | I: trace candidate Energized38-42, GL11, Vec3 |
| `renderers/tile/TileNodeRenderer.java:150` `drawNode` | T@156, R@157 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileNodeRenderer.java:193` `drawNodeBody` | R@215, R@233 | I: trace candidate ItemNodeRenderer.java:117, TileNodeRenderer.java:103 |
| `renderers/tile/TileNodeRenderer.java:54` `render` | T@62, R@63 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileNodeRenderer.java:130` `renderJarNode` | T@143 | I: trace candidate TileJarRenderer.java:54-59, TileJarRenderer:56, TileNodeRenderer.java:176-199, Vec3 |
| `renderers/tile/TileNodeStabilizerRenderer.java:52` `bubble` | T@57, R@57 | I: trace candidate GL11, GL14, UtilsFX153-191 |
| `renderers/tile/TileNodeStabilizerRenderer.java:47` `renderModel` | T@48, R@48, R@49, R@49, T@49 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TilePedestalRenderer.java:21` `render` | T@30, R@31, S@36 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileResearchTableRenderer.java:26` `render` | T@30, R@31, R@33, R@40, R@41, T@42, R@43, S@44, T@50, R@51, R@52, S@53, S@64 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileTableRenderer.java:20` `render` | T@24, R@25, R@26 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileThaumatoriumRenderer.java:21` `render` | T@24, R@25, R@26, T@35, R@36, S@37, T@40, S@45, T@46, R@47, S@48, S@52, T@53, R@54, T@55 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileTubeBufferRenderer.java:102` `orient` | R@103, R@104, R@104, R@105 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileTubeBufferRenderer.java:46` `render` | T@75, S@76, T@76 | I: trace candidate W12, W4, W7, W9 |
| `renderers/tile/TileTubeRenderer.java:172` `orientStem` | R@173, R@175, R@176, R@178 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileTubeRenderer.java:137` `renderOneWay` | T@139, S@143, T@144, T@150 | I: trace candidate TileTubeOnewayRenderer:42-49 |
| `renderers/tile/TileTubeRenderer.java:155` `renderValve` | T@157, R@162, T@163, R@165, T@166, S@167 | I: trace candidate TC4 |
| `renderers/tile/TileVisRelayRenderer.java:46` `renderModel` | T@47, R@47, R@47, R@47, R@47, R@47, R@47, R@47, S@48, T@48 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |
| `renderers/tile/TileWandPedestalRenderer.java:37` `render` | T@46, R@47 | M: no method-local file:line anchor; document basis/axes/order and original or native-adapter rationale |

Additional review examples: `renderers/tile/MirrorProjection.java:14-22` documents OpenGL multiplication order and points at oracle :101–107 but only names the file in the class comment; retain a complete file:line anchor on the method. `renderers/item/ItemThaumometerRenderer.java:27-29` previously contradicted its actual -Y inventory rotation by saying Z; corrected and the 248-degree angle named, retaining all transforms. `renderers/tile/TileTubeBufferRenderer.java:102-105` composes three orientation rotations without a per-branch order explanation, although its class cites a broad original renderer range. `renderers/VisNetRender.java:85-92` deliberately has different strip/quad UV ordering; a single generic billboard rewrite would erase reviewable original differences.

## 6. Magic numbers

**Inspected: 26,764 Java numeric literal nodes across the full surface**, including mesh data, dimensions, structural zero/one values and constants. This is an inventory count, not a claim that all 26,764 need constants. **Worst 30 provenance/naming findings** below; one fixed (and its contradictory axis prose fixed). Original values must not be rounded, normalized, or replaced with mathematically similar float expressions. Generated mesh coordinate tables should have a generator/hash/provenance, not one constant per vertex.

| # | Site | Values / missing semantic name and anchor | Severity |
|---|---|---|---|
| 1 | `renderers/item/ItemThaumometerRenderer.java:51` | 248.0F: FIXED as INVENTORY_Y_ROTATION_DEGREES; existing original ItemThaumometerRenderer:141–145 anchor retained. | L fixed |
| 2 | `renderers/fluid/PureFluidRender.java:59` | .875F and /8F: full-height/quanta factor repeated at 62; name with BlockFluidPure oracle lines. | L |
| 3 | `renderers/VisNetRender.java:108` | .0624375F, .3125F, 16: flare atlas span/inset/row/column count. | M |
| 4 | `renderers/VisNetRender.java:101` | 8.2, .105, .2F/.1F: beam cutoff, half-width and UV scrolling periods. | M |
| 5 | `renderers/entity/RenderPrimalOrb.java:35` | .866D cast to float: original fan cross-section; preserve double multiply/cast order. | M |
| 6 | `renderers/entity/RenderEldritchOrb.java:34` | .866F: similar family, different precision from primal fan. | M |
| 7 | `renderers/entity/RenderEldritchOrb.java:47` | 13,16F,.0624375F,.1875F: atlas animation count/stride/row. | M |
| 8 | `renderers/entity/RenderPrimalOrb.java:50` | .1874375F/.125F/.0624375F: billboard UV edge policy differs from eldritch. | M |
| 9 | `renderers/tile/TileCrucibleRenderer.java:64` | .0624375F: repeated particle cell span. | L |
| 10 | `renderers/tile/MirrorProjection.java:18` | 700000L/250000F: texture time period/rate. | M |
| 11 | `renderers/tile/MirrorProjection.java:20` | 4321,9,2F: layer spin polynomial; also ModelTunnelField:101. | M |
| 12 | `renderers/tile/LinkedMirrorField.java:45` | 31100L: per-face random-reset seed; name its reset ownership. | M |
| 13 | `renderers/tile/LinkedMirrorField.java:42` | .99F/.01F: signed mirror-plane inset. | L |
| 14 | `renderers/models/ModelTunnelField.java:89` | .5F/.1F/.4F: original random color ranges repeated in LinkedMirrorField:48–50. | M |
| 15 | `renderers/tile/TileBannerRenderer.java:122` | .05001F/.0375F: icon offset and model-to-icon scale. | L |
| 16 | `renderers/tile/TileBannerRenderer.java:125` | 57.295776F and *2F: degree conversion and wave multiplier; do not replace with a higher-precision constant. | L |
| 17 | `renderers/entity/RenderPrimalArrow.java:51` | .05625F: original projectile model scale. | L |
| 18 | `renderers/entity/RenderGolemBase.java:156` | -.05F,-1.106F,.099F and .1F scale: inventory-row anchor, repeated at 174–175. | M |
| 19 | `renderers/tile/TileBellowsRenderer.java:20` | .125F + scale*.875F: inflation displacement; name bounds and pivot. | L |
| 20 | `renderers/tile/TileNodeStabilizerRenderer.java:49` | 37F,100F,50,170F: piston displacement and glow ranges. | M |
| 21 | `renderers/tile/TileNodeStabilizerRenderer.java:60` | 0xFF4444 vs piston 0xFF3333; preserve semantic distinction in named constants. | L |
| 22 | `renderers/tile/TileNodeConverterRenderer.java:27` | 50/137F: converter travel clamp/normalizer. | M |
| 23 | `renderers/tile/TileTubeRenderer.java:126` | .001F,.1F,.9F: one-way overlay bounds and anti-z-fight offset. | L |
| 24 | `renderers/tile/TileTubeBufferRenderer.java:76` | 1.2F and -.5: choke stem scale/pivot, currently embedded in transform chain. | L |
| 25 | `renderers/tile/TileWardedGlassRenderer.java:106` | -.001F/1.001F: overlay expansion, duplicated in TileWardedRenderer:61. | L |
| 26 | `renderers/tile/TileResearchTableRenderer.java:50` | -.01-a*.015,.1,.35: parchment layer offset and spacing. | L |
| 27 | `renderers/tile/TilePedestalRenderer.java:30` | 1.15,32767,16,.05: bob center/wrap/period/amplitude. | L |
| 28 | `renderers/tile/TileDeconstructionTableRenderer.java:33` | 1.15,14F,.2F,.1F: different bob policy in same display-item family. | L |
| 29 | `renderers/models/gear/HarnessArmorRenderer.java:72` | .0005F: UV inset repeated in ModelThaumatorium and OBJ readers; name common loader policy with citation. | M |
| 30 | `renderers/item/PrimalWandFocusRenderer.java:46` | 30F: physical sheet frame count versus Geometry.FRAMES timeline length36; naming must distinguish them. | L |

## 7. Resource loading and verification

**Inspected: 246 ResourceLocation construction calls; 44 non-literal/parameterized calls, of which 10 are sound/registry/model-layer/recipe IDs, one is a bounded vanilla drip-sprite selector and 33 are mod asset path construction sites. Also inspected all 15 stream-opening sites, including both getResourceAsStream calls. Findings: 33 mod-asset construction coverage/guard gaps + 2 classpath reload-bypass sites + 2 silent missing-icon paths = 37.** Counting is by site/category, not unique missing files; some sites participate in more than one concern.

Actually ran `python3 scripts/porting/convert_assets.py --verify`: PASS, 3,944 model texture refs, 3,018 local model refs, 518 atlas sprites, 111 sound files, 66 sound events, zero missing references. Also independently checked **159 literal texture/shader construction calls / 127 unique paths** against common resources or the named vanilla jar: all present. Bounded ripple1..15 and warded_glass_1..47 families: all 62 files present.

**M: `scripts/porting/convert_assets.py:650-742` never reads Java.** Its model/atlas/sound validation cannot establish all Java helpers' output families, standalone textures, OBJ/MTL dependencies, shader graph dependencies, or addon material tags. Some files happen to be verified indirectly by the atlas/JSON graph; no call-site-to-family proof is present. This is not 33 proven missing textures. Missing GPU textures generally use engine missing-texture handling, not a thrown exception; atlas `.apply` does not assert the requested sprite exists. Fail loudly at reload for mandatory built-in assets and explicitly distinguish optional/addon assets.

### All 44 dynamic/parameterized construction calls

| Site | Expression | Verification / failure policy |
|---|---|---|
| `BoreClientEffects.java:51` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `ThaumcraftArchitectClient.java:134` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/warded_glass_" + texture + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `ThaumcraftEldritchItemsClient.java:38` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", id)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `ThaumcraftExcavationClient.java:302` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/" + texture + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `ThaumcraftGearModelsClient.java:92` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `ThaumcraftGearModelsClient.java:349` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + layer.sheet())` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `ThaumcraftShockClient.java:216` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/p_" + size + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `ThaumcraftTravelGearClient.java:76` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `ThaumcraftWarpClient.java:150` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "shaders/post/" + name + ".json")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `fx/InfusionClientEffects.java:77` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", name)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `fx/particles/TaintDropParticle.java:24` | `ResourceLocation.withDefaultNamespace(sprite)` | I: vanilla drip_hang/fall/land particle sprite family; outside mod convert_assets scope. Native atlas fallback, not a mod file claim. |
| `fx/particles/WarpSonicParticle.java:80` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/ripple" + frame + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. All 15 currently present; ID still allocated every render. |
| `gui/GuiFocalManipulator.java:60` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `gui/GuiFocusSelect.java:90` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `gui/GuiResearchBrowser.java:61` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `gui/GuiResearchPopup.java:47` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", match.group(1).toLowerCase(java.util.Locale.ROOT))` | M: corpus-derived icon path outside verifier; missing resource silently returns at49. |
| `gui/GuiResearchRecipe.java:131` | `ResourceLocation.parse(image.texture())` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `gui/GuiResearchTable.java:63` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `gui/GuiResearchTable.java:466` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", name)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `gui/HoveredSlotAspectRow.java:71` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `gui/ResearchIconDisplay.java:351` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `gui/ThaumcraftRecipeDisplay.java:392` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", nativeId)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `gui/ThaumcraftRecipeDisplay.java:420` | `ResourceLocation.tryParse(entry.getValue())` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `gui/ThaumcraftRecipeDisplay.java:453` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", id)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `gui/ThaumcraftRecipeDisplay.java:530` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "crucible/balanced_shard_" + primal)` | I: ID, not an asset-file lookup. Do not count as missing texture; sounds have event-graph validation, registry/model-layer/recipe IDs need their own checks. |
| `lib/PlayerNotifications.java:128` | `ResourceLocation.parse(entry.image())` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `lib/UtilsFX.java:117` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | M: caller-supplied PNG path outside verifier; read failures WARN then return16 at 81–83/103–105, not fail-fast; repeated failures are not cached. |
| `renderers/VisNetRender.java:32` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/block/LegacyBlockQuads.java:33` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/" + name)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/block/PositionBlockModels.java:101` | `net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/es_" + (index + 1))` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/entity/RenderPech.java:43` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + name + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/entity/RenderTravelingTrunk.java:75` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/golem_upgrade_" + upgrade.key + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/models/ModelEssentiaCrystalizer.java:30` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + name + ".obj")` | M: --verify does not enumerate this OBJ family; runtime loader does fail loudly (IllegalStateException at 47). |
| `renderers/models/gear/HarnessArmorRenderer.java:41` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/models/gear/ModelHoverHarness.java:49` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/models/gear/ModelWand.java:47` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_" + baseTag + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/models/gear/ModelWand.java:52` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_" + tag + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java:20` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + path + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java:59` | `ResourceLocation.parse(id)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/tile/TileNodeStabilizerRenderer.java:73` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + name + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/tile/TileResearchTableRenderer.java:24` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/tile/TileTubeRenderer.java:230` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/" + name)` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/tile/TileWardedGlassRenderer.java:52` | `ResourceLocation.parse(TEXTURES[texture])` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. |
| `renderers/tile/TileWardedRenderer.java:26` | `ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/warded_glass_" + (index + 1) + ".png")` | M: not enumerated from this Java expression by --verify; no mandatory-asset guard at construction. Enumerate bounded family or declare addon/optional policy; missing textures/atlas sprites use engine fallback. All 47 currently present; cached once, not per-frame. |

### Every stream-open site

| Site | Missing-file behavior / verifier coverage |
|---|---|
| `lib/UtilsFX.java:118` | ResourceManager throws; outer helpers WARN and return fallback16 (81–83,103–105), not silently empty drawing, but no fail-fast and no failed-key cache. Arbitrary path not enumerated by verifier. |
| `fx/particles/RunicHitParticle.java:38` | ResourceManager reader; catch rethrows IllegalStateException at53. OBJ not checked by --verify. |
| `fx/particles/WarpSonicParticle.java:45` | ResourceManager reader; catch rethrows IllegalStateException at60. OBJ not checked by --verify. |
| `renderers/entity/RenderFrostShard.java:72` | ResourceManager stream; throws clear failure at104, bounded mesh/face/coordinate validation. OBJ not checked by --verify. |
| `renderers/models/ModelAdvancedAlchemyFurnace.java:20` | **M:** classloader stream; null explicitly throws at21 and parser failures throw43. Bypasses resource packs/reload; static GROUPS. OBJ not checked by --verify. |
| `renderers/models/ModelThaumometer.java:39` | **M:** classloader stream; null throws40, parser failures throw64. Static original mesh bypasses resource packs/reload. OBJ not checked by --verify. |
| `renderers/models/ModelVisRelay.java:23` | ResourceManager reader; throws IllegalStateException at33. OBJ not checked by --verify. |
| `renderers/models/ModelEssentiaCrystalizer.java:30` | ResourceManager reader; throws IllegalStateException at47. Dynamic OBJ family not checked by --verify. |
| `renderers/models/ModelNodeStabilizer.java:22` | ResourceManager reader; throws IllegalStateException at34. OBJ not checked by --verify. |
| `renderers/models/ModelAlembic.java:28` | MTL ResourceManager reader; throws IllegalStateException at82–83. MTL not checked by --verify. |
| `renderers/models/ModelAlembic.java:43` | OBJ ResourceManager reader; same catch/failure. OBJ not checked by --verify. |
| `renderers/models/ModelInfusionPillar.java:22` | ResourceManager reader; throws IllegalStateException at59–60. OBJ not checked by --verify. |
| `renderers/models/ModelThaumatorium.java:30` | getResourceOrThrow/open; throws IllegalStateException at33. OBJ not checked by --verify. |
| `renderers/models/gear/HarnessArmorRenderer.java:48` | getResourceOrThrow/open; throws IllegalStateException at49. OBJ not checked by --verify. |
| `renderers/models/gear/ModelHoverHarness.java:60` | getResourceOrThrow/open; throws IllegalStateException at63, reset hook70. OBJ not checked by --verify. |

The OBJ/MTL files addressed above exist in the retained resource tree; existence today is not dependency coverage of --verify. No stream was found that returns an empty mesh solely because the file is absent. **M silent fallback sites:** `gui/GuiResearchPopup.java:49` returns without an icon; `gui/GuiResearchBrowser.java:124-128` silently skips an absent corpus icon. Optional assets may legitimately fall back, but mandatory original icons need a once-per-reload diagnostic. These are the two extra silent-path findings, not alleged resource leaks (try-with-resources is used by the readers).

## 8. Dead code, registration and family consistency

**Inspected: 188 renderer type names against all common/Fabric/NeoForge production Java references; all 2,256 current method/constructor declarations for unique-name no-caller candidates, plus the removed accessor. Findings/inventory rows: 7 no-production-reference classes, 37 unique-name no-production-caller methods, 3 family review discrepancies, and 1 fixed private accessor = 48 baseline rows.** The 37 are conservative lexical candidates, not a whole-program reachability proof: same-named overloads, reflection/external addons and engine callbacks need semantic review; test-only APIs are explicitly retained. No registration was added or removed. A model/helper does not itself require an entity-renderer registration.

### No production reference outside own file (7)

| Site | Severity / interpretation |
|---|---|
| `renderers/entity/RenderFallbackLiving.java:14` | L: unregistered fallback, near-duplicate of RenderFallbackBiped; obsolete compatibility policy, not a missing live renderer claim. |
| `renderers/entity/RenderFallbackBiped.java:14` | L: same as above with Mob bound. |
| `renderers/entity/RenderNoop.java:13` | L: unregistered compatibility renderer; other actual empty-body renderers are registered explicitly. |
| `renderers/entity/RenderWandEntityItem.java:13` | L: unused vanilla subclass; its claim that wands have no special context is stale alongside the current builtin wand renderer. |
| `renderers/block/ArcaneFurnaceBakedModel.java:18` | L: pure disconnected rule helper, not a live BakedModel implementation despite its name; test coverage does not wire runtime geometry. |
| `renderers/block/ObsidianTotemBakedModel.java:17` | L: disconnected rule helper, not proof that runtime totem rendering is absent. |
| `renderers/models/ModelManaPod.java:19` | L: no production constructor use; native mana-pod path uses another geometry implementation. Keep/remove only after test/API compatibility review. |

### Every unique-name no-production-caller candidate (37)

Search strips comments/strings and includes `::method` references, so registered callbacks such as receivePool/receiveDiscovery/appendWandUpgradeTooltip are **not** false positives. Test references below are textual counts by file, not proof of runtime test execution. Public methods were not deleted just because production code currently has no caller.

| Site / method | Test files mentioning name | Severity / disposition |
|---|---:|---|
| `ThaumcraftExcavationClient.java:66` `activeBeamCount` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `ThaumcraftFocusClient.java:145` `shouldSuppressOffhandSwap` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `ThaumcraftGearModelsClient.java:120` `hoverHarnessModel` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `ThaumcraftGearModelsClient.java:371` `renderKnightArmor` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `ThaumcraftGearModelsClient.java:377` `renderLeaderArmor` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `ThaumcraftShockClient.java:53` `activeBoltCount` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `VisNetClientEffects.java:33` `diagnostics` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `fx/EssentiaFlowClientEffects.java:143` `runningFlows` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `fx/particles/BoreDigSparkleParticle.java:113` `lifetimeFor` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `gui/GuiHandMirror.java:42` `inputSlotX` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `gui/GuiHandMirror.java:43` `inputSlotY` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `gui/GuiMagicBox.java:48` `boxRows` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `gui/GuiMagicBox.java:50` `linkedRows` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `gui/GuiMagicBox.java:51` `boxSlotX` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `gui/GuiMagicBox.java:52` `boxSlotY` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `gui/ThaumcraftRecipeDisplay.java:426` `aliases` | 2 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `lib/UtilsFX.java:203` `getGuiZLevel` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `lib/UtilsFX.java:219` `getParticleTexture` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/block/ArcaneFurnaceBakedModel.java:46` `textureForSide` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `renderers/block/ArcaneFurnaceBakedModel.java:110` `nozzleGeometry` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `renderers/block/GasSurfaceRules.java:127` `topLight` | 1 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `renderers/block/OreOverlayRules.java:51` `insetMin` | 2 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `renderers/block/OreOverlayRules.java:53` `insetMax` | 2 | I: test-facing seam; retain unless tests/API are deliberately refactored. |
| `renderers/entity/RenderWatcher.java:68` `drawWatcher` | 0 | L: unreachable private retained rendering body; intentional historical watcher policy, not permission to make it visible. |
| `renderers/item/ThaumometerReadout.java:89` `iconsInRow` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/item/ThaumometerReadout.java:243` `textOf` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/ModelCentrifuge.java:41` `renderBoxes` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/ModelCentrifuge.java:47` `renderSpinnyBit` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/ModelJar.java:44` `renderCore` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/ModelJar.java:49` `renderLid` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/ModelManaPod.java:35` `renderFirst` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/ModelManaPod.java:40` `renderSecond` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/ModelManaPod.java:45` `renderThird` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/entities/ModelWatcher.java:117` `eyePosition` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/models/gear/ModelHoverHarness.java:75` `renderBody` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/tile/TileNodeRenderer.java:85` `revealedTo` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |
| `renderers/tile/TileNodeRenderer.java:86` `revealRange` | 0 | L: no production/test name reference; review compatibility/documented purpose before removing public surface. |

**Fixed L:** removed private `ThaumcraftGearModelsClient.fortressLight()` and its otherwise-unused cached field (baseline437–440/82). No Java caller or method reference existed; the FORTRESS_LIGHT model-layer registration is unchanged. Kept the historically unreachable Watcher body because its header explicitly documents the original invisible/unregistered behavior; deleting that historical port or activating it is not a cheap drawing-neutral cleanup.

Three family consistency review findings:

1. **M:** `renderers/tile/TileTubeRenderer.java:241-244` explicitly retains a basic-tube facing mark as an accepted discrepancy, while `TileTubeBufferRenderer.java:108` always uses PIPE. That policy is buried inside a quad helper; give the family an explicit policy/acceptance anchor. Do not make them uniform by guesswork.
2. **M:** `renderers/tile/LinkedMirrorField.java:45-50` resets seed31100 on every face/render; `renderers/models/ModelTunnelField.java:89-91` advances an instance Random across draws. Mirror and hole/lock families also take different immediate/projective versus buffered approximate paths. Their reason, ownership and expected visual distinction need an explicit maintained family contract, not only separate implementation narratives. No RNG/geometry changed here.
3. **M:** `renderers/entity/RenderPrimalOrb.java:35-53` uses actual TRIANGLES for fans, additive immediate billboard, and double-expression endpoint scaling; `RenderEldritchOrb.java:24-50` uses degenerate QUADS and buffered translucent billboard with different UV edges. These may encode real original differences; require side-by-side oracle anchors for topology/blend/precision rather than silently copying whichever renderer is newer.

## Verification, fixes and remaining work

* **Java 21 javac PASS:** all **1,445 common production Java files**, after final source edits. `--release 21 -proc:none -sourcepath ''`; output **this worktree's** `common/build/classes/java/main`, also on the classpath. Initial bootstrap and final compile completed with only existing deprecation/unchecked notes. Final log: ignored `.tmp/client-javac-final.log`; arguments `.tmp/client-javac-all.args`.
* Classpath uses the original named/remapped **1.21.1** merged jar under `~/.gradle-leader/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/`, cached binary dependencies and named Architectury API. Also uses the existing `/tmp/opencode/a2quality/minecraft-access-overlay.jar`: an access-only overlay for normal declared access-widener visibility, **not stubs** and not a modified upstream jar. No Gradle or NeoForge build was run; choosing a named API binary for compilation is not NeoForge runtime acceptance. No other worktree or primary checkout was read or modified.
* **Asset verifier PASS**, plus the explicit literal/bounded-family checks above. This does not validate generated Java path families, reload behavior, shader execution or visual parity.
* **Behavior-neutral token comparison PASS** for the five finally changes: after stripping the inserted try/finally braces, the executable token stream is unchanged. Named 248-degree constant was substituted back to248.0F for comparison; axis prose only changed in comments. Private accessor/field removal reviewed separately.
* **No runtime, client, screenshots, JUnit or GL failure-injection tests.** Allocation counts are not measured GC profiles. Zero malformed push/pop text is not zero exception exposure. State review gaps are not a measured number of corrupted frames.
* Left deliberately: **80 exception-exposed pose scopes**, **38 state-ownership review gaps**, allocation/duplication refactors, method-local oracle anchors, remaining numeric naming/provenance work, Java/OBJ/shader dependency validation, and disconnected public compatibility/test surface. These need focused follow-up; this document is not a green quality gate.
