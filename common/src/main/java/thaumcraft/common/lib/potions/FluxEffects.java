package thaumcraft.common.lib.potions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Small, flux-scoped curative bridge; no global removeAllEffects interception. */
public final class FluxEffects {
    // NeoForge 21.1.259 MobEffectInstance.getCures(): mutable Set<EffectCure>.
    // Reflection bridges that optional native method without a common -> loader dependency.
    // Fabric has no cure-set API and uses the scoped MilkBucketItem redirect instead.
    private static final Method NEO_CURES = findCures();
    private static final Method NEO_FILL_CURES = findFillCures();
    private FluxEffects() {}

    public static MobEffectInstance uncurable(MobEffectInstance effect) {
        if (!(effect instanceof FluxEffectAccess access)) throw new IllegalStateException("FluxEffectInstanceMixin is not registered");
        access.thaumcraft$setUncurableFlux();
        return effect;
    }

    public static boolean isUncurable(MobEffectInstance effect) {
        return effect instanceof FluxEffectAccess access && access.thaumcraft$isUncurableFlux();
    }

    public static void clearNativeCures(MobEffectInstance effect) {
        if (NEO_CURES == null) return;
        try {
            ((Set<?>) NEO_CURES.invoke(effect)).clear();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Cannot clear native NeoForge flux cures", exception);
        }
    }

    /** TC4's custom curatives were not written to NBT; reset on load on both loaders. */
    public static void resetLoadedNativeCures(MobEffectInstance effect) {
        if (NEO_CURES == null) return;
        try {
            Set<?> cures = (Set<?>) NEO_CURES.invoke(effect);
            cures.clear();
            NEO_FILL_CURES.invoke(effect.getEffect().value(), cures, effect);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Cannot reset loaded NeoForge flux curatives", exception);
        }
    }

    private static Method findCures() {
        try { return MobEffectInstance.class.getMethod("getCures"); }
        catch (NoSuchMethodException ignored) { return null; }
    }

    private static Method findFillCures() {
        if (NEO_CURES == null) return null;
        try { return MobEffect.class.getMethod("fillEffectCures", Set.class, MobEffectInstance.class); }
        catch (NoSuchMethodException exception) { throw new IllegalStateException("Native NeoForge cure API is incomplete", exception); }
    }
}
