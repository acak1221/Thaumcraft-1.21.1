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

/** Config.registerBiomes:782-812 / BiomeHandler:53-64, not donor tree weights. */
final class MagicalTreeBiomeAdapter {
    // Share the declared native BiomeDictionary-type mapping with the ore
    // adapter, NOT its primal selection algorithm. Config order is an explicit
    // deterministic replacement for Forge's old type-array enumeration order.
    private static final List<Type> TYPES = OriginalBiomeTypes.all().stream()
            .map(type -> type(type.name(), type.aura(), type.aspect(),
                    type.greatwood() ? type.greatwoodChance() : 0)).toList();
    private static final TagKey<Biome> SILVERWOOD = tag("silverwood_biomes");
    private static final TagKey<Biome> SILVERWOOD_EXCLUDED = tag("silverwood_excluded_biomes");

    private MagicalTreeBiomeAdapter() {}

    static float greatwoodChance(Holder<Biome> biome) {
        for (Type type : TYPES) if (type.chance > 0 && biome.is(type.tag)) return type.chance;
        return 0;
    }

    static boolean silverwood(Holder<Biome> biome) { return biome.is(SILVERWOOD) && !biome.is(SILVERWOOD_EXCLUDED); }

    static Info describe(Holder<Biome> biome) {
        List<Type> matched = new ArrayList<>();
        int total = 0;
        for (Type type : TYPES) if (biome.is(type.tag)) { matched.add(type); total += type.aura; }
        return new Info(matched.isEmpty() ? 100 : total / matched.size(), matched);
    }

    record Info(int aura, List<Type> types) {
        Aspect randomTag(RandomSource random) { return types.isEmpty() ? null : types.get(random.nextInt(types.size())).aspect; }
    }
    private static Type type(String name, int aura, Aspect aspect, float chance) {
        return new Type(tag("ore_biome_types/" + name), aura, aspect, chance);
    }
    private static TagKey<Biome> tag(String path) { return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("thaumcraft", path)); }
    private record Type(TagKey<Biome> tag, int aura, Aspect aspect, float chance) {}
}
