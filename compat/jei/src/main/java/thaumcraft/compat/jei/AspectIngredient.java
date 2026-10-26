package thaumcraft.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;

/**
 * An aspect as a JEI ingredient, so a recipe's essentia or vis can be clicked like an item: the
 * uses of Ignis list every arcane, crucible and infusion recipe that needs it, and its recipes
 * show the two aspects that make it. Drawn like the Thaumonomicon pages and the EMI stack: the
 * grey aspect sheet tinted with the aspect colour, the amount in the corner.
 */
final class AspectIngredient {
    /** An amount of one aspect; the index entry has the amount one and shows no number. */
    record Stack(Aspect aspect, long amount) {
        static final Codec<Stack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.xmap(Aspect::getAspect, Aspect::getTag).fieldOf("aspect").forGetter(Stack::aspect),
                Codec.LONG.fieldOf("amount").forGetter(Stack::amount)).apply(instance, Stack::new));
    }

    static final IIngredientType<Stack> TYPE = new IIngredientType<>() {
        @Override public Class<? extends Stack> getIngredientClass() { return Stack.class; }
        @Override public String getUid() { return "thaumcraft:aspect"; }
    };

    private AspectIngredient() {}

    static final class Helper implements IIngredientHelper<Stack> {
        @Override public IIngredientType<Stack> getIngredientType() { return TYPE; }
        @Override public String getDisplayName(Stack stack) { return stack.aspect().getName(); }
        @SuppressWarnings("removal")
        @Override public String getUniqueId(Stack stack, UidContext context) { return "thaumcraft:aspect/" + stack.aspect().getTag(); }
        @Override public Object getUid(Stack stack, UidContext context) { return stack.aspect(); }
        @Override public ResourceLocation getResourceLocation(Stack stack) {
            return ResourceLocation.fromNamespaceAndPath("thaumcraft", stack.aspect().getTag());
        }
        @Override public Stack copyIngredient(Stack stack) { return new Stack(stack.aspect(), stack.amount()); }
        @Override public long getAmount(Stack stack) { return stack.amount(); }
        @Override public Stack copyWithAmount(Stack stack, long amount) { return new Stack(stack.aspect(), amount); }
        @Override public Stack normalizeIngredient(Stack stack) { return new Stack(stack.aspect(), 1); }
        @Override public boolean isValidIngredient(Stack stack) { return stack.aspect() != null && stack.amount() > 0; }
        @Override public Iterable<Integer> getColors(Stack stack) { return List.of(stack.aspect().getColor()); }
        @Override public String getDisplayModId(Stack stack) { return "thaumcraft"; }
        @Override public String getErrorInfo(Stack stack) {
            return stack == null ? "null" : (stack.aspect() == null ? "?" : stack.aspect().getTag()) + " x" + stack.amount();
        }
    }

    static final class Renderer implements IIngredientRenderer<Stack> {
        @Override public void render(GuiGraphics graphics, Stack stack) {
            int color = stack.aspect().getColor();
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            try {
                graphics.blit(stack.aspect().getImage(), 0, 0, 0, 0, 16, 16, 16, 16);
            } finally {
                graphics.flush();
                graphics.setColor(1, 1, 1, 1);
                RenderSystem.disableBlend();
            }
            if (stack.amount() <= 1) return;
            var font = Minecraft.getInstance().font;
            String text = Long.toString(stack.amount());
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 200);
                graphics.drawString(font, text, 17 - font.width(text), 9, 0xFFFFFFFF, true);
            } finally {
                graphics.pose().popPose();
            }
        }

        @Override public List<Component> getTooltip(Stack stack, TooltipFlag flag) {
            Aspect aspect = stack.aspect();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(aspect.getName()).withStyle(style -> style.withColor(TextColor.fromRgb(aspect.getColor()))));
            lines.add(Component.literal(aspect.getLocalizedDescription()).withStyle(ChatFormatting.GRAY));
            Aspect[] components = aspect.getComponents();
            if (!aspect.isPrimal() && components != null) {
                lines.add(Component.translatable("tc.port.emi.aspect_components",
                        Component.literal(components[0].getName()), Component.literal(components[1].getName())).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.translatable("tc.port.emi.aspect_primal").withStyle(ChatFormatting.DARK_GRAY));
            }
            if (stack.amount() > 1) lines.add(Component.translatable("tc.port.emi.aspect_amount", stack.amount()).withStyle(ChatFormatting.GRAY));
            return lines;
        }
    }
}
