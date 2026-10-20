package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/** Original createClue: no parent/clue/material admission, one world-RNG draw, then only the @KEY marker. */
public final class ResearchClues {
    /** Immutable server-observed scan witness. A node/other phenomenon has no witness and cannot produce a clue. */
    public record Context(ItemStack item, String entityName) {
        public Context {
            if ((item == null || item.isEmpty()) == (entityName == null || entityName.isEmpty()))
                throw new IllegalArgumentException("A scan clue requires exactly one item or original entity name");
            item = item == null ? ItemStack.EMPTY : item.copyWithCount(1);
        }
        @Override public ItemStack item() { return item.copy(); }
        public static Context item(ItemStack stack) { return new Context(Objects.requireNonNull(stack), null); }
        public static Context entity(String originalName) { return new Context(ItemStack.EMPTY, Objects.requireNonNull(originalName)); }
    }
    private ResearchClues() {}

    /** Called after a successful native scan and before knowledge synchronization. */
    public static String reveal(PlayerKnowledge actor, Context context, Map<String, Integer> awarded, IntUnaryOperator worldRandom) {
        if (context == null) return null;
        return reveal(actor, key -> ResearchTriggerBindings.matches(key, context), awarded, worldRandom);
    }

    /** Pure matcher contract also permits original RNG vectors without inventing world or packet inputs. */
    public static String reveal(PlayerKnowledge actor, Predicate<String> directMatch, Map<String, Integer> awarded, IntUnaryOperator worldRandom) {
        Objects.requireNonNull(worldRandom, "worldRandom");
        List<String> keys = eligible(actor, directMatch, awarded);
        if (keys.isEmpty()) return null;
        int index = worldRandom.applyAsInt(keys.size());
        if (index < 0 || index >= keys.size()) throw new IllegalArgumentException("Out-of-range research-clue draw");
        String key = keys.get(index);
        actor.addResearch("@" + key);
        return key;
    }

    public static List<String> eligible(PlayerKnowledge actor, Predicate<String> directMatch, Map<String, Integer> awarded) {
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(directMatch, "directMatch"); Objects.requireNonNull(awarded, "awarded");
        List<String> keys = new ArrayList<>();
        for (var research : ResearchRegistrationOrder.enabled()) {
            if (research.aspectCost().isEmpty() || !(research.isHidden() || research.isLost())
                    || actor.isResearchComplete(research.key()) || actor.isResearchComplete("@" + research.key())) continue;
            boolean matches = (!research.itemTriggerExpressions().isEmpty() || !research.entityTriggers().isEmpty()) && directMatch.test(research.key());
            if (!matches) for (String aspect : research.aspectTriggers()) {
                if (awarded.getOrDefault(aspect, 0) > 0) { matches = true; break; }
            }
            if (matches) keys.add(research.key());
        }
        return List.copyOf(keys);
    }
}
