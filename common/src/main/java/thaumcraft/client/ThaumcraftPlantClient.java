package thaumcraft.client;

import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import thaumcraft.client.fx.particles.ResourceParticle;
import thaumcraft.common.blocks.BlockCustomPlant;
import thaumcraft.common.config.ThaumcraftPlants;

public final class ThaumcraftPlantClient {
    private ThaumcraftPlantClient() {}
    public static void init() {
        RenderTypeRegistry.register(RenderType.cutout(), ThaumcraftPlants.SHIMMERLEAF.get(), ThaumcraftPlants.CINDERPEARL.get());
        BlockCustomPlant.setClientEffects((level, pos, cinderpearl, random) -> {
            if (!cinderpearl && random.nextInt(3) == 0) ResourceParticle.shimmerleaf(level, pos);
            else if (cinderpearl && random.nextBoolean()) {
                var effects = level.random;
                double x = pos.getX() + .5F + (effects.nextFloat() - effects.nextFloat()) * .1F;
                double y = pos.getY() + .6F + (effects.nextFloat() - effects.nextFloat()) * .1F;
                double z = pos.getZ() + .5F + (effects.nextFloat() - effects.nextFloat()) * .1F;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
            }
        });
    }
}
