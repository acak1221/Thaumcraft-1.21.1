package thaumcraft.fabric.compat.emi;

import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeSorting;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftRunic;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.crafting.ProgrammaticRecipes;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.lib.runic.RunicArmor;

/**
 * Optional EMI integration: arcane, crucible and infusion recipes and the aspect combinations.
 *
 * <p>TC4 had no recipe viewer of its own; its recipes were only readable in the Thaumonomicon.
 * EMI loads this class through the {@code emi} entrypoint of fabric.mod.json; nothing else
 * references it, so the mod runs unchanged without EMI. Every recipe is read from the recipes
 * the server synchronized, so datapack edits appear as well.
 *
 * <p>It lives in the Fabric module, not in common: the Architectury development transformer
 * resolves every type a common class names, and EMI's API jars carry a mod descriptor, so they
 * cannot sit on a development classpath without EMI itself. NeoForge needs its own entry point
 * (EMI's {@code @EmiEntrypoint}) when that loader is resumed.</p>
 */
public final class ThaumcraftEmiPlugin implements EmiPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("Thaumcraft/EMI");

    static final EmiRecipeCategory ARCANE = category("arcane", EmiStack.of(ThaumcraftArcane.ARCANE_WORKBENCH_ITEM.get()));
    static final EmiRecipeCategory CRUCIBLE = category("crucible", EmiStack.of(ThaumcraftAlchemy.CRUCIBLE_ITEM.get()));
    static final EmiRecipeCategory INFUSION = category("infusion", EmiStack.of(ThaumcraftInfusion.MATRIX_ITEM.get()));
    static final EmiRecipeCategory INFUSION_ENCHANTMENT = category("infusion_enchantment", EmiStack.of(ThaumcraftInfusion.PEDESTAL_ITEM.get()));
    static final EmiRecipeCategory ASPECT_COMBINATION = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "aspect_combination"), new AspectEmiStack(Aspect.MAGIC, 1),
            new AspectEmiStack(Aspect.MAGIC, 1), EmiRecipeSorting.none());

    private static EmiRecipeCategory category(String path, EmiStack icon) {
        return new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("thaumcraft", path), icon, icon, EmiRecipeSorting.none());
    }

    @Override public void initialize(EmiInitRegistry registry) {
        registry.addIngredientSerializer(AspectEmiStack.class, new AspectEmiStack.Serializer());
    }

    @Override public void register(EmiRegistry registry) {
        registry.addCategory(ARCANE);
        registry.addCategory(CRUCIBLE);
        registry.addCategory(INFUSION);
        registry.addCategory(INFUSION_ENCHANTMENT);
        registry.addCategory(ASPECT_COMBINATION);
        registry.addWorkstation(ARCANE, EmiStack.of(ThaumcraftArcane.ARCANE_WORKBENCH_ITEM.get()));
        registry.addWorkstation(CRUCIBLE, EmiStack.of(ThaumcraftAlchemy.CRUCIBLE_ITEM.get()));
        registry.addWorkstation(INFUSION, EmiStack.of(ThaumcraftInfusion.MATRIX_ITEM.get()));
        registry.addWorkstation(INFUSION_ENCHANTMENT, EmiStack.of(ThaumcraftInfusion.MATRIX_ITEM.get()));
        registry.addWorkstation(ASPECT_COMBINATION, EmiStack.of(ThaumcraftContent.RESEARCH_TABLE_ITEM.get()));
        registry.addRecipeHandler(ThaumcraftArcane.ARCANE_WORKBENCH_MENU.get(), new ArcaneWorkbenchRecipeHandler());
        // A wand is one item whose rod, caps and sceptre flag are components: tell the variants
        // apart, so the recipes of one wand are not buried under every other rod and cap.
        registry.setDefaultComparison(ThaumcraftContent.WAND.get(), Comparison.compareData(ThaumcraftEmiPlugin::wandIdentity));

        for (Aspect aspect : Aspect.aspects.values()) {
            registry.addEmiStack(new AspectEmiStack(aspect, 1));
            if (!aspect.isPrimal() && aspect.getComponents() != null) registry.addRecipe(new EmiAspectCombinationRecipe(aspect));
        }

        RecipeManager manager = registry.getRecipeManager();
        var level = Minecraft.getInstance().level;
        HolderLookup.Provider registries = level == null ? null : level.registryAccess();
        if (registries == null) {
            LOGGER.warn("No client level while EMI reloads; Thaumcraft recipes are not listed");
            return;
        }
        guarded("arcane", manager.getAllRecipesFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get()), holder -> arcane(holder, registries), registry);
        guarded("crucible", ProgrammaticRecipes.crucibles(manager), holder -> List.of(new EmiCrucibleRecipe(holder.id(),
                holder.value().research(), holder.value().catalyst(), holder.value().getResultItem(registries), holder.value().aspects())), registry);
        guarded("infusion", manager.getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get()), holder -> List.of(infusion(holder, registries)), registry);
        guarded("runic augmentation", manager.getAllRecipesFor(ThaumcraftRunic.AUGMENT_TYPE.get()), holder -> runic(holder), registry);
        guarded("enchantment infusion", manager.getAllRecipesFor(ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get()),
                holder -> enchantment(holder, registries), registry);
    }

    private static List<Object> wandIdentity(EmiStack stack) {
        WandData data = stack.getOrDefault(ThaumcraftContent.WAND_DATA.get(), WandData.EMPTY);
        return List.of(data.rod(), data.cap(), stack.getOrDefault(ThaumcraftWandEnchantments.WAND_SCEPTRE.get(), Boolean.FALSE));
    }

    /** One broken datapack or addon recipe must not hide every other Thaumcraft recipe. */
    private static <T> void guarded(String kind, List<T> holders, Function<T, List<? extends dev.emi.emi.api.recipe.EmiRecipe>> convert, EmiRegistry registry) {
        for (T holder : holders) {
            try {
                convert.apply(holder).forEach(registry::addRecipe);
            } catch (RuntimeException broken) {
                LOGGER.warn("Skipping {} recipe {} in EMI", kind, holder instanceof RecipeHolder<?> recipe ? recipe.id() : holder, broken);
            }
        }
    }

    private static List<EmiArcaneRecipe> arcane(RecipeHolder<NativeArcaneRecipe> holder, HolderLookup.Provider registries) {
        NativeArcaneRecipe recipe = holder.value();
        if (recipe instanceof ArcaneShapedRecipe shaped) {
            return List.of(new EmiArcaneRecipe(holder.id(), shaped.research(), EmiArcaneRecipe.shapedGrid(shaped.pattern(), shaped.key()),
                    shaped.getResultItem(registries), shaped.vis(), false));
        }
        if (recipe instanceof ArcaneShapelessRecipe shapeless) {
            return List.of(new EmiArcaneRecipe(holder.id(), shapeless.research(), EmiArcaneRecipe.shapelessGrid(shapeless.ingredients()),
                    shapeless.getResultItem(registries), shapeless.vis(), true));
        }
        // The wand and sceptre recipes take any registered rod and cap: list every combination
        // the registered recipe itself accepts, with the cost it charges for it.
        if (recipe instanceof ArcaneWandRecipe || recipe instanceof ArcaneSceptreRecipe) {
            boolean sceptre = recipe instanceof ArcaneSceptreRecipe;
            List<EmiArcaneRecipe> variants = new ArrayList<>();
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
                    variants.add(new EmiArcaneRecipe(id, research, EmiArcaneRecipe.stackGrid(stacks), result, recipe.vis(input), false));
                }
            }
            return variants;
        }
        return List.of();
    }

    private static EmiInfusionRecipe infusion(RecipeHolder<InfusionStackRecipe> holder, HolderLookup.Provider registries) {
        InfusionStackRecipe recipe = holder.value();
        EmiIngredient central = damaged(recipe.input(), recipe.inputDamage().orElse(null));
        var selected = recipe.selectedComponents();
        List<EmiIngredient> components = new ArrayList<>();
        for (int i = 0; i < recipe.components().size(); i++) {
            var predicate = selected.get(i);
            Integer damage = recipe.componentDamage(i).orElse(null);
            List<EmiStack> choices = new ArrayList<>();
            for (ItemStack choice : recipe.components().get(i).getItems()) {
                ItemStack shown = predicate.displayStack(choice);
                if (shown.isEmpty()) continue;
                if (damage != null) shown.setDamageValue(damage);
                choices.add(EmiStack.of(shown));
            }
            components.add(EmiIngredient.of(choices));
        }
        return new EmiInfusionRecipe(holder.id(), recipe.research(), central, components, recipe.getResultItem(registries),
                recipe.aspects(), recipe.instability(), List.of(), false);
    }

    private static List<EmiInfusionRecipe> runic(RecipeHolder<InfusionRunicAugmentRecipe> holder) {
        InfusionRunicAugmentRecipe recipe = holder.value();
        List<EmiInfusionRecipe> recipes = new ArrayList<>();
        // The book page rotates robes already carrying hardening 0..4, each with its own price.
        for (int hardening = 0; hardening <= 4; hardening++) {
            ItemStack central = recipe.sample(hardening);
            if (!recipe.canApply(central)) continue;
            List<EmiIngredient> components = recipe.components(central).stream().map(EmiIngredient::of).toList();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(),
                    "/" + holder.id().getPath() + "/hardening_" + hardening);
            recipes.add(new EmiInfusionRecipe(id, recipe.research(), EmiStack.of(central), components, RunicArmor.augment(central),
                    costs(recipe.aspectCost(central)), recipe.instability(central), List.of(), false));
        }
        return recipes;
    }

    private static List<EmiInfusionRecipe> enchantment(RecipeHolder<InfusionEnchantmentRecipe> holder, HolderLookup.Provider registries) {
        InfusionEnchantmentRecipe recipe = holder.value();
        var target = recipe.enchantment(registries);
        List<EmiStack> centrals = new ArrayList<>();
        ItemStack sample = ItemStack.EMPTY;
        for (var item : target.value().getSupportedItems()) {
            ItemStack stack = new ItemStack(item);
            if (!InfusionEnchantmentRecipe.canApply(stack, target)) continue;
            if (sample.isEmpty()) sample = stack;
            centrals.add(EmiStack.of(stack));
        }
        if (centrals.isEmpty()) return List.of();
        List<EmiIngredient> components = recipe.components().stream().map(EmiIngredient::of).toList();
        List<Component> details = List.of(Enchantment.getFullname(target, 1),
                Component.translatable("tc.port.book_recipe.xp_levels", recipe.xpLevels(sample, registries)));
        return List.of(new EmiInfusionRecipe(holder.id(), recipe.research(), EmiIngredient.of(centrals), components,
                InfusionEnchantmentRecipe.raise(sample, recipe.enchantment(), registries), costs(recipe.aspectCost(sample, registries)),
                recipe.calculatedInstability(sample), details, true));
    }

    private static EmiIngredient damaged(Ingredient ingredient, Integer damage) {
        if (damage == null) return EmiIngredient.of(ingredient);
        List<EmiStack> stacks = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            ItemStack shown = stack.copy();
            shown.setDamageValue(damage);
            stacks.add(EmiStack.of(shown));
        }
        return EmiIngredient.of(stacks);
    }

    private static Map<String, Integer> costs(AspectList list) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        if (list != null) for (Aspect aspect : list.getAspects()) {
            if (aspect != null && list.getAmount(aspect) > 0) costs.put(aspect.getTag(), list.getAmount(aspect));
        }
        return costs;
    }
}
