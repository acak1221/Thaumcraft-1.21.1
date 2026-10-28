/**
 * Created for the Thaumic Tinkerer imbued fires (the original header of this file names no author).
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.fire;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.PortalShape;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.BlockGas;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;

/**
 * The base of the six imbued fires. The age of the fire is the 1.7.10 metadata (0..15; a source, age 0, never burns
 * out). The fire does not spread like fire does but transmutes the blocks listed by {@link #getBlockTransformation()}
 * next to it, burns out when none is left, and catches only what it can transmute (and what burns, a little).
 *
 * <p>Differences forced by 1.21.1: the rule table is a list of predicates over block states, not a map over block ids
 * ({@link FireRules}); the flammability of other blocks is the table of 1.7.10 vanilla
 * ({@link FireRules#vanillaFlammability}); and the fire keeps the states of the vanilla fire model, so it climbs walls
 * and is drawn like fire. It extends {@code FireBlock}, so it is a block of the {@code minecraft:fire} tag, can be
 * punched out and sets entities on fire like any fire.</p>
 */
public abstract class BlockFireBase extends FireBlock implements ITTinkererRegisterable {

    protected BlockFireBase(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** The properties of every imbued fire: the vanilla fire's (light 15, no collision, replaceable, instant break). */
    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).replaceable().noCollission().instabreak()
                .lightLevel(state -> 15).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).noLootTable();
    }

    // ---- the rules ---------------------------------------------------------------------------------------------

    /** The transmutations of this fire, built the first time they are asked for (the tags are not bound earlier). */
    public abstract List<FireRules.Rule> getBlockTransformation();

    @Nullable
    protected FireRules.Rule ruleFor(BlockState state) {
        for (FireRules.Rule rule : getBlockTransformation()) {
            if (rule.matches(state)) return rule;
        }
        return null;
    }

    public boolean isTransmutationTarget(BlockState state) {
        return ruleFor(state) != null;
    }

    public boolean isTransmutationResult(BlockState state) {
        for (FireRules.Rule rule : getBlockTransformation()) {
            if (rule.producing(state)) return true;
        }
        return false;
    }

    public boolean isNeighborTarget(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos target = pos.relative(direction);
            if (!level.hasChunkAt(target)) continue;

            if (isTransmutationTarget(level.getBlockState(target))) {
                return true;
            }
        }
        return false;
    }

    /** 1.7.10 {@code tickRate}: two hundred ticks, or never when the fire mechanics are switched off. */
    public int tickRate(Level level) {
        return ConfigHandler.enableFireMechanics ? 200 : 0x7ffffff0;
    }

    /** 1.7.10 {@code getBlockFlammability}: transmutation targets burn surely, results never, the rest as in vanilla. */
    public int getBlockFlammability(BlockGetter level, BlockPos pos) {
        return flammabilityOf(level.getBlockState(pos));
    }

    private int flammabilityOf(BlockState state) {
        if (isTransmutationTarget(state)) {
            return 100;
        }
        if (isTransmutationResult(state)) {
            return 0;
        }

        return FireRules.vanillaFlammability(state);
    }

    /** What the vanilla fire asks to decide how a fire block looks (walls it climbs): whether a block can catch fire. */
    @Override
    protected boolean canBurn(BlockState state) {
        return flammabilityOf(state) > 0;
    }

    // ---- the states --------------------------------------------------------------------------------------------

    /** The fire block as the placement would make it at {@code pos} (the climbing flags), with the given age. */
    protected BlockState stateWithAge(BlockGetter level, BlockPos pos, int age) {
        return getStateForPlacement(level, pos).setValue(AGE, age);
    }

    public void setBlockWithTransmutationTarget(Level level, BlockPos pos, int meta, @Nullable Block block) {
        BlockState targetState = level.getBlockState(pos);
        FireRules.Rule rule = ruleFor(targetState);
        if (rule != null) {
            level.setBlock(pos, rule.result(targetState), Block.UPDATE_ALL);
        } else if (block == this) {
            level.setBlock(pos, stateWithAge(level, pos, meta), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, (block == null ? Blocks.AIR : block).defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // ---- the tick ---------------------------------------------------------------------------------------------

    private static boolean rainsAt(Level level, int x, int y, int z) {
        return level.isRainingAt(new BlockPos(x, y, z));
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (!(world.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK) && ConfigHandler.enableFire
                && ConfigHandler.enableFireMechanics)) {
            return;
        }

        if (world.isRaining() && isNearRain(world, pos)) {
            // Check if it's raining and if it can be put out by rain
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        final int blockMeta = state.getValue(AGE);

        // Source block doesn't burn out
        if (blockMeta != 0 && !isNeighborTarget(world, pos)) {
            // Extinguish if there's no valid fuel around
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;

        }

        // Check Transforms
        if (rand.nextInt(20) == 0 && (isNeighborTarget(world, pos))) {
            for (final Direction dir : Direction.values()) {
                final BlockPos target = pos.relative(dir);
                if (!world.hasChunkAt(target)) continue;
                final BlockState targetState = world.getBlockState(target);

                final FireRules.Rule rule = ruleFor(targetState);
                if (rule == null) continue;

                world.setBlock(target, rule.result(targetState), Block.UPDATE_ALL);
            }
        }

        if (blockMeta < 15 && blockMeta > 0) {
            // Randomly increase the age of a fire
            // 0 meta is considered a source and never increases, for any other meta we have a chance to increase
            final int newMeta = Math.min(blockMeta + rand.nextInt(3) / 2, 15);
            if (newMeta > blockMeta) {
                world.setBlock(pos, state.setValue(AGE, newMeta), Block.UPDATE_INVISIBLE);
            }
        } else if (blockMeta >= 15) {
            // No spreading if we're at max meta
            return;
        }

        world.scheduleTick(pos, this, this.tickRate(world) + rand.nextInt(3));

        final boolean highHumidity = world.getBiome(pos).is(BiomeTags.INCREASED_FIRE_BURNOUT);
        byte strMod = 0;

        if (highHumidity) {
            strMod = -50;
        }

        // Spread
        for (final Direction dir : Direction.values()) {
            this.tryCatchFire(world, pos.relative(dir), 300 + strMod, rand, blockMeta);
        }

        for (int xt = pos.getX() - 1; xt <= pos.getX() + 1; xt++) {
            for (int zt = pos.getZ() - 1; zt <= pos.getZ() + 1; zt++) {
                for (int yt = pos.getY() - 1; yt <= pos.getY() + 2; yt++) {
                    if (xt == pos.getX() && yt == pos.getY() && zt == pos.getZ()) {
                        continue;
                    }
                    BlockPos cell = new BlockPos(xt, yt, zt);
                    int fireChance = this.getChanceOfNeighborsEncouragingFire(world, cell);

                    if (fireChance <= 0) {
                        continue;
                    }
                    fireChance = ((fireChance + 70) / (blockMeta + 30)) + 70;
                    if (highHumidity) {
                        fireChance /= 2;
                    }

                    // (the third coordinate of the second test is z, not zt, in the 1.7.10 code)
                    if (fireChance <= 0 || rand.nextInt(100) > fireChance
                            || (world.isRaining() && rainsAt(world, xt, yt, zt))
                            || rainsAt(world, xt - 1, yt, pos.getZ())
                            || rainsAt(world, xt + 1, yt, zt)
                            || rainsAt(world, xt, yt, zt - 1)
                            || rainsAt(world, xt, yt, zt + 1)) {
                        continue;
                    }
                    final int targetMeta = Math.min(blockMeta + 1, 15);
                    setBlockWithTransmutationTarget(world, cell, targetMeta, this);
                }
            }
        }
    }

    private void tryCatchFire(Level world, BlockPos pos, int strength, RandomSource rand, int meta) {
        if (!world.hasChunkAt(pos)) return;
        final int blockFlammability = getBlockFlammability(world, pos);

        if (rand.nextInt(strength) >= blockFlammability) {
            return;
        }
        final boolean isTnt = world.getBlockState(pos).is(Blocks.TNT);

        if (rand.nextInt(meta + 10) < 5 && !world.isRainingAt(pos)) {
            // Newer fires have a higher chance of transmuting
            final int nextMeta = Math.max(Math.min(meta + rand.nextInt(5) / 4, 15), 1);
            setBlockWithTransmutationTarget(world, pos, nextMeta, this);
        } else {
            setBlockWithTransmutationTarget(world, pos, 0, Blocks.AIR);
        }

        if (isTnt) {
            TntBlock.explode(world, pos);
        }
    }

    /**
     * Gets the highest chance of a neighbor block encouraging this block to catch fire: sure (100) next to a block that
     * can be transmuted, if the cell is empty (a gas counts as empty, as in 1.7.10).
     */
    private int getChanceOfNeighborsEncouragingFire(Level world, BlockPos pos) {
        if (!world.hasChunkAt(pos) || !BlockGas.isAirLike(world.getBlockState(pos))) {
            return 0;
        } else {
            if (isNeighborTarget(world, pos)) {
                return 100;
            }
            return 0;
        }
    }

    // ---- placing, living and dying ----------------------------------------------------------------------------

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack itemStack) {
        if (entity instanceof Player player) {
            ThaumicTinkerer.LOG.info("Player: " + player.getGameProfile().getName() + " placed TT Fire");
        }
    }

    /**
     * 1.7.10 {@code canBlockStay} through {@code onNeighborBlockChange}: the fire needs a solid top surface below or a
     * transmutation target next to it. (Placing it anywhere is allowed: {@code ItemBlockFire} does not ask.)
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || isNeighborTarget(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? stateWithAge(level, pos, state.getValue(AGE)) : Blocks.AIR.defaultBlockState();
    }

    /**
     * 1.7.10 {@code onBlockAdded}: a fire in the Overworld or the Nether may light a nether portal frame; otherwise
     * its first tick is scheduled.
     */
    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock())) return;
        if (world.dimension() == Level.OVERWORLD || world.dimension() == Level.NETHER) {
            Optional<PortalShape> portal = PortalShape.findEmptyPortalShape(world, pos, Direction.Axis.X);
            if (portal.isPresent()) {
                portal.get().createPortalBlocks();
                return;
            }
        }
        world.scheduleTick(pos, this, this.tickRate(world) + world.random.nextInt(10));
    }
}
