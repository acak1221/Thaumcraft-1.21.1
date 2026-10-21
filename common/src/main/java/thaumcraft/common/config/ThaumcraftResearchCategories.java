package thaumcraft.common.config;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.research.ResearchCategoryList;

/**
 * The original research tab registration.
 *
 * <p>Original {@code ConfigResearch.initCategories:67-73} registers the six research categories with the
 * texture of their tab icon and the texture of their background. The research corpus keeps the per entry
 * icon ({@code icon_expression}) but not the per category icon, so the original registration is kept
 * here verbatim; the browser draws the icon of a tab from this table.</p>
 */
public final class ThaumcraftResearchCategories {
    private ThaumcraftResearchCategories() {}

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    /**
     * The original category with its icon and background; an unknown category has neither, exactly like
     * a category an add-on registered without them.
     */
    public static ResearchCategoryList create(String category) {
        if (category == null) return new ResearchCategoryList(null, null);
        return switch (category) {
            case "BASICS" -> new ResearchCategoryList(texture("textures/items/thaumonomiconcheat.png"),
                    texture("textures/gui/gui_researchback.png"));
            case "THAUMATURGY" -> new ResearchCategoryList(texture("textures/misc/r_thaumaturgy.png"),
                    texture("textures/gui/gui_researchback.png"));
            case "ALCHEMY" -> new ResearchCategoryList(texture("textures/misc/r_crucible.png"),
                    texture("textures/gui/gui_researchback.png"));
            case "ARTIFICE" -> new ResearchCategoryList(texture("textures/misc/r_artifice.png"),
                    texture("textures/gui/gui_researchback.png"));
            case "GOLEMANCY" -> new ResearchCategoryList(texture("textures/misc/r_golemancy.png"),
                    texture("textures/gui/gui_researchback.png"));
            case "ELDRITCH" -> new ResearchCategoryList(texture("textures/misc/r_eldritch.png"),
                    texture("textures/gui/gui_researchbackeldritch.png"));
            default -> new ResearchCategoryList(null, null);
        };
    }
}
