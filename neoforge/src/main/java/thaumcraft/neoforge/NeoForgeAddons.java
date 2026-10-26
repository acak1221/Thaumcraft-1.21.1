package thaumcraft.neoforge;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import thaumcraft.api.IThaumcraftAddon;
import thaumcraft.common.lib.AddonLifecycle;

/**
 * The {@link IThaumcraftAddon.Addon} classes of add-ons. Common setup runs after every registry is filled; its
 * main-thread work queue keeps the hooks serial, as the 1.7.10 init phases were.
 */
final class NeoForgeAddons {
    private static final Type ADDON = Type.getType(IThaumcraftAddon.Addon.class);

    private NeoForgeAddons() {}

    static void register(IEventBus bus) {
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(NeoForgeAddons::fire));
    }

    private static void fire() {
        List<AddonLifecycle.Hook> hooks = new ArrayList<>();
        for (ModFileScanData scan : ModList.get().getAllScanData()) {
            String modId = scan.getIModInfoData().isEmpty() ? "?" : scan.getIModInfoData().getFirst().getMods().getFirst().getModId();
            for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
                if (!ADDON.equals(annotation.annotationType())) continue;
                String name = annotation.clazz().getClassName();
                try {
                    Object addon = Class.forName(name, true, NeoForgeAddons.class.getClassLoader()).getDeclaredConstructor().newInstance();
                    if (!(addon instanceof IThaumcraftAddon hook))
                        throw new IllegalStateException(name + " is annotated @IThaumcraftAddon.Addon but does not implement IThaumcraftAddon");
                    hooks.add(new AddonLifecycle.Hook(modId, hook));
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalStateException("Cannot create the Thaumcraft add-on hook " + name + " of mod '" + modId + "'", failure);
                }
            }
        }
        AddonLifecycle.fireReady(hooks);
    }
}
