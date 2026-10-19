package thaumcraft.common.testing;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchPurchaseNetworking;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchPurchase;

/**
 * Real-loader checks of the receiver's shared server handler, not C2S transport
 * dispatch. NeoForge additionally observes actual negotiated S2C wire payloads;
 * the Fabric vanilla mock supplies no wire observer and proves state only.
 */
public final class ResearchPurchaseRuntimeChecks {
    private ResearchPurchaseRuntimeChecks() {}

    @FunctionalInterface
    public interface KnowledgeWireObserver {
        void capture(CompoundTag expected, boolean required);
    }

    public static void secondaryPurchaseSpendsExactlyAndReplayIsIdempotent(
            GameTestHelper test, ServerPlayer player, KnowledgeWireObserver wire) {
        isolated(test, player, knowledge -> {
            // Original ConfigResearch:184: CLOTH 3 + MAGIC 3, Secondary, no parents.
            knowledge.setAspectPool("pannus", 3);
            knowledge.setAspectPool("praecantatio", 3);
            PlayerKnowledge expected = knowledge.copy();
            expected.setAspectPool("pannus", 0);
            expected.setAspectPool("praecantatio", 0);
            expected.addResearch("ENCHFABRIC");
            var result = request(test, player, "ENCHFABRIC", wire);
            test.assertTrue(result.success(), "exact original price buys Secondary through the real handler");
            test.assertValueEqual(result.costs(), Map.of("pannus", 3, "praecantatio", 3), "canonical quoted costs");
            test.assertValueEqual(result.unlocked(), Set.of("ENCHFABRIC"), "only purchased research is granted");
            test.assertValueEqual(knowledge.save(), expected.save(), "exact debit preserves every unrelated knowledge field");
            test.assertTrue(knowledge.hasDiscoveredAspect("pannus") && knowledge.hasDiscoveredAspect("praecantatio"),
                    "spending the last point does not erase aspect discovery");
            test.assertTrue(player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty(),
                    "original request admission adds no held-book requirement");

            CompoundTag afterPurchase = knowledge.save();
            var replay = request(test, player, "ENCHFABRIC", wire);
            test.assertValueEqual(replay.failure(), ResearchPurchase.Failure.ALREADY_COMPLETE, "replayed request rejected");
            test.assertTrue(replay.unlocked().isEmpty(), "replay reports no new grants");
            test.assertValueEqual(knowledge.save(), afterPurchase, "replay spends no points and adds no warp");
        });
    }

    public static void secondaryPurchaseRejectsPartialFundsAtomically(
            GameTestHelper test, ServerPlayer player, KnowledgeWireObserver wire) {
        isolated(test, player, knowledge -> {
            knowledge.setAspectPool("pannus", 3);
            knowledge.setAspectPool("praecantatio", 2);
            CompoundTag before = knowledge.save();
            var quote = ResearchPurchase.preview(knowledge, "ENCHFABRIC");
            test.assertValueEqual(quote.failure(), ResearchPurchase.Failure.INSUFFICIENT_ASPECTS, "3/2 cannot pay 3/3");
            test.assertValueEqual(knowledge.save(), before, "preview does not mutate state");
            // Original GUI:646-653 checked all balances, whereas its packet did
            // not. This distinguishes atomic server validation from TC4 overdraw.
            var denied = request(test, player, "ENCHFABRIC", wire);
            test.assertValueEqual(denied.failure(), ResearchPurchase.Failure.INSUFFICIENT_ASPECTS, "server rechecks full price");
            test.assertTrue(denied.unlocked().isEmpty(), "insufficient request grants nothing");
            test.assertValueEqual(knowledge.save(), before, "failure cannot debit the affordable first aspect");
        });
    }

    public static void secondaryPurchaseRequiresClueAndHiddenParents(
            GameTestHelper test, ServerPlayer player, KnowledgeWireObserver wire) {
        isolated(test, player, knowledge -> {
            // Original ConfigResearch:199: Hidden + Secondary BANNERS requires
            // its @ clue; the marker is distinct from actual completion.
            knowledge.setAspectPool("sensus", 3);
            knowledge.setAspectPool("pannus", 3);
            knowledge.setAspectPool("praecantatio", 1);
            CompoundTag noClue = knowledge.save();
            var missingClue = request(test, player, "BANNERS", wire);
            test.assertValueEqual(missingClue.failure(), ResearchPurchase.Failure.MISSING_CLUE, "hidden research requires clue");
            test.assertValueEqual(knowledge.save(), noClue, "missing clue leaves the full snapshot unchanged");

            knowledge.addResearch("@BANNERS");
            var withClue = request(test, player, "BANNERS", wire);
            test.assertTrue(withClue.success(), "clue admits the same fully funded request");
            test.assertTrue(knowledge.isResearchComplete("BANNERS") && knowledge.isResearchComplete("@BANNERS"),
                    "purchase preserves clue while granting the distinct research key");
            test.assertTrue(knowledge.getAspectPoolFor("sensus") == 0 && knowledge.getAspectPoolFor("pannus") == 0
                    && knowledge.getAspectPoolFor("praecantatio") == 0, "BANNERS spends original 3/3/1 costs");

            // Original ConfigResearch:186 / ResearchManager:428-454: ordinary
            // parents AND parentsHidden. Hidden prerequisite is not a clue.
            seedKnowledge(knowledge);
            knowledge.addResearch("VISPOWER");
            knowledge.addResearch("BELLOWS");
            knowledge.addResearch("TUBES");
            knowledge.setAspectPool("praecantatio", 3);
            knowledge.setAspectPool("vinculum", 3);
            knowledge.setAspectPool("aer", 3);
            knowledge.setAspectPool("aqua", 3);
            CompoundTag noHiddenParent = knowledge.save();
            var missingParent = request(test, player, "FLUXSCRUB", wire);
            test.assertValueEqual(missingParent.failure(), ResearchPurchase.Failure.MISSING_PREREQUISITES,
                    "visible prerequisites cannot replace hidden INFUSION prerequisite");
            test.assertValueEqual(knowledge.save(), noHiddenParent, "missing hidden parent is atomic");

            knowledge.addResearch("INFUSION");
            var withHiddenParent = request(test, player, "FLUXSCRUB", wire);
            test.assertTrue(withHiddenParent.success() && knowledge.isResearchComplete("FLUXSCRUB"),
                    "hidden prerequisite permits the same fully funded request");
            for (String tag : Set.of("praecantatio", "vinculum", "aer", "aqua")) {
                test.assertValueEqual(knowledge.getAspectPoolFor(tag), 0, "FLUXSCRUB exact pool debit: " + tag);
            }
        });
    }

