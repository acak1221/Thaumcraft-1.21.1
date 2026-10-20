package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.world.WardedBehaviour;

/** Native tag-based equivalent of original BlockWarded.isBeaconBase forwarding. */
@Mixin(BeaconBlockEntity.class)
public abstract class WardedBeaconMixin {
    @Redirect(method = "updateBase", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState thaumcraft$rememberedBeaconBase(Level level, BlockPos pos) { return WardedBehaviour.query(level, pos); }
}
