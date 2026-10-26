package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.common.lib.research.ResearchCatalog;

/**
 * The icon of an add-on research entry: its {@code icon_item} or its {@code icon_resource}.
 *
 * <p>Original GuiResearchBrowser draws {@code research.icon_item} as an item, or binds
 * {@code research.icon_resource} and draws it over the whole sixteen pixel cell. An entry of the original corpus
 * keeps its own icon expression; this only answers for add-on entries.</p>
 */
final class AddonResearchIcons {
    private AddonResearchIcons() {}

    /** False when the key is an original entry or the add-on entry has no icon. */
    static boolean render(GuiGraphics graphics, String key, int x, int y) {
        if (key == null || ResearchCatalog.isBundled(key)) return false;
        ResearchItem item = ResearchCategories.getResearch(key);
        if (item == null) return false;
        if (item.icon_item != null && !item.icon_item.isEmpty()) {
            // The same depth handling as ResearchIconDisplay: items are drawn with depth, the book is flat.
            graphics.flush();
            graphics.renderItem(item.icon_item, x, y);
            graphics.flush();
            RenderSystem.disableDepthTest();
            return true;
        }
        if (item.icon_resource != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(item.icon_resource, x, y, 16, 16, 0, 0, 16, 16, 16, 16);
            return true;
        }
        return false;
    }
}
