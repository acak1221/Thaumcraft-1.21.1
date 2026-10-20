package thaumcraft.common.lib.loot;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Actual unmodified official1.7 helper oracle output, not values derived from this native implementation. */
class OriginalLootEnchantmentsTest {
    private record Vector(int level,long seed,String picks,int next){}
    @Test void officialBookOracleSelectionsRetainExactWeightsFloatBoostHashOrderAndRandomConsumption() {
        // Executed afv.b(Random,actual vanilla BOOK,level), JDK21, original client SHA1e80d9b3bf5085002218d4be59e668bac718abbc6.
        var vectors=List.of(
                new Vector(5,0,"32:1,2:1",218719),new Vector(5,1,"32:1,48:1",239978),new Vector(5,2,"0:1,32:1",659847),
                new Vector(5,42,"2:1",21505),new Vector(5,20261020,"2:1",401755),
                new Vector(15,0,"32:2,5:1",218719),new Vector(15,1,"32:2,16:2,48:2",136569),new Vector(15,2,"19:1,0:2",659847),
                new Vector(15,42,"33:1",21505),new Vector(15,20261020,"19:1",401755),
                new Vector(30,0,"61:2,50:1,17:4,35:2",187473),new Vector(30,1,"0:3,33:1,50:1",136569),new Vector(30,2,"34:3,2:4",659847),
                new Vector(30,42,"48:3,0:3",255519),new Vector(30,20261020,"19:2",401755));
        for(var vector:vectors) {
            var rng=RandomSource.create(vector.seed());
            var picks=OriginalLootEnchantments.select(rng,1,vector.level(),id->id<150,OriginalLootEnchantments::vanillaCompatible);
            assertEquals(vector.picks(),picks.stream().map(pick->pick.id()+":"+pick.level()).collect(java.util.stream.Collectors.joining(",")),vector.toString());
            assertEquals(vector.next(),rng.nextInt(1000000),vector.toString());
        }
    }
    @Test void actualForgeBilateralPatchChangesWeightsAfterSilkEvenWhenForbiddenLuckWouldNotBeSelected() {
        var rng=RandomSource.create(1);
        var picks=OriginalLootEnchantments.select(rng,1,30,id->id<150);
        assertEquals("0:3,33:1,62:2",picks.stream().map(pick->pick.id()+":"+pick.level()).collect(java.util.stream.Collectors.joining(",")));
        assertEquals(136569,rng.nextInt(1000000));
        assertTrue(OriginalLootEnchantments.vanillaCompatible(33,61));
        assertFalse(OriginalLootEnchantments.vanillaCompatible(61,33));
        assertFalse(OriginalLootEnchantments.compatible(33,61));
    }
    @Test void originalCustomsHaveExactBoundsAndNoModernRegistryCandidatesCanEnter() {
        assertEquals(26,OriginalLootEnchantments.RULES.size());
        assertEquals(3,OriginalLootEnchantments.find(150).weight());assertEquals(2,OriginalLootEnchantments.find(151).weight());
        assertEquals(33,OriginalLootEnchantments.minCost(150,3));assertEquals(81,OriginalLootEnchantments.maxCost(150,3));
        assertEquals(30,OriginalLootEnchantments.minCost(151,2));assertEquals(71,OriginalLootEnchantments.maxCost(151,2));
        assertEquals(61,OriginalLootEnchantments.maxCost(32,1));assertEquals(71,OriginalLootEnchantments.maxCost(32,2));
        assertEquals(List.of(0,1,2,3,4,5,6,7,16,17,18,19,20,21,32,33,34,35,48,49,50,51,61,62,150,151),OriginalLootEnchantments.RULES.stream().map(OriginalLootEnchantments.Rule::id).toList());
        var rng=RandomSource.create(123);var untouched=RandomSource.create(123);
        assertTrue(OriginalLootEnchantments.select(rng,0,30,id->true).isEmpty());assertEquals(untouched.nextInt(),rng.nextInt());
    }
    @Test void forgeBilateralVetoesKeepSilkLootVariantsRepairDurabilityAndProtectionFamiliesExact() {
        for(int loot:new int[]{21,35,61})assertFalse(OriginalLootEnchantments.compatible(33,loot));
        assertFalse(OriginalLootEnchantments.compatible(34,151));assertFalse(OriginalLootEnchantments.compatible(151,34));
        for(int first=0;first<=4;first++)for(int second=0;second<=4;second++)assertEquals(first!=second&&(first==2||second==2),OriginalLootEnchantments.compatible(first,second));
        for(int first=16;first<=18;first++)for(int second=16;second<=18;second++)assertFalse(OriginalLootEnchantments.compatible(first,second));
        assertTrue(OriginalLootEnchantments.compatible(150,151));
    }
}
