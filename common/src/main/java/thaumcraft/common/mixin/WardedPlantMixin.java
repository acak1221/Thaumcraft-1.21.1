package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.world.WardedBehaviour;

/** Native soil/tag checks that replace Forge's original source-block canSustainPlant forwarding. */
@Mixin({BushBlock.class, CactusBlock.class, MushroomBlock.class, SugarCaneBlock.class})
public abstract class WardedPlantMixin {
    @Redirect(method = "canSurvive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelReader;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState thaumcraft$rememberedSoil(LevelReader level, BlockPos pos) { return WardedBehaviour.query(level, pos); }
}
