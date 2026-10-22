package thaumcraft.common.lib.research;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

class EntityAspectBindingsTest {
    @Test void nativeMobNamesReachOriginalAspectRows() {
        // Original ConfigAspects:87,96-101,105-106. Test the same name translation used by describe.
        var expected = Map.of(
                "thaum_slime", Map.of(Aspect.SLIME, 2, Aspect.MAGIC, 1, Aspect.WATER, 1),
                "tainted_pig", Map.of(Aspect.TAINT, 2, Aspect.EARTH, 2),
                "tainted_sheep", Map.of(Aspect.TAINT, 2, Aspect.EARTH, 2),
                "tainted_cow", Map.of(Aspect.TAINT, 3, Aspect.EARTH, 3),
                "tainted_chicken", Map.of(Aspect.TAINT, 2, Aspect.FLIGHT, 2, Aspect.AIR, 1),
                "tainted_villager", Map.of(Aspect.TAINT, 3, Aspect.AIR, 2),
                "taint_creeper", Map.of(Aspect.TAINT, 2, Aspect.FIRE, 2),
                "cultist_knight", Map.of(Aspect.ELDRITCH, 1, Aspect.MAN, 2, Aspect.ENTROPY, 1),
                "cultist_cleric", Map.of(Aspect.ELDRITCH, 1, Aspect.MAN, 2, Aspect.ENTROPY, 1));
        expected.forEach((id, aspects) -> assertEquals(aspects,
                values(EntityAspectBindings.originalName("thaumcraft:" + id), Map.of()), id));
        assertEquals("example:unknown", EntityAspectBindings.originalName("example:unknown"));
    }

    @Test void originalMobQuantitiesAndConditionalRowsReplaceTheirGenericBase() {
        assertEquals(Map.of(Aspect.UNDEAD, 3, Aspect.MAN, 1, Aspect.EARTH, 1), values("Skeleton", Map.of()));
        assertEquals(Map.of(Aspect.UNDEAD, 4, Aspect.MAN, 1, Aspect.FIRE, 2), values("Skeleton", Map.of("SkeletonType", 1)));
        assertEquals(Map.of(Aspect.PLANT, 2, Aspect.FIRE, 2), values("Creeper", Map.of()));
        assertEquals(Map.of(Aspect.PLANT, 3, Aspect.FIRE, 3, Aspect.ENERGY, 3), values("Creeper", Map.of("powered", 1)));
        assertEquals(Map.of(Aspect.UNDEAD, 3, Aspect.MAN, 1, Aspect.MIND, 1, Aspect.EARTH, 1), values("Thaumcraft.BrainyZombie", Map.of()));
        assertEquals(Map.of(Aspect.UNDEAD, 4, Aspect.MAN, 2, Aspect.MIND, 1, Aspect.EARTH, 2), values("Thaumcraft.GiantBrainyZombie", Map.of()));
    }

    @Test void pechAndWispVariantsRetainOriginalPredicateAndDuplicateAspectAddition() {
        assertNull(EntityAspectBindings.forOriginal("Thaumcraft.Pech", Map.of(), null));
        assertEquals(Map.of(Aspect.MAN, 2, Aspect.MAGIC, 2, Aspect.EXCHANGE, 2, Aspect.GREED, 2), values("Thaumcraft.Pech", Map.of("PechType", 0)));
        assertEquals(Map.of(Aspect.MAN, 2, Aspect.MAGIC, 4, Aspect.EXCHANGE, 2), values("Thaumcraft.Pech", Map.of("PechType", 2)));
        assertNull(EntityAspectBindings.forOriginal("Thaumcraft.Wisp", Map.of(), "not_an_aspect"));
        assertEquals(Map.of(Aspect.AIR, 3, Aspect.MAGIC, 1), EntityAspectBindings.forOriginal("Thaumcraft.Wisp", Map.of(), "aer").aspects);
        assertEquals(Map.of(Aspect.AIR, 1, Aspect.MAGIC, 3), EntityAspectBindings.forOriginal("Thaumcraft.Wisp", Map.of(), "praecantatio").aspects);
    }

    @Test void namedPlayerEasterEggsAndIndependentSnapshotsMatchOriginal() {
        assertEquals(Map.of(Aspect.MAN, 4, Aspect.ELDRITCH, 20), EntityAspectBindings.playerAspects("AzAnOr").aspects);
        assertEquals(Map.of(Aspect.MAN, 4, Aspect.BEAST, 20), EntityAspectBindings.playerAspects("direwolf20").aspects);
        assertEquals(Map.of(Aspect.MAN, 4, Aspect.EXCHANGE, 20), EntityAspectBindings.playerAspects("PAHIMAR").aspects);
        var first = EntityAspectBindings.playerAspects("R12Fixture"); var same = EntityAspectBindings.playerAspects("R12Fixture");
        assertEquals(Map.of(Aspect.MAN, 4, Aspect.FLIGHT, 4, Aspect.TREE, 4, Aspect.EARTH, 4), first.aspects);
        assertEquals(16, first.visSize()); assertEquals(first.aspects, same.aspects);
        first.add(Aspect.AIR, 99); assertNotEquals(first.aspects, same.aspects);
        assertNull(EntityAspectBindings.forOriginal("not_registered", Map.of(), null));
    }
    private static Map<Aspect, Integer> values(String name, Map<String, Integer> predicates) {
        AspectList result = EntityAspectBindings.forOriginal(name, predicates, null); assertNotNull(result); return result.aspects;
    }
}
