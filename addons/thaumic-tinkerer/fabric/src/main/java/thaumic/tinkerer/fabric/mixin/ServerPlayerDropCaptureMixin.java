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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumic.tinkerer.fabric.hooks.DropCapture;

/**
 * The capture point for the inventory of a dying player: {@code Player.dropEquipment} calls {@code drop(stack, true, false)},
 * which {@code ServerPlayer} overrides to add the new item entity to the level. Like {@link EntityDropCaptureMixin}, the entity
 * goes to {@link DropCapture} when it is capturing this player's drops. A drop by the player's own hand (the drop key) is not
 * affected: no capture is open then.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDropCaptureMixin {
    @WrapOperation(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean thaumictinkerer$captureInventoryDrop(Level level, Entity spawned, Operation<Boolean> original) {
        if (spawned instanceof ItemEntity drop && DropCapture.offer((Entity) (Object) this, drop)) return true;
        return original.call(level, spawned);
    }
}
