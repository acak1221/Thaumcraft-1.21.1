package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.ItemWispEssence;
import thaumcraft.common.items.WispEssenceData;

/** Native identities for the original wisp essence, not a new phial or fuel. */
public final class ThaumcraftEssences {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);

    public static final RegistrySupplier<DataComponentType<WispEssenceData>> WISP_ESSENCE_DATA = COMPONENTS.register("wisp_essence_data",
            () -> DataComponentType.<WispEssenceData>builder().persistent(WispEssenceData.CODEC).networkSynchronized(WispEssenceData.STREAM_CODEC).build());
    public static final RegistrySupplier<ItemWispEssence> WISP_ESSENCE = ITEMS.register("wisp_essence", () -> new ItemWispEssence(new Item.Properties()));

    private static boolean registered;
    private ThaumcraftEssences() {}

    public static void register() {
        if (registered) return;
        registered = true;
        COMPONENTS.register();
        ITEMS.register();
        CreativeTabRegistry.modify(ThaumcraftContent.TAB, (flags, output, canUseGameMasterBlocks) -> {
            for (var stack : ItemWispEssence.creativeVariants()) output.accept(stack);
        });
    }
}
