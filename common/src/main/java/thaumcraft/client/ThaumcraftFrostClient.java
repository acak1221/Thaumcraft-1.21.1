package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.client.renderers.entity.RenderFrostShard;
import thaumcraft.client.fx.particles.FocusFrostSparkleParticle;
import thaumcraft.common.config.ThaumcraftFrost;
import thaumcraft.common.entities.projectile.EntityFrostShard;

/** Client-only native renderer and actual tracked-entity impact events; no trail or test FX. */
public final class ThaumcraftFrostClient {
    private static final ResourceLocation FRAGMENT = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/frostshard");
    private static boolean initialized;
    private ThaumcraftFrostClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        EntityRendererRegistry.register(ThaumcraftFrost.FROST_SHARD, RenderFrostShard::new);
        EntityFrostShard.setClientImpactHandler(ThaumcraftFrostClient::impact);
        EntityFrostShard.setClientTrailHandler(FocusFrostSparkleParticle::trail);
    }

    private static void impact(EntityFrostShard entity, byte event) {
        Minecraft client = Minecraft.getInstance();
        if (!(entity.level() instanceof ClientLevel level) || client.level != level
                || level.getEntity(entity.getId()) != entity || !level.hasChunkAt(entity.blockPosition())) return;
        float count = entity.getDamage() * (event == EntityFrostShard.TERMINAL_IMPACT_EVENT ? 8 : 1);
        for (int fragment = 0; fragment < count; ++fragment)
            client.particleEngine.add(new FrostFragment(level, entity));
    }

    /** Native digging-fragment lifetime, gravity, terrain sheet and random UV;
     * original retained frostshard sprite, not a copied donor texture/ice cube.
     * Modern particle motion/lighting are an explicit visual adaptation. */
    private static final class FrostFragment extends TerrainParticle {
        private FrostFragment(ClientLevel level, EntityFrostShard entity) {
            super(level, entity.getX(), entity.getY(), entity.getZ(),
                    4 * (level.random.nextFloat() - .5), .5, 4 * (level.random.nextFloat() - .5), Blocks.ICE.defaultBlockState());
            setSprite(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(FRAGMENT));
        }
    }
}
