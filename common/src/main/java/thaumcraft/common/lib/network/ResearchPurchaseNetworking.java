package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchPurchase;

/** Default TC4 Secondary purchase route; original primary notes are a separate action. */
public final class ResearchPurchaseNetworking {
    private static boolean initialized;
    private ResearchPurchaseNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,
                ResearchPurchasePayload.TYPE, ResearchPurchasePayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, payload.key());
                }));
    }

    /** Shared receiver body, also exercised by the real-loader regression tests. */
    public static ResearchPurchase.Result handle(ServerPlayer player, String key) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Research purchase requires the server thread");
        var result = ResearchPurchase.purchase(KnowledgeAccess.get(player), key);
        if (result.success()) {
            KnowledgeEvents.sync(player);
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "learn")),
                    SoundSource.PLAYERS, 0.75F, 1.0F);
            player.sendSystemMessage(Component.translatable("tc.port.research.success",
                    Component.translatable(ResearchCatalog.get(key).nameKey())));
        } else {
            // A known request can be based on an old snapshot. Correct the
            // client without changing authoritative knowledge on refusal.
            if (result.failure() != ResearchPurchase.Failure.UNKNOWN_RESEARCH
                    && result.failure() != ResearchPurchase.Failure.UNAVAILABLE) KnowledgeEvents.sync(player);
            player.sendSystemMessage(Component.translatable("tc.port.research.failure."
                    + result.failure().name().toLowerCase(Locale.ROOT)));
        }
        return result;
    }
}
