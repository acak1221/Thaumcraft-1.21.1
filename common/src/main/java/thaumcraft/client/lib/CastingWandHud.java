package thaumcraft.client.lib;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.foci.FocusData;

/** Original ClientTickEventsFML:397-485. This is a WAND dial, not a thaumometer reticle. */
public final class CastingWandHud {
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/hud.png");
    private static final java.util.Map<Integer, WandData> OLD = new java.util.HashMap<>();
    private static long nextSync;
    private static Object level;

    private CastingWandHud() {}

    public static void render(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        if (mc.level != level) { OLD.clear(); nextSync = 0; level = mc.level; }
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
        var stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemWandCasting)) return;
        var data = ItemWandCasting.data(stack);
        int slot = mc.player.getInventory().selected;
        long now = System.currentTimeMillis();
        if (!OLD.containsKey(slot)) OLD.put(slot, data);
        else if (nextSync <= now) { OLD.put(slot, data); nextSync = now + 1000; }
        var focusStack = FocusData.get(stack);
        var costs = focusStack.getItem() instanceof ItemFocusBasic focus ? focus.getVisCost(focusStack) : null;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        // Native GUI pose replaces the original GL matrix stack. Default dialBottom=false.
        graphics.pose().pushPose();
        graphics.pose().scale(.5F, .5F, .5F);
        graphics.blit(HUD, 0, 0, 0, 0, 64, 64, 256, 256);
        graphics.pose().popPose();
        graphics.pose().translate(16, 16, 0);
        int count = 0;
        for (String tag : WandData.PRIMALS) {
            // Native font rendering may tear down blending after a sneaking label.
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Aspect aspect = Aspect.getAspect(tag);
            int amount = data.amount(tag);
            graphics.pose().pushPose();
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(90));
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-15 + count * 24));
            graphics.pose().translate(0, -32, 0);
            graphics.pose().scale(.5F, .5F, .5F);
            int loc = (int)(30F * amount / ItemWandCasting.getMaxVis(stack));
            int color = aspect.getColor();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, .8F);
            graphics.blit(HUD, -4, 35 - loc, 104, 0, 8, loc, 256, 256);
            graphics.setColor(1, 1, 1, 1);
            graphics.blit(HUD, -8, -3, 72, 0, 16, 42, 256, 256);
            int shift = 0;
            if (costs != null && costs.getAmount(aspect) > 0) {
                graphics.blit(HUD, -4, -8, 136, 0, 8, 8, 256, 256);
                shift = 8;
            }
            int old = OLD.get(slot).amount(tag);
            if (old != amount) graphics.blit(HUD, -4, -8 - shift, old > amount ? 128 : 120, 0, 8, 8, 256, 256);
            if (mc.player.isShiftKeyDown()) {
                graphics.pose().mulPose(Axis.ZP.rotationDegrees(-90));
                graphics.drawCenteredString(mc.font, "" + amount / 100, -32, -4, 0xFFFFFFFF);
            }
            graphics.pose().popPose();
            count++;
        }
        graphics.pose().popPose();
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}
