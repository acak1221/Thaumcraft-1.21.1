package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.architectury.fluid.FluidStack;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.container.ContainerSpa;

/** Original176x166 spa art,48px fluid fill inside55px gauge and8x8 toggle.
 * Sprite comes from the actual registered native fluid renderer; the loader
 * supplies original WHITE, not a newly applied fluid or biome tint. */
public final class GuiSpa extends ThaumcraftContainerScreen<ContainerSpa> {
    private static final ResourceLocation BACKGROUND=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_spa.png");
    private static final SoundEvent CLICK=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","cameraclack"));
    public record FluidVisual(TextureAtlasSprite sprite,int argb) {}
    @FunctionalInterface public interface FluidAppearance {FluidVisual resolve(ClientLevel level,BlockPos pos,FluidStack stack);}
    private static FluidAppearance fluidAppearance;
    /** Loader installs its real native fluid handler bridge with the unit. */
    public static void setFluidAppearance(FluidAppearance appearance){fluidAppearance=Objects.requireNonNull(appearance);}
    public GuiSpa(ContainerSpa menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderBg(GuiGraphics graphics,float delta,int mouseX,int mouseY){
        graphics.setColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
        graphics.blit(BACKGROUND,leftPos+89,topPos+35,208,menu.getMix()?16:32,8,8,256,256);
        FluidStack fluid=menu.fluid();
        if(menu.getFluidAmountMb()>0&&!fluid.isEmpty()&&minecraft!=null&&minecraft.level!=null){
            FluidVisual visual=Objects.requireNonNull(fluidAppearance,"Native spa fluid renderer was not installed").resolve(minecraft.level,menu.spaPos(),fluid);
            // Original Fluid.getIcon could be null; preserve an unsupported
            // addon's no-icon case rather than substituting a water texture.
            if(visual!=null&&visual.sprite()!=null){
                renderFluid(graphics,visual,leftPos+107,topPos+15);
                float ratio=(float)menu.getFluidAmountMb()/menu.getCapacityMb();
                graphics.blit(BACKGROUND,leftPos+107,topPos+15,107,15,10,(int)(48F-48F*ratio),256,256);
            }
        }
        graphics.blit(BACKGROUND,leftPos+106,topPos+11,232,0,10,55,256,256);RenderSystem.disableBlend();
    }
    private static void renderFluid(GuiGraphics graphics,FluidVisual visual,int x,int y) {
        // Original GuiSpa uses maxU on the left and minU on the right. Keep
        // that horizontal icon mirror without reversing the quad's winding.
        graphics.flush();var sprite=visual.sprite();var pose=graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);RenderSystem.setShaderTexture(0,sprite.atlasLocation());
        var vertices=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(int tile=0;tile<6;tile++){
            int top=y+tile*8,bottom=top+8;
            vertices.addVertex(pose,x,bottom,0).setUv(sprite.getU1(),sprite.getV1()).setColor(visual.argb());
            vertices.addVertex(pose,x+8,bottom,0).setUv(sprite.getU0(),sprite.getV1()).setColor(visual.argb());
            vertices.addVertex(pose,x+8,top,0).setUv(sprite.getU0(),sprite.getV0()).setColor(visual.argb());
            vertices.addVertex(pose,x,top,0).setUv(sprite.getU1(),sprite.getV0()).setColor(visual.argb());
        }
        BufferUploader.drawWithShader(vertices.buildOrThrow());
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        super.render(graphics,mouseX,mouseY,delta);renderTooltip(graphics,mouseX,mouseY);
        if(isHovering(104,10,10,55,mouseX,mouseY)){
            FluidStack fluid=menu.fluid();if(!fluid.isEmpty())graphics.renderComponentTooltip(font,List.of(fluid.getName(),Component.literal(menu.getFluidAmountMb()+" mb")),mouseX,mouseY);
        }
        if(isHovering(88,34,10,10,mouseX,mouseY))graphics.renderTooltip(font,Component.translatable(menu.getMix()?"text.spa.mix.true":"text.spa.mix.false"),mouseX,mouseY);
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        boolean result=super.mouseClicked(mouseX,mouseY,button);
        // Original checks the8x8 region after the base click, for every mouse
        // button; it does not optimistically toggle the local server-owned bit.
        if(isHovering(89,35,8,8,mouseX,mouseY)&&minecraft!=null&&minecraft.gameMode!=null){
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(CLICK,1,.4F));return true;
        }
        return result;
    }
}
