package thaumcraft.client.lib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.lib.runic.RunicArmor;

/**
 * The recipe pages a recipe viewer shows for the recipes that are not one fixed grid: every wand
 * and sceptre a construct recipe accepts, every hardening of a runic augmentation, and the items an
 * enchantment infusion takes. EMI and JEI both read them, so the two viewers list the same pages.
 */
public final class RecipeViews {
    private RecipeViews() {}

    /** One arcane page: the nine workbench cells in slot order, the result and the vis. */
    public record Arcane(ResourceLocation id, String research, List<ItemStack> grid, ItemStack result, Map<String, Integer> vis) {}

    /** One infusion page: the central item, the components, the essentia and the details. */
    public record Infusion(ResourceLocation id, String research, Ingredient central, List<Ingredient> components,
                           ItemStack result, Map<String, Integer> aspects, int instability, List<Component> details) {}

    /** The page of a plain infusion recipe. */
    public static Infusion infusion(ResourceLocation id, InfusionStackRecipe recipe, HolderLookup.Provider registries) {
        List<Ingredient> components = new ArrayList<>();
        for (int i = 0; i < recipe.components().size(); i++) components.add(Ingredient.of(componentChoices(recipe, i).stream()));
        return new Infusion(id, recipe.research(), damaged(recipe.input(), recipe.inputDamage().orElse(null)), components,
                recipe.getResultItem(registries), recipe.aspects(), recipe.instability(), List.of());
    }

    /** The wand and sceptre recipes take any registered rod and cap: every combination the recipe accepts, with its cost. */
    public static List<Arcane> constructs(NativeArcaneRecipe recipe, HolderLookup.Provider registries) {
        if (!(recipe instanceof ArcaneWandRecipe || recipe instanceof ArcaneSceptreRecipe)) return List.of();
        boolean sceptre = recipe instanceof ArcaneSceptreRecipe;
        List<Arcane> variants = new ArrayList<>();
        for (var rod : WandMaterials.RODS) {
            for (var cap : WandMaterials.CAPS) {
                ArcaneRecipeInput input;
                try {
                    input = sceptre ? ArcaneSceptreRecipe.displayInput(rod.tag(), cap.tag()) : ArcaneWandRecipe.displayInput(rod.tag(), cap.tag());
                } catch (IllegalArgumentException unsupported) {
                    continue;
                }
                if (!recipe.matches(input, null)) continue;
                ItemStack result = recipe.assemble(input, registries);
                if (result.isEmpty()) continue;
                List<ItemStack> stacks = new ArrayList<>();
                for (int slot = 0; slot < 9; slot++) stacks.add(input.getItem(slot));
                String research = sceptre ? recipe.research() : !rod.tag().equals("wood") ? rod.research() : cap.research();
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft",
                        "/" + (sceptre ? "sceptre" : "wand") + "/" + rod.tag() + "/" + cap.tag());
                variants.add(new Arcane(id, research, List.copyOf(stacks), result, recipe.vis(input)));
            }
        }
        return variants;
    }

    /** The items a component slot of an infusion shows, with the predicate's display stack and the declared damage. */
    public static List<ItemStack> componentChoices(InfusionStackRecipe recipe, int index) {
        var predicate = recipe.selectedComponents().get(index);
        Integer damage = recipe.componentDamage(index).orElse(null);
        List<ItemStack> choices = new ArrayList<>();
        for (ItemStack choice : recipe.components().get(index).getItems()) {
            ItemStack shown = predicate.displayStack(choice);
            if (shown.isEmpty()) continue;
            if (damage != null) shown.setDamageValue(damage);
            choices.add(shown);
        }
        return choices;
    }

    /** The central items of an infusion, worn to the declared damage when the recipe names one. */
    public static Ingredient damaged(Ingredient ingredient, Integer damage) {
        if (damage == null) return ingredient;
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            ItemStack shown = stack.copy();
            shown.setDamageValue(damage);
            stacks.add(shown);
        }
        return Ingredient.of(stacks.stream());
    }

    /** The book page rotates robes already carrying hardening 0..4, each with its own price. */
    public static List<Infusion> runic(ResourceLocation recipeId, InfusionRunicAugmentRecipe recipe) {
        List<Infusion> pages = new ArrayList<>();
        for (int hardening = 0; hardening <= 4; hardening++) {
            ItemStack central = recipe.sample(hardening);
            if (!recipe.canApply(central)) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(recipeId.getNamespace(),
                    "/" + recipeId.getPath() + "/hardening_" + hardening);
            pages.add(new Infusion(id, recipe.research(), Ingredient.of(central), recipe.components(central), RunicArmor.augment(central),
                    costs(recipe.aspectCost(central)), recipe.instability(central), List.of()));
        }
        return pages;
    }

    /** An enchantment infusion over every item the enchantment supports, priced on the first of them. */
    public static Optional<Infusion> enchantment(ResourceLocation id, InfusionEnchantmentRecipe recipe, HolderLookup.Provider registries) {
        var target = recipe.enchantment(registries);
        List<ItemStack> centrals = new ArrayList<>();
        ItemStack sample = ItemStack.EMPTY;
        for (var item : target.value().getSupportedItems()) {
            ItemStack stack = new ItemStack(item);
            if (!InfusionEnchantmentRecipe.canApply(stack, target)) continue;
            if (sample.isEmpty()) sample = stack;
            centrals.add(stack);
        }
        if (centrals.isEmpty()) return Optional.empty();
        List<Component> details = List.of(Enchantment.getFullname(target, 1),
                Component.translatable("tc.port.book_recipe.xp_levels", recipe.xpLevels(sample, registries)));
        return Optional.of(new Infusion(id, recipe.research(), Ingredient.of(centrals.stream()), recipe.components(),
                InfusionEnchantmentRecipe.raise(sample, recipe.enchantment(), registries), costs(recipe.aspectCost(sample, registries)),
                recipe.calculatedInstability(sample), details));
    }

    /** An aspect list as tag to amount, without empty entries. */
    public static Map<String, Integer> costs(AspectList list) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        if (list != null) for (Aspect aspect : list.getAspects()) {
            if (aspect != null && list.getAmount(aspect) > 0) costs.put(aspect.getTag(), list.getAmount(aspect));
        }
        return costs;
    }
}
