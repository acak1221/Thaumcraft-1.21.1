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
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.fabric.hooks.DropCapture;

/**
 * Posts {@link TinkererEvents#LIVING_DROPS}. {@code LivingEntity.dropAllDeathLoot} produces the whole death loot: the loot
 * table and the custom loot ({@code spawnAtLocation}), the equipment and a player's inventory, and the experience. It is
 * wrapped: while the original runs, the item entities that the victim itself spawns are collected by
 * {@link DropCapture} ({@code EntityDropCaptureMixin}, {@code ServerPlayerDropCaptureMixin}) instead of entering the level;
 * afterwards the listeners edit the list, and what is left is added to the level in order, unless they cancelled.
 * Nothing changes when nobody listens: the original runs as it is.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDropsMixin {
    @Shadow
    protected int lastHurtByPlayerTime;

    @WrapMethod(method = "dropAllDeathLoot")
    private void thaumictinkerer$postDrops(ServerLevel level, DamageSource source, Operation<Void> original) {
        if (!TinkererEvents.hasLivingDropsListeners()) {
            original.call(level, source);
            return;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        DropCapture capture = DropCapture.begin(victim);
        try {
            original.call(level, source);
        } finally {
            capture.end();
        }
        List<ItemEntity> drops = capture.drops();
        boolean cancelled = TinkererEvents.postLivingDrops(victim, source, drops, TinkererEvents.lootingLevel(level, source),
                this.lastHurtByPlayerTime > 0);
        if (cancelled) return;
        for (ItemEntity drop : drops) level.addFreshEntity(drop);
    }
}
