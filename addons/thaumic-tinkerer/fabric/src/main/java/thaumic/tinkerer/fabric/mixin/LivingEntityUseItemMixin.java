/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumic.tinkerer.common.hooks.TinkererEvents;

/**
 * Posts {@link TinkererEvents#USE_ITEM_TICK} at the head of {@code LivingEntity.updateUsingItem}, which both sides run once
 * per tick for an entity that holds the use of an item. The listener's result becomes the entity's own
 * {@code useItemRemaining}, so the {@code onUseTick} of the item that follows, the decrement and the completion check, and
 * everything that reads the count later ({@code getTicksUsingItem}, {@code getUseItemRemainingTicks}, the
 * {@code releaseUsing} of the bow) agree with it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityUseItemMixin {
    @Shadow
    protected int useItemRemaining;

    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void thaumictinkerer$useItemTick(ItemStack usingItem, CallbackInfo ci) {
        this.useItemRemaining = TinkererEvents.postUseItemTick((LivingEntity) (Object) this, usingItem, this.useItemRemaining);
    }
}
