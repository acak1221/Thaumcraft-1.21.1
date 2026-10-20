package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.ContainerFocusPouch;
import thaumcraft.common.items.wands.ItemFocusPouchBauble;

public final class ThaumcraftFocusPouches {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Thaumcraft.MODID,Registries.ITEM);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Thaumcraft.MODID,Registries.MENU);
    public static final RegistrySupplier<ItemFocusPouchBauble> FOCUS_POUCH=ITEMS.register("focus_pouch",()->new ItemFocusPouchBauble(new Item.Properties()));
    public static final RegistrySupplier<MenuType<ContainerFocusPouch>> MENU=MENUS.register("focus_pouch",()->MenuRegistry.ofExtended(ContainerFocusPouch::new));
    private static boolean registered;
    private ThaumcraftFocusPouches() {}
    public static void register() { if(registered)return;registered=true;ITEMS.register();MENUS.register();CreativeTabRegistry.append(ThaumcraftContent.TAB,FOCUS_POUCH); }
}
