package thaumic.tinkerer.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.core.helper.EnumMobAspect;
import thaumic.tinkerer.common.item.ItemMobAspect;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The necromancy table: thirty mobs, each made of an unordered set of the seventeen soul aspects.</p>
 */
class EnumMobAspectTest {

    private static List<Aspect> list(Aspect... aspects) {
        return new ArrayList<>(List.of(aspects));
    }

    /** The first row whose aspect set equals the aspects on the pedestals: what the tablet picks. */
    private static EnumMobAspect pick(Aspect... onPedestals) {
        for (EnumMobAspect row : EnumMobAspect.values()) if (row.matchesAspectSet(list(onPedestals))) return row;
        return null;
    }

    @Test
    void thereAreThirtyRowsOfThreeAspects() {
        assertEquals(30, EnumMobAspect.values().length);
        for (EnumMobAspect row : EnumMobAspect.values()) assertEquals(3, row.aspects.length, row.name());
    }

    @Test
    void everyAspectOfARowIsOneOfTheSeventeenSoulAspects() {
        for (EnumMobAspect row : EnumMobAspect.values()) {
            for (Aspect aspect : row.aspects) {
                assertTrue(ItemMobAspect.ASPECTS.contains(aspect), row.name() + " uses " + aspect.getTag());
            }
        }
        assertEquals(17, ItemMobAspect.ASPECTS.size());
        assertEquals(17, new HashSet<>(ItemMobAspect.ASPECTS).size());
    }

    @Test
    void everySoulAspectIsUsedBySomeRow() {
        Set<Aspect> used = new HashSet<>();
        for (EnumMobAspect row : EnumMobAspect.values()) used.addAll(List.of(row.aspects));
        for (Aspect aspect : ItemMobAspect.ASPECTS) assertTrue(used.contains(aspect), aspect.getTag());
    }

    @Test
    void noTwoRowsShareTheirAspectSet() {
        for (EnumMobAspect a : EnumMobAspect.values()) {
            for (EnumMobAspect b : EnumMobAspect.values()) {
                if (a == b) continue;
                assertFalse(a.matchesAspectSet(list(b.aspects)), a.name() + " and " + b.name() + " share a set");
            }
        }
    }

    @Test
    void theAspectsOnThePedestalsAreASetNotAMultiset() {
        // Cow is {bestia, terra, bestia} = {bestia, terra}: any three pedestals with exactly those two aspects
        assertEquals(EnumMobAspect.Cow, pick(Aspect.BEAST, Aspect.EARTH, Aspect.BEAST));
        assertEquals(EnumMobAspect.Cow, pick(Aspect.EARTH, Aspect.EARTH, Aspect.BEAST));
        assertEquals(EnumMobAspect.Cow, pick(Aspect.BEAST, Aspect.BEAST, Aspect.EARTH));
        // the order of the pedestals does not matter
        assertEquals(EnumMobAspect.Pig, pick(Aspect.TRAVEL, Aspect.BEAST, Aspect.EARTH));
        assertEquals(EnumMobAspect.Pig, pick(Aspect.EARTH, Aspect.TRAVEL, Aspect.BEAST));
        // a set with an aspect too many or too few is no mob
        assertEquals(null, pick(Aspect.AIR, Aspect.AIR, Aspect.AIR));
        assertEquals(null, pick(Aspect.BEAST, Aspect.EARTH, Aspect.WATER));
    }

    @Test
    void theRowsThatFollowTheOriginalTable() {
        assertEquals(EnumMobAspect.Chicken, pick(Aspect.CROP, Aspect.FLIGHT, Aspect.BEAST));
        assertEquals(EnumMobAspect.MushroomCow, pick(Aspect.BEAST, Aspect.EARTH, Aspect.CROP));
        assertEquals(EnumMobAspect.Wolf, pick(Aspect.BEAST, Aspect.BEAST, Aspect.BEAST));
        assertEquals(EnumMobAspect.Blaze, pick(Aspect.FIRE, Aspect.FIRE, Aspect.FIRE));
        assertEquals(EnumMobAspect.Zombie, pick(Aspect.FLESH, Aspect.UNDEAD, Aspect.FLESH));
        assertEquals(EnumMobAspect.Skeleton, pick(Aspect.UNDEAD, Aspect.MAN, Aspect.UNDEAD));
        assertEquals(EnumMobAspect.WitherSkeleton, pick(Aspect.UNDEAD, Aspect.POISON, Aspect.FIRE));
        assertEquals(EnumMobAspect.Creeper, pick(Aspect.MAGIC, Aspect.BEAST, Aspect.ELDRITCH));
        assertEquals(EnumMobAspect.Enderman, pick(Aspect.ELDRITCH, Aspect.ELDRITCH, Aspect.MAN));
        assertEquals(EnumMobAspect.Wisp, pick(Aspect.AIR, Aspect.MAGIC, Aspect.MAGIC));
        assertEquals(EnumMobAspect.Firebat, pick(Aspect.FLIGHT, Aspect.FIRE, Aspect.MAGIC));
        assertEquals(EnumMobAspect.BrainyZombie, pick(Aspect.MAGIC, Aspect.UNDEAD, Aspect.FLESH));
    }

    @Test
    void aSubclassIsListedBeforeItsSuperclass() {
        assertTrue(EnumMobAspect.MushroomCow.ordinal() < EnumMobAspect.Cow.ordinal());
        assertTrue(EnumMobAspect.PigZombie.ordinal() < EnumMobAspect.Zombie.ordinal());
        assertTrue(EnumMobAspect.BrainyZombie.ordinal() < EnumMobAspect.Zombie.ordinal());
        assertTrue(EnumMobAspect.LavaSlime.ordinal() < EnumMobAspect.Slime.ordinal());
        assertTrue(EnumMobAspect.CaveSpider.ordinal() < EnumMobAspect.Spider.ordinal());
    }

    @Test
    void scalesAndOffsetsOfTheMobDisplay() {
        assertEquals(1.0f, EnumMobAspect.Chicken.getScale());
        assertEquals(0.9f, EnumMobAspect.Cow.getScale());
        assertEquals(0.6f, EnumMobAspect.Squid.getScale());
        assertEquals(0.75f, EnumMobAspect.Squid.getVerticalOffset());
        assertEquals(0.2f, EnumMobAspect.Ghast.getScale());
        assertEquals(0.6f, EnumMobAspect.Ghast.getVerticalOffset());
        assertEquals(1.9f, EnumMobAspect.Bat.getScale());
        assertEquals(1.5f, EnumMobAspect.Slime.getScale());
        assertEquals(1.2f, EnumMobAspect.Silverfish.getScale());
        assertEquals(0.0f, EnumMobAspect.Wisp.getVerticalOffset());
    }

    @Test
    void rowsAreFoundByTheirNames() {
        for (EnumMobAspect row : EnumMobAspect.values()) assertEquals(row, EnumMobAspect.get(row.name()));
        assertEquals(null, EnumMobAspect.get(""));
        assertEquals(null, EnumMobAspect.get("Dragon"));
    }
}
