package com.example.tc4addon;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.wands.IFocusPouch;
import thaumcraft.api.wands.ItemFocusBasic;

/**
 * Focus Belt: a belt with six cells that the wand's focus selector reads like a focus pouch, worn or carried. It has no
 * screen: a click with a focus in the other hand stores it, a sneaking click gives the last one back.
 */
// [doc:focus-pouch]
public class FocusBelt extends Item implements IBauble, IFocusPouch {
    public static final int CELLS = 6;

    public FocusBelt(Properties properties) { super(properties.stacksTo(1)); }

    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.BELT; }

    /** Every cell, in order, with ItemStack.EMPTY where there is nothing; independent copies the selector may change. */
    @Override public ItemStack[] getInventory(ItemStack pouch) {
        List<ItemStack> stored = pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList();
        // A payload this belt cannot read: the selector then ignores the belt and does not touch it.
        if (stored.size() > CELLS) throw new IllegalArgumentException("a belt has " + CELLS + " cells");
        ItemStack[] cells = new ItemStack[CELLS];
        Arrays.fill(cells, ItemStack.EMPTY);
        for (int cell = 0; cell < stored.size(); cell++) cells[cell] = stored.get(cell); // stream() hands out copies
        return cells;
    }

    /** Stores all cells into the stack it is given and nothing else; the selector also calls it on a scratch copy. */
    @Override public void setInventory(ItemStack pouch, ItemStack[] contents) {
        if (contents.length != CELLS) throw new IllegalArgumentException("a belt has " + CELLS + " cells");
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Arrays.asList(contents))); // copies the stacks
    }

    /**
     * The belt's own use. A pouch is never put on by a click, so unlike the talisman it needs no equipsOnUse: the Baubles
     * menu puts it on, and the selector lists its foci either way. A belt that holds data it cannot read is left alone, as the
     * selector leaves it alone.
     */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack belt = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (level.isClientSide) return InteractionResultHolder.success(belt);
        ItemStack[] cells;
        try { cells = getInventory(belt); }
        catch (IllegalArgumentException unreadable) { return InteractionResultHolder.fail(belt); } // foreign data: change nothing
        if (player.isShiftKeyDown()) {
            for (int cell = CELLS - 1; cell >= 0; cell--) {
                if (cells[cell].isEmpty()) continue;
                player.getInventory().placeItemBackInInventory(cells[cell]);
                cells[cell] = ItemStack.EMPTY;
                break;
            }
        } else if (other.getItem() instanceof ItemFocusBasic) {
            for (int cell = 0; cell < CELLS; cell++) {
                if (!cells[cell].isEmpty()) continue;
                cells[cell] = other.split(1);
                break;
            }
        }
        setInventory(belt, cells);
        return InteractionResultHolder.success(belt);
    }
}
// [/doc]
