package thaumcraft.common.config;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.container.ContainerGhostSlots;
import thaumcraft.common.container.ContainerHandMirror;
import thaumcraft.common.container.ContainerMagicBox;

/**
 * Menu types of the machine and device GUIs that the container layer owns:
 * the magic box, the hand mirror and the shared ghost slot preview.
 */
public final class ThaumcraftDeviceMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);
    public static final RegistrySupplier<MenuType<ContainerMagicBox>> MAGIC_BOX = MENUS.register("magic_box", () -> MenuRegistry.ofExtended(ContainerMagicBox::new));
    public static final RegistrySupplier<MenuType<ContainerHandMirror>> HAND_MIRROR = MENUS.register("hand_mirror", () -> MenuRegistry.ofExtended(ContainerHandMirror::new));
    public static final RegistrySupplier<MenuType<ContainerGhostSlots>> GHOST_SLOTS = MENUS.register("ghost_slots", () -> MenuRegistry.ofExtended(ThaumcraftDeviceMenus::ghostSlotsMenu));
    private static boolean registered;
    private ThaumcraftDeviceMenus() {}

    /** The ghost preview menu needs its own menu type, so the type is read here and not in the initializer. */
    private static ContainerGhostSlots ghostSlotsMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        boolean fluid = buffer.readBoolean();
        int ghosts = buffer.readVarInt();
        return new ContainerGhostSlots(GHOST_SLOTS.get(), id, inventory, ghosts, new SimpleContainer(ghosts), fluid);
    }

    public static void register() { if (registered) return; registered = true; MENUS.register(); }
}
