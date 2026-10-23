package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.lib.AdvancedFurnaceRules;

/** The actual two-layer, seventeen-cell wand formation, anchored to its construct recipe. */
public final class AdvancedFurnaceBookDiagram {
    private AdvancedFurnaceBookDiagram() {}
    public static ThaumcraftRecipeDisplay.Hover render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        float scale = Math.min(width / 160F, height / 174F);
        if (scale <= 0) return null;
        float ox=x+(width-160*scale)/2, oy=y+(height-174*scale)/2;
        float mx=(mouseX-ox)/scale, my=(mouseY-oy)/scale;
        ThaumcraftRecipeDisplay.Hover hover=null;
        graphics.pose().pushPose(); try {graphics.pose().translate(ox,oy,0);graphics.pose().scale(scale,scale,1);
            Component title=Component.translatable("recipe.type.construct");
            graphics.drawString(font,title,Math.max(0,(160-font.width(title))/2),0,0x505050,false);
            for(int layer=0;layer<2;layer++) {
                int left=layer==0?10:94;
                graphics.drawString(font,Component.translatable("tc.port.advanced_furnace.layer."+layer),left,24,0x505050,false);
                for(int row=0;row<3;row++)for(int column=0;column<3;column++) {
                    int part=AdvancedFurnaceRules.part(column-1,layer,row-1),sx=left+column*18,sy=42+row*18;
                    graphics.fill(sx-1,sy-1,sx+17,sy+17,0x22634f36);
                    ItemStack stack=part<0?ItemStack.EMPTY:new ItemStack(layer==0
                            ? part==0?ThaumcraftDistillation.ALCHEMY_FURNACE_ITEM.get():ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT_ITEM.get()
                            : part==2?ThaumcraftAlembics.ALEMBIC_ITEM.get():ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get());
                    if(!stack.isEmpty())graphics.renderItem(stack,sx,sy);
                    if(mx>=sx-1&&mx<sx+17&&my>=sy-1&&my<sy+17)
                        hover=new ThaumcraftRecipeDisplay.Hover(stack,stack.isEmpty()?List.of(Component.translatable("tc.port.advanced_furnace.free_center")):List.of());
                }
            }
            int index=0;
            for(String tag:List.of("ignis","aqua","ordo")) {
                Aspect aspect=Aspect.getAspect(tag);int sx=42+index++*26,rgb=aspect.getColor();
                RenderSystem.enableBlend();graphics.setColor((rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F,1);
                graphics.blit(aspect.getImage(),sx,121,0,0,16,16,16,16);graphics.setColor(1,1,1,1);RenderSystem.disableBlend();
                String amount=Integer.toString(AdvancedFurnaceRules.FORM_VIS.get(tag));
                graphics.drawString(font,amount,sx+(16-font.width(amount))/2,141,0x43301f,false);
                if(mx>=sx&&mx<sx+16&&my>=121&&my<151)hover=new ThaumcraftRecipeDisplay.Hover(ItemStack.EMPTY,List.of(Component.literal(aspect.getName())));
            }
        } finally {
            graphics.pose().popPose();
        }return hover;
    }
}
