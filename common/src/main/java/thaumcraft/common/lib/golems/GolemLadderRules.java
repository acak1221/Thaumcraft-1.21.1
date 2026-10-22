package thaumcraft.common.lib.golems;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * The pure rules of the goal ladder of the golem.
 *
 * <p>TC4 4.2.3.5 oracle, {@code thaumcraft/common/entities/golems/EntityGolemBase.java:266-351} (the
 * registration of the goals) and the 1.7.10 {@code net.minecraft.entity.ai.EntityAITasks} (the mutex
 * bits). The class is deliberately world-free, so the goals and the tests only have to call it.</p>
 *
 * <p><b>The mutex semantics of the original.</b> The 1.7.10 selector stored the number of a goal
 * together with its task ({@code EntityAITasks$EntityAITaskEntry}) and tested two goals with</p>
 *
 * <pre>{@code (entryA.task.getMutexBits() & entryB.task.getMutexBits()) == 0}</pre>
 *
 * <p>(the official Mojang 1.7.10 {@code client.jar}, {@code net.minecraft.entity.ai.EntityAITasks},
 * method {@code func_75777_a}: {@code return (uk2.a.j() & uk3.a.j()) == 0;}), so two goals exclude
 * each other when their <b>bits overlap</b> and not when their numbers are equal. The bits are the
 * movement (one), the look (two) and both (three). Every goal of the inventory ladder of the
 * original took the bits three, so the five entries of a core exclude each other and the return
 * home excludes them as well, while the watch of the closest player (two) and the flight from a
 * swelling creeper (one) exclude the ladder too — but not the idle look (no bits at all).</p>
 *
 * <p>1.21.1 has no mutex bits: the selector owns a set of exclusive {@link Goal.Flag}s and a goal may
 * only take a flag that no running goal holds. The bits three of the original are therefore the two
 * flags {@link Goal.Flag#MOVE} and {@link Goal.Flag#LOOK}: the ladder reproduces the original's
 * mutual exclusion exactly, and the ladder of a core is the only holder of those two flags while it
 * runs.</p>
 */
public final class GolemLadderRules {
    private GolemLadderRules() {}

    /** Original {@code EntityAIBase.func_75248_a(3)} of every goal of the inventory ladder. */
    public static final int LADDER_MUTEX = 3;

    /** Original {@code EntityAIBase.func_75248_a(1)} of the flight from a swelling creeper. */
    public static final int CREEPER_MUTEX = 1;

    /** Original {@code EntityAIBase.func_75248_a(2)} of the watch of the closest player. */
    public static final int WATCH_MUTEX = 2;

    /** Original {@code EntityAIBase} of the idle look: it takes no mutex bit at all. */
    public static final int IDLE_MUTEX = 0;

    /**
     * The original's mutual exclusion test.
     *
     * @param mutexA the mutex bits of the first goal
     * @param mutexB the mutex bits of the second goal
     * @return whether the two goals may run beside each other
     */
    public static boolean compatible(int mutexA, int mutexB) { return (mutexA & mutexB) == 0; }

    /**
     * The native flags of a goal of the ladder: the movement and the look.
     *
     * <p>The bits three of the original are exactly these two flags, so the ladder of a core keeps
     * the original's mutual exclusion in the 1.21.1 selector.</p>
     */
    public static EnumSet<Goal.Flag> ladderFlags() { return EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK); }

    /**
     * The original's hold of the ladder after a transfer that placed only <b>part</b> of the cargo.
     *
     * <p>The original's place and drop goals set their counter to two hundred when they start
     * ({@code AIEmptyPlace.func_75249_e}:99, {@code AISortingPlace.func_75249_e}:98,
     * {@code AIHomeDrop.func_75249_e}:72) and lower it by one every tick
     * ({@code AIEmptyPlace.func_75246_d}:95). Their continuation rule
     * ({@code AIEmptyPlace.func_75253_b}:80) keeps the goal running while the counter is positive and
     * the goal still wants to run, so a transfer that left cargo behind holds the whole ladder of the
     * core for two hundred ticks — ten seconds — before the next entry may start.</p>
     */
    public static final int PARTIAL_HOLD = 200;

    /** One row of the original's registration of the goals of a core. */
    public record Entry(String core, String goal, int priority, int mutex) {}

    /**
     * The original's registration of the goals of the three inventory cores.
     *
     * <p>Oracle {@code EntityGolemBase.java:270-277} (the Fill core), {@code :278-285} (the Empty
     * core) and {@code :335-341} (the Sorting core). The numbers are the priorities the original
     * registered the goals with and the bits are the mutex bits every one of them took.</p>
     */
    public static final List<Entry> LADDER = List.of(
            new Entry("fill", "AIHomeReplace", 0, LADDER_MUTEX),
            new Entry("fill", "AIHomePlace", 1, LADDER_MUTEX),
            new Entry("fill", "AIHomeDrop", 2, LADDER_MUTEX),
            new Entry("fill", "AIFillTake", 3, LADDER_MUTEX),
            new Entry("fill", "AIFillGoto", 4, LADDER_MUTEX),
            new Entry("empty", "AIHomeReplace", 0, LADDER_MUTEX),
            new Entry("empty", "AIEmptyPlace", 1, LADDER_MUTEX),
            new Entry("empty", "AIEmptyDrop", 2, LADDER_MUTEX),
            new Entry("empty", "AIEmptyGoto", 3, LADDER_MUTEX),
            new Entry("empty", "AIHomeTake", 4, LADDER_MUTEX),
            new Entry("sorting", "AIHomeReplace", 0, LADDER_MUTEX),
            new Entry("sorting", "AISortingPlace", 1, LADDER_MUTEX),
            new Entry("sorting", "AISortingGoto", 3, LADDER_MUTEX),
            new Entry("sorting", "AIHomeTakeSorting", 4, LADDER_MUTEX));

    /** The rows of the ladder of one core, in the order of the original's registration. */
    public static List<Entry> ladderOf(String core) {
        return LADDER.stream().filter(entry -> entry.core().equals(core)).toList();
    }
}
