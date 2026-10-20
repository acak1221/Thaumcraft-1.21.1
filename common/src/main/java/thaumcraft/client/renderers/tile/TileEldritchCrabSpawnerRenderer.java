package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelCrabVent;
import thaumcraft.common.tiles.TileEldritchCrabSpawner;

/**
 * Original TileEldritchCrabSpawnerRenderer: the vent of the eldritch crab
 * spawner.
 *
 * <p>The original draws the whole {@code crabvent.obj} model with the
 * {@code crabvent.png} texture, turned to the side the crabs leave through.
 * The native tree already carries the converted wavefront model in
 * {@link ModelCrabVent}, so the geometry is reused as it is.
 */
public final class TileEldritchCrabSpawnerRenderer implements BlockEntityRenderer<TileEldritchCrabSpawner> {
    /** The original texture of the crab vent. */
    public static final ResourceLocation VENT = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/crabvent.png");

    private final ModelCrabVent model = new ModelCrabVent();

    public TileEldritchCrabSpawnerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEldritchCrabSpawner tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5F, .5F, .5F);
            TileEldritchCrystalRenderer.orientation(pose, tile.getFacing());
            model.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(VENT)), light, overlay, 0xffffffff);
        } finally {
            pose.popPose();
        }
    }
}
