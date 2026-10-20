package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.projectile.EntityPechBlast;

/** Original renderer intentionally has NO entity geometry; the curse's trail
 * and impact particles are its appearance. It retains a .1 shadow. */
public final class RenderPechBlast extends EntityRenderer<EntityPechBlast> {
    public RenderPechBlast(EntityRendererProvider.Context context){super(context);shadowRadius=.1F;}
    @Override public ResourceLocation getTextureLocation(EntityPechBlast blast){return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");}
    @Override public void render(EntityPechBlast blast,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){}
}
