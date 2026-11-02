package thaumcraft.fabric.testing;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Keep optional EMI API types out of entrypoint resolution for ordinary client checks. */
public final class EmiIdentityReviewClientEntrypoint implements ClientModInitializer {
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("thaumcraft.emiIdentityReview")) return;
        if (!FabricLoader.getInstance().isModLoaded("emi")) throw new IllegalStateException("Run with -PwithEmi");
        new EmiIdentityReviewClientProbe().onInitializeClient();
    }
}
