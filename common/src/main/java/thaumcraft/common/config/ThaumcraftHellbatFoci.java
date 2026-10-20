package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.wands.foci.ItemFocusHellbat;

/** Hellbat uses the existing real Firebat type, not a second summoned-mob identity. */
public final class ThaumcraftHellbatFoci {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    public static final RegistrySupplier<ItemFocusHellbat> HELLBAT_FOCUS=ITEMS.register("focus_hellbat",()->new ItemFocusHellbat(new Item.Properties()));
    private static boolean registered;
    private ThaumcraftHellbatFoci() {}
    public static void register() { if(registered)return;registered=true;ITEMS.register();CreativeTabRegistry.append(ThaumcraftContent.TAB,HELLBAT_FOCUS); }
}
