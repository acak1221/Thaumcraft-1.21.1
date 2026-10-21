package thaumcraft.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.ItemSpawnerEgg;

/**
 * The numbers of the native creative spawn egg.
 *
 * <p>Original {@code thaumcraft/common/entities/ItemSpawnerEgg.java:95-102} — the mob is spawned
 * one block towards the clicked face and only then the stack is spent:
 * {@code if (!player.field_71075_bZ.field_75098_d) { --stack.field_77994_a; }}. The creative mode
 * of the player is the only condition, so a creative player never loses the egg.
 *
 * <p>The 1.21.1 {@code SpawnEggItem} always spends the stack: its own {@code useOn} calls
 * {@code itemstack.shrink(1)} in the spawner branch and in the mob branch. The native subclass
 * therefore has to reproduce the original rule itself.
 */
class SpawnerEggParityTest {
    @Test void theCreativeEggIsNeverSpent() {
        assertFalse(ItemSpawnerEgg.spendsStack(true),
                "the original does not spend the egg when the player is in the creative mode");
    }

    @Test void theSurvivalEggIsSpent() {
        assertTrue(ItemSpawnerEgg.spendsStack(false),
                "the original spends the egg when the player is not in the creative mode");
    }

    @Test void theCreativeRuleIsTheOnlyCondition() {
        // The original has exactly one condition, so the rule has exactly two answers.
        assertEquals(2, java.util.Set.of(ItemSpawnerEgg.spendsStack(true), ItemSpawnerEgg.spendsStack(false)).size());
    }
}
