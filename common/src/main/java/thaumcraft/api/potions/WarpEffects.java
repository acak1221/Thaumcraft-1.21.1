package thaumcraft.api.potions;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/**
 * The stable keys of the original TC4 warp effects.
 *
 * <p>1.7.10 add-ons found these potions by the numeric ids of Thaumcraft's configuration
 * ({@code Config.potionWarpWardID}, {@code potionSunScornedID}, {@code potionDeathGazeID},
 * {@code potionBlurredID}, {@code potionUnHungerID}, {@code potionInfVisExhaustID},
 * {@code potionThaumarhiaID}) and applied them with {@code new PotionEffect(id, duration, amplifier)}. 1.21.1
 * registers its effects, so each original {@code Potion} subclass became a registered {@link MobEffect} under the
 * {@code thaumcraft} namespace and the numeric id is gone. These keys are the identities of those effects, so an
 * add-on never writes the strings itself. {@link PotionFluxTaint} and {@link PotionVisExhaust} keep the keys of the
 * two flux effects.</p>
 *
 * <p>The keys exist from the first line of the class; the effects are registered by Thaumcraft's own registration,
 * which has run before {@code IThaumcraftAddon.onThaumcraftReady}. {@link #holder} and {@link #instance} read the
 * effects from the built-in registry and throw {@link IllegalStateException} while they are not registered yet
 * (an add-on's own static initialiser may run earlier). Both are safe on any thread once the registry is frozen.</p>
 *
 * <pre>{@code
 * player.addEffect(new MobEffectInstance(WarpEffects.holder(WarpEffects.WARP_WARD), 6000, 0));
 * boolean hungry = player.hasEffect(WarpEffects.holder(WarpEffects.UNNATURAL_HUNGER));
 * }</pre>
 */
public final class WarpEffects {
    /** Original {@code PotionWarpWard}: the one beneficial effect; while it runs the warp check of the player is suspended. */
    public static final ResourceKey<MobEffect> WARP_WARD = key("warp_ward");
    /** Original {@code PotionSunScorned}: bright light can set the afflicted on fire, darkness can heal it. */
    public static final ResourceKey<MobEffect> SUN_SCORNED = key("sun_scorned");
    /** Original {@code PotionDeathGaze}: a passive marker; the server tick of the player runs the gaze query. */
    public static final ResourceKey<MobEffect> DEATH_GAZE = key("death_gaze");
    /** Original {@code PotionBlurredVision}: a passive marker the client reads to blur the view. */
    public static final ResourceKey<MobEffect> BLURRED_VISION = key("blurred_vision");
    /** Original {@code PotionUnnaturalHunger}: food exhaustion of a player on every tick. */
    public static final ResourceKey<MobEffect> UNNATURAL_HUNGER = key("unnatural_hunger");
    /** Original {@code PotionInfectiousVisExhaust}: vis exhaustion that spreads to the creatures around the afflicted. */
    public static final ResourceKey<MobEffect> INFECTIOUS_VIS_EXHAUST = key("infectious_vis_exhaust");
    /** Original {@code PotionThaumarhia}: now and then leaves flux goo in the empty cell at the feet of the afflicted. */
    public static final ResourceKey<MobEffect> THAUMARHIA = key("thaumarhia");

    private static final List<ResourceKey<MobEffect>> ALL = List.of(WARP_WARD, SUN_SCORNED, DEATH_GAZE, BLURRED_VISION,
            UNNATURAL_HUNGER, INFECTIOUS_VIS_EXHAUST, THAUMARHIA);

    private WarpEffects() {}

    /** All seven keys, in the order above. The list is immutable. */
    public static List<ResourceKey<MobEffect>> all() {
        return ALL;
    }

    /**
     * The registered effect of a key, as the holder the 1.21.1 effect calls ({@code addEffect}, {@code hasEffect},
     * {@code removeEffect}, {@code new MobEffectInstance}) take.
     *
     * @throws IllegalStateException when the key is not registered (Thaumcraft's registration has not run yet, or
     *                               the key is not one of this class)
     */
    public static Holder<MobEffect> holder(ResourceKey<MobEffect> key) {
        return BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(key);
    }

    /**
     * The registered effect of a key; the original static {@code instance} of the potion class was this object.
     *
     * @throws IllegalStateException when the key is not registered, see {@link #holder}
     */
    public static MobEffect instance(ResourceKey<MobEffect> key) {
        return holder(key).value();
    }

    private static ResourceKey<MobEffect> key(String path) {
        return ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath("thaumcraft", path));
    }
}