    public static void secondaryPurchaseWarpAndPlayerPersistence(
            GameTestHelper test, ServerPlayer player, KnowledgeWireObserver wire) {
        isolated(test, player, knowledge -> {
            // Original ConfigResearch:338-339: COREBUTCHER Secondary, four
            // original aspects at 3 each, two parents, research warp exactly 1.
            knowledge.addResearch("COREGUARD");
            knowledge.addResearch("COREHARVEST");
            for (String tag : Set.of("telum", "bestia", "sensus", "meto")) knowledge.setAspectPool(tag, 3);
            var result = request(test, player, "COREBUTCHER", wire);
            test.assertTrue(result.success() && knowledge.isResearchComplete("COREBUTCHER"), "warp-bearing Secondary purchases");
            test.assertValueEqual(result.costs(), Map.of("telum", 3, "bestia", 3, "sensus", 3, "meto", 3),
                    "COREBUTCHER original research costs");
            for (String tag : result.costs().keySet()) test.assertValueEqual(knowledge.getAspectPoolFor(tag), 0, "exact warp research debit");
            // ResearchManager:510-526 / Thaumcraft:302-346, wuss default false:
            // 2 permanent +1; sticky 4 and temporary 5 remain; counter total=12.
            test.assertTrue(knowledge.getWarpPerm() == 3 && knowledge.getWarpSticky() == 4
                    && knowledge.getWarpTemp() == 5 && knowledge.getWarpCounter() == 12,
                    "first completion adds original permanent warp and refreshes total counter");
            CompoundTag afterPurchase = knowledge.save();
            var replay = request(test, player, "COREBUTCHER", wire);
            test.assertValueEqual(replay.failure(), ResearchPurchase.Failure.ALREADY_COMPLETE, "warp purchase replay rejected");
            test.assertValueEqual(knowledge.save(), afterPurchase, "warp cannot be awarded twice");

            CompoundTag playerSave = new CompoundTag();
            player.addAdditionalSaveData(playerSave);
            test.assertTrue(playerSave.contains("thaumcraft:player_knowledge"), "applied player mixin persists purchase knowledge");
            var restored = test.makeMockPlayer(GameType.SURVIVAL);
            restored.readAdditionalSaveData(playerSave);
            test.assertValueEqual(KnowledgeAccess.get(restored).save(), afterPurchase,
                    "player save/load preserves spent pools, discovery, research, clue, scans and all warp fields");
        });
    }

    private static ResearchPurchase.Result request(GameTestHelper test, ServerPlayer player,
                                                   String key, KnowledgeWireObserver wire) {
        test.assertTrue(player.server.isSameThread(), "receiver handler runs on the authoritative server thread");
        ResearchPurchase.Result result = ResearchPurchaseNetworking.handle(player, key);
        // These are all known, available catalogue rows: success and known
        // admission failures send the authoritative snapshot. Do not reject
        // correction packets just because an action was refused.
        if (wire != null) wire.capture(KnowledgeAccess.get(player).save(), true);
        return result;
    }

    private static void isolated(GameTestHelper test, ServerPlayer player, Consumer<PlayerKnowledge> scenario) {
        PlayerKnowledge knowledge = KnowledgeAccess.get(player);
        PlayerKnowledge baseline = knowledge.copy();
        ItemStack mainHand = player.getMainHandItem().copy();
        ItemStack offHand = player.getOffhandItem().copy();
        try {
            seedKnowledge(knowledge);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            scenario.accept(knowledge);
        } finally {
            knowledge.copyFrom(baseline);
            player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        }
        test.succeed();
    }

    private static void seedKnowledge(PlayerKnowledge knowledge) {
        knowledge.reset();
        knowledge.setInitializedAspects(true);
        knowledge.setAspectPool("aer", 17);
        knowledge.setAspectPool("victus", 0);
        knowledge.addResearch("@FOCUSHELLBAT");
        knowledge.scanItem("#minecraft:stone");
        knowledge.scanEntity("#minecraft:pig");
        knowledge.scanPhenomena("#runtime-test-node");
        knowledge.setWarpPerm(2);
        knowledge.setWarpSticky(4);
        knowledge.setWarpTemp(5);
        knowledge.setWarpCounter(7);
    }
}
