package thaumcraft.common.crafting;

import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
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
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original ArcaneWandRecipe:31-153, restricted to the registered playable rods/caps. Inert caps never count as materials. */
public final class ArcaneWandRecipe implements NativeArcaneRecipe {
    private static final ArcaneWandRecipe INSTANCE = new ArcaneWandRecipe();
    public static final MapCodec<ArcaneWandRecipe> CODEC = MapCodec.unit(INSTANCE);
    // Despite its parameter names, original TileMagicWorkbench:43-46 indexes
    // x + y*3. Thus cap1=(0,2) is slot6, and cap2=(2,0) is slot2.
    private static final int CAP1 = 6, CAP2 = 2, ROD = 4;

    private record Materials(WandMaterials.Rod rod, WandMaterials.Cap cap) {
        int cost() { return rod.craftCost() * cap.craftCost(); }
    }

    private static Materials materials(ArcaneRecipeInput input) {
        for (int slot = 0; slot < 9; slot++) {
            if (slot != CAP1 && slot != CAP2 && slot != ROD && !input.getItem(slot).isEmpty()) return null;
        }
        ItemStack cap1 = input.getItem(CAP1), cap2 = input.getItem(CAP2), rodItem = input.getItem(ROD);
        if (cap1.isEmpty() || cap2.isEmpty() || rodItem.isEmpty() || !checkItemEquals(cap1, cap2)) return null;
        WandMaterials.Cap cap = null;
        WandMaterials.Rod rod = null;
        for (var candidate : WandMaterials.CAPS) {
            if (checkItemEquals(cap1, ThaumcraftWands.capItem(candidate.tag()))) { cap = candidate; break; }
        }
        for (var candidate : WandMaterials.RODS) {
            if (checkItemEquals(rodItem, ThaumcraftWands.rodItem(candidate.tag()))) { rod = candidate; break; }
        }
        return cap == null || rod == null ? null : new Materials(rod, cap);
    }

    /**
     * Original :133-137 checks NBT only when TARGET has it, not symmetrically.
     * Flattened IDs encode legacy item metadata. A native per-stack component
     * patch is the explicit NBT adapter; item-default components are not NBT.
     * Counts never participate. In particular, plain cap1 + named cap2 is valid,
     * but reversing those stacks fails the canonical untagged cap prototype.
     */
    public static boolean checkItemEquals(ItemStack target, ItemStack input) {
        if (target.isEmpty() || input.isEmpty()) return target.isEmpty() && input.isEmpty();
        return ItemStack.isSameItem(target, input) && (target.getComponentsPatch().isEmpty()
                || ItemStack.isSameItemSameComponents(target, input));
    }

    @Override public boolean matches(ArcaneRecipeInput input, Level level) { return materials(input) != null; }

    @Override public boolean hasResearch(ArcaneRecipeInput input, Player actor) {
        Materials materials = materials(input);
        if (materials == null || actor == null) return false;
        var knowledge = KnowledgeAccess.get(actor);
        // Includes CAP_iron / ROD_wood, whose original research entries auto-unlock.
        return knowledge.isResearchComplete(materials.cap().research())
                && knowledge.isResearchComplete(materials.rod().research());
    }

    @Override public ItemStack assemble(ArcaneRecipeInput input, HolderLookup.Provider registries) {
        Materials materials = materials(input);
        // Original :57 excludes this output even though matches/getAspects accept
        // its ingredients. The real ordinary wood/iron recipe retains priority.
        if (materials == null || materials.rod() == WandMaterials.WOOD && materials.cap() == WandMaterials.IRON) return ItemStack.EMPTY;
        ItemStack output = new ItemStack(ThaumcraftContent.WAND.get());
        output.set(ThaumcraftContent.WAND_DATA.get(), new WandData(materials.rod().tag(), materials.cap().tag(), Map.of()));
        output.set(ThaumcraftWands.WAND_CRAFT_COST.get(), materials.cost());
        return output;
    }

    @Override public Map<String, Integer> vis(ArcaneRecipeInput input) {
        Materials materials = materials(input);
        if (materials == null) return Map.of();
        Map<String, Integer> vis = new LinkedHashMap<>();
        for (String tag : WandData.PRIMALS) vis.put(tag, materials.cost());
        return Map.copyOf(vis);
    }

    @Override public String research() { return ""; }
    @Override public Map<String, Integer> vis() { return Map.of(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.create(); }
    @Override public NonNullList<ItemStack> getRemainingItems(ArcaneRecipeInput input) { return NonNullList.withSize(9, ItemStack.EMPTY); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width == 3 && height == 3; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftWands.ARCANE_WAND_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(); }

    /** Book-only input fixture; derive its output/cost through THIS registered recipe, never a global fake result. */
    public static ArcaneRecipeInput displayInput(String rod, String cap) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        items.set(CAP1, ThaumcraftWands.capItem(cap));
        items.set(CAP2, ThaumcraftWands.capItem(cap));
        items.set(ROD, ThaumcraftWands.rodItem(rod));
        return new ArcaneRecipeInput(items);
    }

    public static final class Serializer implements RecipeSerializer<ArcaneWandRecipe> {
        // StreamCodec.unit checks equality when encoding. JSON and network
        // must share the same stateless value, or joining clients cannot receive
        // ClientboundUpdateRecipesPacket even though server crafting works.
        private static final StreamCodec<RegistryFriendlyByteBuf, ArcaneWandRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        @Override public MapCodec<ArcaneWandRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ArcaneWandRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
