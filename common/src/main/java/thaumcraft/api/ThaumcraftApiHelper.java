package thaumcraft.api;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.lib.research.ObjectAspectRegistry;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/**
 * The original TC4 static helper surface.
 *
 * <p>Forced 1.21.1 changes: {@code World}/{@code IBlockAccess}/{@code EntityPlayer}/{@code ItemStack} became
 * {@link Level}/{@code BlockGetter}/{@link Player}/{@code ItemStack}, {@code ForgeDirection} became
 * {@link Direction}, {@code MovingObjectPosition} became {@link BlockHitResult} and the Forge item NBT tag
 * comparison became the native data-component comparison.</p>
 */
public class ThaumcraftApiHelper {
    private static final Map<Integer, AspectList> ALL_ASPECTS = new HashMap<>();
    private static final Map<Integer, AspectList> ALL_COMPOUND_ASPECTS = new HashMap<>();

    private ThaumcraftApiHelper() {}

    /**
     * Original cap of six aspects, dropping the least valuable ones first.
     *
     * <p>Forced 1.21.1 change (documented deviation from the original): the original started the search of
     * the cheapest aspect at {@code Short.MAX_VALUE} and removed the cheapest one until six aspects were
     * left (oracle {@code thaumcraft/api/ThaumcraftApiHelper.java:38-70}). When every weighted amount of
     * the list was at least {@code Short.MAX_VALUE}, the original found no candidate, removed
     * {@code null} - which changes nothing - and looped forever. The native port stops the culling when
     * there is nothing left to drop, so the call always terminates; the result of a normal list is the
     * original one, aspect for aspect.</p>
     */
    public static AspectList cullTags(AspectList temp) {
        AspectList temp2 = new AspectList();
        for (Aspect tag : temp.getAspects()) {
            if (tag == null) continue;
            temp2.add(tag, temp.getAmount(tag));
        }
        while (temp2 != null && temp2.size() > 6) {
            Aspect lowest = null;
            float low = 32767.0F;
            for (Aspect tag : temp2.getAspects()) {
                if (tag == null) continue;
                float ta = temp2.getAmount(tag);
                if (tag.isPrimal()) {
                    ta *= 0.9F;
                } else {
                    if (!tag.getComponents()[0].isPrimal()) {
                        ta *= 1.1F;
                        if (!tag.getComponents()[0].getComponents()[0].isPrimal()) ta *= 1.05F;
                        if (!tag.getComponents()[0].getComponents()[1].isPrimal()) ta *= 1.05F;
                    }
                    if (!tag.getComponents()[1].isPrimal()) {
                        ta *= 1.1F;
                        if (!tag.getComponents()[1].getComponents()[0].isPrimal()) ta *= 1.05F;
                        if (!tag.getComponents()[1].getComponents()[1].isPrimal()) ta *= 1.05F;
                    }
                }
                if (!(ta < low)) continue;
                low = ta;
                lowest = tag;
            }
            // Oracle ThaumcraftApiHelper:68-70 - the original removed the cheapest aspect. When there is
            // no cheapest aspect at all the original removed null and looped forever; the native port
            // stops instead of hanging the server.
            if (lowest == null) break;
            temp2.aspects.remove(lowest);
        }
        return temp2;
    }

    /**
     * The original stack comparison.
     *
     * <p>Original {@code thaumcraft/api/ThaumcraftApiHelper.java:68-74}: two damageable stacks are the
     * same item whatever their wear is, every other pair is the same item with the same damage value. The
     * count of a stack is never part of the comparison.</p>
     *
     * <p>Forced 1.21.1 change: {@code ItemStack.isItemStackDamageable()} became
     * {@link ItemStack#isDamageableItem()}. Null and EMPTY are equivalent; exactly one absent stack
     * returns false, and components are ignored rather than validated.</p>
     */
    public static boolean areItemsEqual(ItemStack s1, ItemStack s2) {
        if (s1 == null || s1.isEmpty()) return s2 == null || s2.isEmpty();
        if (s2 == null || s2.isEmpty()) return false;
        if (s1.isDamageableItem() && s2.isDamageableItem()) return s1.getItem() == s2.getItem();
        return s1.getItem() == s2.getItem() && s1.getDamageValue() == s2.getDamageValue();
    }

