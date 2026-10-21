package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.fx.FloatyLine;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.entities.monster.EntityCultistLeader;

/**
 * Original TC4 RenderCultist: the cultist sheet on the vanilla biped mesh, the leader at
 * 1.25 scale and the ritualist cleric's slow float plus its link line to the ritual target.
 * The original link line is the scrolling wispy.png ribbon of the 1.7 floaty line helper
 * (oracle {@code thaumcraft/client/renderers/entity/RenderCultist.java:77}); the native port draws
 * the same pair of crossed scrolling ribbons.
 */
public final class RenderCultist<T extends net.minecraft.world.entity.Mob> extends MobRenderer<T, HumanoidModel<T>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/cultist.png");
    /** The original texture of the link line. */
    public static final ResourceLocation WISPY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/wispy.png");
    /** Original link line colour 0x110011 with the original 0.8 alpha. */
    private static final int LINK_COLOR = 0x110011;

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
                // The original random was seeded from the entity's own spawn seed.
                int seed = new java.util.Random(cultist.getUUID().hashCode()).nextInt(1000);
                float phase = cultist.tickCount + partial + seed;
                bob = Mth.sin(phase / 9.0F) * .1F + .21F;
                pose.translate(0.0D, bob, 0.0D);
            }
            super.render(cultist, yaw, partial, pose, buffers, light);
            if (ritual) drawLink(cultist, (EntityCultistCleric) cultist, bob, partial, pose, buffers);
        } finally { pose.popPose(); }
    }

    /**
     * The original link line of the cultist.
     *
     * <p>Original RenderCultist.java:77 calls the original floaty line helper with the colour
     * {@code 0x110011}, the texture {@code textures/misc/wispy.png}, the scroll speed {@code -0.03}, the
     * draw distance {@code min(ticksExisted, 10) / 10} and the half width {@code 0.25}. The native port
     * draws the same pair of crossed scrolling ribbons.</p>
     */
    private static void drawLink(net.minecraft.world.entity.Mob cleric, EntityCultistCleric source, float bob, float partial,
                                  PoseStack pose, MultiBufferSource buffers) {
        var target = source.getRitualTarget();
        if (target == null) return;
        // The entity renderer draws in the space of its own entity, the original line starts at the
        // middle of the cleric, one and a half times its height above the floor.
        double fromX = 0.0D;
        double fromY = cleric.getBbHeight() * 1.2D;
        double fromZ = 0.0D;
        double toX = target.getX() - cleric.getX() + .5D;
        double toY = target.getY() - cleric.getY() + 1.5D - bob;
        double toZ = target.getZ() - cleric.getZ() + .5D;
        float distance = Math.min(cleric.tickCount, 10) / 10F;
        FloatyLine.draw(pose, buffers, fromX, fromY, fromZ, toX, toY, toZ,
                cleric.getX() + fromX, cleric.getY() + fromY, cleric.getZ() + fromZ,
                LINK_COLOR, WISPY, -0.03F, distance, FloatyLine.CULTIST_WIDTH, FloatyLine.Flavour.CULTIST);
    }
}
