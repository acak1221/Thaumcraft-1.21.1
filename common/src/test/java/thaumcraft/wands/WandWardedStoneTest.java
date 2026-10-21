package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.WandWardedStoneRules;

/**
 * The warded stone blocks of the original wand.
 *
 * <p>Original {@code thaumcraft/common/items/wands/ItemWandCasting.java:390-415}: a wand click takes the
 * arcane pressure plate, the warded glass and the arcane door back and gives them to the player. While
 * the "warded stone" option is on, those three blocks have no mining progress at all, so this is the only
 * way to move them again.</p>
 */
class WandWardedStoneTest {
    @Test void withoutWardedStoneAnybodyMayTakeTheBlockBack() {
        // Original ItemWandCasting:390 - !Config.wardedStone removes the ownership gate.
        assertTrue(WandWardedStoneRules.mayRemove(false, "", "Anyone"));
        assertTrue(WandWardedStoneRules.mayRemove(false, "Owner", "Someone else"));
        assertTrue(WandWardedStoneRules.mayRemove(false, null, "Anyone"));
    }

    @Test void withWardedStoneOnlyTheOwnerMayTakeTheBlockBack() {
        // Original ItemWandCasting:390 and :400 - the owner of the block entity decides.
        assertTrue(WandWardedStoneRules.mayRemove(true, "Owner", "Owner"));
        assertFalse(WandWardedStoneRules.mayRemove(true, "Owner", "Someone else"));
        assertFalse(WandWardedStoneRules.mayRemove(true, "", "Owner"), "a block without an owner belongs to nobody");
        assertFalse(WandWardedStoneRules.mayRemove(true, null, "Owner"));
    }

    @Test void theArcaneDoorOnlyDropsItsItemFromTheLowerHalf() {
        // Original ItemWandCasting:407 - Config.wardedStone || !Config.wardedStone && (md & 8) == 0.
        assertTrue(WandWardedStoneRules.dropsDoorItem(false, false), "the lower half drops the door");
        assertFalse(WandWardedStoneRules.dropsDoorItem(false, true), "the upper half never carried a drop");
        assertTrue(WandWardedStoneRules.dropsDoorItem(true, false));
        assertTrue(WandWardedStoneRules.dropsDoorItem(true, true), "a warded door is never broken for a drop");
    }

    @Test void theTradeLeftClickMixinIsRegistered() {
        // Original ItemWandCasting:583-590 -> ItemFocusTrade:168-181 - the left click swaps the block.
        // Without the registration the native mixin would never run and the focus would lose the path.
        String mixins = readResource("thaumcraft-common.mixins.json");
        assertTrue(mixins.contains("\"TradeFocusServerPlayerGameModeMixin\""), "the trade left click mixin is missing");
    }

    @Test void theCopperCapPaysItsOwnPriceForOrderAndEntropy() {
        // Original ItemWandCasting:249 - the cap modifier of the aspect decides the price:
        // getSpecialCostModifierAspects().contains(aspect) ? getSpecialCostModifier() : getBaseCostModifier().
        WandMaterials.Cap copper = WandMaterials.COPPER;
        assertEquals(1.0F, copper.modifierFor("ordo"), 0F);
        assertEquals(1.0F, copper.modifierFor("perditio"), 0F);
        assertEquals(1.1F, copper.modifierFor("aer"), 0F);
        // One hundred whole vis of order cost 10000 centivis, of air 11000 centivis.
        assertEquals(10000, (int) (100 * 100 * WandVisDiscount.modifier(copper.modifierFor("ordo"), 0)));
        assertEquals(11000, (int) (100 * 100 * WandVisDiscount.modifier(copper.modifierFor("aer"), 0)));
        // The frugal focus discount is a separate float subtraction of a tenth per level.
        assertEquals(9000, (int) (100 * 100 * WandVisDiscount.focusModifier(copper.modifierFor("ordo"), 0, 1)));
    }

    private static String readResource(String name) {
        try (InputStream stream = WandWardedStoneTest.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new AssertionError("missing resource " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new AssertionError(failure);
        }
    }
}