    public static boolean isResearchComplete(String username, String researchkey) {
        return ThaumcraftApi.internalMethods.isResearchComplete(username, researchkey);
    }

    /** Native player-scoped form of {@link #isResearchComplete(String, String)}. */
    public static boolean isResearchComplete(Player player, String researchkey) {
        if (player == null) return false;
        return ThaumcraftApi.internalMethods.isResearchComplete(player, researchkey);
    }

    public static boolean hasDiscoveredAspect(String username, Aspect aspect) {
        return ThaumcraftApi.internalMethods.hasDiscoveredAspect(username, aspect);
    }

    public static AspectList getDiscoveredAspects(String username) {
        return ThaumcraftApi.internalMethods.getDiscoveredAspects(username);
    }

    /**
     * The original three by three grid lookup of a crafting grid.
     *
     * <p>Original {@code thaumcraft/api/ThaumcraftApiHelper.java:95-97} and the oracle
     * {@code thaumcraft/common/tiles/TileMagicWorkbench.java} read the cell {@code row + column * 3} of
     * the grid, with the ROW as the first argument: the original recipes call
     * {@code getStackInRowAndColumn(inv, 1, 0)} for the first cap of the sceptre, which is the slot 1 of
     * the grid (oracle {@code thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java:42-46}).</p>
     */
    public static ItemStack getStackInRowAndColumn(Object instance, int row, int column) {
        return ThaumcraftApi.internalMethods.getStackInRowAndColumn(instance, row, column);
    }

    /** Delegates the aspect-unit lookup, replacing a null result with a new empty list; does not convert to centivis.
     * The installed handler returns a copy and treats null/empty input as empty; replacing internalMethods can change ownership/side effects. */
    public static AspectList getObjectAspects(ItemStack is) {
        AspectList result = ThaumcraftApi.internalMethods.getObjectAspects(is);
        return result == null ? new AspectList() : result;
    }

    /** Native world-scoped form: the level-free variant cannot resolve recipe-derived tags. */
    public static AspectList getObjectAspects(ItemStack is, Level level) {
        if (is == null || is.isEmpty() || level == null) return new AspectList();
        return ObjectAspectResolver.scanTags(is, level);
    }

    /** Delegates to the installed handler, normalizing null results to an empty list.
     * The 1.7.10 bonus-enrichment signature now only copies ot in the installed handler (null becomes empty), ignoring is.
     * Use getObjectAspects(ItemStack, Level) for 1.21.1 scan-complete aspect units; no equivalent level-aware bonus facade exists here. */
    public static AspectList getBonusObjectTags(ItemStack is, AspectList ot) {
        AspectList result = ThaumcraftApi.internalMethods.getBonusObjectTags(is, ot);
        return result == null ? new AspectList() : result;
    }

    /** Delegates to the installed handler and normalizes null results to an empty list; meta is native damage points.
     * Unlike 1.7.10 recipe derivation, the installed level-free handler only reads catalogue/registered aspect units (null item becomes empty).
     * Use getObjectAspects(ItemStack, Level) for 1.21.1 scan-complete results; no level-aware generate facade is implemented here. */
    public static AspectList generateTags(Item item, int meta) {
        AspectList result = ThaumcraftApi.internalMethods.generateTags(item, meta);
        return result == null ? new AspectList() : result;
    }

    /** Returns whether any input/target pair matches item/damage; arrays must be non-null, null elements match each other, and neither array is mutated. */
    public static boolean containsMatch(boolean strict, ItemStack[] inputs, ItemStack... targets) {
        for (ItemStack input : inputs) {
            for (ItemStack target : targets) {
                if (!itemMatches(target, input, strict)) continue;
                return true;
            }
        }
        return false;
    }

    /** Rejects either null stack (even two nulls); otherwise tests recipe components as a subset, without item identity validation. */
    public static boolean areItemStackTagsEqualForCrafting(ItemStack slotItem, ItemStack recipeItem) {
        if (recipeItem == null || slotItem == null) return false;
        var recipe = recipeItem.getComponents();
        if (recipe.isEmpty()) return true;
        var slot = slotItem.getComponents();
        for (var type : recipe.keySet()) {
            if (!slot.has(type)) return false;
            if (!java.util.Objects.equals(recipe.get(type), slot.get(type))) return false;
        }
        return true;
    }

