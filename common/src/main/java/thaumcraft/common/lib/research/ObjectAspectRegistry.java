package thaumcraft.common.lib.research;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.lang.ref.WeakReference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;

/** Original object-tag lookup/registration rules, independent of a native registry or recipe manager. */
public final class ObjectAspectRegistry {
    public static final int WILDCARD = Short.MAX_VALUE;
    public record Match(AspectList aspects, boolean range) {}
    private static WeakReference<MinecraftServer> serverContext = new WeakReference<>(null);
    private ObjectAspectRegistry() {}

    public static void serverStarted(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Object-aspect context must start on the server thread");
        serverContext = new WeakReference<>(server);
    }

    public static void serverStopped(MinecraftServer server) {
        if (serverContext.get() == server) serverContext.clear();
    }

    /** Modern recipes exist after server data loading; no Level or RecipeManager is retained by this context. */
    public static ServerLevel requireContext() {
        MinecraftServer server = serverContext.get();
        if (server == null || !server.isSameThread() || server.overworld() == null)
            throw new IllegalStateException("registerComplexObjectTag requires the loaded server recipe context; move mod-init registration to the server recipe lifecycle or use registerComplexObjectTag(stack, aspects, serverLevel)");
        return server.overworld();
    }

    public static ServerLevel requireContext(Level level) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || server.getServer().getLevel(server.dimension()) != server)
            throw new IllegalStateException("Complex object tags require a loaded ServerLevel on its server thread");
        return server;
    }

    /** Original exists ignores int[] range keys and groupedObjectTags. */
    public static boolean exists(Map<List<?>, AspectList> tags, Object item, int metadata) {
        return ordinary(tags, item, metadata) != null;
    }

    public static void register(Map<List<?>, AspectList> tags, Object item, int metadata, AspectList aspects) {
        if (item != null) tags.put(List.of(item, metadata), aspects == null ? new AspectList() : aspects);
    }

    /** The first caller-supplied metadata value is canonical; the shared array is neither copied nor sorted. */
    public static void register(Map<List<?>, AspectList> tags, Map<List<?>, int[]> groups,
                                Object item, int[] metadata, AspectList aspects) {
        if (item == null || metadata == null || metadata.length == 0) return;
        register(tags, item, metadata[0], aspects);
        for (int value : metadata) groups.put(List.of(item, value), metadata);
    }

    /** Exact first, then raw range keys, then wildcard; only a wildcard query searches ordinary metadata0..15. */
    public static Match lookup(Map<List<?>, AspectList> tags, Object item, int metadata) {
        var exact = tags.get(List.of(item, metadata));
        if (exact != null) return new Match(exact, false);
        for (var key : tags.keySet()) {
            if (key.size() < 2 || key.get(0) != item || !(key.get(1) instanceof int[] range)) continue;
            Arrays.sort(range); // Observable original quirk for directly inserted array keys.
            if (Arrays.binarySearch(range, metadata) >= 0) return new Match(tags.get(key), true);
        }
        var value = ordinary(tags, item, metadata);
        return value == null ? null : new Match(value, false);
    }

    /** registerComplexObjectTag's existing branch re-merges OLD amounts, including inserting zero-valued missing tags. */
    public static AspectList complex(boolean exists, AspectList supplied, Supplier<AspectList> current,
                                     Supplier<AspectList> generated) {
        if (!exists) {
            AspectList base = generated.get();
            if (base != null && base.size() > 0) supplied.add(base);
            return supplied;
        }
        AspectList previous = current.get();
        for (var aspect : supplied.getAspects()) previous.merge(aspect, previous.getAmount(aspect));
        return previous;
    }

    private static AspectList ordinary(Map<List<?>, AspectList> tags, Object item, int metadata) {
        var value = tags.get(List.of(item, metadata));
        if (value == null) value = tags.get(List.of(item, WILDCARD));
        if (metadata == WILDCARD && value == null) {
            for (int index = 0; index < 16 && value == null; index++) value = tags.get(List.of(item, index));
        }
        return value;
    }
}
