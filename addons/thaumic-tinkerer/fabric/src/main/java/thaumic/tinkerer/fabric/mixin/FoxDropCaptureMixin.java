/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.fabric.hooks.DropCapture;

/**
 * Opens the drop capture of {@link LivingEntityDropsMixin} early for the fox. {@code Fox.dropAllDeathLoot} drops the item in the
 * fox's mouth with {@code spawnAtLocation} <i>before</i> it calls {@code super.dropAllDeathLoot}, so the wrapper on
 * {@code LivingEntity.dropAllDeathLoot} would start too late to see it. Here the capture of this fox is begun around the whole
 * override; the wrapper on the living entity method finds it open for the same victim, goes on with it, ends it before it
 * posts the event, and so the mouth item is in the list with the rest of the loot. (NeoForge moves that drop into
 * {@code dropEquipment}, inside its capture, which gives the same result.) Nothing changes when nobody listens.
 */
@Mixin(Fox.class)
public abstract class FoxDropCaptureMixin {
    @WrapMethod(method = "dropAllDeathLoot")
    private void thaumictinkerer$captureMouthItem(ServerLevel level, DamageSource source, Operation<Void> original) {
        if (!TinkererEvents.hasLivingDropsListeners()) {
            original.call(level, source);
            return;
        }
        DropCapture capture = DropCapture.begin((Entity) (Object) this);
        try {
            original.call(level, source);
        } finally {
            capture.end(); // a no-op when the living entity wrapper has ended it already
        }
    }
}
