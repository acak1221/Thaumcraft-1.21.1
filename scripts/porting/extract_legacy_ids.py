#!/usr/bin/env python3
"""Write the table of the original TC4 item and block names -> the native 1.21.1 ids.

A 1.7.10 add-on named Thaumcraft content by a field of ``thaumcraft.common.config.ConfigItems`` or
``ConfigBlocks`` plus a metadata value: ``ItemApi.getItem("itemResource", 2)``,
``new ItemStack(ConfigItems.itemShard, 1, 6)``, ``ConfigBlocks.blockJar``. This script accounts for
every public static Item/Block field of the original 4.2.3.5 JAR and every metadata value the
original used with it, and maps each pair to the native item, block, block state or data component.

Where the metadata values come from (``scripts/porting/legacy_bytecode.py`` reads the bytecode):

* ``getSubItems`` / ``getSubBlocks`` of the class each field is built from (run on a small
  interpreter, the other-mod metal switches ``Config.found*`` recorded as a condition);
* every constant ``new ItemStack(ConfigX.field, n, meta)`` in any original class (recipes, research,
  aspects, loot, entities);
* every constant world placement ``setBlock(..., ConfigBlocks.field, meta, ...)`` in any original class;
* the original English language file (``thaumcraft_src/assets/thaumcraft/lang/en_US.lang``, committed);
* ``MAPPING`` below, which may name a further value only with an evidence reference that the script
  checks against the LineNumberTable of the original class.

The script fails when a value found in any source has no row in ``MAPPING``, when ``MAPPING`` names a
value no source and no evidence supports, or when ``MAPPING`` disagrees with the existing partial maps
of the port (``ObjectAspectCatalog``, ``ResearchIconDisplay``, ``early_object_aspects.json``) outside
the differences listed in ``RECONCILED``.

The original JAR is read, never changed, and nothing of it is written here. Evidence references are
``Class:line`` / ``Class.method:first-last`` with the source lines of the original class files.

Usage:
    extract_legacy_ids.py [--jar PATH] [--check] [--doc-only]

``--check`` exits 1 when ``legacy_ids.json`` or ``docs/api/LEGACY_IDS.md`` is stale. Without the JAR
(it is not committed) it still regenerates the reference from the committed JSON and runs every check
that needs no bytecode; the bytecode replay is then reported as skipped.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import legacy_bytecode as bytecode  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
JAR_NAME = "Thaumcraft-1.7.10-4.2.3.5.jar"
OUT = ROOT / "common/src/main/resources/data/thaumcraft/tc4/legacy_ids.json"
DOC = ROOT / "docs/api/LEGACY_IDS.md"
LANG = ROOT / "thaumcraft_src/assets/thaumcraft/lang/en_US.lang"
NATIVE_LANG = ROOT / "common/src/main/resources/assets/thaumcraft/lang/en_us.json"
BLOCKSTATES = ROOT / "common/src/main/resources/assets/thaumcraft/blockstates"
ICONS = ROOT / "common/src/main/java/thaumcraft/client/gui/ResearchIconDisplay.java"
CATALOG = ROOT / "common/src/main/java/thaumcraft/common/lib/research/ObjectAspectCatalog.java"
EARLY = ROOT / "common/src/main/resources/data/thaumcraft/tc4/early_object_aspects.json"
WILDCARD = 32767
SAME = object()  # a block row whose item is the block's own item


def default_jar() -> Path:
    """The original JAR: TC4_ORIGINAL_JAR, the repository root, or the root of the main checkout."""
    if os.environ.get("TC4_ORIGINAL_JAR"):
        return Path(os.environ["TC4_ORIGINAL_JAR"])
    for base in (ROOT, *ROOT.parents):
        if (base / JAR_NAME).is_file():
            return base / JAR_NAME
    return ROOT / JAR_NAME


def ns(path: str | None) -> str | None:
    if path is None:
        return None
    return path if ":" in path else f"thaumcraft:{path}"


def item(path: str | None, note: str | None = None, component: str | None = None, **extra) -> dict:
    return {"id": ns(path), "note": note, "component": component, **extra}


def block(path: str | None, state: dict | None = None, item: object = SAME, note: str | None = None,
          component: str | None = None, **extra) -> dict:
    return {"id": ns(path), "state": state, "item": item, "note": note, "component": component, **extra}


def none(reason: str, **extra) -> dict:
    return {"id": None, "note": reason, **extra}


def variants(paths: list[str], first: int = 0, **common) -> dict[int, dict]:
    return {first + i: item(p, **common) for i, p in enumerate(paths)}


NEVER_ASSIGNED = ("declared by ConfigItems but never assigned in 4.2.3.5: the field is null at run time, "
                  "so the original lookup failed as well")
OTHER_MOD_SILVER = "needs another mod's silver; the port has no silver ingot and no silver cap"
COPPER = "the original listed it only with another mod's copper; copper is vanilla in 1.21.1"
OTHER_METAL = ("the original listed it only with another mod's metal; the native item exists although 1.21.1 has no "
               "such ore")

# The 1.7.10 names the original never resolved itself still count: an add-on copied them verbatim.
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
          "purple", "blue", "brown", "green", "red", "black"]
RESOURCES = ["alumentum", "nitor", "thaumium_ingot", "quick_silver", "magic_tallow", "zombie_brain", "amber_gem",
             "enchanted_fabric", "vis_filter", "knowledge_fragment", "mirrored_glass", "tainted_goo", "taint_tendril",
             "jar_label", "salis_mundus", "primal_charm", "void_ingot", "void_seed", "gold_coin"]
SHARDS = ["air_shard", "fire_shard", "water_shard", "earth_shard", "order_shard", "entropy_shard", "balance_shard"]
ORES = ["cinnabar_ore", "air_infused_stone", "fire_infused_stone", "water_infused_stone", "earth_infused_stone",
        "order_infused_stone", "entropy_infused_stone", "amber_ore"]
# ConfigEntities addMapping order: the spawner egg metadata is the index (ItemSpawnerEgg.getItemStackDisplayName:61).
EGGS = {
    "BrainyZombie": "brainy_zombie_spawn_egg", "GiantBrainyZombie": "giant_brainy_zombie_spawn_egg",
    "Wisp": "wisp_spawn_egg", "Pech": "pech_spawn_egg", "EldritchWarden": "eldritch_warden_spawn_egg",
    "CultistKnight": "cultist_knight_spawn_egg", "CultistCleric": "cultist_cleric_spawn_egg",
    "CultistLeader": "cultist_leader_spawn_egg", "EldritchGolem": "eldritch_golem_spawn_egg",
    "EldritchCrab": "eldritch_crab_spawn_egg", "InhabitedZombie": "inhabited_zombie_spawn_egg",
    "ThaumSlime": "thaum_slime_spawn_egg", "TaintSpider": "taint_spider_spawn_egg",
    "Taintacle": "taintacle_spawn_egg", "TaintacleTiny": "taintacle_small_spawn_egg",
    "TaintSpore": "taint_spore_spawn_egg", "TaintSwarmer": "taint_swarmer_spawn_egg",
    "TaintSwarm": "taint_swarm_spawn_egg", "TaintedChicken": "tainted_chicken_spawn_egg",
    "TaintedCow": "tainted_cow_spawn_egg", "TaintedCreeper": "taint_creeper_spawn_egg",
    "TaintedPig": "tainted_pig_spawn_egg", "TaintedSheep": "tainted_sheep_spawn_egg",
    "TaintedVillager": "tainted_villager_spawn_egg", "TaintacleGiant": "taintacle_giant_spawn_egg",
}
EGGS_WITHOUT_ITEM = {
    "Firebat": "thaumcraft:firebat", "MindSpider": "thaumcraft:mind_spider",
    "EldritchGuardian": "thaumcraft:eldritch_guardian", "CultistPortal": "thaumcraft:cultist_portal",
}
ESSENTIA = "thaumcraft:phial_data (the aspect; the original NBT list \"Aspects\")"
LOG_NAMES = ("tile.blockMagicalLog.greatwood.name", "tile.blockMagicalLog.silverwood.name")
FINITE = ("the 1.7.10 metadata is the finite-fluid quanta minus one (8 is the original nine-unit birth); the native "
          "level state keeps the same number")
JAR_CONTENTS = "minecraft:block_entity_data (the jar's aspect and amount; the original NBT \"Aspects\")"


def single(path: str, note: str | None = None, component: str | None = None) -> dict[int, dict]:
    return {0: item(path, note, component)}


MAPPING: dict[str, dict] = {
    # ------------------------------------------------------------------ ConfigItems
    "itemWandCasting": {
        0: item("wand", "the rod and the cap are data, not metadata; the creative wand is wood/iron",
                "thaumcraft:wand_data (rod, cap and vis; the original NBT \"rod\", \"cap\" and the aspect amounts)"),
        9: item("wand", "the creative gold/greatwood wand; the damage value only orders the creative tab",
                "thaumcraft:wand_data"),
        54: item("wand", "the creative thaumium/silverwood wand; the damage value only orders the creative tab",
                 "thaumcraft:wand_data"),
        128: item("wand", "the creative sceptre; the original NBT byte \"sceptre\"",
                  "thaumcraft:wand_data and thaumcraft:wand_sceptre"),
    },
    "itemWandCap": {
        0: item("wand_cap_iron"), 1: item("wand_cap_gold"), 2: item("wand_cap_thaumium"),
        3: item("wand_cap_copper", COPPER),
        4: none(OTHER_MOD_SILVER), 5: none(OTHER_MOD_SILVER),
        6: item("wand_cap_thaumium_inert"), 7: item("wand_cap_void"), 8: item("wand_cap_void_inert"),
    },
    "itemWandRod": {
        **variants(["wand_rod_greatwood", "wand_rod_obsidian", "wand_rod_silverwood", "wand_rod_ice", "wand_rod_quartz",
                    "wand_rod_reed", "wand_rod_blaze", "wand_rod_bone"]),
        **variants(["staff_rod_greatwood", "staff_rod_obsidian", "staff_rod_silverwood", "staff_rod_ice",
                    "staff_rod_quartz", "staff_rod_reed", "staff_rod_blaze", "staff_rod_bone"], 50),
        100: item("staff_rod_primal"),
    },
    "itemFocusPouch": single("focus_pouch"),
    "itemFocusFire": single("focus_fire", component="thaumcraft:focus_upgrades"),
    "itemFocusFrost": single("focus_frost", component="thaumcraft:focus_upgrades"),
    "itemFocusShock": single("focus_shock", component="thaumcraft:focus_upgrades"),
    "itemFocusTrade": single("focus_trade", component="thaumcraft:focus_upgrades"),
    "itemFocusExcavation": single("focus_excavation", component="thaumcraft:focus_upgrades"),
    "itemFocusHellbat": single("focus_hellbat", component="thaumcraft:focus_upgrades"),
    "itemFocusWarding": single("focus_warding", component="thaumcraft:focus_upgrades"),
    "itemFocusPrimal": single("focus_primal", component="thaumcraft:focus_upgrades"),
    "itemEssence": {0: item("phial_empty"), 1: item("phial_filled", component=ESSENTIA)},
    "itemWispEssence": single("wisp_essence", component="thaumcraft:wisp_essence_data (the aspect)"),
    "itemCrystalEssence": single("crystal_essence", component="thaumcraft:crystal_essence_data (the aspect)"),
    "itemResource": {
        **variants(RESOURCES),
        5: item("zombie_brain", "defunct in 4.2.3.5 (\"Zombie Brain (defunct)\", left out of getSubItems); the same "
                                "native item as ConfigItems.itemZombieBrain"),
    },
    "itemShard": variants(SHARDS),
    "itemNugget": {
        0: item("minecraft:iron_nugget", "TC4 added its own iron nugget (ore dictionary nuggetIron); 1.21.1 has the "
                                         "vanilla one"),
        1: item("copper_nugget", COPPER), 2: item("tin_nugget", OTHER_METAL), 3: item("silver_nugget", OTHER_METAL),
        4: item("lead_nugget", OTHER_METAL),
        5: item("quicksilver_drop"), 6: item("thaumium_nugget"), 7: item("void_nugget"),
        16: item("cluster_iron"), 17: item("cluster_copper", COPPER), 18: item("cluster_tin", OTHER_METAL),
        19: item("cluster_silver", OTHER_METAL), 20: item("cluster_lead", OTHER_METAL),
        21: item("cluster_cinnabar"), 31: item("cluster_gold"),
    },
    "itemNuggetChicken": single("nugget_chicken"),
    "itemNuggetBeef": single("nugget_beef"),
    "itemNuggetPork": single("nugget_pork"),
    "itemNuggetFish": single("nugget_fish"),
    "itemTripleMeatTreat": single("triple_meat_treat"),
    "itemPhotoPlate": {0: none(NEVER_ASSIGNED)},
    "itemManaBean": single("mana_bean", component="thaumcraft:mana_bean_data (the aspect)"),
    "itemZombieBrain": single("zombie_brain"),
    "itemResearchNotes": {
        0: item("research_note", "a damage value below 64 is a research note; the value picks the note colour",
                "thaumcraft:research_note_data (the research key and the note grid)"),
        42: item("research_note", "a damage value below 64 is a research note; the value picks the note colour",
                 "thaumcraft:research_note_data"),
        64: item("discovery", "a damage value of 64 or more is a completed discovery",
                 "thaumcraft:research_note_data", evidence="ItemResearchNotes:144"),
    },
    "itemInkwell": single("inkwell"),
    "itemThaumonomicon": {0: item("thaumonomicon"), 42: item("thaumonomicon_cheat", "the creative cheat sheet")},
    "itemSpawnerEgg": "spawner_eggs",  # filled from the ConfigEntities mapping order, see spawner_eggs()
    "itemFocusPortableHole": single("focus_portable_hole", component="thaumcraft:focus_upgrades"),
    "itemFocusPech": single("focus_pech", component="thaumcraft:focus_upgrades"),
    "itemThaumometer": single("thaumometer"),
    "itemFlyingCarpet": {0: none(NEVER_ASSIGNED)},
    "itemGolemPlacer": variants(["golem_straw", "golem_wood", "golem_tallow", "golem_clay", "golem_flesh", "golem_stone",
                                 "golem_iron", "golem_thaumium"],
                                component="thaumcraft:golem_data (core, upgrades, decorations and the advanced flag; "
                                          "the original NBT)"),
    "itemGolemBell": single("golem_bell"),
    "itemGolemDecoration": variants(["golem_decoration_tophat", "golem_decoration_glasses", "golem_decoration_bowtie",
                                     "golem_decoration_fez", "golem_decoration_dart", "golem_decoration_visor",
                                     "golem_decoration_armor", "golem_decoration_mace"]),
    "itemGolemCore": {
        **variants(["golem_core_fill", "golem_core_empty", "golem_core_gather", "golem_core_harvest", "golem_core_guard",
                    "golem_core_liquid", "golem_core_essentia", "golem_core_lumber", "golem_core_use",
                    "golem_core_butcher", "golem_core_sorting", "golem_core_fishing"]),
        100: item("golem_core_blank"),
    },
    "itemGolemUpgrade": variants(["golem_upgrade_air", "golem_upgrade_earth", "golem_upgrade_fire", "golem_upgrade_water",
                                  "golem_upgrade_order", "golem_upgrade_entropy"]),
    "itemArcaneDoor": single("arcane_door"),
    "itemJarFilled": {
        0: item("jar", "the filled jar is the jar item with contents", JAR_CONTENTS),
        3: item("jar_void", "the filled void jar", JAR_CONTENTS, evidence="ItemJarFilled:81-82"),
    },
    "itemJarNode": single("jar_node", component="minecraft:block_entity_data (the stored node)"),
    "itemKey": {0: item("key_iron", component="thaumcraft:key_link (the door location; the original NBT \"location\")"),
                1: item("key_gold", component="thaumcraft:key_link")},
    "itemHandMirror": single("hand_mirror", component="thaumcraft:mirror_link (the linked mirror)"),
    "itemTrunkSpawner": single("trunk_spawner", component="thaumcraft:trunk_upgrade"),
    "itemResonator": single("resonator"),
    "itemBathSalts": single("bath_salts"),
    "itemBucketDeath": single("bucket_death"),
    "itemBucketPure": single("bucket_pure"),
    "itemEldritchObject": variants(["eldritch_eye", "crimson_rites", "eldritch_tablet", "primordial_pearl",
                                    "eldritch_placer"]),
    "itemSanitySoap": single("sanity_soap"),
    "itemSanityChecker": single("sanity_checker"),
    "itemBottleTaint": single("bottle_taint"),
    "itemGoggles": single("goggles_revealing"),
    "itemBootsTraveller": single("boots_traveller"),
    "itemHelmetThaumium": single("thaumium_helmet"),
    "itemChestThaumium": single("thaumium_chestplate"),
    "itemBootsThaumium": single("thaumium_boots"),
    "itemLegsThaumium": single("thaumium_leggings"),
    "itemShovelThaumium": single("thaumium_shovel"),
    "itemShovelElemental": single("elemental_shovel"),
    "itemPickThaumium": single("thaumium_pickaxe"),
    "itemPickElemental": single("elemental_pickaxe"),
    "itemSwordThaumium": single("thaumium_sword"),
    "itemSwordElemental": single("elemental_sword"),
    "itemAxeThaumium": single("thaumium_axe"),
    "itemAxeElemental": single("elemental_axe"),
    "itemHoeThaumium": single("thaumium_hoe"),
    "itemHoeElemental": single("elemental_hoe"),
    "itemHelmetFortress": single("fortress_helmet", component="thaumcraft:fortress_goggles and thaumcraft:fortress_mask "
                                                              "(the original NBT \"goggles\" and \"mask\")"),
    "itemChestFortress": single("fortress_chestplate"),
    "itemBootsFortress": {0: none(NEVER_ASSIGNED)},
    "itemLegsFortress": single("fortress_leggings"),
    "itemHelmetVoid": single("void_helmet"),
    "itemChestVoid": single("void_chestplate"),
    "itemBootsVoid": single("void_boots"),
    "itemLegsVoid": single("void_leggings"),
    "itemShovelVoid": single("void_shovel"),
    "itemPickVoid": single("void_pickaxe"),
    "itemSwordVoid": single("void_sword"),
    "itemSwordCrimson": single("crimson_sword"),
    "itemAxeVoid": single("void_axe"),
    "itemHoeVoid": single("void_hoe"),
    "itemHelmetVoidRobe": single("void_robe_helmet"),
    "itemChestVoidRobe": single("void_robe_chestplate"),
    "itemLegsVoidRobe": single("void_robe_leggings"),
    "itemHelmetCultistRobe": single("cultist_robe_hood"),
    "itemChestCultistRobe": single("cultist_robe"),
    "itemLegsCultistRobe": single("cultist_robe_legs"),
    "itemBootsCultist": single("cultist_boots"),
    "itemHelmetCultistPlate": single("cultist_plate_helm"),
    "itemChestCultistPlate": single("cultist_plate"),
    "itemLegsCultistPlate": single("cultist_plate_legs"),
    "itemHelmetCultistLeaderPlate": single("cultist_leader_helm"),
    "itemChestCultistLeaderPlate": single("cultist_leader"),
    "itemLegsCultistLeaderPlate": single("cultist_leader_legs"),
    "itemLootbag": variants(["loot_bag_common", "loot_bag_uncommon", "loot_bag_rare"]),
    "itemBaubleBlanks": {
        **variants(["mundane_amulet", "mundane_ring", "mundane_belt"]),
        # ItemBaubleBlanks.getVisDiscount:92: metadata 3..8 is Aspect.getPrimalAspects().get(meta - 3).
        **variants(["discount_ring_aer", "discount_ring_terra", "discount_ring_ignis", "discount_ring_aqua",
                    "discount_ring_ordo", "discount_ring_perditio"], 3,
                   note="the vis discount ring of Aspect.getPrimalAspects().get(meta - 3)"),
    },
    "itemAmuletRunic": variants(["runic_amulet", "runic_amulet_emergency"]),
    "itemRingRunic": variants(["runic_ring_lesser", "runic_ring", "runic_ring_charged", "runic_ring_healing"]),
    "itemGirdleRunic": variants(["runic_girdle", "runic_girdle_kinetic"]),
    "itemGirdleHover": single("hover_girdle"),
    "itemAmuletVis": {0: item("vis_amulet_lesser", component="thaumcraft:lesser_amulet_vis (the stored vis)"),
                      1: item("vis_amulet", component="thaumcraft:amulet_vis (the stored vis)")},
    "itemLegsRobe": single("robe_leggings"),
    "itemChestRobe": single("robe_chestplate"),
    "itemBootsRobe": single("robe_boots"),
    "itemHoverHarness": single("hover_harness", component="thaumcraft:hover_charge and thaumcraft:hover_enabled"),
    "itemBowBone": single("bone_bow"),
    "itemPrimalArrow": variants(["primal_arrow_air", "primal_arrow_fire", "primal_arrow_water", "primal_arrow_earth",
                                 "primal_arrow_order", "primal_arrow_entropy"]),
    "itemCompassStone": {0: item("compass_stone"),
                         1: item("compass_stone", "metadata 1 only selects the second icon "
                                                  "(ItemCompassStone.getIconFromDamage:44)")},
    "itemPrimalCrusher": single("primal_crusher"),
    # ----------------------------------------------------------------- ConfigBlocks
    "blockFluxGoo": {meta: block("flux_goo", {"level": str(meta)}, note=FINITE) for meta in (0, 7, 8)},
    "blockFluxGas": {meta: block("flux_gas", {"level": str(meta)}, note=FINITE) for meta in (0, 7, 8)},
    "blockFluidPure": {0: block("purifying_fluid", item=None, note="the source block; no item, the bucket is "
                                                                   "ConfigItems.itemBucketPure")},
    "blockFluidDeath": {3: block("death_fluid", {"level": "0"}, item=None,
                                 note="metadata 3 is the source cell with all four quanta (the bucket pours it); the "
                                      "native source is level 0. No item, the bucket is ConfigItems.itemBucketDeath")},
    "blockHole": {0: block("hole", item=None, note="the portable hole's temporary block has no item"),
                  15: block("hole", item=None, note="the portable hole's temporary block has no item")},
    "blockArcaneFurnace": {0: block("arcane_furnace", note="the other metadata are the parts of the formed "
                                                           "multiblock")},
    "blockMetalDevice": {
        0: block("crucible"), 1: block("alembic"), 2: block("vis_charge_relay"),
        3: block("advanced_alchemical_construct"), 5: block("item_grate"),
        6: block("item_grate", {"closed": "true"}, note="the closed grate drops the open one "
                                                         "(BlockMetalDevice.damageDropped)"),
        7: block("arcane_lamp"), 8: block("growth_lamp"), 9: block("alchemical_construct"),
        10: block("thaumatorium", item="alchemical_construct",
                  note="the formed thaumatorium drops the alchemical construct (BlockMetalDevice.damageDropped)"),
        11: block("thaumatorium_top", item="alchemical_construct",
                  note="the formed thaumatorium drops the alchemical construct (BlockMetalDevice.damageDropped)"),
        12: block("mnemonic_matrix"), 13: block("fertility_lamp"), 14: block("vis_relay"),
    },
    "blockMagicalLog": {
        0: block("greatwood_log", {"axis": "y"}, lang=LOG_NAMES[0]),
        1: block("silverwood_log", {"axis": "y"}, lang=LOG_NAMES[1]),
        2: block("silverwood_knot", item="silverwood_log",
                 note="the silverwood node knot drops a silverwood log (BlockMagicalLog.damageDropped)"),
        4: block("greatwood_log", {"axis": "x"}, note="1.7.10 log metadata bit 4 is the X axis", lang=LOG_NAMES[0],
                 evidence="BlockMagicalLog:155-156"),
        5: block("silverwood_log", {"axis": "x"}, note="1.7.10 log metadata bit 4 is the X axis", lang=LOG_NAMES[1]),
        8: block("greatwood_log", {"axis": "z"}, note="1.7.10 log metadata bit 8 is the Z axis", lang=LOG_NAMES[0],
                 evidence="BlockMagicalLog:155-156"),
        9: block("silverwood_log", {"axis": "z"}, note="1.7.10 log metadata bit 8 is the Z axis", lang=LOG_NAMES[1]),
    },
    "blockStoneDevice": {
        0: block("alchemy_furnace"), 1: block("arcane_pedestal"), 2: block("infusion_matrix"),
        3: block("infusion_pillar", item=None, note="a part of the formed infusion altar; no item in 1.21.1"),
        4: block("infusion_pillar", item=None, note="a part of the formed infusion altar; no item in 1.21.1"),
        5: block("wand_pedestal", {"focus": "false"}),
        6: block("infusion_pillar", item=None, note="a part of the formed infusion altar; no item in 1.21.1"),
        7: block("infusion_pillar", item=None, note="a part of the formed infusion altar; no item in 1.21.1"),
        8: block("wand_pedestal", {"focus": "true"}, item="wand_pedestal_focus",
                 note="the compound recharge focus is the focus state of the wand pedestal"),
        9: block("node_stabilizer"), 10: block("advanced_node_stabilizer"), 11: block("node_transducer"),
        12: block("arcane_spa"), 13: block("focal_manipulator"),
        14: block("stone_device", {"variant": "14"}, item="flux_scrubber",
                  note="the flux scrubber is the variant 14 of the stone device family"),
    },
    "blockWoodenDevice": {
        0: block("bellows"), 1: block("arcane_ear"),
        2: block("arcane_pressure_plate", {"variant": "2"}),
        3: block("arcane_pressure_plate", {"variant": "3"}, item="arcane_pressure_plate",
                 note="the pressed plate drops the plate (BlockWoodenDevice.damageDropped)",
                 evidence="BlockWoodenDevice:161"),
        4: block("arcane_bore_base"), 5: block("arcane_bore"),
        6: block("greatwood_planks"), 7: block("silverwood_planks"),
        8: block("wooden_device", {"variant": "8"}, item="banner",
                 note="the cult banner; its colour and aspect were NBT",
                 component="thaumcraft:banner_data (the original NBT byte \"color\" and string \"aspect\")"),
    },
    "blockMagicalLeaves": {0: block("greatwood_leaves", lang="tile.blockMagicalLeaves.greatwood.name"),
                           1: block("silverwood_leaves", lang="tile.blockMagicalLeaves.silverwood.name")},
    "blockTable": {
        0: block("table", {"variant": "0"}),
        1: block("table", {"variant": "1"}, item="research_table",
                 note="the original item renderer draws metadata 1 as the research table (its only use, "
                      "ConfigResearch:647)"),
        **{meta: block("table", {"variant": str(meta)}, item="research_table",
                       note="a half of the two-block research table (the variant keeps the original "
                            "metadata); it drops the table", evidence="BlockTable:132-135",
                       lang="tile.blockTable.research.name")
           for meta in range(2, 10)},
        14: block("deconstruction_table"), 15: block("arcane_workbench"),
    },
    "blockChestHungry": {0: block("hungry_chest")},
    "blockCustomOre": {i: block(p) for i, p in enumerate(ORES)},
    "blockCustomPlant": {0: block("greatwood_sapling"), 1: block("silverwood_sapling"), 2: block("shimmerleaf"),
                         3: block("cinderpearl"), 4: block("ethereal_bloom"), 5: block("vishroom")},
    "blockCandle": {0: block("tallow_candle"),
                    **{meta: block(f"{COLORS[meta]}_tallow_candle") for meta in range(1, 16)}},
    "blockJar": {0: block("jar"), 1: block("jar_brain"), 2: block("jar_node"), 3: block("jar_void")},
    "blockArcaneDoor": {0: block("arcane_door", note="the item is ConfigItems.itemArcaneDoor")},
    "blockWarded": {0: block("warded", item=None, note="the warded block disguises another block; no item")},
    "blockLifter": {0: block("levitator")},
    "blockAiry": {
        0: block("aura_node", note="the aura node is its own native block"),
        1: block("nitor", item="nitor", note="the placed nitor; the item is ConfigItems.itemResource:1"),
        2: block("airy", {"variant": "2"}, item=None, note="a glimmer of light; no item"),
        3: block("arcane_light", item=None, note="the arcane lamp's light is its own native block; no item"),
        4: block("airy", {"variant": "4"}, item=None, note="the warding aura of a warding stone; no item"),
        5: block("energized_node", item=None, note="the energized node is its own native block; no item"),
        10: block("airy_hole", {"variant": "10"}, item=None, note="the eldritch warden's static field; no item"),
        11: block("airy_hole", {"variant": "11"}, item=None, note="the eldritch warden's sapping field; no item"),
        12: block("airy_unbreakable", item=None, note="an impassable barrier; no item"),
    },
    "blockCrystal": {
        **{i: block(p) for i, p in enumerate(["crystal_air", "crystal_fire", "crystal_water", "crystal_earth",
                                              "crystal_order", "crystal_entropy", "crystal_mixed"])},
        7: block("eldritch_crystal", note="the strange crystals of the Outer Lands are hosted by the eldritch family"),
    },
    "blockCosmeticOpaque": {0: block("amber_block"), 1: block("amber_bricks"),
                            2: block("cosmetic_opaque_warded", item="warded_glass")},
    "blockCosmeticSolid": {
        0: block("cosmetic_solid_tough", {"variant": "0"}),
        1: block("cosmetic_solid_tough", {"variant": "1"}, item="obsidian_tile"),
        2: block("cosmetic_solid", {"variant": "2"}, item="paving_stone_travel"),
        3: block("cosmetic_solid", {"variant": "3"}, item="paving_stone_warding"),
        4: block("thaumium_block"), 5: block("tallow_block"), 6: block("arcane_stone"),
        7: block("arcane_stone_bricks"),
        8: block("cosmetic_solid_tough", {"variant": "8"}, note="the charged obsidian totem"),
        9: block("golem_fetter", {"active": "false"}),
        10: block("golem_fetter", {"active": "true"}, note="the active fetter drops the fetter "
                                                          "(BlockCosmeticSolid.damageDropped)"),
        **{meta: block("cosmetic_solid", {"variant": str(meta)}) for meta in range(11, 16)},
    },
    "blockMirror": {0: block("mirror", {"essentia": "false"}),
                    6: block("mirror", {"essentia": "true"}, item="mirror_essentia")},
    "blockTaint": {0: block("taint_crust"), 1: block("taint_soil"), 2: block("flesh_block")},
    "blockTaintFibres": {i: block("taint_fibres", {"type": str(i)}, item=p)
                         for i, p in enumerate(["taint_fibres", "taint_grass", "taint_plant", "taint_spore_stalk",
                                                "taint_spore_stalk_mature"])},
    "blockManaPod": {0: block("mana_pod", item=None, note="the pod grows from a planted ConfigItems.itemManaBean; "
                                                          "no item")},
    "blockTube": {i: block(p) for i, p in enumerate(["essentia_tube", "essentia_valve", "centrifuge", "essentia_filter",
                                                     "essentia_buffer", "essentia_restrict", "essentia_oneway",
                                                     "essentia_crystallizer"])},
    "blockMagicBox": {0: block("magic_box")},
    "blockEldritch": {
        **{meta: block("eldritch", {"variant": str(meta)}) for meta in range(0, 4)},
        2: block("eldritch", {"variant": "2"}, note="the middle of the obelisk drops the obelisk "
                                                    "(BlockEldritch.damageDropped)"),
        4: block("eldritch_crust", {"variant": "4"}), 5: block("eldritch_crust", {"variant": "5"}),
        6: block("eldritch_deco"),
        7: block("eldritch_doorway", {"variant": "7"}), 8: block("eldritch_doorway", {"variant": "8"}),
        9: block("eldritch_spawner", {"variant": "9"}), 10: block("eldritch_spawner", {"variant": "10"}),
    },
    "blockEldritchPortal": {0: block("eldritch_portal")},
    "blockEssentiaReservoir": {0: block("essentia_reservoir")},
    "blockStairsArcaneStone": {0: block("arcane_stone_brick_stairs")},
    "blockStairsSilverwood": {0: block("silverwood_stairs")},
    "blockStairsGreatwood": {0: block("greatwood_stairs")},
    "blockStairsEldritch": {0: block("eldritch_stairs")},
    "blockSlabWood": {
        0: block("greatwood_slab", {"type": "bottom"}), 1: block("silverwood_slab", {"type": "bottom"}),
        8: block("greatwood_slab", {"type": "top"}, note="1.7.10 slab metadata bit 8 is the upper half",
                 evidence="BlockCosmeticWoodSlab:75-79"),
        9: block("silverwood_slab", {"type": "top"}, note="1.7.10 slab metadata bit 8 is the upper half",
                 evidence="BlockCosmeticWoodSlab:75-79"),
    },
    "blockSlabStone": {
        0: block("arcane_stone_brick_slab", {"type": "bottom"}),
        1: block("cosmetic_stone_slab_eldritch", {"type": "bottom"}),
        8: block("arcane_stone_brick_slab", {"type": "top"}, note="1.7.10 slab metadata bit 8 is the upper half",
                 evidence="BlockCosmeticStoneSlab:75-79"),
        9: block("cosmetic_stone_slab_eldritch", {"type": "top"}, note="1.7.10 slab metadata bit 8 is the upper half"),
    },
    "blockDoubleSlabWood": {
        0: block("greatwood_slab", {"type": "double"}, item="greatwood_slab",
                 note="the double slab is the double state of the slab", evidence="BlockCosmeticWoodSlab:75-79"),
        1: block("silverwood_slab", {"type": "double"}, item="silverwood_slab",
                 note="the double slab is the double state of the slab", evidence="BlockCosmeticWoodSlab:75-79"),
    },
    "blockDoubleSlabStone": {
        0: block("arcane_stone_brick_slab", {"type": "double"}, item="arcane_stone_brick_slab",
                 note="the double slab is the double state of the slab", evidence="BlockCosmeticStoneSlab:75-79"),
        1: block("cosmetic_stone_slab_eldritch", {"type": "double"}, item="cosmetic_stone_slab_eldritch",
                 note="the double slab is the double state of the slab", evidence="BlockCosmeticStoneSlab:75-79"),
    },
    "blockEldritchNothing": {0: block("eldritch_nothing")},
    "blockAlchemyFurnace": {
        0: block("advanced_alchemical_furnace", {"part": "0"}),
        **{meta: block("advanced_alchemical_furnace", {"part": str(meta)}, item=None,
                       note="a part of the formed advanced alchemical furnace; it drops its component, not the furnace")
           for meta in range(1, 5)},
    },
    "blockLootUrn": {i: block("loot_urn", {"rarity": str(i)},
                              component="minecraft:custom_data {rarity} (BlockLootItem.setRarity)")
                     for i in range(3)},
    "blockLootCrate": {i: block("loot_crate", {"rarity": str(i)},
                                component="minecraft:custom_data {rarity} (BlockLootItem.setRarity)")
                       for i in range(3)},
}

# Differences from the existing partial maps of the port that are deliberate, with the reason.
RECONCILED: dict[str, str] = {
    "early_object_aspects:blockAlchemyFurnace:32767":
        "ConfigBlocks.blockAlchemyFurnace is the Advanced Alchemical Furnace (en_US.lang tile.blockAlchemyFurnace."
        "name); the early aspect corpus binds its ConfigAspects:492 tags to thaumcraft:alchemy_furnace, the "
        "blockStoneDevice:0 furnace. Left for the aspect owners; this table follows the original class.",
}


# ------------------------------------------------------------------ evidence


def lang_names() -> dict[str, str]:
    names = {}
    for line in LANG.read_text(encoding="utf-8", errors="replace").splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            names[key.strip()] = value.strip()
    return names


def lang_metas(names: dict[str, str], prefix: str, unlocalized: str | None) -> dict[int, str]:
    if not unlocalized:
        return {}
    found = {}
    pattern = re.compile(rf"^{re.escape(prefix)}\.{re.escape(unlocalized)}\.(\d+)\.name$")
    for key in names:
        match = pattern.match(key)
        if match:
            found[int(match.group(1))] = key
    return found


def name_of(names: dict[str, str], prefix: str, unlocalized: str | None, meta: int) -> str | None:
    if not unlocalized:
        return None
    for key in (f"{prefix}.{unlocalized}.{meta}.name", f"{prefix}.{unlocalized}.name"):
        if key in names:
            return names[key].strip()
    return None


def spawner_eggs(classes: dict) -> dict[int, dict]:
    """ConfigEntities: ItemSpawnerEgg.addMapping(name, ...) in call order; the metadata is the index."""
    config = classes["thaumcraft/common/config/ConfigEntities"]
    order = []
    for method in config.methods.values():
        last = None
        for _offset, op, _arg, comment in method.code:
            if op.startswith("ldc") and comment.startswith("String "):
                last = comment[7:]
            elif op == "invokestatic" and "ItemSpawnerEgg.addMapping" in comment:
                order.append((last, method.line_of(_offset)))
    rows = {}
    names = lang_names()
    for meta, (name, line) in enumerate(order):
        evidence = f"ConfigEntities:{line}"
        # ItemSpawnerEgg.getItemStackDisplayName:60-65: "item.monsterPlacer.name" (1.7.10 "Spawn") + the entity name.
        shown = names.get(f"entity.Thaumcraft.{name}.name")
        label = {"name": f"Spawn {shown}"} if shown else {}
        if name in EGGS:
            rows[meta] = item(EGGS[name], f"the spawn egg of the original entity Thaumcraft.{name}", evidence=evidence,
                              **label)
        elif name in EGGS_WITHOUT_ITEM:
            rows[meta] = none(f"the port registers the entity {EGGS_WITHOUT_ITEM[name]} without a spawn egg",
                              evidence=evidence, **label)
        else:
            raise SystemExit(f"spawner egg {meta} names the unknown entity {name}")
    return rows


def check_reference(classes: dict, reference: str) -> bool:
    """Class:line or Class.method:first-last names real source lines of an original class."""
    match = re.fullmatch(r"(\w+)(?:\.\w+)?:(\d+)(?:-(\d+))?", reference)
    if not match:
        return False
    owners = [c for name, c in classes.items() if name.rsplit("/", 1)[-1] == match.group(1)]
    first, last = int(match.group(2)), int(match.group(3) or match.group(2))
    lines = {line for owner in owners for method in owner.methods.values() for _start, line in method.lines}
    return bool(owners) and first in lines and last in lines


# ------------------------------------------------------------------ build


def build(jar: Path) -> dict:
    classes = bytecode.disassemble(jar)
    infos = bytecode.fields(classes)
    item_uses = bytecode.usages(classes)
    world_uses = bytecode.world_usages(classes)
    names = lang_names()
    problems: list[str] = []
    rows: list[dict] = []
    fields_out: list[dict] = []
    if set(infos) != set(MAPPING):
        problems.append(f"fields without a mapping: {sorted(set(infos) - set(MAPPING))}; "
                        f"mapped names that are no original field: {sorted(set(MAPPING) - set(infos))}")
    for field, info in infos.items():
        spec = MAPPING.get(field)
        if spec is None:
            continue
        if spec == "spawner_eggs":
            spec = spawner_eggs(classes)
        prefix = "item" if info.kind == "item" else "tile"
        sub_evidence: dict[int, str] = {}
        conditions: dict[int, tuple] = {}
        dynamic = False
        method_name = "getSubItems" if info.kind == "item" else "getSubBlocks"
        if info.cls:
            found = bytecode.find_method(classes, info.cls, bytecode.GET_SUB_ITEMS if info.kind == "item"
                                         else bytecode.GET_SUB_BLOCKS)
            if found and found[0].startswith("thaumcraft/"):
                owner, method = found
                for sub in bytecode.run_subtypes(method, field):
                    if sub.meta is None:
                        dynamic = True
                        continue
                    sub_evidence[sub.meta] = f"{owner.rsplit('/', 1)[-1]}.{method_name}:{sub.line}"
                    conditions[sub.meta] = sub.condition
        lang = lang_metas(names, prefix, info.unlocalized)
        used = {meta: refs for (f, meta), refs in item_uses.items() if f == field}
        placed = {meta: refs for (f, meta), refs in world_uses.items() if f == field}
        sources = set(sub_evidence) | set(lang) | {m for m in used if m != WILDCARD} | set(placed)
        if not sources and not dynamic:
            sources = {0}  # a plain item or block: the default metadata
        for meta in sorted(sources - set(spec)):
            problems.append(f"{field}:{meta} is used by the original but has no row in MAPPING")
        for meta, entry in sorted(spec.items()):
            evidence = []
            if meta in sub_evidence:
                evidence.append(sub_evidence[meta])
            if meta in lang:
                evidence.append(f"en_US.lang {lang[meta]}")
            if meta in used:
                evidence.extend(used[meta][:3])
            if meta in placed:
                evidence.extend(f"{ref} (world)" for ref in placed[meta][:3])
            if entry.get("evidence"):
                if not entry["evidence"].startswith("ConfigEntities:") and not check_reference(classes, entry["evidence"]):
                    problems.append(f"{field}:{meta} evidence {entry['evidence']} names no original source line")
                evidence.append(entry["evidence"])
            if meta not in sources and not entry.get("evidence"):
                problems.append(f"{field}:{meta} is in MAPPING but no original source names it")
            if not evidence:
                evidence.append(f"{info.owner}:{info.line}" if info.line else f"{info.owner} (declared only)")
            rows.append(make_row(info, meta, entry, evidence, names, prefix, conditions.get(meta, ())))
        rows.append(make_wildcard(info, spec, item_uses.get((field, WILDCARD), [])))
        fields_out.append({"kind": info.kind, "field": field, "class": info.cls.replace("/", ".") if info.cls else None,
                           "unlocalized": info.unlocalized, "registry_name": info.registry_name,
                           "item_block": info.item_block.replace("/", ".") if info.item_block else None,
                           "evidence": f"{info.owner}:{info.line}" if info.line else f"{info.owner} (declared only)"})
    problems.extend(reconcile(rows))
    if problems:
        raise SystemExit("\n".join(problems))
    return document(rows, fields_out)


def make_row(info, meta: int, entry: dict, evidence: list[str], names: dict, prefix: str, condition: tuple) -> dict:
    row = {"kind": info.kind, "field": info.field, "meta": meta, "id": entry["id"]}
    if info.kind == "block":
        if entry.get("state"):
            row["state"] = dict(sorted(entry["state"].items()))
        held = entry.get("item", SAME)
        row["item"] = entry["id"] if held is SAME else ns(held)
    if entry.get("component"):
        row["component"] = entry["component"]
    name = entry.get("name") or names.get(entry.get("lang", "")) or name_of(names, prefix, info.unlocalized, meta)
    if name:
        row["name"] = name
    if condition:
        row["condition"] = " && ".join(f"Config.{c}" for c in condition)
    if entry.get("note"):
        row["note"] = entry["note"]
    row["evidence"] = "; ".join(dict.fromkeys(evidence))
    return row


def make_wildcard(info, spec: dict, refs: list[str]) -> dict:
    """OreDictionary.WILDCARD_VALUE: one native id when every variant shares it, else the list."""
    ids = list(dict.fromkeys(e["id"] for _m, e in sorted(spec.items()) if e["id"] is not None))
    row = {"kind": info.kind, "field": info.field, "meta": WILDCARD, "id": ids[0] if len(ids) == 1 else None}
    if info.kind == "block" and len(ids) == 1:
        items = list(dict.fromkeys(ns(e["id"] if e.get("item", SAME) is SAME else e.get("item"))
                                   for _m, e in sorted(spec.items()) if e["id"] is not None))
        items = [i for i in items if i is not None]
        row["item"] = items[0] if len(items) == 1 else None
    if len(ids) != 1:
        row["ids"] = ids
    row["note"] = ("wildcard: every metadata of the field" if len(ids) == 1 else
                   "wildcard: several native ids; ItemApi returns null, use ItemApi.legacyItemIds" if ids else
                   "wildcard: the field has no native counterpart")
    row["evidence"] = "; ".join(refs[:3]) if refs else "OreDictionary.WILDCARD_VALUE"
    return row


def document(rows: list[dict], fields_out: list[dict]) -> dict:
    mapped = [r for r in rows if r["meta"] != WILDCARD]
    return {
        "schema_version": 1,
        "source": "Thaumcraft-1.7.10-4.2.3.5.jar thaumcraft/common/config/ConfigItems and ConfigBlocks, the "
                  "getSubItems/getSubBlocks of each field's class, every constant ItemStack and world placement of "
                  "a field in the original classes (javap -c -l -p, scripts/porting/legacy_bytecode.py) and "
                  "thaumcraft_src/assets/thaumcraft/lang/en_US.lang; written by scripts/porting/extract_legacy_ids.py",
        "policy": {
            "lookup": "ItemApi.getItem/getBlock and ItemsTC/BlocksTC resolve an original field name through these "
                      "rows first. A native id (thaumium_ingot, thaumcraft:thaumium_ingot) keeps resolving as before.",
            "id": "the native item (kind item) or block (kind block) id, or null when 1.21.1 has no counterpart; "
                  "the note says why",
            "state": "block rows: the BlockState properties the original metadata selected; absent means the "
                     "default state",
            "item": "block rows: the item form of the metadata (the original new ItemStack(block, 1, meta)), "
                    "or null when the block has no item",
            "component": "the data component that carries what the original kept in NBT or metadata; described. "
                         "ItemApi.getBlock sets only minecraft:block_state, for a state other than the default "
                         "state when the item is the block's own item",
            "meta": f"{WILDCARD} is OreDictionary.WILDCARD_VALUE: one id when every metadata of the field shares it, "
                    "else id null and the ids list",
            "evidence": "Class.method:lines and Class:line are source lines of the original class files "
                        "(LineNumberTable); en_US.lang names the original language key; (world) marks a world "
                        "placement",
        },
        "counts": {
            "fields": len(fields_out),
            "item_fields": sum(1 for f in fields_out if f["kind"] == "item"),
            "block_fields": sum(1 for f in fields_out if f["kind"] == "block"),
            "rows": len(mapped),
            "mapped": sum(1 for r in mapped if r["id"] is not None),
            "null": sum(1 for r in mapped if r["id"] is None),
            "wildcards": len(rows) - len(mapped),
        },
        "fields": fields_out,
        "rows": rows,
    }


# ------------------------------------------------------------------ reconciliation


def icon_map() -> dict[tuple[str, int], str | None]:
    """The explicit lists and switches of ResearchIconDisplay (thaumcraft namespace only)."""
    text = ICONS.read_text(encoding="utf-8")
    lists = {name: re.findall(r'"([a-z_0-9]+)"', body)
             for name, body in re.findall(r"List<String> (\w+) = List\.of\((.*?)\);", text, re.S)}
    found: dict[tuple[str, int], str | None] = {}
    for case, body in re.findall(r'case "Config(?:Items|Blocks)\.(\w+)" -> (.*?)(?=\n\s+case "Config|\n\s+default -> null;\n\s+\};)',
                                 text, re.S):
        if body.startswith("variant("):
            inner = re.match(r"variant\((\w+|List\.of\((.*?)\)), metadata\)", body, re.S)
            values = lists.get(inner.group(1)) if inner.group(2) is None else re.findall(r'"([a-z_0-9]+)"', inner.group(2))
            for meta, path in enumerate(values):
                found[(case, meta)] = path
        elif body.startswith("metadata == 0"):
            found[(case, 0)] = re.search(r'"([a-z_0-9]+)"', body).group(1)
        else:
            for metas, path in re.findall(r"case ([0-9, ]+) -> \"([a-z_0-9]+)\"", body):
                for meta in metas.split(","):
                    found[(case, int(meta))] = path
    return found


def catalog_lists() -> dict[str, list[str]]:
    text = CATALOG.read_text(encoding="utf-8")
    return {name: re.findall(r'"([a-z_0-9]+)"', body)
            for name, body in re.findall(r"List<String> (\w+) = List\.of\((.*?)\);", text, re.S)}


def reconcile(rows: list[dict]) -> list[str]:
    problems = []
    by_key = {(r["field"], r["meta"]): r for r in rows}

    def form(row):  # the item an icon or an aspect binding names
        return row.get("item", row["id"]) if row["kind"] == "block" else row["id"]

    def agrees(source: str, field: str, meta: int, expected: list[str]) -> None:
        row = by_key.get((field, meta))
        key = f"{source}:{field}:{meta}"
        if row is None:
            problems.append(f"{key} names {expected}, the table has no row")
        elif form(row) not in expected and key not in RECONCILED:
            problems.append(f"{key} names {expected}, the table {form(row)}")

    # ResearchIconDisplay resolves only thaumcraft ids: its absent cases are no disagreement.
    for (field, meta), path in icon_map().items():
        agrees("ResearchIconDisplay", field, meta, [ns(path)])
    lists = catalog_lists()
    for name, field in (("MATERIAL_IDS", "itemResource"), ("SHARD_IDS", "itemShard"), ("ORE_IDS", "blockCustomOre")):
        for meta, path in enumerate(lists.get(name, [])):
            agrees(f"ObjectAspectCatalog.{name}", field, meta, [ns(path)])
    for binding in json.loads(EARLY.read_text(encoding="utf-8"))["bindings"]:
        target = binding.get("original_target", "")
        if not target.startswith(("ConfigItems.", "ConfigBlocks.")):
            continue
        meta = str(binding.get("original_metadata"))
        meta = WILDCARD if meta.startswith("wildcard") else int(meta)
        field = target.split(".", 1)[1]
        row = by_key.get((field, meta))
        if meta == WILDCARD and row is not None and row["id"] is None:
            continue  # a family wildcard: the corpus names one member
        agrees("early_object_aspects", field, meta, binding["modern_ids"])
    return problems


# ------------------------------------------------------------------ static checks (no JAR)


def native_ids() -> tuple[set[str], set[str]]:
    lang = json.loads(NATIVE_LANG.read_text(encoding="utf-8"))
    items = {f"thaumcraft:{k[16:]}" for k in lang if k.startswith("item.thaumcraft.") and k.count(".") == 2}
    blocks = {f"thaumcraft:{k[17:]}" for k in lang if k.startswith("block.thaumcraft.") and k.count(".") == 2}
    return items, blocks


def state_keys(block_id: str) -> set[tuple[str, str]] | None:
    path = BLOCKSTATES / f"{block_id.split(':', 1)[1]}.json"
    if not path.is_file():
        return None
    data = json.loads(path.read_text(encoding="utf-8"))
    pairs = set()
    for key in data.get("variants", {}):
        for part in filter(None, key.split(",")):
            name, value = part.split("=", 1)
            pairs.add((name, value))
    for part in data.get("multipart", []):
        when = part.get("when") or {}
        for clause in when.get("OR", [when]):
            for name, value in clause.items():
                for single_value in str(value).split("|"):
                    pairs.add((name, single_value))
    return pairs


def static_problems(document_: dict) -> list[str]:
    """Checks that need neither the JAR nor a running game."""
    problems = []
    items, blocks = native_ids()
    for row in document_["rows"]:
        where = f"{row['field']}:{row['meta']}"
        ids = [row["id"]] if row["id"] else row.get("ids", [])
        for value in ids:
            if value.startswith("thaumcraft:"):
                known = blocks | items if row["kind"] == "block" else items
                if value not in known:
                    problems.append(f"{where}: {value} has no native language entry")
        if row.get("state") and row["id"]:
            pairs = state_keys(row["id"])
            for name, value in row["state"].items():
                if pairs is not None and pairs and (name, value) not in pairs:
                    problems.append(f"{where}: {row['id']} blockstate has no {name}={value}")
        if row["kind"] == "block" and row.get("item") and row["item"].startswith("thaumcraft:") and row["item"] not in items | blocks:
            problems.append(f"{where}: item {row['item']} has no native language entry")
        if row["id"] is None and row["meta"] != WILDCARD and not row.get("note"):
            problems.append(f"{where}: a null row needs a reason")
    problems.extend(reconcile(document_["rows"]))
    return problems


# ------------------------------------------------------------------ reference document


def cell(value: str | None) -> str:
    if value in (None, ""):
        return ""
    return str(value).replace("|", "\\|").replace("\n", " ")


def render_doc(document_: dict) -> str:
    counts = document_["counts"]
    out = [
        "# Original TC4 item and block names in Thaumcraft 4 Reload",
        "",
        "<!-- Generated by scripts/porting/extract_legacy_ids.py from "
        "common/src/main/resources/data/thaumcraft/tc4/legacy_ids.json. Do not edit by hand; "
        "run the script and `--check` fails while this file is stale. -->",
        "",
        "Thaumcraft 4.2.3.5 (Minecraft 1.7.10) named its content by a field of "
        "`thaumcraft.common.config.ConfigItems` / `ConfigBlocks` plus a metadata value. Minecraft 1.21.1 has no "
        "metadata: every variant is a separate item or block, a block state, or a data component. This table "
        "maps every original field and every metadata value the original used to the native id.",
        "",
        f"It covers {counts['fields']} fields ({counts['item_fields']} items, {counts['block_fields']} blocks) and "
        f"{counts['rows']} field/metadata pairs: {counts['mapped']} map to a native id, {counts['null']} have no "
        f"1.21.1 counterpart (the note says why). Each field also has a wildcard row (`{WILDCARD}`).",
        "",
        "## Use from an add-on",
        "",
        "```java",
        "ItemStack ingot = ItemApi.getItem(\"itemResource\", 2);          // thaumcraft:thaumium_ingot",
        "ItemStack slab  = ItemApi.getBlock(\"blockSlabWood\", 0);        // thaumcraft:greatwood_slab",
        "ResourceLocation id = ItemApi.legacyItemId(\"itemShard\", 6);     // thaumcraft:balance_shard",
        "BlockState state = ItemApi.legacyBlockState(\"blockCosmeticSolid\", 1); // cosmetic_solid_tough[variant=1]",
        "List<ResourceLocation> any = ItemApi.legacyItemIds(\"itemShard\", 32767); // all seven shards",
        "Item thaumometer = ItemsTC.get(\"itemThaumometer\");             // an original field name, metadata 0",
        "Block jar = BlocksTC.get(\"blockJar\");                          // thaumcraft:jar",
        "```",
        "",
        "* A native id (`thaumium_ingot`, `thaumcraft:thaumium_ingot`) keeps resolving as before.",
        "* An unknown name logs the original warning and returns `null`; an original field name never throws.",
        "* `getItem`/`getBlock` return `null` for a row whose id (or, for a block, item) is `null`, and for a "
        "wildcard of a field with several native ids; `legacyItemIds` lists those ids.",
        "* `getBlock` gives a block row whose state is not the default state and whose item is the block's own "
        "item the `minecraft:block_state` component, so placing the stack gives the original state (a top slab, a "
        "cosmetic variant). Other components are only described here: an add-on sets them itself (for example "
        "the aspect of a filled phial).",
        "* The `name` column is the original English name from the 1.7.10 language file.",
        "",
        "Evidence: `Class.method:lines` / `Class:line` are source lines of the original class files "
        "(LineNumberTable), `en_US.lang` names the original language key, `(world)` marks a world placement.",
        "",
    ]
    for kind, title, owner in (("item", "ConfigItems", "items"), ("block", "ConfigBlocks", "blocks")):
        out += [f"## {title} ({owner})", ""]
        if kind == "item":
            out += ["| Original | Name | 1.21.1 id | Component | Note | Evidence |",
                    "|---|---|---|---|---|---|"]
        else:
            out += ["| Original | Name | 1.21.1 block | State | Item | Note | Evidence |",
                    "|---|---|---|---|---|---|---|"]
        for row in document_["rows"]:
            if row["kind"] != kind:
                continue
            original = f"`{row['field']}:{row['meta']}`"
            target = f"`{row['id']}`" if row["id"] else ("any of " + ", ".join(f"`{i}`" for i in row["ids"])
                                                         if row.get("ids") else "—")
            note = row.get("note", "")
            if row.get("condition"):
                note = f"{note} (original condition: {row['condition']})".strip()
            if kind == "item":
                out.append("| " + " | ".join([original, cell(row.get("name")), target, cell(row.get("component")),
                                              cell(note), cell(row["evidence"])]) + " |")
            else:
                state = ", ".join(f"{k}={v}" for k, v in row.get("state", {}).items())
                held = row.get("item")
                item_cell = f"`{held}`" if held else "—"
                if row.get("component"):
                    item_cell += f" ({row['component']})"
                out.append("| " + " | ".join([original, cell(row.get("name")), target, cell(state), cell(item_cell),
                                              cell(note), cell(row["evidence"])]) + " |")
        out.append("")
    out += ["## Original fields", "",
            "| Field | Original class | GameRegistry name | Unlocalized name | Evidence |", "|---|---|---|---|---|"]
    for info in document_["fields"]:
        out.append("| " + " | ".join([f"`{info['field']}`", cell(info["class"] and f"`{info['class']}`"),
                                      cell(info["registry_name"]), cell(info["unlocalized"]),
                                      cell(info["evidence"])]) + " |")
    out.append("")
    return "\n".join(out)


def dump(document_: dict) -> str:
    """Indented JSON with one line per field and per row, so a change reads as a one-line diff."""
    head = {k: v for k, v in document_.items() if k not in ("fields", "rows")}
    text = json.dumps(head, indent=1, ensure_ascii=False)[:-2]
    for key in ("fields", "rows"):
        lines = ",\n".join("  " + json.dumps(entry, ensure_ascii=False) for entry in document_[key])
        text += f',\n "{key}": [\n{lines}\n ]'
    return text + "\n}\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--jar", type=Path, default=None, help=f"the original {JAR_NAME}")
    parser.add_argument("--check", action="store_true", help="fail when the written files are stale")
    parser.add_argument("--doc-only", action="store_true", help="regenerate the reference from the committed JSON")
    args = parser.parse_args()
    jar = args.jar or default_jar()
    replay = not args.doc_only and jar.is_file() and shutil.which("javap") is not None
    if replay:
        document_ = build(jar)
    else:
        if not args.doc_only:
            print(f"bytecode replay skipped: {jar} or javap is not available", file=sys.stderr)
        document_ = json.loads(OUT.read_text(encoding="utf-8"))
    problems = static_problems(document_)
    if problems:
        print("\n".join(problems), file=sys.stderr)
        return 1
    outputs = {DOC: render_doc(document_)}
    if replay:
        outputs[OUT] = dump(document_)
    stale = [path for path, text in outputs.items() if not path.is_file() or path.read_text(encoding="utf-8") != text]
    if args.check:
        for path in stale:
            print(f"stale: {path.relative_to(ROOT)}")
        return 1 if stale else 0
    for path in stale:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(outputs[path], encoding="utf-8")
    counts = document_["counts"]
    print(f"{counts['fields']} fields, {counts['rows']} rows ({counts['mapped']} mapped, {counts['null']} null), "
          f"{counts['wildcards']} wildcards; wrote {len(stale)} files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
