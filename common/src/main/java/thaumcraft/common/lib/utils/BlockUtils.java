package thaumcraft.common.lib.utils;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.world.StreamAxeGeometry;

/**
 * Original TC4 {@code thaumcraft.common.lib.utils.BlockUtils}: the block helpers of the harvesting focus, the
 * lumber golem, the arcane bore and the taint spread.
 *
 * <p>The 1.7.10 helpers used the block and its metadata. 1.21.1 has no block metadata: a block is a
 * {@link BlockState} and a metadata becomes either "any state of this block" or one exact state. The overloads
 * below keep both readings, so a caller that only cares about the block uses the {@link Block} form and a caller
 * that cared about the metadata uses the {@link BlockState} form.
 */
public final class BlockUtils {
    /**
     * Integration point for the entity layer's following-item entity (original {@code EntityFollowingItem}).
     *
     * <p>The original replaced a fresh drop with a drop that follows the player. The native entity layer owns that
     * entity, so it installs its spawner here; without it the fresh drops stay where they fell.
     */
    public interface FollowingItemSpawner {
        /** Replace the fresh drop with one that follows the target player. */
        void spawnFollowingItem(ServerLevel level, ItemEntity drop, Player target, int color);
    }

    private static FollowingItemSpawner followingItemSpawner;

    private BlockUtils() {}

    /** Install the entity layer's following-item spawner. */
    public static void installFollowingItemSpawner(FollowingItemSpawner spawner) {
        followingItemSpawner = spawner;
    }

    // ------------------------------------------------------------------- harvesting

    /** Original {@code harvestBlock(World, EntityPlayer, int, int, int)}. */
    public static boolean harvestBlock(Level level, Player player, int x, int y, int z) {
        return harvestBlock(level, player, x, y, z, false, 0);
    }

    /**
     * Original {@code harvestBlock(World, EntityPlayer, int, int, int, boolean, int)}.
     *
     * <p>The original removed the block, and only a survival player who may harvest it received the drops; a
     * creative player removed it without any. The original returned true even when the removal itself failed.
     */
    public static boolean harvestBlock(Level level, Player player, int x, int y, int z, boolean followItem, int color) {
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        level.levelEvent(2001, pos, Block.getId(state));
        boolean creative = player != null && player.getAbilities().instabuild;
        boolean canHarvest = !creative && canHarvest(level, pos, state, player);
        boolean removed = removeBlock(level, x, y, z, player);
        if (removed && !creative && canHarvest) {
            dropBlockLoot(level, pos, state, player);
            if (followItem) followFreshDrops(level, pos, player, color);
        }
        return true;
    }

    /** Original {@code Block.canHarvestBlock}: may this player take the drops of this block? */
    public static boolean canHarvest(Level level, BlockPos pos, BlockState state, Player player) {
        if (player == null) return false;
        return !state.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(state);
    }

    /**
     * Original {@code removeBlock(World, int, int, int, EntityPlayer)}.
     *
     * <p>The original removed the block without its drops and reported whether the removal happened. The native
     * removal keeps the drops for the caller, exactly as the original separated the two steps.
     *
     * <p>The player may be null (the original passed no player to a block that did not use it). The native
     * {@code playerWillDestroy} of a block announces the break to the player (it angers the piglins of a block in
     * {@code #minecraft:guarded_by_piglins} and dereferences the player to do so), so it is only called for a player;
     * without one the block is removed as the world removes any block.
     */
    public static boolean removeBlock(Level level, int x, int y, int z, Player player) {
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (player != null) state.getBlock().playerWillDestroy(level, pos, state, player);
        return level.destroyBlock(pos, false, player);
    }

