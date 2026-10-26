package thaumcraft.common.lib.crafting;

import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandPartItems;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.enchantment.WandEnchantmentRules;

/**
 * Original ArcaneSceptreRecipe (oracle thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java).
 *
 * The original grid is the TC4 magic-workbench layout of the research page " TF", " RT", "T  "
 * (ConfigResearch.java:122): one cap in row 0 column 1, one cap in row 1 column 2, one cap in row 2
 * column 0, the rod in the centre and the primal charm in row 0 column 2. Every other slot must be
 * empty. The result is a wand of those materials marked as a sceptre and it costs fifty percent
 * more vis than the plain wand of the same materials.
 *
 * The native arcane workbench consumes this recipe through the same arcane recipe type as the plain
 * wand recipe, so the two never compete: their grids are mutually exclusive.
 */
public final class ArcaneSceptreRecipe implements NativeArcaneRecipe {
    private static final ArcaneSceptreRecipe INSTANCE = new ArcaneSceptreRecipe();
    public static final MapCodec<ArcaneSceptreRecipe> CODEC = MapCodec.unit(INSTANCE);
    /**
     * Original ArcaneSceptreRecipe:42-46 reads the grid through
     * {@code ThaumcraftApiHelper.getStackInRowAndColumn(inv, x, y)}, which indexes {@code x + y * 3}.
     * cap1 is (1,0) = slot 1, cap2 is (2,1) = slot 5, cap3 is (0,2) = slot 6, the rod is (1,1) = slot 4
     * and the focus is (2,0) = slot 2; the forbidden slots are (0,0), (0,1), (1,2) and (2,2).
     */
    private static final int CAP1 = 1, CAP2 = 5, CAP3 = 6, ROD = 4, FOCUS = 2;
    private static final int[] FORBIDDEN = {0, 3, 7, 8};
    /** Original ArcaneSceptreRecipe:115: the sceptre recipe needs its own research. */
    public static final String RESEARCH = "SCEPTRE";

    /**
     * The parts the output uses (the first registered part of each item, original :47-61, native first, then add-on parts of
     * {@code WandCap.caps} / {@code WandRod.rods}) and every part of those items, for the research check (original
     * checkMatch:155-169 accepts a cell when ANY part of that item is researched).
     */
    private record Materials(WandMaterials.Rod rod, WandMaterials.Cap cap, List<WandMaterials.Rod> rods, List<WandMaterials.Cap> caps) {
        int cost() {
            // Native parts are at most 32 x 9, so the clamp to the original short metadata only ever binds for add-on parts.
            return (int) Math.min(WandEnchantmentRules.sceptreCost(cap.craftCost(), rod.craftCost()), (long) WandMaterials.MAX_ASSEMBLY_COST);
        }
    }

    /** The original focus is the primal charm (ConfigItems.itemResource metadata 15). */
    private static ItemStack focusItem() {
        var charm = ThaumcraftContent.MATERIALS.get("primal_charm");
        return charm == null ? ItemStack.EMPTY : new ItemStack(charm.get());
    }

    /**
     * Original ArcaneSceptreRecipe:147-152: the sceptre compares the item and its metadata only.
     * It is NOT the stricter wand rule of ArcaneWandRecipe:133-137, which also compares the NBT of
     * a tagged target: a renamed cap or charm still crafts a sceptre here, exactly as in 4.2.3.5.
     */
    private static boolean checkItemEquals(ItemStack target, ItemStack input) {
        if (target.isEmpty() || input.isEmpty()) return target.isEmpty() && input.isEmpty();
        return target.getItem() == input.getItem();
    }

