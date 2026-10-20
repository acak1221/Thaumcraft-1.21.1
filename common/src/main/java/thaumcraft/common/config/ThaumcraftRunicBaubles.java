package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.armor.ItemCultistBoots;
import thaumcraft.common.items.armor.ItemCultistLeaderArmor;
import thaumcraft.common.items.armor.ItemCultistPlateArmor;
import thaumcraft.common.items.armor.ItemCultistRobeArmor;
import thaumcraft.common.items.baubles.ItemAmuletRunic;
import thaumcraft.common.items.baubles.ItemBaubleBlanks;
import thaumcraft.common.items.baubles.ItemGirdleRunic;
import thaumcraft.common.items.baubles.ItemRingRunic;
import thaumcraft.common.items.wands.ItemWandCap;
import thaumcraft.common.items.wands.ItemWandRod;
import thaumcraft.common.items.wands.WandRodPrimalOnUpdate;
import thaumcraft.common.lib.baubles.RunicBaubleRules;

/**
 * The registration point of the original accessory family that the native {@link ThaumcraftBaubles} does not own: the ten pieces
 * of the crimson cult armour that the cultist mobs wear, and the dyeing recipe of the void thaumaturge robes. The runic
 * accessories themselves (the amulet, the ring, the girdle and the blank baubles) stay registered by {@link ThaumcraftBaubles};
 * their original metadata numbering is kept by {@link thaumcraft.common.items.baubles.ItemAmuletRunic},
 * {@link thaumcraft.common.items.baubles.ItemRingRunic}, {@link thaumcraft.common.items.baubles.ItemGirdleRunic} and
 * {@link thaumcraft.common.items.baubles.ItemBaubleBlanks}, and the original wand rod and cap metadata by
 * {@link thaumcraft.common.items.wands.ItemWandRod} and {@link thaumcraft.common.items.wands.ItemWandCap}. The bridges below
 * resolve an original metadata value to the registered native item.
 *
 * <p>Original sources: ConfigItems:529-548, ItemCultistBoots, ItemCultistRobeArmor, ItemCultistPlateArmor,
 * ItemCultistLeaderArmor and RecipesVoidRobeArmorDyes.
 */
