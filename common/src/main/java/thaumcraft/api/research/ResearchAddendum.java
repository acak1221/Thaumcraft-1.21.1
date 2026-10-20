package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * An addendum of a research entry: extra text with the recipes and research it belongs to.
 */
public class ResearchAddendum {
    private String text;
    private ResourceLocation[] recipes;
    private String[] research;

    public String getText() { return text; }

    public String getTextLocalized() {
        return text == null ? null : Component.translatable(text).getString();
    }

    public void setText(String text) { this.text = text; }

    public ResourceLocation[] getRecipes() { return recipes; }

    public void setRecipes(ResourceLocation[] recipes) { this.recipes = recipes; }

    public String[] getResearch() { return research; }

    public void setResearch(String[] research) { this.research = research; }
}
