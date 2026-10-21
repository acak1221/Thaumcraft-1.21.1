package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftUnknownKnowledge;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.items.ItemUnknownResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchHiddenSelection;
import thaumcraft.common.lib.research.ResearchNoteIssuance;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Actual ServerPlayerGameMode use, recipe manager, native codecs and world-post queue retry. No client rendering or packet dispatch claim. */
public final class UnknownKnowledgeRuntimeChecks {
    private static final SavedData.Factory<DeferredItemDrops> QUEUE_READER = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
    private UnknownKnowledgeRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel(); var knowledge = KnowledgeAccess.get(player); var oldKnowledge = knowledge.copy();
        var oldPosition = player.position(); var oldGameType = player.gameMode.getGameModeForPlayer();
        boolean oldCreative = player.getAbilities().instabuild;
        List<ItemStack> inventory = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var pos = test.absolutePos(new BlockPos(3, 3, 3)); var chunk = new ChunkPos(pos);
        boolean force = !level.getForcedChunks().contains(chunk.toLong());
        Map<UUID, Integer> fragmentOutputs = new HashMap<>(); ItemEntity[] blocker = {null}; boolean[] closed = {false};
        Runnable cleanup = () -> {
            if (closed[0]) return; closed[0] = true;
            if (blocker[0] != null) blocker[0].discard();
            for (var id : fragmentOutputs.keySet()) { var entity = level.getEntity(id); if (entity != null) entity.discard(); }
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.setGameMode(oldGameType); player.getAbilities().instabuild = oldCreative; player.setPos(oldPosition); knowledge.copyFrom(oldKnowledge);
            if (force) level.setChunkForced(chunk.x, chunk.z, false);
        };
        try {
            if (force) level.setChunkForced(chunk.x, chunk.z, true);
            player.getInventory().clearContent(); player.setGameMode(GameType.SURVIVAL); player.setPos(Vec3.atCenterOf(pos));
            recipe(test);
            for (Item variant : List.of(ThaumcraftUnknownKnowledge.UNKNOWN_NOTE.get(), ThaumcraftUnknownKnowledge.UNKNOWN_NOTE_24.get())) {
                knowledge.reset(); knowledge.setWarpPerm(7); knowledge.scanItem("#unknown-preserved");
                int count = variant == ThaumcraftUnknownKnowledge.UNKNOWN_NOTE.get() ? 128 : 1;
                ItemStack held = new ItemStack(variant, count);
                held.set(DataComponents.CUSTOM_NAME, Component.literal("unknown extras")); held.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                test.assertTrue(variant instanceof ItemUnknownResearchNotes && held.getMaxStackSize() == 1,
                        "both flattened variants retain the original note class and stack limit1");
                roundTrip(test, held);
                player.setItemInHand(InteractionHand.MAIN_HAND, held); var before = knowledge.save();
                String expected = ResearchHiddenSelection.choose(knowledge, level.getGameTime());
                // Original ResearchManager.findHiddenResearch:154 seeds the private
                // RNG with the absolute world time, never with the time of day.
                String dayTimeChoice = ResearchHiddenSelection.choose(knowledge, level.getDayTime());
                test.assertTrue(expected != null && !knowledge.isResearchComplete("@" + expected), "actual hidden choice needs no clue");
                if (level.getGameTime() != level.getDayTime() && !expected.equals(dayTimeChoice)) {
                    test.assertTrue(!expected.equals(dayTimeChoice), "the hidden choice follows the absolute world time, not the time of day");
                }
                var result = player.gameMode.useItem(player, level, held, InteractionHand.MAIN_HAND);
                var actual = player.getMainHandItem(); var note = ItemResearchNotes.data(actual);
                test.assertTrue(result.consumesAction()
                                && actual.is(ThaumcraftContent.RESEARCH_NOTE.get()) && actual.getCount() == count,
                        "real use changes unknown presentation to normal notes while preserving the entire count");
                test.assertTrue(note != null && note.key().equals(expected) && !note.complete() && note.copies() == 0
                                && note.cells().stream().anyMatch(cell -> cell.type() == 1) && ResearchCatalog.get(note.key()).isHidden(),
                        "actual world-RNG generation creates an unfinished hidden puzzle with copies0");
                test.assertTrue(knowledge.save().equals(before) && Component.literal("unknown extras").equals(actual.get(DataComponents.CUSTOM_NAME))
                                && Boolean.TRUE.equals(actual.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)),
                        "revealing charges no RP, paper, ink, clue, warp or completion and retains extra components");
                roundTrip(test, actual);
            }

