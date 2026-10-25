package thaumcraft.common.crafting;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.api.aspects.Aspect;

/** Display-only opening snapshot; clients never reconstruct or execute server addon callbacks. */
public final class CrucibleRecipeDisplay {
    public static final int MAX_RECIPES = 4096;
    public static final int MAX_BYTES = 1 << 20;
    private CrucibleRecipeDisplay() {}

    public static void write(FriendlyByteBuf raw, RegistryAccess registries, List<RecipeHolder<CrucibleRecipe>> recipes) {
        if (recipes.size() > MAX_RECIPES) throw new IllegalArgumentException("Too many Thaumatorium formulas");
        int start = raw.writerIndex();
        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(raw, registries);
        wire.writeVarInt(recipes.size());
        for (var holder : recipes) {
            CrucibleRecipe recipe = holder.value();
            wire.writeResourceLocation(holder.id());
            wire.writeUtf(recipe.research());
            wire.writeVarInt(recipe.sourceOrder());
            Ingredient.CONTENTS_STREAM_CODEC.encode(wire, recipe.catalyst());
            ItemStack.STREAM_CODEC.encode(wire, recipe.getResultItem(registries));
            var aspects = recipe.aspects();
            wire.writeVarInt(aspects.size());
            aspects.forEach((aspect, amount) -> { wire.writeUtf(aspect); wire.writeVarInt(amount); });
            if (raw.writerIndex() - start > MAX_BYTES) throw new IllegalArgumentException("Thaumatorium opening data is too large");
        }
    }

    public static List<RecipeHolder<CrucibleRecipe>> read(FriendlyByteBuf raw, RegistryAccess registries) {
        if (raw.readableBytes() > MAX_BYTES) throw new DecoderException("Thaumatorium opening data is too large");
        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(raw, registries);
        int count = wire.readVarInt();
        if (count < 0 || count > MAX_RECIPES) throw new DecoderException("Invalid Thaumatorium formula count");
        List<RecipeHolder<CrucibleRecipe>> recipes = new ArrayList<>(count);
        var ids = new HashSet<net.minecraft.resources.ResourceLocation>();
        for (int index = 0; index < count; index++) {
            var id = wire.readResourceLocation();
            if (!ids.add(id)) throw new DecoderException("Duplicate Thaumatorium formula id");
            String research = wire.readUtf();
            int order = wire.readVarInt();
            Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(wire);
            ItemStack output = ItemStack.STREAM_CODEC.decode(wire);
            int aspects = wire.readVarInt();
            if (order < 0 || catalyst.isEmpty() || output.isEmpty() || aspects < 0 || aspects > Aspect.aspects.size())
                throw new DecoderException("Invalid Thaumatorium formula preview");
            var costs = new LinkedHashMap<String, Integer>();
            for (int aspect = 0; aspect < aspects; aspect++) {
                String tag = wire.readUtf(); int amount = wire.readVarInt();
                if (Aspect.getAspect(tag) == null || amount <= 0 || costs.put(tag, amount) != null)
                    throw new DecoderException("Invalid Thaumatorium aspect cost");
            }
            recipes.add(new RecipeHolder<>(id, new CrucibleRecipe(research, catalyst, output, costs, order)));
        }
        if (wire.readableBytes() != 0) throw new DecoderException("Trailing Thaumatorium opening data");
        return List.copyOf(recipes);
    }
}
