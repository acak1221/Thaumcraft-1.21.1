package thaumcraft.api.research;

import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The legacy research entry view: the public shape an add-on may read for one research key.
 *
     * <p>The native port keeps the original corpus in {@link ResearchItem}; of copies scalar values,
     * aliases parent/sibling arrays and icon stacks, and setters do not write back to the ResearchItem.
     * Stages/addenda/rewards are TC6-style DTO storage, not the TC4 1.7.10 progression model;
     * in 1.21.1 use ResearchItem registration with supported pages/triggers for gameplay progression.</p>
 */
public class ResearchEntry {
    String key;
    String category;
    String name;
    String[] parents;
    String[] siblings;
    int displayColumn;
    int displayRow;
    Object[] icons;
    EnumResearchMeta[] meta;
    ItemStack[] rewardItem;
    ResearchStage.Knowledge[] rewardKnow;
    private ResearchStage[] stages;
    private ResearchAddendum[] addenda;

    public enum EnumResearchMeta {
        ROUND,
        SPIKY,
        REVERSE,
        HIDDEN,
        AUTOUNLOCK,
        HEX
    }

    /** The legacy view of one research entry. */
    public static ResearchEntry of(ResearchItem item) {
        ResearchEntry entry = new ResearchEntry();
        entry.key = item.key;
        entry.category = item.category;
        entry.name = "tc.research_name." + item.key;
        entry.parents = item.parents;
        entry.siblings = item.siblings;
        entry.displayColumn = item.displayColumn;
        entry.displayRow = item.displayRow;
        entry.icons = item.icon_item != null ? new Object[]{item.icon_item} : item.icon_resource != null ? new Object[]{item.icon_resource} : new Object[0];
        entry.meta = new EnumResearchMeta[0];
        return entry;
    }

    public String getKey() { return key; }

    public void setKey(String key) { this.key = key; }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    public String getName() { return name; }

    public String getLocalizedName() {
        return name == null ? null : Component.translatable(name).getString();
    }

    public void setName(String name) { this.name = name; }

    public String[] getParents() { return parents; }

    /** The parents without the original negation prefix. */
    public String[] getParentsClean() {
        if (parents == null) return null;
        return Arrays.stream(parents).filter(parent -> parent != null && !parent.startsWith("!")).toArray(String[]::new);
    }

    /** The parents without the original prefix and without their suffix. */
    public String[] getParentsStripped() {
        if (parents == null) return null;
        return Arrays.stream(parents).map(parent -> parent == null ? null : parent.replaceFirst("^[!~]", "").split("@")[0]).toArray(String[]::new);
    }

    protected final void setParentsValue(String[] parents) { this.parents = parents; }

    public String[] getSiblings() { return siblings; }

    protected final void setSiblingsValue(String[] siblings) { this.siblings = siblings; }

    public int getDisplayColumn() { return displayColumn; }

    public void setDisplayColumn(int displayColumn) { this.displayColumn = displayColumn; }

    public int getDisplayRow() { return displayRow; }

    public void setDisplayRow(int displayRow) { this.displayRow = displayRow; }

    public Object[] getIcons() { return icons; }

    public void setIcons(Object[] icons) { this.icons = icons; }

    public EnumResearchMeta[] getMeta() { return meta; }

    public boolean hasMeta(EnumResearchMeta type) {
        return meta != null && Arrays.asList(meta).contains(type);
    }

    public void setMeta(EnumResearchMeta[] meta) { this.meta = meta; }

    public ResearchStage[] getStages() { return stages; }

    /** Stores a nullable stage array by reference without changing gameplay.
     * @deprecated Use ResearchItem.setPages and supported triggers for TC4 progression; TC6 stages have no native consumer. */
    @Deprecated
    public void setStages(ResearchStage[] stages) { this.stages = stages; }

    public ItemStack[] getRewardItem() { return rewardItem; }

    /** Stores a nullable reward array by reference without granting items.
     * @deprecated Use native TC4 ResearchItem/page contracts; no equivalent automatic TC6 reward grant is wired. */
    @Deprecated
    public void setRewardItem(ItemStack[] rewardItem) { this.rewardItem = rewardItem; }

    public ResearchStage.Knowledge[] getRewardKnow() { return rewardKnow; }

    /** Stores a nullable knowledge-reward array by reference without granting knowledge.
     * @deprecated Use native TC4 ResearchItem/page contracts; no TC6 staged reward flow is wired. */
    @Deprecated
    public void setRewardKnow(ResearchStage.Knowledge[] rewardKnow) { this.rewardKnow = rewardKnow; }

    public ResearchAddendum[] getAddenda() { return addenda; }

    /** Stores a nullable addendum array by reference without changing progression.
     * @deprecated Use ResearchItem.setPages for TC4 pages; no TC6 addendum progression is wired. */
    @Deprecated
    public void setAddenda(ResearchAddendum[] addenda) { this.addenda = addenda; }
}
