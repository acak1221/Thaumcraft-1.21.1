package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Original ModelManaPod21-40 (oracle {@code thaumcraft/client/renderers/models/ModelManaPod.java}):
 * the three growth stages of a mana pod on the 32x32 sheet.
 *
 * <p>The original assigns {@code mirror=true} AFTER each {@code addBox}; 1.7.10
 * reads that flag while the box is built, so the constructed cubes keep their
 * plain UVs and no part uses native {@code .mirror()} here.
 */
public final class ModelManaPod {
    private final ModelPart pod0, pod1, pod2;

    public ModelManaPod() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("pod0", CubeListBuilder.create().texOffs(0, 0).addBox(-2, 0, -2, 4, 5, 4), PartPose.ZERO);
        root.addOrReplaceChild("pod1", CubeListBuilder.create().texOffs(0, 0).addBox(-3, 0, -3, 6, 7, 6), PartPose.ZERO);
        root.addOrReplaceChild("pod2", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 0, -3.5F, 7, 9, 7), PartPose.ZERO);
        ModelPart parts = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        pod0 = parts.getChild("pod0");
        pod1 = parts.getChild("pod1");
        pod2 = parts.getChild("pod2");
    }

    /** The first growth stage, 4x5x4. */
    public void renderFirst(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        pod0.render(pose, vertices, light, overlay, -1);
    }

    /** The second growth stage, 6x7x6. */
    public void renderSecond(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        pod1.render(pose, vertices, light, overlay, -1);
    }

    /** The final growth stage, 7x9x7. */
    public void renderThird(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        pod2.render(pose, vertices, light, overlay, -1);
    }

    /** The first growth stage, 4x5x4. */
    public ModelPart pod0() {
        return pod0;
    }

    /** The second growth stage, 6x7x6. */
    public ModelPart pod1() {
        return pod1;
    }

    /** The final growth stage, 7x9x7. */
    public ModelPart pod2() {
        return pod2;
    }
}
