package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.ResearchNotifications;
import thaumcraft.common.lib.research.ResearchCatalog;

/** Exact original top-left160x32 achievement crop and3000ms quartic entry/exit; one queue advances per actual rendered frame. */
public final class GuiResearchPopup {
    private static final ResourceLocation FRAME=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/research_toast.png");
    private static final Pattern TEXTURE=Pattern.compile("new ResourceLocation\\(\\\"thaumcraft\\\",\\s*\\\"([^\\\"]+)\\\"\\)");
    private GuiResearchPopup() {}
    public static void renderHud(GuiGraphics graphics) {
        var client=Minecraft.getInstance();
        if(client.level==null||client.player==null)return;
        var frame=ResearchNotifications.frame();if(frame==null)return;
        var research=ResearchCatalog.get(frame.key());if(research==null)return;
        float[] old=RenderSystem.getShaderColor().clone();boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        graphics.pose().pushPose();
        try {
            // Original GuiResearchPopup draws the toast sheet and the research
            // icon with the ordinary alpha blend of the 1.7.10 GUI. The plain
            // 1.21.1 texture blit neither enables nor disables blending, and the
            // icon is a translucent glyph, so without the blend it is drawn as
            // solid black. The blend is switched on again before the icon,
            // because the text of the toast runs through the font render type,
            // whose teardown leaves the blend off.
            graphics.setColor(1,1,1,1);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(FRAME,0,frame.y(),0,0,160,32,160,32);
            graphics.drawString(client.font,"Research Completed!",30,frame.y()+7,0xFFFFFF00,false); // Original literal, not an invented localization key.
            String name=Component.translatable(research.nameKey()).getString();int width=client.font.width(name);
            if(width<=125)graphics.drawString(client.font,name,30,frame.y()+18,-1,false);
            else {
                float scale=125F/width;graphics.pose().pushPose();
                try { graphics.pose().translate(30,frame.y()+16+2F/scale,0);graphics.pose().scale(scale,scale,scale);graphics.drawString(client.font,name,0,0,-1,false); }
                finally { graphics.pose().popPose(); }
            }
            if(!ResearchIconDisplay.render(graphics,client.font,research.iconExpression(),8,frame.y()+8)) {
                var match=TEXTURE.matcher(research.iconExpression()==null?"":research.iconExpression());
                var texture=match.matches()?ResourceLocation.fromNamespaceAndPath("thaumcraft",match.group(1).toLowerCase(java.util.Locale.ROOT)):
                        ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/items/thaumonomicon.png");
                if(client.getResourceManager().getResource(texture).isEmpty())return;
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.blit(texture,8,frame.y()+8,16,16,0,0,256,256,256,256);
            }
        } finally {
            RenderSystem.disableBlend();
            graphics.pose().popPose();graphics.flush();graphics.setColor(old[0],old[1],old[2],old[3]);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
        }
    }
}
