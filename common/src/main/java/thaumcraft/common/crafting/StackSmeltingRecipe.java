package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import thaumcraft.common.config.ThaumcraftAlchemy;

/**
 * A smelting recipe whose result may be more than one item. The original furnace table
 * ({@code FurnaceRecipes.addSmelting(cluster, new ItemStack(ingot, 2), xp)}, ConfigRecipes:469-471) pays two ingots for one native cluster, but
 * 1.21.1's {@code minecraft:smelting} reads its result with the single-item codec and silently drops the count. This is still a
 * {@code RecipeType.SMELTING}, so every vanilla furnace, and the recipe book, finds and runs it; only the result codec keeps the count.
 */
public class StackSmeltingRecipe extends SmeltingRecipe {
    public static final MapCodec<StackSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(recipe -> recipe.group),
            CookingBookCategory.CODEC.fieldOf("category").orElse(CookingBookCategory.MISC).forGetter(recipe -> recipe.category),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(recipe -> recipe.ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Codec.FLOAT.fieldOf("experience").orElse(0.0F).forGetter(recipe -> recipe.experience),
            Codec.INT.fieldOf("cookingtime").orElse(200).forGetter(recipe -> recipe.cookingTime)
    ).apply(instance, StackSmeltingRecipe::new));

    public StackSmeltingRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(group, category, ingredient, result, experience, cookingTime);
    }

    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftAlchemy.STACK_SMELTING_SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<StackSmeltingRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, StackSmeltingRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<StackSmeltingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, StackSmeltingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
