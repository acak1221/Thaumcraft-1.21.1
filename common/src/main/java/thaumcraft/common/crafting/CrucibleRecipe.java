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
                             Map<String, Integer> aspects, int sourceOrder) implements Recipe<CrucibleRecipeInput> {
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

    private DataResult<CrucibleRecipe> validate() {
        return result.isEmpty() || aspects.isEmpty() || research.isEmpty()
                ? DataResult.error(() -> "Crucible recipe requires a result, research and aspect cost") : DataResult.success(this);
    }

    @Override public boolean matches(CrucibleRecipeInput input, Level level) {
        if (!catalyst.test(input.catalyst())) return false;
        for (var entry : aspects.entrySet()) {
            if (input.aspects().getAmount(Aspect.getAspect(entry.getKey())) < entry.getValue()) return false;
        }
        return true;
    }

    public AspectList removeMatching(AspectList contents) {
        AspectList remaining = contents.copy();
        for (var entry : aspects.entrySet()) remaining.remove(Aspect.getAspect(entry.getKey()), entry.getValue());
        return remaining;
    }

    @Override public ItemStack assemble(CrucibleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
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
