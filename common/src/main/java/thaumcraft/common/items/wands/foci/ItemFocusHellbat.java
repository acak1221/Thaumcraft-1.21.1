package thaumcraft.common.items.wands.foci;

import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original targeted Hellbat summon. Research gates creation/upgrading, never
 * casting a found focus; every actual cast retains the wrong-WAND base price. */
public final class ItemFocusHellbat extends ItemFocusBasic {
    private static final SoundEvent ICE=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","ice"));
    private static final SoundEvent FAIL=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","wandfail"));
    public ItemFocusHellbat(Properties properties) { super(properties); }
    @Override public String getSortingHelper(ItemStack focus) { return "HH"+super.getSortingHelper(focus); }
    @Override public int getFocusColor(ItemStack focus) { return 14431746; }
    @Override public int getActivationCooldown(ItemStack focus) { return 1000; }
    @Override public AspectList getVisCost(ItemStack stack) {
        if(stack.getItem() instanceof ItemFocusHellbat) {
            if(isUpgradedWith(stack,FocusUpgradeType.batbombs))return new AspectList().add(Aspect.FIRE,100).add(Aspect.ENTROPY,200).add(Aspect.AIR,100);
            if(isUpgradedWith(stack,FocusUpgradeType.devilbats))return new AspectList().add(Aspect.FIRE,100).add(Aspect.ENTROPY,100).add(Aspect.AIR,100).add(Aspect.EARTH,100);
        }
        return baseCost();
    }
    private static AspectList baseCost() { return new AspectList().add(Aspect.FIRE,200).add(Aspect.ENTROPY,100).add(Aspect.AIR,100); }
    public static Map<String,Integer> activationCost(ItemStack wand,Player actor) { return FocusVis.price(baseCost(),wand,actor); }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus,int rank) {
        return switch(rank) {
            case 1,2,4 -> new FocusUpgradeType[]{FocusUpgradeType.frugal,FocusUpgradeType.potency};
            case 3 -> new FocusUpgradeType[]{FocusUpgradeType.frugal,FocusUpgradeType.potency,FocusUpgradeType.batbombs,FocusUpgradeType.devilbats};
            case 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal,FocusUpgradeType.potency,FocusUpgradeType.vampirebats};
            default -> new FocusUpgradeType[0];
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus,Player player,FocusUpgradeType type,int rank) {
        if(!super.canApplyUpgrade(focus,player,type,rank))return false;
        // Keep the original actor-dependent query, including its null-player
        // exception. The original applyUpgrade API did NOT call this query.
        return type!=FocusUpgradeType.vampirebats || KnowledgeAccess.get(Objects.requireNonNull(player,"Vampire upgrade query requires an actor")).isResearchComplete("VAMPBAT");
    }
    @Override public boolean applyUpgrade(ItemStack focus,FocusUpgradeType type,int rank) {
        if(type!=FocusUpgradeType.vampirebats)return super.applyUpgrade(focus,type,rank);
        // Focal admission checks its real payer's research. Completion has no
        // actor and, like original applyUpgrade, performs the structural write.
        if(!super.canApplyUpgrade(focus,null,type,rank))return false;
        focus.set(ThaumcraftFoci.FOCUS_UPGRADES.get(),FocusUpgradeData.read(focus).withUpgrade(type,rank));return true;
    }
    @Override public ItemStack onFocusRightClick(ItemStack wand,Level world,Player player,InteractionHand hand) {
        if(!world.isClientSide) {
            if(!(player instanceof ServerPlayer server)||player.level()!=world)return wand;
            if(!server.server.isSameThread())throw new IllegalStateException("Hellbat focus requires the server thread");
            if(!bound(server,wand,hand))return wand;
        }
        Entity target=pointedEntity(player);
        if(!(target instanceof LivingEntity living))return wand;
        if(world.isClientSide) { player.swing(hand);return wand; }
        if(!(player instanceof ServerPlayer server) || target instanceof Player && !server.server.isPvpAllowed())return wand;
        cast(server,wand,hand,living);player.swing(hand);return wand;
    }
    private static void cast(ServerPlayer player,ItemStack wand,InteractionHand hand,LivingEntity target) {
        if(!player.server.isSameThread())throw new IllegalStateException("Hellbat focus requires the server thread");
        var level=player.serverLevel();
        if(!bound(player,wand,hand) || target.level()!=level || target.isRemoved() || !target.isAlive())return;
        ItemStack before=wand.copy();Vec3 position=player.position(),look=player.getLookAngle();
        Map<String,Integer> costs=activationCost(before,player);ItemStack paid=before.copy();
        float yaw=player.getYRot()/180F*3.141593F;
        double x=player.getX()-(double)(Mth.cos(yaw)*.16F)+look.x*.5;
        double y=player.getBoundingBox().minY+player.getBbHeight()/2F+.25-.05000000014901161+look.y*.5;
        double z=player.getZ()-(double)(Mth.sin(yaw)*.16F)+look.z*.5;
        EntityFireBat bat=ThaumcraftMonsters.FIREBAT.get().create(level);
        if(bat==null)return;
        bat.moveTo(x,y+bat.getBbHeight(),z,player.getYRot(),0);bat.setTarget(target);
        ItemStack focus=FocusData.get(before);
        bat.damBonus=FocusUpgradeData.level(focus,FocusUpgradeType.potency);
        bat.setIsSummoned(true);bat.setIsBatHanging(false);
        if(FocusUpgradeData.level(focus,FocusUpgradeType.devilbats)>0)bat.setIsDevil(true);
        if(FocusUpgradeData.level(focus,FocusUpgradeType.batbombs)>0)bat.setIsExplosive(true);
        if(FocusUpgradeData.level(focus,FocusUpgradeType.vampirebats)>0) { bat.owner=player;bat.setIsVampire(true); }
        // Native target callbacks run before payment. A redirected/refused
        // target or a changed hand/ray cannot inherit a pending summon.
        if(player.level()!=level || !bound(player,wand,hand) || !ItemStack.matches(wand,before)
                || !position.equals(player.position()) || !look.equals(player.getLookAngle())
                || target.level()!=level || target.isRemoved() || !target.isAlive() || bat.getTarget()!=target
                || target instanceof Player && !player.server.isPvpAllowed() || pointedEntity(player)!=target
                || !costs.equals(activationCost(wand,player))) { bat.discard();return; }
        // Original calls getVisCost(WAND): Bombs/Devil still spend200/100/100,
        // not their different tooltip costs. Creative needs and spends actual vis.
        if(!FocusVis.debit(paid,costs)) { bat.discard();fail(player);return; }
        wand.set(ThaumcraftContent.WAND_DATA.get(),ItemWandCasting.data(paid));player.getInventory().setChanged();
        if(level.addFreshEntity(bat)) {
            level.levelEvent(2004,new BlockPos((int)x,(int)y,(int)z),0);
            level.playSound(null,bat.getX(),bat.getY(),bat.getZ(),ICE,SoundSource.PLAYERS,.2F,.95F+level.random.nextFloat()*.1F);
        } else fail(player);
        player.inventoryMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastChanges();
    }
    private static void fail(ServerPlayer player) { player.serverLevel().playSound(null,player.getX(),player.getY(),player.getZ(),FAIL,SoundSource.PLAYERS,.1F,.8F+player.level().random.nextFloat()*.1F); }
    private static boolean bound(ServerPlayer player,ItemStack wand,InteractionHand hand) {
        return player.isAlive()&&!player.isRemoved()&&!player.hasDisconnected()&&!player.isSpectator()
                &&player.server.getPlayerList().getPlayer(player.getUUID())==player&&player.getItemInHand(hand)==wand&&wand.getCount()==1
                &&ArcaneVis.isCraftingWand(wand)&&FocusData.get(wand).getItem() instanceof ItemFocusHellbat;
    }
    /** Original EntityUtils.getPointedEntity(player,32,EntityFireBat.class):
     * exclude firebats, but a nearer nonliving pickable entity occludes a mob. */
    private static Entity pointedEntity(Player player) {
        Vec3 eye=player.getEyePosition(),look=player.getLookAngle(),end=eye.add(look.scale(32));
        var search=player.getBoundingBox().expandTowards(look.scale(32)).inflate(1.1F);
        Entity selected=null;double nearest=0;
        for(Entity candidate:player.level().getEntities(player,search)) {
            if(!candidate.isPickable()||candidate.isRemoved()||candidate instanceof EntityFireBat
                    ||player.level().clip(new ClipContext(eye,candidate.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=HitResult.Type.MISS)continue;
            var bounds=candidate.getBoundingBox().inflate(Math.max(.8F,candidate.getPickRadius()));
            if(bounds.contains(eye)) { selected=candidate;nearest=0;continue; }
            var intersection=bounds.clip(eye,end);if(intersection.isEmpty())continue;
            double distance=eye.distanceTo(intersection.get());if(nearest==0||distance<nearest) { selected=candidate;nearest=distance; }
        }
        return selected;
    }
}
