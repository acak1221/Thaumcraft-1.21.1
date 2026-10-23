package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.models.ModelThaumometer;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/**
 * Original ItemThaumometerRenderer:72-312: the thaumometer of the player.
 *
 * <p>The original draws the body of the thaumometer from the original OBJ
 * model {@code textures/models/scanner.obj} on the sheet
 * {@code textures/models/scanner.png} and the glowing scan screen from
 * {@code textures/models/scanscreen.png} on top of it (original
 * ItemThaumometerRenderer:148-154). The brightness of the screen is the
 * original {@code 190 + sin(ticks - random(2)) * 10 + 10}.
 *
 * <p>The original transform chain is kept: the inventory view turns the
 * thaumometer by 60 degrees about X, 30 degrees and 248 degrees about Z and
 * halves it (original ItemThaumometerRenderer:137-146), the equipped view
 * moves it by {@code (1.6, 0.3, 2.0)} and turns it by 90 degrees about -X and
 * 30 degrees about -Z (original ItemThaumometerRenderer:138-141), and the first
 * person view uses the original chain of original
 * ItemThaumometerRenderer:98-135 with the doubled size of original
 * ItemThaumometerRenderer:135.
 *
 * <p>The first person view of the original also draws the arm of the player
 * twice, mirrored, so that the screen of the thaumometer shows the holder
 * (original ItemThaumometerRenderer:113-131). The native draws the same arm
 * from the model of the player, as the original
 * {@code RenderPlayer.renderFirstPersonArm:318-324} does.
 *
 * <p>The original arm lag uses the public native xBob/yBob interpolants; the scan
 * readout is drawn on the scanner face from synchronized research knowledge.
 */
public final class ItemThaumometerRenderer {
    /** Original ItemThaumometerRenderer:95 - the size factor of the first person view. */
    private static final float FIRST_PERSON_SCALE = 0.8F;
    /** Original ItemThaumometerRenderer:154 - the scale of the scan screen quad. */
    private static final float SCREEN_SCALE = 2.5F;
    private static final ModelThaumometer MODEL = new ModelThaumometer();

    private ItemThaumometerRenderer() {}

