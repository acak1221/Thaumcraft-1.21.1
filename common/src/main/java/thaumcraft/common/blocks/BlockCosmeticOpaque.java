package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original BlockCosmeticOpaque: the amber block, the amber brick and the
 * warded glass. The warded glass variant carries the owned block entity.
 */
public final class BlockCosmeticOpaque extends BaseEntityBlock {
    public static final MapCodec<BlockCosmeticOpaque> CODEC = simpleCodec(BlockCosmeticOpaque::new);
    /** Original BlockCosmeticOpaque metadata 0..2. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public BlockCosmeticOpaque(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    /**
     * A single original metadata value of the cosmetic opaque family. The legacy
     * registration names the warded glass, so this constructor keeps that shape
     * next to the default constructor.
     */
    public BlockCosmeticOpaque(Properties properties, int variant) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, variant));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockCosmeticOpaque - the amber block and brick let some light through. */
    @Override protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(VARIANT) <= 1 ? 3 : 0;
    }

    /** BlockCosmeticOpaque:216 - only the warded glass carries the owned block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!EldritchBlockRules.cosmeticOpaqueHasTile(state.getValue(VARIANT))) return null;
        // The warded glass and the eldritch opaque block are different blocks with different
        // owned tile types; the state tells which one is placed.
        var type = state.is(thaumcraft.common.config.ThaumcraftEldritchBlocks.COSMETIC_OPAQUE.get())
                ? thaumcraft.common.config.ThaumcraftEldritchBlocks.OWNED_TILE.get()
                : thaumcraft.common.config.ThaumcraftLegacyTiles.COSMETIC_OPAQUE_BLOCK_ENTITY.get();
        return new TileOwned(type, pos, state);
    }
}
