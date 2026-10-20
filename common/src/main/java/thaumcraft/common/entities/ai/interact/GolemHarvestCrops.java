package thaumcraft.common.entities.ai.interact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.GolemReplant;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIHarvestCrops} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/interact/AIHarvestCrops.java}).
 *
 * <p>The golem looks for a grown crop in a column around itself, walks to it and breaks it: the breaking takes
 * {@code max(10, (20 - 2 * strength) * hardness)} ticks, pulses every sixth of that time, and the crop is removed when the
 * last tick is over. Afterwards the golem looks for the next grown crop around the one it just harvested.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemHarvestCrops extends Goal {
    /** The original 1.7 goal priority of {@code AIHarvestCrops}. */
    public static final int PRIORITY = 2;

    private final EntityGolemBase golem;
    private final List<BlockPos> checklist = new ArrayList<>();
    private BlockPos target;
    private BlockState expected;
    private int delay = -1;
    private int maxDelay = 1;
    private int pulseMod = 1;
    private int count;

    public GolemHarvestCrops(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (delay >= 0) return false;
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (!golem.getNavigation().isDone()) return false;
        BlockPos found = findGrownCrop();
        if (found == null) return false;
        target = found;
        expected = golem.level().getBlockState(found);
        return true;
    }

    @Override public boolean canContinueToUse() {
        if (target == null || expected == null) return false;
        if (!golem.level().getBlockState(target).equals(expected)) return false;
        if (count-- <= 0) return false;
        return delay > 0 || !golem.getNavigation().isDone();
    }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        golem.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override public void stop() {
        if (target != null && golem.level() instanceof ServerLevel server) {
            server.destroyBlockProgress(golem.getId(), target, -1);
        }
        delay = -1;
    }

    @Override public void tick() {
        if (target == null || expected == null) return;
        golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 30, 30);
        double distance = golem.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (distance > GolemAiRules.HARVEST_REACH_SQ) return;
        if (delay < 0) {
            maxDelay = delay = GolemAiRules.cropBreakDelay(golem.getGolemStrength(),
                    expected.getDestroySpeed(golem.level(), target));
            pulseMod = GolemAiRules.pulseMod(delay);
        }
        if (delay <= 0) return;
        if (--delay > 0) {
            if (delay % pulseMod != 0 || !golem.getNavigation().isDone()) return;
            if (golem.level() instanceof ServerLevel server) {
                server.playSound(null, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                        expected.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                        (expected.getSoundType().getVolume() + 0.7F) / 8.0F, expected.getSoundType().getPitch() * 0.5F);
                server.destroyBlockProgress(golem.getId(), target, (int) (9.0F * (1.0F - (float) delay / (float) maxDelay)));
            }
            return;
        }
        harvest();
        checkAdjacent();
    }

    /** Original {@code harvest}: the crop is broken and its drops are left on the ground. */
    private void harvest() {
        count = GolemAiRules.pathBudget();
        if (!(golem.level() instanceof ServerLevel server)) return;
        if (!server.getBlockState(target).equals(expected)) return;
        server.destroyBlock(target, true, golem);
        if (golem.getUpgradeAmount(GolemUpgrade.ORDER) > 0) replant(server);
    }

    /** Original {@code AIHarvestCrops:185-245}: the fresh drops are pushed to the golem and planted back. */
    private void replant(ServerLevel server) {
        AABB box = new AABB(golem.position(), golem.position()).inflate(GolemReplant.RANGE);
        for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!item.isAlive() || item.level() != server) continue;
            if (item.tickCount < GolemReplant.FRESH_TICKS) item.setDeltaMovement(GolemReplant.nudge(golem.position(), item.position()));
            Block crop = GolemReplant.seedBlock(item.getItem());
            if (crop == null) continue;
            for (int cell = 0; cell < GolemReplant.CELLS && item.isAlive() && !item.getItem().isEmpty(); cell++) {
                BlockPos ground = GolemReplant.ground(target, cell);
                if (!server.hasChunkAt(ground) || !server.getBlockState(ground.above()).isAir()) continue;
                BlockState planted = crop.defaultBlockState();
                if (!planted.canSurvive(server, ground.above())) continue;
                if (!server.setBlock(ground.above(), planted, Block.UPDATE_ALL)) continue;
                ItemStack current = item.getItem();
                if (current.getCount() <= 1) item.discard();
                else item.setItem(current.copyWithCount(current.getCount() - 1));
            }
        }
    }

    /** Original {@code checkAdjacent}: the next grown crop around the one just harvested. */
    private void checkAdjacent() {
        if (!(golem.level() instanceof ServerLevel server)) return;
        int radius = GolemAiRules.cropSearchRadius(golem.getRange());
        BlockPos home = golem.getHomePosition();
        for (int dx = -GolemAiRules.CROP_ADJACENT_HORIZONTAL; dx <= GolemAiRules.CROP_ADJACENT_HORIZONTAL; dx++) {
            for (int dz = -GolemAiRules.CROP_ADJACENT_HORIZONTAL; dz <= GolemAiRules.CROP_ADJACENT_HORIZONTAL; dz++) {
                for (int dy = -GolemAiRules.CROP_ADJACENT_VERTICAL; dy <= GolemAiRules.CROP_ADJACENT_VERTICAL; dy++) {
                    BlockPos pos = target.offset(dx, dy, dz);
                    if (Math.abs(home.getX() - pos.getX()) > radius || Math.abs(home.getY() - pos.getY()) > radius
                            || Math.abs(home.getZ() - pos.getZ()) > radius) continue;
                    if (!ripe(server, pos)) continue;
                    target = pos.immutable();
                    expected = server.getBlockState(pos);
                    delay = -1;
                    start();
                    return;
                }
            }
        }
    }

    /**
     * Original {@code findGrownCrop}: the columns around the golem are shuffled and tried one after the other, and every
     * column is scanned from three blocks below to three blocks above the home.
     */
    private BlockPos findGrownCrop() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        int radius = GolemAiRules.cropSearchRadius(golem.getRange());
        net.minecraft.util.RandomSource random = golem.getRandom();
        if (checklist.isEmpty()) {
            BlockPos home = golem.getHomePosition();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) checklist.add(home.offset(dx, 0, dz));
            }
            shuffle(checklist, random);
        }
        BlockPos column = checklist.remove(0);
        BlockPos home = golem.getHomePosition();
        for (int dy = -GolemAiRules.CROP_SCAN_VERTICAL; dy <= GolemAiRules.CROP_SCAN_VERTICAL; dy++) {
            BlockPos pos = new BlockPos(column.getX(), home.getY() + dy, column.getZ());
            if (ripe(server, pos)) return pos.immutable();
        }
        return null;
    }

    /** Original {@code Collections.shuffle}: the columns are tried in a random order. */
    private static void shuffle(List<BlockPos> columns, net.minecraft.util.RandomSource random) {
        for (int index = columns.size(); index > 1; --index) Collections.swap(columns, index - 1, random.nextInt(index));
    }

    /**
     * Original {@code CropUtils.isGrownCrop}: wheat, carrots and potatoes at their last age, nether wart at age three,
     * cocoa at age two, melons and pumpkins always, and the stacked sugar cane and cactus.
     */
    static boolean ripe(ServerLevel server, BlockPos pos) {
        if (!server.hasChunkAt(pos)) return false;
        BlockState state = server.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block == Blocks.NETHER_WART) return state.getValue(NetherWartBlock.AGE) == 3;
        if (block == Blocks.COCOA) return state.getValue(CocoaBlock.AGE) == 2;
        if (block == Blocks.MELON || block == Blocks.PUMPKIN) return true;
        if (block == Blocks.SUGAR_CANE || block == Blocks.CACTUS) {
            BlockPos below = pos.below();
            return server.hasChunkAt(below) && server.getBlockState(below).getBlock() == block;
        }
        return false;
    }
}
