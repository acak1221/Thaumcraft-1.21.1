package thaumcraft.client.renderers.models.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.geometry.HarnessObjMesh;

/**
 * Original ModelHoverHarness49-53 (oracle
 * {@code thaumcraft/client/renderers/models/gear/ModelHoverHarness.java}): the hover
 * harness replaces the body of the biped with one eight by twelve by four chest
 * cube with the original 0.6 expansion and carries the retained OBJ pack on the
 * back.
 *
 * <p>The original ModelBiped keeps its 64x32 sheet for that cube, so the native
 * layer keeps 64x32 even though the retained chest image is 128x64; the
 * normalized UVs are the original ones.
 *
 * <p>The retained {@code HarnessArmorRenderer} already draws this geometry; this
 * class is the canonical port of the original model.
 */
public final class ModelHoverHarness {
    public static final ResourceLocation CHEST = texture("textures/models/hoverharness.png");
    public static final ResourceLocation BACK = texture("textures/models/hoverharness2.png");
    public static final ResourceLocation MESH = texture("textures/models/hoverharness.obj");
    private final ModelPart body;
    private HarnessObjMesh back;

    public ModelHoverHarness() {
        var mesh = new MeshDefinition();
        // Original ModelHoverHarness50-51: the body cube at texture offset16,16
        // with the original 0.6 expansion.
        mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4, new CubeDeformation(.6F)), PartPose.ZERO);
        body = LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("body");
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    /** The chest cube of the original. */
    public ModelPart body() {
        return body;
    }

    /** The retained back pack mesh, loaded once per resource reload. */
    public HarnessObjMesh back() {
        if (back == null) {
            try (var input = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).open()) {
                back = HarnessObjMesh.read(new InputStreamReader(input, StandardCharsets.UTF_8));
            } catch (IOException invalid) {
                throw new IllegalStateException("Cannot load the retained hover harness mesh", invalid);
            }
        }
        return back;
    }

    /** Drops the cached mesh, for a resource reload. */
    public void reset() {
        back = null;
    }

    /** Draws the chest cube of the original. */
    public void renderBody(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        body.render(pose, vertices, light, overlay, -1);
    }
}
