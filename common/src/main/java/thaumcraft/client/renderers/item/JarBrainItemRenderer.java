package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.tile.TileJarBrainRenderer;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/** Original BlockJarRenderer:39-65: default brain/brine, followed by the translucent glass shell. */
public final class JarBrainItemRenderer {
    private JarBrainItemRenderer() {}

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftEldritchBlocks.JAR_BRAIN_ITEM.get())) return;
        var player = Minecraft.getInstance().player;
        pose.pushPose();
        try {
            contentPose(pose);
            TileJarBrainRenderer.renderContents(player == null ? 0 : player.tickCount, 0, pose, buffers, overlay);
        } finally {
            pose.popPose();
        }
        TileJarBrainRenderer.renderShell(ThaumcraftEldritchBlocks.JAR_BRAIN.get().defaultBlockState(),
                pose, buffers, light, overlay);
    }

    /** The old inventory renderer turns only the content, about the centred block. */
    static void contentPose(PoseStack pose) {
        pose.translate(.5F, .5F, .5F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(-.5F, -.5F, -.5F);
    }
}
