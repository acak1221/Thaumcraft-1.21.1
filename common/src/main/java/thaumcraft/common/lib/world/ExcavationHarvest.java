package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;

/**
 * Excavation's native paid-attempt boundary. Primary payment belongs to the
 * channel; extra removals own a narrowly scoped reservation (see Reservation).
 * The REAL player's native destroyBlock dispatches loader protection events once.
 * Only its exact permitted invocation receives the direct-spell harvest policy.
 *
 * Modern policies: native loot (including raw ores), XP, onRemove, neighbours and
 * waterlogged fluid restoration; no playerWillDestroy/destroy/playerDestroy
 * callbacks, tool wear, mining stats, exhaustion, piglin anger or break gameevent.
 * Occupied hives/nests are refused, and only this spell's infestation extra-spawn
 * is suppressed. Native loot retains unopened shulker loot/name/lock/container;
 * chest contents come exclusively from onRemove and jars use the captured BE.
 */
public final class ExcavationHarvest {
    private static final ThreadLocal<Attempt> ACTIVE = new ThreadLocal<>();

    private ExcavationHarvest() {}

    /** Called AFTER the focus has paid for a completed attempt, even if refused. */
    public static boolean attempt(ServerPlayer player, BlockPos pos, ItemStack actualHeldWand) {
        Attempt attempt = perform(player, pos, actualHeldWand, null);
        return attempt != null && attempt.removed;
    }

    /** The only expansion entry point. No externally constructible permit or
     * arbitrary skip-ray switch. Bare attempt() retains its original basic API. */
    public static boolean attemptAndEnlarge(ServerPlayer player, BlockPos pos, ItemStack actualHeldWand) {
        Attempt primary = perform(player, pos, actualHeldWand, null);
        if (primary == null || !primary.removed) return false;
        ItemStack focus = FocusData.get(actualHeldWand);
        if (!(focus.getItem() instanceof ItemFocusExcavation) || !ItemStack.matches(actualHeldWand, primary.binding)) return true;
        int extras = FocusUpgradeData.level(focus, FocusUpgradeType.enlarge);
        PrimaryPermit permit = new PrimaryPermit(primary, extras);
        for (int iteration = 0; iteration < extras && permit.valid(); iteration++) {
            if (!FocusVis.canPay(permit.wand, permit.costs)) continue;
            // Fresh original order on EVERY funded iteration, not Direction.values()
            // (whose WEST/EAST order differs). Five world-RNG draws, even no match.
            var directions = new ArrayList<>(List.of(Direction.DOWN, Direction.UP, Direction.NORTH,
                    Direction.SOUTH, Direction.EAST, Direction.WEST));
            for (int size = directions.size(); size > 1; size--) {
                int selected = permit.level.random.nextInt(size);
                Direction previous = directions.set(size - 1, directions.get(selected));
                directions.set(selected, previous);
            }
            for (Direction direction : directions) {
                if (!permit.valid() || !FocusVis.canPay(permit.wand, permit.costs)) break;
                BlockPos neighbour = permit.root.relative(direction);
                if (!permit.level.hasChunkAt(neighbour) || permit.level.getBlockState(neighbour) != permit.state) continue;
                Attempt extra = perform(player, neighbour, actualHeldWand, permit);
                if (extra != null && extra.removed) {
                    // Successful extra settles the already bound reservation;
                    // NEVER debit a current hand that an AFTER callback replaced.
                    if (extra.reservation == null) throw new IllegalStateException("Unreserved Excavation extra removal");
                    permit.binding = extra.reservation.reservedStack;
                    break;
                }
            }
        }
        return true;
    }

