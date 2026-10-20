package thaumcraft.api.research;

import net.minecraft.resources.ResourceLocation;
import java.util.Arrays;
import thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType;

/**
 * One stage of a research entry: the text, the recipe and the knowledge the stage pays.
 */
public class ResearchStage {
    private String text;
    private ResourceLocation[] recipes;
    private Object[] obtain;
    private Object[] craft;
    private int[] craftReference;
    private Knowledge[] know;
    private String[] research;
    private String[] researchIcon;
    private int warp;

    /** One knowledge reward of a stage. */
    public static class Knowledge {
        public EnumKnowledgeType type;
        public ResearchCategory category;
        public int amount;

        public Knowledge(EnumKnowledgeType type, ResearchCategory category, int amount) {
            this.type = type;
            this.category = category;
            this.amount = amount;
        }
    }

    public String getText() { return text; }

    public String getTextLocalized() {
        return text == null ? null : net.minecraft.network.chat.Component.translatable(text).getString();
    }

    public void setText(String text) { this.text = text; }

    public ResourceLocation[] getRecipes() { return recipes; }

    public void setRecipes(ResourceLocation[] recipes) { this.recipes = recipes; }

    public Object[] getObtain() { return obtain; }

    public void setObtain(Object[] obtain) { this.obtain = obtain; }

    public Object[] getCraft() { return craft; }

    public void setCraft(Object[] craft) { this.craft = craft; }

    public int[] getCraftReference() { return craftReference; }

    public void setCraftReference(int[] craftReference) { this.craftReference = craftReference; }

    public Knowledge[] getKnow() { return know; }

    public void setKnow(Knowledge[] know) { this.know = know; }

    public String[] getResearch() { return research; }

    public void setResearch(String[] research) { this.research = research; }

    public String[] getResearchIcon() { return researchIcon; }

    public void setResearchIcon(String[] researchIcon) { this.researchIcon = researchIcon; }

    public int getWarp() { return warp; }

    public void setWarp(int warp) { this.warp = warp; }

    /** The knowledge rewards of this stage. */
    public Knowledge[] knowledge() { return know == null ? new Knowledge[0] : Arrays.copyOf(know, know.length); }
}
