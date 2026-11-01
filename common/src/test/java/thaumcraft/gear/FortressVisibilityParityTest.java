package thaumcraft.gear;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;

/** Oracle ModelFortressArmor:560-585 assigns isHidden, the inverse of native visible. */
class FortressVisibilityParityTest {
    @Test void bareHelmetHidesAllInfusionsAndFullSetShowsItsOrnaments() {
        ModelPart root=ModelFortressArmor.createBodyLayer(true).bakeRoot();
        var model=new ModelFortressArmor(root,true);var head=root.getChild("head");
        model.setPieceVisibility(3,-1,false);
        for(int mask=0;mask<3;mask++)assertFalse(head.getChild("mask"+mask).visible,"a bare helmet has no infused mask");
        assertFalse(head.getChild("goggles").visible);
        assertTrue(head.getChild("ornament_l").visible);assertTrue(head.getChild("gem").visible);
        assertTrue(root.getChild("body").getChild("scroll").visible);
        assertTrue(root.getChild("body").getChild("book").visible);
    }
    @Test void exactlyTheChosenMaskAndGogglesAppearAndSetChangesDoNotLeak() {
        ModelPart root=ModelFortressArmor.createBodyLayer(true).bakeRoot();var model=new ModelFortressArmor(root,true);var head=root.getChild("head");
        for(int chosen=0;chosen<3;chosen++){
            model.setPieceVisibility(3,chosen,true);assertTrue(head.getChild("goggles").visible);
            for(int mask=0;mask<3;mask++)assertEquals(mask==chosen,head.getChild("mask"+mask).visible);
        }
        model.setPieceVisibility(2,-1,false);
        assertTrue(root.getChild("body").getChild("book").visible);assertFalse(root.getChild("body").getChild("scroll").visible);
        assertTrue(head.getChild("flap_l").visible);assertFalse(head.getChild("ornament_l").visible);
        model.setPieceVisibility(1,-1,false);
        assertFalse(root.getChild("body").getChild("book").visible);assertFalse(head.getChild("flap_l").visible);
    }
}
