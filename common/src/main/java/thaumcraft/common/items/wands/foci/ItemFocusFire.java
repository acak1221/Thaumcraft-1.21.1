package thaumcraft.common.items.wands.foci;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEmbers;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.projectile.EntityExplosiveOrb;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.WandVisDiscount;

/** TC4 Fire with its fireball, focused beam and alchemist's-fire branches. */
public final class ItemFocusFire extends ItemFocusBasic {
    public static final int BASE_COST_CENTIVIS = 10;
    private static final SoundEvent FIRE_LOOP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "fireloop"));
    // The original item singleton throttles this sound globally in wall-clock time.
    private long soundDelay;

    public ItemFocusFire(Properties properties) { super(properties); }

    @Override public AspectList getVisCost(ItemStack focusstack) {
        if (focusstack.getItem() instanceof ItemFocusFire) {
            if (isUpgradedWith(focusstack, FocusUpgradeType.firebeam)) return new AspectList().add(Aspect.FIRE, 10).add(Aspect.ORDER, 3);
            if (isUpgradedWith(focusstack, FocusUpgradeType.fireball)) return new AspectList().add(Aspect.FIRE, 66).add(Aspect.ENTROPY, 33);
        }
        return new AspectList().add(Aspect.FIRE, BASE_COST_CENTIVIS);
    }
    @Override public int getActivationCooldown(ItemStack focus) { return isUpgradedWith(focus, FocusUpgradeType.fireball) ? 1000 : 0; }
    @Override public boolean isVisCostPerTick(ItemStack focusstack) { return true; }
    @Override public int getFocusColor(ItemStack focusstack) { return 15028484; }
    @Override public String getSortingHelper(ItemStack focusstack) { return "AF" + super.getSortingHelper(focusstack); }
    @Override public WandFocusAnimation getAnimation(ItemStack focusstack) { return isUpgradedWith(focusstack, FocusUpgradeType.fireball) ? WandFocusAnimation.WAVE : WandFocusAnimation.CHARGE; }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency};
            case 2, 4 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.alchemistsfire};
            case 3 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.fireball, FocusUpgradeType.firebeam};
            default -> new FocusUpgradeType[0];
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgradeType type, int rank) {
        return super.canApplyUpgrade(focus, player, type, rank)
                && (type != FocusUpgradeType.alchemistsfire || !isUpgradedWith(focus, FocusUpgradeType.fireball)
                    || !isUpgradedWith(focus, FocusUpgradeType.alchemistsfire));
    }

    @Override public ItemStack onFocusRightClick(ItemStack wandstack, Level level, Player player, InteractionHand hand) {
        if (isUpgradedWith(FocusData.get(wandstack), FocusUpgradeType.fireball)) {
            if (level.isClientSide ? FocusVis.canPay(wandstack, Map.of("ignis", tickCost(wandstack, player)))
                    : player instanceof ServerPlayer server && fireball(wandstack, server, hand)) player.swing(hand);
            return wandstack;
        }
        // Original :102-105 does NOT require research, sneaking, or an affordable
        // first tick. Research gates crafting; an empty wand still begins use.
        player.startUsingItem(hand);
        WandFocusActions.clearCooldown(player);
        return wandstack;
    }

    private static boolean fireball(ItemStack wand, ServerPlayer player, InteractionHand hand) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Fireball focus requires the server thread");
        if (!player.isAlive() || player.isSpectator() || player.getItemInHand(hand) != wand || wand.getCount() != 1
                || !ArcaneVis.isCraftingWand(wand) || !(FocusData.get(wand).getItem() instanceof ItemFocusFire)) return false;
        ItemStack before = wand.copy(), paid = before.copy();
        // Original :99 passes the WAND to getVisCost: actual Fireball still pays
        // base ignis10, rather than the focus tooltip's ignis66/perditio33.
        Map<String, Integer> costs = Map.of("ignis", tickCost(before, player));
        if (!FocusVis.debit(paid, costs) || player.getItemInHand(hand) != wand || !ItemStack.matches(wand, before)
                || !costs.equals(Map.of("ignis", tickCost(wand, player))) || !FocusVis.canPay(wand, costs)) return false;
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid)); player.getInventory().setChanged();
        ItemStack focus = FocusData.get(before);
        var orb = new EntityExplosiveOrb(player.level(), player);
        orb.setStrength(1F + ItemWandCasting.focusPotency(before) * .4F);
        orb.setFiery(FocusUpgradeData.level(focus, FocusUpgradeType.alchemistsfire) > 0);
        player.serverLevel().addFreshEntity(orb);
        // Vanilla1.7 event1009's volume2 fireball launch became native event1018
        // (Blaze shoot), rather than the volume10 Ghast launch event1016.
        player.serverLevel().levelEvent(null, 1018, net.minecraft.core.BlockPos.containing((int)player.getX(), (int)player.getY(), (int)player.getZ()), 0);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    /** Original ItemWandCasting:247-259,318-335: clamp FLOAT modifier, multiply, truncate. */
    public static int tickCost(ItemStack wand, Player actor) {
        return (int) (BASE_COST_CENTIVIS * WandVisDiscount.focusModifier(wand, actor, Aspect.FIRE));
    }

    @Override public void onUsingFocusTick(ItemStack wandstack, Player actor, int count) {
        if (!(actor instanceof ServerPlayer player) || player.level().isClientSide) return;
        if (!player.server.isSameThread()) throw new IllegalStateException("Fire focus use requires the server thread");
        InteractionHand hand = player.getUsedItemHand();
        if (!player.isAlive() || player.isSpectator() || !player.isUsingItem()
                || player.getUseItem() != wandstack || player.getItemInHand(hand) != wandstack
                || wandstack.getCount() != 1 || !ArcaneVis.isCraftingWand(wandstack)
                || !(FocusData.get(wandstack).getItem() instanceof ItemFocusFire)) return;

        ItemStack before = wandstack.copy();
        if (FocusUpgradeData.level(FocusData.get(before), FocusUpgradeType.fireball) > 0) return;
        int price = tickCost(before, player);
        Map<String, Integer> costs = Map.of("ignis", price);
        ItemStack paid = before.copy();
        // ArcaneVis.price is for WHOLE-VIS recipes and must not be used here.
        // Like the original, creative players also need and spend actual vis.
        if (!FocusVis.debit(paid, costs)) {
            player.stopUsingItem();
            return;
        }
        ItemStack current = player.getItemInHand(hand);
        if (current != wandstack || player.getUseItem() != current || !player.isUsingItem()
                || !ItemStack.matches(current, before) || price != tickCost(current, player)
                || !FocusVis.canPay(current, costs)) return;

        // Mutate the actual hand's component, not the private copy or the whole
        // held stack: native continuous use keeps its original stack identity.
        current.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        player.getInventory().setChanged();
        var castLevel = player.serverLevel();
        int potency = ItemWandCasting.focusPotency(current);
        int fiery = FocusUpgradeData.level(FocusData.get(current), FocusUpgradeType.alchemistsfire);
        boolean beam = FocusUpgradeData.level(FocusData.get(current), FocusUpgradeType.firebeam) > 0;
        ItemStack committed = current.copy();

        long now = System.currentTimeMillis();
        if (soundDelay < now) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    FIRE_LOOP, SoundSource.PLAYERS, .33F, 2F);
            soundDelay = now + 500L;
        }
        // Original :122-137 pays ONCE BEFORE 2+Potency spawn attempts and ignores
        // spawn rejection. Cancelled spawns still cost vis; no retry or refund.
        for (int shot = 0; shot < 2 + potency; shot++) {
            // A spawn callback cannot lend the paid old channel to a replacement.
            if (player.getItemInHand(hand) != current || player.getUseItem() != current || !player.isUsingItem()
                    || !player.isAlive() || player.level() != castLevel || !ItemStack.matches(current, committed)) break;
            ThaumcraftEmbers.emit(player, beam ? (2F + potency + .5F) * 1.5F : 2F + potency,
                    beam ? .25F : 15F, beam ? 30 : 20, 3 + fiery);
        }
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
    }
}
