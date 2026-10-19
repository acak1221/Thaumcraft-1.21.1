package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.utils.RejectedEntitySpawn;

/** One real golem-owned cast, plus at most one separately owned unpublished catch. */
final class FishingGolemGoals extends Goal {
    private static final String PENDING = "FishingCatch";
    private static final Set<String> PENDING_KEYS = Set.of("Dimension", "Water", "Motion", "Stack");
    private static final Set<String> BURNING_PENDING_KEYS = Set.of("Dimension", "Water", "Motion", "Stack", "Fire");
    private static final Set<String> STACK_KEYS = Set.of("id", "count", "components");
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    private final EntityGolemBase golem;
    private BlockPos water;
    private BlockState waterState;
    private BlockState airState;
    private EntityGolemBobber bobber;
    private RejectedEntitySpawn.Snapshot admission;
    private ServerLevel castLevel;
    private boolean nativeAdded;
    private boolean addingBobber;
    private boolean cleaningBobber;
    private boolean admitted;
    private boolean running;
    private boolean firstUpdate;
    private boolean attempted;
    private int count = -1;
    private float quality;
    private Catch pending;
    private Tag unreadPending;

    private record Cell(BlockPos pos, BlockState state) {}
    private record Sky(BlockPos pos, boolean visible) {}
    private record PondSample(GolemFishingLoot.Pond pond, List<Cell> cells, List<Sky> sky) {}
    /** {@code fireTicks}: the flame the delivered item will carry, decided when the catch was paid (Fire at that moment). */
    private record Catch(ResourceLocation dimension, BlockPos water, Vec3 motion, ItemStack stack, int fireTicks) {}

    FishingGolemGoals(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Original AIFish:71 {@code ceil(range / 2)}, read live so a Water upgrade needs no new goal. */
    private int radius() { return GolemUpgrades.workRadius(golem.getRange(), 2); }

    void install() { golem.addWorkGoal(2, this); }
    boolean hasPending() { return pending != null || unreadPending != null; }
    boolean hasUnreadPending() { return unreadPending != null; }
    boolean hasCast() { return admitted && water != null && bobber != null; }
    boolean ownsBobber(EntityGolemBobber candidate) { return hasCast() && bobber == candidate; }
    void resetRuntime() { running = firstUpdate = false; } // Never resets paid output.
    private boolean present(ServerLevel level) {
        return golem.level() == level && level.getServer().isSameThread() && !golem.isRemoved()
                && level.getEntity(golem.getId()) == golem && level.getEntity(golem.getUUID()) == golem;
    }
    private boolean owner(ServerLevel level) {
        return present(level) && golem.getGolemCore() == GolemData.FISHING && golem.canWork();
    }
    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.getWorldBorder().isWithinBounds(pos.getX() + .5, pos.getZ() + .5) && level.hasChunkAt(pos);
    }
    private static boolean water(BlockState state) { return state.is(Blocks.WATER) && state.getBlock().getClass() == LiquidBlock.class; }
    private boolean inHomeBounds(BlockPos pos) {
        BlockPos home = golem.getHomePosition();
        int radius = radius();
        return Math.abs((long) pos.getX() - home.getX()) <= radius && Math.abs((long) pos.getZ() - home.getZ()) <= radius
                && Math.abs((long) pos.getY() - home.getY()) <= radius / 2;
    }
    private boolean currentBobber(ServerLevel level) {
        return bobber != null && admitted && bobber.level() == level && bobber.isAlive() && !bobber.isRemoved()
                && loaded(level, bobber.blockPosition()) && level.getEntity(bobber.getId()) == bobber && level.getEntity(bobber.getUUID()) == bobber
                && bobber.getFisher() == golem && water != null && water.equals(bobber.getWaterTarget());
    }
    private boolean currentCast(ServerLevel level) {
        return owner(level) && level == castLevel && currentBobber(level) && inHomeBounds(water)
                && loaded(level, water) && loaded(level, water.above())
                && level.getBlockState(water) == waterState && water(waterState)
                && level.getBlockState(water.above()) == airState && airState.isAir();
    }

