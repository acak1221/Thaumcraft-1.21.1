package thaumcraft.client.renderers.models;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Original ModelBellows:21-53 boxes and pivots; bag is scaled only by its renderer. */
public final class ModelBellows {
    public final ModelPart bottom, middle, top, bag, nozzle;
    public ModelBellows() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        // Original mirror/texture-size assignments occur AFTER addBox, so do
        // not retroactively mirror the already-constructed legacy cube UVs.
        parts.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0).addBox(-6, 0, -6, 12, 2, 12), PartPose.offset(0, 22, 0));
        parts.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -1, -6, 12, 2, 12), PartPose.offset(0, 16, 0));
        parts.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0).addBox(-6, 0, -6, 12, 2, 12), PartPose.offset(0, 8, 0));
        parts.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(48, 0).addBox(-10, -12.03333F, -10, 20, 24, 20), PartPose.offset(0, .5F, 0));
        parts.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(0, 36).addBox(-2, -2, 0, 4, 4, 2), PartPose.offset(0, 16, 6));
        var root = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        bottom = root.getChild("bottom"); middle = root.getChild("middle"); top = root.getChild("top");
        bag = root.getChild("bag"); nozzle = root.getChild("nozzle");
    }
}
