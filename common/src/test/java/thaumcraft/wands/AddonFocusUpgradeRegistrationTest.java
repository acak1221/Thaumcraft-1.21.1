package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;

class AddonFocusUpgradeRegistrationTest {
    @Test void anAddonUpgradeKeepsItsIdentityAndCannotReplaceAnExistingUpgrade() {
        var supplied = new AspectList().add(Aspect.MIND, 3);
        var icon = ResourceLocation.fromNamespaceAndPath("test_addon", "textures/sanity.png");
        var upgrade = new FocusUpgradeType(32001, icon, "test.sanity.name", "test.sanity.text", supplied);
        assertSame(upgrade, FocusUpgradeType.get(32001));
        assertEquals(icon, upgrade.icon);
        supplied.add(Aspect.MIND, 5);
        assertEquals(3, upgrade.getAspects().getAmount(Aspect.MIND));
        upgrade.getAspects().remove(Aspect.MIND);
        assertEquals(3, upgrade.getAspects().getAmount(Aspect.MIND));
        assertThrows(IllegalArgumentException.class, () -> new FocusUpgradeType(32001, icon, "other", "other", supplied));
        assertThrows(IllegalArgumentException.class, () -> new FocusUpgradeType(0, icon, "other", "other", supplied));
        assertSame(FocusUpgradeType.potency, FocusUpgradeType.get(0));
        assertThrows(IllegalArgumentException.class, () -> new FocusUpgradeType(32768, icon, "other", "other", supplied));
        assertSame(upgrade, FocusUpgradeType.get(32001));
    }
}
