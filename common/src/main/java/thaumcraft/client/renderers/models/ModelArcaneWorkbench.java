package thaumcraft.client.renderers.models;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** TC4 4.2.3.5 ModelArcaneWorkbench:22-68, original boxes/pivots and 128x64 UVs. */
public final class ModelArcaneWorkbench {
    public final ModelPart root;

    public ModelArcaneWorkbench() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 16, 8, 16), PartPose.offset(-8, 0, -8));
        parts.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 32).addBox(0, 0, 0, 16, 4, 16), PartPose.offset(-8, 12, -8));
        parts.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(72, 0).addBox(0, 0, 0, 4, 4, 4), PartPose.offset(3, 8, -7));
        parts.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(72, 0).addBox(0, 0, 0, 4, 4, 4), PartPose.offset(-7, 8, 3));
        parts.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(72, 0).addBox(0, 0, 0, 4, 4, 4), PartPose.offset(-7, 8, -7));
        parts.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(72, 0).addBox(0, 0, 0, 4, 4, 4), PartPose.offset(3, 8, 3));
        // Original sets mirror after addBox; the existing cubes were not mirrored.
        root = LayerDefinition.create(mesh, 128, 64).bakeRoot();
    }
}
