package thaumcraft.api.internal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The original inert fallback handler: every call succeeds silently or reports "nothing known".
 *
 * <p>The native port installs a real handler in {@code ThaumcraftApi.internalMethods}; this class stays
 * available so that an add-on can restore the inert behaviour deliberately.</p>
 */
public class DummyInternalMethodHandler implements IInternalMethodHandler {
    /** Does nothing; dimension is a legacy numeric ID, endpoints are block coordinates and color is RGB.
     * @deprecated Use VisNetHandler.generateVisEffect with ServerLevel, BlockPos endpoints and Aspect for a real effect. */
    @Deprecated
    @Override public void generateVisEffect(int dim, int x, int y, int z, int x2, int y2, int z2, int color) { }

    /** Returns false even for null inputs, without reading knowledge or changing state. */
    @Override public boolean isResearchComplete(String username, String researchkey) { return false; }

    /** Returns false even for null inputs, without reading knowledge or changing state. */
    @Override public boolean hasDiscoveredAspect(String username, Aspect aspect) { return false; }

    /** Returns null for every username, including null, without state access. */
    @Override public AspectList getDiscoveredAspects(String username) { return null; }

    /** Returns null without reading instance, including null; row/column grid indices are ignored. */
    @Override public ItemStack getStackInRowAndColumn(Object instance, int row, int column) { return null; }

    /** Returns null for every stack, including null, without mutation. */
    @Override public AspectList getObjectAspects(ItemStack is) { return null; }

    /** Returns null without reading or mutating either nullable argument. */
    @Override public AspectList getBonusObjectTags(ItemStack is, AspectList ot) { return null; }

    /** Returns null for every item, including null; native damage-point metadata is ignored. */
    @Override public AspectList generateTags(Item item, int meta) { return null; }

    /** Returns false without debiting centivis or reading nullable arguments/flags. */
    @Override public boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting) { return false; }

    /** Returns false without debiting centivis or reading nullable arguments/flag. */
    @Override public boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit) { return false; }

    /** Returns false without reading nullable arguments or changing inventory/vis. */
    @Override public boolean consumeVisFromInventory(Player player, AspectList cost) { return false; }

    /** Ignores signed warp points, nullable player and temporary flag; sends no packet and changes no state. */
    @Override public void addWarpToPlayer(Player player, int amount, boolean temporary) { }

    /** Ignores signed warp points and nullable player; sends no packet and changes no state. */
    @Override public void addStickyWarpToPlayer(Player player, int amount) { }
}
