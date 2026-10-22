package thaumcraft.common.entities.golems;

import dev.architectury.event.events.common.BlockEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemFluidCargo;
import thaumcraft.common.lib.world.TreeLogTraversal;

/** Loader protection callbacks around finite crop/water work.
 * No player inventory, copied loot, crop-provider registry or Bore authorization. */
public final class GolemHarvestPlatform {
    public static final UUID MACHINE_UUID = UUID.fromString("c3d3eca4-c515-4898-a7ae-2d394d1edab0");
    public static final String MACHINE_NAME = "FakeThaumcraftGolem";
    private static Bridge bridge;

    private GolemHarvestPlatform() {}
    public interface Bridge {
        ServerPlayer actor(ServerLevel level);
        boolean isNativeActor(ServerPlayer actor);
        /**
         * The shared break protection notification of the machine actor.
         *
         * <p>The shared {@code BlockEvent.BREAK} of Architectury is the notification every loader
         * owes to the other mods, and it must be delivered EXACTLY once for one break: the
         * Fabric loader has no native counterpart of it, so the loader delivers it here; the
         * NeoForge loader's native cancellable break event already delivers it, so its
         * implementation answers without a second notification. Two notifications would let a
         * listener of the shared event observe two breaks for one removal.</p>
         *
         * @return whether the break may continue.
         */
        boolean sharedBreak(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state);
        boolean before(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state);
        void canceled(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state);
        void after(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state);
        /**
         * The loader's right-click-on-block consultation for a replanting placement (the actor holds the one
         * seed in its main hand). True only when no listener objected or took the interaction over.
         */
        boolean allowsPlacement(ServerLevel level, ServerPlayer actor, BlockHitResult hit);
    }
    public static synchronized void install(Bridge nativeBridge) {
        Objects.requireNonNull(nativeBridge);
        if (bridge != null && bridge != nativeBridge) throw new IllegalStateException("Golem harvest platform already installed");
        bridge = nativeBridge;
    }

    /** Non-breaking pond/home permission query. It does not fire break/use
     * callbacks, mutate water or borrow the owner's player permissions. */
    static boolean mayInteract(EntityGolemBase golem, BlockPos target) {
        if (golem == null || !(golem.level() instanceof ServerLevel level) || bridge == null || target == null) return false;
        BlockPos pos = target.immutable();
        if (!queryEligible(level, golem, pos)) return false;
        Bridge platform = bridge;
        try {
            ServerPlayer actor = platform.actor(level);
            if (!bound(platform, level, actor) || level.getEntity(actor.getUUID()) != null) return false;
            actor.setPos(golem.getX(), golem.getY(), golem.getZ());
            actor.setYRot(golem.getYRot());
            actor.setXRot(golem.getXRot());
            return queryEligible(level, golem, pos) && bound(platform, level, actor)
                    && actor.distanceToSqr(golem) == 0 && level.mayInteract(actor, pos)
                    && queryEligible(level, golem, pos) && bound(platform, level, actor);
        } catch (RuntimeException failure) {
            // A refused or unknown actor never authorizes a pond. This is not a
            // universal adapter for addon-specific fishing/claim event systems.
            return false;
        }
    }

    private static boolean queryEligible(ServerLevel level, EntityGolemBase golem, BlockPos pos) {
        return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level
                && golem.level() == level && level.getEntity(golem.getUUID()) == golem && golem.canWork()
                && (golem.getGolemCore() == GolemData.FISHING || golem.getGolemCore() == GolemData.LIQUID
                    || golem.getGolemCore() == GolemData.ALCHEMY || golem.getGolemCore() == GolemData.USE
                    || golem.getGolemCore() == GolemData.BUTCHER) && golem.isAlive()
                && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }

