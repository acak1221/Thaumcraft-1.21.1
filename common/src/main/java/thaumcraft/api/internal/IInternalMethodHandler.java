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
    default void generateVisEffect(ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color) {}

    default boolean isResearchComplete(Player player, String researchKey) { return false; }

    default boolean hasDiscoveredAspect(Player player, Aspect aspect) { return false; }

    default AspectList getDiscoveredAspects(Player player) { return null; }

    default ItemStack getStackInRowAndColumn(Object instance, int row, int column) { return net.minecraft.world.item.ItemStack.EMPTY; }

    default AspectList getObjectAspects(Level level, ItemStack stack) { return null; }

    default AspectList getBonusObjectTags(Level level, ItemStack stack, AspectList bonus) { return null; }

    default AspectList generateTags(Level level, Item item, int metadata) { return null; }

    default boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting) { return false; }

    default boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit) { return false; }

    default boolean consumeVisFromInventory(Player player, AspectList cost) { return false; }

    default void addWarpToPlayer(Player player, int amount, boolean temporary) {}

    default void addStickyWarpToPlayer(Player player, int amount) {}

    default void generateVisEffect(int dim, int x, int y, int z, int x2, int y2, int z2, int color) {}

    default boolean isResearchComplete(String username, String researchkey) { return false; }

    default AspectList getObjectAspects(ItemStack is) { return null; }

    default AspectList getBonusObjectTags(ItemStack is, AspectList ot) { return null; }

    default AspectList generateTags(Item item, int meta) { return null; }

    default boolean hasDiscoveredAspect(String username, Aspect aspect) { return false; }

    default AspectList getDiscoveredAspects(String username) { return null; }

    /** Native 1.21.1 overload with block positions. */
    default void generateVisEffect(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos from,net.minecraft.core.BlockPos to,thaumcraft.api.aspects.Aspect aspect) {}

}
