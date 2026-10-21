package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.GrateRules;
import thaumcraft.common.lib.world.BlockFamilyShapes;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileBrainbox;
import thaumcraft.common.tiles.TileGrate;

/**
 * Original BlockMetalDevice: the metal device family. This port owns the two
 * grate variants and the brainbox; every other metadata of the original
 * family already exists as its own native block.
 */
public final class BlockMetalDevice extends BaseEntityBlock implements StateDependentBlockEntity {
    public static final MapCodec<BlockMetalDevice> CODEC = simpleCodec(BlockMetalDevice::new);
    /** Original BlockMetalDevice metadata 0..14. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);
    /** The open grate, the items of a hopper pass through it. */
    private static final VoxelShape GRATE_PLATE = BlockFamilyShapes.itemFilterPlate();
    /** The brainbox is a cube inset by three sixteenths. */
    private static final VoxelShape BRAINBOX = BlockFamilyShapes.brainbox();

    public BlockMetalDevice(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, EldritchBlockRules.DEVICE_METAL_GRATE_OPEN));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockMetalDevice:398-435 - the block entity of the owned metal variants. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int kind = EldritchBlockRules.metalDeviceKind(state.getValue(VARIANT));
        if (kind == EldritchBlockRules.DEVICE_KIND_GRATE) {
            return new TileGrate(pos, state);
        }
        if (kind == EldritchBlockRules.DEVICE_KIND_BRAINBOX) {
            return new TileBrainbox(pos, state);
        }
        return null;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    /**
     * BlockMetalDevice:264-268 - both filter metadata values keep the same selection box, the
     * metal plate across the top three sixteenths of the block.
     */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int kind = EldritchBlockRules.metalDeviceKind(state.getValue(VARIANT));
        if (kind == EldritchBlockRules.DEVICE_KIND_GRATE) {
            return GRATE_PLATE;
        }
        if (kind == EldritchBlockRules.DEVICE_KIND_BRAINBOX) return BRAINBOX;
        return Shapes.block();
    }

    /**
     * BlockMetalDevice:330-337 - the closed filter always stops what walks into it, the open
     * one stops everything that is not a dropped item, and it stops nothing at all when it is
     * asked without an entity.
     */
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int kind = EldritchBlockRules.metalDeviceKind(state.getValue(VARIANT));
        if (kind != EldritchBlockRules.DEVICE_KIND_GRATE) return super.getCollisionShape(state, level, pos, context);
        boolean closed = state.getValue(VARIANT) == EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED;
        Entity entity = context instanceof EntityCollisionContext entityContext ? entityContext.getEntity() : null;
        return GrateRules.collides(closed, entity != null, entity instanceof ItemEntity) ? GRATE_PLATE : Shapes.empty();
    }

    /** BlockMetalDevice:711-720 - the grate follows the redstone power. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        int variant = state.getValue(VARIANT);
        if (variant == EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED || variant == EldritchBlockRules.DEVICE_METAL_GRATE_OPEN) {
            boolean powered = level.hasNeighborSignal(pos);
            if (variant == EldritchBlockRules.DEVICE_METAL_GRATE_OPEN && powered) {
                level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED), Block.UPDATE_ALL);
            } else if (variant == EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED && !powered) {
                level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.DEVICE_METAL_GRATE_OPEN), Block.UPDATE_ALL);
            }
            return;
        }
        if (variant == EldritchBlockRules.DEVICE_METAL_BRAINBOX && level.getBlockEntity(pos) instanceof TileBrainbox brainbox) {
            // BlockMetalDevice:487-491 - a brainbox without a machine in front breaks itself.
            Direction facing = brainbox.getFacing();
            if (facing != null && level.getBlockState(pos.relative(facing)).isAir()) {
                level.destroyBlock(pos, true);
            }
        }
    }

    /** The open grate lets items fall through, the closed one holds them. */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (state.getValue(VARIANT) != EldritchBlockRules.DEVICE_METAL_GRATE_OPEN) return;
        if (!(entity instanceof ItemEntity item) || item.isRemoved()) return;
        if (item.getY() < pos.getY() + 0.8125D) return;
        item.setDeltaMovement(item.getDeltaMovement().x, -0.1D, item.getDeltaMovement().z);
    }
}
