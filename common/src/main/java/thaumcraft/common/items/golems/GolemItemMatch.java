package thaumcraft.common.items.golems;

import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Pure rules of Entropy's three item-comparison switches (original EntityGolemBase:785-805, GuiGolem:214-262, 376-389,
 * ContainerGolem:86, InventoryUtils.areItemStacksEqual:295-323). The original keeps them as three <em>positive</em> bits 5, 6
 * and 7 of the entity's {@code toggles} byte; here they are their own value, so the Guard's inverted bits 1..4 are never
 * reinterpreted.
 *
 * <p>Deliberate, documented deviations (see docs/analysis/2026-10-19-golem-item-match.md):
 * <ul>
 *   <li>1.7 metadata variants are separate items in 1.21, so "ignore damage" ignores the {@code DAMAGE} component and "ignore NBT"
 *       ignores every component (the item alone must match).</li>
 *   <li>"Ore dictionary" shares a <em>group tag</em>: a material-specific {@code c:} convention tag of a material root
 *       ({@code c:ingots/iron}, {@code c:ores/diamond}, {@code c:dyes/red}) or one of vanilla's wood-family tags. The comparison is
 *       symmetric; the original's registration-order "first ore name" has no counterpart. Broad {@code c:} category tags
 *       ({@code c:ingots}) and non-material tags such as {@code c:tools/pickaxe} (which lists every pickaxe) never make two
 *       different items equal.</li>
 * </ul>
 * Menu buttons keep the original ids: 55 tags, 56 damage, 57 components.
 */
public final class GolemItemMatch {
    public static final int TAGS = 1;
    public static final int IGNORE_DAMAGE = 2;
    public static final int IGNORE_COMPONENTS = 4;
    public static final int ALL = TAGS | IGNORE_DAMAGE | IGNORE_COMPONENTS;
    /** Original ContainerGolem button {@code 50 + toggle index}: the first switch (tags) is toggles[5]. */
    public static final int FIRST_BUTTON = 55;
    public static final int BUTTON_COUNT = 3;

    private static final Set<String> WOOD_FAMILIES = Set.of("logs", "planks", "saplings", "leaves", "wooden_slabs", "wooden_stairs");
    /** The {@code c:} roots whose second path segment names a material or colour, the 1.7 ore names ({@code ingotIron}, {@code oreGold}, {@code dyeRed}). */
    private static final Set<String> MATERIAL_ROOTS = Set.of("ingots", "nuggets", "ores", "dusts", "gems", "storage_blocks", "raw_materials",
            "rods", "plates", "gears", "dyes", "crops", "seeds");

    private GolemItemMatch() {}

    public static boolean valid(int flags) { return (flags & ~ALL) == 0; }

    /** The cores whose GUI offers the switches (original {@code ItemGolemCore.canSort}): Fill, Empty, Gather, Use and Sorting. */
    public static boolean appliesTo(int core) {
        return core == GolemData.FILL || core == GolemData.EMPTY_CORE || core == GolemData.GATHER || core == GolemData.USE || core == GolemData.SORTING;
    }

    /** The switches exist only on such a core that carries an Entropy upgrade (original GuiGolem:214). */
    public static boolean configurable(int core, int entropyUpgrades) { return appliesTo(core) && entropyUpgrades > 0; }

    /** A non-zero value is configuration this golem cannot honour without such a core and an Entropy upgrade. */
    public static boolean acceptable(int flags, int core, int entropyUpgrades) {
        return valid(flags) && (flags == 0 || configurable(core, entropyUpgrades));
    }

    public static int toggle(int flags, int flag) {
        return Integer.bitCount(flag) == 1 && (flag & ALL) == flag ? flags ^ flag : flags;
    }

    /** The switch a menu button controls, or 0 for any other button id. */
    public static int flagOfButton(int button) {
        return button >= FIRST_BUTTON && button < FIRST_BUTTON + BUTTON_COUNT ? 1 << (button - FIRST_BUTTON) : 0;
    }

    public static int buttonOf(int flag) {
        return Integer.bitCount(flag) == 1 && (flag & ALL) == flag ? FIRST_BUTTON + Integer.numberOfTrailingZeros(flag) : -1;
    }

    /**
     * What a golem compares with. Entropy alone widens the wanted items of Empty and Use to every tag-equivalent item
     * (original {@code getItemsNeeded(Entropy > 0)}, GolemHelper:432-448), whatever the switches say.
     */
    public static int effective(int flags, int core, int entropyUpgrades) {
        return (core == GolemData.EMPTY_CORE || core == GolemData.USE) && entropyUpgrades > 0 ? flags | TAGS : flags;
    }

    /** Whether a tag stands for an "ore name": a material-specific {@code c:} tag of a material root or a vanilla wood family. */
    public static boolean isGroupTag(ResourceLocation id) {
        if (id == null) return false;
        if (id.getNamespace().equals("c")) {
            int slash = id.getPath().indexOf('/');
            return slash > 0 && slash < id.getPath().length() - 1 && MATERIAL_ROOTS.contains(id.getPath().substring(0, slash));
        }
        return id.getNamespace().equals("minecraft") && WOOD_FAMILIES.contains(id.getPath());
    }

    /** Whether the two stacks share a group tag. Counts are irrelevant. */
    public static boolean sharesGroup(ItemStack stock, ItemStack wanted) {
        return stock.getTags().anyMatch(tag -> isGroupTag(tag.location()) && wanted.is(tag));
    }

    /**
     * Original {@code areItemStacksEqual(stock, wanted, ore, damage, nbt)}: two empties are equal, one empty is not; with no switch
     * the comparison is exact (item and components, never the count).
     */
    public static boolean matches(ItemStack stock, ItemStack wanted, int flags) {
        if (stock.isEmpty() || wanted.isEmpty()) return stock.isEmpty() && wanted.isEmpty();
        if (flags == 0) return ItemStack.isSameItemSameComponents(stock, wanted);
        if ((flags & TAGS) != 0 && sharesGroup(stock, wanted)) return true;
        if (!ItemStack.isSameItem(stock, wanted)) return false;
        if ((flags & IGNORE_COMPONENTS) != 0) return true;
        if ((flags & IGNORE_DAMAGE) != 0) return ItemStack.isSameItemSameComponents(withoutDamage(stock), withoutDamage(wanted));
        return ItemStack.isSameItemSameComponents(stock, wanted);
    }

    private static ItemStack withoutDamage(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        copy.remove(DataComponents.DAMAGE);
        return copy;
    }
}
