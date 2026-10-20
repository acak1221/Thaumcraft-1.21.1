package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;

/** One default Use job, one borrowed native actor, and one actual item owner.
 * Not a general item-use engine, permission query, inventory adapter or refund API. */
public final class GolemUsePlatform {
    private static final Set<Block> BUTTONS = Set.of(Blocks.STONE_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON,
            Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.BIRCH_BUTTON, Blocks.JUNGLE_BUTTON,
            Blocks.ACACIA_BUTTON, Blocks.DARK_OAK_BUTTON, Blocks.MANGROVE_BUTTON, Blocks.CHERRY_BUTTON,
            Blocks.BAMBOO_BUTTON, Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON);
    private static final ThreadLocal<Context> BORROW = new ThreadLocal<>();
    private static Bridge bridge;

    private GolemUsePlatform() {}

    public interface Bridge {
        ServerPlayer actor(ServerLevel level);
        boolean isNativeActor(ServerPlayer actor);
    }

    /** Installed only by the required game-mode mixin; absent wiring fails closed. */
    public interface NativeUseFence {}

    public static synchronized void install(Bridge nativeBridge) {
        Objects.requireNonNull(nativeBridge);
        if (bridge != null && bridge != nativeBridge) throw new IllegalStateException("Golem use platform already installed");
        bridge = nativeBridge;
    }

    public static boolean isDetaching(EntityGolemBase worker) {
        Context context = BORROW.get();
        return context != null && context.worker == worker && context.workerActive
                && context.phase == Phase.DETACHING && !context.detached;
    }

    public static boolean isReturning(EntityGolemBase worker) {
        Context context = BORROW.get();
        return context != null && context.worker == worker && context.workerActive
                && context.phase == Phase.RETURNING && context.detached;
    }

    public static boolean isBorrowing(EntityGolemBase worker) {
        Context context = BORROW.get();
        return context != null && context.worker == worker && context.workerActive;
    }

    /** Also fences empty-hand work and actors retained after a failed restoration. */
    public static boolean isActorBorrowed(ServerPlayer actor) {
        Context context = BORROW.get();
        return actor != null && context != null && context.actor == actor;
    }

    /** Only the worker's private busy/exact-marker intent admits this call. */
    public static boolean useBlock(EntityGolemBase worker, GolemMarker marker,
                                   BlockState expectedTarget, BlockState expectedAdjacent) {
        if (worker == null || marker == null || expectedTarget == null || expectedAdjacent == null
                || bridge == null || BORROW.get() != null || !(worker.level() instanceof ServerLevel level)) return false;
        if (!canonical(level) || !EnumGolemType.isSupported(worker.getGolemMaterial())) return false;
        Context context = null;
        try {
            Bridge platform = bridge;
            int material = worker.getGolemMaterial();
            if (!workerReady(worker, level, marker, expectedTarget, expectedAdjacent, material)) return false;
            ServerPlayer actor = platform.actor(level);
            if (!nativeActor(platform, level, actor) || !emptyActor(actor)) return false;
            context = new Context(worker, actor, platform, level, marker, expectedTarget, expectedAdjacent,
                    material, worker.getCarryLimit());
            BORROW.set(context); // Every other borrower must refuse BEFORE pose or hand changes.
            actor.setPos(worker.getX(), worker.getY(), worker.getZ());
            actor.setYRot(worker.getYRot());
            actor.setXRot(worker.getXRot());
            actor.setPose(Pose.STANDING);
            actor.setShiftKeyDown(false);
            if (!prepared(context) || !emptyActor(actor) || !permissions(context, ItemStack.EMPTY)
                    || !sight(context) || !prepared(context) || !emptyActor(actor)) return false;

            // No external call occurs between receiving the actual reference and
            // installing it in its sole spendable native inventory slot.
            context.phase = Phase.DETACHING;
            context.detachStarted = true;
            ItemStack actual = worker.detachUseCargo();
            context.detached = true;
            context.hand = Objects.requireNonNull(actual, "Native Use detach returned null stock");
            if (actual.isEmpty()) context.hand = ItemStack.EMPTY;
            context.inventory.setItem(context.saved.selected, context.hand);
            context.handInstalled = true;
            context.inputCount = context.hand.getCount();
            context.phase = Phase.READY;
            if (!validUse(context)) return false;
            context.phase = Phase.NATIVE_PENDING;
            // Fabric's actual HEAD injection dispatches UseBlockCallback ONCE.
            // Its PASS path then reaches our scoped, fresh native fence.
            InteractionResult result = actor.gameMode.useItemOn(actor, level, context.hand,
                    InteractionHand.MAIN_HAND, context.hit);
            return result.consumesAction();
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem Use at {} refused or interrupted; no click replay or input refund", marker.pos(), failure);
            return false;
        } finally {
            if (context != null) finish(context);
        }
    }

