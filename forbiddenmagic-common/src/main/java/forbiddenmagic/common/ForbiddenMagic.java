package forbiddenmagic.common;

import forbiddenmagic.common.aspects.DarkAspects;
import forbiddenmagic.common.items.ForbiddenMagicAspectTags;
import forbiddenmagic.common.items.ForbiddenMagicItems;

/**
 * Original Forbidden 0.575 (Forbidden.java) modid "ForbiddenMagic"; this port uses the
 * lowercase namespace convention of the 1.21.1 registries.
 *
 * <p>Call order mirrors the original's FML lifecycle exactly: aspects before items (items'
 * self-tags reference the sin aspects), then object tags only after both exist. The original
 * split this across prelude/outro FML events; this port has no platform entrypoint yet
 * (see docs/addons/forbidden-magic/STATUS.md), so {@link #bootstrap()} is the single call a
 * future Fabric/NeoForge entrypoint must make.</p>
 *
 * <p><b>Not usable from a plain common-module JUnit test as-is</b>: {@link ForbiddenMagicItems#register()}
 * calls {@code DeferredRegister}'s real registration, which needs a live Architectury platform
 * (architectury-fabric/-neoforge) that a bare JUnit run on :common's classpath does not have.
 * Tests that only need the aspect/object-tag data (not real game registration) call
 * {@link DarkAspects#initAspects()}/{@link DarkAspects#addAspects()}/
 * {@link ForbiddenMagicAspectTags#register()} directly instead, skipping
 * {@link ForbiddenMagicItems#register()} - {@code RegistrySupplier.get()} is a plain memoized
 * supplier and works fine without it.</p>
 */
public final class ForbiddenMagic {
    public static final String MODID = "forbiddenmagic";

    private static boolean bootstrapped;

    private ForbiddenMagic() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        DarkAspects.initAspects();
        ForbiddenMagicItems.register();
        DarkAspects.addAspects();
        ForbiddenMagicAspectTags.register();
    }
}
