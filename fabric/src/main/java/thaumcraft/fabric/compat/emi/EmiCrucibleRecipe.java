package thaumcraft.fabric.compat.emi;

import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** A crucible recipe: the catalyst thrown into the essentia the crucible already holds. */
final class EmiCrucibleRecipe extends ThaumcraftEmiRecipe {
    private final EmiIngredient catalyst;
    private final List<AspectEmiStack> aspects;

    EmiCrucibleRecipe(ResourceLocation id, String research, Ingredient catalyst, ItemStack result, Map<String, Integer> aspects) {
        super(ThaumcraftEmiPlugin.CRUCIBLE, id, 118, 0, research);
        this.catalyst = EmiIngredient.of(catalyst);
        this.aspects = aspects(aspects);
        this.inputs = List.of(this.catalyst);
        setAspects(this.aspects);
        this.outputs = List.of(EmiStack.of(result));
        this.height = 30 + rows(this.aspects.size(), 6) * 18 + (hasResearch() ? 11 : 0);
    }

    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(catalyst, 18, 4);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 50, 5);
        widgets.addSlot(outputs.getFirst(), 82, 0).large(true).recipeContext(this);
        int y = addAspects(widgets, aspects, 0, 30, 6);
        addResearch(widgets, y + 2);
    }
}
