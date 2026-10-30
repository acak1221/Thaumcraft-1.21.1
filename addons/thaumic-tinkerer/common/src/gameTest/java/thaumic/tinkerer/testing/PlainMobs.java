/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): GameTest support that spawns mobs which are not Thaumcraft champions. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.testing;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Hostile mobs for the GameTests that are not champions.
 *
 * <p>Thaumcraft rolls a champion for some of the hostile mobs that enter a world (zombie, enderman, blaze, skeleton, spider, witch, ...:
 * about 1 in 100 on Normal difficulty). A champion has +30 maximum health, a loot bag among its drops and effects on what it hits, so an exact check
 * (damage dealt, health left, what a kill drops, a health ratio) fails now and then for no fault of the code it checks: a champion zombie has 50
 * maximum health, so a zombie set to 20 health is below half and Valiance answers; a kill that drops an "Uncommon Treasure" has one item too many.</p>
 *
 * <p>These helpers spawn like {@link GameTestHelper} and spawn again when the mob they got is a champion. A champion is recognised
 * by what it has, not by asking Thaumcraft: its maximum health carries a modifier, which a mob of a fresh world never has. Mobs that are made with
 * {@code EntityType.create} and never added to a level are not rolled and need no helper.</p>
 */
final class PlainMobs {
    private static final int TRIES = 100;

    private PlainMobs() {}

    static <T extends Entity> T spawn(GameTestHelper test, EntityType<T> type, Vec3 at) {
        return plain(test, type, () -> test.spawn(type, at));
    }

    static <T extends Entity> T spawn(GameTestHelper test, EntityType<T> type, BlockPos at) {
        return plain(test, type, () -> test.spawn(type, at));
    }

    static <T extends Mob> T spawnWithNoFreeWill(GameTestHelper test, EntityType<T> type, BlockPos at) {
        return plain(test, type, () -> test.spawnWithNoFreeWill(type, at));
    }

    private static <T extends Entity> T plain(GameTestHelper test, EntityType<T> type, Supplier<T> spawn) {
        for (int i = 0; i < TRIES; i++) {
            T entity = spawn.get();
            if (!isChampion(entity)) return entity;
            entity.discard();
        }
        test.assertTrue(false, "every one of " + TRIES + " " + type + " that entered the world was a champion");
        throw new AssertionError("unreachable");
    }

    private static boolean isChampion(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        return health != null && !health.getModifiers().isEmpty();
    }
}
