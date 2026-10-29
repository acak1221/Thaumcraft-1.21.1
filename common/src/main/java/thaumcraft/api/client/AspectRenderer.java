package thaumcraft.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.AspectTagLayout;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;

/**
 * Draws the tag of an aspect in a screen or a HUD, the way Thaumcraft's own screens draw it.
 *
 * <p>This is the 1.21.1 counterpart of {@code thaumcraft.client.lib.UtilsFX.drawTag}, the helper every 1.7.10 add-on with a
 * screen that listed aspects called (an analyser, a research table, a tooltip). <b>Client only:</b> the class names
 * {@link GuiGraphics}, which a dedicated server does not have, so load it only from client code (a screen, a HUD callback, a
 * Fabric {@code client} entrypoint, a NeoForge {@code Dist.CLIENT} class). The geometry constants below are plain compile-time
 * constants and can be read from anywhere.</p>
 *
 * <p><b>Geometry.</b> The tag is a square of {@link #TAG_SIZE} (16) pixels whose top left corner is the {@code x}, {@code y}
 * you pass, in the coordinate space of the {@code GuiGraphics} (scaled GUI pixels, the pose stack applies):</p>
 * <ul>
 *   <li>the glyph {@code aspect.getImage()} is stretched over {@code (x, y)} to {@code (x + 16, y + 16)} and tinted with
 *       {@code aspect.getColor()}. An {@link Aspect} cannot be built without a texture location, so there is always a glyph to
 *       draw; if the file behind the location is missing, the square shows the game's missing-texture checkerboard, as any
 *       texture that fails to load does;</li>
 *   <li><b>amount</b>: above zero, the number is drawn at half size, white with a shadow, so that its right edge is the right
 *       edge of the glyph ({@code x + } {@link #AMOUNT_RIGHT}) and its top is {@link #AMOUNT_TOP} (11) pixels below {@code y};
 *       it sits in front of the glyph and in front of an item drawn at the same place (depth +200). Zero or less draws no
 *       number. This is the number of the research book and the table: <em>no</em> {@code +}, no thousands separator;</li>
 *   <li><b>bonus</b>: above zero, the sparkle of the research table is drawn over the top left corner of the glyph, a 16 by 16
 *       frame whose top left corner is {@link #BONUS_OFFSET} (-4) pixels from the tag's on both axes; it cycles through sixteen
 *       frames of the particle sheet, one per tick of the local player. Above one, the bonus is also written as a half size
 *       number centred on {@code x + } {@link #BONUS_NUMBER_CENTER_X} (4) with its top {@link #BONUS_NUMBER_TOP} (3) pixels
 *       below {@code y}, in front of the sparkle.</li>
 * </ul>
 * <p>The tag is not clipped, not scaled to the screen and gives no tooltip: the add-on decides where it is, whether the mouse is
 * over it (a {@code TAG_SIZE} square) and what the hover shows. To draw it larger or smaller, scale the pose stack around
 * {@code x}, {@code y} first.</p>
 *
 * <p>Blending is enabled while the tag is drawn and left enabled (the book relies on it); the colour of the graphics is
 * reset to white. Call it from the render thread only, between the begin and the end of a frame of the GUI.</p>
 *
 * <p><b>Differences from the original.</b> {@code draw} is {@code UtilsFX.drawTag} with the blend, the alpha and the black and
 * white switch left out, and <b>the amount is an {@code int}</b>: the original took a {@code float} and showed up to two
 * decimals ({@code DecimalFormat("#######.##")} in the audited legacy source of this repository), so a fraction of an essentia
 * showed there and does not here. The top of the amount is the whole pixel {@link #AMOUNT_TOP} (11), where that source starts
 * its line of text at {@code y + 11.5}.</p>
 *
 * <p><b>Verification.</b> The numbers above are asserted by unit tests (the order of the arguments, the thresholds of the amount
 * and of the bonus and the text of the numbers go through a pure layout object that the renderer reads); the pixels have
 * <em>not yet been inspected on a rendered frame</em> (see section 9 of {@code docs/audit/KNOWN_DEVIATIONS.md}): the tag and
 * its amount are the code the research book already uses for its aspect rows, and the bonus sparkle follows the one the research
 * table draws on its palette.</p>
 */
public final class AspectRenderer {
    /** The side of the tag in GUI pixels. */
    public static final int TAG_SIZE = 16;
    /** The right edge of the amount, from the left edge of the tag. */
    public static final int AMOUNT_RIGHT = 16;
    /** The top of the amount, from the top of the tag. */
    public static final int AMOUNT_TOP = 11;
    /** The distance of the top left corner of the bonus sparkle from the top left corner of the tag, on both axes. */
    public static final int BONUS_OFFSET = -4;
    /** The side of the bonus sparkle, in GUI pixels. */
    public static final int BONUS_SIZE = 16;
    /** The x of the middle of the bonus number, from the left edge of the tag. */
    public static final int BONUS_NUMBER_CENTER_X = 4;
    /** The top of the bonus number, from the top of the tag. */
    public static final int BONUS_NUMBER_TOP = 3;
    /** The number of frames of the bonus sparkle. */
    public static final int BONUS_FRAMES = 16;

    private AspectRenderer() {}

    /**
     * Original {@code UtilsFX.drawTag(x, y, aspect, amount, bonus, zLevel, blend, alpha, bw)} with the blend, the alpha and the
     * black and white switch left out, and with an {@code int} amount (the original took a {@code float}): draws the tag of an
     * aspect with its amount and its bonus at a position of a GUI.
     *
     * @param graphics the graphics of the frame being drawn (the {@code GuiGraphics} a screen's {@code render} or a HUD
     *                 callback is given); must not be {@code null} when an aspect is drawn
     * @param aspect   the aspect; {@code null} draws nothing
     * @param x        the left edge of the tag, in GUI pixels
     * @param y        the top edge of the tag, in GUI pixels
     * @param amount   the whole number shown at the bottom right corner; zero or less shows none
     * @param bonus    zero or less shows no bonus, one shows the sparkle, more shows the sparkle and the number
     * @throws NullPointerException when {@code aspect} is not {@code null} and {@code graphics} is {@code null}
     */
    public static void draw(GuiGraphics graphics, Aspect aspect, int x, int y, int amount, int bonus) {
        if (aspect == null) return;
        java.util.Objects.requireNonNull(graphics, "graphics");
        ThaumcraftRecipeDisplay.drawAspectTag(graphics, Minecraft.getInstance().font, aspect, AspectTagLayout.of(x, y, amount, bonus, 1.0F));
    }
}
