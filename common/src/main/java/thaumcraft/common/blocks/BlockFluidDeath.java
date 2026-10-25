package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.tiles.LegacyTileRules;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;

/**
 * Original BlockFluidDeath: the death fluid.
 * <p>
 * The fluid is a rare, slow and glowing finite liquid. A bucket contains four quanta, which move downwards
 * or are shared with lower adjacent cells; a full cell is not an inexhaustible source. Contact damage is
 * the quanta of the cell (the original metadata plus one).
 * <p>
 * 1.21.1 has no finite fluids. The block owns the original Forge finite flow; the native fluid carries its
 * identity and physical immersion height. Only the block scheduler advances the finite stock.
 * <p>
 * The block owns the level of a cell, so a cell only ever receives a level it owns: a cell that is not a death
 * fluid block has no level property at all and the death fluid block is placed there instead.
 * <p>
 * The original fluid was a rare liquid of the luminosity eight and the viscosity of one thousand five hundred
 * ({@code ConfigBlocks.FLUIDDEATH}). 1.21.1 has no viscosity of its own, so the death fluid keeps the native
 * liquid movement and never pushes an entity.
 * <p>
 * The original death fluid was built over {@code Material.water} (oracle BlockFluidDeath:39,
 * {@code super(ConfigBlocks.FLUIDDEATH, Material.field_151586_h)}; {@code Material.field_151586_h} is
 * {@code Material.water}, see the readable 1.7.10 reference {@code RenderEventHandler:367}), so a 1.7.10
 * entity that stood in it was an entity in water: it swam, it took no fall damage and it drowned after the
 * air supply was gone. The native port therefore carries the death fluid in the vanilla
 * {@code minecraft:water} tag ({@code data/minecraft/tags/fluid/water.json}), which is the 1.21.1
 * counterpart of that material. The drowning of the original is unreachable in practice: the cell damages
 * every living thing by its quanta, so a player dies in five ticks in the source cell, and the original
 * drowned in the same way.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockFluidDeath.java}, lines 33-81 and
 * {@code thaumcraft/common/config/ConfigBlocks.java}:166.
 */
public final class BlockFluidDeath extends LiquidBlock {
    private static final Codec<FlowingFluid> DEATH_CODEC = BuiltInRegistries.FLUID.byNameCodec().comapFlatMap(
            fluid -> fluid instanceof DeathFluid death ? DataResult.success(death) : DataResult.error(() -> "Death fluid required"),
            fluid -> fluid);
    public static final MapCodec<LiquidBlock> CODEC = RecordCodecBuilder.<LiquidBlock>mapCodec(instance -> instance.group(
            DEATH_CODEC.fieldOf("fluid").forGetter(block -> ((BlockFluidDeath) block).fluid),
            propertiesCodec()).apply(instance, BlockFluidDeath::new));
    /** The four quanta of a death fluid source block. */
    public static final int QUANTA_PER_BLOCK = LegacyTileRules.DEATH_FLUID_QUANTA;
    /** The light the death fluid gives off. */
    public static final int LIGHT = 8;
    /** Forge BlockFluidBase: viscosity1500 /200, with integer division. */
    public static final int TICK_DELAY = 7;
    /** The sides the death fluid spreads along. */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public BlockFluidDeath(FlowingFluid fluid, Properties properties) {
        super(fluid, properties.lightLevel(state -> LIGHT));
    }

    @Override public MapCodec<LiquidBlock> codec() { return CODEC; }

    /** The original quantity of a cell, translated from the native level encoding. */
    public static int quanta(int level) { return LegacyTileRules.deathFluidQuanta(level); }

    /** The damage the death fluid does in a cell: the quanta of the cell. */
    public static int damage(int level) { return LegacyTileRules.deathFluidDamage(level); }

    /**
     * The metadata the original bucket of death pours and the original refill event demands
     * (oracle ItemBucketDeath:103 and EventHandlerWorld:216-223): the metadata three, which the original
     * finite fluid uses for the cell that still holds all four quanta, the source cell.
     */
    public static final int BUCKET_CELL_METADATA = LegacyTileRules.DEATH_FLUID_BUCKET_METADATA;

