package thaumcraft.client.renderers.models.gear;

import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Fits native trim UV regions onto existing custom geometry without changing its original sheet. */
final class ArmorTrimModelGeometry {
    record Fit(String pivot, String name, float xScale, float yScale, float zScale) {}
    private ArmorTrimModelGeometry() {}

    static void fit(PartDefinition root, List<Fit> fits, String pivot, String name, int u, int v, boolean mirror,
                    float sourceX, float sourceY, float sourceZ, float sourceWidth, float sourceHeight, float sourceDepth,
                    float x, float y, float z, float width, float height, float depth, float xRot, float yRot, float zRot) {
        float sx = width / sourceWidth, sy = height / sourceHeight, sz = depth / sourceDepth;
        PartDefinition plate = root.getChild(pivot).addOrReplaceChild(name, CubeListBuilder.create(), PartPose.rotation(xRot, yRot, zRot));
        plate.addOrReplaceChild("surface", CubeListBuilder.create().texOffs(u, v).mirror(mirror)
                        .addBox(sourceX, sourceY, sourceZ, sourceWidth, sourceHeight, sourceDepth, new CubeDeformation(.008F)),
                PartPose.offset(x - sourceX * sx, y - sourceY * sy, z - sourceZ * sz));
        fits.add(new Fit(pivot, name, sx, sy, sz));
    }

    static ModelPart bake(MeshDefinition mesh, List<Fit> fits) {
        ModelPart root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        for (Fit fit : fits) {
            ModelPart surface = root.getChild(fit.pivot).getChild(fit.name).getChild("surface");
            surface.xScale = fit.xScale; surface.yScale = fit.yScale; surface.zScale = fit.zScale;
        }
        return root;
    }

    static void copyPartPoses(ModelGearArmor source, ModelGearArmor target, List<Fit> fits) {
        for (Fit fit : fits) {
            ModelPart originalParent = pivot(source, fit.pivot), trim = pivot(target, fit.pivot).getChild(fit.name);
            if (!originalParent.hasChild(fit.name)) { trim.visible = false; continue; }
            ModelPart original = originalParent.getChild(fit.name);
            trim.copyFrom(original);
            trim.visible = original.visible;
        }
    }

    private static ModelPart pivot(ModelGearArmor model, String name) {
        return switch (name) {
            case "head" -> model.head;
            case "body" -> model.body;
            case "right_arm" -> model.rightArm;
            case "left_arm" -> model.leftArm;
            case "right_leg" -> model.rightLeg;
            case "left_leg" -> model.leftLeg;
            default -> throw new IllegalArgumentException("Unknown trim pivot " + name);
        };
    }
}
