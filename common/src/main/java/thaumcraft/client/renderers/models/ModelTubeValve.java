package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Original ModelTubeValve18-27 (oracle {@code thaumcraft/client/renderers/models/ModelTubeValve.java}):
 * the small valve stem of an essentia tube valve on the 64x32 sheet.
 *
 * <p>The original declares a second, never built {@code ValveRing} field; it is
 * not part of the drawn model and is not reproduced here. The original assigns
 * {@code mirror=true} AFTER the {@code addBox}, so the built cube keeps its
 * plain UVs.
 */
public final class ModelTubeValve {
    private final ModelPart stem;

    public ModelTubeValve() {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("stem", CubeListBuilder.create().texOffs(0, 10).addBox(-1, 2, -1, 2, 2, 2), PartPose.ZERO);
        stem = LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("stem");
    }

    /** Original render29-31. */
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        stem.render(pose, vertices, light, overlay, -1);
    }

    /** The original valve stem part. */
    public ModelPart stem() {
        return stem;
    }
}
