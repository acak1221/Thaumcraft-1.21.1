package thaumcraft.api.potions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/**
 * The original TC4 flux taint potion.
 *
 * <p>Forced 1.21.1 change: 1.21.1 registers its effects, so the original {@code Potion} subclass became a
 * registered {@link MobEffect}. The native implementation is
 * {@code thaumcraft.common.lib.potions.PotionFluxTaint}; this class is the public handle of that registered
 * effect, so an add-on never has to know the native class.</p>
 */
public final class PotionFluxTaint {
    /** The registered identity of the original flux taint effect. */
    public static final ResourceKey<MobEffect> KEY = ResourceKey.create(Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "flux_taint"));

    private PotionFluxTaint() {}

    /** The registered effect; the original static instance was this very object. */
    public static MobEffect instance() {
        return BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(KEY).value();
    }

    /** The registered effect as a holder, for the native effect queries. */
    public static Holder<MobEffect> holder() {
        return BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(KEY);
    }
}
