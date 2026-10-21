package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.BlockFamilyShapes;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileFluxScrubber;
import thaumcraft.common.tiles.TileWandPedestal;

/**
 * Original BlockStoneDevice: the stone device family. This port owns the wand
 * pedestal and the flux scrubber; every other metadata of the original family
 * already exists as its own native block.
 */
public final class BlockStoneDevice extends BaseEntityBlock {
    public static final MapCodec<BlockStoneDevice> CODEC = simpleCodec(BlockStoneDevice::new);
    /** Original BlockStoneDevice metadata 0..14. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);
    /** The wand pedestal is a small pillar in the middle of the block. */
    private static final VoxelShape WAND_PEDESTAL = BlockFamilyShapes.wandPedestal();
    /** BlockStoneDevice:449-461 - the pedestal stands on a base and carries the focus in the middle. */
    private static final VoxelShape WAND_PEDESTAL_COLLISION = BlockFamilyShapes.wandPedestalCollision();

    public BlockStoneDevice(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, EldritchBlockRules.DEVICE_STONE_WAND_PEDESTAL));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockStoneDevice:257-289 - the block entity of the owned stone variants. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int kind = EldritchBlockRules.stoneDeviceKind(state.getValue(VARIANT));
        if (kind == EldritchBlockRules.DEVICE_KIND_WAND_PEDESTAL) {
            return new TileWandPedestal(pos, state);
        }
        if (kind == EldritchBlockRules.DEVICE_KIND_FLUX_SCRUBBER) {
            return new TileFluxScrubber(pos, state);
        }
        return null;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, tile) -> {
                    if (tile instanceof TileFluxScrubber scrubber) TileFluxScrubber.tick(lvl, pos, st, scrubber);
                    else if (tile instanceof TileWandPedestal pedestal) TileWandPedestal.tick(lvl, pos, st, pedestal);
                };
    }

    /** BlockStoneDevice:464-480 - the wand pedestal is the half wide column in the middle. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (EldritchBlockRules.stoneDeviceKind(state.getValue(VARIANT)) == EldritchBlockRules.DEVICE_KIND_WAND_PEDESTAL) {
            return WAND_PEDESTAL;
        }
        return Shapes.block();
    }

    /** BlockStoneDevice:449-461 - the pedestal stands on a base and carries the focus in the middle. */
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (EldritchBlockRules.stoneDeviceKind(state.getValue(VARIANT)) == EldritchBlockRules.DEVICE_KIND_WAND_PEDESTAL) {
            return WAND_PEDESTAL_COLLISION;
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    /** BlockStoneDevice - the wand pedestal keeps the original facing of the placer. */
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(VARIANT) != EldritchBlockRules.DEVICE_STONE_WAND_PEDESTAL) return;
        if (!(level.getBlockEntity(pos) instanceof TileWandPedestal pedestal)) return;
        pedestal.setFacing(placer.getDirection());
        pedestal.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }
}
