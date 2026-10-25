package thaumcraft.common.lib.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.world.biomes.OriginalBiomeTypes;

/**
 * Native biome/tag adapter, not a claim of 1.7 biome/seed identity. Every matched
 * old dictionary TYPE has one vote (not one vote per aspect). The aura values
 * and nullable tag are Config.registerBiomes:782-812 and BiomeHandler:26-50.
 * The separate node_biome_types data maps modern surface biomes to those types;
 * unknown biomes use the original 100/null fallback, never ore depth weights.
 */
public final class NormalNodeBiomeAdapter {
    private static final List<BiomeType> TYPES = OriginalBiomeTypes.all().stream()
            .map(type -> type(type.name(), type.aura(), type.aspect())).toList();

    private NormalNodeBiomeAdapter() {}

    public static BiomeInfo describe(Holder<Biome> biome) {
        ArrayList<BiomeType> matched = new ArrayList<>();
        int total = 0;
        for (BiomeType type : TYPES) if (biome.is(type.tag())) {
            matched.add(type);
            total += type.aura();
        }
        return new BiomeInfo(matched.isEmpty() ? 100 : total / matched.size(), List.copyOf(matched));
    }

    public record BiomeInfo(int aura, List<BiomeType> types) {
        public Aspect randomTag(RandomSource random) {
            return types.isEmpty() ? null : types.get(random.nextInt(types.size())).aspect();
        }
    }

    private static BiomeType type(String name, int aura, Aspect aspect) {
        return new BiomeType(TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "node_biome_types/" + name)), aura, aspect);
    }

    public record BiomeType(TagKey<Biome> tag, int aura, Aspect aspect) {}
}
