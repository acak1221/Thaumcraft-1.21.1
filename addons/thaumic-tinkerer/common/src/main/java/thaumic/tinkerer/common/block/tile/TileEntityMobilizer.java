package thaumic.tinkerer.common.block.tile;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.block.mobilizer.BlockMobilizer;
import thaumic.tinkerer.common.content.TileBlocks;

/**
 * The Levitational Locomotive: linked to a pair of relays it slides along the beam between them, one block every five
 * seconds, carrying the block on top of it, and turns round when something is in its way (1.7.10
 * {@code TileEntityMobilizer}; the timing and the order of the steps are the original's).
 */
public class TileEntityMobilizer extends BlockEntity {

    public boolean linked;

    public int firstRelayX;
    public int secondRelayX;

    public int firstRelayZ;
    public int secondRelayZ;
    /** Set when the block is gone, so a block entity that is still being ticked does not act ("ghost" check). */
    public boolean dead = false;

    /** 1.7.10 {@code ForgeDirection}; the saved value is its ordinal, which is {@link Direction#get3DDataValue()} here. */
    public Direction movementDirection = Direction.DOWN;

    public TileEntityMobilizer(BlockPos pos, BlockState state) {
        super(TileBlocks.mobilizerType(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);

        nbt.putBoolean("Linked", linked);

        nbt.putInt("FirstRelayX", firstRelayX);
        nbt.putInt("FirstRelayZ", firstRelayZ);

        nbt.putInt("SecondRelayX", secondRelayX);
        nbt.putInt("SecondRelayZ", secondRelayZ);

        nbt.putInt("Direction", movementDirection != null ? movementDirection.get3DDataValue() : 0);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);

        this.linked = nbt.getBoolean("Linked");

        this.firstRelayX = nbt.getInt("FirstRelayX");
        this.firstRelayZ = nbt.getInt("FirstRelayZ");

        this.secondRelayX = nbt.getInt("SecondRelayX");
        this.secondRelayZ = nbt.getInt("SecondRelayZ");

        movementDirection = Direction.from3DDataValue(nbt.getInt("Direction"));
    }

    @Override
    public void setRemoved() {
        dead = true;
        super.setRemoved();
    }

    public void verifyRelay() {
        BlockPos firstPos = new BlockPos(firstRelayX, worldPosition.getY(), firstRelayZ);
        if (level == null || !level.hasChunkAt(firstPos)) return;
        BlockEntity te = level.getBlockEntity(firstPos);
        if (te instanceof TileEntityRelay relay) {
            relay.verifyPartner();
        }
        if (!(linked && te instanceof TileEntityRelay relay && relay.partnerX == this.secondRelayX
                && relay.partnerZ == this.secondRelayZ)) {
            linked = false;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMobilizer mobilizer) {
        if (level instanceof ServerLevel serverLevel) mobilizer.tick(serverLevel);
    }

    /** {@code updateEntity}: check the relays, turn round at the end of the track, and move. */
    private void tick(ServerLevel level) {
        // Check for ghost TEs
        if (dead) return;
        // Make sure the relays haven't been broken
        verifyRelay();
        long time = level.getGameTime();
        // Bounce
        if (linked && time % 100 == 0 && !level.hasNeighborSignal(worldPosition)) {
            // Target coordinates to check
            BlockPos target = worldPosition.offset(movementDirection.getStepX(), 0, movementDirection.getStepZ());
            if (level.hasChunkAt(target)) {
                // Switch direction if at end of track
                if (!level.getBlockState(target).isAir() || !level.getBlockState(target.above()).isAir()) {
                    movementDirection = movementDirection.getOpposite();
                    setChanged();
                }
            }
        }
        // Move
        if (linked && time % 100 == 1 && !level.hasNeighborSignal(worldPosition)) {
            move(level);
        }
    }

    private void move(ServerLevel level) {
        BlockPos target = worldPosition.offset(movementDirection.getStepX(), 0, movementDirection.getStepZ());
        if (!level.hasChunkAt(target)) return;
        // Check for abandoned TEs
        if (!(level.getBlockState(worldPosition).getBlock() instanceof BlockMobilizer)) return;
        // Check if the space the mobilizer will move into is empty
        if (!level.getBlockState(target).isAir()) return;

        BlockPos above = worldPosition.above();
        BlockPos targetAbove = target.above();
        BlockPos oldPos = worldPosition;

        // Move the block on top of the mobilizer
        BlockEntity passenger = level.getBlockEntity(above);
        BlockState passengerState = level.getBlockState(above);

        // Prevent the passenger from popping off. Not sent to clients.
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 0);
        // Move non-TE blocks
        if (level.isEmptyBlock(above) || canPlaceAt(level, passengerState, targetAbove)) {
            if (passenger == null) {
                if (!passengerState.is(Blocks.BEDROCK)) {
                    level.setBlock(targetAbove, passengerState, Block.UPDATE_ALL);
                    if (!passengerState.isAir() && !passengerState.is(Blocks.MOVING_PISTON)) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            } else if (isVanillaBlockEntity(passenger)) {
                // 1.7.10 moved the tile entities of the vanilla package; the add-on moves modded ones only through AE2.
                moveBlockEntity(level, passenger, passengerState, above, targetAbove);
            }
        }
        // Move self
        BlockState self = level.getBlockState(oldPos);
        level.setBlock(oldPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(target, self, Block.UPDATE_ALL);
        if (level.getBlockEntity(target) instanceof TileEntityMobilizer moved) {
            moved.copyStateFrom(this);
        }
        this.dead = true;
        level.updateNeighborsAt(oldPos, Blocks.AIR);
    }

    /** The original's {@code canPlaceBlockAt}: the space is free (replaceable) and the block can stand there. */
    private static boolean canPlaceAt(ServerLevel level, BlockState state, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced() && state.canSurvive(level, pos);
    }

    private static boolean isVanillaBlockEntity(BlockEntity entity) {
        return entity.getClass().getName().startsWith("net.minecraft.");
    }

    /**
     * Carries a vanilla block entity (a chest with its items, a sign with its text) one block over: its data goes to
     * the new block entity, and its contents are taken out of the old one first so that removing the old block does not
     * spill them.
     */
    private static void moveBlockEntity(ServerLevel level, BlockEntity passenger, BlockState state, BlockPos from, BlockPos to) {
        HolderLookup.Provider registries = level.registryAccess();
        CompoundTag data = passenger.saveWithFullMetadata(registries);
        if (passenger instanceof Clearable clearable) clearable.clearContent();
        level.setBlock(to, state, Block.UPDATE_ALL);
        BlockEntity moved = level.getBlockEntity(to);
        if (moved != null) {
            moved.loadWithComponents(data, registries);
            moved.setChanged();
        }
        level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private void copyStateFrom(TileEntityMobilizer other) {
        this.linked = other.linked;
        this.firstRelayX = other.firstRelayX;
        this.firstRelayZ = other.firstRelayZ;
        this.secondRelayX = other.secondRelayX;
        this.secondRelayZ = other.secondRelayZ;
        this.movementDirection = other.movementDirection;
        this.dead = false;
        setChanged();
    }

    /** For tests and tools: the position of the first relay of the link, or null while unlinked. */
    @Nullable
    public BlockPos firstRelayPos() {
        return linked ? new BlockPos(firstRelayX, worldPosition.getY(), firstRelayZ) : null;
    }
}
