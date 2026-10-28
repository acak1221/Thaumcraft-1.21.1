/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.block.camo;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.wands.WandHelper;

/**
 * A camouflageable device that has a facing and acts on a redstone pulse (the Remote Placer and the Transvector
 * Dislocator; in 1.7.10 two copies of the same code).
 *
 * <ul>
 *   <li>The facing ({@code orientation} of the tile in 1.7.10, a number 0..5 in the order down, up, north, south, west,
 *       east) is a block state property; the model shows a different face on the side it points to.</li>
 *   <li>It is set from where the player stands when placing ({@code BlockPistonBase.determineOrientation}) and with a
 *       click of a wand, which turns the device to face the side that was clicked.</li>
 *   <li>A rising redstone edge (power at the block or at the block above) sets the {@code powered} property, without
 *       telling the clients, and schedules a tick in one game tick; the tick runs {@link RedstonePulseReceiver}.</li>
 * </ul>
 */
public abstract class PulseDeviceBlock extends CamoDeviceBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** 1.7.10 metadata 1 ("on"): a pulse is being processed. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected PulseDeviceBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LivingEntity placer = context.getPlayer();
        Direction facing = placer == null ? Direction.NORTH : determineOrientation(context.getClickedPos(), placer);
        return defaultBlockState().setValue(FACING, facing);
    }

    /**
     * {@code BlockPistonBase.determineOrientation}: the device faces the placing entity. Within two blocks (horizontally)
     * of the device, an entity standing high above it makes it face up and one below it makes it face down; otherwise it
     * is the horizontal direction opposite to where the entity looks (north when the entity looks south, west when it
     * looks east, ...).
     */
    public static Direction determineOrientation(BlockPos pos, LivingEntity entity) {
        return determineOrientation(pos, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot());
    }

    /** {@link #determineOrientation(BlockPos, LivingEntity)} for an entity at the given feet position and yaw. */
    public static Direction determineOrientation(BlockPos pos, double entityX, double entityY, double entityZ, float yaw) {
        if (Mth.abs((float) entityX - (float) pos.getX()) < 2.0F && Mth.abs((float) entityZ - (float) pos.getZ()) < 2.0F) {
            double eyeHeight = entityY + 1.82D;
            if (eyeHeight - pos.getY() > 2.0D) return Direction.UP;
            if (pos.getY() - eyeHeight > 0.0D) return Direction.DOWN;
        }
        int l = Mth.floor((double) (yaw * 4.0F / 360.0F) + 0.5D) & 3;
        return l == 0 ? Direction.NORTH : l == 1 ? Direction.EAST : l == 2 ? Direction.SOUTH : Direction.WEST;
    }

    /** A wand turns the device to face the clicked side. */
    @Nullable
    @Override
    protected ItemInteractionResult useSpecialItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!WandHelper.isWand(stack)) return null;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(FACING, hit.getDirection()), Block.UPDATE_ALL);
            level.playSound(null, pos, DeviceSounds.WAND_TOOL, SoundSource.BLOCKS, 0.6F, 1F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide) return;

        boolean power = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean on = state.getValue(POWERED);

        if (power && !on) {
            level.scheduleTick(pos, this, 1);
            level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_INVISIBLE);
        } else if (!power && on) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_INVISIBLE);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RedstonePulseReceiver receiver) {
            receiver.receiveRedstonePulse();
        }
    }
}
