package thaumcraft.api.research;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * A research category with the original knowledge formula.
 *
 * <p>The formula is a weighted aspect list: every aspect of the formula multiplies the amount the player
 * holds of that aspect, and the square root of the total is the knowledge the category reports.</p>
 */
public class ResearchCategory extends ResearchCategoryList {
    public ResourceLocation background2;
    public String researchKey;
    public String key;
    public AspectList formula;

    public ResearchCategory(String key, String researchKey, AspectList formula,
                            ResourceLocation icon, ResourceLocation background) {
        this(key, researchKey, formula, icon, background, null);
    }

    public ResearchCategory(String key, String researchKey, AspectList formula,
                            ResourceLocation icon, ResourceLocation background,
                            ResourceLocation background2) {
        super(icon, background);
        this.key = key;
        this.researchKey = researchKey;
        this.formula = formula;
        this.background2 = background2;
    }

    /** The original formula: the weighted total, square rooted and rounded up. */
    public int applyFormula(AspectList input) {
        return applyFormula(input, 1.0D);
    }

    public int applyFormula(AspectList input, double multiplier) {
        if (formula == null || input == null) {
            return 0;
        }
        double total = 0.0D;
        for (Aspect aspect : formula.getAspects()) {
            total += multiplier * multiplier * input.getAmount(aspect) * formula.getAmount(aspect) / 10.0D;
        }
        return (int) Math.ceil(total > 0.0D ? Math.sqrt(total) : total);
    }
}