    /** Called under the entity's busy guard at actual server-tick cadence. */
    void serverTick(ServerLevel server) {
        try { idleTick(server); }
        catch (RuntimeException failure) {
            cancelCast();
            Thaumcraft.LOG.warn("Golem fishing update failed; retaining any paid output", failure);
        }
    }
    private void idleTick(ServerLevel server) {
        if (!present(server)) { cancelCast(); return; }
        if (hasPending()) {
            Catch paid = pending;
            if (publishPending(server) && paid != null) finishFeedback(server, paid);
            else cancelCast();
            return;
        }
        if (!owner(server)) { cancelCast(); return; }
        if (water != null && (!currentCast(server) || !clearCastLine(server, water))) cancelCast();
        if (bobber != null && !admitted) cleanBobber();
        if (water != null || bobber != null || running || count > 0 || golem.tickCount % 5 != 0 || !golem.getNavigation().isDone()) return;
        BlockPos home = golem.getHomePosition();
        int radius = radius();
        for (int i = 0; i < radius * 2; i++) {
            int x = home.getX() + golem.getRandom().nextInt(radius * 2 + 1) - radius;
            int y = home.getY() + golem.getRandom().nextInt(radius + 1) - radius / 2;
            int z = home.getZ() + golem.getRandom().nextInt(radius * 2 + 1) - radius;
            BlockPos pos = new BlockPos(x, y, z);
            if (!inHomeBounds(pos) || !loaded(server, pos) || !loaded(server, pos.above())) continue;
            if (!water(server.getBlockState(pos)) || !server.getBlockState(pos.above()).isAir()) continue;
            beginCast(server, pos); // First actual water/air candidate, not a best-pond search.
            return;
        }
    }

