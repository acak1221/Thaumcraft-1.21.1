package thaumcraft.api.crafting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The shared parsing of the original recipe arguments.
 *
 * <p>Forced 1.21.1 change: the original recipe arguments accepted a Forge ore name; the native port accepts
 * a namespaced item tag, because 1.21.1 has no ore dictionary.</p>
 */
public final class ArcaneRecipes {
    private ArcaneRecipes() {}

    /** The vis cost of an original aspect list: only the primal aspects are representable. */
    public static Map<String, Integer> vis(AspectList aspects) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (aspects == null) return result;
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect == null || !aspect.isPrimal()) continue;
            int amount = aspects.getAmount(aspect);
            if (amount <= 0) continue;
            result.put(aspect.getTag(), amount);
        }
        return result;
    }

    /** The aspect cost of an original aspect list: every aspect is representable, not only the primal ones. */
    public static Map<String, Integer> aspectCosts(AspectList aspects) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (aspects == null) return result;
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect == null) continue;
            int amount = aspects.getAmount(aspect);
            if (amount <= 0) continue;
            result.put(aspect.getTag(), amount);
        }
        return result;
    }

    /** The ingredient of one original recipe argument. */
    public static Ingredient ingredient(Object value) {
        if (value instanceof ItemStack stack) {
            if (stack.isEmpty()) throw new IllegalArgumentException("An empty stack is not a recipe ingredient");
            return Ingredient.of(stack.copyWithCount(1));
        }
        if (value instanceof Item item) return Ingredient.of(item);
        if (value instanceof Block block) return Ingredient.of(block.asItem());
        if (value instanceof Ingredient ingredient) return ingredient;
        if (value instanceof String name) {
            ResourceLocation id = name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
            if (id == null) throw new IllegalArgumentException("Invalid recipe tag: " + name);
            Ingredient ingredient = Ingredient.of(TagKey.create(Registries.ITEM, id));
            if (ingredient.isEmpty()) throw new IllegalArgumentException("Unknown recipe item tag: " + name);
            return ingredient;
        }
        throw new IllegalArgumentException("Invalid recipe ingredient: " + value);
    }

    /** The live registry view a recipe needs to answer its result. */
    public static RegistryAccess registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    /** The original pattern rows of a recipe argument list. */
    public static List<String> rows(Object[] recipe, int[] index) {
        List<String> rows = new java.util.ArrayList<>();
        int at = index[0];
        if (at < recipe.length && recipe[at] instanceof String[] parts) {
            rows.addAll(List.of(parts));
            index[0] = at + 1;
            return rows;
        }
        while (at < recipe.length && recipe[at] instanceof String row) {
            rows.add(row);
            at++;
        }
        index[0] = at;
        if (rows.isEmpty()) throw new IllegalArgumentException("A recipe needs a pattern");
        return rows;
    }
}
