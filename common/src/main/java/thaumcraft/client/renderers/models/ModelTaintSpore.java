package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityTaintSpore;

/** Actual original nested12/16 cubes and64x64 UV, twice the wobble when hurt. */
public final class ModelTaintSpore extends EntityModel<EntityTaintSpore> {
    private final ModelPart cube;
    public ModelTaintSpore(){cube=layer().bakeRoot().getChild("cube");}
    public static LayerDefinition layer(){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0).addBox(-6,2,-6,12,12,12).addBox(-8,0,-8,16,16,16),PartPose.offset(0,24,0));return LayerDefinition.create(mesh,64,64);}
    @Override public void setupAnim(EntityTaintSpore entity,float limb,float amount,float age,float yaw,float pitch){float intensity=entity.hurtTime>0?.04F:.02F;cube.xRot=intensity*Mth.sin(age*.05F);cube.zRot=intensity*Mth.sin(age*.1F);}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){cube.render(pose,out,light,overlay,color);}
}
