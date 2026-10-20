package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.WarpMessageNetworking;
import thaumcraft.common.lib.warp.SanitySoapRules;

/** Original scrub/release, consuming one physical soap even in creative, not a food completion. */
public final class ItemSanitySoap extends Item {
    @FunctionalInterface public interface ClientEffects {void lather(Player player,boolean finished);}
    private static ClientEffects clientEffects;
    private static final ThreadLocal<Boolean> RELEASING=new ThreadLocal<>();
    public ItemSanitySoap(Properties properties){super(properties);}
    public static void setClientEffects(ClientEffects value){clientEffects=java.util.Objects.requireNonNull(value);}
    @Override public int getUseDuration(ItemStack stack,LivingEntity user){return SanitySoapRules.DURATION;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BLOCK;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(stack.isEmpty()||stack.getItem()!=this||player.level()!=level||player.isSpectator())return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(!(user instanceof Player player)||!active(level,player,stack,remaining))return;
        // EntityPlayer.stopUsingItem in1.7 invokes onPlayerStoppedUsing; native equivalent is releaseUsingItem.
        if(SanitySoapRules.finished(remaining))player.releaseUsingItem();
        // Original onUsingTick continues its ordinary lather after its automatic release callback.
        if(level.isClientSide&&clientEffects!=null)clientEffects.lather(player,false);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining){
        if(RELEASING.get()!=null||!(user instanceof Player player)||remaining<0||remaining>SanitySoapRules.DURATION
                ||!SanitySoapRules.finished(remaining)||!active(level,player,stack,remaining))return;
        if(!level.isClientSide&&(!(level instanceof ServerLevel server)||!(player instanceof ServerPlayer actor)
                ||!server.getServer().isSameThread()||server.getServer().getLevel(server.dimension())!=server||actor.isRemoved()||!actor.isAlive()
                ||server.getServer().getPlayerList().getPlayer(actor.getUUID())!=actor))return;
        RELEASING.set(Boolean.TRUE);
        try{
            boolean ward=player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD));
            boolean pure=level.getBlockState(BlockPos.containing(player.position())).is(ThaumcraftSpa.PURE_WATER.get());
            // Retire the physical payment before RNG, state mutation or any feedback callback. Creative consumes too.
            stack.shrink(1);
            if(player instanceof ServerPlayer actor&&level instanceof ServerLevel server){
                var result=SanitySoapRules.cleanse(KnowledgeAccess.get(actor),ward,pure,()->server.random.nextFloat());
                if(result.changed())KnowledgeEvents.sync(actor);
                if(result.stickyDelta()!=0)WarpMessageNetworking.send(actor,(byte)1,result.stickyDelta());
                if(result.temporaryDelta()!=0)WarpMessageNetworking.send(actor,(byte)2,result.temporaryDelta());
            }else if(level.isClientSide&&clientEffects!=null)clientEffects.lather(player,true);
        }finally{RELEASING.remove();}
    }
    private boolean active(Level level,Player player,ItemStack stack,int remaining){
        return player.level()==level&&!player.isSpectator()&&!stack.isEmpty()&&stack.getItem()==this&&player.isUsingItem()
                &&player.getUseItem()==stack&&player.getItemInHand(player.getUsedItemHand())==stack&&player.getUseItemRemainingTicks()==remaining;
    }
}