    private static Attempt perform(ServerPlayer player, BlockPos pos, ItemStack actualHeldWand, PrimaryPermit permit) {
        // Do not read the world, inventory or mutable scope from a worker thread.
        if (player == null || pos == null || actualHeldWand == null || !player.server.isSameThread()) return null;
        if (ACTIVE.get() != null) return null;
        ACTIVE.remove();
        if (!(player.gameMode instanceof NativeScope)) return null;
        ServerLevel level = player.serverLevel();
        ItemStack beforeAuthorization = actualHeldWand.copy();
        if (!authorized(player, level, pos, actualHeldWand, permit)) return null;
        BlockState snapshot = level.getBlockState(pos);
        BlockEntity tile = level.getBlockEntity(pos);
        if (!harvestable(snapshot, tile, level, pos) || !ItemStack.matches(actualHeldWand, beforeAuthorization)) return null;
        Attempt attempt = new Attempt(player, level, pos.immutable(), actualHeldWand, snapshot, tile, permit);
        ACTIVE.set(attempt);
        try {
            // Never dispatch BREAK/BEFORE/AFTER manually, nor trust this return:
            // native destroyBlock also returns true when removeBlock returned false.
            player.gameMode.destroyBlock(attempt.pos);
            return attempt;
        } finally {
            ACTIVE.remove(); // Also on event, neighbour, loot or native exceptions.
            if (attempt.removed) level.levelEvent(null, 2001, attempt.pos, Block.getId(snapshot));
        }
    }

    private static boolean authorized(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack wand, PrimaryPermit permit) {
        if (!player.server.isSameThread() || player.level() != level || !player.isAlive() || player.isRemoved()
                || player.hasDisconnected() || player.isSpectator() || player.connection == null
                || player.server.getPlayerList().getPlayer(player.getUUID()) != player
                || wand.isEmpty() || wand.getCount() != 1 || !(wand.getItem() instanceof ItemWandCasting)
                || player.getMainHandItem() != wand && player.getOffhandItem() != wand
                || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
                || !level.mayInteract(player, pos)) return false;
        if (permit != null) return permit.player == player && permit.level == level && permit.wand == wand
                && permit.valid() && permit.root.distManhattan(pos) == 1 && level.getBlockState(pos) == permit.state;
        Vec3 eye = player.getEyePosition();
        // LivingEntity.getViewVector uses interpolated HEAD yaw, which may lag
        // the authoritative player rotation. Match the focus/original
        // BlockUtils:237-253 current body yaw+pitch ray, including final checks.
        Vec3 end = eye.add(player.getLookAngle().scale(10));
        if (!finite(eye) || !finite(end) || !level.hasChunksAt(BlockPos.containing(eye), BlockPos.containing(end))) return false;
        var hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    /** A synchronous capability minted ONLY after this exact wand removed the
     * ray-authorized root. Native BlockState identity is the conservative modern
     * mapping of original block+metadata, including modern-only state properties. */
    private static final class PrimaryPermit {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final ItemStack wand;
        private final BlockPos root;
        private final BlockState state;
        private final Vec3 eye, look;
        private final Map<String, Integer> costs;
        private final int extras;
        private ItemStack binding;
        private boolean indeterminate;

        private PrimaryPermit(Attempt primary, int extras) {
            player = primary.player; level = primary.level; wand = primary.wand;
            root = primary.pos; state = primary.snapshot; binding = primary.binding;
            eye = primary.eye; look = primary.look; this.extras = extras;
            costs = Map.of("terra", ItemFocusExcavation.attemptCost(wand, player));
        }
        private boolean valid() {
            if (indeterminate || player.level() != level || !player.isAlive() || player.isRemoved() || player.hasDisconnected()
                    || player.isSpectator() || !player.isUsingItem() || player.getUseItem() != wand
                    || player.getItemInHand(player.getUsedItemHand()) != wand || !ItemStack.matches(wand, binding)
                    || !player.getEyePosition().equals(eye) || !player.getLookAngle().equals(look)
                    || ItemFocusExcavation.attemptCost(wand, player) != costs.get("terra")) return false;
            ItemStack focus = FocusData.get(wand);
            return focus.getItem() instanceof ItemFocusExcavation
                    && FocusUpgradeData.level(focus, FocusUpgradeType.enlarge) == extras;
        }
    }

    /** Authority adaptation to the original post-success extra debit: BREAK and
     * BEFORE see unreserved vis. Immediately before native removal, escrow the
     * price on the ORIGINAL object so onRemove/AFTER cannot spend it or redirect
     * billing to a replacement. Once the native removeBlock delegate is entered,
     * FALSE is NOT a clean refusal: LevelChunk may run onRemove and then return
     * null after a callback replaces the requested state. Both FALSE and throws
     * retain the reservation, log, and revoke this expansion permit. No refund
     * can be inferred from the boolean, current block state or an item snapshot.
     * All protection/state/hand/funding refusals BEFORE that delegate stay free.
     * Removal callbacks may observe escrow early; literal observer-order parity
     * is NOT claimed. Indeterminate removal never gains success/drop privileges
     * and cannot mint vis or reset unrelated state through an unsafe refund. */
    private static final class Reservation {
        private final ItemStack reservedStack;

        private Reservation(ItemStack wand, Map<String, Integer> costs) {
            if (!FocusVis.debit(wand, costs)) throw new IllegalStateException("Unfunded Excavation reservation");
            reservedStack = wand.copy();
        }
    }

    private static boolean finite(Vec3 vec) {
        return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z);
    }

