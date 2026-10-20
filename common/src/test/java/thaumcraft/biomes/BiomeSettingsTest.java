package thaumcraft.biomes;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.BiomeSettings;

class BiomeSettingsTest {
    @Test void absentPropertiesUseActualOriginalDefaults() {
        assertEquals(new BiomeSettings(5,2,true,true,false),BiomeSettings.read(new Properties()));
        assertEquals(67,BiomeSettings.DEFAULTS.warmWeight());
        assertEquals(47,BiomeSettings.DEFAULTS.coolWeight());
    }
    @Test void exactOriginalKeysControlTicketsTreesAndLocalDisplay() {
        var values=new Properties();values.setProperty("magical_forest_biome_weight"," 0 ");
        values.setProperty("taint_biome_weight","17");values.setProperty("generate_trees","false");
        values.setProperty("generate_taint","false");values.setProperty("blue_magical_forest","TRUE");
        var settings=BiomeSettings.read(values);
        assertEquals(new BiomeSettings(0,17,false,false,true),settings);
        assertEquals(77,settings.warmWeight());assertEquals(57,settings.coolWeight());
        // Old generate_taint had no consumer; false is retained as data and
        // never silently rewritten into the real independently configured weight.
        assertEquals(17,settings.taintWeight());
    }
    @Test void zeroTicketsAreValidButBrokenNativeLotteryBoundsAreRejected() {
        assertEquals(60,new BiomeSettings(0,0,true,true,false).warmWeight());
        assertEquals(Integer.MAX_VALUE,new BiomeSettings(Integer.MAX_VALUE-60,0,true,true,false).warmWeight());
        assertThrows(IllegalArgumentException.class,()->new BiomeSettings(-1,2,true,true,false));
        assertThrows(IllegalArgumentException.class,()->new BiomeSettings(5,-1,true,true,false));
        assertThrows(IllegalArgumentException.class,()->new BiomeSettings(Integer.MAX_VALUE-59,0,true,true,false));
        assertThrows(IllegalArgumentException.class,()->new BiomeSettings(Integer.MAX_VALUE,Integer.MAX_VALUE,true,true,false));
    }
    @Test void malformedValuesFailExplicitlyInsteadOfClampingOrPartiallyLoading() {
        for(var key:new String[]{"magical_forest_biome_weight","taint_biome_weight","generate_trees","generate_taint","blue_magical_forest"}) {
            var values=new Properties();values.setProperty(key,"invalid");
            assertThrows(IllegalArgumentException.class,()->BiomeSettings.read(values),key);
        }
    }
}