    /**
     * The native level of the original bucket cell: the source cell of the native liquid level property.
     * The original stored the quanta of a cell in its metadata (metadata = quanta - 1), so the original
     * metadata three is the native level zero.
     */
    public static int bucketCellLevel() { return LegacyTileRules.deathFluidLevelForOriginalMetadata(BUCKET_CELL_METADATA); }

    /** The state the original bucket of death pours: the cell whose original metadata is three. */
    public static BlockState bucketCellState() {
        return ThaumcraftLegacyTiles.DEATH_FLUID.get().defaultBlockState();
    }

    /**
     * Whether the state is the original bucket cell: a death fluid block whose original metadata is three,
     * the source cell the original bucket of death pours and the original refill event demands.
     */
    public static boolean isBucketCell(BlockState state) {
        if (state == null || !(state.getBlock() instanceof BlockFluidDeath)) return false;
        if (!state.hasProperty(LEVEL)) return false;
        return LegacyTileRules.deathFluidOriginalMetadata(state.getValue(LEVEL)) == BUCKET_CELL_METADATA;
    }

    @Override protected FluidState getFluidState(BlockState state) {
        // A state of another block owns no level at all, so it never carries one.
        if (!state.hasProperty(LEVEL)) return Fluids.EMPTY.defaultFluidState();
        int level = state.getValue(LEVEL);
        if (level <= 0) return fluid.getSource(false);
        if (level < QUANTA_PER_BLOCK) return fluid.getFlowing(QUANTA_PER_BLOCK - level, false);
        return Fluids.EMPTY.defaultFluidState();
    }

