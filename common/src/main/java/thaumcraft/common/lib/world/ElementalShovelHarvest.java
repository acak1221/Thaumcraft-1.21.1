package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.entities.golems.GolemUsePlatform;
import thaumcraft.common.items.equipment.ElementalShovelGeometry;
import thaumcraft.common.items.equipment.ItemElementalShovel;

/**
 * Shovel of the Earthmover mining (original {@code onBlockStartBreak} / {@code onBlockDestroyed}): a survival player who is not sneaking and
 * breaks a shovel block takes the other eight blocks of the 3 x 3 square across the hit face as well, those the shovel mines
 * ({@code mineable/shovel}), that are not unbreakable and that the player may touch, each by an ordinary nested break (a use, the drops, the
 * break events of protection mods). The face is the one the player is looking at; when the look does not meet the broken block only that
 * block goes (the original kept the last face it saw). Creative players, sneaking players, golems and Excavation break one block.
 */
public final class ElementalShovelHarvest {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    private ElementalShovelHarvest() {}

    /** Decided before the anchor breaks: the face to work across, or null for an ordinary break. */
    public static Direction plan(ServerPlayerGameMode mode, ServerLevel level, ServerPlayer player, BlockPos anchor) {
        if (!(player.getMainHandItem().getItem() instanceof ItemElementalShovel) || player.isShiftKeyDown() || !mode.isSurvival()) return null;
        if (ACTIVE.get() != null || ExcavationHarvest.hasActiveAttempt() || GolemUsePlatform.isActorBorrowed(player)) return null;
        if (!realActor(mode, level, player) || !level.getBlockState(anchor).is(BlockTags.MINEABLE_WITH_SHOVEL)) return null;
        HitResult look = player.pick(player.blockInteractionRange() + 1.0, 1.0F, false);
        return look instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(anchor) ? hit.getDirection() : null;
    }

    /** After the anchor went: the other eight cells, while the shovel lasts. */
    public static void finish(ServerPlayerGameMode mode, ServerLevel level, ServerPlayer player, BlockPos anchor, BlockState before, Direction face) {
        if (level.getBlockState(anchor).is(before.getBlock())) return; // the native boolean can say true without a removal
        ACTIVE.set(Boolean.TRUE);
        try {
            for (BlockPos cell : ElementalShovelGeometry.plane(anchor, face)) {
                if (!(player.getMainHandItem().getItem() instanceof ItemElementalShovel)) return;
                if (cell.equals(anchor) || !permitted(level, player, cell)) continue;
                BlockState state = level.getBlockState(cell);
                if (state.isAir() || !state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.getDestroySpeed(level, cell) < 0) continue;
                mode.destroyBlock(cell);
            }
        } finally {
            ACTIVE.remove();
        }
    }

    private static boolean realActor(ServerPlayerGameMode mode, ServerLevel level, ServerPlayer player) {
        return player.server.isSameThread() && player.gameMode == mode && player.level() == level
                && player.isAlive() && !player.isRemoved() && !player.isSpectator() && !player.hasDisconnected()
                && player.connection != null && player.server.getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private static boolean permitted(ServerLevel level, ServerPlayer player, BlockPos cell) {
        return level.isInWorldBounds(cell) && level.getWorldBorder().isWithinBounds(cell) && level.hasChunkAt(cell) && level.mayInteract(player, cell);
    }
}