    /** The loader's builtin item hook. */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof ItemThaumometer)) return;
        pose.pushPose();
        try {
            if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                firstPerson(pose, buffers, light, overlay);
            } else {
                // Original ItemThaumometerRenderer:136-146.
                pose.scale(0.5F, 0.5F, 0.5F);
                if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                    pose.translate(1.6F, 0.3F, 2.0F);
                    pose.mulPose(Axis.XN.rotationDegrees(90.0F));
                    pose.mulPose(Axis.ZN.rotationDegrees(30.0F));
                } else if (context == ItemDisplayContext.GUI) {
                    // Original ItemThaumometerRenderer:141-145 - the inventory chain. Note the
                    // LAST rotation turns about the Y axis with a negative angle, not about Z:
                    // glRotatef(248, 0, -1, 0) is Axis.YN 248, and Z would lay the scanner on
                    // its back in the slot instead of showing its face.
                    pose.mulPose(Axis.XP.rotationDegrees(60.0F));
                    pose.mulPose(Axis.ZN.rotationDegrees(30.0F));
                    pose.mulPose(Axis.YN.rotationDegrees(248.0F));
                }
            }
            // Original ItemThaumometerRenderer:148-149.
            MODEL.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ModelThaumometer.TEXTURE)), light, overlay);
            // Original ItemThaumometerRenderer:150-154.
            pose.pushPose();
            try {
                pose.translate(0.0F, 0.11F, 0.0F);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                screen(pose, buffers, overlay);
                // Original ItemThaumometerRenderer:155-259 - the scan readout, drawn in the
                // world space of the screen of the scanner.
                if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                    readout(pose, buffers);
                }
            } finally {
                pose.popPose();
            }
        } finally {
            pose.popPose();
        }
    }

    /**
     * The original first person view of the thaumometer.
     *
     * <p>Original ItemThaumometerRenderer:98-135: the item is moved and turned
     * with the view of the player, the arm of the player is drawn twice and
     * mirrored and the thaumometer itself is drawn twice as large.
     */
    private static void firstPerson(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        // Original ItemThaumometerRenderer:99-100.
        pose.translate(1.0F, 0.75F, -1.0F);
        pose.mulPose(Axis.YN.rotationDegrees(135.0F));
        // Oracle :101-104. LocalPlayer's xBob/yBob are the native arm pitch/yaw lag.
        pose.mulPose(Axis.XP.rotationDegrees(ThaumometerArmPose.lagDegrees(
                player.getXRot(), player.xBobO, player.xBob, partial)));
        pose.mulPose(Axis.YP.rotationDegrees(ThaumometerArmPose.lagDegrees(
                player.getYRot(), player.yBobO, player.yBob, partial)));
        // Original ItemThaumometerRenderer:107, the equip progress of the item in the hand.
        // The 1.7.10 client read the private fields of its ItemRenderer; the native port
        // reproduces the original curve, see thaumcraft.client.lib.UtilsFX.
        float progress = thaumcraft.client.lib.UtilsFX.getEquippedProgress(partial);
        pose.translate(-0.7F * FIRST_PERSON_SCALE,
                -(-0.65F * FIRST_PERSON_SCALE) + (1.0F - progress) * 1.5F,
                0.9F * FIRST_PERSON_SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(0.0F, 0.0F, -0.9F * FIRST_PERSON_SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        // Original ItemThaumometerRenderer:113-131: the arm of the player, twice.
        pose.pushPose();
        try {
            pose.scale(5.0F, 5.0F, 5.0F);
            for (int side = 0; side < 2; side++) {
                int sign = side * 2 - 1;
                pose.pushPose();
                try {
                    pose.translate(-0.0F, -0.6F, 1.1F * sign);
                    pose.mulPose(Axis.XP.rotationDegrees(-45.0F * sign));
                    pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                    pose.mulPose(Axis.ZP.rotationDegrees(59.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(-65.0F * sign));
                    arm(pose, buffers, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
        // Original ItemThaumometerRenderer:132-135.
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.translate(0.4F, -0.4F, 0.0F);
        pose.scale(2.0F, 2.0F, 2.0F);
    }

    /**
     * The scan readout of the original.
     *
     * <p>Original {@code thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:155-259}:
     * the readout is drawn in the first person view of the local player, on the screen of the
     * scanner. It shows the name of the target, the aspect icons of a target that was already
     * scanned and the type of a node.</p>
     */
    private static void readout(PoseStack pose, MultiBufferSource buffers) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        // Original ItemThaumometerRenderer:98 - the readout belongs to the first person view.
        if (!mc.options.getCameraType().isFirstPerson()) return;
        // Original ItemThaumometerRenderer:161 - the target of the scan.
        var target = ItemThaumometer.readout(player, player.getMainHandItem());
        if (target == null) return;
        // Original ItemThaumometerRenderer:169,179,183 - the icons are only drawn for a target
        // that was already scanned.
        var knowledge = KnowledgeAccess.get(player);
        boolean scanned = thaumcraft.common.lib.research.ScanManager.hasBeenScanned(knowledge, target.signature(), target.kind());
        int ticks = player.tickCount;
        int jitter = player.level() == null ? 0 : player.level().getRandom().nextInt(2);
        ThaumometerReadout.render(pose, buffers, target, scanned, ThaumometerReadout.packedLight(ticks, jitter));
    }

    /** The arm of the player, as the original RenderPlayer.renderFirstPersonArm:318-324 draws it. */
    private static void arm(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> living)) return;
        if (!(living.getModel() instanceof HumanoidModel<?> humanoid)) return;
        humanoid.rightArm.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture())), light, overlay);
    }

    /**
     * The original scan screen of the thaumometer.
     *
     * <p>Original UtilsFX.renderQuadCenteredFromTexture:766-786: a centred quad
     * with the whole texture, the given brightness and the given blend. The
     * original lightmap coordinate is kept: the native lightmap packs the block
     * light in the low and the sky light in the high nibble, which is the same
     * split the 1.7.10 lightmap coordinate uses.
     */
    private static void screen(PoseStack pose, MultiBufferSource buffers, int overlay) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        int ticks = player == null ? 0 : player.tickCount;
        int jitter = player == null || player.level() == null ? 0 : player.level().getRandom().nextInt(2);
        int brightness = ThaumometerReadout.brightness(ticks, jitter);
        int light = ThaumometerReadout.lightmap(brightness);
        // Original ItemThaumometerRenderer:154 renders the screen through
        // UtilsFX.renderQuadCenteredFromTexture(..., 2.5f, ...), which does a glScalef(2.5)
        // before the unit quad (oracle UtilsFX:761). Without that scale the screen is a
        // fifth of the face of the scanner and the body reads as a hollow golden ring.
        pose.pushPose();
        try {
            pose.scale(SCREEN_SCALE, SCREEN_SCALE, SCREEN_SCALE);
            var out = buffers.getBuffer(RenderType.entityTranslucent(ModelThaumometer.SCREEN));
            float[] points = {-0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F};
            float[] uv = {0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F};
            for (int index = 0; index < 4; index++) {
                out.addVertex(pose.last(), points[index * 3], points[index * 3 + 1], points[index * 3 + 2])
                        .setColor(255, 255, 255, 255)
                        .setUv(uv[index * 2], uv[index * 2 + 1]).setOverlay(overlay).setLight(light)
                        .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
            }
        } finally {
            pose.popPose();
        }
    }

    /**
     * The original equip progress of the item in hand.
     *
     * <p>Original ItemRenderer.updateEquippedItem:547-573: the progress of the
     * held item is one while the same item stays in the hand and moves at most
     * 0.4 per tick in both directions. The native port owns that curve in
     * {@code thaumcraft.client.lib.UtilsFX#getEquippedProgress}.
     */
}