    /** Called AFTER Fabric's effectful HEAD hook, before the first native state read.
     * A successful validation admits this exact native body once, not another job. */
    public static boolean canContinueNativeUse(ServerPlayer actor, Level level, ItemStack stack,
                                               InteractionHand hand, BlockHitResult hit) {
        Context context = BORROW.get();
        if (context == null || context.actor != actor || context.level != level
                || context.phase != Phase.NATIVE_PENDING || !context.detached || !context.handInstalled
                || stack != context.hand || hand != InteractionHand.MAIN_HAND || hit != context.hit) return false;
        context.phase = Phase.NATIVE_VALIDATING; // A preflight cannot recursively admit a second native body.
        try {
            if (hit.getType() != HitResult.Type.BLOCK || hit.isInside() || hit.getDirection() != context.marker.face()
                    || !hit.getBlockPos().equals(context.marker.pos())
                    || !hit.getLocation().equals(Vec3.atCenterOf(context.marker.pos())) || !validUse(context)) {
                context.phase = Phase.NATIVE_REJECTED;
                return false;
            }
            context.phase = Phase.NATIVE_BODY;
            return true;
        } catch (RuntimeException failure) {
            context.phase = Phase.NATIVE_REJECTED;
            Thaumcraft.LOG.warn("Golem Use post-hook validation failed at {}; native body refused", context.marker.pos(), failure);
            return false;
        }
    }

    private static boolean canonical(ServerLevel level) {
        return level != null && level.getServer().isSameThread()
                && level.getServer().getLevel(level.dimension()) == level;
    }

    private static boolean nativeActor(Bridge platform, ServerLevel level, ServerPlayer actor) {
        return canonical(level) && actor != null && actor.level() == level
                && actor.serverLevel() == level && actor.server == level.getServer() && platform.isNativeActor(actor)
                && actor.isAlive() && !actor.isRemoved() && actor.connection != null
                && Double.isFinite(actor.getX()) && Double.isFinite(actor.getY()) && Double.isFinite(actor.getZ())
                && Float.isFinite(actor.getYRot()) && Float.isFinite(actor.getXRot())
                && actor.gameMode.getClass() == ServerPlayerGameMode.class && actor.gameMode instanceof NativeUseFence
                && actor.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && !actor.isSpectator()
                && actor.getUUID().equals(GolemHarvestPlatform.MACHINE_UUID)
                && actor.getGameProfile().getId().equals(GolemHarvestPlatform.MACHINE_UUID)
                && actor.getGameProfile().getName().equals(GolemHarvestPlatform.MACHINE_NAME)
                && level.getEntity(actor.getUUID()) == null && level.getEntity(actor.getId()) == null
                && level.getServer().getPlayerList().getPlayer(actor.getUUID()) == null
                && !level.getServer().getPlayerList().isOp(actor.getGameProfile())
                && level.getServer().getProfilePermissions(actor.getGameProfile()) == 0
                && !actor.hasPermissions(2) && !actor.canUseGameMasterBlocks() && ordinary(actor.getAbilities());
    }

    private static boolean ordinary(Abilities abilities) {
        return abilities.mayBuild && !abilities.instabuild && !abilities.mayfly
                && !abilities.flying && !abilities.invulnerable
                && Float.isFinite(abilities.getFlyingSpeed()) && Float.isFinite(abilities.getWalkingSpeed());
    }

    private static boolean inventoryShape(Inventory inventory) {
        return inventory.getClass() == Inventory.class && inventory.items.size() == 36
                && inventory.armor.size() == 4 && inventory.offhand.size() == 1 && inventory.getContainerSize() == 41;
    }

    private static boolean craftingEmpty(InventoryMenu menu) {
        for (int slot = 0; slot < menu.getCraftSlots().getContainerSize(); slot++) {
            if (!menu.getCraftSlots().getItem(slot).isEmpty()) return false;
        }
        return menu.getSlot(0).getItem().isEmpty(); // A result is a preview, never paid stock.
    }

