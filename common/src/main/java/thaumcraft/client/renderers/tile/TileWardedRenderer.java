package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.items.wands.foci.ItemFocusWarding;
import thaumcraft.common.lib.world.WardingOverlay;
import thaumcraft.common.tiles.TileWarded;

/** The actual remembered world block, with TC4's connected owner overlay visible while the camera holds a Warding wand. */
public final class TileWardedRenderer implements BlockEntityRenderer<TileWarded> {
    private static final ResourceLocation[] RUNES = new ResourceLocation[47];
    static {
        for (int index = 0; index < RUNES.length; index++) RUNES[index] = ResourceLocation.fromNamespaceAndPath(
                "thaumcraft", "textures/blocks/warded_glass_" + (index + 1) + ".png");
    }
    private final RandomSource random = RandomSource.create();
    public TileWardedRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TileWarded tile, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        if (!tile.validWard() || tile.getLevel() == null) return;
        var minecraft = Minecraft.getInstance();
        var state = tile.originalState();
        // A world render retains real biome tint, position-specific model randomness and neighbouring light/ambient occlusion.
        minecraft.getBlockRenderer().renderBatched(state, tile.getBlockPos(), tile.getLevel(), poses,
                buffers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(state)), true, random);
        if (!(minecraft.getCameraEntity() instanceof Player viewer)
                || !(FocusData.get(viewer.getMainHandItem()).getItem() instanceof ItemFocusWarding)) return;
        if (tile.getBlockPos().distToCenterSqr(minecraft.gameRenderer.getMainCamera().getPosition()) > 64D * 64D) return;
        // Original TileWardedRenderer: the runes are drawn with GL_TEXTURE_ENV_MODE GL_ADD, the
        // colour added to the rune texture, as the Architect area.
        var add = thaumcraft.client.ThaumcraftArchitectClient.addTextureShader();
        if (add == null || !VisNetRender.flush(buffers)) return;
        boolean owned = tile.ownedBy(viewer.getUUID());
        float ticks = viewer.tickCount + partialTick;
        var pos = tile.getBlockPos();
        float red = Mth.sin(ticks / 2F + pos.getX()) * .2F + .8F;
        float green = Mth.sin(ticks / 3F + pos.getY()) * .2F + (owned ? .7F : .28F);
        float blue = Mth.sin(ticks / 4F + pos.getZ()) * .2F + .28F;
        for (Direction face : Direction.values()) {
            if (!WardingOverlay.visible(tile.getLevel(), pos, face)) continue;
            int texture = WardingOverlay.texture(WardingOverlay.mask(tile.getLevel(), pos, face, tile.owner()));
            VisNetRender.draw(RUNES[texture - 1], GL11.GL_ONE, false, add,
                    out -> face(out, poses.last(), face, red, green, blue, owned ? .5F : .25F));
        }
    }
    @Override public int getViewDistance() {
        // The original saved block was chunk geometry, visible throughout the loaded view; only its rune TESR stopped at64.
        // This renderer is still called only for already-visible chunk block entities, so no extra chunk is requested.
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 32 + 32;
    }
    private static void face(VertexConsumer out, PoseStack.Pose pose, Direction face, float red, float green, float blue, float alpha) {
        float lo = -.001F, hi = 1.001F;
        // Top-left/right and bottom-right/left agree with the original side-specific neighbour bit matrix.
        float[][] corners = switch (face) {
            case DOWN -> new float[][]{{lo,lo,lo},{hi,lo,lo},{hi,lo,hi},{lo,lo,hi}};
            case UP -> new float[][]{{lo,hi,lo},{hi,hi,lo},{hi,hi,hi},{lo,hi,hi}};
            case NORTH -> new float[][]{{hi,hi,lo},{lo,hi,lo},{lo,lo,lo},{hi,lo,lo}};
            case SOUTH -> new float[][]{{lo,hi,hi},{hi,hi,hi},{hi,lo,hi},{lo,lo,hi}};
            case WEST -> new float[][]{{lo,hi,lo},{lo,hi,hi},{lo,lo,hi},{lo,lo,lo}};
            case EAST -> new float[][]{{hi,hi,hi},{hi,hi,lo},{hi,lo,lo},{hi,lo,hi}};
        };
        float[][] uv = {{0,0},{1,0},{1,1},{0,1}};
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose, corners[index][0], corners[index][1], corners[index][2]).setColor(red, green, blue, alpha)
                    .setUv(uv[index][0], uv[index][1]);
        }
    }
}
