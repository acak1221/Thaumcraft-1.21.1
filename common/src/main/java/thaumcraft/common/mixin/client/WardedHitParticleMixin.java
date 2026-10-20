package thaumcraft.common.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.client.fx.particles.WardHitParticle;
import thaumcraft.common.blocks.BlockWarded;

/** The original addHitEffects hook runs before vanilla's invisible-block particle refusal. */
@Mixin(ParticleEngine.class)
public abstract class WardedHitParticleMixin {
    @Shadow protected ClientLevel level;
    @Inject(method = "crack", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$wardImpact(BlockPos pos, Direction face, CallbackInfo callback) {
        if (level == null || !(level.getBlockState(pos).getBlock() instanceof BlockWarded)) return;
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5);
        if (Minecraft.getInstance().hitResult instanceof BlockHitResult actual && actual.getBlockPos().equals(pos)) hit = actual.getLocation();
        ((ParticleEngine) (Object) this).add(new WardHitParticle(level, pos, face,
                (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ())));
        callback.cancel();
    }
}
