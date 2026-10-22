package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.entities.ModelTaintSeed;
import thaumcraft.common.entities.monster.EntityTaintSeed;

/**
 * The tainted seed and the prime tainted seed, drawn with the chain of the seed model.
 *
 * <p>TC4 4.2.3.5 has no tainted seed; the entity comes from
 * {@code Thaumcraft-1.12.2-6.1.BETA26.jar}, whose client registers the seed with
 * {@code new RenderTaintSeed(renderManager)} and the prime seed with
 * {@code new RenderTaintSeed(renderManager, new ModelTaintSeed(), 0.6f)}
 * ({@code thaumcraft/proxies/ProxyEntities.java:200-201}). The shadow of the seed is 0.4
 * and the shadow of the prime seed is 0.6.</p>
 *
 * <p>Declared native adaptation: the backport sheet of the seed is not part of this port's
 * asset tree, so the native renderer draws the chain with the port's own taint tentacle
 * skin. See {@link ModelTaintSeed} for the rest of the adaptation.</p>
 */
public final class RenderTaintSeed extends MobRenderer<EntityTaintSeed, ModelTaintSeed> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/taintacle.png");

    public RenderTaintSeed(EntityRendererProvider.Context context, float shadow) {
        super(context, new ModelTaintSeed(), shadow);
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintSeed entity) {
        return SKIN;
    }
}
