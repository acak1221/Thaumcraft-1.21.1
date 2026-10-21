package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.entities.monster.EntityCultistLeader;

/**
 * Original TC4 RenderCultist: the cultist sheet on the vanilla biped mesh, the leader at
 * 1.25 scale and the ritualist cleric's slow float plus its link line to the ritual target.
 *
 * <p>The original link line is a scrolling ribbon of the {@code wispy.png} sheet that is
 * drawn twice, once widened in y and once widened in x, so that the line always keeps a
 * constant width on the screen. The native tree draws the same two ribbons with the same
 * wave, the same scroll speed and the same colour.
 */
public final class RenderCultist<T extends Mob> extends MobRenderer<T, HumanoidModel<T>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/cultist.png");
    /** The original texture of the link line of the ritualist cleric. */
    public static final ResourceLocation WISPY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/wispy.png");
    /** Original link line colour 0x110011 with the original 0.8 alpha. */
    private static final int LINK_COLOR = 0x110011;
    /** The original alpha of the link line. */
    private static final float LINK_ALPHA = .8F;
    /** The original half width of the link line. */
    private static final float LINK_WIDTH = .25F;
    /** The original scroll speed of the link line. */
    private static final float LINK_SPEED = -.03F;
    /** The original default of the config option golem_link_quality. */
    public static final int GOLEM_LINK_QUALITY = 16;

    public RenderCultist(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), .5F);
    }

    @Override public ResourceLocation getTextureLocation(T cultist) { return SKIN; }

    /** Original func_77041_b:44-48. */
    @Override protected void scale(T cultist, PoseStack pose, float partial) {
        if (cultist instanceof EntityCultistLeader) pose.scale(1.25F, 1.25F, 1.25F);
    }

    @Override public void render(T cultist, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        try {
            float bob = 0.0F;
            boolean ritual = cultist instanceof EntityCultistCleric cleric && cleric.isRitualist();
            if (ritual) {
                // The original random was seeded from the entity id of the cleric.
                int seed = new java.util.Random(cultist.getId()).nextInt(1000);
                float phase = cultist.tickCount + partial + seed;
                bob = Mth.sin(phase / 9.0F) * .1F + .21F;
                pose.translate(0.0D, bob, 0.0D);
            }
            super.render(cultist, yaw, partial, pose, buffers, light);
            if (ritual) drawLink((EntityCultistCleric) cultist, bob, light, pose, buffers);
        } finally { pose.popPose(); }
    }

    /**
     * Original RenderCultist:83-163 - the two ribbons of the link line of the ritualist
     * cleric. The line starts at the eyes of the cleric and ends one and a half blocks
     * above the ritual target; it waves with the world lattice and it scrolls with the
     * original speed.
     */
    private static void drawLink(EntityCultistCleric cleric, float bob, int light, PoseStack pose, MultiBufferSource buffers) {
        BlockPos home = cleric.getRitualTarget();
        if (home == null) return;
        // The original start point: the position of the cleric and one and a fifth of its
        // eye height above it. The original end point: the middle of the ritual target and
        // one and a half blocks above it, less the bob that the matrix already adds.
        double startX = cleric.getX();
        double startY = cleric.getY() + cleric.getEyeHeight() * 1.2F;
        double startZ = cleric.getZ();
        double endX = home.getX() + .5D;
        double endY = home.getY() + 1.5D - bob;
        double endZ = home.getZ() + .5D;
        // The ribbon is drawn in the space of the entity, so the end point is relative to it.
        double localEndX = endX - startX;
        double localEndY = endY - startY;
        double localEndZ = endZ - startZ;
        double deltaX = startX - endX;
        double deltaY = startY - endY;
        double deltaZ = startZ - endZ;
        double distanceWorld = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        float blocks = Math.round((float) distanceWorld);
        float length = blocks * (GOLEM_LINK_QUALITY / 2.0F);
        if (length <= 0.0F) return;
        // The original distance: the share of the ten ticks the ritual needs.
        float distance = Math.min(cleric.tickCount, 10) / 10.0F;
        double time = System.nanoTime() / 30000000L;
        int red = LINK_COLOR >> 16 & 255;
        int green = LINK_COLOR >> 8 & 255;
        int blue = LINK_COLOR & 255;
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(WISPY));
        int segments = (int) (length * distance);
        // The original first ribbon is widened in y, the second one in x.
        for (int pass = 0; pass < 2; pass++) {
            double previousLowX = 0.0D, previousLowY = 0.0D, previousLowZ = 0.0D;
            double previousHighX = 0.0D, previousHighY = 0.0D, previousHighZ = 0.0D;
            float previousU = 0.0F;
            boolean first = true;
            for (int index = 0; index <= segments; index++) {
                float beam = index / length;
                float centre = 1.0F - Math.abs(index - length / 2.0F) / (length / 2.0F);
                double waveX = Mth.sin((float) ((startZ % 16.0D
                        + distanceWorld * (1.0F - beam) * GOLEM_LINK_QUALITY / 2.0F
                        - (time % 32767.0D / 5.0D)) / 4.0D)) * .5D * centre;
                double waveY = Mth.sin((float) ((startX % 16.0D
                        + distanceWorld * (1.0F - beam) * GOLEM_LINK_QUALITY / 2.0F
                        - (time % 32767.0D / 5.0D)) / 3.0D)) * .5D * centre;
                double waveZ = Mth.sin((float) ((startY % 16.0D
                        + distanceWorld * (1.0F - beam) * GOLEM_LINK_QUALITY / 2.0F
                        - (time % 32767.0D / 5.0D)) / 2.0D)) * .5D * centre;
                double x = localEndX + (deltaX + waveX) * beam;
                double y = localEndY + (deltaY + waveY) * beam;
                double z = localEndZ + (deltaZ + waveZ) * beam;
                float u = (1.0F - beam) * (float) distanceWorld - (float) (time * LINK_SPEED);
                // The normal of the ribbon is perpendicular to the line and to the width.
                double lineX = deltaX + waveX;
                double lineY = deltaY + waveY;
                double lineZ = deltaZ + waveZ;
                double normalX, normalY, normalZ;
                if (pass == 0) {
                    normalX = lineY; normalY = -lineX; normalZ = 0.0D;
                } else {
                    normalX = lineZ; normalY = 0.0D; normalZ = -lineX;
                }
                double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
                if (normalLength < 1.0E-6D) { normalX = 0.0D; normalY = 1.0D; normalZ = 0.0D; normalLength = 1.0D; }
                normalX /= normalLength; normalY /= normalLength; normalZ /= normalLength;
                double lowX, lowY, lowZ, highX, highY, highZ;
                if (pass == 0) {
                    lowX = x; lowY = y - LINK_WIDTH; lowZ = z;
                    highX = x; highY = y + LINK_WIDTH; highZ = z;
                } else {
                    lowX = x - LINK_WIDTH; lowY = y; lowZ = z;
                    highX = x + LINK_WIDTH; highY = y; highZ = z;
                }
                if (!first) {
                    // The original quad of the ribbon: the low and the high edge of two segments.
                    vertex(out, pose, previousLowX, previousLowY, previousLowZ, previousU, 1.0F, normalX, normalY, normalZ, red, green, blue, light);
                    vertex(out, pose, previousHighX, previousHighY, previousHighZ, previousU, 0.0F, normalX, normalY, normalZ, red, green, blue, light);
                    vertex(out, pose, lowX, lowY, lowZ, u, 1.0F, normalX, normalY, normalZ, red, green, blue, light);
                    vertex(out, pose, previousHighX, previousHighY, previousHighZ, previousU, 0.0F, normalX, normalY, normalZ, red, green, blue, light);
                    vertex(out, pose, highX, highY, highZ, u, 0.0F, normalX, normalY, normalZ, red, green, blue, light);
                    vertex(out, pose, lowX, lowY, lowZ, u, 1.0F, normalX, normalY, normalZ, red, green, blue, light);
                }
                previousLowX = lowX; previousLowY = lowY; previousLowZ = lowZ;
                previousHighX = highX; previousHighY = highY; previousHighZ = highZ;
                previousU = u;
                first = false;
            }
        }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, double x, double y, double z,
                               float u, float v, double normalX, double normalY, double normalZ,
                               int red, int green, int blue, int light) {
        out.addVertex(pose.last(), x, y, z)
                .setColor(red, green, blue, (int) (LINK_ALPHA * 255.0F))
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose.last(), normalX, normalY, normalZ);
    }
}
