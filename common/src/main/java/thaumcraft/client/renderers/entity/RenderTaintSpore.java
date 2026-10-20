package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.renderers.models.ModelTaintSpore;
import thaumcraft.common.entities.monster.EntityTaintSpore;

public final class RenderTaintSpore extends MobRenderer<EntityTaintSpore,ModelTaintSpore> {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_spore.png");
    public RenderTaintSpore(EntityRendererProvider.Context context){super(context,new ModelTaintSpore(),.25F);}
    @Override public ResourceLocation getTextureLocation(EntityTaintSpore entity){return SKIN;}
    @Override protected float getFlipDegrees(EntityTaintSpore entity){return 0;}
    @Override protected void scale(EntityTaintSpore entity,PoseStack pose,float partial){float size=entity.displaySize;if(size<entity.getSporeSize())size+=.02F*partial;float pulse=.025F*Mth.sin(entity.tickCount*.075F);pose.scale(-.12F*size-pulse,-.12F*size+pulse,-.12F*size-pulse);}
    @Override protected RenderType getRenderType(EntityTaintSpore entity,boolean visible,boolean translucent,boolean glow){return visible||translucent?RenderType.entityTranslucent(SKIN):glow?RenderType.outline(SKIN):null;}
    @Override public void render(EntityTaintSpore entity,float yaw,float partial,PoseStack pose,MultiBufferSource out,int light){super.render(entity,yaw,partial,pose,out,LightTexture.FULL_BRIGHT);}
}