            // Original ordinary completed-note branch precedes metadata24/42; after learning it may reveal on the next use.
            knowledge.reset(); var solved = ResearchNoteRuntimeChecks.solvedNitor();
            var priority = new ItemStack(ThaumcraftUnknownKnowledge.UNKNOWN_NOTE.get(), 2);
            priority.set(ThaumcraftContent.RESEARCH_NOTE_DATA.get(), solved); player.setItemInHand(InteractionHand.MAIN_HAND, priority);
            var beforeMissingParent = knowledge.save(); player.gameMode.useItem(player, level, priority, InteractionHand.MAIN_HAND);
            test.assertTrue(player.getMainHandItem() == priority && priority.getCount() == 2 && ItemResearchNotes.data(priority).equals(solved)
                            && knowledge.save().equals(beforeMissingParent), "missing prerequisites refuse the completed-note branch without revealing unknown knowledge");
            knowledge.addResearch("CRUCIBLE"); var beforeReuse = knowledge.save();
            var reused = ResearchNoteIssuance.issue(player, "NITOR", new java.util.Random(0L) {
                @Override public int nextInt(int bound) { throw new AssertionError("Reusing any note identity must not generate another grid"); }
            });
            test.assertTrue(reused.success() && reused.reused() && player.getMainHandItem() == priority && priority.getCount() == 2
                            && knowledge.save().equals(beforeReuse), "original same-key reuse includes unknown metadata identities and requires no paper, ink or generation RNG");
            player.gameMode.useItem(player, level, priority, InteractionHand.MAIN_HAND);
            test.assertTrue(priority.getCount() == 1 && knowledge.isResearchComplete("NITOR") && priority.is(ThaumcraftUnknownKnowledge.UNKNOWN_NOTE.get()),
                    "the first successful use consumes one completed unknown note to learn NITOR");
            String afterLearning = ResearchHiddenSelection.choose(knowledge, level.getGameTime()); var beforeReveal = knowledge.save();
            player.gameMode.useItem(player, level, priority, InteractionHand.MAIN_HAND);
            test.assertTrue(player.getMainHandItem().is(ThaumcraftContent.RESEARCH_NOTE.get())
                            && ItemResearchNotes.data(player.getMainHandItem()).key().equals(afterLearning) && knowledge.save().equals(beforeReveal),
                    "an already-known completed component takes the unknown reveal branch on the next use");

