#!/usr/bin/env python3
"""Update the review of the eleven legacy-only units of the w5legacy wave.

Every one of the eleven is a class the original TC4 4.2.3.5 never had: a 1.12
helper, a 1.12 only development instrument, or an integration target that does
not exist in 1.21.1. The disposition stays ``exclude``; the native coverage
moves from ``missing``/``not_assessed`` to ``parity_verified``, the way the
sibling rows ``ThaumcraftJeiPlugin``, ``ResearchVisibility`` and
``ResearchVisibilityTicker`` were closed by the w4research wave.

Only the rows of this lane are touched; the JSON shape, the key order and the
formatting of the ledger are preserved exactly (json.dumps(..., indent=2) plus
the final newline, as scripts/porting/update_scope_ledger.py writes it).
"""
from __future__ import annotations

import json
from pathlib import Path

LEDGER = Path("docs/porting/scope-ledger.json")

ORACLE_TREE = "oracle tree: the private 4.2.3.5 oracle has no class named {name} (grep -rl '{name}' over the 859 oracle files: 0 hits)"


def add_evidence(entry, items):
    """Append the evidence that is not in the row yet; the script may run twice."""
    evidence = entry["review"]["evidence"]
    for item in items:
        if item not in evidence:
            evidence.append(item)


def row(rows, path):
    for entry in rows:
        if entry["canonical_path"] == path:
            return entry
    raise KeyError(path)


