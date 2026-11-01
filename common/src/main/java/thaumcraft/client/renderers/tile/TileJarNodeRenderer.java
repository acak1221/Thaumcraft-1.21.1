package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.tiles.TileJarNode;

/**
 * The original node jar: the stored node glows inside the jar.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:53-59}: a node jar is drawn
 * by the node renderer itself, one tenth of a block below the centre of the block. The node of a jar
 * is always visible, it has the size {@code 0.7f} and the depth test is kept (oracle
 * {@code thaumcraft/client/renderers/tile/TileNodeRenderer.java:186-189}).</p>
 *
 * <p>The animation scale in the original node jar is <strong>dead code</strong>: oracle
 * {@code thaumcraft/client/renderers/tile/TileJarRenderer.java:107-115} applies
 * {@code GL11.glScalef(size, size, size)} <em>after</em> every draw call of that renderer and pops the
 * matrix immediately. The requested capture transition deliberately makes that second visible:
 * the glass shell shrinks from three times its size and the node from one to 0.7.</p>
 */
public final class TileJarNodeRenderer implements BlockEntityRenderer<TileJarNode> {
    public TileJarNodeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileJarNode tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.distanceToSqr(Vec3.atCenterOf(tile.getBlockPos()))
                > TileNodeRenderer.JAR_VIEW_DISTANCE * TileNodeRenderer.JAR_VIEW_DISTANCE) return;
        var frame = JarNodeAnimation.sample(tile.animate, System::currentTimeMillis);
        if (!frame.active()) tile.animate = 0L;
        TileNodeRenderer.renderJarNode(tile, minecraft, partialTick, pose, buffers, frame.nodeScale());
        pose.pushPose();
        try {
            // Keep the base and horizontal centre fixed as the shell contracts.
            pose.translate(.5, 0, .5);
            pose.scale(frame.shellScale(), frame.shellScale(), frame.shellScale());
            pose.translate(-.5, 0, -.5);
            var blocks = minecraft.getBlockRenderer();
            var state = tile.getBlockState();
            // Resolve the current model each frame so a resource reload cannot retain an old mesh.
            blocks.getModelRenderer().renderModel(pose.last(),
                    buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),
                    state, blocks.getBlockModel(state), 1, 1, 1, light, overlay);
        } finally {
            pose.popPose();
        }
    }

    @Override public boolean shouldRenderOffScreen(TileJarNode tile) { return tile.animate > 0L; }

    @Override public int getViewDistance() { return (int) TileNodeRenderer.JAR_VIEW_DISTANCE; }
}
