package thaumcraft.compat.jade;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.ThaumcraftGogglesClient;

/**
 * One aspect icon with its amount in the corner, as the goggles popup and the EMI aspect stack
 * draw it: the tinted aspect image, or the unknown image for an aspect the player has not
 * discovered.
 */
final class AspectElement extends Element {
    private final Aspect aspect;
    private final String amount;
    private final boolean known;

    AspectElement(Aspect aspect, int amount, boolean known) {
        this.aspect = aspect;
        this.amount = amount > 0 ? Integer.toString(amount) : "";
        this.known = known;
        message(known ? aspect.getName() + " " + amount : "? " + amount);
    }

    @Override public Vec2 getSize() { return new Vec2(18, 18); }

    @Override public void render(GuiGraphics graphics, float x, float y, float maxX, float maxY) {
        int color = known ? aspect.getColor() : 0xFFFFFF;
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        try {
            graphics.blit(known ? aspect.getImage() : ThaumcraftGogglesClient.UNKNOWN_ASPECT, Math.round(x) + 1, Math.round(y) + 1, 0, 0, 16, 16, 16, 16);
        } finally {
            graphics.flush();
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
        if (amount.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 200);
            graphics.drawString(font, amount, Math.round(x) + 18 - font.width(amount), Math.round(y) + 10, 0xFFFFFFFF, true);
        } finally {
            graphics.pose().popPose();
        }
    }
}
