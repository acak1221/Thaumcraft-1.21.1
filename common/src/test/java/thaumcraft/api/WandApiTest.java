package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.wands.IWandTriggerManager;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.api.wands.WandTriggerRegistry;
import thaumcraft.common.items.wands.WandMaterials;

/** The wand surface of the public API. */
class WandApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void aRegisteredTriggerIsFoundAndDispatched() {
        int[] seen = {-1};
        IWandTriggerManager manager = (level, wand, player, x, y, z, side, event) -> {
            seen[0] = event;
            return true;
        };
        WandTriggerRegistry.registerWandBlockTrigger(manager, 7, Blocks.STONE, "apitest");
        assertTrue(WandTriggerRegistry.hasTrigger(Blocks.STONE, "apitest"));
        assertFalse(WandTriggerRegistry.hasTrigger(Blocks.STONE, "anothermod"));
        assertTrue(WandTriggerRegistry.performTrigger(null, new ItemStack(Items.STICK), null, 1, 2, 3, 0,
                Blocks.STONE.defaultBlockState(), "apitest"));
        assertEquals(7, seen[0]);
        assertFalse(WandTriggerRegistry.performTrigger(null, ItemStack.EMPTY, null, 1, 2, 3, 0,
                Blocks.DIRT.defaultBlockState(), "apitest"));
    }

    @Test void theDefaultModIdRegistrationIsKept() {
        IWandTriggerManager manager = (level, wand, player, x, y, z, side, event) -> true;
        WandTriggerRegistry.registerWandBlockTrigger(manager, 3, Blocks.DIRT);
        assertTrue(WandTriggerRegistry.hasTrigger(Blocks.DIRT));
        assertTrue(WandTriggerRegistry.modIds().contains("default"));
    }

    @Test void anExactBlockStateRegistrationWins() {
        IWandTriggerManager manager = (level, wand, player, x, y, z, side, event) -> true;
        WandTriggerRegistry.registerWandBlockTrigger(manager, 5, Blocks.STONE.defaultBlockState(), "statetest");
        assertTrue(WandTriggerRegistry.hasTrigger(Blocks.STONE.defaultBlockState(), "statetest"));
        assertFalse(WandTriggerRegistry.hasTrigger(Blocks.STONE.defaultBlockState(), "othermod"));
    }

    // The registries are global: every test that registers a part restores them, so other tests see the native views.
    private java.util.LinkedHashMap<String, WandRod> savedRods;
    private java.util.LinkedHashMap<String, WandCap> savedCaps;

    @org.junit.jupiter.api.BeforeEach void saveRegistries() {
        savedRods = new java.util.LinkedHashMap<>(WandRod.rods);
        savedCaps = new java.util.LinkedHashMap<>(WandCap.caps);
    }

    @org.junit.jupiter.api.AfterEach void restoreRegistries() {
        WandRod.rods.clear(); WandRod.rods.putAll(savedRods);
        WandCap.caps.clear(); WandCap.caps.putAll(savedCaps);
    }

    @Test void aRodKeepsTheOriginalShapeAndResearchKey() {
        // Original constructor shape; the capacity is in whole vis, like new WandRod("wood", 25, ...).
        WandRod rod = new WandRod("apitest_rod", 25, new ItemStack(Items.STICK), 1);
        assertEquals("apitest_rod", rod.getTag());
        assertEquals(25, rod.getCapacity());
        assertEquals(1, rod.getCraftCost());
        assertEquals("ROD_apitest_rod", rod.getResearch());
        assertNotNull(rod.getTexture());
        assertSame(rod, WandRod.rods.get("apitest_rod"));
        assertEquals(2500, WandMaterials.capacity("apitest_rod"), "the wand stores centivis");
    }

    @Test void aStaffRodKeepsTheOriginalStaffTag() {
        StaffRod staff = new StaffRod("apitest", 125, new ItemStack(Items.STICK), 8);
        assertEquals("apitest_staff", staff.getTag());
        assertEquals("ROD_apitest_staff", staff.getResearch());
        assertFalse(staff.hasRunes());
        staff.setRunes(true);
        assertTrue(staff.hasRunes());
        assertTrue(WandRod.rods.get("primal_staff") instanceof StaffRod primal && primal.hasRunes(), "the native primal staff view");
    }

    @Test void aCapKeepsTheOriginalShapeAndResearchKey() {
        WandCap cap = new WandCap("apitest_cap", 1.1F, new ItemStack(Items.IRON_INGOT), 1);
        assertEquals("apitest_cap", cap.getTag());
        assertEquals(1.1F, cap.getBaseCostModifier());
        assertEquals("CAP_apitest_cap", cap.getResearch());
        assertSame(cap, WandCap.caps.get("apitest_cap"));
        assertSame(WandMaterials.IRON, WandCap.caps.get("iron").nativeCap(), "the native iron cap is registered as a view");
    }

    @Test void theNativeCatalogueAndThePublicRodAgree() {
        WandRod nativeRod = new WandRod(WandMaterials.WOOD, new ItemStack(Items.STICK));
        assertEquals(WandMaterials.WOOD.tag(), nativeRod.getTag());
        assertEquals(WandMaterials.WOOD.capacity() / 100, nativeRod.getCapacity(), "the API view answers the original whole vis");
        assertEquals(WandMaterials.WOOD.craftCost(), nativeRod.getCraftCost());
        assertEquals("ROD_wood", nativeRod.getResearch());
        WandCap nativeCap = new WandCap(WandMaterials.IRON, new ItemStack(Items.IRON_INGOT));
        assertEquals(WandMaterials.IRON.tag(), nativeCap.getTag());
        assertEquals(WandMaterials.IRON.modifier(), nativeCap.getBaseCostModifier());
        assertEquals(WandMaterials.IRON.craftCost(), nativeCap.getCraftCost());
        assertNull(WandRod.byTag("not_a_rod"));
    }

    @Test void everyNativeRodIncludingStaffsUsesAnExistingBaseMaterialSheet() {
        var previous = new java.util.LinkedHashMap<>(WandRod.rods);
        try {
            for (var material : WandMaterials.RODS) {
                var rod = new WandRod(material, ItemStack.EMPTY);
                assertEquals("textures/models/wand_rod_" + material.baseTag() + ".png", rod.getTexture().getPath());
                assertNotNull(getClass().getClassLoader().getResource(
                        "assets/" + rod.getTexture().getNamespace() + "/" + rod.getTexture().getPath()), material.tag());
            }
        } finally {
            WandRod.rods.clear();
            WandRod.rods.putAll(previous);
        }
    }
}
