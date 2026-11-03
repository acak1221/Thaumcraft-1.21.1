package thaumcraft.fabric.testing;

import net.fabricmc.api.ClientModInitializer;

/** Keep the optional JEI probe unlinked in all other client harness runs. */
public final class AspectSourcesClientEntrypoint implements ClientModInitializer {
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.jeiAspectSourcesClient")) return;
        try {
            ((ClientModInitializer) Class.forName("thaumcraft.compat.jei.AspectSourcesClientProbe").getConstructor().newInstance()).onInitializeClient();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot initialize the optional JEI aspect-sources probe", failure);
        }
    }
}
