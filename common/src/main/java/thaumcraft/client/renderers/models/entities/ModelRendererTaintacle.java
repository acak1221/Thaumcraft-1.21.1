package thaumcraft.client.renderers.models.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;

/** Native mesh traversal keeps original fullbright leaf cubes without leaking light into the stem. */
public final class ModelRendererTaintacle {
    private ModelRendererTaintacle(){}
    public static void render(ModelPart root,PoseStack pose,VertexConsumer out,int light,int overlay,int color){
        root.visit(pose,(current,path,index,cube)->cube.compile(current,out,
                path.endsWith("/orb")||path.endsWith("/head")?LightTexture.FULL_BRIGHT:light,overlay,color));
    }
}
