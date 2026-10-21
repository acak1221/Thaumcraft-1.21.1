package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;
import thaumcraft.common.lib.world.ThaumcraftTotemFeature;

/** Independent, node-only registration. Does not register another block or item. */
public final class ThaumcraftNodes {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Thaumcraft.MODID, Registries.FEATURE);
    public static final RegistrySupplier<ThaumcraftNormalNodeFeature> NORMAL_AURA_NODES =
            FEATURES.register("normal_aura_nodes", ThaumcraftNormalNodeFeature::new);
    /**
     * The aura totem of the original world generator, one chunk in 360 with the
     * original node rarity (oracle ThaumcraftWorldGenerator:148-187).
     */
    public static final RegistrySupplier<ThaumcraftTotemFeature> AURA_TOTEM =
            FEATURES.register("aura_totem", ThaumcraftTotemFeature::new);
    public static final ResourceKey<PlacedFeature> NODE_KEY = ResourceKey.create(Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "normal_aura_nodes"));
    public static final ResourceKey<PlacedFeature> TOTEM_KEY = ResourceKey.create(Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "aura_totem"));
    public static final TagKey<Biome> NODE_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "normal_aura_node_biomes"));

    private ThaumcraftNodes() {}
    public static void register() { FEATURES.register(); }
}
