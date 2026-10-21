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
 * <p>Two parts of the original have no equivalent in the native port and stay
 * open: the two rotations that follow the swing of the arm of the player
 * (original ItemThaumometerRenderer:103-104) need the private arm angles of
 * 1.7.10, and the scan readout of the original - the aspect icons and the name
 * of the scanned target - needs the client side of the research system, which
 * the native port keeps on the server.
 */
public final class ItemThaumometerRenderer {
    /** Original ItemThaumometerRenderer:95 - the size factor of the first person view. */
    private static final float FIRST_PERSON_SCALE = 0.8F;
    /** Original ItemRenderer.updateEquippedItem:547-573 - the progress moves at most 0.4 per tick. */
    private static final float EQUIP_STEP = 0.4F;
    private static final ModelThaumometer MODEL = new ModelThaumometer();

    private static ItemStack equippedStack;
    private static float equippedProgress;

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
                    pose.mulPose(Axis.XP.rotationDegrees(60.0F));
                    pose.mulPose(Axis.ZN.rotationDegrees(30.0F));
                    pose.mulPose(Axis.ZN.rotationDegrees(248.0F));
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
        // Original ItemThaumometerRenderer:103-104 rotates with the difference
        // between the rotation of the player and the swing of its arm. The arm
        // angles of 1.7.10 have no public equivalent in 1.21.1, so the two
        // rotations of the original are left out.
        // Original ItemThaumometerRenderer:107.
        float progress = equipProgress(mc, player.getMainHandItem());
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
        int brightness = (int) (190.0F + (float) Math.sin(ticks - jitter) * 10.0F + 10.0F);
        int light = LightTexture.pack(brightness % 16, (brightness / 16) % 16);
        var out = buffers.getBuffer(RenderType.entityTranslucent(ModelThaumometer.SCREEN));
        float[] points = {-0.5F, 0.5F, 0.0F, 0.5F, 0.5F, 0.0F, 0.5F, -0.5F, 0.0F, -0.5F, -0.5F, 0.0F};
        float[] uv = {0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F};
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), points[index * 3], points[index * 3 + 1], points[index * 3 + 2])
                    .setColor(255, 255, 255, 255)
                    .setUv(uv[index * 2], uv[index * 2 + 1]).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
        }
    }

    /**
     * The original equip progress of the item in hand.
     *
     * <p>Original ItemRenderer.updateEquippedItem:547-573: the progress of the
     * held item is one while the same item stays in the hand and moves at most
     * 0.4 per tick in both directions.
     */
    private static float equipProgress(Minecraft mc, ItemStack held) {
        boolean same = equippedStack != null && ItemStack.matches(equippedStack, held);
        equippedStack = held;
        float target = same ? 1.0F : 0.0F;
        float step = Math.max(-EQUIP_STEP, Math.min(EQUIP_STEP, target - equippedProgress));
        equippedProgress += step;
        return equippedProgress;
    }
}
