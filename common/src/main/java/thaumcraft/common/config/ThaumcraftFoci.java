package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.FocusUpgradeData;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.items.wands.foci.ItemFocusFrost;
import thaumcraft.common.items.wands.foci.ItemFocusShock;

/** The four implemented basic foci; absence of WAND_FOCUS means an unequipped wand. */
public final class ThaumcraftFoci {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<FocusData>> WAND_FOCUS = COMPONENTS.register("wand_focus",
            () -> DataComponentType.<FocusData>builder().persistent(FocusData.CODEC)
                    .networkSynchronized(FocusData.STREAM_CODEC).build());
    public static final RegistrySupplier<DataComponentType<FocusUpgradeData>> FOCUS_UPGRADES = COMPONENTS.register("focus_upgrades",
            () -> DataComponentType.<FocusUpgradeData>builder().persistent(FocusUpgradeData.CODEC)
                    .networkSynchronized(FocusUpgradeData.STREAM_CODEC).build());
    public static final RegistrySupplier<ItemFocusFire> FIRE_FOCUS = ITEMS.register("focus_fire",
            () -> new ItemFocusFire(new Item.Properties()));
    public static final RegistrySupplier<ItemFocusExcavation> EXCAVATION_FOCUS = ITEMS.register("focus_excavation",
            () -> new ItemFocusExcavation(new Item.Properties()));
    public static final RegistrySupplier<ItemFocusFrost> FROST_FOCUS = ITEMS.register("focus_frost",
            () -> new ItemFocusFrost(new Item.Properties()));
    public static final RegistrySupplier<ItemFocusShock> SHOCK_FOCUS = ITEMS.register("focus_shock",
            () -> new ItemFocusShock(new Item.Properties()));

    private static boolean registered;
    private ThaumcraftFoci() {}

    public static void register() {
        if (registered) return;
        registered = true;
        COMPONENTS.register();
        ITEMS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, FIRE_FOCUS);
        CreativeTabRegistry.append(ThaumcraftContent.TAB, EXCAVATION_FOCUS);
        CreativeTabRegistry.append(ThaumcraftContent.TAB, FROST_FOCUS);
        CreativeTabRegistry.append(ThaumcraftContent.TAB, SHOCK_FOCUS);
    }
}
