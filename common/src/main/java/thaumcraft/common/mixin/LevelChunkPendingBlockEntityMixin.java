package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.common.blocks.StateDependentBlockEntity;

/**
 * Silences the "Tried to load a block entity ... but failed" warning for the {@code DUMMY} marker of a
 * state that has no block entity.
 *
 * <p>While a chunk is generated, {@code WorldGenRegion#setBlock} stores a {@code DUMMY} block entity
 * marker for every state whose block is an {@code EntityBlock}. Promoting the marker asks the block for
 * the real entity, and a {@link StateDependentBlockEntity} block answers {@code null} for the variants
 * that never had a tile entity (the original decided per metadata, 1.21.1 decides per block). Only the
 * log line is wrapped: the entity of a state that has one is still created and registered by vanilla.
 * Any other failure, a saved entity that does not load or a block that is not marked, still warns.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkPendingBlockEntityMixin {
    /** The second warning of {@code promotePendingBlockEntity}, the one after the entity was not created. */
    @WrapOperation(method = "promotePendingBlockEntity",
            at = @At(value = "INVOKE",
                    target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                    ordinal = 1))
    private void thaumcraft$quietDummyOfStateWithoutBlockEntity(Logger logger, String message, Object state, Object pos,
            Operation<Void> original, @Local(argsOnly = true) CompoundTag tag) {
        if ("DUMMY".equals(tag.getString("id")) && state instanceof BlockState blockState
                && blockState.getBlock() instanceof StateDependentBlockEntity) {
            return;
        }
        original.call(logger, message, state, pos);
    }
}
