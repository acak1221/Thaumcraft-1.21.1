package thaumcraft.common.testing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftHungryChest;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.ResearchTableCopyNetworking;
import thaumcraft.common.lib.network.ResearchTableCopyPayload;
import thaumcraft.common.tiles.TileChestHungry;
import thaumcraft.common.tiles.TileResearchTable;

/** Real paid copying followed by native inventory packets; no survival acquisition claim. */
final class CopiedNoteContainerRuntimeChecks {
    private CopiedNoteContainerRuntimeChecks() {}

    static void hungryChestTransfers(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            f.level.setBlock(f.base, ThaumcraftContent.TABLE.get().state(2), TubeVariantsRuntimeChecks.QUIET);
            f.level.setBlock(f.base.north(), ThaumcraftContent.TABLE.get().state(7), TubeVariantsRuntimeChecks.QUIET);
            var table = (TileResearchTable) f.level.getBlockEntity(f.base);
            var note = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
            note.set(DataComponents.CUSTOM_NAME, Component.literal("paid copied Hungry Chest research"));
            table.setItem(1, note);
            var knowledge = KnowledgeAccess.get(f.player);
            knowledge.addResearch("RESEARCHDUPE"); knowledge.setAspectPool("lux", 1000); knowledge.setAspectPool("ignis", 1000);
            f.player.getInventory().setItem(0, new ItemStack(Items.PAPER));
            f.player.getInventory().setItem(1, new ItemStack(Items.BLACK_DYE));
            test.assertTrue(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerResearchTable,
                    "the physical research table opens its actual menu for paid copying");
            var research = (ContainerResearchTable) f.player.containerMenu;
            test.assertTrue(ResearchTableCopyNetworking.handle(f.player, new ResearchTableCopyPayload(
                            research.containerId, research.sessionToken(), research.tableRevision()))
                            && note.getCount() == 2 && ItemResearchNotes.data(note).copies() == 1
                            && f.player.getInventory().getItem(0).isEmpty() && f.player.getInventory().getItem(1).isEmpty()
                            && knowledge.getAspectPoolFor("lux") == 997 && knowledge.getAspectPoolFor("ignis") == 999,
                    "one actual copy pays paper, black dye and aspect points for a two-note physical stack");
            ItemStack paid = note.copy();
            click(f, 1);
            test.assertTrue(table.getItem(1).isEmpty() && ItemStack.matches(paid, f.player.getInventory().getItem(8)),
                    "native research-table quick-move returns the complete paid two-note stack");
            f.player.closeContainer();

            f.put(f.base, ThaumcraftHungryChest.HUNGRY_CHEST.get());
            // The second count is an explicit codec-boundary input, not another
            // claimed acquisition/copy run. The first stack was paid above.
            for (int count : new int[] {2, 128}) {
                ItemStack expected = paid.copyWithCount(count);
                f.player.getInventory().setItem(8, expected.copy());
                test.assertTrue(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ChestMenu,
                        "the actual Hungry Chest interaction opens its native three-row menu");
                var chest = (TileChestHungry) f.level.getBlockEntity(f.base);
                click(f, 62); //27 chest +27 main +hotbar8
                test.assertTrue(f.player.getInventory().getItem(8).isEmpty()
                                && ItemStack.matches(expected, chest.getItem(0)) && f.player.containerMenu.getCarried().isEmpty(),
                        "native shift insertion preserves every copied discovery and all its components at count " + count);
                f.player.closeContainer();
                var saved = chest.saveWithFullMetadata(f.level.registryAccess());
                f.level.removeBlockEntity(f.base);
                var restored = BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), saved, f.level.registryAccess());
                test.assertTrue(restored instanceof TileChestHungry && ItemStack.matches(expected, ((TileChestHungry) restored).getItem(0)),
                        "the real registered block-entity codec retains the complete copied stack at count " + count);
                f.level.setBlockEntity(restored);
                test.assertTrue(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ChestMenu,
                        "the restored actual Hungry Chest opens a new native menu");
                click(f, 0);
                test.assertTrue(((TileChestHungry) restored).isEmpty()
                                && ItemStack.matches(expected, f.player.getInventory().getItem(8))
                                && f.player.containerMenu.getCarried().isEmpty() && f.drops().isEmpty(),
                        "native shift withdrawal returns the entire saved stack once without cursor copies or drops at count " + count);
                f.player.closeContainer();
            }

            var chest = (TileChestHungry) f.level.getBlockEntity(f.base);
            chest.setItem(0, new ItemStack(Items.STONE, 65));
            chest.setItem(1, new ItemStack(Items.IRON_PICKAXE, 2));
            test.assertTrue(chest.isEmpty(), "ordinary oversized stacks and unrelated unstackable items keep their original refusal guard");
            for (int slot = 0; slot < chest.getContainerSize(); slot++) chest.setItem(slot, new ItemStack(Items.STONE, 64));
            ItemStack before = f.player.getInventory().getItem(8).copy();
            test.assertTrue(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ChestMenu,
                    "a full native Hungry Chest still opens");
            click(f, 62);
            test.assertTrue(ItemStack.matches(before, f.player.getInventory().getItem(8))
                            && java.util.stream.IntStream.range(0, chest.getContainerSize())
                                    .allMatch(slot -> chest.getItem(slot).is(Items.STONE) && chest.getItem(slot).getCount() == 64),
                    "a full chest refuses the whole copied stack without spending the source or replacing other inventory");
        }
    }

    private static void click(TubeVariantsRuntimeChecks.Fixture f, int slot) {
        var menu = f.player.containerMenu;
        f.player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(),
                slot, 0, ClickType.QUICK_MOVE, ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
    }
}
