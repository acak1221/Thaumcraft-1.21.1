package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;

class ResearchSearchCacheTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); ResearchCatalog.loadBundled(); }

    @Test void buyingResearchRefreshesTheSameQueryInTheSameOpenBook() {
        var cache = new ResearchSearch.Cache();
        var completed = new HashSet<String>();
        assertTrue(cache.find("NITOR", completed, null).isEmpty());
        completed.add("NITOR");
        var result = cache.find("NITOR", completed, null);
        assertEquals("NITOR", result.getFirst().research().key());
        assertSame(result, cache.find("NITOR", completed, null), "unchanged frames keep the cached index/results");
        completed.clear();
        assertTrue(cache.find("NITOR", completed, null).isEmpty(), "revoked research is removed too");
    }

    @Test void aReplacedCatalogueInvalidatesAnAlreadyBuiltIndexWithoutChangingKnowledge() throws Exception {
        Field live = ResearchCatalog.class.getDeclaredField("live");
        live.setAccessible(true);
        Object original = live.get(null);
        var key = "REVIEW_SEARCH_REFRESH";
        var completed = Set.of(key);
        var cache = new ResearchSearch.Cache();
        assertTrue(cache.find(key, completed, null).isEmpty());
        try {
            Map<String, ResearchDefinition> changed = new LinkedHashMap<>();
            ResearchCatalog.all().forEach(entry -> changed.put(entry.key(), entry));
            changed.put(key, new ResearchDefinition(key, "BASICS", 0, 0, 1, 0, Map.of(), Set.of(),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, List.of(), List.of()));
            live.set(null, Collections.unmodifiableMap(changed));
            assertEquals(key, cache.find(key, completed, null).getFirst().research().key());
        } finally { live.set(null, original); }
    }
}
