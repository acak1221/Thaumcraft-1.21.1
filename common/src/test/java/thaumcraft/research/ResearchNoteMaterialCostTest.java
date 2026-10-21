package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The original material cost of a TC4 4.2.3.5 research note and the persistence
 * of the research progress of a player.
 *
 * <p>Oracle {@code thaumcraft/common/lib/research/ResearchManager.java:102-119}
 * {@code createResearchNoteForPlayer} spends the paper and the ink:
 * {@code consumeInkFromPlayer(player, false) && player.inventory.consumeInventoryItem(Items.paper)}.
 * The 1.7.10 JAR calls {@code InventoryPlayer.func_146026_a(Item)}; the official
 * {@code mcp_stable-12-1.7.10} mapping names that method {@code consumeInventoryItem}
 * ("removed one item of specified Item from inventory"). The note therefore costs
 * exactly one paper of the first main-inventory paper stack and one ink point.</p>
 *
 * <p>Oracle {@code thaumcraft/common/lib/research/ResearchManager.java:881-904}
 * {@code saveResearchNBT} persists the completed research; the native port keeps
 * the same state in the player NBT of 1.21.1.</p>
 */
class ResearchNoteMaterialCostTest {
    private static List<ItemStack> inventory() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) items.add(new ItemStack(Items.AIR));
        return items;
    }

    @Test void theOriginalNoteCostsExactlyOnePaperOfTheFirstPaperStack() {
        List<ItemStack> before = inventory();
        before.set(4, new ItemStack(Items.PAPER, 5));
        before.set(6, new ItemStack(Items.PAPER, 9));
        List<ItemStack> staged = ResearchNoteIssuance.stage(before, 4, -1, new ScribingInk.Plan(false, 0, 0));
        assertEquals(4, staged.get(4).getCount(), "one paper of the first paper stack pays for the note");
        assertEquals(9, staged.get(6).getCount(), "a later paper stack is never debited");
        assertEquals(5, before.get(4).getCount(), "staging never mutates the live stack");
    }

    @Test void theOriginalEmptiedPaperStackFreesItsSlot() {
        List<ItemStack> before = inventory();
        before.set(4, new ItemStack(Items.PAPER, 1));
        List<ItemStack> staged = ResearchNoteIssuance.stage(before, 4, -1, new ScribingInk.Plan(false, 0, 0));
        assertTrue(staged.get(4).isEmpty(), "a single paper stack empties and frees its slot");
        assertEquals(1, before.get(4).getCount());
    }

    @Test void theOriginalNoteAlsoSpendsExactlyOneInkPoint() {
        List<ItemStack> before = inventory();
        before.set(4, new ItemStack(Items.PAPER, 5));
        before.set(9, new ItemStack(Items.STONE_PICKAXE));
        List<ItemStack> staged = ResearchNoteIssuance.stage(before, 4, 9, new ScribingInk.Plan(true, 0, 1));
        assertEquals(4, staged.get(4).getCount());
        assertEquals(1, staged.get(9).getDamageValue(), "one ink point is spent");
        assertEquals(0, before.get(9).getDamageValue());
    }

    @Test void theOriginalPaperSlotIsTheFirstMainInventoryPaperStack() {
        List<ItemStack> before = inventory();
        before.set(2, new ItemStack(Items.PAPER, 3));
        assertEquals(2, ResearchNoteIssuance.paperSlot(before));
        before.set(2, new ItemStack(Items.AIR));
        assertEquals(-1, ResearchNoteIssuance.paperSlot(before));
    }

    @Test void theResearchProgressSurvivesASaveAndLoadRoundTrip() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.addResearch("NITOR");
        knowledge.addDiscoveredAspect("lux");
        knowledge.setAspectPool("lux", 7);
        knowledge.scanItem("@minecraft:stone");
        knowledge.scanEntity("#minecraft:pig");
        knowledge.setWarpPerm(3);
        knowledge.setWarpSticky(2);
        knowledge.setWarpTemp(1);
        knowledge.setWarpCounter(6);
        CompoundTag saved = knowledge.save();
        PlayerKnowledge restored = new PlayerKnowledge();
        restored.load(saved);
        assertEquals(saved, restored.save(), "every piece of research progress survives the round trip");
        assertTrue(restored.isResearchComplete("NITOR"));
        assertTrue(restored.hasDiscoveredAspect("lux"));
        assertEquals(7, restored.getAspectPoolFor("lux"));
        assertTrue(restored.hasScannedItem("@minecraft:stone"));
        assertTrue(restored.hasScannedEntity("#minecraft:pig"));
        assertEquals(3, restored.getWarpPerm());
        assertEquals(2, restored.getWarpSticky());
        assertEquals(1, restored.getWarpTemp());
        assertEquals(6, restored.getWarpCounter());
        assertFalse(restored.getResearchComplete().isEmpty());
    }
}
