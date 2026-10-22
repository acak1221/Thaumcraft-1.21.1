# Visual registration fixes — 2026-10-23

## Advanced stabilizer: implemented, not frame-verified

The advanced item now has a renderer on **Fabric and NeoForge**. Both the item
and placed-block paths select lock2. Expected player-visible result: the original
lock body with four pistons and **red piston overlays**, visible in inventory,
hand and dropped-item contexts; the placed active device also has its **red bubble**.
Basic lock1 remains white. No bubble is fabricated for an inactive item.

### Oracle evidence (private 4.2.3.5 oracle, paths relative to its root)

* `thaumcraft/client/renderers/block/BlockStoneDeviceRenderer.java:90-92` renders
  inventory metadata 9 **and 10** via `new TileNodeStabilizer(metadata)`.
* `thaumcraft/common/tiles/TileNodeStabilizer.java:20-25` starts count at zero and
  maps metadata 9 to lock1, otherwise lock2.
* `thaumcraft/client/renderers/tile/TileNodeStabilizerRenderer.java:38-57,66-83`
  loads **one** `node_stabilizer.obj`, draws `lock`, then four rotated `piston`
  parts. There is no separate lock2 mesh. Lines 71-73 tint the overlay only to
  `(1,.2,.2)`; line 95 uses `0xFF4444` for lock2's active bubble.
* The port retains that OBJ (28 lock + 12 piston triangles; four piston draws),
  base texture and overlay texture. The new vertex tint is `0xFF3333`, exactly
  `(1,.2,.2)` at byte precision. Basic base/piston vertices stay white. Bubble
  tint is passed per vertex, so it cannot leak into a later device draw.

Changed: `ModelNodeStabilizer` gains a coloured render overload;
`TileNodeStabilizerRenderer` accepts the advanced stack and uses the tile's
existing `isAdvanced()` for placed devices; both loader bootstraps register
`ADVANCED_NODE_STABILIZER_ITEM`. This fixes the missing callback **and** the
previous basic-only colour path, not a basic-artwork alias.

## Sweep method and limits

Reproduce with `python3 scripts/porting/audit_visual_registration.py`.
The tables include every direct builtin item asset and every modern renderer
source file (including models/rule helpers). Code-reference closure strips
comments, excludes tests and legacy code, and supplies a navigation witness;
**references are not runtime reachability proof**. Registration sites were also
read: Fabric `ThaumcraftFabricClient.onInitializeClient`, NeoForge
`ThaumcraftNeoForgeClient.onRegisterClientExtensions`, common `ClientSetup.init`
and its `Thaumcraft*Client.init` registrations, entity `registerAll`, eldritch
dispatcher delegates, model-loading adapters and client mixins. Both loaders
invoke common setup. NeoForge adapters wrap item callbacks in a vanilla
`BlockEntityWithoutLevelRenderer`; Fabric uses `BuiltinItemRendererRegistry`.

No Gradle, client, Java compilation or visual acceptance run. The assertions in
the inventory script check source routing, colour constants, counts and OBJ
triangle groups only. Existing unrelated suspected findings in VISUAL_FINDINGS
remain open.

**Counts:** 30 direct builtin models; 995 inherited models (1025 total). 188 modern renderer/helper files: 178 referenced, 10 unused/superseded. One missing live item registration fixed on two loaders; no additional missing live registration found.

## Direct builtin item models

Paths: `common/src/main/resources/assets/thaumcraft/models/item/`.

