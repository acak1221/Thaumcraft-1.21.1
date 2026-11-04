package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchNoteRequestNetworking;
import thaumcraft.common.lib.network.ResearchPurchaseNetworking;
import thaumcraft.common.lib.research.ResearchNoteIssuance;
import thaumcraft.common.lib.research.ResearchPurchase;

/** Shared receiver checks with real PlayerList login/respawn; direct calls do not claim C2S dispatch. */
public final class ResearchAuthorityRuntimeChecks {
    private ResearchAuthorityRuntimeChecks() {}

    /** Existing price/payment assertions require an actual admitted sender, not a detached GameTest mock. */
    public static void withActor(GameTestHelper test, Consumer<ServerPlayer> scenario) {
        try (var fixture = new TubeVariantsRuntimeChecks.Fixture(test)) {
            fixture.open();
            scenario.accept(fixture.player);
        }
    }

    public static void run(GameTestHelper test) {
        var fixture = new TubeVariantsRuntimeChecks.Fixture(test);
        ServerPlayer current = fixture.player;
        try {
            fixture.open();
            prepare(current);
            var purchase = ResearchPurchaseNetworking.handle(current, "ENCHFABRIC");
            test.assertTrue(purchase.success() && KnowledgeAccess.get(current).isResearchComplete("ENCHFABRIC")
                    && KnowledgeAccess.get(current).getAspectPoolFor("pannus") == 0
                    && KnowledgeAccess.get(current).getAspectPoolFor("praecantatio") == 0,
                    "funded live sender still purchases and pays the original RP price");
            var note = ResearchNoteRequestNetworking.handle(current, "NITOR");
            test.assertTrue(note.success() && !note.reused() && current.getInventory().getItem(0).getCount() == 2
                    && current.getInventory().getItem(1).getDamageValue() == 13,
                    "funded live sender still receives a note and pays one paper/ink");

            prepare(current);
            current.setGameMode(GameType.SPECTATOR);
            denied(test, current, "spectator");
            current.setGameMode(GameType.SURVIVAL);
            current.setHealth(0);
            test.assertTrue(!current.isAlive(), "native actor is dead before the queued intent runs");
            denied(test, current, "dead actor");

            current.setRespawnPosition(fixture.level.dimension(), fixture.base.below(), 0, true, false);
            ServerPlayer retired = current;
            // PlayerList.respawn creates/registers the replacement; the native
            // packet handler owns the final connection.player reassignment.
            retired.connection.handleClientCommand(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            current = retired.connection.player;
            test.assertTrue(current != retired && current.isAlive()
                    && fixture.level.getServer().getPlayerList().getPlayer(current.getUUID()) == current
                    && retired.connection.player == current,
                    "real respawn replaces the actor and retargets its connection");
            var currentBefore = snapshot(current);
            denied(test, retired, "retired pre-respawn actor");
            unchanged(test, current, currentBefore, "stale intent cannot mutate the replacement actor");
            prepare(current);
            test.assertTrue(ResearchPurchaseNetworking.handle(current, "ENCHFABRIC").success()
                    && ResearchNoteRequestNetworking.handle(current, "NITOR").success(),
                    "the current live respawned sender remains authorized");
        } finally {
            if (current != fixture.player) {
                current.getInventory().clearContent();
                if (current.server.getPlayerList().getPlayer(current.getUUID()) == current) current.server.getPlayerList().remove(current);
                current.discard();
            }
            fixture.close();
        }
        test.succeed();
    }

    private static void prepare(ServerPlayer player) {
        var knowledge = KnowledgeAccess.get(player);
        knowledge.reset(); knowledge.setInitializedAspects(true);
        knowledge.addResearch("CRUCIBLE");
        knowledge.setAspectPool("pannus", 3); knowledge.setAspectPool("praecantatio", 3);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.PAPER, 3));
        ItemStack ink = new ItemStack(ThaumcraftContent.INKWELL.get()); ink.setDamageValue(12);
        player.getInventory().setItem(1, ink);
    }

    private record State(CompoundTag knowledge, List<ItemStack> inventory) {}

    private static State snapshot(ServerPlayer player) {
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) inventory.add(player.getInventory().getItem(i).copy());
        return new State(KnowledgeAccess.get(player).save(), inventory);
    }

    private static void denied(GameTestHelper test, ServerPlayer player, String boundary) {
        State before = snapshot(player);
        var purchase = ResearchPurchaseNetworking.handle(player, "ENCHFABRIC");
        var note = ResearchNoteRequestNetworking.handle(player, "NITOR");
        test.assertTrue(purchase.failure() == ResearchPurchase.Failure.UNAVAILABLE && purchase.unlocked().isEmpty()
                && note.failure() == ResearchNoteIssuance.Failure.UNAVAILABLE && !note.dropped() && !note.reused(),
                boundary + " cannot purchase research or issue a note");
        unchanged(test, player, before, boundary + " preserves knowledge, RP, paper, ink and every inventory slot");
    }

    private static void unchanged(GameTestHelper test, ServerPlayer player, State before, String message) {
        test.assertTrue(KnowledgeAccess.get(player).save().equals(before.knowledge()), message + " (knowledge)");
        for (int i = 0; i < before.inventory().size(); i++) {
            test.assertTrue(ItemStack.matches(before.inventory().get(i), player.getInventory().getItem(i)), message + " at slot " + i);
        }
    }
}
