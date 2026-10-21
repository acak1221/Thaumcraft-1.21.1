package thaumcraft.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.SpecialMining;

/**
 * The special mining result table of the original TC4 harvest event.
 *
 * <p>The table is an immutable {@code Map.of}, whose {@code get} rejects a null key with a
 * NullPointerException, and the replacement item is a registry lookup that answers null when the
 * runtime does not know it. Both are on the block break path of every player with the Pickaxe of
 * the Core, the Primal Crusher or the Dowsing focus.</p>
 */
class SpecialMiningSafetyTest {

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void aMissingStackIsNoListedResult() {
        assertNull(SpecialMining.result(null), "a missing stack is no result");
        assertNull(SpecialMining.result(ItemStack.EMPTY), "an empty stack is no result");
    }

    @Test void aRegisteredDropIsAListedResult() {
        var result = SpecialMining.result(new ItemStack(Items.RAW_IRON));
        assertNotNull(result, "raw iron is the original iron cluster drop");
        assertEquals("thaumcraft:cluster_iron", result.output().toString());
    }

    @Test void anUnlistedDropIsNoListedResult() {
        assertNull(SpecialMining.result(new ItemStack(Items.STONE)));
    }

    /**
     * The replacement item is a registry lookup. When the runtime does not know the cluster, the
     * original drop is kept; the stack must never be built from a missing item.
     */
    @Test void anUnknownReplacementKeepsTheOriginalDrop() {
        ItemStack dropped = new ItemStack(Items.RAW_IRON, 3);
        ItemStack result = SpecialMining.find(dropped, 1.0F, RandomSource.create(7L));
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(dropped.getCount(), result.getCount(), "the drop count is preserved");
        assertNotNull(result.getItem(), "a result never carries a missing item");
    }

    @Test void theRollBoundaryIsTheOriginalOne() {
        // Original Utils.findSpecialMiningResult: a roll exactly on the threshold still succeeds.
        assertTrue(SpecialMining.succeeds(0.2F, 1.0F, 1.0F));
        assertFalse(SpecialMining.succeeds(0.20001F, 1.0F, 1.0F));
    }

    @Test void theChanceIsTheOriginalOne() {
        assertEquals(0.2F, SpecialMining.chance(0), 1.0E-6F);
        assertEquals(0.2F + 0.075F, SpecialMining.chance(1), 1.0E-6F);
    }
}
