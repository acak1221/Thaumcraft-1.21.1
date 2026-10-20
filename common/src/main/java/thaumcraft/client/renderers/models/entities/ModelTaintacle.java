package thaumcraft.client.renderers.models.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.lib.taint.TaintacleRules;

/** Original64x64 mesh, nested .88 taper, separate luminous head/orb and rising animation. */
public final class ModelTaintacle extends EntityModel<EntityTaintacle> {
    private final ModelPart root;
    private final ModelPart[] segments;
    private float height=3,arise;
    public ModelTaintacle(int length){
        super(RenderType::entityTranslucent);
        if(length<2)throw new IllegalArgumentException("A tentacle needs at least two segments");
        root=layer(length).bakeRoot().getChild("tentacle");segments=new ModelPart[length-1];
        ModelPart parent=root;
        for(int k=0;k<segments.length;k++){
            var scale=parent.getChild("taper_"+k);scale.xScale=scale.yScale=scale.zScale=.88F;
            segments[k]=scale.getChild("segment_"+k);parent=segments[k];
        }
        for(String leaf:new String[]{"orb","head"}){var taper=parent.getChild("taper_"+leaf);taper.xScale=taper.yScale=taper.zScale=.88F;}
    }
    public static LayerDefinition layer(int length){
        var mesh=new MeshDefinition();
        var stem=mesh.getRoot().addOrReplaceChild("tentacle",cube(0,0,4),PartPose.offset(0,12,0));
        for(int k=0;k<length-1;k++)stem=taper(stem,"taper_"+k).addOrReplaceChild("segment_"+k,cube(0,16,4),PartPose.offset(0,-8,0));
        taper(stem,"taper_orb").addOrReplaceChild("orb",cube(0,56,2),PartPose.offset(0,-8,0));
        taper(stem,"taper_head").addOrReplaceChild("head",cube(0,32,6),PartPose.offset(0,-8,0));
        return LayerDefinition.create(mesh,64,64);
    }
    private static CubeListBuilder cube(int u,int v,int radius){return CubeListBuilder.create().texOffs(u,v).addBox(-radius,-radius,-radius,radius*2,radius*2,radius*2);}
    // A wrapper is necessary: original scales BEFORE the child's translation, while ModelPart scales after its own translation.
    private static PartDefinition taper(PartDefinition parent,String name){return parent.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.ZERO);}
    @Override public void setupAnim(EntityTaintacle entity,float limb,float amount,float age,float yaw,float pitch){
        height=entity.getBbHeight();arise=TaintacleRules.ariseHeight(entity.tickCount,height);root.xRot=0;
        var rotations=TaintacleRules.segments(segments.length+1,age,entity.flailIntensity,entity.getAgitationState(),entity.hurtTime>0||entity.originalAttackTime()>0);
        for(int k=0;k<segments.length;k++){segments[k].xRot=rotations.get(k).pitch();segments[k].zRot=rotations.get(k).roll();}
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){
        pose.pushPose();
        try{pose.translate(0,(height==3F?.6F:1.2F)+arise,0);float scale=height/3F;pose.scale(scale,scale,scale);ModelRendererTaintacle.render(root,pose,out,light,overlay,color);}
        finally{pose.popPose();}
    }
}
