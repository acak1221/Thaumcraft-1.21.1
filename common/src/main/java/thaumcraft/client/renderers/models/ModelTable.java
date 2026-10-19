package thaumcraft.client.renderers.models;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** TC4 4.2.3.5 table boxes, pivots and 64x32 UVs, in model pixels. */
public final class ModelTable {
    public final ModelPart root;

    public ModelTable() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 16, 4, 16), PartPose.offset(-8, 0, -8));
        parts.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(0, 20).addBox(0, 0, 0, 4, 8, 4), PartPose.offset(2, 4, -2));
        parts.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(0, 20).addBox(0, 0, 0, 4, 8, 4), PartPose.offset(-6, 4, -2));
        parts.addOrReplaceChild("crossbar", CubeListBuilder.create().texOffs(16, 20).addBox(0, 0, 0, 16, 4, 8), PartPose.offset(-8, 12, -4));
        // The original sets mirror after addBox; those already-created cubes are not mirrored.
        root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
