/**
 * Created by Katrina on 13/04/2015.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumic.tinkerer.common.block.tile.TileWardedSlab;
import thaumic.tinkerer.common.content.SimpleBlocks;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCraftingBenchRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;

/**
 * Paving Slab of Warding: the travel slab's cousin that repels mobs. Its block entity pushes non-player creatures
 * that float above it away and keeps the two blocks above it sealed with Thaumcraft's warding aura, unless a redstone
 * signal switches it off.
 *
 * <p>The 1.7.10 {@code getBlocksMovement} (passable for the path finder while unpowered) is the {@link #POWERED}
 * block-state property here, kept up to date by {@link #neighborChanged}: {@code isPathfindable} has no level to ask.
 * The 1.7.10 double slab block ({@code ward_slab_full}) is the vanilla double state of this slab.</p>
 */
public class BlockWardSlab extends SlabBlock implements EntityBlock, ITTinkererRegisterable {
    /** Whether a redstone signal reaches the slab (it keeps no aura and pushes nobody then). */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** The data recipe of the slab (1.7.10 name {@code PAVEWARD}). */
    public static final String RECIPE = "pave_ward_slab";

    public BlockWardSlab(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    /** Hardness 0.8, resistance 10 in the 1.7.10 setter (6 now). */
    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        return state.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
            boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    /** 1.7.10 {@code getBlocksMovement}: passable for the path finder exactly when no redstone signal reaches the slab. */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        if (type == PathComputationType.LAND) return !state.getValue(POWERED);
        return super.isPathfindable(state, type);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererCraftingBenchRecipe(RECIPE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileWardedSlab(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != SimpleBlocks.TILE_WARDED_SLAB.get()) return null;
        return (lvl, pos, st, be) -> TileWardedSlab.serverTick(lvl, pos, st, (TileWardedSlab) be);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        BlockTravelSlab.walkEffects(level, pos, state, this, entity);
        super.stepOn(level, pos, state, entity);
    }

    private static boolean isAiry(BlockState state) {
        BlockState aura = BlocksTC.state("blockAiry", 4);
        return aura != null && state.getBlock() == aura.getBlock();
    }

    /**
     * 1.7.10 {@code getBlocksMovement} of the block above: true for what does not stop movement (air, plants, water, a
     * carpet or a snow layer); {@code blocksMotion} is the solidity test of 1.21.1 that matches the old material flags (the
     * collision shape would call a carpet solid).
     */
    @SuppressWarnings("deprecation")
    private static boolean isPassable(BlockState state) {
        return !state.blocksMotion();
    }

    private static void runes(Level level, double x, double y, double z, float r, float g, float b, int duration, float gravity) {
        ThaumcraftClientFX.blockRunes(level, x, y, z, r, g, b, duration, gravity);
    }

    /** 1.7.10 {@code randomDisplayTick}: runes in the colour of the slab's state. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.is(this)) return;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (state.getValue(POWERED)) {
            for (int list = 0; list < ThaumcraftClientFX.particleCount(level, 2); ++list) {
                runes(level, x, y + 0.7F, z, 0.2F + random.nextFloat() * 0.4F, random.nextFloat() * 0.3F,
                        0.8F + random.nextFloat() * 0.2F, 20, -0.02F);
            }
        } else {
            BlockState above1 = level.getBlockState(pos.above());
            BlockState above2 = level.getBlockState(pos.above(2));
            if (!isAiry(above1) && isPassable(above1) || !isAiry(above2) && isPassable(above1)) {
                for (int list = 0; list < ThaumcraftClientFX.particleCount(level, 3); ++list) {
                    runes(level, x, y + 0.7F, z, 0.9F + random.nextFloat() * 0.1F, random.nextFloat() * 0.3F,
                            random.nextFloat() * 0.3F, 24, -0.02F);
                }
            } else {
                for (Entity entity : level.getEntities((Entity) null, new AABB(x, y, z, x + 1, y + 1, z + 1).inflate(1.0D, 1.0D, 1.0D))) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        runes(level, x, y + 0.6F + random.nextFloat() * Math.max(0.8F, entity.getEyeHeight()), z,
                                0.6F + random.nextFloat() * 0.4F, 0.0F, 0.3F + random.nextFloat() * 0.7F, 20, 0.0F);
                        break;
                    }
                }
            }
        }
    }
}
