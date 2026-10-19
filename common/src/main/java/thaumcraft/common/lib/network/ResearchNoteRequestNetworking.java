package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchNoteIssuance;

/** Original type1 action, now validated against its actual sender on the server thread. */
public final class ResearchNoteRequestNetworking {
    private static boolean initialized;
    private ResearchNoteRequestNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,
                ResearchNoteRequestPayload.TYPE, ResearchNoteRequestPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, payload.key());
                }));
    }

    /** Also exercised by real-loader tests; a direct call does not prove C2S dispatch. */
    public static ResearchNoteIssuance.Result handle(ServerPlayer player, String key) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Note issuance requires the server thread");
        var result = ResearchNoteIssuance.issue(player, key);
        // Correct stale client inventory even on refusal. No optimistic item,
        // paper or ink changes are accepted from the request.
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        if (result.failure() != ResearchNoteIssuance.Failure.UNKNOWN_RESEARCH
                && result.failure() != ResearchNoteIssuance.Failure.UNAVAILABLE) KnowledgeEvents.sync(player);
        if (result.success()) {
            // PacketPlayerCompleteToServer:104 played learn for type1 too,
            // including a server-reused existing note. Issuance grants no research.
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "learn")),
                    SoundSource.PLAYERS, 0.75F, 1.0F);
            String message = result.reused() ? "tc.port.note.reused"
                    : result.discarded() ? "tc.port.note.discarded" : "tc.port.note.success";
            player.sendSystemMessage(Component.translatable(message,
                    Component.translatable(ResearchCatalog.get(key).nameKey())));
        } else {
            player.sendSystemMessage(Component.translatable("tc.port.note.failure."
                    + result.failure().name().toLowerCase(Locale.ROOT)));
        }
        return result;
    }
}
