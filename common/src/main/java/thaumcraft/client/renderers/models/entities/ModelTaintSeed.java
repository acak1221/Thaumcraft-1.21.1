package thaumcraft.client.renderers.models.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityTaintSeed;

/**
 * The tainted seed, drawn with the chain of tentacle cubes the 1.12.2 backport used for it.
 *
 * <p>TC4 4.2.3.5 has no tainted seed; the entity and its model come from
 * {@code Thaumcraft-1.12.2-6.1.BETA26.jar}. The backport's {@code ModelTaintSeed} is a
 * {@code ModelBase} with one eight unit cube and seven eight or four unit segment cubes
 * that all sit on the origin and twist around the y axis, and its
 * {@code func_78088_a} lifts the whole chain by 1.2 plus the growth of the seed and scales
 * it with half the height of the entity.</p>
 *
 * <p>Declared native adaptation: the backport drew the chain with its own
 * {@code textures/entity/taintseed.png} sheet, which is not part of this port's asset tree,
 * so the native model keeps the original chain, the original lift, the original scale and
 * the original wobble and takes the port's own taint tentacle sheet for the missing one.
 * The original per segment pulse is the taint tentacle's own taper here, so the chain
 * keeps the same silhouette as the rest of the taint creatures of the port.</p>
 */
public final class ModelTaintSeed extends EntityModel<EntityTaintSeed> {
    /** The original model length: the root cube and seven segments. */
    private static final int LENGTH = 8;
    private final ModelPart root;
    private final ModelPart[] segments;
    private float height = 1.5F;
    private float growth;

    public ModelTaintSeed() {
        super(RenderType::entityTranslucent);
        root = layer().bakeRoot().getChild("tentacle");
        segments = new ModelPart[LENGTH - 1];
        ModelPart parent = root;
        for (int k = 0; k < segments.length; k++) {
            segments[k] = parent.getChild("segment_" + k);
            parent = segments[k];
        }
    }

    /** The original mesh: the root cube and the seven segment cubes of the seed. */
    public static LayerDefinition layer() {
        var mesh = new MeshDefinition();
        PartDefinition stem = mesh.getRoot().addOrReplaceChild("tentacle", cube(0, 0, 4), PartPose.ZERO);
        for (int k = 0; k < LENGTH - 1; k++) {
            stem = stem.addOrReplaceChild("segment_" + k,
                    cube(0, k < 4 ? 16 : 56, k < 4 ? 4 : 2), PartPose.offset(0, -8, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder cube(int u, int v, int radius) {
        return CubeListBuilder.create().texOffs(u, v).addBox(-radius, -radius, -radius, radius * 2, radius * 2, radius * 2);
    }

    @Override public void setupAnim(EntityTaintSeed entity, float limb, float amount, float age, float yaw, float pitch) {
        height = entity.getBbHeight();
        // Original ModelTaintSeed.func_78087_a: the health gate and the scale argument of
        // the original RenderLiving, which is one, so the modulo term is 0.2.
        float ht = entity.getMaxHealth() / 200.0F;
        float mod = 0.2F;
        float fi = (0.1F + (ht > 0.0F ? mod : -mod)) * 3.0F;
        for (int k = 0; k < segments.length; k++) {
            float wave = 0.1F / fi * Mth.sin(age * 0.06F - k / 2.0F) / 5.0F;
            segments[k].xRot = (float) Math.toRadians(wave + ht + entity.attackAnim);
            segments[k].zRot = (float) Math.toRadians(0.1F / fi * Mth.sin(age * 0.05F - k / 2.0F) / 5.0F);
            segments[k].yRot = (float) Math.toRadians(k <= 4 ? -8.0F : -4.0F);
        }
        // Original ModelTaintSeed.func_78088_a: the seed grows back to its full size while
        // it is younger than ten times its own height.
        float limit = height * 10.0F;
        growth = entity.tickCount < limit ? (limit - entity.tickCount) / limit * height : 0.0F;
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer out, int light, int overlay, int color) {
        pose.pushPose();
        try {
            // Original ModelTaintSeed.func_78088_a: the lift of 1.2 plus the growth and the
            // scale of half the height of the entity.
            pose.translate(0.0D, 1.2D + growth, 0.0D);
            float scale = height / 2.0F;
            pose.scale(scale, scale, scale);
            root.render(pose, out, light, overlay, color);
        } finally { pose.popPose(); }
    }
}
