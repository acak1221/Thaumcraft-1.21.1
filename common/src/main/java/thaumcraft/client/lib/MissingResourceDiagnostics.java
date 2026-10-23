package thaumcraft.client.lib;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import thaumcraft.common.Thaumcraft;

/** Optional/addon icon fallback must be observable, without per-frame log spam. */
public final class MissingResourceDiagnostics {
    private static final Set<ResourceLocation> REPORTED = ConcurrentHashMap.newKeySet();

    private MissingResourceDiagnostics() {}

    /** Reports each absent identifier at most once per client session. */
    public static boolean isPresent(ResourceManager resources, ResourceLocation id) {
        if (resources.getResource(id).isPresent()) return true;
        if (REPORTED.add(id)) Thaumcraft.LOG.warn("Missing research icon resource {}; skipping icon", id);
        return false;
    }
}
