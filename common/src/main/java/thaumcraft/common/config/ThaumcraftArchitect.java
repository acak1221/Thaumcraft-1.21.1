package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.wands.WandArchitect;

/** Server-owned persistent/synced Architect settings, shared by Trade and Warding. */
public final class ThaumcraftArchitect {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);
    public static final RegistrySupplier<DataComponentType<WandArchitect.Area>> AREA = COMPONENTS.register("architect_area", () ->
            DataComponentType.<WandArchitect.Area>builder().persistent(WandArchitect.Area.CODEC).networkSynchronized(WandArchitect.Area.STREAM_CODEC).build());
    private static boolean registered;
    private ThaumcraftArchitect() {}
    public static void register() { if (!registered) { registered = true; COMPONENTS.register(); } }
}
