package thaumcraft.common.config;

import java.util.ArrayList;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

/** Owner-requested modern branches, registered beside the unchanged original research corpus. */
public final class ThaumcraftModernResearch {
    private static boolean registered;
    private ThaumcraftModernResearch() {}

    public static Set<String> keys() { return Set.of("MODERNINFUSION", "DEEPINFUSION"); }

    public static void register() {
        if (registered) return;
        registered = true;
        new ResearchItem("MODERNINFUSION", "ARTIFICE", new AspectList().add(Aspect.MAGIC, 10)
                .add(Aspect.MIND, 6).add(Aspect.TOOL, 4).add(Aspect.WEAPON, 4).add(Aspect.MOTION, 6),
                -6, 13, 3, new ItemStack(Items.MACE))
                .setParents("INFUSIONENCHANTMENT").setConcealed()
                .setPages(pages("MODERNINFUSION", "loyalty", "impaling", "riptide", "channeling",
                        "multishot", "piercing", "quick_charge", "density", "breach"))
                .registerResearchItem();
        new ResearchItem("DEEPINFUSION", "ARTIFICE", new AspectList().add(Aspect.MAGIC, 16)
                .add(Aspect.ELDRITCH, 8).add(Aspect.MIND, 8).add(Aspect.VOID, 6).add(Aspect.SOUL, 6),
                -6, 15, 3, new ItemStack(Items.HEAVY_CORE))
                .setParents("MODERNINFUSION").setParentsHidden("ELDRITCHMINOR").setConcealed()
                .setPages(pages("DEEPINFUSION", "wind_burst", "soul_speed", "swift_sneak"))
                .registerResearchItem();
    }

    private static ResearchPage[] pages(String key, String... recipes) {
        var pages = new ArrayList<ResearchPage>();
        pages.add(new ResearchPage("tc.research_page." + key + ".1"));
        for (String recipe : recipes) pages.add(ResearchPage.recipe(ResourceLocation.fromNamespaceAndPath(
                "thaumcraft", "infusion_enchantment/modern/" + recipe)));
        return pages.toArray(ResearchPage[]::new);
    }
}
