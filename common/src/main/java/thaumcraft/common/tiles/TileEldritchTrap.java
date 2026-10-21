package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original TileEldritchTrap: the runed eldritch trap. It hurts the player that
 * steps onto it and warps them away.
 */
public final class TileEldritchTrap extends BlockEntity {
    /** Original TileEldritchTrap:16 - the first check happens after twenty ticks. */
    int count = 20;

    public TileEldritchTrap(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    /** Original TileEldritchTrap:20-33 - the trap damage. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchTrap trap) {
        if (level.isClientSide) return;
        if (trap.count-- > 0) return;
        trap.count = 10 + level.getRandom().nextInt(25);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                pos.getX() + .5D - 3.0D, pos.getY() + .5D - 3.0D, pos.getZ() + .5D - 3.0D,
                pos.getX() + .5D + 3.0D, pos.getY() + .5D + 3.0D, pos.getZ() + .5D + 3.0D);
        Player player = null;
        double best = Double.MAX_VALUE;
        for (Player candidate : level.getEntitiesOfClass(Player.class, box)) {
            double distance = candidate.distanceToSqr(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D);
            if (distance < best) {
                best = distance;
                player = candidate;
            }
        }
        if (player == null) return;
        player.hurt(level.damageSources().magic(), 2.0F);
        // Original TileEldritchTrap:40 - the trap draws the node bolt to the player it hit.
        if (level instanceof ServerLevel server) thaumcraft.common.lib.network.BlockFxNetworking.sendZap(server,
                new net.minecraft.world.phys.Vec3(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D),
                new net.minecraft.world.phys.Vec3(player.getX(), player.getEyeY(), player.getZ()));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        count = tag.contains("count") ? tag.getInt("count") : 20;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("count", count);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
