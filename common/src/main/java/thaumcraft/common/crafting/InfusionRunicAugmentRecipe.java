package thaumcraft.common.crafting;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftRunic;
import thaumcraft.common.config.ThaumcraftWearables;
import thaumcraft.common.lib.crafting.InfusionMatching;
import thaumcraft.common.lib.runic.RunicArmor;
import thaumcraft.common.lib.runic.RunicRules;

/** Original dynamic recipe: components/cost/instability depend on this actual centre's FINAL charge. */
public final class InfusionRunicAugmentRecipe implements Recipe<InfusionRecipeInput> {
    public static final MapCodec<InfusionRunicAugmentRecipe> CODEC = MapCodec.unit(InfusionRunicAugmentRecipe::new);
    public static final Serializer SERIALIZER = new Serializer();
    public String research() { return "RUNICAUGMENTATION"; }
    public boolean canApply(ItemStack central) {
        if (central.isEmpty() || central.getCount() != 1 || !(central.getItem() instanceof IRunicArmor)) return false;
        int charge = RunicArmor.finalCharge(central), instability = RunicRules.instability(charge);
        // The common matrix already refuses unsafe original nextInt bounds.
        // Preserve original byte/cost arithmetic, including fc25+ huge positive entries.
        return RunicRules.safeInstability(instability) && (long)Math.max(0, charge) + 2 <= InfusionComponentPredicate.MAX_COMPONENTS;
    }
    public List<Ingredient> components(ItemStack central) {
        int size = RunicRules.componentCount(RunicArmor.finalCharge(central));
        if (size > InfusionComponentPredicate.MAX_COMPONENTS) throw new IllegalArgumentException("Too many original runic components");
        var result = new ArrayList<Ingredient>(size);
        result.add(Ingredient.of(Items.DIAMOND));
        for (int index = 1; index < size; index++) result.add(Ingredient.of(ThaumcraftContent.MATERIALS.get("salis_mundus").get()));
        return List.copyOf(result);
    }
    public List<InfusionComponentPredicate> selectedComponents(ItemStack central) {
        return components(central).stream().map(InfusionComponentPredicate::new).toList();
    }
    public int instability(ItemStack central) { return RunicRules.instability(RunicArmor.finalCharge(central)); }
    public AspectList aspectCost(ItemStack central) {
        AspectList result = new AspectList();
        RunicRules.aspects(RunicArmor.finalCharge(central)).forEach((tag, amount) -> result.add(Aspect.getAspect(tag), amount));
        return result;
    }
    @Override public boolean matches(InfusionRecipeInput offered, Level world) {
        if (!canApply(offered.central())) return false;
        List<Ingredient> required = components(offered.central());
        if (offered.components().size() != required.size()) return false;
        var remaining = new ArrayList<>(offered.components());
        for (Ingredient component : required) {
            int found = -1;
            for (int index = 0; index < remaining.size(); index++) {
                ItemStack stack = remaining.get(index);
                if (stack.getCount() <= stack.getMaxStackSize() && InfusionMatching.matches(component, stack)) { found = index; break; }
            }
            if (found < 0) return false;
            remaining.remove(found);
        }
        return remaining.isEmpty();
    }
    @Override public ItemStack assemble(InfusionRecipeInput offered, HolderLookup.Provider registries) { return RunicArmor.augment(offered.central()); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return RunicArmor.augment(sample(0)); }
    /** Original book rotates robe centres carrying hardening0..4. */
    public ItemStack sample(int hardening) {
        var sample = new ItemStack(ThaumcraftWearables.ROBE_CHESTPLATE.get());
        if (hardening != 0) sample.set(ThaumcraftRunic.HARDENING.get(), (int)(byte)hardening);
        return sample;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public NonNullList<Ingredient> getIngredients() {
        var result = NonNullList.<Ingredient>create(); result.addAll(components(sample(0))); return result;
    }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftRunic.AUGMENT_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftRunic.AUGMENT_TYPE.get(); }
    public static final class Serializer implements RecipeSerializer<InfusionRunicAugmentRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, InfusionRunicAugmentRecipe> STREAM = StreamCodec.unit(new InfusionRunicAugmentRecipe());
        @Override public MapCodec<InfusionRunicAugmentRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, InfusionRunicAugmentRecipe> streamCodec() { return STREAM; }
    }
}