    private static boolean emptyActor(ServerPlayer actor) {
        Inventory inventory = actor.getInventory();
        if (!inventoryShape(inventory) || !Inventory.isHotbarSlot(inventory.selected)
                || actor.inventoryMenu.getClass() != InventoryMenu.class || actor.containerMenu != actor.inventoryMenu
                || actor.isUsingItem() || !actor.inventoryMenu.getCarried().isEmpty() || !craftingEmpty(actor.inventoryMenu)) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    private static boolean cell(ServerLevel level, BlockPos pos, BlockState expected) {
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getLevel() == level && !expected.hasBlockEntity()
                && chunk.getBlockState(pos) == expected;
    }

    private static boolean nativeSwitch(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.LEVER && block.getClass() == LeverBlock.class
                || block.getClass() == ButtonBlock.class && BUTTONS.contains(block);
    }

    private static boolean nativeTarget(GolemMarker marker, BlockState target, BlockState adjacent) {
        return adjacent.isAir() && (nativeSwitch(target)
                || marker.face() == Direction.UP && target.is(Blocks.FARMLAND) && target.getBlock().getClass() == FarmBlock.class);
    }

    private static boolean workerReady(EntityGolemBase worker, ServerLevel level, GolemMarker marker,
                                       BlockState target, BlockState adjacent, int material) {
        // Reject foreign dimensions BEFORE block access. Home/ghost/busy/current
        // marker authority remains the actual private Entity intent, not this API.
        return canonical(level) && marker.dimension().equals(level.dimension().location())
                && worker.getClass() == EntityGolemBase.class && worker.level() == level
                && level.getEntity(worker.getUUID()) == worker && level.getEntity(worker.getId()) == worker
                && worker.isAlive() && !worker.isRemoved() && worker.getGolemCore() == GolemData.USE
                && worker.getGolemMaterial() == material && EnumGolemType.isSupported(material)
                && worker.getUseOptions() != null && worker.getUseOptions().supportsDefaultBaseline()
                && nativeTarget(marker, target, adjacent) && cell(level, marker.pos(), target)
                && cell(level, marker.pos().relative(marker.face()), adjacent)
                && worker.distanceToSqr(Vec3.atCenterOf(marker.pos())) <= 4
                && worker.canUseMarkedBlock(marker, target, adjacent);
    }

    private static boolean prepared(Context context) {
        ServerPlayer actor = context.actor;
        return workerReady(context.worker, context.level, context.marker, context.target, context.adjacent, context.material)
                && nativeActor(context.platform, context.level, actor) && actor.getId() == context.saved.id
                && actor.connection == context.saved.connection && actor.getInventory() == context.inventory
                && inventoryShape(context.inventory) && context.inventory.selected == context.saved.selected
                && actor.inventoryMenu == context.saved.menu && actor.containerMenu == context.saved.menu
                && actor.gameMode.getPreviousGameModeForPlayer() == context.saved.previousMode
                && context.saved.abilities.matches(actor.getAbilities()) && !actor.isUsingItem()
                && actor.getPose() == Pose.STANDING && !actor.isShiftKeyDown()
                && actor.getX() == context.worker.getX() && actor.getY() == context.worker.getY() && actor.getZ() == context.worker.getZ()
                && actor.getYRot() == context.worker.getYRot() && actor.getXRot() == context.worker.getXRot();
    }

    private static boolean pristineSeeds(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.WHEAT_SEEDS) && stack.getItem().getClass() == ItemNameBlockItem.class
                && ((ItemNameBlockItem) stack.getItem()).getBlock() == Blocks.WHEAT && stack.getComponentsPatch().isEmpty()
                && !stack.has(DataComponents.FOOD) && !stack.has(DataComponents.BLOCK_STATE)
                && !stack.has(DataComponents.BLOCK_ENTITY_DATA) && !stack.has(DataComponents.ENTITY_DATA)
                && !stack.has(DataComponents.BUCKET_ENTITY_DATA) && !stack.has(DataComponents.CONTAINER)
                && !stack.has(DataComponents.BUNDLE_CONTENTS) && !stack.has(DataComponents.CUSTOM_DATA);
    }