    private static boolean harvestable(BlockState state, BlockEntity tile, ServerLevel level, BlockPos pos) {
        return !state.isAir() && state.getDestroySpeed(level, pos) >= 0
                && (!state.hasBlockEntity() || tile != null && !tile.isRemoved())
                && (!(tile instanceof BeehiveBlockEntity hive) || hive.isEmpty());
    }

    /** True while a paid wand attempt owns the current thread's native destroyBlock. */
    public static boolean hasActiveAttempt() {
        if (ACTIVE.get() != null) return true;
        ACTIVE.remove();
        return false;
    }

    /** Applied by the required game-mode mixin: refuse rather than harvest unscoped. */
    public interface NativeScope {}

    /** Mixin bridge. EVERY native invocation, including same-actor/same-pos nesting,
     * pushes a frame; only the first exact invocation can consume the permit. */
    public static Frame enterNative(ServerPlayerGameMode mode, ServerLevel level, ServerPlayer player, BlockPos pos) {
        Attempt attempt = ACTIVE.get();
        if (attempt == null) { ACTIVE.remove(); return null; }
        boolean first = !attempt.claimed && attempt.frame == null;
        boolean permitted = first && mode == attempt.player.gameMode
                && player == attempt.player && level == attempt.level && pos.equals(attempt.pos);
        if (first) attempt.claimed = true; // A mismatched first call consumes, never lends, the permit.
        Frame frame = new Frame(attempt, attempt.frame, mode, permitted);
        attempt.frame = frame;
        return frame;
    }

    /** Only the CURRENT frame can possess privileges; a nested ordinary call
     * cannot inherit them even when its player, position and state are identical. */
    public static Frame currentNative(ServerPlayerGameMode mode) {
        Attempt attempt = ACTIVE.get();
        if (attempt == null) { ACTIVE.remove(); return null; }
        Frame frame = attempt.frame;
        return frame != null && frame.permitted && frame.mode == mode ? frame : null;
    }

    public static final class Frame implements AutoCloseable {
        private final Attempt attempt;
        private final Frame previous;
        private final ServerPlayerGameMode mode;
        private final boolean permitted;

        private Frame(Attempt attempt, Frame previous, ServerPlayerGameMode mode, boolean permitted) {
            this.attempt = attempt; this.previous = previous; this.mode = mode; this.permitted = permitted;
        }

        public BlockState snapshot() { return attempt.snapshot; }

