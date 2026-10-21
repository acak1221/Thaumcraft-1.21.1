package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * The ordinary recipes of the original TC4 4.2.3.5, pinned against the oracle.
 *
 * <p>The oracle registers 114 ordinary recipes: 103 in {@code initializeNormalRecipes} (oracle
 * {@code thaumcraft/common/config/ConfigRecipes.java:399-481}), the three arcane stone recipes of the
 * arcane recipe section (oracle {@code :206-208}), the void robe dye of oracle {@code :385} and the eight
 * furnace recipes of {@code initializeSmelting} (oracle {@code :485-492}). Every row of the table below is
 * one of those registrations with the native recipe that carries it.</p>
 *
 * <p>Four rows need a note of their own:
 * <ul>
 *   <li>the two iron nugget recipes of oracle {@code :399} and {@code :402} are the vanilla recipes of
 *       1.21.1 ({@code minecraft:iron_nugget} and {@code minecraft:iron_ingot_from_nuggets}), because 1.7.10
 *       had no iron nugget of its own;</li>
 *   <li>the sixteen aspect bound jar labels of oracle {@code :423} became the one native recipe
 *       {@code thaumcraft:jar_label_bind}, because 1.21.1 carries the aspect of a label in a data component
 *       instead of in a metadata value;</li>
 *   <li>the magical log of oracle {@code :487} became two native recipes, one for the greatwood and one for
 *       the silverwood log, because 1.21.1 has one log item per wood;</li>
 *   <li>the port ships two recipes of its own, {@code thaumcraft:copper_nuggets} and
 *       {@code thaumcraft:copper_ingot_from_nuggets}: copper is vanilla in 1.21.1, while the original copper
 *       nugget was a third-party ore name, so the original had no copper recipe at all.</li>
 * </ul>
 */
class NormalRecipeParityTest {
    private static final String RECIPE = "data/thaumcraft/recipe/";

    /** One original ordinary registration and the native recipe that carries it. */
    private record Row(int oracleLine, int member, String nativeRecipe, String original) {}

