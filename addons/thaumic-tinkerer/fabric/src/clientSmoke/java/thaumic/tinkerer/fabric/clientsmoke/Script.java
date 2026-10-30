package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

/**
 * A short sequence of steps that the client tick runs one at a time: "put the camera there, wait for six frames, take
 * the picture". Test-only code of the port (no 1.7.10 counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>A step returns {@code true} when it is finished and {@code false} when it wants to be called again on the next tick.
 * A step that waits has a time limit; a step that throws or times out ends the script, and the owner records it. The
 * scripts of a phase are independent of each other, so one failing script does not hide the others.</p>
 */
final class Script {
    /** One step. {@code true} = done. */
    @FunctionalInterface
    interface Step {
        boolean tick(SmokeRun run) throws Exception;
    }

    /** Thrown by a step that already recorded its result and wants the rest of the script skipped (not a harness error). */
    static final class Abort extends RuntimeException {
        Abort() {
            super(null, null, false, false);
        }
    }

    /** The outcome of a tick of the script. */
    enum State { RUNNING, DONE, FAILED }

    private record Entry(String label, Step step, long limitNanos) {}

    private final String name;
    private final Deque<Entry> steps = new ArrayDeque<>();
    private long stepStarted;
    private boolean started;
    private String failure;
    private Throwable failureCause;

    Script(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    String failure() {
        return failure;
    }

    Throwable failureCause() {
        return failureCause;
    }

    /** A step with the default time limit (30 s). */
    Script then(String label, Step step) {
        return then(label, step, 30);
    }

    Script then(String label, Step step, int limitSeconds) {
        steps.addLast(new Entry(label, step, limitSeconds * 1_000_000_000L));
        return this;
    }

    /** A step that runs once, without waiting. */
    Script run(String label, ThrowingRunnable action) {
        return then(label, run -> {
            action.run(run);
            return true;
        });
    }

    /** Waits until {@code condition} holds; a timeout is a failure of the script with {@code label} as the reason. */
    Script until(String label, BooleanSupplier condition, int limitSeconds) {
        return then("wait for " + label, run -> condition.getAsBoolean(), limitSeconds);
    }

    /** Waits for {@code count} rendered frames (see {@link Frames}). */
    Script frames(int count) {
        long[] target = { -1 };
        return then("wait " + count + " frames", run -> {
            if (target[0] < 0) target[0] = run.frames().count() + count;
            return run.frames().count() >= target[0];
        }, 60);
    }

    /** Waits for the given number of client ticks (20 per second at best; the game catches up when frames are slow). */
    Script ticks(int count) {
        int[] left = { count };
        return then("wait " + count + " ticks", run -> --left[0] <= 0, 60);
    }

    State tick(SmokeRun run) {
        if (failure != null) return State.FAILED;
        Entry head = steps.peekFirst();
        if (head == null) return State.DONE;
        long now = System.nanoTime();
        if (!started) {
            started = true;
            stepStarted = now;
        }
        try {
            if (head.step().tick(run)) {
                steps.pollFirst();
                started = false;
                return steps.isEmpty() ? State.DONE : State.RUNNING;
            }
        } catch (Abort abort) {
            steps.clear();
            return State.DONE;
        } catch (Exception | LinkageError e) {
            failure = "step '" + head.label() + "' threw " + e;
            failureCause = e;
            return State.FAILED;
        }
        if (now - stepStarted > head.limitNanos()) {
            failure = "step '" + head.label() + "' did not finish in " + head.limitNanos() / 1_000_000_000L + " s";
            return State.FAILED;
        }
        return State.RUNNING;
    }

    /** An action that may throw. */
    @FunctionalInterface
    interface ThrowingRunnable {
        void run(SmokeRun run) throws Exception;
    }
}
