package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.entities.ModelTaintacle;
import thaumcraft.common.entities.monster.EntityTaintacle;

/** The original regular/small entity renderer. Giant boss behavior belongs to its actual boss unit. */
public final class RenderTaintacle extends MobRenderer<EntityTaintacle,ModelTaintacle> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taintacle.png");
    public RenderTaintacle(EntityRendererProvider.Context context,float shadow,int length){super(context,new ModelTaintacle(length),shadow);}
    @Override public ResourceLocation getTextureLocation(EntityTaintacle entity){return TEXTURE;}
}
