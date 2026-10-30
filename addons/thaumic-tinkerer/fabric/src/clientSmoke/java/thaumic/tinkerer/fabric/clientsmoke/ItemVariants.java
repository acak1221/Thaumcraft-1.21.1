package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.item.ItemCleansingTalisman;
import thaumic.tinkerer.common.item.ItemInfusedSeeds;
import thaumic.tinkerer.common.item.ItemXPTalisman;
import thaumic.tinkerer.common.item.kami.KamiComponents;
import thaumic.tinkerer.common.item.kami.tool.ToolHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * The stacks whose model changes with their state: the item-model overrides of the add-on, set to the state that switches
 * them. Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The states are set with the add-on's own setters and components (the ones the items write when a player uses them), not by
 * hand-building NBT. This is the one hand-written list of the harness that is about the add-on's items: a new override needs a
 * line here to be exercised, its predicate is still looked up by {@link OverridesCheck} without it.</p>
 */
final class ItemVariants {
    /** A stack in a non-default state, the path of its item, and the words for its label. */
    record Variant(String path, String note, ItemStack stack) {}

    /** The note of the seed variants: {@link OverridesCheck} also requires their models to differ from each other. */
    static final String SEEDS_NOTE_PREFIX = "seeds of ";

    private ItemVariants() {}

    static List<Variant> all() {
        List<Variant> variants = new ArrayList<>();
        for (String tool : new String[] { LibItemNames.ICHOR_PICK_GEM, LibItemNames.ICHOR_SHOVEL_GEM, LibItemNames.ICHOR_AXE_GEM, LibItemNames.ICHOR_SWORD_GEM }) {
            ItemStack stack = TTRegistry.stack(tool);
            ToolHandler.changeMode(stack);
            variants.add(new Variant(tool, "mode 1", stack.copy()));
            ToolHandler.changeMode(stack);
            variants.add(new Variant(tool, "mode 2", stack.copy()));
        }
        ItemStack cleansing = TTRegistry.stack(LibItemNames.CLEANSING_TALISMAN);
        ItemCleansingTalisman.setEnabled(cleansing, true);
        variants.add(new Variant(LibItemNames.CLEANSING_TALISMAN, "enabled", cleansing));
        ItemStack xp = TTRegistry.stack(LibItemNames.XP_TALISMAN);
        ItemXPTalisman.setAbsorbing(xp, true);
        variants.add(new Variant(LibItemNames.XP_TALISMAN, "absorbing", xp));
        ItemStack ring = TTRegistry.stack(LibItemNames.BLOCK_TALISMAN);
        ring.set(KamiComponents.TALISMAN_ACTIVE.get(), true);
        variants.add(new Variant(LibItemNames.BLOCK_TALISMAN, "active", ring));
        ItemStack blade = TTRegistry.stack(LibItemNames.BLOOD_SWORD);
        blade.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        variants.add(new Variant(LibItemNames.BLOOD_SWORD, "activated", blade));
        for (Aspect primal : Aspect.getPrimalAspects()) {
            variants.add(new Variant(LibItemNames.INFUSED_SEEDS, SEEDS_NOTE_PREFIX + primal.getTag(), ItemInfusedSeeds.creativeStack(primal)));
        }
        return variants;
    }
}
