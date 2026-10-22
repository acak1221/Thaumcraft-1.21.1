#!/usr/bin/env python3
"""Update the review of the six research/scanning units of the w4research wave.

Only the rows of this lane are touched; the JSON shape, the key order and the
formatting of the ledger are preserved exactly (json.dumps(..., indent=2) plus
the final newline, as scripts/porting/update_scope_ledger.py writes it).
"""
from __future__ import annotations

import json
from pathlib import Path

LEDGER = Path("docs/porting/scope-ledger.json")


def row(rows, path):
    for entry in rows:
        if entry["canonical_path"] == path:
            return entry
    raise KeyError(path)


def main() -> None:
    data = json.loads(LEDGER.read_text(encoding="utf-8"))
    rows = data["units"]

    # ---------------------------------------------------------------- browser
    browser = row(rows, "thaumcraft/client/gui/GuiResearchBrowser.java")
    browser["review"]["native_coverage"] = "parity_verified"
    browser["review"]["evidence"] += [
        "oracle thaumcraft/client/gui/GuiResearchBrowser.java:327-348 the sibling connectors of the map",
        "oracle thaumcraft/client/gui/GuiResearchBrowser.java:732-787 the three pixel wide gradient connector line with the sine wobble",
        "native common/src/main/java/thaumcraft/common/lib/research/ResearchBrowserConnectors.java:56-146 the parent and the sibling connectors with the original colours, the original gates and the original geometry",
        "native common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:112-208 the connector line of the original, drawn over every research of the category",
        "oracle thaumcraft/client/gui/GuiResearchBrowser.java:404-412 an icon expression that names nothing draws nothing",
        "native common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:196-207 the icon of a cell is drawn only when its expression resolves",
    ]
    browser["review"]["notes"] = [
        "The map, the clue/parent/category visibility rules and the authoritative purchase/note actions are reproduced (native ResearchBrowserData.isVisible:21-29 mirrors oracle GuiResearchBrowser:318/371).",
        "CLOSED in this wave: the sibling connectors (oracle GuiResearchBrowser:327-348) are drawn, and the connector line is the three pixel wide gradient strip with the sine wobble of the original (oracle :732-787) instead of the one pixel line of the native port.",
        "CLOSED in this wave: an icon expression that names nothing draws nothing (oracle :404-412); the native port no longer falls back to the icon of the thaumonomicon.",
        "The frame of a cell is the original one: 54 for a round research, 230 or 86 for a hidden research and 110 or 0 for the others, with the original rule of the research difficulty (native GuiResearchBrowser.badgeFrame mirrors oracle :387-399).",
        "The Special overlay (oracle :400-402) and the warp 'forbidden' marker (oracle :366,789-802) are reproduced.",
        "Deviation: the pane is the native 510x300 panel with the map at the 1.0 scale, the original draws a 256x230 pane with the map at the 0.5 scale (oracle :75-76, :285-291). The map keeps the original 24 pixel grid, the original coordinates and the original categories.",
    ]

    # ----------------------------------------------------------- research book
    book = row(rows, "thaumcraft/client/gui/GuiResearchRecipe.java")
    book["review"]["native_coverage"] = "partial"
    book["review"]["evidence"] += [
        "oracle thaumcraft/common/config/ConfigResearch.java:193-199 the BANNERS page holds the sixteen banner recipes as an array of arcane recipes",
        "oracle thaumcraft/common/config/ConfigResearch.java:221-226 the PRIMALARROW page holds the six primal arrow recipes as an array",
        "oracle thaumcraft/common/config/ConfigResearch.java:366-374 the ORE page holds the seven crystal cluster recipes as an array",
        "oracle thaumcraft/common/config/ConfigResearch.java:110-144 the SCEPTRE page holds the three sceptre recipes as an array",
        "oracle thaumcraft/common/config/ConfigResearch.java:302-305 the JARLABEL page holds the sixteen jar label recipes as an array",
        "oracle thaumcraft/client/gui/GuiResearchRecipe.java:647-655 the array page shows one of its recipes per second",
        "native common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java:363-430 the BANNERS, the PRIMALARROW and the ORE arrays are resolved and cycle once per second, like the original",
        "native common/src/main/java/thaumcraft/client/gui/GuiResearchRecipe.java:51 the page resolver knows the key of its research, so the two original arrays that share one Java expression stay apart",
    ]
    book["review"]["notes"] = [
        "The book renders the original text pages and every recipe page whose original recipe key resolves to a native recipe file (250 mapped keys, verified against the packaged data/thaumcraft/recipe tree).",
        "CLOSED in this wave: four of the eight array/smelting pages are rendered again: the BANNERS page (the sixteen banner recipes), the PRIMALARROW page (the six primal arrow recipes), the ORE page (the seven crystal cluster recipes) and the CRUCIBLE smelting page (the six balanced shards). The page cycles through its array once per second, exactly as the original (oracle GuiResearchRecipe:647-655).",
        "REMAINING DIFFERENCE: the SCEPTRE page (the three sceptre recipes, oracle ConfigResearch:110-144) and the JARLABEL page (the sixteen jar label recipes, oracle ConfigResearch:302-305) still have no renderer. The native data is there - the registered recipe thaumcraft:arcane_sceptre carries the three variants through ArcaneSceptreRecipe.displayInput(rod, cap) and the registered recipe thaumcraft:jar_label_bind binds any filled phial - but the book renderer draws the recipe of the page and not a synthetic input, so a dedicated diagram for these two pages is missing.",
        "Deviation: the original's per-PageType renderers (aspect page, image page, infusion-enchantment page, the original's own recipe renderers) are replaced by one native recipe renderer.",
    ]

    # ---------------------------------------------------------- research table
    table = row(rows, "thaumcraft/client/gui/GuiResearchTable.java")
    table["review"]["native_coverage"] = "parity_verified"
    table["review"]["evidence"] += [
        "oracle thaumcraft/client/gui/GuiResearchTable.java:408-441 the runes of the sheet: one rune every 250 ms on a free hex, one of the sixteen cells of the script sheet, fading over fifteen to twenty five seconds",
        "oracle thaumcraft/client/gui/GuiResearchTable.java:517-540 the rune is a black quad of ten pixels of textures/misc/script.png, turned by ninety degrees in the plane",
        "native common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java:520-575 the runes of the sheet",
        "oracle thaumcraft/client/gui/GuiResearchTable.java:385-405 the three pixel wide additive line with the colour 0.0/0.6/0.8 and the pulsing alpha 0.3..0.9",
        "native common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java the line of the original, with the additive blend and the pulsing alpha",
        "oracle thaumcraft/client/gui/GuiResearchTable.java:411 the sheet is the single parchment textures/misc/parchment3.png",
        "native common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java:156 the same single parchment",
    ]
    table["review"]["notes"] = [
        "The palette, the hex grid, drag and drop placement, erasing, the researcher bonus rolls, the aspect combination and the paid copy are implemented and the original RNG order and thresholds are preserved.",
        "CLOSED in this wave: the runes of the sheet (oracle GuiResearchTable:408-441) are drawn again, and the line of the original (:385-405) uses its three pixel width, its additive blend (770, 1) and its pulsing alpha.",
        "The sheet of the original is ONE parchment texture (textures/misc/parchment3.png, oracle :411). The earlier note about six parchment sheets named a difference the original never had.",
        "Deviation: the server-authoritative edit/combine/copy requests add a revision/session handshake that the original does not have (a deliberate native authority decision).",
    ]

    # ------------------------------------------------------------- thaumometer
    meter = row(rows, "thaumcraft/client/renderers/item/ItemThaumometerRenderer.java")
    meter["review"]["native_coverage"] = "partial"
    meter["review"]["evidence"] += [
        "oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:72-135 the IItemRenderer contract of the original, its first person view model and the mirrored arm of the player",
        "native common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java the native renderer draws the same first person view model, the same arm and the same scanner OBJ through the 1.21.1 builtin item hook",
        "oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:103-104 the two rotations that follow the swing of the arm of the player",
        "oracle thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:155-259 the scan readout: the scanned stack, the aspect icons and the name of the target",
    ]
    meter["review"]["notes"] = [
        "Excluded as a contract: 1.21.1 removed IItemRenderer/IModelCustom, so the original's public item renderer contract has no target API. The port renders the thaumometer through the 1.21.1 builtin item hook instead.",
        "The native renderer is NOT a plain item model: it draws the three dimensional scanner of the original (the scanner OBJ and the glowing scan screen) and the first person view model with the arm of the player.",
        "REMAINING DIFFERENCE: the scan readout of the original (oracle ItemThaumometerRenderer:155-259 - the scanned stack, the aspect icons and the name of the target) is not drawn, and the two rotations that follow the swing of the arm of the player (oracle :103-104) are left out, because the arm angles of 1.7.10 (renderArmPitch and renderArmYaw) have no public equivalent in 1.21.1.",
    ]

    # ------------------------------------------------------------ scan session
    item = row(rows, "thaumcraft/common/items/relics/ItemThaumometer.java")
    item["review"]["native_coverage"] = "parity_verified"
    item["review"]["evidence"] += [
        "oracle thaumcraft/common/items/relics/ItemThaumometer.java:92 the rune over the pointed entity, at (x - 0.5, y + eyeHeight / 2, z - 0.5) with the duration (int)(height * 15)",
        "oracle thaumcraft/common/items/relics/ItemThaumometer.java:103,128 the rune over a node and over a block, at (x, y + 0.25, z) with the duration 15 and the gravity 0.03",
        "oracle thaumcraft/client/ClientProxy.java:918-922 the client half of the rune: exactly ONE FXBlockRunes at (x + 0.5, y + 0.5, z + 0.5)",
        "oracle thaumcraft/common/lib/research/ScanManager.java:277-315 a target that was already scanned is not a valid scan target",
        "native common/src/main/java/thaumcraft/common/lib/research/ScanRunes.java the position, the duration, the gravity and the colour of the rune of the original",
        "native common/src/main/java/thaumcraft/common/lib/network/ScanRuneFxPayload.java the S2C rune of the scan",
        "native common/src/main/java/thaumcraft/client/fx/ScanRuneClientEffects.java the client half of the rune: one particle, the colour drawn from the client random",
        "native common/src/main/java/thaumcraft/common/lib/research/ScanManager.java:63-72 alreadyScanned, the original gate of a valid scan target",
        "native common/src/main/java/thaumcraft/common/items/relics/ItemThaumometer.java the session of the scan starts only for a valid target and the rune follows the original gate",
    ]
    item["review"]["notes"] = [
        "The use duration, the 20-tick completion window, the target order (entity -> node -> block -> phenomena callback) and the entity pick range are the original numbers.",
        "Forced change: the scan is resolved on the server; the original's PacketScannedToServer (client picks, server re-validates) has no counterpart because the client no longer chooses the target.",
        "CLOSED in this wave: the rune particles of the original scan (oracle ItemThaumometer:92,103,128 through Thaumcraft.proxy.blockRunes, oracle ClientProxy:918-922) are drawn again. The native scan is server owned, so the server sends the rune of the target and the client draws the particle and its colour from the client random: the server world random that generates the research puzzles stays untouched, exactly as the original left it.",
        "CLOSED in this wave: the original refuses a target that was already scanned before the use begins (oracle ScanManager.isValidScanTarget:277-315). The native session now starts only for a valid target, so a duplicate target raises the instrument without a rune, without a camera tick and without a reward.",
        "DEVIATION (documented): the original played the camera tick on the scanning client only and drew its pitch from the CLIENT world random. The native plays the sound from the server and draws the pitch from its own generator, which keeps the server world random untouched.",
    ]

    # ---------------------------------------------------------- scanned packet
    packet = row(rows, "thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java")
    packet["review"]["native_coverage"] = "parity_verified"
    packet["review"]["evidence"] += [
        "oracle thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java:44-91 the C2S scan packet of the original and the server side re-validation",
        "native common/src/main/java/thaumcraft/common/lib/network/ScanRuneFxPayload.java the S2C rune of the scan replaces the client side effect of the original",
        "native common/src/main/java/thaumcraft/client/fx/ScanRuneClientEffects.java the client half of the rune of the original scan",
    ]
    packet["review"]["notes"] = [
        "The C2S scan packet is gone: the server derives the target itself, which removes the original's client-authority surface.",
        "CLOSED in this wave: the client feedback of the scan is complete. The camera tick sound of the original (thaumcraft:cameraticks every second tick, volume .2) is played from the server, and the rune particles of the original (oracle ItemThaumometer:92,103,128) are drawn again through the S2C payload ScanRuneFxPayload and the client half ScanRuneClientEffects.",
        "DEVIATION (documented): the original played the sound on the scanning client only and drew its pitch from the CLIENT world random; the native plays the sound from the server and draws the pitch from its own generator, which keeps the server world random untouched.",
    ]

    # ------------------------------------------------------------- the JEI rows
    for path, notes in (
        ("thaumcraft/client/integration/jei/ResearchVisibility.java", [
            "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The private oracle has no thaumcraft/client/integration/jei directory at all, so the original never published it; it exists only in the retained 1.12 reference tree.",
            "The 1.21.1 port ships no JEI/EMI dependency, so the class is deliberately not carried over and no recipe-viewer integration exists.",
            "The research visibility rules it enforced are still honoured inside the mod by ResearchBrowserData.isVisible and the server-side admission in ResearchPurchase/ResearchNoteIssuance.",
        ]),
        ("thaumcraft/client/integration/jei/ResearchVisibilityTicker.java", [
            "Excluded with evidence: the class is NOT an original TC4 4.2.3.5 class. The private oracle has no thaumcraft/client/integration/jei directory at all, so the original never published it; it exists only in the retained 1.12 reference tree.",
            "Excluded with ResearchVisibility: there is no JEI plugin in the 1.21.1 port, so the ticker has no event source.",
            "The knowledge refresh the ticker polled for is now an explicit server snapshot (KnowledgeEvents.sync).",
        ]),
    ):
        entry = row(rows, path)
        entry["review"]["native_coverage"] = "parity_verified"
        entry["review"]["disposition"] = "exclude"
        entry["review"]["notes"] = notes
        entry["review"]["evidence"] += [
            "oracle tree: the private 4.2.3.5 oracle has no thaumcraft/client/integration/jei directory, so the original never published this class",
            "native common/src/main/java/thaumcraft and fabric/src/main/java/thaumcraft contain no JEI dependency and no integration package",
        ]

    LEDGER.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("updated 8 rows")


if __name__ == "__main__":
    main()
