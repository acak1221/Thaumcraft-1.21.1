package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import thaumcraft.common.tiles.TileJarNode;

/**
 * The original node jar: the stored node glows inside the jar.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:53-59}: a node jar is drawn
 * by the node renderer itself, one tenth of a block below the centre of the block. The node of a jar
 * is always visible, it has the size {@code 0.7f} and the depth test is kept (oracle
 * {@code thaumcraft/client/renderers/tile/TileNodeRenderer.java:186-189}).</p>
 *
 * <p>The "one second growth" of the original node jar is <strong>dead code</strong>: oracle
 * {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:107-115} applies
 * {@code GL11.glScalef(size, size, size)} <em>after</em> every draw call of that renderer and pops the
 * matrix immediately, so the scale of one second never reaches a single vertex. The native tree
 * therefore does not grow the node either; the visible result of the original is the plain node.</p>
 */
public final class TileJarNodeRenderer implements BlockEntityRenderer<TileJarNode> {
    public TileJarNodeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileJarNode tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TileNodeRenderer.renderJarNode(tile, Minecraft.getInstance(), partialTick, pose, buffers);
    }
}
