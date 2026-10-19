package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchTableCombineNetworking;
import thaumcraft.common.lib.network.ResearchTableCombinePayload;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.tiles.TileResearchTable;

/** One real table/menu/service lifecycle; not a C2S-dispatch or GUI-rendering claim. */
public final class ResearchAspectCombinationRuntimeChecks {
    private ResearchAspectCombinationRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        int firstY = Math.max(pos.getY() - 8, level.getMinBuildHeight() + 1);
        BlockPos producer = new BlockPos(pos.getX() - 8, firstY, pos.getZ() - 8);
        List<SavedBlock> blocks = List.of(save(level, pos), save(level, pos.north()), save(level, producer));
        var knowledge = KnowledgeAccess.get(player);
        var beforeKnowledge = knowledge.copy();
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) inventory.add(player.getInventory().getItem(i).copy());
        var beforePosition = player.position();
        boolean creative = player.getAbilities().instabuild, sneaking = player.isShiftKeyDown();
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getAbilities().instabuild = true;
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atCenterOf(pos));
            knowledge.reset();
            knowledge.setInitializedAspects(true);
            knowledge.setAspectPool("aer", 2);
            knowledge.setAspectPool("ignis", 2);
            knowledge.addResearch("RESEARCHER1");
            knowledge.addResearch("RESEARCHER2");
            knowledge.scanItem("#r09-unchanged");
            knowledge.setWarpPerm(3);
            for (SavedBlock block : blocks) clear(level, block.pos());
            level.setBlock(pos, ThaumcraftContent.TABLE.get().state(2), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(pos.north(), ThaumcraftContent.TABLE.get().state(7), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            var table = (TileResearchTable) level.getBlockEntity(pos);
            ItemStack ink = new ItemStack(ThaumcraftContent.INKWELL.get());
            ink.setDamageValue(ink.getMaxDamage());
            ItemStack notes = ItemResearchNotes.create(new ResearchNoteData("NITOR", 0x123456, false, 2,
                    List.of(new ResearchNoteData.Cell(0, 0, 1, "aer"))), false);
            notes.set(DataComponents.CUSTOM_NAME, Component.literal("R09 untouched note"));
            table.setItem(0, ink);
            table.setItem(1, notes);
            notes.setCount(127);
            ItemStack inkSnapshot = ink.copy(), noteSnapshot = notes.copy();
            table.setBonusPools(Map.of("aer", 1, "ignis", 1));
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            test.assertTrue(player.containerMenu instanceof ContainerResearchTable, "native table opens the registered combine menu");
            var menu = (ContainerResearchTable) player.containerMenu;

            var first = intent(menu, "aer", "ignis");
            accepted(test, player, menu, first);
            test.assertTrue(knowledge.hasDiscoveredAspect("lux") && knowledge.getAspectPoolFor("lux") == 3
                    && knowledge.getAspectPoolFor("aer") == 1 && knowledge.getAspectPoolFor("ignis") == 1
                    && table.bonusFor("aer") == 1 && table.bonusFor("ignis") == 1,
                    "first compound gives 1+2 RP and pays actor pools before shared bonuses, even in creative with Researcher2");
            denied(test, player, first);
            accepted(test, player, menu, intent(menu, "ignis", "aer"));
            test.assertTrue(knowledge.getAspectPoolFor("lux") == 4 && knowledge.getAspectPoolFor("aer") == 0
                    && knowledge.getAspectPoolFor("ignis") == 0, "known unordered compound gives only one RP");
            test.assertTrue(table.getItem(0) == ink && table.getItem(1) == notes && ItemStack.matches(ink, inkSnapshot)
                    && ItemStack.matches(notes, noteSnapshot), "combination preserves exhausted ink, slot identities, count127 and all note components");

            knowledge.setAspectPool("aer", 1);
            table.setBonusPools(Map.of());
            denied(test, player, intent(menu, "aer", "aer"));
            table.setBonusPools(Map.of("aer", 1));
            accepted(test, player, menu, intent(menu, "aer", "aer"));
            test.assertTrue(knowledge.getAspectPoolFor("aer") == 0 && table.bonusFor("aer") == 0
                    && knowledge.getAspectPoolFor("lux") == 4, "same-aspect experiment atomically costs two: RP1 then bonus1, with no invented failure output");
            table.setBonusPools(Map.of("aer", 1, "ignis", 1));
            accepted(test, player, menu, intent(menu, "aer", "ignis"));
            test.assertTrue(knowledge.getAspectPoolFor("lux") == 5 && table.bonusPools().isEmpty(), "zero actor RP can combine using two real table bonuses");
            table.setBonusPools(Map.of("motus", 1, "aer", 1));
            denied(test, player, intent(menu, "motus", "aer"));
            denied(test, player, new ResearchTableCombinePayload(menu.containerId, menu.sessionToken() + 1,
                    menu.tableRevision(), "aer", "ignis"));
            test.assertTrue(!knowledge.hasDiscoveredAspect("motus") && knowledge.hasScannedItem("#r09-unchanged")
                    && knowledge.getWarpPerm() == 3, "shared bonuses cannot discover an input or mutate unrelated knowledge");

            table.clearContent();
            table.setBonusPools(Map.of());
            level.setBlock(producer, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            long revision = table.tableRevision();
            for (int tick = 0; tick < 601; tick++) TileResearchTable.tickTopologyOnLoad(level, pos, table.getBlockState(), table);
            test.assertTrue(table.bonusPools().isEmpty() && table.tableRevision() == revision
                    && table.saveWithFullMetadata(level.registryAccess()).getInt("NextBonusRecalc") == 601,
                    "valid main keeps ticking after R07 repair but ecology waits the original 601 ticks");
            // The first scanned block is a real dirt producer. Find a seed whose
            // first producer roll wins, after all admitted environmental draws.
            int draws = environmentalDraws(level, pos);
            long seed = 0;
            for (;; seed++) {
                RandomSource probe = RandomSource.create(seed);
                for (int i = 0; i < draws; i++) probe.nextInt(20);
                if (probe.nextInt(20) == 0) break;
            }
            level.random.setSeed(seed);
            TileResearchTable.tickTopologyOnLoad(level, pos, table.getBlockState(), table);
            test.assertTrue(table.bonusFor("terra") == 1 && table.tableRevision() == revision + 1,
                    "602nd native tick produces real surroundings bonus and exactly one material revision");
            var restored = new TileResearchTable(pos, table.getBlockState());
            restored.loadWithComponents(table.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            test.assertTrue(restored.bonusPools().equals(table.bonusPools()) && restored.tableRevision() == table.tableRevision(),
                    "produced shared bonuses and revision persist in the native BE schema");
            knowledge.setAspectPool("aqua", 1);
            accepted(test, player, menu, intent(menu, "terra", "aqua"));
            test.assertTrue(table.bonusFor("terra") == 0 && knowledge.getAspectPoolFor("aqua") == 0
                    && knowledge.getAspectPoolFor("victus") == 3 && table.isEmpty(),
                    "produced bonus is consumable with empty tool/note slots and discovers the new compound");
            Map<String, Integer> full = new LinkedHashMap<>();
            Aspect.aspects.keySet().forEach(tag -> full.put(tag, 1));
            table.setBonusPools(full);
            revision = table.tableRevision();
            for (int tick = 0; tick < 602; tick++) TileResearchTable.tickTopologyOnLoad(level, pos, table.getBlockState(), table);
            test.assertTrue(table.bonusPools().equals(full) && table.tableRevision() == revision,
                    "ecology merges max(existing,1), does not stack free creative RP, and unchanged pools do not revise the table");
        } finally {
            player.closeContainer();
            for (SavedBlock block : blocks) clear(level, block.pos());
            for (SavedBlock block : blocks) {
                level.setBlock(block.pos(), block.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var be = level.getBlockEntity(block.pos());
                if (be != null && block.tag() != null) be.loadWithComponents(block.tag(), level.registryAccess());
            }
            knowledge.copyFrom(beforeKnowledge);
            for (int i = 0; i < inventory.size(); i++) player.getInventory().setItem(i, inventory.get(i));
            player.getAbilities().instabuild = creative;
            player.setShiftKeyDown(sneaking);
            player.setPos(beforePosition);
        }
        test.succeed();
    }

    private static ResearchTableCombinePayload intent(ContainerResearchTable menu, String left, String right) {
        return new ResearchTableCombinePayload(menu.containerId, menu.sessionToken(), menu.tableRevision(), left, right);
    }

    private static void accepted(GameTestHelper test, ServerPlayer player, ContainerResearchTable menu, ResearchTableCombinePayload payload) {
        test.assertTrue(ResearchTableCombineNetworking.handle(player, payload) && menu.tableRevision() == payload.expectedTableRevision() + 1,
                "production receiver commits the complete combine as exactly one revision");
    }

    private static void denied(GameTestHelper test, ServerPlayer player, ResearchTableCombinePayload payload) {
        var table = ((ContainerResearchTable) player.containerMenu).tile();
        CompoundTag beforeTable = table.saveWithFullMetadata(player.registryAccess()), beforeKnowledge = KnowledgeAccess.get(player).save();
        test.assertTrue(!ResearchTableCombineNetworking.handle(player, payload), "unfunded, unknown, wrong-session or replayed intent is refused");
        test.assertValueEqual(table.saveWithFullMetadata(player.registryAccess()), beforeTable, "refusal leaves table, bonuses, slots and revision intact");
        test.assertValueEqual(KnowledgeAccess.get(player).save(), beforeKnowledge, "refusal spends neither input or grants discovery/RP");
    }

    private static int environmentalDraws(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        int draws = level.getSkyDarken() >= 4 && level.getMaxLocalRawBrightness(above) < 4 && !level.canSeeSky(above) ? 1 : 0;
        int altitude = pos.getY() - level.getMinBuildHeight(), height = level.dimensionType().logicalHeight();
        for (float fraction : new float[]{.5F, .66F, .75F}) if ((float) altitude > (float) height * fraction) draws++;
        return draws;
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