def main() -> None:
    data = json.loads(LEDGER.read_text(encoding="utf-8"))
    rows = data["units"]

    # ------------------------------------------------- the beam origin helper
    origin = row(rows, "thaumcraft/client/fx/beams/WandEffectOrigin.java")
    origin["review"]["native_coverage"] = "parity_verified"
    add_evidence(origin, [
        ORACLE_TREE.format(name="WandEffectOrigin"),
        "oracle tree: the directory thaumcraft/client/fx/beams/ of the oracle holds exactly six files (FXArc, FXBeam, FXBeamBore, FXBeamGolemBoss, FXBeamPower, FXBeamWand) and no WandEffectOrigin",
        "oracle grep TipCalibration / debugFrame: no file of the 4.2.3.5 tree mentions the per staff/sceptre/wand calibration table or the debug frame of this 1.12 helper",
        "oracle thaumcraft/client/fx/beams/FXBeamWand.java:37,58-62 the beam of the original starts at the player itself: the offset field is 0 for the local player and height/2 + 0.25 for a remote one",
        "oracle thaumcraft/client/fx/beams/FXBeamWand.java:209-228 the origin of the original is the position of the player, moved by the yaw and the pitch and by the look vector times 0.3 - it is NOT the tip of the wand",
        "native common/src/main/java/thaumcraft/common/items/wands/foci/ItemFocusExcavation.java:162-163 the native origin of the beam is the very same one (the eye, the yaw, the pitch and the look times 0.3)",
        "native grep wandTip/tipOrigin over common/src, fabric/src and neoforge/src: no reference at all, so no native code expects this helper",
        "legacy callsite audit: the only callers of the 1.12 helper are the 1.12 tree itself (src/main/java/thaumcraft/client/fx/beams/FXBeamWand.java:115,119-120,180; src/main/java/thaumcraft/client/ClientProxy.java:1926-1927; src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java:23)",
    ])
    origin["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The private oracle has no WandEffectOrigin, no calibration table and no debug frame; it exists only in the retained 1.12 reference tree, where it feeds the 1.12 FXBeamWand, the 1.12 ClientProxy and the 1.12 TileNodeRenderer.",
        "STALE NOTE CORRECTED 2026-10-23 (w5legacy): the old note said the native beam starts at the player while the original drew it from the wand tip. The premise is false. The original starts the beam at the player itself: oracle FXBeamWand:37,58-62 (the offset is 0 for the local player) and :209-228 (the position of the player moved by the yaw and the pitch and by the look vector times 0.3).",
        "The native already used exactly that origin (ItemFocusExcavation:162-163), and the FXBeamWand row is parity_verified since the w4wands wave, which reproduced the fan of the original. There is no wand tip origin left to port.",
        "The 1.12 helper is the ANIMATED wand tip of the 1.12 tree (a per staff/sceptre/wand calibration table and the use pose of the 1.12 ItemWandRenderer). The original has neither, and the 1.21.1 port has no reference to a wand tip at all.",
        "Nothing of the original is lost by the exclusion.",
    ]

    # ---------------------------------------------------------- the JEI holder
    jei_data = row(rows, "thaumcraft/client/integration/jei/JeiRecipeData.java")
    jei_data["review"]["native_coverage"] = "parity_verified"
    add_evidence(jei_data, [
        "oracle tree: the private 4.2.3.5 oracle has no thaumcraft/client/integration directory at all (find -type d -name 'integration*' over the oracle tree: 0 hits), so the original never published this class",
        ORACLE_TREE.format(name="JeiRecipeData"),
        "oracle grep 'mezz.jei' / IRecipeCategory / IRecipeWrapper / IRecipeLayout: 0 hits; the only recipe-viewer line of the original is oracle thaumcraft/client/ClientProxy.java:444 Loader.isModLoaded(\"NotEnoughItems\")",
        "legacy src/main/java/thaumcraft/client/integration/jei/JeiRecipeData.java:1-15 the 1.12 holder imports the 1.12 only mezz.jei API",
        "legacy callsite audit: the only caller is the 1.12 JEI plugin (src/main/java/thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java:45,50,56)",
        "native grep 'jei' over common/src, fabric/src and neoforge/src: no dependency and no integration package at all",
        "native common/src/main/java/thaumcraft/common/lib/research/ResearchRecipeCoverage.java:41 the coverage map of all 308 original recipe keys of the oracle ConfigRecipes.java; the recipes the 1.12 JEI plugin would have shown are the native recipe tree",
        "native common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java:587-607 the native draws the aspects of a recipe, the counterpart of the aspect row of the 1.12 wrapper",
    ])
    jei_data["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The private oracle has no thaumcraft/client/integration/jei directory at all, so the original never published it; it exists only in the retained 1.12 reference tree.",
        "The 1.21.1 port ships no JEI/EMI dependency, so the class is deliberately not carried over and no recipe-viewer integration exists.",
        "The recipe DATA the 1.12 plugin would have shown is covered: every one of the 308 original recipe keys of the oracle is a native recipe file, and the research book of the port shows them (native ResearchRecipeCoverage, native ThaumcraftRecipeDisplay).",
    ]

    # ------------------------------------------------------ the JEI category
    jei_category = row(rows, "thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java")
    jei_category["review"]["native_coverage"] = "parity_verified"
    add_evidence(jei_category, [
        "oracle tree: the private 4.2.3.5 oracle has no thaumcraft/client/integration directory at all (find -type d -name 'integration*' over the oracle tree: 0 hits), so the original never published this class",
        ORACLE_TREE.format(name="ThaumcraftRecipeCategory"),
        "oracle grep 'mezz.jei' / IRecipeCategory / IRecipeLayout: 0 hits; the only recipe-viewer line of the original is oracle thaumcraft/client/ClientProxy.java:444 Loader.isModLoaded(\"NotEnoughItems\")",
        "legacy src/main/java/thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java:1-11 the 1.12 category implements the 1.12 only mezz.jei.api.recipe.IRecipeCategory",
        "legacy callsite audit: the only caller is the 1.12 JEI plugin (src/main/java/thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java:33,35,37)",
        "native grep 'jei' over common/src, fabric/src and neoforge/src: no dependency and no integration package at all",
        "native common/src/main/java/thaumcraft/common/lib/research/ResearchRecipeCoverage.java:41 the coverage map of all 308 original recipe keys of the oracle ConfigRecipes.java; the recipes the 1.12 JEI plugin would have shown are the native recipe tree",
        "native common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java the native recipe display that shows those recipes in the research book",
    ])
    jei_category["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The private oracle has no thaumcraft/client/integration/jei directory at all, so the original never published it; it exists only in the retained 1.12 reference tree.",
        "The 1.21.1 port ships no JEI/EMI dependency, so the class is deliberately not carried over and no recipe-viewer integration exists.",
        "The recipe DATA the 1.12 plugin would have shown is covered: every one of the 308 original recipe keys of the oracle is a native recipe file, and the research book of the port shows them (native ResearchRecipeCoverage, native ThaumcraftRecipeDisplay).",
    ]

    # ------------------------------------------------------ the JEI wrapper
    jei_wrapper = row(rows, "thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java")
    jei_wrapper["review"]["native_coverage"] = "parity_verified"
    add_evidence(jei_wrapper, [
        "oracle tree: the private 4.2.3.5 oracle has no thaumcraft/client/integration directory at all (find -type d -name 'integration*' over the oracle tree: 0 hits), so the original never published this class",
        ORACLE_TREE.format(name="ThaumcraftRecipeWrapper"),
        "oracle grep 'mezz.jei' / IRecipeWrapper / IGuiItemStackGroup: 0 hits; the only recipe-viewer line of the original is oracle thaumcraft/client/ClientProxy.java:444 Loader.isModLoaded(\"NotEnoughItems\")",
        "legacy src/main/java/thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java:1-20 the 1.12 wrapper implements the 1.12 only mezz.jei.api.recipe.IRecipeWrapper and draws the aspect row of a recipe with UtilsFX.drawTag",
        "legacy callsite audit: the only callers are the 1.12 JEI plugin (src/main/java/thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java:26-27,33,35,37,49-57) and the 1.12 category (src/main/java/thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java:9)",
        "native grep 'jei' over common/src, fabric/src and neoforge/src: no dependency and no integration package at all",
        "native common/src/main/java/thaumcraft/common/lib/research/ResearchRecipeCoverage.java:41 the coverage map of all 308 original recipe keys of the oracle ConfigRecipes.java; the recipes the 1.12 JEI plugin would have shown are the native recipe tree",
        "oracle thaumcraft/client/gui/GuiResearchRecipe.java:318-345 the original draws the aspect row of a recipe in the research book; native common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java:587-607 the native does the same",
    ])
    jei_wrapper["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The private oracle has no thaumcraft/client/integration/jei directory at all, so the original never published it; it exists only in the retained 1.12 reference tree.",
        "The 1.21.1 port ships no JEI/EMI dependency, so the class is deliberately not carried over and no recipe-viewer integration exists.",
        "The recipe DATA the 1.12 plugin would have shown is covered: every one of the 308 original recipe keys of the oracle is a native recipe file, and the research book of the port shows them (native ResearchRecipeCoverage, native ThaumcraftRecipeDisplay).",
        "The aspect row of a recipe, which the 1.12 wrapper drew with UtilsFX.drawTag, is drawn by the native research book (native ThaumcraftRecipeDisplay:587-607), the same place the original drew it (oracle GuiResearchRecipe:318-345).",
    ]

    # -------------------------------------------------- the first person tip
    tip = row(rows, "thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java")
    tip["review"]["native_coverage"] = "parity_verified"
    add_evidence(tip, [
        ORACLE_TREE.format(name="FirstPersonWandTipOrigin"),
        "oracle grep gluUnProject / GL_MODELVIEW_MATRIX / GL_PROJECTION_MATRIX / GL_VIEWPORT / glGetFloatv / glGetIntegerv: 0 hits - the original never reads the GL matrices back, so it never recovers a world space wand tip from the drawn model",
        "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:45-119 the first person wand of the original is drawn by transforms only (:59-86), by the model basis correction (:87) and by the use pose (:88-113); the first person case is :73-75 and :94-99",
        "oracle thaumcraft/client/fx/beams/FXBeamWand.java:209-228 the only origin of the original's wand beam is the position of the player moved by the yaw and the pitch and by the look vector times 0.3",
        "legacy src/main/java/thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java:52-59 the 1.12 helper captures the wand tip by inverting the GL projection matrix of the first person hand pass (GLU.gluUnProject)",
        "legacy callsite audit: the only callers are the 1.12 tree (src/main/java/thaumcraft/client/fx/beams/FXBeamWand.java:101; src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java:127; src/main/java/thaumcraft/client/renderers/item/ItemWandRenderer.java:121; src/main/java/thaumcraft/client/lib/RenderEventHandler.java:437)",
        "native grep wandTip/tipOrigin over common/src, fabric/src and neoforge/src: no reference at all, so no native code expects this helper",
    ])
    tip["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The oracle has no such class and no readback of the GL matrices, so the original never recovered a world space wand tip; the helper exists only in the retained 1.12 reference tree.",
        "STALE NOTE CORRECTED 2026-10-23 (w5legacy): the old note said the native port draws no beam from the wand tip, so the origin helper has no counterpart. The premise is false: the original never drew its beam from the wand tip. Oracle FXBeamWand:37,58-62,209-228 - the beam starts at the player itself, moved by the yaw and the pitch and by the look vector times 0.3 - and the native uses exactly that origin (ItemFocusExcavation:162-163).",
        "The FXBeamWand row is parity_verified since the w4wands wave, which reproduced the fan of the original, so there is no wand tip origin left to port and the 1.12 readback of the GL matrices has no original contract to lose.",
    ]

    # ------------------------------------------------------ the pose helpers
    pose_math = row(rows, "thaumcraft/client/renderers/item/WandPoseMath.java")
    pose_math["review"]["native_coverage"] = "parity_verified"
    add_evidence(pose_math, [
        ORACLE_TREE.format(name="WandPoseMath"),
        "oracle grep transformPoint / contextRotateX / contextRotateZ / startupRotateX / waveRotateX / waveRotateZ: 0 hits - none of the six members of the 1.12 pose appears in the 4.2.3.5 tree",
        "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:88-113 the use pose of the original is inline in the renderer: the elapsed use time capped at three ticks (:89-92), the raise of the wand by one block (:93), the thirty three degrees of the third person (:95), the ten plus ten degrees of the first person (:97-98), the sixty degrees of the startup (:100), the WAVE animation (:101-105) and the CHARGE animation (:106-110), and the pivot one block below (:112)",
        "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:87 the model basis correction of a hundred and eighty degrees about X in which the pose of the original is expressed",
        "legacy src/main/java/thaumcraft/client/renderers/item/WandPoseMath.java:14-30 the 1.12 arithmetic that applies the pose to a point in the corrected basis",
        "legacy callsite audit: the only caller is the 1.12 tree (src/main/java/thaumcraft/client/fx/beams/WandEffectOrigin.java:13,79)",
        "native grep WandPoseMath over common/src, fabric/src and neoforge/src: no reference at all",
    ])
    pose_math["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The oracle has no such class and none of its six members; the use pose of the original lives inline in the original's ItemWandRenderer:88-113, and the 1.12 helper only mirrors it for the 1.12 tree.",
        "STALE NOTE CORRECTED 2026-10-23 (w5legacy): the old note claimed the native port does not raise the wand while a focus is used, so this helper has no counterpart. The remaining difference is real, but it is NOT this row's: the owner of the use pose is the original's ItemWandRenderer, whose own row (thaumcraft/client/renderers/item/ItemWandRenderer.java) is open and owned by another lane. This 1.12 helper has no original counterpart of its own, so the row closes as a legacy-only exclusion.",
        "When the ItemWandRenderer row closes, the native will draw the pose with the numbers of oracle ItemWandRenderer:88-113 (the raise over three ticks, the thirty three degrees, the ten plus ten, the sixty degrees of the startup and the WAVE and CHARGE waves) in the basis that the correction of :87 establishes. The numbers of the 1.12 sampler are the numbers of the original.",
    ]

    pose = row(rows, "thaumcraft/client/renderers/item/WandUsePose.java")
    pose["review"]["native_coverage"] = "parity_verified"
    add_evidence(pose, [
        ORACLE_TREE.format(name="WandUsePose"),
        "oracle grep startup / contextRotateX / contextRotateZ / startupRotateX / waveRotateX / waveRotateZ as pose members: 0 hits - the 1.12 pose enum has no counterpart in the 4.2.3.5 tree",
        "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:88-113 the use pose of the original is inline in the renderer: the elapsed use time capped at three ticks (:89-92), the raise of the wand by one block (:93), the thirty three degrees of the third person (:95), the ten plus ten degrees of the first person (:97-98), the sixty degrees of the startup (:100), the WAVE animation (:101-105) and the CHARGE animation (:106-110), and the pivot one block below (:112)",
        "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:87 the model basis correction of a hundred and eighty degrees about X in which the pose of the original is expressed",
        "legacy src/main/java/thaumcraft/client/renderers/item/WandUsePose.java:11-25 the 1.12 pose holder with its six fields",
        "legacy callsite audit: the only callers are the 1.12 tree (src/main/java/thaumcraft/client/fx/beams/WandEffectOrigin.java:14,71; src/main/java/thaumcraft/client/renderers/item/WandPoseMath.java:14; src/main/java/thaumcraft/client/renderers/item/WandUsePoseSampler.java:35)",
        "native grep WandUsePose over common/src, fabric/src and neoforge/src: no reference at all",
    ])
    pose["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The oracle has no such class and none of its six members; the use pose of the original lives inline in the original's ItemWandRenderer:88-113, and the 1.12 holder only mirrors it for the 1.12 tree.",
        "STALE NOTE CORRECTED 2026-10-23 (w5legacy): the old note claimed the native port does not raise the wand while a focus is used, so the pose has no counterpart. The remaining difference is real, but it is NOT this row's: the owner of the use pose is the original's ItemWandRenderer, whose own row (thaumcraft/client/renderers/item/ItemWandRenderer.java) is open and owned by another lane. This 1.12 holder has no original counterpart of its own, so the row closes as a legacy-only exclusion.",
        "When the ItemWandRenderer row closes, the native will draw the pose with the numbers of oracle ItemWandRenderer:88-113 in the basis that the correction of :87 establishes. The numbers of the 1.12 holder are the numbers of the original.",
    ]

    sampler = row(rows, "thaumcraft/client/renderers/item/WandUsePoseSampler.java")
    sampler["review"]["native_coverage"] = "parity_verified"
    add_evidence(sampler, [
        ORACLE_TREE.format(name="WandUsePoseSampler"),
        "oracle grep 'sample(' with an elapsed time and a WandFocusAnimation: 0 hits - the 1.12 sampler has no counterpart in the 4.2.3.5 tree, the original computes the pose inline",
        "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:88-113 the use pose of the original is inline in the renderer: the elapsed use time capped at three ticks (:89-92), the raise of the wand by one block (:93), the thirty three degrees of the third person (:95), the ten plus ten degrees of the first person (:97-98), the sixty degrees of the startup (:100), the WAVE animation (:101-105) and the CHARGE animation (:106-110), and the pivot one block below (:112)",
        "oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:87 the model basis correction of a hundred and eighty degrees about X in which the pose of the original is expressed",
        "legacy src/main/java/thaumcraft/client/renderers/item/WandUsePoseSampler.java:16-36 the 1.12 sampler: the raise over three ticks, the thirty three degrees, the ten plus ten degrees and the WAVE and CHARGE waves",
        "legacy callsite audit: the only callers are the 1.12 tree (src/main/java/thaumcraft/client/fx/beams/WandEffectOrigin.java:15,71; src/main/java/thaumcraft/client/renderers/item/ItemWandRenderer.java:272)",
        "native grep WandUsePoseSampler over common/src, fabric/src and neoforge/src: no reference at all",
    ])
    sampler["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The oracle has no such class; the original computes the use pose inline in its ItemWandRenderer:88-113, and the 1.12 sampler only mirrors it for the 1.12 tree.",
        "STALE NOTE CORRECTED 2026-10-23 (w5legacy): the old note claimed the native port does not raise the wand while a focus is used, so the sampler has no counterpart. The remaining difference is real, but it is NOT this row's: the owner of the use pose is the original's ItemWandRenderer, whose own row (thaumcraft/client/renderers/item/ItemWandRenderer.java) is open and owned by another lane. This 1.12 sampler has no original counterpart of its own, so the row closes as a legacy-only exclusion.",
        "The numbers of the 1.12 sampler are the numbers of the original (oracle ItemWandRenderer:88-113), so nothing is lost by the exclusion: when the ItemWandRenderer row closes, the native will draw the pose with those numbers.",
    ]

    # ------------------------------------------------ the eldritch instrument
    diagnostics = row(rows, "thaumcraft/client/lib/EldritchDiagnostics.java")
    diagnostics["review"]["native_coverage"] = "parity_verified"
    add_evidence(diagnostics, [
        ORACLE_TREE.format(name="EldritchDiagnostics"),
        "oracle grep 'EldritchPerf' / nothingTESRCalls / obeliskTESRCalls / fieldHelperFaces: 0 hits - the 4.2.3.5 tree has no FPS instrumentation of any kind",
        "legacy src/main/java/thaumcraft/client/lib/EldritchDiagnostics.java:11-16,44-86 the 1.12 development instrument: per frame counters of an Eldritch dimension FPS investigation, logged every sixty frames",
        "legacy callsite audit: every call site of the 1.12 tree is commented out (src/main/java/thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java:26,48,50,52; src/main/java/thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java:67,79), so the class is dead code even in the retained 1.12 reference",
        "native grep EldritchDiagnostics over common/src, fabric/src and neoforge/src: no reference at all",
        "oracle thaumcraft/client/ClientProxy.java:533-541,609 the eldritch rendering of the original is the world renderers and the eldritch block renderer; the native has the same ones (native common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java, native common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchRenderer.java)",
    ])
    diagnostics["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. It is a 1.12 development instrument (per frame counters of an Eldritch FPS investigation), and the original has no counterpart of it and no need for one.",
        "It is dead code even in the retained 1.12 reference: every call site of the 1.12 tree is commented out, and the native port has no reference to it.",
        "The eldritch rendering it measured is the original's and is present in the native (the world renderers and the eldritch block renderer).",
    ]

    # ------------------------------------------------ the eldritch item view
    item_eldritch = row(rows, "thaumcraft/client/renderers/item/ItemEldritchRenderer.java")
    item_eldritch["review"]["native_coverage"] = "parity_verified"
    add_evidence(item_eldritch, [
        ORACLE_TREE.format(name="ItemEldritchRenderer"),
        "oracle grep TileEntityItemStackRenderer / renderByItem / setTileEntityItemStackRenderer: 0 hits - the 4.2.3.5 tree never uses the item stack renderer API, and 1.21.1 removed it",
        "oracle thaumcraft/client/ClientProxy.java:453-462 the complete list of the item renderers of the original: the filled jar, the node jar, the aura node, the thaumometer, the casting wand, the trunk spawner, the bone bow and the wooden device - no eldritch tile among them",
        "oracle thaumcraft/client/ClientProxy.java:533-541,609 the eldritch tiles of the original are rendered in the world only (the special tile renderers and the eldritch block renderer); they are world structures, not items",
        "legacy src/main/java/thaumcraft/client/renderers/item/ItemEldritchRenderer.java:18-84 the 1.12 item stack renderer that draws the eldritch tiles in the inventory",
        "legacy callsite audit: the only caller is the 1.12 tree (src/main/java/thaumcraft/client/ClientProxy.java:886 eldritchItem.setTileEntityItemStackRenderer(new ItemEldritchRenderer()))",
        "native grep ItemEldritchRenderer over common/src, fabric/src and neoforge/src: no reference at all",
        "native common/src/main/java/thaumcraft/client/renderers/tile/TileEldritchCapRenderer.java, TileEldritchObeliskRenderer.java, TileEldritchLockRenderer.java, TileEldritchCrabSpawnerRenderer.java the native has the same world renderers of the original",
    ])
    item_eldritch["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The oracle has no TileEntityItemStackRenderer at all and its complete list of item renderers (ClientProxy:453-462) contains no eldritch tile; the eldritch tiles of the original are world structures drawn by the world renderers (ClientProxy:533-541,609), which the native has.",
        "The 1.21.1 port has no item stack renderer API and no reference to this class; the eldritch tiles are drawn in the world exactly as the original draws them.",
    ]

    # --------------------------------------------------------- the WAILA row
    waila = row(rows, "thaumcraft/common/integration/waila/GolemWailaPlugin.java")
    waila["review"]["native_coverage"] = "parity_verified"
    add_evidence(waila, [
        "oracle tree: the class is absent from the TC4 4.2.3.5 oracle (grep -i 'waila' over the 859 oracle files: 0 hits; WAILA did not exist for 1.7.10)",
        ORACLE_TREE.format(name="GolemWailaPlugin"),
        "oracle thaumcraft/common/entities/golems/ItemTrunkSpawner.java:76-91 the original's own way of showing the upgrade of a golem: the name of the upgrade in the hover text of the trunk spawner item that carries the golem",
        "native common/src/main/java/thaumcraft/common/items/golems/ItemTrunkSpawner.java:28-38 the native reproduces exactly that (the name of the upgrade in blue at :33, the contents line at :37)",
        "native common/src/main/java/thaumcraft/client/gui/GuiGolem.java:52,159-169 the native golem screen shows the material frame and the core blurb of the original",
        "oracle thaumcraft/client/gui/GuiGolem.java:102,134,167,237,330,399 the golem screen of the original only tests the amounts of the upgrades (4 and 5) to enable its features; it never lists the installed upgrades either",
        "legacy src/main/java/thaumcraft/common/integration/waila/GolemWailaPlugin.java:16-34,41-69 the 1.12 WAILA provider that writes the material, the core and the upgrade list into the hover tooltip of the golem entity",
        "legacy callsite audit: the class has no caller in the retained 1.12 tree; it is registered by the WAILA annotation @WailaPlugin (:16)",
        "native grep waila/huduma/journeymap over common/src, fabric/src and neoforge/src: no reference at all, so the mod has neither a soft nor a hard dependency and loads without WAILA",
    ])
    waila["review"]["notes"] = [
        "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The oracle has no WAILA integration at all (WAILA did not exist for 1.7.10), so the original never published it; it exists only in the retained 1.12 reference tree.",
        "WAILA has no 1.21.1 version, so the hover integration cannot be ported as an integration. The native carries no WAILA code and no WAILA reference, so the mod loads unchanged without it.",
        "The information the plugin showed is present in the native and in the original's own places: the installed upgrade is named in the hover text of the trunk spawner item, exactly as the original does it (oracle ItemTrunkSpawner:76-91 -> native ItemTrunkSpawner:28-38), and the golem screen shows the material and the core (native GuiGolem:52,159-169).",
        "The list of the installed upgrades is NOT shown on the golem screen - but the original did not show it there either (oracle GuiGolem only tests the upgrade amounts at :102,:134,:167,:237,:330,:399), so no original contract is lost. The only thing that is missing is the 1.12 hover tooltip of the golem ENTITY, which the original never had either. If that list is ever wanted, it belongs to the GuiGolem row, not to this one.",
    ]

    LEDGER.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("w5legacy: eleven rows closed as exclude/parity_verified")


if __name__ == "__main__":
    main()