    private static Materials materials(ArcaneRecipeInput input) {
        for (int slot : FORBIDDEN) if (!input.getItem(slot).isEmpty()) return null;
        ItemStack cap1 = input.getItem(CAP1), cap2 = input.getItem(CAP2), cap3 = input.getItem(CAP3);
        ItemStack rodItem = input.getItem(ROD), focus = input.getItem(FOCUS);
        if (cap1.isEmpty() || cap2.isEmpty() || cap3.isEmpty() || rodItem.isEmpty() || focus.isEmpty()) return null;
        if (!checkItemEquals(cap1, cap2) || !checkItemEquals(cap1, cap3)) return null;
        if (!checkItemEquals(focus, focusItem())) return null;
        var caps = WandPartItems.caps(cap1, ArcaneSceptreRecipe::checkItemEquals);
        var rods = WandPartItems.rods(rodItem, ArcaneSceptreRecipe::checkItemEquals);
        return caps.isEmpty() || rods.isEmpty() ? null : new Materials(rods.getFirst(), caps.getFirst(), rods, caps);
    }

    @Override public boolean matches(ArcaneRecipeInput input, Level level) { return materials(input) != null; }

    @Override public boolean hasResearch(ArcaneRecipeInput input, Player actor) {
        Materials materials = materials(input);
        if (materials == null || actor == null) return false;
        var knowledge = KnowledgeAccess.get(actor);
        return knowledge.isResearchComplete(RESEARCH)
                && materials.caps().stream().anyMatch(cap -> knowledge.isResearchComplete(cap.research()))
                && materials.rods().stream().anyMatch(rod -> knowledge.isResearchComplete(rod.research()));
    }

    @Override public ItemStack assemble(ArcaneRecipeInput input, HolderLookup.Provider registries) {
        Materials materials = materials(input);
        if (materials == null) return ItemStack.EMPTY;
        ItemStack output = new ItemStack(ThaumcraftContent.WAND.get());
        ItemWandCasting.withData(output, new WandData(materials.rod().tag(), materials.cap().tag(), Map.of()));
        output.set(ThaumcraftWands.WAND_CRAFT_COST.get(), materials.cost());
        // Original ArcaneSceptreRecipe:68 writes the NBT byte tag "sceptre" on the crafted wand.
        output.set(ThaumcraftWandEnchantments.WAND_SCEPTRE.get(), Boolean.TRUE);
        return output;
    }

    @Override public Map<String, Integer> vis(ArcaneRecipeInput input) {
        Materials materials = materials(input);
        if (materials == null) return Map.of();
        Map<String, Integer> vis = new LinkedHashMap<>();
        for (String tag : WandData.PRIMALS) vis.put(tag, materials.cost());
        return Map.copyOf(vis);
    }

    @Override public String research() { return RESEARCH; }
    @Override public Map<String, Integer> vis() { return Map.of(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.create(); }
    @Override public NonNullList<ItemStack> getRemainingItems(ArcaneRecipeInput input) { return NonNullList.withSize(9, ItemStack.EMPTY); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width == 3 && height == 3; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftWandEnchantments.ARCANE_SCEPTRE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(); }

    /**
     * Book-only input fixture; derive its output/cost through THIS registered recipe, never a global fake result.
     * The parts may be native or add-on tags; an unknown tag, or an add-on part without an item, throws.
     */
    public static ArcaneRecipeInput displayInput(String rod, String cap) {
        ItemStack capItem = WandPartItems.capItem(WandMaterials.cap(cap)), rodItem = WandPartItems.rodItem(WandMaterials.rod(rod));
        if (capItem.isEmpty() || rodItem.isEmpty()) throw new IllegalArgumentException("Wand part without an item: " + rod + "/" + cap);
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        items.set(CAP1, capItem.copyWithCount(1));
        items.set(CAP2, capItem.copyWithCount(1));
        items.set(CAP3, capItem.copyWithCount(1));
        items.set(ROD, rodItem.copyWithCount(1));
        items.set(FOCUS, focusItem());
        return new ArcaneRecipeInput(items);
    }

    public static final class Serializer implements RecipeSerializer<ArcaneSceptreRecipe> {
        // StreamCodec.unit checks equality when encoding. JSON and network must
        // share the same stateless value, or joining clients cannot receive
        // ClientboundUpdateRecipesPacket even though server crafting works.
        private static final StreamCodec<RegistryFriendlyByteBuf, ArcaneSceptreRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        @Override public MapCodec<ArcaneSceptreRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ArcaneSceptreRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
