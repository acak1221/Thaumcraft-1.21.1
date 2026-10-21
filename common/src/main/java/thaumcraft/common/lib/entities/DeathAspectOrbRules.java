package thaumcraft.common.lib.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The original TC4 death aspect orbs.
 *
 * <p>Oracle: {@code thaumcraft/common/lib/events/EventHandlerEntity.java:534-546}. The original
 * drops one aspect orb per PRIMAL aspect of a mob that died while it was recently hit: every
 * primal aspect of the reduced aspect list has a fifty percent chance and the value of the orb
 * is {@code 1 + worldRand.nextInt(amount of that aspect)}.</p>
 *
 * <p>The world facing half lives in {@code thaumcraft.common.entities.DeathAspectOrbDrop}. The class
 * is pure so the draw order and the values can be unit tested without a world.</p>
 */
public final class DeathAspectOrbRules {
    private DeathAspectOrbRules() {}

    /** One dropped orb: the primal aspect and the vis value it carries. */
    public record Drop(Aspect aspect, int value) {}

    /**
     * The original death orbs of a dead mob.
     *
     * <p>The original reduces the scanned aspects of the mob to their primals first
     * ({@code ResearchManager.reduceToPrimals}, oracle
     * {@code thaumcraft/common/lib/research/ResearchManager.java:525-558}), then walks the reduced
     * list in its own order and draws the fifty percent roll and the value roll per aspect.</p>
     *
     * @param entityAspects the scanned aspects of the dead mob, may be null
     * @param nextBoolean   the fifty percent roll of the world random
     * @param nextInt       the value roll of the world random, {@code nextInt(amount)}
     */
    public static List<Drop> drops(AspectList entityAspects, BooleanSupplier nextBoolean, IntUnaryOperator nextInt) {
        if (entityAspects == null || entityAspects.size() == 0) return List.of();
        AspectList reduced = entityAspects.reduceToPrimals(false);
        List<Drop> drops = new ArrayList<>();
        for (Aspect aspect : reduced.getAspects()) {
            if (!nextBoolean.getAsBoolean()) continue;
            int amount = reduced.getAmount(aspect);
            if (amount <= 0) continue;
            drops.add(new Drop(aspect, 1 + nextInt.applyAsInt(amount)));
        }
        return drops;
    }
}
