package thaumcraft.fabric.world;

import java.util.Calendar;
import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.lib.world.biomes.OriginalSpawnRules;

/** Narrow Fabric registry adapter for original constructor/global spawn order.
 * No independent addSpawn hooks may also install these same original rows. */
public final class FabricBiomeSpawns {
    private static boolean initialized;
    private FabricBiomeSpawns() {}
    public static void init() {
        if(initialized)return;initialized=true;
        var settings=ThaumcraftBiomeSettings.spawns();var calendar=Calendar.getInstance();
        boolean halloween=calendar.get(Calendar.MONTH)==Calendar.OCTOBER&&calendar.get(Calendar.DAY_OF_MONTH)==31;
        BiomeModifications.create(ResourceLocation.fromNamespaceAndPath("thaumcraft","original_entity_spawns")).add(ModificationPhase.REPLACEMENTS,
            selection->selection.hasTag(ThaumcraftManaBeans.MAGICAL_BIOMES)||selection.hasTag(ThaumcraftMonsters.BRAINY_SPAWNS)||selection.hasTag(BiomeTags.IS_NETHER),
            (selection,context)->{
                List<OriginalSpawnRules.Row> before=selection.getBiome().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                    .map(row->new OriginalSpawnRules.Row(BuiltInRegistries.ENTITY_TYPE.getKey(row.type).toString(),row.getWeight().asInt(),row.minCount,row.maxCount)).toList();
                var after=OriginalSpawnRules.apply(selection.getBiomeKey().location().toString(),selection.hasTag(ThaumcraftManaBeans.MAGICAL_BIOMES),
                    selection.hasTag(ThaumcraftMonsters.BRAINY_SPAWNS),selection.hasTag(BiomeTags.IS_NETHER),before,settings,halloween);
                if(before.equals(after))return;
                context.getSpawnSettings().clearSpawns(MobCategory.MONSTER);
                for(var row:after)context.getSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(
                    BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(row.entity())),row.weight(),row.min(),row.max()));
            });
    }
}
