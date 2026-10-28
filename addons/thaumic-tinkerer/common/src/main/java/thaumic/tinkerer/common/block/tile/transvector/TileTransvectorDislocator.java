/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Nov 24, 2013, 6:48:04 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.transvector;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.blocks.BlocksTC;
import thaumic.tinkerer.common.block.camo.PulseDeviceBlock;
import thaumic.tinkerer.common.block.camo.RedstonePulseReceiver;
import thaumic.tinkerer.common.content.TileBlocks;

/**
 * The Transvector Dislocator: on a redstone pulse it swaps the block in front of it with the block it is bound to (up to
 * sixteen blocks away), block entities with their data, and moves every entity standing in those two blocks to the other
 * one (1.7.10 {@code TileTransvectorDislocator}). The facing is the block state's.
 */
public class TileTransvectorDislocator extends TileTransvector implements RedstonePulseReceiver {

    /** Game ticks between two swaps. */
    private static final int COOLDOWN = 10;

    private int cooldown = 0;
    private boolean pulseStored = false;

    public TileTransvectorDislocator(BlockPos pos, BlockState state) {
        super(TileBlocks.dislocatorType(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileTransvectorDislocator tile) {
        tile.tick();
    }

    private void tick() {
        cooldown = Math.max(0, cooldown - 1);
        if (cooldown == 0 && pulseStored) {
            pulseStored = false;
            receiveRedstonePulse();
        }
    }

    @Override
    public void receiveRedstonePulse() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        getTile(); // sanity check

        BlockPos endCoords = getTarget();
        if (endCoords == null) return;

        if (cooldown > 0) {
            pulseStored = true;
            return;
        }

        // The end must be loaded (1.7.10 loaded it on demand; it still skipped the swap when it was not loaded).
        if (!serverLevel.hasChunkAt(endCoords)) return;

        BlockPos targetCoords = getBlockTarget();
        List<Entity> entitiesAtEnd = getEntitiesAtPoint(serverLevel, endCoords);
        List<Entity> entitiesAtTarget = getEntitiesAtPoint(serverLevel, targetCoords);

        Vec3 targetToEnd = asVector(targetCoords, endCoords);
        Vec3 endToTarget = asVector(endCoords, targetCoords);

        BlockData endData = new BlockData(serverLevel, endCoords);
        BlockData targetData = new BlockData(serverLevel, targetCoords);

        if (checkBlock(serverLevel, targetCoords) && checkBlock(serverLevel, endCoords)) {
            endData.clearBlockEntityAt();
            targetData.clearBlockEntityAt();

            endData.setTo(targetCoords);
            targetData.setTo(endCoords);

            endData.notifyNeighbor(targetCoords);
            targetData.notifyNeighbor(endCoords);
        }

        for (Entity entity : entitiesAtEnd) moveEntity(entity, endToTarget);
        for (Entity entity : entitiesAtTarget) moveEntity(entity, targetToEnd);

        cooldown = COOLDOWN;
    }

    /**
     * {@code checkBlock}, with the original expression: {@code (not a node && not blacklisted && not unbreakable) ||
     * block != air}. The {@code ||} makes it true for every block, so the Dislocator also swaps nodes, bedrock and the
     * portable hole's blacklist; the check is kept as it was written (see docs/deviations/TileBlocks.md).
     */
    public static boolean checkBlock(Level level, BlockPos coords) {
        BlockState state = level.getBlockState(coords);
        boolean node = state.is(BlocksTC.auraNode());
        return !node && !ThaumcraftApi.portableHoleBlackList.contains(state.getBlock())
                && state.getDestroySpeed(level, coords) != -1F
                || !state.isAir();
    }

    private static List<Entity> getEntitiesAtPoint(Level level, BlockPos coords) {
        return level.getEntitiesOfClass(Entity.class, new AABB(coords));
    }

    private static Vec3 asVector(BlockPos source, BlockPos target) {
        return new Vec3(target.getX() - source.getX(), target.getY() - source.getY(), target.getZ() - source.getZ());
    }

    private static void moveEntity(Entity entity, Vec3 vec) {
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(entity.getX() + vec.x, entity.getY() + vec.y, entity.getZ() + vec.z,
                    player.getYRot(), player.getXRot());
        } else {
            entity.setPos(entity.getX() + vec.x, entity.getY() + vec.y, entity.getZ() + vec.z);
        }
    }

    /** The block in front of the dislocator. */
    public BlockPos getBlockTarget() {
        return worldPosition.relative(getFacing());
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(PulseDeviceBlock.FACING) ? state.getValue(PulseDeviceBlock.FACING) : Direction.NORTH;
    }

    @Override
    public int getMaxDistance() {
        return TileBlocks.DISLOCATOR_DISTANCE;
    }

    @Override
    boolean tileRequiredAtLink() {
        return false;
    }

    /** A block with its state and the data of its block entity, as it is at the moment of the swap. */
    private final class BlockData {
        private final ServerLevel world;
        private final BlockState state;
        @Nullable
        private final CompoundTag tile;
        private final BlockPos coords;

        BlockData(ServerLevel world, BlockPos coords) {
            this.world = world;
            this.coords = coords;
            this.state = world.getBlockState(coords);
            BlockEntity entity = world.getBlockEntity(coords);
            this.tile = entity == null ? null : entity.saveWithFullMetadata(world.registryAccess());
        }

        /**
         * Replaces the block entity at the block's place with a fresh one of the same kind, so that taking the block away
         * does not spill the contents of the old one (the data is in {@link #tile}).
         */
        void clearBlockEntityAt() {
            if (state.getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity fresh = entityBlock.newBlockEntity(coords, state);
                if (fresh != null) world.setBlockEntity(fresh);
            }
        }

        void setTo(BlockPos destination) {
            world.setBlock(destination, state, Block.UPDATE_CLIENTS);

            BlockEntity entity = tile == null ? null : BlockEntity.loadStatic(destination, state, tile, world.registryAccess());
            if (entity != null) {
                world.setBlockEntity(entity);
            } else if (world.getBlockEntity(destination) != null) {
                world.removeBlockEntity(destination);
            }
        }

        void notifyNeighbor(BlockPos at) {
            state.handleNeighborChanged(world, at, TileTransvectorDislocator.this.getBlockState().getBlock(), worldPosition, false);
        }
    }
}
