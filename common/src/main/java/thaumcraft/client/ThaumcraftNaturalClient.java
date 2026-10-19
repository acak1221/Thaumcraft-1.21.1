package thaumcraft.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.FoliageColor;
import thaumcraft.common.config.ThaumcraftNaturals;

/** Original leaf species colours; models reference already-extracted TC4 textures. */
public final class ThaumcraftNaturalClient {
    private static boolean initialized;
    private ThaumcraftNaturalClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        RenderTypeRegistry.register(RenderType.cutoutMipped(), ThaumcraftNaturals.GREATWOOD_LEAVES.get(), ThaumcraftNaturals.SILVERWOOD_LEAVES.get());
        RenderTypeRegistry.register(RenderType.cutout(), ThaumcraftNaturals.GREATWOOD_SAPLING.get(), ThaumcraftNaturals.SILVERWOOD_SAPLING.get());
        ColorHandlerRegistry.registerBlockColors((state, level, pos, tint) -> tint != 0 ? -1
                : level == null || pos == null ? FoliageColor.getDefaultColor() : BiomeColors.getAverageFoliageColor(level, pos),
                ThaumcraftNaturals.GREATWOOD_LEAVES);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? 0xff000000 | FoliageColor.getDefaultColor() : -1,
                () -> ThaumcraftNaturals.GREATWOOD_LEAVES.get().asItem());
        ColorHandlerRegistry.registerBlockColors((state, level, pos, tint) -> tint == 0 ? 0x8899aa : -1, ThaumcraftNaturals.SILVERWOOD_LEAVES);
        ColorHandlerRegistry.registerItemColors((stack, tint) -> tint == 0 ? 0xff8899aa : -1,
                () -> ThaumcraftNaturals.SILVERWOOD_LEAVES.get().asItem());
    }
}
