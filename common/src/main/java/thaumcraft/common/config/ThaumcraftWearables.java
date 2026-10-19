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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.armor.ItemGoggles;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.armor.RecipesRobeArmorDyes;

/** Original Api:52 / ConfigItems:385-386,431-436: goggles and enchanted robes. */
public final class ThaumcraftWearables {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Thaumcraft.MODID, Registries.ARMOR_MATERIAL);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);

    // Gold equip sound and native humanoid texture layers are explicit 1.21 adapters.
    // The goggles override SPECIAL's ordinary helmet durability with the original350.
    public static final RegistrySupplier<ArmorMaterial> SPECIAL_ARMOR_MATERIAL = ARMOR_MATERIALS.register("special", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3, ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1),
            25, SoundEvents.ARMOR_EQUIP_GOLD, () -> Ingredient.of(Items.GOLD_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "goggles"))), 0, 0));

    public static final RegistrySupplier<ItemGoggles> GOGGLES = ITEMS.register("goggles_revealing",
            () -> new ItemGoggles(SPECIAL_ARMOR_MATERIAL, new Item.Properties()));

    // Same original SPECIAL defenses/enchantability, but a separate native holder:
    // goggles' gold repair and single texture layer must NOT leak into the robes.
    // Native leather equip sound is an explicit cloth-sound adapter (TC4 had none).
    public static final RegistrySupplier<ArmorMaterial> ROBE_ARMOR_MATERIAL = ARMOR_MATERIALS.register("robe", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3, ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1),
            25, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "robes"), "", true),
                    // Native Layer appends the suffix literally, including its separator.
                    new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "robes"), "_overlay", false)), 0, 0));
    public static final RegistrySupplier<ItemRobeArmor> ROBE_CHESTPLATE = ITEMS.register("robe_chestplate",
            () -> new ItemRobeArmor(ROBE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistrySupplier<ItemRobeArmor> ROBE_LEGGINGS = ITEMS.register("robe_leggings",
            () -> new ItemRobeArmor(ROBE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistrySupplier<ItemRobeArmor> ROBE_BOOTS = ITEMS.register("robe_boots",
            () -> new ItemRobeArmor(ROBE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistrySupplier<RecipeSerializer<RecipesRobeArmorDyes>> ROBE_DYE_RECIPE_SERIALIZER = SERIALIZERS.register("robe_dye",
            () -> new SimpleCraftingRecipeSerializer<>(RecipesRobeArmorDyes::new));

    private static boolean registered;
    private ThaumcraftWearables() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ARMOR_MATERIALS.register();
        ITEMS.register();
        SERIALIZERS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, GOGGLES);
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ROBE_CHESTPLATE, ROBE_LEGGINGS, ROBE_BOOTS);
    }
}
