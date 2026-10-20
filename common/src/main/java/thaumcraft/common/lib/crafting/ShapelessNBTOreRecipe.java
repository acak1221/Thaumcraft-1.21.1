package thaumcraft.common.lib.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftWandEnchantments;

/**
 * Original ShapelessNBTOreRecipe (oracle thaumcraft/common/lib/crafting/ShapelessNBTOreRecipe.java).
 *
 * A shapeless crafting-table recipe whose ingredients may be named by an ore-dictionary style tag and
 * whose result keeps its own data. The original semantics are preserved exactly:
 * - the result is the declared stack, copied, never rebuilt (line 71-77),
 * - an ingredient matches only when the item, the whole NBT and the stack size agree; the ingredient
 *   count is the wildcard {@code Short.MAX_VALUE} (line 107-109),
 * - the grid is consumed greedily: every filled slot takes the FIRST still missing requirement that
 *   fits it, and the recipe only matches when no requirement is left over (line 79-105).
 *
 * 1.21.1 has no NBT tags and no ore dictionary: the tag form of an ingredient is the item tag, and
 * the NBT of a stack is its data component patch.
 */
public class ShapelessNBTOreRecipe implements CraftingRecipe {
    /** The wildcard stack size of the original ingredient stacks (line 108). */
    public static final int WILDCARD_COUNT = Short.MAX_VALUE;

    /**
     * One required ingredient. The original stored an ItemStack or an ore-dictionary list; the tag form
     * is the 1.21.1 equivalent of an ore name and the count keeps the original stack size rule.
     */
    public record Requirement(Ingredient ingredient, int count, DataComponentPatch components) {
        public Requirement {
            if (ingredient == null || ingredient.isEmpty()) throw new IllegalArgumentException("Empty requirement");
            if (count < 1) throw new IllegalArgumentException("Invalid requirement count: " + count);
            if (components == null) components = DataComponentPatch.EMPTY;
        }

        /** Original ShapelessNBTOreRecipe:107-109. */
        public boolean matches(ItemStack slot) {
            if (slot.isEmpty() || !ingredient.test(slot)) return false;
            // The original compares the whole NBT of the ingredient and of the slot.
            if (!components.equals(slot.getComponentsPatch())) return false;
            return count == WILDCARD_COUNT || count == slot.getCount();
        }
    }

    private static final MapCodec<Requirement> REQUIREMENT_CODEC = RecordCodecBuilder.<Requirement>mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Requirement::ingredient),
            // The original built its ingredients with new ItemStack(item), so the count was one and the
            // slot had to hold exactly one item. Short.MAX_VALUE keeps the original wildcard.
            Codec.intRange(1, WILDCARD_COUNT).optionalFieldOf("count", 1).forGetter(Requirement::count),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(Requirement::components)
    ).apply(instance, Requirement::new));

    public static final MapCodec<ShapelessNBTOreRecipe> CODEC = RecordCodecBuilder.<ShapelessNBTOreRecipe>mapCodec(instance -> instance.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(ShapelessNBTOreRecipe::category),
            REQUIREMENT_CODEC.codec().listOf().fieldOf("ingredients").forGetter(ShapelessNBTOreRecipe::requirements),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ShapelessNBTOreRecipe::result)
    ).apply(instance, ShapelessNBTOreRecipe::new));

    private final CraftingBookCategory category;
    private final List<Requirement> requirements;
    private final ItemStack result;

    public ShapelessNBTOreRecipe(CraftingBookCategory category, List<Requirement> requirements, ItemStack result) {
        this.category = category == null ? CraftingBookCategory.MISC : category;
        this.requirements = List.copyOf(requirements);
        this.result = result.copy();
    }

    public CraftingBookCategory category() { return category; }
    public List<Requirement> requirements() { return requirements; }
    public ItemStack result() { return result; }

    /**
     * Original ShapelessNBTOreRecipe:79-105. Every filled slot takes the FIRST still missing
     * requirement that fits it; a slot that fits nothing fails the recipe, and the recipe only
     * matches when no requirement is left over.
     */
    public static boolean matches(List<Requirement> required, CraftingInput input) {
        List<Requirement> remaining = new ArrayList<>(required);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            boolean inRecipe = false;
            for (int index = 0; index < remaining.size(); index++) {
                Requirement requirement = remaining.get(index);
                if (!requirement.matches(stack)) continue;
                remaining.remove(index);
                inRecipe = true;
                break;
            }
            if (!inRecipe) return false;
        }
        return remaining.isEmpty();
    }

    @Override public boolean matches(CraftingInput input, Level level) { return matches(requirements, input); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public boolean canCraftInDimensions(int width, int height) { return (long) width * height >= requirements.size(); }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (Requirement requirement : requirements) ingredients.add(requirement.ingredient());
        return ingredients;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }
    @Override public boolean isSpecial() { return false; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftWandEnchantments.SHAPELESS_NBT_ORE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return RecipeType.CRAFTING; }

    public static final class Serializer implements RecipeSerializer<ShapelessNBTOreRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, ShapelessNBTOreRecipe> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        @Override public MapCodec<ShapelessNBTOreRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ShapelessNBTOreRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
