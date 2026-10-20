package thaumcraft.gear;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.client.renderers.models.ModelBanner;
import thaumcraft.client.renderers.models.ModelBrain;
import thaumcraft.client.renderers.models.ModelCentrifuge;
import thaumcraft.client.renderers.models.ModelJar;
import thaumcraft.client.renderers.models.ModelManaPod;
import thaumcraft.client.renderers.models.ModelTubeValve;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;
import thaumcraft.client.renderers.models.gear.ModelKnightArmor;
import thaumcraft.client.renderers.models.gear.ModelLeaderArmor;
import thaumcraft.client.renderers.models.gear.ModelRobe;
import thaumcraft.client.renderers.models.gear.ModelWand;

/**
 * The ported gear models must keep the original part tree, the original pivots
 * and the original rotations; these checks read the baked layers, so a lost or
 * renamed part fails here instead of on screen.
 */
class GearModelGeometryTest {
    /** The number of parts of the whole tree, the root included. */
    private static long parts(ModelPart root) {
        return root.getAllParts().count();
    }

    /** The number of direct children of a part. */
    private static long children(ModelPart part) {
        return part.getAllParts().count() - 1;
    }

    @Test void robeKeepsTheOriginalPartTreeAndPivots() {
        ModelPart armoured = ModelRobe.createBodyLayer(true).bakeRoot();
        ModelPart cloth = ModelRobe.createBodyLayer(false).bakeRoot();
        // The original declares 58 parts, but BeltR and BeltL are never hung
        // on a biped part, so the original never draws them either.
        assertEquals(53, parts(armoured));
        assertEquals(57, parts(cloth));
        assertTrue(armoured.getChild("body").hasChild("chestplate"));
        assertFalse(armoured.getChild("body").hasChild("frontcloth_r1"));
        assertTrue(cloth.getChild("body").hasChild("frontcloth_r1"));
        assertTrue(cloth.getChild("left_leg").hasChild("foci_pouch"));
        // The vanilla biped pivots of the original ModelBiped.
        assertEquals(-5, armoured.getChild("right_arm").x);
        assertEquals(5, armoured.getChild("left_arm").x);
        assertEquals(-1.9F, armoured.getChild("right_leg").x);
        assertEquals(1.9F, armoured.getChild("left_leg").x);
        // The four original hood segments.
        assertEquals(4, children(armoured.getChild("head")));
        assertEquals(-.5759587F, armoured.getChild("head").getChild("hood4").xRot);
        assertEquals(-.2268928F, armoured.getChild("head").getChild("hood2").xRot);
    }

    @Test void fortressArmorKeepsTheOriginalPartTree() {
        ModelPart armoured = ModelFortressArmor.createBodyLayer(true).bakeRoot();
        ModelPart light = ModelFortressArmor.createBodyLayer(false).bakeRoot();
        assertEquals(71, parts(armoured));
        assertEquals(68, parts(light));
        // The original helm carries the three masks and the goggles.
        assertEquals(17, children(armoured.getChild("head")));
        assertTrue(armoured.getChild("head").hasChild("mask0"));
        assertTrue(armoured.getChild("head").hasChild("mask2"));
        assertTrue(armoured.getChild("head").hasChild("goggles"));
        assertEquals(12, children(armoured.getChild("right_leg")));
        assertEquals(12, children(armoured.getChild("left_leg")));
    }

    @Test void cultistArmorKeepsTheOriginalPartTree() {
        assertEquals(44, parts(ModelKnightArmor.createBodyLayer(true).bakeRoot()));
        assertEquals(34, parts(ModelKnightArmor.createBodyLayer(false).bakeRoot()));
        assertEquals(58, parts(ModelLeaderArmor.createBodyLayer(true).bakeRoot()));
        assertEquals(40, parts(ModelLeaderArmor.createBodyLayer(false).bakeRoot()));
    }

    @Test void theSimpleModelsKeepTheirOriginalBoxes() {
        // Original ModelBanner21-52: the pole hangs at (-1,-7,-2).
        ModelPart banner = new ModelBanner().pole();
        assertEquals(-1, banner.x);
        assertEquals(-7, banner.y);
        assertEquals(-2, banner.z);
        // Original ModelJar20-33: the lid hangs at (0,-14,0).
        ModelPart lid = new ModelJar().lid();
        assertEquals(0, lid.x);
        assertEquals(-14, lid.y);
        // Original ModelBrain21-40: the third shape keeps its 0.4089647 tilt.
        assertEquals(.4089647F, new ModelBrain().shape3().xRot);
        // Original ModelCentrifuge20-58 and ModelTubeValve18-27.
        assertNotNull(new ModelCentrifuge().rotor());
        assertNotNull(new ModelTubeValve().stem());
        assertNotNull(new ModelManaPod().pod2());
    }

    @Test void theWandKeepsTheOriginalScales() {
        assertEquals(1.2F, ModelWand.Geometry.STAFF_ROD_XZ);
        assertEquals(2F, ModelWand.Geometry.STAFF_ROD_Y);
        assertEquals(1.3F, ModelWand.Geometry.STAFF_CAP_XZ);
        assertEquals(1.1F, ModelWand.Geometry.STAFF_CAP_Y);
        assertEquals(1.2F, ModelWand.Geometry.WAND_CAP_XZ);
        assertEquals(.525F, ModelWand.Geometry.STAFF_FOCUS_XZ);
        assertEquals(.5525F, ModelWand.Geometry.STAFF_FOCUS_Y);
        assertEquals(.5F, ModelWand.Geometry.WAND_FOCUS);
        assertEquals(.66F, ModelWand.Geometry.STAFF_RING_Y);
        assertEquals(.225F, ModelWand.Geometry.STAFF_RING_SHIFT);
        assertEquals(.65F, ModelWand.Geometry.STAFF_BOTTOM_SHIFT);
    }
}
