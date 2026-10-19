package thaumcraft.aspects;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AspectParityTest {
    @Test
    void everyCanonicalAspectMatchesExtractedOriginal() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("data/thaumcraft/tc4/aspects.json")) {
            assertNotNull(stream);
            var rows = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("aspects");
            assertEquals(48, rows.size());
            assertEquals(48, Aspect.aspects.size());
            for (var element : rows) {
                JsonObject row = element.getAsJsonObject();
                Aspect actual = Aspect.getAspect(row.get("tag").getAsString());
                assertNotNull(actual);
                assertSame(actual, Aspect.class.getField(row.get("symbol").getAsString()).get(null));
                assertEquals(row.get("color").getAsInt(), actual.getColor());
                assertEquals(row.get("blend").getAsInt(), actual.getBlend());
                assertEquals(row.get("image").getAsString(), actual.getImage().toString());
                var expected = row.getAsJsonArray("components");
                assertEquals(expected.isEmpty(), actual.isPrimal());
                if (!actual.isPrimal()) {
                    assertEquals(2, actual.getComponents().length);
                    for (int i = 0; i < 2; i++) {
                        assertSame(Aspect.class.getField(expected.get(i).getAsString()).get(null), actual.getComponents()[i]);
                    }
                } else {
                    assertEquals(row.get("chatcolor").getAsString(), actual.getChatcolor());
                }
            }
        }
    }

    @Test
    void sixPrimalTagsAndSymmetricCompoundRecipesAreStable() {
        assertEquals(List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"),
                Aspect.getPrimalAspects().stream().map(Aspect::getTag).toList());
        assertEquals(42, Aspect.getCompoundAspects().size());
        for (Aspect aspect : Aspect.getCompoundAspects()) {
            Aspect[] parents = aspect.getComponents();
            assertSame(aspect, Aspect.getCombinationResult(parents[0], parents[1]));
            assertSame(aspect, Aspect.getCombinationResult(parents[1], parents[0]));
        }
        assertNull(Aspect.getCombinationResult(Aspect.AIR, Aspect.EARTH));
        assertNull(Aspect.getAspect("unknown"));
    }

    @Test
    void addMergeAndZeroRetentionKeepDifferentTc4Meanings() {
        AspectList amounts = new AspectList().add(Aspect.AIR, 3).add(Aspect.AIR, 2).merge(Aspect.AIR, 4);
        assertEquals(5, amounts.getAmount(Aspect.AIR));
        amounts.merge(Aspect.AIR, 7);
        assertEquals(7, amounts.getAmount(Aspect.AIR));
        assertFalse(amounts.reduce(Aspect.AIR, 8));
        assertEquals(7, amounts.visSize());
        assertTrue(amounts.reduce(Aspect.AIR, 7));
        assertEquals(1, amounts.size());
        assertEquals(0, amounts.visSize());
        amounts.remove(Aspect.AIR, 1);
        assertEquals(0, amounts.size());
        assertArrayEquals(new Aspect[0], amounts.getAspects());
    }

    @Test
    void primalReductionAndNbtRoundTripPreserveAmounts() {
        AspectList source = new AspectList().add(Aspect.LIFE, 3).add(Aspect.FIRE, 2);
        AspectList reduced = source.reduceToPrimals(false);
        assertEquals(3, reduced.getAmount(Aspect.WATER));
        assertEquals(3, reduced.getAmount(Aspect.EARTH));
        assertEquals(2, reduced.getAmount(Aspect.FIRE));
        assertTrue(Arrays.stream(reduced.getAspects()).allMatch(Aspect::isPrimal));
        CompoundTag nbt = new CompoundTag();
        source.writeToNBT(nbt);
        AspectList copy = new AspectList().add(Aspect.AIR, 99);
        copy.readFromNBT(nbt);
        assertEquals(source.aspects, copy.aspects);
        copy.remove(Aspect.LIFE);
        assertEquals(3, source.getAmount(Aspect.LIFE));
    }
}
