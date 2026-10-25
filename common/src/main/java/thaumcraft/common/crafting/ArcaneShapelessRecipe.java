package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.wands.WandData;

/** TC4 ShapelessArcaneRecipe, consumed by the same authoritative arcane menu as shaped recipes. */
public record ArcaneShapelessRecipe(String research, List<Ingredient> ingredients, ItemStack result,
                                   Map<String, Integer> vis) implements NativeArcaneRecipe {
    private static final int MAX_VIS = Integer.MAX_VALUE / 110;
    private static final Codec<String> PRIMAL_CODEC = Codec.STRING.comapFlatMap(tag -> WandData.PRIMALS.contains(tag)
            ? DataResult.success(tag) : DataResult.error(() -> "Not a primal vis aspect: " + tag), tag -> tag);
    private static final Codec<Map<String, Integer>> VIS_CODEC = Codec.unboundedMap(PRIMAL_CODEC, Codec.intRange(1, MAX_VIS));
    // ItemStack.STRICT_CODEC still uses the vanilla LENIENT optional count
    // field: an explicitly invalid count0 can silently become its default1.
    // Recipe data must reject that malformed output, not manufacture a result.
    private static final Codec<ItemStack> RESULT_CODEC = RecordCodecBuilder.<ItemStack>create(instance -> instance.group(
            ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemStack::getCount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStack::getComponentsPatch)
    ).apply(instance, ItemStack::new)).flatXmap(ArcaneShapelessRecipe::validateResult, ArcaneShapelessRecipe::validateResult);
    public static final MapCodec<ArcaneShapelessRecipe> CODEC = RecordCodecBuilder.<ArcaneShapelessRecipe>mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("research", "").forGetter(ArcaneShapelessRecipe::research),
            Ingredient.CODEC_NONEMPTY.listOf(1, 9).fieldOf("ingredients").forGetter(ArcaneShapelessRecipe::ingredients),
            RESULT_CODEC.fieldOf("result").forGetter(ArcaneShapelessRecipe::result),
            VIS_CODEC.fieldOf("vis").forGetter(ArcaneShapelessRecipe::vis)
    ).apply(instance, ArcaneShapelessRecipe::new)).flatXmap(ArcaneShapelessRecipe::validate, ArcaneShapelessRecipe::validate);

    public ArcaneShapelessRecipe {
        ingredients = List.copyOf(ingredients);
        result = result.copy();
        vis = Collections.unmodifiableMap(new LinkedHashMap<>(vis));
    }

    private static DataResult<ItemStack> validateResult(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()) {
            return DataResult.error(() -> "Shapeless arcane recipe requires a nonempty, valid result stack");
        }
        return ItemStack.validateComponents(stack.getComponents()).map(ignored -> stack);
    }

    private DataResult<ArcaneShapelessRecipe> validate() {
        if (ingredients.isEmpty() || ingredients.size() > 9 || ingredients.stream().anyMatch(Ingredient::isEmpty)) {
            return DataResult.error(() -> "Shapeless arcane recipe requires 1..9 nonempty ingredients");
        }
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) {
            return DataResult.error(() -> "Shapeless arcane recipe requires a nonempty, valid result stack");
        }
        if (vis.isEmpty() || vis.entrySet().stream().anyMatch(entry -> !WandData.PRIMALS.contains(entry.getKey())
                || entry.getValue() == null || entry.getValue() <= 0 || entry.getValue() > MAX_VIS)) {
            return DataResult.error(() -> "Shapeless arcane recipe requires positive, bounded primal vis costs");
        }
        return DataResult.success(this);
    }

    @Override public boolean matches(ArcaneRecipeInput input, Level level) {
        return matches(input, level, ArcaneIngredientMatching::matchesStack);
    }

    public boolean matches(ArcaneRecipeInput input, Level level, java.util.function.BiPredicate<ItemStack, ItemStack> comparison) {
        var remaining = new ArrayList<>(ingredients);
        // Oracle ShapelessArcaneRecipe:93-117 visits slots0..8, removing the FIRST
        // remaining matching ingredient. Preserve that greedy overlap quirk: no
        // sorting, aggregate item counts or bipartite/backtracking replacement.
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            // Original filled/labelled jars were a different item. Flattening
            // must not turn their stored contents/filter into a plain ingredient.
            if (stack.getItem() instanceof ItemJar && !ItemJar.isEmptyIngredient(stack)) return false;
            int matched = -1;
            for (int index = 0; index < remaining.size(); index++) {
                if (ArcaneIngredientMatching.matches(remaining.get(index), stack, comparison)) { matched = index; break; }
            }
            if (matched < 0) return false;
            remaining.remove(matched);
        }
        return remaining.isEmpty();
    }

    @Override public ItemStack assemble(ArcaneRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width > 0 && height > 0 && (long) width * height >= ingredients.size(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> copy = NonNullList.create();
        copy.addAll(ingredients);
        return copy;
    }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftArcane.ARCANE_SHAPELESS_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<ArcaneShapelessRecipe> {
        // Both directions share JSON's validation; the network cannot bypass the
        // ingredient count, strict result, primal identity or positive cost checks.
        private static final StreamCodec<RegistryFriendlyByteBuf, ArcaneShapelessRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<ArcaneShapelessRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ArcaneShapelessRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
