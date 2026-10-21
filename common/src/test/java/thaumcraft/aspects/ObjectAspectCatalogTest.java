package thaumcraft.aspects;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.EarlyObjectAspectBindings;
import thaumcraft.common.lib.research.ObjectAspectCatalog;

import static org.junit.jupiter.api.Assertions.*;

class ObjectAspectCatalogTest {
    @BeforeAll
    static void load() { ObjectAspectCatalog.loadBundled(); }

    @Test
    void vanillaAndMetadataAliasesUseExtractedAmounts() {
        assertEquals(2, ObjectAspectCatalog.get(ResourceLocation.parse("minecraft:stone")).getAmount(Aspect.EARTH));
        var granite = ObjectAspectCatalog.get(ResourceLocation.parse("minecraft:granite"));
        assertEquals(3, granite.getAmount(Aspect.EARTH));
        assertEquals(1, granite.getAmount(Aspect.CRYSTAL));
        assertEquals(4, ObjectAspectCatalog.get(ResourceLocation.parse("minecraft:oak_log")).getAmount(Aspect.TREE));
        assertEquals(2, ObjectAspectCatalog.get(ResourceLocation.parse("minecraft:short_grass")).visSize());
    }

    @Test
    void flattenedShardAndResourceRetainNativeNumericBindings() {
        var airShard = ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:air_shard"));
        assertEquals(1, airShard.getAmount(Aspect.MAGIC));
        assertEquals(2, airShard.getAmount(Aspect.AIR));
        assertEquals(1, airShard.getAmount(Aspect.CRYSTAL));
        assertEquals(8, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:knowledge_fragment")).getAmount(Aspect.MIND));
        assertEquals(3, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:quick_silver")).getAmount(Aspect.METAL));
        var airOre = ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:air_infused_stone"));
        assertEquals(1, airOre.getAmount(Aspect.EARTH));
        assertEquals(3, airOre.getAmount(Aspect.AIR));
        assertEquals(2, airOre.getAmount(Aspect.CRYSTAL));
        assertEquals(4, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:earth_infused_stone")).getAmount(Aspect.EARTH));
        assertEquals(3, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:amber_ore")).getAmount(Aspect.TRAP));
    }

    @Test
    void unknownDynamicAndCrystalBindingsFailClosedAndReturnedAmountsAreCopies() {
        assertEquals(0, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:salis_mundus")).size());
        assertEquals(0, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:crystal_air")).size());
        assertEquals(0, ObjectAspectCatalog.get(ResourceLocation.parse("missing:unknown")).size());
        var copy = ObjectAspectCatalog.get(ResourceLocation.parse("minecraft:stone"));
        copy.add(Aspect.EARTH, 500);
        assertEquals(2, ObjectAspectCatalog.get(ResourceLocation.parse("minecraft:stone")).getAmount(Aspect.EARTH));
    }

    /**
     * Oracle ConfigAspects:407-414, 432, 448 and 450 register the object tags of the loot bags,
     * the edible nuggets, the triple meat treat, the primal arrow and the two eldritch objects that
     * the flattened native items carry. The 1.12 backport added a literal balanced shard tag
     * (src/main/java/thaumcraft/common/config/ConfigAspects.java:340-341) that the 1.7.10 original
     * never had, so the balanced shard must stay recipe-derived.
     */
    @Test
    void originalItemObjectTagsArePresentAndTheLegacyOnlyBalancedShardStaysRecipeDerived() {
        var eye = ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:eldritch_eye"));
        assertEquals(5, eye.getAmount(Aspect.ELDRITCH));
        assertEquals(3, eye.getAmount(Aspect.AURA));
        assertEquals(3, eye.getAmount(Aspect.MAGIC));
        assertEquals(3, eye.getAmount(Aspect.SENSES));
        assertEquals(3, eye.getAmount(Aspect.SOUL));
        var tablet = ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:eldritch_tablet"));
        assertEquals(4, tablet.getAmount(Aspect.TRAP));
        assertEquals(4, tablet.getAmount(Aspect.MIND));
        assertEquals(4, tablet.getAmount(Aspect.MECHANISM));
        assertEquals(8, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:loot_bag_common")).getAmount(Aspect.GREED));
        assertEquals(16, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:loot_bag_uncommon")).getAmount(Aspect.GREED));
        assertEquals(32, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:loot_bag_rare")).getAmount(Aspect.GREED));
        for (String nugget : new String[]{"nugget_chicken", "nugget_beef", "nugget_pork", "nugget_fish"}) {
            assertEquals(1, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:" + nugget)).getAmount(Aspect.HUNGER), nugget);
        }
        // Oracle ConfigAspects:435 registers the discount ring metadata3, the air discount ring.
        assertEquals(5, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:discount_ring_aer")).getAmount(Aspect.MAGIC));
        assertEquals(0, ObjectAspectCatalog.get(ResourceLocation.parse("thaumcraft:balance_shard")).size());
        assertFalse(ObjectAspectCatalog.registeredIds().contains(ResourceLocation.parse("thaumcraft:balance_shard")));
        var complex = EarlyObjectAspectBindings.complex();
        assertEquals(1, complex.get(ResourceLocation.parse("thaumcraft:primal_arrow_air")).getAmount(Aspect.WEAPON));
        assertEquals(1, complex.get(ResourceLocation.parse("thaumcraft:triple_meat_treat")).getAmount(Aspect.HEAL));
        assertFalse(complex.containsKey(ResourceLocation.parse("thaumcraft:primal_arrow_fire")));
    }
}
