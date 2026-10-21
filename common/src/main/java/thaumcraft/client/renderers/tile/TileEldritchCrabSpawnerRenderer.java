package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import thaumcraft.client.fx.particles.FXVent;
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
 *
 * <p>Oracle {@code TileEldritchCrabSpawner:47-55,59-67} - the client vents the
 * spawner for twenty ticks after the block event of the server and then lets a
 * single particle escape once in twenty ticks.
 */
public final class TileEldritchCrabSpawnerRenderer implements BlockEntityRenderer<TileEldritchCrabSpawner> {
    /** The original texture of the crab vent. */
    public static final ResourceLocation VENT = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/crabvent.png");
    /** Oracle TileEldritchCrabSpawner:53 - three particles per tick of the vent. */
    public static final int VENT_PARTICLES = 3;
    /** Oracle TileEldritchCrabSpawner:53 - one idle particle in twenty ticks. */
    public static final int IDLE_VENT_CHANCE = 20;

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
        vent(tile);
    }

    /**
     * Oracle {@code TileEldritchCrabSpawner:47-55} - the client side of the vent.
     *
     * <p>The vent of the original runs in the tick of the tile entity, but the
     * 1.21.1 eldritch block only ticks its tile on the server, so the renderer
     * runs the vent once per client tick instead.
     */
    private static void vent(TileEldritchCrabSpawner tile) {
        Level level = tile.getLevel();
        if (level == null || !level.isClientSide) return;
        if (!tile.clientTick(level)) return;
        if (tile.getVenting() > 0) {
            tile.clientVentTick();
            for (int a = 0; a < VENT_PARTICLES; ++a) drawVent(tile, level);
        } else if (level.getRandom().nextInt(IDLE_VENT_CHANCE) == 0) {
            drawVent(tile, level);
        }
    }

    /** Oracle {@code TileEldritchCrabSpawner:59-67} - one particle of the vent. */
    private static void drawVent(TileEldritchCrabSpawner tile, Level level) {
        RandomSource random = level.getRandom();
        Direction dir = Direction.from3DDataValue(tile.getFacing());
        float fx = 0.15F - random.nextFloat() * 0.3F;
        float fz = 0.15F - random.nextFloat() * 0.3F;
        float fy = 0.15F - random.nextFloat() * 0.3F;
        float fx2 = 0.1F - random.nextFloat() * 0.2F;
        float fz2 = 0.1F - random.nextFloat() * 0.2F;
        float fy2 = 0.1F - random.nextFloat() * 0.2F;
        var pos = tile.getBlockPos();
        FXVent.emit(level,
                pos.getX() + .5F + fx + dir.getStepX() / 2.1F,
                pos.getY() + .5F + fy + dir.getStepY() / 2.1F,
                pos.getZ() + .5F + fz + dir.getStepZ() / 2.1F,
                dir.getStepX() / 3.0F + fx2,
                dir.getStepY() / 3.0F + fy2,
                dir.getStepZ() / 3.0F + fz2,
                TileEldritchCrabSpawner.VENT_COLOUR);
    }
}
