package thaumcraft.golems;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemUseOptions;
import static org.junit.jupiter.api.Assertions.*;

/** Pure configuration checks, not native actor/world-use verification. */
class GolemUseOptionsTest {
    @Test void allOrdinaryFlagCombinationsRoundTripWithoutItemBootstrap() {
        for (int bits = 0; bits < 8; bits++) {
            var options = new GolemUseOptions((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0);
            var json = GolemUseOptions.CODEC.encodeStart(JsonOps.INSTANCE, options).getOrThrow();
            var nbt = GolemUseOptions.CODEC.encodeStart(NbtOps.INSTANCE, options).getOrThrow();
            assertEquals(options, GolemUseOptions.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            assertEquals(options, GolemUseOptions.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
            assertEquals(bits == 0, options.supportsDefaultBaseline());
        }
    }

    @Test void absenceAloneDefaultsToBlockRightNotSneaking() {
        assertEquals(GolemUseOptions.DEFAULT, GolemUseOptions.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{}")).getOrThrow());
        assertEquals(new GolemUseOptions(true, false, false), GolemUseOptions.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"empty_space\":true}")).getOrThrow());
    }

    @Test void malformedOrUnknownJsonNeverBecomesDefaultOrEntityMode() {
        for (String json : List.of("null", "[]", "true", "{\"entity\":true}",
                "{\"left_click\":null}", "{\"sneaking\":\"false\"}",
                "{\"empty_space\":0}", "{\"empty_space\":1}", "{\"empty_space\":0.0}")) {
            var decoded = GolemUseOptions.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
            assertTrue(decoded.error().isPresent(), json);
            assertTrue(decoded.result().isEmpty(), json);
        }
    }

    @Test void nativeNbtRequiresActualBooleanBytes() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("left_click", true);
        assertTrue(GolemUseOptions.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().leftClick());
        tag.put("left_click", IntTag.valueOf(1));
        assertTrue(GolemUseOptions.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
        tag.put("left_click", ByteTag.valueOf((byte) 2));
        assertTrue(GolemUseOptions.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
    }
}
