package thaumcraft.common.lib.research;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.api.IScribeTools;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Original TC4 one-point ink consumption. Native commit methods are for an
 * already validated server action.
 */
public final class ScribingInk {
    private ScribingInk() {}

    /** Damageability is separate from the mod's marker/damage selector. */
    public record State(boolean scribeTools, int damage, int maximum, boolean damageable) {
        public State {
            if (damage < 0 || maximum < 0) throw new IllegalArgumentException("Negative ink durability");
        }

        public boolean usable() { return scribeTools && damage < maximum; }
    }

    /** Accepted consumption may spend zero; discovery of usable ink is independent of its cost. */
    public record Plan(boolean accepted, int beforeDamage, int afterDamage) {
        public boolean spendsPoint() { return afterDamage > beforeDamage; }
    }

    /**
     * Original ResearchManager.consumeInkFromPlayer plus inherited 1.7.10
     * damageItem/attemptDamageItem semantics, without changing any state.
     * boundedRandom is needed only for a positive Unbreaking level. Negative
     * levels follow the original level>0 condition and behave like level zero.
     */
    public static Plan planPlayer(State state, boolean creative, int unbreakingLevel,
                                  IntUnaryOperator boundedRandom) {
        Objects.requireNonNull(state, "state");
        if (!state.usable()) return new Plan(false, state.damage(), state.damage());
        if (creative || !state.damageable()) return new Plan(true, state.damage(), state.damage());
        if (unbreakingLevel > 0) {
            // Native ItemEnchantments bounds levels to 0..255. Bound this pure
            // injected API too, without allowing level+1 integer overflow.
            if (unbreakingLevel == Integer.MAX_VALUE) throw new IllegalArgumentException("Unbreaking bound overflow");
            int bound = unbreakingLevel + 1;
            int roll = Objects.requireNonNull(boundedRandom, "boundedRandom").applyAsInt(bound);
            if (roll < 0 || roll >= bound) throw new IllegalArgumentException("RNG result is outside its ink bound");
            // Quirk kept: original nonarmor negateDamage uses nextInt, not
            // modern RemoveBinomial.nextFloat; zero is the surviving damage.
            if (roll != 0) return new Plan(true, state.damage(), state.damage());
        }
        // Preflight damage<maximum makes overflow and old damage>max breaking
        // unreachable. In particular 99 -> 100 retains the original inkwell.
        return new Plan(true, state.damage(), state.damage() + 1);
    }

    /** Original consumeInkFromTable is a literal increment, without player/enchantment rules. */
    public static Plan planTable(State state) {
        Objects.requireNonNull(state, "state");
        if (!state.usable()) return new Plan(false, state.damage(), state.damage());
        return new Plan(true, state.damage(), state.damage() + 1);
    }

    /**
     * Native adapter. The caller selects the original inventory-order
     * stack and owns server admission/atomic inventory checks. A preflight call
     * never accesses enchantment registries or draws randomness.
     */
    public static boolean consumeFromPlayer(Player player, ItemStack stack, boolean doIt) {
        Objects.requireNonNull(player, "player");
        State state = stateOf(stack);
        if (!state.usable()) return false;
        if (!doIt) return true;

        Plan plan = planFor(player, stack);
        // No shrink/hurtAndBreak: modern >=maximum breaks the empty-at100 item.
        // Avoid even rewriting a component when creative/Unbreakable/Unbreaking
        // prevented damage, matching original zero-mutation branches.
        if (plan.spendsPoint()) stack.setDamageValue(plan.afterDamage());
        return plan.accepted();
    }

    /**
     * The same plan as {@link #consumeFromPlayer} with doIt, but without touching
     * the stack. The caller owns the mutation, so a staged inventory can be built
     * explicitly; only this call draws the Unbreaking roll.
     */
    public static Plan planFor(Player player, ItemStack stack) {
        Objects.requireNonNull(player, "player");
        State state = stateOf(stack);
        if (!state.usable()) return new Plan(false, state.damage(), state.damage());
        boolean creative = player.getAbilities().instabuild;
        int level = 0;
        if (!creative && state.damageable()) {
            var unbreaking = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.UNBREAKING);
            level = EnchantmentHelper.getItemEnchantmentLevel(unbreaking, stack);
        }
        return planPlayer(state, creative, level, bound -> player.getRandom().nextInt(bound));
    }

    /** Separate native table path; it intentionally ignores Unbreakable and Unbreaking. */
    public static boolean consumeFromTable(ItemStack stack, boolean doIt) {
        Plan plan = planTable(stateOf(stack));
        if (!plan.accepted()) return false;
        if (doIt) stack.setDamageValue(plan.afterDamage());
        return true;
    }

    private static State stateOf(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        return new State(!stack.isEmpty() && stack.getItem() instanceof IScribeTools,
                stack.getDamageValue(), stack.getMaxDamage(), stack.isDamageableItem());
    }
}
