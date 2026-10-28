/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The recipes of the add-on by the name a research page asks for: the counterpart of
 * {@code ConfigResearch.recipes} of Thaumcraft 4.
 *
 * <p>Arcane, crucible, infusion and infusion-enchantment recipes are registered through
 * {@code thaumcraft.api.ThaumcraftApi} when Thaumcraft is ready and the returned recipe object is kept here.
 * Plain crafting-table recipes are JSON files ({@code data/thaumictinkerer/recipe/<name>.json}); here is only the id
 * the page shows.</p>
 */
public final class TTRecipes {
    private static final Map<String, Object> RECIPES = new LinkedHashMap<>();

    private TTRecipes() {}

    public static void put(String name, Object recipe) {
        if (name == null || name.isEmpty()) return;
        RECIPES.put(name, recipe);
    }

    public static Object get(String name) {
        return RECIPES.get(name);
    }

    /** The recipe registered under {@code name}, which must be of the given type. */
    public static <T> T get(String name, Class<T> type) {
        Object recipe = RECIPES.get(name);
        if (recipe == null) throw new IllegalStateException("No Thaumic Tinkerer recipe named " + name);
        return type.cast(recipe);
    }

    public static boolean has(String name) {
        return RECIPES.containsKey(name);
    }

    /** The id of the data-driven recipe file for {@code name}. */
    public static ResourceLocation dataRecipeId(String name) {
        return LibMisc.id(name);
    }

    /** Test hook: forget everything (the registrations are repeated by the next ready hook). */
    public static void clear() {
        RECIPES.clear();
    }
}
