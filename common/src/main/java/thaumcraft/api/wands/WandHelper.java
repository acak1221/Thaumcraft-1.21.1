package thaumcraft.api.wands;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.foci.FocusData;

/**
 * The public methods of the original {@code thaumcraft.common.items.wands.ItemWandCasting} that add-ons called, as static
 * methods of the API, so a ported add-on never needs the internal wand item.
 *
 * <p>A 1.7.10 call {@code ((ItemWandCasting) stack.getItem()).addVis(stack, Aspect.FIRE, 1, true)} becomes
 * {@code WandHelper.addVis(stack, Aspect.FIRE, 1, true)}. Every method accepts any stack: a stack that is not a wand, staff or
 * sceptre ({@link #isWand}) answers "nothing" (null, zero, an empty list, false) and is never changed.</p>
 *
 * <h2>Units</h2>
 * <ul>
 *   <li>The wand stores <b>centivis</b> (hundredths of vis), like the original NBT: {@link #getVis}, {@link #getAllVis},
 *       {@link #getMaxVis}, {@link #addRealVis}, {@link #consumeVis} and {@link #consumeAllVis} speak centivis
 *       ({@code ItemFocusBasic.getVisCost} is centivis too).</li>
 *   <li>{@link #addVis} and {@link #consumeAllVisCrafting} speak <b>whole vis</b> and multiply by a hundred, like the original.</li>
 *   <li>{@link WandRod#getCapacity()} is whole vis; {@link #getMaxVis} is its centivis capacity (times 1.5 for a sceptre).</li>
 * </ul>
 *
 * <h2>Authority</h2>
 * <p>The wand's vis is server state that the inventory synchronises. Change it on the logical server. The methods that take
 * a player answer exactly as the original but leave the stack unchanged when that player is on the client (the original
 * {@code consumeAllVis} already did so; {@code consumeVis} did not), so a focus may call them on both sides and the client
 * waits for the server's copy. The methods without a player change the stack they are given.</p>
 */
public final class WandHelper {
    private WandHelper() {}

    /** True for a wand, staff or sceptre of Thaumcraft (one item, told apart by its parts and its sceptre marker). */
    public static boolean isWand(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemWandCasting;
    }

    /**
     * Original {@code getRod(stack)}: the rod of the wand, a native view or an add-on rod; null for a non-wand.
     * For a native tag this is always the native view, even when an add-on registered another rod under that tag.
     */
    public static WandRod getRod(ItemStack stack) {
        return isWand(stack) ? WandRod.forGameplay(ItemWandCasting.data(stack).rod()) : null;
    }

    /** Original {@code getCap(stack)}: the cap of the wand, a native view or an add-on cap; null for a non-wand. */
    public static WandCap getCap(ItemStack stack) {
        return isWand(stack) ? WandCap.forGameplay(ItemWandCasting.data(stack).cap()) : null;
    }

    /**
     * Original {@code setRod(stack, rod)}: the wand now has this rod (by {@link WandRod#getTag()}); a staff rod gives it the
     * staff's weapon damage, a wand rod takes it away. Stored vis above the new capacity is cut to it.
     *
     * @throws IllegalArgumentException when the rod's tag is neither native nor registered in {@link WandRod#rods}
     */
    public static void setRod(ItemStack stack, WandRod rod) {
        if (!isWand(stack) || rod == null) return;
        String tag = rod.getTag();
        if (!WandMaterials.supportsRod(tag)) throw new IllegalArgumentException("Unregistered wand rod: " + tag);
        WandData before = ItemWandCasting.data(stack);
        int capacity = WandMaterials.capacity(tag, ItemWandCasting.isSceptre(stack));
        Map<String, Integer> vis = new LinkedHashMap<>();
        before.vis().forEach((aspect, amount) -> vis.put(aspect, Math.min(amount, capacity)));
        ItemWandCasting.withData(stack, new WandData(tag, before.cap(), vis));
    }

    /**
     * Original {@code setCap(stack, cap)}: the wand now has this cap (by {@link WandCap#getTag()}).
     *
     * @throws IllegalArgumentException when the cap's tag is neither native nor registered in {@link WandCap#caps}
     */
    public static void setCap(ItemStack stack, WandCap cap) {
        if (!isWand(stack) || cap == null) return;
        String tag = cap.getTag();
        if (!WandMaterials.supportsCap(tag)) throw new IllegalArgumentException("Unregistered wand cap: " + tag);
        WandData before = ItemWandCasting.data(stack);
        ItemWandCasting.withData(stack, new WandData(before.rod(), tag, before.vis()));
    }

    /** Original {@code isStaff(stack)}: the rod is a {@link StaffRod}. */
    public static boolean isStaff(ItemStack stack) { return isWand(stack) && ItemWandCasting.isStaff(stack); }

    /** Original {@code isSceptre(stack)}: the wand was made by the sceptre recipe. */
    public static boolean isSceptre(ItemStack stack) { return isWand(stack) && ItemWandCasting.isSceptre(stack); }

