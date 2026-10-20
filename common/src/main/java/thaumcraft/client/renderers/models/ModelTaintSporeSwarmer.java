package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityTaintSporeSwarmer;

/** Original independently lit/pulsing inner cube and world-lit fixed base. */
public final class ModelTaintSporeSwarmer extends EntityModel<EntityTaintSporeSwarmer> {
    private final ModelPart cube,base;
    private float size,pulse;
    public ModelTaintSporeSwarmer(){var root=layer().bakeRoot();cube=root.getChild("cube");base=root.getChild("base");}
    public static LayerDefinition layer(){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0).addBox(-8,0,-8,16,16,16),PartPose.ZERO);mesh.getRoot().addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,32).addBox(-8,-8,-8,16,16,16),PartPose.offset(0,16,0));return LayerDefinition.create(mesh,64,64);}
    @Override public void setupAnim(EntityTaintSporeSwarmer entity,float limb,float amount,float age,float yaw,float pitch){float intensity=entity.hurtTime>0?.04F:.02F;cube.xRot=intensity*Mth.sin(age*.05F);cube.zRot=intensity*Mth.sin(age*.1F);size=entity.displaySize;pulse=.025F*Mth.sin(entity.tickCount*.075F);}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){
        pose.pushPose();try{float x=-.07F*size-pulse,y=-.07F*size+pulse;pose.translate(0,1.6,0);pose.scale(x,y,x);pose.translate(0,-y/2F,0);cube.render(pose,out,LightTexture.FULL_BRIGHT,overlay,color);}finally{pose.popPose();}
        base.render(pose,out,light,overlay,color);
    }
}
