package thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The nine wand constructs of the original research book, drawn as the COMPOUND_CRAFTING pages they were.
 *
 * <p>Original 4.2.3.5 {@code ConfigRecipes.initializeCompoundRecipes} stores every construct as the list
 * {@code [AspectList, dx, dy, dz, blocks]} under its recipe key, and the research pages cast it as
 * {@code (List) recipes.get(key)}. The tables below are those lists with the blocks of 1.21.1; the order of the cells is
 * the order of the original list ({@link CompoundLayout#cellsInOrder()}). A cell is:</p>
 * <ul>
 *   <li>{@code null}: an entry the original left null, nothing is drawn and nothing is shown on hover;</li>
 *   <li>{@link #EMPTY}: the original {@code new ItemStack(ConfigBlocks.blockHole, 1, 15)}, the invisible "Empty
 *       Space" block (its icon is {@code thaumcraft:empty}); nothing is drawn, the hover names it;</li>
 *   <li>{@link #WAND}: the original {@code basicWand}, a wand of a wooden rod and iron caps;</li>
 *   <li>{@code "ore:<name>"}: an ore dictionary name of the original, shown one member per second;</li>
 *   <li>an item id.</li>
 * </ul>
 * <p>Two blocks of the original have no item in 1.21.1. The lava source in the middle of the Infernal Furnace
 * ({@code new ItemStack(Blocks.lava)}, which 1.7.10 could hold and draw as a block) is the lava bucket, and the energized
 * aura node in the middle of the jar of the Node in a Jar ({@code blockAiry} metadata 5, which the original drew with the
 * aura node item renderer, {@code ItemNodeRenderer.handleRenderType} answers metadata 0 and 5) is the aura node item,
 * the node the construct is actually built around.</p>
 */
public final class CompoundConstructs {
    /** The original invisible "Empty Space" block of a cell, see the class comment. */
    public static final String EMPTY = "empty";
    /** The original basic wand of a cell, see the class comment. */
    public static final String WAND = "wand";
    /** The prefix of an ore dictionary name of the original. */
    public static final String ORE = "ore:";

    /**
     * One construct of the original.
     *
     * @param aspects the vis of the construct, aspect tag to amount, in the order of the original {@code AspectList}
     * @param cells   the blocks, in the order of the original list
     */
    public record Definition(Map<String, Integer> aspects, int dx, int dy, int dz, List<String> cells) {
        public Definition {
            aspects = Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
            cells = Collections.unmodifiableList(new ArrayList<>(cells));
        }

        public CompoundLayout layout() {
            return new CompoundLayout(dx, dy, dz);
        }
    }

    private static final String NETHER_BRICKS = "minecraft:nether_bricks", OBSIDIAN = "minecraft:obsidian",
            GLASS = "minecraft:glass", ALEMBIC = "thaumcraft:alembic", CONSTRUCT = "thaumcraft:alchemical_construct",
            ADVANCED = "thaumcraft:advanced_alchemical_construct", STONE = "thaumcraft:arcane_stone",
            BRICKS = "thaumcraft:arcane_stone_bricks", TABLE = "thaumcraft:table";

    /** The constructs, by the recipe key of the original, in the order of {@code initializeCompoundRecipes}. */
    private static final Map<String, Definition> DEFINITIONS = definitions();

    private CompoundConstructs() {}

    /** The constructs of the original, by recipe key. */
    public static Map<String, Definition> all() {
        return DEFINITIONS;
    }

    /**
     * The page of a construct, its blocks looked up in the item registry; null for another key, or when a block of the
     * construct is not registered (the page is then not shown, like every other page whose recipe is missing).
     */
    public static CompoundPage page(String key) {
        Definition definition = key == null ? null : DEFINITIONS.get(key);
        if (definition == null) return null;
        AspectList aspects = new AspectList();
        for (var cost : definition.aspects().entrySet()) {
            Aspect aspect = Aspect.getAspect(cost.getKey());
            if (aspect == null) return null;
            aspects.add(aspect, cost.getValue());
        }
        List<Object> entries = new ArrayList<>(definition.cells().size());
        for (String cell : definition.cells()) {
            if (cell == null) entries.add(null);
            else if (cell.equals(EMPTY)) entries.add(ItemStack.EMPTY);
            else if (cell.startsWith(ORE)) entries.add(cell.substring(ORE.length()));
            else {
                ItemStack stack = registered(cell.equals(WAND) ? "thaumcraft:wand" : cell);
                if (stack.isEmpty()) return null;
                entries.add(stack);
            }
        }
        return new CompoundPage(aspects, definition.layout(), Collections.unmodifiableList(entries), key);
    }

    private static ItemStack registered(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        // The wand item carries a wooden rod and iron caps unless told otherwise: the original basicWand.
        return location != null && BuiltInRegistries.ITEM.containsKey(location)
                ? new ItemStack(BuiltInRegistries.ITEM.get(location)) : ItemStack.EMPTY;
    }

    private static Map<String, Definition> definitions() {
        Map<String, Definition> constructs = new LinkedHashMap<>();
        // A bookshelf struck with a wand.
        constructs.put("Thaumonomicon", new Definition(Map.of(), 1, 2, 1, List.of(WAND, "minecraft:bookshelf")));
        // A table struck with a wand.
        constructs.put("ArcTable", new Definition(Map.of(), 1, 2, 1, List.of(WAND, TABLE)));
        // Two tables side by side, the scribing tools on one of them.
        constructs.put("ResTable", new Definition(Map.of(), 1, 2, 2, Arrays.asList(null, "thaumcraft:inkwell", TABLE, TABLE)));
        // A cauldron struck with a wand.
        constructs.put("Crucible", new Definition(Map.of(), 1, 2, 1, List.of(WAND, "minecraft:cauldron")));
        constructs.put("InfernalFurnace", new Definition(aspects("ignis", 50, "terra", 50), 3, 3, 3, List.of(
                NETHER_BRICKS, OBSIDIAN, NETHER_BRICKS, OBSIDIAN, EMPTY, OBSIDIAN, NETHER_BRICKS, OBSIDIAN, NETHER_BRICKS,
                NETHER_BRICKS, OBSIDIAN, NETHER_BRICKS, OBSIDIAN, "minecraft:lava_bucket", "minecraft:iron_bars", NETHER_BRICKS, OBSIDIAN, NETHER_BRICKS,
                NETHER_BRICKS, OBSIDIAN, NETHER_BRICKS, OBSIDIAN, OBSIDIAN, OBSIDIAN, NETHER_BRICKS, OBSIDIAN, NETHER_BRICKS)));
        constructs.put("InfusionAltar", new Definition(aspects("ignis", 25, "terra", 25, "ordo", 25, "aer", 25, "perditio", 25, "aqua", 25),
                3, 3, 3, Arrays.asList(
                EMPTY, null, EMPTY, null, "thaumcraft:infusion_matrix", null, EMPTY, null, EMPTY,
                STONE, null, STONE, null, null, null, STONE, null, STONE,
                BRICKS, null, BRICKS, null, "thaumcraft:arcane_pedestal", null, BRICKS, null, BRICKS)));
        List<String> nodeJar = new ArrayList<>();
        for (int index = 0; index < 9; index++) nodeJar.add(ORE + "slabWood");
        for (int index = 9; index < 36; index++) nodeJar.add(index == 22 ? "thaumcraft:aura_node" : GLASS);
        constructs.put("NodeJar", new Definition(aspects("ignis", 70, "terra", 70, "aer", 70, "aqua", 70, "ordo", 70, "perditio", 70),
                3, 4, 3, nodeJar));
        constructs.put("Thaumatorium", new Definition(aspects("ignis", 15, "ordo", 30, "aqua", 30), 1, 3, 1,
                List.of(CONSTRUCT, CONSTRUCT, "thaumcraft:crucible")));
        constructs.put("AdvAlchemyFurnace", new Definition(aspects("ignis", 50, "aqua", 50, "ordo", 50), 3, 2, 3, List.of(
                ALEMBIC, CONSTRUCT, ALEMBIC, CONSTRUCT, EMPTY, CONSTRUCT, ALEMBIC, CONSTRUCT, ALEMBIC,
                ADVANCED, ADVANCED, ADVANCED, ADVANCED, "thaumcraft:alchemy_furnace", ADVANCED, ADVANCED, ADVANCED, ADVANCED)));
        return Collections.unmodifiableMap(constructs);
    }

    private static Map<String, Integer> aspects(Object... tagsAndAmounts) {
        Map<String, Integer> aspects = new LinkedHashMap<>();
        for (int index = 0; index < tagsAndAmounts.length; index += 2) {
            aspects.put((String) tagsAndAmounts[index], (Integer) tagsAndAmounts[index + 1]);
        }
        return aspects;
    }
}
