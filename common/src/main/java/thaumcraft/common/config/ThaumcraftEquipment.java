package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.armor.FortressRules;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.items.armor.ItemThaumiumArmor;
import thaumcraft.common.items.armor.ItemVoidArmor;
import thaumcraft.common.items.armor.ItemVoidRobeArmor;
import thaumcraft.common.items.equipment.ItemVoidAxe;
import thaumcraft.common.items.equipment.ItemVoidHoe;
import thaumcraft.common.items.equipment.ItemVoidPickaxe;
import thaumcraft.common.items.equipment.ItemVoidShovel;
import thaumcraft.common.items.equipment.ItemVoidSword;
import thaumcraft.common.items.equipment.ElementalShovelGeometry;
import thaumcraft.common.items.equipment.ItemElementalAxe;
import thaumcraft.common.items.equipment.ItemElementalHoe;
import thaumcraft.common.items.equipment.ItemElementalPickaxe;
import thaumcraft.common.items.equipment.ItemElementalShovel;
import thaumcraft.common.items.equipment.ItemElementalSword;
import thaumcraft.common.items.equipment.ItemThaumiumAxe;
import thaumcraft.common.items.equipment.ItemThaumiumHoe;
import thaumcraft.common.items.equipment.ItemThaumiumPickaxe;
import thaumcraft.common.items.equipment.ItemThaumiumShovel;
import thaumcraft.common.items.equipment.ItemThaumiumSword;

/**
 * The nine ordinary Thaumium tools/armor, TC4 Api:48,51 / ConfigItems:387-404, plus the Axe of the Stream (Api:50, ConfigItems:427),
 * and the nine Void metal tools/armor (Api:38,43 / ConfigItems:489-506).
 */
