package thaumcraft.biomes;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.OriginalSpawnSettings;
import thaumcraft.common.config.OriginalWorldgenSettings;
import thaumcraft.common.lib.world.biomes.OriginalSpawnRules;
import thaumcraft.common.lib.world.biomes.OriginalSpawnRules.Row;

class OriginalSpawnRulesTest {
    private static final Row ZOMBIE=new Row("minecraft:zombie",100,4,4),PECH=new Row("thaumcraft:pech",10,1,2),
        WISP=new Row("thaumcraft:wisp",10,1,2),TAINTACLE=new Row("thaumcraft:taintacle",1,1,1);
    @Test void originalForgeUpdatesFirstPechInPlaceThenAppendsAnotherRow() {
        var rows=OriginalSpawnRules.apply("thaumcraft:magical_forest",true,false,false,List.of(ZOMBIE,PECH,WISP),OriginalSpawnSettings.DEFAULTS,false);
        assertEquals(List.of(ZOMBIE,new Row("thaumcraft:pech",10,1,1),WISP,new Row("thaumcraft:pech",10,1,1)),rows);
    }
    @Test void everyRealConstructorSwitchIsAppliedBeforeTheGlobalNonemptyGate() {
        var allOff=new OriginalSpawnSettings(false,false,false,false,false,false);
        assertEquals(List.of(ZOMBIE),OriginalSpawnRules.apply("thaumcraft:magical_forest",true,false,false,List.of(ZOMBIE,PECH,WISP),allOff,false));
        assertEquals(List.of(ZOMBIE),OriginalSpawnRules.apply("thaumcraft:eerie",true,false,false,List.of(ZOMBIE,
            new Row("thaumcraft:brainy_zombie",32,1,1),new Row("thaumcraft:giant_brainy_zombie",8,1,1),
            new Row("thaumcraft:wisp",3,1,1),new Row("thaumcraft:eldritch_guardian",1,1,1)),allOff,false));
        var noTaintacle=new OriginalSpawnSettings(true,true,true,false,true,true);
        assertEquals(List.of(),OriginalSpawnRules.apply("thaumcraft:taint",true,false,false,List.of(TAINTACLE),noTaintacle,false),
            "spawn_pechs=true cannot populate the originally empty Taint monster list after disabling Taintacles");
        assertEquals(List.of(ZOMBIE,new Row("thaumcraft:pech",10,1,1)),OriginalSpawnRules.apply("thaumcraft:taint",true,false,false,List.of(TAINTACLE,ZOMBIE),noTaintacle,false),
            "a genuine addon monster restores the original nonempty-list condition");
    }
    @Test void flagsDoNotDeleteUnrelatedAddonSpawnsOrMoveExistingRows() {
        var allOff=new OriginalSpawnSettings(false,false,false,false,false,false);var addonPech=new Row("thaumcraft:pech",7,2,3);
        assertEquals(List.of(ZOMBIE,addonPech,WISP),OriginalSpawnRules.apply("thaumcraft:magical_forest",true,false,false,List.of(ZOMBIE,PECH,addonPech,WISP,WISP),allOff,false));
        var secondPech=new Row("thaumcraft:pech",99,3,4);
        var result=OriginalSpawnRules.apply("addon:magical",true,false,false,List.of(ZOMBIE,PECH,secondPech),OriginalSpawnSettings.DEFAULTS,false);
        assertEquals(List.of(ZOMBIE,new Row("thaumcraft:pech",10,1,1),secondPech,new Row("thaumcraft:pech",10,1,1)),result);
    }
    @Test void globalBranchesRetainOriginalOrderWeightsAndHalloweenOnlyFirebatGate() {
        var result=OriginalSpawnRules.apply("addon:allowed_magical_nether",true,true,true,List.of(ZOMBIE),OriginalSpawnSettings.DEFAULTS,true);
        assertEquals(List.of(ZOMBIE,new Row("thaumcraft:brainy_zombie",10,1,1),new Row("thaumcraft:pech",10,1,1),
            new Row("thaumcraft:firebat",5,1,2),new Row("thaumcraft:firebat",5,1,2),new Row("thaumcraft:wisp",5,1,1)),result);
        assertEquals(List.of(new Row("thaumcraft:firebat",10,1,2),new Row("thaumcraft:wisp",5,1,1)),
            OriginalSpawnRules.apply("minecraft:nether_wastes",false,false,true,List.of(),OriginalSpawnSettings.DEFAULTS,false));
        assertEquals(List.of(),OriginalSpawnRules.apply("addon:empty_magical",true,true,false,List.of(),OriginalSpawnSettings.DEFAULTS,false));
    }
    @Test void exactOriginalPropertyNamesRemainIndependentIncludingTheOriginalCinnibarTypo() {
        var flags=new Properties();for(var key:List.of("spawn_angry_zombies","spawn_fire_bats","spawn_wisps","spawn_taintacles","spawn_pechs","spawn_eldercreatures"))flags.setProperty(key,"false");
        assertEquals(new OriginalSpawnSettings(false,false,false,false,false,false),OriginalSpawnSettings.read(flags));
        flags.setProperty("spawn_taint_spores","false");assertEquals(new OriginalSpawnSettings(false,false,false,false,false,false),OriginalSpawnSettings.read(flags),"spore setting is separately owned");
        var worldgen=new Properties();worldgen.setProperty("generate_cinnibar_ore","false");worldgen.setProperty("generate_aura_nodes","false");
        assertEquals(new OriginalWorldgenSettings(false,true,false,true,true),OriginalWorldgenSettings.read(worldgen));
        assertEquals(OriginalWorldgenSettings.DEFAULTS,OriginalWorldgenSettings.read(new Properties()));
        flags.setProperty("spawn_wisps","garbage");assertThrows(IllegalArgumentException.class,()->OriginalSpawnSettings.read(flags));
        worldgen.setProperty("generate_structures","garbage");assertThrows(IllegalArgumentException.class,()->OriginalWorldgenSettings.read(worldgen));
    }
}
