package thaumcraft.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeSorting;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.RecipeViews;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftRunic;
import thaumcraft.common.config.ThaumcraftWandEnchantments;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.crafting.ProgrammaticRecipes;
import thaumcraft.common.items.wands.WandData;

/**
 * Optional EMI integration: arcane, crucible and infusion recipes and the aspect combinations.
 *
 * <p>TC4 had no recipe viewer of its own; its recipes were only readable in the Thaumonomicon.
 * EMI loads this class itself: on Fabric through the {@code emi} entrypoint of fabric.mod.json,
 * on NeoForge through {@link EmiEntrypoint}. Nothing else references it, so the mod runs
 * unchanged without EMI. Every recipe is read from the recipes the server synchronized, so
 * datapack edits appear as well.
 *
 * <p>The plugin source ({@code compat/emi}) is compiled into both loader modules, not into
 * common: the Architectury development transformer resolves every type a common class names,
 * and EMI's API jars carry a mod descriptor, so they cannot sit on a development classpath
 * without EMI itself. Loader modules are not transformed.</p>
 */
@EmiEntrypoint
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
        // Recipes add-ons registered through ThaumcraftApi live outside the recipe manager.
        guarded("add-on arcane", synthetic(manager, ProgrammaticRecipes.arcaneViews()), holder -> arcane(holder, registries), registry);
        guarded("add-on infusion", synthetic(manager, ProgrammaticRecipes.infusionViews()), holder -> List.of(infusion(holder, registries)), registry);
        guarded("add-on enchantment infusion", synthetic(manager, ProgrammaticRecipes.enchantmentViews()), holder -> enchantment(holder, registries), registry);
        guarded("crucible", synthetic(manager, ProgrammaticRecipes.crucibles(manager)), holder -> List.of(new EmiCrucibleRecipe(holder.id(),
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

    /**
     * EMI's convention for a recipe that is not in the recipe manager (an add-on's ThaumcraftApi registration): its id
     * path starts with '/', so it never collides with a data pack recipe and EMI's dev mode accepts it.
     */
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> List<RecipeHolder<T>> synthetic(RecipeManager manager, List<RecipeHolder<T>> holders) {
        return holders.stream().map(holder -> manager.byKey(holder.id()).isPresent() || holder.id().getPath().startsWith("/") ? holder
                : new RecipeHolder<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(holder.id().getNamespace(), "/" + holder.id().getPath()),
                holder.value())).toList();
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
        return RecipeViews.constructs(recipe, registries).stream().map(view -> new EmiArcaneRecipe(view.id(), view.research(),
                EmiArcaneRecipe.stackGrid(view.grid()), view.result(), view.vis(), false)).toList();
    }

    private static EmiInfusionRecipe infusion(RecipeHolder<InfusionStackRecipe> holder, HolderLookup.Provider registries) {
        return page(RecipeViews.infusion(holder.id(), holder.value(), registries), false);
    }

    private static List<EmiInfusionRecipe> runic(RecipeHolder<InfusionRunicAugmentRecipe> holder) {
        return RecipeViews.runic(holder.id(), holder.value()).stream().map(view -> page(view, false)).toList();
    }

    private static List<EmiInfusionRecipe> enchantment(RecipeHolder<InfusionEnchantmentRecipe> holder, HolderLookup.Provider registries) {
        return RecipeViews.enchantment(holder.id(), holder.value(), registries).map(view -> List.of(page(view, true))).orElse(List.of());
    }

    private static EmiInfusionRecipe page(RecipeViews.Infusion view, boolean enchantment) {
        return new EmiInfusionRecipe(view.id(), view.research(), EmiIngredient.of(view.central()),
                view.components().stream().map(EmiIngredient::of).toList(), view.result(), view.aspects(), view.instability(),
                view.details(), enchantment);
    }
}
