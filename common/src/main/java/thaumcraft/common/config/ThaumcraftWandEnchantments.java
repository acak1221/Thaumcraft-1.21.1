package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.crafting.ArcaneSceptreRecipe;
import thaumcraft.common.lib.crafting.ShapelessNBTOreRecipe;

/**
 * Registration of the wand enchantment / wand sceptre slice.
 *
 * 1.21.1 registers no enchantment in code: {@code net.minecraft.world.item.enchantment.Enchantment}
 * is a datapack record, so thaumcraft:frugal, thaumcraft:potency and thaumcraft:wandfortune are
 * defined by data/thaumcraft/enchantment/*.json and their identity lives in
 * thaumcraft.common.lib.enchantment.Enchantment{Frugal,Potency,WandFortune}.
 *
 * What still needs a registry entry is the rest of the slice: the two remaining recipe types of the
 * original (the arcane sceptre recipe and the NBT preserving shapeless recipe) and the marker that
 * replaces the original "sceptre" NBT byte tag on a crafted wand.
 *
 * Call after ThaumcraftContent/ThaumcraftWands/ThaumcraftArcane: the arcane recipe type it uses is
 * registered by ThaumcraftArcane and the wand materials by ThaumcraftWands.
 */
public final class ThaumcraftWandEnchantments {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);

    /** Original ArcaneSceptreRecipe: the wand crafting recipe of the magic workbench. */
    public static final RegistrySupplier<RecipeSerializer<ArcaneSceptreRecipe>> ARCANE_SCEPTRE_SERIALIZER =
            SERIALIZERS.register("arcane_sceptre", ArcaneSceptreRecipe.Serializer::new);
    /** Original ShapelessNBTOreRecipe: the shapeless crafting recipe that keeps the data of its result. */
    public static final RegistrySupplier<RecipeSerializer<ShapelessNBTOreRecipe>> SHAPELESS_NBT_ORE_SERIALIZER =
            SERIALIZERS.register("shapeless_nbt_ore", ShapelessNBTOreRecipe.Serializer::new);
    /**
     * Original ArcaneSceptreRecipe:68 writes the NBT byte tag "sceptre" on the crafted wand. The
     * 1.21.1 equivalent is a data component; the reader side (the extra vis discount and the extra
     * vis capacity of a sceptre) belongs to the wand system.
     */
    public static final RegistrySupplier<DataComponentType<Boolean>> WAND_SCEPTRE = COMPONENTS.register("wand_sceptre",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    private ThaumcraftWandEnchantments() {}

    public static void register() {
        SERIALIZERS.register();
        COMPONENTS.register();
    }

    /** True when the wand carries the original sceptre marker. */
    public static boolean isSceptre(net.minecraft.world.item.ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getOrDefault(WAND_SCEPTRE.get(), Boolean.FALSE);
    }
}
