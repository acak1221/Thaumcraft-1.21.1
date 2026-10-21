package thaumcraft.common.config;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Publication of the creative tab stacks of the port.
 *
 * <p>The shared code publishes its tab stacks here instead of accepting them into the Architectury
 * output directly. Fabric keeps the Architectury output, which appends the stacks. NeoForge 13.0.11
 * of Architectury maps the plain output accept onto a NeoForge "insert after the empty stack"
 * operation, and NeoForge rejects an empty target with "Itemstack 0 minecraft:air does not exist
 * in tab's list". The NeoForge module therefore installs its own sink, which publishes the same
 * stacks through the native NeoForge creative tab event.</p>
 *
 * <p>The stacks are published as a supplier: the NeoForge registry is only filled after the mod
 * construction, so the item stacks of the tab must not be resolved before the tab is built.</p>
 */
public final class ThaumcraftCreativeTabs {
    /** The loader sink of the published tab stacks. */
    public interface Sink {
        void publish(RegistrySupplier<CreativeModeTab> tab, Supplier<List<ItemStack>> stacks);
    }

    private static Sink sink = (tab, stacks) -> CreativeTabRegistry.modify(tab, (flags, output, canUseGameMasterBlocks) -> {
        for (var stack : stacks.get()) output.accept(stack);
    });

    private ThaumcraftCreativeTabs() {}

    /** Installs the native sink of the loader. The Fabric default keeps the Architectury output. */
    public static void install(Sink nativeSink) {
        if (nativeSink == null) throw new IllegalArgumentException("The creative tab sink must not be null");
        sink = nativeSink;
    }

    /** Publishes the stacks of a creative tab. The stacks are resolved when the tab is built. */
    public static void publish(RegistrySupplier<CreativeModeTab> tab, Supplier<List<ItemStack>> stacks) {
        if (stacks == null) throw new IllegalArgumentException("The creative tab stacks must not be null");
        sink.publish(tab, stacks);
    }
}
