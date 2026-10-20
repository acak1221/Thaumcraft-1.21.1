package thaumcraft.common.lib;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneShapedRecipe;

/** Original EventHandlerWorld163–167 is a recipe-specific CENTER retention, not a global item remainder. */
public final class AdvancedFurnaceCrafting {
    private static final ResourceLocation RECIPE=ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/advanced_alchemical_construct");
    private AdvancedFurnaceCrafting() {}
    public static boolean retainCenterPearl(ArcaneCrafting.Match match,int slot) {
        return slot==4 && match.arcane() && match.recipe().id().equals(RECIPE) && match.recipe().value() instanceof ArcaneShapedRecipe
                && match.output().is(ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT_ITEM.get()) && match.output().getCount()==4
                && match.input().getItem(4).is(ThaumcraftContent.PRIMORDIAL_PEARL.get()) && !match.input().getItem(4).isEmpty();
    }
}
