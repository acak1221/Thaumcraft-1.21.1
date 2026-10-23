package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.Config;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/**
 * The aspect row of the hovered container slot.
 *
 * <p>Original {@code thaumcraft/client/lib/ClientTickEventsFML.java:660-729}
 * ({@code renderAspectsInGui}). For the hovered slot of a container screen it draws the aspects
 * of the item at the mouse position, one icon every eighteen pixels, each icon on the twenty
 * pixel wide {@code _back.png} background scaled by {@code 1.25}, and the {@code _unknown.png}
 * icon for an aspect that the player has not discovered yet.</p>
 *
 * <p>The original shows the row when the state of the shift key differs from the
 * {@code display_aspects} configuration flag: the flag is false by default
 * (oracle {@code thaumcraft/common/config/Config.java:458-460,849}), so the row appears while
 * the shift key is held ("Item aspects are hidden by default and pressing shift reveals them",
 * the comment of the original configuration), and with the flag true the row is always shown
 * unless the shift key is held (oracle {@code ClientTickEventsFML.java:301-309}).</p>
 *
 * <p>The icon itself is the coloured aspect icon with alpha 1: line 710 of the oracle calls
 * {@code UtilsFX.drawTag(x + shiftx, y + shifty, tag, tags.getAmount(tag), 0, z)} — the
 * six-argument overload at {@code UtilsFX.java:540-542}, which forwards
 * {@code blend = 771, alpha = 1.0f, bw = false}. Only the three-argument overload
 * ({@code UtilsFX.java:544-546}, {@code bw = true}) draws the dark silhouette, and the aspect row
 * does not use it.</p>
 */
public final class HoveredSlotAspectRow {
    /** Original ClientTickEventsFML:678-679 - the row is shifted by eight pixels. */
    public static final int SHIFT = -8;
    /** Original ClientTickEventsFML:691 - {@code x = mouseX + 17}. */
    public static final int FIRST_X = 17;
    /** Original ClientTickEventsFML:692 - {@code y = mouseY + 7 - 33}. */
    public static final int FIRST_Y = 7 - 33;
    /** Original ClientTickEventsFML:698 - {@code x = mouseX + 17 + index * 18}. */
    public static final int STEP = 18;
    /** Original ClientTickEventsFML:704-706 - the background of one icon is scaled by 1.25. */
    public static final float BACK_SCALE = 1.25F;
    /** Original ClientTickEventsFML:704 - the background is drawn two pixels above the icon. */
    public static final int BACK_OFFSET = -2;
    /**
     * The original {@code Thaumcraft.instance.aspectShift} (oracle
     * {@code ClientProxy.java:443-446}) is set only when OptiFine is loaded or NEI is present, and
     * shifts the whole row by another eight pixels so the overlay of those mods does not cover
     * it. The native port runs without either, so the faithful value is {@code false}.
     */
    public static final boolean ASPECT_SHIFT = false;
    /** Original UtilsFX.drawTag:540-542 - the row uses the six-argument overload, blend 771. */
    public static final int ROW_BLEND = 771;
    /** Original UtilsFX.drawTag:540-542 - the row uses the six-argument overload, alpha 1. */
    public static final float ROW_ALPHA = 1.0F;

    private static final ResourceLocation ASPECT_BACK = texture("aspects/_back.png");
    private static final ResourceLocation UNKNOWN = texture("aspects/_unknown.png");

