package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import thaumcraft.client.renderers.tile.TileBannerRenderer;
import thaumcraft.client.renderers.tile.TileBrainboxRenderer;
import thaumcraft.client.renderers.tile.TileEldritchNothingRenderer;
import thaumcraft.client.renderers.tile.TileEldritchPortalRenderer;
import thaumcraft.client.renderers.tile.TileEldritchRenderer;
import thaumcraft.client.renderers.tile.TileEtherealBloomRenderer;
import thaumcraft.client.renderers.tile.TileFluxScrubberRenderer;
import thaumcraft.client.renderers.tile.TileHoleRenderer;
import thaumcraft.client.renderers.tile.TileMirrorRenderer;
import thaumcraft.client.renderers.tile.TileWandPedestalRenderer;
import thaumcraft.client.renderers.tile.TileWardedGlassRenderer;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftHoles;
import thaumcraft.common.config.ThaumcraftLegacyTiles;

/**
 * The client registration of the eldritch, cosmetic, device and legacy tile
 * families.
 *
 * <p>Every renderer here is the original TC4 4.2.3.5 client presentation of its
 * tile. The 1.7.10 renderers that used the fixed function pipeline are
 * reproduced with the native equivalents and the differences are documented in
 * the renderer itself.
 */
public final class ThaumcraftEldritchBlocksClient {
    private static boolean initialized;
    private ThaumcraftEldritchBlocksClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        // The eldritch stone family: the altar, the obelisk, the capstone, the
        // lock, the crab spawner and the eldritch crystal.
        register(ThaumcraftEldritchBlocks.ELDRITCH_TILE, TileEldritchRenderer::new);
        // The blank eldritch filler and the eldritch gate.
        register(ThaumcraftEldritchBlocks.ELDRITCH_NOTHING_TILE, TileEldritchNothingRenderer::new);
        register(ThaumcraftEldritchBlocks.ELDRITCH_PORTAL_TILE, TileEldritchPortalRenderer::new);
        // The item mirror and the essentia mirror.
        register(ThaumcraftEldritchBlocks.MIRROR_TILE, TileMirrorRenderer::new);
        register(ThaumcraftEldritchBlocks.MIRROR_ESSENTIA_TILE, TileMirrorRenderer::new);
        // The cultist banner of the wooden device.
        register(ThaumcraftEldritchBlocks.BANNER_TILE, TileBannerRenderer::new);
        // The brain box of the metal device.
        register(ThaumcraftEldritchBlocks.BRAINBOX_TILE, TileBrainboxRenderer::new);
        // The wand pedestal and the flux scrubber of the stone device.
        register(ThaumcraftEldritchBlocks.WAND_PEDESTAL_TILE, TileWandPedestalRenderer::new);
        register(ThaumcraftEldritchBlocks.FLUX_SCRUBBER_TILE, TileFluxScrubberRenderer::new);
        // The ethereal bloom.
        register(ThaumcraftEldritchBlocks.ETHEREAL_BLOOM_TILE, TileEtherealBloomRenderer::new);
        // The rune faces of the warded glass of the cosmetic opaque family.
        register(ThaumcraftEldritchBlocks.OWNED_TILE, TileWardedGlassRenderer::new);
        // The wand pedestal of the legacy tiles.
        register(ThaumcraftLegacyTiles.WAND_PEDESTAL_BLOCK_ENTITY, TileWandPedestalRenderer::new);
        // The hole a portable hole leaves behind.
        register(ThaumcraftHoles.HOLE_BLOCK_ENTITY, TileHoleRenderer::new);
    }

    /**
     * Registers one block entity renderer. A tile type that its content class
     * never registered is skipped, so the client starts even while a content
     * wave is still being wired into the common initializer.
     */
    private static <T extends BlockEntity> void register(RegistrySupplier<BlockEntityType<T>> type,
                                                           BlockEntityRendererProvider<T> factory) {
        if (!type.isPresent()) return;
        BlockEntityRendererRegistry.register(type.get(), factory);
    }
}
