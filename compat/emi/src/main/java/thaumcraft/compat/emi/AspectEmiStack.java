package thaumcraft.compat.emi;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiStackSerializer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

/**
 * An aspect as an EMI stack, so a recipe's essentia or vis cost can be clicked like an item:
 * "uses" of Ignis lists every arcane, crucible and infusion recipe that needs it.
 *
 * <p>Drawn like the Thaumonomicon recipe pages: the grey aspect sheet tinted with the aspect
 * colour under the ordinary alpha blend, the amount in the corner.</p>
 */
public final class AspectEmiStack extends EmiStack {
    private final Aspect aspect;
    /** A recipe cost of one still reads "1"; the index entry stays unnumbered. */
    private final boolean cost;

    public AspectEmiStack(Aspect aspect, long amount) { this(aspect, amount, false); }

    private AspectEmiStack(Aspect aspect, long amount, boolean cost) {
        this.aspect = aspect;
        this.amount = amount;
        this.cost = cost;
    }

    public static AspectEmiStack cost(Aspect aspect, long amount) { return new AspectEmiStack(aspect, amount, true); }

    public Aspect aspect() { return aspect; }

    @Override public EmiStack copy() {
        AspectEmiStack copy = new AspectEmiStack(aspect, amount, cost);
        copy.setChance(getChance());
        copy.setRemainder(getRemainder().copy());
        copy.comparison = comparison;
        return copy;
    }

    @Override public boolean isEmpty() { return aspect == null || amount <= 0; }
    @Override public DataComponentPatch getComponentChanges() { return DataComponentPatch.EMPTY; }
    @Override public Object getKey() { return aspect; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("thaumcraft", aspect.getTag()); }
    @Override public Component getName() { return Component.literal(aspect.getName()).withStyle(style -> style.withColor(TextColor.fromRgb(aspect.getColor()))); }

    @Override public List<Component> getTooltipText() {
        List<Component> lines = new ArrayList<>();
        lines.add(getName());
        lines.add(Component.literal(aspect.getLocalizedDescription()).withStyle(ChatFormatting.GRAY));
        Aspect[] components = aspect.getComponents();
        if (!aspect.isPrimal() && components != null) {
            lines.add(Component.translatable("tc.port.emi.aspect_components",
                    Component.literal(components[0].getName()), Component.literal(components[1].getName())).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.translatable("tc.port.emi.aspect_primal").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    /** EMI's base stack adds only the remainder line; the text lines are each stack type's own. */
    @Override public List<ClientTooltipComponent> getTooltip() {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        for (Component line : getTooltipText()) tooltip.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        if (cost) tooltip.add(ClientTooltipComponent.create(Component.translatable("tc.port.emi.aspect_amount", amount)
                .withStyle(ChatFormatting.GRAY).getVisualOrderText()));
        EmiTooltipComponents.appendModName(tooltip, "thaumcraft");
        tooltip.addAll(super.getTooltip());
        return tooltip;
    }

    @Override public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        if (isEmpty()) return;
        if ((flags & RENDER_ICON) != 0) drawIcon(graphics, aspect, x, y);
        if ((flags & RENDER_AMOUNT) != 0 && (cost || amount != 1)) drawAmount(graphics, Long.toString(amount), x, y);
    }

    static void drawIcon(GuiGraphics graphics, Aspect aspect, int x, int y) {
        int color = aspect.getColor();
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        try {
            graphics.blit(aspect.getImage(), x, y, 0, 0, 16, 16, 16, 16);
        } finally {
            graphics.flush();
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }

    static void drawAmount(GuiGraphics graphics, String text, int x, int y) {
        var font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 200);
            graphics.drawString(font, text, x + 17 - font.width(text), y + 9, 0xFFFFFFFF, true);
        } finally {
            graphics.pose().popPose();
        }
    }

    /** Favourites and the recipe tree persist aspects as {@code thaumcraft_aspect:thaumcraft:<tag>}. */
    public static final class Serializer implements EmiStackSerializer<AspectEmiStack> {
        @Override public String getType() { return "thaumcraft_aspect"; }

        @Override public EmiStack create(ResourceLocation id, DataComponentPatch components, long amount) {
            Aspect aspect = Aspect.getAspect(id.getPath());
            return aspect == null ? EmiStack.EMPTY : new AspectEmiStack(aspect, amount);
        }
    }
}