    /** Compares item identity/damage without count/components; two nulls match, one null does not, and target damage 32767 is wildcard only when not strict. */
    public static boolean itemMatches(ItemStack target, ItemStack input, boolean strict) {
        if (input == null && target != null || input != null && target == null) return false;
        if (input == null) return true;
        if (target.getItem() != input.getItem()) return false;
        return target.getDamageValue() == ObjectAspectRegistry.WILDCARD && !strict
                || target.getDamageValue() == input.getDamageValue();
    }

    /** Reads the neighboring block coordinates and tests its opposite transport face; null world/face or no connectable transport returns null without explicit mutation. */
    public static BlockEntity getConnectableTile(Level world, int x, int y, int z, Direction face) {
        return getConnectableTile((BlockGetter) world, x, y, z, face);
    }

    /** Reads the neighboring block coordinates and calls isConnectable on its opposite face; null world/face or no matching transport returns null. */
    public static BlockEntity getConnectableTile(BlockGetter world, int x, int y, int z, Direction face) {
        if (world == null || face == null) return null;
        BlockEntity te = world.getBlockEntity(new BlockPos(x + face.getStepX(), y + face.getStepY(), z + face.getStepZ()));
        if (te instanceof IEssentiaTransport transport && transport.isConnectable(face.getOpposite())) return te;
        return null;
    }

    /** Returns a caller-owned mutable copy of the cached aspects for this amount. */
    public static AspectList getAllAspects(int amount) {
        AspectList cached = ALL_ASPECTS.get(amount);
        if (cached != null) return cached.copy();
        AspectList al = new AspectList();
        for (Aspect aspect : Aspect.aspects.values()) al.add(aspect, amount);
        ALL_ASPECTS.put(amount, al);
        return al.copy();
    }

    /** Returns a caller-owned mutable copy of the cached compound aspects for this amount. */
    public static AspectList getAllCompoundAspects(int amount) {
        AspectList cached = ALL_COMPOUND_ASPECTS.get(amount);
        if (cached != null) return cached.copy();
        AspectList al = new AspectList();
        for (Aspect aspect : Aspect.getCompoundAspects()) al.add(aspect, amount);
        ALL_COMPOUND_ASPECTS.put(amount, al);
        return al.copy();
    }

    public static boolean consumeVisFromWand(ItemStack wand, Player player, AspectList cost, boolean doit, boolean crafting) {
        return ThaumcraftApi.internalMethods.consumeVisFromWand(wand, player, cost, doit, crafting);
    }

    public static boolean consumeVisFromWandCrafting(ItemStack wand, Player player, AspectList cost, boolean doit) {
        return ThaumcraftApi.internalMethods.consumeVisFromWandCrafting(wand, player, cost, doit);
    }

    public static boolean consumeVisFromInventory(Player player, AspectList cost) {
        return ThaumcraftApi.internalMethods.consumeVisFromInventory(player, cost);
    }

    public static void addWarpToPlayer(Player player, int amount, boolean temporary) {
        ThaumcraftApi.internalMethods.addWarpToPlayer(player, amount, temporary);
    }

    public static void addStickyWarpToPlayer(Player player, int amount) {
        ThaumcraftApi.internalMethods.addStickyWarpToPlayer(player, amount);
    }

    // ------------------------------------------------------------------ knowledge of a player
    // Not members of the 1.7.10 helper: an add-on of the original reached these through
    // thaumcraft.common.lib.research.ResearchManager or Thaumcraft.proxy.getPlayerKnowledge(). They take the player
    // because 1.21.1 keeps knowledge on the player, and read the same native state as the rest of the mod.

    /** Original ResearchManager.completeResearch: completes the research with its parents and siblings, pays its warp
     * and syncs. Server side only; returns false on a client and for an unknown or disabled key. */
    public static boolean completeResearch(Player player, String researchKey) {
        return ThaumcraftApi.internalMethods.completeResearch(player, researchKey);
    }

