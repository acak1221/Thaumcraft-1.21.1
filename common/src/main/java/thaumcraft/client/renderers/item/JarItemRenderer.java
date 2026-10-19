package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.tile.TileJarRenderer;
import thaumcraft.common.items.ItemJar;

/** Loader-neutral builtin item hook. Original ItemJarFilledRenderer:55-82, with no fake world/tile. */
public final class JarItemRenderer {
    private JarItemRenderer() {}

    /** The caller has already applied the item model's native display transform. */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!ItemJar.isJar(stack)) return;
        // Original item facing is always EAST(5), with a zero-position crooked
        // label seed. Never read or modify the world under the local player.
        TileJarRenderer.renderContents(ItemJar.getAspect(stack), ItemJar.getAmount(stack), ItemJar.CAPACITY, ItemJar.getFilter(stack),
                Direction.EAST, 0, pose, buffers, light, overlay);

        var state = ((ItemJar) stack.getItem()).getBlock().defaultBlockState();
        var blocks = Minecraft.getInstance().getBlockRenderer();
        // Fetch the retained ordinary/void BLOCK shell, not the builtin/entity
        // item model. Direct quad rendering cannot recurse into this item hook.
        // Its original PNG alpha needs blending, not a cutout reinterpretation.
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),
                state, blocks.getBlockModel(state), 1, 1, 1, light, overlay);
    }
}
