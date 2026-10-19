package thaumcraft.common.lib.research;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Registry predicates model the same runtime validation without bootstrapping Minecraft. */
class ObjectAspectRegistryValidationTest {
    @Test
    void absentItemsAbsentBlocksAndConflictingIdsAreAllExcluded() {
        ResourceLocation good = ResourceLocation.parse("minecraft:stone");
        ResourceLocation missingItem = ResourceLocation.parse("removed:material");
        ResourceLocation missingBlock = ResourceLocation.parse("removed:block");
        ResourceLocation collision = ResourceLocation.parse("minecraft:state_alias");
        Map<ResourceLocation, AspectList> candidates = new LinkedHashMap<>();
        for (var id : Set.of(good, missingItem, missingBlock, collision)) {
            candidates.put(id, new AspectList().add(Aspect.EARTH, 2));
        }
        var result = ObjectAspectCatalog.validate(candidates, Set.of(good, missingBlock), Set.of(collision),
                id -> !id.equals(missingItem), id -> !id.equals(missingBlock));
        assertEquals(Set.of(good), result.accepted().keySet());
        assertEquals(1, result.report().acceptedCount());
        assertEquals(Set.of(missingItem), result.report().missingItemIds());
        assertEquals(Set.of(missingBlock), result.report().missingBlockIds());
        assertEquals(Set.of(collision), result.report().conflictingIds());
        result.accepted().get(good).add(Aspect.EARTH, 500);
        assertEquals(2, candidates.get(good).getAmount(Aspect.EARTH));
    }

    @Test
    void itemOnlyBindingsDoNotRequireABlockAndOriginalCandidatesCanBeRevalidated() {
        ResourceLocation item = ResourceLocation.parse("thaumcraft:air_shard");
        var candidates = Map.of(item, new AspectList().add(Aspect.AIR, 2));
        var absent = ObjectAspectCatalog.validate(candidates, Set.of(), Set.of(), id -> false, id -> false);
        assertTrue(absent.accepted().isEmpty());
        var present = ObjectAspectCatalog.validate(candidates, Set.of(), Set.of(), id -> true, id -> false);
        assertEquals(Set.of(item), present.accepted().keySet());
        assertTrue(present.report().missingBlockIds().isEmpty());
    }
}
