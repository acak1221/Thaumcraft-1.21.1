package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/** TC4 models/entities/ModelTrunk: a chest with a hinged lid and a latch (64x64 texture). The lid angle comes from the renderer;
 * the latch follows the lid, as the original copied the lid rotation onto it before drawing. The upgrade badge on the latch is a
 * separate render layer because it needs its own texture. */
public final class ModelTrunk<T extends LivingEntity> extends EntityModel<T> {
    public final ModelPart lid, knob, below;

    public ModelTrunk(ModelPart root) {
        lid = root.getChild("lid");
        knob = root.getChild("knob");
        below = root.getChild("below");
    }

    /** Original constructor: lid, knob and body boxes with their pivot offsets. */
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0, -5, -14, 14, 5, 14), PartPose.offset(1, 7, 15));
        root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -2, -15, 2, 4, 1), PartPose.offset(8, 7, 15));
        root.addOrReplaceChild("below", CubeListBuilder.create().texOffs(0, 19).addBox(0, 0, 0, 14, 10, 14), PartPose.offset(1, 6, 1));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Original {@code chestLid.rotateAngleX}; the knob always repeats it. */
    public void setLidAngle(float angle) {
        lid.xRot = angle;
        knob.xRot = angle;
    }

    @Override public void setupAnim(T entity, float walkPhase, float walkSpeed, float age, float headYaw, float headPitch) {}

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        lid.render(pose, buffer, light, overlay, color);
        below.render(pose, buffer, light, overlay, color);
        knob.render(pose, buffer, light, overlay, color);
    }
}
