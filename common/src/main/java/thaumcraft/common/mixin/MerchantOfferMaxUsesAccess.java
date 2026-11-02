package thaumcraft.common.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Original1.7 merchant replenishment increases maximum uses while preserving the paid uses count. */
@Mixin(MerchantOffer.class)
public interface MerchantOfferMaxUsesAccess {
    @Mutable
    @Accessor("maxUses")
    void thaumcraft$setMaxUses(int maxUses);
}
