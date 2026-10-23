package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.lib.InternalMethodHandler;

class ApiCacheOwnershipTest {
    @Test void allAspectsFirstAnswerAndCacheHitsAreCallerOwned() {
        assertOwned(ThaumcraftApiHelper::getAllAspects, 731, Aspect.AIR, Aspect.aspects.size());
    }

    @Test void compoundAspectsFirstAnswerAndCacheHitsAreCallerOwned() {
        assertOwned(ThaumcraftApiHelper::getAllCompoundAspects, 732, Aspect.VOID, Aspect.getCompoundAspects().size());
    }

    private static void assertOwned(java.util.function.IntFunction<AspectList> getter,
                                    int amount, Aspect aspect, int size) {
        AspectList first = getter.apply(amount);
        assertEquals(size, first.size());
        first.aspects.clear();
        AspectList second = getter.apply(amount);
        assertEquals(size, second.size(), "first answer must not escape the cache");
        assertEquals(amount, second.getAmount(aspect));
        second.aspects.put(aspect, -99);
        AspectList third = getter.apply(amount);
        assertEquals(amount, third.getAmount(aspect), "cache hits must also be independent");
        assertNotSame(second, third);
        assertNotSame(second.aspects, third.aspects);
    }

    @Test void numericDimensionEffectIsExplicitlyDeprecatedOnTheInstalledHandler() throws Exception {
        for (Class<?> type : new Class<?>[]{IInternalMethodHandler.class,
                ThaumcraftApi.internalMethods.getClass(), InternalMethodHandler.INSTANCE.getClass()}) {
            var method = type.getMethod("generateVisEffect",
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            assertTrue(method.isAnnotationPresent(Deprecated.class), "inert legacy overload must warn addon authors: " + type);
        }
    }
}
