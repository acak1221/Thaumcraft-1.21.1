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

/** TC4 basic Frost plus Frugal/Potency. No scattershot, boulder or alchemist branch. */
public final class ItemFocusFrost extends ItemFocusBasic {
    public static final int COOLDOWN_MILLIS = 200;
    private static final SoundEvent ICE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "ice"));

    public ItemFocusFrost(Properties properties) { super(properties); }

    @Override public AspectList getVisCost(ItemStack focus) {
        return new AspectList().add(Aspect.WATER, 5).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2);
    }
    @Override public int getActivationCooldown(ItemStack focus) { return COOLDOWN_MILLIS; }
    @Override public int getFocusColor(ItemStack focus) { return 5204428; }
    @Override public String getSortingHelper(ItemStack focus) { return "BF" + super.getSortingHelper(focus); }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return rank >= 1 && rank <= 5 ? new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency} : new FocusUpgradeType[0];
    }
    // Original inherits WAVE and a per-activation (not per-tick) cost.

    /** CENTIVIS, not ArcaneVis.price's whole-vis recipe units. Preserve FLOAT truncation. */
    public static Map<String, Integer> activationCost(ItemStack wand, Player actor) {
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
        int potency = FocusUpgradeData.level(FocusData.get(before), FocusUpgradeType.potency);
        // :61 pays ALL three aspects before even constructing the projectile.
        // The same atomic debit and FLOAT gear/cap modifier apply to creative.
        if (!FocusVis.debit(paid, costs)) return;
        if (player.getItemInHand(hand) != wand || !ItemStack.matches(wand, before)
                || !costs.equals(activationCost(wand, player)) || !FocusVis.canPay(wand, costs)) return;
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        player.getInventory().setChanged();

        EntityFrostShard shard = new EntityFrostShard(level, player, 1F);
        shard.setDamage(3F + potency * 1.5F);
        level.addFreshEntity(shard);
        // :83-85 ignores insertion refusal: one PAID attempt, no retry/refund,
        // and sound at the constructed shard even when it never entered the index.
        level.playSound(null, shard.getX(), shard.getY(), shard.getZ(), ICE, SoundSource.PLAYERS,
                .4F, 1F + level.random.nextFloat() * .1F);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
    }
}
