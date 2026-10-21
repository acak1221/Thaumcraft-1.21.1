package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.world.damagesource.DamageTypes;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemDamageRules;

/**
 * The damage sources of the original golem and of the original traveling trunk.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/EntityGolemBase.java}:1131-1138 and
 * {@code thaumcraft/common/entities/golems/EntityTravelingTrunk.java}:106-109. Both read the SRG fields
 * {@code DamageSource.field_76367_g} and {@code DamageSource.field_76376_m}; the 1.7.10 vanilla names of those fields are
 * {@code cactus} and {@code magic} (1.7.10 {@code net/minecraft/util/DamageSource.java}), and the SRG-to-MCP mapping of the
 * 1.12 legacy tree of this repository resolves the same field to {@code IN_WALL} instead. The oracle is 1.7.10, so the port
 * refuses the cactus source and halves the magic source.</p>
 */
class GolemDamageRulesTest {
    @Test void theRefusedSourceIsTheCactusOfTheOriginalAndNotTheInWallOne() {
        assertEquals(DamageTypes.CACTUS, GolemDamageRules.IMMUNE_SOURCE);
        assertEquals("cactus", GolemDamageRules.IMMUNE_SOURCE.location().getPath());
        assertNotEquals(DamageTypes.IN_WALL, GolemDamageRules.IMMUNE_SOURCE);
    }

    @Test void theHalvedSourceIsTheMagicOfTheOriginalAndNotTheIndirectOne() {
        assertEquals(DamageTypes.MAGIC, GolemDamageRules.MAGIC_SOURCE);
        assertEquals("magic", GolemDamageRules.MAGIC_SOURCE.location().getPath());
        assertNotEquals(DamageTypes.INDIRECT_MAGIC, GolemDamageRules.MAGIC_SOURCE);
    }

    @Test void onlyTheThaumiumGolemTakesHalfOfTheMagicDamage() {
        assertEquals(0.5F, GolemDamageRules.THAUMIUM_MAGIC_FACTOR);
        assertEquals(4F, GolemDamageRules.amount(true, true, 8F));
        assertEquals(8F, GolemDamageRules.amount(false, true, 8F));
        assertEquals(8F, GolemDamageRules.amount(true, false, 8F));
        assertEquals(8F, GolemDamageRules.amount(false, false, 8F));
        assertEquals(0F, GolemDamageRules.amount(true, true, 0F));
    }
}
