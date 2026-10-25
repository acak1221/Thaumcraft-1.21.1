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
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.projectile.EntityPechBlast;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;

/** Original Pech curse focus, including the getVisCost(WAND) payment quirk. */
public final class ItemFocusPech extends ItemFocusBasic {
    private static final SoundEvent ICE=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","ice"));
    public ItemFocusPech(Properties properties) {super(properties);}
    @Override public ResourceLocation getFocusDepthLayerIcon(ItemStack focus) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/focus_pech_depth.png");
    }
    private static AspectList baseCost() {return new AspectList().add(Aspect.EARTH,10).add(Aspect.ENTROPY,10).add(Aspect.WATER,10);}
    @Override public AspectList getVisCost(ItemStack focus) {
        return focus.getItem() instanceof ItemFocusPech&&isUpgradedWith(focus,FocusUpgradeType.nightshade)?new AspectList().add(Aspect.AIR,10).add(Aspect.FIRE,10).add(Aspect.EARTH,10).add(Aspect.ORDER,10).add(Aspect.ENTROPY,10).add(Aspect.WATER,10):baseCost();
    }
    @Override public int getActivationCooldown(ItemStack focus) {return 250;}
    @Override public int getFocusColor(ItemStack focus) {return 0x229944;}
    @Override public String getSortingHelper(ItemStack focus) {return "PP"+super.getSortingHelper(focus);}
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus,int rank) {
        return switch(rank) {
            case 1,3 ->new FocusUpgradeType[]{FocusUpgradeType.frugal,FocusUpgradeType.potency};
            case 2,4 ->new FocusUpgradeType[]{FocusUpgradeType.frugal,FocusUpgradeType.potency,FocusUpgradeType.extend};
            case 5 ->new FocusUpgradeType[]{FocusUpgradeType.frugal,FocusUpgradeType.potency,FocusUpgradeType.nightshade};
            default ->new FocusUpgradeType[0];
        };
    }
    public static Map<String,Integer> activationCost(ItemStack wand,Player player) {return FocusVis.price(baseCost(),wand,player);}
    @Override public ItemStack onFocusRightClick(ItemStack wand,Level level,Player player,InteractionHand hand) {
        if(!level.isClientSide && player instanceof ServerPlayer server)emit(wand,level,server,hand);
        player.swing(hand);return wand;
    }
    private static void emit(ItemStack wand,Level level,ServerPlayer player,InteractionHand hand) {
        if(!player.server.isSameThread())throw new IllegalStateException("Pech focus use requires the server thread");
        if(player.level()!=level || !player.isAlive() || player.isSpectator() || player.getItemInHand(hand)!=wand || wand.getCount()!=1
                || !ArcaneVis.isCraftingWand(wand) || !(FocusData.get(wand).getItem() instanceof ItemFocusPech))return;
        ItemStack before=wand.copy(),paid=before.copy(),focus=FocusData.get(before);Map<String,Integer> cost=activationCost(before,player);
        int strength=ItemWandCasting.focusPotency(before),duration=FocusUpgradeData.level(focus,FocusUpgradeType.extend);
        boolean nightshade=FocusUpgradeData.level(focus,FocusUpgradeType.nightshade)>0;
        if(!FocusVis.debit(paid,cost))return;
        if(player.getItemInHand(hand)!=wand || !ItemStack.matches(wand,before) || !cost.equals(activationCost(wand,player)) || !FocusVis.canPay(wand,cost))return;
        wand.set(ThaumcraftContent.WAND_DATA.get(),ItemWandCasting.data(paid));player.getInventory().setChanged();
        var blast=new EntityPechBlast(level,player,strength,duration,nightshade);level.addFreshEntity(blast);
        // Original sound follows the paid insertion attempt even when insertion refuses; no retry/refund.
        level.playSound(null,blast.getX(),blast.getY(),blast.getZ(),ICE,SoundSource.PLAYERS,.4F,1+level.random.nextFloat()*.1F);
        player.inventoryMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastChanges();
    }
}
