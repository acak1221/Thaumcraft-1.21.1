package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.items.armor.Hover;

public final class HoverNetworking {
    private static boolean initialized;
    private HoverNetworking() {}
    public static void init() {if(initialized)return;initialized=true;Hover.initialize();NetworkManager.registerReceiver(NetworkManager.Side.C2S,HoverTogglePayload.TYPE,HoverTogglePayload.STREAM_CODEC,
            (payload,context)->context.queue(()->{if(context.getPlayer() instanceof ServerPlayer player)Hover.toggleHover(player);}));}
    public static void requestToggle() {NetworkManager.sendToServer(new HoverTogglePayload());}
}
