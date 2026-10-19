package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchTableEditNetworking;
import thaumcraft.common.lib.network.ResearchTableEditPayload;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.tiles.TileResearchTable;

/** One native world/menu/service scenario, not a C2S-dispatch or client-rendering claim. */
public final class ResearchTableEditingRuntimeChecks {
    private ResearchTableEditingRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        List<SavedBlock> blocks = List.of(save(level, pos), save(level, pos.north()));
        var knowledge = KnowledgeAccess.get(player);
        var originalKnowledge = knowledge.copy();
        List<ItemStack> inventory = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var originalPosition = player.position();
        int selected = player.getInventory().selected;
        boolean creative = player.getAbilities().instabuild;
        boolean sneaking = player.isShiftKeyDown();
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.getAbilities().instabuild = true;
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(pos));
            knowledge.reset();
            knowledge.setInitializedAspects(true);
            knowledge.setAspectPool("lux", 2);
            knowledge.scanItem("#r08-unchanged-scan");
            knowledge.setWarpPerm(3);
            var expectedKnowledge = knowledge.copy();
            expectedKnowledge.setAspectPool("lux", 0);

            for (SavedBlock block : blocks) clear(level, block.pos());
            var tableBlock = ThaumcraftContent.TABLE.get();
            level.setBlock(pos, tableBlock.state(2), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(pos.north(), tableBlock.state(7), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            var table = (TileResearchTable) level.getBlockEntity(pos);
            ItemStack ink = new ItemStack(ThaumcraftContent.INKWELL.get());
            ink.setDamageValue(97);
            ink.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
            table.setItem(0, ink);
            var note = new ResearchNoteData("NITOR", 0x14253a, false, 7, List.of(
                    new ResearchNoteData.Cell(0, 0, 1, "aer"),
                    new ResearchNoteData.Cell(2, 0, 1, "ignis"),
                    new ResearchNoteData.Cell(1, 0, 0, null),
                    new ResearchNoteData.Cell(-1, 0, 0, null)));
            ItemStack stack = ItemResearchNotes.create(note, false);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("R08 preserved name"));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            table.setItem(1, stack);
            table.setBonusPools(Map.of("lux", 1));
            open(player, pos);
            test.assertTrue(player.containerMenu instanceof ContainerResearchTable, "placed native table opens the actual registered menu");
            var menu = (ContainerResearchTable) player.containerMenu;
            test.assertTrue(menu.tile() == table && menu.sessionToken() > 0, "current menu binds exact physical table and a server-issued session");

            long revision = menu.tableRevision();
            table.getItem(1).setCount(127); // Direct-loaded/R10 shape, deliberately not setter insertion.
            menu.broadcastChanges();
            test.assertTrue(menu.tableRevision() == revision + 1, "menu broadcast detects in-place count changes without setChanged");
            var stale = intent(menu, ResearchNoteEditing.Action.PLACE, -1, 0, "lux");
            CompoundTag unrelated = new CompoundTag();
            unrelated.putString("r08-unrelated", "in-place component survives discovery");
            var customData = CustomData.of(unrelated);
            table.getItem(1).set(DataComponents.CUSTOM_DATA, customData);
            test.assertTrue(!ResearchTableEditNetworking.handle(player, stale)
                    && menu.tableRevision() == stale.expectedTableRevision() + 1,
                    "admission independently detects in-place component changes and refuses stale intent");
            test.assertTrue(table.getItem(0).getDamageValue() == 97 && knowledge.getAspectPoolFor("lux") == 2,
                    "stale component request spends neither ink nor actor RP");
            denied(test, player, menu, intent(menu, ResearchNoteEditing.Action.ERASE, 0, 0, null));
            denied(test, player, menu, intent(menu, ResearchNoteEditing.Action.PLACE, 1, 0, "perditio"));
            denied(test, player, menu, new ResearchTableEditPayload(menu.containerId, menu.sessionToken() + 1,
                    menu.tableRevision(), ResearchNoteEditing.Action.PLACE, -1, 0, "lux"));

            var place = intent(menu, ResearchNoteEditing.Action.PLACE, -1, 0, "lux");
            test.assertTrue(ResearchTableEditNetworking.handle(player, place), "funded discovered aspect is admitted through production receiver");
            test.assertTrue(menu.tableRevision() == place.expectedTableRevision() + 1
                    && ItemResearchNotes.data(table.getItem(1)).cellAt(-1, 0).type() == 2
                    && !ItemResearchNotes.data(table.getItem(1)).complete(), "one logical edit changes one revision and keeps disconnected endpoints unfinished");
            test.assertTrue(knowledge.getAspectPoolFor("lux") == 1 && table.bonusFor("lux") == 1
                    && table.getItem(0).getDamageValue() == 98, "actor RP is spent before bonus, with literal ink despite creative and Unbreakable");
            denied(test, player, menu, place); // Exact replay of the already-consumed revision.

            var erase = intent(menu, ResearchNoteEditing.Action.ERASE, -1, 0, null);
            test.assertTrue(ResearchTableEditNetworking.handle(player, erase), "placed cell erases using its authoritative aspect");
            test.assertTrue(menu.tableRevision() == erase.expectedTableRevision() + 1
                    && ItemResearchNotes.data(table.getItem(1)).cellAt(-1, 0).type() == 0
                    && knowledge.getAspectPoolFor("lux") == 1 && table.getItem(0).getDamageValue() == 99,
                    "erase without Researcher spends ink, leaves a blank and cannot refund RP");

            var solve = intent(menu, ResearchNoteEditing.Action.PLACE, 1, 0, "lux");
            test.assertTrue(ResearchTableEditNetworking.handle(player, solve), "aer-lux-ignis bridge solves through the native transaction");
            ItemStack discovery = table.getItem(1);
            var solved = ItemResearchNotes.data(discovery);
            test.assertTrue(menu.tableRevision() == solve.expectedTableRevision() + 1
                    && discovery.is(ThaumcraftContent.DISCOVERY.get()) && solved.complete() && solved.cells().size() == 3
                    && solved.cellAt(-1, 0) == null, "solver prunes blank branch and changes normal note to discovery exactly once");
            test.assertTrue(discovery.getCount() == 127 && "NITOR".equals(solved.key()) && solved.color() == note.color()
                    && solved.copies() == 7 && Component.literal("R08 preserved name").equals(discovery.get(DataComponents.CUSTOM_NAME))
                    && Boolean.TRUE.equals(discovery.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
                    && customData.equals(discovery.get(DataComponents.CUSTOM_DATA)), "solve preserves count127, note identity fields and unrelated component patch");
            test.assertTrue(table.getItem(0).getCount() == 1 && table.getItem(0).getDamageValue() == 100
                    && table.bonusFor("lux") == 1, "last literal ink point retains the exhausted tool and actor-first bonus");
            test.assertValueEqual(knowledge.save(), expectedKnowledge.save(), "solving spends only two actor RP and grants no research, scans or warp");
            denied(test, player, menu, solve);
            denied(test, player, menu, intent(menu, ResearchNoteEditing.Action.ERASE, 1, 0, null));

            var restored = new TileResearchTable(pos, table.getBlockState());
            restored.loadWithComponents(table.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(ItemStack.matches(restored.getItem(0), table.getItem(0))
                    && ItemStack.matches(restored.getItem(1), discovery) && restored.bonusPools().equals(table.bonusPools())
                    && restored.tableRevision() == table.tableRevision(), "native persistence retains solved components/count, bonus and revision without setter clamping");
            long oldSession = menu.sessionToken();
            player.closeContainer();
            open(player, pos);
            var reopened = (ContainerResearchTable) player.containerMenu;
            test.assertTrue(reopened.sessionToken() != oldSession, "each actual menu opening receives a fresh non-world-RNG session");
            denied(test, player, reopened, solve);
        } finally {
            player.closeContainer();
            for (SavedBlock block : blocks) clear(level, block.pos());
            for (SavedBlock block : blocks) {
                level.setBlock(block.pos(), block.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var be = level.getBlockEntity(block.pos());
                if (be != null && block.tag() != null) be.loadWithComponents(block.tag(), level.registryAccess());
            }
            knowledge.copyFrom(originalKnowledge);
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected;
            player.getAbilities().instabuild = creative;
            player.setShiftKeyDown(sneaking);
            player.setPos(originalPosition);
        }
        test.succeed();
    }

    private static ResearchTableEditPayload intent(ContainerResearchTable menu, ResearchNoteEditing.Action action, int q, int r, String aspect) {
        return new ResearchTableEditPayload(menu.containerId, menu.sessionToken(), menu.tableRevision(), action, q, r, aspect);
    }

    private static void denied(GameTestHelper test, ServerPlayer player, ContainerResearchTable menu, ResearchTableEditPayload payload) {
        var table = menu.tile();
        CompoundTag inventory = table.saveWithFullMetadata(player.registryAccess());
        CompoundTag knowledge = KnowledgeAccess.get(player).save();
        test.assertTrue(!ResearchTableEditNetworking.handle(player, payload), "invalid/replayed edit is refused");
        test.assertValueEqual(table.saveWithFullMetadata(player.registryAccess()), inventory, "refusal preserves complete table inventory, components, bonus and revision");
        test.assertValueEqual(KnowledgeAccess.get(player).save(), knowledge, "refusal preserves all actor knowledge and costs");
    }

    private static void open(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    private static SavedBlock save(ServerLevel level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        return new SavedBlock(pos, level.getBlockState(pos), be == null ? null : be.saveWithFullMetadata(level.registryAccess()));
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileResearchTable table) table.clearContent();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private record SavedBlock(BlockPos pos, BlockState state, CompoundTag tag) {}
}