    /** Original {@code hasRunes(stack)}: the rod is a staff rod with runes (the primal staff). */
    public static boolean hasRunes(ItemStack stack) {
        return isWand(stack) && WandMaterials.rod(ItemWandCasting.data(stack).rod()).hasRunes();
    }

    /** Original {@code getFocus(stack)}: the focus item mounted on the wand, or null. */
    public static ItemFocusBasic getFocus(ItemStack stack) {
        return getFocusItem(stack).getItem() instanceof ItemFocusBasic focus ? focus : null;
    }

    /**
     * Original {@code getFocusItem(stack)}: a copy of the mounted focus stack. Forced 1.21.1 change: an empty stack, never null,
     * when no focus is mounted.
     */
    public static ItemStack getFocusItem(ItemStack stack) { return isWand(stack) ? FocusData.get(stack) : ItemStack.EMPTY; }

    /**
     * Original {@code setFocus(stack, focus)}: mounts a copy of one focus (native or any add-on {@link ItemFocusBasic}), or
     * removes the focus for an empty/null stack. The caller owns the focus item it takes from the player.
     *
     * @return false when the stack is not a wand or the focus cannot be mounted (not a focus, a focus carrying a wand or a
     *         focus, or upgrade ranks its own {@code getPossibleUpgradesByRank} does not allow); the wand is then unchanged
     */
    public static boolean setFocus(ItemStack stack, ItemStack focus) {
        if (!isWand(stack)) return false;
        if (focus == null || focus.isEmpty()) {
            stack.remove(ThaumcraftFoci.WAND_FOCUS.get());
            return true;
        }
        ItemStack one = focus.copyWithCount(1);
        if (!FocusData.isSupportedFocus(one)) return false;
        stack.set(ThaumcraftFoci.WAND_FOCUS.get(), new FocusData(one));
        return true;
    }

    /** Original {@code getMaxVis(stack)}: the capacity of one primal in centivis ({@code rod capacity * 100}, a sceptre 150). */
    public static int getMaxVis(ItemStack stack) { return isWand(stack) ? ItemWandCasting.getMaxVis(stack) : 0; }

    /** Original {@code getVis(stack, aspect)}: the stored centivis of one aspect; zero for a non-primal or null aspect. */
    public static int getVis(ItemStack stack, Aspect aspect) {
        return isWand(stack) && aspect != null && aspect.getTag() != null ? ItemWandCasting.getVis(stack, aspect.getTag()) : 0;
    }

    /** Original {@code getAllVis(stack)}: a new list of the six primals with their stored centivis (zero included). */
    public static AspectList getAllVis(ItemStack stack) {
        AspectList all = new AspectList();
        if (!isWand(stack)) return all;
        for (Aspect primal : Aspect.getPrimalAspects()) all.merge(primal, getVis(stack, primal));
        return all;
    }

    /** Original {@code getAspectsWithRoom(stack)}: the primals below the capacity, each with the amount one. */
    public static AspectList getAspectsWithRoom(ItemStack stack) {
        AspectList room = new AspectList();
        if (!isWand(stack)) return room;
        int max = getMaxVis(stack);
        AspectList all = getAllVis(stack);
        for (Aspect aspect : all.getAspects()) if (all.getAmount(aspect) < max) room.add(aspect, 1);
        return room;
    }

    /**
     * Original {@code addVis(stack, aspect, amount, doit)}: adds {@code amount} WHOLE vis of a primal, up to the capacity.
     *
     * @return the whole vis that did not fit (the original's {@code leftover / 100}); zero for a non-primal aspect, a non-wand
     *         or an amount that is not positive (the original would have drained the wand with a negative one)
     */
    public static int addVis(ItemStack stack, Aspect aspect, int amount, boolean doit) {
        if (!isPrimal(aspect) || !isWand(stack) || amount <= 0) return 0;
        int centivis = (int) Math.min((long) amount * 100L, Integer.MAX_VALUE);
        return ItemWandCasting.addRealVis(stack, aspect.getTag(), centivis, doit) / 100;
    }

    /**
     * Original {@code addRealVis(stack, aspect, amount, doit)}: adds {@code amount} CENTIVIS of a primal, up to the capacity.
     *
     * @return the centivis that did not fit; zero for a non-primal aspect, a non-wand or an amount that is not positive
     */
    public static int addRealVis(ItemStack stack, Aspect aspect, int amount, boolean doit) {
        if (!isPrimal(aspect) || !isWand(stack) || amount <= 0) return 0;
        return ItemWandCasting.addRealVis(stack, aspect.getTag(), amount, doit);
    }

