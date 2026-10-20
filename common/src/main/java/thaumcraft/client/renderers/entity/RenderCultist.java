package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.entities.monster.EntityCultistLeader;

/**
 * Original TC4 RenderCultist: the cultist sheet on the vanilla biped mesh, the leader at
 * 1.25 scale and the ritualist cleric's slow float plus its link line to the ritual target.
 * The original link line was a scrolling wispy.png ribbon drawn by the 1.7 FX engine; the
 * native port draws the same line as a native vertex coloured polyline instead.
 */
public final class RenderCultist<T extends net.minecraft.world.entity.Mob> extends MobRenderer<T, HumanoidModel<T>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/cultist.png");
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
     * Native approximation of the original drawFloatyLine: the same endpoints and the
     * same tint, drawn as a native polyline instead of a scrolling textured ribbon.
     */
    private static void drawLink(net.minecraft.world.entity.Mob cleric, EntityCultistCleric source, float bob, float partial,
                                  PoseStack pose, MultiBufferSource buffers) {
        var target = source.getRitualTarget();
        if (target == null) return;
        Vec3 start = new Vec3(0, cleric.getBbHeight() * 1.2F, 0);
        Vec3 end = new Vec3(target.getX() - cleric.getX() + .5D,
                target.getY() - cleric.getY() + 1.5D - bob,
                target.getZ() - cleric.getZ() + .5D);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-6) return;
        Vec3 direction = delta.scale(1.0D / length);
        Vec3 side = direction.cross(new Vec3(0, 1, 0)).normalize();
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        int segments = Math.max(2, (int) Math.min(48, length * 4));
        VertexConsumer out = buffers.getBuffer(RenderType.lineStrip());
        double time = System.nanoTime() / 30000000L;
        for (int i = 0; i <= segments; i++) {
            Vec3 point = point(start, delta, side, time, i, segments);
            out.addVertex(pose.last(), (float) point.x, (float) point.y, (float) point.z)
                    .setColor(LINK_COLOR >> 16 & 255, LINK_COLOR >> 8 & 255, LINK_COLOR & 255, 204)
                    .setNormal(pose.last(), 0, 1, 0);
        }
    }

    private static Vec3 point(Vec3 start, Vec3 delta, Vec3 side, double time, int index, int segments) {
        float f = index / (float) segments;
        float wobble = (float) Math.sin(time * .05 + index * .9F) * .25F * (1.0F - Math.abs(f - .5F) * 2.0F);
        return start.add(delta.scale(f)).add(side.scale(wobble));
    }
}