            for (var research : ResearchCatalog.all()) knowledge.addResearch(research.key());
            test.assertTrue(ResearchHiddenSelection.eligible(knowledge).isEmpty(), "the natural failure fixture has no remaining hidden candidates");
            int index = 0;
            for (Item variant : List.of(ThaumcraftUnknownKnowledge.UNKNOWN_NOTE.get(), ThaumcraftUnknownKnowledge.UNKNOWN_NOTE_24.get())) {
                player.setGameMode(index++ == 0 ? GameType.CREATIVE : GameType.SURVIVAL);
                // Every fragment drops where its actor stands, so the second note is used three
                // blocks away: two knowledge fragments on the same spot would merge into one stack
                // and the scenario could not read the original amount of each of them.
                player.setPos(Vec3.atCenterOf(pos).add(3.0 * (index - 1), 0, 0));
                var held = new ItemStack(variant, 2); player.setItemInHand(InteractionHand.MAIN_HAND, held);
                var beforeKnowledge = knowledge.save(); Set<UUID> before = new HashSet<>(pendingIds(test));
                player.gameMode.useItem(player, level, held, InteractionHand.MAIN_HAND);
                var ids = new ArrayList<>(pendingIds(test)); ids.removeAll(before);
                test.assertTrue(held.getCount() == 1 && player.getMainHandItem() == held && knowledge.save().equals(beforeKnowledge)
                                && ids.size() == 1, "empty pool consumes exactly one unknown note in creative and survival and gives one durable fragment output");
                UUID id = ids.getFirst(); var row = pendingRow(test, id); int count = row.getInt("Count");
                var physical = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElseThrow();
                test.assertTrue(count >= 7 && count <= 9 && physical.is(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get()),
                        "actual failure amount is original7+world.nextInt(3) knowledge fragments");
                fragmentOutputs.put(id, count);
            }
            var ids = new ArrayList<>(fragmentOutputs.keySet()); UUID blocked = ids.getFirst();
            blocker[0] = new ItemEntity(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, new ItemStack(Items.DIAMOND));
            blocker[0].setUUID(blocked);
            test.assertTrue(level.addFreshEntity(blocker[0]), "real conflicting entity reserves one durable output UUID before world POST");
            player.setPos(Vec3.atCenterOf(pos).add(20, 0, 0));
            test.runAfterDelay(2, () -> {
                if (closed[0]) return;
                try {
                    var queue = queue(test);
                    test.assertTrue(queue.pendingIds().contains(blocked) && level.getEntity(blocked) == blocker[0],
                            "real level-post preserves paid fragments when the native output UUID is occupied");
                    var persisted = queue.save(new CompoundTag(), level.registryAccess());
                    var reloaded = DeferredItemDrops.load(persisted, level.registryAccess());
                    test.assertTrue(reloaded.pendingIds().contains(blocked) && pendingRow(test, blocked).getInt("Count") == fragmentOutputs.get(blocked),
                            "pending unknown-note output survives native SavedData serialization without clamping or rerolling");
                    blocker[0].discard();
                } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
            });
            test.runAfterDelay(45, () -> {
                if (closed[0]) return;
                try {
                    for (var output : fragmentOutputs.entrySet()) {
                        var entity = level.getEntity(output.getKey());
                        test.assertTrue(entity instanceof ItemEntity item && item.getItem().is(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get())
                                        && item.getItem().getCount() == output.getValue() && !pendingIds(test).contains(output.getKey()),
                                "native POST retry delivers each paid fragment result exactly once with its original random amount");
                    }
                    cleanup.run(); test.succeed();
                } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
            });
        } catch (RuntimeException | Error error) { cleanup.run(); throw error; }
    }

    private static DeferredItemDrops queue(GameTestHelper test) {
        return test.getLevel().getDataStorage().get(QUEUE_READER, "thaumcraft_pending_inventory_drops");
    }
    private static List<UUID> pendingIds(GameTestHelper test) { var queue = queue(test); return queue == null ? List.of() : queue.pendingIds(); }
    private static CompoundTag pendingRow(GameTestHelper test, UUID id) {
        for (Tag raw : queue(test).save(new CompoundTag(), test.getLevel().registryAccess()).getList("Pending", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw; if (row.getUUID("Id").equals(id)) return row;
        }
        throw new IllegalStateException("Missing pending output " + id);
    }
    private static void roundTrip(GameTestHelper test, ItemStack actual) {
        var registries = test.getLevel().registryAccess();
        test.assertTrue(ItemStack.matches(ItemStack.parse(registries, actual.save(registries)).orElseThrow(), actual),
                "revealed whole note and extra components survive native persistent codec");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, actual);
            test.assertTrue(ItemStack.matches(ItemStack.STREAM_CODEC.decode(buffer), actual) && !buffer.isReadable(),
                    "revealed whole note survives native registry wire codec");
        } finally { buffer.release(); }
    }
    private static void recipe(GameTestHelper test) {
        var level = test.getLevel(); var id = ResourceLocation.fromNamespaceAndPath("thaumcraft", "unknown_research_note");
        var recipe = level.getRecipeManager().byKey(id).orElseThrow().value();
        test.assertTrue(recipe instanceof ShapedRecipe shaped && shaped.getWidth() == 3 && shaped.getHeight() == 3,
                "KnowFrag is an actual native shaped3x3 recipe");
        List<ItemStack> grid = new ArrayList<>();
        for (int cell = 0; cell < 9; cell++) grid.add(new ItemStack(ThaumcraftContent.MATERIALS.get("knowledge_fragment").get()));
        var input = CraftingInput.of(3, 3, grid);
        var matched = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        var result = matched.value().assemble(input, level.registryAccess());
        test.assertTrue(matched.id().equals(id) && result.is(ThaumcraftUnknownKnowledge.UNKNOWN_NOTE.get()) && result.getCount() == 1
                        && ItemResearchNotes.data(result) == null, "nine actual fragments craft one unknown metadata42 identity without preselected research");
        grid.set(4, ItemStack.EMPTY);
        test.assertTrue(!((ShapedRecipe) recipe).matches(CraftingInput.of(3, 3, grid), level), "eight fragments do not satisfy the original full nine-cell recipe");
    }
}
