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
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.projectile.EntityFrostShard;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.FocusVis;

/** TC4 Frost, including its original scattershot, boulder and slowing branches. */
public final class ItemFocusFrost extends ItemFocusBasic {
    public static final int COOLDOWN_MILLIS = 200;
    private static final SoundEvent ICE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "ice"));

    public ItemFocusFrost(Properties properties) { super(properties); }

    @Override public AspectList getVisCost(ItemStack focus) {
        if (focus.getItem() instanceof ItemFocusFrost) {
            if (isUpgradedWith(focus, FocusUpgradeType.scattershot))
                return new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2).add(Aspect.AIR, 5);
            if (isUpgradedWith(focus, FocusUpgradeType.iceboulder))
                return new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2).add(Aspect.EARTH, 5);
        }
        return new AspectList().add(Aspect.WATER, 5).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2);
    }
    @Override public int getActivationCooldown(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgradeType.scattershot) || isUpgradedWith(focus, FocusUpgradeType.iceboulder) ? 500 : COOLDOWN_MILLIS;
    }
    @Override public int getFocusColor(ItemStack focus) { return 5204428; }
    @Override public String getSortingHelper(ItemStack focus) { return "BF" + super.getSortingHelper(focus); }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.alchemistsfrost};
            case 2, 4 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency};
            case 3 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.scattershot, FocusUpgradeType.iceboulder, FocusUpgradeType.alchemistsfrost};
            default -> new FocusUpgradeType[0];
        };
    }
    // Original inherits WAVE and a per-activation (not per-tick) cost.

    /** CENTIVIS, not ArcaneVis.price's whole-vis recipe units. Preserve FLOAT truncation. */
    public static Map<String, Integer> activationCost(ItemStack wand, Player actor) {
        // Original :61 calls getVisCost(WAND), so even specialized casts pay
        // this base price. Only the actual focus's tooltip advertises 20 aqua.
        return FocusVis.price(new AspectList().add(Aspect.WATER, 5).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2), wand, actor);
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) emit(wand, level, serverPlayer, hand);
        // :87 is unconditional once the dispatcher actually invokes the focus,
        // including an unfunded activation. An unexpired cooldown never invokes it.
        player.swing(hand);
        return wand;
    }

    private static void emit(ItemStack wand, Level level, ServerPlayer player, InteractionHand hand) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Frost focus use requires the server thread");
        if (player.level() != level || !player.isAlive() || player.isSpectator() || player.getItemInHand(hand) != wand
                || wand.getCount() != 1 || !ArcaneVis.isCraftingWand(wand)
                || !(FocusData.get(wand).getItem() instanceof ItemFocusFrost)) return;

        ItemStack before = wand.copy(), paid = before.copy();
        Map<String, Integer> costs = activationCost(before, player);
        int potency = ItemWandCasting.focusPotency(before);
        // :61 pays ALL three aspects before even constructing the projectile.
        // The same atomic debit and FLOAT gear/cap modifier apply to creative.
        if (!FocusVis.debit(paid, costs)) return;
        if (player.getItemInHand(hand) != wand || !ItemStack.matches(wand, before)
                || !costs.equals(activationCost(wand, player)) || !FocusVis.canPay(wand, costs)) return;
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        player.getInventory().setChanged();

        ItemStack focus = FocusData.get(before);
        int frost = FocusUpgradeData.level(focus, FocusUpgradeType.alchemistsfrost);
        boolean scatter = FocusUpgradeData.level(focus, FocusUpgradeType.scattershot) > 0;
        boolean boulder = FocusUpgradeData.level(focus, FocusUpgradeType.iceboulder) > 0;
        ItemStack committed = wand.copy();
        EntityFrostShard shard = null;
        for (int shot = 0; shot < (scatter ? 5 + potency * 2 : 1); shot++) {
            if (player.level() != level || !player.isAlive() || player.getItemInHand(hand) != wand || !ItemStack.matches(wand, committed)) break;
            shard = new EntityFrostShard(level, player, scatter ? 8F : 1F);
            shard.setDamage(scatter ? 1F : boulder ? 4F + potency * 2 : 3F + potency * 1.5F);
            shard.setFragile(scatter);
            if (boulder) shard.configureBoulder();
            shard.setFrosty(frost);
            level.addFreshEntity(shard);
        }
        // :83-85 ignores insertion refusal: one PAID attempt, no retry/refund,
        // and sound at the constructed shard even when it never entered the index.
        if (shard != null) level.playSound(null, shard.getX(), shard.getY(), shard.getZ(), ICE, SoundSource.PLAYERS,
                .4F, 1F + level.random.nextFloat() * .1F);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
    }
}
