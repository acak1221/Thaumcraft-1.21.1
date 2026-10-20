package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApi.EntityTags;
import thaumcraft.api.ThaumcraftApi.EntityTagsNBT;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

class EntityScanApiTest {
    private ArrayList<EntityTags> previous;
    @BeforeEach void isolate() { previous = ThaumcraftApi.scanEntities; ThaumcraftApi.scanEntities = new ArrayList<>(EntityAspectBindings.originalTags()); }
    @AfterEach void restore() { ThaumcraftApi.scanEntities = previous; }

    @Test void expandedOriginalPublicListContainsAllWispVariantsAndCanActuallyBeReplaced() {
        assertEquals(116, ThaumcraftApi.scanEntities.size());
        assertEquals(48, ThaumcraftApi.scanEntities.stream().filter(row -> row.entityName.equals("Thaumcraft.Wisp")).count());
        assertEquals(4, EntityAspectBindings.forOriginal("Skeleton", Map.of("SkeletonType", 1), null).getAmount(Aspect.UNDEAD));
        ThaumcraftApi.scanEntities.removeIf(row -> row.entityName.equals("Skeleton") && row.nbts.length > 0);
        assertEquals(3, EntityAspectBindings.forOriginal("Skeleton", Map.of("SkeletonType", 1), null).getAmount(Aspect.UNDEAD));
        ThaumcraftApi.scanEntities.clear();
        assertNull(EntityAspectBindings.forOriginal("Skeleton", Map.of(), null));
    }

    @Test void lastMatchingGenericRegistrationCanOverrideAMoreSpecificRowAndKeepsCallerReferencesLive() {
        ThaumcraftApi.registerEntityTag("api_test:mob", new AspectList().add(Aspect.AIR, 1));
        ThaumcraftApi.registerEntityTag("api_test:mob", new AspectList().add(Aspect.FIRE, 2), new EntityTagsNBT("Flag", (byte) 1));
        var nbt = new CompoundTag(); nbt.putByte("Flag", (byte) 1);
        assertEquals(2, EntityAspectBindings.forNames("original_mod.mob", "api_test:mob", nbt).getAmount(Aspect.FIRE));
        var late = new AspectList().add(Aspect.WATER, 3); ThaumcraftApi.registerEntityTag("api_test:mob", late);
        late.add(Aspect.EARTH, 4);
        var resolved = EntityAspectBindings.forNames("original_mod.mob", "api_test:mob", nbt);
        assertEquals(Map.of(Aspect.WATER, 3, Aspect.EARTH, 4), resolved.aspects);
        resolved.add(Aspect.AIR, 99); assertEquals(0, late.getAmount(Aspect.AIR));
        ThaumcraftApi.registerEntityTag("api_test:mob", null);
        assertNull(EntityAspectBindings.forNames("original_mod.mob", "api_test:mob", nbt));
    }

    @Test void nbtPredicatesRetainExactBoxedTypesRatherThanCoercingEveryNumber() {
        var nbt = new CompoundTag(); nbt.putByte("Flag", (byte) 1);
        assertTrue(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("Flag", (byte) 1)}, nbt));
        assertFalse(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("Flag", 1)}, nbt));
        assertFalse(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("Flag", true)}, nbt));
        assertFalse(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("Missing", (byte) 0)}, nbt));
        ThaumcraftApi.registerEntityTag("api_test:mob", new AspectList().add(Aspect.ORDER, 1));
        ThaumcraftApi.registerEntityTag("api_test:mob", new AspectList().add(Aspect.FIRE, 9), new EntityTagsNBT("Flag", 1));
        assertEquals(1, EntityAspectBindings.forNames("api_test:mob", "api_test:mob", nbt).getAmount(Aspect.ORDER));
    }

    @Test void legacyArraysUseReferenceEqualityAndListPredicatesReadOnlyCompoundLists() {
        var nbt = new CompoundTag(); byte[] bytes = {1, 2}; nbt.putByteArray("Array", bytes);
        assertTrue(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("Array", bytes)}, nbt));
        assertFalse(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("Array", new byte[]{1, 2})}, nbt));
        var strings = new ListTag(); strings.add(StringTag.valueOf("original getTagList asks for compound tags")); nbt.put("List", strings);
        assertTrue(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("List", new ListTag())}, nbt));
        nbt.putLongArray("ModernOnly", new long[]{1});
        assertFalse(EntityScanPredicates.matches(new EntityTagsNBT[]{new EntityTagsNBT("ModernOnly", null)}, nbt));
    }
}
