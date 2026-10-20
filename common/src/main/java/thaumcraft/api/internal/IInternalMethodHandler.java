package thaumcraft.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The original TC4 plug point that lets the API reach the gameplay internals it must not own.
 *
 * <p>Forced 1.21.1 change: {@code EntityPlayer} became {@link Player} and the integer dimension of
 * {@link #generateVisEffect} became a {@link Level} on the native overload. The eight-integer signature
 * stays so that existing add-ons still compile.</p>
 */
public interface IInternalMethodHandler {
    /** Original dimension-encoded effect request. The native port resolves the level from the running server. */
    void generateVisEffect(int dim, int x, int y, int z, int x2, int y2, int z2, int color);

    boolean isResearchComplete(String username, String researchkey);

    ItemStack getStackInRowAndColumn(Object instance, int row, int column);

    AspectList getObjectAspects(ItemStack is);

    AspectList getBonusObjectTags(ItemStack is, AspectList ot);

    AspectList generateTags(Item item, int meta);

    boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting);

    boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit);

    boolean consumeVisFromInventory(Player player, AspectList cost);

    void addWarpToPlayer(Player player, int amount, boolean temporary);

    void addStickyWarpToPlayer(Player player, int amount);

    boolean hasDiscoveredAspect(String username, Aspect aspect);

    AspectList getDiscoveredAspects(String username);

    /** Native 1.21.1 overload: the dimension is a resource key, not an integer index. */
    default void generateVisEffect(Level level, BlockPos from, BlockPos to, Aspect aspect) {
        generateVisEffect(0, from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), aspect == null ? 0 : aspect.getColor());
    }
}
