package thaumcraft.common.lib.world.biomes;

import java.util.ArrayList;
import java.util.List;
import thaumcraft.common.config.OriginalSpawnSettings;

/** Literal TC4 biome constructor guards, then ConfigEntities.initEntitySpawns
 * order and official Forge1.7 addSpawn (update FIRST existing row, APPEND one).
 * Native biome tables keep original constructor rows; this transforms them
 * once per native registry load, preserving weighted row order/duplicates. */
public final class OriginalSpawnRules {
    public record Row(String entity,int weight,int min,int max) {}
    private static final String PECH="thaumcraft:pech",WISP="thaumcraft:wisp",BRAINY="thaumcraft:brainy_zombie",
        GIANT="thaumcraft:giant_brainy_zombie",GUARDIAN="thaumcraft:eldritch_guardian",TAINTACLE="thaumcraft:taintacle",FIREBAT="thaumcraft:firebat";
    private OriginalSpawnRules() {}
    public static List<Row> apply(String biome,boolean magical,boolean allowedSpawnBiome,boolean nether,List<Row> constructorRows,OriginalSpawnSettings options,boolean halloween) {
        var rows=new ArrayList<>(constructorRows);
        switch(biome) {
            case "thaumcraft:magical_forest" -> {
                if(!options.pechs())removeConstructor(rows,new Row(PECH,10,1,2));
                if(!options.wisps())removeConstructor(rows,new Row(WISP,10,1,2));
            }
            case "thaumcraft:eerie" -> {
                if(!options.angryZombies()){removeConstructor(rows,new Row(BRAINY,32,1,1));removeConstructor(rows,new Row(GIANT,8,1,1));}
                if(!options.wisps())removeConstructor(rows,new Row(WISP,3,1,1));
                if(!options.elderCreatures())removeConstructor(rows,new Row(GUARDIAN,1,1,1));
            }
            case "thaumcraft:taint" -> {if(!options.taintacles())removeConstructor(rows,new Row(TAINTACLE,1,1,1));}
            default -> {}
        }
        if(options.angryZombies()&&allowedSpawnBiome&&!rows.isEmpty())addSpawn(rows,new Row(BRAINY,10,1,1));
        if(options.pechs()&&magical&&!rows.isEmpty())addSpawn(rows,new Row(PECH,10,1,1));
        if(options.fireBats()) {
            if(nether)addSpawn(rows,new Row(FIREBAT,10,1,2));
            if(halloween&&allowedSpawnBiome)addSpawn(rows,new Row(FIREBAT,5,1,2));
        }
        if(options.wisps()&&nether)addSpawn(rows,new Row(WISP,5,1,1));
        return List.copyOf(rows);
    }
    /** Native datapack/addon rows remain independent: only the original
     * constructor's first exact entry is removed when its switch is false. */
    private static void removeConstructor(List<Row> rows,Row expected) {rows.remove(expected);}
    private static void addSpawn(List<Row> rows,Row addition) {
        for(int i=0;i<rows.size();i++)if(rows.get(i).entity().equals(addition.entity())){rows.set(i,addition);break;}
        rows.add(addition);
    }
}
