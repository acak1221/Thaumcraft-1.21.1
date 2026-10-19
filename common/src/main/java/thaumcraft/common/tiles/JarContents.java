package thaumcraft.common.tiles;

import thaumcraft.api.aspects.Aspect;

/** Both TC4 jar variants store at most 64 units of one aspect. */
public final class JarContents {
    public static final int CAPACITY = 64;
    private String tag;
    private int amount;

    public String tag() { return tag; }
    public int amount() { return amount; }
    public boolean accepts(String aspect) {
        return Aspect.getAspect(aspect) != null && (amount == 0 || aspect.equals(tag));
    }
    public int add(String aspect, int requested) {
        return add(aspect, requested, false);
    }
    public int add(String aspect, int requested, boolean voidOverflow) {
        if (requested <= 0 || !accepts(aspect)) return requested;
        // Bound the addition BEFORE adding, including Integer.MAX_VALUE offers.
        // A void jar consumes the whole compatible offer, not extra storage.
        int accepted = Math.min(requested, CAPACITY - amount);
        if (accepted > 0) { tag = aspect; amount += accepted; }
        return voidOverflow ? 0 : requested - accepted;
    }
    public boolean take(String aspect, int requested) {
        if (requested <= 0 || aspect == null || !aspect.equals(tag) || amount < requested) return false;
        amount -= requested;
        if (amount == 0) tag = null;
        return true;
    }
    public void restore(String aspect, int savedAmount) {
        tag = null; amount = 0;
        if (Aspect.getAspect(aspect) != null && savedAmount > 0) {
            tag = aspect; amount = Math.min(CAPACITY, savedAmount);
        }
    }
}
