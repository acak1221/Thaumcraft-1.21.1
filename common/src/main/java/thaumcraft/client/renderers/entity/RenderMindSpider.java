package thaumcraft.client.renderers.entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityMindSpider;
public final class RenderMindSpider extends SpiderRenderer<EntityMindSpider> {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_spider.png");
    private static final RenderType EYES=RenderType.eyes(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_spider_eyes.png"));
    public RenderMindSpider(EntityRendererProvider.Context context) {
        super(context);shadowRadius=0;layers.clear();
        addLayer(new EyesLayer<EntityMindSpider,SpiderModel<EntityMindSpider>>(this) { @Override public RenderType renderType() { return EYES; } });
    }
    @Override public ResourceLocation getTextureLocation(EntityMindSpider entity) { return SKIN; }
    private static boolean visible(EntityMindSpider entity) {
        var player=Minecraft.getInstance().player;
        return entity.getViewer().isEmpty() || player!=null && entity.getViewer().equals(player.getGameProfile().getName());
    }
    @Override public boolean shouldRender(EntityMindSpider entity,Frustum frustum,double x,double y,double z) { return visible(entity)&&super.shouldRender(entity,frustum,x,y,z); }
    @Override protected void scale(EntityMindSpider entity,PoseStack pose,float partial) { pose.scale(.3F,.3F,.3F); }
    @Override protected float getFlipDegrees(EntityMindSpider entity) { return 180; }
    @Override protected RenderType getRenderType(EntityMindSpider entity,boolean body,boolean translucent,boolean glow) { return RenderType.entityTranslucent(SKIN); }
    @Override public void render(EntityMindSpider entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        if (visible(entity)) super.render(entity,yaw,partial,pose,type->type==EYES?buffers.getBuffer(type):new WarpAlphaVertex(buffers.getBuffer(type),Math.min(.1F,entity.tickCount/100F)),light);
    }
}
