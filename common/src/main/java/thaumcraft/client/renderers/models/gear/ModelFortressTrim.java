package thaumcraft.client.renderers.models.gear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Native 64x32 armour-trim UVs fitted to the fortress's exposed metal plates.
 * The original 128x64 sheet is not a trim layout: drawing a trim over that UV map
 * would put arbitrary fragments on masks, goggles, books and scrolls. This skin
 * follows the same pivots and plate rotations while leaving those accessories alone.
 */
public final class ModelFortressTrim extends ModelGearArmor {
    private final List<ArmorTrimModelGeometry.Fit> plates;

    private ModelFortressTrim(ModelPart root, List<ArmorTrimModelGeometry.Fit> plates) { super(root); this.plates = List.copyOf(plates); }

    public static ModelFortressTrim create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = biped(mesh.getRoot());
        List<ArmorTrimModelGeometry.Fit> fits = new ArrayList<>();
        // The complete vanilla helmet pattern fits the fortress's central helmet band.
        ArmorTrimModelGeometry.fit(root, fits, "head", "helmet", 0, 0, false,
                -4, -8, -4, 8, 8, 8, -4.5F, -9, -4.5F, 9, 4, 9, 0, 0, 0);
        ArmorTrimModelGeometry.fit(root, fits, "body", "chestplate", 16, 16, false,
                -4, 0, -2, 8, 12, 4, -4, 1, -4, 8, 7, 2, 0, 0, 0);
        ArmorTrimModelGeometry.fit(root, fits, "body", "backplate", 16, 16, false,
                -4, 0, -2, 8, 12, 4, -4, 1, 2, 8, 11, 2, 0, 0, 0);
        ArmorTrimModelGeometry.fit(root, fits, "right_arm", "shoulder_r", 40, 16, false,
                -3, -2, -2, 4, 12, 4, -3.5F, -2.5F, -2.5F, 5, 5, 5, 0, 0, 0);
        ArmorTrimModelGeometry.fit(root, fits, "left_arm", "shoulder_l", 40, 16, true,
                -1, -2, -2, 4, 12, 4, -1.5F, -2.5F, -2.5F, 5, 5, 5, 0, 0, 0);
        // Each articulated strip samples its corresponding height of the native
        // leggings pattern rather than repeating the entire pattern on every strip.
        for (boolean left : new boolean[]{false, true}) {
            String pivot = left ? "left_leg" : "right_leg";
            float frontX = left ? -2 : -1;
            leg(root, fits, pivot, "legpanel_" + (left ? "l" : "r") + 1, left, 0, frontX, .5F, -3.5F, 3, 4, 1, -.4363323F, 0, 0);
            leg(root, fits, pivot, "legpanel_" + (left ? "l" : "r") + 2, left, 4, frontX, 3.5F, -2.5F, 3, 4, 1, -.4363323F, 0, 0);
            leg(root, fits, pivot, "legpanel_" + (left ? "l" : "r") + 3, left, 8, frontX, 6.5F, -1.5F, 3, 3, 1, -.4363323F, 0, 0);
            float sideX = left ? 1.5F : -2.5F;
            float sideAngle = left ? -.4363323F : .4363323F;
            leg(root, fits, pivot, "sidepanel_" + (left ? "l" : "r") + 1, left, 0, sideX, .5F, -2.5F, 1, 4, 5, 0, 0, sideAngle);
            leg(root, fits, pivot, "sidepanel_" + (left ? "l" : "r") + 2, left, 4, sideX + (left ? -1 : 1), 3.5F, -2.5F, 1, 3, 5, 0, 0, sideAngle);
            leg(root, fits, pivot, "sidepanel_" + (left ? "l" : "r") + 3, left, 7, sideX + (left ? -2 : 2), 5.5F, -2.5F, 1, 3, 5, 0, 0, sideAngle);
            float backX = left ? -2 : -3;
            leg(root, fits, pivot, "backpanel_" + (left ? "l" : "r") + 1, left, 0, backX, .5F, 2.5F, 5, 3, 1, .4363323F, 0, 0);
            leg(root, fits, pivot, "backpanel_" + (left ? "l" : "r") + 2, left, 3, backX, 2.5F, 1.5F, 5, 3, 1, .4363323F, 0, 0);
            leg(root, fits, pivot, "backpanel_" + (left ? "l" : "r") + 3, left, 6, backX, 4.5F, .5F, 5, 3, 1, .4363323F, 0, 0);
        }
        return new ModelFortressTrim(ArmorTrimModelGeometry.bake(mesh, fits), fits);
    }

    public void copyPlatePoses(ModelFortressArmor source) {
        ArmorTrimModelGeometry.copyPartPoses(source, this, plates);
    }

    private static void leg(PartDefinition root, List<ArmorTrimModelGeometry.Fit> fits, String pivot, String name, boolean mirror,
                            int patternY, float x, float y, float z, float width, float height, float depth,
                            float xRot, float yRot, float zRot) {
        ArmorTrimModelGeometry.fit(root, fits, pivot, name, 0, 16 + patternY, mirror,
                -2, 0, -2, 4, height, 4, x, y, z, width, height, depth, xRot, yRot, zRot);
    }

    @Override public void showSlot(EquipmentSlot slot) {
        super.showSlot(slot);
        // Only the greaves receive the inner leggings pattern. The original
        // fortress model also renders torso geometry with greaves; don't paint
        // that chest metal with the leggings waistband.
        body.visible = slot == EquipmentSlot.CHEST;
        hat.visible = false;
    }
}
