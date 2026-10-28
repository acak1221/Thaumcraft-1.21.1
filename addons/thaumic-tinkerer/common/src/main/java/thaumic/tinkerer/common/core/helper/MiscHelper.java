/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 01:29:47 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.utils.GameInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Odds and ends of 1.7.10 that several groups use: distances, motion towards a point, block breaking with a check. */
public final class MiscHelper {
    private static final Logger LOG = LoggerFactory.getLogger("ThaumicTinkerer");

    private MiscHelper() {}

    public static double pointDistanceSpace(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
    }

    /**
     * Replaces the block at the position with the given state, unless a listener of the block-break event vetoes it.
     * 1.7.10 posted {@code BlockEvent.BreakEvent} on the Forge bus; the port asks the Architectury
     * {@link BlockEvent#BREAK} listeners (which protection mods and the claim systems that use Architectury join), and
     * only a {@link ServerPlayer} can be asked: for any other player, or none, the replacement is not vetoed.
     *
     * @param flag the {@code Block.UPDATE_*} flags of {@code Level.setBlock}
     * @return true when the block was set
     */
    public static boolean breakBlockWithCheck(Level level, BlockPos pos, BlockState replacement, int flag, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            EventResult result = BlockEvent.BREAK.invoker().breakBlock(level, pos, level.getBlockState(pos), serverPlayer,
                    null);
            if (result.isFalse()) return false;
        }
        level.setBlock(pos, replacement, flag);
        return true;
    }

    /** 1.7.10 {@code breakBlockWithCheck(w, x, y, z, block, meta, flag, player)}: the block's default state stands for the metadata. */
    public static boolean breakBlockWithCheck(Level level, BlockPos pos, Block block, int flag, Player player) {
        return breakBlockWithCheck(level, pos, block.defaultBlockState(), flag, player);
    }

    /** Turns the block into air with flag 3 (1.7.10 {@code breakBlockToAirWithCheck}). */
    public static boolean breakBlockToAirWithCheck(Level level, BlockPos pos, Player player) {
        return breakBlockWithCheck(level, pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL, player);
    }

    /** Same as {@link #breakBlockWithCheck(Level, BlockPos, Block, int, Player)} with flag 0, as the 1.7.10 overload had. */
    public static boolean breakBlockWithCheck(Level level, BlockPos pos, Block block, Player player) {
        return breakBlockWithCheck(level, pos, block, 0, player);
    }

    /** The running server, or null when none runs (1.7.10 {@code MinecraftServer.getServer()}). */
    public static MinecraftServer server() {
        return GameInstance.getServer();
    }

    /**
     * Sets the motion of the entity to the vector from its centre to the point, normalised when longer than a block,
     * times the modifier (the pull of the magnets and of the Focus of Telekinesis).
     */
    public static void setEntityMotionFromVector(Entity entity, Vec3 originalPosVector, float modifier) {
        Vec3 entityVector = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ());
        Vec3 finalVector = originalPosVector.subtract(entityVector);

        if (finalVector.length() > 1) finalVector = finalVector.normalize();

        entity.setDeltaMovement(finalVector.x * modifier, finalVector.y * modifier, finalVector.z * modifier);
    }

    /** A copy of the list with every amount multiplied (and truncated), the list itself when the factor is 1. */
    public static AspectList multiplyAspectList(AspectList list, double multiplier) {
        AspectList newList = list.copy();
        if (multiplier == 1) return newList;

        for (Aspect aspect : newList.getAspects()) {
            newList.aspects.put(aspect, (int) ((double) newList.aspects.get(aspect) * multiplier));
        }

        return newList;
    }

    public static void printCurrentStackTrace(String message) {
        if (message != null) LOG.info(message);

        for (StackTraceElement element : Thread.currentThread().getStackTrace()) LOG.info("{}", element);
    }
}
