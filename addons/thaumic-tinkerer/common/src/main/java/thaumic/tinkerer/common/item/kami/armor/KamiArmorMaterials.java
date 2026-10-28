/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami.armor;

import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * {@code EnumHelper.addArmorMaterial("ICHOR", 0, new int[] {3, 8, 6, 3}, 20)}: no durability, armour 3/8/6/3, enchantability
 * 20. 1.21.1 armour materials are registry entries and name their worn layers by path
 * ({@code textures/models/armor/<name>_layer_1.png} for the helmet, chestplate and boots, {@code _layer_2} for the
 * leggings), so the plain and the awakened armour are two materials of the same numbers with different layers.
 *
 * <p>Toughness 5 per piece (20 for the set, the largest value the attribute takes) stands in for 1.7.10's
 * {@code ISpecialArmor}, which absorbed {@code 0.0425 x armour} of every blow, 85% for the whole set, without wearing out;
 * the 1.21.1 armour formula cannot reach that (see {@code docs/deviations/KamiGear.md}).</p>
 */
public final class KamiArmorMaterials {
    public static final float TOUGHNESS = 5.0F;

    public static RegistrySupplier<ArmorMaterial> ICHOR;
    public static RegistrySupplier<ArmorMaterial> ICHOR_GEM;

    private KamiArmorMaterials() {}

    public static void register() {
        ICHOR = TTRegistry.ARMOR_MATERIALS.register("ichor", () -> create("ichor"));
        ICHOR_GEM = TTRegistry.ARMOR_MATERIALS.register("ichor_gem", () -> create("ichor_gem"));
    }

    private static ArmorMaterial create(String layer) {
        Map<ArmorItem.Type, Integer> defense = Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3);
        return new ArmorMaterial(defense, 20, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(LibMisc.id(layer))), TOUGHNESS, 0.0F);
    }
}
