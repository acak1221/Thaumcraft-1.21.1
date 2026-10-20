package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.tiles.LegacyTileRules;
import thaumcraft.common.tiles.TileArcaneFurnace;

/**
 * Original BlockArcaneFurnace: the three by three by three arcane furnace.
 * <p>
 * The furnace is one block with eleven metadata values. The core is the metadata zero block in the middle of
 * the structure; it holds the thirty two slots of the furnace and ejects its results towards the output
 * nozzle. The four blocks around the core in the same layer are essentia nozzles and the rest of the shell is
 * plain. When any of the twenty five blocks around the core is no longer part of the furnace the whole
 * structure collapses into the original blocks and a blaze is released.
 * <p>
 * 1.21.1 has no block metadata, so the eleven values became the {@code variant} block state and the output
 * nozzle became its own block, {@link BlockArcaneFurnaceNozzle}, because its collision box depends on the
 * block it is attached to.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockArcaneFurnace.java}, lines 58-402.
 */
public final class BlockArcaneFurnace extends BaseEntityBlock {
    public static final MapCodec<BlockArcaneFurnace> CODEC = simpleCodec(BlockArcaneFurnace::new);
    /** The eleven original metadata values, flattened into a block state. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 9);
    /** The render level of a shell block, worked out from the blocks above and below it. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 18);
    /** The core of the furnace is a quarter high block. */
    public static final VoxelShape CORE_SHAPE = Shapes.box(0, 0, 0, 16, 4, 16);

