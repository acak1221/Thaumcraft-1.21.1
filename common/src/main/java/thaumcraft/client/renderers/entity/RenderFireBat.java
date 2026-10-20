package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.renderers.models.ModelFireBat;
import thaumcraft.common.entities.monster.EntityFireBat;

public final class RenderFireBat extends MobRenderer<EntityFireBat,ModelFireBat> {
    private static final ResourceLocation NORMAL=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/firebat.png");
    private static final ResourceLocation VAMPIRE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/vampirebat.png");
    public RenderFireBat(EntityRendererProvider.Context context) { super(context,new ModelFireBat(),.25F); }
    @Override public ResourceLocation getTextureLocation(EntityFireBat bat) { return bat.getIsVampire()?VAMPIRE:NORMAL; }
    @Override protected void scale(EntityFireBat bat,PoseStack pose,float partial) { float scale=bat.getIsDevil()||bat.getIsVampire()?.6F:.35F;pose.scale(scale,scale,scale); }
    @Override protected void setupRotations(EntityFireBat bat,PoseStack pose,float age,float yaw,float partial,float scale) { pose.translate(0,bat.getIsBatHanging()?-.1F:Mth.cos(age*.3F)*.1F,0);super.setupRotations(bat,pose,age,yaw,partial,scale); }
    @Override public void render(EntityFireBat bat,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) { super.render(bat,yaw,partial,pose,buffers,15728880); }
}
