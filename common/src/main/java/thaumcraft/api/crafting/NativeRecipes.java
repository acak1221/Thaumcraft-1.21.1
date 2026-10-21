package thaumcraft.api.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.InfusionStackRecipe;

/**
 * The native recipe registry behind the public API.
 *
 * <p>Forced 1.21.1 change: the original kept every recipe of the game in the static list of
 * {@code ThaumcraftApi}; 1.21.1 keeps the recipes of a world in the recipe manager of the loaded level.
 * The runtime installs that manager through {@link #install(RecipeManager)} and every answer of the
 * public API is read from it.</p>
 *
 * <p>The native recipes are the real ones; the public API answers with the original views of them
 * ({@link ShapedArcaneRecipe}, {@link ShapelessArcaneRecipe}, {@link CrucibleRecipe},
 * {@link InfusionRecipe} and {@link InfusionEnchantmentRecipe}), exactly as the original published
 * the objects it registered itself.</p>
 */
public final class NativeRecipes {
    private static final AtomicReference<RecipeManager> MANAGER = new AtomicReference<>();
    private static final AtomicReference<HolderLookup.Provider> REGISTRIES = new AtomicReference<>();

    private NativeRecipes() {}

    /** The recipe manager of the loaded level, or null when no level is loaded. */
    public static RecipeManager manager() { return MANAGER.get(); }

    /** The registry access of the loaded level, or null when no level is loaded. */
    public static HolderLookup.Provider registries() { return REGISTRIES.get(); }

    /** The runtime installs the recipe manager and the registries of the level it loaded. */
    public static void install(RecipeManager manager, HolderLookup.Provider registries) {
        MANAGER.set(manager);
        REGISTRIES.set(registries);
    }

    /** Every native recipe of the loaded level, in the order of the recipe manager. */
    public static List<Object> nativeRecipes() {
        RecipeManager manager = MANAGER.get();
        if (manager == null) return List.of();
        List<Object> result = new ArrayList<>();
        for (var holder : manager.getRecipes()) result.add(holder.value());
        return result;
    }

    /** Every native recipe of the loaded level as the original public view of it. */
    public static List<Object> views() {
        List<Object> result = new ArrayList<>();
        for (Object recipe : nativeRecipes()) {
            Object view = view(recipe);
            if (view != null) result.add(view);
        }
        return result;
    }

    /**
     * The original public view of a native recipe.
     *
     * @return the view, or null when the native recipe has no original counterpart
     */
    public static Object view(Object recipe) {
        if (recipe instanceof ArcaneShapedRecipe shaped) return ShapedArcaneRecipe.of(shaped);
        if (recipe instanceof ArcaneShapelessRecipe shapeless) return ShapelessArcaneRecipe.of(shapeless);
        if (recipe instanceof thaumcraft.common.crafting.CrucibleRecipe crucible) return CrucibleRecipe.of(crucible);
        if (recipe instanceof InfusionStackRecipe infusion) return InfusionRecipe.of(infusion);
        if (recipe instanceof thaumcraft.common.crafting.InfusionEnchantmentRecipe enchantment) return InfusionEnchantmentRecipe.of(enchantment);
        // The original also registered its wand and sceptre recipes themselves; they stay native objects.
        return recipe;
    }

    /** The aspect list of a native aspect cost. */
    public static AspectList aspects(java.util.Map<String, Integer> costs) {
        AspectList result = new AspectList();
        if (costs == null) return result;
        costs.forEach((tag, amount) -> {
            Aspect aspect = Aspect.getAspect(tag);
            if (aspect != null && amount != null && amount > 0) result.add(aspect, amount);
        });
        return result;
    }

    /** The first item an ingredient accepts; the original recipes named one stack per ingredient. */
    public static ItemStack sample(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return ItemStack.EMPTY;
        for (ItemStack stack : ingredient.getItems()) if (!stack.isEmpty()) return stack.copy();
        return ItemStack.EMPTY;
    }

    /** The registry holder of an enchantment, or null when the runtime does not know it. */
    public static Holder<Enchantment> holder(ResourceLocation id) {
        HolderLookup.Provider registries = REGISTRIES.get();
        if (registries == null || id == null) return null;
        return registries.lookup(Registries.ENCHANTMENT)
                .map(lookup -> lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null))
                .orElse(null);
    }

    /** The recipe of the loaded level with this id, or null. */
    public static Recipe<?> byId(ResourceLocation id) {
        RecipeManager manager = MANAGER.get();
        if (manager == null || id == null) return null;
        return manager.byKey(id).map(holder -> holder.value()).orElse(null);
    }
}
