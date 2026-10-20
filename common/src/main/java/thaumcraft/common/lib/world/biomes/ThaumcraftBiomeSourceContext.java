package thaumcraft.common.lib.world.biomes;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/** Actual registry holders owned by the canonical OVERWORLD parameter preset. */
public interface ThaumcraftBiomeSourceContext {
    Stream<Holder<Biome>> thaumcraft$possibleBiomes();
    Holder<Biome> thaumcraft$biome(ResourceKey<Biome> key);
}
