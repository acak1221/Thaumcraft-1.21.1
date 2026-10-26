package thaumcraft.compat.emi;

import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * An infusion: the central item on the matrix pedestal, the components on the surrounding
 * pedestals, and the essentia drawn during the ritual.
 *
 * <p>The ring follows {@code GuiResearchRecipe}: components start straight above the centre
 * and step clockwise by {@code 360 / count} degrees, an integer division as in the original.
 * The essentia sits beside the ring, under the result, so the page stays short enough for small
 * windows. Enchantment infusions name the enchantment and its experience cost under the essentia.</p>
 */
final class EmiInfusionRecipe extends ThaumcraftEmiRecipe {
    private static final int ASPECTS_PER_ROW = 4;
    private final EmiIngredient central;
    private final List<EmiIngredient> components;
    private final List<AspectEmiStack> aspects;
    private final int instability;
    private final List<Component> details;
    private final int radius, ring;

    EmiInfusionRecipe(ResourceLocation id, String research, EmiIngredient central, List<EmiIngredient> components,
                      ItemStack result, Map<String, Integer> aspects, int instability, List<Component> details, boolean enchantment) {
        super(enchantment ? ThaumcraftEmiPlugin.INFUSION_ENCHANTMENT : ThaumcraftEmiPlugin.INFUSION, id, 0, 0, research);
        this.central = central;
        this.components = List.copyOf(components);
        this.aspects = aspects(aspects);
        this.instability = instability;
        this.details = List.copyOf(details);
        this.radius = this.components.size() <= 8 ? 28 : 36;
        this.ring = (radius + 10) * 2;
        List<EmiIngredient> items = new ArrayList<>();
        items.add(central);
        items.addAll(this.components);
        this.inputs = items;
        setAspects(this.aspects);
        this.outputs = List.of(EmiStack.of(result));
        this.width = ring + 4 + ASPECTS_PER_ROW * 18;
        this.height = body() + 2 + 10 + (hasResearch() ? 11 : 0);
    }

    private int detailTop() { return 30 + rows(aspects.size(), ASPECTS_PER_ROW) * 18 + 2; }

    /** The ring, or the result, essentia and detail column beside it when that is taller. */
    private int body() { return Math.max(ring, detailTop() + details.size() * 10); }

    @Override public void addWidgets(WidgetHolder widgets) {
        int center = radius + 10;
        widgets.addSlot(central, center - 9, center - 9);
        int count = components.size();
        int slice = count == 0 ? 0 : 360 / count;
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(-90 + i * slice);
            int x = center + (int) Math.round(Math.cos(angle) * radius) - 9;
            int y = center + (int) Math.round(Math.sin(angle) * radius) - 9;
            widgets.addSlot(components.get(i), x, y);
        }
        int right = ring + 4;
        widgets.addTexture(EmiTexture.EMPTY_ARROW, right, 5);
        widgets.addSlot(outputs.getFirst(), right + 28, 0).large(true).recipeContext(this);
        addAspects(widgets, aspects, right, 30, ASPECTS_PER_ROW);
        int line = detailTop();
        for (Component detail : details) {
            String text = trim(detail.getString(), width - right);
            widgets.addText(Component.literal(text), right, line, TEXT, false);
            if (!text.equals(detail.getString())) widgets.addTooltipText(List.of(detail), right, line, width - right, 10);
            line += 10;
        }
        int y = body() + 2;
        Component stability = Component.translatable("tc.inst").append(" ")
                .append(Component.translatable("tc.inst." + Math.min(5, instability / 2)));
        widgets.addText(stability, 0, y, TEXT, false);
        addResearch(widgets, y + 11);
    }
}
