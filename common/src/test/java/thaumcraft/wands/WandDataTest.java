package thaumcraft.wands;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WandDataTest {
    @Test void nativeComponentRoundTripPreservesCentivisAndParts() {
        WandData original = new WandData("wood", "iron", Map.of("aer", 2450, "ignis", 100));
        var encoded = WandData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(original, WandData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        assertEquals(2500, original.capacity());
    }
    @Test void immutableCopyAndClampingCannotChangePreviousStackState() {
        Map<String, Integer> mutable = new LinkedHashMap<>(Map.of("terra", 100));
        WandData original = new WandData("wood", "iron", mutable);
        mutable.put("terra", 2000);
        assertEquals(100, original.amount("terra"));
        assertThrows(UnsupportedOperationException.class, () -> original.vis().put("terra", 2000));
        assertEquals(2500, original.withAmount("terra", 9999).amount("terra"));
        assertEquals(0, original.withAmount("terra", -100).amount("terra"));
        assertEquals(100, original.amount("terra"));
    }
    @Test void unsupportedComponentsFailRatherThanCreatingFakeGameplay() {
        assertThrows(IllegalArgumentException.class, () -> new WandData("wood", "iron", Map.of("vitium", 100)));
        assertTrue(WandData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"vis\":{\"aer\":9999}}")).error().isPresent());
    }
    @Test void malformedJsonReturnsDecodeErrorsWithoutThrowing() {
        for (String json : List.of(
                "{\"rod\":\"silverwood_staff\"}",
                "{\"rod\":\"greatwood_staff\"}",
                "{\"cap\":\"void\"}",
                "{\"cap\":\"thaumium_inert\"}",
                "{\"vis\":{\"vitium\":100}}",
                "{\"vis\":{\"unknown\":100}}",
                "{\"vis\":{\"aer\":-1}}",
                "{\"vis\":{\"aer\":2501}}",
                "{\"rod\":\"wood\",\"cap\":\"gold\",\"vis\":{\"aer\":5000}}",
                "{\"rod\":\"greatwood\",\"vis\":{\"aer\":5001}}",
                "{\"rod\":\"silverwood\",\"vis\":{\"aer\":10001}}")) {
            var result = assertDoesNotThrow(() -> WandData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
            assertTrue(result.error().isPresent(), json);
            assertTrue(result.result().isEmpty(), json);
        }
    }
    @Test void malformedSavedNbtDoesNotEscapeTheCodecBoundary() {
        CompoundTag malformed = new CompoundTag();
        malformed.putString("rod", "unknown");
        var result = assertDoesNotThrow(() -> WandData.CODEC.parse(NbtOps.INSTANCE, malformed));
        assertTrue(result.error().isPresent());
        assertTrue(result.result().isEmpty());
        CompoundTag unknownVis = new CompoundTag();
        CompoundTag vis = new CompoundTag(); vis.putInt("vitium", 100);
        unknownVis.put("vis", vis);
        var visResult = assertDoesNotThrow(() -> WandData.CODEC.parse(NbtOps.INSTANCE, unknownVis));
        assertTrue(visResult.error().isPresent());
        assertTrue(visResult.result().isEmpty());
    }
    @Test void codecValidationDoesNotWeakenDirectConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new WandData("unknown", "iron", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new WandData("wood", "void", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new WandData("wood", "gold", Map.of("aer", 2501)));
        assertThrows(IllegalArgumentException.class, () -> new WandData("greatwood", "gold", Map.of("aer", 5001)));
        assertThrows(IllegalArgumentException.class, () -> new WandData("silverwood", "gold", Map.of("aer", 10001)));
    }
    @Test void allNativeMaterialPairsRoundTripJsonNbtAndNetworkAtTheirRealCapacity() {
        for (var rod : WandMaterials.RODS) for (var cap : WandMaterials.CAPS) {
            WandData original = new WandData(rod.tag(), cap.tag(), Map.of("aer", rod.capacity(), "ordo", 1234));
            var json = WandData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
            assertEquals(original, WandData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            var nbt = WandData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
            assertEquals(original, WandData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
            var buffer = Unpooled.buffer();
            try {
                WandData.STREAM_CODEC.encode(buffer, original);
                assertEquals(original, WandData.STREAM_CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void originalDefaultsAndGreatwoodClampingStayIndependent() {
        assertEquals(WandData.EMPTY, WandData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
        WandData greatwood = new WandData("greatwood", "gold", Map.of("aer", 2600));
        assertEquals(5000, greatwood.capacity());
        assertEquals(5000, greatwood.withAmount("aer", Integer.MAX_VALUE).amount("aer"));
        assertEquals(2600, greatwood.amount("aer"));
        assertEquals(2500, WandData.EMPTY.capacity());
    }
    @Test void originalMaterialCostsUseProductForAssemblyButNotAspectBonuses() {
        assertEquals(1, WandMaterials.rodCraftCost("wood"));
        assertEquals(3, WandMaterials.rodCraftCost("greatwood"));
        assertEquals(9, WandMaterials.rodCraftCost("silverwood"));
        assertEquals(1, WandMaterials.capCraftCost("iron"));
        assertEquals(3, WandMaterials.capCraftCost("gold"));
        assertEquals(6, WandMaterials.capCraftCost("thaumium"));
        assertEquals(1.1F, WandMaterials.capModifier("iron"));
        assertEquals(1.0F, WandMaterials.capModifier("gold"));
        assertEquals(.9F, WandMaterials.capModifier("thaumium"));
        assertEquals(9, WandMaterials.assemblyCost("greatwood", "gold"));
        assertEquals(27, WandMaterials.assemblyCost("silverwood", "gold"));
        assertEquals(54, WandMaterials.assemblyCost("silverwood", "thaumium"));
        assertEquals(18, WandMaterials.assemblyCost("greatwood", "thaumium"));
        assertEquals(6, WandMaterials.assemblyCost("wood", "thaumium"));
        assertEquals(9, WandMaterials.assemblyCost("silverwood", "iron"));
        assertEquals(3, WandMaterials.assemblyCost("greatwood", "iron"));
        assertEquals(3, WandMaterials.assemblyCost("wood", "gold"));
        assertEquals("ROD_wood", WandMaterials.WOOD.research());
        assertEquals("CAP_iron", WandMaterials.IRON.research());
    }
}