    /** The drops of a broken block, dropped as the original's {@code harvestBlock} did. */
    private static void dropBlockLoot(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(level instanceof ServerLevel server)) return;
        BlockEntity tile = level.getBlockEntity(pos);
        ItemStack tool = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        for (ItemStack drop : Block.getDrops(state, server, pos, tile, player, tool)) {
            if (drop.isEmpty()) continue;
            Block.popResource(level, pos, drop);
        }
    }

    /** The original's replacement of the fresh drops by following drops. */
    private static void followFreshDrops(Level level, BlockPos pos, Player player, int color) {
        if (followingItemSpawner == null || player == null || !(level instanceof ServerLevel server)) return;
        List<ItemEntity> drops = EntityUtils.getEntitiesInRange(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                player, ItemEntity.class, 2.0);
        for (ItemEntity drop : drops) {
            if (!drop.isAlive() || drop.tickCount != 0) continue;
            followingItemSpawner.spawnFollowingItem(server, drop, player, color);
            drop.discard();
        }
    }

    // ------------------------------------------------------------------------ drops

    /** Original {@code createStackedBlock(Block, int)}: the item of a block. */
    public static ItemStack createStackedBlock(Block block) {
        if (block == null) return ItemStack.EMPTY;
        var item = block.asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The item of a block state, the 1.21.1 form of the original's metadata aware {@code createStackedBlock}. */
    public static ItemStack createStackedBlock(BlockState state, LevelReader level, BlockPos pos) {
        if (state == null) return ItemStack.EMPTY;
        ItemStack stack = state.getBlock().getCloneItemStack(level, pos, state);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /** Original {@code dropBlockAsItem(World, int, int, int, ItemStack, Block)}. */
    public static void dropBlockAsItem(Level level, int x, int y, int z, ItemStack stack, Block block) {
        if (level == null || level.isClientSide || stack == null || stack.isEmpty()) return;
        Block.popResource(level, new BlockPos(x, y, z), stack);
    }

    /**
     * Original {@code dropBlockAsItemWithChance(World, Block, int, int, int, int, float, int, EntityPlayer)}.
     *
     * <p>1.21.1 applies the fortune level through the held tool's enchantments, so the fortune argument no longer
     * has a separate effect.
     */
    public static void dropBlockAsItemWithChance(Level level, Block block, int x, int y, int z, int meta, float dropchance,
                                                 int fortune, Player player) {
        if (level == null || level.isClientSide || block == null) return;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        for (ItemStack drop : drops(level, pos, state, player)) {
            if (drop.isEmpty()) continue;
            if (!(level.getRandom().nextFloat() <= dropchance)) continue;
            dropBlockAsItem(level, x, y, z, drop, block);
        }
    }

    /** The native drops of a block, with the held tool of the player. */
    private static List<ItemStack> drops(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(level instanceof ServerLevel server)) return List.of();
        BlockEntity tile = level.getBlockEntity(pos);
        ItemStack tool = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        return Block.getDrops(state, server, pos, tile, player, tool);
    }

    /** Original {@code destroyBlockPartially(World, int, int, int, int, int)}. */
    public static void destroyBlockPartially(Level level, int breakerId, int x, int y, int z, int progress) {
        if (level == null || level.isClientSide) return;
        level.destroyBlockProgress(breakerId, new BlockPos(x, y, z), progress);
    }

    // ------------------------------------------------------------------ greedy search

    /**
     * Original {@code findBlocks(World, int, int, int, Block)}: the greedy walk to the furthest connected log, answered
     * as the cell it ends on instead of being left in static fields.
     *
     * <p>The original searched a five by five by five neighbourhood around the current block, took the first
     * candidate that was further away than the last one and repeated from there. Its early exits return from the
     * whole method, not from the loop, and that quirk is kept (it is the walk of {@link StreamAxeGeometry#walk}, which
     * the Axe of the Stream shares). A candidate is a cell that holds exactly {@code block}, is a log by
     * {@link Utils#isWoodLog}, can be broken and lies in a loaded chunk inside the build height: an unloaded cell is
     * no link, so the walk ends at the loaded edge and loads nothing (the original read, and so generated, such a
     * chunk).
     *
     * <p>Read-only, and it does not look at the start cell itself: when no cell qualifies the answer is the start.
     */
    public static BlockPos findFurthestBlock(Level level, int x, int y, int z, Block block) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        StreamAxeGeometry.Cell end = StreamAxeGeometry.walk(x, y, z, (cx, cy, cz) -> {
            cursor.set(cx, cy, cz);
            if (level.isOutsideBuildHeight(cursor) || !level.hasChunkAt(cursor)) return false;
            BlockState state = level.getBlockState(cursor);
            return state.getBlock() == block && Utils.isWoodLogState(state) && state.getDestroySpeed(level, cursor) >= 0.0f;
        });
        return new BlockPos(end.x(), end.y(), end.z());
    }

    /** Original {@code breakFurthestBlock(World, int, int, int, Block, EntityPlayer)}. */
    public static boolean breakFurthestBlock(Level level, int x, int y, int z, Block block, Player player) {
        return breakFurthestBlock(level, x, y, z, block, player, false, 0);
    }

    /** Original {@code breakFurthestBlock(World, int, int, int, Block, EntityPlayer, boolean, int)}. */
    public static boolean breakFurthestBlock(Level level, int x, int y, int z, Block block, Player player, boolean followitem, int color) {
        if (level == null) return false;
        return breakFurthestBlockAt(level, new BlockPos(x, y, z), findFurthestBlock(level, x, y, z, block), player, followitem, color);
    }

    /**
     * The second half of {@link #breakFurthestBlock(Level, int, int, int, Block, Player, boolean, int)} for a walk that
     * was already done: harvests the {@code end} cell, tells the clients about the start and the end, and gives every
     * loaded cell within three blocks of the end the delayed block tick of the original (150 to 299 ticks, which the
     * leaves around a felled log answer). Returns what {@link #harvestBlock} returns.
     */
    public static boolean breakFurthestBlockAt(Level level, BlockPos origin, BlockPos end, Player player, boolean followitem, int color) {
        if (level == null || origin == null || end == null) return false;
        boolean worked = harvestBlock(level, player, end.getX(), end.getY(), end.getZ(), followitem, color);
        markForRenderUpdate(level, origin.getX(), origin.getY(), origin.getZ());
        if (worked) {
            markForRenderUpdate(level, end.getX(), end.getY(), end.getZ());
            for (int xx = -3; xx <= 3; ++xx) {
                for (int yy = -3; yy <= 3; ++yy) {
                    for (int zz = -3; zz <= 3; ++zz) {
                        BlockPos pos = end.offset(xx, yy, zz);
                        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
                        level.scheduleTick(pos, level.getBlockState(pos).getBlock(), 150 + level.getRandom().nextInt(150));
                    }
                }
            }
        }
        return worked;
    }

    /** The 1.21.1 form of the original's {@code markBlockRangeForRenderUpdate}. */
    private static void markForRenderUpdate(Level level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    // --------------------------------------------------------------------- ray traces

    /** Original {@code getTargetBlock(World, double, double, double, float, float, boolean, double)}. */
    public static BlockHitResult getTargetBlock(Level level, double x, double y, double z, float yaw, float pitch, boolean par3, double range) {
        Vec3 start = new Vec3(x, y, z);
        float yawRadians = (float) (-yaw * ((float) Math.PI / 180) - (float) Math.PI);
        float pitchRadians = (float) (-pitch * ((float) Math.PI / 180));
        float cos = (float) Math.cos(yawRadians);
        float sin = (float) Math.sin(yawRadians);
        float negCos = -(float) Math.cos(pitchRadians);
        float sinPitch = (float) Math.sin(pitchRadians);
        float xOffset = sin * negCos;
        float zOffset = cos * negCos;
        Vec3 end = start.add(xOffset * range, sinPitch * range, zOffset * range);
        return rayTrace(level, start, end, par3);
    }

    /** Original {@code getTargetBlock(World, Entity, boolean)}. */
    public static BlockHitResult getTargetBlock(Level level, Entity entity, boolean par3) {
        float factor = 1.0f;
        float yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * factor;
        float pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * factor;
        double x = entity.xo + (entity.getX() - entity.xo) * factor;
        double y = entity.yo + (entity.getY() - entity.yo) * factor + 1.62 - entity.getEyeHeight();
        double z = entity.zo + (entity.getZ() - entity.zo) * factor;
        Vec3 start = new Vec3(x, y, z);
        float yawRadians = (float) (-yaw * ((float) Math.PI / 180) - (float) Math.PI);
        float pitchRadians = (float) (-pitch * ((float) Math.PI / 180));
        float cos = (float) Math.cos(yawRadians);
        float sin = (float) Math.sin(yawRadians);
        float negCos = -(float) Math.cos(pitchRadians);
        float sinPitch = (float) Math.sin(pitchRadians);
        float xOffset = sin * negCos;
        float zOffset = cos * negCos;
        Vec3 end = start.add(xOffset * 10.0, sinPitch * 10.0, zOffset * 10.0);
        return rayTrace(level, start, end, par3);
    }

    /**
     * The native block ray trace of the original's {@code World.getIntercept}.
     *
     * <p>The original passed two booleans: whether a block without a collision box still counts and whether fluids
     * count. The native trace takes the outline shape for the first and the fluid mode for the second.
     */
    private static BlockHitResult rayTrace(Level level, Vec3 start, Vec3 end, boolean blockIn) {
        ClipContext context = new ClipContext(start, end,
                blockIn ? ClipContext.Block.OUTLINE : ClipContext.Block.COLLIDER,
                blockIn ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY, (net.minecraft.world.entity.Entity) null);
        return level.clip(context);
    }

    // ------------------------------------------------------------------ neighbourhood

    /** Original {@code isBlockAdjacentToAtleast(IBlockAccess, int, int, int, Block, int, int)}. */
    public static boolean isBlockAdjacentToAtleast(BlockGetter level, int x, int y, int z, Block block, int amount) {
        return isBlockAdjacentToAtleast(level, x, y, z, block, amount, 1);
    }

    /**
     * Original {@code isBlockAdjacentToAtleast(IBlockAccess, int, int, int, Block, int, int, int)}.
     *
     * <p>The original compared the block and its metadata. The metadata {@code Short.MAX_VALUE} meant "any
     * metadata", which is the {@link Block} overload; an exact metadata is one exact {@link BlockState}, which is
     * the {@link BlockState} overload.
     */
    public static boolean isBlockAdjacentToAtleast(BlockGetter level, int x, int y, int z, Block block, int amount, int range) {
        int count = 0;
        for (int xx = -range; xx <= range; ++xx) {
            for (int yy = -range; yy <= range; ++yy) {
                for (int zz = -range; zz <= range; ++zz) {
                    if (xx == 0 && yy == 0 && zz == 0) continue;
                    if (level.getBlockState(new BlockPos(x + xx, y + yy, z + zz)).getBlock() == block) ++count;
                    if (count < amount) continue;
                    return true;
                }
            }
        }
        return count >= amount;
    }

    /** The metadata exact form of {@link #isBlockAdjacentToAtleast(BlockGetter, int, int, int, Block, int, int)}. */
    public static boolean isBlockAdjacentToAtleast(BlockGetter level, int x, int y, int z, BlockState state, int amount, int range) {
        int count = 0;
        for (int xx = -range; xx <= range; ++xx) {
            for (int yy = -range; yy <= range; ++yy) {
                for (int zz = -range; zz <= range; ++zz) {
                    if (xx == 0 && yy == 0 && zz == 0) continue;
                    if (level.getBlockState(new BlockPos(x + xx, y + yy, z + zz)) == state) ++count;
                    if (count < amount) continue;
                    return true;
                }
            }
        }
        return count >= amount;
    }

    /** Original {@code getContentsOfBlock(World, int, int, int)}. */
    public static List<ItemEntity> getContentsOfBlock(Level level, int x, int y, int z) {
        if (level == null) return List.of();
        BlockPos pos = new BlockPos(x, y, z);
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
    }

    /** Original {@code countExposedSides(World, int, int, int)}. */
    public static int countExposedSides(Level level, int x, int y, int z) {
        if (level == null) return 0;
        BlockPos pos = new BlockPos(x, y, z);
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).isAir()) continue;
            ++count;
        }
        return count;
    }

    /** Original {@code isBlockExposed(World, int, int, int)}. */
    public static boolean isBlockExposed(Level level, int x, int y, int z) {
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        for (Direction direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).isSolidRender(level, pos.relative(direction))) return true;
        }
        return false;
    }

    /** Original {@code isAdjacentToSolidBlock(World, int, int, int)}. */
    public static boolean isAdjacentToSolidBlock(Level level, int x, int y, int z) {
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        for (Direction direction : Direction.values()) {
            BlockPos side = pos.relative(direction);
            if (!level.getBlockState(side).isFaceSturdy(level, side, direction.getOpposite())) continue;
            return true;
        }
        return false;
    }

    /** Original {@code isBlockTouching(IBlockAccess, int, int, int, Block)}. */
    public static boolean isBlockTouching(BlockGetter level, int x, int y, int z, Block block) {
        if (level == null || block == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() == block) return true;
        }
        return false;
    }

    /** The metadata exact form of {@link #isBlockTouching(BlockGetter, int, int, int, Block)}. */
    public static boolean isBlockTouching(BlockGetter level, int x, int y, int z, BlockState state) {
        if (level == null || state == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)) == state) return true;
        }
        return false;
    }

    /**
     * Original {@code isBlockTouchingOnSide(IBlockAccess, int, int, int, Block, int, int)}.
     *
     * <p>The original side numbers were the {@code ForgeDirection} ordinals: 0 down, 1 up, 2 north, 3 south,
     * 4 west, 5 east. The native {@link Direction} has the same ordinals, so the original's comparisons are used
     * unchanged.
     */
    public static boolean isBlockTouchingOnSide(BlockGetter level, int x, int y, int z, BlockState state, Direction side) {
        if (level == null || state == null || side == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        int ordinal = side.ordinal();
        if (ordinal > 3 && matches(level, pos.offset(0, 0, 1), state) || ordinal > 3 && matches(level, pos.offset(0, 0, -1), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.offset(1, 0, 0), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.offset(-1, 0, 0), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.above(), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.below(), state)) return true;
        if (ordinal > 3 && matches(level, pos.above().offset(0, 0, 1), state)
                || ordinal > 3 && matches(level, pos.above().offset(0, 0, -1), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.above().offset(1, 0, 0), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.above().offset(-1, 0, 0), state)) return true;
        if (ordinal > 3 && matches(level, pos.below().offset(0, 0, 1), state)
                || ordinal > 3 && matches(level, pos.below().offset(0, 0, -1), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.below().offset(1, 0, 0), state)
                || ordinal > 1 && ordinal < 4 && matches(level, pos.below().offset(-1, 0, 0), state)) return true;
        switch (ordinal) {
            case 0: {
                if (!matches(level, pos.below(), state)) break;
                return true;
            }
            case 1: {
                if (!matches(level, pos.above(), state)) break;
                return true;
            }
        }
        return false;
    }

    private static boolean matches(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos) == state;
    }
}
