package thaumcraft.fabric.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;

/** A compound aspect and the two aspects the research table combines into it. */
final class EmiAspectCombinationRecipe extends BasicEmiRecipe {
    EmiAspectCombinationRecipe(Aspect aspect) {
        super(ThaumcraftEmiPlugin.ASPECT_COMBINATION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "/aspect_combination/" + aspect.getTag()), 104, 18);
        Aspect[] components = aspect.getComponents();
        this.inputs = List.of(new AspectEmiStack(components[0], 1), new AspectEmiStack(components[1], 1));
        this.outputs = List.of(new AspectEmiStack(aspect, 1));
    }

    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 0);
        widgets.addTexture(EmiTexture.PLUS, 21, 4);
        widgets.addSlot(inputs.get(1), 36, 0);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 58, 1);
        widgets.addSlot(outputs.getFirst(), 86, 0).recipeContext(this);
    }
}
