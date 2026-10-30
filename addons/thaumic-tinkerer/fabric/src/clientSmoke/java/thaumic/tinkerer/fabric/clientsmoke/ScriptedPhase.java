package thaumic.tinkerer.fabric.clientsmoke;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A phase made of independent {@link Script}s run one after the other. Test-only code of the port (no 1.7.10
 * counterpart); CC BY-NC-SA 3.0 like the add-on, not distributed.
 *
 * <p>The scripts are built on the first tick (the world is there by then). A script that fails is recorded under its own
 * name and the next one starts, so a block row that cannot be framed does not cost the item pictures.</p>
 */
abstract class ScriptedPhase implements Phase {
    private Deque<Script> queue;
    private Script current;

    /** Builds the scripts of the phase; called once. */
    protected abstract List<Script> scripts(SmokeRun run) throws Exception;

    @Override
    public boolean needsWorld() {
        return true;
    }

    @Override
    public boolean tick(SmokeRun run) throws Exception {
        if (queue == null) queue = new ArrayDeque<>(scripts(run));
        if (current == null) {
            current = queue.pollFirst();
            if (current == null) return true;
        }
        switch (current.tick(run)) {
            case RUNNING -> { }
            case DONE -> {
                current = null;
                run.report().write();
            }
            case FAILED -> {
                String failure = current.failure();
                if (current.failureCause() != null) run.report().harnessError(name(), current.name(), current.failureCause());
                else run.report().add(name(), current.name(), "", "", SmokeReport.Result.HARNESS_ERROR, failure);
                run.onScriptFailed(current);
                current = null;
                run.report().write();
            }
        }
        return false;
    }
}
