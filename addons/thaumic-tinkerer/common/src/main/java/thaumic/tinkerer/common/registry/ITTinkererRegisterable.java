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

import thaumic.tinkerer.common.research.IRegisterableResearch;

/**
 * A block, item or other object of the add-on that brings its own research and recipe.
 *
 * <p>Every object created through {@link TTRegistry#item}, {@link TTRegistry#block} (and the other helpers of that
 * class) that implements this interface is asked once, when Thaumcraft is ready: first for its recipe, then for its
 * research, in registration order. 1.7.10 split this over {@code ITTinkererBlock} and {@code ITTinkererItem}; the
 * names, the metadata blocks/items and {@code shouldRegister} of those are gone, because the registration path and
 * the flattened variants are given to {@link TTRegistry} directly.</p>
 */
public interface ITTinkererRegisterable {
    /** The research entry of this object, or null. */
    default IRegisterableResearch getResearchItem() {
        return null;
    }

    /** The recipe(s) of this object, or null. Registered before any research, so pages can look them up by name. */
    default ThaumicTinkererRecipe getRecipeItem() {
        return null;
    }

    /** False keeps the object out of the creative tab. */
    default boolean shouldDisplayInTab() {
        return true;
    }
}
