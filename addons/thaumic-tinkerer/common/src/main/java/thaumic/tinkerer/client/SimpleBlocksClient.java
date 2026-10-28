/**
 * This class is new in the port of Thaumic Tinkerer to Minecraft 1.21.1 for Thaumcraft 4 Reload (the client side of
 * the 1.7.10 renderers RenderInfusedCrops and RenderGenericSeeds, which have no counterpart as classes); it is
 * distributed under the same licence as the add-on (Creative Commons Attribution-NonCommercial-ShareAlike 3.0, see
 * addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.client.render.tile.RenderTileCamo;
import thaumic.tinkerer.common.block.BlockInfusedGrain;
import thaumic.tinkerer.common.block.tile.TileInfusedGrain;
import thaumic.tinkerer.common.content.SimpleBlocks;
import thaumic.tinkerer.common.item.ItemInfusedSeeds;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Client registrations of the SimpleBlocks group.
 *
 * <p>{@code RenderInfusedCrops} of 1.7.10 drew the crop of a texture group (aer, ignis, aqua, terra, ordo, perditio,
 * generic) at the age of the metadata, tinted with the colour of the aspect when it was not primal: that is the
 * blockstate file of the crop, the cutout render layer and the colour handler below. {@code RenderGenericSeeds}
 * picked the icon of the seeds by their aspect: that is the {@code thaumictinkerer:aspect_texture} item property and
 * the overrides of the model of the seeds. The camouflage of the platform is {@link RenderTileCamo}.</p>
 */
public final class SimpleBlocksClient {
    /** The item property of the infused seeds: the texture group of their aspect (0..6, 6 for any other). */
    public static final String ASPECT_TEXTURE_PROPERTY = "aspect_texture";

    private SimpleBlocksClient() {}

    public static void register() {
        // translucent pixels of the crop and the fire models
        RenderTypeRegistry.register(RenderType.cutout(), SimpleBlocks.INFUSED_GRAIN.get(), SimpleBlocks.FIRE_AIR.get(),
                SimpleBlocks.FIRE_WATER.get(), SimpleBlocks.FIRE_EARTH.get(), SimpleBlocks.FIRE_FIRE.get(),
                SimpleBlocks.FIRE_ORDER.get(), SimpleBlocks.FIRE_CHAOS.get());

        // RenderInfusedCrops: the generic crop takes the colour of its aspect
        BlockColor grainColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) return 0xFFFFFF;
            Aspect aspect = level.getBlockEntity(pos) instanceof TileInfusedGrain tile ? tile.aspect : null;
            return aspect == null || aspect.isPrimal() ? 0xFFFFFF : aspect.getColor();
        };
        ColorHandlerRegistry.registerBlockColors(grainColor, (Block) SimpleBlocks.INFUSED_GRAIN.get());

        // RenderGenericSeeds: the icon of the seeds follows their aspect
        ItemPropertiesRegistry.register(SimpleBlocks.INFUSED_SEEDS.get(), LibMisc.id(ASPECT_TEXTURE_PROPERTY),
                (stack, level, entity, seed) -> {
                    Aspect aspect = ItemInfusedSeeds.getAspect(stack);
                    return aspect == null ? 0 : BlockInfusedGrain.getNumberFromAspectForTexture(aspect);
                });

        // the camouflage of the Ethereal Platform
        BlockEntityRendererRegistry.register(SimpleBlocks.TILE_CAMO.get(), RenderTileCamo::new);
    }
}