    public BlockArcaneFurnace(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0).setValue(LEVEL, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, LEVEL);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int variant = state.getValue(VARIANT);
        if (variant == 0) return new TileArcaneFurnace(pos, state);
        if (variant == 2 || variant == 4 || variant == 5 || variant == 6 || variant == 8) {
            return new thaumcraft.common.tiles.TileArcaneFurnaceNozzle(pos, state);
        }
        return null;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftLegacyTiles.ARCANE_FURNACE_BLOCK_ENTITY.get(), TileArcaneFurnace::tick);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VARIANT) == 0 ? CORE_SHAPE : Shapes.block();
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(VARIANT) == 0 ? CORE_SHAPE : Shapes.block();
    }

    /** BlockArcaneFurnace:215-221 - the core lights up to thirteen; the properties carry that. */
    @Override protected boolean isRandomlyTicking(BlockState state) { return state.getValue(VARIANT) == 0; }

    /** BlockArcaneFurnace:368-377 - smoke rises from the core when water stands on it. */
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(VARIANT) != 0) return;
        BlockPos above = pos.above();
        if (!level.hasChunkAt(above)) return;
        BlockState over = level.getBlockState(above);
        if (!over.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) return;
        for (int round = 0; round < 3; round++) {
            double x = pos.getX() + random.nextFloat();
            double y = pos.getY() + 1D + random.nextFloat() * .5F;
            double z = pos.getZ() + random.nextFloat();
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x, y, z, 0D, 0D, 0D);
        }
    }

    /** BlockArcaneFurnace:257-283 - the mouth of the furnace swallows dropped items and burns the living. */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(VARIANT) != 0) return;
        double x = entity.getX(), z = entity.getZ();
        if (x < pos.getX() + .3D) entity.push(.0001D, 0D, 0D);
        if (x > pos.getX() + .7D) entity.push(-.0001D, 0D, 0D);
        if (z < pos.getZ() + .3D) entity.push(0D, 0D, .0001D);
        if (z > pos.getZ() + .7D) entity.push(0D, 0D, -.0001D);
        if (entity instanceof ItemEntity item) {
            item.setDeltaMovement(item.getDeltaMovement().x, .025F, item.getDeltaMovement().z);
            if (!level.isClientSide && item.onGround() && level.getBlockEntity(pos) instanceof TileArcaneFurnace furnace) {
                ItemStack stack = item.getItem();
                if (!stack.isEmpty() && furnace.addItemsToInventory(stack.copy())) item.discard();
            }
            return;
        }
        if (entity instanceof LivingEntity living && !living.isInvulnerableTo(level.damageSources().hotFloor())) {
            living.hurt(level.damageSources().hotFloor(), 3F);
            living.setRemainingFireTicks(200);
        }
    }

    /** BlockArcaneFurnace:301-319 - the furnace collapses when a block of the shell is missing. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
        if (level.isClientSide) return;
        updateLevel(level, pos, state);
        if (state.getValue(VARIANT) != 0) return;
        if (!structureIntact(level, pos)) collapse(level, pos);
    }

    /** The render level of a shell block follows the blocks above and below it. */
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) updateLevel(level, pos, state);
    }

    /**
     * BlockArcaneFurnace:321-331 - breaking the core releases the blaze and collapses the furnace.
     */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (!level.isClientSide && state.getValue(VARIANT) == 0) collapse(level, pos);
        return result;
    }

    /** The core always collapses the furnace when it is removed. */
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && state.getValue(VARIANT) == 0 && !replacement.is(this)) collapse(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }

    /** BlockArcaneFurnace:194-213 - the render level of a shell block. */
    public static int calculateLevel(Level level, BlockPos pos, int variant) {
        BlockPos above = pos.above(), below = pos.below();
        BlockState aboveState = level.hasChunkAt(above) ? level.getBlockState(above) : null;
        BlockState belowState = level.hasChunkAt(below) ? level.getBlockState(below) : null;
        boolean aboveIsFurnace = aboveState != null && aboveState.getBlock() instanceof BlockArcaneFurnace;
        boolean belowIsFurnace = belowState != null && belowState.getBlock() instanceof BlockArcaneFurnace;
        int metaAbove = aboveIsFurnace ? aboveState.getValue(VARIANT) : 0;
        int metaBelow = belowIsFurnace ? belowState.getValue(VARIANT) : 0;
        return LegacyTileRules.furnaceLevel(variant, metaAbove, metaBelow, aboveIsFurnace, belowIsFurnace);
    }

    private static void updateLevel(Level level, BlockPos pos, BlockState state) {
        int level0 = calculateLevel(level, pos, state.getValue(VARIANT));
        if (level0 != state.getValue(LEVEL)) level.setBlock(pos, state.setValue(LEVEL, level0), Block.UPDATE_CLIENTS);
    }

    /**
     * BlockArcaneFurnace:301-319 - the twenty five blocks around the core have to belong to the furnace.
     */
    public static boolean structureIntact(Level level, BlockPos core) {
        return LegacyTileRules.structureIntact(cell -> {
            int dx = cell / 100 - 1;
            int dy = cell % 100 / 10 - 1;
            int dz = cell % 10 - 1;
            BlockPos target = core.offset(dx, dy, dz);
            if (!level.hasChunkAt(target)) return false;
            BlockState state = level.getBlockState(target);
            return state.getBlock() instanceof BlockArcaneFurnace || state.getBlock() instanceof BlockArcaneFurnaceNozzle;
        });
    }

    /**
     * BlockArcaneFurnace:285-299 - the furnace collapses into the original blocks and releases a blaze.
     */
    public static void collapse(Level level, BlockPos core) {
        if (level.isClientSide || !(level instanceof ServerLevel server)) return;
        releaseBlaze(server, core);
        // BlockContainer:breakBlock - the furnace keeps its contents until it is broken.
        if (server.getBlockEntity(core) instanceof TileArcaneFurnace furnace) furnace.dropContents(server);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos target = core.offset(dx, dy, dz);
                    if (!server.hasChunkAt(target)) continue;
                    BlockState state = server.getBlockState(target);
                    Block block = state.getBlock();
                    if (block instanceof BlockArcaneFurnace furnace) {
                        server.setBlock(target, restoredState(furnace, state.getValue(VARIANT)), Block.UPDATE_ALL);
                    } else if (block instanceof BlockArcaneFurnaceNozzle nozzle) {
                        server.setBlock(target, restoredState(nozzle, 10), Block.UPDATE_ALL);
                    }
                }
            }
        }
        server.sendBlockUpdated(core, server.getBlockState(core), server.getBlockState(core), Block.UPDATE_ALL);
    }

    /** BlockArcaneFurnace:347-349 - the block a broken piece of the furnace leaves behind. */
    public static BlockState restoredState(Block block, int variant) {
        return switch (LegacyTileRules.restoredBlock(variant)) {
            case AIR -> net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            case IRON_BARS -> net.minecraft.world.level.block.Blocks.IRON_BARS.defaultBlockState();
            case OBSIDIAN -> net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState();
            case NETHER_BRICK -> net.minecraft.world.level.block.Blocks.NETHER_BRICKS.defaultBlockState();
        };
    }

    /** BlockArcaneFurnace:321-331 - breaking the core releases a blaze with regeneration and resistance. */
    private static void releaseBlaze(ServerLevel level, BlockPos core) {
        if (!(level.getBlockEntity(core) instanceof TileArcaneFurnace furnace)) return;
        if (furnace.blazeReleased()) return;
        furnace.markBlazeReleased();
        Blaze blaze = EntityType.BLAZE.create(level);
        if (blaze == null) return;
        blaze.moveTo(core.getX() + .5D, core.getY() + 1D, core.getZ() + .5D, 0F, 0F);
        blaze.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 6000, 2));
        blaze.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 12000, 0));
        level.addFreshEntity(blaze);
    }

    /** BlockArcaneFurnace:393-396 - the furnace is not a pathfinding obstacle. */
}
