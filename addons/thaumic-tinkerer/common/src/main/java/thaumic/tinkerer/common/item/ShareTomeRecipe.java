/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the survival recipe of the Tome of Knowledge Sharing
 * as a recipe type that honours the option {@code shareTome.survival.enabled}. Distributed under the same licence as the
 * add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.ItemsTC;
import thaumic.tinkerer.common.content.BasicItems;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/**
 * A shaped recipe that only crafts while {@link ConfigHandler#enableSurvivalShareTome} is on, and only with unused Scribing
 * Tools.
 *
 * <p>1.7.10 registered the recipe of the Tome of Knowledge Sharing only when the option was on; a data recipe is always
 * loaded, so the option is asked each time the grid is matched. The file is an ordinary shaped recipe with {@code "type":
 * "thaumictinkerer:share_tome"}, and the research page and the recipe viewers draw it as the shaped recipe it is.</p>
 *
 * <p>The pattern asked for {@code new ItemStack(ConfigItems.itemInkwell)}, the Scribing Tools with the damage value 0, and
 * a 1.7.10 shaped recipe matches the damage value of a stack that is not the wildcard exactly: only an unused set of tools
 * (full ink) made the tome. An ingredient of 1.21.1 has no damage value, so the recipe checks it here: a used set of tools
 * anywhere in the grid does not match.</p>
 */
public class ShareTomeRecipe extends ShapedRecipe {
    /** {@code thaumcraft:inkwell}, the Scribing Tools ({@code itemInkwell:0}). */
    private static final ResourceLocation SCRIBING_TOOLS = ItemsTC.id("inkwell");

    private final ShapedRecipePattern pattern;
    private final ItemStack result;

    public ShareTomeRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result,
            boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!ConfigHandler.enableSurvivalShareTome || !super.matches(input, level)) return false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getDamageValue() != 0
                    && SCRIBING_TOOLS.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BasicItems.SHARE_TOME_SERIALIZER.get();
    }

    /** The serializer: the format and the network form of the vanilla shaped recipe. */
    public static final class Serializer implements RecipeSerializer<ShareTomeRecipe> {
        public static final MapCodec<ShareTomeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC)
                        .forGetter(ShapedRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification))
                .apply(instance, ShareTomeRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShareTomeRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
                CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                ByteBufCodecs.BOOL, ShapedRecipe::showNotification,
                ShareTomeRecipe::new);

        @Override
        public MapCodec<ShareTomeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShareTomeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
