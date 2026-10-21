package thaumcraft.neoforge;

import com.mojang.serialization.MapCodec;
import java.util.Calendar;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.lib.world.biomes.OriginalSpawnRules;

/**
 * NeoForge half of the original TC4 entity spawn rules.
 *
 * <p>Fabric applies them with a {@code BiomeModifications} entry in the
 * {@code REPLACEMENTS} phase, whose selector is
 * {@code hasTag(magical_biomes) || hasTag(brainy_zombie_spawns) || hasTag(is_nether)}. NeoForge has
 * no runtime biome modification event: its biome modifiers are datapack entries that are applied
 * while the biome registry is built. This modifier is the exact equivalent: the same selector, the
 * same {@link OriginalSpawnRules} transformation and the same one-per-level-tick order.</p>
 *
 * <p>The datapack entry that activates it is
 * {@code data/thaumcraft/neoforge/biome_modifier/original_entity_spawns.json}.</p>
 */
public final class OriginalEntitySpawnsBiomeModifier implements BiomeModifier {
    /** The modifier carries no data: the selector and the rules are the original ones. */
    public static final MapCodec<OriginalEntitySpawnsBiomeModifier> CODEC = MapCodec.unit(new OriginalEntitySpawnsBiomeModifier());

    private static final boolean HALLOWEEN = isHalloween();

    private OriginalEntitySpawnsBiomeModifier() {}

    private static boolean isHalloween() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH) == Calendar.OCTOBER && calendar.get(Calendar.DAY_OF_MONTH) == 31;
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // Fabric's REPLACEMENTS phase runs after the add/remove phases and before the last one.
        if (phase != Phase.AFTER_EVERYTHING) return;
        boolean magical = biome.is(ThaumcraftManaBeans.MAGICAL_BIOMES);
        boolean allowedSpawnBiome = biome.is(ThaumcraftMonsters.BRAINY_SPAWNS);
        boolean nether = biome.is(BiomeTags.IS_NETHER);
        if (!magical && !allowedSpawnBiome && !nether) return;
        var monsters = builder.getMobSpawnSettings().getSpawner(MobCategory.MONSTER);
        List<OriginalSpawnRules.Row> before = monsters.stream()
                .map(row -> new OriginalSpawnRules.Row(BuiltInRegistries.ENTITY_TYPE.getKey(row.type).toString(),
                        row.getWeight().asInt(), row.minCount, row.maxCount)).toList();
        var after = OriginalSpawnRules.apply(biome.unwrapKey().map(key -> key.location().toString()).orElse(""),
                magical, allowedSpawnBiome, nether, before, ThaumcraftBiomeSettings.spawns(), HALLOWEEN);
        if (before.equals(after)) return;
        monsters.clear();
        for (var row : after) {
            var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(row.entity()));
            monsters.add(new MobSpawnSettings.SpawnerData(type, row.weight(), row.min(), row.max()));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
