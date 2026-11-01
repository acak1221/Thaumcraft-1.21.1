package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.tile.TileAlembicRenderer;
import thaumcraft.common.config.ThaumcraftAlembics;

/** Original BlockMetalDeviceRenderer:45 uses the unconnected alembic OBJ, not an approximation. */
public final class AlembicItemRenderer {
    private AlembicItemRenderer() {}
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftAlembics.ALEMBIC_ITEM.get())) return;
        pose.pushPose();
        try {
            // The original inventory dispatcher subtracts X/Z halves but not Y;
            // the native builtin caller subtracts all three halves.
            inventoryPose(pose);
            TileAlembicRenderer.renderInventory(pose, buffers, light, overlay);
        } finally { pose.popPose(); }
    }
    static void inventoryPose(PoseStack pose) { pose.translate(0, .5, 0); }
}
