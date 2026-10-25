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
import thaumcraft.common.entities.projectile.EntityPrimalOrb;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.primal.PrimalFocusRules;

/** Original primal focus: six independently clock-rerolled raw prices and optional rank-three Seeker. */
public final class ItemFocusPrimal extends ItemFocusBasic {
    private static final SoundEvent ICE=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","ice"));
    public ItemFocusPrimal(Properties properties){super(properties);}
    @Override public ResourceLocation getFocusDepthLayerIcon(ItemStack focus) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/focus_primal_depth.png");
    }
    public static AspectList costAtMillis(long millis){
        var cost=PrimalFocusRules.costAtMillis(millis);
        return new AspectList().add(Aspect.WATER,cost.aqua()).add(Aspect.AIR,cost.aer()).add(Aspect.EARTH,cost.terra())
                .add(Aspect.FIRE,cost.ignis()).add(Aspect.ORDER,cost.ordo()).add(Aspect.ENTROPY,cost.perditio());
    }
    @Override public AspectList getVisCost(ItemStack focus){return costAtMillis(System.currentTimeMillis());}
    @Override public int getActivationCooldown(ItemStack focus){return 500;}
    @Override public int getFocusColor(ItemStack focus){return 10854849;}
    @Override public String getSortingHelper(ItemStack focus){return "FP"+super.getSortingHelper(focus);}
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus,int rank){
        return switch(rank){
            case 1,2,4,5->new FocusUpgradeType[]{FocusUpgradeType.frugal};
            case 3->new FocusUpgradeType[]{FocusUpgradeType.frugal,FocusUpgradeType.seeker};
            default->new FocusUpgradeType[0];
        };
    }
    public static Map<String,Integer> activationCost(ItemStack wand,Player player,long millis){return FocusVis.price(costAtMillis(millis),wand,player);}
    @Override public ItemStack onFocusRightClick(ItemStack wand,Level level,Player player,InteractionHand hand){
        if(!level.isClientSide&&player instanceof ServerPlayer server)emit(wand,level,server,hand);
        player.swing(hand);return wand;
    }
    private static void emit(ItemStack wand,Level level,ServerPlayer player,InteractionHand hand){
        if(!player.server.isSameThread())throw new IllegalStateException("Primal focus use requires the server thread");
        if(player.level()!=level||!player.isAlive()||player.isSpectator()||player.getItemInHand(hand)!=wand||wand.getCount()!=1
                ||!ArcaneVis.isCraftingWand(wand)||!(FocusData.get(wand).getItem() instanceof ItemFocusPrimal))return;
        ItemStack before=wand.copy(),paid=before.copy(),focus=FocusData.get(before);
        // Capture the single original getVisCost call; crossing a200ms boundary cannot turn a paid cast into a second quotation.
        Map<String,Integer> cost=activationCost(before,player,System.currentTimeMillis());
        var orb=new EntityPrimalOrb(level,player,FocusUpgradeData.level(focus,FocusUpgradeType.seeker)>0);
        if(!FocusVis.debit(paid,cost))return;
        if(player.getItemInHand(hand)!=wand||!ItemStack.matches(wand,before)||!FocusVis.canPay(wand,cost))return;
        wand.set(ThaumcraftContent.WAND_DATA.get(),ItemWandCasting.data(paid));player.getInventory().setChanged();
        level.addFreshEntity(orb);
        // Original paid insertion attempt always sounds; no invented refund/retry when native entity insertion refuses.
        level.playSound(null,orb.getX(),orb.getY(),orb.getZ(),ICE,SoundSource.PLAYERS,.3F,.8F+level.random.nextFloat()*.1F);
        player.inventoryMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastChanges();
    }
}
