package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.lib.runic.RunicShield;

/** Existing IRunicArmor gains its actual augmentation/armor shield; accessory items require their own real slots. */
public final class ThaumcraftRunic {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);
    public static final RegistrySupplier<DataComponentType<Integer>> HARDENING = COMPONENTS.register("runic_hardening", () ->
            DataComponentType.<Integer>builder().persistent(Codec.intRange(-128,127)).networkSynchronized(ByteBufCodecs.BYTE.map(Byte::intValue,value -> {
                if(value < -128 || value > 127) throw new IllegalArgumentException("Invalid signed runic hardening byte");
                return value.byteValue();
            })).build());
    public static final RegistrySupplier<RecipeType<InfusionRunicAugmentRecipe>> AUGMENT_TYPE = TYPES.register("infusion_runic_augment", () -> new RecipeType<InfusionRunicAugmentRecipe>() {
        @Override public String toString() { return "thaumcraft:infusion_runic_augment"; }
    });
    public static final RegistrySupplier<RecipeSerializer<InfusionRunicAugmentRecipe>> AUGMENT_SERIALIZER = SERIALIZERS.register("infusion_runic_augment", () -> InfusionRunicAugmentRecipe.SERIALIZER);
    private static boolean initialized;
    private ThaumcraftRunic() {}
    public static void register() {
        if (initialized) return; initialized = true;
        COMPONENTS.register(); TYPES.register(); SERIALIZERS.register();
        thaumcraft.common.lib.runic.RunicSettings.load(dev.architectury.platform.Platform.getConfigFolder().resolve("thaumcraft.properties"));
        thaumcraft.common.lib.network.RunicNetworking.initServer();
        RunicShield.initialize();
    }
}
