package thaumcraft.client;


/** Client-only binding. No shared atlas mutation or replacement flame particles. */
public final class ThaumcraftEmberClient {
    private static boolean initialized;
    private ThaumcraftEmberClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
    }
}
