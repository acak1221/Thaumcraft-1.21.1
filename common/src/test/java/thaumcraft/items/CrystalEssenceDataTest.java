package thaumcraft.items;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.CrystalEssenceData;
import static org.junit.jupiter.api.Assertions.*;

class CrystalEssenceDataTest {
    @Test void orderedWholeListsAndExplicitEmptyRoundTripWithoutAliases() {
        var amounts = new LinkedHashMap<String, Integer>(); amounts.put("ignis", 7); amounts.put("aer", 3);
        var original = new CrystalEssenceData(amounts); amounts.clear();
        for (var data : List.of(original, CrystalEssenceData.EMPTY)) {
            var encoded = CrystalEssenceData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            assertEquals(data, CrystalEssenceData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
            var wire = Unpooled.buffer();
            try { CrystalEssenceData.STREAM_CODEC.encode(wire, data); assertEquals(data, CrystalEssenceData.STREAM_CODEC.decode(wire)); assertFalse(wire.isReadable()); }
            finally { wire.release(); }
        }
        assertEquals("ignis", original.firstTag());
        assertEquals(List.of("ignis", "aer"), original.amounts().keySet().stream().toList());
        assertThrows(UnsupportedOperationException.class, () -> original.amounts().clear());
        var copy = original.toAspects(); copy.aspects.clear();
        assertEquals(7, original.amount("ignis"));
        assertNotEquals(original, CrystalEssenceData.fromAspects(new AspectList().add(Aspect.AIR, 3).add(Aspect.FIRE, 7)));
        assertTrue(CrystalEssenceData.EMPTY.isEmpty()); assertNull(CrystalEssenceData.EMPTY.firstTag());
    }

    @Test void invalidPayloadHasNoUsablePartialSubset() {
        for (String invalid : List.of(
                "[{\"tag\":\"aer\",\"amount\":1},{\"tag\":\"unknown\",\"amount\":1}]",
                "[{\"tag\":\"aer\",\"amount\":1},{\"tag\":\"aer\",\"amount\":2}]",
                "[{\"tag\":\"aer\",\"amount\":1},{\"tag\":\"ignis\",\"amount\":0}]",
                "[{\"tag\":\"aer\",\"amount\":-1}]", "[{\"tag\":\"aer\",\"amount\":1.5}]",
                "[{\"tag\":\"aer\",\"amount\":1.000000000000000001}]",
                "[{\"tag\":\"aer\",\"amount\":2147483649}]",
                "[{\"tag\":\"aer\",\"amount\":2147483647},{\"tag\":\"ignis\",\"amount\":1}]",
                "[{\"tag\":\"aer\"}]")) {
            var result = CrystalEssenceData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(invalid));
            assertTrue(result.error().isPresent(), invalid);
            assertTrue(result.resultOrPartial(ignored -> {}).isEmpty(), "NO spendable partial: " + invalid);
        }
    }

    @Test void wholeRegistryAndNativeIntBoundDoNotImposeASingletonOrBuiltinWhitelist() {
        var list = new AspectList(); Aspect.aspects.values().forEach(type -> list.add(type, 1));
        var all = CrystalEssenceData.fromAspects(list);
        assertEquals(Aspect.aspects.size(), all.amounts().size());
        assertEquals(all, CrystalEssenceData.CODEC.parse(NbtOps.INSTANCE, CrystalEssenceData.CODEC.encodeStart(NbtOps.INSTANCE, all).getOrThrow()).getOrThrow());
        assertEquals(Integer.MAX_VALUE, CrystalEssenceData.fromAspects(new AspectList().add(Aspect.AIR, Integer.MAX_VALUE)).amount("aer"));
        String tag = "crystal_codec_test_addon";
        var addon = new Aspect(tag, 0x123456, new Aspect[] {Aspect.AIR, Aspect.FIRE});
        try {
            var valid = CrystalEssenceData.fromAspects(new AspectList().add(addon, 37));
            assertEquals(valid, CrystalEssenceData.CODEC.parse(JsonOps.INSTANCE, CrystalEssenceData.CODEC.encodeStart(JsonOps.INSTANCE, valid).getOrThrow()).getOrThrow());
            Aspect.aspects.put("crystal_codec_alias", addon);
            try { assertThrows(IllegalArgumentException.class, () -> new CrystalEssenceData(java.util.Map.of("crystal_codec_alias", 1))); }
            finally { Aspect.aspects.remove("crystal_codec_alias", addon); }
        } finally { Aspect.aspects.remove(tag, addon); }
    }

    @Test void networkRejectsWholeUnknownDuplicateNegativeAndOversizedLists() {
        for (String branch : List.of("unknown", "duplicate", "negative", "size", "total")) {
            var wire = Unpooled.buffer();
            try {
                int size = branch.equals("size") ? Aspect.aspects.size() + 1 : branch.equals("duplicate") || branch.equals("total") ? 2 : 1;
                ByteBufCodecs.VAR_INT.encode(wire, size);
                if (!branch.equals("size")) {
                    ByteBufCodecs.STRING_UTF8.encode(wire, branch.equals("unknown") ? "unknown" : "aer");
                    ByteBufCodecs.VAR_INT.encode(wire, branch.equals("negative") ? -1 : branch.equals("total") ? Integer.MAX_VALUE : 1);
                    if (size == 2) { ByteBufCodecs.STRING_UTF8.encode(wire, branch.equals("total") ? "ignis" : "aer"); ByteBufCodecs.VAR_INT.encode(wire, 1); }
                }
                assertThrows(DecoderException.class, () -> CrystalEssenceData.STREAM_CODEC.decode(wire), branch);
            } finally { wire.release(); }
        }
    }

    @Test void invalidMutableLegacyListsAreNotSilentlyCulled() {
        assertThrows(IllegalArgumentException.class, () -> CrystalEssenceData.fromAspects(null));
        var list = new AspectList().add(Aspect.AIR, 1); list.aspects.put(null, 3);
        assertThrows(IllegalArgumentException.class, () -> CrystalEssenceData.fromAspects(list));
        list.aspects.remove(null); list.aspects.put(Aspect.FIRE, 0);
        assertThrows(IllegalArgumentException.class, () -> CrystalEssenceData.fromAspects(list));
        list.aspects.put(Aspect.FIRE, null);
        assertThrows(IllegalArgumentException.class, () -> CrystalEssenceData.fromAspects(list));
    }
}
