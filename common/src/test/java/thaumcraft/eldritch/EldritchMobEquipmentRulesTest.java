package thaumcraft.eldritch;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.item.ArmorItem;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.world.EldritchMobRules;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The original eldritch bestiary equipment: the size and the eye height of the golem, the armour
 * of the warden and of the eldritch crab and the cultist plate items of the crab and of the
 * inhabited zombie.
 */
class EldritchMobEquipmentRulesTest {
    @Test void theGolemKeepsItsOriginalSizeAndOnlyTheHeadlessEyeHeightChanges() {
        // Oracle EntityEldritchGolem:92 - func_70105_a(1.75F, 3.5F).
        EntityDimensions headed = EldritchMobRules.golemDimensions(false);
        assertEquals(1.75F, headed.width());
        assertEquals(3.5F, headed.height());
        assertEquals(3.0F, headed.eyeHeight());
        // Oracle EntityEldritchGolem:133-135 - the headless golem looks three and a third up.
        EntityDimensions headless = EldritchMobRules.golemDimensions(true);
        assertEquals(1.75F, headless.width());
        assertEquals(3.5F, headless.height());
        assertEquals(3.33F, headless.eyeHeight());
        assertEquals(3F, EldritchMobRules.golemEyeHeight(false));
        assertEquals(3.33F, EldritchMobRules.golemEyeHeight(true));
    }

    @Test void theOriginalArmourNumbersArePreserved() {
        // Oracle EntityEldritchWarden:155-156 - the warden wears four armour points.
        assertEquals(4F, EldritchMobRules.WARDEN.armor());
        // Oracle EntityEldritchCrab:104-106 - the helm of the crab is worth five armour points.
        assertEquals(5F, EldritchMobRules.CRAB_HELMET_ARMOR);
        // Oracle EntityInhabitedZombie:47-52 and the vanilla zombie - thirty health, five damage
        // and a follow range of thirty five. The original never gave it a knockback resistance.
        assertEquals(30F, EldritchMobRules.INHABITED_ZOMBIE.maxHealth());
        assertEquals(5F, EldritchMobRules.INHABITED_ZOMBIE.attackDamage());
        assertEquals(35F, EldritchMobRules.INHABITED_ZOMBIE.followRange());
    }

    @Test void theCultistPlateIdsAreTheRegisteredItems() {
        // The eldritch crab drops the cultist chest plate and the inhabited zombie wears the
        // three cultist plates, so their ids have to be the registered ones.
        assertEquals("cultist_plate_helm", EldritchMobRules.CULTIST_PLATE_HELMET_ID);
        assertEquals("cultist_plate", EldritchMobRules.CULTIST_PLATE_CHEST_ID);
        assertEquals("cultist_plate_legs", EldritchMobRules.CULTIST_PLATE_LEGS_ID);
        assertEquals(ArmorItem.Type.HELMET, RunicBaubleRules.cultist(EldritchMobRules.CULTIST_PLATE_HELMET_ID).type());
        assertEquals(ArmorItem.Type.CHESTPLATE, RunicBaubleRules.cultist(EldritchMobRules.CULTIST_PLATE_CHEST_ID).type());
        assertEquals(ArmorItem.Type.LEGGINGS, RunicBaubleRules.cultist(EldritchMobRules.CULTIST_PLATE_LEGS_ID).type());
    }
}