    /**
     * The original refill of the bucket of death (oracle EventHandlerWorld:216-223): the cell whose original
     * metadata is three is removed and the bucket of death is the result.
     *
     * <p>The native liquid pickup would remove the source cell and hand out the AIR bucket of the death fluid
     * (the original death fluid has no bucket item of its own), so the cell would be lost without a result.
     * The bucket of death is therefore the pickup of that one cell.</p>
     */
    @Override public ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        return thaumcraft.common.lib.fluid.DeathBucketActions.capture(player, level, pos, state);
    }

    /**
     * BlockFluidDeath:55-59 - the death fluid dissolves every living thing in it.
     */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
        if (!state.hasProperty(LEVEL)) return;
        living.hurt(DamageSourceThaumcraft.dissolve(level), damage(state.getValue(LEVEL)));
    }

    /**
     * The original death fluid is a liquid of {@code Material.water} and does not collide with an entity.
     * The native cell is in the vanilla water tag, so the 1.21.1 engine owns the swim, the fall damage
     * and the drowning of an entity that stands in it, exactly like the 1.7.10 material did.
     */
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * The original random display tick of the death fluid: a slimy bubble over the cell and, in one
     * of fifty, the pop of the lava (oracle BlockFluidDeath:65-80).
     */
    @FunctionalInterface public interface ClientEffects { void slimyBubble(Level level, double x, double y, double z, float size, float alpha); }
    private static ClientEffects clientEffects = (level, x, y, z, size, alpha) -> {};

    /** The client effect of the slimy bubble of the death fluid. */
    public static void setClientEffects(ClientEffects effects) { clientEffects = java.util.Objects.requireNonNull(effects); }

    /**
     * Oracle BlockFluidDeath:65-80 - the random display tick of a cell of the death fluid draws the
     * slimy bubble at the height of the cell and plays the lava pop in one of fifty rolls.
     */
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int meta = state.hasProperty(LEVEL) ? LegacyTileRules.deathFluidOriginalMetadata(state.getValue(LEVEL)) : 0;
        float h = random.nextFloat() * 0.075F;
        clientEffects.slimyBubble(level, pos.getX() + random.nextFloat(), pos.getY() + 0.1F + 0.225F * meta,
                pos.getZ() + random.nextFloat(), 0.075F + h, 0.8F);
        if (random.nextInt(50) == 0) {
            double x = pos.getX() + random.nextFloat();
            double y = pos.getY() + 1.0D;
            double z = pos.getZ() + random.nextFloat();
            level.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.LAVA_POP,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.1F + random.nextFloat() * 0.1F,
                    0.9F + random.nextFloat() * 0.15F, false);
        }
    }

    /** Forge1.7 finite4: vertical transfer, then strict-lower N/S/W/E sharing and original remainder RNG. */
    public void flowTick(Level level, BlockPos pos, BlockState state) {
        if (!state.is(this)) return;
        // The existing finite-fluid writer installs every balance before notifications and
        // fences the seven-cell footprint against synchronous reentry. It is capacity-independent.
        try (var step = FluxFiniteBlock.begin(level, pos, state)) {
            if (step == null) return;
            int before = quanta(state.getValue(LEVEL));
            if (before == 0) {
                step.commit(step.change(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
                return;
            }
            int remaining = flowDown(step, before);
            if (remaining <= 1 || !step.coherent()) return;

            for (Direction side : SIDES) {
                BlockPos target = pos.relative(side);
                if (displace(step, target)) step.commit(step.change(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
                if (!step.coherent()) return;
            }
            int[] neighbors = new int[4];
            for (int i = 0; i < SIDES.length; i++) neighbors[i] = quantity(step.observe(pos.relative(SIDES[i])));
            int[] shared = LegacyTileRules.deathFluidDistribution(remaining, neighbors, step.level.random::nextInt);
            var changes = new ArrayList<FluxFiniteBlock.Change>();
            for (int i = 0; i < SIDES.length; i++) if (neighbors[i] >= 0 && neighbors[i] != shared[i + 1])
                changes.add(step.change(pos.relative(SIDES[i]), amountState(shared[i + 1]), Block.UPDATE_CLIENTS));
            if (shared[0] != remaining) changes.add(step.change(pos, amountState(shared[0]), Block.UPDATE_CLIENTS));
            if (!changes.isEmpty() && step.commit(changes.toArray(FluxFiniteBlock.Change[]::new))) {
                for (var change : changes) schedule(step.level, change.pos());
            }
        }
    }

    private int flowDown(FluxFiniteBlock.Step step, int amount) {
        BlockPos target = step.pos.below();
        if (step.level.isOutsideBuildHeight(target)) {
            // Original finite fluids leave the world at its vertical build boundary.
            step.commit(step.change(step.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            return 0;
        }
        BlockState other = step.observe(target);
        if (other == null) return amount;
        int otherAmount = quantity(other);
        if (otherAmount >= 0 && otherAmount < QUANTA_PER_BLOCK) {
            int total = amount + otherAmount;
            int remaining = Math.max(0, total - QUANTA_PER_BLOCK);
            if (step.commit(step.change(target, amountState(Math.min(total, QUANTA_PER_BLOCK)), Block.UPDATE_ALL),
                    step.change(step.pos, amountState(remaining), Block.UPDATE_ALL))) {
                schedule(step.level, target);
                if (remaining > 0) schedule(step.level, step.pos);
                return remaining;
            }
            return 0;
        }
        if (other.getBlock() instanceof FluxFiniteBlock flux && flux.density() < 1000) {
            if (step.commit(step.change(target, amountState(amount), Block.UPDATE_ALL),
                    step.change(step.pos, other, Block.UPDATE_ALL))) {
                schedule(step.level, target);
                step.schedule(step.pos, flux);
            }
            return 0;
        }
        if (displace(step, target)) {
            if (step.commit(step.change(target, amountState(amount), Block.UPDATE_ALL),
                    step.change(step.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))) schedule(step.level, target);
            return 0;
        }
        return amount;
    }

    private int quantity(BlockState state) {
        return state == null ? -1 : state.isAir() ? 0 : state.is(this) ? quanta(state.getValue(LEVEL)) : -1;
    }

    private BlockState amountState(int amount) {
        if (amount < 0 || amount > QUANTA_PER_BLOCK) throw new IllegalArgumentException("Invalid death-fluid quantity " + amount);
        return amount == 0 ? Blocks.AIR.defaultBlockState() : defaultBlockState().setValue(LEVEL, QUANTA_PER_BLOCK - amount);
    }

    /** Reuses the already mapped original BlockFluidBase material/density admission. */
    private boolean displace(FluxFiniteBlock.Step step, BlockPos pos) {
        BlockState state = step.observe(pos);
        if (state == null || state.is(this) || !BlockFluidPure.canDisplace(step.level, pos)) return false;
        var owner = step.level.getBlockEntity(pos);
        if (owner instanceof Container inventory && !inventory.isEmpty()) return false;
        if (!state.isAir()) Block.dropResources(state, step.level, pos, owner);
        return step.coherent() && step.level.getBlockEntity(pos) == owner;
    }

    private void schedule(LevelAccessor level, BlockPos pos) {
        if (level instanceof ServerLevel server && FluxFiniteBlock.loaded(server, pos)
                && server.getBlockState(pos).is(this)) server.scheduleTick(pos, this, TICK_DELAY);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(this)) schedule(level, pos);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        schedule(level, pos);
    }

    @Override protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        schedule(level, pos);
        return state;
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        flowTick(level, pos, state);
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        flowTick(level, pos, state);
    }

    @Override protected boolean isRandomlyTicking(BlockState state) { return true; }

    /**
     * The death fluid itself: a rare liquid that glows, is very slow and never becomes a source again.
     */
    public abstract static class DeathFluid extends FlowingFluid {
        @Override public Fluid getSource() { return ThaumcraftLegacyTiles.DEATH_FLUID_SOURCE.get(); }
        @Override public Fluid getFlowing() { return ThaumcraftLegacyTiles.DEATH_FLUID_FLOWING.get(); }
        @Override public Item getBucket() { return Items.AIR; }
        @Override public boolean isSame(Fluid other) { return other instanceof DeathFluid; }
        @Override protected boolean canConvertToSource(Level level) { return false; }
        @Override protected float getExplosionResistance() { return 100F; }
        @Override public int getTickDelay(LevelReader level) { return TICK_DELAY; }
        @Override public int getSlopeFindDistance(LevelReader level) { return 4; }
        @Override public int getDropOff(LevelReader level) { return 1; }
        @Override public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
            return direction == Direction.DOWN && !other.is(FluidTags.WATER);
        }
        @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            Block.dropResources(state, level, pos, level.getBlockEntity(pos));
        }
        @Override public BlockState createLegacyBlock(FluidState state) {
            BlockState block = ThaumcraftLegacyTiles.DEATH_FLUID.get().defaultBlockState();
            if (state.isSource()) return block.setValue(LiquidBlock.LEVEL, 0);
            int level = Math.max(0, Math.min(QUANTA_PER_BLOCK - state.getAmount(), QUANTA_PER_BLOCK - 1));
            return block.setValue(LiquidBlock.LEVEL, level);
        }
        @Override public float getOwnHeight(FluidState state) {
            // Forge BlockFluidBase.getFilledPercentage adds one quantum, then caps at full height.
            return Math.min(1F, (state.getAmount() + 1F) / QUANTA_PER_BLOCK);
        }
        @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
            return state.isSource() ? Shapes.block() : Shapes.box(0, 0, 0, 1, getHeight(state, level, pos), 1);
        }
        @Override public void tick(Level level, BlockPos pos, FluidState state) {
            // BlockFluidFinite schedules the BLOCK. The native fluid scheduler must not
            // advance the same stock a second time or invalidate a pending tick on level changes.
        }
    }

    /** The source of the death fluid. */
    public static final class Source extends DeathFluid {
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return QUANTA_PER_BLOCK; }
    }

    /** The flowing death fluid. */
    public static final class Flowing extends DeathFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public boolean isSource(FluidState state) { return false; }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }
}
