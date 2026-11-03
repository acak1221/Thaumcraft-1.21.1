package thaumcraft.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

/**
 * An aspect lookup with a live, paged grid of scanned items and their amount per item.
 * EMI indexes the aspect alone: undiscovered source items never enter recipe metadata.
 */
final class EmiAspectSourcesRecipe extends BasicEmiRecipe {
    private static final int COLUMNS = 8;
    private final Aspect aspect;
    private final AspectSources sources;

    record Source(ItemStack item, int amount, EmiStack stack) {
        Source(ItemStack item, int amount) { this(item, amount, EmiStack.of(item)); }
    }

    EmiAspectSourcesRecipe(Aspect aspect, AspectSources sources) {
        super(ThaumcraftEmiPlugin.ASPECT_SOURCES,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "/aspect_sources/" + aspect.getTag()), 144, 116);
        this.aspect = aspect;
        this.sources = sources;
        outputs = List.of(new AspectEmiStack(aspect, 1));
    }

    Aspect aspect() { return aspect; }
    List<Source> sources() { return sources.forAspect(aspect); }

    @Override public boolean supportsRecipeTree() { return false; }
    @Override public boolean hideCraftable() { return true; }

    @Override public void addWidgets(WidgetHolder widgets) {
        int rows = Math.max(1, Math.min(3, (widgets.getHeight() - 35) / 27));
        int footer = 23 + rows * 27;
        View view = new View(COLUMNS * rows);
        widgets.addSlot(outputs.getFirst(), 0, 0).drawBack(false);
        widgets.addText(Component.translatable("jei.thaumcraft.aspect_sources.contains"), 22, 4, ThaumcraftEmiRecipe.TEXT, false);
        for (int i = 0; i < view.pageSize; i++) {
            int offset = i;
            var slot = new SlotWidget(EmiStack.EMPTY, i % COLUMNS * 18, 22 + i / COLUMNS * 27) {
                @Override public EmiIngredient getStack() {
                    return view.stack(offset);
                }

                @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                    if (!getStack().isEmpty()) super.render(graphics, mouseX, mouseY, delta);
                }
            };
            slot.appendTooltip(() -> {
                Source source = view.source(offset);
                return ClientTooltipComponent.create(Component.translatable("tc.port.emi.aspect_sources.amount",
                        source == null ? 0 : source.amount()).getVisualOrderText());
            });
            widgets.add(slot);
        }
        widgets.addDrawable(0, 22, 144, rows * 27, (graphics, mouseX, mouseY, delta) -> {
            var font = Minecraft.getInstance().font;
            if (view.items().isEmpty()) {
                graphics.drawCenteredString(font, ThaumcraftEmiRecipe.trim(
                        Component.translatable("tc.port.emi.aspect_sources.empty").getString(), 140), 72, 8, ThaumcraftEmiRecipe.TEXT);
            }
            for (int i = 0; i < view.pageSize; i++) {
                Source source = view.source(i);
                if (source != null) graphics.drawCenteredString(font, Integer.toString(source.amount()),
                        i % COLUMNS * 18 + 9, i / COLUMNS * 27 + 18, ThaumcraftEmiRecipe.TEXT);
            }
        });
        widgets.addButton(0, footer, 12, 12, 0, 0, () -> view.page > 0, (x, y, button) -> view.scroll(-1));
        widgets.addButton(132, footer, 12, 12, 12, 0, () -> view.page + 1 < view.pages(), (x, y, button) -> view.scroll(1));
        widgets.addDrawable(16, footer + 2, 112, 9, (graphics, mouseX, mouseY, delta) ->
                graphics.drawCenteredString(Minecraft.getInstance().font, (view.page + 1) + " / " + view.pages(), 56, 0, ThaumcraftEmiRecipe.TEXT));
    }

    /** Read shared results directly; changing pages and drawing slots never resolves object aspects. */
    private final class View {
        private final int pageSize;
        private List<Source> items = List.of();
        private int page;

        private View(int pageSize) { this.pageSize = pageSize; }

        private List<Source> items() {
            List<Source> current = sources();
            if (items != current) {
                items = current;
                page = Math.min(page, Math.max(0, (items.size() - 1) / pageSize));
            }
            return items;
        }

        private int pages() { return Math.max(1, (items().size() + pageSize - 1) / pageSize); }

        private void scroll(int delta) { page = Math.clamp(page + delta, 0, pages() - 1); }

        private EmiStack stack(int offset) {
            Source source = source(offset);
            return source == null ? EmiStack.EMPTY : source.stack();
        }

        private Source source(int offset) {
            List<Source> current = items();
            int index = page * pageSize + offset;
            if (index >= current.size()) return null;
            Source source = current.get(index);
            // Knowledge reset/revocation must hide a cached slot immediately, including its tooltip and clicks.
            return AspectSources.scanned(source.item()) ? source : null;
        }
    }
}
