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
import thaumcraft.common.entities.monster.EntityFireBat;

/** Original sixty-four-pixel model, its folded wings and the two hanging/flying poses. */
public final class ModelFireBat extends EntityModel<EntityFireBat> {
    private final ModelPart head,body,right,left,outerRight,outerLeft;
    public ModelFireBat() {
        ModelPart root=layer().bakeRoot();head=root.getChild("head");body=root.getChild("body");
        right=body.getChild("right");left=body.getChild("left");outerRight=right.getChild("outer");outerLeft=left.getChild("outer");
    }
    public static LayerDefinition layer() {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-3,6,6,6),PartPose.ZERO);
        head.addOrReplaceChild("right_ear",CubeListBuilder.create().texOffs(24,0).addBox(-4,-6,-2,3,4,1),PartPose.ZERO);
        head.addOrReplaceChild("left_ear",CubeListBuilder.create().texOffs(24,0).mirror().addBox(1,-6,-2,3,4,1),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,16).addBox(-3,4,-3,6,12,6).texOffs(0,34).addBox(-5,16,0,10,6,1),PartPose.ZERO);
        var right=body.addOrReplaceChild("right",CubeListBuilder.create().texOffs(42,0).addBox(-12,1,1.5F,10,16,1),PartPose.ZERO);
        right.addOrReplaceChild("outer",CubeListBuilder.create().texOffs(24,16).addBox(-8,1,0,8,12,1),PartPose.offset(-12,1,1.5F));
        var left=body.addOrReplaceChild("left",CubeListBuilder.create().texOffs(42,0).mirror().addBox(2,1,1.5F,10,16,1),PartPose.ZERO);
        left.addOrReplaceChild("outer",CubeListBuilder.create().texOffs(24,16).mirror().addBox(0,1,0,8,12,1),PartPose.offset(12,1,1.5F));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(EntityFireBat bat,float limbSwing,float limbAmount,float age,float yaw,float pitch) {
        head.xRot=pitch/57.295776F;
        if(bat.getIsBatHanging()) {
            head.yRot=Mth.PI-yaw/57.295776F;head.zRot=Mth.PI;head.setPos(0,-2,0);
            right.setPos(-3,0,3);left.setPos(3,0,3);body.xRot=Mth.PI;
            right.xRot=-.15707964F;right.yRot=-1.2566371F;outerRight.yRot=-1.7278761F;
            left.xRot=right.xRot;left.yRot=-right.yRot;outerLeft.yRot=-outerRight.yRot;
        } else {
            head.yRot=yaw/57.295776F;head.zRot=0;head.setPos(0,0,0);
            right.setPos(0,0,0);left.setPos(0,0,0);body.xRot=.7853982F+Mth.cos(age*.1F)*.15F;body.yRot=0;
            // Original leaves each wing's x rotation untouched when leaving the hanging pose.
            right.yRot=Mth.cos(age*1.3F)*Mth.PI*.25F;left.yRot=-right.yRot;
            outerRight.yRot=right.yRot*.5F;outerLeft.yRot=-right.yRot*.5F;
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color) { head.render(pose,out,light,overlay,color);body.render(pose,out,light,overlay,color); }
}