| Asset | Registered? | Loader | Finding / callback |
|---|---|---|---|
| `advanced_alchemical_furnace.json` | Yes | Both | TileAlchemyFurnaceAdvancedRenderer |
| `advanced_node_stabilizer.json` | Yes | Both | TileNodeStabilizerRenderer (lock2; FIXED) |
| `arcane_bore.json` | Yes | Both | TileArcaneBoreRenderer |
| `arcane_bore_base.json` | Yes | Both | TileArcaneBoreBaseRenderer |
| `aura_node.json` | Yes | Both | ItemNodeRenderer |
| `banner.json` | Yes | Both | ItemBannerRenderer |
| `centrifuge.json` | Yes | Both | TileCentrifugeRenderer |
| `crystal_air.json` | Yes | Both | TileCrystalRenderer (cluster registration loop) |
| `crystal_earth.json` | Yes | Both | TileCrystalRenderer (cluster registration loop) |
| `crystal_entropy.json` | Yes | Both | TileCrystalRenderer (cluster registration loop) |
| `crystal_fire.json` | Yes | Both | TileCrystalRenderer (cluster registration loop) |
| `crystal_mixed.json` | Yes | Both | TileCrystalRenderer (cluster registration loop) |
| `crystal_order.json` | Yes | Both | TileCrystalRenderer (cluster registration loop) |
| `crystal_water.json` | Yes | Both | TileCrystalRenderer (cluster registration loop) |
| `essentia_buffer.json` | Yes | Both | TileTubeBufferRenderer |
| `essentia_crystallizer.json` | Yes | Both | TileEssentiaCrystalizerRenderer |
| `essentia_filter.json` | Yes | Both | TileTubeRenderer.renderAdvancedItem |
| `essentia_oneway.json` | Yes | Both | TileTubeRenderer.renderAdvancedItem |
| `essentia_restrict.json` | Yes | Both | TileTubeRenderer.renderAdvancedItem |
| `essentia_valve.json` | Yes | Both | TileTubeRenderer.renderValveItem |
| `hungry_chest.json` | Yes | Both | synthetic TileChestHungry -> registered BER |
| `jar.json` | Yes | Both | JarItemRenderer |
| `jar_void.json` | Yes | Both | JarItemRenderer |
| `node_stabilizer.json` | Yes | Both | TileNodeStabilizerRenderer (lock1) |
| `node_transducer.json` | Yes | Both | TileNodeConverterRenderer |
| `staff_shape.json` | Via WAND (template) | Both | renderWand (WAND override template, not a separate item) |
| `thaumometer.json` | Yes | Both | ItemThaumometerRenderer |
| `vis_charge_relay.json` | Yes | Both | TileMagicWorkbenchChargerRenderer |
| `vis_relay.json` | Yes | Both | TileVisRelayRenderer |
| `wand_shape.json` | Via WAND (template) | Both | renderWand (WAND override template, not a separate item) |

### Inherited builtin models

| Asset/group | Count | Registered? | Loader | Finding |
|---|---:|---|---|---|
| `wand_*` material/cap/focus variants (excluding shape templates) | 990 | Via WAND | Both | WAND model overrides share renderWand; not separate registered items. |
| `jar_node.json` | 1 | Yes / via owner | Both | ItemJarNodeRenderer |
| `staff_focus_shape.json` | 1 | Yes / via owner | Both | WAND / renderWand template |
| `staff_primal_shape.json` | 1 | Yes / via owner | Both | WAND / renderWand template |
| `wand_focus_shape.json` | 1 | Yes / via owner | Both | WAND / renderWand template |
| `wand_primal_shape.json` | 1 | Yes / via owner | Both | WAND / renderWand template |

## Every modern renderer/helper source

Paths below are relative to `common/src/main/java/thaumcraft/client/renderers/`. “Indirect” means model/helper/delegate, not a missing standalone registry entry.