public final class ThaumcraftEquipment {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Thaumcraft.MODID, Registries.ARMOR_MATERIAL);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);

    public static final Tier THAUMIUM_TIER = new Tier() {
        @Override public int getUses() { return 400; }
        @Override public float getSpeed() { return 7; }
        @Override public float getAttackDamageBonus() { return 2; }
        @Override public int getEnchantmentValue() { return 22; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
        @Override public Ingredient getRepairIngredient() { return thaumiumRepairIngredient(); }
    };

    /** Api THAUMIUM_ELEMENTAL: harvest tier 3 (diamond-level), 1500 uses, speed 10, damage bonus 3, enchantability 18. */
    public static final Tier ELEMENTAL_TIER = new Tier() {
        @Override public int getUses() { return 1500; }
        @Override public float getSpeed() { return 10; }
        @Override public float getAttackDamageBonus() { return 3; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
        @Override public Ingredient getRepairIngredient() { return thaumiumRepairIngredient(); }
    };

    /**
     * Api toolMatVoid: harvest level 4 (above diamond), 150 uses, speed 8, damage bonus 3, enchantability 10. The original never set a
     * crafting material for it, and the tools' own repair check names the item with metadata 15, the PRIMAL CHARM (their armor names the
     * void ingot, metadata 16): preserved as the original quirk. The tools mend themselves anyway.
     */
    public static final Tier VOID_TIER = new Tier() {
        @Override public int getUses() { return 150; }
        @Override public float getSpeed() { return 8; }
        @Override public float getAttackDamageBonus() { return 3; }
        @Override public int getEnchantmentValue() { return 10; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ThaumcraftContent.MATERIALS.get("primal_charm").get()); }
    };

    // Native humanoid layers and iron equip sound are 1.21 adapters, not new TC4 abilities/SFX.
    public static final RegistrySupplier<ArmorMaterial> THAUMIUM_ARMOR_MATERIAL = ARMOR_MATERIALS.register("thaumium", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6, ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 2),
            25, SoundEvents.ARMOR_EQUIP_IRON, ThaumcraftEquipment::thaumiumRepairIngredient,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "thaumium"))), 0, 0));

    /** Api armorMatVoid: durability factor 10 (set on the items), protection 3/7/6/3, enchantability 10. */
    public static final RegistrySupplier<ArmorMaterial> VOID_ARMOR_MATERIAL = ARMOR_MATERIALS.register("void", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7, ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3),
            10, SoundEvents.ARMOR_EQUIP_IRON, ThaumcraftEquipment::voidRepairIngredient,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "void"))), 0, 0));

    /**
     * Api armorMatThaumiumFortress: durability factor 40 (set on the items), protection 3/7/6/3 (no boots exist), enchantability 25, mended
     * with the thaumium ingot. INTERIM: the worn look is the Thaumium armor's layers until the original ModelFortressArmor is ported.
     */
    public static final RegistrySupplier<ArmorMaterial> FORTRESS_ARMOR_MATERIAL = ARMOR_MATERIALS.register("fortress", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7, ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3),
            25, SoundEvents.ARMOR_EQUIP_IRON, ThaumcraftEquipment::thaumiumRepairIngredient,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "thaumium"))), 0, 0));
    /** The helm's goggles upgrade and its mask (0 Grinning Devil, 1 Angry Ghost, 2 Sipping Fiend): the original's NBT "goggles" and "mask". */
    public static final RegistrySupplier<DataComponentType<Boolean>> FORTRESS_GOGGLES = COMPONENTS.register("fortress_goggles",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static final RegistrySupplier<DataComponentType<Integer>> FORTRESS_MASK = COMPONENTS.register("fortress_mask",
            () -> DataComponentType.<Integer>builder().persistent(Codec.intRange(FortressRules.MASK_DEVIL, FortressRules.MASK_FIEND))
                    .networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /**
     * The void armor material again (original ItemVoidRobeArmor takes armorMatVoid, not the unused armorMatVoidFortress), as its own
     * holder: the robes' worn look is another model. INTERIM: until the original ModelRobe (a 128 x 64 model with its own texture) is
     * ported the worn layers are the void armor's, undyed.
     */
    public static final RegistrySupplier<ArmorMaterial> VOID_ROBE_ARMOR_MATERIAL = ARMOR_MATERIALS.register("void_robe", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7, ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3),
            10, SoundEvents.ARMOR_EQUIP_IRON, ThaumcraftEquipment::voidRepairIngredient,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "void"))), 0, 0));

    public static final RegistrySupplier<ItemThaumiumSword> THAUMIUM_SWORD = ITEMS.register("thaumium_sword",
            () -> new ItemThaumiumSword(THAUMIUM_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumPickaxe> THAUMIUM_PICKAXE = ITEMS.register("thaumium_pickaxe",
            () -> new ItemThaumiumPickaxe(THAUMIUM_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumAxe> THAUMIUM_AXE = ITEMS.register("thaumium_axe",
            () -> new ItemThaumiumAxe(THAUMIUM_TIER, new Item.Properties()));
    /** Axe of the Stream, ConfigItems itemAxeElemental; obtained only by the researched infusion. */
    public static final RegistrySupplier<ItemElementalAxe> ELEMENTAL_AXE = ITEMS.register("elemental_axe",
            () -> new ItemElementalAxe(ELEMENTAL_TIER, new Item.Properties()));
    /** Pickaxe of the Core, ConfigItems itemPickElemental; obtained only by the researched infusion. */
    public static final RegistrySupplier<ItemElementalPickaxe> ELEMENTAL_PICKAXE = ITEMS.register("elemental_pickaxe",
            () -> new ItemElementalPickaxe(ELEMENTAL_TIER, new Item.Properties()));
    /** Hoe of Growth, ConfigItems itemHoeElemental; obtained only by the researched infusion. */
    public static final RegistrySupplier<ItemElementalHoe> ELEMENTAL_HOE = ITEMS.register("elemental_hoe",
            () -> new ItemElementalHoe(ELEMENTAL_TIER, new Item.Properties()));
    /** Shovel of the Earthmover, ConfigItems itemShovelElemental; obtained only by the researched infusion. */
    public static final RegistrySupplier<ItemElementalShovel> ELEMENTAL_SHOVEL = ITEMS.register("elemental_shovel",
            () -> new ItemElementalShovel(ELEMENTAL_TIER, new Item.Properties()));
    /** The shovel's placing orientation, original NBT byte "or" (0..2); it is native component state, not item NBT. */
    public static final RegistrySupplier<DataComponentType<Integer>> SHOVEL_ORIENTATION = COMPONENTS.register("shovel_orientation",
            () -> DataComponentType.<Integer>builder().persistent(Codec.intRange(0, ElementalShovelGeometry.ORIENTATIONS - 1))
                    .networkSynchronized(ByteBufCodecs.VAR_INT).build());
    /** Sword of the Zephyr, ConfigItems itemSwordElemental; obtained only by the researched infusion. */
    public static final RegistrySupplier<ItemElementalSword> ELEMENTAL_SWORD = ITEMS.register("elemental_sword",
            () -> new ItemElementalSword(ELEMENTAL_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumShovel> THAUMIUM_SHOVEL = ITEMS.register("thaumium_shovel",
            () -> new ItemThaumiumShovel(THAUMIUM_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumHoe> THAUMIUM_HOE = ITEMS.register("thaumium_hoe",
            () -> new ItemThaumiumHoe(THAUMIUM_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumArmor> THAUMIUM_HELMET = ITEMS.register("thaumium_helmet",
            () -> new ItemThaumiumArmor(THAUMIUM_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumArmor> THAUMIUM_CHESTPLATE = ITEMS.register("thaumium_chestplate",
            () -> new ItemThaumiumArmor(THAUMIUM_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumArmor> THAUMIUM_LEGGINGS = ITEMS.register("thaumium_leggings",
            () -> new ItemThaumiumArmor(THAUMIUM_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<ItemThaumiumArmor> THAUMIUM_BOOTS = ITEMS.register("thaumium_boots",
            () -> new ItemThaumiumArmor(THAUMIUM_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistrySupplier<ItemVoidSword> VOID_SWORD = ITEMS.register("void_sword",
            () -> new ItemVoidSword(VOID_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidPickaxe> VOID_PICKAXE = ITEMS.register("void_pickaxe",
            () -> new ItemVoidPickaxe(VOID_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidAxe> VOID_AXE = ITEMS.register("void_axe",
            () -> new ItemVoidAxe(VOID_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidShovel> VOID_SHOVEL = ITEMS.register("void_shovel",
            () -> new ItemVoidShovel(VOID_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidHoe> VOID_HOE = ITEMS.register("void_hoe",
            () -> new ItemVoidHoe(VOID_TIER, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidArmor> VOID_HELMET = ITEMS.register("void_helmet",
            () -> new ItemVoidArmor(VOID_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidArmor> VOID_CHESTPLATE = ITEMS.register("void_chestplate",
            () -> new ItemVoidArmor(VOID_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidArmor> VOID_LEGGINGS = ITEMS.register("void_leggings",
            () -> new ItemVoidArmor(VOID_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidArmor> VOID_BOOTS = ITEMS.register("void_boots",
            () -> new ItemVoidArmor(VOID_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistrySupplier<ItemFortressArmor> FORTRESS_HELMET = ITEMS.register("fortress_helmet",
            () -> new ItemFortressArmor(FORTRESS_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<ItemFortressArmor> FORTRESS_CHESTPLATE = ITEMS.register("fortress_chestplate",
            () -> new ItemFortressArmor(FORTRESS_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemFortressArmor> FORTRESS_LEGGINGS = ITEMS.register("fortress_leggings",
            () -> new ItemFortressArmor(FORTRESS_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidRobeArmor> VOID_ROBE_HELMET = ITEMS.register("void_robe_helmet",
            () -> new ItemVoidRobeArmor(VOID_ROBE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidRobeArmor> VOID_ROBE_CHESTPLATE = ITEMS.register("void_robe_chestplate",
            () -> new ItemVoidRobeArmor(VOID_ROBE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemVoidRobeArmor> VOID_ROBE_LEGGINGS = ITEMS.register("void_robe_leggings",
            () -> new ItemVoidRobeArmor(VOID_ROBE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    private static boolean registered;
    private ThaumcraftEquipment() {}

    private static Ingredient thaumiumRepairIngredient() {
        // Original explicit itemResource:2 repair checks are NOT ore-dictionary
        // recipe matching. Do not widen repair to every c:ingots/thaumium item.
        return Ingredient.of(ThaumcraftContent.MATERIALS.get("thaumium_ingot").get());
    }

    private static Ingredient voidRepairIngredient() {
        return Ingredient.of(ThaumcraftContent.MATERIALS.get("void_ingot").get());
    }

    public static void register() {
        if (registered) return;
        registered = true;
        ARMOR_MATERIALS.register();
        COMPONENTS.register();
        ITEMS.register();
        // Architectury's supplier varargs require one invariant item type;
        // these registered suppliers intentionally retain their specific APIs.
        for (RegistrySupplier<? extends Item> item : List.of(THAUMIUM_SWORD, THAUMIUM_PICKAXE, THAUMIUM_AXE, ELEMENTAL_AXE, ELEMENTAL_PICKAXE, ELEMENTAL_HOE, ELEMENTAL_SHOVEL, ELEMENTAL_SWORD,
                THAUMIUM_SHOVEL, THAUMIUM_HOE, THAUMIUM_HELMET, THAUMIUM_CHESTPLATE, THAUMIUM_LEGGINGS, THAUMIUM_BOOTS,
                VOID_SWORD, VOID_PICKAXE, VOID_AXE, VOID_SHOVEL, VOID_HOE, VOID_HELMET, VOID_CHESTPLATE, VOID_LEGGINGS, VOID_BOOTS,
                VOID_ROBE_HELMET, VOID_ROBE_CHESTPLATE, VOID_ROBE_LEGGINGS, FORTRESS_HELMET, FORTRESS_CHESTPLATE, FORTRESS_LEGGINGS)) {
            CreativeTabRegistry.append(ThaumcraftContent.TAB, item);
        }
    }
}
