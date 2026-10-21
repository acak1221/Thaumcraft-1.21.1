package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.ResearchDefinition;

/**
 * The original TC4 research entry.
 *
 * <p>The native port keeps the original corpus in {@link ResearchDefinition}; this class is the public view
 * of it and keeps the original builder methods, so an add-on may still register its own research.</p>
 */
public class ResearchItem {
    public final String key;
    public final String category;
    public final AspectList tags;
    public String[] parents = null;
    public String[] parentsHidden = null;
    public String[] siblings = null;
    public final int displayColumn;
    public final int displayRow;
    public final ItemStack icon_item;
    public final ResourceLocation icon_resource;
    private int complexity;
    private boolean isSpecial;
    private boolean isSecondary;
    private boolean isRound;
    private boolean isStub;
    private boolean isVirtual;
    private boolean isConcealed;
    private boolean isHidden;
    private boolean isLost;
    private boolean isAutoUnlock;
    private ItemStack[] itemTriggers;
    private String[] entityTriggers;
    private Aspect[] aspectTriggers;
    private ResearchPage[] pages = null;
    private ResearchDefinition definition;

    public ResearchItem(String key, String category) {
        this.key = key;
        this.category = category;
        this.tags = new AspectList();
        this.icon_resource = null;
        this.icon_item = null;
        this.displayColumn = 0;
        this.displayRow = 0;
        this.setVirtual();
    }

    public ResearchItem(String key, String category, AspectList tags, int col, int row, int complex, ResourceLocation icon) {
        this.key = key;
        this.category = category;
        this.tags = tags;
        this.icon_resource = icon;
        this.icon_item = null;
        this.displayColumn = col;
        this.displayRow = row;
        this.complexity = complex;
        if (this.complexity < 1) this.complexity = 1;
        if (this.complexity > 3) this.complexity = 3;
    }

    public ResearchItem(String key, String category, AspectList tags, int col, int row, int complex, ItemStack icon) {
        this.key = key;
        this.category = category;
        this.tags = tags;
        this.icon_item = icon;
        this.icon_resource = null;
        this.displayColumn = col;
        this.displayRow = row;
        this.complexity = complex;
        if (this.complexity < 1) this.complexity = 1;
        if (this.complexity > 3) this.complexity = 3;
    }

    /** The public view of an original corpus entry. */
    public static ResearchItem of(ResearchDefinition definition) {
        AspectList tags = new AspectList();
        for (var cost : definition.aspectCost().entrySet()) {
            Aspect aspect = Aspect.getAspect(cost.getKey());
            if (aspect != null) tags.add(aspect, cost.getValue());
        }
        ResearchItem item = new ResearchItem(definition.key(), definition.category(), tags, definition.column(),
                definition.row(), definition.complexity(), (ResourceLocation) null);
        item.definition = definition;
        item.complexity = Math.min(3, Math.max(1, definition.complexity()));
        item.isVirtual = definition.isVirtual();
        item.isHidden = definition.isHidden();
        item.isLost = definition.isLost();
        item.isSecondary = definition.isSecondary();
        item.isAutoUnlock = definition.isAutoUnlock();
        item.isConcealed = definition.flags().contains("Concealed");
        item.isStub = definition.flags().contains("Stub");
        item.isSpecial = definition.flags().contains("Special");
        item.isRound = definition.flags().contains("Round");
        item.parents = definition.parents().toArray(String[]::new);
        item.parentsHidden = definition.parentsHidden().toArray(String[]::new);
        item.siblings = definition.siblings().toArray(String[]::new);
        // The original corpus carried the executable aspect triggers of an entry; the
        // public accessor has to answer them, not an empty array.
        if (!definition.aspectTriggers().isEmpty()) {
            java.util.List<Aspect> triggers = new java.util.ArrayList<>();
            for (String tag : definition.aspectTriggers()) {
                Aspect aspect = Aspect.getAspect(tag);
                if (aspect != null) triggers.add(aspect);
            }
            if (!triggers.isEmpty()) item.aspectTriggers = triggers.toArray(new Aspect[0]);
        }
        if (!definition.entityTriggers().isEmpty()) {
            item.entityTriggers = definition.entityTriggers().toArray(String[]::new);
        }
        item.pages = new ResearchPage[definition.pages().size()];
        for (int index = 0; index < definition.pages().size(); index++) {
            item.pages[index] = ResearchPage.of(definition.pages().get(index));
        }
        return item;
    }

