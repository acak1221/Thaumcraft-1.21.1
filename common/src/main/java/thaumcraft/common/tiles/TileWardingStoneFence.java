package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileWardingStoneFence: the warding fence above a warding stone.
 * It breaks when the stone below it is gone.
 */
public final class TileWardingStoneFence extends BlockEntity {
    int count = 0;

    public TileWardingStoneFence(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.WARDING_FENCE_TILE.get(), pos, state);
    }

    /** Original TileWardingStoneFence:18-25 - the fence needs a warding stone. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileWardingStoneFence fence) {
        if (level.isClientSide) return;
        fence.count++;
        if (fence.count % 100 != 0) return;
        boolean stoneBelow = isWardingStone(level, pos.below());
        boolean stoneTwoBelow = isWardingStone(level, pos.below(2));
        if (!EldritchBlockRules.wardingStoneFenceSupported(stoneBelow, stoneTwoBelow)) {
            level.destroyBlock(pos, false);
        }
    }

    /** The original fence only survives above a warding stone. */
    private static boolean isWardingStone(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() == ThaumcraftEldritchBlocks.COSMETIC_SOLID.get()
                && state.getValue(thaumcraft.common.blocks.BlockCosmeticSolid.VARIANT) == EldritchBlockRules.COSMETIC_WARDING_STONE;
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        count = tag.getInt("count");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("count", count);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
