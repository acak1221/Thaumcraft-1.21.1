package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityTaintSpider;

public final class RenderTaintSpider extends SpiderRenderer<EntityTaintSpider> {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_spider.png");
    private static final RenderType EYES=RenderType.eyes(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_spider_eyes.png"));
    public RenderTaintSpider(EntityRendererProvider.Context context){super(context);shadowRadius=.5F;layers.clear();addLayer(new EyesLayer<EntityTaintSpider,SpiderModel<EntityTaintSpider>>(this){@Override public RenderType renderType(){return EYES;}});}
    @Override public ResourceLocation getTextureLocation(EntityTaintSpider entity){return SKIN;}
    @Override protected void scale(EntityTaintSpider entity,PoseStack pose,float partial){float size=entity.spiderScaleAmount();pose.scale(size,size*1.25F,size);}
    @Override protected float getFlipDegrees(EntityTaintSpider entity){return 180;}
}
