package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blocks.ItemJarNode;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * The item form of the node jar.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/ItemJarNodeRenderer.java:36-87}: the item of the
 * original draws the node of the jar in three crossed planes at {@code (0.5, 0.4, 0.5)} and then the
 * glass shell of the jar with the alpha blend of the original. The node uses the aspects, the node type
 * and the node modifier of the item itself.</p>
 *
 * <p>The three rotations of the original are {@code glRotatef(90, 0, 1, 0)} and
 * {@code glRotatef(90, 1, 0, 0)}, so they are around the Z axis and around the X axis.</p>
 */
public final class ItemJarNodeRenderer {
    /** The place of the node inside the item. */
    public static final float NODE_X = .5F, NODE_Y = .4F, NODE_Z = .5F;
    /** The size of the node of the original item form. */
    public static final float NODE_SIZE = 1.0F;
    private ItemJarNodeRenderer() {}

    /** The original item renderer of the node jar. */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                              int light, int overlay) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemJarNode item)) return;
        AspectList aspects = item.getAspects(stack);
        NodeType type = item.getNodeType(stack);
        NodeModifier modifier = item.getNodeModifier(stack);
        if (aspects != null && aspects.size() > 0 && type != null) {
            // Oracle ItemJarNodeRenderer:68-73 - the node of the jar in three crossed planes.
            pose.pushPose();
            pose.translate(NODE_X, NODE_Y, NODE_Z);
            float age = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
            TileNodeRenderer.drawItemNode(pose, buffers, aspects, type, modifier, .5F, NODE_SIZE, age);
            pose.mulPose(Axis.ZP.rotationDegrees(90));
            TileNodeRenderer.drawItemNode(pose, buffers, aspects, type, modifier, .5F, NODE_SIZE, age);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            TileNodeRenderer.drawItemNode(pose, buffers, aspects, type, modifier, .5F, NODE_SIZE, age);
            pose.popPose();
        }
        // Oracle ItemJarNodeRenderer:75-83 - the glass shell of the jar with the alpha blend.
        var state = ThaumcraftEldritchBlocks.JAR_NODE.get().defaultBlockState();
        var blocks = Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),
                state, blocks.getBlockModel(state), 1, 1, 1, light, overlay);
    }
}
