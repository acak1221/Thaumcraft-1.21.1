package thaumcraft.wands;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import static org.junit.jupiter.api.Assertions.*;

class FocusUpgradeDataTest {
    @Test void canonicalClosedCatalogueHasDefensiveAspectsAndSafeLookup() {
        var types = List.of(FocusUpgradeType.potency, FocusUpgradeType.frugal, FocusUpgradeType.treasure, FocusUpgradeType.enlarge,
                FocusUpgradeType.architect, FocusUpgradeType.extend, FocusUpgradeType.silktouch, FocusUpgradeType.dowsing,
                FocusUpgradeType.alchemistsfire, FocusUpgradeType.alchemistsfrost, FocusUpgradeType.fireball, FocusUpgradeType.firebeam,
                FocusUpgradeType.scattershot, FocusUpgradeType.iceboulder, FocusUpgradeType.chainlightning, FocusUpgradeType.earthshock,
                FocusUpgradeType.batbombs, FocusUpgradeType.devilbats, FocusUpgradeType.vampirebats, FocusUpgradeType.nightshade, FocusUpgradeType.seeker);
        var aspects = List.of(Aspect.WEAPON, Aspect.HUNGER, Aspect.GREED, Aspect.TRAVEL, Aspect.CRAFT, Aspect.EXCHANGE, Aspect.GREED, Aspect.MINE,
                Aspect.ENERGY, Aspect.COLD, Aspect.DARKNESS, Aspect.ENERGY, Aspect.COLD, Aspect.COLD, Aspect.WEATHER, Aspect.WEATHER,
                Aspect.ENERGY, Aspect.ARMOR, Aspect.HUNGER, Aspect.LIFE, Aspect.SENSES);
        int[] ids = {0, 1, 2, 3, 6, 7, 8, 20, 4, 5, 9, 10, 11, 12, 17, 18, 13, 14, 19, 15, 16};
        for (int index = 0; index < ids.length; index++) {
            int id = ids[index];
            var type = types.get(index);
            assertEquals(id, type.id); assertSame(type, FocusUpgradeType.get(id));
            assertEquals(1, type.getAspects().getAmount(aspects.get(index)));
            type.getAspects().aspects.clear();
            assertEquals(1, type.getAspects().getAmount(aspects.get(index)));
            assertTrue(type.icon.getPath().startsWith("textures/foci/"));
        }
        assertEquals(1, FocusUpgradeType.alchemistsfire.getAspects().getAmount(Aspect.SLIME));
        assertEquals(1, FocusUpgradeType.alchemistsfrost.getAspects().getAmount(Aspect.TRAP));
        assertEquals(1, FocusUpgradeType.firebeam.getAspects().getAmount(Aspect.AIR));
        assertEquals(1, FocusUpgradeType.scattershot.getAspects().getAmount(Aspect.WEAPON));
        assertEquals(1, FocusUpgradeType.iceboulder.getAspects().getAmount(Aspect.CRYSTAL));
        assertEquals(1, FocusUpgradeType.batbombs.getAspects().getAmount(Aspect.TRAP));
        assertEquals(1, FocusUpgradeType.vampirebats.getAspects().getAmount(Aspect.LIFE));
        assertEquals(1, FocusUpgradeType.nightshade.getAspects().getAmount(Aspect.POISON));
        assertEquals(1, FocusUpgradeType.nightshade.getAspects().getAmount(Aspect.MAGIC));
        assertEquals(1, FocusUpgradeType.seeker.getAspects().getAmount(Aspect.MIND));
        for (int id : new int[]{Integer.MIN_VALUE, -1, 21, 65536, Integer.MAX_VALUE}) assertNull(FocusUpgradeType.get(id));
    }

    @Test void orderedHolesAndOccurrenceLevelsNeverAlias() {
        var input = new ArrayList<>(List.of((short)1, (short)-1, (short)0, (short)-1, (short)1));
        var data = new FocusUpgradeData(input); input.set(0, (short)0);
        assertArrayEquals(new short[]{1, -1, 0, -1, 1}, data.applied());
        var copy = data.applied(); copy[0] = 0;
        assertEquals(2, data.level(FocusUpgradeType.frugal));
        assertEquals(1, data.level(FocusUpgradeType.potency));
        assertThrows(UnsupportedOperationException.class, () -> data.ranks().set(0, (short)0));
        var upgraded = data.withUpgrade(FocusUpgradeType.enlarge, 4);
        assertEquals((short)-1, data.ranks().get(3)); assertEquals(1, upgraded.level(FocusUpgradeType.enlarge));
        assertThrows(IllegalArgumentException.class, () -> data.withUpgrade(FocusUpgradeType.potency, 1));
        for (int rank : new int[]{Integer.MIN_VALUE, -1, 0, 6, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> data.withUpgrade(FocusUpgradeType.potency, rank));
    }

    @Test void fixedSlotsRoundTripJsonNbtAndExactlyTenWireBytes() {
        for (var data : List.of(FocusUpgradeData.EMPTY, new FocusUpgradeData(List.of((short)0, (short)16, (short)0, (short)-1, (short)16)), new FocusUpgradeData(List.of((short)2, (short)3, (short)0, (short)3, (short)1)),
                new FocusUpgradeData(List.of((short)1, (short)7, (short)6, (short)3, (short)8)),
                new FocusUpgradeData(List.of((short)2, (short)-1, (short)20, (short)-1, (short)8)),
                new FocusUpgradeData(List.of((short)0, (short)0, (short)13, (short)1, (short)19)))) {
            var json = FocusUpgradeData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
            assertEquals(data, FocusUpgradeData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            var nbt = FocusUpgradeData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            assertEquals(data, FocusUpgradeData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
            var buffer = Unpooled.buffer();
            try {
                FocusUpgradeData.STREAM_CODEC.encode(buffer, data);
                assertEquals(10, buffer.readableBytes());
                assertEquals(data, FocusUpgradeData.STREAM_CODEC.decode(buffer)); assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }

    @Test void malformedListsAndUnimplementedIdsReturnDecodeErrors() {
        for (String json : List.of("[]", "[-1]", "[-1,-1,-1,-1,-1,-1]", "[0,1,2,3,21]",
                "[0,1,2,3,-2]", "[0,1,2,3,65536]", "[0,1,2,3,null]", "{}")) {
            var result = assertDoesNotThrow(() -> FocusUpgradeData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
            assertTrue(result.error().isPresent(), json); assertTrue(result.result().isEmpty(), json);
        }
        assertThrows(IllegalArgumentException.class, () -> new FocusUpgradeData(List.of((short)0)));
        assertThrows(IllegalArgumentException.class, () -> new FocusUpgradeData(List.of((short)0, (short)1, (short)2, (short)3, (short)21)));
        var buffer = Unpooled.buffer();
        try {
            for (int id : new int[]{0, 1, 2, 3, 21}) buffer.writeShort(id);
            assertThrows(DecoderException.class, () -> FocusUpgradeData.STREAM_CODEC.decode(buffer));
            buffer.clear().writeShort(0);
            assertThrows(IndexOutOfBoundsException.class, () -> FocusUpgradeData.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
