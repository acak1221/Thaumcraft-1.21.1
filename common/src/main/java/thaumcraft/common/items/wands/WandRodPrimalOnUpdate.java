package thaumcraft.common.items.wands;

import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.baubles.RunicBaubleRules;

/**
 * Original WandRodPrimalOnUpdate: the passive of a wand rod. An elemental rod (earth, fire, water, order, entropy or air, as a
 * wand rod or as a staff core) carries a fixed aspect and refills it in the wand it sits in: on every two hundredth tick of the
 * carrier, if the wand holds less than a tenth of its capacity of that aspect, one whole vis is added. The primal staff core has
 * no fixed aspect: on every fiftieth tick it adds one whole vis to ONE primal picked at random among the primals that are below a
 * tenth of the capacity. The native wand applies the same rule on the carrier's tick through {@link WandRodPassive}; this class
 * is the original's own rod passive, with the decision in {@link RunicBaubleRules}.
 */
public final class WandRodPrimalOnUpdate {
    /** The passive of a rod tag, or null when the rod has none. */
    private final String aspect;

    private WandRodPrimalOnUpdate(String aspect) { this.aspect = aspect; }

    /** The passive of a native rod tag; null when that rod never refills anything. */
    public static WandRodPrimalOnUpdate of(String rodTag) {
        return new WandRodPrimalOnUpdate(WandMaterials.rod(rodTag).passive());
    }

    /** The passive of an original ItemWandRod metadata value. */
    public static WandRodPrimalOnUpdate of(int originalMetadata) {
        return of(RunicBaubleRules.rod(originalMetadata).tag());
    }

    /** The passive of the primal staff core: it refills any of the six primals. */
    public static WandRodPrimalOnUpdate anyPrimalStaff() { return new WandRodPrimalOnUpdate(null); }

    /** The fixed aspect this rod refills, or null for the any-primal case and for a rod without a passive. */
    public String aspect() { return aspect; }

    /** True for the primal staff core, the only rod without a fixed aspect. */
    public boolean anyPrimal() { return aspect == null; }

    /** The original onUpdate: the carrier's tick refills the wand this rod sits in. */
    public void onUpdate(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || player == null) return;
        WandData before = ItemWandCasting.data(stack);
        WandData after = aspect == null ? anyPrimal(before, player) : fixedAspect(before, player);
        if (after != before) stack.set(ThaumcraftContent.WAND_DATA.get(), after);
    }

    private WandData fixedAspect(WandData wand, Player player) {
        int amount = wand.amount(aspect);
        int refilled = RunicBaubleRules.fixedAspectRefill(player.tickCount, wand.capacity(), amount, aspect);
        return refilled == amount ? wand : wand.withAmount(aspect, refilled);
    }

    private WandData anyPrimal(WandData wand, Player player) {
        Map<String, Integer> vis = wand.vis();
        RunicBaubleRules.AnyPrimalRefill refill = RunicBaubleRules.anyPrimalRefill(player.tickCount, wand.capacity(), vis,
                player.getRandom()::nextInt);
        if (!refill.due()) return wand;
        return wand.withAmount(refill.aspect(), refill.amount());
    }

    /** The primals below a tenth of the capacity, in the original primal order. */
    public static List<String> lowPrimals(WandData wand) {
        return RunicBaubleRules.PRIMALS.stream()
                .filter(primal -> wand.amount(primal) < RunicBaubleRules.refillThreshold(wand.capacity())).toList();
    }
}
