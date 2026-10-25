package thaumcraft.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;
import thaumcraft.client.renderers.models.gear.ModelGearArmor;
import thaumcraft.client.renderers.models.gear.ModelKnightArmor;
import thaumcraft.client.renderers.models.gear.ModelLeaderArmor;
import thaumcraft.client.renderers.models.gear.ModelRobe;

class GearZombiePoseParityTest {
    private static final float EPS = 1E-5F;

    @Test void allFourGearModelsRestoreOriginalZombieArmsAfterCopyingTheNativeWearer() {
        // Literal TC4 setRotationAnglesZombie fixtures: idle and halfway through an attack.
        float[][] fixtures = {{0, 0, -1.5707964F, -1.5707964F, -.1F, .1F, .1F, -.1F},
                {.5F, 20, -2.4392795F, -2.5366278F, .5F, -.5F, .0386399F, -.0386399F}};
        for (var model : models()) for (float[] fixture : fixtures) {
            var nativePose = nativePose(fixture[0], fixture[1]);
            float nativePitch = nativePose.rightArm.xRot;
            model.attackTime = fixture[0];
            model.copyPose(nativePose, true, fixture[1]);
            assertEquals(fixture[2], model.rightArm.xRot, EPS);
            assertEquals(fixture[3], model.leftArm.xRot, EPS);
            assertEquals(fixture[4], model.rightArm.yRot, EPS);
            assertEquals(fixture[5], model.leftArm.yRot, EPS);
            assertEquals(fixture[6], model.rightArm.zRot, EPS);
            assertEquals(fixture[7], model.leftArm.zRot, EPS);
            assertNotEquals(nativePitch, model.rightArm.xRot, .01F, "modern native zombie angles differ from original gear");
            assertEquals(.35F, model.head.yRot, EPS, "head continues to follow wearer");
            assertEquals(.6F, model.rightLeg.xRot, EPS, "legs continue to follow wearer");
            assertEquals(nativePitch, nativePose.rightArm.xRot, EPS, "armor cannot edit the wearer's body model");
        }
    }

    @Test void ordinaryWearersKeepTheSuppliedPoseAndTheOldCopyApiRemainsCompatible() {
        for (var model : models()) {
            var nativePose = nativePose(.5F, 20);
            model.attackTime = .5F;
            model.copyPose(nativePose, false, 20);
            assertEquals(nativePose.rightArm.xRot, model.rightArm.xRot, EPS);
            assertEquals(nativePose.leftArm.yRot, model.leftArm.yRot, EPS);
            model.copyPose(nativePose);
            assertEquals(nativePose.rightArm.zRot, model.rightArm.zRot, EPS);
        }
    }

    private static HumanoidModel<LivingEntity> nativePose(float attack, float age) {
        var context = new HumanoidModel<LivingEntity>(ModelKnightArmor.createBodyLayer(false).bakeRoot());
        AnimationUtils.animateZombieArms(context.leftArm, context.rightArm, true, attack, age);
        context.head.yRot = .35F;
        context.rightLeg.xRot = .6F;
        return context;
    }

    private static List<ModelGearArmor> models() {
        return List.of(new ModelRobe(ModelRobe.createBodyLayer(true).bakeRoot()),
                new ModelFortressArmor(ModelFortressArmor.createBodyLayer(true).bakeRoot()),
                new ModelKnightArmor(ModelKnightArmor.createBodyLayer(true).bakeRoot()),
                new ModelLeaderArmor(ModelLeaderArmor.createBodyLayer(true).bakeRoot()));
    }
}
