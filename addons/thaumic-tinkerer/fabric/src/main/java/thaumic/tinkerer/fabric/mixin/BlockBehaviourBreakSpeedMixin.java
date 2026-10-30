/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumic.tinkerer.common.hooks.TinkererEvents;

/**
 * Posts {@link TinkererEvents#BREAK_SPEED} where the dig speed enters the destroy progress:
 * {@code BlockBehaviour.getDestroyProgress} computes {@code player.getDestroySpeed(state) / hardness / (30 or 100)}, and
 * the call of {@code Player.getDestroySpeed} is wrapped so the listeners see the position as well (the method itself has none).
 * Both sides run {@code getDestroyProgress} (the client predicts the crack from it), and so does any code that asks a
 * player's progress, a never-ticked fake player included, so the event is posted for all of them. A speed of zero (a listener
 * said the player cannot dig the block) gives a progress of zero.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourBreakSpeedMixin {
    @WrapOperation(method = "getDestroyProgress", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
    private float thaumictinkerer$breakSpeed(Player player, BlockState state, Operation<Float> original,
            @Local(argsOnly = true) BlockPos pos) {
        float speed = original.call(player, state);
        return TinkererEvents.postBreakSpeed(player, state, pos, speed);
    }
}
