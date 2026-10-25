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
     * must adapt 1.7.10 Minecraft types to 1.21.1 types; this is not source or binary compatibility.</p>
     * <p>Native-looking defaults are not automatically implemented services: the installed
     * ThaumcraftInternalMethods inherits several false/null/no-op defaults. Use the explicitly
     * implemented service named below rather than assuming a default has gameplay effects.</p>
 */
public interface IInternalMethodHandler {
    /** Does nothing by default, including for null level; coordinates are blocks and color is RGB.
     * Unlike 1.7.10 numeric dimensions this takes a ServerLevel, but the installed handler still inherits this no-op.
     * @deprecated Use VisNetHandler.generateVisEffect(ServerLevel, BlockPos, BlockPos, Aspect), which sends the actual effect. */
    @Deprecated
    default void generateVisEffect(ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color) {}

    /** Player-scoped service. Old addon handlers retain their username override through this default. */
    default boolean isResearchComplete(Player player, String researchKey) {
        return player != null && isResearchComplete(player.getGameProfile().getName(), researchKey);
    }

    /** Returns false by default, including null arguments, without mutation; the installed handler inherits this inert overload.
     * @deprecated Use thaumcraft.common.lib.InternalMethodHandler.INSTANCE.hasDiscoveredAspect(Player, Aspect). */
    @Deprecated
    default boolean hasDiscoveredAspect(Player player, Aspect aspect) { return false; }

    /** Returns null by default even for a valid player; the installed handler inherits this inert overload.
     * @deprecated Use thaumcraft.common.lib.InternalMethodHandler.INSTANCE.getDiscoveredAspects(Player). */
    @Deprecated
    default AspectList getDiscoveredAspects(Player player) { return null; }

    /** Returns EMPTY by default, including null instance; row/column are zero-based 3x3 grid indices, not block coordinates. */
    default ItemStack getStackInRowAndColumn(Object instance, int row, int column) { return net.minecraft.world.item.ItemStack.EMPTY; }

    /** Returns null by default, including null inputs; the installed handler inherits this inert native-looking overload.
     * @deprecated Use ThaumcraftApiHelper.getObjectAspects(ItemStack, Level) for world-scoped aspect-unit scan results. */
    @Deprecated
    default AspectList getObjectAspects(Level level, ItemStack stack) { return null; }

    /** Returns null by default without changing the nullable arguments; the installed handler inherits this inert overload.
     * @deprecated Use thaumcraft.common.lib.InternalMethodHandler.INSTANCE.getBonusObjectTags(Level, ItemStack, AspectList). */
    @Deprecated
    default AspectList getBonusObjectTags(Level level, ItemStack stack, AspectList bonus) { return null; }

    /** Returns null by default without mutation; metadata is native damage points, not old subtype metadata.
     * @deprecated Use thaumcraft.common.lib.InternalMethodHandler.INSTANCE.generateTags(Level, Item, int). */
    @Deprecated
    default AspectList generateTags(Level level, Item item, int metadata) { return null; }

    /** Returns false by default without reading nullable inputs or debiting vis; installed handlers define their own cost units/discount policy. */
    default boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting) { return false; }

    /** Returns false by default without reading nullable inputs or changing stacks; installed handlers define cost conversion and payment semantics. */
    default boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit) { return false; }

    /** Returns false by default without reading nullable inputs or changing inventory; installed handlers own payment behavior. */
    default boolean consumeVisFromInventory(Player player, AspectList cost) { return false; }

    /** Default no-op, ignoring nullable player, signed warp-point amount and temporary flag; sends no packet. */
    default void addWarpToPlayer(Player player, int amount, boolean temporary) {}

    /** Default no-op, ignoring nullable player and signed warp-point amount; sends no packet. */
    default void addStickyWarpToPlayer(Player player, int amount) {}

    /**
     * Retained numeric-dimension compatibility signature; the installed 1.21.1 handler emits no effect.
     * Legacy Forge dimension numbers cannot identify a native {@link ServerLevel}.
     *
     * @deprecated Inert in this port. Use
     * {@link thaumcraft.api.visnet.VisNetHandler#generateVisEffect(ServerLevel, BlockPos, BlockPos, Aspect)}
     * with the actual server level, endpoint positions and aspect instead.
     */
    @Deprecated
    default void generateVisEffect(int dim, int x, int y, int z, int x2, int y2, int z2, int color) {}

    /** Returns false by default for all nullable username/key pairs, without knowledge access or mutation. */
    default boolean isResearchComplete(String username, String researchkey) { return false; }

    /** Returns null by default for every stack, including null, without mutation; a replacement handler defines ownership. */
    default AspectList getObjectAspects(ItemStack is) { return null; }

    /** Returns null by default without reading or mutating nullable arguments; a replacement handler defines enrichment behavior. */
    default AspectList getBonusObjectTags(ItemStack is, AspectList ot) { return null; }

    /** Returns null by default, ignoring nullable item and native damage-point metadata without changing state. */
    default AspectList generateTags(Item item, int meta) { return null; }

    /** Returns false by default for all nullable arguments, without knowledge access or mutation. */
    default boolean hasDiscoveredAspect(String username, Aspect aspect) { return false; }

    /** Returns null by default for every username, including null; no list is allocated and no state changes. */
    default AspectList getDiscoveredAspects(String username) { return null; }

    /** Does nothing by default, including null arguments; unlike 1.7.10 integer dimensions this takes a Level and block positions but remains inert in the installed handler.
     * @deprecated Use VisNetHandler.generateVisEffect(ServerLevel, BlockPos, BlockPos, Aspect) with the actual server level. */
    @Deprecated
    default void generateVisEffect(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos from,net.minecraft.core.BlockPos to,thaumcraft.api.aspects.Aspect aspect) {}

}
