package thaumcraft.gear;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;
import thaumcraft.client.renderers.models.gear.ModelFortressTrim;
import thaumcraft.client.renderers.models.gear.ModelRobe;
import thaumcraft.client.renderers.models.gear.ModelRobeTrim;

/** Trim skins must follow real gear visibility and the cloth's independent animation. */
class NativeArmorTrimGeometryTest {
    @Test void fortressTrimFollowsSetVisibilityWithoutPaintingInfusedAccessories() {
        var source = new ModelFortressArmor(ModelFortressArmor.createBodyLayer(true).bakeRoot());
        var trim = ModelFortressTrim.create();
        source.setPieceVisibility(1, 1, true);
        trim.copyPlatePoses(source);
        assertFalse(trim.rightLeg.getChild("sidepanel_r2").visible);
        assertFalse(trim.leftLeg.getChild("sidepanel_l3").visible);
        source.setPieceVisibility(3, 1, true);
        source.rightLeg.getChild("sidepanel_r2").zRot = .7F;
        trim.copyPlatePoses(source);
        assertTrue(trim.rightLeg.getChild("sidepanel_r2").visible);
        assertTrue(trim.leftLeg.getChild("sidepanel_l3").visible);
        assertEquals(.7F, trim.rightLeg.getChild("sidepanel_r2").zRot);
        for (String accessory : new String[]{"mask0", "mask1", "mask2", "goggles", "ornament_l", "gem"}) {
            assertFalse(trim.head.hasChild(accessory), accessory + " retains its own original texture");
        }
        assertFalse(trim.body.hasChild("book"));
        assertFalse(trim.body.hasChild("scroll"));
        trim.showSlot(EquipmentSlot.LEGS);
        assertFalse(trim.body.visible, "native leggings pattern never paints the chest plate");
        assertTrue(trim.leftLeg.visible);
        trim.showSlot(EquipmentSlot.CHEST);
        assertTrue(trim.body.visible);
        assertFalse(trim.leftLeg.visible);
    }

    @Test void robeTrimTracksMovingClothAndRetainsItsFittedSurfaceScale() {
        var source = new ModelRobe(ModelRobe.createBodyLayer(false).bakeRoot(), false);
        var trim = ModelRobeTrim.create(false);
        var moving = source.body.getChild("frontcloth_r2");
        moving.xRot = -.8F;
        moving.y = 12;
        trim.copyPanelPoses(source);
        var copied = trim.body.getChild("frontcloth_r2");
        assertEquals(-.8F, copied.xRot);
        assertEquals(12, copied.y);
        assertEquals(.75F, copied.getChild("surface").xScale);
        moving.xRot = .2F;
        trim.copyPanelPoses(source);
        assertEquals(.2F, copied.xRot, "the skin follows later frames rather than its rest pose");
        assertFalse(trim.leftLeg.hasChild("foci_pouch"));
        assertFalse(trim.head.hasChild("hood4"), "the pointed hood keeps its original ornament texture");
        trim.showSlot(EquipmentSlot.CHEST);
        assertTrue(trim.body.visible);
        assertTrue(trim.leftArm.visible);
        trim.showSlot(EquipmentSlot.LEGS);
        assertFalse(trim.body.visible);
        assertTrue(trim.rightLeg.visible);
    }

    @Test void armouredRobeHasOnlyThePartsOwnedByItsOriginalVariant() {
        var source = new ModelRobe(ModelRobe.createBodyLayer(true).bakeRoot(), true);
        var trim = ModelRobeTrim.create(true);
        assertDoesNotThrow(() -> trim.copyPanelPoses(source));
        assertFalse(trim.body.hasChild("frontcloth_r1"));
        trim.showSlot(EquipmentSlot.HEAD);
        assertTrue(trim.head.visible);
        assertFalse(trim.body.visible);
        assertFalse(trim.rightArm.visible);
    }
}
