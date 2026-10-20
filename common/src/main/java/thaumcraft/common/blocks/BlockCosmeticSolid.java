package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileWardingStone;

/**
 * Original BlockCosmeticSolid: the sixteen decorative stones of the Outer Lands.
 *
 * <p>The original is ONE block with sixteen metadata values; the metadata became the
 * {@link #VARIANT} block state. The two metadata values that carry a block entity, the warding
 * stone and the node, keep it.
 */
public final class BlockCosmeticSolid extends BaseEntityBlock {
    public static final MapCodec<BlockCosmeticSolid> CODEC = simpleCodec(BlockCosmeticSolid::new);
    /** Original BlockCosmeticSolid metadata 0..15. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    public BlockCosmeticSolid(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    /** The original metadata value of a state. */
    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockCosmeticSolid:227-239 - the per metadata light level. */
    public static int lightOf(BlockState state) { return EldritchBlockRules.cosmeticLight(state.getValue(VARIANT)); }

    /** BlockCosmeticSolid:278-296 - the warding stone and the node tile carry a block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int variant = state.getValue(VARIANT);
        if (variant == EldritchBlockRules.COSMETIC_WARDING_STONE) {
            return new TileWardingStone(pos, state);
        }
        if (variant == EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED) {
            return new TileNode(ThaumcraftEldritchBlocks.COSMETIC_NODE_TILE.get(), pos, state);
        }
        return null;
    }

    /** BlockCosmeticSolid:360-370 - the golem stone toggles with the redstone power. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        int variant = state.getValue(VARIANT);
        if (variant == EldritchBlockRules.COSMETIC_GOLEM_STONE && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.COSMETIC_GOLEM_STONE_ACTIVE), Block.UPDATE_ALL);
        } else if (variant == EldritchBlockRules.COSMETIC_GOLEM_STONE_ACTIVE && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(VARIANT, EldritchBlockRules.COSMETIC_GOLEM_STONE), Block.UPDATE_ALL);
        }
    }

    /** BlockCosmeticSolid:182-188 - the decorative stone is a full block. */
    @Override protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return true;
    }
}
