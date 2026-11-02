package thaumcraft.common.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.config.ThaumcraftVillageProfessions;
import thaumcraft.common.lib.world.VillageBankerManager;
import thaumcraft.common.lib.world.VillageTradeRules;
import thaumcraft.common.lib.world.VillageProfessionBehaviors;
import thaumcraft.common.lib.world.VillageWizardManager;

/**
 * The trade tables of the TC4 villages on the villagers of the two professions.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/VillageBankerManager.java:41-56} and
 * {@code thaumcraft/common/lib/world/VillageWizardManager.java:35-53}: the managers of the
 * original were Forge village trade handlers. They added their recipes to the trade list of
 * every villager of their profession. 1.21.1 builds the trade list of a villager in
 * {@code Villager#updateTrades}, which only knows the vanilla professions, so the two tables
 * of the original are added there.
 *
 * <p>The list is only filled once: the original rebuilt the whole list when the career of
 * the villager grew, and the native {@code updateTrades} is called again for that, so the
 * recipes of the original would be added twice without the guard.
 */
@Mixin(Villager.class)
public abstract class VillagerVillageTradesMixin {
    /** Original reset intent is transient; only a qualifying paid last-offer trade arms it. */
    @Unique private int thaumcraft$originalRestockTicks;

    @Inject(method = "rewardTradeXp(Lnet/minecraft/world/item/trading/MerchantOffer;)V", at = @At("TAIL"))
    private void thaumcraft$rememberOriginalLastTrade(MerchantOffer offer, CallbackInfo info) {
        if (VillageTradeRules.startsOriginalRestock((Villager) (Object) this, offer)) thaumcraft$originalRestockTicks = 40;
    }

    @Inject(method = "customServerAiStep()V", at = @At("TAIL"))
    private void thaumcraft$originalIdleRestock(CallbackInfo info) {
        Villager villager = (Villager) (Object) this;
        if (!VillageProfessionBehaviors.hasOriginalProfession(villager)) {
            thaumcraft$originalRestockTicks = 0;
            return;
        }
        if (thaumcraft$originalRestockTicks <= 0 || villager.getTradingPlayer() != null) return;
        if (--thaumcraft$originalRestockTicks == 0) VillageTradeRules.finishOriginalRestock(villager);
    }

    @Inject(method = "updateTrades", at = @At("HEAD"))
    private void thaumcraft$addVillageTrades(CallbackInfo info) {
        Villager villager = (Villager) (Object) this;
        if (villager.level().isClientSide) return;
        if (!villager.getOffers().isEmpty()) return;
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == ThaumcraftVillageProfessions.BANKER.get()) {
            VillageTradeRules.apply(villager, VillageBankerManager.trades(), villager.getRandom());
        } else if (profession == ThaumcraftVillageProfessions.WIZARD.get()) {
            VillageTradeRules.apply(villager, VillageWizardManager.trades(), villager.getRandom());
        }
    }
}
