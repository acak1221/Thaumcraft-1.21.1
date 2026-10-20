package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Original ModelCentrifuge20-58 (oracle {@code thaumcraft/client/renderers/models/ModelCentrifuge.java}):
 * the fixed bowls and the spinning cross of the essentia centrifuge, on the
 * retained 64x32 world sheet.
 *
 * <p>The original assigns {@code mirror=true} AFTER every {@code addBox}, so the
 * constructed cubes keep their plain UVs and native {@code .mirror()} is not used.
 */
public final class ModelCentrifuge {
    private final ModelPart crossbar, dingus1, dingus2, core, top, bottom;

    public ModelCentrifuge() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("crossbar", CubeListBuilder.create().texOffs(16, 0).addBox(-4, -1, -1, 8, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("dingus1", CubeListBuilder.create().texOffs(0, 16).addBox(4, -3, -2, 4, 6, 4), PartPose.ZERO);
        root.addOrReplaceChild("dingus2", CubeListBuilder.create().texOffs(0, 16).addBox(-8, -3, -2, 4, 6, 4), PartPose.ZERO);
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -4, -1.5F, 3, 8, 3), PartPose.ZERO);
        root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(20, 16).addBox(-4, -8, -4, 8, 4, 8), PartPose.ZERO);
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(20, 16).addBox(-4, 4, -4, 8, 4, 8), PartPose.ZERO);
        ModelPart parts = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        crossbar = parts.getChild("crossbar");
        dingus1 = parts.getChild("dingus1");
        dingus2 = parts.getChild("dingus2");
        core = parts.getChild("core");
        top = parts.getChild("top");
        bottom = parts.getChild("bottom");
    }

    /** Original renderBoxes60-64: the two fixed bowls. */
    public void renderBoxes(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        top.render(pose, vertices, light, overlay, -1);
        bottom.render(pose, vertices, light, overlay, -1);
    }

    /** Original renderSpinnyBit66-71: the rotating cross, arms and core. */
    public void renderSpinnyBit(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        crossbar.render(pose, vertices, light, overlay, -1);
        dingus1.render(pose, vertices, light, overlay, -1);
        dingus2.render(pose, vertices, light, overlay, -1);
        core.render(pose, vertices, light, overlay, -1);
    }

    /** The original crossbar of the rotor. */
    public ModelPart crossbar() {
        return crossbar;
    }

    /** The original rotor core. */
    public ModelPart rotor() {
        return core;
    }

    /** The original upper bowl. */
    public ModelPart top() {
        return top;
    }

    /** The original lower bowl. */
    public ModelPart bottom() {
        return bottom;
    }
}
