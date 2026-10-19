package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** TC4 ModelCube(int):27-38; the two halves of the original 64x64 infuser texture. */
public final class ModelCube {
    private final ModelPart cube;

    public ModelCube(int textureShift) {
        if (textureShift != 0 && textureShift != 32) throw new IllegalArgumentException("Infuser cube texture half");
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, textureShift)
                .addBox(-8, -8, -8, 16, 16, 16), PartPose.ZERO);
        // Legacy mirror=true was assigned AFTER addBox: it did not change the
        // constructed box's UVs. No extra global model-layer registration needed.
        cube = LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("cube");
    }

    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, int argb) {
        cube.render(pose, buffer, light, overlay, argb);
    }
}
