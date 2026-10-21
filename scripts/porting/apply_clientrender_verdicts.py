#!/usr/bin/env python3
"""Apply the port/clientrender review verdicts to docs/porting/scope-ledger.json.

Only the units of the client render lane are touched. Every write keeps the
authored oracle/legacy fingerprints, so the ledger stays refreshable with
scripts/porting/update_scope_ledger.py.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEDGER = ROOT / "docs/porting/scope-ledger.json"
OWNER = "port-clientrender"

# path -> (native_coverage, source_semantics, disposition, evidence, notes)
VERDICTS: dict[str, tuple[str, str | None, str | None, list[str], list[str]]] = {
    # ---------------------------------------------------------------- oracle renderers
    "thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java": (
        "partial", None, None,
        ["oracle thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java:24-25 the inventory art shrinks the vessel to W2..W14",
         "oracle thaumcraft/client/renderers/block/BlockEssentiaReservoirRenderer.java:33-42 the world block is the plain cube",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java the contents of the tank",
         "native common/src/main/resources/assets/thaumcraft/models/block/essentia_reservoir.json the vessel",
         "native common/src/main/resources/assets/thaumcraft/models/item/essentia_reservoir.json the item"],
        ["The world cube and the tank contents are covered; the original shrinks the vessel in the inventory slot to 2/16..14/16, the native item model shows the full cube.",
         "Callsite audit: the class is registered in oracle thaumcraft/client/ClientProxy.java only."]),
    "thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java": (
        "parity_verified", None, None,
        ["oracle thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java:38-56 the fibre planes sit on 0.995 and the glow planes on 0.98",
         "oracle thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java:57-59 the glow planes use the lightmap coordinate 200",
         "oracle thaumcraft/client/renderers/block/BlockTaintFibreRenderer.java:85-95 the crossed stalks use the deterministic grass offset",
         "native fabric/src/main/java/thaumcraft/fabric/client/FabricTaintFibresModel.java the 0.995 and 0.98 planes, the lightmap 200 and the crossed stalks"],
        ["The native model reproduces the offsets, the lightmap coordinate, the overlay faces and the deterministic stalk offset of the original.",
         "Callsite audit: the class is registered in oracle thaumcraft/client/ClientProxy.java only."]),
    "thaumcraft/client/renderers/block/BlockTaintRenderer.java": (
        "partial", None, None,
        ["oracle thaumcraft/client/renderers/block/BlockTaintRenderer.java:26-27 the inventory art uses one taint texture per metadata",
         "oracle thaumcraft/client/renderers/block/BlockTaintRenderer.java:37-58 the tainted soil cube and the fibre overlay faces",
         "native common/src/main/resources/assets/thaumcraft/models/block/taint_soil.json the tainted soil cube with the biome tint",
         "native fabric/src/main/java/thaumcraft/fabric/client/FabricTaintFibresModel.java the fibre overlay of the tainted soil"],
        ["The cube and the fibre overlay are covered; the original picks one of three soil textures per metadata, the native uses one cube for all of them.",
         "Callsite audit: the class is registered in oracle thaumcraft/client/ClientProxy.java only."]),
    "thaumcraft/client/renderers/block/BlockTubeRenderer.java": (
        "parity_verified", None, None,
        ["oracle thaumcraft/client/renderers/block/BlockTubeRenderer.java:53-235 the world arms of the tube and the central core",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:28-33 the class comment cites the original lines",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:62-104 the arms, the core and the filter colour"],
        ["The native renderer reproduces the arms of the original, the W6/W7/W9/W10 bounds and the aspect colour of the filter.",
         "Callsite audit: the class is registered in oracle thaumcraft/client/ClientProxy.java only."]),
    "thaumcraft/client/renderers/item/ItemBannerRenderer.java": (
        "parity_verified", "adapted", None,
        ["oracle thaumcraft/client/renderers/item/ItemBannerRenderer.java:27-32 the custom renderer only claims the banner with a colour and an aspect",
         "oracle thaumcraft/client/renderers/item/ItemBannerRenderer.java:43-50 the transform chain of the item",
         "oracle thaumcraft/client/renderers/tile/TileBannerRenderer.java:31-74 the banner model, the dye colour and the aspect icon",
         "native common/src/main/java/thaumcraft/client/renderers/item/ItemBannerRenderer.java the new native item renderer",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileBannerRenderer.java:73-118 the banner drawing shared by the block and the item",
         "native common/src/main/resources/assets/thaumcraft/models/item/banner.json the builtin item model",
         "native fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java the registration of the builtin renderer"],
        ["The banner item now carries the colour and the aspect of the original and is drawn with the original banner model.",
         "The 1.7.10 GUI item pipeline that framed the original item does not exist in 1.21.1, so the item is centred in the slot with the numbers derived from the original chain; the geometry, the colours and the aspect icon are the original ones."]),
    "thaumcraft/client/renderers/item/ItemThaumometerRenderer.java": (
        "partial", "adapted", None,
        ["oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:74-75 the scanner OBJ and its sheet",
         "oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:98-146 the transform chain of the three views",
         "oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:148-154 the body and the glowing scan screen",
         "oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:113-131 the mirrored arm of the player",
         "oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:155-259 the scan readout with the aspect icons and the name",
         "native common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java the new native item renderer",
         "native common/src/main/java/thaumcraft/client/renderers/models/ModelThaumometer.java the original scanner OBJ",
         "native common/src/main/resources/assets/thaumcraft/models/item/thaumometer.json the builtin item model"],
        ["The thaumometer was a flat sprite before; it is now the three dimensional scanner of the original with its glowing screen.",
         "Two parts of the original stay open: the two rotations that follow the swing of the arm of the player need the private arm angles of 1.7.10, and the scan readout needs the client side of the research system, which the native port keeps on the server."]),
    "thaumcraft/client/renderers/models/ModelVoidRobe.java": (
        "parity_verified", None, "exclude",
        ["oracle thaumcraft/client/renderers/models/ModelVoidRobe.java the class exists in the TC4 4.2.3.5 oracle",
         "oracle tree-wide reference scan: ModelVoidRobe has ZERO references outside its own file",
         "oracle thaumcraft/common/items/armor/ItemVoidRobeArmor.java:155-158 the void robe uses gear/ModelRobe, not ModelVoidRobe",
         "native common/src/main/java/thaumcraft/client/renderers/models/gear/ModelRobe.java the native gear model of the robe"],
        ["Dead code in the original: the void robe is drawn with the cultist robe model, which the native port carries.",
         "Complete callsite audit: zero external references in the oracle tree."]),
    # ---------------------------------------------------------------- shared path renderers
    "thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java": (
        "parity_verified", None, None,
        ["oracle thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java:61-90 the contents of the tank",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java:14-24 the original numbers",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java:40-46 the fill of units/256, the alpha 0.9 and the lightmap 200"],
        ["The native renderer keeps the three sixteenths to thirteen sixteenths box, the fill of units/256, the alpha 0.9 and the fixed lightmap coordinate 200.",
         "Callsite audit: the class is registered in oracle thaumcraft/client/ClientProxy.java only."]),
    "thaumcraft/client/renderers/tile/TileTubeOnewayRenderer.java": (
        "parity_verified", None, None,
        ["oracle thaumcraft/client/renderers/tile/TileTubeOnewayRenderer.java:39-49 the three valve rods in the colour 0.45/0.5/1.0 with the scale 1.1/0.5/1.1",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:137-153 the native oneway rods"],
        ["The native renderer draws the same three rods with the same scale, the same local travel of a quarter block and the nearest ARGB colour of the original.",
         "Callsite audit: the class is registered in oracle thaumcraft/client/ClientProxy.java only."]),
    "thaumcraft/client/renderers/tile/TileTubeValveRenderer.java": (
        "parity_verified", None, None,
        ["oracle thaumcraft/client/renderers/tile/TileTubeValveRenderer.java:31-48 the valve stem, the wheel and the rotation of the valve",
         "oracle thaumcraft/client/renderers/tile/TileTubeValveRenderer.java:52-67 the wheel is drawn with the original ItemRenderer.renderItemIn2D and a depth of 0.1",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:155-170 the native valve",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:192-223 the native wheel with the depth of 0.1"],
        ["The native renderer keeps the original rotation of the valve, the 0.12 block travel and the extruded wheel with its half texel sampling.",
         "Callsite audit: the class is registered in oracle thaumcraft/client/ClientProxy.java only."]),
    # ---------------------------------------------------------------- legacy only renderers
    "thaumcraft/client/renderers/entity/RenderFallbackBiped.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/entity/RenderFallbackBiped.java the 1.12 helper",
         "native common/src/main/java/thaumcraft/client/renderers/entity/RenderFallbackBiped.java the native fallback renderer",
         "oracle tree-wide reference scan: the oracle has no RenderFallbackBiped at all"],
        ["The class has no counterpart in the 1.7.10 original, so no original behaviour depends on it; the native keeps an equivalent helper."]),
    "thaumcraft/client/renderers/entity/RenderFallbackLiving.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/entity/RenderFallbackLiving.java the 1.12 helper",
         "native common/src/main/java/thaumcraft/client/renderers/entity/RenderFallbackLiving.java the native fallback renderer",
         "oracle tree-wide reference scan: the oracle has no RenderFallbackLiving at all"],
        ["The class has no counterpart in the 1.7.10 original, so no original behaviour depends on it; the native keeps an equivalent helper."]),
    "thaumcraft/client/renderers/entity/RenderNoop.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/entity/RenderNoop.java the 1.12 helper",
         "native common/src/main/java/thaumcraft/client/renderers/entity/RenderNoop.java the native renderer that draws nothing",
         "oracle tree-wide reference scan: the oracle has no RenderNoop at all"],
        ["The class has no counterpart in the 1.7.10 original, so no original behaviour depends on it; the native keeps an equivalent helper."]),
    "thaumcraft/client/renderers/item/CrystalPerspectiveModel.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/CrystalPerspectiveModel.java the 1.12 perspective model",
         "native common/src/main/resources/assets/thaumcraft/models/item/crystal_air.json the native item model with its display transform",
         "native fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java the builtin item renderer of the crystals"],
        ["The 1.12 model registry has no counterpart in 1.21.1: the native keeps the crystal geometry in the item model and in the builtin item renderer of the crystals."]),
    "thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java": (
        "missing", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java the 1.12 helper for the origin of the wand beam",
         "legacy src/main/java/thaumcraft/client/fx/beams/FXBeamWand.java the 1.12 beam effect",
         "oracle thaumcraft/client/fx/beams/FXBeamWand.java the original beam effect",
         "native common/src/main/java/thaumcraft/client/ the native tree has no wand beam origin"],
        ["Open gap: the native port draws no beam from the wand tip, so the origin helper has no counterpart.",
         "The beam itself belongs to the client effect lane, not to the renderer lane of this review."]),
    "thaumcraft/client/renderers/item/ItemChestHungryRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemChestHungryRenderer.java the 1.12 item renderer",
         "native fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java the builtin item renderer of the hungry chest",
         "native common/src/main/resources/assets/thaumcraft/models/item/hungry_chest.json the builtin item model"],
        ["The hungry chest item draws the block entity of the hungry chest in the native port, which is the original art."]),
    "thaumcraft/client/renderers/item/ItemCrystalRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemCrystalRenderer.java the 1.12 item renderer",
         "native fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java the builtin item renderer of the crystals",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileCrystalRenderer.java:41-48 the native crystal item"],
        ["The crystal clusters are drawn by the native builtin item renderer of the crystals, which keeps the original cluster geometry."]),
    "thaumcraft/client/renderers/item/ItemEssentiaReservoirRenderer.java": (
        "partial", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemEssentiaReservoirRenderer.java the 1.12 item renderer",
         "native common/src/main/resources/assets/thaumcraft/models/item/essentia_reservoir.json the native item model",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java the tank contents"],
        ["The item shows the vessel and its contents; the 1.12 renderer shrank the vessel, the native item model shows the full cube."]),
    "thaumcraft/client/renderers/item/ItemMetalDeviceRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemMetalDeviceRenderer.java the 1.12 item renderer",
         "native common/src/main/resources/assets/thaumcraft/models/item/metal_device.json the native item model",
         "native common/src/main/resources/assets/thaumcraft/models/block/metal_device.json the native block model"],
        ["The metal device is drawn by its block model in the native port, which is the original art of the device family."]),
    "thaumcraft/client/renderers/item/ItemNodeRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemNodeRenderer.java the 1.12 item renderer",
         "native common/src/main/resources/assets/thaumcraft/models/item/airy_node.json the native item model of the nodes",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java the native node renderer"],
        ["The node items are drawn by their block models in the native port, which is the original art of the node family."]),
    "thaumcraft/client/renderers/item/ItemStoneDeviceRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemStoneDeviceRenderer.java the 1.12 item renderer",
         "native common/src/main/resources/assets/thaumcraft/models/item/stone_device.json the native item model",
         "native common/src/main/resources/assets/thaumcraft/models/block/stone_device.json the native block model"],
        ["The stone device is drawn by its block model in the native port, which is the original art of the device family."]),
    "thaumcraft/client/renderers/item/ItemTableRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemTableRenderer.java the 1.12 item renderer",
         "native common/src/main/resources/assets/thaumcraft/models/item/table.json the native item model of the table",
         "native common/src/main/resources/assets/thaumcraft/models/item/research_table.json the native item model of the research table",
         "native common/src/main/resources/assets/thaumcraft/models/item/deconstruction_table.json the native item model of the deconstruction table",
         "native common/src/main/resources/assets/thaumcraft/models/item/arcane_workbench.json the native item model of the arcane workbench"],
        ["The four tables of the original are drawn by their block models with the native display transforms in the native port."]),
    "thaumcraft/client/renderers/item/ItemTubeRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemTubeRenderer.java the 1.12 item renderer",
         "native common/src/main/resources/assets/thaumcraft/models/item/essentia_tube.json the native item model of the tube",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:109-135 the native item art of the valve, filter, restrict and oneway",
         "native fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java the registrations of the tube items"],
        ["The native port draws the basic tube with its block model and the four advanced tubes with the builtin item renderer, which keeps the original inventory art of oracle BlockTubeRenderer:39-64."]),
    "thaumcraft/client/renderers/item/ItemWoodenDeviceRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ItemWoodenDeviceRenderer.java the 1.12 item renderer",
         "native common/src/main/resources/assets/thaumcraft/models/item/wooden_device.json the native item model",
         "native common/src/main/resources/assets/thaumcraft/models/item/arcane_bore.json the native item model of the arcane bore",
         "native fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java the builtin item renderers of the bore"],
        ["The wooden device family is drawn by its block models and by the builtin item renderers of the bore in the native port."]),
    "thaumcraft/client/renderers/item/LifterItemColor.java": (
        "partial", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/LifterItemColor.java the 1.12 item colour of the lifter",
         "oracle thaumcraft/common/blocks/BlockCustomOreItem.java:17 the original colour table of the ores",
         "oracle thaumcraft/client/renderers/block/BlockLifterRenderer.java:26-40 the original inventory art tints the levitator with colours 4 and 5",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileLevitatorRenderer.java the glowing faces of the levitator"],
        ["The glowing faces of the levitator are now drawn in the world; the inventory art of the levitator is not tinted with the original ore colours, the native item model shows the plain block."]),
    "thaumcraft/client/renderers/item/ThaumometerPerspectiveModel.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/ThaumometerPerspectiveModel.java the 1.12 perspective model",
         "native common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java the native item renderer of the thaumometer"],
        ["The 1.12 model registry has no counterpart in 1.21.1; the native draws the original three dimensional scanner."]),
    "thaumcraft/client/renderers/item/WandPerspectiveModel.java": (
        "partial", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/WandPerspectiveModel.java the 1.12 perspective model of the wand",
         "native common/src/main/resources/assets/thaumcraft/models/item/wand.json the native item model of the wand",
         "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java the original three dimensional wand"],
        ["The 1.21.1 item model carries the display transform that the 1.12 perspective model provided.",
         "The wand itself is still drawn with the flat item model of the native port instead of the three dimensional ModelWand of the original; that belongs to the ItemWandRenderer unit of another lane."]),
    "thaumcraft/client/renderers/item/WandPoseMath.java": (
        "missing", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/WandPoseMath.java the 1.12 CPU counterpart of the wand pose",
         "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:63-93 the original raises the wand while the player uses an item",
         "native common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java the native wand renderer without the use pose"],
        ["Open gap: the native port does not raise the wand while the player uses a focus, so the pose has no counterpart.",
         "The wand item renderer itself belongs to the ItemWandRenderer unit of another lane."]),
    "thaumcraft/client/renderers/item/WandRenderCalibration.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/WandRenderCalibration.java the 1.12 calibration table",
         "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java the original wand renderer without a calibration table"],
        ["The calibration table has no counterpart in the 1.7.10 original; the native port needs no calibration."]),
    "thaumcraft/client/renderers/item/WandUsePose.java": (
        "missing", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/WandUsePose.java the 1.12 pose of the used wand",
         "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:63-93 the original raises the wand while the player uses an item",
         "native common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java the native wand renderer without the use pose"],
        ["Open gap: the native port does not raise the wand while the player uses a focus, so the pose has no counterpart.",
         "The wand item renderer itself belongs to the ItemWandRenderer unit of another lane."]),
    "thaumcraft/client/renderers/item/WandUsePoseSampler.java": (
        "missing", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/WandUsePoseSampler.java the 1.12 sampler of the wand pose",
         "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:63-93 the original raises the wand while the player uses an item",
         "native common/src/main/java/thaumcraft/client/ThaumcraftGearModelsClient.java the native wand renderer without the use pose"],
        ["Open gap: the native port does not raise the wand while the player uses a focus, so the sampler has no counterpart.",
         "The wand item renderer itself belongs to the ItemWandRenderer unit of another lane."]),
    "thaumcraft/client/renderers/item/WoodenDevicePerspectiveModel.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/item/WoodenDevicePerspectiveModel.java the 1.12 perspective model",
         "native common/src/main/resources/assets/thaumcraft/models/item/wooden_device.json the native item model of the wooden device"],
        ["The 1.12 model registry has no counterpart in 1.21.1; the native keeps the wooden device in its item model."]),
    "thaumcraft/client/renderers/tile/ExtrudedSpriteRenderHelper.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/ExtrudedSpriteRenderHelper.java the 1.12 helper",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:188-223 the native extrusion of a sprite with the depth of 0.1"],
        ["The 1.21.1 item models extrude their sprites themselves; the native renderer keeps its own extrusion for the valve wheel."]),
    "thaumcraft/client/renderers/tile/HoleRenderBatchCache.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/HoleRenderBatchCache.java the 1.12 batch cache of the portable hole",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileHoleRenderer.java the native renderer of the portable hole"],
        ["The 1.12 helper batches the quads of the hole; the native renderer writes its quads into the native buffers directly."]),
    "thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java the 1.12 helper of the layered field planes",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileHoleRenderer.java the native renderer of the portable hole"],
        ["The 1.12 helper belongs to the 1.12 tunnel effect of the portable hole; the native renderer draws the hole with its own buffers."]),
    "thaumcraft/client/renderers/tile/TileLifterRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/TileLifterRenderer.java the 1.12 renderer of the lifter",
         "oracle thaumcraft/client/renderers/block/BlockLifterRenderer.java:66-92 the original glowing faces",
         "oracle thaumcraft/common/blocks/BlockCustomOreItem.java:17 the original colour table",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileLevitatorRenderer.java the new native renderer of the levitator",
         "native common/src/main/java/thaumcraft/client/ClientSetup.java the registration of the block entity renderer"],
        ["The native renderer draws the glowing faces of the levitator with the original colours, the original offsets of one hundredth of a block and the original lightmap coordinate 180."]),
    "thaumcraft/client/renderers/tile/TileRenderHelper.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/TileRenderHelper.java the 1.12 helper of the textured faces",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileLevitatorRenderer.java the native quad helper"],
        ["The 1.12 helper writes into the 1.12 buffers; the native renderers write their faces into the native buffers themselves."]),
    "thaumcraft/client/renderers/tile/TileTubeFilterRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/TileTubeFilterRenderer.java the 1.12 renderer of the filter tube",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:89-94 the filter and its aspect colour"],
        ["The native tube renderer draws the filter with the colour of its aspect, which is the original behaviour of oracle BlockTubeRenderer:158-168."]),
    "thaumcraft/client/renderers/tile/TileTubeRestrictRenderer.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/TileTubeRestrictRenderer.java the 1.12 renderer of the restrict tube",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java:95-100 the core of the restrict tube"],
        ["The native tube renderer draws the restrict core with the original sheet, which is the original behaviour of oracle BlockTubeRenderer:139-140."]),
    "thaumcraft/client/renderers/tile/TubeConduitRenderHelper.java": (
        "parity_verified", None, None,
        ["legacy src/main/java/thaumcraft/client/renderers/tile/TubeConduitRenderHelper.java the 1.12 helper of the conduits",
         "native common/src/main/java/thaumcraft/client/renderers/tile/TileTubeRenderer.java the native tube renderer",
         "native common/src/main/java/thaumcraft/common/lib/essentia/EssentiaTransfers.java the native connection rules"],
        ["The 1.12 helper draws the conduits of the tube family; the native tube renderer draws the arms with the native connection rules."]),
    # ---------------------------------------------------------------- fonts
    "thaumcraft/client/lib/TCFontRenderer.java": (
        "partial", "different", None,
        ["oracle thaumcraft/client/lib/TCFontRenderer.java:38-96 a copy of the vanilla font renderer with the extra atlas textures/font/ascii_sga.png",
         "oracle thaumcraft/client/gui/GuiResearchRecipe.java:175 the only instantiation: new TCFontRenderer(gameSettings, TCFontRenderer.FONT_NORMAL, textureManager, true)",
         "oracle tree-wide reference scan: TCFontRenderer is instantiated only in GuiResearchRecipe",
         "native common/src/main/java/thaumcraft/client/gui/GuiResearchRecipe.java:104-120 the native research book draws its text with the vanilla 1.21.1 font",
         "native common/src/main/resources/assets/thaumcraft/ the native tree has no textures/font/ascii_sga.png"],
        ["The research book draws its text with the vanilla font of 1.21.1, so the galactic atlas of the original is not available.",
         "The original never used the galactic atlas either - FONT_GALACTIC is defined but never used - but the glyph shapes of the vanilla font changed between 1.7.10 and 1.21.1, so the original look of the research book is not reproduced.",
         "Complete callsite audit: TCFontRenderer is instantiated only in GuiResearchRecipe."]),
    "truetyper/FontHelper.java": (
        "parity_verified", None, "exclude",
        ["oracle truetyper/FontHelper.java the class exists in the TC4 4.2.3.5 decompilation",
         "oracle tree-wide reference scan: FontHelper has ZERO references outside its own file",
         "native common/src/main/java/thaumcraft/ the native tree has no truetyper package"],
        ["Dead code in the original: the bundled font helper is never referenced, so the native omits it without changing any picture.",
         "Complete callsite audit: zero external references in the oracle tree."]),
    "truetyper/FontLoader.java": (
        "parity_verified", None, "exclude",
        ["oracle truetyper/FontLoader.java:16-31 the loader of the system font",
         "oracle thaumcraft/client/lib/RenderEventHandler.java:139-141 the original loads Arial 12 once",
         "oracle thaumcraft/client/lib/RenderEventHandler.java:112-140 the loaded font is never used to draw",
         "native common/src/main/java/thaumcraft/client/ the native tree has no truetyper package"],
        ["The original loads the system font once in its overlay handler and never draws with it, so the native omits the load without changing any picture.",
         "Complete callsite audit: the only reference is the unused field of RenderEventHandler."]),
    "truetyper/Formatter.java": (
        "parity_verified", None, "exclude",
        ["oracle truetyper/Formatter.java the colour table of the format codes",
         "oracle tree-wide reference scan: Formatter is referenced only by truetyper/FontHelper.java",
         "native common/src/main/java/thaumcraft/ the native tree has no truetyper package"],
        ["Dead code in the original: the formatter is only used by the dead font helper, so the native omits it.",
         "Complete callsite audit: zero external references outside the dead helper."]),
    "truetyper/TrueTypeFont.java": (
        "parity_verified", None, "exclude",
        ["oracle truetyper/TrueTypeFont.java the bundled raster font",
         "oracle tree-wide reference scan: TrueTypeFont is referenced by the dead helper, by the loader and by the unused field of RenderEventHandler",
         "oracle thaumcraft/client/lib/RenderEventHandler.java:112-140 the loaded font is never used to draw",
         "native common/src/main/java/thaumcraft/ the native tree has no truetyper package"],
        ["The original never draws with the bundled raster font, so the native omits it without changing any picture.",
         "Complete callsite audit: every reference is either the dead helper or the unused font field."]),
    "thaumcraft/truetyper/FontHelper.java": (
        "parity_verified", None, "exclude",
        ["legacy src/main/java/thaumcraft/truetyper/FontHelper.java the 1.12 CodeChicken helper",
         "oracle tree-wide reference scan: the TC4 4.2.3.5 oracle has no thaumcraft/truetyper package"],
        ["The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one."]),
    "thaumcraft/truetyper/FontLoader.java": (
        "parity_verified", None, "exclude",
        ["legacy src/main/java/thaumcraft/truetyper/FontLoader.java the 1.12 CodeChicken helper",
         "oracle tree-wide reference scan: the TC4 4.2.3.5 oracle has no thaumcraft/truetyper package"],
        ["The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one."]),
    "thaumcraft/truetyper/Formatter.java": (
        "parity_verified", None, "exclude",
        ["legacy src/main/java/thaumcraft/truetyper/Formatter.java the 1.12 CodeChicken helper",
         "oracle tree-wide reference scan: the TC4 4.2.3.5 oracle has no thaumcraft/truetyper package"],
        ["The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one."]),
    "thaumcraft/truetyper/TrueTypeFont.java": (
        "parity_verified", None, "exclude",
        ["legacy src/main/java/thaumcraft/truetyper/TrueTypeFont.java the 1.12 CodeChicken helper",
         "oracle tree-wide reference scan: the TC4 4.2.3.5 oracle has no thaumcraft/truetyper package"],
        ["The package is a 1.12 only addition of the legacy reference; the 1.7.10 original has no such package and the native port does not need one."]),
}


def main() -> None:
    data = json.loads(LEDGER.read_text(encoding="utf-8"))
    units = {row["canonical_path"]: row for row in data["units"]}
    missing = sorted(set(VERDICTS) - set(units))
    if missing:
        raise SystemExit(f"Unknown ledger rows: {missing}")
    for path, (coverage, semantics, disposition, evidence, notes) in VERDICTS.items():
        review = units[path]["review"]
        review["native_coverage"] = coverage
        if semantics is not None:
            review["source_semantics"] = semantics
        if disposition is not None:
            review["disposition"] = disposition
        review["owner"] = OWNER
        for line in evidence:
            if line not in review["evidence"]:
                review["evidence"].append(line)
        for line in notes:
            if line not in review["notes"]:
                review["notes"].append(line)
    LEDGER.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"updated": len(VERDICTS)}))


if __name__ == "__main__":
    main()
