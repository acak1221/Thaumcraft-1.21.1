package thaumcraft.api.machines;

import net.minecraft.world.item.ItemStack;

/**
 * The Arcane Bore (block {@code thaumcraft:arcane_bore}, the original {@code TileArcaneBore}): its two tools and what
 * they make of it.
 *
 * <p>The bore digs while it is powered by redstone and holds an Excavation focus (slot 0) and a pickaxe (slot 1) that is
 * not worn down to its last point. Each method answers one question of the original peripheral {@code tt_arcanebore}
 * ({@code hasPickaxe}, {@code hasFocus}, {@code isPickaxeBroken}, {@code isWorking}, {@code getRadius},
 * {@code getSpeed}, {@code hasNativeClusters}, {@code getFortune}, {@code hasSilkTouch}), with the original formula.</p>
 *
 * <p>The tools are saved with the block and synced to clients, so the item questions answer on both sides;
 * {@link #isPowered()} and {@link #isWorking()} read the redstone around the bore and its base, which a client knows
 * only as far as its own copy of the world goes. Read-only: nothing here changes the bore.</p>
 */
public interface IArcaneBore {
    /**
     * Whether slot 0 holds a usable Excavation focus (original {@code hasFocus}).
     *
     * @return true for an Excavation focus with upgrades this version supports
     */
    boolean hasFocus();

    /**
     * Whether slot 1 holds a pickaxe (original {@code hasPickaxe}), whatever its wear.
     *
     * @return true for a single pickaxe
     */
    boolean hasPickaxe();

    /**
     * A copy of the stack in the pickaxe slot (original {@code getStackInSlot(1)}); changing it changes nothing in the
     * bore.
     *
     * @return the pickaxe, or {@link ItemStack#EMPTY}
     */
    ItemStack getPickaxe();

    /**
     * Whether the pickaxe is worn down to its last point (original peripheral {@code isPickaxeBroken}: damage + 1 equals
     * the maximum damage). The bore does not dig with such a pickaxe, so that it never breaks.
     *
     * @return true for a pickaxe one use from breaking; false without a pickaxe
     */
    boolean isPickaxeBroken();

    /**
     * Whether the bore or its base receives redstone power (original {@code gettingPower()}). False for a bore that is
     * not in a loaded, current block of its level; the call never loads a chunk.
     *
     * @return true when powered
     */
    boolean isPowered();

    /**
     * Whether the bore is able to dig now (original peripheral {@code isWorking}): powered, with a focus, and with a
     * damageable pickaxe that is not worn down to its last point. A bore whose saved data could not be read (it keeps
     * the data untouched and does not dig) answers false. It does not say whether there is a block in reach to dig.
     *
     * @return true when the bore can work
     */
    boolean isWorking();

    /**
     * The radius of the hole the bore digs around its axis: 2 plus the Enlarge upgrades of the focus (original
     * {@code maxRadius + area}). The original peripheral's {@code getRadius} answered the width of the hole,
     * {@code 1 + 2 * getRadius()}.
     *
     * @return the radius in blocks
     */
    int getRadius();

    /**
     * The dig speed: the Potency upgrades of the focus plus the Efficiency level of the pickaxe (original
     * {@code speed}).
     *
     * @return the speed bonus, zero without either
     */
    int getSpeed();

    /**
     * Whether the pickaxe is the Pickaxe of the Core ({@code thaumcraft:elemental_pickaxe}), whose special mining turns
     * ores into native clusters (original peripheral {@code hasNativeClusters}).
     *
     * @return true for the Pickaxe of the Core
     */
    boolean hasNativeClusters();

    /**
     * The Fortune level of the pickaxe's own enchantments (original peripheral {@code getFortune}). The bore digs with
     * the higher of this and the Treasure upgrades of its focus.
     *
     * @return the Fortune level, zero without a pickaxe
     */
    int getPickaxeFortune();

    /**
     * Whether the pickaxe has Silk Touch (original peripheral {@code hasSilkTouch}).
     *
     * @return true for a pickaxe with Silk Touch
     */
    boolean hasSilkTouch();
}
