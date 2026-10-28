package thaumcraft.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
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
     * <p>The defaults are inert so that a replacement handler written for 1.7.10 still compiles; the installed
     * ThaumcraftInternalMethods implements every service of this interface except the numeric-dimension
     * effect, which has no 1.21.1 meaning.</p>
 */
public interface IInternalMethodHandler {
    /** The original vis drain effect between two block positions of a server level; color is RGB.
     * Does nothing by default; the installed handler sends the original effect packet. The 1.21.1 form of
     * the numeric-dimension overload below. */
    default void generateVisEffect(ServerLevel level, int x, int y, int z, int x2, int y2, int z2, int color) {}

    /** Player-scoped service. Old addon handlers retain their username override through this default. */
    default boolean isResearchComplete(Player player, String researchKey) {
        return player != null && isResearchComplete(player.getGameProfile().getName(), researchKey);
    }

    /** Player-scoped form of {@link #hasDiscoveredAspect(String, Aspect)}; false by default and for null arguments.
     * The installed handler reads the player's own knowledge, on either side. */
    default boolean hasDiscoveredAspect(Player player, Aspect aspect) { return false; }

    /** Player-scoped form of {@link #getDiscoveredAspects(String)}: a new list of the discovered aspects and their
     * pools; null by default and for a null player. */
    default AspectList getDiscoveredAspects(Player player) { return null; }

    /** Returns EMPTY by default, including null instance; row/column are zero-based 3x3 grid indices, not block coordinates. */
    default ItemStack getStackInRowAndColumn(Object instance, int row, int column) { return net.minecraft.world.item.ItemStack.EMPTY; }

    /** World-scoped base aspects of a stack (registered, recipe-derived and inferred), as a new list; null by default.
     * The installed handler needs a loaded server recipe context for recipe-derived tags. */
    default AspectList getObjectAspects(Level level, ItemStack stack) { return null; }

    /** World-scoped full aspects of a stack (equipment, enchantments, contents) plus {@code bonus}, as a new list;
     * null by default. */
    default AspectList getBonusObjectTags(Level level, ItemStack stack, AspectList bonus) { return null; }

    /** World-scoped recipe-derived aspects of an item, as a new list; null by default. metadata is native damage
     * points, not old subtype metadata. */
    default AspectList generateTags(Level level, Item item, int metadata) { return null; }

    /** Returns false by default without reading nullable inputs or debiting vis; installed handlers define their own cost units/discount policy. */
    default boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting) { return false; }

    /** Returns false by default without reading nullable inputs or changing stacks; installed handlers define cost conversion and payment semantics. */
    default boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit) { return false; }

    /** Returns false by default without reading nullable inputs or changing inventory; installed handlers own payment behavior. */
    default boolean consumeVisFromInventory(Player player, AspectList cost) { return false; }

    // ------------------------------------------------------------------ knowledge services an add-on of the
    // original reached through thaumcraft.common.lib.research.ResearchManager and PlayerKnowledge. Not 1.7.10 members
    // of this interface; inert by default so a replacement handler still compiles.

    /** Original {@code ResearchManager.completeResearch(player, key)}: completes the research, its parents and siblings
     * as the original did, pays its warp and syncs the player. Server side only; false by default, on a client, for an
     * unknown or disabled key. */
    default boolean completeResearch(Player player, String researchKey) { return false; }

    /** The research keys a player completed, as a new set without the "@" clue markers; empty by default and for null. */
    default java.util.Set<String> getCompletedResearch(Player player) { return java.util.Set.of(); }

    /** Original {@code ResearchManager.completeAspect}: adds research points of an aspect to the player's pool (the
     * points the research table spends) and syncs. Server side only; no-op by default. amount is clamped to a short. */
    default void addAspectPool(Player player, Aspect aspect, int amount) {}

    /** The research points of an aspect in the player's pool; zero by default and for null arguments. */
    default int getAspectPool(Player player, Aspect aspect) { return 0; }

    /** The permanent, sticky and temporary warp of a player, in that order, as a new three-element array; zeros by default. */
    default int[] getWarp(Player player) { return new int[3]; }

    /** Whether the player scanned the item of the stack, the way the thaumometer records it; false by default, for a
     * null player and for an empty stack. The installed handler answers from the player's own knowledge, on either side. */
    default boolean hasScannedItem(Player player, ItemStack stack) { return false; }

    /** Marks the aspect as discovered for the player and syncs; true only when this call discovered it. Server side only;
     * false by default, on a client, for a null argument and for an aspect already discovered. */
    default boolean discoverAspect(Player player, Aspect aspect) { return false; }

    /** The scan markers of the player as an immutable snapshot; {@link ThaumcraftApiHelper.Scans#EMPTY} by default and
     * for a null player. */
    default ThaumcraftApiHelper.Scans getScans(Player player) { return ThaumcraftApiHelper.Scans.EMPTY; }

    /** Adds scan markers to the player's knowledge and syncs; the number of markers that were new. Server side only;
     * zero by default, on a client and for null arguments. */
    default int addScans(Player player, ThaumcraftApiHelper.Scans scans) { return 0; }

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

    /** The vis drain effect from a node to its consumer for one aspect; does nothing by default, on a client level and
     * for null arguments. The installed handler forwards to VisNetHandler.generateVisEffect(ServerLevel, BlockPos, BlockPos, Aspect). */
    default void generateVisEffect(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos from,net.minecraft.core.BlockPos to,thaumcraft.api.aspects.Aspect aspect) {}

}
