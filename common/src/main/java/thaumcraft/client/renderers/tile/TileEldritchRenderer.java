package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.tiles.TileEldritchAltar;
import thaumcraft.common.tiles.TileEldritchCap;
import thaumcraft.common.tiles.TileEldritchCrabSpawner;
import thaumcraft.common.tiles.TileEldritchCrystal;
import thaumcraft.common.tiles.TileEldritchLock;
import thaumcraft.common.tiles.TileEldritchObelisk;

/**
 * The block entity renderer of the eldritch stone family.
 *
 * <p>In 1.7.10 every eldritch tile had its own special renderer; in 1.21.1 the
 * whole family shares one block entity type, so this renderer dispatches to the
 * original renderers of the original tiles.
 */
public final class TileEldritchRenderer implements BlockEntityRenderer<BlockEntity> {
    private final TileEldritchCapRenderer cap;
    private final TileEldritchObeliskRenderer obelisk;
    private final TileEldritchCrystalRenderer crystal;
    private final TileEldritchCrabSpawnerRenderer spawner;
    private final TileEldritchLockRenderer lock;

    public TileEldritchRenderer(BlockEntityRendererProvider.Context context) {
        cap = new TileEldritchCapRenderer();
        obelisk = new TileEldritchObeliskRenderer(context);
        crystal = new TileEldritchCrystalRenderer(context);
        spawner = new TileEldritchCrabSpawnerRenderer(context);
        lock = new TileEldritchLockRenderer(context);
    }

    @Override public void render(BlockEntity tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile instanceof TileEldritchAltar altar) {
            renderCap(pose, buffers, light, overlay, TileEldritchCapRenderer.CAP_ALTAR, altar);
            return;
        }        if (tile instanceof TileEldritchCap capstone) {
            renderCap(pose, buffers, light, overlay, TileEldritchCapRenderer.CAP, capstone);
            return;
        }
        if (tile instanceof TileEldritchObelisk obeliskTile) {
            obelisk.render(obeliskTile, partialTick, pose, buffers, light, overlay);
            return;
        }
        if (tile instanceof TileEldritchCrystal crystalTile) {
            crystal.render(crystalTile, partialTick, pose, buffers, light, overlay);
            return;
        }
        if (tile instanceof TileEldritchCrabSpawner spawnerTile) {
            spawner.render(spawnerTile, partialTick, pose, buffers, light, overlay);
            return;
        }
        if (tile instanceof TileEldritchLock lockTile) {
            lock.render(lockTile, partialTick, pose, buffers, light, overlay);
        }
    }

    /** The original cap of the altar and of the capstone. */
    private void renderCap(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                           net.minecraft.resources.ResourceLocation texture, BlockEntity tile) {
        boolean outer = tile.getLevel() != null
                && tile.getLevel().dimension().location().getPath().equals("outer_lands");
        cap.render(pose, buffers, light, overlay, texture, outer);
        if (tile instanceof TileEldritchAltar altar) {
            cap.renderEyes(pose, buffers, light, overlay, altar.getEyes(), tile.getLevel());
        }
    }
}
