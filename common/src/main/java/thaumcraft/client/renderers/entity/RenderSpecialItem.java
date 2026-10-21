package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Original TC4 RenderSpecialItem: the rotating three ray additive star behind the item.
 * The original star was drawn with fixed function GL and a fixed random seed; the native
 * port keeps the same geometry, colours and seed with native vertex buffers. The item
 * itself keeps the vanilla item entity rendering the original delegated to.
 *
 * <p>The native renderer accepts any {@link ItemEntity} and not only the special item
 * class: Minecraft 1.21.1 renders the item pickup animation with a copy of the picked
 * entity, and {@code ItemEntity.copy()} keeps the entity <em>type</em> but not the entity
 * <em>class</em> (Minecraft 1.21.1 {@code ItemEntity(ItemEntity)} calls
 * {@code super(itemEntity.getType(), ...)}), so the copy of a special item is a plain item
 * entity that still carries the {@code thaumcraft:special_item} type. A renderer typed to
 * the special item class would throw a {@code ClassCastException} in the bridge method
 * and crash the client as soon as the player picks the item up.
 */
public final class RenderSpecialItem extends EntityRenderer<ItemEntity> {
    private static final RenderType RAYS = new Rays();
    private final ItemEntityRenderer itemRenderer;

    public RenderSpecialItem(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .15F;
        itemRenderer = new ItemEntityRenderer(context);
    }

    @Override public ResourceLocation getTextureLocation(ItemEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/potions.png");
    }

    @Override public void render(ItemEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        try {
            // Original doRenderItem: the star sits above the item and bobs slowly.
            float bob = Mth.sin((entity.tickCount + partial) / 10.0F + entity.bobOffs) * .1F + .1F;
            pose.translate(0.0D, bob + .15D, 0.0D);
            drawStar(entity, pose, buffers);
        } finally { pose.popPose(); }
        itemRenderer.render(entity, yaw, partial, pose, buffers, light);
    }

    /** The original rotating star: five seeded three ray fans. */
    private static void drawStar(ItemEntity entity, PoseStack pose, MultiBufferSource buffers) {
        var random = new Random(245L);
        float spin = entity.tickCount / 500.0F;
        VertexConsumer out = buffers.getBuffer(RAYS);
        for (int i = 0; i < 5; i++) {
            pose.pushPose();
            try {
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360F));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360F + spin * 360F));
                float divisor = 30F / (Math.min(entity.tickCount, 10) / 10F);
                float length = (random.nextFloat() * 20F + 5F) / divisor;
                float width = (random.nextFloat() * 2F + 1F) / divisor;
                float[][] ends = {{-.866F * width, length, -.5F * width}, {.866F * width, length, -.5F * width}, {0, length, width}};
                for (int side = 0; side < 3; side++) {
                    vertex(out, pose, 0, 0, 0, 255, 255, 255, 255);
                    vertex(out, pose, ends[side][0], ends[side][1], ends[side][2], 255, 0, 255, 0);
                    vertex(out, pose, ends[(side + 1) % 3][0], ends[(side + 1) % 3][1], ends[(side + 1) % 3][2], 255, 0, 255, 0);
                }
            } finally { pose.popPose(); }
        }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float z, int r, int g, int b, int a) {
        out.addVertex(pose.last(), x, y, z).setColor(r, g, b, a);
    }

    /** The original star was additive over the item. */
    private static final class Rays extends RenderType {
        private Rays() {
            super("thaumcraft_special_item_rays", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 4096,
                    false, false, Rays::begin, Rays::end);
        }

        private static void begin() {
            RENDERTYPE_LIGHTNING_SHADER.setupRenderState();
            NO_TEXTURE.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState();
            CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            NO_TEXTURE.clearRenderState();
            RENDERTYPE_LIGHTNING_SHADER.clearRenderState();
        }
    }
}
