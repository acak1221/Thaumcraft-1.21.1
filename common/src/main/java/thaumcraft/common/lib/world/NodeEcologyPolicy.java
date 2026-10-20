package thaumcraft.common.lib.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Original empty-by-default worldgen blacklist levels, with native registry keys in place of removed numeric IDs. */
public final class NodeEcologyPolicy {
    public static final Map<ResourceKey<Level>, Integer> dimensionBlacklist = new ConcurrentHashMap<>();
    public static final Map<ResourceKey<Biome>, Integer> biomeBlacklist = new ConcurrentHashMap<>();
    private NodeEcologyPolicy() {}

    public static int dimensionLevel(ResourceKey<Level> dimension) { return dimensionBlacklist.getOrDefault(dimension, -1); }
    public static int biomeLevel(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> biomeBlacklist.getOrDefault(key, -1)).orElse(-1);
    }
    public static boolean permitsLevel(int blacklistLevel) { return blacklistLevel != 0 && blacklistLevel != 2; }
    public static boolean permitsDimension(ResourceKey<Level> dimension) {
        return !dimension.equals(Level.NETHER) && !dimension.equals(Level.END) && permitsLevel(dimensionLevel(dimension));
    }
}
