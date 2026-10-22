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
 * ({@code renderAspectsInGui}). The original draws the row of the aspects of the item
 * of the hovered slot of a container screen at the mouse position, one icon every
 * eighteen pixels, each icon on the twenty pixel wide {@code _back.png} background, and
 * the {@code _unknown.png} icon for an aspect that the player has not discovered yet.</p>
 *
 * <p>The original shows the row when the state of the shift key differs from the
 * {@code display_aspects} configuration flag: the flag is false by default
 * (oracle {@code thaumcraft/common/config/Config.java:458-460,849}), so the row
 * appears while the shift key is held ("Item aspects are hidden by default and pressing
 * shift reveals them", the comment of the original configuration), and with the flag
 * true the row is always shown unless the shift key is held.</p>
 */
public final class HoveredSlotAspectRow {
    /** Original ClientTickEventsFML:678-679 - the row is shifted by eight pixels. */
    private static final int SHIFT = -8;
    /** Original ClientTickEventsFML:691-692 - the first icon of the row. */
    private static final int FIRST_X = 17;
    private static final int FIRST_Y = 7 - 33;
    /** Original ClientTickEventsFML:698 - the distance between two icons of the row. */
    private static final int STEP = 18;
    /** Original ClientTickEventsFML:704-706 - the background of one icon is scaled by 1.25. */
    private static final float BACK_SCALE = 1.25F;
    /** Original ClientTickEventsFML:704 - the background is drawn two pixels above the icon. */
    private static final int BACK_OFFSET = -2;

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
        if (Screen.hasShiftDown() == Config.showTags) return;
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        // Original ClientTickEventsFML:685-687 - only a scanned object shows its row.
        String id = "@" + BuiltInRegistries.ITEM.getKey(stack.getItem());
        var scanned = KnowledgeAccess.get(player).getScannedItems();
        if (scanned == null || (!scanned.contains(id) && !scanned.contains("#" + id))) return;
        // Original ClientTickEventsFML:688-690 - the object tags and the bonus tags.
        AspectList tags = ObjectAspectResolver.get(stack, player.level());
        if (tags == null || tags.size() == 0) return;
        Aspect[] aspects = tags.getAspectsSortedAmount();
        int index = 0;
        for (Aspect aspect : aspects) {
            if (aspect == null) continue;
            int x = mouseX + FIRST_X + index * STEP;
            int y = mouseY + FIRST_Y;
            background(graphics, x, y);
            if (KnowledgeAccess.get(player).hasDiscoveredAspect(aspect.getTag())) {
                icon(graphics, aspect.getImage(), x, y, aspect.getColor(), 1.0F);
                int amount = tags.getAmount(aspect);
                if (amount > 0) amount(graphics, amount, x, y);
            } else {
                icon(graphics, UNKNOWN, x, y, 0xFFFFFF, 1.0F);
            }
            index++;
        }
    }

    /** Original ClientTickEventsFML:700-708 - the background of one icon. */
    private static void background(GuiGraphics graphics, int x, int y) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + SHIFT + BACK_OFFSET, y + SHIFT + BACK_OFFSET, 0.0F);
            graphics.pose().scale(BACK_SCALE, BACK_SCALE, 1.0F);
            blit(graphics, ASPECT_BACK, 0, 0, 0, 0, 16, 16, 16, 16, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            graphics.pose().popPose();
        }
    }

    /** Original UtilsFX.drawTag:552-581 - the aspect icon with the colour of the aspect. */
    private static void icon(GuiGraphics graphics, ResourceLocation texture, int x, int y, int color, float alpha) {
        blit(graphics, texture, x + SHIFT, y + SHIFT, 0, 0, 16, 16, 16, 16,
                (color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, alpha);
    }

    /** Original UtilsFX.drawTag:583-599 - the amount of the aspect at half scale. */
    private static void amount(GuiGraphics graphics, int amount, int x, int y) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.font == null) return;
        String text = Integer.toString(amount);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + 16, y + 11, 0.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(minecraft.font, text, -minecraft.font.width(text), 0, 0xFFFFFF, true);
        } finally {
            graphics.pose().popPose();
        }
    }

    /**
     * The plain 1.21.1 texture blit does not enable the blending, the original draws the
     * row with the ordinary alpha blend (770, 771).
     */
    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v,
                             int width, int height, int textureWidth, int textureHeight,
                             float red, float green, float blue, float alpha) {
        graphics.flush();
        graphics.setColor(red, green, blue, alpha);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } finally {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
}
