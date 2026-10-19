package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Independently mapped original ModelBore22–74 + ModelBoreEmit21–64.
 * Literal cube UVs/pivots, not an approximate block mesh. Original mirror/size
 * changes happened AFTER addBox: constructed boxes use unmirrored128x64 UVs. */
public final class ModelArcaneBore {
    private final ModelPart base, side1, side2, crossbar, front, middle;
    private final ModelPart knob, upperCross, lowerCross, middleCross, rod, jarCore;

    public ModelArcaneBore() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 32).addBox(-6, 0, -6, 12, 2, 12), PartPose.ZERO);
        root.addOrReplaceChild("side1", CubeListBuilder.create().texOffs(0, 0).addBox(-2, 2, -5.5F, 4, 8, 1), PartPose.ZERO);
        root.addOrReplaceChild("side2", CubeListBuilder.create().texOffs(0, 0).addBox(-2, 2, 4.5F, 4, 8, 1), PartPose.ZERO);
        root.addOrReplaceChild("crossbar", CubeListBuilder.create().texOffs(0, 48).addBox(-1, -1, -6, 2, 2, 12), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("front", CubeListBuilder.create().texOffs(30, 14).addBox(4, -2.5F, -2.5F, 4, 5, 5), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 14).addBox(-2, -4, -4, 6, 8, 8), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(66, 0).addBox(-2, 12, -2, 4, 4, 4), PartPose.ZERO);
        root.addOrReplaceChild("upper_cross", CubeListBuilder.create().texOffs(56, 16).addBox(-2, 0, -2, 4, 1, 4), PartPose.offset(0, 8, 0));
        root.addOrReplaceChild("lower_cross", CubeListBuilder.create().texOffs(56, 16).addBox(-2, 0, -2, 4, 1, 4), PartPose.ZERO);
        root.addOrReplaceChild("middle_cross", CubeListBuilder.create().texOffs(56, 24).addBox(-3, 4, -3, 6, 1, 6), PartPose.ZERO);
        root.addOrReplaceChild("rod", CubeListBuilder.create().texOffs(56, 0).addBox(-1, 1, -1, 2, 11, 2), PartPose.ZERO);
        ModelPart parts = LayerDefinition.create(mesh, 128, 64).bakeRoot();
        base = parts.getChild("base"); side1 = parts.getChild("side1"); side2 = parts.getChild("side2");
        crossbar = parts.getChild("crossbar"); front = parts.getChild("front"); middle = parts.getChild("middle");
        knob = parts.getChild("knob"); upperCross = parts.getChild("upper_cross"); lowerCross = parts.getChild("lower_cross");
        middleCross = parts.getChild("middle_cross"); rod = parts.getChild("rod");
        // The bore renders ONLY ModelJar.Core, never its brine or lid.
        var jar = new MeshDefinition();
        jar.getRoot().addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -12, -5, 10, 12, 10), PartPose.ZERO);
        jarCore = LayerDefinition.create(jar, 64, 32).bakeRoot().getChild("core");
    }

    public void renderBase(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        base.render(pose, vertices, light, overlay, -1);
        side1.render(pose, vertices, light, overlay, -1);
        side2.render(pose, vertices, light, overlay, -1);
        crossbar.render(pose, vertices, light, overlay, -1);
    }
    public void renderNozzle(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        front.render(pose, vertices, light, overlay, -1);
        middle.render(pose, vertices, light, overlay, -1);
    }
    public void renderEmitter(boolean focus, PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        if (focus) knob.render(pose, vertices, light, overlay, -1);
        upperCross.render(pose, vertices, light, overlay, -1);
        lowerCross.render(pose, vertices, light, overlay, -1);
        middleCross.render(pose, vertices, light, overlay, -1);
        rod.render(pose, vertices, light, overlay, -1);
    }
    public void renderJar(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        jarCore.render(pose, vertices, light, overlay, -1);
    }
}
