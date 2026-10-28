/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): construction helpers of the Kami research
 * entries. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.research.KamiResearchItem;

/**
 * The 1.7.10 code chained {@code new KamiResearchItem(...).setWarp(..).setConcealed().setParents(..)...setPages(..)} and
 * cast in between because the setters return different types. The helpers here do the same in statements.
 *
 * <p>A Kami entry is concealed by its constructor, requires every other entry once registered (see
 * {@link KamiResearchItem#registerResearch()}), and is queued for the last registration pass by the registrar.</p>
 */
public final class KamiResearch {
    public static final String[] NONE = new String[0];

    private KamiResearch() {}

    /** A complexity-5 Kami entry with the given parents; the pages are set last, as in 1.7.10. */
    public static KamiResearchItem entry(String key, AspectList tags, int column, int row, ItemStack icon, int warp,
            String[] parents, String[] hiddenParents, ResearchPage... pages) {
        KamiResearchItem research = new KamiResearchItem(key, tags, column, row, 5, icon);
        if (warp != 0) research.setWarp(warp);
        if (parents.length > 0) research.setParents(parents);
        if (hiddenParents.length > 0) research.setParentsHidden(hiddenParents);
        research.setPages(pages);
        return research;
    }

    public static String[] of(String... keys) {
        return keys;
    }

    /** {@code ItemApi.getItem(field, meta)}: a stack of the Thaumcraft item the 1.7.10 field and metadata named. */
    public static ItemStack tc(String field, int meta) {
        return Objects.requireNonNull(ItemApi.getItem(field, meta), "Thaumcraft item " + field + ":" + meta);
    }

    /** {@code ItemApi.getBlock(field, meta)}: the block item of a Thaumcraft block named by its 1.7.10 field and metadata. */
    public static ItemStack tcBlock(String field, int meta) {
        return Objects.requireNonNull(ItemApi.getBlock(field, meta), "Thaumcraft block " + field + ":" + meta);
    }
}
