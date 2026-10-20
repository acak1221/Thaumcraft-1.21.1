package thaumcraft.api.internal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Original TC4 {@code thaumcraft.api.internal.IInternalMethodHandler}: the internal services the original exposed
 * to its own code and to addons.
 *
 * <p>The native signatures follow the 1.21.1 model: the knowledge of a player lives on the player, not in a table
 * keyed by a name, the vis effect needs the world that carries the dimension and the object tags need the level
 * that owns the dynamic wand and potion tags.
 */
public interface IInternalMethodHandler {
    /** The visual drain between two points of a world. */
    void generateVisEffect(ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color);

    /** Has the player completed the research? */
    boolean isResearchComplete(Player player, String researchKey);

    /** Has the player discovered the aspect? */
    boolean hasDiscoveredAspect(Player player, Aspect aspect);

    /** The aspects the player has discovered. */
    AspectList getDiscoveredAspects(Player player);

    /** The stack of a row and column of a crafting grid. */
    ItemStack getStackInRowAndColumn(Object instance, int row, int column);

    /** The registered object aspects of an item. */
    AspectList getObjectAspects(Level level, ItemStack stack);

    /** The object aspects of an item including its own bonuses. */
    AspectList getBonusObjectTags(Level level, ItemStack stack, AspectList bonus);

    /** The generated object aspects of an item. */
    AspectList generateTags(Level level, Item item, int metadata);

    /** Pay an aspect cost with the vis of a wand. */
    boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting);

    /** Pay an aspect cost with the vis of a wand for a crafting recipe. */
    boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit);

    /** Pay an aspect cost with the vis of the player's inventory. */
    boolean consumeVisFromInventory(Player player, AspectList cost);

    /** Add warp to the player. */
    void addWarpToPlayer(Player player, int amount, boolean temporary);

    /** Add sticky warp to the player. */
    void addStickyWarpToPlayer(Player player, int amount);
}