    /** Only the exact native WATER-source -> AIR pair has a callback-free
     * lifecycle body inside LevelChunk.setBlockState. Never generalize to BEs,
     * arbitrary blocks, flowing fluids or third-party fluid providers. */
    static boolean collectSourceFluid(EntityGolemBase golem, BlockPos target, BlockState expected, net.minecraft.resources.ResourceLocation fluid) {
        if (golem == null || !(golem.level() instanceof ServerLevel level) || bridge == null || target == null) return false;
        BlockPos pos = target.immutable();
        LevelChunk chunk = loadedWaterChunk(level, pos);
        if (!waterEligible(level, golem, chunk, pos, expected, fluid)) return false;
        Bridge platform = bridge;
        boolean committed = false;
        try {
            ServerPlayer actor = platform.actor(level);
            if (!bound(platform, level, actor) || level.getEntity(actor.getUUID()) != null) return false;
            actor.setPos(golem.getX(), golem.getY(), golem.getZ());
            actor.setYRot(golem.getYRot());
            actor.setXRot(golem.getXRot());
            if (!waterEligible(level, golem, chunk, pos, expected, fluid) || !level.mayInteract(actor, pos)) return false;
            if (!platform.sharedBreak(level, actor, pos, expected)) {
                platform.canceled(level, actor, pos, expected);
                return false;
            }
            if (!waterEligible(level, golem, chunk, pos, expected, fluid) || !bound(platform, level, actor)
                    || actor.distanceToSqr(golem) != 0 || !level.mayInteract(actor, pos)) return false;
            if (!platform.before(level, actor, pos, expected)) {
                platform.canceled(level, actor, pos, expected);
                return false;
            }
            if (!waterEligible(level, golem, chunk, pos, expected, fluid) || !bound(platform, level, actor)
                    || actor.distanceToSqr(golem) != 0 || !level.mayInteract(actor, pos)) {
                platform.canceled(level, actor, pos, expected);
                return false;
            }
            int oldAmount = golem.getFluidAmountMb();
            new GolemFluidCargo(fluid, oldAmount + GolemFluidCargo.BUCKET)
                    .validate(golem.getGolemMaterial(), golem.getUpgradeAmount(GolemUpgrade.EARTH));
            BlockState air = Blocks.AIR.defaultBlockState();
            // The native chunk API owns palette counts, heightmaps, skylight,
            // light tasks and dirty state. WATER.onRemove and AIR.onPlace have
            // no native external callbacks. Client/neighbor notifications follow
            // ONLY after the worker's raw credit, unlike Level.setBlock.
            BlockState removed = chunk.setBlockState(pos, air, false);
            if (removed != expected || loadedWaterChunk(level, pos) != chunk || chunk.getBlockState(pos) != air) {
                Thaumcraft.LOG.warn("Water drain at {} changed outside its finite native contract; no cargo grant or restoration", pos);
                return false;
            }
            if (!golem.creditCollectedSourceFluid(pos, oldAmount)) {
                // No external notification has run. Restore only our unchanged
                // AIR, never a replacement/refilled source or committed stock.
                if (golem.getFluidAmountMb() == oldAmount && loadedWaterChunk(level, pos) == chunk
                        && chunk.getBlockState(pos) == air && chunk.setBlockState(pos, expected, false) == air) {
                    notifyWaterChange(level, chunk, pos, air, expected);
                }
                return false;
            }
            committed = true;
            try { notifyWaterChange(level, chunk, pos, expected, air); }
            finally {
                // AFTER observes the actual captured old source, not air. It
                // cannot refund or extract this source a second time.
                if (bound(platform, level, actor)) platform.after(level, actor, pos, expected);
            }
            return true;
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem water drain {} at {}; no repeated grant or post-commit rollback",
                    committed ? "committed before feedback failed" : "refused or interrupted", pos, failure);
            return committed;
        }
    }

    private static LevelChunk loadedWaterChunk(ServerLevel level, BlockPos pos) {
        if (!level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level
                || level.isDebug() || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) return null;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getLevel() == level && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) ? chunk : null;
    }

    private static boolean waterEligible(ServerLevel level, EntityGolemBase golem, LevelChunk chunk, BlockPos pos, BlockState expected, net.minecraft.resources.ResourceLocation fluid) {
        if (chunk == null || loadedWaterChunk(level, pos) != chunk || expected == null
                || !expected.getFluidState().isSource() || expected.hasBlockEntity()
                || fluid == null || !thaumcraft.common.lib.fluid.GolemFluidBlocks.isFluid(expected, fluid)
                || chunk.getBlockState(pos) != expected || !golem.canCollectSourceFluid(pos, expected)
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        // Guard the immediate native notification/shape halo. Normal downstream
        // Minecraft propagation is not a universal addon callback sandbox.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            BlockPos cell = pos.offset(dx, 0, dz);
            if (!level.getWorldBorder().isWithinBounds(cell)
                    || level.getChunkSource().getChunkNow(cell.getX() >> 4, cell.getZ() >> 4) == null) return false;
        }
        return true;
    }

    /** Literal LiquidBlock.pickupBlock flags11, and Level's post-chunk tail.
     * Stop stale notifications if a normal neighbor callback replaces the cell. */
    private static void notifyWaterChange(ServerLevel level, LevelChunk chunk, BlockPos pos, BlockState old, BlockState now) {
        if (!sameCell(level, chunk, pos, now)) return;
        level.setBlocksDirty(pos, old, now);
        if (!sameCell(level, chunk, pos, now)) return;
        level.sendBlockUpdated(pos, old, now, Block.UPDATE_ALL_IMMEDIATE);
        if (!sameCell(level, chunk, pos, now)) return;
        level.blockUpdated(pos, old.getBlock());
        if (!sameCell(level, chunk, pos, now)) return;
        old.updateIndirectNeighbourShapes(level, pos, 9, 511);
        if (!sameCell(level, chunk, pos, now)) return;
        now.updateNeighbourShapes(level, pos, 9, 511);
        if (!sameCell(level, chunk, pos, now)) return;
        now.updateIndirectNeighbourShapes(level, pos, 9, 511);
        if (sameCell(level, chunk, pos, now)) level.onBlockStateChange(pos, old, now);
    }
    private static boolean sameCell(ServerLevel level, LevelChunk chunk, BlockPos pos, BlockState state) {
        return loadedWaterChunk(level, pos) == chunk && chunk.getBlockState(pos) == state;
    }

    /** Per-block admission; called again after every external callback. */
    private interface Eligibility {
        boolean test(ServerLevel level, EntityGolemBase golem, BlockPos pos, BlockState expected);
    }

    static boolean harvest(EntityGolemBase golem, BlockPos target, BlockState expected) {
        return guardedBreak(golem, target, expected, GolemHarvestPlatform::eligible);
    }

    /**
     * Lumber: remove ONE endpoint of the anchor's tree. The anchor needs the ordinary golem
     * reach; the remote endpoint is the original walk's bounded ability, so the full
     * callback and protection sequence runs at the ACTUAL endpoint, never once for a tree.
     */
    static boolean chop(EntityGolemBase golem, BlockPos anchor, BlockState expectedAnchor) {
        if (!(golem.level() instanceof ServerLevel level) || bridge == null || anchor == null || expectedAnchor == null) return false;
        BlockPos from = anchor.immutable();
        if (!chopAnchorEligible(level, golem, from, expectedAnchor)) return false;
        BlockPos endpoint = TreeLogTraversal.endpoint(level, from, expectedAnchor);
        if (endpoint == null) return false;
        boolean removed = guardedBreak(golem, endpoint, level.getBlockState(endpoint), GolemHarvestPlatform::chopEndpointEligible);
        // The original also plays its break effect at the anchor the golem was hitting.
        if (removed) level.levelEvent(2001, from, Block.getId(expectedAnchor));
        return removed;
    }

    private static boolean chopAnchorEligible(ServerLevel level, EntityGolemBase golem, BlockPos anchor, BlockState expected) {
        return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level
                && golem.level() == level && level.getEntity(golem.getUUID()) == golem && golem.canWork()
                && golem.getGolemCore() == GolemData.LUMBER && golem.isAlive()
                && !level.isOutsideBuildHeight(anchor) && level.getWorldBorder().isWithinBounds(anchor) && level.hasChunkAt(anchor)
                && level.getBlockState(anchor) == expected && TreeLogTraversal.isLog(level, anchor, expected)
                && golem.distanceToSqr(anchor.getX() + .5, anchor.getY() + .5, anchor.getZ() + .5) <= 4;
    }

    private static boolean chopEndpointEligible(ServerLevel level, EntityGolemBase golem, BlockPos pos, BlockState expected) {
        return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level
                && golem.level() == level && level.getEntity(golem.getUUID()) == golem && golem.canWork()
                && golem.getGolemCore() == GolemData.LUMBER && golem.isAlive()
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos)
                && expected != null && level.getBlockState(pos) == expected && !expected.hasBlockEntity()
                && level.getBlockEntity(pos) == null && TreeLogTraversal.isLog(level, pos, expected);
    }

    /** The one callback-fenced native removal shared by crops and logs. */
    private static boolean guardedBreak(EntityGolemBase golem, BlockPos target, BlockState expected, Eligibility eligible) {
        if (!(golem.level() instanceof ServerLevel level) || bridge == null || target == null) return false;
        BlockPos pos = target.immutable();
        if (!eligible.test(level, golem, pos, expected)) return false;
        Bridge platform = bridge;
        try {
            ServerPlayer actor = platform.actor(level);
            if (!bound(platform, level, actor)) return false;
            actor.setPos(golem.getX(), golem.getY(), golem.getZ());
            actor.setYRot(golem.getYRot());
            actor.setXRot(golem.getXRot());
            if (!eligible.test(level, golem, pos, expected) || !level.mayInteract(actor, pos)) return false;
            if (!platform.sharedBreak(level, actor, pos, expected)) {
                platform.canceled(level, actor, pos, expected);
                return false;
            }
            if (!eligible.test(level, golem, pos, expected) || !bound(platform, level, actor)
                    || actor.distanceToSqr(golem) != 0 || !level.mayInteract(actor, pos)) return false;
            if (!platform.before(level, actor, pos, expected)) {
                platform.canceled(level, actor, pos, expected);
                return false;
            }
            if (!eligible.test(level, golem, pos, expected) || !bound(platform, level, actor)
                    || actor.distanceToSqr(golem) != 0 || !level.mayInteract(actor, pos)) {
                platform.canceled(level, actor, pos, expected);
                return false;
            }
            // Native destruction alone emits normal drops and honors doTileDrops.
            // Do not also evaluate loot tables, collect cargo, or manufacture items.
            boolean destroyed = level.destroyBlock(pos, true, golem);
            if (!destroyed) {
                platform.canceled(level, actor, pos, expected);
                return false;
            }
            try { platform.after(level, actor, pos, expected); }
            catch (RuntimeException feedback) {
                Thaumcraft.LOG.warn("Golem post-break callback failed at {}; the actual removal is not replayed", pos, feedback);
            }
            return true;
        } catch (RuntimeException failure) {
            // A callback may already have changed the world. Never restore the
            // captured block or replay its drops in response to an exception.
            Thaumcraft.LOG.warn("Golem block action refused or interrupted at {}; no synthetic drops or rollback", pos, failure);
            return false;
        }
    }

    /**
     * Order (original AIHarvestCrops:194-245), only after {@link #harvest} actually removed a crop. Every real drop within
     * the box around the golem is considered: a fresh one is pushed toward the golem, cocoa beans re-grow the pod against its
     * jungle log, and the vanilla seed family is planted into the nine cells below the harvested column. Each placement runs
     * the full permission sequence and is paid by exactly one real seed AFTER the block is set. Never throws and never undoes
     * the harvest; a refused cell loses nothing.
     */
    static void replant(EntityGolemBase golem, BlockPos harvested, BlockState harvestedState) {
        if (!(golem.level() instanceof ServerLevel level) || bridge == null || harvested == null || harvestedState == null) return;
        try {
            if (!replantEligible(level, golem)) return;
            BlockPos at = harvested.immutable();
            Vec3 origin = golem.position();
            List<ItemEntity> seeds = new ArrayList<>();
            List<Block> crops = new ArrayList<>();
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(origin, origin).inflate(GolemReplant.RANGE))) {
                if (!replantEligible(level, golem)) return;
                if (!item.isAlive() || item.level() != level) continue;
                if (item.tickCount < GolemReplant.FRESH_TICKS) item.setDeltaMovement(GolemReplant.nudge(origin, item.position()));
                if (!DeferredItemDrops.allowPlayerTouch(item)) continue; // A queued native ADD candidate is not ours to touch.
                if (item.getItem().is(Items.COCOA_BEANS)) { plantCocoa(level, golem, item, at, harvestedState); break; }
                Block crop = GolemReplant.seedBlock(item.getItem());
                if (crop == null) continue;
                seeds.add(item);
                crops.add(crop);
            }
            // Pass 0 plants every cell the engine's own light data already accepts, in the original item-then-cell order.
            // Pass 1 plants the cells accepted only on predicted light (a just-cleared opaque cell, see survives) LAST: until
            // the asynchronous light update lands, a neighbour's planting would trigger that crop's shape update, find it
            // dark and delete it, wasting the seed. With settled light pass 0 is the whole original loop, unchanged.
            for (int pass = 0; pass < 2; pass++) {
                for (int index = 0; index < seeds.size(); index++) {
                    ItemEntity item = seeds.get(index);
                    Block crop = crops.get(index);
                    for (int cell = 0; cell < GolemReplant.CELLS && item.isAlive() && !item.getItem().isEmpty(); cell++) {
                        BlockPos ground = GolemReplant.ground(at, cell);
                        boolean settled = crop.defaultBlockState().canSurvive(level, ground.above());
                        if (settled != (pass == 0)) continue;
                        if (plantSeed(level, golem, item, crop, ground)) debit(item);
                    }
                }
            }
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem replanting at {} failed; the harvest is not undone and no seed is refunded", harvested, failure);
        }
    }

    private static boolean plantSeed(ServerLevel level, EntityGolemBase golem, ItemEntity item, Block crop, BlockPos ground) {
        ItemStack seed = item.getItem().copyWithCount(1);
        return place(level, golem, seed, crop.defaultBlockState(), ground.above(), ground, Direction.UP,
                () -> item.isAlive() && ItemStack.isSameItemSameComponents(item.getItem(), seed));
    }

    /** Adaptation: the original plants cocoa from any bean drop using the HARVESTED block's metadata; here only a harvested pod regrows. */
    private static void plantCocoa(ServerLevel level, EntityGolemBase golem, ItemEntity item, BlockPos at, BlockState harvested) {
        if (!harvested.is(Blocks.COCOA)) return;
        Direction facing = harvested.getValue(CocoaBlock.FACING);
        BlockPos log = at.relative(facing);
        if (!cellLoaded(level, log) || !level.getBlockState(log).is(BlockTags.JUNGLE_LOGS)) return;
        BlockState planted = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, facing);
        ItemStack bean = item.getItem().copyWithCount(1);
        if (place(level, golem, bean, planted, at, log, facing.getOpposite(),
                () -> item.isAlive() && ItemStack.isSameItemSameComponents(item.getItem(), bean))) debit(item);
    }

    /** One placement through the whole permission sequence; the world is changed only when every check still holds. */
    private static boolean place(ServerLevel level, EntityGolemBase golem, ItemStack held, BlockState state,
                                 BlockPos pos, BlockPos clicked, Direction face, BooleanSupplier owned) {
        Bridge platform = bridge;
        if (!placementEligible(level, golem, state, pos, clicked, owned)) return false;
        ServerPlayer actor = platform.actor(level);
        if (!bound(platform, level, actor)) return false;
        actor.setPos(golem.getX(), golem.getY(), golem.getZ());
        actor.setYRot(golem.getYRot());
        actor.setXRot(golem.getXRot());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).relative(face, .5), face, clicked, false);
        int slot = actor.getInventory().selected;
        boolean allowed;
        actor.getInventory().setItem(slot, held.copy());
        try {
            allowed = placementEligible(level, golem, state, pos, clicked, owned)
                    && level.mayInteract(actor, clicked) && level.mayInteract(actor, pos) && actor.mayUseItemAt(pos, face, held)
                    && !BlockEvent.PLACE.invoker().placeBlock(level, pos, state, actor).isFalse()
                    && platform.allowsPlacement(level, actor, hit);
        } finally { actor.getInventory().setItem(slot, ItemStack.EMPTY); }
        if (!allowed || !bound(platform, level, actor) || actor.distanceToSqr(golem) != 0
                || !placementEligible(level, golem, state, pos, clicked, owned)) return false;
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) return false;
        SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(golem, state));
        return true;
    }

    /** The one real seed is paid only after its block exists; a stack of one disappears, a larger one shrinks in place. */
    private static void debit(ItemEntity item) {
        ItemStack current = item.getItem();
        if (current.getCount() <= 1) item.discard();
        else item.setItem(current.copyWithCount(current.getCount() - 1));
    }

    private static boolean placementEligible(ServerLevel level, EntityGolemBase golem, BlockState state, BlockPos pos, BlockPos clicked,
                                             BooleanSupplier owned) {
        return replantEligible(level, golem) && owned.getAsBoolean() && cellLoaded(level, pos) && cellLoaded(level, clicked)
                && level.getBlockState(pos).isAir() && survives(level, state, pos)
                && level.isUnobstructed(state, pos, CollisionContext.empty());
    }

    /**
     * Block survival with one engine adaptation. 1.21 light data is asynchronous: for a tick or two a cell that just held an
     * opaque block (a harvested melon or pumpkin) still reads dark, so a light-dependent crop (wheat, carrots, potatoes,
     * beetroots, stems) is refused there although the original, whose light was updated at once, would have planted it.
     * When, and only when, the block's own rules fail on that light alone (farmland below, an empty cell) the light the engine
     * is about to compute is predicted from the neighbours: each loses 1 on its way in, except sky light of 15 coming straight
     * down. A roofed, dark cellar still fails; a lit one passes exactly when the settled cell would. Nether wart has no light
     * rule and never reaches this. Soil, occupancy and every other refusal are unchanged.
     */
    private static boolean survives(ServerLevel level, BlockState state, BlockPos pos) {
        if (state.canSurvive(level, pos)) return true;
        if (!(state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock)
                || !level.getBlockState(pos.below()).is(Blocks.FARMLAND)) return false;
        int predicted = 0;
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (!cellLoaded(level, next) || level.getBlockState(next).isSolidRender(level, next)) continue;
            int raw = level.getRawBrightness(next, 0);
            boolean openSkyAbove = side == Direction.UP && level.getBrightness(LightLayer.SKY, next) >= 15;
            predicted = Math.max(predicted, openSkyAbove ? raw : raw - 1);
        }
        return predicted >= 8;
    }

    private static boolean cellLoaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }

    private static boolean replantEligible(ServerLevel level, EntityGolemBase golem) {
        return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level
                && golem.level() == level && level.getEntity(golem.getUUID()) == golem && golem.canWork()
                && golem.getGolemCore() == GolemData.HARVEST && golem.isAlive()
                && golem.getUpgradeAmount(GolemUpgrade.ORDER) > 0
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private static boolean eligible(ServerLevel level, EntityGolemBase golem, BlockPos pos, BlockState expected) {
        return level.getServer().isSameThread() && level.getServer().getLevel(level.dimension()) == level
                && golem.level() == level && level.getEntity(golem.getUUID()) == golem && golem.canWork()
                && golem.getGolemCore() == GolemData.HARVEST && golem.isAlive()
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos)
                && expected != null && level.getBlockState(pos) == expected && !expected.hasBlockEntity()
                && expected.getDestroySpeed(level, pos) >= 0 && level.getBlockEntity(pos) == null
                && golem.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 4
                && GolemCrops.ripe(level, pos, expected);
    }
    private static boolean bound(Bridge platform, ServerLevel level, ServerPlayer actor) {
        return actor != null && !GolemUsePlatform.isActorBorrowed(actor) && platform.isNativeActor(actor) && actor.serverLevel() == level
                && actor.server == level.getServer() && !actor.isRemoved()
                && level.getServer().getPlayerList().getPlayer(actor.getUUID()) == null
                && !actor.hasPermissions(2) && !actor.isSpectator() && !actor.getAbilities().instabuild
                && actor.getAbilities().mayBuild && actor.getInventory().isEmpty();
    }
}
