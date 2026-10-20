package thaumcraft.loot;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.loot.LootPotionVariants;

class LootPotionVariantsTest {
    @Test void originalLoopRetainsSeventyTwoDistinctMetadataRowsAndWeightOneHundredFortyFour() {
        var rows = LootPotionVariants.all();
        assertEquals(72, rows.size()); assertEquals(144, rows.stream().mapToInt(LootPotionVariants.Variant::weight).sum());
        assertEquals(72, new HashSet<>(rows.stream().map(LootPotionVariants.Variant::metadata).toList()).size());
        assertEquals(36, rows.stream().filter(LootPotionVariants.Variant::splash).count());
        assertEquals(12, rows.stream().map(LootPotionVariants.Variant::effectId).distinct().count());
    }
    @Test void untouchedVanillaOracleVectorsPreserveOldDurationAmplifierAndColor() {
        vector(8193,900,0,13458603); vector(8225,450,1,13458603); vector(8257,2400,0,13458603);
        vector(16385,676,0,13458603); vector(16417,338,1,13458603); vector(16449,1801,0,13458603);
        vector(8194,3600,0,8171462); vector(16450,7201,0,8171462);
        vector(8227,3600,0,14981690); vector(8259,9600,0,14981690);
        vector(16389,1,0,16262179); vector(16421,1,1,16262179); vector(16453,1,0,16262179);
        vector(8200,1800,0,4738376); vector(16456,3601,0,4738376);
        vector(8204,1,0,4393481); vector(16428,1,1,4393481);
    }
    @Test void strongNonAmplifiableAndExtendedInstantVariantsRemainSeparateWeightedEntries() {
        var normal = row(8195); var strong = row(8227);
        assertEquals(normal.duration(),strong.duration()); assertEquals(normal.amplifier(),strong.amplifier());
        assertEquals(1,normal.weight()); assertEquals(2,strong.weight());
        assertEquals(1,row(8261).duration()); assertEquals(3,row(8261).weight());
        assertThrows(UnsupportedOperationException.class,()->LootPotionVariants.all().clear());
    }
    private static LootPotionVariants.Variant row(int metadata) { return LootPotionVariants.all().stream().filter(v->v.metadata()==metadata).findFirst().orElseThrow(); }
    private static void vector(int metadata,int duration,int amplifier,int color) {
        var row=row(metadata); assertEquals(duration,row.duration(),"duration meta="+metadata); assertEquals(amplifier,row.amplifier(),"amp meta="+metadata); assertEquals(color,row.color(),"color meta="+metadata);
    }
}
