package thaumcraft.neoforge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import thaumcraft.common.config.ThaumcraftCreativeTabs;

/**
 * The native NeoForge publication of the creative tab stacks.
 *
 * <p>Architectury 13.0.11 maps the plain accept of its creative tab output onto a NeoForge
 * "insert after the empty stack" operation, which NeoForge rejects. The published stacks are
 * therefore collected here and appended through the native NeoForge creative tab event, which is the
 * exact equivalent of the Fabric output accept.</p>
 */
final class NeoForgeCreativeTabs {
    private static final Map<net.minecraft.resources.ResourceKey<CreativeModeTab>, List<Supplier<List<ItemStack>>>> PUBLISHED =
            new LinkedHashMap<>();

    private NeoForgeCreativeTabs() {}

    static void init(net.neoforged.bus.api.IEventBus bus) {
        ThaumcraftCreativeTabs.install((tab, stacks) ->
                PUBLISHED.computeIfAbsent(tab.getKey(), ignored -> new ArrayList<>()).add(stacks));
        bus.addListener(NeoForgeCreativeTabs::onBuildContents);
    }

    private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        var publishers = PUBLISHED.get(event.getTabKey());
        if (publishers == null) return;
        for (var publisher : publishers) {
            for (var stack : publisher.get()) event.accept(stack);
        }
    }
}
