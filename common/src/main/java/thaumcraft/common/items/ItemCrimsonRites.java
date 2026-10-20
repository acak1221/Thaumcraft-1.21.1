package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;
import thaumcraft.common.lib.network.KnowledgeNotificationNetworking;
import thaumcraft.common.lib.research.CrimsonRitesResearch;

/** Original ItemEldritchObject metadata1. Studying is reusable even in survival. */
public final class ItemCrimsonRites extends Item {
    public ItemCrimsonRites(Properties properties){super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(stack.isEmpty()||stack.getItem()!=this||player.level()!=level||player.isSpectator())return InteractionResultHolder.fail(stack);
        if(level instanceof ServerLevel server&&player instanceof ServerPlayer actor){
            if(!server.getServer().isSameThread()||server.getServer().getLevel(server.dimension())!=server||actor.isRemoved()||!actor.isAlive()
                    ||server.getServer().getPlayerList().getPlayer(actor.getUUID())!=actor)return InteractionResultHolder.fail(stack);
            if(CrimsonRitesResearch.study(KnowledgeAccess.get(actor),WarpEvents.isWussMode())){
                KnowledgeEvents.sync(actor);KnowledgeNotificationNetworking.completed(actor,List.of(CrimsonRitesResearch.KEY));
                level.playSound(null,player.blockPosition(),SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","learn")),SoundSource.PLAYERS,.75F,1F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);
        tooltip.add(Component.translatable("item.ItemEldritchObject.text.2").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.ItemEldritchObject.text.3").withStyle(ChatFormatting.DARK_BLUE));
    }
}
