package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Original ModelBrain21-40 (oracle {@code thaumcraft/client/renderers/models/ModelBrain.java}):
 * the brainy zombie's three brain shapes on the 128x64 sheet.
 *
 * <p>The original assigns {@code mirror=true} AFTER each {@code addBox}, so the
 * built cubes keep their plain UVs; native {@code .mirror()} would flip them.
 * Shape3 keeps the original {@code 0.4089647} X tilt.
 */
public final class ModelBrain {
    private final ModelPart shape1, shape2, shape3;

    public ModelBrain() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 12, 10, 16), PartPose.offset(-6, 8, -8));
        root.addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(64, 0).addBox(0, 0, 0, 8, 3, 7), PartPose.offset(-4, 18, 0));
        root.addOrReplaceChild("shape3", CubeListBuilder.create().texOffs(0, 32).addBox(0, 0, 0, 2, 6, 2), PartPose.offsetAndRotation(-1, 18, -2, .4089647F, 0, 0));
        ModelPart parts = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        shape1 = parts.getChild("shape1");
        shape2 = parts.getChild("shape2");
        shape3 = parts.getChild("shape3");
    }

    /** Original render42-46: the three shapes in the original order. */
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        shape1.render(pose, vertices, light, overlay, -1);
        shape2.render(pose, vertices, light, overlay, -1);
        shape3.render(pose, vertices, light, overlay, -1);
    }

    /** The original first brain shape. */
    public ModelPart shape1() {
        return shape1;
    }

    /** The original second brain shape. */
    public ModelPart shape2() {
        return shape2;
    }

    /** The original third brain shape, the tilted stalk. */
    public ModelPart shape3() {
        return shape3;
    }
}
