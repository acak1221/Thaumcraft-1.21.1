package thaumcraft.common.items.golems;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * The original's damage rules of a golem (oracle {@code thaumcraft/common/entities/golems/EntityGolemBase.java}:1131-1138).
 *
 * <p>The original refused one damage source outright - the source it shared with the traveling trunk - and halved the magic
 * damage of a thaumium golem.
 *
 * <p>Oracle: {@code thaumcraft/common/entities/golems/EntityGolemBase.java}, lines 1131-1138.
 *
 * <p>The two sources are the 1.7.10 {@code DamageSource.field_76367_g} and {@code DamageSource.field_76376_m}. The 1.7.10
 * vanilla names of those fields are {@code cactus} and {@code magic} (1.7.10 {@code net/minecraft/util/DamageSource.java}:
 * {@code public static DamageSource cactus = new DamageSource("cactus")} and
 * {@code public static DamageSource magic = new DamageSource("magic").setDamageBypassesArmor().setMagicDamage()}).
 * The 1.12 legacy tree of this repository maps the same SRG field to {@code IN_WALL} instead, because the SRG-to-MCP mapping
 * changed between 1.7.10 and 1.12; the 1.7.10 oracle is the specification, so the port uses the cactus and the magic source.
 */
public final class GolemDamageRules {
    /** The original's factor for the magic damage of a thaumium golem: {@code par2 *= 0.5f}. */
    public static final float THAUMIUM_MAGIC_FACTOR = 0.5f;
    /**
     * The damage type the original refused outright, the one it shared with the traveling trunk
     * (oracle {@code EntityGolemBase.java}:1133 and {@code EntityTravelingTrunk.java}:107}).
     *
     * <p>The 1.7.10 source is the {@code cactus} one, {@code DamageSource.field_76367_g}.</p>
     */
    public static final ResourceKey<DamageType> IMMUNE_SOURCE = DamageTypes.CACTUS;
    /**
     * The 1.7.10 {@code magic} damage source, {@code DamageSource.field_76376_m}, the one a thaumium golem
     * takes at half strength (oracle {@code EntityGolemBase.java}:1136-1138).
     */
    public static final ResourceKey<DamageType> MAGIC_SOURCE = DamageTypes.MAGIC;

    private GolemDamageRules() {}

    /** Whether the damage source is the one the original refused outright. */
    public static boolean isImmuneSource(DamageSource source) {
        return source != null && source.is(IMMUNE_SOURCE);
    }

    /** Whether the damage source is the 1.7.10 magic source a thaumium golem takes at half strength. */
    public static boolean isMagicSource(DamageSource source) {
        return source != null && source.is(MAGIC_SOURCE);
    }

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
