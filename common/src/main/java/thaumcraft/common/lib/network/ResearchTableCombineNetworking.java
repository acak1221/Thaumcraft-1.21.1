package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchAspectCombination;

/** Server-authorized R09 combination, sharing the R08 menu/session/revision acknowledgement. */
public final class ResearchTableCombineNetworking {
    private static boolean initialized;
    private ResearchTableCombineNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S,
                ResearchTableCombinePayload.TYPE, ResearchTableCombinePayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, payload);
                }));
    }

    /** Production receiver entry point; direct runtime checks do not claim C2S dispatch. */
    public static boolean handle(ServerPlayer player, ResearchTableCombinePayload payload) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Research combination requires the server thread");
        if (!(player.containerMenu instanceof ContainerResearchTable menu) || menu.tile() == null) return false;
        if (payload == null || payload.containerId() != menu.containerId || payload.sessionToken() != menu.sessionToken()) {
            return correct(player, menu);
        }
        if (!menu.stillValid(player)) return false;
        var table = menu.tile();
        long revision = table.tableRevision();
        if (revision == Long.MAX_VALUE || revision != payload.expectedTableRevision()) return correct(player, menu);
        var knowledge = KnowledgeAccess.get(player);
        var plan = ResearchAspectCombination.plan(knowledge, table.bonusPools(), payload.left(), payload.right());
        if (plan == null) return correct(player, menu);

        // No callbacks or inventory setters between actor/shared-bonus writes.
        // Combine works with empty slots and must retain their object identities.
        table.commitKnowledgeBonus(revision, plan.bonusPools(), knowledge, plan.knowledge());
        table.setChanged();
        KnowledgeEvents.sync(player);
        menu.broadcastChanges();
        for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
            if (viewer != player && viewer.containerMenu instanceof ContainerResearchTable other
                    && other.tile() == table && other.stillValid(viewer)) other.broadcastChanges();
        }
        // Reuse the native scan discovery/points presentation. Invalid pairs
        // have no output, just as in the original; no made-up failure reward.
        if (plan.output() != null) {
            if (plan.discovery()) player.sendSystemMessage(Component.translatable("tc.port.scan.discovery",
                    Component.literal(plan.output().getName())));
            player.sendSystemMessage(Component.translatable("tc.port.scan.points",
                    Component.literal(plan.output().getName() + " +" + plan.award())));
        }
        return true;
    }

    private static boolean correct(ServerPlayer player, ContainerResearchTable menu) {
        if (player.containerMenu == menu && menu.stillValid(player)) {
            KnowledgeEvents.sync(player);
            // Full correction also sends an unchanged-revision State, clearing
            // the client's pending flag on insufficient points/stale selection.
            menu.sendAllDataToRemote();
        }
        return false;
    }
}
