package thaumcraft.client.renderers.models.gear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;

/** Native trim regions fitted to the hood, sleeves and articulated robe panels. */
public final class ModelRobeTrim extends ModelGearArmor {
    private final List<ArmorTrimModelGeometry.Fit> panels;

    private ModelRobeTrim(ModelPart root, List<ArmorTrimModelGeometry.Fit> panels) {
        super(root);
        this.panels = List.copyOf(panels);
    }

    public static ModelRobeTrim create(boolean armoured) {
        var mesh = new MeshDefinition();
        var root = biped(mesh.getRoot());
        List<ArmorTrimModelGeometry.Fit> fits = new ArrayList<>();
        fit(root, fits, "head", "hood1", 0, 0, false,
                -4, -8, -4, 8, 8, 8, -4.5F, -9, -4.6F, 9, 9, 9);

        // The belt uses the bottom five rows of the native torso. Books,
        // scrolls, the foci pouch and the pointed hood remain on their own sheet.
        fit(root, fits, "body", "mbelt", 16, 23, false,
                -4, 0, -2, 8, 5, 4, -4, 7, -3, 8, 5, 1);
        fit(root, fits, "body", "mbelt_b", 16, 23, false,
                -4, 0, -2, 8, 5, 4, -4, 7, -4, 8, 5, 1);
        fit(root, fits, "body", "mbelt_l", 16, 25, false,
                -4, 0, -2, 8, 3, 4, 4, 8, -3, 1, 3, 6);
        fit(root, fits, "body", "mbelt_r", 16, 25, false,
                -4, 0, -2, 8, 3, 4, -5, 8, -3, 1, 3, 6);

        if (!armoured) {
            // Split the native torso front/back across the actual moving cloth
            // segments. Lower segments continue the same UV rows; they do not
            // repeat an entire pattern on every articulated piece.
            cloth(root, fits, "frontcloth_r1", 16, 16, 4, 9, 0, 0, 0, 3, 8, 1);
            cloth(root, fits, "frontcloth_l1", 20, 16, 4, 9, 0, 0, 0, 3, 8, 1);
            cloth(root, fits, "frontcloth_r2", 16, 25, 4, 3, 0, 7.5F, 1.7F, 3, 3, 1);
            cloth(root, fits, "frontcloth_l2", 20, 25, 4, 3, 0, 7.5F, 1.7F, 3, 3, 1);
            cloth(root, fits, "clothback_r1", 20, 16, 4, 9, 0, 0, 0, 4, 8, 1);
            cloth(root, fits, "clothback_l1", 24, 16, 4, 9, 0, 0, 0, 4, 8, 1);
            cloth(root, fits, "clothback_r2", 23, 25, 1, 3, 0, 7.8F, -.9F, 1, 2, 1);
            cloth(root, fits, "clothback_r3", 22, 25, 3, 3, 1, 7.8F, -.9F, 3, 3, 1);
            cloth(root, fits, "clothback_l3", 25, 25, 3, 3, 0, 7.8F, -.9F, 3, 3, 1);
            cloth(root, fits, "clothback_l2", 30, 25, 1, 3, 3, 7.8F, -.9F, 1, 2, 1);
        }

        for (boolean left : new boolean[]{false, true}) {
            String arm = left ? "left_arm" : "right_arm";
            float sourceX = left ? -1 : -3, x = left ? -1.5F : -3.5F;
            fit(root, fits, arm, left ? "shoulder_l" : "shoulder_r", 40, 16, left,
                    sourceX, -2, -2, 4, 5, 4, x, -2.5F, -2.5F, 5, 5, 5);
            fit(root, fits, arm, left ? "larm1" : "rarm1", 40, 21, left,
                    sourceX, 0, -2, 4, 7, 4, x, 2.5F, -2.5F, 5, 7, 5);

            String leg = left ? "left_leg" : "right_leg", suffix = left ? "l" : "r";
            float frontX = left ? 1 : -3;
            leggings(root, fits, leg, "legpanel_" + suffix + "4", left, 0, frontX, .5F, -3.5F, 2, 3, 1);
            leggings(root, fits, leg, "legpanel_" + suffix + "5", left, 3, frontX, 2.5F, -2.5F, 2, 3, 1);
            leggings(root, fits, leg, "legpanel_" + suffix + "6", left, 6, frontX, 4.5F, -1.5F, 2, 3, 1);
            leggings(root, fits, leg, "sidecloth_" + suffix + "1", left, 0, left ? 1.5F : -2.5F, .5F, -2.5F, 1, 5, 5);
            leggings(root, fits, leg, "sidecloth_" + suffix + "2", left, 5, left ? .5F : -1.5F, 5.5F, -2.5F, 1, 3, 5);
            leggings(root, fits, leg, "sidecloth_" + suffix + "3", left, 8, left ? -1.4F : .4F, 8.4F, -2.5F, 1, 3, 5);
        }
        return new ModelRobeTrim(ArmorTrimModelGeometry.bake(mesh, fits), fits);
    }

    public void copyPanelPoses(ModelRobe source) {
        // ModelRobe animates its child cloths, independently of the body pivot.
        ArmorTrimModelGeometry.copyPartPoses(source, this, panels);
    }

    private static void cloth(PartDefinition root, List<ArmorTrimModelGeometry.Fit> fits, String name,
                              int u, int v, float sourceWidth, float sourceHeight,
                              float x, float y, float z, float width, float height, float depth) {
        fit(root, fits, "body", name, u, v, false, 0, 0, 0, sourceWidth, sourceHeight, 4, x, y, z, width, height, depth);
    }

    private static void leggings(PartDefinition root, List<ArmorTrimModelGeometry.Fit> fits, String pivot, String name,
                                 boolean mirror, int patternY, float x, float y, float z, float width, float height, float depth) {
        fit(root, fits, pivot, name, 0, 16 + patternY, mirror, -2, 0, -2, 4, height, 4, x, y, z, width, height, depth);
    }

    private static void fit(PartDefinition root, List<ArmorTrimModelGeometry.Fit> fits, String pivot, String name,
                            int u, int v, boolean mirror, float sourceX, float sourceY, float sourceZ,
                            float sourceWidth, float sourceHeight, float sourceDepth,
                            float x, float y, float z, float width, float height, float depth) {
        ArmorTrimModelGeometry.fit(root, fits, pivot, name, u, v, mirror, sourceX, sourceY, sourceZ,
                sourceWidth, sourceHeight, sourceDepth, x, y, z, width, height, depth, 0, 0, 0);
    }

    @Override public void showSlot(EquipmentSlot slot) {
        super.showSlot(slot);
        // The inner texture belongs to greaves, not the torso rendered by the
        // original armoured robe variant alongside those greaves.
        body.visible = slot == EquipmentSlot.CHEST;
        hat.visible = false;
    }
}
