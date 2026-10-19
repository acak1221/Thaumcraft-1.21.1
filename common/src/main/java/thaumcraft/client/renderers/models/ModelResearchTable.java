package thaumcraft.client.renderers.models;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Original 128x64 furniture and independently textured scroll/tool parts. */
public final class ModelResearchTable {
    public final ModelPart root, inkwell, scrollTube, scrollRibbon;

    public ModelResearchTable() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 32, 4, 16), PartPose.offset(-8, 0, -8));
        int[][] positions = {{-6, 4, -6}, {-6, 4, 2}, {18, 4, -6}, {18, 4, 2}};
        for (int i = 0; i < positions.length; i++) {
            int[] p = positions[i];
            parts.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 24).addBox(0, 0, 0, 4, 12, 4), PartPose.offset(p[0], p[1], p[2]));
        }
        parts.addOrReplaceChild("crossbar", CubeListBuilder.create().texOffs(24, 24).addBox(0, 0, 0, 24, 4, 4), PartPose.offset(-4, 10, -2));
        root = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        inkwell = part(128, 64, 0, 44, 0, 0, 0, 3, 2, 3, PartPose.offset(-6, -2, 3));
        // The original ModelRenderer rotation is 10 radians, not ten degrees.
        scrollTube = part(128, 64, 0, 0, -21, -.5F, -8, 8, 2, 2, PartPose.offsetAndRotation(-2, -2, 2, 0, 10, 0));
        scrollRibbon = part(128, 64, 0, 4, -15.1F, -.275F, -6.75F, 1, 2, 2, PartPose.offsetAndRotation(-2, -2, 2, 0, 10, 0));
    }

    private static ModelPart part(int width, int height, int u, int v, float x, float y, float z,
                                  float dx, float dy, float dz, PartPose pose) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("part", CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, dx, dy, dz), pose);
        return LayerDefinition.create(mesh, width, height).bakeRoot();
    }
}