    private void beginCast(ServerLevel server, BlockPos pos) {
        water = pos.immutable();
        waterState = server.getBlockState(pos);
        airState = server.getBlockState(pos.above());
        castLevel = server;
        try {
            PondSample sample = samplePond(server, pos);
            if (sample == null || !sampleCurrent(server, sample, true) || !clearCastLine(server, pos) || !owner(server)
                    || water == null || !water.equals(pos) || server.getBlockState(pos) != waterState
                    || server.getBlockState(pos.above()) != airState) { cancelCast(); return; }
            quality = sample.pond().quality();
            bobber = new EntityGolemBobber(ThaumcraftGolems.GOLEM_BOBBER.get(), server);
            bobber.setup(golem, pos);
            if (bobber.level() != server || bobber.isRemoved() || server.getEntity(bobber.getId()) != null
                    || server.getEntity(bobber.getUUID()) != null || !owner(server)) { cancelCast(); return; }
            // Root supplies the shared attempt-scoped utility's Entity overload;
            // its earlier ItemEntity-only signature cannot clean a bobber reservation.
            admission = RejectedEntitySpawn.snapshotBefore(server, bobber);
            if (admission.knownBefore()) { cancelCast(); return; }
            addingBobber = true;
            try { nativeAdded = server.addFreshEntity(bobber); }
            finally { addingBobber = false; }
            admitted = nativeAdded;
            if (!currentCast(server) || !sampleCurrent(server, sample, true) || !clearCastLine(server, pos)
                    || !currentCast(server) || !sampleCurrent(server, sample, false)) { cancelCast(); return; }
            attempted = false;
            count = 300 + server.random.nextInt(200); // Original AIFish:161 uses world RNG, not probe RNG.
            golem.syncFishingBobber(bobber);
            golem.startFishingArm();
            server.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.ARROW_SHOOT,
                    SoundSource.NEUTRAL, .5F, .4F / (server.random.nextFloat() * .4F + .8F));
        } catch (RuntimeException failure) {
            cancelCast();
            Thaumcraft.LOG.warn("Golem fishing admission failed at {}", pos, failure);
        }
    }

    private BlockState sampleCell(ServerLevel server, BlockPos pos, List<Cell> cells) {
        if (!owner(server) || !loaded(server, pos)) return null;
        BlockState state = server.getBlockState(pos);
        if (!GolemHarvestPlatform.mayInteract(golem, pos) || !owner(server) || !loaded(server, pos) || server.getBlockState(pos) != state) return null;
        cells.add(new Cell(pos.immutable(), state));
        return state;
    }
    private PondSample samplePond(ServerLevel server, BlockPos pos) {
        List<Cell> cells = new ArrayList<>(22);
        List<Sky> sky = new ArrayList<>(4);
        BlockState center = sampleCell(server, pos, cells), above = sampleCell(server, pos.above(), cells);
        if (center == null || above == null || !water(center) || !above.isAir()) return null;
        int adjacent = 0, exposed = 0, depth = 0;
        for (Direction side : SIDES) {
            BlockPos neighbor = pos.relative(side), upper = neighbor.above();
            BlockState n = sampleCell(server, neighbor, cells), air = sampleCell(server, upper, cells);
            if (n == null || air == null) return null;
            if (!water(n) || !air.isAir()) continue;
            ++adjacent;
            boolean visible = server.canSeeSky(upper);
            sky.add(new Sky(upper, visible));
            if (visible) ++exposed;
            for (int d = 1; d <= 3; d++) {
                BlockState below = sampleCell(server, neighbor.below(d), cells);
                if (below == null) return null;
                if (water(below)) ++depth; // Independent cells; a gap does not end depth sampling.
            }
        }
        return new PondSample(new GolemFishingLoot.Pond(adjacent, exposed, depth), cells, sky);
    }
    private boolean sampleCurrent(ServerLevel server, PondSample sample, boolean permission) {
        if (!owner(server)) return false;
        if (permission) for (Cell cell : sample.cells()) {
            if (!loaded(server, cell.pos()) || !GolemHarvestPlatform.mayInteract(golem, cell.pos()) || !owner(server)) return false;
        }
        for (Cell cell : sample.cells()) if (!loaded(server, cell.pos()) || server.getBlockState(cell.pos()) != cell.state()) return false;
        for (Sky sky : sample.sky()) if (server.canSeeSky(sky.pos()) != sky.visible()) return false;
        return true;
    }
    private boolean clearCastLine(ServerLevel server, BlockPos pos) {
        Vec3 from = golem.getEyePosition(), to = new Vec3(pos.getX() + .5, pos.getY() + 1.0, pos.getZ() + .5);
        if (!finite(from) || !finite(to)) return false;
        // Guard only the actual native ray's cells (and a one-cell X/Z shape halo),
        // NOT the whole17x17 search region or a force-loaded navigation region.
        boolean blocked = BlockGetter.traverseBlocks(from, to, server, (level, cell) -> {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (!loaded(level, cell.offset(dx, 0, dz))) return Boolean.TRUE;
            }
            return null;
        }, level -> Boolean.FALSE);
        return !blocked && server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, golem)).getType() == HitResult.Type.MISS;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        return !running && !attempted && !hasPending() && golem.level() instanceof ServerLevel server && currentCast(server)
                && golem.getNavigation().isDone();
    }
    @Override public boolean canContinueToUse() {
        return running && !attempted && !hasPending() && golem.level() instanceof ServerLevel server && currentCast(server);
    }
    @Override public void start() { running = firstUpdate = true; }
    @Override public void tick() { golem.tickFishingCast(); }
    void activeTick(ServerLevel server) {
        BlockPos selected = water;
        try {
            if (!running || attempted || hasPending() || !currentCast(server) || !clearCastLine(server, selected)) { cancelCast(); return; }
            // Vanilla's selector continuation cleanup is not an every-tick clock.
            // Match F:108 on EACH active update after the original start update.
            if (firstUpdate) firstUpdate = false;
            else if (!(count-- > 0)) { cancelCast(); return; }
            EntityGolemBobber ownedBobber = bobber;
            golem.getLookControl().setLookAt(selected.getX() + .5, selected.getY() + 1.0, selected.getZ() + .5, 30, 30);
            if (server.random.nextFloat() >= quality + golem.getGolemStrength() * .00015F) return;
            attempted = true; // Latch BEFORE permission callbacks/generation; at most one roll per cast.
            PondSample sample = samplePond(server, selected); // Re-sample CATEGORY only on success, not the cast chance.
            if (sample == null || !currentCast(server) || bobber != ownedBobber || !selected.equals(water)
                    || !clearCastLine(server, selected) || !sampleCurrent(server, sample, true)
                    || !currentCast(server) || !clearCastLine(server, selected)
                    || !currentCast(server) || !sampleCurrent(server, sample, false)) { cancelCast(); return; }
            // All authority rechecks finish BEFORE this pure loot/RNG/queue handoff;
            // no world callback can invalidate a newly owned result in that gap.
            // Air (original AIFish:118-120): sometimes a second result. Each one is rolled, cooked (Fire, :124-126), given its own
            // trajectory and published before the next is rolled, so a failed publication can never strand a rolled result.
            int catches = GolemFishingLoot.catchCount(server.random, golem);
            for (int caught = 0; caught < catches; caught++) {
                ItemStack result = GolemFishingLoot.nextCatch(server, server.random, golem, sample.pond());
                if (result.isEmpty()) throw new IllegalStateException("Fishing returned an empty paid catch");
                double dx = golem.getX() + (double) server.random.nextFloat() - (double) server.random.nextFloat() - selected.getX() + .5;
                double dy = golem.getY() - selected.getY() + 1.0;
                double dz = golem.getZ() + (double) server.random.nextFloat() - (double) server.random.nextFloat() - selected.getZ() + .5;
                double length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                Vec3 motion = new Vec3(.1 * dx, .1 * dy + .08 * (double) (float) Math.sqrt(length), .1 * dz);
                pending = new Catch(server.dimension().location(), selected, motion, result, GolemFishingSmelting.burningTicks(golem));
                Catch paid = pending;
                if (!publishPending(server)) { cancelCast(); return; } // The kept result is retried by the idle tick.
                if (caught == catches - 1) finishFeedback(server, paid);
            }
        } catch (RuntimeException failure) {
            cancelCast(); // A paid result, if already assigned, is NOT cleared or rerolled.
            Thaumcraft.LOG.warn("Golem fishing catch failed at {}; retaining any paid output", selected, failure);
        }
    }

    /** Existing paid owner only; may evacuate after drying, death, recall or core changes. */
    boolean publishPending(ServerLevel current) {
        if (unreadPending != null) return false;
        Catch owned = pending;
        if (owned == null) return true;
        if (!present(current)) return false; // A removed transfer source cannot later publish its cloned pending image.
        ServerLevel source = current.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, owned.dimension()));
        if (source == null) return false;
        try {
            DeferredItemDrops.enqueueGolemFishingCatch(source, owned.water(), owned.motion(), owned.stack(), owned.fireTicks());
            pending = null; // FIRST work after normal return: no feedback/removal/callback before raw detach.
            return true;
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem fishing publication failed at {}; retaining the same catch/trajectory", owned.water(), failure);
            return false;
        }
    }
    private void finishFeedback(ServerLevel server, Catch paid) {
        try {
            if (golem.level() == server && server.dimension().location().equals(paid.dimension()) && owner(server)) {
                golem.startFishingArm();
                if (currentBobber(server) && loaded(server, BlockPos.containing(bobber.getX(), bobber.getY() + .5, bobber.getZ()))) {
                    server.playSound(null, bobber.getX(), bobber.getY(), bobber.getZ(), SoundEvents.GENERIC_SPLASH,
                            SoundSource.NEUTRAL, .15F, 1.0F + (server.random.nextFloat() - server.random.nextFloat()) * .4F);
                    server.sendParticles(ParticleTypes.SPLASH, bobber.getX(), bobber.getY() + .5, bobber.getZ(),
                            20 + server.random.nextInt(20), .1, 0, .1, 0);
                }
            }
        } finally { cancelCast(); }
    }

    /** Cancellation NEVER clears pending paid stock or its raw unread representation. */
    void cancelCast() {
        admitted = false;
        water = null;
        waterState = airState = null;
        count = -1;
        quality = 0;
        attempted = true;
        firstUpdate = false;
        golem.syncFishingBobber(null);
        cleanBobber();
    }
    private void cleanBobber() {
        EntityGolemBobber old = bobber;
        if (old == null || addingBobber || cleaningBobber) return; // Never release an in-progress native reservation from its callbacks.
        cleaningBobber = true;
        try {
            old.discard(); // Exact owned source object, never a lookup replacement/foreign UUID.
            if (!nativeAdded && admission != null && castLevel != null) {
                RejectedEntitySpawn.releaseRejected(castLevel, old, admission, false);
            }
            bobber = null;
            admission = null;
            castLevel = null;
            nativeAdded = false;
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.warn("Golem bobber cleanup failed; retaining its exact reference for retry", failure);
        } finally { cleaningBobber = false; }
    }
    @Override public void stop() {
        try { golem.cancelFishingCast(); }
        finally { running = false; }
    }

    void savePending(CompoundTag tag) {
        if (unreadPending != null) { tag.put(PENDING, unreadPending.copy()); return; }
        if (pending == null) { tag.remove(PENDING); return; }
        CompoundTag encoded = new CompoundTag();
        encoded.putString("Dimension", pending.dimension().toString());
        encoded.putIntArray("Water", new int[]{pending.water().getX(), pending.water().getY(), pending.water().getZ()});
        ListTag motion = new ListTag();
        motion.add(DoubleTag.valueOf(pending.motion().x)); motion.add(DoubleTag.valueOf(pending.motion().y)); motion.add(DoubleTag.valueOf(pending.motion().z));
        encoded.put("Motion", motion);
        encoded.put("Stack", pending.stack().save(golem.registryAccess()));
        if (pending.fireTicks() != 0) encoded.putInt("Fire", pending.fireTicks()); // Absent in saves from before the burning catch.
        tag.put(PENDING, encoded);
    }
    boolean loadPending(CompoundTag tag) {
        pending = null;
        unreadPending = null;
        if (!tag.contains(PENDING)) return true;
        Tag raw = tag.get(PENDING);
        try {
            if (!(raw instanceof CompoundTag data) || !data.getAllKeys().equals(PENDING_KEYS) && !data.getAllKeys().equals(BURNING_PENDING_KEYS)
                    || !data.contains("Dimension", Tag.TAG_STRING) || !data.contains("Water", Tag.TAG_INT_ARRAY)
                    || !data.contains("Motion", Tag.TAG_LIST) || !data.contains("Stack", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Catch shape");
            ResourceLocation dimension = ResourceLocation.parse(data.getString("Dimension"));
            if (!dimension.toString().equals(data.getString("Dimension"))) throw new IllegalArgumentException("Catch dimension");
            int[] pos = data.getIntArray("Water");
            ListTag motion = data.getList("Motion", Tag.TAG_DOUBLE);
            if (pos.length != 3 || motion.size() != 3) throw new IllegalArgumentException("Catch geometry");
            Vec3 velocity = new Vec3(motion.getDouble(0), motion.getDouble(1), motion.getDouble(2));
            CompoundTag stackTag = data.getCompound("Stack");
            ItemStack stack = ItemStack.CODEC.parse(golem.registryAccess().createSerializationContext(NbtOps.INSTANCE), stackTag).result().orElse(ItemStack.EMPTY);
            if (!finite(velocity) || !STACK_KEYS.containsAll(stackTag.getAllKeys()) || stack.isEmpty() || stack.getCount() > stack.getMaxStackSize()
                    || !stack.save(golem.registryAccess()).equals(stackTag)) throw new IllegalArgumentException("Catch stack");
            int fireTicks = data.contains("Fire") ? data.getInt("Fire") : 0;
            if (data.contains("Fire") && (!data.contains("Fire", Tag.TAG_INT) || fireTicks != GolemFishingSmelting.BURNING_TICKS)) throw new IllegalArgumentException("Catch flame");
            pending = new Catch(dimension, new BlockPos(pos[0], pos[1], pos[2]), velocity, stack, fireTicks);
            return true;
        } catch (RuntimeException invalid) {
            unreadPending = raw.copy();
            return false; // No partial/native-codec normalization may flatten a paid future/malformed row.
        }
    }
    private static boolean finite(Vec3 vec) { return Double.isFinite(vec.x) && Double.isFinite(vec.y) && Double.isFinite(vec.z); }
}
