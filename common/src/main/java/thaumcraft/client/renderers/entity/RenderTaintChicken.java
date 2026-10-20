package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityTaintChicken;

/**
 * Original TC4 RenderTaintChicken: the vanilla chicken mesh with the taint sheet and the
 * original wing swing. The original wing swing was the renderer's swing progress; the
 * native renderer feeds the same value through the attack animation hook.
 */
public final class RenderTaintChicken extends MobRenderer<EntityTaintChicken, ChickenModel<EntityTaintChicken>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/chicken.png");

    public RenderTaintChicken(EntityRendererProvider.Context context) {
        super(context, new ChickenModel<>(context.bakeLayer(ModelLayers.CHICKEN)), .3F);
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintChicken chicken) { return SKIN; }

    /** Original getWingRotation and func_77044_a. */
    @Override protected float getAttackAnim(EntityTaintChicken chicken, float partial) {
        float angle = chicken.oFlap + (chicken.flap - chicken.oFlap) * partial;
        float speed = chicken.oFlapSpeed + (chicken.flapSpeed - chicken.oFlapSpeed) * partial;
        return (Mth.sin(angle) + 1.0F) * speed;
    }
}
