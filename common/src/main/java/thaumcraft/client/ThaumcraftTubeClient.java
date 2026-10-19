package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import thaumcraft.client.fx.particles.FXVent;
import thaumcraft.client.renderers.tile.TileTubeRenderer;
import thaumcraft.client.renderers.tile.TileTubeBufferRenderer;
import thaumcraft.client.renderers.tile.TileCentrifugeRenderer;
import thaumcraft.client.renderers.tile.TileEssentiaCrystalizerRenderer;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;

/** Client-only registration; the native tube/server never loads renderer classes. */
public final class ThaumcraftTubeClient {
    private static boolean initialized;
    private static final SoundEvent CREAK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "creak"));
    private ThaumcraftTubeClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), TileTubeRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftTubes.BUFFER_BLOCK_ENTITY.get(), TileTubeBufferRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftTubes.CENTRIFUGE_BLOCK_ENTITY.get(), TileCentrifugeRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftTubes.CRYSTALLIZER_BLOCK_ENTITY.get(), TileEssentiaCrystalizerRenderer::new);
        TileEssentiaCrystalizer.setClientEffects(CrystallizerClientEffects::vent);
        TileTube.setClientEffects(new TileTube.ClientEffects() {
            @Override public void tick(Level level, BlockPos pos, int venting, int color) {
                if (venting > 0) FXVent.emit(level, pos, color);
            }

            @Override public void event(Level level, BlockPos pos, int event, int color, boolean startingVenting) {
                // Original TileTube:294-309. Block events already arbitrate the
                // server40/client50 timers; audio never changes transport state.
                if (event == 0) level.playLocalSound(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5,
                        CREAK, SoundSource.BLOCKS, 1, 1.3F + level.random.nextFloat() * .2F, false);
                else if (event == 1 && startingVenting) level.playLocalSound(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5,
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .1F, 1 + level.random.nextFloat() * .1F, false);
            }
        });
    }
}
