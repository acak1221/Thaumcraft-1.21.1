package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.wands.WandData;

/** Client-only, explicit aliases for original icons whose native items exist. */
final class ResearchIconDisplay {
    // Provenance is NOT executable Java. Only these fields, literal counts 1..64
    // and metadata 0..18 are accepted; every field/metadata pair is whitelisted below.
    private static final Pattern STACK = Pattern.compile("new ItemStack\\((ConfigItems\\.(?:itemResource|itemShard|itemNugget|itemWandRod|itemWandCap|itemInkwell|itemThaumonomicon|itemThaumometer|itemEssence|itemFocusFire|itemFocusPortableHole|itemFocusShock|itemFocusExcavation|itemFocusFrost|itemGoggles|itemChestRobe|itemLegsRobe|itemBootsRobe|itemSwordThaumium|itemPickThaumium|itemAxeThaumium|itemAxeElemental|itemPickElemental|itemHoeElemental|itemShovelElemental|itemSwordElemental|itemHelmetVoidRobe|itemHelmetFortress|itemShovelThaumium|itemHoeThaumium|itemHelmetThaumium|itemChestThaumium|itemLegsThaumium|itemBootsThaumium|itemGolemPlacer|itemGolemCore|itemGolemUpgrade|itemGolemDecoration|itemGolemBell|itemTrunkSpawner|itemZombieBrain)"
            + "|ConfigBlocks\\.(?:blockTable|blockCosmeticSolid|blockMetalDevice|blockStoneDevice|blockWoodenDevice|blockJar|blockTube|blockMagicalLog|blockMagicalLeaves|blockCustomPlant|blockCustomOre|blockChestHungry|blockTaint))"
            + "(?:,\\s*([1-9][0-9]?)(?:,\\s*([0-9]|1[0-8]))?)?\\)");

    // Original ItemResource:81-100; native ThaumcraftContent.MATERIALS flattening.
    private static final List<String> RESOURCES = List.of("alumentum", "nitor", "thaumium_ingot", "quick_silver",
            "magic_tallow", "zombie_brain", "amber_gem", "enchanted_fabric", "vis_filter", "knowledge_fragment",
            "mirrored_glass", "tainted_goo", "taint_tendril", "jar_label", "salis_mundus", "primal_charm",
            "void_ingot", "void_seed", "gold_coin");
    // Original ConfigItems:267-272 / ItemShard:45-60; native SHARDS includes balance, not "balanced_shard".
    private static final List<String> SHARDS = List.of("air_shard", "fire_shard", "water_shard", "earth_shard",
            "order_shard", "entropy_shard", "balance_shard");
    // Native ThaumcraftOres keeps original blockCustomOre metadata 0..7 in this order.
    private static final List<String> ORES = List.of("cinnabar_ore", "air_infused_stone", "fire_infused_stone",
            "water_infused_stone", "earth_infused_stone", "order_infused_stone", "entropy_infused_stone", "amber_ore");

    // ConfigItems itemGolemPlacer damage 0..7 (the wildcard of ConfigResearch:344-345 cycles through every material).
    private static final List<String> PLACERS = List.of("golem_straw", "golem_wood", "golem_tallow", "golem_clay",
            "golem_flesh", "golem_stone", "golem_iron", "golem_thaumium");

    private ResearchIconDisplay() {}

    /** False leaves the browser's existing explicit-texture/book fallback intact. */
    static boolean render(GuiGraphics graphics, Font font, String expression, int x, int y) {
        ItemStack stack = resolve(expression);
        if (stack.isEmpty()) return false;
        // Flush graph lines before the item, and decorations before leaving the
        // caller's graph scissor. renderItem/flush enable depth; restore flat GUI
        // drawing for the following frame, lock overlay and tooltip.
        graphics.flush();
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        graphics.flush();
        RenderSystem.disableDepthTest();
        return true;
    }

