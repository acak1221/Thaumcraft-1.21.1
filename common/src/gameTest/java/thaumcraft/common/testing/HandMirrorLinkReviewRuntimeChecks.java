package thaumcraft.common.testing;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.items.relics.ItemHandMirror;

/** The persisted public link format accepts raw strings; world lookup must handle missing/invalid dimensions safely. */
public final class HandMirrorLinkReviewRuntimeChecks {
    private HandMirrorLinkReviewRuntimeChecks() {}

    public static void malformedStoredDimensionDoesNotCrashMirrorLookup(GameTestHelper test) {
        var json = JsonParser.parseString("""
                {"x":0,"y":170,"z":0,"dimension":"Bad Namespace:Bad Path","dimension_name":"retained link"}
                """);
        ItemHandMirror.MirrorLink link = ThaumcraftLegacyItems.MIRROR_LINK_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        test.assertTrue(!ItemHandMirror.mirrorStillThere(test.getLevel(), link),
                "a syntactically invalid stored dimension cannot become a real mirror destination");
        test.assertTrue(!ItemHandMirror.mirrorStillThere(test.getLevel(),
                        new ItemHandMirror.MirrorLink(0, 170, 0, "thaumcraft_review:absent", "missing world")),
                "a valid but unavailable dimension remains a safe missing destination");
        test.assertTrue(!ItemHandMirror.mirrorStillThere(test.getLevel(), null), "an unlinked mirror still has no destination");
        test.assertTrue(ThaumcraftLegacyItems.MIRROR_LINK_CODEC.encodeStart(JsonOps.INSTANCE, link).getOrThrow().equals(json),
                "lookup must not destroy the retained original component data");
        test.succeed();
    }
}
