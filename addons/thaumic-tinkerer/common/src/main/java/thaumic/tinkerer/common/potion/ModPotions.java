/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [4 Sep 2013, 16:59:57 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.potion;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The four imbued effects. 1.7.10 reached into the private potion array to make room for four potion ids it read from
 * the configuration; here they are ordinary registry entries ({@code thaumictinkerer:potion_fire} and so on) and the
 * four id options of the configuration are gone. Like the {@code DummyPotions} of 1.7.10 they are "bad" effects of
 * colour zero that do nothing by themselves: the behaviour is in {@link PotionEffectHandler}.
 *
 * <p>The index order is the metadata order of the potions: air, fire, earth, water.</p>
 */
public final class ModPotions {
    /** The registry paths in the metadata order of the infused potions. */
    public static final String[] PATHS = { "potion_air", "potion_fire", "potion_earth", "potion_water" };
    /** The names the potions carried in code, {@code setPotionName}. */
    public static final String[] ENGLISH_NAMES = { "Air Imbued", "Fire Imbued", "Earth Imbued", "Water Imbued" };

    public static RegistrySupplier<MobEffect> potionAir;
    public static RegistrySupplier<MobEffect> potionFire;
    public static RegistrySupplier<MobEffect> potionEarth;
    public static RegistrySupplier<MobEffect> potionWater;

    private ModPotions() {}

    /** The effect of the 1.7.10 {@code DummyPotions}: harmful category, colour 0, no tick effect. */
    public static class ImbuedEffect extends MobEffect {
        public ImbuedEffect() {
            super(MobEffectCategory.HARMFUL, 0);
        }
    }

    /** Registers the four effects with the deferred register of the add-on. Call once from the register phase. */
    public static void initPotions() {
        if (potionAir != null) return;
        potionAir = TTRegistry.MOB_EFFECTS.register(PATHS[0], ImbuedEffect::new);
        potionFire = TTRegistry.MOB_EFFECTS.register(PATHS[1], ImbuedEffect::new);
        potionEarth = TTRegistry.MOB_EFFECTS.register(PATHS[2], ImbuedEffect::new);
        potionWater = TTRegistry.MOB_EFFECTS.register(PATHS[3], ImbuedEffect::new);
    }

    public static ResourceKey<MobEffect> key(int variant) {
        return ResourceKey.create(Registries.MOB_EFFECT, LibMisc.id(PATHS[variant]));
    }

    /** The registered effect of a variant (0 air, 1 fire, 2 earth, 3 water), as a holder to apply. */
    public static Holder<MobEffect> holder(int variant) {
        return BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(key(variant));
    }

    public static Holder<MobEffect> air() {
        return holder(0);
    }

    public static Holder<MobEffect> fire() {
        return holder(1);
    }

    public static Holder<MobEffect> earth() {
        return holder(2);
    }

    public static Holder<MobEffect> water() {
        return holder(3);
    }
}