    private static final List<Row> ORACLE = List.of(
            new Row(206, 0, "arcane_stone_bricks", "four arcane stones make four arcane stone bricks"),
            new Row(207, 0, "arcane_stone_brick_stairs", "four arcane stone bricks make four stairs"),
            new Row(208, 0, "arcane_stone_brick_slab", "six arcane stone bricks make six slabs"),
            new Row(385, 0, "robe_dye", "the void robe dye"),
            new Row(399, 0, "minecraft:iron_nugget", "one iron ingot makes nine iron nuggets"),
            new Row(400, 0, "thaumium_nuggets", "one thaumium ingot makes nine thaumium nuggets"),
            new Row(401, 0, "void_nuggets", "one void ingot makes nine void nuggets"),
            new Row(402, 0, "minecraft:iron_ingot_from_nuggets", "nine iron nuggets make one iron ingot"),
            new Row(403, 0, "thaumium_ingot_from_nuggets", "nine thaumium nuggets make one thaumium ingot"),
            new Row(404, 0, "quick_silver_from_drops", "nine quicksilver drops make one quicksilver"),
            new Row(405, 0, "quicksilver_drops", "one quicksilver makes nine quicksilver drops"),
            new Row(406, 0, "void_ingot_from_nuggets", "nine void nuggets make one void ingot"),
            new Row(407, 0, "mundane_amulet", "the mundane amulet"),
            new Row(408, 0, "mundane_ring", "the mundane ring"),
            new Row(409, 0, "mundane_belt", "the mundane belt"),
            new Row(410, 0, "triple_meat_treat_beef_pork_fish", "the first triple meat treat"),
            new Row(411, 0, "triple_meat_treat_chicken_beef_fish", "the second triple meat treat"),
            new Row(412, 0, "triple_meat_treat_chicken_beef_pork", "the third triple meat treat"),
            new Row(413, 0, "triple_meat_treat_chicken_pork_fish", "the fourth triple meat treat"),
            new Row(414, 0, "quick_silver_from_shimmerleaf", "one shimmerleaf makes one quicksilver"),
            new Row(415, 0, "blaze_powder_from_cinderpearl", "one cinderpearl makes one blaze powder"),
            new Row(416, 0, "jar_label", "four jar labels"),
            new Row(423, 0, "jar_label_bind", "the sixteen aspect bound jar labels"),
            new Row(428, 0, "jar_label_clear", "the jar label of the water aspect"),
            new Row(429, 0, "wand_wood_iron", "the basic wand"),
            new Row(430, 0, "wand_cap_iron", "the iron wand cap"),
            new Row(431, 0, "unknown_research_note", "the knowledge fragment"),
            new Row(432, 0, "greatwood_planks", "four greatwood planks"),
            new Row(433, 0, "silverwood_planks", "four silverwood planks"),
            new Row(434, 0, "greatwood_stairs", "four greatwood stairs"),
            new Row(435, 0, "silverwood_stairs", "four silverwood stairs"),
            new Row(436, 0, "greatwood_slab", "six greatwood slabs"),
            new Row(437, 0, "silverwood_slab", "six silverwood slabs"),
            new Row(438, 0, "flesh_block", "the flesh block"),
            new Row(439, 0, "thaumium_block", "the thaumium block"),
            new Row(440, 0, "thaumium_block_unpack", "the thaumium block makes nine thaumium ingots"),
            new Row(441, 0, "tallow_block", "the tallow block"),
            new Row(442, 0, "tallow_block_unpack", "the tallow block makes nine tallow"),
            new Row(444, 1, "clusters/air", "the air cluster"),
            new Row(444, 2, "clusters/earth", "the earth cluster"),
            new Row(444, 3, "clusters/entropy", "the entropy cluster"),
            new Row(444, 4, "clusters/fire", "the fire cluster"),
            new Row(444, 5, "clusters/order", "the order cluster"),
            new Row(444, 6, "clusters/water", "the water cluster"),
            new Row(446, 0, "clusters/mixed", "the mixed cluster"),
            new Row(447, 0, "amber_block", "four amber make one amber block"),
            new Row(448, 0, "amber_bricks", "four amber blocks make four amber bricks"),
            new Row(449, 0, "obsidian_tile", "four obsidian make four obsidian tiles"),
            new Row(450, 0, "amber_gems_from_block", "one amber block makes four amber"),
            new Row(451, 0, "amber_gems_from_bricks", "one amber brick makes four amber"),
            new Row(452, 0, "item_grate", "the grate"),
            new Row(453, 0, "phial_empty", "eight empty phials"),
            new Row(454, 0, "table", "the arcane table"),
            new Row(455, 0, "scribing_tools_phial", "the scribing tools of the phial"),
            new Row(456, 0, "scribing_tools_bottle", "the scribing tools of the bottle"),
            new Row(457, 0, "scribing_tools_refill", "the scribing tools of the refill"),
            new Row(458, 0, "thaumometer", "the thaumometer"),
            new Row(459, 0, "thaumium_helmet", "the thaumium helmet"),
            new Row(460, 0, "thaumium_chestplate", "the thaumium chestplate"),
            new Row(461, 0, "thaumium_leggings", "the thaumium leggings"),
            new Row(462, 0, "thaumium_boots", "the thaumium boots"),
            new Row(463, 0, "thaumium_shovel", "the thaumium shovel"),
            new Row(464, 0, "thaumium_pickaxe", "the thaumium pickaxe"),
            new Row(465, 0, "thaumium_axe", "the thaumium axe"),
            new Row(466, 0, "thaumium_hoe", "the thaumium hoe"),
            new Row(467, 0, "thaumium_sword", "the thaumium sword"),
            new Row(468, 0, "void_helmet", "the void helmet"),
            new Row(469, 0, "void_chestplate", "the void chestplate"),
            new Row(470, 0, "void_leggings", "the void leggings"),
            new Row(471, 0, "void_boots", "the void boots"),
            new Row(472, 0, "void_shovel", "the void shovel"),
            new Row(473, 0, "void_pickaxe", "the void pickaxe"),
            new Row(474, 0, "void_axe", "the void axe"),
            new Row(475, 0, "void_hoe", "the void hoe"),
            new Row(476, 0, "void_sword", "the void sword"),
            new Row(477, 0, "tallow_candle", "three tallow candles"),
            new Row(479, 1, "orange_tallow_candle", "the orange candle"),
            new Row(479, 2, "magenta_tallow_candle", "the magenta candle"),
            new Row(479, 3, "light_blue_tallow_candle", "the light blue candle"),
            new Row(479, 4, "yellow_tallow_candle", "the yellow candle"),
            new Row(479, 5, "lime_tallow_candle", "the lime candle"),
            new Row(479, 6, "pink_tallow_candle", "the pink candle"),
            new Row(479, 7, "gray_tallow_candle", "the gray candle"),
            new Row(479, 8, "light_gray_tallow_candle", "the light gray candle"),
            new Row(479, 9, "cyan_tallow_candle", "the cyan candle"),
            new Row(479, 10, "purple_tallow_candle", "the purple candle"),
            new Row(479, 11, "blue_tallow_candle", "the blue candle"),
            new Row(479, 12, "brown_tallow_candle", "the brown candle"),
            new Row(479, 13, "green_tallow_candle", "the green candle"),
            new Row(479, 14, "red_tallow_candle", "the red candle"),
            new Row(479, 15, "black_tallow_candle", "the black candle"),
            new Row(481, 0, "tallow_candle_bleach", "bone meal bleaches the candle"),
            new Row(485, 0, "quick_silver_from_smelting_cinnabar_ore", "the cinnabar ore smelts into quicksilver"),
            new Row(486, 0, "amber_from_smelting_amber_ore", "the amber ore smelts into amber"),
            new Row(487, 1, "charcoal_from_smelting_greatwood_log", "the greatwood log smelts into charcoal"),
            new Row(487, 2, "charcoal_from_smelting_silverwood_log", "the silverwood log smelts into charcoal"),
            new Row(488, 0, "cluster_iron_smelting", "the iron cluster melts into two iron ingots"),
            new Row(489, 0, "cluster_cinnabar_smelting", "the cinnabar cluster melts into two quicksilver"),
            new Row(490, 0, "cluster_gold_smelting", "the gold cluster melts into two gold ingots"),
            new Row(491, 0, "salis_mundus_from_smelting_balance_shard", "the balance shard smelts into salis mundus")
    );

