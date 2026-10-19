package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.research.TableTopology;

/** Original dummy half; its only native tick initializes post-load topology repair. */
public final class TileTable extends BlockEntity {
    private boolean topologyCheckPending = true;

    public TileTable(BlockPos pos, BlockState state) {
        super(ThaumcraftContent.TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override public void clearRemoved() {
        super.clearRemoved();
        topologyCheckPending = true;
    }

    public static void tickTopologyOnLoad(Level level, BlockPos pos, BlockState state, TileTable table) {
        if (level.isClientSide || !table.topologyCheckPending) return;
        table.topologyCheckPending = false;
        if (state.getBlock() instanceof BlockTable block) block.validateTopology(level, pos);
    }

    @Override public boolean isValidBlockState(BlockState state) {
        // Both native BE types accept the same block in their factory. Clients
        // skip same-block onRemove, so the variant must invalidate the old BE.
        return state.is(ThaumcraftContent.TABLE.get()) && !TableTopology.isMain(state.getValue(BlockTable.VARIANT));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
