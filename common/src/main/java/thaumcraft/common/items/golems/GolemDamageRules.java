package thaumcraft.common.items.golems;

/**
 * The original's damage rules of a golem (oracle {@code thaumcraft/common/entities/golems/EntityGolemBase.java}:1131-1138).
 *
 * <p>The original refused one damage source outright - the source it shared with the traveling trunk - and halved the magic
 * damage of a thaumium golem.
 *
 * <p>Oracle: {@code thaumcraft/common/entities/golems/EntityGolemBase.java}, lines 1131-1138.
 */
public final class GolemDamageRules {
    /** The original's factor for the magic damage of a thaumium golem: {@code par2 *= 0.5f}. */
    public static final float THAUMIUM_MAGIC_FACTOR = 0.5f;

    private GolemDamageRules() {}

    /**
     * The damage a golem actually takes: a thaumium golem takes half of the magic damage, every other golem takes the damage as it is.
     *
     * @param thaumium whether the golem is made of thaumium
     * @param magic whether the damage source is the original's magic source
     */
    public static float amount(boolean thaumium, boolean magic, float amount) {
        return thaumium && magic ? amount * THAUMIUM_MAGIC_FACTOR : amount;
    }
}
