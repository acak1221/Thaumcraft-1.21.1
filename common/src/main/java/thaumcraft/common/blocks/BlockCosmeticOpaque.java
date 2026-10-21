package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

/**
 * Original BlockCosmeticOpaque: the amber block, the amber brick and the
 * warded glass. The warded glass variant carries the owned block entity.
 *
 * <p>Minecraft 1.21.1 binds the hardness and the blast resistance to the block and
 * not to the block state, so the warded glass (the original metadata two, which is
 * unbreakable and resists every explosion with 999) became a block of its own; the
 * amber block and the amber brick stay the metadata zero and one of this block.</p>
 */
public final class BlockCosmeticOpaque extends BaseEntityBlock {
    public static final MapCodec<BlockCosmeticOpaque> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 1).fieldOf("group").forGetter(BlockCosmeticOpaque::group),
            propertiesCodec()).apply(instance, BlockCosmeticOpaque::new));
    /** Original BlockCosmeticOpaque metadata 0..2. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);
    /** The amber block and the amber brick. */
    public static final int GROUP_AMBER = 0;
    /** The warded glass, the original metadata two. */
    public static final int GROUP_WARDED = 1;

    private final int group;

    public BlockCosmeticOpaque(int group, Properties properties) {
        super(properties);
        this.group = group;
        registerDefaultState(stateDefinition.any().setValue(VARIANT,
                group == GROUP_WARDED ? EldritchBlockRules.COSMETIC_OPAQUE_OWNED : 0));
    }

    /** The hardness group of this block. */
    public int group() { return group; }

    /** True when this block is the warded glass. */
    public boolean wardedGlass() { return group == GROUP_WARDED; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }

    public static int variant(BlockState state) { return state.getValue(VARIANT); }

    /** BlockCosmeticOpaque - the amber block and brick let some light through. */
    @Override protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(VARIANT) <= 1 ? 3 : 0;
    }

    /**
     * Original BlockCosmeticOpaque:253-259 - the warded glass is unbreakable while
     * the warded stone option is on and has a hardness of five otherwise; the amber
     * block and the amber brick keep the hardness of one and a half.
     *
     * <p>Minecraft 1.21.1 binds the destroy speed to the block state, so the
     * unbreakable state of the original is expressed as a mining progress of zero.</p>
     */
    @Override protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (wardedGlass() && ThaumcraftSettings.current().wardedStone()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** BlockCosmeticOpaque:216 - only the warded glass carries the owned block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!EldritchBlockRules.cosmeticOpaqueHasTile(state.getValue(VARIANT))) return null;
        // The warded glass is the metadata two of this block, so the block owns the owned tile.
        return new TileOwned(ThaumcraftEldritchBlocks.OWNED_TILE.get(), pos, state);
    }
}
