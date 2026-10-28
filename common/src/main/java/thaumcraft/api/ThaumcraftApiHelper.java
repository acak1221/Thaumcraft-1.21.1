package thaumcraft.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.Utils;

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

    /**
     * The complete aspects of one item as a scan sees them, plus {@code bonus} on top: the world-scoped form of the
     * original {@code getBonusObjectTags(ItemStack, AspectList)}. Not a 1.7.10 signature.
     *
     * <p><b>This is not the 1.7.10 contract.</b> The original took the BASE aspects of the item in its second argument
     * and returned them enriched with the bonuses. Here the base is already part of the result, so do not pass
     * {@code getObjectAspects(stack, level)} as {@code bonus}: every amount would be counted twice. Pass {@code null}
     * for the aspects of the stack alone, or only the extra aspects you want added.</p>
     *
     * <p><b>The result</b> is a new list, per single item (the count of the stack is not applied), made of: the
     * registered, recipe-derived or inferred aspects of the item ({@link #getObjectAspects(ItemStack, Level)}), the
     * contents of an essentia container (a filled jar, a phial, a crystal, wisp essence, a bound jar label), and the
     * bonuses of worn equipment and of enchantments, culled to the six most valuable aspects ({@link #cullTags})
     * without capping amounts; then {@code bonus} is added on top by summing, <em>after</em> the culling, so the result
     * can hold more than six aspects. {@code bonus} is never modified.</p>
     *
     * <p>An empty or null stack or a null level contributes nothing: the result is then a copy of {@code bonus} (an
     * empty list for null). A replaced handler that answers null gives an empty list. Either side; the recipe-derived
     * aspects are read from the recipes of {@code level}.</p>
     */
    public static AspectList getBonusObjectTags(ItemStack is, AspectList bonus, Level level) {
        AspectList result = ThaumcraftApi.internalMethods.getBonusObjectTags(level, is, bonus);
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

    /**
     * Original ThaumcraftApiHelper.getAllAspects: every registered aspect, add-on aspects included, with
     * this amount, in registry order. Returns a caller-owned mutable copy of the cached list.
     *
     * <p>Deliberate deviation: the original cached the first answer for an amount forever, so an aspect
     * registered after the first call (any add-on whose initialiser runs later) was missing from every
     * later answer. The cache is rebuilt whenever it no longer holds exactly the registered aspects.</p>
     */
    public static synchronized AspectList getAllAspects(int amount) {
        List<Aspect> registered;
        synchronized (Aspect.aspects) { registered = new ArrayList<>(Aspect.aspects.values()); }
        return cached(ALL_ASPECTS, amount, registered);
    }

    /**
     * Original ThaumcraftApiHelper.getAllCompoundAspects: every registered compound, add-on compounds
     * included, with this amount; a caller-owned copy, rebuilt like {@link #getAllAspects(int)} when the
     * registry or a component array changed.
     */
    public static synchronized AspectList getAllCompoundAspects(int amount) {
        return cached(ALL_COMPOUND_ASPECTS, amount, Aspect.getCompoundAspects());
    }

    private static AspectList cached(Map<Integer, AspectList> cache, int amount, List<Aspect> registered) {
        AspectList cached = cache.get(amount);
        // Aspect has identity equality: the cache is current only for the very same aspects in order.
        if (cached == null || !Arrays.asList(cached.getAspects()).equals(registered)) {
            cached = new AspectList();
            for (Aspect aspect : registered) cached.add(aspect, amount);
            cache.put(amount, cached);
        }
        return cached.copy();
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

    // ------------------------------------------------------------------ what a player scanned
    // Not members of the 1.7.10 helper: an add-on of the original reached the scanned objects through
    // Thaumcraft.proxy.getScannedObjects/getScannedEntities/getScannedPhenomena and ResearchManager.completeScanned*.

    /**
     * Whether the player has scanned the item of the stack with a thaumometer: the answer the thaumometer readout uses to
     * show the aspects of a target that was already scanned. Not a 1.7.10 member (the original compared a hash of the
     * item and its metadata in {@code Thaumcraft.proxy.getScannedObjects}).
     *
     * <p><b>Identity.</b> The item is looked up by its registry id (the marker {@code "@" + id}, as
     * {@code ItemThaumometer} writes it for the item it picked from a block or an item entity); the count, the damage
     * and the data components of the stack do not matter, and there is no metadata in 1.21.1. The marker of an
     * advanced scan ({@code "#" + id}) counts as scanned too. Scanning a dropped item counts for that item and scanning
     * a block for the item a pick-block of it gives (a redstone wire is {@code minecraft:redstone}); scanning a mob
     * counts for no item.</p>
     *
     * <p>False for a null player, a null or empty stack, and an item nobody scanned. Works on both sides for the
     * client's own player (its knowledge is synced); the knowledge of another player is only known to the server.</p>
     */
    public static boolean hasScannedItem(Player player, ItemStack stack) {
        return ThaumcraftApi.internalMethods.hasScannedItem(player, stack);
    }

    /**
     * Marks the aspect as discovered for the player, so the research table and the thaumometer treat it as known, and
     * syncs the player's knowledge to the client. The native counterpart of the original
     * {@code PlayerKnowledge.addDiscoveredAspect}. Not a 1.7.10 helper member.
     *
     * <p>The aspect starts with an empty research pool; an aspect the player already knows keeps its pool. It does
     * <em>not</em> check that the parents of a compound aspect are known (a scan does), shows no discovery notice and
     * gives no research points: it is the quiet "learn this aspect" of the original, for an item that copies knowledge.
     * (A positive {@link #addAspectPool} discovers the aspect too, because in the player's knowledge an aspect is
     * discovered exactly when it has a pool entry.) Only a registered aspect can be discovered.</p>
     *
     * <p>Server side only, on the server thread (another thread throws {@link IllegalStateException}).</p>
     *
     * @return true when this call discovered the aspect; false when the player already knew it, on a client, for a
     *         null player or aspect, and for an aspect that is not registered under its tag
     */
    public static boolean discoverAspect(ServerPlayer player, Aspect aspect) {
        return ThaumcraftApi.internalMethods.discoverAspect(player, aspect);
    }

    /**
     * The scan markers of the player: everything the thaumometer recorded for items (and the blocks they were picked
     * from), entities and phenomena (aura nodes). An immutable snapshot, so a Tome of Sharing can copy what one player
     * scanned to another with {@link #addScans}.
     *
     * <p><b>The markers are the mod's own keys</b> and are meant to travel unchanged, not to be built or parsed: a basic
     * scan is {@code "@"} followed by the registry id of the item or the name of the entity or phenomenon, an advanced
     * scan {@code "#"} followed by the same (it replaces the {@code "@"} marker). A marker is never empty and at most
     * {@link Scans#MAX_KEY_LENGTH} characters long.</p>
     *
     * <p><b>Not included:</b> the research clues a scan revealed ({@code "@KEY"} entries among the completed research;
     * {@link #getCompletedResearch} leaves them out and {@link #completeResearch} refuses them), the discovered aspects
     * ({@link #getDiscoveredAspects(Player)}) and the research points ({@link #getAspectPool}).</p>
     *
     * <p>Both sides for the client's own player; the knowledge of another player is only known to the server. An empty
     * snapshot for a null player.</p>
     */
    public static Scans getScans(Player player) {
        Scans scans = ThaumcraftApi.internalMethods.getScans(player);
        return scans == null ? Scans.EMPTY : scans;
    }

    /**
     * Adds scan markers to the player's knowledge and syncs it to the client: the native counterpart of the original
     * {@code ResearchManager.completeScannedObject/Entity/Phenomena}. Not a 1.7.10 helper member. The markers are those
     * of {@link #getScans}; they give no research points, aspects or clues, only the fact "scanned".
     *
     * <p>A marker the player already has is not added again, in either of its two forms: a basic marker ({@code "@x"})
     * is skipped when the player holds {@code "@x"} or {@code "#x"}; an advanced marker ({@code "#x"}) is added and
     * replaces a basic one. Adding is idempotent, and the order of the markers does not matter.</p>
     *
     * <p>Server side only, on the server thread (another thread throws {@link IllegalStateException}).</p>
     *
     * @return the number of markers that were new; the player is synced only when it is above zero. Zero on a
     *         client and for null arguments
     */
    public static int addScans(ServerPlayer player, Scans scans) {
        return ThaumcraftApi.internalMethods.addScans(player, scans);
    }

    /**
     * An immutable snapshot of the scan markers of a player, as {@link #getScans} returns it and {@link #addScans}
     * takes it. Not a 1.7.10 type. The three sets keep the order of the player's knowledge; each marker is a non-empty
     * string of at most {@value #MAX_KEY_LENGTH} characters (the keys of the mod are far shorter; the bound keeps the knowledge
     * sync packet well inside the limit of an NBT string).
     *
     * @param items      markers of scanned items, for example {@code "@minecraft:diamond"}
     * @param entities   markers of scanned entities
     * @param phenomena  markers of scanned phenomena (aura nodes)
     */
    public record Scans(Set<String> items, Set<String> entities, Set<String> phenomena) {
        /** The longest marker a snapshot accepts; the markers the mod writes are a registry id or a node id, well below it. */
        public static final int MAX_KEY_LENGTH = 1024;
        /** No scan at all. */
        public static final Scans EMPTY = new Scans(Set.of(), Set.of(), Set.of());

        /** Copies the sets (insertion order kept) into unmodifiable ones; null sets, null or empty markers and markers
         * longer than {@link #MAX_KEY_LENGTH} are rejected with a {@link NullPointerException} or an
         * {@link IllegalArgumentException}. */
        public Scans {
            items = copy("items", items);
            entities = copy("entities", entities);
            phenomena = copy("phenomena", phenomena);
        }

        /** True when no marker of any kind is held. */
        public boolean isEmpty() { return items.isEmpty() && entities.isEmpty() && phenomena.isEmpty(); }

        /** The number of markers of all three kinds. */
        public int size() { return items.size() + entities.size() + phenomena.size(); }

        private static Set<String> copy(String name, Set<String> keys) {
            Objects.requireNonNull(keys, name);
            Set<String> copy = new LinkedHashSet<>(Math.max(16, keys.size() * 2));
            for (String key : keys) {
                if (key == null || key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
                    throw new IllegalArgumentException(name + " holds a null, empty or over-long scan marker");
                }
                copy.add(key);
            }
            return Collections.unmodifiableSet(copy);
        }
    }

    // ------------------------------------------------------------------ felling trees
    // Not members of the 1.7.10 helper: an add-on of the original called thaumcraft.common.lib.utils.Utils.isWoodLog and
    // BlockUtils.breakFurthestBlock, the two helpers of the Axe of the Stream.

    /**
     * Whether the block at the position is a log by the rule of the Axe of the Stream: the 1.21.1 form of the original
     * {@code Utils.isWoodLog(world, x, y, z)}. It answers "may the furthest-log walk of {@link #breakFurthestBlock} step
     * on this cell", and nothing else: it says nothing about whether the block belongs to a tree.
     *
     * <p><b>The rule.</b> True for a block in the block tag {@code #minecraft:logs} (so every log, wood and stem of the
     * tag, stripped ones and the Greatwood and Silverwood logs of Thaumcraft included) and for any leaf block; false for
     * everything else, air included, and false for the plain oak log ({@code minecraft:oak_log}). This is the rule of the
     * port's {@code Utils.isWoodLog}. The original asked the block whether it can sustain leaves (a log does) or whether
     * a configured list of "woodLog" ore entries names it; how the port's rule differs for oak and for leaves is listed
     * in {@code docs/audit/KNOWN_DEVIATIONS.md} section 9.</p>
     *
     * <p>Only the block state is read (no block entity, no hardness, no ownership). A position in a chunk that is not
     * loaded, or outside the build height, is not a log; the call never loads a chunk. Either side; read-only; cheap.
     * False for null arguments.</p>
     */
    public static boolean isWoodLog(Level level, BlockPos pos) {
        if (level == null || pos == null || !level.hasChunkAt(pos)) return false;
        return Utils.isWoodLog(level, pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Breaks the log at the far end of a tree, as the original Axe of the Stream did when it felled a tree from the
     * clicked log: the 1.21.1 form of the original {@code BlockUtils.breakFurthestBlock(world, x, y, z, block, player)}.
     * One call breaks ONE block; felling a whole tree is a loop of the caller.
     *
     * <p><b>What "furthest" means.</b> A greedy walk, not a search of the whole tree. It starts at {@code pos} with a
     * distance of zero. At each step it scans the cells within two blocks of the cell it stands on, in the order x from
     * -2 to +2, y from +2 down to -2, z from -2 to +2, and moves to the FIRST cell that (1) holds exactly {@code block}
     * (the same block, not the same tag; the log axis and other state properties are not compared), (2) satisfies
     * {@link #isWoodLog}, (3) can be broken (a destroy speed of zero or more) and (4) is strictly further from
     * {@code pos} than the cell the walk reached before (squared distance, so an equal distance does not count). Then it
     * repeats from there and stops when no cell qualifies. Consequences: gaps of up to two blocks and diagonals are
     * crossed; a branch that is scanned first and ends early hides a longer branch (the walk is the first local
     * maximum, not the farthest log of the tree); a neighbouring tree of the same wood is followed too. The walk never
     * leaves the box |dx| &lt;= 24, |dy| &lt;= 48, |dz| &lt;= 24 around {@code pos}; the first scanned cell outside the box
     * ends the whole walk (an original quirk: the remaining cells of that scan are not looked at).</p>
     *
     * <p><b>What is broken.</b> Only the cell the walk ended on; the logs on the way stay. When no cell qualifies that
     * cell is {@code pos} itself, <em>whatever block it holds</em>: the original never checked the start, because the
     * axe called {@link #isWoodLog} first. Do the same, and pass the block that is at {@code pos}, or a stone will be
     * mined by a call meant for a tree.</p>
     *
     * <p><b>Drops.</b> The block is removed and drops its normal loot, rolled with the main-hand item of
     * {@code player} (Fortune and Silk Touch count), as items at the broken cell; they do not follow the player. A
     * creative player and a null player get no drops. The game rule for block drops applies.</p>
     *
     * <p><b>Tool wear: none.</b> The held item takes no damage, the player gets no statistics or exhaustion, and no
     * loader break event is fired; the player's own Axe of the Stream does not use this helper in the port (it breaks
     * through the normal break path and wears the axe). <b>No protection check:</b> spawn protection, the world border,
     * claims and permission mods are not asked for any cell, and the broken cell can be up to 24 blocks sideways and
     * 48 up or down from {@code pos}. An add-on that cares must check {@code level.mayInteract(player, pos)} or walk
     * the tree itself.</p>
     *
     * <p><b>Afterwards</b> every cell within three blocks of the broken one gets a scheduled block tick 150 to 299 ticks
     * later (the original's delayed neighbourhood update, which the leaves around a felled log answer) and clients see
     * the block update.</p>
     *
     * <p><b>Return value.</b> True when the walk ended on a cell that is not air and can be broken: that cell was handed
     * to the harvester (like the original, true does not prove the block was removed: another mod can veto the removal
     * and the call still answers true). False, with nothing changed, for a client level, a null argument, a start in a
     * chunk that is not loaded, an air start (the helper behind this method answers true for air and plays a break
     * effect), and an unbreakable block such as bedrock at the end of the walk. So {@code while (breakFurthestBlock(...))} ends
     * once the start cell is air.</p>
     *
     * <p><b>Side and thread.</b> Server side, on the server thread; on another thread it throws
     * {@link IllegalStateException}, because the walk keeps its state in static fields (as the original did) and is not
     * reentrant. Cells inside the box that lie in unloaded chunks are read, and so loaded, like any block read.</p>
     *
     * @param level  the level; a client level returns false
     * @param pos    the clicked log, where the walk starts
     * @param block  the block the walk follows, normally {@code level.getBlockState(pos).getBlock()}
     * @param player the player who breaks it; decides the drops; may be null
     */
    public static boolean breakFurthestBlock(Level level, BlockPos pos, Block block, Player player) {
        if (!(level instanceof ServerLevel server) || pos == null || block == null) return false;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("breakFurthestBlock may only be called on the server thread");
        if (!server.hasChunkAt(pos) || server.getBlockState(pos).isAir()) return false;
        return BlockUtils.breakFurthestBlock(server, pos.getX(), pos.getY(), pos.getZ(), block, player);
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
