package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Original ModelBanner21-52 (oracle {@code thaumcraft/client/renderers/models/ModelBanner.java}):
 * the cultist banner's two tabs, beam, cloth and pole on one 128x64 sheet.
 *
 * <p>The original assigns {@code mirror=true} to every part AFTER its
 * {@code addBox}; 1.7.10's ModelBox reads that flag while the box is built, so
 * the already constructed cubes are NOT mirrored. Native {@code CubeListBuilder}
 * mirrors the cube it builds, therefore no part uses {@code .mirror()} here.
 */
public final class ModelBanner {
    private final ModelPart tabLeft, tabRight, beam, banner, pole;

    public ModelBanner() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("tab_left", CubeListBuilder.create().texOffs(0, 29).addBox(-5, -7.5F, -1.5F, 2, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("tab_right", CubeListBuilder.create().texOffs(0, 29).addBox(3, -7.5F, -1.5F, 2, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(30, 0).addBox(-7, -7, -1, 14, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(0, 0).addBox(-7, 0, -.5F, 14, 28, 1), PartPose.offset(0, -5, 0));
        root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(62, 0).addBox(0, 0, -1, 2, 31, 2), PartPose.offset(-1, -7, -2));
        ModelPart parts = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        tabLeft = parts.getChild("tab_left");
        tabRight = parts.getChild("tab_right");
        beam = parts.getChild("beam");
        banner = parts.getChild("banner");
        pole = parts.getChild("pole");
    }

    /** Original renderPole55-57. */
    public void renderPole(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        pole.render(pose, vertices, light, overlay, -1);
    }

    /** The original pole part. */
    public ModelPart pole() {
        return pole;
    }

    /** The original beam part. */
    public ModelPart beam() {
        return beam;
    }

    /** The original banner cloth part. */
    public ModelPart banner() {
        return banner;
    }

    /** The original left tab part. */
    public ModelPart tabLeft() {
        return tabLeft;
    }

    /** The original right tab part. */
    public ModelPart tabRight() {
        return tabRight;
    }

    /** Original renderBeam59-61. */
    public void renderBeam(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        beam.render(pose, vertices, light, overlay, -1);
    }

    /** Original renderTabs63-65: the two banner tabs. */
    public void renderTabs(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        tabLeft.render(pose, vertices, light, overlay, -1);
        tabRight.render(pose, vertices, light, overlay, -1);
    }

    /** Original renderBanner67-69. */
    public void renderBanner(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        banner.render(pose, vertices, light, overlay, -1);
    }
}
