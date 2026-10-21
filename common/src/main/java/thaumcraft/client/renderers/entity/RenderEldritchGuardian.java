package thaumcraft.client.renderers.entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import thaumcraft.client.renderers.models.ModelEldritchGuardian;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
public final class RenderEldritchGuardian extends MobRenderer<EntityEldritchGuardian,ModelEldritchGuardian> {
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/eldritch_guardian.png");
    public RenderEldritchGuardian(EntityRendererProvider.Context context) { super(context,new ModelEldritchGuardian(ModelEldritchGuardian.createBodyLayer().bakeRoot()),.5F); }
    @Override public ResourceLocation getTextureLocation(EntityEldritchGuardian entity) { return SKIN; }
    @Override protected RenderType getRenderType(EntityEldritchGuardian entity,boolean body,boolean translucent,boolean glow) { return RenderType.entityTranslucent(SKIN); }
    @Override public void render(EntityEldritchGuardian entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        float alpha=1;
        var camera=Minecraft.getInstance().getCameraEntity();
        if (camera!=null && !entity.level().dimension().location().getPath().equals("outer_lands")) {
            double distance=entity.distanceToSqr(camera), cap=entity.level().getDifficulty()==Difficulty.HARD?576:1024;
            alpha=distance<256?.6F:(float)(1-Math.min(cap-256,distance-256)/(cap-256))*.6F;
        }
        final float opacity=alpha;
        // Original doRenderLiving:68 - the guardian mesh was drawn one entity height below
        // the position of the entity; the original mesh is built around its own middle.
        pose.pushPose();
        try {
            pose.translate(0.0F, -entity.getBbHeight(), 0.0F);
            super.render(entity,yaw,partial,pose,type->new WarpAlphaVertex(buffers.getBuffer(type),opacity),light);
        } finally { pose.popPose(); }
    }
}
