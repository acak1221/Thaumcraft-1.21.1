package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.ItemJar;

/** TC4 ShapedArcaneRecipe: literal borders, translated patterns and optional horizontal mirroring. */
public record ArcaneShapedRecipe(String research, List<String> pattern, Map<String, Ingredient> key,
                                ItemStack result, Map<String, Integer> vis, boolean mirrored)
        implements NativeArcaneRecipe {
    private static final Codec<String> PRIMAL_CODEC = Codec.STRING.comapFlatMap(tag -> WandData.PRIMALS.contains(tag)
            ? DataResult.success(tag) : DataResult.error(() -> "Not a primal vis aspect: " + tag), tag -> tag);
    // Multiplication by 100 and the supported iron-cap modifier must remain in int range.
    private static final Codec<Map<String, Integer>> VIS_CODEC = Codec.unboundedMap(PRIMAL_CODEC, Codec.intRange(1, Integer.MAX_VALUE / 110));
    public static final MapCodec<ArcaneShapedRecipe> CODEC = RecordCodecBuilder.<ArcaneShapedRecipe>mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("research", "").forGetter(ArcaneShapedRecipe::research),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(ArcaneShapedRecipe::pattern),
            Codec.unboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(ArcaneShapedRecipe::key),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ArcaneShapedRecipe::result),
            VIS_CODEC.fieldOf("vis").forGetter(ArcaneShapedRecipe::vis),
            Codec.BOOL.optionalFieldOf("mirrored", true).forGetter(ArcaneShapedRecipe::mirrored)
    ).apply(instance, ArcaneShapedRecipe::new)).flatXmap(ArcaneShapedRecipe::validate, ArcaneShapedRecipe::validate);

    public ArcaneShapedRecipe {
        pattern = List.copyOf(pattern);
        key = Map.copyOf(key);
        result = result.copy();
        vis = Collections.unmodifiableMap(new LinkedHashMap<>(vis));
    }

    private DataResult<ArcaneShapedRecipe> validate() {
        if (pattern.isEmpty() || pattern.size() > 3 || pattern.getFirst().isEmpty() || pattern.getFirst().length() > 3) {
            return DataResult.error(() -> "Arcane pattern must be between 1x1 and 3x3");
        }
        Set<String> used = new HashSet<>();
        for (String row : pattern) {
            if (row.length() != pattern.getFirst().length()) return DataResult.error(() -> "Unequal arcane pattern rows");
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == ' ') continue;
                String symbol = row.substring(x, x + 1);
                if (!key.containsKey(symbol)) return DataResult.error(() -> "Undefined arcane ingredient: " + symbol);
                used.add(symbol);
            }
        }
        if (used.isEmpty() || !used.equals(key.keySet()) || key.keySet().stream().anyMatch(symbol -> symbol.length() != 1 || symbol.equals(" "))) {
            return DataResult.error(() -> "Arcane key must contain exactly the non-space pattern symbols");
        }
        if (result.isEmpty() || vis.isEmpty()) return DataResult.error(() -> "Arcane recipe requires a result and vis cost");
        return DataResult.success(this);
    }

    @Override public boolean matches(ArcaneRecipeInput input, Level level) {
        int width = pattern.getFirst().length(), height = pattern.size();
        // Source: ShapedArcaneRecipe:130-172. Do not shrink explicit blank borders.
        for (int x = 0; x <= 3 - width; x++) {
            for (int y = 0; y <= 3 - height; y++) {
                if (matchesAt(input, x, y, false) || mirrored && matchesAt(input, x, y, true)) return true;
            }
        }
        return false;
    }

    private boolean matchesAt(ArcaneRecipeInput input, int offsetX, int offsetY, boolean mirror) {
        int width = pattern.getFirst().length(), height = pattern.size();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                int sx = x - offsetX, sy = y - offsetY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (sx >= 0 && sy >= 0 && sx < width && sy < height) {
                    int index = mirror ? width - 1 - sx : sx;
                    String symbol = pattern.get(sy).substring(index, index + 1);
                    if (!symbol.equals(" ")) ingredient = key.get(symbol);
                }
                ItemStack stack = input.getItem(x + y * 3);
                if (!ArcaneIngredientMatching.matches(ingredient, stack)) return false;
                // Original filled OR labelled jars were ItemJarFilled, not the
                // blockJar item named by plain shaped ingredients (e.g. JarVoid).
                // Flattening them to one native Item must not erase contents.
                // Ordinary custom names on a genuinely empty jar still match.
                if (!ingredient.isEmpty() && stack.getItem() instanceof ItemJar && !ItemJar.isEmptyIngredient(stack)) return false;
            }
        }
        return true;
    }

    @Override public ItemStack assemble(ArcaneRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public boolean canCraftInDimensions(int width, int height) { return width >= pattern.getFirst().length() && height >= pattern.size(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (String row : pattern) for (int x = 0; x < row.length(); x++) ingredients.add(key.getOrDefault(row.substring(x, x + 1), Ingredient.EMPTY));
        return ingredients;
    }
    @Override public boolean isSpecial() { return true; } // Not a vanilla crafting-table/recipe-book recipe.
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftArcane.ARCANE_SHAPED_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<ArcaneShapedRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, ArcaneShapedRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<ArcaneShapedRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ArcaneShapedRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
