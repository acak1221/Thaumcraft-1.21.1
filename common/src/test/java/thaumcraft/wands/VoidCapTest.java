package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;

/** The void metal wand cap (original ConfigItems:297: {@code new WandCap("void", 0.8f, ..., 9)}). */
class VoidCapTest {
    @Test void theVoidCapIsTheDearestAndTheCheapestToUse() {
        var cap = WandMaterials.cap("void");
        assertEquals(.8F, cap.modifier(), 0F, "a fifth off every vis the wand pays");
        assertEquals(9, cap.craftCost());
        assertEquals("CAP_void", cap.research());
        assertEquals(List.of("iron", "gold", "thaumium", "void"), WandMaterials.CAPS.stream().map(WandMaterials.Cap::tag).toList(),
                "the earlier caps keep their places, the void cap follows them");
        assertTrue(WandMaterials.supportsCap("void"));
        assertEquals(.8F, WandMaterials.capModifier("void"), 0F);
        for (var other : List.of("iron", "gold", "thaumium")) assertTrue(cap.modifier() < WandMaterials.cap(other).modifier(), other);
        assertThrows(IllegalArgumentException.class, () -> WandMaterials.cap("copper"), "the other-mod caps stay unsupported");
    }

    @Test void aWandIsAssembledAtRodTimesNinePerPrimal() {
        assertEquals(1 * 9, WandMaterials.assemblyCost("wood", "void"));
        assertEquals(3 * 9, WandMaterials.assemblyCost("greatwood", "void"));
        assertEquals(9 * 9, WandMaterials.assemblyCost("silverwood", "void"));
        assertEquals(6 * 9, WandMaterials.assemblyCost("obsidian", "void"));
        assertEquals(24 * 9, WandMaterials.assemblyCost("silverwood_staff", "void"));
        assertEquals(32 * 9, WandMaterials.assemblyCost("primal_staff", "void"), "the dearest wand in the game");
        int dearest = 0;
        for (var rod : WandMaterials.RODS) for (var cap : WandMaterials.CAPS) dearest = Math.max(dearest, rod.craftCost() * cap.craftCost());
        assertEquals(288, dearest, "and the craft-cost component (0..288) holds every price");
    }

    @Test void aVoidCappedWandSurvivesTheCodecs() {
        WandData wand = new WandData("silverwood", "void", Map.of("aer", 10000, "ordo", 5));
        var json = WandData.CODEC.encodeStart(JsonOps.INSTANCE, wand).getOrThrow();
        assertEquals(wand, WandData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json.toString())).getOrThrow());
        assertEquals("void", wand.cap());
        assertTrue(WandData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"cap\":\"void\"}")).result().isPresent());
    }
}
