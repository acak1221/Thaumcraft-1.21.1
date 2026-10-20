package thaumcraft.biomes;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.BiomeSettings;
import thaumcraft.common.lib.world.biomes.BiomePatchPolicy;

class BiomePatchPolicyTest {
    @BeforeAll static void bootstrap(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    private static Climate.Sampler climate(double temperature) {
        var zero=DensityFunctions.constant(0);
        return new Climate.Sampler(DensityFunctions.constant(temperature),zero,zero,zero,zero,zero,List.of());
    }
    @Test void disabledTicketsNeverSurviveAnyZoomEvenAtNegativeCoordinates() {
        var policy=new BiomePatchPolicy(912873,climate(0),new BiomeSettings(0,0,true,true,false));
        for(int x=-160;x<=160;x+=3)for(int z=-160;z<=160;z+=5) {
            int selected=policy.sample(x,z);
            assertNotEquals(BiomePatchPolicy.MAGICAL_FOREST,selected);assertNotEquals(BiomePatchPolicy.TAINT,selected);
        }
    }
    @Test void independentRealTicketSwitchesRemainEffectiveAfterAllSixZooms() {
        var warm=climate(0);
        var forest=new BiomePatchPolicy(912873,warm,new BiomeSettings(80,0,true,false,false));
        var taint=new BiomePatchPolicy(912873,warm,new BiomeSettings(0,80,true,false,false));
        boolean foundForest=false,foundTaint=false;
        for(int x=-1024;x<=1024;x+=16)for(int z=-1024;z<=1024;z+=16) {
            int first=forest.sample(x,z),second=taint.sample(x,z);
            assertNotEquals(BiomePatchPolicy.TAINT,first);assertNotEquals(BiomePatchPolicy.MAGICAL_FOREST,second);
            foundForest|=first==BiomePatchPolicy.MAGICAL_FOREST;foundTaint|=second==BiomePatchPolicy.TAINT;
        }
        assertTrue(foundForest);assertTrue(foundTaint,"generate_taint=false must retain the original unused-option quirk");
    }
    @Test void originalColdAndHotPalettesHaveNoCustomTicketsAndCacheEvictionIsOrderIndependent() {
        for(double temperature:new double[]{-1,1}) {
            var policy=new BiomePatchPolicy(831,climate(temperature),new BiomeSettings(100,100,true,true,false));
            for(int x=-128;x<=128;x+=8)for(int z=-128;z<=128;z+=8)assertEquals(temperature<0?12:2,policy.sample(x,z));
        }
        var sampler=climate(0);var forward=new BiomePatchPolicy(381,sampler,BiomeSettings.DEFAULTS);
        var reverse=new BiomePatchPolicy(381,sampler,BiomeSettings.DEFAULTS);int[] expected=new int[10000];
        for(int i=0;i<expected.length;i++)expected[i]=forward.sample(i*13-65536,i*31-101003);
        for(int i=expected.length-1;i>=0;i--)assertEquals(expected[i],reverse.sample(i*13-65536,i*31-101003));
        assertTrue(forward.matches(381,sampler,BiomeSettings.DEFAULTS));
        assertFalse(forward.matches(381,climate(0),BiomeSettings.DEFAULTS));
        assertFalse(forward.matches(381,sampler,new BiomeSettings(4,2,true,true,false)));
    }
}