        public boolean remove(ServerLevel level, BlockPos pos, boolean moving) {
            if (!permitted || attempt.frame != this || attempt.removalTried) return false;
            attempt.removalTried = true;
            if (level != attempt.level || !pos.equals(attempt.pos)
                    || !authorized(attempt.player, level, pos, attempt.wand, attempt.permit)
                    || !ItemStack.matches(attempt.wand, attempt.binding)
                    || level.getBlockState(pos) != attempt.snapshot || level.getBlockEntity(pos) != attempt.tile
                    || !harvestable(attempt.snapshot, attempt.tile, level, pos)
                    || attempt.player.blockActionRestricted(level, pos, mode.getGameModeForPlayer())) return false;
            // Exact native delegate ONCE. Its returned success is the witness,
            // never destroyBlock's optimistic result, nor a subsequent air check.
            // Stateless equal-state non-BE ABA cannot be distinguished without a
            // world mutation witness; BE replacement is always identity-checked.
            if (attempt.permit != null) {
                if (!FocusVis.canPay(attempt.wand, attempt.permit.costs)) return false;
                attempt.reservation = new Reservation(attempt.wand, attempt.permit.costs);
            }
            try {
                attempt.removed = level.removeBlock(pos, moving);
                if (!attempt.removed && attempt.reservation != null) {
                    attempt.permit.indeterminate = true;
                    Thaumcraft.LOG.warn("Excavation extra reservation retained after indeterminate native FALSE at {}; expansion revoked, no success/drop witness or refund", pos);
                }
            }
            catch (RuntimeException | Error failure) {
                // Native may already have removed the block before an onRemove
                // exception. Do not infer success or refund a potentially free extra.
                if (attempt.reservation != null) {
                    attempt.permit.indeterminate = true;
                    Thaumcraft.LOG.warn("Excavation extra reservation retained after exceptional native removal; expansion revoked, no success/drop witness or refund", failure);
                }
                throw failure;
            }
            return attempt.removed;
        }

        public void dropCapturedResources() {
            if (!permitted || attempt.frame != this || !attempt.removed || attempt.dropsStarted) return;
            attempt.dropsStarted = true;
            attempt.dropping = true;
            try {
                // AFTER may already have placed a replacement. Do not re-read,
                // remove, unpack or harvest it; finish only the witnessed OLD BE.
                Block.dropResources(attempt.snapshot, attempt.level, attempt.pos, attempt.tile, attempt.player, attempt.lootTool);
            } finally { attempt.dropping = false; }
        }

        @Override public void close() { attempt.frame = previous; }
    }

    /** InfestedBlock's super/ordinary XP work has already run at this callsite. */
    public static boolean suppressInfestation(InfestedBlock block, BlockState state, ServerLevel level, BlockPos pos, ItemStack tool) {
        Attempt attempt = ACTIVE.get();
        if (attempt == null) { ACTIVE.remove(); return false; }
        return attempt.frame != null && attempt.frame.permitted && attempt.dropping
                && attempt.removed && attempt.snapshot == state && state.getBlock() == block
                && attempt.level == level && attempt.pos.equals(pos) && attempt.lootTool == tool;
    }

    private static final class Attempt {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final BlockPos pos;
        private final ItemStack wand;
        private final BlockState snapshot;
        private final BlockEntity tile;
        private final ItemStack lootTool;
        private final ItemStack binding;
        private final Vec3 eye, look;
        private final PrimaryPermit permit;
        private Reservation reservation;
        private Frame frame;
        private boolean claimed, removalTried, removed, dropsStarted, dropping;

        private Attempt(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack wand, BlockState snapshot, BlockEntity tile, PrimaryPermit permit) {
            this.player = player; this.level = level; this.pos = pos; this.wand = wand; this.snapshot = snapshot; this.tile = tile;
            this.permit = permit; binding = wand.copy(); eye = player.getEyePosition(); look = player.getLookAngle();
            lootTool = wand.copyWithCount(1);
            lootTool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            lootTool.remove(DataComponents.STORED_ENCHANTMENTS);
            ItemStack focus = FocusData.get(wand);
            int fortune = FocusUpgradeData.level(focus, FocusUpgradeType.treasure);
            if (fortune > 0) lootTool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), fortune);
            // Original Forge's default canSilkHarvest explicitly excludes tile entities. In particular, a spawner retains
            // its ordinary XP path rather than receiving native Silk's unconditional XP suppression.
            if (!snapshot.hasBlockEntity() && FocusUpgradeData.level(focus, FocusUpgradeType.silktouch) > 0)
                lootTool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        }
    }
}
