package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.relics.ItemThaumometer;

/**
 * The scan readout of the thaumometer of the original.
 *
 * <p>Original {@code thaumcraft/client/renderers/item/ItemThaumometerRenderer.java:155-259}: while
 * the instrument is raised in the first person view, the screen of the scanner shows the name of
 * the target, the aspect icons of a target that was already scanned and the type of a node. The
 * readout is drawn in the world space of the item, in front of the scan screen, with the additive
 * blend of the original.</p>
 *
 * <p>The numbers of this class are the numbers of the original:</p>
 * <ul>
 *   <li>the readout is pushed in front of the scan screen by {@code -0.01} (oracle
 *       {@code ItemThaumometerRenderer.java:164});</li>
 *   <li>the aspect icons are drawn with the scale {@code 0.0075}, initially five per row
 *       with the row width decreasing by one each time; the first row starts at
 *       {@code -min(5, count) * 8} and every row is sixteen pixels lower
 *       (oracle {@code ItemThaumometerRenderer.java:222-238});</li>
 *   <li>the name is drawn with the scale {@code 0.005}, which shrinks by {@code 2.5e-5} for
 *       every pixel of the name that is wider than ninety, below the icons at
 *       {@code -0.25} (oracle {@code ItemThaumometerRenderer.java:243-258});</li>
 *   <li>the type of a node is drawn with the scale {@code 0.004} in the colour
 *       {@code 0xEEAE16} (oracle {@code ItemThaumometerRenderer.java:194-203}).</li>
 * </ul>
 */
public final class ThaumometerReadout {
    /** Original ItemThaumometerRenderer:235: the first row holds up to five aspect icons. */
    public static final int ICONS_PER_ROW = 5;
    /** Original ItemThaumometerRenderer:228: the scale of the aspect icons. */
    public static final float ICON_SCALE = 0.0075F;
    /** Original ItemThaumometerRenderer:250: the scale of the name of the target. */
    public static final float NAME_SCALE = 0.005F;
    /** Original ItemThaumometerRenderer:251-253: the name shrinks after ninety pixels. */
    public static final float NAME_SHRINK = 2.5E-5F;
    /** Original ItemThaumometerRenderer:251: the width after which the name shrinks. */
    public static final int NAME_MAX_WIDTH = 90;
    /** Original ItemThaumometerRenderer:248: the name sits below the icons. */
    public static final float NAME_OFFSET_Y = -0.25F;
    /** Original ItemThaumometerRenderer:164: the readout is pushed in front of the screen. */
    public static final float READOUT_Z = -0.01F;
    /** Original ItemThaumometerRenderer:200: the scale of the type of a node. */
    public static final float NODE_SCALE = 0.004F;
    /**
     * Original ItemThaumometerRenderer:201: the colour of the type of a node. The 1.7.10 font
     * renderer forces the alpha of a colour that has none, so the colour is opaque here too.
     */
    public static final int NODE_COLOUR = 0xFFEEAE16;

    private ThaumometerReadout() {}

    /**
     * Original ItemThaumometerRenderer:250-253: the scale of the name of a target.
     *
     * @param width the width of the name in pixels
     */
    public static float nameScale(int width) {
        float scale = NAME_SCALE;
        if (width > NAME_MAX_WIDTH) scale -= NAME_SHRINK * (width - NAME_MAX_WIDTH);
        return scale;
    }

    /**
     * Original ItemThaumometerRenderer:225-238: the x offset of the first row of the icons.
     *
     * @param aspectCount the number of the aspects of the target
     */
    public static int firstRowOffset(int aspectCount) {
        return Math.min(ICONS_PER_ROW, aspectCount) * 8;
    }

    /**
     * Original ItemThaumometerRenderer:235-238: the number of the icons of a row.
     *
     * @param aspectCount the number of the aspects of the target
     * @param row         the row, zero based
     */
    public static int iconsInRow(int aspectCount, int row) {
        int perRow = ICONS_PER_ROW - row;
        int remaining = aspectCount - row * ICONS_PER_ROW;
        return Math.max(0, Math.min(perRow, remaining));
    }

