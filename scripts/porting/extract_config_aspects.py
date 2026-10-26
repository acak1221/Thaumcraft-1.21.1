#!/usr/bin/env python3
"""Account for every object registration of the original TC4 ConfigAspects and write the ones the
port still lacks.

The original 4.2.3.5 JAR is read, never changed: ``thaumcraft/common/config/ConfigAspects.class`` is
disassembled with ``javap -c -l -p`` into memory and ``registerItemAspects`` is evaluated
symbolically (string and item stack targets, ``new AspectList()``, ``add``/``merge``/``remove``,
``new AspectList(ItemStack)`` copies). The 1.7.10 SRG field names are resolved with the MCP stable
12-1.7.10 mapping, which is a build-time input and not committed (see ``resolve_srg.py``).

Every registration ends in exactly one of four places, and the script fails if one does not:

* the bounded original resource ``early_object_aspects.json`` already holds it (its values are
  checked against the bytecode here);
* ``ROWS`` maps it to the 1.21.1 items that stand for the 1.7.10 members of the original target;
* ``EXCLUDED`` names why it has no 1.21.1 item;
* it is an ore dictionary name with no 1.21.1 member, which only ``FAMILIES`` can reach.

``FAMILIES`` is the port's own aspect inference (the server setting ``aspect_inference``): the
original ore dictionary registrations, and the wildcard families, reach the members a modern
1.21.1 tag or an explicit list adds. It never replaces a registration of an item.

Usage:
    extract_config_aspects.py --mcp /tmp/mcp-stable [--check]
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAR = ROOT / "Thaumcraft-1.7.10-4.2.3.5.jar"
CLASS = "thaumcraft/common/config/ConfigAspects.class"
EARLY = ROOT / "common/src/main/resources/data/thaumcraft/tc4/early_object_aspects.json"
OUT = ROOT / "common/src/main/resources/data/thaumcraft/tc4/original_object_aspects.json"
TAGS = ROOT / "common/src/main/resources/data/thaumcraft/tags/item/aspect_family"
ASPECT_SOURCE = ROOT / "common/src/main/java/thaumcraft/api/aspects/Aspect.java"
WILDCARD = 32767

# The resolved tags of a COMPLEX registration that a later row copies with new AspectList(ItemStack).
# generateTags derives them from the recipe, so they are written here with the recipe that gives them.
STATIC_BASES: dict[str, dict[str, int]] = {
    # Four sand (terra 1, perditio 1 each) craft one sandstone in 1.7.10 and in 1.21.1: 4 x 0.75 = 3.
    "Blocks.sandstone@0": {"terra": 3, "perditio": 3},
}

WOODS_1_7 = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
          "cyan", "purple", "blue", "brown", "green", "red", "black"]


def mc(*names: str) -> list[str]:
    return [f"minecraft:{name}" for name in names]


# Registration key -> the 1.21.1 items of the 1.7.10 members. "base" names an item whose resolved
# tags the original copied at run time (new AspectList(ItemStack)); the listed aspects are added.
ROWS: dict[str, dict] = {
    "slabWood": {"ids": mc(*[f"{w}_slab" for w in WOODS_1_7]),
                 "why": "Forge 1.7.10 registers wooden_slab wildcard (metadata 0-5) as slabWood"},
    "stairWood": {"ids": mc(*[f"{w}_stairs" for w in WOODS_1_7]),
                  "why": "Forge 1.7.10 registers the six wooden stairs as stairWood"},
    "treeSapling": {"ids": mc(*[f"{w}_sapling" for w in WOODS_1_7]),
                    "why": "Forge 1.7.10 registers sapling wildcard (metadata 0-5) as treeSapling"},
    "treeLeaves": {"ids": mc(*[f"{w}_leaves" for w in WOODS_1_7]),
                   "why": "Forge 1.7.10 registers leaves and leaves2 wildcard as treeLeaves"},
    "dyes": {"ids": mc("ink_sac", "red_dye", "green_dye", "cocoa_beans", "purple_dye", "cyan_dye",
                       "light_gray_dye", "gray_dye", "pink_dye", "lime_dye", "yellow_dye", "light_blue_dye",
                       "magenta_dye", "orange_dye", "bone_meal"),
             "why": "ConfigAspects.dyes loop; Forge 1.7.10 dyeBlack..dyeWhite are dye metadata 0-15, flattened "
                    "by 1.13; dyeBlue (lapis) is already an early row"},
    "ingotCopper": {"ids": mc("copper_ingot"), "why": "Config.foundCopperIngot branch; copper is vanilla in 1.21.1"},
    "oreCopper": {"ids": mc("copper_ore"), "why": "Config.foundCopperIngot branch; copper is vanilla in 1.21.1"},
    "nuggetTin": {"ids": ["thaumcraft:tin_nugget"], "why": "Config.foundTinIngot branch; the native nugget exists"},
    "nuggetSilver": {"ids": ["thaumcraft:silver_nugget"], "why": "Config.foundSilverIngot branch; the native nugget exists"},
    "nuggetLead": {"ids": ["thaumcraft:lead_nugget"], "why": "Config.foundLeadIngot branch; the native nugget exists"},
    "ConfigItems.itemNugget@18": {"ids": ["thaumcraft:cluster_tin"], "why": "Config.foundTinIngot branch; native cluster"},
    "ConfigItems.itemNugget@19": {"ids": ["thaumcraft:cluster_silver"], "why": "Config.foundSilverIngot branch; native cluster"},
    "ConfigItems.itemNugget@20": {"ids": ["thaumcraft:cluster_lead"], "why": "Config.foundLeadIngot branch; native cluster"},
    "Items.fish@32767": {"ids": mc("salmon", "tropical_fish", "pufferfish"),
                         "why": "fish metadata 1-3; the early row holds metadata 0 (cod) only"},
    "Blocks.monster_egg@32767": {"ids": mc("infested_cobblestone", "infested_stone_bricks", "infested_mossy_stone_bricks",
                                           "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks"),
                                 "why": "monster_egg metadata 1-5; the early row holds metadata 0 (infested_stone) only"},
    "Blocks.dirt@32767": {"ids": mc("dirt", "coarse_dirt", "podzol"), "why": "dirt metadata 0-2"},
    "Blocks.dirt@2": {"ids": mc("podzol"), "why": "dirt metadata 2 is podzol"},
    "Blocks.grass@0": {"ids": mc("grass_block"), "why": "grass is grass_block"},
    "Blocks.mycelium@0": {"ids": mc("mycelium"), "why": "plain identifier"},
    "Blocks.clay@32767": {"ids": mc("clay"), "why": "plain identifier"},
    "Blocks.stained_glass@32767": {"ids": mc(*[f"{c}_stained_glass" for c in COLORS]),
                                   "why": "stained_glass metadata 0-15"},
    "Blocks.obsidian@32767": {"ids": mc("obsidian"), "why": "plain identifier"},
    "Blocks.sandstone@32767": {"ids": mc("sandstone"),
                               "why": "sandstone metadata 0; the later early rows hold metadata 1 and 2"},
    "Blocks.tallgrass@32767": {"ids": mc("short_grass", "fern"),
                               "why": "tallgrass metadata 1-2; metadata 0 became dead_bush, which the later deadbush row replaces"},
    "Blocks.double_plant@32767": {"ids": mc("sunflower", "lilac", "tall_grass", "large_fern", "rose_bush", "peony"),
                                  "why": "double_plant metadata 0-5"},
    "Blocks.waterlily@32767": {"ids": mc("lily_pad"), "why": "waterlily is lily_pad"},
    "Blocks.deadbush@32767": {"ids": mc("dead_bush"), "why": "plain identifier"},
    "Blocks.vine@32767": {"ids": mc("vine"), "why": "plain identifier"},
    "Items.melon@32767": {"ids": mc("melon_slice"), "why": "the 1.7.10 item melon is the slice; the block is melon_block"},
    "Blocks.red_flower@32767": {"ids": mc("poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
                                          "white_tulip", "pink_tulip", "oxeye_daisy"),
                                "why": "red_flower metadata 0-8"},
    "Blocks.yellow_flower@32767": {"ids": mc("dandelion"), "why": "yellow_flower is dandelion"},
    "Blocks.cactus@0": {"ids": mc("cactus"), "why": "plain identifier"},
    "Blocks.brown_mushroom@0": {"ids": mc("brown_mushroom"), "why": "plain identifier"},
    "Blocks.red_mushroom@0": {"ids": mc("red_mushroom"), "why": "plain identifier"},
    "Blocks.brown_mushroom_block@32767": {"ids": mc("brown_mushroom_block"),
                                          "why": "the stem metadata became mushroom_stem, shared with the red block"},
    "Blocks.red_mushroom_block@32767": {"ids": mc("red_mushroom_block"),
                                        "why": "the stem metadata became mushroom_stem, shared with the brown block"},
    "Items.apple@0": {"ids": mc("apple"), "why": "plain identifier"},
    "Blocks.pumpkin@32767": {"ids": mc("pumpkin", "carved_pumpkin"),
                             "why": "the 1.7.10 pumpkin has a face; 1.13 split it into pumpkin and carved_pumpkin"},
    "Blocks.melon_block@32767": {"ids": mc("melon"), "why": "melon_block is the melon block"},
    "Blocks.ice@32767": {"ids": mc("ice"), "why": "plain identifier"},
    "Items.cooked_fish@32767": {"ids": mc("cooked_cod", "cooked_salmon"), "why": "cooked_fish metadata 0-1"},
    "Items.cooked_chicken@32767": {"ids": mc("cooked_chicken"), "why": "plain identifier"},
    "Items.cooked_porkchop@32767": {"ids": mc("cooked_porkchop"), "why": "plain identifier"},
    "Items.cooked_beef@32767": {"ids": mc("cooked_beef"), "why": "plain identifier"},
    "Items.record_far@0": {"ids": mc("music_disc_far"), "why": "record_far is music_disc_far"},
    "Items.nether_star@0": {"ids": mc("nether_star"), "why": "plain identifier"},
    "Items.enchanted_book@0": {"ids": mc("enchanted_book"), "base": "minecraft:book",
                               "why": "the additions are new AspectList(new ItemStack(Items.book)): the book's tags"},
    "Blocks.dragon_egg@0": {"ids": mc("dragon_egg"), "why": "plain identifier"},
    "Blocks.end_portal_frame@32767": {"ids": mc("end_portal_frame"), "why": "plain identifier"},
    "Blocks.chest@32767": {"ids": mc("chest"), "why": "the 1.7.10 trapped chest is a separate block without a row"},
    "Items.golden_apple@0": {"ids": mc("golden_apple"), "why": "golden_apple metadata 0"},
    "Items.golden_apple@1": {"ids": mc("enchanted_golden_apple"),
                             "why": "golden_apple metadata 1; 1.21.1 has no recipe, so the generated base is empty"},
    "Items.minecart@0": {"ids": mc("minecart"), "why": "plain identifier"},
    "Items.boat@0": {"ids": mc("oak_boat"), "why": "the 1.7.10 boat is the oak boat"},
    "Items.bucket@0": {"ids": mc("bucket"), "why": "plain identifier"},
    "Items.water_bucket@0": {"ids": mc("water_bucket"), "why": "the bucket row before it is literal"},
    "Items.lava_bucket@0": {"ids": mc("lava_bucket"), "why": "the bucket row before it is literal"},
    "Items.cake@0": {"ids": mc("cake"), "why": "plain identifier"},
    "Blocks.rail@32767": {"ids": mc("rail"), "why": "plain identifier"},
    "Blocks.redstone_torch@32767": {"ids": mc("redstone_torch"), "why": "the lit torch is the item"},
    "Blocks.sticky_piston@32767": {"ids": mc("sticky_piston"), "why": "plain identifier"},
    "Blocks.enchanting_table@0": {"ids": mc("enchanting_table"), "why": "plain identifier"},
    "Items.clock@0": {"ids": mc("clock"), "why": "plain identifier"},
    "Blocks.anvil@0,1,2": {"ids": mc("anvil", "chipped_anvil", "damaged_anvil"),
                           "why": "registerObjectTag(ItemStack, int[] {0, 1, 2}): anvil damage metadata 0-2"},
    "Blocks.beacon@0": {"ids": mc("beacon"), "why": "plain identifier"},
    "Items.golden_carrot@0": {"ids": mc("golden_carrot"), "why": "plain identifier"},
    "Blocks.ender_chest@32767": {"ids": mc("ender_chest"), "why": "plain identifier"},
    "Blocks.hopper@32767": {"ids": mc("hopper"), "why": "plain identifier"},
    "Blocks.dropper@32767": {"ids": mc("dropper"), "why": "plain identifier"},
    "Blocks.dispenser@32767": {"ids": mc("dispenser"), "why": "plain identifier"},
    "Blocks.tripwire_hook@32767": {"ids": mc("tripwire_hook"), "why": "plain identifier"},
    "Blocks.daylight_detector@32767": {"ids": mc("daylight_detector"), "why": "plain identifier"},
    "ConfigBlocks.blockTable@0": {"ids": ["thaumcraft:table"], "why": "the plain table, metadata 0"},
    "ConfigBlocks.blockTable@15": {"ids": ["thaumcraft:arcane_workbench"], "base": "thaumcraft:table",
                                   "why": "new AspectList(table metadata 0) plus fabrico4"},
    "ConfigBlocks.blockTable@2": {"ids": ["thaumcraft:research_table"], "base": "thaumcraft:table",
                                  "why": "new AspectList(table metadata 0) plus cognitio4"},
}

EXCLUDED: dict[str, str] = {
    "Blocks.lit_redstone_ore@0": "the lit ore is a separate legacy block; the item is the unlit ore of oreRedstone",
    "Blocks.fire@32767": "1.21.1 has no fire item",
    "Blocks.water@32767": "1.21.1 has no water block item",
    "Blocks.lava@32767": "1.21.1 has no lava block item",
    "Blocks.portal@32767": "1.21.1 has no nether portal item",
    "Blocks.end_portal@32767": "1.21.1 has no end portal item",
    "Blocks.lit_furnace@32767": "the lit furnace is a separate legacy block; the item is the furnace row",
    "Items.potionitem@0": "the native resolver computes the potion tags from the potion contents",
    "Items.potionitem@loop": "the native resolver computes the potion tags from the potion contents",
    "ConfigBlocks.blockCosmeticSolid@0": "grouped flattening: cosmetic_solid_tough also hosts metadata 1 and 8",
    "ConfigBlocks.blockCosmeticSolid@11": "grouped flattening: cosmetic_solid hosts ten metadata variants",
    "ConfigBlocks.blockCosmeticSolid@12": "grouped flattening: cosmetic_solid hosts ten metadata variants",
    "ConfigBlocks.blockEldritch@32767": "grouped flattening: six native eldritch items host the eleven variants",
    "ConfigBlocks.blockEldritch@3": "grouped flattening: eldritch_doorway also hosts the lock",
    "ConfigBlocks.blockEldritch@4": "grouped flattening: eldritch_crust also hosts the glyphed stone",
    "ConfigBlocks.blockEldritch@5": "grouped flattening: eldritch_crust also hosts the glowing crust",
    "ConfigBlocks.blockEldritch@6": "grouped flattening: eldritch_spawner also hosts the runed trap",
    "ConfigBlocks.blockEldritchPortal@0": "the eldritch portal has no item",
    "ConfigBlocks.blockAiry@2": "the airy blocks have no item",
    "ConfigBlocks.blockAiry@3": "the airy blocks have no item",
    "ConfigBlocks.blockWoodenDevice@8": "the banner variant of wooden_device is a grouped flattening",
    "ConfigItems.itemResource@14": "salis mundus: the native resolver evaluates the original expression",
    "ConfigBlocks.blockMetalDevice@0": "crucible: the native resolver evaluates the original expression",
    "ConfigItems.itemThaumonomicon@32767": "the native resolver evaluates the original bookshelf expression",
    "ConfigItems.itemCrystalEssence@0": "ObjectAspectCatalog keeps the explicit empty crystal essence base",
}
# Ore dictionary names with no 1.21.1 member: only an aspect family (a mod's tag) can reach them.
FAMILY_ONLY = {"dustIron", "dustGold", "dustCopper", "ingotTin", "dustTin", "oreTin", "ingotSilver",
               "dustSilver", "oreSilver", "ingotLead", "dustLead", "oreLead"}

# Original ore dictionary names and wildcard families -> the members a 1.21.1 tag or list adds.
# The tag file holds the 1.7.10 members, the modern vanilla members, and an optional convention tag.
FAMILIES: dict[str, dict] = {
    "stone": {"tag": "stone", "values": mc("stone", "deepslate", "tuff"), "optional": ["c:stones"]},
    "cobblestone": {"tag": "cobblestone", "values": mc("cobblestone", "cobbled_deepslate"), "optional": ["c:cobblestones"]},
    "logWood": {"tag": "log_wood", "values": [], "optional": ["minecraft:logs"]},
    "plankWood": {"tag": "plank_wood", "values": [], "optional": ["minecraft:planks"]},
    "slabWood": {"tag": "slab_wood", "values": [], "optional": ["minecraft:wooden_slabs"]},
    "stairWood": {"tag": "stair_wood", "values": [], "optional": ["minecraft:wooden_stairs"]},
    "stickWood": {"tag": "stick_wood", "values": mc("stick"), "optional": ["c:rods/wooden"]},
    "treeSapling": {"tag": "tree_sapling", "values": [], "optional": ["minecraft:saplings"]},
    "treeLeaves": {"tag": "tree_leaves", "values": [], "optional": ["minecraft:leaves"]},
    "dyes": {"tag": "dye", "values": mc("ink_sac", "cocoa_beans", "lapis_lazuli", "bone_meal",
                                         *[f"{c}_dye" for c in COLORS]), "optional": ["c:dyes"]},
    "oreLapis": {"tag": "ore_lapis", "values": mc("lapis_ore", "deepslate_lapis_ore"), "optional": ["c:ores/lapis"]},
    "oreDiamond": {"tag": "ore_diamond", "values": mc("diamond_ore", "deepslate_diamond_ore"), "optional": ["c:ores/diamond"]},
    "gemDiamond": {"tag": "gem_diamond", "values": mc("diamond"), "optional": ["c:gems/diamond"]},
    "oreRedstone": {"tag": "ore_redstone", "values": mc("redstone_ore", "deepslate_redstone_ore"), "optional": ["c:ores/redstone"]},
    "oreEmerald": {"tag": "ore_emerald", "values": mc("emerald_ore", "deepslate_emerald_ore"), "optional": ["c:ores/emerald"]},
    "gemEmerald": {"tag": "gem_emerald", "values": mc("emerald"), "optional": ["c:gems/emerald"]},
    "oreQuartz": {"tag": "ore_quartz", "values": mc("nether_quartz_ore"), "optional": ["c:ores/quartz"]},
    "nuggetIron": {"tag": "nugget_iron", "values": mc("iron_nugget"), "optional": ["c:nuggets/iron"]},
    "oreIron": {"tag": "ore_iron", "values": mc("iron_ore", "deepslate_iron_ore"), "optional": ["c:ores/iron"]},
    "dustIron": {"tag": "dust_iron", "values": [], "optional": ["c:dusts/iron"]},
    "oreGold": {"tag": "ore_gold", "values": mc("gold_ore", "deepslate_gold_ore", "nether_gold_ore"), "optional": ["c:ores/gold"]},
    "dustGold": {"tag": "dust_gold", "values": [], "optional": ["c:dusts/gold"]},
    "dustRedstone": {"tag": "dust_redstone", "values": mc("redstone"), "optional": ["c:dusts/redstone"]},
    "dustGlowstone": {"tag": "dust_glowstone", "values": mc("glowstone_dust"), "optional": ["c:dusts/glowstone"]},
    "glowstone": {"tag": "glowstone", "values": mc("glowstone"), "optional": []},
    "nuggetCopper": {"tag": "nugget_copper", "values": ["thaumcraft:copper_nugget"], "optional": ["c:nuggets/copper"]},
    "ingotCopper": {"tag": "ingot_copper", "values": mc("copper_ingot"), "optional": ["c:ingots/copper"]},
    "dustCopper": {"tag": "dust_copper", "values": [], "optional": ["c:dusts/copper"]},
    "oreCopper": {"tag": "ore_copper", "values": mc("copper_ore", "deepslate_copper_ore"), "optional": ["c:ores/copper"]},
    "nuggetTin": {"tag": "nugget_tin", "values": ["thaumcraft:tin_nugget"], "optional": ["c:nuggets/tin"]},
    "ingotTin": {"tag": "ingot_tin", "values": [], "optional": ["c:ingots/tin"]},
    "dustTin": {"tag": "dust_tin", "values": [], "optional": ["c:dusts/tin"]},
    "oreTin": {"tag": "ore_tin", "values": [], "optional": ["c:ores/tin"]},
    "nuggetSilver": {"tag": "nugget_silver", "values": ["thaumcraft:silver_nugget"], "optional": ["c:nuggets/silver"]},
    "ingotSilver": {"tag": "ingot_silver", "values": [], "optional": ["c:ingots/silver"]},
    "dustSilver": {"tag": "dust_silver", "values": [], "optional": ["c:dusts/silver"]},
    "oreSilver": {"tag": "ore_silver", "values": [], "optional": ["c:ores/silver"]},
    "nuggetLead": {"tag": "nugget_lead", "values": ["thaumcraft:lead_nugget"], "optional": ["c:nuggets/lead"]},
    "ingotLead": {"tag": "ingot_lead", "values": [], "optional": ["c:ingots/lead"]},
    "dustLead": {"tag": "dust_lead", "values": [], "optional": ["c:dusts/lead"]},
    "oreLead": {"tag": "ore_lead", "values": [], "optional": ["c:ores/lead"]},
    # Wildcard families: the post-1.7 members of an original metadata family.
    "Blocks.stained_glass@32767": {"tag": "stained_glass", "values": [], "optional": ["c:glass_blocks/cheap"]},
    "Blocks.red_flower@32767": {"tag": "small_flower", "values": mc("cornflower", "lily_of_the_valley", "torchflower"),
                                "optional": []},
    "Blocks.double_plant@32767": {"tag": "tall_flower", "values": mc("pitcher_plant"), "optional": []},
    "Blocks.brown_mushroom_block@32767": {"tag": "mushroom_block", "values": mc("mushroom_stem"), "optional": []},
    "Items.fish@32767": {"tag": "raw_fish", "values": [], "optional": ["c:foods/raw_fish"]},
    "Items.cooked_fish@32767": {"tag": "cooked_fish", "values": [], "optional": ["c:foods/cooked_fish"]},
    "Blocks.monster_egg@32767": {"tag": "infested_block", "values": mc("infested_deepslate"), "optional": []},
    "Items.boat@0": {"tag": "boat", "values": [], "optional": ["minecraft:boats"]},
    "Items.record_11@0": {"tag": "music_disc", "values": [], "optional": ["minecraft:creeper_drop_music_discs", "c:music_discs"]},
    "Blocks.trapdoor@32767": {"tag": "wooden_trapdoor", "values": [], "optional": ["minecraft:wooden_trapdoors"]},
    "Blocks.wooden_button@32767": {"tag": "wooden_button", "values": [], "optional": ["minecraft:wooden_buttons"]},
    "Blocks.wooden_pressure_plate@32767": {"tag": "wooden_pressure_plate", "values": [],
                                           "optional": ["minecraft:wooden_pressure_plates"]},
    "Blocks.fence_gate@32767": {"tag": "fence_gate", "values": [], "optional": ["minecraft:fence_gates"]},
    "Items.wooden_door@0": {"tag": "wooden_door", "values": [], "optional": ["minecraft:wooden_doors"]},
    "Blocks.chest@32767": {"tag": "chest", "values": mc("trapped_chest"), "optional": []},
    "Blocks.rail@32767": {"tag": "rail", "values": [], "optional": ["minecraft:rails"]},
    "Blocks.coal_ore@0": {"tag": "ore_coal", "values": mc("deepslate_coal_ore"), "optional": ["c:ores/coal"]},
    "Blocks.glass@32767": {"tag": "glass", "values": mc("tinted_glass"), "optional": []},
}


def load_names(mcp: Path) -> dict[str, str]:
    names = {}
    for file in ("fields.csv", "methods.csv"):
        with (mcp / file).open(encoding="utf-8", errors="replace", newline="") as handle:
            for row in csv.reader(handle):
                if len(row) >= 2:
                    names[row[0].strip()] = row[1].strip()
    return names


def disassemble(jar: Path) -> str:
    with zipfile.ZipFile(jar) as archive, tempfile.TemporaryDirectory() as temporary:
        target = Path(temporary) / "ConfigAspects.class"
        target.write_bytes(archive.read(CLASS))
        return subprocess.run(["javap", "-c", "-l", "-p", str(target)], capture_output=True, text=True,
                              check=True).stdout


INSTRUCTION = re.compile(r"^\s+(\d+): (\w+)\s*(.*?)(?:\s*// (.*))?$")


def registrations(text: str, names: dict[str, str], aspect_tags: dict[str, str]) -> list[dict]:
    lines = text.split("\n")
    start = next(i for i, line in enumerate(lines) if "private static void registerItemAspects()" in line)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("  ") and not lines[i].startswith("   ")
                and ("(" in lines[i] or lines[i].strip().startswith("static {"))), len(lines))
    table = [(int(m.group(2)), int(m.group(1))) for m in re.finditer(r"line (\d+): (\d+)", "\n".join(lines[start:end]))]
    table.sort()

    def line_of(offset: int) -> int:
        return max((line for start_offset, line in table if start_offset <= offset), default=0)

    stack: list = []
    arrays: list = []
    literal: dict[str, dict] = {}
    found = []
    for raw in lines[start + 2:end]:
        match = INSTRUCTION.match(raw)
        if not match:
            continue
        offset, op, arg, comment = int(match.group(1)), match.group(2), match.group(3), match.group(4) or ""
        if op.startswith("ldc"):
            string = re.match(r"String (.*)", comment)
            stack.append(("str", string.group(1)) if string else ("const", comment))
        elif op.startswith("iconst_"):
            stack.append(("int", -1 if op == "iconst_m1" else int(op[-1])))
        elif op in ("bipush", "sipush"):
            stack.append(("int", int(arg)))
        elif op == "new":
            stack.append(("new", comment.split("/")[-1]))
        elif op == "dup":
            stack.append(stack[-1])
        elif op == "newarray":
            stack.pop()
            arrays.append([])
            stack.append(("array", arrays[-1]))
        elif op == "iastore":
            value, _index, array = stack.pop(), stack.pop(), stack.pop()
            array[1].append(value[1])
        elif op == "getstatic":
            field = re.match(r"Field (?:(\S+)\.)?([^.:\s]+):", comment)
            owner = (field.group(1) or "ConfigAspects").split("/")[-1]
            name = names.get(field.group(2), field.group(2))
            stack.append(("aspect", aspect_tags[name]) if owner == "Aspect" else ("obj", f"{owner}.{name}"))
        elif op in ("invokespecial", "invokevirtual", "invokestatic"):
            signature = comment.replace("Method ", "")
            if 'AspectList."<init>":()V' in signature:
                stack.pop(); stack.pop(); stack.append(("list", {}, None))
            elif 'AspectList."<init>":(Lnet/minecraft/item/ItemStack;)V' in signature:
                source = stack.pop(); stack.pop(); stack.pop()
                key = source[1]
                # getObjectTags(stack) finds the exact metadata first, then the wildcard registration.
                wildcard = key.rsplit("@", 1)[0] + f"@{WILDCARD}"
                copied = literal.get(key, literal.get(wildcard, STATIC_BASES.get(key)))
                stack.append(("list", dict(copied) if copied is not None else {}, None if copied is not None else key))
            elif 'ItemStack."<init>"' in signature:
                params = re.findall(r"L[^;]+;|I", re.match(r".*\((.*)\)V", signature).group(1))
                args = [stack.pop() for _ in params][::-1]
                stack.pop(); stack.pop()
                meta = (args[2][1] if args[2][0] == "int" else "var") if len(args) == 3 else 0
                stack.append(("stack", f"{args[0][1]}@{meta}"))
            elif re.search(r"AspectList\.(add|merge|remove):\(Lthaumcraft/api/aspects/Aspect;I\)", signature):
                amount, aspect, listed = stack.pop(), stack.pop(), stack.pop()
                kind = re.search(r"AspectList\.(add|merge|remove)", signature).group(1)
                values = dict(listed[1])
                if kind == "add":
                    values[aspect[1]] = values.get(aspect[1], 0) + amount[1]
                elif kind == "merge":
                    values[aspect[1]] = max(values.get(aspect[1], 0), amount[1])
                else:  # Original AspectList.remove(Aspect, int): drops the aspect at zero or below.
                    left = values.get(aspect[1], 0) - amount[1]
                    if left <= 0:
                        values.pop(aspect[1], None)
                    else:
                        values[aspect[1]] = left
                stack.append(("list", values, listed[2]))
            elif "AspectList.remove:(Lthaumcraft/api/aspects/Aspect;)" in signature:
                aspect, listed = stack.pop(), stack.pop()
                values = dict(listed[1]); values.pop(aspect[1], None)
                stack.append(("list", values, listed[2]))
            elif "ThaumcraftApi.register" in signature and "EntityTag" not in signature:
                params = re.findall(r"L[^;]+;|\[I", re.match(r".*\((.*)\)V", signature).group(1))
                args = [stack.pop() for _ in params][::-1]
                target = args[0]
                if target[0] == "stack" and len(args) == 3:
                    target = ("stack", target[1].rsplit("@", 1)[0] + "@" + ",".join(map(str, args[1][1])))
                if target[0] == "str" and target[1].startswith("<"):
                    key = "dyes"
                elif target[0] == "stack" and target[1].endswith("@var"):
                    key = target[1].split("@")[0] + "@loop"
                else:
                    key = target[1]
                listed = args[-1]
                kind = "complex" if "Complex" in signature else "direct"
                found.append({"key": key, "registration": kind, "line": line_of(offset), "offset": offset,
                              "aspects": listed[1], "base": listed[2]})
                if kind == "direct" and listed[2] is None:
                    literal[key] = dict(listed[1])
                stack = []
            else:
                stack = [("unknown", signature)]
        elif op == "aaload":
            stack.pop(); array = stack.pop(); stack.append(("str", "<dyes>"))
        elif op.startswith(("iload", "aload")):
            stack.append(("var", op + arg))
        elif op.startswith(("istore", "astore")) or op == "pop":
            if stack:
                stack.pop()
        elif op == "invokeinterface":
            stack = [("unknown", comment)]
    return found


def early_keys(names: dict[str, str]) -> dict[str, dict]:
    keys = {}
    for row in json.loads(EARLY.read_text(encoding="utf-8"))["bindings"]:
        target = row["original_target"]
        if target.startswith("ore_dictionary:"):
            key = target.split(":", 1)[1]
            if key.startswith("dye"):
                key = "dyes:" + key
        else:
            owner, field = target.split(".", 1)
            meta = str(row.get("original_metadata")).replace("wildcard:", "")
            key = f"{owner}.{names.get(field, field)}@{meta}"
        keys[key] = row
    return keys


def build(mcp: Path) -> tuple[dict, dict[str, dict]]:
    names = load_names(mcp)
    aspect_tags = dict(re.findall(r'public static final Aspect ([A-Z_]+) = new Aspect\("(\w+)"',
                                  ASPECT_SOURCE.read_text(encoding="utf-8")))
    found = registrations(disassemble(JAR), names, aspect_tags)
    early = early_keys(names)
    rows, families, problems = [], [], []
    accounted = set()
    for registration in found:
        key = registration["key"]
        lookup = "dyes:dyeBlue" if key == "dyes" else key
        if lookup in early:
            row = early[lookup]
            if registration["base"] is None and row["aspects"] != registration["aspects"]:
                problems.append(f"early row {key} differs from the bytecode: {row['aspects']} != {registration['aspects']}")
            if row["registration"] != registration["registration"]:
                problems.append(f"early row {key} is {row['registration']}, the bytecode {registration['registration']}")
            accounted.add(key)
        if key in ROWS:
            spec = ROWS[key]
            base = spec.get("base")
            if registration["base"] is not None and base is None:
                problems.append(f"{key} copies {registration['base']} at run time; ROWS must name its base item")
            out = {"original_jar_line": registration["line"], "bytecode_offset": registration["offset"],
                   "registration": registration["registration"], "original_target": key,
                   "modern_ids": spec["ids"], "aspects": dict(sorted(registration["aspects"].items())),
                   "evidence": spec["why"]}
            if base is not None:
                out["base_item"] = base
            rows.append(out)
            accounted.add(key)
        if key in EXCLUDED:
            accounted.add(key)
        if key in FAMILY_ONLY:
            accounted.add(key)
        if key in FAMILIES:
            spec = FAMILIES[key]
            families.append({"original_jar_line": registration["line"], "bytecode_offset": registration["offset"],
                             "registration": registration["registration"], "original_target": key,
                             "tag": f"thaumcraft:aspect_family/{spec['tag']}",
                             "aspects": dict(sorted(registration["aspects"].items())),
                             **({"base_item": ROWS[key]["base"]} if key in ROWS and "base" in ROWS[key] else {})})
        if key not in accounted:
            problems.append(f"unaccounted original registration {key} at line {registration['line']}")
    for key in list(ROWS) + list(EXCLUDED) + list(FAMILIES):
        if not any(r["key"] == key for r in found):
            problems.append(f"{key} names no original registration")
    if problems:
        raise SystemExit("\n".join(problems))
    document = {
        "schema_version": 1,
        "source": "Thaumcraft-1.7.10-4.2.3.5.jar thaumcraft/common/config/ConfigAspects.registerItemAspects, "
                  "read with javap -c -l and evaluated by scripts/porting/extract_config_aspects.py; 1.7.10 SRG "
                  "names resolved with MCP stable 12-1.7.10",
        "policy": {
            "rows": "Original registrations that early_object_aspects.json does not hold, mapped to the 1.21.1 items "
                    "of their 1.7.10 members. They apply after the early rows and never replace one: no id of a row "
                    "here has an early row. base_item is the original new AspectList(ItemStack) copy that the "
                    "native resolver evaluates at run time.",
            "families": "The port's own aspect inference (server setting aspect_inference, on by default): an "
                        "item with no registration takes the row of the LAST family tag that holds it, in the "
                        "original registration order, before any recipe derivation, as the original ore "
                        "dictionary registrations did for the members of a name.",
            "accounting": f"{len(found)} original object registrations; every one is an early row, a row here, "
                          "an excluded registration or an ore name without a 1.21.1 member.",
            "excluded": EXCLUDED,
        },
        "rows": rows,
        "families": families,
    }
    early_ids = {i for row in early.values() for i in row["modern_ids"]}
    clash = sorted({i for row in rows for i in row["modern_ids"]} & early_ids)
    if clash:
        raise SystemExit(f"rows here may not repeat an early id: {clash}")
    tags = {}
    for spec in FAMILIES.values():
        values = list(spec["values"]) + [{"id": f"#{tag}", "required": False} for tag in spec["optional"]]
        tags[spec["tag"]] = {"replace": False, "values": values}
    return document, tags


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--mcp", type=Path, default=Path("/tmp/mcp-stable"))
    parser.add_argument("--check", action="store_true", help="fail when the written files differ")
    args = parser.parse_args()
    document, tags = build(args.mcp)
    outputs = {OUT: json.dumps(document, indent=2, ensure_ascii=False) + "\n"}
    for name, tag in tags.items():
        outputs[TAGS / f"{name}.json"] = json.dumps(tag, indent=2) + "\n"
    stale = [path for path, text in outputs.items() if not path.exists() or path.read_text(encoding="utf-8") != text]
    if args.check:
        for path in stale:
            print(f"stale: {path.relative_to(ROOT)}")
        return 1 if stale else 0
    TAGS.mkdir(parents=True, exist_ok=True)
    for path in stale:
        path.write_text(outputs[path], encoding="utf-8")
    print(f"{len(document['rows'])} rows, {len(document['families'])} families, {len(tags)} tags; wrote {len(stale)} files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
