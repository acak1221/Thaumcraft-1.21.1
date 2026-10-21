package thaumcraft.api.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.InfusionRecipeInput;

/**
 * The original TC4 enchantment infusion recipe.
 *
 * <p>The native port owns the real recipe in {@code thaumcraft.common.crafting.InfusionEnchantmentRecipe};
 * this class keeps the original argument shape and delegates every answer to the native recipe.</p>
 *
 * <p>Forced 1.21.1 changes: the world became {@link Level}, the player became {@link Player} and the
 * enchantment is a registry holder, because 1.21.1 has no numeric enchantment id.</p>
 */
public class InfusionEnchantmentRecipe {
    public AspectList aspects;
    public String research;
    public ItemStack[] components;
    public Enchantment enchantment;
    public int recipeXP;
    public int instability;
    private final Holder<Enchantment> enchantmentHolder;
    private thaumcraft.common.crafting.InfusionEnchantmentRecipe nativeRecipe;

    public InfusionEnchantmentRecipe(String research, Holder<Enchantment> input, int inst, AspectList aspects, ItemStack[] recipe) {
        if (input == null) throw new IllegalArgumentException("An enchantment infusion recipe needs an enchantment");
        this.research = research;
        this.enchantmentHolder = input;
        this.enchantment = input.value();
        this.aspects = aspects;
        this.components = recipe == null ? new ItemStack[0] : recipe.clone();
        this.instability = inst;
        this.recipeXP = Math.max(1, this.enchantment.getMinCost(1) / 3);
        ResourceLocation id = key(input);
        List<net.minecraft.world.item.crafting.Ingredient> ingredients = new ArrayList<>();
        for (ItemStack component : this.components) ingredients.add(ArcaneRecipes.ingredient(component));
        Map<String, Integer> costs = ArcaneRecipes.aspectCosts(aspects);
        this.nativeRecipe = new thaumcraft.common.crafting.InfusionEnchantmentRecipe(research, id, ingredients, costs, inst, this.enchantment.getMinCost(1));
    }

    /** The native recipe behind this view. */
    public thaumcraft.common.crafting.InfusionEnchantmentRecipe nativeRecipe() { return nativeRecipe; }

    /**
     * The public view of a native enchantment infusion recipe.
     *
     * @return the view, or null when the runtime does not know the enchantment of the recipe
     */
    public static InfusionEnchantmentRecipe of(thaumcraft.common.crafting.InfusionEnchantmentRecipe recipe) {
        Holder<Enchantment> holder = NativeRecipes.holder(recipe.enchantment());
        if (holder == null) return null;
        InfusionEnchantmentRecipe view = new InfusionEnchantmentRecipe(recipe.research(), holder, recipe.instability(),
                NativeRecipes.aspects(recipe.aspects()), samples(recipe.components()));
        view.nativeRecipe = recipe;
        return view;
    }

    private static ItemStack[] samples(List<net.minecraft.world.item.crafting.Ingredient> components) {
        ItemStack[] result = new ItemStack[components.size()];
        for (int index = 0; index < components.size(); index++) result[index] = NativeRecipes.sample(components.get(index));
        return result;
    }

    /** The registry holder of the enchanted effect. */
    public Holder<Enchantment> enchantmentHolder() { return enchantmentHolder; }

    public boolean matches(ArrayList<ItemStack> input, ItemStack central, Level world, Player player) {
        if (this.research != null && !this.research.isEmpty() && !ThaumcraftApiHelper.isResearchComplete(player, this.research)) return false;
        if (input == null || central == null) return false;
        return nativeRecipe.matches(new InfusionRecipeInput(central, input), world);
    }

    /** The original stack comparison; the ore-dictionary shortcut has no 1.21.1 equivalent. */
    protected boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, boolean fuzzy) {
        return InfusionRecipe.areItemStacksEqual(stack0, stack1, fuzzy);
    }

    public Enchantment getEnchantment() { return this.enchantment; }

    public AspectList getAspects() { return this.aspects; }

    public String getResearch() { return this.research; }

    /** The original instability: the enchantments of the item plus the recipe's own instability. */
    public int calcInstability(ItemStack recipeInput) {
        int total = 0;
        for (var holder : recipeInput.getEnchantments().keySet()) {
            total += recipeInput.getEnchantments().getLevel(holder);
        }
        return total / 2 + this.instability;
    }

    /** The original experience cost of the infusion. */
    public int calcXP(ItemStack recipeInput) {
        return this.recipeXP * (1 + recipeInput.getEnchantments().getLevel(enchantmentHolder));
    }

    /** The original essentia modifier of the infusion. */
    public float getEssentiaMod(ItemStack recipeInput) {
        float mod = recipeInput.getEnchantments().getLevel(enchantmentHolder);
        for (var holder : recipeInput.getEnchantments().keySet()) {
            if (holder.is(enchantmentHolder)) continue;
            mod += recipeInput.getEnchantments().getLevel(holder) * 0.1F;
        }
        return mod;
    }

    /** The registry identifier of the enchantment; a direct holder has none. */
    private static ResourceLocation key(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(key -> key.location()).orElseThrow(() ->
                new IllegalArgumentException("An enchantment infusion recipe needs a registered enchantment"));
    }
}
