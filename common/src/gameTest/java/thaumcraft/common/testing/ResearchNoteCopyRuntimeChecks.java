package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchTableCopyNetworking;
import thaumcraft.common.lib.network.ResearchTableCopyPayload;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.tiles.TileResearchTable;

/** Actual production receiver on an actual table/menu; direct calls are not a C2S-dispatch or client-rendering claim. */
public final class ResearchNoteCopyRuntimeChecks {
    private ResearchNoteCopyRuntimeChecks() {}
    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel(); var pos = test.absolutePos(new BlockPos(3, 3, 3));
        var first = level.getBlockState(pos); var second = level.getBlockState(pos.north()); var oldPosition = player.position();
        var knowledge = KnowledgeAccess.get(player); var oldKnowledge = knowledge.copy();
        List<ItemStack> oldInventory = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) oldInventory.add(player.getInventory().getItem(slot).copy());
        boolean creative = player.getAbilities().instabuild;
        TileResearchTable table = null;
        try {
            player.closeContainer(); player.getInventory().clearContent(); player.setPos(Vec3.atCenterOf(pos));
            player.getAbilities().instabuild = true; // Explicit duplicate costs apply even to creative, as in the original.
            level.setBlock(pos, ThaumcraftContent.TABLE.get().state(2), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(pos.north(), ThaumcraftContent.TABLE.get().state(7), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            table = (TileResearchTable) level.getBlockEntity(pos);
            test.assertTrue(table != null, "real research table is installed");
            var menu = new ContainerResearchTable(65, player.getInventory(), table); player.containerMenu = menu;
            knowledge.reset(); knowledge.addResearch("RESEARCHDUPE"); knowledge.setAspectPool("lux", 1000); knowledge.setAspectPool("ignis", 1000);
            knowledge.setWarpPerm(7); knowledge.scanItem("#copy-preserved");
            table.setBonusPools(Map.of("lux", 99, "ignis", 99));
            ItemStack tool = new ItemStack(ThaumcraftContent.INKWELL.get()); tool.setDamageValue(100); table.setItem(0, tool);
            ItemStack note = discovery(1, 0, true); table.setItem(1, note);
            ItemStack paper = new ItemStack(Items.PAPER, 2); paper.set(DataComponents.CUSTOM_NAME, Component.literal("named paper is legal"));
            ItemStack ink = new ItemStack(Items.BLACK_DYE, 2); ink.set(DataComponents.CUSTOM_NAME, Component.literal("named black dye is legal"));
            player.getInventory().setItem(0, paper); player.getInventory().setItem(1, ink); player.getInventory().setItem(2, new ItemStack(Items.FEATHER, 9));
            var stale = intent(menu);
            test.assertTrue(ResearchTableCopyNetworking.handle(player, stale), "real server receiver performs the first paid copy");
            test.assertTrue(table.getItem(1) == note && note.getCount() == 2 && ItemResearchNotes.data(note).copies() == 1,
                    "the SAME physical stack grows by one and shares one updated counter");
            test.assertTrue(knowledge.getAspectPoolFor("lux") == 997 && knowledge.getAspectPoolFor("ignis") == 999 && paper.getCount() == 1 && ink.getCount() == 1,
                    "first cost is original NITOR lux3/fire1 plus one paper and one BLACK DYE, even in creative");
            var beforeReplay = knowledge.save();
            test.assertTrue(!ResearchTableCopyNetworking.handle(player, stale) && beforeReplay.equals(knowledge.save()) && note.getCount() == 2 && paper.getCount() == 1,
                    "stale replay cannot copy or spend a second time");
            test.assertTrue(ResearchTableCopyNetworking.handle(player, intent(menu)), "a fresh second intent copies again");
            test.assertTrue(note.getCount() == 3 && ItemResearchNotes.data(note).copies() == 2 && knowledge.getAspectPoolFor("lux") == 993
                            && knowledge.getAspectPoolFor("ignis") == 997 && paper.isEmpty() && ink.isEmpty(),
                    "second cost uses OLD copies1: lux4/fire2, not the new counter");
            test.assertTrue(table.getItem(0) == tool && tool.getDamageValue() == 100 && player.getInventory().getItem(2).getCount() == 9
                            && table.bonusPools().equals(Map.of("lux", 99, "ignis", 99)) && knowledge.getWarpPerm() == 7
                            && knowledge.hasScannedItem("#copy-preserved") && !knowledge.isResearchComplete("NITOR"),
                    "exhausted scribing tool, feathers, shared bonus, warp and research completion are untouched");
            var savedName = note.get(DataComponents.CUSTOM_NAME);
            test.assertTrue(savedName.equals(Component.literal("R10 ownership")) && Boolean.TRUE.equals(note.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)),
                    "custom stack components survive copying");

            supplies(player); knowledge.setAspectPool("lux", 0);
            refuse(test, player, menu, table, "shared table bonus cannot pay a missing personal aspect");
            knowledge.setAspectPool("lux", 1000); knowledge.removeResearch("RESEARCHDUPE");
            refuse(test, player, menu, table, "missing RESEARCHDUPE is refused on the server"); knowledge.addResearch("RESEARCHDUPE");
            table.setItem(1, discovery(1, 0, false)); refuse(test, player, menu, table, "forged discovery presentation without solved data is refused");
            var ordinary = discovery(1, 0, true); ordinary = ItemResearchNotes.create(ItemResearchNotes.data(ordinary), false);
            table.setItem(1, ordinary); refuse(test, player, menu, table, "normal presentation cannot invoke the discovery-only copy branch");
            table.setItem(1, discovery(1, 0, true));
            var correct = intent(menu); var beforeForged = knowledge.save();
            test.assertTrue(!ResearchTableCopyNetworking.handle(player, new ResearchTableCopyPayload(menu.containerId, menu.sessionToken() + 1, correct.expectedTableRevision()))
                            && knowledge.save().equals(beforeForged), "another session cannot copy this table");
            player.setPos(Vec3.atCenterOf(pos).add(20, 0, 0)); refuse(test, player, menu, table, "an out-of-range sender cannot copy");
            player.setPos(Vec3.atCenterOf(pos));
            player.getInventory().setItem(0, ItemStack.EMPTY); player.getInventory().setItem(40, new ItemStack(Items.PAPER));
            refuse(test, player, menu, table, "offhand paper is not the original main-inventory supply"); player.getInventory().setItem(40, ItemStack.EMPTY);

            for (int count : List.of(99, 127)) {
                knowledge.setAspectPool("lux", 1000); knowledge.setAspectPool("ignis", 1000); supplies(player);
                ItemStack crossing = discovery(count, count - 1, true); table.setItem(1, crossing);
                test.assertTrue(ResearchTableCopyNetworking.handle(player, intent(menu)), "natural copy crosses count " + count);
                test.assertTrue(crossing.getCount() == count + 1 && ItemResearchNotes.data(crossing).copies() == count
                                && knowledge.getAspectPoolFor("lux") == 1000 - (count + 2) && knowledge.getAspectPoolFor("ignis") == 1000 - count,
                        "no clamp, split or counter reset at " + count + "; exact personal aspect prices");
                test.assertTrue(ItemStack.matches(ItemStack.parse(level.registryAccess(), crossing.save(level.registryAccess())).orElseThrow(), crossing),
                        "naturally produced count " + (count + 1) + " persists through native ItemStack codec");
            }
            supplies(player); table.setItem(1, discovery(Integer.MAX_VALUE, 0, true));
            refuse(test, player, menu, table, "positive-int count overflow is refused before any resource is consumed");
            table.setItem(1, discovery(1, Integer.MAX_VALUE, true));
            refuse(test, player, menu, table, "copy-counter overflow is refused before any resource is consumed");
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            supplies(player); table.setItem(1, discovery(1, 0, true));
            knowledge.setAspectPool("lux", 1000); knowledge.setAspectPool("ignis", 1000);
            test.assertTrue(ResearchTableCopyNetworking.handle(player, intent(menu)) && table.getItem(1).getCount() == 2,
                    "a full player inventory needs no output space because the same table stack grows");
            test.succeed();
        } finally {
            player.containerMenu = player.inventoryMenu; player.getInventory().clearContent();
            for (int slot = 0; slot < oldInventory.size(); slot++) player.getInventory().setItem(slot, oldInventory.get(slot));
            player.getAbilities().instabuild = creative; player.setPos(oldPosition); knowledge.copyFrom(oldKnowledge);
            if (table != null) table.clearContent();
            level.setBlock(pos, first, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(pos.north(), second, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
    private static ItemStack discovery(int count, int copies, boolean complete) {
        var base = ResearchNoteRuntimeChecks.solvedNitor();
        var data = new ResearchNoteData(base.key(), base.color(), complete, copies, base.cells());
        ItemStack result = ItemResearchNotes.create(data, true); result.setCount(count);
        result.set(DataComponents.CUSTOM_NAME, Component.literal("R10 ownership")); result.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); return result;
    }
    private static ResearchTableCopyPayload intent(ContainerResearchTable menu) {
        return new ResearchTableCopyPayload(menu.containerId, menu.sessionToken(), menu.tableRevision());
    }
    private static void supplies(ServerPlayer player) {
        player.getInventory().setItem(0, new ItemStack(Items.PAPER)); player.getInventory().setItem(1, new ItemStack(Items.BLACK_DYE));
    }
    private static void refuse(GameTestHelper test, ServerPlayer player, ContainerResearchTable menu, TileResearchTable table, String reason) {
        var knowledge = KnowledgeAccess.get(player); var before = knowledge.save();
        ItemStack note = table.getItem(1).copy(), paper = player.getInventory().getItem(0).copy(), ink = player.getInventory().getItem(1).copy();
        test.assertTrue(!ResearchTableCopyNetworking.handle(player, intent(menu)) && knowledge.save().equals(before)
                        && ItemStack.matches(note, table.getItem(1)) && ItemStack.matches(paper, player.getInventory().getItem(0))
                        && ItemStack.matches(ink, player.getInventory().getItem(1)), reason);
    }
}
