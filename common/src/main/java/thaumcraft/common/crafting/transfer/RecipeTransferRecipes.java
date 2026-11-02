package thaumcraft.common.crafting.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import thaumcraft.common.crafting.ArcaneIngredientMatching;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.crafting.ProgrammaticRecipes;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.ItemJar;

/** Canonical server identities and workbench matrices; never accepts client ingredients. */
public final class RecipeTransferRecipes {
    private RecipeTransferRecipes() {}

    public static ResourceLocation normalize(ResourceLocation id) {
        if (id != null && id.getPath().startsWith("/")) return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(1));
        return id;
    }

    public static RecipeHolder<?> holder(Level level, ResourceLocation id) {
        if (level == null || id == null) return null;
        ResourceLocation canonical = normalize(id);
        RecipeHolder<?> holder = level.getRecipeManager().byKey(canonical).orElse(null);
        if (holder != null) return holder;
        for (var view : ProgrammaticRecipes.arcaneViews()) if (view.id().equals(canonical)) return view;
        Template construct = construct(level, canonical);
        return construct == null ? null : construct.holder();
    }

    public static Optional<ResourceLocation> canonicalId(Level level, Recipe<?> recipe) {
        if (level == null || recipe == null) return Optional.empty();
        for (var holder : level.getRecipeManager().getRecipes()) if (holder.value() == recipe) return Optional.of(holder.id());
        for (var holder : ProgrammaticRecipes.arcaneViews()) if (sameRecipe(holder.value(), recipe)) return Optional.of(holder.id());
        return Optional.empty();
    }

    public static boolean supported(Level level, Recipe<?> recipe) {
        return canonicalId(level, recipe).map(id -> supported(level, id)).orElse(false);
    }
    public static boolean supported(Level level, ResourceLocation id) {
        // Addon recipe snapshots may not have passed a native JSON codec. Keep an optional
        // client transfer button from breaking the book for a malformed/oversized template.
        try { return resolve(level, id) != null; }
        catch (IllegalArgumentException | IndexOutOfBoundsException unsupportedTemplate) { return false; }
    }

    public static Template resolve(Level level, ResourceLocation id) {
        if (level == null || id == null) return null;
        Template construct = construct(level, normalize(id));
        if (construct != null) return construct;
        RecipeHolder<?> holder = holder(level, id);
        if (holder == null) return null;
        Recipe<?> recipe = holder.value();
        List<Ingredient> matrix = new ArrayList<>(java.util.Collections.nCopies(9, Ingredient.EMPTY));
        if (recipe instanceof ShapedRecipe shaped) shaped(matrix, shaped.getIngredients(), shaped.getWidth(), shaped.getHeight());
        else if (recipe instanceof ShapelessRecipe shapeless) shapeless(matrix, shapeless.getIngredients());
        else if (recipe instanceof ArcaneShapedRecipe shaped) shaped(matrix, shaped.getIngredients(), shaped.pattern().getFirst().length(), shaped.pattern().size());
        else if (recipe instanceof ArcaneShapelessRecipe shapeless) shapeless(matrix, shapeless.getIngredients());
        else return null;
        if (matrix.stream().allMatch(Ingredient::isEmpty)) return null;
        return new Template(holder, List.copyOf(matrix), recipe instanceof NativeArcaneRecipe, List.of());
    }

    /** Same canonical variant ids as the shared EMI/JEI construct catalogue, resolved from server parts. */
    private static Template construct(Level level, ResourceLocation id) {
        if (!id.getNamespace().equals("thaumcraft")) return null;
        boolean sceptre = id.getPath().startsWith("sceptre/");
        if (!sceptre && !id.getPath().startsWith("wand/")) return null;
        for (var holder : level.getRecipeManager().getAllRecipesFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get())) {
            NativeArcaneRecipe recipe = holder.value();
            if (sceptre ? !(recipe instanceof ArcaneSceptreRecipe) : !(recipe instanceof ArcaneWandRecipe)) continue;
            for (var rod : WandMaterials.allRods()) for (var cap : WandMaterials.allCaps()) {
                ResourceLocation variant = ResourceLocation.tryBuild("thaumcraft", (sceptre ? "sceptre/" : "wand/") + rod.tag() + "/" + cap.tag());
                if (!id.equals(variant)) continue;
                try {
                    ArcaneRecipeInput input = sceptre ? ArcaneSceptreRecipe.displayInput(rod.tag(), cap.tag()) : ArcaneWandRecipe.displayInput(rod.tag(), cap.tag());
                    if (!recipe.matches(input, level) || recipe.assemble(input, level.registryAccess()).isEmpty()) return null;
                    List<Ingredient> ingredients = input.items().stream().map(stack -> stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack)).toList();
                    return new Template(new RecipeHolder<>(variant, recipe), ingredients, true, input.items());
                } catch (IllegalArgumentException invalidPart) { return null; }
            }
        }
        return null;
    }

    private static void shaped(List<Ingredient> result, List<Ingredient> ingredients, int width, int height) {
        if (width < 1 || width > 3 || height < 1 || height > 3 || ingredients.size() != width * height) throw new IllegalArgumentException("Unsupported crafting dimensions");
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) result.set(x + y * 3, ingredients.get(x + y * width));
    }
    private static void shapeless(List<Ingredient> result, List<Ingredient> ingredients) {
        if (ingredients.isEmpty() || ingredients.size() > 9) throw new IllegalArgumentException("Unsupported shapeless dimensions");
        for (int i = 0; i < ingredients.size(); i++) result.set(i, ingredients.get(i));
    }

    private static boolean sameRecipe(Recipe<?> first, Recipe<?> second) {
        if (first == second) return true;
        if (first instanceof ArcaneShapedRecipe a && second instanceof ArcaneShapedRecipe b)
            return a.research().equals(b.research()) && a.pattern().equals(b.pattern()) && a.vis().equals(b.vis())
                    && a.mirrored() == b.mirrored() && ItemStack.matches(a.result(), b.result()) && sameIngredients(a.getIngredients(), b.getIngredients());
        if (first instanceof ArcaneShapelessRecipe a && second instanceof ArcaneShapelessRecipe b)
            return a.research().equals(b.research()) && a.vis().equals(b.vis()) && ItemStack.matches(a.result(), b.result())
                    && sameIngredients(a.getIngredients(), b.getIngredients());
        return false;
    }
    private static boolean sameIngredients(List<Ingredient> first, List<Ingredient> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            ItemStack[] a = first.get(i).getItems(), b = second.get(i).getItems();
            if (a.length != b.length) return false;
            for (int j = 0; j < a.length; j++) if (!ItemStack.matches(a[j], b[j])) return false;
        }
        return true;
    }

    private static thaumcraft.api.crafting.IArcaneRecipe original(RecipeHolder<?> holder) {
        String prefix = "api/arcane/";
        if (!holder.id().getNamespace().equals("thaumcraft") || !holder.id().getPath().startsWith(prefix)) return null;
        try {
            int index = Integer.parseInt(holder.id().getPath().substring(prefix.length()));
            List<Object> registrations = ProgrammaticRecipes.registrations();
            if (index < 0 || index >= registrations.size()) return null;
            Object source = registrations.get(index);
            NativeArcaneRecipe view = source instanceof thaumcraft.api.crafting.ShapedArcaneRecipe shaped ? shaped.nativeRecipe()
                    : source instanceof thaumcraft.api.crafting.ShapelessArcaneRecipe shapeless ? shapeless.nativeRecipe() : null;
            // ShapelessArcaneRecipe builds a fresh native view on every call. Match the
            // canonical registration's contents instead of relying on snapshot identity.
            return sameRecipe(view, holder.value()) && source instanceof thaumcraft.api.crafting.IArcaneRecipe recipe ? recipe : null;
        } catch (NumberFormatException unsupportedId) { return null; }
    }

    public record Template(RecipeHolder<?> holder, List<Ingredient> matrix, boolean arcane, List<ItemStack> prototypes) {
        public boolean accepts(int cell, ItemStack offered) {
            Ingredient ingredient = matrix.get(cell);
            if (!ingredient.test(offered)) return false;
            if (!prototypes.isEmpty()) {
                ItemStack required = prototypes.get(cell);
                return holder.value() instanceof ArcaneSceptreRecipe ? ItemStack.isSameItem(required, offered)
                        : ArcaneWandRecipe.checkItemEquals(required, offered);
            }
            if (!arcane || offered.isEmpty()) return true;
            if (offered.getItem() instanceof ItemJar && !ItemJar.isEmptyIngredient(offered)) return false;
            for (ItemStack required : ingredient.getItems()) if (ArcaneIngredientMatching.matchesStack(required, offered)) return true;
            return false;
        }
        public boolean researched(ServerPlayer player, List<ItemStack> offered) {
            ArcaneRecipeInput input = new ArcaneRecipeInput(offered);
            if (holder.value() instanceof NativeArcaneRecipe recipe && !recipe.hasResearch(input, player)) return false;
            // Runtime API subclasses retain their virtual matching/research predicate in ArcaneCrafting.
            // A serializable display view cannot grant transfer authority over that live recipe.
            var registration = original(holder);
            return registration == null || registration.matches(input, player.level(), player);
        }
        public boolean initialResearch(ServerPlayer player, List<ItemStack> current) {
            return !(holder.value() instanceof NativeArcaneRecipe recipe)
                    || recipe.hasResearch(new ArcaneRecipeInput(prototypes.isEmpty() ? current : prototypes), player);
        }
        public boolean matches(Level level, List<ItemStack> offered) {
            if (!prototypes.isEmpty()) for (int cell = 0; cell < 9; cell++) {
                if (prototypes.get(cell).isEmpty() ? !offered.get(cell).isEmpty() : !accepts(cell, offered.get(cell))) return false;
            }
            if (holder.value() instanceof NativeArcaneRecipe recipe) return recipe.matches(new ArcaneRecipeInput(offered), level);
            return holder.value() instanceof CraftingRecipe recipe && recipe.matches(CraftingInput.ofPositioned(3, 3, offered).input(), level);
        }
    }
}
