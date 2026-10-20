package thaumcraft.common.entities.ai.interact;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityGolemBobber;
import thaumcraft.common.entities.golems.GolemFishingLoot;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIFish} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/interact/AIFish.java}).
 *
 * <p>The golem looks for a water block with air above it, throws its bobber into it and waits: the cast lasts 300 to 499
 * ticks, and every tick the golem rolls its catch chance, which is the quality of the pond plus a little for every point of
 * its strength. A catch is one result, two with an Air upgrade now and then, and it is cooked when the golem carries a
 * Fire upgrade.</p>
 *
 * <p>The pond quality is the original's: three ten-thousandths for every direction with water beside the pond, again for
 * every direction that sees the sky, and one and a half ten-thousandths for every deeper water block below it.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemFish extends Goal {
    /** The original 1.7 goal priority of {@code AIFish}. */
    public static final int PRIORITY = 2;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final EntityGolemBase golem;
    private BlockPos target;
    private EntityGolemBobber bobber;
    private int count;
    private float quality;
    private int adjacentWater, skyWater, depthWater;

    public GolemFish(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (target != null || count > 0) return false;
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (!golem.getNavigation().isDone()) return false;
        if (bobber != null) bobber.discard();
        bobber = null;
        BlockPos water = findWater();
        if (water == null) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        target = water;
        samplePond(server, water);
        server.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL,
                0.5F, 0.4F / (server.random.nextFloat() * 0.4F + 0.8F));
        bobber = new EntityGolemBobber(ThaumcraftGolems.GOLEM_BOBBER.get(), server);
        bobber.setup(golem, water);
        if (!server.addFreshEntity(bobber)) {
            bobber = null;
            return false;
        }
        return true;
    }

    @Override public boolean canContinueToUse() {
        return bobber != null && !bobber.isRemoved() && target != null && count-- > 0;
    }

    @Override public void start() {
        count = GolemAiRules.fishingDelay(golem.getRandom().nextInt(GolemAiRules.FISH_DELAY_SPAN));
    }

    @Override public void stop() {
        if (bobber != null) bobber.discard();
        bobber = null;
        target = null;
        count = -1;
    }

    @Override public void tick() {
        if (target == null || bobber == null) return;
        if (!(golem.level() instanceof ServerLevel server)) return;
        golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, 30, 30);
        float chance = GolemAiRules.fishingCatchChance(adjacentWater, skyWater, depthWater, golem.getGolemStrength());
        if (server.random.nextFloat() >= chance) return;
        GolemFishingLoot.Pond pond = new GolemFishingLoot.Pond(adjacentWater, skyWater, depthWater);
        int catches = GolemFishingLoot.catchCount(server.random, golem);
        for (int caught = 0; caught < catches; caught++) {
            ItemStack result = GolemFishingLoot.nextCatch(server, server.random, golem, pond);
            if (result.isEmpty()) return;
            ItemEntity item = new ItemEntity(server, target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, result);
            double dx = golem.getX() + server.random.nextFloat() - server.random.nextFloat() - target.getX() + 0.5;
            double dy = golem.getY() - target.getY() + 1.0;
            double dz = golem.getZ() + server.random.nextFloat() - server.random.nextFloat() - target.getZ() + 0.5;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(length) * 0.08, dz * 0.1);
            item.setDefaultPickUpDelay(); // The original waits twenty ticks before the catch may be picked up.
            server.addFreshEntity(item);
        }
        server.playSound(null, bobber.getX(), bobber.getY(), bobber.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                0.15F, 1.0F + (server.random.nextFloat() - server.random.nextFloat()) * 0.4F);
        bobber.discard();
        bobber = null;
        target = null;
    }

    /**
     * Original {@code findWater}: a water block with air above it, looked for in a random column around the golem, twice
     * the search radius in attempts.
     */
    private BlockPos findWater() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        int radius = GolemAiRules.fishSearchRadius(golem.getRange());
        BlockPos home = golem.getHomePosition();
        int attempts = (int) (radius * 2.0F);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = home.getX() + golem.getRandom().nextInt(1 + radius * 2) - radius;
            int y = home.getY() + golem.getRandom().nextInt(1 + radius) - radius / 2;
            int z = home.getZ() + golem.getRandom().nextInt(1 + radius * 2) - radius;
            BlockPos pos = new BlockPos(x, y, z);
            if (!server.hasChunkAt(pos) || !server.hasChunkAt(pos.above())) continue;
            if (!server.getBlockState(pos).is(Blocks.WATER)) continue;
            if (!server.getBlockState(pos.above()).isAir()) continue;
            return pos.immutable();
        }
        return null;
    }

    /** Original {@code AIFish}: the pond's good directions, its sky-lit ones and the water below it. */
    private void samplePond(ServerLevel server, BlockPos pos) {
        adjacentWater = 0;
        skyWater = 0;
        depthWater = 0;
        for (Direction side : SIDES) {
            BlockPos neighbour = pos.relative(side);
            BlockPos upper = neighbour.above();
            if (!server.hasChunkAt(neighbour) || !server.hasChunkAt(upper)) continue;
            if (!server.getBlockState(neighbour).is(Blocks.WATER) || !server.getBlockState(upper).isAir()) continue;
            ++adjacentWater;
            if (server.canSeeSky(upper)) ++skyWater;
            for (int depth = 1; depth <= GolemAiRules.FISH_DEPTH; depth++) {
                BlockPos below = neighbour.below(depth);
                if (!server.hasChunkAt(below)) break;
                if (server.getBlockState(below).is(Blocks.WATER)) ++depthWater;
            }
        }
        quality = GolemAiRules.fishingQuality(adjacentWater, skyWater, depthWater);
    }

    /** The pond quality of the last cast. */
    public float quality() { return quality; }

    /** The entity that carries the bobber; the original's {@code theGolem}. */
    public Entity owner() { return golem; }
}
