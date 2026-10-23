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

    @Test void aRodKeepsTheOriginalShapeAndResearchKey() {
        WandRod rod = new WandRod("wood", 2500, new ItemStack(Items.STICK), 1);
        assertEquals("wood", rod.getTag());
        assertEquals(2500, rod.getCapacity());
        assertEquals(1, rod.getCraftCost());
        assertEquals("ROD_wood", rod.getResearch());
        assertNotNull(rod.getTexture());
        assertSame(rod, WandRod.rods.get("wood"));
    }

    @Test void aStaffRodKeepsTheOriginalStaffTag() {
        StaffRod staff = new StaffRod("greatwood", 12500, new ItemStack(Items.STICK), 8);
        assertEquals("greatwood_staff", staff.getTag());
        assertEquals("ROD_greatwood_staff", staff.getResearch());
        assertFalse(staff.hasRunes());
        staff.setRunes(true);
        assertTrue(staff.hasRunes());
    }

    @Test void aCapKeepsTheOriginalShapeAndResearchKey() {
        WandCap cap = new WandCap("iron", 1.1F, new ItemStack(Items.IRON_INGOT), 1);
        assertEquals("iron", cap.getTag());
        assertEquals(1.1F, cap.getBaseCostModifier());
        assertEquals("CAP_iron", cap.getResearch());
        assertSame(cap, WandCap.caps.get("iron"));
    }

    @Test void theNativeCatalogueAndThePublicRodAgree() {
        WandRod nativeRod = new WandRod(WandMaterials.WOOD, new ItemStack(Items.STICK));
        assertEquals(WandMaterials.WOOD.tag(), nativeRod.getTag());
        assertEquals(WandMaterials.WOOD.capacity(), nativeRod.getCapacity());
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