    /**
     * Original {@code getConsumptionModifier(stack, player, aspect, crafting)}: the cap's modifier for the aspect (its special
     * one when the cap lists it), minus the player's vis discount gear and the vis exhaustion surcharge, minus the frugal upgrade
     * of the mounted focus unless {@code crafting}, minus a tenth for a sceptre; never below a tenth. A null player has no gear.
     * One for a non-wand.
     */
    public static float getConsumptionModifier(ItemStack stack, Player player, Aspect aspect, boolean crafting) {
        if (!isWand(stack)) return 1.0F;
        return crafting ? WandVisDiscount.modifier(stack, player, aspect) : WandVisDiscount.focusModifier(stack, player, aspect);
    }

    /**
     * Original {@code consumeVis(stack, player, aspect, amount, crafting)}: pays {@code (int) (amount * modifier)} CENTIVIS of one
     * aspect when the wand holds that much.
     *
     * @return true when it was (or, for a client player, would be) paid; a price that truncates to zero or below is free
     */
    public static boolean consumeVis(ItemStack stack, Player player, Aspect aspect, int amount, boolean crafting) {
        if (!isWand(stack) || aspect == null) return false;
        AspectList cost = new AspectList().add(aspect, amount);
        return consumeAllVis(stack, player, cost, true, crafting);
    }

    /**
     * Original {@code consumeAllVis(stack, player, aspects, doit, crafting)}: prices every aspect at
     * {@code (int) (amount * getConsumptionModifier(...))} CENTIVIS and pays all of them or none.
     *
     * @param doit false only checks; true pays (on the server: a client player never changes the stack)
     * @return false for a null/empty list, a non-wand, or when any priced aspect is missing; true otherwise
     */
    public static boolean consumeAllVis(ItemStack stack, Player player, AspectList aspects, boolean doit, boolean crafting) {
        if (aspects == null || aspects.size() == 0 || !isWand(stack)) return false;
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (Aspect aspect : aspects.getAspects()) {
            int price = (int) (aspects.getAmount(aspect) * getConsumptionModifier(stack, player, aspect, crafting));
            if (price <= 0) continue; // the original's "getVis >= price" holds; a negative price never refills the wand
            if (!isPrimal(aspect)) return false; // a wand holds primals only: the original getVis answered zero
            prices.merge(aspect.getTag(), price, Integer::sum);
        }
        if (prices.isEmpty()) return true;
        // The native casting payment: atomic, primal-only, never below zero.
        if (!FocusVis.canPay(stack, prices)) return false;
        if (doit && (player == null || !player.level().isClientSide)) FocusVis.debit(stack, prices);
        return true;
    }

    /**
     * Original {@code consumeAllVisCrafting(stack, player, aspects, doit)}: like {@link #consumeAllVis} with {@code crafting} set,
     * for amounts in WHOLE vis (multiplied by a hundred first), as an arcane recipe declares them.
     */
    public static boolean consumeAllVisCrafting(ItemStack stack, Player player, AspectList aspects, boolean doit) {
        if (aspects == null || aspects.size() == 0) return false;
        AspectList centivis = new AspectList();
        for (Aspect aspect : aspects.getAspects()) centivis.add(aspect, (int) Math.min((long) aspects.getAmount(aspect) * 100L, Integer.MAX_VALUE));
        return consumeAllVis(stack, player, centivis, doit, true);
    }

    /** Original {@code getFocusPotency(stack)}: the potency upgrades of the focus, plus one on a staff with runes; zero without a focus. */
    public static int getFocusPotency(ItemStack stack) {
        ItemFocusBasic focus = getFocus(stack);
        if (focus == null) return 0;
        return WandMaterials.focusPotency(focus.getUpgradeLevel(getFocusItem(stack), FocusUpgradeType.potency),
                WandMaterials.rod(ItemWandCasting.data(stack).rod()));
    }

    /** Original {@code getFocusTreasure(stack)}: the treasure (fortune) upgrades of the mounted focus; zero without a focus. */
    public static int getFocusTreasure(ItemStack stack) { return focusLevel(stack, FocusUpgradeType.treasure); }

    /** Original {@code getFocusFrugal(stack)}: the frugal upgrades of the mounted focus; zero without a focus. */
    public static int getFocusFrugal(ItemStack stack) { return focusLevel(stack, FocusUpgradeType.frugal); }

    /** Original {@code getFocusEnlarge(stack)}: the enlarge upgrades of the mounted focus; zero without a focus. */
    public static int getFocusEnlarge(ItemStack stack) { return focusLevel(stack, FocusUpgradeType.enlarge); }

    /** Original {@code getFocusExtend(stack)}: the extend upgrades of the mounted focus; zero without a focus. */
    public static int getFocusExtend(ItemStack stack) { return focusLevel(stack, FocusUpgradeType.extend); }

    private static int focusLevel(ItemStack stack, FocusUpgradeType type) {
        ItemFocusBasic focus = getFocus(stack);
        return focus == null ? 0 : focus.getUpgradeLevel(getFocusItem(stack), type);
    }

    private static boolean isPrimal(Aspect aspect) {
        return aspect != null && aspect.isPrimal() && WandData.PRIMALS.contains(aspect.getTag());
    }
}
