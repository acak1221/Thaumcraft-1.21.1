package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/**
 * The item form of the aura node of the original.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/tile/ItemNodeRenderer.java:37-170}. The original
 * draws the item of the invisible aura node as the node itself: a fixture node with the four
 * primal aspects air, fire, earth and water at forty each and the type {@code NORMAL}
 * ({@code ItemNodeRenderer:39}), drawn as flat animated strips of the 32 frame node atlas
 * ({@code :72-170}).</p>
 *
 * <p>The three strips of the original are the same quad in three orientations: the first one in the
 * plane of the item, the second one spun in that very plane by ninety degrees about the Z axis
 * ({@code :64}) and the third one tipped by ninety degrees about the X axis ({@code :66}), so the
 * item shows two crossed squares. The whole node is placed at the centre of the item and scaled by
 * two ({@code :61-62}).</p>
 *
 * <p>The numbers of the strips are the original's: the alpha of the node starts at one half
 * ({@code :75}), the alpha of an aspect strip is the alpha of the node divided by the number of the
 * aspects ({@code :117}), the pulse of a strip is
 * {@code sin(ticks / (14 - count)) * 0.25 + 0.5} and its size is {@code 0.2 + pulse * amount / 50}
 * ({@code :115-116}), the core strip is {@code 0.1 + average / 150} ({@code :127}) and the frame of
 * the atlas is {@code (nanoseconds / 40000000 + 1) % 32} ({@code :103,126}).</p>
 *
 * <p>Forced 1.21.1 change: the 1.7.10 item space is a sixteenth of a block and the native item
 * space is not, so the placement of the node inside the item is the native one; the numbers of the
 * strips themselves are the original's.</p>
 */
public final class ItemNodeRenderer {
    /** The fixture node of the original item renderer: air, fire, earth and water at forty each. */
    public static final AspectList ITEM_ASPECTS = new AspectList()
            .add(Aspect.AIR, 40).add(Aspect.FIRE, 40).add(Aspect.EARTH, 40).add(Aspect.WATER, 40);

    /** The alpha the original starts the node of the item with ({@code ItemNodeRenderer:75}). */
    public static final float ITEM_ALPHA = 0.5F;
    /** The number of frames of the node atlas ({@code ItemNodeRenderer:100}). */
    public static final int ATLAS_FRAMES = 32;
    /** The scale the original puts the node of the item at ({@code ItemNodeRenderer:62}). */
    public static final float ITEM_SCALE = 2.0F;
    /** The base of the pulse of a strip ({@code ItemNodeRenderer:97}). */
    public static final float PULSE_BASE = 0.25F;

    private ItemNodeRenderer() {}

