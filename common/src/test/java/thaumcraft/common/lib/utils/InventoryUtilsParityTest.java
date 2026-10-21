package thaumcraft.common.lib.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The inventory helpers of the original TC4 4.2.3.5 {@code thaumcraft.common.lib.utils.InventoryUtils}.
 *
 * <p>Every expectation is the value of the original oracle file
 * {@code OpenTC4/src/main/java/thaumcraft/common/lib/utils/InventoryUtils.java}.</p>
 *
 * <p>These tests are AUTHORED BUT NOT EXECUTED: the wave that produced them may not run Gradle
 * (the shared Loom cache is owned by the leader).</p>
 */
class InventoryUtilsParityTest {

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ------------------------------------------------------- the wand of the hotbar

    /**
     * Original {@code ItemWandCasting.addVis(is, aspect, amount, doit)} (oracle
     * {@code ItemWandCasting.java:331-343}): {@code storeAmount = getVis(is, aspect) + amount * 100},
     * {@code leftover = max(storeAmount - getMaxVis(is), 0)} and the return value is
     * {@code leftover / 100}, that is the leftover in WHOLE vis.
     */
    @Test void theWandLeftoverIsTheOriginalWholeVisLeftover() {
        // A wooden wand holds 25 vis (2500 centivis). An empty wand asked for 25 vis keeps nothing.
        assertEquals(0, InventoryUtils.visLeftover(0, 2500, 25));
        // A full wand asked for 25 vis keeps all 25.
        assertEquals(25, InventoryUtils.visLeftover(2500, 2500, 25));
        // A wand that holds 24 vis of the 25 asked for keeps one vis: 24 < 25, so the original
        // still accepts this wand (its truncation to whole vis is the original's own quirk).
        assertEquals(24, InventoryUtils.visLeftover(2400, 2500, 25));
        // The truncation of the original: a wand of half a vis asked for one vis keeps 0.5 vis,
        // which the original's integer division reports as 0, so the original accepts the wand.
        assertEquals(0, InventoryUtils.visLeftover(0, 50, 1));
    }

    /** The leftover must not wrap around for a huge request. */
    @Test void theWandLeftoverDoesNotOverflow() {
        int leftover = InventoryUtils.visLeftover(0, 2500, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE - 25, leftover);
        assertTrue(leftover > 0);
    }

    // ------------------------------------------------------------- the stack equality

    /**
     * Original {@code InventoryUtils.areItemStacksEqual} (oracle
     * {@code InventoryUtils.java:333-339}, the same rule in
     * {@code areItemStacksEqualForCrafting} at oracle {@code InventoryUtils.java:303-309}):
     * the damage of 32767 is the original's "any metadata" wildcard, so two stacks whose damage
     * values differ only by that wildcard are equal when the caller ignores the damage.
     */
    @Test void theDamageWildcardOfTheOriginalIsKept() {
        // A stone is not damageable, so only the wildcard rule can equalise the two stacks.
        assertTrue(InventoryUtils.damageMatches(0, 32767, true, false, false),
                "the 32767 wildcard is the original's any-metadata value");
        assertTrue(InventoryUtils.damageMatches(32767, 0, true, false, false));
        assertFalse(InventoryUtils.damageMatches(0, 5, true, false, false),
                "a real damage difference is still a difference");
        assertFalse(InventoryUtils.damageMatches(0, 32767, false, false, false),
                "the wildcard only applies when the caller ignores the damage");
        // Two damageable stacks ignore their damage difference without any wildcard.
        assertTrue(InventoryUtils.damageMatches(0, 5, true, true, true));
        // The wildcard does not rescue a damageable pair that the ignore rule already equalised.
        assertTrue(InventoryUtils.damageMatches(0, 32767, true, true, true));
        // The wildcard rule of the original does NOT ask for two damageable stacks: oracle
        // InventoryUtils.java:337-339 reads
        //   if (t2 && ignoreDamage && (stack0.getItemDamage() == 32767 || stack1.getItemDamage() == 32767)) t2 = false;
        // so the wildcard equalises the pair as soon as the caller ignores the damage and one of
        // the two damage values is the wildcard, even when only one of the two stacks is
        // damageable. The port keeps that rule in InventoryUtils.damageMatches:407.
        assertTrue(InventoryUtils.damageMatches(0, 32767, true, true, false),
                "the wildcard equalises the pair even when only one stack is damageable");
        assertTrue(InventoryUtils.damageMatches(0, 32767, true, false, false));
    }

    /**
     * Original {@code InventoryUtils.areItemStacksEqualForCrafting} (oracle
     * {@code InventoryUtils.java:285-311}): the answer also needs
     * {@code stack0.stackSize <= stack0.getMaxStackSize()}.
     */
    @Test void theCraftingComparisonRefusesAnOverfullSlot() {
        assertFalse(InventoryUtils.withinStackLimit(65, 64), "65 stones are above the stack limit of 64");
        assertTrue(InventoryUtils.withinStackLimit(64, 64));
        assertTrue(InventoryUtils.withinStackLimit(1, 64));
    }

    /**
     * Original {@code ThaumcraftApiHelper.areItemStackTagsEqualForCrafting} (oracle
     * {@code thaumcraft/api/ThaumcraftApiHelper.java:128-146}, called from oracle
     * {@code InventoryUtils.java:301}): a recipe tag that the slot does not carry is a mismatch,
     * but a slot with more tags than the recipe still matches.
     */
    @Test void theCraftingTagRuleIsTheOriginalOne() {
        ItemStack plain = new ItemStack(Items.STONE);
        ItemStack tagged = new ItemStack(Items.STONE);
        CompoundTag tag = new CompoundTag();
        tag.putString("Mark", "one");
        tagged.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        assertTrue(InventoryUtils.areItemStackTagsEqualForCrafting(plain, plain));
        assertTrue(InventoryUtils.areItemStackTagsEqualForCrafting(tagged, plain),
                "a tagged slot still matches a recipe without tags");
        assertFalse(InventoryUtils.areItemStackTagsEqualForCrafting(plain, tagged),
                "a recipe tag that the slot does not carry is a mismatch");
    }
}
