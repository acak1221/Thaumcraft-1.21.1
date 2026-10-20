package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Original ModelJar20-33 (oracle {@code thaumcraft/client/renderers/models/ModelJar.java}):
 * the glass core, the brine volume and the lid of a jar on one 64x32 sheet.
 *
 * <p>The original toggles GL blending around the brine and the core. Native
 * 1.21.1 keeps the blend state in the render type, so the caller supplies the
 * translucent buffer for {@link #renderBrine} and {@link #renderCore}; the
 * geometry itself is unchanged.
 *
 * <p>The original assigns {@code mirror=true} AFTER each {@code addBox}, so the
 * built cubes keep their plain UVs.
 */
public final class ModelJar {
    private final ModelPart core, brine, lid;

    public ModelJar() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -12, -5, 10, 12, 10), PartPose.ZERO);
        root.addOrReplaceChild("brine", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -11, -4, 8, 10, 8), PartPose.ZERO);
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 24).addBox(-3, 0, -3, 6, 2, 6), PartPose.offset(0, -14, 0));
        ModelPart parts = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        core = parts.getChild("core");
        brine = parts.getChild("brine");
        lid = parts.getChild("lid");
    }

    /** Original renderBrine38-42: the stored essentia volume only. */
    public void renderBrine(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        brine.render(pose, vertices, light, overlay, -1);
    }

    /** Original renderAll44-50, split: the glass shell. */
    public void renderCore(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        core.render(pose, vertices, light, overlay, -1);
    }

    /** Original renderAll44-50, split: the lid. */
    public void renderLid(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        lid.render(pose, vertices, light, overlay, -1);
    }

    /** The original glass shell part. */
    public ModelPart core() {
        return core;
    }

    /** The original brine part. */
    public ModelPart brine() {
        return brine;
    }

    /** The original lid part. */
    public ModelPart lid() {
        return lid;
    }
}