    /** The original corpus entry behind this view, or null for an add-on registration. */
    public ResearchDefinition definition() { return this.definition; }

    public ResearchItem setSpecial() { this.isSpecial = true; return this; }

    public ResearchItem setStub() { this.isStub = true; return this; }

    public ResearchItem setLost() { this.isLost = true; return this; }

    public ResearchItem setConcealed() { this.isConcealed = true; return this; }

    public ResearchItem setHidden() { this.isHidden = true; return this; }

    public ResearchItem setVirtual() { this.isVirtual = true; return this; }

    public ResearchItem setParents(String... par) { this.parents = par; return this; }

    public ResearchItem setParentsHidden(String... par) { this.parentsHidden = par; return this; }

    public ResearchItem setSiblings(String... sib) { this.siblings = sib; return this; }

    public ResearchItem setPages(ResearchPage... par) { this.pages = par; return this; }

    public ResearchPage[] getPages() { return this.pages; }

    public ResearchItem setItemTriggers(ItemStack... par) { this.itemTriggers = par; return this; }

    public ResearchItem setEntityTriggers(String... par) { this.entityTriggers = par; return this; }

    public ResearchItem setAspectTriggers(Aspect... par) { this.aspectTriggers = par; return this; }

    /**
     * The original item triggers of the entry.
     *
     * <p>Forced 1.21.1 change: the original registered the item triggers of a corpus entry with
     * {@code setItemTriggers}. The native corpus keeps the provenance of the original expressions
     * and the native identity of the bound items in the common layer, so the public accessor
     * resolves the bound items of the entry on demand. An entry without an item trigger answers
     * {@code null}, exactly like the original.</p>
     */
    public ItemStack[] getItemTriggers() {
        if (this.itemTriggers == null && this.definition != null) {
            // The native identity of a mod item only exists once the mod registered it,
            // so the bound stacks are resolved on every call and never cached half way.
            ItemStack[] bound = thaumcraft.common.lib.research.ResearchTriggerBindings.boundStacks(this.definition.key());
            if (bound.length > 0) return bound;
        }
        return this.itemTriggers;
    }

    public String[] getEntityTriggers() { return this.entityTriggers; }

    public Aspect[] getAspectTriggers() { return this.aspectTriggers; }

    public ResearchItem registerResearchItem() { ResearchCategories.addResearch(this); return this; }

    public String getName() { return Component.translatable("tc.research_name." + this.key).getString(); }

    public String getText() { return Component.translatable("tc.research_text." + this.key).getString(); }

    public boolean isSpecial() { return this.isSpecial; }

    public boolean isStub() { return this.isStub; }

    public boolean isLost() { return this.isLost; }

    public boolean isConcealed() { return this.isConcealed; }

    public boolean isHidden() { return this.isHidden; }

    public boolean isVirtual() { return this.isVirtual; }

    public boolean isAutoUnlock() { return this.isAutoUnlock; }

    public ResearchItem setAutoUnlock() { this.isAutoUnlock = true; return this; }

    public boolean isRound() { return this.isRound; }

    public ResearchItem setRound() { this.isRound = true; return this; }

    public boolean isSecondary() { return this.isSecondary; }

    public ResearchItem setSecondary() { this.isSecondary = true; return this; }

    public int getComplexity() { return this.complexity; }

    public ResearchItem setComplexity(int complexity) { this.complexity = complexity; return this; }

    /** The aspect with the highest amount; the original returned null for an empty tag list. */
    public Aspect getResearchPrimaryTag() {
        Aspect aspect = null;
        int highest = 0;
        if (this.tags != null) {
            for (Aspect tag : this.tags.getAspects()) {
                if (this.tags.getAmount(tag) <= highest) continue;
                aspect = tag;
                highest = this.tags.getAmount(tag);
            }
        }
        return aspect;
    }
}
