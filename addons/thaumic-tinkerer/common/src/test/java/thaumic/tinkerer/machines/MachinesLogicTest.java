package thaumic.tinkerer.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.block.tile.TileMagnet;
import thaumic.tinkerer.common.block.tile.TileRepairer;
import thaumic.tinkerer.common.item.ItemMobAspect;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The plain rules of the machines: the magnet's range and motion, what the repairer accepts, the names of the
 * soul aspect items.</p>
 */
class MachinesLogicTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- Kinetic / Corporeal Attractor ---------------------------------------------------------------------------

    @Test
    void theRangeIsAnIntegerDivisionOfTheSignal() {
        assertEquals(0, TileMagnet.rangeFor(0));
        assertEquals(0, TileMagnet.rangeFor(1)); // a box of no width: signal 1 does nothing
        assertEquals(1, TileMagnet.rangeFor(2));
        assertEquals(1, TileMagnet.rangeFor(3));
        assertEquals(7, TileMagnet.rangeFor(15));
    }

    @Test
    void aPullingMagnetMovesAFarEntityAtFullSpeedTowardsItself() {
        Vec3 magnet = new Vec3(0.5, 0.5, 0.5);
        Vec3 motion = TileMagnet.motionTowards(new Vec3(4.5, 0.5, 0.5), magnet, 0.25F);
        assertEquals(-0.25, motion.x, 1.0E-6);
        assertEquals(0, motion.y, 1.0E-6);
        assertEquals(0, motion.z, 1.0E-6);
    }

    @Test
    void aNearEntityGetsLessSpeedBecauseTheVectorIsNotNormalised() {
        Vec3 magnet = new Vec3(0.5, 0.5, 0.5);
        Vec3 motion = TileMagnet.motionTowards(new Vec3(0.5, 1.0, 0.5), magnet, 0.25F);
        assertEquals(-0.125, motion.y, 1.0E-6);
    }

    @Test
    void aPushingMagnetReversesTheMotion() {
        Vec3 magnet = new Vec3(0.5, 0.5, 0.5);
        Vec3 pull = TileMagnet.motionTowards(new Vec3(0.5, 0.5, -2.5), magnet, 0.25F);
        Vec3 push = TileMagnet.motionTowards(new Vec3(0.5, 0.5, -2.5), magnet, -0.25F);
        assertEquals(0.25, pull.z, 1.0E-6);
        assertEquals(-0.25, push.z, 1.0E-6);
    }

    @Test
    void theDirectionIsNormalisedInThreeDimensions() {
        Vec3 motion = TileMagnet.motionTowards(new Vec3(3.5, 4.5, 0.5), new Vec3(0.5, 0.5, 0.5), 0.25F);
        assertEquals(0.25, motion.length(), 1.0E-6);
        assertEquals(-0.15, motion.x, 1.0E-6);
        assertEquals(-0.2, motion.y, 1.0E-6);
    }

    // ---- Thaumic Restorer ------------------------------------------------------------------------------------------

    @Test
    void theRepairerTakesDamagedToolsOnly() {
        ItemStack damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(100);
        assertTrue(TileRepairer.isRepairable(damaged));

        assertFalse(TileRepairer.isRepairable(new ItemStack(Items.DIAMOND_PICKAXE)), "mended");
        assertFalse(TileRepairer.isRepairable(new ItemStack(Items.STICK)), "nothing to mend");
        assertFalse(TileRepairer.isRepairable(ItemStack.EMPTY));
    }

    // ---- soul aspects ----------------------------------------------------------------------------------------------

    @Test
    void thereAreFiftyOneSoulAspectItemsWithDistinctNames() {
        Set<String> names = new HashSet<>();
        for (Aspect aspect : ItemMobAspect.ASPECTS) {
            for (ItemMobAspect.Form form : ItemMobAspect.Form.values()) names.add(ItemMobAspect.path(aspect, form));
        }
        assertEquals(51, names.size());
        assertTrue(names.contains("mob_aspect_aqua"));
        assertTrue(names.contains("mob_aspect_condensed_messis"));
        assertTrue(names.contains("mob_aspect_infused_humanus"));
    }

    @Test
    void theOrderOfTheAspectsIsTheOrderOfTheOldMetadata() {
        // NumericAspectHelper: the index of an aspect was the metadata of the old item
        assertEquals(Aspect.WATER, ItemMobAspect.ASPECTS.get(0));
        assertEquals(Aspect.MAN, ItemMobAspect.ASPECTS.get(1));
        assertEquals(Aspect.AIR, ItemMobAspect.ASPECTS.get(2));
        assertEquals(Aspect.FIRE, ItemMobAspect.ASPECTS.get(4));
        assertEquals(Aspect.EARTH, ItemMobAspect.ASPECTS.get(10));
        assertEquals(Aspect.CROP, ItemMobAspect.ASPECTS.get(16));
    }

    @Test
    void theResearchPageShowsTheLastRecipeLikeInTheOriginal() {
        assertEquals("summon1", ItemMobAspect.CONDENSE_RECIPE_NAME);
    }
}
