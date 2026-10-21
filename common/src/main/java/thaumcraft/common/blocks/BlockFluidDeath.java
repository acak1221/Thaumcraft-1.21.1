package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
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
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * Original BlockFluidDeath: the death fluid.
 * <p>
 * The fluid is a rare, slow and glowing liquid. It holds four quanta in a source block, so it spreads three
 * blocks and dries up again, and it dissolves every living thing that touches it: the damage is the quanta of
 * the cell plus one.
 * <p>
 * 1.21.1 has no finite fluids, so the death fluid follows the native pure fluid structure: the block owns the
 * classic quanta flow and the fluid only carries its identity.
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
    /** The sides the death fluid spreads along. */
    private static final Direction[] SIDES = {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};

    public BlockFluidDeath(FlowingFluid fluid, Properties properties) {
        super(fluid, properties.lightLevel(state -> LIGHT));
    }

    @Override public MapCodec<LiquidBlock> codec() { return CODEC; }

    /** The original quants of a cell: four in the source and one less in every step away from it. */
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
        if (!isBucketCell(state)) return ItemStack.EMPTY;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return new ItemStack(ThaumcraftLegacyItems.BUCKET_DEATH.get());
    }

    /**
     * BlockFluidDeath:55-59 - the death fluid dissolves every living thing in it.
     */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
        if (!state.hasProperty(LEVEL)) return;
        living.hurt(level.damageSources().magic(), damage(state.getValue(LEVEL)));
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
        int meta = state.hasProperty(LEVEL) ? state.getValue(LEVEL) : 0;
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

    /**
     * The classic quanta flow of the death fluid: the source spreads downwards first and then sideways, and
     * every flowing cell without a stronger neighbour dries up again.
     */
    public void flowTick(Level level, BlockPos pos, BlockState state) {
        // Only a cell of the death fluid carries a level; a foreign state never does.
        if (!(state.getBlock() instanceof BlockFluidDeath)) return;
        int current = state.getValue(LEVEL);
        if (current >= QUANTA_PER_BLOCK) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (current == 0) {
            if (canFlowInto(level, pos.below())) {
                spread(level, pos.below(), 1);
                return;
            }
            for (Direction side : SIDES) {
                BlockPos target = pos.relative(side);
                if (canFlowInto(level, target)) spread(level, target, 1);
            }
            return;
        }
        int best = QUANTA_PER_BLOCK;
        for (Direction side : new Direction[]{Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH}) {
            BlockPos target = pos.relative(side);
            if (!level.hasChunkAt(target)) continue;
            BlockState neighbour = level.getBlockState(target);
            if (!(neighbour.getBlock() instanceof BlockFluidDeath)) continue;
            int neighbourLevel = neighbour.getValue(LEVEL);
            if (neighbourLevel < best) best = neighbourLevel;
        }
        int next = LegacyTileRules.deathFluidNextLevel(current, best);
        if (next == current) return;
        if (next >= QUANTA_PER_BLOCK) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        else level.setBlock(pos, withLevel(state, next), Block.UPDATE_ALL);
    }

    /**
     * The classic quanta cell of the death fluid. A cell that is not a death fluid block owns no level
     * property at all, so the death fluid block itself is placed there; a state without the level
     * property never receives one.
     */
    private void spread(Level level, BlockPos pos, int cellLevel) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockFluidDeath)) state = ThaumcraftLegacyTiles.DEATH_FLUID.get().defaultBlockState();
        BlockState target = withLevel(state, cellLevel);
        if (target == state) return;
        level.setBlock(pos, target, Block.UPDATE_ALL);
    }

    /**
     * The level a death fluid cell carries. A state that owns no level property keeps the state it had,
     * so a level is never set on a block that does not know it.
     */
    private static BlockState withLevel(BlockState state, int cellLevel) {
        int level = LegacyTileRules.deathFluidCellLevel(state.hasProperty(LEVEL), cellLevel);
        return level < 0 ? state : state.setValue(LEVEL, level);
    }

    private static boolean canFlowInto(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockFluidDeath) return state.getValue(LEVEL) > 1;
        return state.isAir();
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(this)) level.scheduleTick(pos, this, 5);
        super.onPlace(state, level, pos, oldState, moved);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        level.scheduleTick(pos, this, 5);
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        flowTick(level, pos, state);
    }

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
        @Override public int getTickDelay(LevelReader level) { return 5; }
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
            return state.isSource() ? 1F : (QUANTA_PER_BLOCK - state.getAmount()) / (float) QUANTA_PER_BLOCK;
        }
        @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
            return state.isSource() ? Shapes.block() : Shapes.box(0, 0, 0, 1, getHeight(state, level, pos), 1);
        }
        @Override public void tick(Level level, BlockPos pos, FluidState state) {
            if (level.getBlockState(pos).getBlock() instanceof BlockFluidDeath death) death.flowTick(level, pos, level.getBlockState(pos));
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
