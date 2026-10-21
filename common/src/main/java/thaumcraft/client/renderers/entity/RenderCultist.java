package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
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
import thaumcraft.client.fx.FloatyLine;
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
     * The original link line of the ritualist cleric.
     *
     * <p>Original RenderCultist.java:83-163 is its own copy of the floaty line helper: the two
     * crossed ribbons with the ordinary alpha blend, the depth write and the constant alpha
     * {@code 0.8f}. The native port draws the same two ribbons through the shared helper
     * {@code thaumcraft.client.fx.FloatyLine}.</p>
     */
    private static void drawLink(EntityCultistCleric cleric, float bob, int light, PoseStack pose, MultiBufferSource buffers) {
        BlockPos home = cleric.getRitualTarget();
        if (home == null) return;
        // The original start point: the position of the cleric and one and a fifth of its eye
        // height above it. The original end point: the middle of the ritual target and one and a
        // half blocks above it, less the bob that the matrix already adds. The ribbon is drawn in
        // the space of the entity, so the end point is relative to it.
        double fromX = 0.0D;
        double fromY = cleric.getEyeHeight() * 1.2D;
        double fromZ = 0.0D;
        double toX = home.getX() + .5D - cleric.getX();
        double toY = home.getY() + 1.5D - bob - cleric.getY();
        double toZ = home.getZ() + .5D - cleric.getZ();
        // The original distance: the share of the ten ticks the ritual needs.
        float distance = Math.min(cleric.tickCount, 10) / 10.0F;
        FloatyLine.draw(pose, buffers, fromX, fromY, fromZ, toX, toY, toZ,
                cleric.getX() + fromX, cleric.getY() + fromY, cleric.getZ() + fromZ,
                LINK_COLOR, WISPY, LINK_SPEED, distance, LINK_WIDTH, FloatyLine.Flavour.CULTIST);
    }

}
