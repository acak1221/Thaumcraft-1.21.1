package thaumcraft.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.armor.VoidRobeLayers;

/**
 * The worn layers of the void thaumaturge robe.
 *
 * <p>Oracle {@code thaumcraft/common/items/armor/ItemVoidRobeArmor.java:98-100}: the worn look of the
 * void thaumaturge is two sheets, the {@code _overlay} sheet that carries the dye and the plain
 * sheet that carries the metal trim. The official Mojang 1.7.10 {@code client.jar} settles the
 * order: {@code RenderPlayer} draws the dyed sheet first and the untinted trim after it.</p>
 *
 * <p>No test constructs a {@code Block}: the rule under test is a pure static list.</p>
 */
class VoidRobeLayersTest {
    @Test void theVoidRobeIsDrawnInTwoLayers() {
        assertEquals(2, VoidRobeLayers.WORN_LAYERS.size(),
                "the original void thaumaturge is drawn in two layers");
        assertEquals(VoidRobeLayers.Layer.DYED_CLOTH, VoidRobeLayers.WORN_LAYERS.get(0),
                "the dyed cloth of the original is the first layer");
        assertEquals(VoidRobeLayers.Layer.UNTINTED_TRIM, VoidRobeLayers.WORN_LAYERS.get(1),
                "the metal trim of the original is the second layer");
    }

    @Test void theSheetsAreTheOriginalOnes() {
        assertEquals("void_robe_armor_overlay.png", VoidRobeLayers.layer(0).sheet(),
                "the first layer of the original is the overlay sheet");
        assertEquals("void_robe_armor.png", VoidRobeLayers.layer(1).sheet(),
                "the second layer of the original is the plain sheet");
    }

    @Test void onlyTheClothCarriesTheDye() {
        assertTrue(VoidRobeLayers.layerIsDyed(0), "the cloth of the original is dyed");
        assertFalse(VoidRobeLayers.layerIsDyed(1), "the trim of the original stays untinted");
        assertFalse(VoidRobeLayers.layerIsDyed(-1), "there is no layer before the first one");
        assertFalse(VoidRobeLayers.layerIsDyed(2), "there is no layer after the second one");
        assertNull(VoidRobeLayers.layer(2), "there is no third layer");
    }

    @Test void theDefaultDyeIsTheOriginalOne() {
        // The original ItemVoidRobeArmor.func_82814_b returns 6961280 for a
        // stack without a colour, which is 0x6A3880.
        assertEquals(6961280, VoidRobeLayers.DEFAULT_DYE, "the default dye of the original is 6961280");
    }
}
