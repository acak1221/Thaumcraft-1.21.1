package forbiddenmagic.items;

import forbiddenmagic.common.aspects.DarkAspects;
import forbiddenmagic.common.config.ForbiddenMagicConfig;
import forbiddenmagic.common.items.ForbiddenMagicAspectTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies each checkpoint-1 item's self aspect tag data against the decompiled original
 * DarkAspects.addAspects() lines registering ForbiddenItems.deadlyShards/gluttonyShard/resource
 * (fox.spiteful.forbidden.DarkAspects:185-203, Forbidden Magic 0.575 oracle jar). Tests the plain
 * {@code *_TAGS} data in {@link ForbiddenMagicAspectTags}, not the real registered items - see
 * that class's doc for why: building a real registered Item needs a live Fabric/NeoForge
 * platform that a common-module JUnit run does not have. */
class ForbiddenMagicAspectTagsTest {
    @BeforeAll
    static void init() {
        ForbiddenMagicConfig.noLust = false;
        DarkAspects.initAspects();
    }

    @Test void wrathShard() {
        var tags = ForbiddenMagicAspectTags.WRATH_SHARD_TAGS;
        assertEquals(1, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.getAmount(DarkAspects.WRATH));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
        assertEquals(3, tags.size());
    }

    @Test void envyShard() {
        var tags = ForbiddenMagicAspectTags.ENVY_SHARD_TAGS;
        assertEquals(1, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.getAmount(DarkAspects.ENVY));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
    }

    @Test void taintShardHasNoNetherAspectUnlikeTheOtherShards() {
        var tags = ForbiddenMagicAspectTags.TAINT_SHARD_TAGS;
        assertEquals(3, tags.getAmount(Aspect.TAINT));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
        assertEquals(0, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.size());
    }

    @Test void prideShard() {
        var tags = ForbiddenMagicAspectTags.PRIDE_SHARD_TAGS;
        assertEquals(1, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.getAmount(DarkAspects.PRIDE));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
    }

    @Test void lustShard() {
        var tags = ForbiddenMagicAspectTags.LUST_SHARD_TAGS;
        assertEquals(1, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.getAmount(DarkAspects.LUST));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
    }

    @Test void despairShardIsTheNoLustAlternateView() {
        var tags = ForbiddenMagicAspectTags.DESPAIR_SHARD_TAGS;
        assertEquals(1, tags.getAmount(Aspect.VOID));
        assertEquals(1, tags.size());
    }

    @Test void slothShard() {
        var tags = ForbiddenMagicAspectTags.SLOTH_SHARD_TAGS;
        assertEquals(1, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.getAmount(DarkAspects.SLOTH));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
    }

    @Test void greedShardUsesBaseGreedAspectNotACustomOne() {
        var tags = ForbiddenMagicAspectTags.GREED_SHARD_TAGS;
        assertEquals(1, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.getAmount(Aspect.GREED));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
    }

    @Test void gluttonyShard() {
        var tags = ForbiddenMagicAspectTags.GLUTTONY_SHARD_TAGS;
        assertEquals(1, tags.getAmount(DarkAspects.NETHER));
        assertEquals(2, tags.getAmount(DarkAspects.GLUTTONY));
        assertEquals(1, tags.getAmount(Aspect.CRYSTAL));
    }

    @Test void emeraldFragment() {
        assertEquals(1, ForbiddenMagicAspectTags.EMERALD_FRAGMENT_TAGS.getAmount(Aspect.CRYSTAL));
    }

    @Test void shadowInk() {
        assertEquals(1, ForbiddenMagicAspectTags.SHADOW_INK_TAGS.getAmount(Aspect.DARKNESS));
    }
}