    /** The native recipes the port ships without an original registration of their own. */
    private static final List<String> PORT_ONLY = List.of("copper_nuggets", "copper_ingot_from_nuggets");

    /** The original registrations the native recipe of 1.21.1 already covers. */
    private static final List<String> VANILLA = List.of("minecraft:iron_nugget", "minecraft:iron_ingot_from_nuggets");

    @Test void everyOriginalOrdinaryRecipeHasItsNativeRecipe() throws Exception {
        List<String> missing = new ArrayList<>();
        for (Row row : ORACLE) {
            if (VANILLA.contains(row.nativeRecipe())) continue;
            if (!ResourceTree.exists(RECIPE + row.nativeRecipe() + ".json")) {
                missing.add("oracle line " + row.oracleLine() + " -> " + row.nativeRecipe());
            }
        }
        assertEquals(List.of(), missing, "every original ordinary recipe has a native recipe");
    }

    @Test void thePortShipsNoOrdinaryRecipeWithoutAnOriginalRegistration() throws Exception {
        Set<String> shipped = new LinkedHashSet<>();
        for (String stem : ResourceTree.stems(RECIPE, ".json")) {
            if (stem.contains("/")) continue; // the sub trees are the arcane, infusion and crucible recipes
            shipped.add(stem);
        }
        for (String stem : ResourceTree.stems(RECIPE + "clusters", ".json")) shipped.add("clusters/" + stem);
        Set<String> mapped = new LinkedHashSet<>();
        for (Row row : ORACLE) if (!VANILLA.contains(row.nativeRecipe())) mapped.add(row.nativeRecipe());
        mapped.addAll(PORT_ONLY);
        List<String> extra = new ArrayList<>(new TreeSet<>(shipped));
        extra.removeAll(mapped);
        List<String> lost = new ArrayList<>(new TreeSet<>(mapped));
        lost.removeAll(shipped);
        assertEquals(List.of(), extra, "native ordinary recipes the original does not register");
        assertEquals(List.of(), lost, "native ordinary recipes that are missing");
    }

    @Test void theNumbersAreTheOriginalOnes() throws Exception {
        // 103 registrations in initializeNormalRecipes, three arcane stone recipes, one robe dye and eight
        // furnace recipes. The table holds one row for the sixteen aspect bound jar labels and two rows for
        // the one magical log of the original, so it holds 114 - 15 + 1 = 100 rows.
        assertEquals(114, ORACLE.size() + 15 - 1, "the original ordinary registrations the table pins");
        // Every row but the two vanilla iron nugget recipes is a native recipe of the port, and the port
        // ships two recipes of its own for the copper that is vanilla in 1.21.1.
        assertEquals(100, ORACLE.size() - VANILLA.size() + PORT_ONLY.size(), "the native ordinary recipes of the port");
        assertEquals(2, PORT_ONLY.size(), "the two copper recipes of 1.21.1");
        assertEquals(2, VANILLA.size(), "the two vanilla iron nugget recipes");
        // The obsidian tile of oracle line 449 is the recipe this wave added.
        JsonObject tile = ResourceTree.object(RECIPE + "obsidian_tile.json");
        assertEquals("minecraft:crafting_shaped", tile.get("type").getAsString());
        assertEquals(List.of("##", "##"), List.of(tile.getAsJsonArray("pattern").get(0).getAsString(),
                tile.getAsJsonArray("pattern").get(1).getAsString()));
        assertEquals("minecraft:obsidian", tile.getAsJsonObject("key").get("#").getAsJsonObject().get("item").getAsString());
        assertEquals("thaumcraft:obsidian_tile", tile.getAsJsonObject("result").get("id").getAsString());
        assertEquals(4, tile.getAsJsonObject("result").get("count").getAsInt());
    }
}
