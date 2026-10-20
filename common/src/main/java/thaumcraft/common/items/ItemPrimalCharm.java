package thaumcraft.common.items;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.KnowledgeNotificationNetworking;
import thaumcraft.common.lib.research.PrimalCharmRules;
import thaumcraft.common.lib.utils.AspectOrbQueue;

/** Original ItemResource15 passive inventory update; ordinary dropped items do NOT run this hook. */
public final class ItemPrimalCharm extends Item {
    private static Supplier<? extends Player> tooltipViewer=()->null;
    private static final ThreadLocal<Frame> ACTIVE=new ThreadLocal<>();
    private static final class Frame {
        final ItemPrimalCharm item;final ServerLevel level;final ServerPlayer player;final ItemStack stack;final int slot,count;
        ItemStack witness;Aspect emission;boolean issued;
        Frame(ItemPrimalCharm item,ServerLevel level,ServerPlayer player,ItemStack stack,int slot) {
            this.item=item;this.level=level;this.player=player;this.stack=stack;this.slot=slot;count=stack.getCount();witness=stack.copy();
        }
    }
    public ItemPrimalCharm(Properties properties) { super(properties.stacksTo(1)); }
    public static void setTooltipViewer(Supplier<? extends Player> viewer) { tooltipViewer=Objects.requireNonNull(viewer); }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flags) {
        Player viewer=tooltipViewer.get();
        if(viewer!=null&&viewer.level().isClientSide) {
            int hint=PrimalCharmRules.tooltip(stack.hashCode(),viewer.tickCount);
            if(hint>=0)tooltip.add(Component.translatable("tc.primalcharm."+hint).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack,context,tooltip,flags);
    }
    @Override public void inventoryTick(ItemStack stack,Level world,Entity owner,int slot,boolean selected) {
        super.inventoryTick(stack,world,owner,slot,selected);
        if(ACTIVE.get()!=null||!(world instanceof ServerLevel level)||!(owner instanceof ServerPlayer player)
                ||!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level
                ||stack.isEmpty()||stack.getItem()!=this||slot<0||slot>=player.getInventory().items.size()
                ||player.getInventory().items.get(slot)!=stack)return;
        Frame frame=new Frame(this,level,player,stack,slot);ACTIVE.set(frame);
        try {
            if(!current(frame))return;
            int roll=PrimalCharmRules.worldRoll(level.random::nextInt);
            if(!current(frame))return;
            // Original clears this obsolete NBT key AFTER nextInt20000 and BEFORE a possible nextInt6.
            CustomData data=stack.get(DataComponents.CUSTOM_DATA);
            if(data!=null&&data.contains("blurb")) {
                var tag=data.copyTag();tag.remove("blurb");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));frame.witness=stack.copy();
            }
            if(PrimalCharmRules.emits(roll)) {
                frame.emission=PrimalCharmRules.aspect(level.random::nextInt);
                AspectOrbQueue.issuePrimalCharm(level,player,stack,slot,frame.emission);
            } else if(roll==42) {
                var knowledge=KnowledgeAccess.get(player);
                if(PrimalCharmRules.clues(roll,knowledge.isResearchComplete("FOCUSPRIMAL"),knowledge.isResearchComplete("@FOCUSPRIMAL"))&&current(frame)
                        &&knowledge.addResearch("@FOCUSPRIMAL")) {
                    player.sendSystemMessage(Component.translatable("tc.primalcharm.trigger").withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
                    KnowledgeEvents.sync(player);
                    KnowledgeNotificationNetworking.research(player,"@FOCUSPRIMAL");
                }
            }
        } finally { ACTIVE.remove(); }
    }
    /** Consumes exactly this canonical native inventory tick's successful original chance, never an item/vis debit. */
    public static boolean authorizeOrbEmission(ServerLevel level,ServerPlayer player,ItemStack stack,int slot,Aspect aspect,Vec3 position) {
        Frame frame=ACTIVE.get();
        if(frame==null||frame.issued||frame.level!=level||frame.player!=player||frame.stack!=stack||frame.slot!=slot||frame.emission!=aspect
                ||!player.position().equals(position)||!current(frame))return false;
        frame.issued=true;return true;
    }
    private static boolean current(Frame frame) {
        ServerPlayer player=frame.player;ServerLevel level=frame.level;
        return level.getServer().isSameThread()&&level.getServer().getLevel(level.dimension())==level
                &&player.level()==level&&!player.isRemoved()&&level.getServer().getPlayerList().getPlayer(player.getUUID())==player
                &&player.getInventory().items.get(frame.slot)==frame.stack&&frame.stack.getItem()==frame.item
                &&frame.stack.getCount()==frame.count&&frame.count>0&&ItemStack.isSameItemSameComponents(frame.stack,frame.witness);
    }
}