    private static boolean actualHand(Context context) {
        Inventory inventory = context.inventory;
        if (!inventoryShape(inventory) || inventory.selected != context.saved.selected
                || context.actor.getMainHandItem() != context.hand || inventory.getItem(inventory.selected) != context.hand
                || context.hand.getCount() != context.inputCount || !context.saved.menu.getCarried().isEmpty()
                || !craftingEmpty(context.saved.menu)) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot != inventory.selected && !inventory.getItem(slot).isEmpty()) return false;
        }
        return context.hand.isEmpty() ? nativeSwitch(context.target)
                : context.marker.face() == Direction.UP && context.target.is(Blocks.FARMLAND)
                    && pristineSeeds(context.hand) && context.hand.getCount() <= context.capacity
                    && context.hand.getCount() <= context.hand.getMaxStackSize();
    }

    private static boolean permissions(Context context, ItemStack hand) {
        BlockPos target = context.marker.pos();
        BlockPos affected = target.relative(context.marker.face());
        ServerPlayer actor = context.actor;
        return context.level.mayInteract(actor, target) && context.level.mayInteract(actor, affected)
                && actor.canInteractWithBlock(target, 0) && actor.canInteractWithBlock(affected, 0)
                && actor.mayUseItemAt(target, context.marker.face(), hand)
                && actor.mayUseItemAt(affected, context.marker.face(), hand);
    }

    private static boolean sight(Context context) {
        Vec3 eyes = context.worker.getEyePosition();
        Vec3 end = Vec3.atCenterOf(context.marker.pos());
        if (!Double.isFinite(eyes.x) || !Double.isFinite(eyes.y) || !Double.isFinite(eyes.z)) return false;
        BlockPos start = BlockPos.containing(eyes);
        // The worker is already <=2 blocks away. Check only the ray's finite
        // chunk footprint, so Level.clip cannot demand an unloaded source chunk.
        int minX = Math.min(start.getX(), context.marker.pos().getX()) >> 4;
        int maxX = Math.max(start.getX(), context.marker.pos().getX()) >> 4;
        int minZ = Math.min(start.getZ(), context.marker.pos().getZ()) >> 4;
        int maxZ = Math.max(start.getZ(), context.marker.pos().getZ()) >> 4;
        if (context.level.isOutsideBuildHeight(start) || !context.level.getWorldBorder().isWithinBounds(start)) return false;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (context.level.getChunkSource().getChunkNow(x, z) == null) return false;
        }
        BlockHitResult hit = context.level.clip(new ClipContext(eyes, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.worker));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(context.marker.pos());
    }

    private static boolean validUse(Context context) {
        if (!prepared(context) || !actualHand(context) || !permissions(context, context.hand) || !sight(context)) return false;
        // Shape/private preflight code may call out. Re-read owners, live intent,
        // actual hand, native target/adjacent state and permissions afterward.
        return prepared(context) && actualHand(context) && permissions(context, context.hand)
                && prepared(context) && actualHand(context)
                && cell(context.level, context.marker.pos(), context.target)
                && cell(context.level, context.marker.pos().relative(context.marker.face()), context.adjacent);
    }

    private static void finish(Context context) {
        try {
            if (context.detachStarted) {
                if (!context.detached || !context.handInstalled) {
                    throw new IllegalStateException("Use detach/hand installation violated its raw ownership contract");
                }
                returnActualStock(context);
            }
            context.phase = Phase.RESTORING;
            boolean clean = restoreActor(context);
            context.workerActive = false; // Actual owners are settled; Entity finishes deferred removal itself.
            if (clean) BORROW.remove();
            else quarantine(context, "Unexpected actor state retained after Use settlement", null);
        } catch (RuntimeException failure) {
            // Do not clear actual actor stock, repeat a raw handoff, emit another
            // copy, or pretend EMPTY when a native preparation invariant fails.
            context.workerActive = context.detachStarted && !context.stockSettled;
            quarantine(context, "Use ownership/restoration failed; actor retained and no automatic retry", failure);
        }
    }

    private static void returnActualStock(Context context) {
        Inventory inventory = context.inventory;
        if (context.returned || !inventoryShape(inventory) || context.actor.getInventory() != inventory
                || context.actor.inventoryMenu != context.saved.menu) throw new IllegalStateException("Use inventory owner changed");
        context.foreignMenu = context.actor.containerMenu == context.saved.menu ? null : context.actor.containerMenu;
        if (context.foreignMenu != null || !craftingEmpty(context.saved.menu) || context.actor.isUsingItem()) {
            // These cannot arise from the closed vanilla unit. Do not turn
            // previews/foreign storage/active-use references into a second owner.
            throw new IllegalStateException("Use actor has uncertain menu/crafting/active-use state; left intact");
        }
        ItemStack[] slots = new ItemStack[inventory.getContainerSize()];
        for (int slot = 0; slot < slots.length; slot++) slots[slot] = Objects.requireNonNull(inventory.getItem(slot));
        ItemStack cursor = Objects.requireNonNull(context.saved.menu.getCarried());
        int selected = Inventory.isHotbarSlot(inventory.selected) ? inventory.selected : -1;
        ItemStack hand = Objects.requireNonNull(context.actor.getMainHandItem());
        if (selected >= 0 && hand != slots[selected] || selected < 0 && !hand.isEmpty()) {
            throw new IllegalStateException("Use post-hand is not the actual native selected slot");
        }
        ArrayList<ItemStack> actual = new ArrayList<>(slots.length + 1);
        IdentityHashMap<ItemStack, Boolean> seen = new IdentityHashMap<>();
        if (selected >= 0) addActual(actual, seen, hand);
        for (int slot = 0; slot < slots.length; slot++) if (slot != selected) addActual(actual, seen, slots[slot]);
        addActual(actual, seen, cursor);
        Returned returned = new Returned();
        context.actualReturn = returned; // Retain any real split if the exact raw return contract fails.
        if (!actual.isEmpty()) {
            ItemStack first = actual.removeFirst();
            int limit = Math.min(context.capacity, first.getMaxStackSize());
            if (limit <= 0) actual.addFirst(first);
            else if (first.getCount() > limit) {
                returned.primary = first.split(limit); // Moves real count; never recreates pre-call input.
                actual.addFirst(first);
            } else returned.primary = first;
        }
        returned.extras = List.copyOf(actual);
        for (int slot = 0; slot < slots.length; slot++) {
            if (inventory.getItem(slot) != slots[slot]) throw new IllegalStateException("Use source slot changed during preparation");
        }
        if (context.saved.menu.getCarried() != cursor) throw new IllegalStateException("Use cursor changed during preparation");
        if (context.actor.containerMenu != context.saved.menu || !craftingEmpty(context.saved.menu) || context.actor.isUsingItem()) {
            throw new IllegalStateException("Use actor acquired uncertain state during return preparation");
        }
        context.phase = Phase.RETURNING;
        context.worker.restoreUseCargo(returned.primary, returned.extras);
        context.returned = true;
        // No sync, callback, logging or menu notification between installing the
        // actual Entity owners and these native raw slot/cursor removals.
        for (int slot = 0; slot < slots.length; slot++) {
            if (inventory.getItem(slot) != slots[slot]) throw new IllegalStateException("Raw Use return changed a source slot");
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        if (context.saved.menu.getCarried() != cursor) throw new IllegalStateException("Raw Use return changed the cursor");
        context.saved.menu.setCarried(ItemStack.EMPTY);
        context.stockSettled = true;
        context.actualReturn = null;
        context.hand = null; // No pre-call reference is returned a second time.
    }

    private static void addActual(List<ItemStack> actual, IdentityHashMap<ItemStack, Boolean> seen, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (seen.put(stack, Boolean.TRUE) != null) throw new IllegalStateException("Use inventory contains aliased stock pointers");
        actual.add(stack);
    }

    private static boolean restoreActor(Context context) {
        ServerPlayer actor = context.actor;
        Saved saved = context.saved;
        // Do not send abilities/game-mode updates through a replaced connection,
        // move an actor across levels, restore OPs, or overwrite uncertain gear.
        if (!canonical(context.level) || actor.level() != context.level || !context.platform.isNativeActor(actor)
                || actor.connection != saved.connection || actor.getId() != saved.id) return false;
        if (actor.containerMenu != saved.menu) {
            context.contaminated = true;
            context.foreignMenu = actor.containerMenu; // Retain it without removed/close/drop callbacks.
            return false;
        }
        actor.setPos(saved.x, saved.y, saved.z);
        actor.setPose(saved.pose);
        actor.setYRot(saved.yRot);
        actor.setXRot(saved.xRot);
        actor.setShiftKeyDown(saved.shift);
        context.inventory.selected = saved.selected;
        if (actor.containerMenu != saved.menu) {
            context.contaminated = true;
            context.foreignMenu = actor.containerMenu;
            return false;
        }
        actor.containerMenu = saved.menu;
        if (actor.gameMode.getGameModeForPlayer() != saved.mode) actor.gameMode.changeGameModeForPlayer(saved.mode);
        saved.abilities.restore(actor.getAbilities());
        return !context.contaminated && nativeActor(context.platform, context.level, actor) && emptyActor(actor)
                && actor.gameMode.getGameModeForPlayer() == saved.mode
                && actor.gameMode.getPreviousGameModeForPlayer() == saved.previousMode
                && saved.abilities.matches(actor.getAbilities()) && actor.containerMenu == saved.menu
                && actor.getX() == saved.x && actor.getY() == saved.y && actor.getZ() == saved.z
                && actor.getPose() == saved.pose && actor.getYRot() == saved.yRot && actor.getXRot() == saved.xRot
                && actor.isShiftKeyDown() == saved.shift && context.inventory.selected == saved.selected;
    }

    private static void quarantine(Context context, String reason, RuntimeException failure) {
        context.phase = Phase.QUARANTINED;
        // Intentionally retain this single context: shared Harvest refuses the
        // actual actor too. There is no automatic reset/cleanup or click replay.
        Thaumcraft.LOG.warn("{} (worker {}, target {}); actual actor remains unavailable", reason,
                context.worker.getUUID(), context.marker.pos(), failure);
    }

    private enum Phase { PREPARING, DETACHING, READY, NATIVE_PENDING, NATIVE_VALIDATING, NATIVE_BODY, NATIVE_REJECTED, RETURNING, RESTORING, QUARANTINED }

    private static final class Returned {
        ItemStack primary = ItemStack.EMPTY;
        List<ItemStack> extras = List.of();
    }

    private record SavedAbilities(boolean invulnerable, boolean flying, boolean mayfly, boolean instabuild,
                                  boolean mayBuild, float flyingSpeed, float walkingSpeed) {
        SavedAbilities(Abilities abilities) {
            this(abilities.invulnerable, abilities.flying, abilities.mayfly, abilities.instabuild,
                    abilities.mayBuild, abilities.getFlyingSpeed(), abilities.getWalkingSpeed());
        }
        boolean matches(Abilities abilities) {
            return invulnerable == abilities.invulnerable && flying == abilities.flying && mayfly == abilities.mayfly
                    && instabuild == abilities.instabuild && mayBuild == abilities.mayBuild
                    && flyingSpeed == abilities.getFlyingSpeed() && walkingSpeed == abilities.getWalkingSpeed();
        }
        void restore(Abilities abilities) {
            abilities.invulnerable = invulnerable; abilities.flying = flying; abilities.mayfly = mayfly;
            abilities.instabuild = instabuild; abilities.mayBuild = mayBuild;
            abilities.setFlyingSpeed(flyingSpeed); abilities.setWalkingSpeed(walkingSpeed);
        }
    }

    private record Saved(double x, double y, double z, float yRot, float xRot, Pose pose, boolean shift, int selected,
                         GameType mode, GameType previousMode, InventoryMenu menu, int id,
                         ServerGamePacketListenerImpl connection, SavedAbilities abilities) {
        Saved(ServerPlayer actor) {
            this(actor.getX(), actor.getY(), actor.getZ(), actor.getYRot(), actor.getXRot(), actor.getPose(),
                    actor.isShiftKeyDown(), actor.getInventory().selected, actor.gameMode.getGameModeForPlayer(),
                    actor.gameMode.getPreviousGameModeForPlayer(), actor.inventoryMenu, actor.getId(), actor.connection,
                    new SavedAbilities(actor.getAbilities()));
        }
    }

    private static final class Context {
        final EntityGolemBase worker;
        final ServerPlayer actor;
        final Bridge platform;
        final ServerLevel level;
        final GolemMarker marker;
        final BlockState target, adjacent;
        final int material, capacity;
        final Inventory inventory;
        final Saved saved;
        final BlockHitResult hit;
        Phase phase = Phase.PREPARING;
        boolean workerActive = true, detachStarted, detached, handInstalled, returned, stockSettled, contaminated;
        ItemStack hand;
        int inputCount;
        Returned actualReturn;
        AbstractContainerMenu foreignMenu;

        Context(EntityGolemBase worker, ServerPlayer actor, Bridge platform, ServerLevel level, GolemMarker marker,
                BlockState target, BlockState adjacent, int material, int capacity) {
            this.worker = worker; this.actor = actor; this.platform = platform; this.level = level;
            this.marker = marker; this.target = target; this.adjacent = adjacent;
            this.material = material; this.capacity = capacity; this.inventory = actor.getInventory();
            this.saved = new Saved(actor);
            this.hit = new BlockHitResult(Vec3.atCenterOf(marker.pos()), marker.face(), marker.pos(), false);
        }
    }
}
