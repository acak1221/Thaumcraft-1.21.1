package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import thaumcraft.api.IScribeTools;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemInkwell;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteGenerator;
import thaumcraft.common.lib.research.ResearchNotePuzzle;

/** Real-loader component/item/recipe checks; table gameplay is a later unit. */
public final class ResearchNoteRuntimeChecks {
    private ResearchNoteRuntimeChecks() {}

    public static void noteComponentsPersistAndEncode(GameTestHelper test) {
        var data = ResearchNoteGenerator.generate("NITOR", new Random(42));
        test.assertTrue(data.cells().size() == 19 && !data.complete(), "canonical complexity1 note is generated unfinished");
        var note = ItemResearchNotes.create(data, false);
        var registries = test.getLevel().registryAccess();
        var restored = ItemStack.parse(registries, note.save(registries)).orElseThrow();
        test.assertValueEqual(ItemResearchNotes.data(restored), data, "native item component survives persistent ItemStack codec");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, note);
            var decoded = ItemStack.STREAM_CODEC.decode(buffer);
            test.assertTrue(!buffer.isReadable(), "item component consumes exact actual registry wire bytes");
            test.assertValueEqual(ItemResearchNotes.data(decoded), data, "native ItemStack wire codec preserves note grid/order/color");
        } finally { buffer.release(); }
        var solved = solvedNitor();
        test.assertTrue(solved.complete() && solved.cells().size() == 6, "real generated note solves and prunes to connected occupied path");
        var discovery = ItemResearchNotes.create(solved, true);
        var discoveryRestored = ItemStack.parse(registries, discovery.save(registries)).orElseThrow();
        test.assertTrue(discoveryRestored.is(ThaumcraftContent.DISCOVERY.get()), "native discovery identity survives save separately from solved flag");
        test.assertValueEqual(ItemResearchNotes.data(discoveryRestored), solved, "pruned solved component survives native save/load");
        test.succeed();
    }

    public static void solvedNoteLearnsOnceAndTransfers(GameTestHelper test, ServerPlayer player,
            ResearchPurchaseRuntimeChecks.KnowledgeWireObserver wire) {
        isolated(test, player, knowledge -> {
            knowledge.addResearch("CRUCIBLE");
            var pools = knowledge.getAspectsDiscovered();
            test.assertTrue(!knowledge.hasDiscoveredAspect("lux"), "reader has not discovered the solver's aspect");
            var data = solvedNitor();
            var stack = ItemResearchNotes.create(data, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(stack.isEmpty() && knowledge.isResearchComplete("NITOR"), "real item use consumes one transferred discovery and learns primary");
            test.assertValueEqual(knowledge.getAspectsDiscovered(), pools, "learning does not charge RP or discover the donor's aspects");
            if (wire != null) wire.capture(knowledge.save(), true);
            var beforeReplay = knowledge.save();
            // ItemResearchNotes checks complete, not old metadata64: a normal
            // presentation containing a solved component also learns/is replay-safe.
            var replay = ItemResearchNotes.create(data, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, replay);
            replay.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(replay.getCount() == 1, "already-known note is retained");
            test.assertValueEqual(knowledge.save(), beforeReplay, "replay changes neither knowledge nor warp");
            CompoundTag saved = new CompoundTag();
            player.addAdditionalSaveData(saved);
            var restored = test.makeMockPlayer(GameType.SURVIVAL);
            test.assertTrue(!KnowledgeAccess.get(restored).isResearchComplete("NITOR"), "another player's knowledge does not inherit the solver's award");
            restored.readAdditionalSaveData(saved);
            test.assertValueEqual(KnowledgeAccess.get(restored).save(), knowledge.save(), "learned primary survives actual player NBT lifecycle");
        });
    }

    public static void noteRefusalsAndWarpUseOriginalRules(GameTestHelper test, ServerPlayer player,
            ResearchPurchaseRuntimeChecks.KnowledgeWireObserver wire) {
        isolated(test, player, knowledge -> {
            var discovery = ItemResearchNotes.create(solvedNitor(), true);
            player.setItemInHand(InteractionHand.MAIN_HAND, discovery);
            var before = knowledge.save();
            discovery.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(discovery.getCount() == 1, "missing parent retains note");
            test.assertValueEqual(knowledge.save(), before, "missing parent changes no state");
            knowledge.addResearch("CRUCIBLE");
            var unfinished = ItemResearchNotes.create(ResearchNoteGenerator.generate("NITOR", new Random(42)), true);
            player.setItemInHand(InteractionHand.MAIN_HAND, unfinished);
            var beforeUnfinished = knowledge.save();
            unfinished.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(unfinished.getCount() == 1, "discovery presentation cannot override unfinished data");
            test.assertValueEqual(knowledge.save(), beforeUnfinished, "unfinished note cannot grant research");

            knowledge.addResearch("INFUSION");
            knowledge.setWarpPerm(2); knowledge.setWarpSticky(4); knowledge.setWarpTemp(5);
            var unsolved = ResearchNoteGenerator.generate("JARBRAIN", new Random(42));
            // Trusted solved-state fixture isolates original item learning/warp;
            // generation and actual puzzle solving are tested independently.
            var data = unsolved.withCells(unsolved.cells(), true);
            var normal = ItemResearchNotes.create(data, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, normal);
            normal.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(normal.isEmpty() && knowledge.isResearchComplete("JARBRAIN"), "complete flag learns hidden note even without discovery identity or reader clue");
            test.assertTrue(!knowledge.isResearchComplete("@JARBRAIN"), "learning does not manufacture clue");
            test.assertTrue(knowledge.getWarpPerm() == 4 && knowledge.getWarpSticky() == 5
                    && knowledge.getWarpTemp() == 5 && knowledge.getWarpCounter() == 14, "original warp3 splits2/1 and counter is full14");
            var awarded = knowledge.save();
            var duplicate = ItemResearchNotes.create(data, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, duplicate);
            duplicate.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
            test.assertTrue(duplicate.getCount() == 1 && awarded.equals(knowledge.save()), "JARBRAIN replay retains note and cannot grant warp3 twice");
            if (wire != null) wire.capture(knowledge.save(), true);
        });
    }

    public static void originalScribingRecipesAndInkBounds(GameTestHelper test) {
        var tool = new ItemStack(ThaumcraftContent.INKWELL.get());
        test.assertTrue(tool.getItem() instanceof IScribeTools && tool.getMaxDamage() == 100 && tool.getMaxStackSize() == 1,
                "original scribe tool marker, ink capacity100 and stack1");
        tool.setDamageValue(99);
        test.assertTrue(ItemInkwell.isUsable(tool), "last ink point is usable");
        tool.setDamageValue(100);
        test.assertTrue(!tool.isEmpty() && !ItemInkwell.isUsable(tool), "exhausted ink remains an item and is unusable");
        var registries = test.getLevel().registryAccess();
        var restored = ItemStack.parse(registries, tool.save(registries)).orElseThrow();
        test.assertTrue(restored.getDamageValue() == 100 && !ItemInkwell.isUsable(restored), "exhausted boundary persists");
        checkScribeRecipe(test, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get()), new ItemStack(Items.FEATHER), new ItemStack(Items.INK_SAC));
        checkScribeRecipe(test, new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.FEATHER), new ItemStack(Items.BLACK_DYE));
        checkScribeRecipe(test, restored, new ItemStack(Items.INK_SAC));
        test.assertTrue(restored.getDamageValue() == 100, "recipe assembly does not mutate its input before native crafting commit");
        var missingFeather = CraftingInput.of(2, 1, List.of(new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.INK_SAC)));
        test.assertTrue(test.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, missingFeather, test.getLevel()).isEmpty(),
                "scribe creation needs feather; note copying uses different ink_sac costs later");
        test.succeed();
    }

    private static void checkScribeRecipe(GameTestHelper test, ItemStack... items) {
        var input = CraftingInput.of(items.length, 1, List.of(items));
        var recipe = test.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
        var output = recipe.value().assemble(input, test.getLevel().registryAccess());
        test.assertTrue(output.is(ThaumcraftContent.INKWELL.get()) && output.getCount() == 1 && output.getDamageValue() == 0,
                "original Scribe1/2/3 yields exactly one fresh100-ink tool");
    }

    public static ResearchNoteData solvedNitor() {
        var note = ResearchNoteGenerator.generate("NITOR", new Random(42));
        var cells = new ArrayList<>(note.cells());
        for (var placed : List.of(new ResearchNoteData.Cell(-1, 2, 2, "ignis"), new ResearchNoteData.Cell(0, 1, 2, "lux"),
                new ResearchNoteData.Cell(0, 0, 2, "ignis"), new ResearchNoteData.Cell(1, -1, 2, "lux"))) {
            cells.replaceAll(cell -> cell.q() == placed.q() && cell.r() == placed.r() ? placed : cell);
        }
        var solved = ResearchNotePuzzle.solve(note.withCells(cells, false), Set.of("ignis", "lux"));
        if (!solved.complete()) throw new IllegalStateException("Original NITOR path fixture failed to connect");
        return solved;
    }

    private static void isolated(GameTestHelper test, ServerPlayer player, Consumer<PlayerKnowledge> scenario) {
        var knowledge = KnowledgeAccess.get(player);
        var baseline = knowledge.copy();
        var hand = player.getMainHandItem().copy();
        try {
            knowledge.reset();
            knowledge.setInitializedAspects(true);
            knowledge.setAspectPool("aer", 17);
            scenario.accept(knowledge);
        } finally {
            knowledge.copyFrom(baseline);
            player.setItemInHand(InteractionHand.MAIN_HAND, hand);
        }
        test.succeed();
    }
}
