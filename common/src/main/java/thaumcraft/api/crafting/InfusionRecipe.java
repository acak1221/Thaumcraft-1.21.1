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
    private boolean nativeView;

    /** Builds an unregistered infusion recipe with dimensionless instability and essentia-unit costs (not centivis).
     * Copies the central input and component array, but retains output/aspects and component stack references;
     * null input throws IAE, null aspects means no native cost, null components means none.
     * The 1.7.10 Object[] NBT-output form now recognizes only ADVANCEDGOLEM, HELMGOGGLES and the three MASK keys;
     * other patch keys throw IAE. In 1.21.1 use named InfusionResultPatches/data-component changes,
     * not arbitrary legacy NBT payloads (additional array values are not general NBT transforms). */
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
        view.nativeView = true;
        return view;
    }

    private static ItemStack[] samples(List<Ingredient> components) {
        ItemStack[] result = new ItemStack[components.size()];
        for (int index = 0; index < components.size(); index++) result[index] = NativeRecipes.sample(components.get(index));
        return result;
    }

    /** Tests live programmatic fields and virtual getters without consuming supplied stacks.
     * Views of datapack recipes retain native ingredient/patch predicates. Null input/central returns false;
     * null player fails nonempty research gates. Null component entries may throw. */
    public boolean matches(ArrayList<ItemStack> input, ItemStack central, Level world, Player player) {
        if (this.getRecipeInput() == null) return false;
        if (this.research != null && !this.research.isEmpty() && !ThaumcraftApiHelper.isResearchComplete(player, this.research)) return false;
        if (input == null || central == null) return false;
        if (nativeView) return nativeRecipe.matches(new InfusionRecipeInput(central, input), world);
        if (!areItemStacksEqual(central, getRecipeInput(), true)) return false;
        ArrayList<ItemStack> remaining = new ArrayList<>(input.stream().map(ItemStack::copy).toList());
        for (ItemStack required : getComponents()) {
            int match = -1;
            for (int index = 0; index < remaining.size(); index++) {
                if (areItemStacksEqual(remaining.get(index).copy(), required, true)) { match = index; break; }
            }
            if (match < 0) return false;
            remaining.remove(match);
        }
        return remaining.isEmpty() && !assembleOutput(central).isEmpty();
    }

    /**
     * The original stack comparison.
     *
     * <p>Forced 1.21.1 change: the original ore-dictionary shortcut has no 1.21.1 equivalent, so the fuzzy
     * flag is ignored entirely. Use native Ingredient/TagKey matching for 1.21.1 equivalence.</p>
     * <p>Two nulls match, exactly one null does not. Otherwise compares item identity, damage
     * (second-stack damage 32767 is wildcard), and requires the second stack's components in the first;
     * count is ignored and neither stack is changed.</p>
     */
    public static boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, boolean fuzzy) {
        return thaumcraft.common.crafting.ArcaneIngredientMatching.matchesStack(stack1, stack0);
    }

    /** Returns the live nullable output/patch via the input-aware overload; the base implementation makes no copy. */
    public Object getRecipeOutput() { return this.getRecipeOutput(this.getRecipeInput()); }

    /** Returns the live nullable essentia-unit cost list via the input-aware overload; not centivis. */
    public AspectList getAspects() { return this.getAspects(this.getRecipeInput()); }

    /** Returns dimensionless instability via the input-aware overload, without changing state. */
    public int getInstability() { return this.getInstability(this.getRecipeInput()); }

    /** Returns the stored nullable research key without changing state. */
    public String getResearch() { return this.research; }

    /** Returns the live central input used by programmatic matching; native datapack views retain their predicates. */
    public ItemStack getRecipeInput() { return this.recipeInput; }

    /** Returns the live component array and stacks used by programmatic matching; native views retain their predicates. */
    public ItemStack[] getComponents() { return this.components; }

    /** Returns the live nullable output/patch; this base implementation ignores input, including null. */
    public Object getRecipeOutput(ItemStack input) { return this.recipeOutput; }

    /** Returns the live nullable essentia-unit cost list; this base implementation ignores input, including null. */
    public AspectList getAspects(ItemStack input) { return this.aspects; }

    /** Returns stored dimensionless instability; this base implementation ignores input, including null. */
    public int getInstability(ItemStack input) { return this.instability; }

    /** Captures an input-aware virtual result before an infusion session starts. */
    public ItemStack assembleOutput(ItemStack central) {
        Object output = getRecipeOutput(central);
        if (output instanceof ItemStack stack) return stack.copy();
        return output instanceof Object[] ? thaumcraft.common.crafting.InfusionResultPatches.apply(patchName(output), central) : ItemStack.EMPTY;
    }

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
