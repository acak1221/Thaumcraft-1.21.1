package thaumcraft.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The original TC4 plug point that lets the API reach the gameplay internals it must not own.
 *
 * <p>The original declares one interface with two implementations: the real
 * {@code thaumcraft.common.lib.InternalMethodHandler} and the dummy
 * {@code thaumcraft.api.internal.DummyInternalMethodHandler}. Both are ported; the interface therefore
 * carries the original method set <em>and</em> the 1.21.1 forms of the same services, so an add-on
 * written for the original 1.7.10 signature still compiles.</p>
 */
public interface IInternalMethodHandler {
    void generateVisEffect(ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color);

    boolean isResearchComplete(Player player, String researchKey);

    boolean hasDiscoveredAspect(Player player, Aspect aspect);

    AspectList getDiscoveredAspects(Player player);

    ItemStack getStackInRowAndColumn(Object instance, int row, int column);

    AspectList getObjectAspects(Level level, ItemStack stack);

    AspectList getBonusObjectTags(Level level, ItemStack stack, AspectList bonus);

    AspectList generateTags(Level level, Item item, int metadata);

    boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting);

    boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit);

    boolean consumeVisFromInventory(Player player, AspectList cost);

    void addWarpToPlayer(Player player, int amount, boolean temporary);

    void addStickyWarpToPlayer(Player player, int amount);

    void generateVisEffect(int dim, int x, int y, int z, int x2, int y2, int z2, int color);

    boolean isResearchComplete(String username, String researchkey);

    AspectList getObjectAspects(ItemStack is);

    AspectList getBonusObjectTags(ItemStack is, AspectList ot);

    AspectList generateTags(Item item, int meta);

    boolean hasDiscoveredAspect(String username, Aspect aspect);

    AspectList getDiscoveredAspects(String username);

    /** Original dimension-encoded effect request. The native port resolves the level from the running server. */
    default void generateVisEffect(int dim,int x,int y,int z,int x2,int y2,int z2,int color) {}

    /** Native 1.21.1 overload: the dimension is a level, not an integer index. */
    default void generateVisEffect(net.minecraft.server.level.ServerLevel level,int x,int y,int z,int x2,int y2,int z2,int color) {}

    /** Native 1.21.1 overload with block positions. */
    default void generateVisEffect(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos from,net.minecraft.core.BlockPos to,thaumcraft.api.aspects.Aspect aspect) {}

}