public final class ThaumcraftRunicBaubles {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Thaumcraft.MODID, Registries.ARMOR_MATERIAL);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);

    // The cultist robe, plate and boots use the vanilla iron material: protection 2/6/5/2 and the iron enchantability 15. The
    // Praetor uses the original armorMatThaumiumFortress: protection 3/7/6/3 and the enchantability 25. Every piece is mended
    // with an iron ingot, like the original repair check. The worn layers are the original cultist textures; they are the
    // 1.21.1 adaptation of the original ModelRobe, ModelKnightArmor and ModelLeaderArmor.
    public static final RegistrySupplier<ArmorMaterial> CULTIST_ROBE_ARMOR_MATERIAL = ARMOR_MATERIALS.register("cultist_robe", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6, ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 2),
            15, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cultist_robe"), "", true),
                    new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cultist_robe"), "_overlay", false)), 0, 0));
    public static final RegistrySupplier<ArmorMaterial> CULTIST_PLATE_ARMOR_MATERIAL = ARMOR_MATERIALS.register("cultist_plate", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6, ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 2),
            15, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cultist_plate"), "", true)), 0, 0));
    public static final RegistrySupplier<ArmorMaterial> CULTIST_LEADER_ARMOR_MATERIAL = ARMOR_MATERIALS.register("cultist_leader", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7, ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3),
            25, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cultist_leader"), "", true)), 0, 0));
    public static final RegistrySupplier<ArmorMaterial> CULTIST_BOOTS_ARMOR_MATERIAL = ARMOR_MATERIALS.register("cultist_boots", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6, ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 2),
            15, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cultist_boots"), "", true)), 0, 0));

    public static final RegistrySupplier<ItemCultistRobeArmor> CULTIST_ROBE_HOOD = ITEMS.register("cultist_robe_hood",
            () -> new ItemCultistRobeArmor(CULTIST_ROBE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistRobeArmor> CULTIST_ROBE = ITEMS.register("cultist_robe",
            () -> new ItemCultistRobeArmor(CULTIST_ROBE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistRobeArmor> CULTIST_ROBE_LEGS = ITEMS.register("cultist_robe_legs",
            () -> new ItemCultistRobeArmor(CULTIST_ROBE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistPlateArmor> CULTIST_PLATE_HELM = ITEMS.register("cultist_plate_helm",
            () -> new ItemCultistPlateArmor(CULTIST_PLATE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistPlateArmor> CULTIST_PLATE = ITEMS.register("cultist_plate",
            () -> new ItemCultistPlateArmor(CULTIST_PLATE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistPlateArmor> CULTIST_PLATE_LEGS = ITEMS.register("cultist_plate_legs",
            () -> new ItemCultistPlateArmor(CULTIST_PLATE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistLeaderArmor> CULTIST_LEADER_HELM = ITEMS.register("cultist_leader_helm",
            () -> new ItemCultistLeaderArmor(CULTIST_LEADER_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistLeaderArmor> CULTIST_LEADER = ITEMS.register("cultist_leader",
            () -> new ItemCultistLeaderArmor(CULTIST_LEADER_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistLeaderArmor> CULTIST_LEADER_LEGS = ITEMS.register("cultist_leader_legs",
            () -> new ItemCultistLeaderArmor(CULTIST_LEADER_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<ItemCultistBoots> CULTIST_BOOTS = ITEMS.register("cultist_boots",
            () -> new ItemCultistBoots(CULTIST_BOOTS_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties()));

    /** The original RecipesVoidRobeArmorDyes, the dyeing recipe of the void thaumaturge robes. */
    // The original had a second, identical dye recipe for the void robes
    // (RecipesRobeArmorDyes and RecipesVoidRobeArmorDyes). The native port
    // serves both robe families with the single thaumcraft:robe_dye recipe,
    // which matches every DyedRobe.

    // The original ConfigItems:529-548 order, so the creative tab lists the pieces as the original did.
    private static final Map<String, RegistrySupplier<? extends Item>> CULTIST_PIECES = pieceMap();

    private static Map<String, RegistrySupplier<? extends Item>> pieceMap() {
        Map<String, RegistrySupplier<? extends Item>> pieces = new java.util.LinkedHashMap<>();
        pieces.put("cultist_robe_hood", CULTIST_ROBE_HOOD);
        pieces.put("cultist_robe", CULTIST_ROBE);
        pieces.put("cultist_robe_legs", CULTIST_ROBE_LEGS);
        pieces.put("cultist_boots", CULTIST_BOOTS);
        pieces.put("cultist_plate_helm", CULTIST_PLATE_HELM);
        pieces.put("cultist_plate", CULTIST_PLATE);
        pieces.put("cultist_plate_legs", CULTIST_PLATE_LEGS);
        pieces.put("cultist_leader_helm", CULTIST_LEADER_HELM);
        pieces.put("cultist_leader", CULTIST_LEADER);
        pieces.put("cultist_leader_legs", CULTIST_LEADER_LEGS);
        return java.util.Collections.unmodifiableMap(pieces);
    }

    private static boolean registered;
    private ThaumcraftRunicBaubles() {}

    /** The registered cultist piece of a flattened id, for the cultist mobs that wear it. */
    public static ItemStack cultistPiece(String id) {
        RegistrySupplier<? extends Item> piece = CULTIST_PIECES.get(id);
        if (piece == null) throw new IllegalArgumentException("No cultist armour piece: " + id);
        return new ItemStack(piece.get());
    }

    /** The registered runic accessory of an original metadata value: the amulet, the ring or the girdle. */
    public static ItemStack runicAccessory(RunicBaubleRules.Family family, int metadata) {
        return switch (family) {
            case AMULET -> ItemAmuletRunic.stack(metadata);
            case RING -> ItemRingRunic.stack(metadata);
            case GIRDLE -> ItemGirdleRunic.stack(metadata);
        };
    }

    /** The registered blank bauble of an original metadata value. */
    public static ItemStack blankBauble(int metadata) { return ItemBaubleBlanks.stack(metadata); }

    /** The registered wand rod of an original metadata value. */
    public static ItemStack wandRod(int metadata) { return ItemWandRod.stack(metadata); }

    /** The registered wand cap of an original metadata value. */
    public static ItemStack wandCap(int metadata) { return ItemWandCap.stack(metadata); }

    /** The passive of a wand rod: an elemental rod refills its own primal, the primal staff any of them. */
    public static WandRodPrimalOnUpdate rodPassive(int metadata) { return WandRodPrimalOnUpdate.of(metadata); }

    /** The original cultist piece table: the id, the slot, the protection, the durability, the rarity, the discount and the warp. */
    public static List<RunicBaubleRules.CultistPiece> cultistPieces() { return RunicBaubleRules.cultistArmor(); }

    public static void register() {
        if (registered) return;
        registered = true;
        ARMOR_MATERIALS.register();
        ITEMS.register();
        SERIALIZERS.register();
        for (RegistrySupplier<? extends Item> piece : CULTIST_PIECES.values()) CreativeTabRegistry.append(ThaumcraftContent.TAB, piece);
    }
}
