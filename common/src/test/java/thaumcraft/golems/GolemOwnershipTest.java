package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemDamageRules;
import thaumcraft.common.items.golems.GolemOwnership;

/**
 * The original's owner protection of a golem (oracle {@code thaumcraft/common/lib/events/EventHandlerEntity.java}:348-356)
 * and its damage rules (oracle {@code thaumcraft/common/entities/golems/EntityGolemBase.java}:1131-1138).
 */
class GolemOwnershipTest {
    @Test void aGolemWithoutAnOwnerAnswersToEverybody() {
        assertFalse(GolemOwnership.denies("", "Steve"));
        assertFalse(GolemOwnership.denies(null, "Steve"));
        assertTrue(GolemOwnership.allows("", "Steve"));
        assertTrue(GolemOwnership.allows(null, "Steve"));
    }

    @Test void onlyTheOwnerMayInteractWithAnOwnedGolem() {
        assertFalse(GolemOwnership.denies("Steve", "Steve"));
        assertTrue(GolemOwnership.allows("Steve", "Steve"));
        assertTrue(GolemOwnership.denies("Steve", "Alex"));
        assertFalse(GolemOwnership.allows("Steve", "Alex"));
    }

    @Test void aMissingPlayerNameIsNotTheOwner() {
        assertTrue(GolemOwnership.denies("Steve", null));
        assertFalse(GolemOwnership.allows("Steve", null));
    }

    @Test void theOriginalMessageIsTheOneTheLanguageFileCarries() {
        assertEquals("tc.golem.notowner", GolemOwnership.NOT_OWNER_KEY);
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
