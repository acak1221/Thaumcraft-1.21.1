package thaumcraft.infusion;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import thaumcraft.common.crafting.InfusionComponentPredicate.Declaration;
import static org.junit.jupiter.api.Assertions.*;

/** No Items/Ingredient/bootstrap initialization: native item checks live in GameTests. */
class InfusionComponentPredicateTest {
    @Test void indexedDamageDeclarationsRoundTripWithoutRegistryBootstrap() {
        for (var declaration : List.of(new Declaration(4, 0), new Declaration(5, 17), new Declaration(4095, Integer.MAX_VALUE))) {
            var json = Declaration.CODEC.encodeStart(JsonOps.INSTANCE, declaration).getOrThrow();
            assertEquals(declaration, Declaration.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            var nbt = Declaration.CODEC.encodeStart(NbtOps.INSTANCE, declaration).getOrThrow();
            assertEquals(declaration, Declaration.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
        }
    }

    @Test void typedDeclarationsRoundTripWithAbsentDamageWithoutRegistryBootstrap() {
        for (var declaration : List.of(new Declaration(0, -1, false, true), new Declaration(1, -1, true, false),
                new Declaration(4095, -1, true, false))) {
            assertEquals(-1, declaration.damage());
            assertTrue(declaration.optionalDamage().isEmpty());
            var json = Declaration.CODEC.encodeStart(JsonOps.INSTANCE, declaration).getOrThrow();
            assertFalse(json.getAsJsonObject().has("damage"));
            assertEquals(declaration, Declaration.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            var nbt = Declaration.CODEC.encodeStart(NbtOps.INSTANCE, declaration).getOrThrow();
            assertEquals(declaration, Declaration.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
        }
        assertEquals(new Declaration(1, -1, true, false), Declaration.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"component\":1,\"water_potion\":true}")).getOrThrow());
        assertEquals(new Declaration(0, -1, false, true), Declaration.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"component\":0,\"empty_jar\":true}")).getOrThrow());
    }

    @Test void legacyDamageDefaultsBothTypedFlagsToFalse() {
        var declaration = Declaration.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"component\":4,\"damage\":0}")).getOrThrow();
        assertEquals(new Declaration(4, 0), declaration);
        assertEquals(0, declaration.damage());
        assertEquals(java.util.Optional.of(0), declaration.optionalDamage());
        assertFalse(declaration.waterPotion());
        assertFalse(declaration.emptyJar());
        assertTrue(Declaration.CODEC.encodeStart(JsonOps.INSTANCE, new Declaration(4, -1)).error().isPresent());
    }

    @Test void scalarCodecRejectsNegativeMissingAndWrongTypedFields() {
        for (String json : List.of("{}", "{\"component\":4}", "{\"damage\":0}",
                "{\"component\":-1,\"damage\":0}", "{\"component\":4096,\"damage\":0}",
                "{\"component\":4,\"damage\":-1}", "{\"component\":\"pick\",\"damage\":0}",
                "{\"component\":4,\"damage\":null}", "[]",
                "{\"component\":1,\"water_potion\":false}", "{\"component\":0,\"empty_jar\":false}",
                "{\"component\":1,\"water_potion\":true,\"empty_jar\":true}",
                "{\"component\":1,\"damage\":-1,\"water_potion\":true}",
                "{\"component\":1,\"damage\":-2,\"water_potion\":true}",
                "{\"component\":4096,\"water_potion\":true}", "{\"water_potion\":true}",
                "{\"component\":1,\"water_potion\":\"yes\"}")) {
            var decoded = assertDoesNotThrow(() -> Declaration.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
            assertTrue(decoded.error().isPresent(), json);
            assertTrue(decoded.result().isEmpty(), json);
        }
    }

    @Test void indicesAreExactUniqueAndBoundedBeforeAllocation() {
        var valid = new ArrayList<>(List.of(new Declaration(5, 0), new Declaration(4, 0)));
        var checked = Declaration.validateIndices(8, valid).getOrThrow();
        valid.clear();
        assertEquals(List.of(new Declaration(5, 0), new Declaration(4, 0)), checked);
        assertThrows(UnsupportedOperationException.class, () -> checked.add(new Declaration(0, 0)));
        assertTrue(Declaration.validateIndices(8, List.of()).result().isPresent());
        assertEquals(List.of(new Declaration(0, -1, false, true), new Declaration(1, -1, true, false)),
                Declaration.validateIndices(4, List.of(new Declaration(0, -1, false, true), new Declaration(1, -1, true, false))).getOrThrow());
        for (int count : new int[] {Integer.MIN_VALUE, -1, 0, 4097, Integer.MAX_VALUE}) {
            assertTrue(assertDoesNotThrow(() -> Declaration.validateIndices(count, List.of())).error().isPresent());
        }
        for (var invalid : List.of(List.of(new Declaration(-1, 0)), List.of(new Declaration(8, 0)),
                List.of(new Declaration(4, -1)), List.of(new Declaration(4, 0), new Declaration(4, 0)),
                List.of(new Declaration(4, 0), new Declaration(4, 1)),
                List.of(new Declaration(4, -2, true, false)), List.of(new Declaration(4, -1, true, true)),
                List.of(new Declaration(4, -1, true, false), new Declaration(4, -1, false, true)),
                List.of(new Declaration(4, 0), new Declaration(4, -1, true, false)))) {
            assertTrue(Declaration.validateIndices(8, invalid).error().isPresent());
        }
        assertTrue(Declaration.validateIndices(1, List.of(new Declaration(0, 0), new Declaration(0, 1))).error().isPresent());
    }
}
