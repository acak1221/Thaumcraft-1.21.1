package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.entities.ModelEldritchGolem;
import thaumcraft.common.entities.monster.EntityEldritchGolem;

/**
 * Original TC4 RenderEldritchGolem: the golem sheet at 2.15 scale with alpha blending.
 * The original registered its own boss bar through BossStatus; native 1.21.1 bosses
 * publish their bar through the entity's own BossEvent data, so no renderer work is
 * needed for it.
 */
public final class RenderEldritchGolem extends MobRenderer<EntityEldritchGolem, ModelEldritchGolem> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/eldritch_golem.png");

    public RenderEldritchGolem(EntityRendererProvider.Context context) {
        super(context, new ModelEldritchGolem(ModelEldritchGolem.createBodyLayer().bakeRoot()), .5F);
    }

    @Override public ResourceLocation getTextureLocation(EntityEldritchGolem golem) { return SKIN; }

    /** Original func_77041_b:56-59. */
    @Override protected void scale(EntityEldritchGolem golem, PoseStack pose, float partial) {
        pose.scale(2.15F, 2.15F, 2.15F);
    }

    /** Original doRenderLiving:61-79 blended the whole body. */
    @Override protected RenderType getRenderType(EntityEldritchGolem golem, boolean body, boolean translucent, boolean glow) {
        return RenderType.entityTranslucent(SKIN);
    }
}
