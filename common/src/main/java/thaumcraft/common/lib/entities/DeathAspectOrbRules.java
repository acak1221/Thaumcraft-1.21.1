package thaumcraft.common.lib.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The original death aspect orbs.
 *
 * <p>Oracle {@code thaumcraft/common/lib/events/EventHandlerEntity.java:508-516} (CFR 0.152
 * decompilation of {@code Thaumcraft-1.7.10-4.2.3.5.jar}):</p>
 *
 * <pre>
 * } else if (!event.entityLiving.field_70170_p.field_72995_K &amp;&amp; EntityUtils.getRecentlyHit(event.entityLiving) &gt; 0) {
 *    AspectList aspectsCompound = ScanManager.generateEntityAspects((Entity)event.entityLiving);
 *    if (aspectsCompound != null &amp;&amp; aspectsCompound.size() &gt; 0) {
 *       AspectList aspects = ResearchManager.reduceToPrimals(aspectsCompound);
 *       for (Aspect aspect : aspects.getAspects()) {
 *          if (!event.entityLiving.field_70170_p.field_73012_v.nextBoolean()) continue;
 *          EntityAspectOrb orb = new EntityAspectOrb(event.entityLiving.field_70170_p, ..., aspect,
 *                  1 + event.entityLiving.field_70170_p.field_73012_v.nextInt(aspects.getAmount(aspect)));
 *          event.entityLiving.field_70170_p.func_72838_d((Entity)orb);
 *       }
 *    }
 * }
 * </pre>
 *
 * <p>So: one roll of the WORLD random per <em>primal</em> aspect of the dead mob, a fifty percent
 * chance of an orb, and a value of {@code 1 + nextInt(amount of that aspect)} from the same world
 * random. The reduction to the primals is the original
 * {@code ResearchManager.reduceToPrimals(al, false)} (oracle
 * {@code thaumcraft/common/lib/research/ResearchManager.java:466-495}), which the native
 * {@link AspectList#reduceToPrimals(boolean)} reproduces in the same order.</p>
 *
 * <p>Forced 1.21.1 change: the original would call {@code nextInt(0)} for a primal without an
 * amount and crash the death with an {@code IllegalArgumentException}. The native rule skips such a
 * primal instead of taking the server down.</p>
 */
public final class DeathAspectOrbRules {
    /** One orb of the original: an aspect and the value it carries. */
    public record Drop(Aspect aspect, int value) {}

    private DeathAspectOrbRules() {}

    /**
     * The original orb block of a death.
     *
     * @param aspects the scanned aspects of the dead mob
     * @param nextBoolean the fifty percent roll of the world random
     * @param nextInt the value roll of the world random
     * @return the orbs of the death, in the original order
     */
    public static List<Drop> drops(AspectList aspects, BooleanSupplier nextBoolean, IntUnaryOperator nextInt) {
        List<Drop> drops = new ArrayList<>();
        if (aspects == null || nextBoolean == null || nextInt == null) return drops;
        AspectList primals = aspects.reduceToPrimals(false);
        for (Aspect aspect : primals.getAspects()) {
            if (!nextBoolean.getAsBoolean()) continue;
            int amount = primals.getAmount(aspect);
            if (amount <= 0) continue;
            int value = 1 + nextInt.applyAsInt(amount);
            if (value <= 0 || value > Short.MAX_VALUE) continue;
            drops.add(new Drop(aspect, value));
        }
        return drops;
    }
}
