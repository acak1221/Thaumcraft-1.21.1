/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 22:13:36 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;

/**
 * The base of the three invisible gases (Gaseous Illuminae, Gaseous Tenebrae, Nitor Gas).
 *
 * <p>A gas is an invisible, intangible, replaceable block that drops nothing. Its 1.7.10 metadata is the
 * {@link #LEVEL} property: a freshly released cloud has level 4; every scheduled or random tick of a block with a
 * level above zero spreads gas of the next lower level into the six neighbouring air cells and drops itself to level
 * zero, so a released phial leaves a diamond of gas four cells wide behind (all blocks of level zero, which stay).</p>
 *
 * <p>The 1.7.10 gases were {@code Material.air} blocks and counted as air for {@code isAirBlock}. A light-emitting
 * air block would not light up in a chunk section that holds nothing else (the light engine keeps no data for
 * sections that hold only air), so here a gas is not an air block; {@link #isAirLike} stands in for the 1.7.10
 * {@code isAirBlock} wherever the add-on asks that question.</p>
 */
public abstract class BlockGas extends Block implements ITTinkererRegisterable {
    /** The 1.7.10 metadata of a gas: 0 (settled) to 15; 4 is what a phial releases. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 15);
    /** The level a released phial starts with ({@code par2World.setBlock(x, y, z, setBlock, 4, 2)}). */
    public static final int RELEASE_LEVEL = 4;

    protected BlockGas(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    /**
     * The properties every gas shares: no collision, no outline, no drops, replaceable by anything, destroyed by a
     * piston, ticking randomly (1.7.10 {@code setTickRandomly(true)}).
     */
    public static BlockBehaviour.Properties gasProperties() {
        return BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().instabreak().noLootTable()
                .randomTicks().pushReaction(PushReaction.DESTROY).noTerrainParticles();
    }

    /** What 1.7.10 {@code world.isAirBlock} answered: air itself, or one of the invisible gases. */
    public static boolean isAirLike(BlockState state) {
        return state.isAir() || state.getBlock() instanceof BlockGas;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    /** 1.7.10 {@code getDrops}: an empty list. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    /** 1.7.10 {@code updateTick}: spread the gas of the next lower level, then settle. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int meta = state.getValue(LEVEL);
        if (meta != 0) {
            // the order of the 1.7.10 code: -x, +x, -y, +y, -z, +z
            setAt(level, pos.relative(Direction.WEST), meta - 1);
            setAt(level, pos.relative(Direction.EAST), meta - 1);

            setAt(level, pos.below(), meta - 1);
            setAt(level, pos.above(), meta - 1);

            setAt(level, pos.relative(Direction.NORTH), meta - 1);
            setAt(level, pos.relative(Direction.SOUTH), meta - 1);

            // Just in case...
            level.setBlock(pos, state.setValue(LEVEL, 0), Block.UPDATE_CLIENTS);

            placeParticle(level, pos);
        }
    }

    /**
     * 1.7.10 {@code updateTick} answered the scheduled tick and the random tick alike ({@code setTickRandomly(true)});
     * in 1.21.1 the default {@code randomTick} does nothing, so the random tick runs the same code (a settled gas, level
     * zero, does nothing; the Nitor Gas checks for its carrier).
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }

    /** 1.7.10 {@code placeParticle}: the visual cue of a gas that appears or is dissipated. */
    public void placeParticle(Level level, BlockPos pos) {
        // NO-OP, override
    }

    private void setAt(Level level, BlockPos pos, int meta) {
        BlockState there = level.getBlockState(pos);
        if (isAirLike(there) && !there.is(this)) {
            if (!level.isClientSide) level.setBlock(pos, defaultBlockState().setValue(LEVEL, meta), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, 10);
        }
    }
}
