package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchNoteRequestNetworking;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchNoteGenerator;
import thaumcraft.common.lib.research.ResearchNoteIssuance;

/**
 * Real-loader issuance/inventory checks. Call each with a fresh mock player;
 * deterministic service checks never reseed the world's RNG. The handler check
 * exercises shared server admission, not C2S dispatch or client rendering.
 */
public final class ResearchNoteIssuanceRuntimeChecks {
    private ResearchNoteIssuanceRuntimeChecks() {}

    public static void primaryRequestConsumesPaperAndLastInk(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            paper(player, 0, 3);
            ink(player, 1, 99);
            CompoundTag beforeKnowledge = knowledge.save();
            test.assertTrue(!knowledge.hasDiscoveredAspect("lux"), "requester need not discover every puzzle endpoint first");
            test.assertTrue(player.server.isSameThread(), "shared receiver is called on the server thread");
            var result = ResearchNoteRequestNetworking.handle(player, "NITOR");
            test.assertTrue(result.success() && !result.reused() && !result.dropped(), "actual receiver issues one normal primary note");
            // Oracle ResearchManager.createResearchNoteForPlayer:126-127 - the original only CHECKS
            // that the player carries paper (InventoryPlayer.inventoryContains) and never takes it.
            test.assertTrue(player.getInventory().getItem(0).getCount() == 3, "the original never spends the paper");
            ItemStack tool = player.getInventory().getItem(1);
            test.assertTrue(tool.getCount() == 1 && tool.getDamageValue() == 100, "last ink point retains the exhausted inkwell");
            var notes = mainNotes(player);
            test.assertTrue(notes.size() == 1 && notes.getFirst().is(ThaumcraftContent.RESEARCH_NOTE.get()), "one normal note is delivered");
            var data = ItemResearchNotes.data(notes.getFirst());
            test.assertTrue("NITOR".equals(data.key()) && !data.complete() && data.copies() == 0 && data.cells().size() == 19,
                    "issued component is the canonical unfinished NITOR puzzle");
            test.assertValueEqual(knowledge.save(), beforeKnowledge, "issuance grants no research, aspects, RP, scans or warp");

            List<ItemStack> after = inventorySnapshot(player);
            var replay = ResearchNoteRequestNetworking.handle(player, "NITOR");
            test.assertTrue(replay.success() && replay.reused(), "receiver reuses existing note despite exhausted ink");
            assertInventory(test, player, after, "receiver replay changes no inventory");
            test.assertValueEqual(knowledge.save(), beforeKnowledge, "receiver replay changes no knowledge");

            CompoundTag saved = new CompoundTag();
            player.addAdditionalSaveData(saved);
            var restored = test.makeMockPlayer(GameType.SURVIVAL);
            restored.readAdditionalSaveData(saved);
            test.assertValueEqual(KnowledgeAccess.get(restored).save(), beforeKnowledge, "issuance knowledge survives player persistence unchanged");
            for (int slot = 0; slot < after.size(); slot++) {
                test.assertTrue(ItemStack.matches(restored.getInventory().getItem(slot), after.get(slot)), "issued inventory persists at slot " + slot);
            }
        });
    }

    public static void existingNotesReuseWithoutResourcesOrRandomness(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            for (boolean discovery : List.of(false, true)) {
                player.getInventory().clearContent();
                var data = discovery ? ResearchNoteRuntimeChecks.solvedNitor() : ResearchNoteGenerator.generate("NITOR", new Random(42));
                player.getInventory().setItem(17, ItemResearchNotes.create(data, discovery));
                List<ItemStack> before = inventorySnapshot(player);
                CompoundTag beforeKnowledge = knowledge.save();
                var random = new ForbiddenRandom();
                var result = ResearchNoteIssuance.issue(player, "NITOR", random);
                test.assertTrue(result.success() && result.reused() && !result.dropped(), "same-key normal/discovery note is reused without resources");
                test.assertTrue(random.calls == 0, "reuse never generates a puzzle");
                assertInventory(test, player, before, "same-key component and presentation remain untouched");
                test.assertValueEqual(knowledge.save(), beforeKnowledge, "reuse cannot learn a solved discovery");
            }
        });
    }

    public static void primaryRequestRefusalsAreAtomic(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            ink(player, 4, 12);
            denied(test, player, "NITOR", ResearchNoteIssuance.Failure.MISSING_PAPER);
            player.getInventory().clearContent();
            paper(player, 3, 2);
            denied(test, player, "NITOR", ResearchNoteIssuance.Failure.MISSING_INK);
            ink(player, 4, 12);
            knowledge.removeResearch("CRUCIBLE");
            denied(test, player, "NITOR", ResearchNoteIssuance.Failure.MISSING_PREREQUISITES);
            knowledge.addResearch("VISPOWER");
            denied(test, player, "FOCALMANIPULATION", ResearchNoteIssuance.Failure.MISSING_PREREQUISITES);
            knowledge.addResearch("FOCUSFIRE"); knowledge.addResearch("INFUSION");
            denied(test, player, "FOCUSHELLBAT", ResearchNoteIssuance.Failure.MISSING_CLUE);
            denied(test, player, "ENCHFABRIC", ResearchNoteIssuance.Failure.SECONDARY_REQUIRES_PURCHASE);
            denied(test, player, "RESEARCH", ResearchNoteIssuance.Failure.UNAVAILABLE);
            for (String key : new String[] {null, "", "@NITOR", "nitor", "x".repeat(129)}) {
                denied(test, player, key, ResearchNoteIssuance.Failure.UNKNOWN_RESEARCH);
            }
            knowledge.addResearch("NITOR");
            denied(test, player, "NITOR", ResearchNoteIssuance.Failure.ALREADY_COMPLETE);
        });
    }

    public static void primaryRequestUsesOnlyOrderedMainInventory(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            ink(player, 1, 100); // Exhausted earlier tool must be skipped.
            paper(player, 3, 2); ink(player, 4, 12);
            paper(player, 5, 3); ink(player, 6, 41);
            player.getInventory().offhand.set(0, new ItemStack(Items.PAPER, 64));
            player.getInventory().armor.set(0, new ItemStack(ThaumcraftContent.INKWELL.get()));
            var result = ResearchNoteIssuance.issue(player, "NITOR", new Random(42));
            test.assertTrue(result.success(), "ordered main-inventory resources issue a note");
            test.assertTrue(player.getInventory().getItem(3).getCount() == 2 && player.getInventory().getItem(5).getCount() == 3,
                    "the paper stacks are located but never debited");
            test.assertTrue(player.getInventory().getItem(1).getDamageValue() == 100
                    && player.getInventory().getItem(4).getDamageValue() == 13 && player.getInventory().getItem(6).getDamageValue() == 41,
                    "first usable main tool is debited, depleted and later tools remain");
            test.assertTrue(player.getInventory().offhand.getFirst().getCount() == 64
                    && player.getInventory().armor.getFirst().getDamageValue() == 0, "offhand and armor are excluded");

            player.getInventory().clearContent();
            player.getInventory().armor.set(0, new ItemStack(Items.PAPER, 2));
            player.getInventory().offhand.set(0, new ItemStack(ThaumcraftContent.INKWELL.get()));
            denied(test, player, "NITOR", ResearchNoteIssuance.Failure.MISSING_PAPER);
            paper(player, 0, 2);
            player.getInventory().armor.set(1, new ItemStack(ThaumcraftContent.INKWELL.get()));
            denied(test, player, "NITOR", ResearchNoteIssuance.Failure.MISSING_INK);
        });
    }

    public static void fullInventoryReusesFreedPaperSlotOrDrops(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            fillMain(player); paper(player, 4, 1); ink(player, 7, 99);
            var freed = ResearchNoteIssuance.issue(player, "NITOR", new Random(42));
            // The original never takes the paper, so its slot is never freed and the note is
            // dropped exactly like the overflow below.
            test.assertTrue(freed.success() && freed.dropped(), "the unspent paper never frees a destination");
            test.assertTrue(player.getInventory().getItem(4).is(Items.PAPER) && player.getInventory().getItem(4).getCount() == 1,
                    "the paper stays in its slot");
            test.assertTrue(player.getInventory().getItem(7).getDamageValue() == 100 && player.getInventory().getItem(7).getCount() == 1,
                    "full inventory retains depleted ink as well");
            test.assertTrue(droppedNotes(test, player).size() == 1, "the note is dropped, not delivered");
            // The dropped note of this step must not be counted by the overflow below.
            for (ItemEntity drop : droppedNotes(test, player)) drop.discard();

            fillMain(player); paper(player, 4, 2); ink(player, 7, 98);
            var overflow = ResearchNoteIssuance.issue(player, "NITOR", new Random(42));
            test.assertTrue(overflow.success() && overflow.dropped() && !overflow.discarded(), "full survival inventory uses actual native item drop");
            test.assertTrue(mainNotes(player).isEmpty() && player.getInventory().getItem(4).getCount() == 2
                    && player.getInventory().getItem(7).getDamageValue() == 99,
                    "successful overflow spends the ink once and never the paper");
            var drops = droppedNotes(test, player);
            test.assertTrue(drops.size() == 1 && drops.getFirst().isAlive() && drops.getFirst().getItem().getCount() == 1,
                    "one actual live ItemEntity carries the overflow note");
            for (ItemEntity drop : drops) drop.discard();

            fillMain(player); paper(player, 4, 2); ink(player, 7, 98);
            player.getAbilities().instabuild = true;
            var creative = ResearchNoteIssuance.issue(player, "NITOR", new Random(42));
            // Original InventoryPlayer.addItemStackToInventory has a distinct
            // full-creative discard branch (official 1.7.10 yx.a, offsets56-83).
            test.assertTrue(creative.success() && creative.discarded() && !creative.dropped(), "full creative inventory preserves original silent discard");
            test.assertTrue(mainNotes(player).isEmpty() && droppedNotes(test, player).isEmpty()
                    && player.getInventory().getItem(4).getCount() == 2 && player.getInventory().getItem(7).getDamageValue() == 98,
                    "creative overflow spends the ink, keeps the paper, and creates no note entity");
        });
    }

    public static void nativeInkPreservesCreativeAndUnbreakable(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            for (boolean creative : List.of(true, false)) {
                player.getInventory().clearContent();
                paper(player, 0, 1); ink(player, 1, 99);
                player.getAbilities().instabuild = creative;
                if (!creative) player.getInventory().getItem(1).set(DataComponents.UNBREAKABLE, new Unbreakable(true));
                player.getRandom().setSeed(235711L);
                var expected = RandomSource.create(235711L);
                CompoundTag before = knowledge.save();
                var result = ResearchNoteIssuance.issue(player, "NITOR", new Random(42));
                test.assertTrue(result.success(), "creative or native Unbreakable tool permits issue");
                test.assertTrue(player.getInventory().getItem(0).is(Items.PAPER) && player.getInventory().getItem(0).getCount() == 1
                        && player.getInventory().getItem(2).is(ThaumcraftContent.RESEARCH_NOTE.get()),
                        "the paper is not spent even in creative and the note takes the first free slot");
                test.assertTrue(player.getInventory().getItem(1).getDamageValue() == 99
                        && player.getInventory().getItem(1).getCount() == 1, "native no-damage branch preserves the inkwell");
                test.assertTrue(player.getRandom().nextLong() == expected.nextLong(), "no player random draw when damage is bypassed");
                test.assertValueEqual(knowledge.save(), before, "no-damage issuance still grants no knowledge");
            }
        });
    }

    public static void nativeUnbreakingUsesExactlyOnePlayerDraw(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            var unbreaking = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING);
            // LegacyRandomSource, like original java.util.Random: these seeds
            // exercise a nonzero roll and a zero roll for exclusive bound4.
            for (long seed : new long[] {0L, 4096L}) {
                player.getInventory().clearContent();
                paper(player, 0, 2); ink(player, 1, 12);
                player.getInventory().getItem(1).enchant(unbreaking, 3);
                player.getRandom().setSeed(seed);
                var expected = RandomSource.create(seed);
                int roll = expected.nextInt(4);
                test.assertTrue((seed == 4096L) == (roll == 0), "native seed fixture covers both Unbreaking outcomes");
                var generation = new CountingRandom(42);
                var result = ResearchNoteIssuance.issue(player, "NITOR", generation);
                test.assertTrue(result.success() && generation.calls == 1, "one complexity1 generation draw uses the supplied RNG");
                test.assertTrue(player.getInventory().getItem(1).getDamageValue() == 12 + (roll == 0 ? 1 : 0),
                        "native Unbreaking uses old zero-roll damage rule");
                test.assertTrue(player.getRandom().nextLong() == expected.nextLong(), "exactly one bounded-int draw comes from player's RNG");
                test.assertTrue(player.getInventory().getItem(0).getCount() == 2, "the paper is never spent, whatever the ink does");
            }
        });
    }

    public static void invalidGenerationLeavesResourcesAndKnowledgeUnchanged(GameTestHelper test, ServerPlayer player) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("FOCUSFIRE"); knowledge.addResearch("INFUSION"); knowledge.addResearch("@FOCUSHELLBAT");
            paper(player, 0, 3); ink(player, 1, 12);
            List<ItemStack> before = inventorySnapshot(player);
            CompoundTag beforeKnowledge = knowledge.save();
            player.getRandom().setSeed(101L);
            var expected = RandomSource.create(101L);
            var badRandom = new CountingRandom(42) {
                @Override public int nextInt(int bound) { calls++; return bound; }
            };
            var result = ResearchNoteIssuance.issue(player, "FOCUSHELLBAT", badRandom);
            test.assertValueEqual(result.failure(), ResearchNoteIssuance.Failure.INVALID_GENERATION, "out-of-bound generation is refused");
            test.assertTrue(badRandom.calls == 2, "original discarded ring draw precedes rejected removal candidate");
            assertInventory(test, player, before, "generation failure commits no paper, ink or output");
            test.assertValueEqual(knowledge.save(), beforeKnowledge, "generation failure changes no knowledge");
            test.assertTrue(player.getRandom().nextLong() == expected.nextLong(), "generation failure does not reach ink/player RNG");
        });
    }

    public static void deliveryFailurePreservesPaperInkAndKnowledge(GameTestHelper test, ServerPlayer player) {
        for (boolean unspawned : List.of(false, true)) {
            // Actual native Player/Inventory/ItemEntity objects. Overriding the
            // delivery result isolates both failures; it does not claim to fire
            // NeoForge's ItemTossEvent or exercise its cancellation subscribers.
            var refusing = new RefusingDropPlayer(player, unspawned);
            try {
                isolatedState(test, refusing, knowledge -> {
                    knowledge.addResearch("CRUCIBLE");
                    fillMain(refusing); paper(refusing, 4, 2); ink(refusing, 7, 99);
                    List<ItemStack> before = inventorySnapshot(refusing);
                    CompoundTag beforeKnowledge = knowledge.save();
                    var generation = new CountingRandom(42);
                    var result = ResearchNoteIssuance.issue(refusing, "NITOR", generation);
                    test.assertValueEqual(result.failure(), ResearchNoteIssuance.Failure.DELIVERY_FAILED,
                            "null or unspawned native delivery fails authoritatively");
                    test.assertTrue(!result.success() && !result.reused() && !result.dropped() && !result.discarded(),
                            "delivery failure reports neither reuse, drop nor creative discard");
                    test.assertTrue(refusing.dropCalls == 1 && generation.calls == 1,
                            "valid output reaches delivery exactly once after canonical generation");
                    assertInventory(test, refusing, before, "failed delivery cannot spend paper or last ink");
                    test.assertValueEqual(knowledge.save(), beforeKnowledge, "failed delivery changes no knowledge or warp");
                    test.assertTrue(mainNotes(refusing).isEmpty() && droppedNotes(test, refusing).isEmpty(),
                            "failed delivery leaves no note in inventory or world");
                    if (unspawned) {
                        test.assertTrue(refusing.returned != null && refusing.returned.isRemoved()
                                && test.getLevel().getEntity(refusing.returned.getId()) == null,
                                "returned unspawned ItemEntity is discarded without material debit");
                    }
                });
            } finally {
                if (refusing.returned != null && !refusing.returned.isRemoved()) refusing.returned.discard();
            }
        }
        test.succeed();
    }

    private static void denied(GameTestHelper test, ServerPlayer player, String key, ResearchNoteIssuance.Failure failure) {
        List<ItemStack> before = inventorySnapshot(player);
        CompoundTag knowledge = KnowledgeAccess.get(player).save();
        var random = new ForbiddenRandom();
        var result = ResearchNoteIssuance.issue(player, key, random);
        test.assertValueEqual(result.failure(), failure, "authoritative refusal: " + key);
        test.assertTrue(random.calls == 0 && !result.success() && !result.reused() && !result.dropped(), "refusal generates nothing");
        assertInventory(test, player, before, "refusal preserves inventory");
        test.assertValueEqual(KnowledgeAccess.get(player).save(), knowledge, "refusal preserves complete knowledge snapshot");
    }

    private static void paper(ServerPlayer player, int slot, int count) { player.getInventory().setItem(slot, new ItemStack(Items.PAPER, count)); }
    private static void ink(ServerPlayer player, int slot, int damage) {
        var stack = new ItemStack(ThaumcraftContent.INKWELL.get());
        stack.setDamageValue(damage);
        player.getInventory().setItem(slot, stack);
    }
    private static void fillMain(ServerPlayer player) {
        player.getInventory().clearContent();
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
    }
    private static List<ItemStack> mainNotes(ServerPlayer player) {
        return player.getInventory().items.stream().filter(stack -> !stack.isEmpty() && ItemResearchNotes.data(stack) != null).toList();
    }
    private static List<ItemEntity> droppedNotes(GameTestHelper test, ServerPlayer player) {
        return test.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4),
                entity -> ItemResearchNotes.data(entity.getItem()) != null);
    }
    private static List<ItemStack> inventorySnapshot(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) result.add(player.getInventory().getItem(slot).copy());
        return result;
    }
    private static void assertInventory(GameTestHelper test, ServerPlayer player, List<ItemStack> expected, String message) {
        for (int slot = 0; slot < expected.size(); slot++) {
            test.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), expected.get(slot)), message + " at slot " + slot);
        }
    }
    private static void isolated(GameTestHelper test, ServerPlayer player, Consumer<PlayerKnowledge> scenario) {
        isolatedState(test, player, scenario);
        test.succeed();
    }
    private static void isolatedState(GameTestHelper test, ServerPlayer player, Consumer<PlayerKnowledge> scenario) {
        var knowledge = KnowledgeAccess.get(player);
        var baselineKnowledge = knowledge.copy();
        List<ItemStack> baselineInventory = inventorySnapshot(player);
        int selected = player.getInventory().selected;
        boolean creative = player.getAbilities().instabuild;
        var position = player.position();
        var here = test.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(here.getX() + 0.5, here.getY(), here.getZ() + 0.5);
        Set<UUID> baselineDrops = new HashSet<>();
        for (ItemEntity entity : droppedNotes(test, player)) baselineDrops.add(entity.getUUID());
        try {
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.getAbilities().instabuild = false;
            knowledge.reset(); knowledge.setInitializedAspects(true);
            knowledge.setAspectPool("aer", 17); knowledge.setAspectPool("victus", 0);
            knowledge.addResearch("@UNCHANGED_CLUE");
            knowledge.scanItem("#minecraft:stone"); knowledge.scanEntity("#minecraft:pig"); knowledge.scanPhenomena("#unchanged-node");
            knowledge.setWarpPerm(2); knowledge.setWarpSticky(4); knowledge.setWarpTemp(5); knowledge.setWarpCounter(7);
            scenario.accept(knowledge);
        } finally {
            for (ItemEntity entity : droppedNotes(test, player)) if (!baselineDrops.contains(entity.getUUID())) entity.discard();
            knowledge.copyFrom(baselineKnowledge);
            for (int slot = 0; slot < baselineInventory.size(); slot++) player.getInventory().setItem(slot, baselineInventory.get(slot));
            player.getInventory().selected = selected;
            player.getAbilities().instabuild = creative;
            player.setPos(position);
        }
    }
    private static final class RefusingDropPlayer extends ServerPlayer {
        private final boolean unspawned;
        private int dropCalls;
        private ItemEntity returned;

        private RefusingDropPlayer(ServerPlayer source, boolean unspawned) {
            super(source.server, source.serverLevel(), new GameProfile(UUID.randomUUID(), "tc4-denied-drop"), ClientInformation.createDefault());
            this.unspawned = unspawned;
        }

        @Override public ItemEntity drop(ItemStack stack, boolean throwRandomly) {
            dropCalls++;
            if (!unspawned) return null;
            returned = new ItemEntity(serverLevel(), getX(), getY(), getZ(), stack.copy());
            return returned;
        }
    }
    private static class CountingRandom extends Random {
        int calls;
        CountingRandom(long seed) { super(seed); }
        @Override public int nextInt(int bound) { calls++; return super.nextInt(bound); }
    }
    private static final class ForbiddenRandom extends CountingRandom {
        ForbiddenRandom() { super(0); }
        @Override public int nextInt(int bound) { calls++; throw new AssertionError("Refusal/reuse reached puzzle RNG"); }
    }
}