    /**
     * The whole readout of the original.
     *
     * @param target the target of the scan
     * @param scanned true when the target was already scanned, which is the only case in which
     *                the original draws the aspect icons
     * @param light   the packed light of the readout
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, ItemThaumometer.Readout target,
                              boolean scanned, int light) {
        if (target == null) return;
        var font = Minecraft.getInstance().font;
        pose.pushPose();
        try {
            // Original ItemThaumometerRenderer:164.
            pose.translate(0.0F, 0.0F, READOUT_Z);
            if (scanned && target.aspects() != null && target.aspects().size() > 0) {
                icons(pose, buffers, font, target.aspects(), light);
            }
            if (target.node() != null && !target.node().isEmpty()) {
                // Original ItemThaumometerRenderer:191-203: the type of the node.
                pose.pushPose();
                try {
                    int width = font.width(target.node());
                    pose.scale(NODE_SCALE, NODE_SCALE, NODE_SCALE);
                    font.drawInBatch(target.node(), -width / 2, -40, NODE_COLOUR, false, pose.last().pose(), buffers,
                            Font.DisplayMode.NORMAL, 0, light);
                } finally {
                    pose.popPose();
                }
            }
            String name = target.name();
            if (name == null || name.isEmpty()) name = "?";
            // Original ItemThaumometerRenderer:243-258.
            pose.pushPose();
            try {
                int width = font.width(name);
                pose.translate(0.0F, NAME_OFFSET_Y, 0.0F);
                float scale = nameScale(width);
                pose.scale(scale, scale, scale);
                font.drawInBatch(name, -width / 2, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                        Font.DisplayMode.NORMAL, 0, light);
            } finally {
                pose.popPose();
            }
        } finally {
            pose.popPose();
        }
    }

    /**
     * The aspect icons of the original.
     *
     * <p>Original {@code ItemThaumometerRenderer.java:221-239}: the icons are the glyph of the
     * aspect with the colour of the aspect, the additive blend and the amount of the aspect
     * below, initially five per row and then one fewer in each successive row.</p>
     */
    private static void icons(PoseStack pose, MultiBufferSource buffers, Font font, AspectList aspects, int light) {
        AspectLayout layout = new AspectLayout(aspects.size());
        for (Aspect aspect : aspects.getAspectsSortedAmount()) {
            pose.pushPose();
            try {
                pose.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
                int colour = aspect.getColor();
                // Original UtilsFX.drawTag:548-582 draws the glyph of the aspect with the colour
                // of the aspect and the blend (GL_SRC_ALPHA, GL_ONE). The alpha of the glyph is
                // one, so the additive transparency of the vanilla RenderType.eyes is the very
                // blend of the original.
                VertexConsumer out = buffers.getBuffer(RenderType.eyes(aspect.getImage()));
                float r = (colour >> 16 & 255) / 255.0F, g = (colour >> 8 & 255) / 255.0F, b = (colour & 255) / 255.0F;
                float[] points = {0.0F, 16.0F, 0.01F, 16.0F, 16.0F, 0.01F, 16.0F, 0.0F, 0.01F, 0.0F, 0.0F, 0.01F};
                float[] uv = {0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F};
                for (int index = 0; index < 4; index++) {
                    out.addVertex(pose.last(), points[index * 3] + layout.x(), points[index * 3 + 1] + layout.y(), points[index * 3 + 2])
                            .setColor(r, g, b, 1.0F)
                            .setUv(uv[index * 2], uv[index * 2 + 1]).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
                }
                // Original UtilsFX.drawTag: the amount of the aspect in white at half the size, at the
                // lower right corner of ITS glyph; the thaumometer passes the blend 1, which draws no
                // black outline.
                String amount = Integer.toString(aspects.getAmount(aspect));
                pose.pushPose();
                try {
                    pose.scale(0.5F, 0.5F, 0.5F);
                    font.drawInBatch(amount, amountX(font.width(amount), layout.x()), amountY(font.lineHeight, layout.y()),
                            0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
                } finally {
                    pose.popPose();
                }
            } finally {
                pose.popPose();
            }
            layout.next();
        }
    }

    /**
     * The x of an amount, in the half size space of the text: original {@code UtilsFX.drawTag}
     * draws it at {@code 32 - width + x * 2}, right-aligned to the right edge of the glyph at
     * {@code x}.
     */
    public static int amountX(int textWidth, int iconX) { return 32 - textWidth + iconX * 2; }

    /** The y of an amount: {@code 32 - FONT_HEIGHT + y * 2}, on the bottom edge of the glyph at {@code y}. */
    public static int amountY(int lineHeight, int iconY) { return 32 - lineHeight + iconY * 2; }

    /** The original JAR's bytecode1490-1529; CFR's combined expression changes evaluation order. */
    static final class AspectLayout {
        private int column, row, remaining, offset;
        AspectLayout(int count) { remaining = count; offset = firstRowOffset(count); }
        int x() { return -offset + column * 16; }
        int y() { return -8 + row * 16; }
        void next() {
            if (++column >= ICONS_PER_ROW - row) {
                column = 0;
                row++;
                remaining -= ICONS_PER_ROW - row;
                offset = Math.min(ICONS_PER_ROW - row, remaining) * 8;
            }
        }
    }

    /**
     * The brightness of the readout of the original.
     *
     * <p>Original {@code ItemThaumometerRenderer.java:157}: the lightmap coordinate of the
     * readout is {@code 190 + sin(ticks - random(2)) * 10 + 10}.</p>
     *
     * @param ticks  the ticks of the player
     * @param jitter the random offset of the original, zero or one
     */
    public static int brightness(int ticks, int jitter) {
        return (int) (190.0F + (float) Math.sin(ticks - jitter) * 10.0F + 10.0F);
    }

    /**
     * The 1.7.10 lightmap coordinate of the original, converted to the 1.21.1 pack.
     *
     * <p>Original {@code UtilsFX.renderQuadCenteredFromTexture}:766-786 hands the brightness to
     * {@code Tessellator.setBrightness} (MCP {@code func_78380_c}), which stores it as the composed
     * lightmap coordinate of a fixed-function texture unit: the Block light lives in the low 16
     * bits and the Sky light in the high 16, and the layer is sampled in 1/16 units
     * ({@code OpenGlHelper.setLightmapTextureCoords(lightmapTexUnit, brightness % 65536,
     * brightness / 65536)} with the vanilla {@code 240.0F} for full brightness).</p>
     *
     * <p>So a brightness of 190..210 is <b>Block 11..13 and Sky 0</b>, not the
     * {@code pack(brightness % 16, (brightness / 16) % 16)} of an earlier revision, which turned
     * the same number into Block 14/8/2 and Sky 11/12/13 - a light that changed two components
     * in opposite directions as the value drifted, and made the screen flicker and wash out.</p>
     */
    public static int lightmap(int brightness) {
        // 1.7.10: block = brightness % 65536, sky = brightness / 65536, both in 1/16 units.
        // LightTexture.pack then shifts the 0..15 levels into the packed coordinate itself
        // (pack(block, sky) = block << 4 | sky << 20), so the levels are handed over unscaled.
        int block = (brightness & 0xFFFF) / 16;
        int sky = (brightness >>> 16) / 16;
        return LightTexture.pack(Math.min(block, 15), Math.min(sky, 15));
    }

    /** The packed light of the readout of the original. */
    public static int packedLight(int ticks, int jitter) {
        return lightmap(brightness(ticks, jitter));
    }

    /** The text of the readout, for the tests and for the log. */
    public static String textOf(ItemThaumometer.Readout target) {
        if (target == null) return "";
        String name = target.name();
        return name == null || name.isEmpty() ? "?" : name;
    }
}
