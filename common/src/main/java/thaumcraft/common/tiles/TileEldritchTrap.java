package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.entities.OriginalPlayerSearch;

/**
 * Original TileEldritchTrap: the runed eldritch trap. It hurts the player that
 * steps onto it and warps them away.
 *
 * <p>Oracle {@code TileEldritchTrap:18-32} - the trap waits twenty ticks, then it looks
 * for the closest player within three blocks, hurts that player for two points of magic
 * damage and warps half of the hits one or two points into the void.
 */
public final class TileEldritchTrap extends BlockEntity {
    /** Original TileEldritchTrap:16 - the first check happens after twenty ticks. */
    int count = 20;

    /** Original TileEldritchTrap:22 - the radius of the search of the trap. */
    public static final double PLAYER_RADIUS = 3.0D;
    /** Original TileEldritchTrap:24 - the damage of one hit. */
    public static final float TRAP_DAMAGE = 2.0F;
    /** Original TileEldritchTrap:26 - the warp of a hit is one or two points. */
    public static final int WARP_MIN = 1;
    public static final int WARP_MAX = 2;
    /** Original TileEldritchTrap:21 - the next check happens after ten to thirty four ticks. */
    public static final int NEXT_CHECK_MIN = 10;
    public static final int NEXT_CHECK_SPAN = 25;

    public TileEldritchTrap(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    /** Original TileEldritchTrap:20-33 - the trap damage. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchTrap trap) {
        if (level.isClientSide) return;
        if (trap.count-- > 0) return;
        trap.count = NEXT_CHECK_MIN + level.getRandom().nextInt(NEXT_CHECK_SPAN);
        // Original TileEldritchTrap:22 - the original looks for the closest player within
        // three blocks; the search is a sphere and a creative player is never found.
        Player player = OriginalPlayerSearch.closestPlayer(level, pos.getX() + .5D, pos.getY() + .5D,
                pos.getZ() + .5D, PLAYER_RADIUS);
        if (player == null) return;
        player.hurt(level.damageSources().magic(), TRAP_DAMAGE);
        // Original TileEldritchTrap:25-27 - half of the hits warp the player away.
        if (level.getRandom().nextBoolean()) {
            int warp = WARP_MIN + level.getRandom().nextInt(WARP_MAX - WARP_MIN + 1);
            if (player instanceof ServerPlayer server) WarpEvents.addTemporaryWarp(server, warp);
        }
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
