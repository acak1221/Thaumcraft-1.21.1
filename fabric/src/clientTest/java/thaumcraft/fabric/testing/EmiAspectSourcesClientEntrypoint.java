package thaumcraft.fabric.testing;

import net.fabricmc.api.ClientModInitializer;

/** Keep the optional EMI probe unlinked in all other client harness runs. */
public final class EmiAspectSourcesClientEntrypoint implements ClientModInitializer {
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.emiAspectSourcesClient")) return;
        try {
            ((ClientModInitializer) Class.forName("thaumcraft.compat.emi.EmiAspectSourcesClientProbe").getConstructor().newInstance()).onInitializeClient();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot initialize the optional EMI aspect-sources probe", failure);
        }
    }
}
