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
 * <p>The numbers are the display of 1.7.10's {@code ISpecialArmor.getArmorDisplay} (the HUD showed 3/8/6/3, twenty points for the
 * set); the toughness is 0, as the original had none. What the pieces really absorb is not this formula but 1.7.10's
 * {@code ArmorProperties(0, points * 0.0425, MAX)}: 85% of every blow that armour reduces for the full set. It is applied by
 * {@link IchorAbsorption} through {@code TinkererEvents.LIVING_ARMOR_ABSORB}, which <i>replaces</i> the amount of the 1.21.1
 * formula, so these attribute values never stack with it (see {@code docs/deviations/KamiGear.md} section 3).</p>
 *
 * <p>The array is laid out by armour type (chestplate 8, leggings 6). 1.7.10 may have indexed it by the slot in {@code armorInventory}
 * (boots first), which would have swapped chestplate and leggings for the absorption and the HUD of a partial set; that could not be
 * verified and is not carried over (section 3 of the same document, "open point").</p>
 */
public final class KamiArmorMaterials {
    /** The original has no toughness. */
    public static final float TOUGHNESS = 0.0F;

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
