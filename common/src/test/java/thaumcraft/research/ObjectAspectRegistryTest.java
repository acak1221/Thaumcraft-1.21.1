package thaumcraft.common.lib.research;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

class ObjectAspectRegistryTest {
    @Test void directWritesRetainReferencesOverwriteAndConvertNullToExplicitEmpty() {
        var tags = new ConcurrentHashMap<List<?>, AspectList>(); Object item = new Object();
        var live = new AspectList().add(Aspect.AIR, 1); ObjectAspectRegistry.register(tags, item, 0, live);
        live.add(Aspect.FIRE, 2); assertSame(live, ObjectAspectRegistry.lookup(tags, item, 0).aspects());
        ObjectAspectRegistry.register(tags, item, 0, null);
        assertTrue(ObjectAspectRegistry.exists(tags, item, 0)); assertEquals(0, ObjectAspectRegistry.lookup(tags, item, 0).aspects().size());
    }

    @Test void exactPrecedesWildcardAndOnlyWildcardQuerySearchesZeroToFifteenInAscendingOrder() {
        var tags = new ConcurrentHashMap<List<?>, AspectList>(); Object item = new Object();
        var first = new AspectList().add(Aspect.AIR, 1); var last = new AspectList().add(Aspect.FIRE, 2);
        ObjectAspectRegistry.register(tags, item, 15, last); ObjectAspectRegistry.register(tags, item, 3, first);
        assertNull(ObjectAspectRegistry.lookup(tags, item, 2));
        assertSame(first, ObjectAspectRegistry.lookup(tags, item, 32767).aspects());
        assertFalse(ObjectAspectRegistry.exists(tags, item, 2)); assertTrue(ObjectAspectRegistry.exists(tags, item, 32767));
        var wildcard = new AspectList().add(Aspect.EARTH, 9); ObjectAspectRegistry.register(tags, item, 32767, wildcard);
        assertSame(first, ObjectAspectRegistry.lookup(tags, item, 3).aspects()); assertSame(wildcard, ObjectAspectRegistry.lookup(tags, item, 2).aspects());
        tags.clear(); ObjectAspectRegistry.register(tags, item, 16, last); assertFalse(ObjectAspectRegistry.exists(tags, item, 32767));
    }

    @Test void groupedRegistrationUsesCallerFirstValueAndDoesNotMakeOtherMetadataDirectlyExist() {
        var tags = new ConcurrentHashMap<List<?>, AspectList>(); var groups = new ConcurrentHashMap<List<?>, int[]>(); Object item = new Object();
        int[] metadata = {7, 2, 5}; var aspects = new AspectList().add(Aspect.MAGIC, 4);
        ObjectAspectRegistry.register(tags, groups, item, metadata, aspects);
        assertArrayEquals(new int[]{7, 2, 5}, metadata); assertSame(aspects, tags.get(List.of(item, 7)));
        assertFalse(ObjectAspectRegistry.exists(tags, item, 2)); assertNull(ObjectAspectRegistry.lookup(tags, item, 2));
        assertSame(metadata, groups.get(List.of(item, 2))); metadata[0] = 9;
        assertEquals(9, groups.get(List.of(item, 5))[0]); assertSame(aspects, tags.get(List.of(item, 7)));
    }

    @Test void manuallyInsertedRangeKeysSortTheirSharedArrayAndRemainSeparateFromExists() {
        var tags = new ConcurrentHashMap<List<?>, AspectList>(); Object item = new Object(); int[] range = {9, 2, 5};
        var ranged = new AspectList().add(Aspect.AIR, 70); tags.put(List.of(item, range), ranged);
        assertFalse(ObjectAspectRegistry.exists(tags, item, 5));
        var result = ObjectAspectRegistry.lookup(tags, item, 5);
        assertTrue(result.range()); assertSame(ranged, result.aspects()); assertArrayEquals(new int[]{2, 5, 9}, range);
        ObjectAspectRegistry.register(tags, item, 5, new AspectList().add(Aspect.EARTH, 1));
        assertFalse(ObjectAspectRegistry.lookup(tags, item, 5).range());
    }

    @Test void complexAbsentMutatesCallerByAddingGeneratedBaseWhileExistingIgnoresItsValues() {
        var supplied = new AspectList().add(Aspect.AIR, 4); var generated = new AspectList().add(Aspect.AIR, 3).add(Aspect.FIRE, 2);
        assertSame(supplied, ObjectAspectRegistry.complex(false, supplied, () -> { throw new AssertionError("no existing branch"); }, () -> generated));
        assertEquals(7, supplied.getAmount(Aspect.AIR)); assertEquals(2, supplied.getAmount(Aspect.FIRE)); assertEquals(3, generated.getAmount(Aspect.AIR));
        var existing = new AspectList().add(Aspect.EARTH, 5);
        var result = ObjectAspectRegistry.complex(true, new AspectList().add(Aspect.EARTH, 90).add(Aspect.WATER, 8), () -> existing, () -> { throw new AssertionError("existing tags do not derive recipes"); });
        assertSame(existing, result); assertEquals(5, result.getAmount(Aspect.EARTH));
        assertTrue(result.aspects.containsKey(Aspect.WATER)); assertEquals(0, result.getAmount(Aspect.WATER));
    }
}