    private HoveredSlotAspectRow() {}

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path);
    }

    /**
     * Draws the aspect row of the hovered slot.
     *
     * @param graphics the graphics of the screen
     * @param slot     the hovered slot, the row is not drawn without one
     * @param mouseX   the mouse position in the screen coordinates
     * @param mouseY   the mouse position in the screen coordinates
     */
    public static void render(GuiGraphics graphics, Slot slot, int mouseX, int mouseY) {
        if (slot == null || !slot.hasItem()) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || player.level() == null) return;
        // Original ClientTickEventsFML:301-309 - the row is shown when the shift key state
        // differs from the display_aspects flag of the configuration.
        if (!shouldShow(Screen.hasShiftDown(), Config.showTags)) return;
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        // Original ClientTickEventsFML:685-687 - only a scanned object shows its row.
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var scanned = KnowledgeAccess.get(player).getScannedItems();
        if (!isScanned(scanned, id)) return;
        // Original ClientTickEventsFML:688-690 - the object tags and the bonus tags.
        AspectList tags = ObjectAspectResolver.get(stack, player.level());
        if (tags == null || tags.size() == 0) return;
        Aspect[] aspects = tags.getAspectsSortedAmount();
        graphics.flush();
        boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        graphics.pose().pushPose();
        try {
        // Tooltip text lives at z=400 in native GuiGraphics. The original disables depth
        // for the entire row (ClientTickEventsFML:693-724), so it must cover that text too.
        graphics.pose().translate(0, 0, 500);
        RenderSystem.disableDepthTest();
        int index = 0;
        for (Aspect aspect : aspects) {
            if (aspect == null) continue;
            int x = iconX(mouseX, index);
            int y = iconY(mouseY);
            background(graphics, x, y);
            if (KnowledgeAccess.get(player).hasDiscoveredAspect(aspect.getTag())) {
                // Original ClientTickEventsFML:710 -> UtilsFX.drawTag(..., 771, 1.0f, false):
                // the coloured icon of the aspect and its amount, NOT a silhouette.
                icon(graphics, aspect.getImage(), x, y, aspect.getColor(), ROW_ALPHA);
                amount(graphics, tags.getAmount(aspect), x, y);
            } else {
                // Original ClientTickEventsFML:712-719: the _unknown.png sheet at alpha 1,
                // with the colour left at white by the drawTag of the previous icon.
                icon(graphics, UNKNOWN, x, y, 0xFFFFFF, ROW_ALPHA);
            }
            index++;
        }
        } finally {
            try {
                graphics.flush();
            } finally {
                graphics.pose().popPose();
            }
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        }
    }

    public static boolean isScanned(java.util.Set<String> scanned, String id) {
        return scanned != null && (scanned.contains("@" + id) || scanned.contains("#" + id));
    }

    /**
     * The visibility rule of the original (oracle {@code ClientTickEventsFML.java:301-309}): the
     * row is drawn when the shift key differs from the {@code display_aspects} flag.
     */
    public static boolean shouldShow(boolean shiftDown, boolean showTags) {
        return shiftDown != showTags;
    }

    /** The x of the icon of the given index (oracle {@code ClientTickEventsFML.java:691,698}). */
    public static int iconX(int mouseX, int index) {
        return mouseX + FIRST_X + index * STEP;
    }

    /** The y of every icon of the row (oracle {@code ClientTickEventsFML.java:692}). */
    public static int iconY(int mouseY) {
        return mouseY + FIRST_Y;
    }

    /** The shift of the whole row: eight pixels, or sixteen under OptiFine or NEI. */
    public static int rowShift() {
        return ASPECT_SHIFT ? SHIFT - 8 : SHIFT;
    }

    /** The x of the background of one icon (oracle {@code ClientTickEventsFML.java:679,704}). */
    public static int backgroundX(int iconX) {
        return iconX + rowShift() + BACK_OFFSET;
    }

    /** The y of the background of one icon (oracle {@code ClientTickEventsFML.java:679,704}). */
    public static int backgroundY(int iconY) {
        return iconY + rowShift() + BACK_OFFSET;
    }

    /** Original ClientTickEventsFML:700-708 - the background of one icon, blend 770/771. */
    private static void background(GuiGraphics graphics, int x, int y) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(backgroundX(x), backgroundY(y), 0.0F);
            graphics.pose().scale(BACK_SCALE, BACK_SCALE, 1.0F);
            blit(graphics, ASPECT_BACK, 0, 0, 0, 0, 16, 16, 16, 16, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            graphics.pose().popPose();
        }
    }

    /**
     * Original UtilsFX.drawTag:552-581 with {@code bw = false}: the aspect icon tinted by the
     * colour of the aspect at the alpha of the caller, with the additive-source alpha blend
     * {@code (770, 771)} that the row passes.
     */
    private static void icon(GuiGraphics graphics, ResourceLocation texture, int x, int y, int color, float alpha) {
        blit(graphics, texture, x + rowShift(), y + rowShift(), 0, 0, 16, 16, 16, 16,
                (color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
    }

    /**
     * Original UtilsFX.drawTag:583-599 - the amount of the aspect.
     *
     * <p>The original scales by half, then draws the black outline (four orthogonal copies one
     * pixel away, because the blend is {@code 771 > 1}) and the white glyph at
     * {@code (32 - sw + (int)x * 2, 32 - fontHeight + (int)y * 2)} in the SCALED coordinate space.
     * Dividing by the half scale gives the unscaled offset {@code (16 - sw/2, 16 - fontHeight/2)}
     * from the icon's top left, which is what this method reproduces after the scale.</p>
     */
    private static void amount(GuiGraphics graphics, int amount, int x, int y) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.font == null || amount <= 0) return;
        String text = Integer.toString(amount);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + rowShift(), y + rowShift(), 0.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            int textX = 32 - minecraft.font.width(text);
            int textY = 32 - minecraft.font.lineHeight;
            // blend 771 > 1 in the original, so the black outline is drawn first.
            if (ROW_BLEND > 1) {
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        if ((a != 0) == (b != 0)) continue;
                        graphics.drawString(minecraft.font, text, textX + a, textY + b, 0xFF000000, false);
                    }
                }
            }
            graphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    /**
     * The plain 1.21.1 texture blit does not enable blending; the original draws the row with the
     * ordinary alpha blend {@code (770, 771)} and clears the colour afterwards.
     */
    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
                             int width, int height, int textureWidth, int textureHeight,
                             float red, float green, float blue, float alpha) {
        graphics.flush();
        graphics.setColor(red, green, blue, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } finally {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }
}
