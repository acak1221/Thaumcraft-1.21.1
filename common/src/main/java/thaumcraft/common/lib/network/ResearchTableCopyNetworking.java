package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchNoteCopying;

/** R10 server receiver, using R08's real menu/session/revision and authoritative correction acknowledgement. */
public final class ResearchTableCopyNetworking {
    private static boolean initialized;
    private ResearchTableCopyNetworking() {}
    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ResearchTableCopyPayload.TYPE, ResearchTableCopyPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player, payload);
                }));
    }
    /** Original main inventory only, item+metadata0; custom names/components are allowed as the old isItemEqual allowed NBT. */
    public static int findSupply(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!stack.isEmpty() && stack.is(item) && stack.getDamageValue() == 0) return slot;
        }
        return -1;
    }
    /** The actual network receiver calls this with its sender; runtime direct calls do not claim C2S transport coverage. */
    public static boolean handle(ServerPlayer player, ResearchTableCopyPayload payload) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Research copying requires the server thread");
        if (!(player.containerMenu instanceof ContainerResearchTable menu) || menu.tile() == null) return false;
        if (payload == null || payload.containerId() != menu.containerId || payload.sessionToken() != menu.sessionToken()) return correct(player, menu);
        if (player.isSpectator() || !menu.stillValid(player)) return false;
        var table = menu.tile();
        long revision = table.tableRevision();
        if (revision == Long.MAX_VALUE || revision != payload.expectedTableRevision()) return correct(player, menu);
        ItemStack discovery = table.getItem(1);
        var data = ItemResearchNotes.data(discovery);
        if (!discovery.is(ThaumcraftContent.DISCOVERY.get()) || data == null || !data.complete()) return correct(player, menu);
        // Original TileResearchTable:407-441 (oracle) - the copy costs ONE PAPER and the BLACK DYE
        // (the original Items.dye of the metadata zero), not an ink sac.
        int paperSlot = findSupply(player.getInventory(), Items.PAPER), dyeSlot = findSupply(player.getInventory(), Items.BLACK_DYE);
        if (paperSlot < 0 || dyeSlot < 0) return correct(player, menu);
        var knowledge = KnowledgeAccess.get(player);
        var plan = ResearchNoteCopying.plan(knowledge, data, discovery.getCount());
        if (plan == null) return correct(player, menu);
        table.commitCopy(revision, discovery, plan, knowledge, player.getInventory().getItem(paperSlot), player.getInventory().getItem(dyeSlot));
        player.getInventory().setChanged(); table.setChanged();
        KnowledgeEvents.sync(player);
        for (String tag : plan.costs().keySet()) KnowledgeNotificationNetworking.pool(player, tag, 0);
        menu.broadcastChanges();
        for (var viewer : player.server.getPlayerList().getPlayers()) {
            if (viewer != player && viewer.containerMenu instanceof ContainerResearchTable other && other.tile() == table && other.stillValid(viewer)) other.broadcastChanges();
        }
        player.serverLevel().blockEvent(table.getBlockPos(), table.getBlockState().getBlock(), 1, 1);
        return true;
    }
    private static boolean correct(ServerPlayer player, ContainerResearchTable menu) {
        if (player.containerMenu == menu && menu.stillValid(player)) { KnowledgeEvents.sync(player); menu.sendAllDataToRemote(); }
        return false;
    }
}
