package thaumcraft.common.lib.world.biomes;

/** Vanilla's RegistrySetBuilder forward lookup creates holders for unknown
 * keys. TC4 datapack biomes must never be requested while the builtin-only
 * parameter-list bootstrap is assembling that intentionally vanilla registry. */
public final class BiomeSourceBootstrapScope {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    private BiomeSourceBootstrapScope() {}
    public static void enter() {DEPTH.set(DEPTH.get()+1);}
    public static void leave() {int depth=DEPTH.get();if(depth<=1)DEPTH.remove();else DEPTH.set(depth-1);}
    public static boolean active() {return DEPTH.get()!=0;}
}
