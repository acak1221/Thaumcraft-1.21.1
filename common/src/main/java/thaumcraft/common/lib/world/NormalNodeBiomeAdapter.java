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

/**
 * Native biome/tag adapter, not a claim of 1.7 biome/seed identity. Every matched
 * old dictionary TYPE has one vote (not one vote per aspect). The aura values
 * and nullable tag are Config.registerBiomes:782-812 and BiomeHandler:26-50.
 * The separate node_biome_types data maps modern surface biomes to those types;
 * unknown biomes use the original 100/null fallback, never ore depth weights.
 */
public final class NormalNodeBiomeAdapter {
    private static final List<BiomeType> TYPES = List.of(
            type("water", 100, Aspect.WATER), type("ocean", 120, Aspect.WATER),
            type("river", 100, Aspect.WATER), type("wet", 80, Aspect.WATER),
            type("hot", 100, Aspect.FIRE), type("desert", 100, Aspect.FIRE),
            type("nether", 120, Aspect.FIRE), type("mesa", 80, Aspect.FIRE),
            type("dense", 100, Aspect.ORDER), type("snowy", 80, Aspect.ORDER),
            type("cold", 80, Aspect.ORDER), type("frozen", 100, Aspect.ORDER),
            type("mushroom", 140, Aspect.ORDER), type("coniferous", 100, Aspect.EARTH),
            type("forest", 120, Aspect.EARTH), type("sandy", 80, Aspect.EARTH),
            type("beach", 80, Aspect.EARTH), type("savanna", 80, Aspect.AIR),
            type("mountain", 100, Aspect.AIR), type("hills", 120, Aspect.AIR),
            type("plains", 80, Aspect.AIR), type("dry", 80, Aspect.ENTROPY),
            type("sparse", 80, Aspect.ENTROPY), type("swamp", 120, Aspect.ENTROPY),
            type("wasteland", 80, Aspect.ENTROPY), type("jungle", 100, Aspect.PLANT),
            type("lush", 100, Aspect.PLANT), type("magical", 100, null),
            type("end", 80, Aspect.VOID), type("spooky", 80, Aspect.SOUL),
            type("dead", 50, Aspect.DEATH));

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
