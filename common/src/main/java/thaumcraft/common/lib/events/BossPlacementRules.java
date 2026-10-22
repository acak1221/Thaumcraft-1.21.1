package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.IThaumcraftBoss;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.OuterLandsRules;

/**
 * The original rule that cancels a block placement next to an active eldritch boss.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerWorld.java:225-245} - the original
 * cancelled the {@code BlockEvent.PlaceEvent} and the {@code BlockEvent.MultiPlaceEvent} of a
 * placement that stood in the Outer Lands, in a labyrinth cell of a boss room, while a boss of
 * the original {@code EntityThaumcraftBoss} family was alive within thirty-two blocks.</p>
 *
 * <p>The rule itself is loader independent; the two loaders call it from their own placement
 * hook, because the cancellable placement event of the common artifact of Architectury and the
 * one of NeoForge do not share a signature.</p>
 */
public final class BossPlacementRules {
    private BossPlacementRules() {}

    /** Oracle {@code EventHandlerWorld:244} - the radius of the boss search. */
    public static final double BOSS_RANGE = 32.0D;

    /**
     * The original decision of {@code EventHandlerWorld.isNearActiveBoss:239-245}.
     *
     * @param outerLands   the world is the Outer Lands
     * @param spectator    the placer is a spectator
     * @param bossRoomCell the chunk of the placement is a quadrant of a boss room
     * @param bossNear     a boss of the original boss family is alive within the range
     * @return whether the placement has to be cancelled
     */
    public static boolean blocksPlacement(boolean outerLands, boolean spectator, boolean bossRoomCell, boolean bossNear) {
        return outerLands && !spectator && bossRoomCell && bossNear;
    }

    /**
     * Oracle {@code EventHandlerWorld.isNearActiveBoss:239-245}.
     *
     * @param level  the world of the placement
     * @param placer the entity that places the block, the original required a player
     * @param x      the X coordinate of the placement
     * @param y      the Y coordinate of the placement
     * @param z      the Z coordinate of the placement
     * @return whether the placement has to be cancelled
     */
    public static boolean nearActiveBoss(Level level, Entity placer, int x, int y, int z) {
        if (level == null) return false;
        // Oracle EventHandlerWorld:244 - the world has to be the Outer Lands and the placer a
        // player that is not a spectator.
        boolean outerLands = OuterLandsProvider.isOuterLands(level);
        boolean spectator = placer instanceof Player player && player.isSpectator();
        if (!outerLands || spectator) return false;
        if (!bossRoomCell(level, x >> 4, z >> 4)) return false;
        return bossNear(level, x, y, z);
    }

    /**
     * Oracle {@code EventHandlerWorld:244} - the chunk of the placement has to be a quadrant of
     * a boss room of the labyrinth.
     */
    public static boolean bossRoomCell(Level level, int chunkX, int chunkZ) {
        if (!(level instanceof ServerLevel server)) return false;
        short packed = OuterLandsProvider.maze(server).cell(chunkX, chunkZ);
        if (packed == 0) return false;
        return OuterLandsRules.isBossQuadrant(new MazeGenerator.Cell(packed).feature);
    }

    /**
     * Oracle {@code EventHandlerWorld:244} - a boss of the original {@code EntityThaumcraftBoss}
     * family has to be alive within thirty-two blocks of the placement.
     */
    public static boolean bossNear(Level level, int x, int y, int z) {
        AABB box = new AABB(x - BOSS_RANGE, y - BOSS_RANGE, z - BOSS_RANGE,
                x + BOSS_RANGE, y + BOSS_RANGE, z + BOSS_RANGE);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (entity instanceof IThaumcraftBoss) return true;
        }
        return false;
    }

    /**
     * The placement of a block at a position, for the loader hooks.
     *
     * @param level  the world of the placement
     * @param placer the entity that places the block
     * @param pos    the position of the placement
     * @return whether the placement has to be cancelled
     */
    public static boolean nearActiveBoss(Level level, Entity placer, BlockPos pos) {
        return pos != null && nearActiveBoss(level, placer, pos.getX(), pos.getY(), pos.getZ());
    }
}
