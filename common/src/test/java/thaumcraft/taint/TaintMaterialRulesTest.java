package thaumcraft.taint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.HoleBlockRules;
import thaumcraft.common.lib.world.TaintMaterialRules;

/**
 * The block sets of the taint conversion against the 4.2.3.5 oracle.
 *
 * <p>Oracle {@code thaumcraft/common/blocks/BlockTaintFibres.java}:173 converts a wood log, a gourd
 * ({@code Material.field_151572_C}) and a cactus ({@code Material.field_151570_A}) into taint crust and
 * :177 converts sand ({@code field_151595_p}), ground ({@code field_151578_c}), grass ({@code field_151577_b})
 * and clay ({@code field_151571_B}) into taint soil. The 1.7.10 names of those fields are resolved with
 * {@code scripts/porting/resolve_srg.py}.</p>
 */
class TaintMaterialRulesTest {
    @Test void theCrustSetIsTheOriginalOne() {
        // A wood log, a gourd (pumpkin and melon in 1.7.10) and a cactus. The wood log is
        // answered by the minecraft:logs tag, which only a loaded level binds, so the
        // tag part of the rule is not checked here.
        assertTrue(TaintMaterialRules.crust(Blocks.CACTUS.defaultBlockState()));
        assertTrue(TaintMaterialRules.crust(Blocks.PUMPKIN.defaultBlockState()));
        assertTrue(TaintMaterialRules.crust(Blocks.CARVED_PUMPKIN.defaultBlockState()));
        assertTrue(TaintMaterialRules.crust(Blocks.MELON.defaultBlockState()));
        assertFalse(TaintMaterialRules.crust(Blocks.DIRT.defaultBlockState()));
        assertFalse(TaintMaterialRules.crust(Blocks.SAND.defaultBlockState()));
    }

    @Test void theSoilSetIsTheOriginalOne() {
        // Sand and gravel (Material.sand in 1.7.10), dirt and farmland (Material.ground),
        // the grass block (Material.grass) and clay (Material.clay).
        assertTrue(TaintMaterialRules.soil(Blocks.SAND.defaultBlockState()));
        assertTrue(TaintMaterialRules.soil(Blocks.GRAVEL.defaultBlockState()));
        assertTrue(TaintMaterialRules.soil(Blocks.DIRT.defaultBlockState()));
        assertTrue(TaintMaterialRules.soil(Blocks.FARMLAND.defaultBlockState()));
        assertTrue(TaintMaterialRules.soil(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertTrue(TaintMaterialRules.soil(Blocks.CLAY.defaultBlockState()));
    }

    @Test void theBlocksThatTheOriginalNeverHadAreNotConverted() {
        // Mycelium, soul sand, coarse dirt, rooted dirt and red sand have no 1.7.10 material,
        // so the original never converted them into taint soil.
        assertFalse(TaintMaterialRules.soil(Blocks.MYCELIUM.defaultBlockState()));
        assertFalse(TaintMaterialRules.soil(Blocks.SOUL_SAND.defaultBlockState()));
        assertFalse(TaintMaterialRules.soil(Blocks.COARSE_DIRT.defaultBlockState()));
        assertFalse(TaintMaterialRules.soil(Blocks.ROOTED_DIRT.defaultBlockState()));
        assertFalse(TaintMaterialRules.soil(Blocks.RED_SAND.defaultBlockState()));
    }

    @Test void theHoleKeepsTheEffectiveBlastResistanceOfTheOriginal() {
        // Oracle BlockHole:53 - setResistance(6000000) stores 18000000 and the vanilla getter
        // divides the stored value by five, so the effective blast resistance is 3600000.
        assertEquals(3600000.0F, HoleBlockRules.HOLE_RESISTANCE);
        // Oracle BlockHole:55 - setLightLevel(0.7) stores (int)(15*0.7) = 10.
        assertEquals(10, HoleBlockRules.HOLE_LIGHT);
    }
}
