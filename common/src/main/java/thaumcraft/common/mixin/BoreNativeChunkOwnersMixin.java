package thaumcraft.common.mixin;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumcraft.common.lib.bore.BoreNativeContext;

/** Shadow on the actual native declaring class so AP/runtime can remap the
 * protected field. Raw existing-owner read only; never exposes the mutable map. */
@Mixin(ChunkAccess.class)
public abstract class BoreNativeChunkOwnersMixin implements BoreNativeContext.Owners {
    @Shadow @Final protected Map<BlockPos, BlockEntity> blockEntities;
    @Shadow @Final protected Map<BlockPos, CompoundTag> pendingBlockEntities;
    @Override public BlockEntity thaumcraft$boreExistingOwner(BlockPos pos) { return blockEntities.get(pos); }
    @Override public boolean thaumcraft$boreHasPendingOwner(BlockPos pos) { return pendingBlockEntities.containsKey(pos); }
}