    /**
     * The item renderer of the aura node.
     *
     * @param stack the item that is drawn
     * @param context the context the item is drawn in
     * @param pose the pose of the item
     * @param buffers the buffers of the client
     * @param light the light of the item
     * @param overlay the overlay of the item
     */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (context == ItemDisplayContext.NONE) return;
        pose.pushPose();
        try {
            // Original ItemNodeRenderer:50-54 - the item that lies in the world is moved back by
            // half a block and a quarter of a block; the other contexts are placed by the native
            // item renderer itself.
            if (context == ItemDisplayContext.GROUND) pose.translate(-0.5F, -0.25F, -0.5F);
            // Original ItemNodeRenderer:60-68 - the node of the item sits in the centre of the
            // item and is scaled by two.
            pose.pushPose();
            try {
                pose.translate(0.5F, 0.5F, 0.5F);
                pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
                renderItemNode(pose, buffers, ITEM_ASPECTS, NodeType.NORMAL, null, ITEM_ALPHA);
                // Original ItemNodeRenderer:64 - the second strip is spun in the plane of the item.
                pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                renderItemNode(pose, buffers, ITEM_ASPECTS, NodeType.NORMAL, null, ITEM_ALPHA);
                // Original ItemNodeRenderer:66 - the third strip is tipped about the X axis.
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                renderItemNode(pose, buffers, ITEM_ASPECTS, NodeType.NORMAL, null, ITEM_ALPHA);
            } finally { pose.popPose(); }
        } finally { pose.popPose(); }
    }

    /**
     * The strips of the node of the item.
     *
     * <p>Original {@code ItemNodeRenderer:72-170}. The strips are flat quads of the node atlas; they
     * are <em>not</em> turned towards the viewer, because the original item renderer draws them in
     * the frame of the item itself.</p>
     *
     * @param pose the pose of the item
     * @param buffers the buffers of the client
     * @param aspects the aspects of the node
     * @param type the type of the node
     * @param modifier the modifier of the node
     * @param alpha the alpha of the node
     */
    public static void renderItemNode(PoseStack pose, MultiBufferSource buffers, AspectList aspects,
                                      NodeType type, NodeModifier modifier, float alpha) {
        if (aspects == null || aspects.size() == 0) return;
        long now = System.nanoTime();
        // Original ItemNodeRenderer:103 and :126 - the frame of the atlas, with the offset of one.
        int frame = atlasFrame(now);
        float value = alpha;
        if (modifier == NodeModifier.BRIGHT) value *= 1.5F;
        else if (modifier == NodeModifier.PALE) value *= 0.66F;
        else if (modifier == NodeModifier.FADING) value *= (float) Math.sin(ticks() / 3.0F) * 0.25F + 0.33F;
        float total = 0.0F;
        int count = 0;
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            total += amount;
            // Original ItemNodeRenderer:108-109 and :121-122 - a strip of a blended aspect is
            // brighter and the alpha of the node is restored after it.
            float brightness = aspect.getBlend() == 771 ? value * 1.5F : value;
            // Original ItemNodeRenderer:115-116 - the pulse and the size of the strip.
            float size = stripSize(stripPulse(ticks(), count), amount);
            // Original ItemNodeRenderer:117 - the alpha of an aspect strip.
            float opacity = stripAlpha(brightness, aspects.size());
            TileNodeRenderer.quad(pose, buffers.getBuffer(aspect.getBlend() == 771
                            ? TileNodeRenderer.TRANSLUCENT : TileNodeRenderer.ADDITIVE),
                    size, opacity, aspect.getColor(), frame, 0);
            ++count;
        }
        // Original ItemNodeRenderer:124-162 - the core strip of the type of the node.
        int strip = switch (type) {
            case NORMAL -> 1;
            case UNSTABLE -> 6;
            case DARK -> 2;
            case TAINTED -> 5;
            case PURE -> 4;
            case HUNGRY -> 3;
        };
        float core = coreSize(total, aspects.size());
        if (type == NodeType.HUNGRY) core *= 0.75F;
        boolean dark = type == NodeType.DARK || type == NodeType.TAINTED;
        TileNodeRenderer.quad(pose, buffers.getBuffer(dark ? TileNodeRenderer.TRANSLUCENT : TileNodeRenderer.ADDITIVE),
                core, value, 0xFFFFFF, frame, strip);
    }

    /**
     * The frame of the node atlas the original draws ({@code ItemNodeRenderer:103,126}).
     *
     * @param nanoseconds the clock of the client
     * @return the frame of the atlas, from zero to thirty one
     */
    public static int atlasFrame(long nanoseconds) {
        return (int) Math.floorMod(nanoseconds / 40000000L + 1L, (long) ATLAS_FRAMES);
    }

    /**
     * The pulse of an aspect strip of the original ({@code ItemNodeRenderer:115}).
     *
     * @param ticks the age of the client
     * @param count the index of the aspect in the list
     * @return the pulse of the strip
     */
    public static float stripPulse(float ticks, int count) {
        return (float) Math.sin(ticks / (14.0F - count)) * PULSE_BASE + PULSE_BASE * 2.0F;
    }

    /**
     * The size of an aspect strip of the original ({@code ItemNodeRenderer:116}).
     *
     * @param pulse the pulse of the strip
     * @param amount the essence of the aspect
     * @return the size of the strip
     */
    public static float stripSize(float pulse, int amount) {
        return 0.2F + pulse * (amount / 50.0F);
    }

    /**
     * The alpha of an aspect strip of the original ({@code ItemNodeRenderer:117}).
     *
     * @param alpha the alpha of the node
     * @param aspectCount the number of the aspects of the node
     * @return the alpha of the strip
     */
    public static float stripAlpha(float alpha, int aspectCount) {
        return alpha / aspectCount;
    }

    /**
     * The size of the core strip of the original ({@code ItemNodeRenderer:127}).
     *
     * @param total the sum of the essence of the aspects
     * @param aspectCount the number of the aspects of the node
     * @return the size of the core strip
     */
    public static float coreSize(float total, int aspectCount) {
        return 0.1F + (total / aspectCount) / 150.0F;
    }

    /** The age of the client the original pulses its strips with. */
    private static float ticks() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity viewer = minecraft.player;
        return viewer == null ? 0.0F : viewer.tickCount + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
    }
}
