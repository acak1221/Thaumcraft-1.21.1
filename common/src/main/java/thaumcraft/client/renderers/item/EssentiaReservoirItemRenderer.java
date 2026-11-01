package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.tile.TileEssentiaReservoirRenderer;
import thaumcraft.common.config.ThaumcraftTubes;

/** Original BlockEssentiaReservoirRenderer: the glass vessel followed by the complete reservoir OBJ. */
public final class EssentiaReservoirItemRenderer {
    private EssentiaReservoirItemRenderer() {}
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftTubes.ESSENTIA_RESERVOIR_ITEM.get())) return;
        var blocks = Minecraft.getInstance().getBlockRenderer();
        var state = ThaumcraftTubes.ESSENTIA_RESERVOIR.get().defaultBlockState();
        blocks.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),
                state, blocks.getBlockModel(state), 1, 1, 1, light, overlay);
        TileEssentiaReservoirRenderer.renderInventoryFrame(pose, buffers, light, overlay);
    }
}