| Asset/class | Registered / referenced? | Loader | Finding / source witness |
|---|---|---|---|
| `CameraBasis.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java` |
| `VisNetRender.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/fx/particles/SanityBubbleParticle.java` |
| `block/ArcaneFurnaceBakedModel.java` | No — unused | Neither (unused) | Unused pure texture/geometry rule helper, not a BakedModel implementation; furnace has resource models. |
| `block/CandleDripRules.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/block/PositionBlockModels.java` |
| `block/EldritchCrustBakedModel.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/block/PositionBlockModels.java` |
| `block/GasSurfaceRules.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/block/GasWorldQuads.java` |
| `block/GasWorldQuads.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/block/LegacyBlockQuads.java` |
| `block/LegacyBlockQuads.java` | Yes / indirect | Both (shared / platform-adapted) | `fabric/src/main/java/thaumcraft/fabric/client/FabricPositionBlockModels.java` |
| `block/ObsidianTotemBakedModel.java` | No — unused | Neither (unused) | Unused pure texture-index helper, not a BakedModel implementation; totem has resource models. |
| `block/OreOverlayRules.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/block/OreRenderRules.java` |
| `block/OreRenderRules.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/common/mixin/client/OreAnisotropyMixin.java` |
| `block/PositionBlockModels.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/mixin/client/NeoForgePositionBlockModelMixin.java` |
| `block/TaintFibreWorldQuads.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/block/LegacyBlockQuads.java` |
| `block/TaintOverlayRules.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/common/mixin/client/TaintOverlayChunkLayerMixin.java` |
| `block/WardedGlassBakedModel.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileWardedGlassRenderer.java` |
| `entity/GolemBobberSprite.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBobber.java` |
| `entity/RenderAlumentum.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderAspectOrb.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderBrainyZombie.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderCultist.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderCultistPortal.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderDart.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderEldritchCrab.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderEldritchGolem.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderEldritchGuardian.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderEldritchOrb.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderEldritchWarden.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderElectricOrb.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemOrb.java` |
| `entity/RenderEmber.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderExplosiveOrb.java` | No — unused | Neither (unused) | Superseded: ThaumcraftEntityRenderers binds explosive orb to RenderFocusOrb(ctx,true). |
| `entity/RenderFallbackBiped.java` | No — unused | Neither (unused) | Unused generic fallback; no entity binding requests it. |
| `entity/RenderFallbackLiving.java` | No — unused | Neither (unused) | Unused generic fallback; no entity binding requests it. |
| `entity/RenderFallingTaint.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderFireBat.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderFocusOrb.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderFollowingItem.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderFrostShard.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderGolemBase.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderGolemBobber.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderGolemOrb.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderInhabitedZombie.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderMindSpider.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderNoop.java` | No — unused | Neither (unused) | Unused no-op fallback; no entity binding requests it. |
| `entity/RenderPech.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderPechBlast.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderPrimalArrow.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderPrimalArrowEntity.java` |
| `entity/RenderPrimalArrowEntity.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderPrimalOrb.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderSpecialItem.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintChicken.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintCow.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintCrawler.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintCreeper.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintPig.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintSeed.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintSheep.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintSpider.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintSpore.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintSporeSwarmer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintSwarm.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintVillager.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTaintacle.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderThaumicSlime.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderTravelingTrunk.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderWandEntityItem.java` | No — unused | Neither (unused) | Unused vanilla ItemEntityRenderer subclass; vanilla dropped-item renderer invokes the registered wand item renderer. |
| `entity/RenderWatcher.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/RenderWisp.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/ThaumcraftEntityRenderers.java` |
| `entity/ThaumcraftEntityRenderers.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `entity/WarpAlphaVertex.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchGuardian.java` |
| `fluid/PureFluidRender.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/mixin/client/NeoForgePureFluidMixin.java` |
| `item/BoneBowGlint.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/common/mixin/client/BoneBowHeldPoseMixin.java` |
| `item/BoneBowGlintRules.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/item/BoneBowGlint.java` |
| `item/BoneBowPose.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/common/mixin/client/BoneBowHeldPoseMixin.java` |
| `item/ItemBannerRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `item/ItemThaumometerRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `item/JarItemRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `item/PrimalWandFocusRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `item/ThaumometerArmPose.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` |
| `item/ThaumometerReadout.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` |
| `item/WandContextPose.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `item/WandRenderContext.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/common/mixin/client/WandRenderContextMixin.java` |
| `item/WandUsePose.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `models/ModelAdvancedAlchemyFurnace.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java` |
| `models/ModelAlembic.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileAlembicRenderer.java` |
| `models/ModelArcaneBore.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileArcaneBoreRenderer.java` |
| `models/ModelArcaneBoreBase.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileArcaneBoreBaseRenderer.java` |
| `models/ModelArcaneWorkbench.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileArcaneWorkbenchRenderer.java` |
| `models/ModelBanner.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileBannerRenderer.java` |
| `models/ModelBellows.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileBellowsRenderer.java` |
| `models/ModelBrain.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileJarBrainRenderer.java` |
| `models/ModelCentrifuge.java` | No — unused | Neither (unused) | Superseded: TileCentrifugeRenderer bakes its own six-box geometry. |
| `models/ModelCrabVent.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchCrabSpawnerRenderer.java` |
| `models/ModelCube.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java` |
| `models/ModelEldritchCap.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java` |
| `models/ModelEldritchCrystal.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java` |
| `models/ModelEldritchGuardian.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchGuardian.java` |
| `models/ModelEssentiaCrystalizer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEssentiaCrystalizerRenderer.java` |
| `models/ModelFireBat.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderFireBat.java` |
| `models/ModelGolem.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java` |
| `models/ModelInfusionPillar.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileInfusionPillarRenderer.java` |
| `models/ModelJar.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileJarBrainRenderer.java` |
| `models/ModelManaPod.java` | No — unused | Neither (unused) | Superseded: TileManaPodRenderer builds its own inner/outer fruit. |
| `models/ModelNodeStabilizer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileNodeConverterRenderer.java` |
| `models/ModelPech.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderPech.java` |
| `models/ModelResearchTable.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileResearchTableRenderer.java` |
| `models/ModelTable.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileTableRenderer.java` |
| `models/ModelTaintSpore.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintSpore.java` |
| `models/ModelTaintSporeSwarmer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintSporeSwarmer.java` |
| `models/ModelThaumatorium.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java` |
| `models/ModelThaumometer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` |
| `models/ModelTrunk.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderTravelingTrunk.java` |
| `models/ModelTubeValve.java` | No — unused | Neither (unused) | Superseded: TileTubeRenderer.bakeStem builds the valve geometry. |
| `models/ModelTunnelField.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/LinkedMirrorField.java` |
| `models/ModelVisRelay.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileMagicWorkbenchChargerRenderer.java` |
| `models/entities/ModelEldritchCrab.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchCrab.java` |
| `models/entities/ModelEldritchGolem.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchGolem.java` |
| `models/entities/ModelGolemAccessories.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java` |
| `models/entities/ModelRendererTaintacle.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintacle.java` |
| `models/entities/ModelTaintSeed.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintSeed.java` |
| `models/entities/ModelTaintSheep1.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintSheep.java` |
| `models/entities/ModelTaintSheep2.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintSheep.java` |
| `models/entities/ModelTaintacle.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderTaintacle.java` |
| `models/entities/ModelWatcher.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/entity/RenderWatcher.java` |
| `models/gear/HarnessArmorRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `fabric/src/main/java/thaumcraft/fabric/client/FabricTravelGearClient.java` |
| `models/gear/ModelFortressArmor.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `models/gear/ModelGearArmor.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `models/gear/ModelHoverHarness.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `models/gear/ModelKnightArmor.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `models/gear/ModelLeaderArmor.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `models/gear/ModelRobe.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `models/gear/ModelWand.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java` |
| `tile/ItemJarNodeRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/ItemNodeRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/LinkedMirrorField.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/MirrorProjection.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/LinkedMirrorField.java` |
| `tile/ThaumatoriumPose.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java` |
| `tile/TileAlchemyFurnaceAdvancedRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileAlembicRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftAlembicClient.java` |
| `tile/TileArcaneBoreBaseRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileArcaneBoreRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileArcaneLampRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileArcaneWorkbenchRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftArcaneClient.java` |
| `tile/TileBannerRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileBellowsRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftBellowsClient.java` |
| `tile/TileBrainboxRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileCentrifugeRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileChestHungryRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftHungryChestClient.java` |
| `tile/TileCrucibleRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftAlchemyClient.java` |
| `tile/TileCrystalRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileDeconstructionTableRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileEldritchCapRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchRenderer.java` |
| `tile/TileEldritchCrabSpawnerRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchRenderer.java` |
| `tile/TileEldritchCrystalRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchRenderer.java` |
| `tile/TileEldritchLockRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchRenderer.java` |
| `tile/TileEldritchNothingRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileEldritchObeliskRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchRenderer.java` |
| `tile/TileEldritchPortalRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileEldritchRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileEssentiaCrystalizerRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileEssentiaLampRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftBiologicalLampClient.java` |
| `tile/TileEssentiaReservoirRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftReservoirClient.java` |
| `tile/TileEtherealBloomRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileFluxScrubberRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileFocalManipulatorRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftFocalClient.java` |
| `tile/TileHoleRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileInfusionMatrixRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftInfusionClient.java` |
| `tile/TileInfusionPillarRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftInfusionClient.java` |
| `tile/TileJarBrainRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileJarNodeRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileJarRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileLevitatorRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileMagicWorkbenchChargerRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileManaPodRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftPechClient.java` |
| `tile/TileMirrorRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileNodeConverterRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileNodeEnergizedRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileNodeRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftNodeClient.java` |
| `tile/TileNodeStabilizerRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TilePedestalRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftInfusionClient.java` |
| `tile/TileResearchTableRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileTableRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |
| `tile/TileThaumatoriumRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftAlchemyClient.java` |
| `tile/TileTubeBufferRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileTubeRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileVisRelayRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java` |
| `tile/TileWandPedestalRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileWardedGlassRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ThaumcraftEldritchBlocksClient.java` |
| `tile/TileWardedRenderer.java` | Yes / indirect | Both (shared / platform-adapted) | `common/src/main/java/thaumcraft/client/ClientSetup.java` |

## Retained legacy tree (not loaded by either modern loader)

`src/main` is retained 1.12 reference, intentionally not compiled. Its files must not be mistaken for unregistered modern content; none were changed.

18 direct builtin legacy assets; 181 legacy renderer/helper files.

| Asset/class | Registered? | Loader | Finding |
|---|---|---|---|
| `src/main/resources/assets/thaumcraft/models/item/blockairy.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockchesthungry_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockcrystal_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockeldritch_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockessentiareservoir_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockjar.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockmetaldevice_dynamic_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockmetaldevice_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockstonedevice_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blocktable_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blocktube_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockwoodendevice_banner_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockwoodendevice_bellows_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/blockwoodendevice_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/itembowbone_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/itemthaumometer_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/trunkspawner_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/resources/assets/thaumcraft/models/item/wandcasting_tesr.json` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/block/AlchemyFurnaceBakedModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/block/ArcaneFurnaceBakedModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/block/EldritchCrustBakedModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/block/ObsidianTotemBakedModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/block/WardedGlassBakedModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderAlumentum.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderAspectOrb.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderBrainyZombie.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderCultist.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderCultistPortal.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderDart.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderEldritchCrab.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderEldritchGolem.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderEldritchGuardian.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderEldritchOrb.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderEldritchWarden.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderElectricOrb.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderEmber.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderExplosiveOrb.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderFallbackBiped.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderFallbackLiving.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderFallingTaint.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderFireBat.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderFollowingItem.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderFrostShard.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderGolemBase.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderGolemBobber.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderInhabitedZombie.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderMindSpider.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderNoop.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderPech.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderPechBlast.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderPrimalArrow.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderPrimalOrb.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderSpecialItem.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintChicken.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintCow.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintCreeper.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintPig.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintSheep.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintSpider.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintSpore.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintSporeSwarmer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintSwarm.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintVillager.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTaintacle.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderThaumicSlime.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderTravelingTrunk.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderWandEntityItem.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderWatcher.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/entity/RenderWisp.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/CrystalPerspectiveModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemBowBoneRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemChestHungryRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemCrystalRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemEldritchRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemEssentiaReservoirRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemJarRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemMetalDeviceRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemNodeRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemStoneDeviceRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemTableRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemTrunkSpawnerRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemTubeRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemWandRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ItemWoodenDeviceRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/LifterItemColor.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/ThaumometerPerspectiveModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/TrunkSpawnerPerspectiveModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/WandPerspectiveModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/WandPoseMath.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/WandRenderCalibration.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/WandUsePose.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/WandUsePoseSampler.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/item/WoodenDevicePerspectiveModel.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelAlembic.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelArcaneWorkbench.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelBanner.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelBellows.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelBore.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelBoreBase.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelBoreEmit.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelBrain.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelCentrifuge.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelCrabVent.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelCrystal.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelCrystalizer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelCube.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelEldritchCap.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelEldritchCrystal.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelFluxScrubber.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelInfusionPillar.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelJar.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelManaPod.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelNodeStabilizer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelResearchTable.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelTable.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelThaumatorium.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelTubeValve.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/ModelVisRelay.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelEldritchCrab.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelEldritchGolem.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelFireBat.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelGolem.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelGolemAccessories.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelPech.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelRendererTaintacle.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintSheep1.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintSheep2.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintSpore.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintSporeSwarmer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelTaintacle.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelTrunk.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/entities/ModelWatcher.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/gear/ModelFortressArmor.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/gear/ModelHoverHarness.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/gear/ModelKnightArmor.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/gear/ModelLeaderArmor.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/gear/ModelRobe.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/models/gear/ModelWand.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/ExtrudedSpriteRenderHelper.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/HoleRenderBatchCache.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileAlchemyFurnaceAdvancedRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileAlembicRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileArcaneBoreBaseRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileArcaneBoreRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileArcaneLampRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileArcanePressurePlateRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileArcaneWorkbenchRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileBannerRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileBellowsRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileBrainboxRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileCentrifugeRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileChestHungryRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileCrucibleRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileCrystalRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileDeconstructionTableRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEldritchCapRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEldritchCrabSpawnerRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEldritchCrystalRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEldritchLockRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEldritchObeliskRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEldritchPortalRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEssentiaCrystalizerRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileFocalManipulatorRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileHoleRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileInfusionPillarRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileJarRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileLifterRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileMagicWorkbenchChargerRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileManaPodRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileMirrorRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileNodeConverterRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileNodeEnergizedRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileNodeStabilizerRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TilePedestalRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileRenderHelper.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileResearchTableRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileRunicMatrixRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileSensorRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileTableRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileTubeBufferRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileTubeFilterRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileTubeOnewayRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileTubeRestrictRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileTubeValveRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileVisRelayRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileWandPedestalRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TileWardedRenderer.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |
| `src/main/java/thaumcraft/client/renderers/tile/TubeConduitRenderHelper.java` | N/A | Neither modern loader | Archived reference; outside modern compilation. |

Test-source files under `client/renderers` are not renderer registrations:

18 test files (not included in production-source counts).

* `common/src/test/java/thaumcraft/client/renderers/ClientDeviceModelParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/EldritchBlockModelTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/entity/ClientEntityRendererNumbersTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/entity/ClientModelGeometryTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/entity/EntityRendererCoverageTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/item/BoneBowGlintParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/item/ThaumometerLightmapParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/item/WandContextPoseParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/item/WandUsePoseParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/tile/FluxScrubberRendererParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/tile/ItemNodeRendererTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/tile/JarBrainTurnAxisParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/tile/JarRendererParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/tile/LevitatorGlowParityTest.java` — test only, no loader registration needed.
* `common/src/test/java/thaumcraft/client/renderers/tile/MirrorProjectionParityTest.java` — test only, no loader registration needed.
* `src/test/java/thaumcraft/client/renderers/block/ArcaneFurnaceNozzleGeometryTest.java` — test only, no loader registration needed.
* `src/test/java/thaumcraft/client/renderers/item/WandUsePoseSamplerTest.java` — test only, no loader registration needed.
* `src/test/java/thaumcraft/client/renderers/models/ModelThaumatoriumUvParityTest.java` — test only, no loader registration needed.
