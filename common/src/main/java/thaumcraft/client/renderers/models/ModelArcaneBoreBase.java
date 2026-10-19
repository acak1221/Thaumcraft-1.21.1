package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Original ModelBoreBase25–99: two plates, five pillars and two nozzle pieces.
 * Render order is explicit, not dependent on native child-map iteration. */
public final class ModelArcaneBoreBase {
    private final ModelPart bottom, top, center, northwest, southwest, southeast, northeast, nozzle, lip;

    public ModelArcaneBoreBase() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(64, 24).addBox(-8, 0, -8, 16, 2, 16), PartPose.ZERO);
        root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(64, 24).addBox(-8, 0, -8, 16, 2, 16), PartPose.offset(0, 14, 0));
        root.addOrReplaceChild("center", CubeListBuilder.create().texOffs(84, 42).addBox(-2.5F, 0, -2.5F, 5, 12, 5), PartPose.offset(0, 2, 0));
        root.addOrReplaceChild("northwest", CubeListBuilder.create().texOffs(64, 42).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-5, 2, -5));
        root.addOrReplaceChild("southwest", CubeListBuilder.create().texOffs(64, 42).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-5, 2, 5));
        root.addOrReplaceChild("southeast", CubeListBuilder.create().texOffs(64, 42).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(5, 2, 5));
        root.addOrReplaceChild("northeast", CubeListBuilder.create().texOffs(64, 42).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(5, 2, -5));
        root.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(106, 42).addBox(2.5F, -2, -2, 5, 4, 4), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("lip", CubeListBuilder.create().texOffs(106, 51).addBox(7, -2.5F, -2.5F, 1, 5, 5), PartPose.offset(0, 8, 0));
        ModelPart parts = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        bottom = parts.getChild("bottom"); top = parts.getChild("top"); center = parts.getChild("center");
        northwest = parts.getChild("northwest"); southwest = parts.getChild("southwest"); southeast = parts.getChild("southeast"); northeast = parts.getChild("northeast");
        nozzle = parts.getChild("nozzle"); lip = parts.getChild("lip");
    }

    public void renderBase(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        bottom.render(pose, vertices, light, overlay, -1); top.render(pose, vertices, light, overlay, -1);
        center.render(pose, vertices, light, overlay, -1);
        northwest.render(pose, vertices, light, overlay, -1); southwest.render(pose, vertices, light, overlay, -1);
        southeast.render(pose, vertices, light, overlay, -1); northeast.render(pose, vertices, light, overlay, -1);
    }
    public void renderNozzle(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        nozzle.render(pose, vertices, light, overlay, -1); lip.render(pose, vertices, light, overlay, -1);
    }
}
