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
    // cap x rod price: the dearest native rod is the primal staff (32), the dearest native cap the void one (9).
    // A sceptre of the same materials costs half as much again (432), and it carries this metadata too. Add-on
    // parts may cost more, so the range is the original one: the item metadata the original wrote (a short).
    private static final Codec<Integer> CRAFT_COST_CODEC = Codec.intRange(0, WandMaterials.MAX_ASSEMBLY_COST);

    public static final RegistrySupplier<Item> GREATWOOD_WAND_ROD = ITEMS.register("wand_rod_greatwood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SILVERWOOD_WAND_ROD = ITEMS.register("wand_rod_silverwood", () -> new Item(new Item.Properties()));
    // Original ItemWandRod metadata 1, 6, 3, 4, 7, 5: the elemental rods, each refilling its own primal in the wand.
    public static final RegistrySupplier<Item> OBSIDIAN_WAND_ROD = ITEMS.register("wand_rod_obsidian", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLAZE_WAND_ROD = ITEMS.register("wand_rod_blaze", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICE_WAND_ROD = ITEMS.register("wand_rod_ice", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> QUARTZ_WAND_ROD = ITEMS.register("wand_rod_quartz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BONE_WAND_ROD = ITEMS.register("wand_rod_bone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REED_WAND_ROD = ITEMS.register("wand_rod_reed", () -> new Item(new Item.Properties()));
    // Original ItemWandRod metadata 50-57 and 100: the staff cores (ConfigItems:304-313); a staff is a wand with one of these as its rod.
    public static final RegistrySupplier<Item> GREATWOOD_STAFF_ROD = ITEMS.register("staff_rod_greatwood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> OBSIDIAN_STAFF_ROD = ITEMS.register("staff_rod_obsidian", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> SILVERWOOD_STAFF_ROD = ITEMS.register("staff_rod_silverwood", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ICE_STAFF_ROD = ITEMS.register("staff_rod_ice", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> QUARTZ_STAFF_ROD = ITEMS.register("staff_rod_quartz", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> REED_STAFF_ROD = ITEMS.register("staff_rod_reed", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BLAZE_STAFF_ROD = ITEMS.register("staff_rod_blaze", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> BONE_STAFF_ROD = ITEMS.register("staff_rod_bone", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> PRIMAL_STAFF_ROD = ITEMS.register("staff_rod_primal", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> GOLD_WAND_CAP = ITEMS.register("wand_cap_gold", () -> new Item(new Item.Properties()));
    /** Original ItemWandCap metadata 3: the copper cap (ConfigItems:331). */
    public static final RegistrySupplier<Item> COPPER_WAND_CAP = ITEMS.register("wand_cap_copper", () -> new Item(new Item.Properties()));
    /** Crafting ingredient alias; ThaumcraftContent owns the original ItemNugget behavior. */
    public static final RegistrySupplier<Item> THAUMIUM_NUGGET = ThaumcraftContent.MATERIALS.get("thaumium_nugget");
    public static final RegistrySupplier<Item> THAUMIUM_WAND_CAP_INERT = ITEMS.register("wand_cap_thaumium_inert", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> THAUMIUM_WAND_CAP = ITEMS.register("wand_cap_thaumium", () -> new Item(new Item.Properties()));
    // Original ItemNugget metadata 7 and ItemWandCap metadata 8 (inert) and 7: the void metal cap and the nugget it is made of.
    public static final RegistrySupplier<Item> VOID_NUGGET = ThaumcraftContent.MATERIALS.get("void_nugget");
    public static final RegistrySupplier<Item> VOID_WAND_CAP_INERT = ITEMS.register("wand_cap_void_inert", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> VOID_WAND_CAP = ITEMS.register("wand_cap_void", () -> new Item(new Item.Properties()));
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
        CreativeTabRegistry.append(ThaumcraftContent.TAB, GREATWOOD_WAND_ROD, SILVERWOOD_WAND_ROD, OBSIDIAN_WAND_ROD, BLAZE_WAND_ROD, ICE_WAND_ROD,
                QUARTZ_WAND_ROD, BONE_WAND_ROD, REED_WAND_ROD, GREATWOOD_STAFF_ROD, OBSIDIAN_STAFF_ROD, BLAZE_STAFF_ROD, ICE_STAFF_ROD,
                QUARTZ_STAFF_ROD, BONE_STAFF_ROD, REED_STAFF_ROD, SILVERWOOD_STAFF_ROD, PRIMAL_STAFF_ROD, GOLD_WAND_CAP, COPPER_WAND_CAP,
                THAUMIUM_WAND_CAP_INERT, THAUMIUM_WAND_CAP, VOID_WAND_CAP_INERT, VOID_WAND_CAP);
    }

    /** The codec of the original wand craft cost metadata: the item metadata of the assembled wand. */
    public static Codec<Integer> craftCostCodec() { return CRAFT_COST_CODEC; }

    /** Fresh canonical material stacks; no singleton ItemStack is exposed for mutation. */
    public static ItemStack rodItem(String tag) {
        WandMaterials.rod(tag);
        return new ItemStack(switch (tag) {
            case "wood" -> Items.STICK;
            case "greatwood" -> GREATWOOD_WAND_ROD.get();
            case "silverwood" -> SILVERWOOD_WAND_ROD.get();
            case "obsidian" -> OBSIDIAN_WAND_ROD.get();
            case "blaze" -> BLAZE_WAND_ROD.get();
            case "ice" -> ICE_WAND_ROD.get();
            case "quartz" -> QUARTZ_WAND_ROD.get();
            case "bone" -> BONE_WAND_ROD.get();
            case "reed" -> REED_WAND_ROD.get();
            case "greatwood_staff" -> GREATWOOD_STAFF_ROD.get();
            case "obsidian_staff" -> OBSIDIAN_STAFF_ROD.get();
            case "blaze_staff" -> BLAZE_STAFF_ROD.get();
            case "ice_staff" -> ICE_STAFF_ROD.get();
            case "quartz_staff" -> QUARTZ_STAFF_ROD.get();
            case "bone_staff" -> BONE_STAFF_ROD.get();
            case "reed_staff" -> REED_STAFF_ROD.get();
            case "silverwood_staff" -> SILVERWOOD_STAFF_ROD.get();
            case "primal_staff" -> PRIMAL_STAFF_ROD.get();
            default -> throw new IllegalArgumentException("Unsupported wand rod: " + tag);
        });
    }

    public static ItemStack capItem(String tag) {
        WandMaterials.cap(tag);
        return new ItemStack(switch (tag) {
            case "iron" -> ThaumcraftContent.IRON_WAND_CAP.get();
            case "gold" -> GOLD_WAND_CAP.get();
            case "thaumium" -> THAUMIUM_WAND_CAP.get();
            case "copper" -> COPPER_WAND_CAP.get();
            case "void" -> VOID_WAND_CAP.get();
            default -> throw new IllegalArgumentException("Unsupported wand cap: " + tag);
        });
    }
}