    private static ItemStack resolve(String expression) {
        if (expression == null) return ItemStack.EMPTY;
        // ConfigResearch:55-57,77: this literal variable is WOOD / IRON, not
        // a research-key/output guess or a fabricated collection of wand tiers.
        if (expression.equals("wand")) {
            ItemStack stack = registered("wand", 1);
            if (!stack.isEmpty()) stack.set(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY);
            return stack;
        }
        // ConfigResearch:344-345: the ADVANCEDGOLEM icon is the placer wildcard tagged advanced = 1.
        if (expression.equals("ag")) {
            ItemStack stack = registered(PLACERS.get((int) (System.currentTimeMillis() / 1000L % PLACERS.size())), 1);
            if (!stack.isEmpty()) stack.set(ThaumcraftGolems.GOLEM_DATA.get(), GolemData.EMPTY.withAdvanced(true));
            return stack;
        }
        // ConfigResearch:374 and InventoryUtils.cycleItemStack:481-488. Only
        // this literal wildcard is supported; never enumerate future item placeholders.
        if (expression.equals("new ItemStack(ConfigBlocks.blockCustomOre, 1, Short.MAX_VALUE)")) {
            return registered(ORES.get((int) (System.currentTimeMillis() / 1000L % ORES.size())), 1);
        }
        var match = STACK.matcher(expression);
        if (!match.matches()) return ItemStack.EMPTY;
        int count = match.group(2) == null ? 1 : Integer.parseInt(match.group(2));
        if (count > 64) return ItemStack.EMPTY;
        int metadata = match.group(3) == null ? 0 : Integer.parseInt(match.group(3));
        String id = switch (match.group(1)) {
            case "ConfigItems.itemResource" -> variant(RESOURCES, metadata);
            case "ConfigItems.itemShard" -> variant(SHARDS, metadata);
            // ItemNugget metadata 16 / 21 / 31 are the native iron / cinnabar / gold clusters (ConfigAspects:360-362); 0 is the other-mod iron nugget.
            case "ConfigItems.itemNugget" -> switch (metadata) {
                case 6 -> "thaumium_nugget";
                case 7 -> "void_nugget";
                case 16 -> "cluster_iron";
                case 21 -> "cluster_cinnabar";
                case 31 -> "cluster_gold";
                default -> null;
            };
            case "ConfigItems.itemZombieBrain" -> metadata == 0 ? "zombie_brain" : null;
            // ConfigResearch:89-97; ConfigItems:292-313 (staff cores are metadata 50-57 and 100). The copper and silver caps (other-mod metals) remain unsupported.
            case "ConfigItems.itemWandRod" -> switch (metadata) {
                case 0 -> "wand_rod_greatwood";
                case 1 -> "wand_rod_obsidian";
                case 2 -> "wand_rod_silverwood";
                case 3 -> "wand_rod_ice";
                case 4 -> "wand_rod_quartz";
                case 5 -> "wand_rod_reed";
                case 6 -> "wand_rod_blaze";
                case 7 -> "wand_rod_bone";
                case 50 -> "staff_rod_greatwood";
                case 51 -> "staff_rod_obsidian";
                case 52 -> "staff_rod_silverwood";
                case 53 -> "staff_rod_ice";
                case 54 -> "staff_rod_quartz";
                case 55 -> "staff_rod_reed";
                case 56 -> "staff_rod_blaze";
                case 57 -> "staff_rod_bone";
                case 100 -> "staff_rod_primal";
                default -> null;
            };
            case "ConfigItems.itemWandCap" -> switch (metadata) {
                case 0 -> "wand_cap_iron";
                case 1 -> "wand_cap_gold";
                case 2 -> "wand_cap_thaumium";
                case 6 -> "wand_cap_thaumium_inert";
                case 7 -> "wand_cap_void";
                case 8 -> "wand_cap_void_inert";
                default -> null;
            };
            // ConfigResearch:172,249,367,369. No filled-phial aspect or potion is guessed.
            case "ConfigItems.itemInkwell" -> metadata == 0 ? "inkwell" : null;
            case "ConfigItems.itemThaumonomicon" -> metadata == 0 ? "thaumonomicon" : null;
            case "ConfigItems.itemThaumometer" -> metadata == 0 ? "thaumometer" : null;
            case "ConfigItems.itemGoggles" -> metadata == 0 ? "goggles_revealing" : null;
            case "ConfigItems.itemEssence" -> metadata == 0 ? "phial_empty" : null;
            case "ConfigItems.itemFocusFire" -> metadata == 0 ? "focus_fire" : null;
            case "ConfigItems.itemFocusShock" -> metadata == 0 ? "focus_shock" : null;
            case "ConfigItems.itemFocusExcavation" -> metadata == 0 ? "focus_excavation" : null;
            case "ConfigItems.itemFocusFrost" -> metadata == 0 ? "focus_frost" : null;
            case "ConfigItems.itemFocusPortableHole" -> metadata == 0 ? "focus_portable_hole" : null;
            case "ConfigItems.itemChestRobe" -> metadata == 0 ? "robe_chestplate" : null;
            case "ConfigItems.itemLegsRobe" -> metadata == 0 ? "robe_leggings" : null;
            case "ConfigItems.itemBootsRobe" -> metadata == 0 ? "robe_boots" : null;
            // Original ConfigItems:387-404. Only these nine real equipment IDs.
            case "ConfigItems.itemSwordThaumium" -> metadata == 0 ? "thaumium_sword" : null;
            case "ConfigItems.itemPickThaumium" -> metadata == 0 ? "thaumium_pickaxe" : null;
            case "ConfigItems.itemAxeThaumium" -> metadata == 0 ? "thaumium_axe" : null;
            case "ConfigItems.itemAxeElemental" -> metadata == 0 ? "elemental_axe" : null;
            case "ConfigItems.itemPickElemental" -> metadata == 0 ? "elemental_pickaxe" : null;
            case "ConfigItems.itemHoeElemental" -> metadata == 0 ? "elemental_hoe" : null;
            case "ConfigItems.itemShovelElemental" -> metadata == 0 ? "elemental_shovel" : null;
            case "ConfigItems.itemSwordElemental" -> metadata == 0 ? "elemental_sword" : null;
            case "ConfigItems.itemHelmetVoidRobe" -> metadata == 0 ? "void_robe_helmet" : null;
            case "ConfigItems.itemHelmetFortress" -> metadata == 0 ? "fortress_helmet" : null;
            case "ConfigItems.itemShovelThaumium" -> metadata == 0 ? "thaumium_shovel" : null;
            case "ConfigItems.itemHoeThaumium" -> metadata == 0 ? "thaumium_hoe" : null;
            case "ConfigItems.itemHelmetThaumium" -> metadata == 0 ? "thaumium_helmet" : null;
            case "ConfigItems.itemChestThaumium" -> metadata == 0 ? "thaumium_chestplate" : null;
            case "ConfigItems.itemLegsThaumium" -> metadata == 0 ? "thaumium_leggings" : null;
            case "ConfigItems.itemBootsThaumium" -> metadata == 0 ? "thaumium_boots" : null;
            case "ConfigItems.itemGolemPlacer" -> switch (metadata) {
                case 0 -> "golem_straw";
                case 1 -> "golem_wood";
                case 2 -> "golem_tallow";
                case 3 -> "golem_clay";
                case 4 -> "golem_flesh";
                case 5 -> "golem_stone";
                case 6 -> "golem_iron";
                case 7 -> "golem_thaumium";
                default -> null;
            };
            case "ConfigItems.itemGolemCore" -> switch (metadata) {
                case 0 -> "golem_core_fill";
                case 1 -> "golem_core_empty";
                case 2 -> "golem_core_gather";
                case 3 -> "golem_core_harvest";
                case 4 -> "golem_core_guard";
                case 5 -> "golem_core_liquid";
                case 6 -> "golem_core_essentia";
                case 7 -> "golem_core_lumber";
                case 8 -> "golem_core_use";
                case 9 -> "golem_core_butcher";
                case 10 -> "golem_core_sorting";
                case 11 -> "golem_core_fishing";
                default -> null;
            };
            // Original ItemGolemUpgrade damage 0 Air / 1 Earth / 2 Fire / 3 Water / 4 Order / 5 Entropy.
            case "ConfigItems.itemGolemUpgrade" -> switch (metadata) {
                case 0 -> "golem_upgrade_air";
                case 1 -> "golem_upgrade_earth";
                case 2 -> "golem_upgrade_fire";
                case 3 -> "golem_upgrade_water";
                case 4 -> "golem_upgrade_order";
                case 5 -> "golem_upgrade_entropy";
                default -> null;
            };
            // Original ItemGolemDecoration damage 0..7 (hat, spectacles, bowtie, fez, dart, visor, plating, mace).
            case "ConfigItems.itemGolemDecoration" -> switch (metadata) {
                case 0 -> "golem_decoration_tophat";
                case 1 -> "golem_decoration_glasses";
                case 2 -> "golem_decoration_bowtie";
                case 3 -> "golem_decoration_fez";
                case 4 -> "golem_decoration_dart";
                case 5 -> "golem_decoration_visor";
                case 6 -> "golem_decoration_armor";
                case 7 -> "golem_decoration_mace";
                default -> null;
            };
            case "ConfigItems.itemGolemBell" -> metadata == 0 ? "golem_bell" : null;
            case "ConfigItems.itemTrunkSpawner" -> metadata == 0 ? "trunk_spawner" : null;
            case "ConfigBlocks.blockTaint" -> metadata == 2 ? "flesh_block" : null;
            // ConfigResearch:169-171 uses metadata 1 for RESTABLE; original
            // BlockTableRenderer:42-54 renders both 1 and 2 as a research table.
            case "ConfigBlocks.blockTable" -> switch (metadata) {
                case 0 -> "table";
                case 1, 2 -> "research_table";
                case 15 -> "arcane_workbench";
                default -> null;
            };
            // ConfigResearch:167,254,300,305 and native masonry/alchemy registrations.
            case "ConfigBlocks.blockCosmeticSolid" -> switch (metadata) {
                case 4 -> "thaumium_block";
                case 5 -> "tallow_block";
                case 6 -> "arcane_stone";
                case 7 -> "arcane_stone_bricks";
                case 9 -> "golem_fetter";
                default -> null;
            };
            case "ConfigBlocks.blockMetalDevice" -> switch (metadata) {
                case 0 -> "crucible";
                case 1 -> "alembic";
                case 9 -> "alchemical_construct";
                case 14 -> "vis_relay";
                default -> null;
            };
            case "ConfigBlocks.blockStoneDevice" -> switch (metadata) {
                case 0 -> "alchemy_furnace";
                case 1 -> "arcane_pedestal";
                case 2 -> "infusion_matrix";
                case 9 -> "node_stabilizer";
                case 11 -> "node_transducer";
                case 13 -> "focal_manipulator";
                default -> null;
            };
            // ConfigResearch:183 / ConfigBlocks wooden-device variants.
            case "ConfigBlocks.blockWoodenDevice" -> switch (metadata) {
                case 0 -> "bellows";
                case 4 -> "arcane_bore_base";
                case 5 -> "arcane_bore";
                case 6 -> "greatwood_planks";
                case 7 -> "silverwood_planks";
                default -> null;
            };
            case "ConfigBlocks.blockJar" -> switch (metadata) {
                case 0 -> "jar";
                case 3 -> "jar_void";
                default -> null;
            };
            case "ConfigBlocks.blockTube" -> switch (metadata) {
                case 0 -> "essentia_tube";
                case 1 -> "essentia_valve";
                case 2 -> "centrifuge";
                case 3 -> "essentia_filter";
                case 4 -> "essentia_buffer";
                case 5 -> "essentia_restrict";
                case 6 -> "essentia_oneway";
                case 7 -> "essentia_crystallizer";
                default -> null;
            };
            // ConfigBlocks:264-265; original magical wood/leaves 0/1 and
            // BlockCustomPlantItem:48-49. No knot log or other plants are substituted.
            case "ConfigBlocks.blockMagicalLog" -> switch (metadata) {
                case 0 -> "greatwood_log";
                case 1 -> "silverwood_log";
                default -> null;
            };
            case "ConfigBlocks.blockMagicalLeaves" -> switch (metadata) {
                case 0 -> "greatwood_leaves";
                case 1 -> "silverwood_leaves";
                default -> null;
            };
            case "ConfigBlocks.blockCustomPlant" -> switch (metadata) {
                case 0 -> "greatwood_sapling";
                case 1 -> "silverwood_sapling";
                case 2 -> "shimmerleaf";
                case 3 -> "cinderpearl";
                default -> null;
            };
            case "ConfigBlocks.blockCustomOre" -> variant(ORES, metadata);
            case "ConfigBlocks.blockChestHungry" -> metadata == 0 ? "hungry_chest" : null;
            default -> null;
        };
        return id == null ? ItemStack.EMPTY : registered(id, count);
    }

    private static String variant(List<String> variants, int metadata) {
        return metadata >= 0 && metadata < variants.size() ? variants.get(metadata) : null;
    }

    private static ItemStack registered(String path, int count) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
        // Resolve only after bootstrap, through the actual item registry. Static
        // aliases never call deferred item suppliers or turn a missing ID into AIR.
        return BuiltInRegistries.ITEM.containsKey(id) ? new ItemStack(BuiltInRegistries.ITEM.get(id), count) : ItemStack.EMPTY;
    }
}
