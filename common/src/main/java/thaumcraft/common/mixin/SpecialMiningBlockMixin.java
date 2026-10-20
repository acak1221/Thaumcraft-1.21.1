package thaumcraft.common.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.utils.SpecialMining;

/**
 * The drop list of a broken block, as the original HarvestDropsEvent saw it. A player's break, the Excavation focus and the Arcane Bore
 * all reach their loot through this one static method with the miner and the tool; anything that asks for drops without a miner is untouched.
 */
@Mixin(Block.class)
public abstract class SpecialMiningBlockMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void thaumcraft$specialMining(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
                                                 Entity miner, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> result) {
        result.setReturnValue(SpecialMining.apply(result.getReturnValue(), level, pos, miner, tool));
    }
}
