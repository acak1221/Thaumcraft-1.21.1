package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import thaumcraft.common.mixin.MerchantOfferMaxUsesAccess;

/**
 * The trade tables of the TC4 villages as the trade offers of a villager.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/VillageBankerManager.java:41-56} and
 * {@code thaumcraft/common/lib/world/VillageWizardManager.java:35-53}: the two managers of
 * the original were Forge village trade handlers. Every time the trade list of a villager of
 * the banker or of the wizard profession was built, the manager added its own recipes to
 * it. The recipes of the original are plain {@code MerchantRecipe} pairs: the item the
 * player pays with and the item the villager gives, both with a rolled amount.
 *
 * <p>The native tables live in {@link VillageWorldGenRules}; this class turns them into the
 * offers of a 1.21.1 villager and keeps the order of the rolls of the original: the amount
 * of the payment, then the amount of the reward, then the variant of the reward.
 */
public final class VillageTradeRules {
    private VillageTradeRules() {}

    /**
     * Adds the trades of the original to the trade list of a villager.
     *
     * @param villager the villager of the banker or of the wizard profession
     * @param trades the trade table of the original
     * @param random the random of the villager, the rolls of the original
     */
    public static void apply(Villager villager, List<VillageWorldGenRules.TradeSpec> trades, RandomSource random) {
        MerchantOffers offers = villager.getOffers();
        for (VillageWorldGenRules.TradeSpec trade : trades) {
            Item payment = item(trade.payItem());
            if (payment == null) continue;
            int paymentAmount = amount(trade.payMin(), trade.payMax(), random);
            int rewardAmount = amount(trade.receiveMin(), trade.receiveMax(), random);
            int variant = trade.receiveItems().size() == 1 ? 0 : random.nextInt(trade.receiveItems().size());
            Item reward = item(trade.receiveItems().get(variant));
            if (reward == null) continue;
            // Oracle VillageBankerManager:43 uses the ordinary MerchantRecipe constructor,
            // whose original default is seven uses. Native maxUses=0 means already exhausted.
            offers.add(new MerchantOffer(
                    new ItemCost(payment, paymentAmount),
                    new ItemStack(reward, rewardAmount),
                    7, 0, 1.0F));
        }
    }

    /** Original EntityVillager.useRecipe: trading an item-equivalent last offer arms the idle reset. */
    public static boolean startsOriginalRestock(Villager villager, MerchantOffer paid) {
        if (villager.level().isClientSide || !VillageProfessionBehaviors.hasOriginalProfession(villager) || paid == null) return false;
        MerchantOffers offers = villager.getOffers();
        if (offers.isEmpty()) return false;
        MerchantOffer last = offers.getLast();
        // Original comparison ignores amounts, metadata and NBT. Flattened native item IDs stay distinct;
        // no cross-family aliases are invented for the old metadata families.
        return sameItem(paid.getCostA(), last.getCostA()) && sameItem(paid.getResult(), last.getResult())
                && sameItem(paid.getCostB(), last.getCostB());
    }

    /** Original1.7 EntityVillager.updateAITick: replenish exhausted offers, then regenerate. */
    public static void finishOriginalRestock(Villager villager) {
        if (villager.level().isClientSide || !VillageProfessionBehaviors.hasOriginalProfession(villager)) return;
        MerchantOffers offers = villager.getOffers();
        if (offers.size() > 1) {
            for (MerchantOffer offer : offers) {
                if (!offer.isOutOfStock()) continue;
                int extra = villager.getRandom().nextInt(6) + villager.getRandom().nextInt(6) + 2;
                ((MerchantOfferMaxUsesAccess) offer).thaumcraft$setMaxUses(offer.getMaxUses() + extra);
            }
        }
        // Original also requests one new candidate recipe. This port already exposes the full TC4 table;
        // preserve that existing list rather than inventing Forge's unverified candidate-pool integration.
        villager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return first.isEmpty() ? second.isEmpty() : !second.isEmpty() && ItemStack.isSameItem(first, second);
    }

    /**
     * The amount of a trade of the original.
     *
     * <p>A bound pair that is equal is a fixed amount and does not roll, the same as the
     * original, which wrote the amount into the stack only when it was random.
     */
    private static int amount(int min, int max, RandomSource random) {
        return min == max ? min : min + random.nextInt(max - min + 1);
    }

    /** The item of a trade of the original, or null when the native tree does not carry it. */
    private static Item item(String id) {
        var location = ResourceLocation.tryParse(id);
        if (location == null) return null;
        return BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }
}
