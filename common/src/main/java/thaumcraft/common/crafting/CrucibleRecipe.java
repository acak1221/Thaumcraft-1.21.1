package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;

/** Native recipe-manager representation of TC4's crucible recipe, not a crafting-table recipe. */
public record CrucibleRecipe(String research, Ingredient catalyst, ItemStack result,
                             Map<String, Integer> aspects, int sourceOrder,
                             thaumcraft.api.crafting.CrucibleRecipe programmatic) implements Recipe<CrucibleRecipeInput> {
    private static final Codec<String> ASPECT_CODEC = Codec.STRING.comapFlatMap(tag -> Aspect.getAspect(tag) == null
            ? DataResult.error(() -> "Unknown crucible aspect: " + tag) : DataResult.success(tag), tag -> tag);
    public static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.<CrucibleRecipe>mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("research").forGetter(CrucibleRecipe::research),
            Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(CrucibleRecipe::catalyst),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrucibleRecipe::result),
            Codec.unboundedMap(ASPECT_CODEC, Codec.intRange(1, Integer.MAX_VALUE)).fieldOf("aspects").forGetter(CrucibleRecipe::aspects),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("source_order").forGetter(CrucibleRecipe::sourceOrder)
    ).apply(instance, CrucibleRecipe::new)).flatXmap(CrucibleRecipe::validate, CrucibleRecipe::validate);

    public CrucibleRecipe {
        result = result.copy();
        aspects = Collections.unmodifiableMap(new LinkedHashMap<>(aspects));
    }

    public CrucibleRecipe(String research, Ingredient catalyst, ItemStack result, Map<String, Integer> aspects, int sourceOrder) {
        this(research, catalyst, result, aspects, sourceOrder, null);
    }

    /** Runtime-only adapter. It never enters RecipeManager/network recipe serialization. */
    public static CrucibleRecipe programmatic(thaumcraft.api.crafting.CrucibleRecipe recipe) {
        CrucibleRecipe nativeView = recipe.nativeRecipe();
        return new CrucibleRecipe(nativeView.research(), nativeView.catalyst(), nativeView.result(),
                nativeView.aspects(), nativeView.sourceOrder(), recipe);
    }

    public String research() { return programmatic == null ? research : java.util.Objects.toString(programmatic.key, ""); }
    public Map<String, Integer> aspects() { return programmatic == null ? aspects : thaumcraft.api.crafting.ArcaneRecipes.aspectCosts(programmatic.aspects); }
    public boolean catalystMatches(ItemStack stack) { return programmatic == null ? catalyst.test(stack) : programmatic.catalystMatches(stack); }

    private DataResult<CrucibleRecipe> validate() {
        return result.isEmpty() || aspects.isEmpty() || research.isEmpty()
                ? DataResult.error(() -> "Crucible recipe requires a result, research and aspect cost") : DataResult.success(this);
    }

    @Override public boolean matches(CrucibleRecipeInput input, Level level) {
        if (programmatic != null) return programmatic.matches(input.aspects(), input.catalyst());
        if (!catalyst.test(input.catalyst())) return false;
        for (var entry : aspects.entrySet()) {
            if (input.aspects().getAmount(Aspect.getAspect(entry.getKey())) < entry.getValue()) return false;
        }
        return true;
    }

    public AspectList removeMatching(AspectList contents) {
        if (programmatic != null) return programmatic.removeMatching(contents);
        AspectList remaining = contents.copy();
        for (var entry : aspects.entrySet()) remaining.remove(Aspect.getAspect(entry.getKey()), entry.getValue());
        return remaining;
    }

    @Override public ItemStack assemble(CrucibleRecipeInput input, HolderLookup.Provider registries) { return getResultItem(registries); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return (programmatic == null ? result : programmatic.getRecipeOutput()).copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, catalyst); }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftAlchemy.CRUCIBLE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftAlchemy.CRUCIBLE_RECIPE_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<CrucibleRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<CrucibleRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
