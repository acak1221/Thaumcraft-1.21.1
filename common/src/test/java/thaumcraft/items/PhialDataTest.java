package thaumcraft.items;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.PhialData;
import static org.junit.jupiter.api.Assertions.*;

class PhialDataTest {
    @Test void allKnownAspectsRoundTripAsEightUnitPhials() {
        for (var aspect : thaumcraft.api.aspects.Aspect.aspects.values()) {
            PhialData original = new PhialData(aspect.getTag());
            var nbt = PhialData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
            assertEquals(original, PhialData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
        }
        assertEquals(8, PhialData.AMOUNT);
    }
    @Test void unknownAspectReturnsAnErrorForJsonAndSavedNbt() {
        var jsonResult = assertDoesNotThrow(() -> PhialData.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("unknown")));
        assertTrue(jsonResult.error().isPresent());
        assertTrue(jsonResult.result().isEmpty());
        var nbtResult = assertDoesNotThrow(() -> PhialData.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf("unknown")));
        assertTrue(nbtResult.error().isPresent());
        assertTrue(nbtResult.result().isEmpty());
    }
    @Test void publicConstructorRemainsStrict() {
        assertThrows(IllegalArgumentException.class, () -> new PhialData("unknown"));
        assertThrows(IllegalArgumentException.class, () -> new PhialData(null));
    }
}
