package thaumcraft.common.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.config.ThaumcraftAlchemy;

/** Initialization registrations survive datapack reloads without putting arbitrary callbacks on the wire. */
public final class ProgrammaticRecipes {
    private static final Map<thaumcraft.api.crafting.CrucibleRecipe, RecipeHolder<CrucibleRecipe>> CRUCIBLES = new IdentityHashMap<>();
    private static final Map<Integer, Integer> NEXT_CRUCIBLE_SUFFIX = new LinkedHashMap<>();
    private ProgrammaticRecipes() {}

    public static List<Object> registrations() { return ThaumcraftApi.getProgrammaticCraftingRecipes(); }

    // The ordered list is asked for per crucible item, per Thaumatorium work tick and per frame of
    // its renderer, so it is kept per manager (client and server have one each) until the recipes of
    // that manager or the registrations change.
    private record Ordered(Object source, List<Object> registrations, List<RecipeHolder<CrucibleRecipe>> crucibles,
                           Map<ResourceLocation, CrucibleRecipe> byId) {}
    private static final Map<RecipeManager, Ordered> ORDERED = new java.util.WeakHashMap<>();

    /** Stock recipes retain ConfigRecipes ordering; addon registrations follow in their original order. */
    public static synchronized List<RecipeHolder<CrucibleRecipe>> crucibles(RecipeManager manager) {
        return ordered(manager).crucibles();
    }

    private static Ordered ordered(RecipeManager manager) {
        // Native byName.values changes identity whenever the recipes are replaced (a reload, a sync).
        Object source = manager.getRecipes();
        List<Object> registrations = registrations();
        Ordered cached = ORDERED.get(manager);
        if (cached != null && cached.source() == source && sameIdentities(registrations, cached.registrations())) return cached;
        List<RecipeHolder<CrucibleRecipe>> crucibles = build(manager, registrations);
        Map<ResourceLocation, CrucibleRecipe> byId = new java.util.HashMap<>();
        for (var holder : crucibles) byId.putIfAbsent(holder.id(), holder.value());
        Ordered created = new Ordered(source, registrations, crucibles, byId);
        ORDERED.put(manager, created);
        return created;
    }

    private static boolean sameIdentities(List<Object> first, List<Object> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) if (first.get(i) != second.get(i)) return false;
        return true;
    }

    private static List<RecipeHolder<CrucibleRecipe>> build(RecipeManager manager, List<Object> registrations) {
        List<RecipeHolder<CrucibleRecipe>> recipes = new ArrayList<>(manager.getAllRecipesFor(ThaumcraftAlchemy.CRUCIBLE_RECIPE_TYPE.get()).stream()
                .sorted(Comparator.<RecipeHolder<CrucibleRecipe>>comparingInt(holder -> holder.value().sourceOrder())
                        .thenComparing(holder -> holder.id().toString())).toList());
        for (Object registration : registrations) {
            if (!(registration instanceof thaumcraft.api.crafting.CrucibleRecipe recipe)) continue;
            RecipeHolder<CrucibleRecipe> holder = CRUCIBLES.get(recipe);
            if (holder == null) {
                int collision = NEXT_CRUCIBLE_SUFFIX.merge(recipe.hash, 1, Integer::sum) - 1;
                // The original hash, not the position among unrelated addon recipes, owns persistent jobs.
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft", "api/crucible/"
                        + Integer.toUnsignedString(recipe.hash, 16) + "_" + collision);
                holder = new RecipeHolder<>(id, CrucibleRecipe.programmatic(recipe));
                CRUCIBLES.put(recipe, holder);
            }
            recipes.add(holder);
        }
        return List.copyOf(recipes);
    }

    public static synchronized void forget(Object recipe) { CRUCIBLES.remove(recipe); ORDERED.clear(); }

    /** The first recipe of the ordered list with this id, as the linear search of the list. */
    public static synchronized CrucibleRecipe crucible(RecipeManager manager, ResourceLocation id) {
        if (id == null) return null;
        return ordered(manager).byId().get(id);
    }

    public record PreparedInfusion(ItemStack output, List<InfusionComponentPredicate> components, AspectList cost,
                                   int instability, ResourceLocation enchantment, int xp) {}

    /** Calls the original object's virtual methods, then freezes a fully serializable ritual plan. */
    public static PreparedInfusion prepareInfusion(Object registration, InfusionRecipeInput offered, ServerLevel level, ServerPlayer actor) {
        ItemStack central = offered.central().copy();
        ArrayList<ItemStack> supplied = new ArrayList<>();
        for (ItemStack stack : offered.components()) supplied.add(stack.copy());
        ItemStack output;
        AspectList cost;
        int instability, xp = 0;
        ResourceLocation enchantment = null;
        if (registration instanceof thaumcraft.api.crafting.InfusionRecipe recipe) {
            if (!recipe.matches(supplied, central, level, actor)) return null;
            output = recipe.assembleOutput(central);
            cost = recipe.getAspects(central);
            instability = recipe.getInstability(central);
            if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) return null;
        } else if (registration instanceof thaumcraft.api.crafting.InfusionEnchantmentRecipe recipe) {
            if (!recipe.matches(supplied, central, level, actor)) return null;
            output = ItemStack.EMPTY;
            enchantment = recipe.enchantmentHolder().unwrapKey().orElseThrow().location();
            instability = recipe.calcInstability(central);
            xp = recipe.calcXP(central);
            cost = new AspectList();
            AspectList base = recipe.getAspects();
            if (base != null) for (Aspect aspect : base.getAspects()) {
                cost.add(aspect, thaumcraft.common.lib.infusion.InfusionEnchantmentRules.aspectCost(base.getAmount(aspect), recipe.getEssentiaMod(central)));
            }
        } else return null;
        if (instability < 0 || instability > 24 || xp < 0 || offered.components().isEmpty()
                || offered.components().size() > InfusionComponentPredicate.MAX_COMPONENTS) return null;
        AspectList frozen = cost == null ? new AspectList() : cost.copy();
        long total = 0;
        for (Aspect aspect : frozen.getAspects()) {
            if (aspect == null || frozen.getAmount(aspect) < 0) return null;
            total += frozen.getAmount(aspect);
        }
        if (total > Integer.MAX_VALUE) return null;
        // Never let an override rewrite the physical source snapshots supplied by the matrix.
        List<InfusionComponentPredicate> components = offered.components().stream().map(InfusionComponentPredicate::snapshot).toList();
        return new PreparedInfusion(output.copy(), components, frozen, instability, enchantment, xp);
    }
}
