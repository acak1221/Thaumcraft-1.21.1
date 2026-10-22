package thaumcraft.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
     *
     * <p>The 1.21.1 item registry is defaulted, so the lookup of an unknown id answers the air item
     * and never {@code null}; the drop may not be replaced by that empty stack.
     */
    @Test void anUnknownReplacementKeepsTheOriginalDrop() {
        // The unit test JVM knows the vanilla items only, so the cluster of the table is missing
        // there and the lookup of the port falls back to the air item.
        assertFalse(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "cluster_iron")),
                "the iron cluster is not registered in the unit test JVM");
        ItemStack dropped = new ItemStack(Items.RAW_IRON, 3);
        ItemStack result = SpecialMining.find(dropped, 1.0F, RandomSource.create(7L));
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(Items.RAW_IRON, result.getItem(), "the original drop is kept");
        assertEquals(dropped.getCount(), result.getCount(), "the drop count is preserved");
        assertNotNull(result.getItem(), "a result never carries a missing item");
    }

    @Test void theRollBoundaryIsTheOriginalOne() {
        // Original Utils.findSpecialMiningResult (oracle Utils:178): the roll that lands exactly on
        // the threshold still succeeds. The threshold is the chance of the tool times the factor of
        // the entry, and the chance of the original is 0.2 + 0.075 per fortune level
        // (oracle EventHandlerWorld:182).
        assertTrue(SpecialMining.succeeds(0.2F, SpecialMining.BASE_CHANCE, 1.0F));
        assertFalse(SpecialMining.succeeds(0.20001F, SpecialMining.BASE_CHANCE, 1.0F));
        // The gold and the cinnabar cluster carry the factor 0.9, so their threshold is 0.18.
        assertTrue(SpecialMining.succeeds(0.17F, SpecialMining.BASE_CHANCE, SpecialMining.GOLD));
        assertFalse(SpecialMining.succeeds(0.19F, SpecialMining.BASE_CHANCE, SpecialMining.GOLD));
    }

    @Test void theChanceIsTheOriginalOne() {
        // Oracle EventHandlerWorld:182: float chance = 0.2f + (float)fortune * 0.075f, in float.
        assertEquals(0.2F, SpecialMining.chance(0));
        assertEquals(0.275F, SpecialMining.chance(1));
    }
}
