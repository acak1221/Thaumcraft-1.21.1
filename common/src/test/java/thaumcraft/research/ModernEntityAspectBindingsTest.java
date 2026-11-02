package thaumcraft.common.lib.research;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;

class ModernEntityAspectBindingsTest {
    @Test void modernRowsExtendRatherThanRewriteTheOriginalDeclarations() {
        var original = EntityAspectBindings.originalTags();
        var baseline = EntityAspectBindings.baselineTags();
        var modern = ModernEntityAspectBindings.tags();
        assertEquals(original.size() + 9, baseline.size());
        assertEquals(original.stream().map(row -> row.entityName).toList(),
                baseline.subList(0, original.size()).stream().map(row -> row.entityName).toList());
        assertEquals(9, modern.stream().map(row -> row.entityName).distinct().count());
        assertTrue(modern.stream().allMatch(row -> row.entityName.startsWith("minecraft:") && row.nbts.length == 0));
        assertTrue(modern.stream().noneMatch(row -> original.stream().anyMatch(old -> old.entityName.equals(row.entityName))));
    }

    @Test void extensionAmountsAreFiniteAndEachApiCallOwnsItsMutableAspectLists() {
        var modern = ModernEntityAspectBindings.tags();
        var byName = modern.stream().collect(Collectors.toMap(row -> row.entityName, row -> row.aspects));
        assertEquals(Map.of(Aspect.AIR, 3, Aspect.MOTION, 2, Aspect.WEATHER, 2), byName.get("minecraft:breeze").aspects);
        assertEquals(Map.of(Aspect.BEAST, 2, Aspect.WATER, 2, Aspect.SLIME, 1), byName.get("minecraft:frog").aspects);
        assertEquals(Map.of(Aspect.BEAST, 4, Aspect.SENSES, 4, Aspect.DARKNESS, 4), byName.get("minecraft:warden").aspects);
        assertTrue(modern.stream().allMatch(row -> row.aspects.visSize() >= 5 && row.aspects.visSize() <= 12));
        byName.get("minecraft:frog").add(Aspect.WATER, 999);
        assertEquals(2, ModernEntityAspectBindings.tags().stream().filter(row -> row.entityName.equals("minecraft:frog"))
                .findFirst().orElseThrow().aspects.getAmount(Aspect.WATER));
    }
}
