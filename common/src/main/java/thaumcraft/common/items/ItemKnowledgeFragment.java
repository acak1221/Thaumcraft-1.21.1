package thaumcraft.common.items;

import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.AspectPoolPayload;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.KnowledgeNotificationNetworking;

/** Original ItemResource metadata9 RMB: one physical fragment, six independent original1..2 primal RP draws. */
public final class ItemKnowledgeFragment extends Item {
    private static final ThreadLocal<Boolean> USING=new ThreadLocal<>();
    public ItemKnowledgeFragment(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level raw,Player player,InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        if(held.isEmpty()||held.getItem()!=this||player.level()!=raw||player.isSpectator())return InteractionResultHolder.fail(held);
        if(raw.isClientSide)return InteractionResultHolder.sidedSuccess(held,true);
        if(!(raw instanceof ServerLevel world)||!(player instanceof ServerPlayer actor)||USING.get()!=null)return InteractionResultHolder.fail(held);
        USING.set(true);
        try {
            boolean creative=actor.getAbilities().instabuild;
            if(!current(world,actor,hand,held,creative))return InteractionResultHolder.fail(held);
            ItemStack before=held.copy();var knowledge=KnowledgeAccess.get(actor);var beforeKnowledge=knowledge.save();var after=knowledge.copy();
            var packets=new ArrayList<AspectPoolPayload>();
            for(Aspect aspect:Aspect.getPrimalAspects()) {
                int amount=world.random.nextInt(2)+1;
                after.addAspectPool(aspect.getTag(),amount);
                packets.add(new AspectPoolPayload(aspect.getTag(),(short)amount,(short)after.getAspectPoolFor(aspect.getTag())));
            }
            if(!current(world,actor,hand,held,creative)||!ItemStack.matches(held,before)||KnowledgeAccess.get(actor)!=knowledge||!knowledge.save().equals(beforeKnowledge))return InteractionResultHolder.fail(held);
            // Native physical debit and preallocated knowledge replacement have no intervening world/network callbacks.
            if(!creative)held.shrink(1);
            knowledge.copyFrom(after);
            try { KnowledgeEvents.sync(actor); for(var packet:packets)KnowledgeNotificationNetworking.pool(actor,packet); }
            catch(RuntimeException failure) { Thaumcraft.LOG.warn("Knowledge fragment was consumed and its RP committed; feedback will not replay",failure); }
            return InteractionResultHolder.sidedSuccess(held,false);
        } finally { USING.remove(); }
    }
    private boolean current(ServerLevel world,ServerPlayer player,InteractionHand hand,ItemStack held,boolean creative) {
        return world.getServer().isSameThread()&&world.getServer().getLevel(world.dimension())==world&&player.level()==world
                &&world.getServer().getPlayerList().getPlayer(player.getUUID())==player&&!player.isRemoved()&&player.isAlive()&&!player.isSpectator()
                &&player.getAbilities().instabuild==creative&&player.getItemInHand(hand)==held&&!held.isEmpty()&&held.getItem()==this;
    }
}