    /** The research keys the player completed, as a new set without "@" clue markers. Works on both sides for the
     * client's own player. */
    public static java.util.Set<String> getCompletedResearch(Player player) {
        return ThaumcraftApi.internalMethods.getCompletedResearch(player);
    }

    /** Player-scoped form of {@link #hasDiscoveredAspect(String, Aspect)}. */
    public static boolean hasDiscoveredAspect(Player player, Aspect aspect) {
        return ThaumcraftApi.internalMethods.hasDiscoveredAspect(player, aspect);
    }

    /** Player-scoped form of {@link #getDiscoveredAspects(String)}: a new list, or null for a null player. */
    public static AspectList getDiscoveredAspects(Player player) {
        return ThaumcraftApi.internalMethods.getDiscoveredAspects(player);
    }

    /** Original ResearchManager.completeAspect: adds research points of an aspect to the player's pool and syncs.
     * Server side only. Discovering the aspect itself is a separate step of the original (scanning). */
    public static void addAspectPool(Player player, Aspect aspect, int amount) {
        ThaumcraftApi.internalMethods.addAspectPool(player, aspect, amount);
    }

    /** The research points of an aspect in the player's pool. */
    public static int getAspectPool(Player player, Aspect aspect) {
        return ThaumcraftApi.internalMethods.getAspectPool(player, aspect);
    }

    /** Permanent warp of the player (original PlayerKnowledge.getWarpPerm). */
    public static int getWarpPermanent(Player player) { return ThaumcraftApi.internalMethods.getWarp(player)[0]; }
    /** Sticky warp of the player (original PlayerKnowledge.getWarpSticky). */
    public static int getWarpSticky(Player player) { return ThaumcraftApi.internalMethods.getWarp(player)[1]; }
    /** Temporary warp of the player (original PlayerKnowledge.getWarpTemp). */
    public static int getWarpTemporary(Player player) { return ThaumcraftApi.internalMethods.getWarp(player)[2]; }
    /** Total warp of the player: permanent, sticky and temporary (original PlayerKnowledge.getWarpTotal). */
    public static int getWarpTotal(Player player) {
        int[] warp = ThaumcraftApi.internalMethods.getWarp(player);
        return warp[0] + warp[1] + warp[2];
    }

    /**
     * Original TC4 voxel ray that ignores the cell the ray starts in.
     *
     * <p>Forced 1.21.1 change: the result is a native {@link BlockHitResult}; the traversal itself is the
     * native one, so the original per-face step quirks are replaced by the native block outline.</p>
     * <p>Endpoints are world coordinates in blocks; null world/endpoints or NaN components return null.
     * The 1.7.10 includeFluid/reportMiss switches do not restore their old semantics: traversal tests
     * block shapes only, never fluid shapes, and an unobstructed ray returns null even when reportMiss
     * is true. Use Level.clip with a suitable ClipContext for 1.21.1 fluid/miss semantics.
     * This method reads the world without explicitly mutating it.</p>
     */
    public static BlockHitResult rayTraceIgnoringSource(Level world, Vec3 v1, Vec3 v2, boolean includeFluid, boolean requireCollision, boolean reportMiss) {
        if (world == null || v1 == null || v2 == null) return null;
        if (Double.isNaN(v1.x) || Double.isNaN(v1.y) || Double.isNaN(v1.z)) return null;
        if (Double.isNaN(v2.x) || Double.isNaN(v2.y) || Double.isNaN(v2.z)) return null;
        BlockPos source = BlockPos.containing(v1);
        ClipContext context = new ClipContext(v1, v2,
                requireCollision ? ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE,
                includeFluid ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, CollisionContext.empty());
        BlockHitResult hit = BlockGetter.traverseBlocks(v1, v2, context, (ray, pos) -> {
            if (pos.equals(source)) return null; // The original ignores its initial source cell.
            BlockState state = world.getBlockState(pos);
            return world.clipWithInteractionOverride(v1, v2, pos, ray.getBlockShape(state, world, pos), state);
        }, ray -> null);
        if (hit != null) return hit;
        // The original returned the last visited cell as a "miss" position when bool3 was set; the native
        // clip context has no such result, so an unobstructed ray reports no hit at all.
        return null;
    }
}
