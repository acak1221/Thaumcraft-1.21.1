package thaumcraft.common.crafting;

import java.util.Map;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.ItemGolemPlacer;

/**
 * Named results that patch the infusion's central item instead of producing a fixed stack: the original's
 * {@code addInfusionCraftingRecipe(research, new Object[]{key, tag}, ...)} form (ConfigRecipes:326, 367-370), which copies
 * the centre and sets one NBT tag. The 1.21 data components are whole records, so each patch is a named operation
 * implemented in code; a recipe refers to it by id and keeps an ordinary stack only as the book/tooltip sample.
 */
public final class InfusionResultPatches {
    /** ADVANCEDGOLEM: the golem placer keeps everything it carries and gains the {@code advanced} flag. */
    public static final String ADVANCED_GOLEM = "advanced_golem";

    private interface Patch {
        boolean canApply(ItemStack center);
        ItemStack apply(ItemStack center);
    }

    private static final Map<String, Patch> PATCHES = Map.of(ADVANCED_GOLEM, new Patch() {
        @Override public boolean canApply(ItemStack center) {
            return !center.isEmpty() && center.getItem() instanceof ItemGolemPlacer;
        }

        @Override public ItemStack apply(ItemStack center) {
            ItemStack result = center.copyWithCount(1);
            GolemData data = result.get(ThaumcraftGolems.GOLEM_DATA.get());
            result.set(ThaumcraftGolems.GOLEM_DATA.get(), (data == null ? GolemData.EMPTY : data).withAdvanced(true));
            return result;
        }
    });

    private InfusionResultPatches() {}

    public static boolean known(String id) { return id != null && PATCHES.containsKey(id); }

    /** Whether the patch can be applied to this centre; an unknown id never can. */
    public static boolean canApply(String id, ItemStack center) {
        Patch patch = id == null ? null : PATCHES.get(id);
        return patch != null && center != null && patch.canApply(center);
    }

    /** The patched copy of {@code center}, or {@link ItemStack#EMPTY} when the patch does not apply. */
    public static ItemStack apply(String id, ItemStack center) {
        return canApply(id, center) ? PATCHES.get(id).apply(center) : ItemStack.EMPTY;
    }
}
