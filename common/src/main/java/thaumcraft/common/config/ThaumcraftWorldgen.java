package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.world.ThaumcraftOreFeature;

/** Data-driven configured/placed features; biome injection belongs to loader adapters. */
public final class ThaumcraftWorldgen {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Thaumcraft.MODID, Registries.FEATURE);
    public static final RegistrySupplier<ThaumcraftOreFeature> ORES = FEATURES.register("tc4_ores", ThaumcraftOreFeature::new);
    public static final ResourceKey<PlacedFeature> ORES_KEY = ResourceKey.create(Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc4_ores"));

    private ThaumcraftWorldgen() {}
    public static void register() { FEATURES.register(); }
}
