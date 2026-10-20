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
import thaumcraft.common.entities.monster.EntityPech;

/** Original128x64 nine-part mesh, independent jaw/pack movement, riding,
 * crouching and right-arm attack animation. None of the packs is an item layer. */
public final class ModelPech extends EntityModel<EntityPech> {
    private final ModelPart root,body,rightLeg,leftLeg,head,jowls,lowerPack,upperPack,rightArm,leftArm;
    public ModelPech(){root=layer().bakeRoot();body=root.getChild("body");rightLeg=root.getChild("right_leg");leftLeg=root.getChild("left_leg");head=root.getChild("head");jowls=root.getChild("jowls");lowerPack=root.getChild("lower_pack");upperPack=root.getChild("upper_pack");rightArm=root.getChild("right_arm");leftArm=root.getChild("left_arm");}
    public ModelPart rightArm(){return rightArm;}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(34,12).mirror().addBox(-3,0,0,6,10,6),PartPose.offsetAndRotation(0,9,-3,.3129957F,0,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(35,1).addBox(-2.9F,0,0,3,6,3),PartPose.offset(0,18,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(35,1).mirror().addBox(-.1F,0,0,3,6,3),PartPose.offset(0,18,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(2,11).mirror().addBox(-3.5F,-5,-5,7,5,5),PartPose.offset(0,8,0));
        root.addOrReplaceChild("jowls",CubeListBuilder.create().texOffs(1,21).mirror().addBox(-4,-1,-6,8,3,5),PartPose.offset(0,8,0));
        root.addOrReplaceChild("lower_pack",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-5,0,0,10,5,5),PartPose.offsetAndRotation(0,10,3.5F,.3013602F,0,0));
        root.addOrReplaceChild("upper_pack",CubeListBuilder.create().texOffs(64,1).mirror().addBox(-7.5F,-14,0,15,14,11),PartPose.offsetAndRotation(0,10,3,.4537856F,0,0));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(52,2).addBox(-2,0,-1,2,6,2),PartPose.offset(-3,10,-1));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(52,2).mirror().addBox(0,0,-1,2,6,2),PartPose.offset(3,10,-1));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void setupAnim(EntityPech pech,float limb,float amount,float age,float yaw,float pitch){
        head.yRot=yaw/57.295776F;head.xRot=pitch/57.295776F;jowls.yRot=head.yRot;
        jowls.xRot=head.xRot+(.2617994F+Mth.cos(limb*.6662F)*amount*.25F)+.34906587F*Math.abs(Mth.sin(pech.mumble));
        rightArm.xRot=Mth.cos(limb*.6662F+Mth.PI)*2*amount*.5F;leftArm.xRot=Mth.cos(limb*.6662F)*2*amount*.5F;rightArm.zRot=leftArm.zRot=0;
        rightLeg.xRot=Mth.cos(limb*.6662F)*1.4F*amount;leftLeg.xRot=Mth.cos(limb*.6662F+Mth.PI)*1.4F*amount;rightLeg.yRot=leftLeg.yRot=0;
        lowerPack.yRot=lowerPack.zRot=Mth.cos(limb*.6662F)*2*amount*.125F;
        if(riding){rightArm.xRot-=.62831855F;leftArm.xRot-=.62831855F;rightLeg.xRot=leftLeg.xRot=-1.2566371F;rightLeg.yRot=.31415927F;leftLeg.yRot=-.31415927F;}
        rightArm.yRot=leftArm.yRot=0;
        if(attackTime> -9990){
            rightArm.yRot+=body.yRot;leftArm.yRot+=body.yRot;leftArm.xRot+=body.yRot;
            float wave=1-attackTime;wave*=wave;wave*=wave;wave=1-wave;
            float swing=Mth.sin(wave*Mth.PI),headSwing=Mth.sin(attackTime*Mth.PI)*-(head.xRot-.7F)*.75F;
            rightArm.xRot=(float)(rightArm.xRot-(swing*1.2+headSwing));rightArm.yRot+=body.yRot*2;rightArm.zRot=Mth.sin(attackTime*Mth.PI)*-.4F;
        }
        if(pech.isShiftKeyDown()){rightArm.xRot+=.4F;leftArm.xRot+=.4F;}
        rightArm.zRot+=Mth.cos(age*.09F)*.05F+.05F;leftArm.zRot-=Mth.cos(age*.09F)*.05F+.05F;
        rightArm.xRot+=Mth.sin(age*.067F)*.05F;leftArm.xRot-=Mth.sin(age*.067F)*.05F;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){root.render(pose,out,light,overlay,color);}
}
