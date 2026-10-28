package thaumcraft.api;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;
import thaumcraft.common.config.ThaumcraftEquipment;

/**
 * The original TC4 material constants.
 *
 * <p>Forced 1.21.1 changes: {@code Item.ToolMaterial} became {@link Tier} and {@code ItemArmor.ArmorMaterial}
 * became the registry-backed {@link ArmorMaterial}. The original numbers are preserved exactly:
 * thaumium 3/400/7.0/2.0/22 with 2/6/5/2 armour and enchantability 25, void 4/150/8.0/3.0/10 with 3/7/6/3
 * and enchantability 10, fortress 40 durability with 3/7/6/3 and enchantability 25, the special armour
 * 1/3/2/1 with enchantability 25 and the void fortress armour 4/8/7/4 with enchantability 10.</p>
 */
public final class ThaumcraftMaterials {
    public static final Tier TOOLMAT_THAUMIUM = ThaumcraftEquipment.THAUMIUM_TIER;
    public static final Tier TOOLMAT_VOID = ThaumcraftEquipment.VOID_TIER;
    public static final Tier TOOLMAT_ELEMENTAL = ThaumcraftEquipment.ELEMENTAL_TIER;

    /** Original armorMatSpecial: 1/3/2/1, enchantability 25. */
    public static final ArmorMaterial ARMORMAT_SPECIAL = new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3, ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1),
            25, equipSound(), () -> Ingredient.EMPTY, List.of(), 0, 0);
    /**
     * Original armorMatVoidFortress: 4/8/7/4, durability 18, enchantability 10.
     *
     * <p>Oracle {@code thaumcraft/api/ThaumcraftApi.java:55}:
     * {@code EnumHelper.addArmorMaterial("VOIDFORTRESS", 18, new int[]{4, 8, 7, 4}, 10)}. The
     * enchantability of 1.7.10 lives on the item in 1.21.1 and has no place in the material.</p>
     */
    public static final ArmorMaterial ARMORMAT_VOID_FORTRESS = new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 8, ArmorItem.Type.LEGGINGS, 7, ArmorItem.Type.BOOTS, 4),
            18, equipSound(), () -> Ingredient.EMPTY, List.of(), 0, 0);

    private ThaumcraftMaterials() {}

    private static Holder<net.minecraft.sounds.SoundEvent> equipSound() {
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.SOUND_EVENT,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "item.armor.equip_iron")));
    }

    /** The registered native thaumium armour material. */
    public static ArmorMaterial armorMatThaumium() { return ThaumcraftEquipment.THAUMIUM_ARMOR_MATERIAL.get(); }
    /** The registered native fortress armour material. */
    public static ArmorMaterial armorMatThaumiumFortress() { return ThaumcraftEquipment.FORTRESS_ARMOR_MATERIAL.get(); }
    /** The registered native void armour material. */
    public static ArmorMaterial armorMatVoid() { return ThaumcraftEquipment.VOID_ARMOR_MATERIAL.get(); }
    /** The registered native void robe armour material. */
    public static ArmorMaterial armorMatVoidRobe() { return ThaumcraftEquipment.VOID_ROBE_ARMOR_MATERIAL.get(); }

    /** Registry key of {@link #armorMatThaumium()}: {@code thaumcraft:thaumium}. */
    public static final ResourceKey<ArmorMaterial> ARMORMAT_THAUMIUM_KEY = armorKey("thaumium");
    /** Registry key of {@link #armorMatThaumiumFortress()}: {@code thaumcraft:fortress}. */
    public static final ResourceKey<ArmorMaterial> ARMORMAT_THAUMIUM_FORTRESS_KEY = armorKey("fortress");
    /** Registry key of {@link #armorMatVoid()}: {@code thaumcraft:void}. */
    public static final ResourceKey<ArmorMaterial> ARMORMAT_VOID_KEY = armorKey("void");
    /** Registry key of {@link #armorMatVoidRobe()}: {@code thaumcraft:void_robe}. */
    public static final ResourceKey<ArmorMaterial> ARMORMAT_VOID_ROBE_KEY = armorKey("void_robe");

    private static ResourceKey<ArmorMaterial> armorKey(String path) {
        return ResourceKey.create(Registries.ARMOR_MATERIAL, ResourceLocation.fromNamespaceAndPath("thaumcraft", path));
    }

    /**
     * {@link #armorMatThaumium()} as the registry holder 1.21.1 armour items take ({@code ArmorItem} wants a
     * {@code Holder<ArmorMaterial>}); the 1.7.10 field {@code ThaumcraftApi.armorMatThaumium} was passed to
     * {@code ItemArmor} directly. The holder is the registry reference of {@link #ARMORMAT_THAUMIUM_KEY}, not a direct
     * wrapper of the object.
     *
     * @throws IllegalStateException while Thaumcraft's registration has not run
     */
    public static Holder<ArmorMaterial> armorMatThaumiumHolder() {
        return BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(ARMORMAT_THAUMIUM_KEY);
    }
    /** {@link #armorMatThaumiumFortress()} as its registry holder, see {@link #armorMatThaumiumHolder()}. */
    public static Holder<ArmorMaterial> armorMatThaumiumFortressHolder() {
        return BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(ARMORMAT_THAUMIUM_FORTRESS_KEY);
    }
    /** {@link #armorMatVoid()} as its registry holder, see {@link #armorMatThaumiumHolder()}. */
    public static Holder<ArmorMaterial> armorMatVoidHolder() {
        return BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(ARMORMAT_VOID_KEY);
    }
    /** {@link #armorMatVoidRobe()} as its registry holder, see {@link #armorMatThaumiumHolder()}. */
    public static Holder<ArmorMaterial> armorMatVoidRobeHolder() {
        return BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(ARMORMAT_VOID_ROBE_KEY);
    }

    /** The worn layer of a material; the native layers are the registered resource locations. */
    public static ArmorMaterial.Layer layer(String path) {
        return new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("thaumcraft", path));
    }
}
