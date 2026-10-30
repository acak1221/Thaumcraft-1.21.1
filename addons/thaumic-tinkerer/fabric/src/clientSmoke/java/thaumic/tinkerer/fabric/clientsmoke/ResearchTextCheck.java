package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.fabric.clientsmoke.Translations.Needed;

/**
 * The research tab of the add-on and every research entry it registered: the tab's tooltip, each entry's name and summary
 * ({@code tc.research_name.<KEY>}, {@code tc.research_text.<KEY>}), the text of its pages, and the icon and image files.
 * Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The entries are read from the Thaumcraft API registry ({@link ResearchCategories}) after the add-on registered them,
 * so a research that was dropped at registration is not here to be checked: that loss is the add-on's own
 * {@code ThaumicTinkerer.failures()} and the parity report.</p>
 */
final class ResearchTextCheck implements SmokeCheck {
    private static final String GROUP = "Research";

    @Override
    public String name() {
        return "research_text";
    }

    @Override
    public void run(SmokeRun run) {
        ResearchCategoryList category = ResearchCategories.getResearchList(LibResearch.CATEGORY_THAUMICTINKERER);
        if (category == null) {
            run.report().fail(PHASE, "research_tab_missing", GROUP, LibResearch.CATEGORY_THAUMICTINKERER,
                    "the Thaumic Tinkerer research tab is not registered");
            return;
        }
        ModelFiles files = new ModelFiles(run.client().getResourceManager());
        List<Needed> tab = List.of(new Needed("tc.research_category." + LibResearch.CATEGORY_THAUMICTINKERER, GROUP, "research_tab"));
        Translations.verify(run, "research_tab", tab);
        checkFile(run, files, "research_tab_icon", category.icon);
        checkFile(run, files, "research_tab_background", category.background);

        List<Needed> names = new ArrayList<>();
        List<Needed> texts = new ArrayList<>();
        List<Needed> pages = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (ResearchItem research : category.research.values()) {
            keys.add(research.key);
            String target = "research:" + research.key;
            names.add(new Needed("tc.research_name." + research.key, GROUP, target));
            texts.add(new Needed("tc.research_text." + research.key, GROUP, target));
            if (research.icon_resource != null) checkFile(run, files, target + " icon", research.icon_resource);
            ResearchPage[] entryPages = research.getPages();
            if (entryPages == null) continue;
            for (ResearchPage page : entryPages) {
                if ((page.type == ResearchPage.PageType.TEXT || page.type == ResearchPage.PageType.TEXT_CONCEALED) && page.text != null) {
                    pages.add(new Needed(page.text, GROUP, target));
                }
                if (page.type == ResearchPage.PageType.IMAGE && page.image != null) checkFile(run, files, target + " image", page.image);
            }
        }
        Translations.verify(run, "research_names", names);
        Translations.verify(run, "research_texts", texts);
        Translations.verify(run, "research_pages", pages);
        run.report().inventory("research_keys", keys);
        run.report().info(PHASE, name(), keys.size() + " research entries, " + pages.size() + " text pages of the Thaumic Tinkerer tab");
    }

    private static void checkFile(SmokeRun run, ModelFiles files, String what, ResourceLocation file) {
        if (file == null) return;
        if (files.exists(file)) run.report().pass(PHASE, "research_file", GROUP, what, file + " exists");
        else run.report().fail(PHASE, "missing_texture", GROUP, what, file + " does not exist");
    }
}
