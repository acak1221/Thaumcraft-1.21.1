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

/** Config.registerBiomes:782-812 / BiomeHandler:53-64, not donor tree weights. */
final class MagicalTreeBiomeAdapter {
    // Share the declared native BiomeDictionary-type mapping with the ore
    // adapter, NOT its primal selection algorithm. Config order is an explicit
    // deterministic replacement for Forge's old type-array enumeration order.
    private static final List<Type> TYPES = List.of(
            type("water", 100, Aspect.WATER, 0), type("ocean", 120, Aspect.WATER, 0),
            type("river", 100, Aspect.WATER, 0), type("wet", 80, Aspect.WATER, 0),
            type("hot", 100, Aspect.FIRE, 0), type("desert", 100, Aspect.FIRE, 0),
            type("nether", 120, Aspect.FIRE, 0), type("mesa", 80, Aspect.FIRE, 0),
            type("dense", 100, Aspect.ORDER, 0), type("snowy", 80, Aspect.ORDER, 0),
            type("cold", 80, Aspect.ORDER, 0), type("frozen", 100, Aspect.ORDER, 0),
            type("mushroom", 140, Aspect.ORDER, 0), type("coniferous", 100, Aspect.EARTH, .2F),
            type("forest", 120, Aspect.EARTH, 1), type("sandy", 80, Aspect.EARTH, 0),
            type("beach", 80, Aspect.EARTH, 0), type("savanna", 80, Aspect.AIR, .2F),
            type("mountain", 100, Aspect.AIR, 0), type("hills", 120, Aspect.AIR, 0),
            type("plains", 80, Aspect.AIR, .2F), type("dry", 80, Aspect.ENTROPY, 0),
            type("sparse", 80, Aspect.ENTROPY, 0), type("swamp", 120, Aspect.ENTROPY, .2F),
            type("wasteland", 80, Aspect.ENTROPY, 0), type("jungle", 100, Aspect.PLANT, 0),
            type("lush", 100, Aspect.PLANT, .5F), type("magical", 100, null, 1),
            type("end", 80, Aspect.VOID, 0), type("spooky", 80, Aspect.SOUL, 0), type("dead", 50, Aspect.DEATH, 0));
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
