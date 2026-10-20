package thaumcraft.common.entities.ai.misc;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIConvertGrass} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/misc/AIConvertGrass.java}).
 *
 * <p>The taint sheep turns the grass it stands on into taint: once in two hundred and fifty tries it starts a conversion
 * that takes forty ticks, and in the middle of it the grass becomes taint fibres and the column becomes tainted ground.</p>
 *
 * <p>Documented adaptation: the original wanted the snow-covered grass block (metadata one) and 1.21.1 has no metadata,
 * so every grass block counts.</p>
 */
public final class GolemConvertGrass extends Goal {
    /** The original 1.7 goal priority of {@code AIConvertGrass}. */
    public static final int PRIORITY = 7;

    private final Mob entity;
    private int countdown;

    public GolemConvertGrass(Mob entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!GolemAiRules.convertGrassRoll(entity.getRandom().nextInt(GolemAiRules.CONVERT_GRASS_CHANCE))) return false;
        BlockPos feet = entity.blockPosition();
        if (!(entity.level() instanceof ServerLevel server) || !server.hasChunkAt(feet)) return false;
        if (server.getBlockState(feet).is(Blocks.GRASS_BLOCK)) return true;
        BlockPos below = feet.below();
        return server.hasChunkAt(below) && isTallGrass(server.getBlockState(below));
    }

    @Override public boolean canContinueToUse() { return countdown > 0; }

    @Override public void start() {
        countdown = GolemAiRules.CONVERT_GRASS_TICKS;
        entity.level().broadcastEntityEvent(entity, (byte) 10);
        entity.getNavigation().stop();
    }

    @Override public void stop() { countdown = 0; }

    @Override public void tick() {
        countdown = Math.max(0, countdown - 1);
        if (countdown != 4) return;
        if (!(entity.level() instanceof ServerLevel server)) return;
        BlockPos feet = entity.blockPosition();
        if (!server.hasChunkAt(feet)) return;
        BlockState state = server.getBlockState(feet);
        if (state.is(Blocks.GRASS_BLOCK)) {
            server.destroyBlock(feet, false);
            server.setBlock(feet.below(), ThaumcraftTaint.FIBRES.get().defaultBlockState(), 3);
            BiomeColumns.set(server, feet.getX(), feet.getZ(), ThaumcraftBiomes.TAINT);
            return;
        }
        BlockPos below = feet.below();
        if (!server.hasChunkAt(below) || !isTallGrass(server.getBlockState(below))) return;
        server.destroyBlock(below, false);
        server.setBlock(feet, ThaumcraftTaint.FIBRES.get().defaultBlockState(), 3);
        BiomeColumns.set(server, feet.getX(), feet.getZ(), ThaumcraftBiomes.TAINT);
    }

    /** The ticks the conversion still needs. */
    public int countdown() { return countdown; }

    private static boolean isTallGrass(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN);
    }
}
