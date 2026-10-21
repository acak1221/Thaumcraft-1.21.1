package thaumcraft.api.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;

/**
 * The original TC4 infusion recipe.
 *
 * <p>The native port owns the real recipe in {@link InfusionStackRecipe}; this class keeps the original
 * argument shape and delegates every answer to the native recipe.</p>
 *
 * <p>Forced 1.21.1 changes: the world became {@link Level}, the player became {@link Player} and the original
 * {@code Object[]} result form became a named native result patch.</p>
 */
public class InfusionRecipe {
    protected AspectList aspects;
    protected String research;
    private ItemStack[] components;
    private ItemStack recipeInput;
    protected Object recipeOutput;
    protected int instability;
    private InfusionStackRecipe nativeRecipe;

    public InfusionRecipe(String research, Object output, int inst, AspectList aspects, ItemStack input, ItemStack[] recipe) {
        this.research = research;
        this.recipeOutput = output;
        this.recipeInput = input == null ? null : input.copy();
        this.aspects = aspects;
        this.components = recipe == null ? new ItemStack[0] : recipe.clone();
        this.instability = inst;
        List<Ingredient> ingredients = new ArrayList<>();
        for (ItemStack component : this.components) ingredients.add(ArcaneRecipes.ingredient(component));
        Map<String, Integer> costs = ArcaneRecipes.aspectCosts(aspects);
        if (input == null) throw new IllegalArgumentException("An infusion recipe needs a central input");
        ItemStack sample = output instanceof ItemStack stack ? stack.copy() : input.copy();
        String patch = output instanceof Object[] ? patchName(output) : null;
        this.nativeRecipe = new InfusionStackRecipe(research, ArcaneRecipes.ingredient(input), ingredients, costs, inst, sample,
                List.of(), java.util.Optional.empty(), patch == null ? java.util.Optional.empty() : java.util.Optional.of(patch));
    }

    /** The native recipe behind this view. */
    public InfusionStackRecipe nativeRecipe() { return nativeRecipe; }

    /**
     * The public view of a native infusion recipe.
     *
     * <p>Forced 1.21.1 change: the native recipe of the recipe manager is the real one, with its own
     * component predicates and result patches; the view keeps the original argument shape.</p>
     */
    public static InfusionRecipe of(InfusionStackRecipe recipe) {
        InfusionRecipe view = new InfusionRecipe(recipe.research(), recipe.result(), recipe.instability(),
                NativeRecipes.aspects(recipe.aspects()), NativeRecipes.sample(recipe.input()), samples(recipe.components()));
        view.nativeRecipe = recipe;
        return view;
    }

    private static ItemStack[] samples(List<Ingredient> components) {
        ItemStack[] result = new ItemStack[components.size()];
        for (int index = 0; index < components.size(); index++) result[index] = NativeRecipes.sample(components.get(index));
        return result;
    }

    public boolean matches(ArrayList<ItemStack> input, ItemStack central, Level world, Player player) {
        if (this.getRecipeInput() == null) return false;
        if (this.research != null && !this.research.isEmpty() && !ThaumcraftApiHelper.isResearchComplete(player, this.research)) return false;
        if (input == null || central == null) return false;
        return nativeRecipe.matches(new InfusionRecipeInput(central, input), world);
    }

    /**
     * The original stack comparison.
     *
     * <p>Forced 1.21.1 change: the original ore-dictionary shortcut has no 1.21.1 equivalent, so the fuzzy
     * flag only reports that the shortcut was requested.</p>
     */
    public static boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, boolean fuzzy) {
        if (stack0 == null && stack1 != null) return false;
        if (stack0 != null && stack1 == null) return false;
        if (stack0 == null) return true;
        if (!ThaumcraftApiHelper.areItemStackTagsEqualForCrafting(stack0, stack1)) return false;
        boolean damage = stack0.getDamageValue() == stack1.getDamageValue() || stack1.getDamageValue() == Short.MAX_VALUE;
        return stack0.getItem() == stack1.getItem() && damage;
    }

    public Object getRecipeOutput() { return this.getRecipeOutput(this.getRecipeInput()); }

    public AspectList getAspects() { return this.getAspects(this.getRecipeInput()); }

    public int getInstability() { return this.getInstability(this.getRecipeInput()); }

    public String getResearch() { return this.research; }

    public ItemStack getRecipeInput() { return this.recipeInput; }

    public ItemStack[] getComponents() { return this.components; }

    public Object getRecipeOutput(ItemStack input) { return this.recipeOutput; }

    public AspectList getAspects(ItemStack input) { return this.aspects; }

    public int getInstability(ItemStack input) { return this.instability; }

    /** The original result keys became named native result patches. */
    private static String patchName(Object output) {
        Object[] patch = (Object[]) output;
        String key = patch.length > 0 && patch[0] != null ? patch[0].toString() : "";
        return switch (key.toUpperCase(java.util.Locale.ROOT)) {
            case "ADVANCEDGOLEM" -> thaumcraft.common.crafting.InfusionResultPatches.ADVANCED_GOLEM;
            case "HELMGOGGLES" -> thaumcraft.common.crafting.InfusionResultPatches.FORTRESS_GOGGLES;
            case "MASKGRINNINGDEVIL" -> thaumcraft.common.crafting.InfusionResultPatches.FORTRESS_MASK_DEVIL;
            case "MASKANGRYGHOST" -> thaumcraft.common.crafting.InfusionResultPatches.FORTRESS_MASK_GHOST;
            case "MASKSIPPINGFIEND" -> thaumcraft.common.crafting.InfusionResultPatches.FORTRESS_MASK_FIEND;
            default -> throw new IllegalArgumentException("Unknown infusion result patch: " + key);
        };
    }

    /** The aspects of this recipe, in the native map form. */
    public Map<String, Integer> aspectCosts() { return ArcaneRecipes.aspectCosts(aspects); }

    /** The aspect with the highest amount of this recipe, or null. */
    public Aspect primaryAspect() {
        if (aspects == null) return null;
        Aspect best = null;
        int highest = 0;
        for (Aspect aspect : aspects.getAspects()) {
            if (aspects.getAmount(aspect) <= highest) continue;
            best = aspect;
            highest = aspects.getAmount(aspect);
        }
        return best;
    }
}
