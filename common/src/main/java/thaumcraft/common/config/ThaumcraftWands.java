package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.items.wands.WandMaterials;

/** Register after ThaumcraftContent/ThaumcraftArcane; the basic iron cap and wooden rod already exist. */
public final class ThaumcraftWands {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumcraft.MODID, Registries.RECIPE_SERIALIZER);
    private static final Codec<Integer> CRAFT_COST_CODEC = Codec.intRange(0, 54);

    public static final RegistrySupplier<Item> GREATWOOD_WAND_ROD = ITEMS.register("wand_rod_greatwood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SILVERWOOD_WAND_ROD = ITEMS.register("wand_rod_silverwood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GOLD_WAND_CAP = ITEMS.register("wand_cap_gold", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THAUMIUM_NUGGET = ITEMS.register("thaumium_nugget", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THAUMIUM_WAND_CAP_INERT = ITEMS.register("wand_cap_thaumium_inert", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THAUMIUM_WAND_CAP = ITEMS.register("wand_cap_thaumium", () -> new Item(new Item.Properties()));
    // Original ArcaneWandRecipe:59 writes cc*cr to item metadata. Preserve it
    // explicitly rather than misusing modern damage/custom_model_data as metadata.
    public static final RegistrySupplier<DataComponentType<Integer>> WAND_CRAFT_COST = COMPONENTS.register("wand_craft_cost",
            () -> DataComponentType.<Integer>builder().persistent(CRAFT_COST_CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(CRAFT_COST_CODEC)).build());
    public static final RegistrySupplier<RecipeSerializer<ArcaneWandRecipe>> ARCANE_WAND_SERIALIZER = SERIALIZERS.register("arcane_wand", ArcaneWandRecipe.Serializer::new);

    private ThaumcraftWands() {}

    public static void register() {
        COMPONENTS.register();
        ITEMS.register();
        SERIALIZERS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, GREATWOOD_WAND_ROD, SILVERWOOD_WAND_ROD, GOLD_WAND_CAP,
                THAUMIUM_NUGGET, THAUMIUM_WAND_CAP_INERT, THAUMIUM_WAND_CAP);
    }

    /** Fresh canonical material stacks; no singleton ItemStack is exposed for mutation. */
    public static ItemStack rodItem(String tag) {
        WandMaterials.rod(tag);
        return new ItemStack(switch (tag) {
            case "wood" -> Items.STICK;
            case "greatwood" -> GREATWOOD_WAND_ROD.get();
            case "silverwood" -> SILVERWOOD_WAND_ROD.get();
            default -> throw new IllegalArgumentException("Unsupported wand rod: " + tag);
        });
    }

    public static ItemStack capItem(String tag) {
        WandMaterials.cap(tag);
        return new ItemStack(switch (tag) {
            case "iron" -> ThaumcraftContent.IRON_WAND_CAP.get();
            case "gold" -> GOLD_WAND_CAP.get();
            case "thaumium" -> THAUMIUM_WAND_CAP.get();
            default -> throw new IllegalArgumentException("Unsupported wand cap: " + tag);
        });
    }
}
