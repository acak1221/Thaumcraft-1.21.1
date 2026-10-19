package thaumcraft.aspects;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
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
}
