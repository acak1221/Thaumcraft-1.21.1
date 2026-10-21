package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.lib.baubles.PlayerAccessories;

public final class AccessoryNetworking {
    private static final Map<ServerPlayer,List<ItemStack>> LAST=new WeakHashMap<>();
    private static boolean initialized,client;
    private AccessoryNetworking(){}
    public static void init(){
        if(initialized)return;initialized=true;PlayerAccessories.initialize();
        if(Platform.getEnvironment()==Env.SERVER)NetworkManager.registerS2CPayloadType(AccessoryContentsPayload.TYPE,AccessoryContentsPayload.STREAM_CODEC);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,AccessoryOpenPayload.TYPE,AccessoryOpenPayload.STREAM_CODEC,(payload,context)->context.queue(()->{if(context.getPlayer() instanceof ServerPlayer player)open(player);}));
    }
    public static void initClient(){
        if(client)return;client=true;
        // The client context carries no player while the local player is being torn
        // down, so the snapshot is dropped instead of dereferencing a null player.
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AccessoryContentsPayload.TYPE,AccessoryContentsPayload.STREAM_CODEC,(payload,context)->context.queue(()->{var player=context.getPlayer();if(player!=null)PlayerAccessories.get(player).receive(payload.stacks());}));
    }
    public static void requestOpen(){NetworkManager.sendToServer(new AccessoryOpenPayload());}
    public static boolean open(ServerPlayer player){
        if(!player.server.isSameThread())throw new IllegalStateException("Accessory menu requires server thread");
        if(!player.isAlive()||player.isSpectator()||player.server.getPlayerList().getPlayer(player.getUUID())!=player
                ||player.containerMenu!=player.inventoryMenu||!player.inventoryMenu.getCarried().isEmpty())return false;
        dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(player,new SimpleMenuProvider(
                (id,inventory,owner)->new ContainerAccessories(id,inventory),Component.translatable("container.thaumcraft.accessories")),buffer->{});
        return player.containerMenu instanceof ContainerAccessories;
    }
    public static void sync(ServerPlayer player){
        if(player.connection==null)return;
        var stacks=PlayerAccessories.get(player).getItems().stream().map(ItemStack::copy).toList();
        LAST.put(player,stacks);NetworkManager.sendToPlayer(player,new AccessoryContentsPayload(stacks));
    }
    public static void syncChanged(ServerPlayer player){
        if(player.connection==null||player.server.getPlayerList().getPlayer(player.getUUID())!=player)return;
        var current=PlayerAccessories.get(player).getItems();var last=LAST.get(player);boolean changed=last==null;
        for(int i=0;!changed&&i<4;i++)changed=!ItemStack.matches(current.get(i),last.get(i));
        if(changed)sync(player);
    }
}
