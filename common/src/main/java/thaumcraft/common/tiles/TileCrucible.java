package thaumcraft.common.tiles;

import java.util.ArrayDeque;
import java.util.UUID;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.crafting.CrucibleCrafting;
import thaumcraft.common.crafting.CrucibleRecipeInput;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.utils.RejectedEntitySpawn;

/** Original TileCrucible's physical alchemy loop; native fluid-loader adapters are not implemented here. */
public final class TileCrucible extends BlockEntity {
    public static final int CAPACITY = 1000;
    public static final int MAX_TAGS = 100;
    private static final ResourceLocation BELLOWS = ResourceLocation.fromNamespaceAndPath("thaumcraft", "bellows");
    private static final ResourceLocation NITOR = ResourceLocation.fromNamespaceAndPath("thaumcraft", "nitor");
    private static final SoundEvent BUBBLE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "bubble"));
    private AspectList aspects = new AspectList();
    private int water;
    private short heat;
    private long counter = -100L;
    private boolean processing;
    /**
     * Original TileCrucible:69 - the bellows count is found once, on the first
     * server tick, and then kept. The original never looks at the neighbours
     * again, so a bellows that is placed or removed later is only noticed when
     * the chunk is read again. The furnace, in contrast, counts them every tick.
     */
    private int bellows = -1;

    /** Client-only implementation is installed without linking client classes from common/server code. */
    public interface ClientEffects {
        void tick(TileCrucible tile);
        void event(TileCrucible tile, int id, int data);
    }
    private static ClientEffects clientEffects;
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }

    public TileCrucible(BlockPos pos, BlockState state) { super(ThaumcraftAlchemy.CRUCIBLE_BLOCK_ENTITY.get(), pos, state); }
    public AspectList getAspects() { return aspects.copy(); }
    public int getWaterAmount() { return water; }
    public short getHeat() { return heat; }
    public int tagAmount() { return aspects.visSize(); }

    public int fillWater(int amount, boolean doFill) {
        int accepted = acceptedFill(water, amount);
        if (doFill && accepted > 0) {
            if (!(level instanceof ServerLevel server) || !isCurrent() || !server.getServer().isSameThread()) return 0;
            water += accepted;
            changed();
        }
        return accepted;
    }

    /**
     * The accepted amount of a fill of the tank of the original.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:71}: the tank holds one thousand
     * millibuckets and the original {@code FluidTank} never takes more than the free space.</p>
     */
    public static int acceptedFill(int current, int millibuckets) {
        return Math.min(Math.max(0, millibuckets), CAPACITY - current);
    }

    /**
     * The drained amount of the tank of the original.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:395-397}: the tank never gives away
     * more than it holds.</p>
     */
    public static int drained(int current, int maxDrain) {
        return Math.min(Math.max(0, maxDrain), current);
    }

    /**
     * The original's named drain of the tank.
     *
     * <p>Oracle {@code TileCrucible:384-393} refuses a resource that is not
     * {@code isFluidEqual(tank.getFluid())}. The Forge comparison of 1.7.10 compares the
     * <b>fluid identity and its tag</b>, never the amount, and an empty tank equals nothing.</p>
     *
     * @param requested the fluid of the caller
     * @param content the fluid of the tank
     * @param current the content of the tank in millibuckets
     * @param maxDrain the requested amount in millibuckets
     * @return the drained amount in millibuckets
     */
    public static int drainedOf(net.minecraft.world.level.material.Fluid requested,
                                net.minecraft.world.level.material.Fluid content,
                                int current, int maxDrain) {
        if (requested == null || content == null || requested != content || current <= 0) return 0;
        return drained(current, maxDrain);
    }

    /**
     * The original fluid handler of the crucible.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:373-382}: the crucible is a Forge
     * {@code IFluidHandler} with a tank of one thousand millibuckets that accepts <strong>water
     * only</strong>. The original marks the tile dirty and sends the block update before the tank
     * answers, so a refused fill still refreshes the tile.</p>
     *
     * @param fluid the fluid of the caller, {@code null} is the empty fluid of the original
     * @param millibuckets the amount of the caller
     * @param doFill false asks the original what it would do
     * @return the accepted amount in millibuckets
     */
    public int fillWater(net.minecraft.world.level.material.Fluid fluid, int millibuckets, boolean doFill) {
        // Oracle TileCrucible:374-376 - only the water of the original registry is accepted. The
        // null resource of the original passes that guard but the Forge tank then accepts nothing.
        if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.WATER) return 0;
        int accepted = fluid == null ? 0 : acceptedFill(water, millibuckets);
        if (doFill) {
            if (accepted > 0) water += accepted;
            // The original marks the tile dirty even when the tank refuses the fill.
            changed();
        }
        return accepted;
    }

    /**
     * The original drain of a named fluid.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:384-393}: the original refuses a
     * resource that is not the content of the tank. The Forge comparison of 1.7.10
     * ({@code FluidStack.isFluidEqual}) compares the <b>fluid and its tag only</b>, never the
     * amount, so a request of one millibucket drains one millibucket of a full tank. The original
     * then drains {@code min(content, requested)}.</p>
     *
     * @return the drained amount in millibuckets
     */
    public int drainWater(net.minecraft.world.level.material.Fluid fluid, int millibuckets, boolean doDrain) {
        // The tank of the original is empty without a fluid, so isFluidEqual answers false.
        boolean matches = fluid == net.minecraft.world.level.material.Fluids.WATER && water > 0;
        int amount = drainedOf(fluid, getWaterFluid(), water, millibuckets);
        if (doDrain && matches) {
            if (amount > 0) water -= amount;
            // The original marks the tile dirty before the tank answers.
            changed();
        }
        return amount;
    }

    /**
     * The original drain of the tank.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:395-397}: the tank gives away at most
     * the requested amount ({@code FluidTank.drain}). The original's unnamed drain does not touch the
     * tile flags; the native {@code changed()} is the sync that keeps an in-world tank visible and
     * the saved water durable.</p>
     *
     * @return the drained amount in millibuckets
     */
    public int drainWater(int maxDrain, boolean doDrain) {
        int drained = drained(water, maxDrain);
        if (doDrain) {
            if (drained > 0) water -= drained;
            changed();
        }
        return drained;
    }

    /** Oracle TileCrucible:399-401 - the crucible can be filled with water only. */
    public static boolean canFillWater(net.minecraft.world.level.material.Fluid fluid) {
        return fluid != null && fluid == net.minecraft.world.level.material.Fluids.WATER;
    }

    /** Oracle TileCrucible:403-405 - the crucible can be drained with any fluid. */
    public static boolean canDrainWater(net.minecraft.world.level.material.Fluid fluid) { return true; }

    /** Oracle TileCrucible:71 - the tank of the original holds one thousand millibuckets. */
    public int getWaterCapacity() { return CAPACITY; }

    /** The fluid of the tank of the original. */
    public net.minecraft.world.level.material.Fluid getWaterFluid() {
        return net.minecraft.world.level.material.Fluids.WATER;
    }

    /**
     * The original's tank of the crucible is water only and the Decanting golem pays it through the
     * loader independent fluid handler of the common layer, so the tile owns no transfer of its own.
     *
     * @see thaumcraft.common.lib.fluid.GolemFluidAccess
     */
    private boolean isCurrent() {
        return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(ThaumcraftAlchemy.CRUCIBLE.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileCrucible tile) {
        if (!tile.isCurrent()) return;
        if (level.isClientSide) {
            if (clientEffects != null) clientEffects.tick(tile);
            return;
        }
        tile.serverTick((ServerLevel) level);
    }

    private void serverTick(ServerLevel server) {
        ++counter;
        short previous = heat;
        // Original TileCrucible:98-100 - the bellows are counted once.
        if (bellows < 0) bellows = countBellows();
        if (water > 0 && isHeatSource(server.getBlockState(worldPosition.below()))) {
            // Intentionally do not clamp: original short heat can overshoot 200
            // by a bellows step before the heat<200 guard closes.
            if (heat < 200) heat = (short) (heat + 1 + bellows * 2);
        } else if (heat > 0) --heat;
        if (heat != previous) {
            setChanged();
            if (previous < 151 && heat >= 151 || heat == 149) sync();
        }
        // Overflow is independent of heat/water; counter is NOT reset here.
        if (tagAmount() > MAX_TAGS && counter % 5L == 0L) {
            Aspect tag = randomAspect(server);
            if (tag != null) aspects.remove(tag, 1);
            changed();
            ThaumcraftFlux.spill(server, worldPosition);
        }
        if (counter > 100L && heat > 150) {
            counter = 0;
            if (tagAmount() > 0) {
                Aspect tag = randomAspect(server);
                // Original preferential compound decay draws again ONCE if the
                // first draw is primal. It is not a search for any compound.
                if (tag != null && tag.isPrimal()) tag = randomAspect(server);
                if (tag != null) {
                    water = Math.max(0, water - 2);
                    aspects.remove(tag, 1);
                    if (tag.isPrimal()) ThaumcraftFlux.spill(server, worldPosition);
                    else aspects.add(tag.getComponents()[server.random.nextBoolean() ? 0 : 1], 1);
                }
            }
            changed();
        }
    }

    /** The heat sources of the original TileCrucible:99-101. */
    public static boolean isHeatSource(BlockState state) {
        // Original TileCrucible:99-101 - the heat sources are the blocks with the material fire, the blocks
        // with the material lava and the nitor. The 1.7.10 fire MATERIAL is the fire block alone, so the
        // vanilla fire block tag must not be used: a campfire or a soul campfire did not heat the crucible.
        return state.is(net.minecraft.world.level.block.Blocks.FIRE)
                || state.getFluidState().is(FluidTags.LAVA)
                || BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(NITOR);
    }

    private int countBellows() {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = worldPosition.relative(direction);
            if (level.hasChunkAt(pos) && BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(BELLOWS)) ++count;
        }
        // Unlike the furnace, original crucible counts every horizontal bellows
        // regardless of power/orientation. Never count substitute vanilla fans.
        return count;
    }

    private Aspect randomAspect(ServerLevel server) {
        return aspects.size() == 0 ? null : aspects.getAspects()[server.random.nextInt(aspects.size())];
    }

    public float getFluidHeight() {
        // Oracle TileCrucible:316-317 divides the amount by the capacity first and multiplies by the half afterwards.
        float base = .3F + .5F * (water / (float) CAPACITY);
        float height = base + tagAmount() / 100F * (1 - base);
        if (height > 1) height = 1.001F;
        if (height == 1) height = .9999F;
        return height;
    }

    public void attemptSmelt(ItemEntity entity) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || !isCurrent() || processing
                || heat <= 150 || water <= 0 || entity instanceof EntitySpecialItem || entity.isRemoved()
                || entity.level() != level || server.getEntity(entity.getUUID()) != entity || entity.getItem().isEmpty()) return;
        processing = true;
        boolean bubble = false, crafted = false;
        try {
            int remaining = entity.getItem().getCount();
            // Confirmed in the original JAR: loop offset36 compares i with the
            // shrinking local7; offsets155/357 decrement it, offset367 increments
            // i. Preserve ceil(stackSize/2) per collision, NOT whole-stack smelting.
            for (int i = 0; i < remaining; ++i) {
                if (!isCurrent() || entity.isRemoved()) break;
                var holder = CrucibleCrafting.find(server, entity, aspects);
                if (holder != null && water > 0) {
                    var recipe = holder.value();
                    ItemStack output = recipe.assemble(new CrucibleRecipeInput(entity.getItem(), aspects), server.registryAccess());
                    // Original TileCrucible:253-259 - the craft fires the player crafting
                    // event of the original for the player who threw the item, with the
                    // fake inventory of the input item as the matrix. The sticky warp of
                    // the output reaches that player through the event.
                    ServerPlayer thrower = CrucibleCrafting.thrower(server, entity);
                    if (thrower != null) {
                        SimpleContainer matrix = new SimpleContainer(1);
                        matrix.setItem(0, entity.getItem().copy());
                        PlayerEvent.CRAFT_ITEM.invoker().craft(thrower, output, matrix);
                    }
                    aspects = recipe.removeMatching(aspects);
                    // Original accepts ANY positive water, even the last <50mB.
                    water = Math.max(0, water - 50);
                    // Enqueue the paid output before discarding the catalyst:
                    // entity-remove callbacks may synchronously replace this BE.
                    // No output may vanish behind ejectItem's stale-tile guard.
                    ejectItem(output);
                    consume(entity, --remaining);
                    crafted = true;
                    counter = -250L;
                } else {
                    AspectList tags = ObjectAspectResolver.get(entity.getItem(), server);
                    if (tags == null || tags.size() == 0) {
                        entity.setDeltaMovement((server.random.nextFloat() - server.random.nextFloat()) * .2F,
                                .35F, (server.random.nextFloat() - server.random.nextFloat()) * .2F);
                        server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_PICKUP,
                                SoundSource.BLOCKS, .2F, (server.random.nextFloat() - server.random.nextFloat()) * .7F + 1);
                        // Original offset302 returns before writing local7 back:
                        // a successful craft followed by an unknown catalyst can
                        // duplicate outputs. Each earlier consume is already
                        // committed here; bounce preserves ONLY the remainder.
                        break;
                    }
                    aspects.add(tags);
                    consume(entity, --remaining);
                    bubble = true;
                    counter = -150L;
                }
            }
            if (bubble) {
                server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), BUBBLE, SoundSource.BLOCKS, .2F, 1 + server.random.nextFloat() * .4F);
                server.blockEvent(worldPosition, getBlockState().getBlock(), 2, 1);
            }
            if (crafted) server.blockEvent(worldPosition, getBlockState().getBlock(), 2, 5);
            if (bubble || crafted) changed();
        } finally { processing = false; }
    }

    private static void consume(ItemEntity entity, int remaining) {
        if (remaining <= 0) entity.discard();
        else entity.setItem(entity.getItem().copyWithCount(remaining));
    }

    public void ejectItem(ItemStack items) {
        if (!(level instanceof ServerLevel server) || !isCurrent() || !server.getServer().isSameThread() || items.isEmpty()) return;
        CrucibleActions queue = CrucibleActions.get(server);
        ItemStack remaining = items.copy();
        boolean first = true;
        // JAR ejectItem offsets4-30 copy THEN cap, fixing CFR's uninitialized
        // spitout artifact. Every split keeps output count and all components.
        while (!remaining.isEmpty()) {
            ItemStack stack = remaining.split(Math.min(remaining.getCount(), remaining.getMaxStackSize()));
            double dx = first ? 0 : (server.random.nextFloat() - server.random.nextFloat()) * .01F;
            double dz = first ? 0 : (server.random.nextFloat() - server.random.nextFloat()) * .01F;
            queue.outputs.addLast(new Output(UUID.randomUUID(), worldPosition.immutable(), stack, new Vec3(dx, .1F, dz), Long.MIN_VALUE));
            first = false;
        }
        queue.requested = true;
        queue.setDirty();
    }

    public void spillRemnants() {
        if (!(level instanceof ServerLevel server) || !isCurrent() || !server.getServer().isSameThread() || water == 0 && tagAmount() == 0) return;
        water = 0;
        for (int i = 0; i < tagAmount() / 2; ++i) ThaumcraftFlux.spill(server, worldPosition);
        aspects = new AspectList();
        changed();
        server.blockEvent(worldPosition, getBlockState().getBlock(), 2, 5);
    }

    /** Called by Block.onRemove: enqueue only, never mutate adjacent blocks/other BEs in that native transaction. */
    public void onReplaced() {
        if (!(level instanceof ServerLevel server)) return;
        int spills = tagAmount() / 2;
        if (spills > 0) {
            CrucibleActions queue = CrucibleActions.get(server);
            queue.spills.addLast(new Spill(worldPosition.immutable(), spills));
            queue.requested = true;
            queue.setDirty();
        }
        water = 0;
        aspects = new AspectList();
        setChanged();
    }

    private void changed() { setChanged(); sync(); }
    private void sync() {
        if (level != null && !level.isClientSide && !isRemoved()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public boolean triggerEvent(int id, int data) {
        if (id == 1 || id == 2) {
            if (level != null && level.isClientSide && clientEffects != null) clientEffects.event(this, id, data);
            return true;
        }
        return super.triggerEvent(id, data);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("Heat", heat);
        tag.putInt("Water", water);
        aspects.writeToNBT(tag);
        // Original counter is transient, initialized to -100 after loading.
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getShort("Heat");
        water = Math.max(0, Math.min(CAPACITY, tag.getInt("Water")));
        aspects = new AspectList();
        aspects.readFromNBT(tag);
        for (Aspect aspect : aspects.getAspects()) if (aspects.getAmount(aspect) <= 0) aspects.remove(aspect);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public static void tickOutputs(ServerLevel level) {
        CrucibleActions queue = level.getDataStorage().get(CrucibleActions.FACTORY, CrucibleActions.ID);
        if (queue != null) queue.tick(level);
    }
    public static int pendingOutputCount(ServerLevel level) {
        CrucibleActions queue = level.getDataStorage().get(CrucibleActions.FACTORY, CrucibleActions.ID);
        return queue == null ? 0 : queue.outputs.size();
    }

    private record Output(UUID id, BlockPos source, ItemStack stack, Vec3 motion, long retryAfter) {
        Output retry(long tick) { return new Output(id, source, stack, motion, tick + 20); }
    }
    private record Spill(BlockPos source, int count) {}

    /**
     * Same stable-ID/cancellation recovery as DeferredItemDrops, but retaining
     * TC4's required SPECIAL item type, exact ejection position and motion.
     * Level SavedData owns outputs even after the crucible is broken/unloaded.
     */
    private static final class CrucibleActions extends SavedData {
        private static final String ID = "thaumcraft_pending_crucible_actions";
        private static final Factory<CrucibleActions> FACTORY = new Factory<>(CrucibleActions::new, CrucibleActions::load, null);
        private final ArrayDeque<Output> outputs = new ArrayDeque<>();
        private final ArrayDeque<Spill> spills = new ArrayDeque<>();
        private final ListTag quarantined = new ListTag();
        private boolean flushing, requested;
        private long retryAfter;

        private static CrucibleActions get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(FACTORY, ID); }

        private void tick(ServerLevel level) {
            if (!level.getServer().isSameThread()) throw new IllegalStateException("Crucible delivery requires the server thread");
            if (flushing || outputs.isEmpty() && spills.isEmpty() || !requested && level.getGameTime() < retryAfter) return;
            requested = false;
            retryAfter = level.getGameTime() + 20;
            flushing = true;
            try {
                int spillCount = Math.min(64, spills.size());
                for (int index = 0; index < spillCount; ++index) {
                    Spill entry = spills.removeFirst();
                    if (!level.hasChunkAt(entry.source)) { spills.addLast(entry); continue; }
                    for (int i = 0; i < entry.count; ++i) ThaumcraftFlux.spill(level, entry.source);
                    setDirty();
                }
                int count = Math.min(64, outputs.size());
                for (int index = 0; index < count; ++index) {
                    Output entry = outputs.removeFirst();
                    if (level.getGameTime() < entry.retryAfter || !level.hasChunkAt(entry.source)) { outputs.addLast(entry); continue; }
                    var existing = level.getEntity(entry.id);
                    if (existing != null) {
                        if (existing instanceof EntitySpecialItem item && !item.isRemoved() && ItemStack.matches(item.getItem(), entry.stack)) setDirty();
                        else outputs.addLast(entry.retry(level.getGameTime()));
                        continue;
                    }
                    EntitySpecialItem output;
                    RejectedEntitySpawn.Snapshot snapshot;
                    try {
                        output = new EntitySpecialItem(ThaumcraftContent.SPECIAL_ITEM.get(), level);
                        output.setUUID(entry.id);
                        output.setPos((float) entry.source.getX() + .5F, (float) entry.source.getY() + .71F, (float) entry.source.getZ() + .5F);
                        output.setItem(entry.stack.copy());
                        output.setDeltaMovement(entry.motion);
                        snapshot = RejectedEntitySpawn.snapshotBefore(level, output);
                    } catch (RuntimeException exception) {
                        outputs.addLast(entry.retry(level.getGameTime()));
                        Thaumcraft.LOG.warn("Crucible output preparation failed at {}; retaining result", entry.source, exception);
                        continue;
                    }
                    boolean nativeAdded = false;
                    try { nativeAdded = level.addFreshEntity(output); }
                    catch (RuntimeException exception) { Thaumcraft.LOG.warn("Crucible output spawn failed at {}; retaining result", entry.source, exception); }
                    if (level.getEntity(entry.id) == output && !output.isRemoved() && ItemStack.matches(output.getItem(), entry.stack)) {
                        setDirty();
                    } else {
                        try {
                            output.discard();
                            RejectedEntitySpawn.releaseRejected(level, output, snapshot, nativeAdded);
                        } catch (RuntimeException exception) {
                            Thaumcraft.LOG.warn("Crucible rejected output cleanup failed at {}; retaining result", entry.source, exception);
                        } finally { outputs.addLast(entry.retry(level.getGameTime())); }
                    }
                }
            } finally { flushing = false; }
        }

        private static CrucibleActions load(CompoundTag tag, HolderLookup.Provider registries) {
            CrucibleActions queue = new CrucibleActions();
            if (tag.getInt("Version") != 1) {
                queue.quarantined.add(tag.copy());
                Thaumcraft.LOG.error("Unknown crucible deferred-action version; retaining data in quarantine");
                return queue;
            }
            for (String name : new String[]{"Outputs", "Spills", "Quarantined"}) {
                if (tag.contains(name) && (!(tag.get(name) instanceof ListTag rows)
                        || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)) {
                    queue.quarantined.add(tag.copy());
                    Thaumcraft.LOG.error("Malformed crucible deferred-action collection {}; retaining whole owner", name);
                    return queue;
                }
            }
            for (Tag raw : tag.getList("Quarantined", Tag.TAG_COMPOUND)) queue.quarantined.add(raw.copy());
            for (Tag raw : tag.getList("Outputs", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) raw;
                try {
                    UUID id = row.getUUID("Id");
                    int count = row.getInt("Count");
                    ItemStack stack = ItemStack.parse(registries, row.getCompound("Stack")).orElseThrow();
                    Vec3 motion = new Vec3(row.getDouble("DX"), row.getDouble("DY"), row.getDouble("DZ"));
                    if (!row.contains("Source", Tag.TAG_ANY_NUMERIC) || count <= 0 || stack.isEmpty()
                            || !Double.isFinite(motion.x) || !Double.isFinite(motion.y) || !Double.isFinite(motion.z)
                            || queue.outputs.stream().anyMatch(output -> output.id.equals(id))) throw new IllegalArgumentException("Invalid crucible result");
                    stack.setCount(count);
                    queue.outputs.addLast(new Output(id, BlockPos.of(row.getLong("Source")), stack, motion, Long.MIN_VALUE));
                } catch (RuntimeException exception) {
                    queue.quarantined.add(row.copy());
                    Thaumcraft.LOG.error("Invalid pending crucible output retained in quarantine", exception);
                }
            }
            for (Tag raw : tag.getList("Spills", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) raw;
                if (row.contains("Source", Tag.TAG_ANY_NUMERIC) && row.getInt("Count") > 0) queue.spills.addLast(new Spill(BlockPos.of(row.getLong("Source")), row.getInt("Count")));
                else queue.quarantined.add(row.copy());
            }
            return queue;
        }

        @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("Version", 1);
            ListTag results = new ListTag();
            for (Output entry : outputs) {
                CompoundTag row = new CompoundTag();
                row.putUUID("Id", entry.id);
                row.putLong("Source", entry.source.asLong());
                row.put("Stack", entry.stack.copyWithCount(1).save(registries));
                row.putInt("Count", entry.stack.getCount());
                row.putDouble("DX", entry.motion.x); row.putDouble("DY", entry.motion.y); row.putDouble("DZ", entry.motion.z);
                results.add(row);
            }
            tag.put("Outputs", results);
            ListTag pendingSpills = new ListTag();
            for (Spill entry : spills) {
                CompoundTag row = new CompoundTag();
                row.putLong("Source", entry.source.asLong());
                row.putInt("Count", entry.count);
                pendingSpills.add(row);
            }
            tag.put("Spills", pendingSpills);
            tag.put("Quarantined", quarantined.copy());
            return tag;
        }
    }
}
